import assert from 'node:assert/strict';
import test from 'node:test';
import { hexToBytes } from 'viem';
import { privateKeyToAccount } from 'viem/accounts';
import { REGISTERED } from '../ensv2/contracts.mjs';
import {
  buildAccessChallengeTypedData, challengeState, IssuedChallengeStore, issueAccessChallenge, verificationReason,
  verifyAccessAttempt,
} from './holder-proof.mjs';
import {
  GateESecureBridge, gateECredential, gateEResourceId, parseProofLine, serializeChallenge,
} from './gate-e-secure-bridge.mjs';

// TEST ONLY deterministic fixtures. No project wallet or captured physical proof is used.
const holder = privateKeyToAccount(`0x${'41'.repeat(32)}`);
const other = privateKeyToAccount(`0x${'42'.repeat(32)}`);
const startTime = 1_700_000_000n;

function credentialState(active = true, owner = holder.address) {
  return {
    state: { status: REGISTERED },
    owner,
    expiry: startTime + 600n,
    block: { number: 100n, timestamp: startTime },
    access: { active, validUntil: startTime + 300n },
  };
}

function harness({ active = true, checkReplay = false, readState, proofTimeoutMs = 30_000 } = {}) {
  const store = new IssuedChallengeStore();
  const clock = { now: startTime };
  const sent = [];
  const timers = [];
  let issueCount = 0;
  let verifyCount = 0;
  let ensReads = 0;
  const issue = () => {
    issueCount++;
    return issueAccessChallenge({
      store,
      credential: gateECredential.node,
      resource: gateEResourceId,
      now: () => clock.now,
      randomBytes: () => new Uint8Array(32).fill(0x5a),
    });
  };
  const verifyProof = ({ challenge, signature }) => {
    verifyCount++;
    return verifyAccessAttempt({
      store,
      nonce: challenge.nonce,
      signature,
      expectedCredential: gateECredential.node,
      expectedResource: gateEResourceId,
      now: () => clock.now,
      readCredentialState: async () => {
        ensReads++;
        return readState ? readState() : credentialState(active);
      },
    });
  };
  const bridge = new GateESecureBridge({
    issueChallenge: issue,
    verifyProof,
    sendLine: async line => { sent.push(line); },
    checkReplay,
    proofTimeoutMs,
    setTimer: callback => { timers.push(callback); return timers.length - 1; },
    clearTimer: id => { if (id !== undefined) timers[id] = undefined; },
  });
  return {
    bridge, clock, sent, store, timers,
    counts: () => ({ issueCount, verifyCount, ensReads }),
  };
}

async function reachChallenge(bridge) {
  await bridge.receiveLine('TARGET_ACTIVATION: PASS');
  await bridge.receiveLine('SELECT: PASS');
  await bridge.receiveLine('WAITING_CHALLENGE');
}

async function signChallenge(account, challenge) {
  return account.signTypedData(buildAccessChallengeTypedData(challenge));
}

test('WAITING_CHALLENGE issues exactly one Gate A challenge', async () => {
  const h = harness();
  await reachChallenge(h.bridge);
  assert.equal(h.counts().issueCount, 1);
  assert.equal(h.bridge.state, 'AWAITING_PROOF');
  assert.equal(h.sent.filter(line => line.startsWith('CHALLENGE=')).length, 1);
});

test('challenge serial payload is exact canonical 104-byte field concatenation', async () => {
  const h = harness();
  await reachChallenge(h.bridge);
  const line = h.sent[0];
  assert.match(line, /^CHALLENGE=0x[0-9a-f]{208}$/);
  const bytes = hexToBytes(line.slice('CHALLENGE='.length));
  assert.equal(bytes.length, 104);
  assert.equal(`0x${Buffer.from(bytes.slice(0, 32)).toString('hex')}`, gateECredential.node);
  assert.equal(`0x${Buffer.from(bytes.slice(32, 64)).toString('hex')}`, gateEResourceId);
  assert.equal(`0x${Buffer.from(bytes.slice(64, 96)).toString('hex')}`, h.bridge.challenge.nonce);
  assert.equal(Buffer.from(bytes.slice(96)).readBigUInt64BE(), h.bridge.challenge.expiresAt);
  assert.equal(line, serializeChallenge(h.bridge.challenge));
});

