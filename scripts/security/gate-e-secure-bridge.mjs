import { pathToFileURL } from 'node:url';
import {
  concatHex, createPublicClient, http, numberToHex,
} from 'viem';
import { sepolia } from 'viem/chains';
import { readCredential } from '../ensv2/access-record.mjs';
import { demoConfig } from './demo-config.mjs';
import { recordAttempt } from '../../demo/evidence.mjs';
import { requireCondition } from '../ensv2/contracts.mjs';
import { gateESerialMaxLineBytes, gateETiming } from './gate-e-config.mjs';
import {
  IssuedChallengeStore, issueAccessChallenge, verificationReason, verifyAccessAttempt,
} from './holder-proof.mjs';

export const gateECredential = demoConfig.credential;
export const gateEIntendedOwner = demoConfig.expectedOwner;
export const gateEResourceName = demoConfig.resourceName;
export const gateEResourceId = demoConfig.resourceId;

export const gateEOutcome = Object.freeze({
  TRANSPORT_FAILURE: 'TRANSPORT_FAILURE',
  VERIFIER_ALLOW: 'VERIFIER_ALLOW',
  VERIFIER_DENY: 'VERIFIER_DENY',
  CONTROLLER_CONFIRMED: 'CONTROLLER_CONFIRMED',
  CONTROLLER_UNCONFIRMED: 'CONTROLLER_UNCONFIRMED',
});

const proofPattern = /^PROOF=(0x[0-9a-fA-F]{130})$/;
const firmwareAuthorizationPattern = /^AUTHORIZATION: (ALLOW|DENY)$/;
const firmwareStopPrefix = 'GATE_E: STOP';
const firmwareStopPattern = /^GATE_E: STOP - ([A-Z_]+): .+$/;
const knownFirmwareStopStages = new Set([
  'INITIALIZATION', 'TARGET_ACTIVATION', 'SELECT', 'CHALLENGE', 'SEND_CHALLENGE',
  'STATUS', 'GET_SIGNATURE', 'SIGNATURE_LENGTH', 'AUTHORIZATION', 'SERIAL_INPUT',
]);
const maxUint64 = (1n << 64n) - 1n;
const systemNowMs = () => Date.now();
const systemNowSeconds = () => BigInt(Math.floor(Date.now() / 1000));

export class GateESerialFinalizationError extends Error {
  constructor(reason, message, details = {}) {
    super(message);
    this.name = 'GateESerialFinalizationError';
    this.reason = reason;
    Object.assign(this, details);
  }
}

export class GateESnapshotError extends Error {
  constructor(reason) {
    super(`Gate E snapshot failed: ${reason}.`);
    this.name = 'GateESnapshotError';
    this.reason = reason;
  }
}

export function serializeChallenge(challenge) {
  const payload = concatHex([
    challenge.credential,
    challenge.resource,
    challenge.nonce,
    numberToHex(challenge.expiresAt, { size: 8 }),
  ]);
  requireCondition(/^0x[0-9a-f]{208}$/.test(payload),
    'Gate E challenge must serialize to exactly 104 bytes.');
  return `CHALLENGE=${payload}`;
}

export function parseProofLine(line) {
  const match = proofPattern.exec(line.replace(/\r$/, ''));
  requireCondition(Boolean(match), 'Gate E proof must be exactly 65-byte hexadecimal.');
  return match[1].toLowerCase();
}

export function parseFirmwareStop(line) {
  if (!line.startsWith(firmwareStopPrefix)) return null;
  const stage = firmwareStopPattern.exec(line)?.[1];
  return {
    stage,
    reason: stage && knownFirmwareStopStages.has(stage)
      ? `FIRMWARE_STOP_${stage}` : 'UNKNOWN_FIRMWARE_STOP',
  };
}

export class GateELineBuffer {
  constructor(maxLineBytes = gateESerialMaxLineBytes) {
    requireCondition(Number.isSafeInteger(maxLineBytes) && maxLineBytes > 0,
      'Serial line limit must be a positive safe integer.');
    this.maxLineBytes = maxLineBytes;
    this.bytes = [];
    this.overflowed = false;
  }

