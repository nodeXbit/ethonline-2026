import assert from 'node:assert/strict';
import test from 'node:test';
import { readFile } from 'node:fs/promises';
import { encodeAbiParameters, zeroAddress } from 'viem';
import { privateKeyToAccount } from 'viem/accounts';
import { resources, encodeResources, decodeResources, validateResources } from './access-resources.mjs';
import { credentialCandidate, issuer, validateCredentialSnapshot, readResourceCredential, verifyResourceProof } from './resource-verifier.mjs';
import { IssuedChallengeStore, issueAccessChallenge, buildAccessChallengeTypedData } from './holder-proof.mjs';
import { resourceVectors, vectorText } from './resource-vectors.mjs';
import { encodeAccess } from '../ensv2/access-record.mjs';

const now = 1_800_000_000n;
const holder = privateKeyToAccount(`0x${'11'.repeat(32)}`);
const other = privateKeyToAccount(`0x${'22'.repeat(32)}`);
const candidate = credentialCandidate('staff-001.keys.demo-access.eth');
export function resourceSnapshot(overrides = {}) {
  return { credentialName: candidate.name, credentialNode: candidate.node, registry: issuer.issuerRegistry,
    resolver: issuer.issuerResolver, subregistry: zeroAddress, provenanceMatches: true,
    state: { status: 2 }, owner: holder.address, expiry: now + 600n,
    block: { number: 100n, timestamp: now }, access: { active: true, validUntil: now + 300n },
    resourcePolicy: resources.map(r => r.resourceId),
    parent: { state: { status: 2 }, subregistry: issuer.parentRegistry, expiry: now + 1000n },
    namespace: { state: { status: 2 }, subregistry: issuer.issuerRegistry, resolver: issuer.issuerResolver, expiry: now + 1000n },
    ...overrides };
}
async function harness(overrides = {}, resourceId = resources[1].resourceId) {
  const store = new IssuedChallengeStore();
  const clock = { now };
  const challenge = issueAccessChallenge({ store, credential: candidate.node, resource: resourceId, now: () => clock.now });
  const signature = await holder.signTypedData(buildAccessChallengeTypedData(challenge));
  let snapshot = resourceSnapshot(overrides), reads = 0;
  const args = { store, challenge, signature, credential: candidate, resourceId, now: () => clock.now,
    readSnapshot: async () => { reads++; return snapshot; } };
  return { args, clock, set: value => { snapshot = value; }, reads: () => reads, verify: extra => verifyResourceProof({ ...args, ...extra }) };
}

test('shared public vectors match exact resource IDs ABI namehash challenge digest APDU and serial', async () => {
  assert.equal(await readFile(new URL('../../fixtures/lockens-access-v1.properties', import.meta.url), 'utf8'), vectorText(await resourceVectors()));
  assert.throws(() => validateResources({ version: 1, resources: resources.map(r => ({ ...r, resourceId: resources[0].resourceId })) }));
});
test('canonical resources policy rejects missing malformed duplicate unsorted unknown padded and trailing data', () => {
  assert.equal(decodeResources('0x'), null);
  assert.deepEqual(decodeResources(encodeResources([])), []);
  assert.deepEqual(decodeResources(encodeResources(resources.map(r => r.resourceId))), resources.map(r => r.resourceId).sort());
  for (const ids of [[resources[0].resourceId, resources[0].resourceId], resources.map(r => r.resourceId), [`0x${'ff'.repeat(32)}`]]) {
    assert.throws(() => decodeResources(encodeAbiParameters([{ type: 'bytes32[]' }], [ids])));
  }
  for (const raw of ['0x00', `${encodeResources([])}00`, `0x${'ff'.repeat(512)}`]) assert.throws(() => decodeResources(raw));
});
for (const [name, expected] of [
  ['Staff-001.keys.demo-access.eth', 'INVALID_CREDENTIAL_NAME'], ['staff-001.keys.demo-access.eth.', 'INVALID_CREDENTIAL_NAME'],
  ['staff-001.demo-access.eth', 'WRONG_NAMESPACE'], ['a.b.keys.demo-access.eth', 'INVALID_CREDENTIAL_NAME'],
  ['guest-001.other.eth', 'WRONG_NAMESPACE'], ['', 'INVALID_CREDENTIAL_NAME'],
  [`${'a'.repeat(45)}.keys.demo-access.eth`, 'INVALID_CREDENTIAL_NAME'],
]) test(`untrusted candidate rejects ${name || 'empty'}`, () => assert.throws(() => credentialCandidate(name), { reason: expected }));