test('device progress and arbitrary input do not issue before WAITING_CHALLENGE', async () => {
  const h = harness();
  for (const line of ['GATE_E_READY', 'PRESENT_SEEKER', 'TARGET_ACTIVATION: PASS', 'SELECT: PASS']) {
    await h.bridge.receiveLine(line);
  }
  assert.equal(h.counts().issueCount, 0);
  assert.deepEqual(h.sent, []);
});

test('duplicate concurrent WAITING_CHALLENGE cannot issue a second challenge', async () => {
  let release;
  let issues = 0;
  const sent = [];
  const bridge = new GateESecureBridge({
    issueChallenge: async () => {
      issues++;
      await new Promise(resolve => { release = resolve; });
      return {
        credential: gateECredential.node, resource: gateEResourceId,
        nonce: `0x${'55'.repeat(32)}`, expiresAt: startTime + 60n,
      };
    },
    verifyProof: async () => ({ allowed: false }),
    sendLine: async line => { sent.push(line); },
    setTimer: () => 0,
    clearTimer: () => {},
  });
  await bridge.receiveLine('TARGET_ACTIVATION: PASS');
  await bridge.receiveLine('SELECT: PASS');
  const first = bridge.receiveLine('WAITING_CHALLENGE');
  const duplicate = bridge.receiveLine('WAITING_CHALLENGE');
  release();
  await Promise.all([first, duplicate]);
  assert.equal(issues, 1);
  assert.equal(sent.length, 1);
});

test('exact 65-byte proof is parsed and accepted', () => {
  const signature = `0x${'ab'.repeat(65)}`;
  assert.equal(parseProofLine(`PROOF=${signature}`), signature);
});

test('malformed, non-hex, and wrong-length proofs fail closed', async () => {
  for (const proof of ['PROOF=0x1234', `PROOF=0x${'gg'.repeat(65)}`, `PROOF=0x${'11'.repeat(64)}`]) {
    const h = harness();
    await reachChallenge(h.bridge);
    await h.bridge.receiveLine(proof);
    assert.deepEqual(h.sent.slice(-1), ['AUTHORIZATION=DENY']);
    assert.equal(h.bridge.result.reason, 'MALFORMED_PROOF');
    assert.equal(h.counts().verifyCount, 0);
  }
});

test('correct current holder plus ACTIVE snapshot sends ALLOW exactly once', async () => {
  const h = harness({ active: true });
  await reachChallenge(h.bridge);
  const signature = await signChallenge(holder, h.bridge.challenge);
  await h.bridge.receiveLine(`PROOF=${signature}`);
  assert.equal(h.bridge.result.reason, verificationReason.ALLOW);
  assert.equal(h.bridge.result.allowed, true);
  assert.deepEqual(h.sent.filter(line => line.startsWith('AUTHORIZATION=')), ['AUTHORIZATION=ALLOW']);
});

test('correct current holder plus INACTIVE snapshot denies after consumption', async () => {
  const h = harness({ active: false });
  await reachChallenge(h.bridge);
  const signature = await signChallenge(holder, h.bridge.challenge);
  await h.bridge.receiveLine(`PROOF=${signature}`);
  assert.equal(h.bridge.result.reason, verificationReason.ACCESS_DENIED);
  assert.equal(h.store.stateOf(h.bridge.challenge.nonce), challengeState.CONSUMED);
  assert.deepEqual(h.sent.slice(-1), ['AUTHORIZATION=DENY']);
});