  push(chunk) {
    if (this.overflowed) return { lines: [], overflow: true };
    const input = Buffer.isBuffer(chunk) ? chunk : Buffer.from(chunk);
    const lines = [];
    for (const byte of input) {
      if (byte === 0x0a) {
        lines.push(Buffer.from(this.bytes).toString('utf8'));
        this.bytes.length = 0;
        continue;
      }
      if (this.bytes.length === this.maxLineBytes) {
        this.bytes.length = 0;
        this.overflowed = true;
        return { lines, overflow: true };
      }
      this.bytes.push(byte);
    }
    return { lines, overflow: false };
  }
}

export function validateGateESnapshotFreshness(snapshot, {
  nowSeconds = systemNowSeconds,
  maxBlockAgeSeconds = gateETiming.maxBlockAgeSeconds,
  maxFutureSkewSeconds = gateETiming.maxFutureSkewSeconds,
} = {}) {
  const now = nowSeconds();
  if (typeof now !== 'bigint' || now < 0n || now > maxUint64) {
    throw new GateESnapshotError('LOCAL_CLOCK_INVALID');
  }
  const timestamp = snapshot?.block?.timestamp;
  if (typeof timestamp !== 'bigint' || timestamp < 0n || timestamp > maxUint64) {
    throw new GateESnapshotError('BLOCK_TIMESTAMP_INVALID');
  }
  if (timestamp > now && timestamp - now > maxFutureSkewSeconds) {
    throw new GateESnapshotError('BLOCK_FROM_FUTURE');
  }
  if (now > timestamp && now - timestamp > maxBlockAgeSeconds) {
    throw new GateESnapshotError('STALE_BLOCK');
  }
  return snapshot;
}

function runWithTimeout(operation, timeoutMs, { setTimer, clearTimer }) {
  if (!Number.isFinite(timeoutMs) || timeoutMs <= 0) {
    return Promise.reject(new GateESnapshotError('ATTEMPT_DEADLINE_EXCEEDED'));
  }
  return new Promise((resolve, reject) => {
    let settled = false;
    let timer;
    const settle = (callback, value) => {
      if (settled) return;
      settled = true;
      clearTimer(timer);
      callback(value);
    };
    timer = setTimer(() => settle(reject, new GateESnapshotError('ENS_TIMEOUT')), timeoutMs);
    Promise.resolve().then(operation).then(
      value => settle(resolve, value),
      error => settle(reject, error),
    );
  });
}

export async function readGateECredentialSnapshot({
  primaryClient,
  fallbackClient,
  label = gateECredential.label,
  readSnapshot = (client, credentialLabel) => readCredential(client, { label: credentialLabel }),
  remainingMs = () => Number.POSITIVE_INFINITY,
  ensVerificationBudgetMs = gateETiming.ensVerificationBudgetMs,
  nowMs = systemNowMs,
  nowSeconds = systemNowSeconds,
  setTimer = setTimeout,
  clearTimer = clearTimeout,
}) {
  requireCondition(Boolean(primaryClient), 'Primary Sepolia RPC client is required.');
  const providers = [['PRIMARY', primaryClient]];
  if (fallbackClient) providers.push(['FALLBACK', fallbackClient]);
  const ensDeadlineAt = nowMs() + ensVerificationBudgetMs;
  let lastFailure;
  for (let index = 0; index < providers.length; index++) {
    const [provider, client] = providers[index];
    const ensRemaining = ensDeadlineAt - nowMs();
    if (ensRemaining <= 0) throw new GateESnapshotError('ENS_TIMEOUT');
    const attemptRemaining = Math.max(0, remainingMs());
    if (attemptRemaining <= 0) throw new GateESnapshotError('ATTEMPT_DEADLINE_EXCEEDED');
    const providersRemaining = providers.length - index;
    // Reserve an equal share for fallback only when it is configured. Immediate
    // primary failures leave the unused portion available to the fallback.
    const providerBudget = Math.floor(ensRemaining / providersRemaining);
    const budget = Math.min(providerBudget, attemptRemaining);
    try {
      const snapshot = await runWithTimeout(async () => {
        if (await client.getChainId() !== sepolia.id) {
          throw new GateESnapshotError('WRONG_CHAIN');
        }
        return readSnapshot(client, label);
      }, budget, { setTimer, clearTimer });
      if (nowMs() >= ensDeadlineAt) throw new GateESnapshotError('ENS_TIMEOUT');
      validateGateESnapshotFreshness(snapshot, { nowSeconds });
      return { snapshot, provider };
    } catch (error) {
      lastFailure = error instanceof GateESnapshotError
        ? error : new GateESnapshotError('RPC_SNAPSHOT_ERROR');
    }
  }
  throw lastFailure ?? new GateESnapshotError('RPC_SNAPSHOT_ERROR');
}

