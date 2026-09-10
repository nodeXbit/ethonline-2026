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
  GateELineBuffer, GateESecureBridge, GateESnapshotError, gateECredential, gateEOutcome,
  gateEResourceId, parseFirmwareStop, parseProofLine, readGateECredentialSnapshot,
  serializeChallenge, validateGateESnapshotFreshness, waitForBridgeAndClose,
} from './gate-e-secure-bridge.mjs';
import { gateESerialMaxLineBytes, gateETiming } from './gate-e-config.mjs';

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

function harness({
  active = true,
  checkReplay = false,
  readState,
  postChallengeDeadlineMs = 50_000,
  proofTimeoutMs = 30_000,
  firmwareConfirmationTimeoutMs = 2_000,
} = {}) {
  const store = new IssuedChallengeStore();
  const clock = { now: startTime, ms: 0 };
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
    postChallengeDeadlineMs,
    proofTimeoutMs,
    firmwareConfirmationTimeoutMs,
    nowMs: () => clock.ms,
    setTimer: (callback, delay) => {
      timers.push({ callback, delay, cleared: false });
      return timers.length - 1;
    },
    clearTimer: id => { if (id !== undefined && timers[id]) timers[id].cleared = true; },
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

function authorizationLines(sent) {
  return sent.filter(line => line.startsWith('AUTHORIZATION='));
}

function deferred() {
  let resolve;
  let reject;
  const promise = new Promise((resolvePromise, rejectPromise) => {
    resolve = resolvePromise;
    reject = rejectPromise;
  });
  return { promise, resolve, reject };
}

function fakeTimers() {
  const timers = [];
  return {
    timers,
    setTimer(callback, delay) {
      timers.push({ callback, delay, cleared: false });
      return timers.length - 1;
    },
    clearTimer(id) {
      if (timers[id]) timers[id].cleared = true;
    },
    fire(delay) {
      const timer = timers.find(item => !item.cleared && item.delay === delay);
      assert.ok(timer, `Missing active ${delay}ms timer`);
      timer.callback();
    },
  };
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
  assert.deepEqual(authorizationLines(h.sent), ['AUTHORIZATION=ALLOW']);
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
  const completion = h.bridge.waitForCompletion();
  await h.bridge.receiveLine('AUTHORIZATION: ALLOW');
  await completion;
  assert.deepEqual(authorizationLines(h.sent), ['AUTHORIZATION=ALLOW']);
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
  const timeout = h.timers.find(timer => !timer.cleared && timer.delay === 30_000).callback;
  const completion = h.bridge.waitForCompletion();
  timeout();
  await Promise.resolve();
  await h.bridge.receiveLine('AUTHORIZATION: DENY');
  await completion;
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
  assert.equal(authorizationLines(h.sent).length, 1);
});

test('DENY waits for matching firmware confirmation before completion and serial close', async () => {
  const h = harness({ active: false });
  const serial = { closeCount: 0 };
  await reachChallenge(h.bridge);
  const completion = waitForBridgeAndClose(h.bridge, async () => { serial.closeCount++; });
  await h.bridge.receiveLine(`PROOF=${await signChallenge(holder, h.bridge.challenge)}`);

  assert.deepEqual(authorizationLines(h.sent), ['AUTHORIZATION=DENY']);
  assert.equal(h.bridge.state, 'AWAITING_FIRMWARE_CONFIRMATION');
  assert.equal(serial.closeCount, 0);

  await h.bridge.receiveLine('AUTHORIZATION: DENY');
  const result = await completion;
  assert.equal(result.reason, verificationReason.ACCESS_DENIED);
  assert.equal(result.verifierOutcome, gateEOutcome.VERIFIER_DENY);
  assert.equal(result.controllerOutcome, gateEOutcome.CONTROLLER_CONFIRMED);
  assert.equal(h.bridge.firmwareConfirmation, 'AUTHORIZATION: DENY');
  assert.equal(serial.closeCount, 1);
});

test('ALLOW waits for matching firmware confirmation and sends authorization exactly once', async () => {
  const h = harness({ active: true });
  await reachChallenge(h.bridge);
  const completion = h.bridge.waitForCompletion();
  await h.bridge.receiveLine(`PROOF=${await signChallenge(holder, h.bridge.challenge)}`);

  assert.deepEqual(authorizationLines(h.sent), ['AUTHORIZATION=ALLOW']);
  assert.equal(h.bridge.state, 'AWAITING_FIRMWARE_CONFIRMATION');
  await h.bridge.receiveLine('AUTHORIZATION: ALLOW');
  const result = await completion;
  assert.equal(result.allowed, true);
  assert.equal(h.bridge.firmwareConfirmation, 'AUTHORIZATION: ALLOW');
  assert.deepEqual(authorizationLines(h.sent), ['AUTHORIZATION=ALLOW']);
});

