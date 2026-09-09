import assert from 'node:assert/strict';
import test from 'node:test';
import { keccak256, namehash, stringToHex, zeroAddress } from 'viem';
import { privateKeyToAccount } from 'viem/accounts';
import { AVAILABLE, REGISTERED } from '../ensv2/contracts.mjs';
import {
  accessChallengeDomain, buildAccessChallengeTypedData, challengeState,
  IssuedChallengeStore, issueAccessChallenge, verificationReason, verifyAccessAttempt,
} from './holder-proof.mjs';

// TEST ONLY deterministic fixtures. These keys do not come from project configuration.
const ownerA = privateKeyToAccount(`0x${'11'.repeat(32)}`);
const ownerB = privateKeyToAccount(`0x${'22'.repeat(32)}`);
const credentialA = namehash('cred-001.demo-access.eth');
const credentialB = namehash('cred-002.demo-access.eth');
const resourceA = keccak256(stringToHex('door:front'));
const resourceB = keccak256(stringToHex('door:back'));
const startTime = 1_700_000_000n;

function harness() {
  const clock = { now: startTime };
  const store = new IssuedChallengeStore();
  let nonceByte = 1;
  const issue = (overrides = {}) => issueAccessChallenge({
    store,
    credential: credentialA,
    resource: resourceA,
    now: () => clock.now,
    randomBytes: () => new Uint8Array(32).fill(nonceByte++),
    ...overrides,
  });
  return { clock, store, issue };
}

function credentialState(owner = ownerA.address, overrides = {}) {
  return {
    state: { status: REGISTERED },
    owner,
    expiry: startTime + 600n,
    block: { timestamp: startTime },
    access: { active: true, validUntil: startTime + 300n },
    ...overrides,
  };
}

async function sign(account, challenge, typedDataOverrides = {}) {
  const typedData = buildAccessChallengeTypedData(challenge);
  return account.signTypedData({ ...typedData, ...typedDataOverrides });
}

function verifyArgs(h, challenge, signature, overrides = {}) {
  return {
    store: h.store,
    nonce: challenge.nonce,
    signature,
    expectedCredential: credentialA,
    expectedResource: resourceA,
    now: () => h.clock.now,
    readCredentialState: async () => credentialState(),
    ...overrides,
  };
}

test('valid current holder with ACTIVE access is allowed and consumed', async () => {
  const h = harness();
  const challenge = h.issue();
  assert.equal(challenge.expiresAt, startTime + 60n);
  const result = await verifyAccessAttempt(verifyArgs(h, challenge, await sign(ownerA, challenge)));
  assert.equal(result.allowed, true);
  assert.equal(result.reason, verificationReason.ALLOW);
  assert.equal(result.recoveredSigner, ownerA.address);
  assert.equal(h.store.stateOf(challenge.nonce), challengeState.CONSUMED);
});

test('a successful ALLOW proof cannot be replayed or re-read ENS state', async () => {
  const h = harness();
  const challenge = h.issue();
  const signature = await sign(ownerA, challenge);
  let reads = 0;
  const args = verifyArgs(h, challenge, signature, {
    readCredentialState: async () => { reads++; return credentialState(); },
  });
  assert.equal((await verifyAccessAttempt(args)).allowed, true);
  assert.deepEqual(await verifyAccessAttempt(args), {
    allowed: false, reason: verificationReason.REPLAYED_CHALLENGE,
  });
  assert.equal(reads, 1);
});

test('valid holder with INACTIVE access consumes; later ACTIVE state cannot enable replay', async () => {
  const h = harness();
  const challenge = h.issue();
  const signature = await sign(ownerA, challenge);
  let active = false;
  const args = verifyArgs(h, challenge, signature, {
    readCredentialState: async () => credentialState(ownerA.address, {
      access: { active, validUntil: startTime + 300n },
    }),
  });
  const denied = await verifyAccessAttempt(args);
  assert.equal(denied.reason, verificationReason.ACCESS_DENIED);
  assert.equal(h.store.stateOf(challenge.nonce), challengeState.CONSUMED);
  active = true;
  assert.equal((await verifyAccessAttempt(args)).reason, verificationReason.REPLAYED_CHALLENGE);
});

