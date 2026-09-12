import { isAddress, isAddressEqual, namehash, recoverTypedDataAddress, zeroAddress } from 'viem';
import { normalize } from 'viem/ens';
import { readFileSync } from 'node:fs';
import { registryAbi, resolverAbi, factoryAbi } from '../ensv2/issuer-bootstrap-plan.mjs';
import { decodeAccess } from '../ensv2/access-record.mjs';
import { validateIssuerSpaceConfig } from '../ensv2/issuer-space-verify.mjs';
import { buildAccessChallengeTypedData, challengeState } from './holder-proof.mjs';
import { decodeResources, encodeResources, resources } from './access-resources.mjs';
import { validateGateESnapshotFreshness } from './gate-e-secure-bridge.mjs';

export const issuer = Object.freeze(validateIssuerSpaceConfig(JSON.parse(readFileSync(new URL('../../config/issuer-space.json', import.meta.url)))));
export class AccessError extends Error { constructor(reason) { super(reason); this.reason = reason; } }
function requireAccess(condition, reason) { if (!condition) throw new AccessError(reason); }
const same = (a, b) => isAddress(a ?? '') && isAddressEqual(a, b);
export function credentialCandidate(name) {
  requireAccess(typeof name === 'string' && Buffer.byteLength(name, 'utf8') <= 64 && name.length > 0, 'INVALID_CREDENTIAL_NAME');
  try { requireAccess(normalize(name) === name, 'INVALID_CREDENTIAL_NAME'); } catch { throw new AccessError('INVALID_CREDENTIAL_NAME'); }
  requireAccess(name.endsWith(`.${issuer.namespace}`), 'WRONG_NAMESPACE');
  const label = name.slice(0, -(issuer.namespace.length + 1));
  requireAccess(/^[a-z0-9](?:[a-z0-9-]{0,61}[a-z0-9])?$/.test(label), 'INVALID_CREDENTIAL_NAME');
  return Object.freeze({ name, label, node: namehash(name) });
}

// Each provider executes the entire hierarchy/provenance/records read at one block.
export async function readResourceCredential(client, name) {
  const credential = credentialCandidate(name);
  requireAccess(await client.getChainId() === issuer.chainId, 'WRONG_CHAIN');
  const block = await client.getBlock({ blockTag: 'latest' });
  requireAccess(typeof block.number === 'bigint' && /^0x[0-9a-fA-F]{64}$/.test(block.hash), 'RPC_UNAVAILABLE');
  const read = (address, functionName, args, abi = registryAbi) => client.readContract({ address, abi, functionName, args, blockNumber: block.number });
  async function entry(registry, label) {
    const tokenId = await read(registry, 'findTokenId', [label]);
    const [state, owner, expiry, resolver, subregistry] = await Promise.all([
      read(registry, 'getState', [tokenId]), read(registry, 'getOwner', [tokenId]), read(registry, 'getExpiry', [tokenId]),
      read(registry, 'getResolver', [label]), read(registry, 'getSubregistry', [label]),
    ]);
    requireAccess(state.tokenId === tokenId && state.expiry === expiry &&
      (state.status !== 2 || same(state.latestOwner, owner)), 'RPC_UNAVAILABLE');
    return { state, owner, expiry, resolver, subregistry };
  }
  const parent = await entry(issuer.ethRegistry, 'demo-access');
  requireAccess(same(parent.subregistry, issuer.parentRegistry), 'WRONG_REGISTRY');
  const namespace = await entry(issuer.parentRegistry, 'keys');
  requireAccess(same(namespace.subregistry, issuer.issuerRegistry), 'WRONG_REGISTRY');
  requireAccess(same(namespace.resolver, issuer.issuerResolver), 'WRONG_RESOLVER');
  requireAccess(parent.state.status === 2 && namespace.state.status === 2 &&
    parent.expiry > block.timestamp && namespace.expiry > block.timestamp, 'CREDENTIAL_NOT_REGISTERED');
  const [r1Code, s1Code, r1Impl, s1Impl, credentialEntry] = await Promise.all([
    client.getCode({ address: issuer.issuerRegistry, blockNumber: block.number }),
    client.getCode({ address: issuer.issuerResolver, blockNumber: block.number }),
    read(issuer.verifiableFactory, 'verifyContract', [issuer.issuerRegistry], factoryAbi),
    read(issuer.verifiableFactory, 'verifyContract', [issuer.issuerResolver], factoryAbi),
    entry(issuer.issuerRegistry, credential.label),
  ]);
  requireAccess(r1Code && r1Code !== '0x' && same(r1Impl, issuer.userRegistryImplementation), 'WRONG_REGISTRY');
  requireAccess(s1Code && s1Code !== '0x' && same(s1Impl, issuer.permissionedResolverImplementation), 'WRONG_RESOLVER');
  // Never query a phone-supplied or substituted resolver.
  let access = null, resourcePolicy = null, policyError;
  if (same(credentialEntry.resolver, issuer.issuerResolver)) {
    const [rawAccess, rawResources] = await Promise.all([
      read(issuer.issuerResolver, 'data', [credential.node, 'access.v1'], resolverAbi),
      read(issuer.issuerResolver, 'data', [credential.node, 'resources.v1'], resolverAbi),
    ]);
    try { access = decodeAccess(rawAccess); } catch { policyError = 'ACCESS_POLICY_INVALID'; }
    try { resourcePolicy = decodeResources(rawResources); } catch { policyError ??= 'RESOURCE_POLICY_INVALID'; }
  }
  const canonicalBlock = await client.getBlock({ blockNumber: block.number });
  requireAccess(canonicalBlock.hash === block.hash, 'RPC_UNAVAILABLE');
  return { ...credentialEntry, block, credentialName: credential.name, credentialNode: credential.node,
    registry: issuer.issuerRegistry, provenanceMatches: true, parent, namespace, access, resourcePolicy, policyError };
}

