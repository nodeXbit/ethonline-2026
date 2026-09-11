import assert from 'node:assert/strict';
import { readFile } from 'node:fs/promises';
import test from 'node:test';
import { decodeFunctionData, isAddressEqual, zeroAddress } from 'viem';
import {
  CREDENTIAL_HORIZON,
  DEV,
  FACTORY,
  ISSUER,
  KEYS_OWNER_ROLES,
  PERMISSIONED_RESOLVER_IMPL,
  RECOMMENDED_KEYS_EXPIRY,
  R1_ROOT_ROLES,
  ROLE_REGISTRAR,
  ROLE_REGISTRAR_ADMIN,
  ROLE_RENEW,
  ROLE_RENEW_ADMIN,
  ROLE_SET_DATA,
  ROLE_SET_DATA_ADMIN,
  ROLE_SET_TEXT,
  ROLE_SET_TEXT_ADMIN,
  S1_ROOT_ROLES,
  USER_REGISTRY_IMPL,
  assertCalldataRoundTrip,
  assertKeysAvailable,
  bootstrapSalt,
  buildTransactions,
  expiryPlan,
  factoryAbi,
  predictProxy,
  resolverAbi,
  userRegistryAbi,
} from './issuer-bootstrap-plan.mjs';

const proxyLogic = '0x4444444444444444444444444444444444444444';

test('role bitmaps contain exactly the locked roles', () => {
  assert.equal(ROLE_REGISTRAR, 1n << 0n);
  assert.equal(ROLE_RENEW, 1n << 16n);
  assert.equal(ROLE_REGISTRAR_ADMIN, 1n << 128n);
  assert.equal(ROLE_RENEW_ADMIN, 1n << 144n);
  assert.equal(R1_ROOT_ROLES,
    ROLE_REGISTRAR | ROLE_REGISTRAR_ADMIN | ROLE_RENEW | ROLE_RENEW_ADMIN);
  assert.equal(S1_ROOT_ROLES,
    ROLE_SET_TEXT | ROLE_SET_TEXT_ADMIN | ROLE_SET_DATA | ROLE_SET_DATA_ADMIN);
  assert.equal(KEYS_OWNER_ROLES, ROLE_RENEW);
  assert.equal(KEYS_OWNER_ROLES & ROLE_RENEW_ADMIN, 0n);
});

test('public domain-separated salts are deterministic and distinct', () => {
  assert.equal(bootstrapSalt('r1'),
    0x0390709141520dfbc3c4d8621bd5cef26ad19a30792974bedefdb2c0227d5333n);
  assert.equal(bootstrapSalt('s1'),
    0x4aecba10edcc95650c277561fdf76b6c9b6e7025975c2672c8ce7609f1979c25n);
  assert.notEqual(bootstrapSalt('r1'), bootstrapSalt('s1'));
  assert.throws(() => bootstrapSalt('other'));
});

test('initializer and transaction calldata round trip exactly', () => {
  const plan = buildTransactions({ proxyLogic });
  assert.equal(assertCalldataRoundTrip(plan), true);
  const tx1 = decodeFunctionData({ abi: factoryAbi, data: plan.tx1 });
  const tx2 = decodeFunctionData({ abi: factoryAbi, data: plan.tx2 });
  const init1 = decodeFunctionData({ abi: userRegistryAbi, data: tx1.args[2] });
  const init2 = decodeFunctionData({ abi: resolverAbi, data: tx2.args[2] });
  assert.ok(isAddressEqual(tx1.args[0], USER_REGISTRY_IMPL));
  assert.ok(isAddressEqual(tx2.args[0], PERMISSIONED_RESOLVER_IMPL));
  assert.deepEqual(init1.args, [ISSUER, R1_ROOT_ROLES]);
  assert.deepEqual(init2.args, [ISSUER, S1_ROOT_ROLES, []]);
});

test('proxy prediction matches the pinned factory clone algorithm vector', () => {
  const prediction = predictProxy(DEV, 123n, proxyLogic);
  assert.equal(prediction.outerSalt,
    '0xe1a3bfeaf9608ab0624a01adc54da2f16d691e3a167056115c821f63639fa29e');
  assert.equal(prediction.address, '0x14081E1dd9C4859E1E76F064ac6aAE89A66d9B26');
  assert.notEqual(predictProxy(ISSUER, 123n, proxyLogic).address, prediction.address);
  assert.equal(FACTORY, '0x10dc6333cdfe1fcef624c6e0a8221b91804cd7ef');
});

test('keys availability accepts only a pristine never-registered entry', () => {
  const available = {
    state: { status: 0, expiry: 0n, latestOwner: zeroAddress },
    owner: zeroAddress,
    subregistry: zeroAddress,
    resolver: zeroAddress,
    roleCount: 0n,
  };
  assert.equal(assertKeysAvailable(available), true);
  for (const change of [
    { state: { ...available.state, status: 2 } },
    { state: { ...available.state, expiry: 1n } },
    { state: { ...available.state, latestOwner: DEV } },
    { owner: DEV },
    { subregistry: DEV },
    { resolver: DEV },
    { roleCount: 1n },
  ]) assert.throws(() => assertKeysAvailable({ ...available, ...change }));
});

test('recommended expiry covers credentials and remains safely below every ancestor', () => {
  const parentExpiry = RECOMMENDED_KEYS_EXPIRY + 31n * 86400n;
  const rootExpiry = parentExpiry + 1000n;
  assert.deepEqual(expiryPlan([rootExpiry, parentExpiry]), {
    maximum: parentExpiry,
    recommended: RECOMMENDED_KEYS_EXPIRY,
    margin: 31n * 86400n,
  });
  assert.ok(RECOMMENDED_KEYS_EXPIRY > CREDENTIAL_HORIZON);
  assert.throws(() => expiryPlan([RECOMMENDED_KEYS_EXPIRY]));
  assert.throws(() => expiryPlan([RECOMMENDED_KEYS_EXPIRY + 29n * 86400n]));
  assert.throws(() => expiryPlan([], RECOMMENDED_KEYS_EXPIRY));
});

test('planner source has no signer, private-key, broadcast, or write API', async () => {
  const source = await readFile(new URL('./issuer-bootstrap-plan.mjs', import.meta.url), 'utf8');
  for (const forbidden of [
    'createWalletClient', 'privateKeyToAccount', 'DEV_PRIVATE_KEY', '.writeContract',
    '.sendTransaction', 'eth_sendTransaction', 'eth_sendRawTransaction',
  ]) assert.equal(source.includes(forbidden), false, `forbidden planner API: ${forbidden}`);
});
