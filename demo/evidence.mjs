import { randomUUID } from 'node:crypto';
import { mkdir, open, rename, rm } from 'node:fs/promises';
import { demoConfig } from '../scripts/security/demo-config.mjs';

export const attemptFile = new URL('../.runtime/gate-e-last-attempt.json', import.meta.url);
export const preflightFile = new URL('../.runtime/demo-preflight.json', import.meta.url);
const address = value => typeof value === 'string' && /^0x[0-9a-fA-F]{40}$/.test(value);
const timestamp = value => typeof value === 'string' && /^\d{4}-\d\d-\d\dT\d\d:\d\d:\d\d\.\d{3}Z$/.test(value) && Number.isFinite(Date.parse(value));
const oneOf = (value, values) => values.includes(value);
const reasons = new Set([
  'ALLOW', 'ACCESS_DENIED', 'UNKNOWN_CHALLENGE', 'EXPIRED_CHALLENGE', 'REPLAYED_CHALLENGE',
  'WRONG_CREDENTIAL', 'WRONG_RESOURCE', 'INVALID_SIGNATURE', 'WRONG_SIGNER', 'ENS_STATE_ERROR',
  'ENS_POLICY_ERROR', 'VERIFICATION_ERROR', 'PROOF_TIMEOUT', 'MALFORMED_PROOF', 'CHALLENGE_ERROR',
  'BRIDGE_ERROR', 'SERIAL_ERROR', 'SERIAL_CLOSED', 'SERIAL_INPUT_OVERFLOW', 'ATTEMPT_DEADLINE_EXCEEDED',
  'FIRMWARE_CONFIRMATION_TIMEOUT', 'FIRMWARE_AUTHORIZATION_MISMATCH', 'AUTHORIZATION_WRITE_ERROR',
  'UNKNOWN_FIRMWARE_STOP', 'LOCAL_CLOCK_INVALID', 'BLOCK_TIMESTAMP_INVALID', 'BLOCK_FROM_FUTURE',
  'STALE_BLOCK', 'WRONG_CHAIN', 'ENS_TIMEOUT', 'RPC_SNAPSHOT_ERROR', 'BRIDGE_START_FAILED',
  ...['INITIALIZATION', 'TARGET_ACTIVATION', 'SELECT', 'CHALLENGE', 'SEND_CHALLENGE', 'STATUS',
    'GET_SIGNATURE', 'SIGNATURE_LENGTH', 'AUTHORIZATION', 'SERIAL_INPUT'].map(stage => `FIRMWARE_STOP_${stage}`),
]);

export function newAttempt() {
  return { schemaVersion: 1, attemptId: randomUUID(), startedAt: new Date().toISOString(),
    credential: demoConfig.credential.name, resource: demoConfig.resourceId, state: 'IN_PROGRESS' };
}

