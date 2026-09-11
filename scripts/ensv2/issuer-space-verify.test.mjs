import assert from 'node:assert/strict';
import { readFile } from 'node:fs/promises';
import test from 'node:test';
import {
  loadIssuerSpaceConfig,
  validateIssuerSpaceConfig,
} from './issuer-space-verify.mjs';

test('public issuer namespace config matches the checkpointed bootstrap', async () => {
  const config = validateIssuerSpaceConfig(await loadIssuerSpaceConfig());
  assert.equal(config.chainId, 11155111);
  assert.equal(config.namespace, 'keys.demo-access.eth');
  assert.equal(config.issuerRegistry, '0x0AeB395be893149c0b60D9DAC3Ba55139D0dcA1a');
  assert.equal(config.issuerResolver, '0x20766FB21498a99350F922ee3163F5a95F354a7f');
  assert.equal(config.namespaceExpiry, 1814392799);
  assert.equal(config.roles.namespaceOwner, '0x10000');
});

test('config validation rejects address, role, expiry, and evidence drift', async () => {
  const config = await loadIssuerSpaceConfig();
  for (const changed of [
    { ...config, chainId: 1 },
    { ...config, namespace: 'staff-001.keys.demo-access.eth' },
    { ...config, issuerRegistry: config.parentRegistry },
    { ...config, namespaceExpiry: config.namespaceExpiry + 1 },
    { ...config, roles: { ...config.roles, namespaceOwner: '0x10001' } },
    { ...config, bootstrap: { ...config.bootstrap, r1DeploymentBlock: 1 } },
  ]) assert.throws(() => validateIssuerSpaceConfig(changed));
});

test('config and verifier contain no secrets, signer, or blockchain write APIs', async () => {
  const [configSource, verifierSource] = await Promise.all([
    readFile(new URL('../../config/issuer-space.json', import.meta.url), 'utf8'),
    readFile(new URL('./issuer-space-verify.mjs', import.meta.url), 'utf8'),
  ]);
  for (const forbidden of [
    'DEV_PRIVATE_KEY', 'createWalletClient', 'privateKeyToAccount', '.writeContract',
    '.sendTransaction', 'eth_sendTransaction', 'eth_sendRawTransaction', 'apiKey', 'rpcUrl',
  ]) assert.equal(`${configSource}\n${verifierSource}`.includes(forbidden), false,
    `forbidden public config/verifier content: ${forbidden}`);
});
