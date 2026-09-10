import assert from 'node:assert/strict';
import test from 'node:test';
import { EventEmitter } from 'node:events';
import { mkdtemp, readFile, rm, writeFile } from 'node:fs/promises';
import { tmpdir } from 'node:os';
import { join } from 'node:path';
import { pathToFileURL } from 'node:url';
import { keccak256, stringToHex } from 'viem';
import { demoConfig } from '../scripts/security/demo-config.mjs';
import { credentialLabel, credentialIdentity } from '../scripts/ensv2/access-record.mjs';
import { createProductionDependencies } from './server.mjs';
import { checkCredential, runPreflight } from './preflight.mjs';
import { inspectSerial, readinessObserver, selectController } from './serial-readiness.mjs';
import { completeAttempt, newAttempt, readReport, recordAttempt, sanitizeAttempt, sanitizePreflight, writeReport } from './evidence.mjs';
import { attemptView } from './public/evidence-view.js';

const now = 1_700_000_000n;
const device = { path: 'COM17', vendorId: '1A86', productId: '55D3', friendlyName: 'CH343' };
function snapshot(active = true) {
  return { credentialName: demoConfig.credential.name, state: { status: 2 },
    owner: demoConfig.expectedOwner, resolver: demoConfig.expectedResolver, expiry: now + 200000n,
    access: { active, validUntil: now + 100000n }, block: { timestamp: now, number: 42n } };
}
function preflight(overrides = {}) {
  return runPreflight({ loadConfig: async () => ({ fallbackConfigured: true, rpcUrl: 'https://secret.example/token' }),
    listSerial: async () => [device], probeSerial: async () => 'BOOT_OBSERVED',
    readSnapshot: async (_, label) => { assert.equal(label, 'guest-001'); return snapshot(); },
    readNonces: async () => ({ latest: 4, pending: 4 }), now: () => now, ...overrides });
}
function terminal(overrides = {}) {
  return completeAttempt(newAttempt(), { result: { allowed: true, reason: 'ALLOW',
    verifierOutcome: 'VERIFIER_ALLOW', controllerOutcome: 'CONTROLLER_CONFIRMED',
    recoveredSigner: demoConfig.expectedOwner, currentEnsOwner: demoConfig.expectedOwner, snapshotBlock: '42' },
    authorizationCommand: 'AUTHORIZATION=ALLOW', firmwareConfirmation: 'AUTHORIZATION: ALLOW', ...overrides });
}

test('demo identity is canonical while generic historical defaults stay cred-001', async () => {
  assert.equal(demoConfig.credential.name, 'guest-001.demo-access.eth');
  assert.equal(demoConfig.expectedOwner, '0x3419148731087b970d2059C53780163B452D5FF7');
  assert.equal(demoConfig.resourceId, keccak256(stringToHex('demo-access.eth:door-001')));
  assert.equal(demoConfig.chainId, 11155111);
  assert.equal(credentialLabel, 'cred-001');
  assert.equal(credentialIdentity().name, 'cred-001.demo-access.eth');
  const html = await readFile(new URL('./public/index.html', import.meta.url), 'utf8');
  assert.ok(html.includes(demoConfig.credential.name));
  assert.doesNotMatch(html, /cred-001/);
});

test('production server reads guest and sends both actions through existing writer with guest owner', async () => {
  const calls = [];
  const client = { getChainId: async () => 11155111 };
  const dependencies = createProductionDependencies({ makeClient: () => client,
    read: async (actual, options) => { assert.equal(actual, client); calls.push(options); return snapshot(); },
    connectWriter: async () => ({ parent: 'demo-access.eth' }),
    update: async (_, action, options) => { calls.push({ action, ...options }); },
  });
  await dependencies.readState();
  await dependencies.writeAccess('activate');
  await dependencies.writeAccess('deactivate');
  assert.deepEqual(calls, [{ label: 'guest-001' }, ...['activate', 'deactivate'].map(action => ({
    action, credentialLabel: 'guest-001', credentialOwner: demoConfig.expectedOwner,
  }))]);
});