test('opposite firmware authorization fails closed without resending authorization', async () => {
  const h = harness({ active: false });
  await reachChallenge(h.bridge);
  const completion = h.bridge.waitForCompletion();
  await h.bridge.receiveLine(`PROOF=${await signChallenge(holder, h.bridge.challenge)}`);
  await h.bridge.receiveLine('AUTHORIZATION: ALLOW');

  await assert.rejects(completion, error => {
    assert.equal(error.reason, 'FIRMWARE_AUTHORIZATION_MISMATCH');
    assert.equal(error.expectedFirmwareConfirmation, 'AUTHORIZATION: DENY');
    assert.equal(error.observedFirmwareConfirmation, 'AUTHORIZATION: ALLOW');
    return true;
  });
  assert.equal(h.bridge.state, 'DONE');
  assert.deepEqual(authorizationLines(h.sent), ['AUTHORIZATION=DENY']);
});

test('firmware confirmation timeout stops without ALLOW or a second operation', async () => {
  const h = harness({ active: false });
  await reachChallenge(h.bridge);
  const completion = h.bridge.waitForCompletion();
  await h.bridge.receiveLine(`PROOF=${await signChallenge(holder, h.bridge.challenge)}`);
  const confirmationTimeout = h.timers.find(timer => !timer.cleared && timer.delay === 2_000).callback;
  confirmationTimeout();

  await assert.rejects(completion, error => {
    assert.equal(error.reason, 'FIRMWARE_CONFIRMATION_TIMEOUT');
    assert.match(error.message, /was sent but firmware confirmation was not observed/);
    return true;
  });
  assert.ok(!h.sent.includes('AUTHORIZATION=ALLOW'));
  assert.deepEqual(authorizationLines(h.sent), ['AUTHORIZATION=DENY']);
  assert.deepEqual(h.counts(), { issueCount: 1, verifyCount: 1, ensReads: 1 });
  assert.equal(h.sent.filter(line => line.startsWith('CHALLENGE=')).length, 1);
});

test('unrelated and early terminal lines cannot satisfy firmware confirmation', async () => {
  const h = harness({ active: false });
  let settled = false;
  await h.bridge.receiveLine('AUTHORIZATION: DENY');
  await reachChallenge(h.bridge);
  const completion = h.bridge.waitForCompletion().then(result => {
    settled = true;
    return result;
  });
  await h.bridge.receiveLine(`PROOF=${await signChallenge(holder, h.bridge.challenge)}`);

  await h.bridge.receiveLine('PROCESSING');
  await h.bridge.receiveLine('AUTHORIZATION: DENY ');
  await Promise.resolve();
  assert.equal(settled, false);
  assert.equal(h.bridge.state, 'AWAITING_FIRMWARE_CONFIRMATION');

  await h.bridge.receiveLine('AUTHORIZATION: DENY');
  await completion;
  assert.equal(settled, true);
});

test('NFC UID text is ignored and is not a secure authorization input', async () => {
  const h = harness();
  await h.bridge.receiveLine('ISO14443A tag detected; UID=91:2D:E3:06 (4 bytes)');
  assert.equal(h.counts().issueCount, 0);
  assert.equal(h.counts().verifyCount, 0);
  assert.deepEqual(h.sent, []);
});

test('central Gate E timing and serial limits preserve the required budgets', () => {
  assert.deepEqual(gateETiming, {
    challengeTtlSeconds: 60n,
    postChallengeDeadlineMs: 50_000,
    proofTimeoutMs: 30_000,
    ensVerificationBudgetMs: 8_000,
    firmwareConfirmationTimeoutMs: 2_000,
    maxBlockAgeSeconds: 60n,
    maxFutureSkewSeconds: 15n,
  });
  assert.equal(gateESerialMaxLineBytes, 1_024);
});

