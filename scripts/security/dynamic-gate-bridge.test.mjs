import assert from 'node:assert/strict';
import test from 'node:test';
import { readFile } from 'node:fs/promises';
import { DynamicGateBridge, GateMonitorState, VirtualGateProfiles, parseCredentialFrame } from './dynamic-gate-bridge.mjs';
import { simulateResourceSession, simulatedMatrix } from './resource-simulation.mjs';
import { resources } from './access-resources.mjs';
import { GateELineBuffer } from './gate-e-secure-bridge.mjs';

test('complete simulated matrix exercises discovery holder resource replay and controller confirmation', async () => {
  const rows = await simulatedMatrix();
  assert.deepEqual(rows.map(row => row.reason), ['ALLOW', 'RESOURCE_NOT_ALLOWED', 'ACCESS_SUSPENDED', 'ALLOW', 'ALLOW', 'HOLDER_MISMATCH']);
  assert.ok(rows.slice(0,5).every(row => row.replay === 'REPLAY'));
});
test('virtual gate is latched before asynchronous discovery and selection applies only to next session', async () => {
  const run = await simulateResourceSession({ onPreflight: profiles => profiles.select('server-room') });
  assert.equal(run.result.allowed, true); assert.equal(run.monitor.resource.slug, 'lab');
  assert.equal(run.monitor.selected.slug, 'server-room'); assert.equal(run.issued, 1); assert.equal(run.finalReads, 1);
});
test('monitor never grants before controller confirms and timeout removes apparent verifier ALLOW', async () => {
  const run = await simulateResourceSession({ confirm: false });
  assert.equal(run.beforeConfirmation.final, null); assert.equal(run.result.allowed, true);
  assert.equal(run.monitor.final, 'SESSION ERROR'); assert.equal(run.monitor.reason, 'CONTROLLER_NOT_CONFIRMED');
  assert.equal(run.monitor.controller, 'Not confirmed');
});
test('owner and policy changes after discovery cause final authoritative denial', async () => {
  const owner = await simulateResourceSession({ ownerChanged: true }); assert.equal(owner.result.reason, 'HOLDER_MISMATCH');
  const policy = await simulateResourceSession({ policyChanged: true }); assert.equal(policy.result.reason, 'RESOURCE_NOT_ALLOWED');
});
test('existing staff missing resource policy denies clearly without legacy authorization', async () => {
  const run = await simulateResourceSession({ snapshotChange: { resourcePolicy: null } });
  assert.equal(run.result.reason, 'RESOURCE_POLICY_MISSING'); assert.equal(run.monitor.final, 'ACCESS DENIED');
});
test('frame parser rejects controls malformed hex oversized data and invalid UTF8', () => {
  for (const frame of ['CREDENTIAL_V1=0xff', 'CREDENTIAL_V1=0x', `CREDENTIAL_V1=0x${'61'.repeat(65)}`,
    'CREDENTIAL_V1=0x61620a', 'CREDENTIAL_V1=0xFf', 'CREDENTIAL=staff-001.keys.demo-access.eth']) assert.throws(() => parseCredentialFrame(frame));
});
function emptyBridge(discover = async () => {}) {
  let issues = 0; const sent = [], profiles = new VirtualGateProfiles();
  const bridge = new DynamicGateBridge({ profiles, discover, issue: () => { issues++; throw Error('UNEXPECTED_ISSUE'); },
    verify: () => { throw Error('UNEXPECTED_VERIFY'); }, sendLine: async line => sent.push(line) });
  bridge.waitForCompletion().catch(() => {});
  return { bridge, sent, issues: () => issues };
}

test('SAM startup failure preserves command diagnostics and cannot become ready', async () => {
  const monitor = new GateMonitorState(); monitor.begin();
  const f = emptyBridge(); f.bridge.onEvent = event => monitor.event(event);
  await f.bridge.receiveLine('PN532_BOOT_START_V1');
  await f.bridge.receiveLine('PN532_BOOT_V1=CMD:14;WRITE_RC:-2;READ_RC:NONE');
  await f.bridge.receiveLine('GATE_E: STOP - INITIALIZATION: PN532 SAM configuration failed');
  try { await f.bridge.waitForCompletion(); } catch (error) { monitor.fail(error, f.bridge.result); }
  await f.bridge.receiveLine('PRESENT_SEEKER');
  assert.equal(monitor.publicState().final, 'SESSION ERROR');
  assert.deepEqual(monitor.publicState().readerDiagnostic, { command:'14', writeResult:-2, readResult:null });
  assert.equal(f.issues(), 0);
});

