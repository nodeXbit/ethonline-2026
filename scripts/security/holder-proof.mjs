import { randomBytes as nodeRandomBytes } from 'node:crypto';
import { isAddress, isAddressEqual, recoverTypedDataAddress } from 'viem';
import { isAuthorized } from '../ensv2/access-record.mjs';

export const accessChallengeDomain = Object.freeze({
  name: 'ENSv2 Access',
  version: '1',
  chainId: 11155111,
});

export const accessChallengeTypes = Object.freeze({
  AccessChallenge: Object.freeze([
    Object.freeze({ name: 'credential', type: 'bytes32' }),
    Object.freeze({ name: 'resource', type: 'bytes32' }),
    Object.freeze({ name: 'nonce', type: 'bytes32' }),
    Object.freeze({ name: 'expiresAt', type: 'uint64' }),
  ]),
});

export const challengeState = Object.freeze({
  PENDING: 'PENDING',
  CONSUMED: 'CONSUMED',
});

export const verificationReason = Object.freeze({
  ALLOW: 'ALLOW',
  ACCESS_DENIED: 'ACCESS_DENIED',
  UNKNOWN_CHALLENGE: 'UNKNOWN_CHALLENGE',
  EXPIRED_CHALLENGE: 'EXPIRED_CHALLENGE',
  REPLAYED_CHALLENGE: 'REPLAYED_CHALLENGE',
  WRONG_CREDENTIAL: 'WRONG_CREDENTIAL',
  WRONG_RESOURCE: 'WRONG_RESOURCE',
  INVALID_SIGNATURE: 'INVALID_SIGNATURE',
  WRONG_SIGNER: 'WRONG_SIGNER',
  ENS_STATE_ERROR: 'ENS_STATE_ERROR',
  ENS_POLICY_ERROR: 'ENS_POLICY_ERROR',
});

const bytes32Pattern = /^0x[0-9a-fA-F]{64}$/;
const signaturePattern = /^0x[0-9a-fA-F]{130}$/;
const maxUint64 = (1n << 64n) - 1n;
const systemNow = () => BigInt(Math.floor(Date.now() / 1000));

function requireBytes32(value, label) {
  if (!bytes32Pattern.test(value)) throw new TypeError(`${label} must be bytes32 hex.`);
  return value.toLowerCase();
}

function requireUint64(value, label) {
  const integer = typeof value === 'bigint' ? value : BigInt(value);
  if (integer < 0n || integer > maxUint64) throw new RangeError(`${label} must be uint64.`);
  return integer;
}

function challengeKey(nonce) {
  return requireBytes32(nonce, 'nonce');
}

export function buildAccessChallengeTypedData(challenge) {
  return {
    domain: accessChallengeDomain,
    types: accessChallengeTypes,
    primaryType: 'AccessChallenge',
    message: {
      credential: requireBytes32(challenge.credential, 'credential'),
      resource: requireBytes32(challenge.resource, 'resource'),
      nonce: requireBytes32(challenge.nonce, 'nonce'),
      expiresAt: requireUint64(challenge.expiresAt, 'expiresAt'),
    },
  };
}

export class IssuedChallengeStore {
  #records = new Map();

  add(challenge) {
    const canonical = Object.freeze(buildAccessChallengeTypedData(challenge).message);
    const key = challengeKey(canonical.nonce);
    if (this.#records.has(key)) throw new Error('Challenge nonce collision.');
    this.#records.set(key, { challenge: canonical, state: challengeState.PENDING });
    return canonical;
  }

  lookup(nonce) {
    if (!bytes32Pattern.test(nonce)) return undefined;
    return this.#records.get(nonce.toLowerCase());
  }

  stateOf(nonce) {
    return this.lookup(nonce)?.state;
  }

  consumePending(nonce, now) {
    const record = this.lookup(nonce);
    if (!record) return verificationReason.UNKNOWN_CHALLENGE;
    if (record.state === challengeState.CONSUMED) return verificationReason.REPLAYED_CHALLENGE;
    if (requireUint64(now, 'now') >= record.challenge.expiresAt) {
      return verificationReason.EXPIRED_CHALLENGE;
    }
    record.state = challengeState.CONSUMED;
    return undefined;
  }
}

export function issueAccessChallenge({
  store,
  credential,
  resource,
  ttlSeconds = 60n,
  now = systemNow,
  randomBytes = nodeRandomBytes,
}) {
  const issuedAt = requireUint64(now(), 'now');
  const ttl = requireUint64(ttlSeconds, 'ttlSeconds');
  if (ttl === 0n || issuedAt + ttl > maxUint64) throw new RangeError('Invalid challenge TTL.');
  const bytes = randomBytes(32);
  if (!(bytes instanceof Uint8Array) || bytes.length !== 32) {
    throw new TypeError('randomBytes must return exactly 32 bytes.');
  }
  const nonce = `0x${Buffer.from(bytes).toString('hex')}`;
  return store.add({ credential, resource, nonce, expiresAt: issuedAt + ttl });
}

function deny(reason, details = {}) {
  return { allowed: false, reason, ...details };
}

export async function verifyAccessAttempt({
  store,
  nonce,
  signature,
  expectedCredential,
  expectedResource,
  readCredentialState,
  now = systemNow,
}) {
  const record = store.lookup(nonce);
  if (!record) return deny(verificationReason.UNKNOWN_CHALLENGE);
  if (record.state === challengeState.CONSUMED) {
    return deny(verificationReason.REPLAYED_CHALLENGE);
  }

  const currentTime = requireUint64(now(), 'now');
  if (currentTime >= record.challenge.expiresAt) {
    return deny(verificationReason.EXPIRED_CHALLENGE);
  }
  if (!bytes32Pattern.test(expectedCredential) ||
      expectedCredential.toLowerCase() !== record.challenge.credential) {
    return deny(verificationReason.WRONG_CREDENTIAL);
  }
  if (!bytes32Pattern.test(expectedResource) ||
      expectedResource.toLowerCase() !== record.challenge.resource) {
    return deny(verificationReason.WRONG_RESOURCE);
  }
  if (!signaturePattern.test(signature)) return deny(verificationReason.INVALID_SIGNATURE);

  let recoveredSigner;
  try {
    recoveredSigner = await recoverTypedDataAddress({
      ...buildAccessChallengeTypedData(record.challenge), signature,
    });
  } catch {
    return deny(verificationReason.INVALID_SIGNATURE);
  }

  let credentialState;
  try {
    credentialState = await readCredentialState({ credential: record.challenge.credential });
  } catch {
    return deny(verificationReason.ENS_STATE_ERROR, { recoveredSigner });
  }
  if (!isAddress(credentialState?.owner) ||
      !isAddressEqual(recoveredSigner, credentialState.owner)) {
    return deny(verificationReason.WRONG_SIGNER, { recoveredSigner });
  }

  // This synchronous state transition is the one-shot boundary. It occurs only
  // after current-holder proof and before the ENS access policy is evaluated.
  const consumptionFailure = store.consumePending(nonce, now());
  if (consumptionFailure) return deny(consumptionFailure, { recoveredSigner });

  try {
    const allowed = isAuthorized(credentialState);
    return {
      allowed,
      reason: allowed ? verificationReason.ALLOW : verificationReason.ACCESS_DENIED,
      recoveredSigner,
    };
  } catch {
    return deny(verificationReason.ENS_POLICY_ERROR, { recoveredSigner });
  }
}