test('healthy ALLOW flow completes inside the post-challenge deadline', async () => {
  const h = harness();
  await reachChallenge(h.bridge);
  h.clock.ms = 49_000;
  const completion = h.bridge.waitForCompletion();
  await h.bridge.receiveLine(`PROOF=${await signChallenge(holder, h.bridge.challenge)}`);
  await h.bridge.receiveLine('AUTHORIZATION: ALLOW');
  const result = await completion;
  assert.equal(result.verifierOutcome, gateEOutcome.VERIFIER_ALLOW);
  assert.equal(result.controllerOutcome, gateEOutcome.CONTROLLER_CONFIRMED);
  assert.deepEqual(authorizationLines(h.sent), ['AUTHORIZATION=ALLOW']);
});

test('proof arriving after the total deadline cannot verify or send authorization', async () => {
  const h = harness({ proofTimeoutMs: 60_000 });
  await reachChallenge(h.bridge);
  const completion = assert.rejects(h.bridge.waitForCompletion(), error =>
    error.reason === 'ATTEMPT_DEADLINE_EXCEEDED');
  h.clock.ms = 50_000;
  h.timers.find(timer => !timer.cleared && timer.delay === 50_000).callback();
  await completion;
  await h.bridge.receiveLine(`PROOF=${await signChallenge(holder, h.bridge.challenge)}`);
  assert.deepEqual(h.counts(), { issueCount: 1, verifyCount: 0, ensReads: 0 });
  assert.deepEqual(authorizationLines(h.sent), []);
});

test('a late verifier ALLOW is ignored after the total deadline', async () => {
  const verdict = deferred();
  const sent = [];
  const clock = { ms: 0 };
  const timers = fakeTimers();
  const bridge = new GateESecureBridge({
    issueChallenge: async () => ({
      credential: gateECredential.node, resource: gateEResourceId,
      nonce: `0x${'55'.repeat(32)}`, expiresAt: startTime + 60n,
    }),
    verifyProof: () => verdict.promise,
    sendLine: async line => { sent.push(line); },
    postChallengeDeadlineMs: 50,
    proofTimeoutMs: 40,
    nowMs: () => clock.ms,
    setTimer: timers.setTimer.bind(timers),
    clearTimer: timers.clearTimer.bind(timers),
  });
  await reachChallenge(bridge);
  const proofAttempt = bridge.receiveLine(`PROOF=0x${'11'.repeat(65)}`);
  const completion = assert.rejects(bridge.waitForCompletion(), error =>
    error.reason === 'ATTEMPT_DEADLINE_EXCEEDED');
  clock.ms = 50;
  timers.fire(50);
  await completion;
  verdict.resolve({ allowed: true, reason: verificationReason.ALLOW });
  await proofAttempt;
  assert.deepEqual(authorizationLines(sent), []);
  assert.equal(bridge.result.allowed, false);
});

test('an ENS read resolving after the total deadline cannot produce ALLOW', async () => {
  const state = deferred();
  const h = harness({ readState: () => state.promise, postChallengeDeadlineMs: 50,
    proofTimeoutMs: 40 });
  await reachChallenge(h.bridge);
  const attempt = h.bridge.receiveLine(`PROOF=${await signChallenge(holder, h.bridge.challenge)}`);
  while (h.counts().ensReads === 0) await Promise.resolve();
  const completion = assert.rejects(h.bridge.waitForCompletion(), error =>
    error.reason === 'ATTEMPT_DEADLINE_EXCEEDED');
  h.clock.ms = 50;
  h.timers.find(timer => !timer.cleared && timer.delay === 50).callback();
  await completion;
  state.resolve(credentialState(true));
  await attempt;
  assert.deepEqual(authorizationLines(h.sent), []);
  assert.equal(h.store.stateOf(h.bridge.challenge.nonce), challengeState.CONSUMED);
});

test('an ALLOW computed at or after the deadline is never sent', async () => {
  const clock = { ms: 0 };
  const sent = [];
  const bridge = new GateESecureBridge({
    issueChallenge: async () => ({
      credential: gateECredential.node, resource: gateEResourceId,
      nonce: `0x${'56'.repeat(32)}`, expiresAt: startTime + 60n,
    }),
    verifyProof: async () => {
      clock.ms = 50;
      return { allowed: true, reason: verificationReason.ALLOW };
    },
    sendLine: async line => { sent.push(line); },
    postChallengeDeadlineMs: 50,
    nowMs: () => clock.ms,
  });
  await reachChallenge(bridge);
  const completion = assert.rejects(bridge.waitForCompletion(), error =>
    error.reason === 'ATTEMPT_DEADLINE_EXCEEDED');
  await bridge.receiveLine(`PROOF=0x${'11'.repeat(65)}`);
  await completion;
  assert.deepEqual(authorizationLines(sent), []);
});