test('recorded boot failure before any tap remains a technical error with no challenge', async () => {
  const monitor = new GateMonitorState(); monitor.begin();
  const f = emptyBridge(); f.bridge.onEvent = event => monitor.event(event);
  await f.bridge.receiveLine('PN532_INIT_V1=ADDRESS:24;WIRE_RC:5');
  await f.bridge.receiveLine('GATE_E: STOP - INITIALIZATION: PN532 did not ACK at I2C address 0x24');
  await f.bridge.receiveLine('AUTHORIZATION: DENY');
  try { monitor.complete(await f.bridge.waitForCompletion()); }
  catch (error) { monitor.fail(error, f.bridge.result); }
  assert.equal(monitor.publicState().final, 'SESSION ERROR');
  assert.equal(monitor.publicState().reason, 'READER_INITIALIZATION_FAILED');
  assert.equal(monitor.publicState().stage, 'INITIALIZATION');
  assert.deepEqual(monitor.publicState().readerDiagnostic, { address: '0x24', wireResult: 5 });
  assert.equal(monitor.publicState().busy, false);
  assert.equal(f.issues(), 0); assert.deepEqual(f.sent, []);
});

test('reader readiness requires dynamic marker and NFC only starts on activation', async () => {
  const monitor = new GateMonitorState(); monitor.begin();
  const f = emptyBridge(); f.bridge.onEvent = event => monitor.event(event);
  await f.bridge.receiveLine('PRESENT_SEEKER');
  assert.equal(monitor.publicState().stage, 'INITIALIZATION');
  await f.bridge.receiveLine('LOCKENS_DYNAMIC_V1_READY');
  await f.bridge.receiveLine('PRESENT_SEEKER');
  assert.equal(monitor.publicState().stage, 'READER_READY');
  await f.bridge.receiveLine('TARGET_ACTIVATION: PASS');
  assert.equal(monitor.publicState().stage, 'NFC_STARTED');
  f.bridge.transportFailure('SESSION_TIMEOUT');
  try { await f.bridge.waitForCompletion(); } catch (error) { monitor.fail(error, f.bridge.result); }
  assert.equal(monitor.publicState().final, 'SESSION ERROR');
  assert.equal(monitor.publicState().reason, 'SESSION_TIMEOUT');
});

test('startup probe retries cannot advertise readiness before configuration completes', async () => {
  const monitor = new GateMonitorState(); monitor.begin();
  const f = emptyBridge(); f.bridge.onEvent = event => monitor.event(event);
  for (const rc of [2, 2, 0]) {
    await f.bridge.receiveLine(`PN532_INIT_V1=ADDRESS:24;WIRE_RC:${rc}`);
    assert.equal(monitor.publicState().stage, 'INITIALIZATION');
    assert.equal(monitor.publicState().final, null);
  }
  await f.bridge.receiveLine('GATE_E: STOP - INITIALIZATION: PN532 firmware/version query failed');
  try { await f.bridge.waitForCompletion(); } catch (error) { monitor.fail(error, f.bridge.result); }
  assert.equal(monitor.publicState().reason, 'READER_INITIALIZATION_FAILED');
  assert.equal(monitor.publicState().final, 'SESSION ERROR');
  assert.equal(f.issues(), 0);
});

