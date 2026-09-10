import assert from 'node:assert/strict';
import test from 'node:test';
import { privateKeyToAccount } from 'viem/accounts';
import { accessChallengeDomain, accessChallengeTypes } from './holder-proof.mjs';
import { loadGateBTypedData, verifyGateBSignature } from './gate-b-verify.mjs';

const testAccount = privateKeyToAccount(`0x${'44'.repeat(32)}`);

test('Gate B Android fixture exactly matches the canonical Gate A schema', async () => {
  const { androidTypedData, canonical } = await loadGateBTypedData();
  assert.deepEqual(androidTypedData.domain, accessChallengeDomain);
  assert.deepEqual(androidTypedData.types.AccessChallenge, accessChallengeTypes.AccessChallenge);
  assert.equal(androidTypedData.primaryType, 'AccessChallenge');
  assert.deepEqual(canonical.domain, accessChallengeDomain);
  assert.deepEqual(canonical.types, accessChallengeTypes);
  assert.equal(canonical.primaryType, 'AccessChallenge');
  assert.equal(canonical.message.expiresAt, 1_700_000_060n);
});

test('Gate B verifier recovers and matches an EIP-712 signer', async () => {
  const { canonical } = await loadGateBTypedData();
  const signature = await testAccount.signTypedData(canonical);
  const result = await verifyGateBSignature({
    walletAddress: testAccount.address,
    signature,
  });
  assert.equal(result.recoveredSigner, testAccount.address);
  assert.equal(result.matches, true);
});

test('Gate B verifier rejects a different expected wallet', async () => {
  const { canonical } = await loadGateBTypedData();
  const signature = await testAccount.signTypedData(canonical);
  const differentAccount = privateKeyToAccount(`0x${'55'.repeat(32)}`);
  const result = await verifyGateBSignature({
    walletAddress: differentAccount.address,
    signature,
  });
  assert.equal(result.recoveredSigner, testAccount.address);
  assert.equal(result.matches, false);
});