test('terminal completion clears timers once and late callbacks cannot finalize twice', async () => {
  const h = harness();
  await reachChallenge(h.bridge);
  const callbacks = h.timers.map(timer => timer.callback);
  const completion = h.bridge.waitForCompletion();
  await h.bridge.receiveLine(`PROOF=${await signChallenge(holder, h.bridge.challenge)}`);
  await h.bridge.receiveLine('AUTHORIZATION: ALLOW');
  await completion;
  assert.equal(h.bridge.timers.size, 0);
  for (const callback of callbacks) callback();
  assert.equal(h.bridge.state, 'DONE');
  assert.deepEqual(authorizationLines(h.sent), ['AUTHORIZATION=ALLOW']);
});

test('existing firmware STOP output terminates immediately in every phase without Node authorization', async () => {
  for (const phase of ['BEFORE_CHALLENGE', 'AFTER_TARGET', 'AWAITING_PROOF']) {
    const h = harness();
    if (phase === 'AFTER_TARGET') await h.bridge.receiveLine('TARGET_ACTIVATION: PASS');
    if (phase === 'AWAITING_PROOF') await reachChallenge(h.bridge);
    const completion = assert.rejects(h.bridge.waitForCompletion(), error => {
      assert.equal(error.reason, 'FIRMWARE_STOP_STATUS');
      assert.equal(error.origin, gateEOutcome.TRANSPORT_FAILURE);
      return true;
    });
    await h.bridge.receiveLine('AUTHORIZATION: DENY');
    await h.bridge.receiveLine('GATE_E: STOP - STATUS: Android proof provider reported ERROR');
    await completion;
    assert.equal(h.bridge.state, 'DONE');
    assert.deepEqual(authorizationLines(h.sent), []);
  }
});

test('firmware STOP during verification wins over a later verifier result', async () => {
  const state = deferred();
  const h = harness({ readState: () => state.promise });
  await reachChallenge(h.bridge);
  const attempt = h.bridge.receiveLine(`PROOF=${await signChallenge(holder, h.bridge.challenge)}`);
  while (h.counts().ensReads === 0) await Promise.resolve();
  const completion = assert.rejects(h.bridge.waitForCompletion(), error =>
    error.reason === 'FIRMWARE_STOP_GET_SIGNATURE');
  await h.bridge.receiveLine('GATE_E: STOP - GET_SIGNATURE: ISO-DEP exchange failed');
  await completion;
  state.resolve(credentialState(true));
  await attempt;
  assert.deepEqual(authorizationLines(h.sent), []);
  assert.equal(h.counts().ensReads, 1);
});

test('pre-verification firmware STOP performs no ENS read and closes exactly once', async () => {
  const h = harness();
  const serial = { closes: 0 };
  const completion = assert.rejects(
    waitForBridgeAndClose(h.bridge, async () => { serial.closes++; }),
    error => error.reason === 'FIRMWARE_STOP_INITIALIZATION',
  );
  await h.bridge.receiveLine('GATE_E: STOP - INITIALIZATION: PN532 firmware/version query failed');
  await h.bridge.receiveLine('GATE_E: STOP - INITIALIZATION: duplicate');
  await completion;
  assert.deepEqual(h.counts(), { issueCount: 0, verifyCount: 0, ensReads: 0 });
  assert.equal(serial.closes, 1);
  assert.deepEqual(authorizationLines(h.sent), []);
});

test('unknown firmware STOP fails closed instead of being ignored', async () => {
  const h = harness();
  assert.deepEqual(parseFirmwareStop('GATE_E: STOP - NEW_STAGE: local failure'), {
    stage: 'NEW_STAGE', reason: 'UNKNOWN_FIRMWARE_STOP',
  });
  const completion = assert.rejects(h.bridge.waitForCompletion(), error =>
    error.reason === 'UNKNOWN_FIRMWARE_STOP');
  await h.bridge.receiveLine('GATE_E: STOP - NEW_STAGE: local failure');
  await completion;
  assert.deepEqual(h.sent, []);
});

