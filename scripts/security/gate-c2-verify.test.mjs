import assert from 'node:assert/strict';
import test from 'node:test';
import { loadGateBTypedData } from './gate-b-verify.mjs';

test('Gate C2 manual challenge is the canonical Gate A and Gate B vector', async () => {
  const { androidTypedData, canonical } = await loadGateBTypedData();
  assert.deepEqual(androidTypedData.message, {
    credential: `0x${'11'.repeat(32)}`,
    resource: `0x${'22'.repeat(32)}`,
    nonce: `0x${'33'.repeat(32)}`,
    expiresAt: '1700000060',
  });
  assert.equal(canonical.message.expiresAt, 1_700_000_060n);
});
