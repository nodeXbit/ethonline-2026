import { GateESecureBridge, GateELineBuffer, gateEOutcome, readGateECredentialSnapshot } from './gate-e-secure-bridge.mjs';
import { IssuedChallengeStore, issueAccessChallenge } from './holder-proof.mjs';
import { gateETiming } from './gate-e-config.mjs';
import { credentialCandidate, validateCredentialSnapshot, readResourceCredential, verifyResourceProof, AccessError } from './resource-verifier.mjs';
import { resourceBySlug, resources } from './access-resources.mjs';

export const denyReasons = Object.freeze({
  NO_SELECTED_CREDENTIAL: 'Select a pass in LockENS.', INVALID_CREDENTIAL_NAME: 'The pass identity is invalid.',
  WRONG_NAMESPACE: 'This pass is outside the LockENS namespace.', WRONG_REGISTRY: 'The pass registry could not be verified.',
  WRONG_RESOLVER: 'The pass resolver could not be verified.', CREDENTIAL_NOT_REGISTERED: 'The pass is not registered.',
  REGISTRATION_EXPIRED: 'The pass registration has expired.', HOLDER_MISMATCH: 'The wallet does not own this pass.',
  CHALLENGE_EXPIRED: 'The proof challenge expired.', REPLAY: 'This proof was already used.',
  INVALID_SIGNATURE: 'The holder proof is invalid.', ACCESS_SUSPENDED: 'Access is suspended.', ACCESS_EXPIRED: 'Access has expired.',
  ACCESS_POLICY_INVALID: 'Global access is not configured correctly.', RESOURCE_POLICY_INVALID: 'The resource policy is invalid.',
  RESOURCE_POLICY_MISSING: 'No resource policy is configured.', RESOURCE_NOT_ALLOWED: 'This pass does not allow this resource.',
  RPC_UNAVAILABLE: 'Current pass state is unavailable.', CONTROLLER_NOT_CONFIRMED: 'The controller did not confirm the decision.',
  DYNAMIC_PROTOCOL_REQUIRED: 'Dynamic NFC firmware and a selected pass are required.',
  SERIAL_PROTOCOL_INVALID: 'The reader session could not be verified.', TRANSPORT_FAILURE: 'The NFC session did not complete.',
  READER_INITIALIZATION_FAILED: 'Reader initialization failed. NFC was not started.',
  SESSION_TIMEOUT: 'The listening window expired. No access decision was made.',
});
export const safeReason = reason => Object.hasOwn(denyReasons, reason) ? reason : 'TRANSPORT_FAILURE';

export function parseCredentialFrame(line) {
  const match = /^CREDENTIAL_V1=(0x(?:[0-9a-f]{2}){1,64})$/.exec(line);
  if (!match) throw new AccessError('INVALID_CREDENTIAL_NAME');
  let name;
  try { name = new TextDecoder('utf-8', { fatal: true }).decode(Buffer.from(match[1].slice(2), 'hex')); }
  catch { throw new AccessError('INVALID_CREDENTIAL_NAME'); }
  return credentialCandidate(name);
}