test('replay check reuses the same proof and store with zero new challenge', async () => {
  const h = harness({ checkReplay: true });
  await reachChallenge(h.bridge);
  const signature = await signChallenge(holder, h.bridge.challenge);
  await h.bridge.receiveLine(`PROOF=${signature}`);
  assert.equal(h.bridge.result.reason, verificationReason.ALLOW);
  assert.equal(h.bridge.replayResult.reason, verificationReason.REPLAYED_CHALLENGE);
  assert.equal(h.bridge.replayResult.allowed, false);
  assert.deepEqual(h.counts(), { issueCount: 1, verifyCount: 2, ensReads: 1 });
  assert.deepEqual(h.sent.filter(line => line.startsWith('AUTHORIZATION=')), ['AUTHORIZATION=ALLOW']);
});

test('wrong holder denies and never sends ALLOW', async () => {
  const h = harness();
  await reachChallenge(h.bridge);
  await h.bridge.receiveLine(`PROOF=${await signChallenge(other, h.bridge.challenge)}`);
  assert.equal(h.bridge.result.reason, verificationReason.WRONG_SIGNER);
  assert.ok(!h.sent.includes('AUTHORIZATION=ALLOW'));
  assert.equal(h.store.stateOf(h.bridge.challenge.nonce), challengeState.PENDING);
});

test('expired challenge denies without authorization read', async () => {
  const h = harness();
  await reachChallenge(h.bridge);
  const signature = await signChallenge(holder, h.bridge.challenge);
  h.clock.now = h.bridge.challenge.expiresAt;
  await h.bridge.receiveLine(`PROOF=${signature}`);
  assert.equal(h.bridge.result.reason, verificationReason.EXPIRED_CHALLENGE);
  assert.equal(h.counts().ensReads, 0);
  assert.deepEqual(h.sent.slice(-1), ['AUTHORIZATION=DENY']);
});

test('coherent ENS read failure fails closed and never allows', async () => {
  const h = harness({ readState: () => { throw new Error('RPC unavailable'); } });
  await reachChallenge(h.bridge);
  await h.bridge.receiveLine(`PROOF=${await signChallenge(holder, h.bridge.challenge)}`);
  assert.equal(h.bridge.result.reason, verificationReason.ENS_STATE_ERROR);
  assert.ok(!h.sent.includes('AUTHORIZATION=ALLOW'));
  assert.deepEqual(h.sent.slice(-1), ['AUTHORIZATION=DENY']);
});

test('proof timeout denies with no second challenge or SEND_CHALLENGE payload', async () => {
  const h = harness();
  await reachChallenge(h.bridge);
  const timeout = h.timers.find(Boolean);
  timeout();
  await h.bridge.waitForCompletion();
  assert.equal(h.bridge.result.reason, 'PROOF_TIMEOUT');
  assert.equal(h.counts().issueCount, 1);
  assert.equal(h.sent.filter(line => line.startsWith('CHALLENGE=')).length, 1);
  assert.deepEqual(h.sent.slice(-1), ['AUTHORIZATION=DENY']);
});

test('duplicate and late serial messages cannot create a second authorization attempt', async () => {
  const h = harness();
  await reachChallenge(h.bridge);
  const proof = `PROOF=${await signChallenge(holder, h.bridge.challenge)}`;
  await h.bridge.receiveLine(proof);
  await h.bridge.receiveLine(proof);
  await h.bridge.receiveLine('WAITING_CHALLENGE');
  await h.bridge.receiveLine('PROOF=malformed');
  assert.deepEqual(h.counts(), { issueCount: 1, verifyCount: 1, ensReads: 1 });
  assert.equal(h.sent.filter(line => line.startsWith('AUTHORIZATION=')).length, 1);
});

test('NFC UID text is ignored and is not a secure authorization input', async () => {
  const h = harness();
  await h.bridge.receiveLine('ISO14443A tag detected; UID=91:2D:E3:06 (4 bytes)');
  assert.equal(h.counts().issueCount, 0);
  assert.equal(h.counts().verifyCount, 0);
  assert.deepEqual(h.sent, []);
});