test('valid holder with expired access is denied, consumed, and cannot replay', async () => {
  const h = harness();
  const challenge = h.issue();
  const args = verifyArgs(h, challenge, await sign(ownerA, challenge), {
    readCredentialState: async () => credentialState(ownerA.address, {
      access: { active: true, validUntil: startTime },
    }),
  });
  assert.equal((await verifyAccessAttempt(args)).reason, verificationReason.ACCESS_DENIED);
  assert.equal(h.store.stateOf(challenge.nonce), challengeState.CONSUMED);
  assert.equal((await verifyAccessAttempt(args)).reason, verificationReason.REPLAYED_CHALLENGE);
});

test('valid holder with expired registry policy is denied and consumed', async () => {
  const h = harness();
  const challenge = h.issue();
  const args = verifyArgs(h, challenge, await sign(ownerA, challenge), {
    readCredentialState: async () => credentialState(ownerA.address, { expiry: startTime }),
  });
  assert.equal((await verifyAccessAttempt(args)).reason, verificationReason.ACCESS_DENIED);
  assert.equal(h.store.stateOf(challenge.nonce), challengeState.CONSUMED);
});

test('a valid self-issued challenge is rejected as UNKNOWN_CHALLENGE', async () => {
  const h = harness();
  const selfIssued = {
    credential: credentialA,
    resource: resourceA,
    nonce: `0x${'ab'.repeat(32)}`,
    expiresAt: startTime + 60n,
  };
  const result = await verifyAccessAttempt(verifyArgs(h, selfIssued, await sign(ownerA, selfIssued)));
  assert.equal(result.reason, verificationReason.UNKNOWN_CHALLENGE);
});

test('an expired server challenge is rejected without consumption', async () => {
  const h = harness();
  const challenge = h.issue();
  const signature = await sign(ownerA, challenge);
  h.clock.now = challenge.expiresAt;
  assert.equal((await verifyAccessAttempt(verifyArgs(h, challenge, signature))).reason,
    verificationReason.EXPIRED_CHALLENGE);
  assert.equal(h.store.stateOf(challenge.nonce), challengeState.PENDING);
});

test('wrong resource does not consume and the correct context can still succeed', async () => {
  const h = harness();
  const challenge = h.issue();
  const signature = await sign(ownerA, challenge);
  const wrong = await verifyAccessAttempt(verifyArgs(h, challenge, signature, {
    expectedResource: resourceB,
  }));
  assert.equal(wrong.reason, verificationReason.WRONG_RESOURCE);
  assert.equal(h.store.stateOf(challenge.nonce), challengeState.PENDING);
  assert.equal((await verifyAccessAttempt(verifyArgs(h, challenge, signature))).allowed, true);
});

test('wrong credential does not consume the challenge', async () => {
  const h = harness();
  const challenge = h.issue();
  const result = await verifyAccessAttempt(verifyArgs(h, challenge, await sign(ownerA, challenge), {
    expectedCredential: credentialB,
  }));
  assert.equal(result.reason, verificationReason.WRONG_CREDENTIAL);
  assert.equal(h.store.stateOf(challenge.nonce), challengeState.PENDING);
});

test('wrong signer is denied and does not consume the challenge', async () => {
  const h = harness();
  const challenge = h.issue();
  const result = await verifyAccessAttempt(verifyArgs(h, challenge, await sign(ownerB, challenge)));
  assert.equal(result.reason, verificationReason.WRONG_SIGNER);
  assert.equal(h.store.stateOf(challenge.nonce), challengeState.PENDING);
});

test('owner A signature fails after current owner changes to B without consumption', async () => {
  const h = harness();
  const challenge = h.issue();
  const result = await verifyAccessAttempt(verifyArgs(h, challenge, await sign(ownerA, challenge), {
    readCredentialState: async () => credentialState(ownerB.address),
  }));
  assert.equal(result.reason, verificationReason.WRONG_SIGNER);
  assert.equal(result.recoveredSigner, ownerA.address);
  assert.equal(h.store.stateOf(challenge.nonce), challengeState.PENDING);
});