export class VirtualGateProfiles {
  #selected = resourceBySlug('front-door');
  select(slug) { this.#selected = resourceBySlug(slug); return this.#selected; }
  current() { return this.#selected; }
}

/** Resource preflight extends the existing one-shot transport and confirmation state machine. */
export class DynamicGateBridge extends GateESecureBridge {
  constructor({ profiles, discover, issue, verify, onEvent = () => {}, ...options }) {
    const context = { resource: profiles.current() };
    super({ ...options,
      issueChallenge: () => issue(context.credential, context.resource),
      verifyProof: args => verify({ ...args, credential: context.credential, resourceId: context.resource.resourceId }),
    });
    this.context = context;
    this.profiles = profiles;
    this.discover = discover;
    this.onEvent = onEvent;
    this.protocolReady = false;
    this.waitingChallenge = false;
    this.discoveryComplete = false;
  }
  async receiveLine(raw) {
    const line = raw.replace(/\r$/, '');
    if (this.state === 'DONE') return;
    if (line === 'PN532_BOOT_START_V1' && this.state === 'AWAITING_TARGET_ACTIVATION') {
      this.onEvent({ status: 'Initializing reader', stage: 'INITIALIZATION', readerDiagnostic: null }); return;
    }
    const boot = /^PN532_BOOT_V1=CMD:(14|32);WRITE_RC:(-?[0-9]{1,3});READ_RC:(NONE|-?[0-9]{1,5})$/.exec(line);
    if (boot && this.state === 'AWAITING_TARGET_ACTIVATION') {
      this.onEvent({ readerDiagnostic: { command: boot[1], writeResult: Number(boot[2]),
        readResult: boot[3] === 'NONE' ? null : Number(boot[3]) } }); return;
    }
    const probe = /^PN532_INIT_V1=ADDRESS:24;WIRE_RC:([0-5])$/.exec(line);
    if (probe && this.state === 'AWAITING_TARGET_ACTIVATION') {
      this.onEvent({ readerDiagnostic: { address: '0x24', wireResult: Number(probe[1]) } }); return;
    }
    if (line === 'LOCKENS_DYNAMIC_V1_READY' && this.state === 'AWAITING_TARGET_ACTIVATION') { this.protocolReady = true; return; }
    if (line === 'PRESENT_SEEKER' && this.protocolReady && this.state === 'AWAITING_TARGET_ACTIVATION') {
      this.onEvent({ status: 'Reader ready — present phone', stage: 'READER_READY' }); return;
    }
    if (line.startsWith('GATE_E: STOP - INITIALIZATION:')) {
      this.onEvent({ stage: 'INITIALIZATION' });
      return this.transportFailure('READER_INITIALIZATION_FAILED');
    }
    if (line === 'TARGET_ACTIVATION: PASS' && this.state === 'AWAITING_TARGET_ACTIVATION') {
      if (!this.protocolReady) return this.transportFailure('DYNAMIC_PROTOCOL_REQUIRED');
      this.onEvent({ status: 'NFC session started', stage: 'NFC_STARTED', resource: this.context.resource });
    }
    if (line.startsWith('CREDENTIAL_V1=')) {
      if (this.state !== 'AWAITING_WAITING_CHALLENGE' || this.context.credential) return this.transportFailure('SERIAL_PROTOCOL_INVALID');
      this.state = 'DISCOVERING_CREDENTIAL';
      try {
        const credential = parseCredentialFrame(line);
        await this.discover(credential);
        if (this.state !== 'DISCOVERING_CREDENTIAL') return;
        this.context.credential = credential;
        this.discoveryComplete = true;
        this.onEvent({ status: 'Credential identified', credential: credential.name });
        this.state = 'AWAITING_WAITING_CHALLENGE';
        if (this.waitingChallenge) await super.receiveLine('WAITING_CHALLENGE');
      } catch (error) { this.transportFailure(error instanceof AccessError ? safeReason(error.reason) : 'RPC_UNAVAILABLE'); }
      return;
    }
    if (line === 'WAITING_CHALLENGE') {
      if (this.state === 'DISCOVERING_CREDENTIAL') { this.waitingChallenge = true; return; }
      if (!this.discoveryComplete) return this.transportFailure('DYNAMIC_PROTOCOL_REQUIRED');
    }
    if (line.startsWith('GATE_E: STOP - GET_CREDENTIAL:')) {
      this.onEvent({ stage: 'GET_CREDENTIAL' });
      return this.transportFailure('TRANSPORT_FAILURE');
    }
    await super.receiveLine(line);
  }
}

export function createResourceBridge({ primaryClient, fallbackClient, profiles, sendLine, onEvent,
  store = new IssuedChallengeStore(), nowSeconds = () => BigInt(Math.floor(Date.now() / 1000)), ...options }) {
  let minimumBlock;
  const read = async (credential, remainingMs) => {
    let failure;
    try {
      const { snapshot } = await readGateECredentialSnapshot({ primaryClient, fallbackClient,
        label: credential.name, nowSeconds, remainingMs,
        readSnapshot: async (client, name) => {
          try { return validateCredentialSnapshot(await readResourceCredential(client, name), credential, nowSeconds()); }
          catch (error) { failure = error; throw error; }
        },
      });
      if (minimumBlock !== undefined && snapshot.block.number < minimumBlock) throw new AccessError('RPC_UNAVAILABLE');
      minimumBlock = snapshot.block.number;
      return snapshot;
    } catch { throw failure instanceof AccessError ? failure : new AccessError('RPC_UNAVAILABLE'); }
  };
  return new DynamicGateBridge({ ...options, profiles, sendLine, onEvent,
    discover: credential => read(credential),
    issue: (credential, resource) => issueAccessChallenge({ store, credential: credential.node,
      resource: resource.resourceId, ttlSeconds: gateETiming.challengeTtlSeconds, now: nowSeconds }),
    verify: ({ credential, remainingMs, ...args }) => verifyResourceProof({ ...args, store, credential,
      now: nowSeconds, readSnapshot: () => read(credential, remainingMs) }),
  });
}

export class GateMonitorState {
  constructor(profiles = new VirtualGateProfiles()) { this.profiles = profiles; this.busy = false; this.reset(); }
  reset() { this.attempt = { status: 'Ready to start listening', stage: 'IDLE', resource: this.profiles.current(), controller: 'Not confirmed', final: null }; }
  begin() { if (this.busy) throw Error('SESSION_BUSY'); this.busy = true; this.reset(); this.event({ status: 'Initializing reader', stage: 'INITIALIZATION' }); }
  event(value) { this.attempt = { ...this.attempt, ...value }; }
  complete(result) {
    const confirmed = result.controllerOutcome === gateEOutcome.CONTROLLER_CONFIRMED;
    if (!confirmed) return this.fail({ reason: 'CONTROLLER_NOT_CONFIRMED' }, result);
    const allowed = result.allowed === true && confirmed;
    this.event({ checks: result.checks, controller: confirmed ? 'Confirmed' : 'Not confirmed',
      status: 'Access decision confirmed', stage: 'DECISION', final: allowed ? 'ACCESS GRANTED' : 'ACCESS DENIED',
      reason: allowed ? null : safeReason(confirmed ? result.reason : 'CONTROLLER_NOT_CONFIRMED') });
    this.busy = false;
  }
  fail(error, result) {
    this.event({ checks: result?.checks, controller: 'Not confirmed', status: 'Session stopped — no access decision', final: 'SESSION ERROR',
      reason: safeReason(result?.allowed ? 'CONTROLLER_NOT_CONFIRMED' : error?.reason) });
    this.busy = false;
  }
  publicState() { return { ...this.attempt, busy: this.busy, selected: this.profiles.current(), resources,
    humanReason: this.attempt.reason ? denyReasons[this.attempt.reason] : null }; }
}

// No environment writes, signing API, raw proof logging, or actuator claim.
export async function runDynamicSerial({ portPath, monitor, primaryClient, fallbackClient }) {
  monitor.begin();
  let port, bridge;
  try {
    const { SerialPort } = await import('serialport');
    port = new SerialPort({ path: portPath, baudRate: 115200, autoOpen: false, hupcl: false, rtscts: false });
    bridge = createResourceBridge({ primaryClient, fallbackClient, profiles: monitor.profiles,
      onEvent: event => monitor.event(event),
      sendLine: line => new Promise((resolve, reject) => port.write(`${line}\n`, error => {
        if (error) return reject(error);
        port.drain(error => error ? reject(error) : resolve());
      })),
    });
    const buffer = new GateELineBuffer();
    const done = bridge.waitForCompletion();
    done.catch(() => {});
    port.on('data', chunk => {
      const { lines, overflow } = buffer.push(chunk);
      if (overflow) return bridge.transportFailure('SERIAL_PROTOCOL_INVALID');
      for (const line of lines) void bridge.receiveLine(line).catch(() => bridge.transportFailure('SERIAL_PROTOCOL_INVALID'));
    });
    port.on('error', () => bridge.transportFailure('TRANSPORT_FAILURE'));
    port.on('close', () => bridge.transportFailure('TRANSPORT_FAILURE'));
    await new Promise((resolve, reject) => port.open(error => error ? reject(error) : resolve()));
    // Allow manual reset and phone preparation; proof/challenge deadlines remain separate.
    bridge.schedule(() => bridge.transportFailure('SESSION_TIMEOUT'), 600_000);
    monitor.complete(await done);
  } catch (error) { monitor.fail(error, bridge?.result); }
  finally {
    if (bridge && bridge.state !== 'DONE') bridge.transportFailure('TRANSPORT_FAILURE');
    if (port?.isOpen) await new Promise(resolve => port.close(() => resolve()));
  }
}
