import { pathToFileURL } from 'node:url';
import {
  concatHex, createPublicClient, http, keccak256, numberToHex, stringToHex,
} from 'viem';
import { sepolia } from 'viem/chains';
import { credentialIdentity, readCredential } from '../ensv2/access-record.mjs';
import { requireCondition } from '../ensv2/contracts.mjs';
import {
  IssuedChallengeStore, issueAccessChallenge, verifyAccessAttempt,
} from './holder-proof.mjs';

export const gateECredential = credentialIdentity('guest-001');
export const gateEIntendedOwner = '0x3419148731087b970d2059C53780163B452D5FF7';
export const gateEResourceName = 'demo-access.eth:door-001';
export const gateEResourceId = keccak256(stringToHex(gateEResourceName));

const proofPattern = /^PROOF=(0x[0-9a-fA-F]{130})$/;

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

export class GateESecureBridge {
  constructor({
    issueChallenge: issue,
    verifyProof,
    sendLine,
    checkReplay = false,
    proofTimeoutMs = 30_000,
    setTimer = setTimeout,
    clearTimer = clearTimeout,
  }) {
    this.issueChallenge = issue;
    this.verifyProof = verifyProof;
    this.sendLine = sendLine;
    this.checkReplay = checkReplay;
    this.proofTimeoutMs = proofTimeoutMs;
    this.setTimer = setTimer;
    this.clearTimer = clearTimer;
    this.state = 'AWAITING_TARGET_ACTIVATION';
    this.challenge = undefined;
    this.result = undefined;
    this.replayResult = undefined;
    this.timer = undefined;
    this.completion = new Promise(resolve => { this.resolveCompletion = resolve; });
  }

  async receiveLine(rawLine) {
    const line = rawLine.replace(/\r$/, '');
    if (this.state === 'DONE') return;

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
        this.challenge = await this.issueChallenge();
        this.state = 'AWAITING_PROOF';
        await this.sendLine(serializeChallenge(this.challenge));
        this.timer = this.setTimer(() => {
          void this.failClosed('PROOF_TIMEOUT').catch(() => {});
        }, this.proofTimeoutMs);
      } catch {
        await this.failClosed('CHALLENGE_ERROR');
      }
      return;
    }

    if (line.startsWith('PROOF=') && this.state === 'AWAITING_PROOF') {
      this.state = 'VERIFYING';
      this.clearTimer(this.timer);
      let signature;
      try {
        signature = parseProofLine(line);
      } catch {
        await this.failClosed('MALFORMED_PROOF');
        return;
      }

      try {
        this.result = await this.verifyProof({ challenge: this.challenge, signature });
      } catch {
        this.result = { allowed: false, reason: 'VERIFICATION_ERROR' };
      }
      if (this.checkReplay) {
        try {
          this.replayResult = await this.verifyProof({ challenge: this.challenge, signature });
        } catch {
          this.replayResult = { allowed: false, reason: 'VERIFICATION_ERROR' };
        }
      }
      await this.finish(this.result);
    }
  }

  async failClosed(reason = 'BRIDGE_ERROR') {
    if (this.state === 'DONE') return;
    this.clearTimer(this.timer);
    this.result = { allowed: false, reason };
    await this.finish(this.result);
  }

  async finish(result) {
    if (this.state === 'DONE') return;
    this.state = 'DONE';
    try {
      await this.sendLine(`AUTHORIZATION=${result.allowed ? 'ALLOW' : 'DENY'}`);
    } finally {
      this.resolveCompletion(result);
    }
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

export async function main(args = process.argv.slice(2)) {
  const options = parseGateEArguments(args);
  const portPath = options.port ?? process.env.NFC_SERIAL_PORT?.trim();
  requireCondition(Boolean(portPath), 'NFC_SERIAL_PORT or --port is required.');

  const publicClient = createPublicClient({
    chain: sepolia,
    transport: http(process.env.SEPOLIA_RPC_URL?.trim() || undefined),
  });
  requireCondition(await publicClient.getChainId() === sepolia.id, 'RPC must be Sepolia.');

  const store = new IssuedChallengeStore();
  const issue = () => issueAccessChallenge({
    store,
    credential: gateECredential.node,
    resource: gateEResourceId,
  });
  const verifyProof = ({ challenge, signature }) => verifyAccessAttempt({
    store,
    nonce: challenge.nonce,
    signature,
    expectedCredential: gateECredential.node,
    expectedResource: gateEResourceId,
    readCredentialState: () => readCredential(publicClient, { label: gateECredential.label }),
  });

  const { SerialPort } = await import('serialport');
  const port = new SerialPort({ path: portPath, baudRate: 115200, autoOpen: false });
  await new Promise((resolve, reject) => {
    port.open(error => error ? reject(error) : resolve());
  });

  const bridge = new GateESecureBridge({
    issueChallenge: issue,
    verifyProof,
    sendLine: line => writeLine(port, line),
    checkReplay: options.checkReplay,
  });
  let buffer = '';
  let queue = Promise.resolve();
  const receive = chunk => {
    buffer += chunk.toString('utf8');
    let newline = buffer.indexOf('\n');
    while (newline !== -1) {
      const line = buffer.slice(0, newline);
      buffer = buffer.slice(newline + 1);
      console.log(`ESP32: ${line.replace(/\r$/, '')}`);
      queue = queue.then(() => bridge.receiveLine(line))
        .catch(() => bridge.failClosed('SERIAL_ERROR').catch(() => {}));
      newline = buffer.indexOf('\n');
    }
  };
  const serialFailure = error => {
    void bridge.failClosed(error ? 'SERIAL_ERROR' : 'SERIAL_CLOSED').catch(() => {});
  };
  port.on('data', receive);
  port.once('error', serialFailure);
  port.once('close', serialFailure);

  console.log(`CREDENTIAL: ${gateECredential.name}`);
  console.log(`RESOURCE: ${gateEResourceName}`);
  console.log(`RESOURCE_ID: ${gateEResourceId}`);
  try {
    const result = await bridge.waitForCompletion();
    console.log(`VERIFICATION: ${result.reason}`);
    console.log(`AUTHORIZATION: ${result.allowed ? 'ALLOW' : 'DENY'}`);
    if (options.checkReplay) {
      console.log(`REPLAY: ${bridge.replayResult?.reason ?? 'NOT_CHECKED'}`);
    }
  } finally {
    port.off('data', receive);
    port.off('error', serialFailure);
    port.off('close', serialFailure);
    await closePort(port);
  }
}

if (process.argv[1] && pathToFileURL(process.argv[1]).href === import.meta.url) {
  main().catch(() => {
    console.error('Gate E secure bridge stopped. AUTHORIZATION: DENY');
    process.exitCode = 1;
  });
}
