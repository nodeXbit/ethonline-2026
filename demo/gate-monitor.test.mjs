import assert from 'node:assert/strict';
import test from 'node:test';
import { request } from 'node:http';
import { createGateMonitorServer } from './gate-monitor.mjs';
import { GateMonitorState } from '../scripts/security/dynamic-gate-bridge.mjs';

test('monitor HTTP allowlist local origin profile latching and physical disabled by default', async () => {
  const monitor = new GateMonitorState();
  const server = createGateMonitorServer({ monitor });
  await new Promise(resolve => server.listen(0, '127.0.0.1', resolve));
  const base = `http://127.0.0.1:${server.address().port}`;
  try {
    assert.equal((await fetch(base)).status, 200);
    assert.equal((await fetch(`${base}/state`)).status, 200);
    assert.equal((await fetch(`${base}/secret`)).status, 404);
    const invalidHost = await new Promise(resolve => {
      request(`${base}/state`, { headers: { host: 'attacker.test' } }, response => {
        response.resume(); response.on('end', () => resolve(response.statusCode));
      }).end();
    });
    assert.equal(invalidHost, 403);
    assert.equal((await fetch(`${base}/gate/lab`, { method: 'POST' })).status, 403);
    const post = (url, extra = {}) => fetch(base + url, { method: 'POST', headers: { origin: base }, ...extra });
    assert.equal((await post('/gate/lab')).status, 200);
    assert.equal(monitor.publicState().resource.slug, 'lab'); monitor.begin();
    await post('/gate/server-room'); assert.equal(monitor.publicState().resource.slug, 'lab');
    assert.equal(monitor.publicState().selected.slug, 'server-room');
    assert.equal((await post('/gate/anything')).status, 400);
    assert.equal((await post('/attempt')).status, 409);
    assert.equal((await post('/gate/lab', { body: 'anything' })).status, 403);
  } finally { await new Promise(resolve => server.close(resolve)); }
});
