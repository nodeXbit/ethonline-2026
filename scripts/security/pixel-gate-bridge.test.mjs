import assert from 'node:assert/strict';
import { request } from 'node:http';
import { test } from 'node:test';
import { privateKeyToAccount } from 'viem/accounts';
import { buildAccessChallengeTypedData } from './holder-proof.mjs';
import {
  createPixelGateServer, createPixelGateService, pixelGateChallengeTtlSeconds,
} from './pixel-gate-bridge.mjs';

const holder = privateKeyToAccount(`0x${'11'.repeat(32)}`);
const credential = 'staff-001.keys.demo-access.eth';
const now = 1_800_000_000n;
const word = value => ({ tokenId: 1n, expiry: now + 1_000n, status: 2, latestOwner: value });
function snapshot(name = credential, options = {}) {
  const owner = options.owner ?? holder.address;
  return {
    block: { number: 1n, hash: `0x${'12'.repeat(32)}`, timestamp: now },
    credentialName: name,
    credentialNode: options.node,
    registry: '0x0AeB395be893149c0b60D9DAC3Ba55139D0dcA1a',
    provenanceMatches: true,
    parent: { state: word(owner), expiry: now + 1_000n, subregistry: '0x2d249472B83A453086254Acd8a42913D8e45a2Fd' },
    namespace: { state: word(owner), expiry: now + 1_000n, subregistry: '0x0AeB395be893149c0b60D9DAC3Ba55139D0dcA1a', resolver: '0x20766FB21498a99350F922ee3163F5a95F354a7f' },
    state: word(owner), owner, expiry: now + 1_000n,
    subregistry: '0x0000000000000000000000000000000000000000',
    resolver: '0x20766FB21498a99350F922ee3163F5a95F354a7f',
    access: { active: true, validUntil: now + 1_000n },
    resourcePolicy: options.resourcePolicy ?? null,
  };
}
function harness(options = {}) {
  let sequence = 0;
  let lastName;
  const service = createPixelGateService({
    now: () => options.now ?? now,
    random: () => Buffer.alloc(32, ++sequence),
    sessionRandom: () => Buffer.alloc(16, ++sequence),
    readSnapshot: async name => {
      lastName = name;
      if (options.rpcFailure) throw Error('secret rpc url');
      const started = service.startResult;
      return snapshot(name, { node: started.challenge.slice(0, 66), resourcePolicy: options.resourcePolicy });
    },
  });
  return { service, get lastName() { return lastName; } };
}
async function start(h, resource = 'lab', name = credential) {
  const result = h.service.start({ resource, credential: name });
  h.service.startResult = result;
  return result;
}
async function sign(result) {
  const raw = result.challenge.slice(2);
  const challenge = {
    credential: `0x${raw.slice(0, 64)}`, resource: `0x${raw.slice(64, 128)}`,
    nonce: `0x${raw.slice(128, 192)}`, expiresAt: BigInt(`0x${raw.slice(192)}`),
  };
  return holder.signTypedData(buildAccessChallengeTypedData(challenge));
}