for (const active of [true, false]) test(`preflight accepts healthy ${active ? 'ACTIVE' : 'INACTIVE'} policy`, async () => {
  const report = await preflight({ readSnapshot: async () => snapshot(active) });
  assert.equal(report.result, 'PASS');
  assert.equal(report.checks.find(check => check.name === 'PRIMARY RPC / POLICY').detail, active ? 'ACTIVE' : 'INACTIVE');
  assert.doesNotMatch(JSON.stringify(report), /https|secret|rpcUrl/);
});

for (const [reason, mutate] of [
  ['WRONG_OWNER', state => { state.owner = '0x1111111111111111111111111111111111111111'; }],
  ['WRONG_RESOLVER', state => { state.resolver = '0x1111111111111111111111111111111111111111'; }],
  ['REGISTRY_EXPIRED', state => { state.expiry = now; }],
  ['ACCESS_VALIDITY_MARGIN', state => { state.access.validUntil = now + 86399n; }],
  ['STALE_BLOCK', state => { state.block.timestamp = now - 61n; }],
  ['NOT_REGISTERED', state => { state.state.status = 0; }],
  ['WRONG_CREDENTIAL', state => { state.credentialName = 'cred-001.demo-access.eth'; }],
  ['ACCESS_RECORD_INVALID', state => { state.access = null; }],
]) test(`preflight fails ${reason}`, async () => {
  const state = snapshot(); mutate(state);
  const report = await preflight({ readSnapshot: async () => state });
  assert.equal(report.result, 'FAIL');
  assert.ok(report.checks.some(check => check.detail === reason));
});

test('24-hour validity boundary passes', () => {
  const state = snapshot(); state.access.validUntil = now + 86400n;
  assert.equal(checkCredential(state, now), 'ACTIVE');
});

test('nonce mismatch stops planned writes but can be reported for a read-only rehearsal', async () => {
  for (const writesPlanned of [true, false]) {
    const report = await preflight({ writesPlanned, readNonces: async () => ({ latest: 4, pending: 5 }) });
    assert.equal(report.result, writesPlanned ? 'FAIL' : 'PASS');
    assert.equal(report.checks.find(check => check.name === 'DEV pending').detail, '5');
  }
});

test('missing, ambiguous, busy hardware and absent boot readiness never pass', async () => {
  assert.throws(() => selectController([]), /CH343_UNAVAILABLE/);
  assert.throws(() => selectController([device, { ...device, path: 'COM18' }]), /CH343_AMBIGUOUS/);
  assert.equal(selectController([device]).path, 'COM17');
  for (const overrides of [
    { listSerial: async () => [] },
    { probeSerial: async () => { throw new Error('SERIAL_BUSY_OR_UNAVAILABLE'); } },
    { probeSerial: async () => 'MANUAL_COLD_BOOT_REQUIRED' },
  ]) assert.equal((await preflight(overrides)).result, 'FAIL');
});

test('preflight reports fallback configuration only and sanitizes arbitrary errors', async () => {
  const report = await preflight({ loadConfig: async () => ({ fallbackConfigured: false }),
    readSnapshot: async () => { throw new Error('https://secret.example/token'); } });
  assert.equal(report.checks.find(check => check.name === 'FALLBACK').detail, 'NOT CONFIGURED');
  assert.doesNotMatch(JSON.stringify(report), /https|secret|token/);
});

test('passive serial uses reset-free Windows settings and never writes or sets control lines', async () => {
  let opened = 0; let closed = 0;
  class Port extends EventEmitter {
    constructor(options) {
      super();
      assert.equal(options.path, 'COM17'); assert.equal(options.hupcl, false);
      assert.equal(options.rtscts, false); assert.equal(options.lock, true);
    }
    open(callback) { opened++; this.isOpen = true; callback(); this.emit('data', Buffer.from('PN532_FIRMWARE=1.6\nGATE_E_READY\nPRESENT_SEEKER\n')); }
    close(callback) { closed++; this.isOpen = false; callback(); }
    write() { assert.fail('No protocol writes permitted'); }
    set() { assert.fail('No automatic reset permitted'); }
  }
  assert.equal(await inspectSerial({ SerialPort: Port, path: 'COM17', observeMs: 0, platform: 'win32' }), 'BOOT_OBSERVED');
  assert.equal(opened, 1); assert.equal(closed, 1);
  class Busy extends Port { open(callback) { callback(new Error('Access denied')); } }
  await assert.rejects(inspectSerial({ SerialPort: Busy, path: 'COM17', observeMs: 0, platform: 'win32' }), /SERIAL_BUSY_OR_UNAVAILABLE/);
});