for (const [name, overrides, reason] of [
  ['missing policy', { resourcePolicy: null }, 'RESOURCE_POLICY_MISSING'],
  ['malformed policy', { policyError: 'RESOURCE_POLICY_INVALID' }, 'RESOURCE_POLICY_INVALID'],
  ['invalid resource set shape', { resourcePolicy: 'all' }, 'RESOURCE_POLICY_INVALID'],
  ['missing access expiry', { access: { active: true } }, 'ACCESS_POLICY_INVALID'],
  ['invalid active flag', { access: { active: 'true', validUntil: now + 60n } }, 'ACCESS_POLICY_INVALID'],
  ['empty policy', { resourcePolicy: [] }, 'RESOURCE_NOT_ALLOWED'],
  ['Lab denied', { resourcePolicy: [resources[0].resourceId] }, 'RESOURCE_NOT_ALLOWED'],
  ['suspended', { access: { active: false, validUntil: now + 60n } }, 'ACCESS_SUSPENDED'],
  ['access expired', { access: { active: true, validUntil: now - 1n } }, 'ACCESS_EXPIRED'],
  ['registration expired', { expiry: now }, 'REGISTRATION_EXPIRED'],
  ['unregistered', { state: { status: 0 } }, 'CREDENTIAL_NOT_REGISTERED'],
  ['wrong owner', { owner: other.address }, 'HOLDER_MISMATCH'],
  ['wrong R1', { registry: zeroAddress }, 'WRONG_REGISTRY'],
  ['wrong S1', { resolver: zeroAddress }, 'WRONG_RESOLVER'],
  ['wrong provenance', { provenanceMatches: false }, 'WRONG_REGISTRY'],
  ['unsupported subregistry', { subregistry: issuer.issuerRegistry }, 'CREDENTIAL_NOT_REGISTERED'],
  ['old block', { block: { number: 1n, timestamp: now - 61n } }, 'RPC_UNAVAILABLE'],
  ['future block', { block: { number: 1n, timestamp: now + 16n } }, 'RPC_UNAVAILABLE'],
]) test(`resource verifier denies ${name}`, async () => {
  const h = await harness(overrides); const result = await h.verify(); assert.equal(result.allowed, false); assert.equal(result.reason, reason);
});
test('Front permitted and inclusive access validity boundary allowed', async () => {
  const h = await harness({ resourcePolicy: [resources[0].resourceId], access: { active: true, validUntil: now } }, resources[0].resourceId);
  assert.equal((await h.verify()).allowed, true);
});
test('same proof same resource replay denied without fresh ENS read; concurrent consumption once', async () => {
  const h = await harness(); const results = await Promise.all([h.verify(), h.verify()]);
  assert.equal(results.filter(r => r.allowed).length, 1); assert.equal(results.filter(r => r.reason === 'REPLAY').length, 1);
  const reads = h.reads(); assert.equal((await h.verify()).reason, 'REPLAY'); assert.equal(h.reads(), reads);
});
test('credential substitution resource substitution and cross-resource proof fail without consuming valid context', async () => {
  const h = await harness();
  assert.equal((await h.verify({ credential: credentialCandidate('visitor-001.keys.demo-access.eth') })).allowed, false);
  assert.equal((await h.verify({ resourceId: resources[0].resourceId })).allowed, false);
  const second = issueAccessChallenge({ store: h.args.store, credential: candidate.node, resource: resources[0].resourceId, now: () => now });
  assert.equal((await h.verify({ challenge: second, resourceId: resources[0].resourceId })).allowed, false);
  assert.equal((await h.verify()).allowed, true);
});
test('TOCTOU owner and policy are reread after proof; old snapshot cannot grant', async () => {
  for (const [change, reason] of [[{ owner: other.address }, 'HOLDER_MISMATCH'], [{ resourcePolicy: [] }, 'RESOURCE_NOT_ALLOWED']]) {
    const h = await harness(); validateCredentialSnapshot(resourceSnapshot(), candidate, now);
    h.set(resourceSnapshot(change)); assert.equal((await h.verify()).reason, reason); assert.equal(h.reads(), 1);
  }
});
test('expiry after ENS await denies and unavailable RPC has no untrusted error text', async () => {
  const h = await harness();
  assert.equal((await h.verify({ readSnapshot: async () => { throw Error('https://secret.invalid/token'); } })).reason, 'RPC_UNAVAILABLE');
  const result = await h.verify({ readSnapshot: async () => { h.clock.now += 60n; return resourceSnapshot(); } });
  assert.equal(result.reason, 'CHALLENGE_EXPIRED');
});
test('suspended proof consumed and cannot be replayed after policy restoration', async () => {
  const h = await harness({ access: { active: false, validUntil: now + 30n } });
  assert.equal((await h.verify()).reason, 'ACCESS_SUSPENDED'); h.set(resourceSnapshot());
  assert.equal((await h.verify()).reason, 'REPLAY');
});

