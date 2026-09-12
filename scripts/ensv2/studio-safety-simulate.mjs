// Read-only regression rehearsal. No account, wallet client, env credentials, signing or broadcast API.
import assert from 'node:assert/strict';
import { readFile } from 'node:fs/promises';
import { createPublicClient, http, encodeFunctionData, decodeFunctionResult,
  encodeAbiParameters, decodeAbiParameters, namehash, zeroAddress } from 'viem';
import { sepolia } from 'viem/chains';

const config = JSON.parse(await readFile(new URL('../../config/issuer-space.json', import.meta.url), 'utf8'));
const base = `https://raw.githubusercontent.com/ensdomains/contracts-v2/${config.source.ensContractsV2Commit}/contracts/deployments/sepolia/`;
const [registryAbi, resolverAbi] = await Promise.all(['UserRegistryImpl', 'PermissionedResolverImpl'].map(async name => {
  const response = await fetch(`${base}${name}.json`);
  assert(response.ok);
  return (await response.json()).abi;
}));
const client = createPublicClient({ chain: sepolia, transport: http('https://ethereum-sepolia-rpc.publicnode.com') });
const allowedMethods = new Set(['eth_chainId', 'eth_getBlockByNumber', 'eth_call', 'eth_getTransactionCount', 'eth_simulateV1']);
const rpc = async (method, params) => {
  assert(allowedMethods.has(method), 'Read-only RPC allowlist');
  return client.request({ method, params });
};
assert.equal(BigInt(await rpc('eth_chainId', [])), 11155111n);
const block = await rpc('eth_getBlockByNumber', ['latest', false]);
const tag = block.number;
const now = BigInt(block.timestamp);
const expiry = now + 3600n;
assert(expiry + 3600n < BigInt(config.namespaceExpiry));
const holder = '0x3419148731087b970d2059C53780163B452D5FF7';
const recipient = '0x3333333333333333333333333333333333333333';
const transferRole = 1n << 156n;
const schema = [{ type: 'bool' }, { type: 'uint64' }];
const descriptor = (target, fn, args, from = config.issuerAddress) => {
  const abi = target === config.issuerRegistry ? registryAbi : resolverAbi;
  return { abi, fn, call: { from, to: target, data: encodeFunctionData({ abi, functionName: fn, args }), gas: '0x989680' } };
};
const r = (fn, args, from) => descriptor(config.issuerRegistry, fn, args, from);
const s = (fn, args, from) => descriptor(config.issuerResolver, fn, args, from);
const decode = (d, result) => decodeFunctionResult({ abi: d.abi, functionName: d.fn, data: result });
async function read(d, at = tag) { return decode(d, await rpc('eth_call', [d.call, at])); }
async function simulate(descriptors) {
  const result = await rpc('eth_simulateV1', [{ validation: false, traceTransfers: false,
    blockStateCalls: [{ calls: descriptors.map(d => d.call) }] }, tag]);
  assert.equal(result.length, 1);
  assert.equal(result[0].calls.length, descriptors.length);
  return result[0].calls.map((call, i) => {
    assert.equal(call.status, '0x1', `Simulation reverted: ${descriptors[i].fn}`);
    return decode(descriptors[i], call.returnData);
  });
}
async function staff(at) {
  const label = 'staff-001', node = namehash(`${label}.${config.namespace}`);
  const id = await read(r('findTokenId', [label]), at);
  const [state, owner, resolver, subregistry, roles, description, avatar, access] = await Promise.all([
    read(r('getState', [id]), at), read(r('getOwner', [id]), at), read(r('getResolver', [label]), at),
    read(r('getSubregistry', [label]), at), read(r('roles', [id, holder]), at),
    read(s('text', [node, 'description']), at), read(s('text', [node, 'avatar']), at),
    read(s('data', [node, 'access.v1']), at),
  ]);
  return { id, state, owner, resolver, subregistry, roles, description, avatar, access: decodeAbiParameters(schema, access) };
}
const before = await staff(tag);
assert.equal(before.owner.toLowerCase(), holder.toLowerCase());
assert.equal(before.state.status, 2);
assert.equal(before.state.expiry, 1793487599n);
assert.equal(before.roles, 0n);
assert.equal(before.description, 'Staff Access Pass');
assert.equal(before.avatar, '');
assert.deepEqual(before.access, [true, 1793487599n]);
const nonceBefore = await rpc('eth_getTransactionCount', [config.issuerAddress, 'latest']);
const summaries = [];
for (const template of ['STAFF', 'VISITOR', 'CONTRACTOR']) {
  const label = `astra-safety-${template.toLowerCase()}-${BigInt(tag)}`;
  const node = namehash(`${label}.${config.namespace}`);
  const roles = template === 'VISITOR' ? transferRole : 0n;
  const active = template !== 'CONTRACTOR';
  const description = `${template} safety simulation`;
  const register = r('register', [label, holder, zeroAddress, config.issuerResolver, roles, expiry]);
  const [id] = await simulate([register]);
  const configure = s('multicall', [[s('setText', [node, 'description', description]).call.data,
    s('setData', [node, 'access.v1', encodeAbiParameters(schema, [active, expiry - 60n])]).call.data]]);
  const checks = [r('getState', [id]), r('getOwner', [id]), r('roles', [id, holder]),
    s('text', [node, 'description']), s('data', [node, 'access.v1'])];
  const results = await simulate([register, configure, ...checks]);
  assert.equal(results[2].status, 2);
  assert.equal(results[2].expiry, expiry);
  assert.equal(results[3].toLowerCase(), holder.toLowerCase());
  assert.equal(results[4], roles);
  assert.equal(results[5], description);
  assert.deepEqual(decodeAbiParameters(schema, results[6]), [active, expiry - 60n]);
  // Existing management path: explicit access, presentation and renewal, entirely in temporary state.
  const managed = await simulate([register, configure,
    s('setData', [node, 'access.v1', encodeAbiParameters(schema, [false, expiry - 60n])]),
    s('setData', [node, 'access.v1', encodeAbiParameters(schema, [true, expiry - 30n])]),
    s('setText', [node, 'description', 'Updated simulation']), r('renew', [id, expiry + 3600n]),
    r('getExpiry', [id]), s('data', [node, 'access.v1']), s('text', [node, 'description'])]);
  assert.equal(managed[6], expiry + 3600n);
  assert.deepEqual(decodeAbiParameters(schema, managed[7]), [true, expiry - 30n]);
  assert.equal(managed[8], 'Updated simulation');
  const summary = { template, issueConfigure: 'PASS', managementSimulation: 'PASS' };
  if (template === 'VISITOR') {
    const transferred = await simulate([register,
      r('safeTransferFrom', [holder, recipient, id, 1n, '0x'], holder), configure,
      r('getOwner', [id]), r('roles', [id, recipient]), s('data', [node, 'access.v1'])]);
    assert.equal(transferred[3].toLowerCase(), recipient.toLowerCase());
    assert.equal(transferred[4], transferRole);
    assert.deepEqual(decodeAbiParameters(schema, transferred[5]), [true, expiry - 60n]);
    const revoked = await simulate([register, r('revokeRoles', [id, transferRole, holder], holder), configure,
      r('roles', [id, holder]), r('getOwner', [id]), s('data', [node, 'access.v1'])]);
    assert.equal(revoked[3], 0n);
    assert.equal(revoked[4].toLowerCase(), holder.toLowerCase());
    assert.deepEqual(decodeAbiParameters(schema, revoked[5]), [true, expiry - 60n]);
    summary.transferBeforeTX2 = 'PASS';
    summary.selfRevokeBeforeTX2 = 'PASS';
  }
  assert.equal((await read(r('getState', [await read(r('findTokenId', [label]), 'latest')]), 'latest')).status, 0);
  summaries.push(summary);
}
const afterBlock = await rpc('eth_getBlockByNumber', ['latest', false]);
assert.deepEqual(await staff(afterBlock.number), before);
assert.equal(await rpc('eth_getTransactionCount', [config.issuerAddress, 'latest']), nonceBefore);
console.log(JSON.stringify({ simulationBlock: BigInt(tag), timestamp: now, finalReadBlock: BigInt(afterBlock.number),
  sourceCommit: config.source.ensContractsV2Commit, results: summaries, existingStaff: before,
  issuerNonce: BigInt(nonceBefore), blockchainWrites: 0, signatures: 0 }, (_, value) => typeof value === 'bigint' ? value.toString() : value, 2));