test('early firmware DENY during verification cannot confirm a later Node DENY', async () => {
  const verdict = deferred();
  const sent = [];
  const bridge = new GateESecureBridge({
    issueChallenge: async () => ({
      credential: gateECredential.node, resource: gateEResourceId,
      nonce: `0x${'57'.repeat(32)}`, expiresAt: startTime + 60n,
    }),
    verifyProof: () => verdict.promise,
    sendLine: async line => { sent.push(line); },
    setTimer: () => 1,
    clearTimer: () => {},
  });
  await reachChallenge(bridge);
  const attempt = bridge.receiveLine(`PROOF=0x${'11'.repeat(65)}`);
  await bridge.receiveLine('AUTHORIZATION: DENY');
  verdict.resolve({ allowed: false, reason: verificationReason.ACCESS_DENIED });
  await attempt;
  let settled = false;
  const completion = bridge.waitForCompletion().then(() => { settled = true; });
  await Promise.resolve();
  assert.equal(settled, false);
  await bridge.receiveLine('AUTHORIZATION: DENY');
  await completion;
  assert.deepEqual(authorizationLines(sent), ['AUTHORIZATION=DENY']);
});

test('serial line buffer accepts the exact maximum and rejects one byte over', () => {
  const exact = new GateELineBuffer();
  assert.deepEqual(exact.push(Buffer.from(`${'a'.repeat(gateESerialMaxLineBytes)}\n`)), {
    lines: ['a'.repeat(gateESerialMaxLineBytes)], overflow: false,
  });
  const over = new GateELineBuffer();
  assert.deepEqual(over.push(Buffer.from(`${'a'.repeat(gateESerialMaxLineBytes + 1)}\n`)), {
    lines: [], overflow: true,
  });
  assert.equal(over.bytes.length, 0);
});

test('no-newline flood and oversized PROOF permanently poison the attempt buffer', () => {
  for (const payload of [
    'x'.repeat(gateESerialMaxLineBytes + 1),
    `PROOF=0x${'11'.repeat(gateESerialMaxLineBytes)}`,
  ]) {
    const buffer = new GateELineBuffer();
    assert.equal(buffer.push(Buffer.from(payload)).overflow, true);
    assert.deepEqual(buffer.push(Buffer.from('TARGET_ACTIVATION: PASS\n')), {
      lines: [], overflow: true,
    });
  }
});

test('serial overflow is terminal: a later valid line cannot revive or issue again', async () => {
  const h = harness();
  await reachChallenge(h.bridge);
  const completion = assert.rejects(h.bridge.waitForCompletion(), error =>
    error.reason === 'SERIAL_INPUT_OVERFLOW');
  h.bridge.transportFailure('SERIAL_INPUT_OVERFLOW');
  await completion;
  await h.bridge.receiveLine('WAITING_CHALLENGE');
  await h.bridge.receiveLine(`PROOF=${await signChallenge(holder, h.bridge.challenge)}`);
  assert.deepEqual(h.counts(), { issueCount: 1, verifyCount: 0, ensReads: 0 });
  assert.deepEqual(authorizationLines(h.sent), []);
});

function rpcClient(id, chainId = 11155111) {
  return { id, getChainId: async () => chainId };
}

function rpcSnapshot(timestamp = startTime) {
  return { block: { number: 100n, timestamp } };
}

test('fresh primary snapshot uses exactly one provider', async () => {
  const calls = [];
  const selected = await readGateECredentialSnapshot({
    primaryClient: rpcClient('primary'),
    fallbackClient: rpcClient('fallback'),
    readSnapshot: async client => { calls.push(client.id); return rpcSnapshot(); },
    nowSeconds: () => startTime,
  });
  assert.equal(selected.provider, 'PRIMARY');
  assert.deepEqual(calls, ['primary']);
});

test('primary timeout discards it and runs one complete fallback snapshot', async () => {
  const timers = fakeTimers();
  const clock = { ms: 0 };
  const calls = [];
  const primary = { id: 'primary', getChainId: () => new Promise(() => {}) };
  const resultPromise = readGateECredentialSnapshot({
    primaryClient: primary,
    fallbackClient: rpcClient('fallback'),
    readSnapshot: async client => { calls.push(client.id); return rpcSnapshot(); },
    nowSeconds: () => startTime,
    nowMs: () => clock.ms,
    setTimer: timers.setTimer.bind(timers),
    clearTimer: timers.clearTimer.bind(timers),
  });
  clock.ms = 4_000;
  timers.fire(4_000);
  const selected = await resultPromise;
  assert.equal(selected.provider, 'FALLBACK');
  assert.deepEqual(calls, ['fallback']);
});