test('readiness requires the committed ordered boot lines, not stale authorization or partial output', () => {
  for (const lines of ['GATE_E_READY\nPRESENT_SEEKER\n', 'PN532_FIRMWARE=1.5\nGATE_E_READY\nPRESENT_SEEKER\n',
    'PN532_FIRMWARE=1.6\nGATE_E_READY\nPRESENT_SEEKER\nGATE_E: STOP - STATUS: failure\n']) {
    const observer = readinessObserver(); observer.receive(Buffer.from(lines));
    assert.notEqual(observer.result(), 'BOOT_OBSERVED');
  }
});

test('preflight production source has no blockchain writer, signature request or challenge issuance', async () => {
  const source = await readFile(new URL('./preflight.mjs', import.meta.url), 'utf8');
  assert.doesNotMatch(source, /createWalletClient|privateKeyToAccount|signTransaction|signTypedData|writeContract|sendTransaction|issueAccessChallenge|simulateContract/);
  const operations = [];
  await preflight({ readSnapshot: async () => { operations.push('snapshot'); return snapshot(); },
    readNonces: async () => { operations.push('nonces'); return { latest: 0, pending: 0 }; } });
  assert.deepEqual(operations, ['snapshot', 'nonces']);
});

test('IN_PROGRESS removes every previous terminal success field', () => {
  const stale = terminal();
  const report = sanitizeAttempt({ ...stale, state: 'IN_PROGRESS' });
  assert.equal(report.verifierResult, undefined);
  assert.equal(report.controllerConfirmation, undefined);
  assert.equal(report.completedAt, undefined);
  assert.deepEqual(attemptView(report), { verifier: 'IN PROGRESS', controller: 'NOT RUN', replay: 'NOT CHECKED', confirmedAllow: false });
});

test('physical ALLOW requires matching confirmed controller evidence', () => {
  assert.equal(attemptView(terminal()).controller, 'CONFIRMED ALLOW');
  const report = terminal({ firmwareConfirmation: undefined });
  assert.equal(report.verifierResult, 'VERIFIER_ALLOW');
  assert.equal(report.controllerConfirmation, 'CONTROLLER_UNCONFIRMED');
  assert.equal(attemptView(report).controller, 'UNCONFIRMED');
  assert.equal(attemptView(report).confirmedAllow, false);
  assert.equal(attemptView(terminal({ firmwareConfirmation: 'AUTHORIZATION: DENY' })).confirmedAllow, false);
});

test('reporting failure preserves successful and failed bridge outcomes without changing authorization', async () => {
  for (const write of [async () => false, async () => { throw new Error('disk failure'); }]) {
    const result = { allowed: true, reason: 'ALLOW' };
    assert.equal(await recordAttempt(async capture => {
      capture({ result }); return result;
    }, { write, warn: () => {} }), result);
    const failure = new Error('original bridge failure');
    await assert.rejects(recordAttempt(async () => { throw failure; }, { write, warn: () => {} }),
      error => error === failure);
  }
});

test('attempt lifecycle publishes IN_PROGRESS before running and sanitized completion afterward', async () => {
  const events = [];
  await recordAttempt(async capture => {
    assert.deepEqual(events, ['IN_PROGRESS']);
    capture({ result: { origin: 'TRANSPORT_FAILURE', reason: 'SERIAL_ERROR', signature: 'SECRET' } });
  }, { write: async (_, report) => {
    assert.doesNotMatch(JSON.stringify(report), /SECRET|signature/);
    events.push(report.state); return true;
  } });
  assert.deepEqual(events, ['IN_PROGRESS', 'COMPLETE']);
});