export class GateESecureBridge {
  constructor({
    issueChallenge: issue,
    verifyProof,
    sendLine,
    checkReplay = false,
    postChallengeDeadlineMs = gateETiming.postChallengeDeadlineMs,
    proofTimeoutMs = gateETiming.proofTimeoutMs,
    firmwareConfirmationTimeoutMs = gateETiming.firmwareConfirmationTimeoutMs,
    nowMs = systemNowMs,
    setTimer = setTimeout,
    clearTimer = clearTimeout,
  }) {
    this.issueChallenge = issue;
    this.verifyProof = verifyProof;
    this.sendLine = sendLine;
    this.checkReplay = checkReplay;
    this.postChallengeDeadlineMs = postChallengeDeadlineMs;
    this.proofTimeoutMs = proofTimeoutMs;
    this.firmwareConfirmationTimeoutMs = firmwareConfirmationTimeoutMs;
    this.nowMs = nowMs;
    this.setTimer = setTimer;
    this.clearTimer = clearTimer;
    this.state = 'AWAITING_TARGET_ACTIVATION';
    this.challenge = undefined;
    this.result = undefined;
    this.replayResult = undefined;
    this.authorizationCommand = undefined;
    this.expectedFirmwareConfirmation = undefined;
    this.firmwareConfirmation = undefined;
    this.deadlineAt = undefined;
    this.timers = new Set();
    this.proofTimer = undefined;
    this.completion = new Promise((resolve, reject) => {
      this.resolveCompletion = resolve;
      this.rejectCompletion = reject;
    });
  }

  schedule(callback, delayMs) {
    let handle;
    handle = this.setTimer(() => {
      this.timers.delete(handle);
      callback();
    }, delayMs);
    this.timers.add(handle);
    return handle;
  }

  cancelTimer(handle) {
    if (handle === undefined || !this.timers.delete(handle)) return;
    this.clearTimer(handle);
  }

  clearAllTimers() {
    for (const handle of this.timers) this.clearTimer(handle);
    this.timers.clear();
  }

  remainingMs() {
    return this.deadlineAt === undefined ? Number.POSITIVE_INFINITY : this.deadlineAt - this.nowMs();
  }

  terminalize(callback, value) {
    if (this.state === 'DONE') return false;
    this.state = 'DONE';
    this.clearAllTimers();
    callback(value);
    return true;
  }

  transportFailure(reason, details = {}) {
    if (this.state === 'DONE') return;
    this.result = {
      allowed: false,
      reason,
      origin: gateEOutcome.TRANSPORT_FAILURE,
      verifierOutcome: undefined,
      controllerOutcome: gateEOutcome.CONTROLLER_UNCONFIRMED,
    };
    this.terminalize(this.rejectCompletion, new GateESerialFinalizationError(
      reason,
      `Gate E transport failed closed: ${reason}.`,
      { origin: gateEOutcome.TRANSPORT_FAILURE, ...details },
    ));
  }

