import assert from 'node:assert/strict';
import { createServer, request as httpRequest } from 'node:http';
import test from 'node:test';
import {
  createDemoHandler, demoAuthority, demoOrigin, publicCredential, safePublicError,
} from './server.mjs';

const owner = '0x1111111111111111111111111111111111111111';
const resolver = '0x2222222222222222222222222222222222222222';
const transactionHash = `0x${'ab'.repeat(32)}`;

function snapshot(overrides = {}) {
  return {
    state: { status: 2 }, owner, expiry: 1_900_000_000n,
    tokenId: 123456789012345678901234567890n, resolver,
    block: { number: 42n, timestamp: 1_700_000_000n },
    access: { active: false, validUntil: 1_800_000_000n }, authorized: false,
    privateKey: 'must-not-leak', rpcUrl: 'https://rpc.example/secret',
    walletClient: { account: 'must-not-leak' }, ...overrides,
  };
}

async function withServer(run, overrides = {}) {
  const state = snapshot();
  const dependencies = {
    readState: async () => state,
    writeAccess: async () => ({ transactionHash, credential: state }),
    ...(typeof overrides === 'function' ? overrides(state) : overrides),
  };
  const server = createServer(createDemoHandler(dependencies));
  await new Promise((resolve, reject) => {
    server.once('error', reject);
    server.listen(0, '127.0.0.1', resolve);
  });
  try {
    await run(server.address().port, state);
  } finally {
    await new Promise(resolve => {
      server.close(resolve);
      server.closeAllConnections();
    });
  }
}

function request(port, path, { method = 'GET', headers = {}, body } = {}) {
  return new Promise((resolve, reject) => {
    const outgoing = httpRequest({ hostname: '127.0.0.1', port, path, method, headers }, response => {
      const chunks = [];
      response.on('data', chunk => chunks.push(chunk));
      response.on('end', () => resolve({
        status: response.statusCode,
        headers: { get: name => response.headers[name.toLowerCase()] ?? null },
        text: Buffer.concat(chunks).toString('utf8'),
      }));
    });
    outgoing.on('error', reject);
    if (body !== undefined) outgoing.write(body);
    outgoing.end();
  });
}

const validWriteHeaders = { Host: demoAuthority, Origin: demoOrigin };

test('public credential serialization keeps chain integers as strings and excludes secrets', () => {
  const result = publicCredential(snapshot());
  assert.deepEqual(result, {
    name: 'cred-001.demo-access.eth', status: 'REGISTERED', owner,
    registryExpiry: '1900000000', tokenId: '123456789012345678901234567890', resolver,
    access: { active: false, validUntil: '1800000000' }, authorization: 'DENY',
  });
  const json = JSON.stringify(result);
  for (const secret of ['must-not-leak', 'privateKey', 'rpcUrl', 'walletClient']) {
    assert.equal(json.includes(secret), false);
  }
});

test('static and API routes are explicitly allowlisted with 404 and 405 handling', async () => {
  await withServer(async port => {
    for (const path of ['/', '/index.html', '/styles.css', '/app.js', '/access-action.js']) {
      assert.equal((await request(port, path)).status, 200);
    }
    assert.equal((await request(port, '/package.json')).status, 404);
    assert.equal((await request(port, '/api/activate')).status, 405);
    assert.equal((await request(port, '/api/credential', { method: 'POST' })).status, 405);
  });
});

test('writes require exact local Host and Origin and reject request bodies', async () => {
  await withServer(async port => {
    assert.equal((await request(port, '/api/activate', {
      method: 'POST', headers: { Host: 'localhost:4173', Origin: demoOrigin },
    })).status, 403);
    assert.equal((await request(port, '/api/activate', {
      method: 'POST', headers: { Host: demoAuthority },
    })).status, 403);
    assert.equal((await request(port, '/api/deactivate', {
      method: 'POST', headers: { ...validWriteHeaders, 'Content-Length': '1' }, body: 'x',
    })).status, 400);
    assert.equal((await request(port, '/api/deactivate', {
      method: 'POST', headers: { ...validWriteHeaders, 'Transfer-Encoding': 'chunked' }, body: 'x',
    })).status, 400);
  });
});

test('one in-process write lock rejects a concurrent write with 409', async () => {
  let releaseWrite;
  let announceStart;
  const started = new Promise(resolve => { announceStart = resolve; });
  const blocked = new Promise(resolve => { releaseWrite = resolve; });
  await withServer(async port => {
    const first = request(port, '/api/activate', { method: 'POST', headers: validWriteHeaders });
    await started;
    const second = await request(port, '/api/activate', {
      method: 'POST', headers: validWriteHeaders,
    });
    assert.equal(second.status, 409);
    releaseWrite();
    assert.equal((await first).status, 200);
  }, { writeAccess: async () => {
    announceStart();
    await blocked;
    return { transactionHash, credential: snapshot() };
  } });
});

for (const action of ['activate', 'deactivate']) {
  test(`sequential ${action} requests write once then return an idempotent no-op`, async () => {
    let writes = 0;
    await withServer(async port => {
      const first = await request(port, `/api/${action}`, {
        method: 'POST', headers: validWriteHeaders,
      });
      const second = await request(port, `/api/${action}`, {
        method: 'POST', headers: validWriteHeaders,
      });
      assert.equal(first.status, 200);
      assert.equal(second.status, 200);
      assert.equal(JSON.parse(first.text).changed, true);
      assert.deepEqual(JSON.parse(second.text), {
        changed: false,
        transactionHash: null,
        credential: publicCredential(action === 'activate'
          ? snapshot({ access: { active: true, validUntil: 1_800_000_000n }, authorized: true })
          : snapshot()),
      });
      assert.equal(writes, 1);
    }, state => {
      if (action === 'deactivate') {
        state.access.active = true;
        state.authorized = true;
      }
      return { writeAccess: async requested => {
        writes++;
        state.access.active = requested === 'activate';
        state.authorized = requested === 'activate';
        return { changed: true, transactionHash, credential: state };
      } };
    });
  });
}

test('safe errors expose only allowed fields and redact URLs', () => {
  const error = new Error('RPC failed via https://rpc.example/private-token');
  error.persistentContext = { operation: 'setData', transactionHash };
  const result = safePublicError(error, { code: 'WRITE_FAILED', stage: 'Sepolia access update' });
  assert.deepEqual(Object.keys(result).sort(), ['code', 'message', 'stage', 'transactionHash']);
  assert.equal(result.message.includes('https://'), false);
  assert.equal(result.message.includes('private-token'), false);
  assert.equal(result.transactionHash, transactionHash);
});

test('security headers protect static and API responses and API is not cached', async () => {
  await withServer(async port => {
    for (const path of ['/', '/api/credential']) {
      const response = await request(port, path);
      assert.match(response.headers.get('content-security-policy'), /frame-ancestors 'none'/);
      assert.equal(response.headers.get('x-content-type-options'), 'nosniff');
      assert.equal(response.headers.get('referrer-policy'), 'no-referrer');
      if (path.startsWith('/api/')) assert.equal(response.headers.get('cache-control'), 'no-store');
    }
  });
});