test('primary and fallback together cannot exceed the 8-second ENS budget', async () => {
  const timers = fakeTimers();
  const clock = { ms: 0 };
  const hanging = id => ({ id, getChainId: () => new Promise(() => {}) });
  const resultPromise = readGateECredentialSnapshot({
    primaryClient: hanging('primary'),
    fallbackClient: hanging('fallback'),
    nowSeconds: () => startTime,
    nowMs: () => clock.ms,
    setTimer: timers.setTimer.bind(timers),
    clearTimer: timers.clearTimer.bind(timers),
  });
  clock.ms = 4_000;
  timers.fire(4_000);
  await Promise.resolve();
  clock.ms = 8_000;
  timers.fire(4_000);
  await assert.rejects(resultPromise, error => error.reason === 'ENS_TIMEOUT');
});

test('stale, wrong-chain and partial primary failures restart the whole fallback snapshot', async () => {
  for (const failure of ['STALE', 'WRONG_CHAIN', 'PARTIAL']) {
    const trace = [];
    const primary = rpcClient('primary', failure === 'WRONG_CHAIN' ? 1 : 11155111);
    const fallback = rpcClient('fallback');
    const selected = await readGateECredentialSnapshot({
      primaryClient: primary,
      fallbackClient: fallback,
      readSnapshot: async client => {
        trace.push(`${client.id}:begin`);
        if (client === primary && failure === 'PARTIAL') {
          trace.push('primary:owner');
          throw new Error('partial provider failure');
        }
        trace.push(`${client.id}:end`);
        return rpcSnapshot(client === primary && failure === 'STALE' ? startTime - 61n : startTime);
      },
      nowSeconds: () => startTime,
    });
    assert.equal(selected.provider, 'FALLBACK');
    assert.ok(trace.every(entry => entry.startsWith('primary:') || entry.startsWith('fallback:')));
    assert.ok(trace.lastIndexOf('primary:begin') < trace.indexOf('fallback:begin'));
    assert.equal(trace.filter(entry => entry === 'fallback:begin').length, 1);
  }
});

test('missing or failed fallback fails closed with no retries beyond two providers', async () => {
  let reads = 0;
  await assert.rejects(readGateECredentialSnapshot({
    primaryClient: rpcClient('primary'),
    readSnapshot: async () => { reads++; throw new Error('down'); },
    nowSeconds: () => startTime,
  }), error => error instanceof GateESnapshotError && error.reason === 'RPC_SNAPSHOT_ERROR');
  assert.equal(reads, 1);

  reads = 0;
  await assert.rejects(readGateECredentialSnapshot({
    primaryClient: rpcClient('primary'),
    fallbackClient: rpcClient('fallback'),
    readSnapshot: async () => { reads++; throw new Error('down'); },
    nowSeconds: () => startTime,
  }), error => error instanceof GateESnapshotError && error.reason === 'RPC_SNAPSHOT_ERROR');
  assert.equal(reads, 2);
});

test('snapshot freshness rejects old, future, missing timestamps and invalid local clocks', () => {
  assert.equal(validateGateESnapshotFreshness(rpcSnapshot(startTime), {
    nowSeconds: () => startTime + 60n,
  }).block.timestamp, startTime);
  assert.throws(() => validateGateESnapshotFreshness(rpcSnapshot(startTime), {
    nowSeconds: () => startTime + 61n,
  }), error => error.reason === 'STALE_BLOCK');
  assert.throws(() => validateGateESnapshotFreshness(rpcSnapshot(startTime + 16n), {
    nowSeconds: () => startTime,
  }), error => error.reason === 'BLOCK_FROM_FUTURE');
  assert.throws(() => validateGateESnapshotFreshness({ block: {} }, {
    nowSeconds: () => startTime,
  }), error => error.reason === 'BLOCK_TIMESTAMP_INVALID');
  assert.throws(() => validateGateESnapshotFreshness(rpcSnapshot(), {
    nowSeconds: () => -1n,
  }), error => error.reason === 'LOCAL_CLOCK_INVALID');
});

test('total attempt deadline dominates RPC fallback selection', async () => {
  let remainingCalls = 0;
  let reads = 0;
  await assert.rejects(readGateECredentialSnapshot({
    primaryClient: rpcClient('primary'),
    fallbackClient: rpcClient('fallback'),
    remainingMs: () => remainingCalls++ === 0 ? 100 : 0,
    readSnapshot: async () => { reads++; throw new Error('primary failed'); },
    nowSeconds: () => startTime,
  }), error => error.reason === 'ATTEMPT_DEADLINE_EXCEEDED');
  assert.equal(reads, 1);
});