  async receiveLine(rawLine) {
    const line = rawLine.replace(/\r$/, '');
    if (this.state === 'DONE') return;

    const stop = parseFirmwareStop(line);
    if (stop) {
      this.transportFailure(stop.reason, { firmwareStage: stop.stage });
      return;
    }

    if (this.state === 'AWAITING_FIRMWARE_CONFIRMATION') {
      const authorization = firmwareAuthorizationPattern.exec(line)?.[1];
      if (!authorization) return;
      if (line !== this.expectedFirmwareConfirmation) {
        this.failFinalization(
          'FIRMWARE_AUTHORIZATION_MISMATCH',
          `Firmware authorization mismatch: expected ${this.expectedFirmwareConfirmation}, received ${line}.`,
          { observedFirmwareConfirmation: line },
        );
        return;
      }
      this.firmwareConfirmation = line;
      this.result.controllerOutcome = gateEOutcome.CONTROLLER_CONFIRMED;
      this.terminalize(this.resolveCompletion, this.result);
      return;
    }

    if (firmwareAuthorizationPattern.test(line)) return;

    if (line === 'TARGET_ACTIVATION: PASS' && this.state === 'AWAITING_TARGET_ACTIVATION') {
      this.state = 'AWAITING_SELECT';
      return;
    }
    if (line === 'SELECT: PASS' && this.state === 'AWAITING_SELECT') {
      this.state = 'AWAITING_WAITING_CHALLENGE';
      return;
    }

    if (line === 'WAITING_CHALLENGE') {
      if (this.state !== 'AWAITING_WAITING_CHALLENGE') return;
      this.state = 'ISSUING_CHALLENGE';
      try {
        const challenge = await this.issueChallenge();
        if (this.state !== 'ISSUING_CHALLENGE') return;
        this.challenge = challenge;
        this.deadlineAt = this.nowMs() + this.postChallengeDeadlineMs;
        this.schedule(() => this.transportFailure('ATTEMPT_DEADLINE_EXCEEDED'),
          this.postChallengeDeadlineMs);
        this.state = 'AWAITING_PROOF';
        await this.sendLine(serializeChallenge(this.challenge));
        if (this.state !== 'AWAITING_PROOF') return;
        const proofBudget = Math.min(this.proofTimeoutMs, this.remainingMs());
        if (proofBudget <= 0) {
          this.transportFailure('ATTEMPT_DEADLINE_EXCEEDED');
          return;
        }
        this.proofTimer = this.schedule(() => {
          void this.failClosed('PROOF_TIMEOUT').catch(() => {});
        }, proofBudget);
      } catch {
        if (this.state !== 'DONE') await this.failClosed('CHALLENGE_ERROR');
      }
      return;
    }

    if (line.startsWith('PROOF=') && this.state === 'AWAITING_PROOF') {
      this.state = 'VERIFYING';
      this.cancelTimer(this.proofTimer);
      let signature;
      try {
        signature = parseProofLine(line);
      } catch {
        await this.failClosed('MALFORMED_PROOF');
        return;
      }

      let verifiedResult;
      try {
        verifiedResult = await this.verifyProof({
          challenge: this.challenge,
          signature,
          remainingMs: () => this.remainingMs(),
        });
      } catch {
        verifiedResult = { allowed: false, reason: 'VERIFICATION_ERROR' };
      }
      if (this.state === 'DONE' || this.remainingMs() <= 0) {
        if (this.state !== 'DONE') this.transportFailure('ATTEMPT_DEADLINE_EXCEEDED');
        return;
      }
      this.result = verifiedResult;
      if (this.checkReplay) {
        try {
          this.replayResult = await this.verifyProof({
            challenge: this.challenge,
            signature,
            remainingMs: () => this.remainingMs(),
          });
        } catch {
          this.replayResult = { allowed: false, reason: 'VERIFICATION_ERROR' };
        }
        if (this.state === 'DONE' || this.remainingMs() <= 0) {
          if (this.state !== 'DONE') this.transportFailure('ATTEMPT_DEADLINE_EXCEEDED');
          return;
        }
      }
      await this.finish(this.result);
    }
  }

  async failClosed(reason = 'BRIDGE_ERROR') {
    if (this.state === 'DONE') return;
    if (['SENDING_AUTHORIZATION', 'AWAITING_FIRMWARE_CONFIRMATION'].includes(this.state)) {
      this.failFinalization(reason, `Serial finalization failed after ${this.authorizationCommand} was sent.`);
      return;
    }
    this.cancelTimer(this.proofTimer);
    this.result = {
      allowed: false,
      reason,
      origin: gateEOutcome.TRANSPORT_FAILURE,
      verifierOutcome: undefined,
    };
    await this.finish(this.result);
  }