// Reconstruct, never spread untrusted runtime or verifier objects into a public report.
export function sanitizeAttempt(value) {
  if (!value || value.schemaVersion !== 1 ||
      !/^[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/.test(value.attemptId ?? '') ||
      !timestamp(value.startedAt) || value.credential !== demoConfig.credential.name ||
      value.resource !== demoConfig.resourceId || !oneOf(value.state, ['IN_PROGRESS', 'COMPLETE'])) return null;
  const report = { schemaVersion: 1, attemptId: value.attemptId, startedAt: value.startedAt,
    credential: demoConfig.credential.name, resource: demoConfig.resourceId, state: value.state };
  if (value.state === 'IN_PROGRESS') return report;
  if (!timestamp(value.completedAt) || Date.parse(value.completedAt) < Date.parse(value.startedAt) ||
      !oneOf(value.transportResult, ['COMPLETE', 'TRANSPORT_FAILURE']) ||
      !oneOf(value.verifierResult, ['NOT_RUN', 'VERIFIER_ALLOW', 'VERIFIER_DENY']) ||
      !oneOf(value.controllerConfirmation, ['CONTROLLER_CONFIRMED', 'CONTROLLER_UNCONFIRMED']) ||
      !oneOf(value.controllerDecision, ['NOT_RUN', 'ALLOW', 'DENY']) ||
      !oneOf(value.replayResult, ['DENIED', 'NOT_CHECKED', 'UNEXPECTED_RESULT'])) return null;
  if (value.controllerConfirmation === 'CONTROLLER_CONFIRMED' &&
      (value.controllerDecision === 'NOT_RUN' ||
       (value.transportResult === 'COMPLETE' && value.verifierResult !== `VERIFIER_${value.controllerDecision}`) ||
       (value.controllerDecision === 'ALLOW' && (value.verifierResult !== 'VERIFIER_ALLOW' ||
        value.transportResult !== 'COMPLETE')))) return null;
  Object.assign(report, { completedAt: value.completedAt, transportResult: value.transportResult,
    verifierResult: value.verifierResult, controllerDecision: value.controllerDecision,
    controllerConfirmation: value.controllerConfirmation, replayResult: value.replayResult });
  for (const key of ['verifierReason', 'controllerReason']) {
    if (reasons.has(value[key])) report[key] = value[key];
  }
  for (const key of ['recoveredSigner', 'currentEnsOwner']) {
    if (address(value[key])) report[key] = value[key];
  }
  if (report.recoveredSigner && report.currentEnsOwner) {
    report.ownerMatch = report.recoveredSigner.toLowerCase() === report.currentEnsOwner.toLowerCase();
  }
  if (typeof value.snapshotBlock === 'string' && /^\d{1,20}$/.test(value.snapshotBlock)) {
    report.snapshotBlock = value.snapshotBlock;
  }
  return report;
}

export function completeAttempt(start, bridge, error) {
  const result = bridge?.result;
  return sanitizeAttempt({ ...start, state: 'COMPLETE', completedAt: new Date().toISOString(),
    transportResult: result?.origin === 'TRANSPORT_FAILURE' || !result ? 'TRANSPORT_FAILURE' : 'COMPLETE',
    verifierResult: result?.verifierOutcome ?? 'NOT_RUN',
    verifierReason: result?.ensFailureReason ?? result?.reason,
    recoveredSigner: result?.recoveredSigner, currentEnsOwner: result?.currentEnsOwner,
    snapshotBlock: result?.snapshotBlock,
    controllerDecision: /^AUTHORIZATION=(ALLOW|DENY)$/.test(bridge?.authorizationCommand ?? '')
      ? bridge.authorizationCommand.slice('AUTHORIZATION='.length) : 'NOT_RUN',
    controllerConfirmation: bridge?.authorizationCommand &&
      bridge?.firmwareConfirmation === bridge.authorizationCommand.replace('=', ': ') &&
      result?.controllerOutcome === 'CONTROLLER_CONFIRMED'
      ? 'CONTROLLER_CONFIRMED' : 'CONTROLLER_UNCONFIRMED',
    controllerReason: error?.reason ?? (!bridge ? 'BRIDGE_START_FAILED' : undefined),
    replayResult: !bridge?.checkReplay || !bridge?.replayResult ? 'NOT_CHECKED'
      : bridge.replayResult.allowed === false && bridge.replayResult.reason === 'REPLAYED_CHALLENGE'
        ? 'DENIED' : 'UNEXPECTED_RESULT',
  });
}

export function sanitizePreflight(value) {
  if (!value || value.schemaVersion !== 1 || !timestamp(value.completedAt) ||
      !oneOf(value.result, ['PASS', 'FAIL']) || value.credential !== demoConfig.credential.name) return null;
  return { schemaVersion: 1, completedAt: value.completedAt,
    credential: demoConfig.credential.name, result: value.result };
}

export async function writeReport(file, report) {
  const temporary = new URL(`.${randomUUID()}.tmp`, file);
  try {
    if (!report) return false;
    await mkdir(new URL('.', file), { recursive: true });
    const handle = await open(temporary, 'wx', 0o600);
    try { await handle.writeFile(JSON.stringify(report)); } finally { await handle.close(); }
    await rename(temporary, file);
    return true;
  } catch { return false; }
  finally { await rm(temporary, { force: true }).catch(() => {}); }
}

export async function readReport(file, sanitize) {
  let handle;
  try {
    handle = await open(file, 'r');
    if ((await handle.stat()).size > 8192) return null;
    return sanitize(JSON.parse(await handle.readFile('utf8')));
  } catch { return null; }
  finally { await handle?.close().catch(() => {}); }
}

export async function recordAttempt(run, { write = writeReport, file = attemptFile,
  warn = () => console.warn('ATTEMPT REPORT: UNAVAILABLE') } = {}) {
  const start = newAttempt();
  const report = async value => {
    try { if (!await write(file, value)) warn(); } catch { try { warn(); } catch { /* advisory only */ } }
  };
  await report(start);
  let bridge;
  let failure;
  try { return await run(value => { bridge = value; }); }
  catch (error) { failure = error; throw error; }
  finally { await report(completeAttempt(start, bridge, failure)); }
}