test('valid holder with unavailable credential policy is denied and consumed', async () => {
  const h = harness();
  const challenge = h.issue();
  const args = verifyArgs(h, challenge, await sign(ownerA, challenge), {
    readCredentialState: async () => credentialState(ownerA.address, {
      state: { status: AVAILABLE },
    }),
  });
  assert.equal((await verifyAccessAttempt(args)).reason, verificationReason.ACCESS_DENIED);
  assert.equal(h.store.stateOf(challenge.nonce), challengeState.CONSUMED);
  assert.equal((await verifyAccessAttempt(args)).reason, verificationReason.REPLAYED_CHALLENGE);
});

test('malformed signatures are safely rejected without consumption', async () => {
  const h = harness();
  const challenge = h.issue();
  for (const signature of ['', '0x1234', 'not-hex', undefined]) {
    const result = await verifyAccessAttempt(verifyArgs(h, challenge, signature));
    assert.equal(result.reason, verificationReason.INVALID_SIGNATURE);
    assert.equal(h.store.stateOf(challenge.nonce), challengeState.PENDING);
  }
});

test('cryptographically tampered signature is denied without consumption', async () => {
  const h = harness();
  const challenge = h.issue();
  const signature = await sign(ownerA, challenge);
  const alteredRByte = (Number.parseInt(signature.slice(2, 4), 16) ^ 1)
    .toString(16).padStart(2, '0');
  const tampered = `0x${alteredRByte}${signature.slice(4)}`;
  const result = await verifyAccessAttempt(verifyArgs(h, challenge, tampered));
  assert.equal(result.allowed, false);
  assert.ok([verificationReason.INVALID_SIGNATURE, verificationReason.WRONG_SIGNER].includes(result.reason));
  assert.equal(h.store.stateOf(challenge.nonce), challengeState.PENDING);
});

test('different EIP-712 domain does not validate against the canonical domain', async () => {
  const h = harness();
  const challenge = h.issue();
  const signature = await sign(ownerA, challenge, {
    domain: { ...accessChallengeDomain, chainId: 1 },
  });
  const result = await verifyAccessAttempt(verifyArgs(h, challenge, signature));
  assert.equal(result.reason, verificationReason.WRONG_SIGNER);
  assert.equal(h.store.stateOf(challenge.nonce), challengeState.PENDING);
});

test('altered challenge fields cannot validate against server-stored canonical data', async () => {
  const alterations = [
    challenge => ({ ...challenge, credential: credentialB }),
    challenge => ({ ...challenge, resource: resourceB }),
    challenge => ({ ...challenge, nonce: `0x${'cc'.repeat(32)}` }),
    challenge => ({ ...challenge, expiresAt: challenge.expiresAt + 1n }),
  ];
  for (const alter of alterations) {
    const h = harness();
    const challenge = h.issue();
    const signature = await sign(ownerA, alter(challenge));
    const result = await verifyAccessAttempt(verifyArgs(h, challenge, signature));
    assert.equal(result.reason, verificationReason.WRONG_SIGNER);
    assert.equal(h.store.stateOf(challenge.nonce), challengeState.PENDING);
  }
});

test('concurrent verification consumes exactly once', async () => {
  const h = harness();
  const challenge = h.issue();
  const signature = await sign(ownerA, challenge);
  const results = await Promise.all([
    verifyAccessAttempt(verifyArgs(h, challenge, signature)),
    verifyAccessAttempt(verifyArgs(h, challenge, signature)),
  ]);
  assert.equal(results.filter(result => result.reason === verificationReason.ALLOW).length, 1);
  assert.equal(results.filter(result => result.reason === verificationReason.REPLAYED_CHALLENGE).length, 1);
  assert.equal(h.store.stateOf(challenge.nonce), challengeState.CONSUMED);
});