  async finish(result) {
    if (['DONE', 'SENDING_AUTHORIZATION', 'AWAITING_FIRMWARE_CONFIRMATION'].includes(this.state)) return;
    if (this.remainingMs() <= 0) {
      this.transportFailure('ATTEMPT_DEADLINE_EXCEEDED');
      return;
    }
    const decision = result.allowed ? 'ALLOW' : 'DENY';
    if (!result.origin) {
      result.verifierOutcome = result.allowed
        ? gateEOutcome.VERIFIER_ALLOW : gateEOutcome.VERIFIER_DENY;
      result.origin = result.verifierOutcome;
    }
    result.controllerOutcome = gateEOutcome.CONTROLLER_UNCONFIRMED;
    this.authorizationCommand = `AUTHORIZATION=${decision}`;
    this.expectedFirmwareConfirmation = `AUTHORIZATION: ${decision}`;
    this.state = 'SENDING_AUTHORIZATION';
    try {
      await this.sendLine(this.authorizationCommand);
    } catch {
      this.failFinalization(
        'AUTHORIZATION_WRITE_ERROR',
        `Failed to send ${this.authorizationCommand}; firmware confirmation was not observed.`,
      );
      return;
    }
    if (this.state === 'DONE') return;
    const confirmationBudget = Math.min(this.firmwareConfirmationTimeoutMs, this.remainingMs());
    if (confirmationBudget <= 0) {
      this.transportFailure('ATTEMPT_DEADLINE_EXCEEDED');
      return;
    }
    this.state = 'AWAITING_FIRMWARE_CONFIRMATION';
    this.schedule(() => {
      this.failFinalization(
        'FIRMWARE_CONFIRMATION_TIMEOUT',
        `Authorization command ${this.authorizationCommand} was sent but firmware confirmation was not observed.`,
      );
    }, confirmationBudget);
  }

  failFinalization(reason, message, details = {}) {
    if (this.state === 'DONE') return;
    this.terminalize(this.rejectCompletion, new GateESerialFinalizationError(reason, message, {
      authorizationCommand: this.authorizationCommand,
      expectedFirmwareConfirmation: this.expectedFirmwareConfirmation,
      verifierOutcome: this.result?.verifierOutcome,
      controllerOutcome: gateEOutcome.CONTROLLER_UNCONFIRMED,
      ...details,
    }));
  }

  waitForCompletion() {
    return this.completion;
  }
}

export function parseGateEArguments(args) {
  const options = { checkReplay: false };
  for (let index = 0; index < args.length; index++) {
    if (args[index] === '--check-replay') options.checkReplay = true;
    else if (args[index] === '--port') {
      requireCondition(Boolean(args[index + 1]), '--port requires a value.');
      options.port = args[++index];
    } else {
      throw new Error(`Unknown option: ${args[index]}`);
    }
  }
  return options;
}

function writeLine(port, line) {
  return new Promise((resolve, reject) => {
    port.write(`${line}\n`, error => {
      if (error) return reject(error);
      port.drain(drainError => drainError ? reject(drainError) : resolve());
    });
  });
}

function closePort(port) {
  if (!port?.isOpen) return Promise.resolve();
  return new Promise((resolve, reject) => port.close(error => error ? reject(error) : resolve()));
}

export async function waitForBridgeAndClose(bridge, close) {
  try {
    return await bridge.waitForCompletion();
  } finally {
    await close();
  }
}