test('exhausted startup probes remain a technical failure without issuing a challenge', async () => {
  const monitor = new GateMonitorState(); monitor.begin();
  const f = emptyBridge(); f.bridge.onEvent = event => monitor.event(event);
  for (let i = 0; i < 10; i++) await f.bridge.receiveLine('PN532_INIT_V1=ADDRESS:24;WIRE_RC:2');
  await f.bridge.receiveLine('GATE_E: STOP - INITIALIZATION: PN532 I2C probe failed; see WIRE_RC');
  try { await f.bridge.waitForCompletion(); } catch (error) { monitor.fail(error, f.bridge.result); }
  await f.bridge.receiveLine('PRESENT_SEEKER');
  assert.equal(monitor.publicState().stage, 'INITIALIZATION');
  assert.equal(monitor.publicState().final, 'SESSION ERROR');
  assert.equal(monitor.publicState().readerDiagnostic.wireResult, 2);
  assert.equal(f.issues(), 0); assert.deepEqual(f.sent, []);
});
test('unsupported dynamic firmware and missing credential never silently issue a guest challenge', async () => {
  const f = emptyBridge(); await f.bridge.receiveLine('TARGET_ACTIVATION: PASS');
  await assert.rejects(f.bridge.waitForCompletion(), { reason: 'DYNAMIC_PROTOCOL_REQUIRED' }); assert.equal(f.issues(), 0);
  const g = emptyBridge();
  for (const line of ['LOCKENS_DYNAMIC_V1_READY', 'TARGET_ACTIVATION: PASS', 'SELECT: PASS', 'WAITING_CHALLENGE']) await g.bridge.receiveLine(line);
  await assert.rejects(g.bridge.waitForCompletion(), { reason: 'DYNAMIC_PROTOCOL_REQUIRED' }); assert.equal(g.issues(), 0);
});
test('untrusted candidate provenance failure consumes no challenge and late discovery cannot revive a stopped attempt', async () => {
  let complete;
  const f = emptyBridge(() => new Promise(resolve => { complete = resolve; }));
  for (const line of ['LOCKENS_DYNAMIC_V1_READY', 'TARGET_ACTIVATION: PASS', 'SELECT: PASS']) await f.bridge.receiveLine(line);
  const pending = f.bridge.receiveLine(`CREDENTIAL_V1=0x${Buffer.from('staff-001.keys.demo-access.eth').toString('hex')}`);
  await f.bridge.receiveLine('WAITING_CHALLENGE');
  await f.bridge.receiveLine('GATE_E: STOP - STATUS: transport failed');
  complete(); await pending; assert.equal(f.issues(), 0); assert.equal(f.bridge.state, 'DONE');
  const g = emptyBridge(async () => { throw Error('fake RPC secret'); });
  for (const line of ['LOCKENS_DYNAMIC_V1_READY', 'TARGET_ACTIVATION: PASS', 'SELECT: PASS']) await g.bridge.receiveLine(line);
  await g.bridge.receiveLine(`CREDENTIAL_V1=0x${Buffer.from('staff-001.keys.demo-access.eth').toString('hex')}`);
  await assert.rejects(g.bridge.waitForCompletion(), { reason: 'RPC_UNAVAILABLE' }); assert.equal(g.issues(), 0);
});
test('malformed discovery no challenge; credential transport failure does not imply missing selection', async () => {
  for (const [line, reason] of [['CREDENTIAL_V1=0xff', 'INVALID_CREDENTIAL_NAME'],
    ['GATE_E: STOP - GET_CREDENTIAL: ISO-DEP exchange failed', 'TRANSPORT_FAILURE']]) {
    const f = emptyBridge();
    for (const start of ['LOCKENS_DYNAMIC_V1_READY', 'TARGET_ACTIVATION: PASS', 'SELECT: PASS']) await f.bridge.receiveLine(start);
    await f.bridge.receiveLine(line); await assert.rejects(f.bridge.waitForCompletion(), { reason }); assert.equal(f.issues(), 0);
  }
});
test('shared APDU and serial frames match firmware constants and bounded serial buffer', async () => {
  const fixture = Object.fromEntries((await readFile(new URL('../../fixtures/lockens-access-v1.properties', import.meta.url), 'utf8')).trim().split('\n').map(line => {
    const split = line.indexOf('='); return [line.slice(0,split), line.slice(split+1)];
  }));
  assert.equal(parseCredentialFrame(fixture['serial.credential']).name, fixture['credential.name']);
  const source = await readFile(new URL('../../firmware/pn532_secure_access/pn532_secure_access.ino', import.meta.url), 'utf8');
  const get = source.match(/getCredential\[\] = \{([^}]+)\}/)[1].match(/0x[\da-f]+/gi).map(x => Number(x).toString(16).padStart(2,'0')).join('');
  assert.equal(get, fixture['apdu.credential']);
  assert.match(source, /#if LOCKENS_DYNAMIC_NFC/); assert.match(source, /credentialLength > 66/);
  assert.match(source, /kChallengeBodyLength = 104/); assert.match(source, /kChallengeLineLength = 220/);
  for (const key of ['serial.ready','serial.credential','serial.challenge','serial.allow','serial.allowConfirmed','serial.deny','serial.denyConfirmed']) {
    const buffer = new GateELineBuffer(); const bytes = Buffer.from(fixture[key] + '\r\n');
    const lines = []; for (const byte of bytes) lines.push(...buffer.push(Buffer.from([byte])).lines);
    assert.deepEqual(lines, [fixture[key] + '\r']);
  }
});
test('monitor resets prior grant at session start and exact profiles reject unknown resources', () => {
  const monitor = new GateMonitorState(); monitor.begin(); monitor.complete({ allowed: true, controllerOutcome: 'CONTROLLER_CONFIRMED' });
  assert.equal(monitor.publicState().final, 'ACCESS GRANTED'); monitor.begin(); assert.equal(monitor.publicState().final, null);
  assert.throws(() => monitor.begin()); assert.throws(() => monitor.profiles.select('all'));
  assert.equal(resources.length, 3);
});