test('Pixel session creation returns an exact 104-byte resource-bound challenge', async () => {
  const h = harness(); const created = await start(h);
  assert.match(created.sessionId, /^[0-9a-f]{32}$/);
  assert.equal(created.challenge.length, 210);
  assert.equal(created.resource.slug, 'lab');
  assert.equal(BigInt(created.expiresAt) - now, pixelGateChallengeTtlSeconds);
});
test('resource and credential are immutable after issuance', async () => {
  const h = harness(); const created = await start(h, 'lab');
  const signature = await sign(created);
  const result = await h.service.complete({ sessionId: created.sessionId, signature, resource: 'front-door', credential: 'other' });
  assert.equal(result.body.reason, 'RESOURCE_POLICY_MISSING');
  assert.equal(h.lastName, credential);
});
test('valid proof completion remains subject to the existing resource policy verifier', async () => {
  const h = harness(); const created = await start(h);
  const result = await h.service.complete({ sessionId: created.sessionId, signature: await sign(created) });
  assert.deepEqual([result.status, result.body.allowed, result.body.reason], [200, false, 'RESOURCE_POLICY_MISSING']);
  assert.equal(result.body.checks.holder, 'Verified');
  assert.equal(result.body.checks.registration, 'Valid');
  assert.equal(result.body.checks.globalAccess, 'Allowed');
});
test('completed session rejects replay', async () => {
  const h = harness(); const created = await start(h); const signature = await sign(created);
  await h.service.complete({ sessionId: created.sessionId, signature });
  assert.deepEqual(await h.service.complete({ sessionId: created.sessionId, signature }), { status: 409, body: { error: 'REPLAY' } });
});
test('expired session is denied before ENS', async () => {
  const h = harness(); const created = await start(h); const signature = await sign(created);
  const expired = createPixelGateService({ now: () => now + 61n, random: () => Buffer.alloc(32, 8), sessionRandom: () => Buffer.alloc(16, 9), readSnapshot: async () => { throw Error('must not read'); } });
  const old = expired.start({ resource: 'lab', credential });
  // This service issues at its own clock, so mutate the clock through a closure instead.
  let clock = now; const timed = createPixelGateService({ now: () => clock, random: () => Buffer.alloc(32, 7), sessionRandom: () => Buffer.alloc(16, 6), readSnapshot: async () => { throw Error('must not read'); } });
  const item = timed.start({ resource: 'lab', credential }); clock += pixelGateChallengeTtlSeconds;
  const denied = await timed.complete({ sessionId: item.sessionId, signature: await sign(item) });
  assert.equal(denied.body.reason, 'CHALLENGE_EXPIRED');
  assert.ok(created && old && signature);
});
test('unknown session and malformed input fail closed', async () => {
  const h = harness();
  assert.equal((await h.service.complete({ sessionId: '00'.repeat(16), signature: `0x${'00'.repeat(65)}` })).body.error, 'UNKNOWN_SESSION');
  assert.equal((await h.service.complete({ sessionId: 'bad', signature: 'bad' })).body.error, 'MALFORMED_INPUT');
  assert.throws(() => h.service.start({ resource: 'lab', credential: 'INVALID' }));
});
test('RPC failures return sanitized authoritative denial', async () => {
  const h = harness({ rpcFailure: true }); const created = await start(h);
  const result = await h.service.complete({ sessionId: created.sessionId, signature: await sign(created) });
  assert.equal(result.body.reason, 'RPC_UNAVAILABLE');
  assert.doesNotMatch(JSON.stringify(result.body), /secret|url/i);
});
test('HTTP bridge is localhost-only and validates request shape', async t => {
  const h = harness(); const server = createPixelGateServer({ service: h.service });
  await new Promise(resolve => server.listen(0, '127.0.0.1', resolve));
  t.after(() => server.close());
  const port = server.address().port;
  const call = (body, path = '/gate/sessions', host = `127.0.0.1:${port}`) => new Promise((resolve, reject) => {
    const bytes = Buffer.from(JSON.stringify(body));
    const req = request({ hostname: '127.0.0.1', port, path, method: 'POST', headers: { host, 'content-type': 'application/json', 'content-length': bytes.length } }, res => {
      let data = ''; res.on('data', chunk => { data += chunk; }); res.on('end', () => resolve({ status: res.statusCode, body: JSON.parse(data) }));
    }); req.on('error', reject); req.end(bytes);
  });
  assert.equal((await call({ resource: 'lab', credential })).status, 201);
  assert.equal((await call({ resource: 'lab', credential, owner: holder.address })).body.error, 'MALFORMED_INPUT');
  assert.equal((await call({ resource: 'lab', credential }, '/gate/sessions', 'evil.example')).body.error, 'LOCAL_HOST_REQUIRED');
});