async function runBridge(args, captureBridge, preserveBoot) {
  const options = parseGateEArguments(args);
  const portPath = options.port ?? process.env.NFC_SERIAL_PORT?.trim();
  requireCondition(Boolean(portPath), 'NFC_SERIAL_PORT or --port is required.');

  const primaryClient = createPublicClient({
    chain: sepolia,
    transport: http(process.env.SEPOLIA_RPC_URL?.trim() || undefined),
  });
  const fallbackUrl = process.env.SEPOLIA_RPC_FALLBACK_URL?.trim();
  const fallbackClient = fallbackUrl ? createPublicClient({
    chain: sepolia,
    transport: http(fallbackUrl),
  }) : undefined;

  const store = new IssuedChallengeStore();
  const issue = () => issueAccessChallenge({
    store,
    credential: gateECredential.node,
    resource: gateEResourceId,
    ttlSeconds: gateETiming.challengeTtlSeconds,
  });
  const verifyProof = async ({ challenge, signature, remainingMs }) => {
    let snapshotFailureReason;
    let publicSnapshot;
    const result = await verifyAccessAttempt({
      store,
      nonce: challenge.nonce,
      signature,
      expectedCredential: gateECredential.node,
      expectedResource: gateEResourceId,
      readCredentialState: async () => {
        try {
          const selected = await readGateECredentialSnapshot({
            primaryClient, fallbackClient, remainingMs,
          });
          publicSnapshot = { currentEnsOwner: selected.snapshot.owner,
            snapshotBlock: selected.snapshot.block.number.toString() };
          return selected.snapshot;
        } catch (error) {
          snapshotFailureReason = error instanceof GateESnapshotError
            ? error.reason : 'RPC_SNAPSHOT_ERROR';
          throw error;
        }
      },
    });
    return { ...result, ...publicSnapshot,
      ...(result.reason === verificationReason.ENS_STATE_ERROR && snapshotFailureReason
        ? { ensFailureReason: snapshotFailureReason } : {}) };
  };

  const { SerialPort } = await import('serialport');
  const port = new SerialPort({ path: portPath, baudRate: 115200, autoOpen: false,
    ...(preserveBoot ? { hupcl: false, rtscts: false } : {}) });
  await new Promise((resolve, reject) => {
    port.open(error => error ? reject(error) : resolve());
  });

  const bridge = new GateESecureBridge({
    issueChallenge: issue,
    verifyProof,
    sendLine: line => writeLine(port, line),
    checkReplay: options.checkReplay,
  });
  captureBridge(bridge);
  const lineBuffer = new GateELineBuffer();
  const receive = chunk => {
    const { lines, overflow } = lineBuffer.push(chunk);
    for (const line of lines) {
      const publicLine = line.startsWith('PROOF=') ? 'PROOF=[redacted]' : line.replace(/\r$/, '');
      console.log(`ESP32: ${publicLine}`);
      void bridge.receiveLine(line).catch(() => bridge.transportFailure('SERIAL_ERROR'));
    }
    if (overflow) bridge.transportFailure('SERIAL_INPUT_OVERFLOW');
  };
  const serialFailure = error => {
    bridge.transportFailure(error ? 'SERIAL_ERROR' : 'SERIAL_CLOSED');
  };
  port.on('data', receive);
  port.once('error', serialFailure);
  port.once('close', serialFailure);

  console.log(`CREDENTIAL: ${gateECredential.name}`);
  console.log(`RESOURCE: ${gateEResourceName}`);
  console.log(`RESOURCE_ID: ${gateEResourceId}`);
  console.log('BRIDGE LISTENING: present Seeker only after PRESENT_SEEKER from this boot.');
  const result = await waitForBridgeAndClose(bridge, async () => {
    port.off('data', receive);
    port.off('error', serialFailure);
    port.off('close', serialFailure);
    await closePort(port);
  });
  console.log(`VERIFICATION: ${result.ensFailureReason ?? result.reason}`);
  console.log(`AUTHORIZATION: ${result.allowed ? 'ALLOW' : 'DENY'}`);
  if (options.checkReplay) {
    console.log(`REPLAY: ${bridge.replayResult?.reason ?? 'NOT_CHECKED'}`);
  }
}

export async function main(args = process.argv.slice(2), { preserveBoot = false } = {}) {
  // Before opening serial or issuing a challenge; reporting never controls authorization.
  return recordAttempt(capture => runBridge(args, capture, preserveBoot));
}

if (process.argv[1] && pathToFileURL(process.argv[1]).href === import.meta.url) {
  main().catch(error => {
    const reason = error instanceof GateESerialFinalizationError ? ` (${error.reason})` : '';
    console.error(`Gate E secure bridge stopped${reason}. AUTHORIZATION: DENY`);
    process.exitCode = 1;
  });
}