export function validateCredentialSnapshot(snapshot, credential, now) {
  validateGateESnapshotFreshness(snapshot, { nowSeconds: () => now });
  requireAccess(typeof snapshot.block.number === 'bigint' && snapshot.block.number >= 0n, 'RPC_UNAVAILABLE');
  requireAccess(snapshot.credentialName === credential.name && snapshot.credentialNode === credential.node, 'INVALID_CREDENTIAL_NAME');
  requireAccess(same(snapshot.registry, issuer.issuerRegistry) && snapshot.provenanceMatches === true, 'WRONG_REGISTRY');
  requireAccess(snapshot.parent?.state?.status === 2 && snapshot.namespace?.state?.status === 2 &&
    same(snapshot.parent.subregistry, issuer.parentRegistry) && same(snapshot.namespace.subregistry, issuer.issuerRegistry), 'WRONG_REGISTRY');
  requireAccess(snapshot.parent.expiry > now && snapshot.namespace.expiry > now, 'REGISTRATION_EXPIRED');
  requireAccess(snapshot.state?.status === 2 && same(snapshot.subregistry, zeroAddress) &&
    isAddress(snapshot.owner ?? '') && !same(snapshot.owner, zeroAddress), 'CREDENTIAL_NOT_REGISTERED');
  requireAccess(snapshot.expiry > now, 'REGISTRATION_EXPIRED');
  requireAccess(same(snapshot.namespace.resolver, issuer.issuerResolver) && same(snapshot.resolver, issuer.issuerResolver), 'WRONG_RESOLVER');
  return snapshot;
}

export async function verifyResourceProof({ store, challenge, signature, credential, resourceId, readSnapshot,
  now = () => BigInt(Math.floor(Date.now() / 1000)) }) {
  const checks = { holder: 'Not checked', registration: 'Not checked', globalAccess: 'Not checked', resourcePolicy: 'Not checked', proof: 'Not checked' };
  const deny = reason => ({ allowed: false, reason, checks: { ...checks } });
  const record = store.lookup(challenge?.nonce);
  if (!record) return deny('INVALID_SIGNATURE');
  if (record.state === challengeState.CONSUMED) { checks.proof = 'Replay'; return deny('REPLAY'); }
  if (now() >= record.challenge.expiresAt) return deny('CHALLENGE_EXPIRED');
  if (record.challenge.credential !== credential.node || challenge.credential !== credential.node) return deny('INVALID_CREDENTIAL_NAME');
  if (!resources.some(r => r.resourceId === resourceId) || record.challenge.resource !== resourceId || challenge.resource !== resourceId) return deny('RESOURCE_NOT_ALLOWED');
  if (challenge.expiresAt !== record.challenge.expiresAt || !/^0x[0-9a-fA-F]{130}$/.test(signature)) return deny('INVALID_SIGNATURE');
  let signer;
  try { signer = await recoverTypedDataAddress({ ...buildAccessChallengeTypedData(record.challenge), signature }); }
  catch { checks.proof = 'Invalid'; return deny('INVALID_SIGNATURE'); }
  let snapshot;
  try { snapshot = await readSnapshot(); validateCredentialSnapshot(snapshot, credential, now()); }
  catch (error) { checks.registration = error?.reason === 'REGISTRATION_EXPIRED' ? 'Expired' : 'Invalid'; return deny(error instanceof AccessError ? error.reason : 'RPC_UNAVAILABLE'); }
  checks.registration = 'Valid';
  if (!same(signer, snapshot.owner)) { checks.holder = 'Failed'; return deny('HOLDER_MISMATCH'); }
  checks.holder = 'Verified';
  const consumed = store.consumePending(record.challenge.nonce, now());
  if (consumed) return deny(consumed === 'REPLAYED_CHALLENGE' ? 'REPLAY' : 'CHALLENGE_EXPIRED');
  checks.proof = 'Fresh';
  if (snapshot.policyError) return deny(snapshot.policyError);
  if (!snapshot.access || typeof snapshot.access.active !== 'boolean' || typeof snapshot.access.validUntil !== 'bigint' ||
      snapshot.access.validUntil < 0n || snapshot.access.validUntil > (1n << 64n) - 1n) return deny('ACCESS_POLICY_INVALID');
  if (!snapshot.access.active) { checks.globalAccess = 'Suspended'; return deny('ACCESS_SUSPENDED'); }
  if (snapshot.access.validUntil < now()) { checks.globalAccess = 'Expired'; return deny('ACCESS_EXPIRED'); }
  checks.globalAccess = 'Allowed';
  if (snapshot.resourcePolicy === null || snapshot.resourcePolicy === undefined) return deny('RESOURCE_POLICY_MISSING');
  try { encodeResources(snapshot.resourcePolicy); } catch { return deny('RESOURCE_POLICY_INVALID'); }
  if (!snapshot.resourcePolicy.includes(resourceId)) { checks.resourcePolicy = 'Denied'; return deny('RESOURCE_NOT_ALLOWED'); }
  checks.resourcePolicy = 'Allowed';
  return { allowed: true, reason: 'ALLOW', checks, currentEnsOwner: snapshot.owner, snapshotBlock: String(snapshot.block.number) };
}