function rpcFixture(change = () => {}) {
  const calls = [], hash = `0x${'ab'.repeat(32)}`;
  const client = { getChainId: async () => 11155111, getBlock: async () => ({ number: 100n, timestamp: now, hash }),
    getCode: async args => { calls.push(args); return '0x1234'; },
    readContract: async args => {
      calls.push(args); const { address, functionName: fn, args: params } = args;
      let value;
      if (fn === 'findTokenId') value = 1n;
      else if (fn === 'getState') value = { tokenId: 1n, status: 2, latestOwner: holder.address, expiry: now + 1000n };
      else if (fn === 'getOwner') value = holder.address;
      else if (fn === 'getExpiry') value = now + 1000n;
      else if (fn === 'getResolver') value = issuer.issuerResolver;
      else if (fn === 'getSubregistry') value = address === issuer.ethRegistry ? issuer.parentRegistry : address === issuer.parentRegistry ? issuer.issuerRegistry : zeroAddress;
      else if (fn === 'verifyContract') value = params[0] === issuer.issuerRegistry ? issuer.userRegistryImplementation : issuer.permissionedResolverImplementation;
      else if (fn === 'data') value = params[1] === 'resources.v1' ? encodeResources(resources.map(r => r.resourceId)) : encodeAccess({ active: true, validUntil: now + 100n });
      else throw Error('UNEXPECTED_READ');
      return change(args, value) ?? value;
    } };
  return { client, calls };
}
test('production ENS adapter pins all hierarchy provenance and resource reads to one canonical block', async () => {
  const f = rpcFixture(); const snapshot = await readResourceCredential(f.client, candidate.name);
  validateCredentialSnapshot(snapshot, candidate, now);
  assert.deepEqual(snapshot.resourcePolicy, resources.map(r => r.resourceId).sort());
  assert.ok(f.calls.every(call => call.blockNumber === 100n));
  assert.equal(f.calls.filter(c => c.functionName === 'data').length, 2);
});
for (const [name, change] of [
  ['hierarchy R0', (a,v) => a.address === issuer.ethRegistry && a.functionName === 'getSubregistry' ? zeroAddress : v],
  ['hierarchy R1', (a,v) => a.address === issuer.parentRegistry && a.functionName === 'getSubregistry' ? zeroAddress : v],
  ['provenance', (a,v) => a.functionName === 'verifyContract' ? zeroAddress : v],
  ['state coherence', (a,v) => a.functionName === 'getState' ? { ...v, tokenId: 9n } : v],
]) test(`production adapter rejects ${name}`, async () => { const f = rpcFixture(change); await assert.rejects(readResourceCredential(f.client, candidate.name)); });
test('production adapter rejects a block reorg during snapshot', async () => {
  const f = rpcFixture(); let reads = 0; f.client.getBlock = async () => ({ number: 100n, timestamp: now, hash: `0x${(++reads === 1 ? 'ab' : 'cd').repeat(32)}` });
  await assert.rejects(readResourceCredential(f.client, candidate.name));
});