test('controller timeout retains verifier ALLOW and reports unconfirmed', () => {
  const report = completeAttempt(newAttempt(), { result: { allowed: true, reason: 'ALLOW', verifierOutcome: 'VERIFIER_ALLOW' },
    authorizationCommand: 'AUTHORIZATION=ALLOW' }, { reason: 'FIRMWARE_CONFIRMATION_TIMEOUT' });
  assert.equal(report.verifierResult, 'VERIFIER_ALLOW');
  assert.equal(report.controllerReason, 'FIRMWARE_CONFIRMATION_TIMEOUT');
  assert.equal(attemptView(report).controller, 'UNCONFIRMED');
});

test('policy DENY and transport failure remain distinct; replay means consumed-proof denial only', () => {
  const deny = terminal({ result: { reason: 'ACCESS_DENIED', allowed: false, verifierOutcome: 'VERIFIER_DENY', controllerOutcome: 'CONTROLLER_CONFIRMED' },
    authorizationCommand: 'AUTHORIZATION=DENY', firmwareConfirmation: 'AUTHORIZATION: DENY',
    checkReplay: true, replayResult: { allowed: false, reason: 'REPLAYED_CHALLENGE' } });
  assert.equal(attemptView(deny).verifier, 'DENY');
  assert.equal(attemptView(deny).controller, 'CONFIRMED DENY');
  assert.equal(attemptView(deny).replay, 'DENIED');
  const failure = terminal({ result: { origin: 'TRANSPORT_FAILURE', reason: 'FIRMWARE_STOP_STATUS' },
    authorizationCommand: undefined, firmwareConfirmation: undefined });
  assert.equal(attemptView(failure).verifier, 'TRANSPORT FAILURE');
  assert.equal(attemptView(failure).controller, 'TRANSPORT FAILURE');
});

test('report objects whitelist fields and reject sensitive values in metadata', () => {
  const report = sanitizeAttempt({ ...terminal(), signature: 'SECRET', proof: 'SECRET', nonce: 'SECRET',
    privateKey: 'SECRET', rpcUrl: 'SECRET', otp: 'SECRET', privyToken: 'SECRET', appSecret: 'SECRET', typedData: 'SECRET',
    verifierReason: 'SECRET', recoveredSigner: 'SECRET', snapshotBlock: 'SECRET' });
  assert.doesNotMatch(JSON.stringify(report), /SECRET|signature|proof|nonce|privateKey|rpcUrl|otp|privyToken|appSecret|typedData/);
  assert.equal(report.verifierReason, undefined);
  assert.equal(report.snapshotBlock, undefined);
  assert.equal(report.ownerMatch, undefined);
  assert.equal(sanitizeAttempt({ ...terminal(), completedAt: 'bad' }), null);
  assert.equal(sanitizeAttempt({ schemaVersion: 1 }), null);
  assert.equal(sanitizePreflight({ ...terminal(), result: 'PASS', rpcUrl: 'SECRET' }).rpcUrl, undefined);
});

test('atomic file replacement, malformed/partial JSON and failed writes are safe', async () => {
  const directory = await mkdtemp(join(tmpdir(), 'demo-evidence-test-'));
  const file = pathToFileURL(join(directory, 'attempt.json'));
  try {
    assert.equal(await writeReport(file, terminal()), true);
    const inProgress = newAttempt();
    assert.equal(await writeReport(file, inProgress), true);
    assert.deepEqual(await readReport(file, sanitizeAttempt), inProgress);
    await writeFile(file, '{"schemaVersion":');
    assert.equal(await readReport(file, sanitizeAttempt), null);
    await writeFile(file, 'x'.repeat(8193));
    assert.equal(await readReport(file, sanitizeAttempt), null);
    assert.equal(await writeReport(new URL('./missing/attempt.json', file), null), false);
  } finally { await rm(directory, { recursive: true, force: true }); }
});
