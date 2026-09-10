import { readFile } from 'node:fs/promises';
import { parseEnv } from 'node:util';
import { execFileSync } from 'node:child_process';
import { pathToFileURL } from 'node:url';
import { createPublicClient, http } from 'viem';
import { privateKeyToAddress } from 'viem/accounts';
import { sepolia } from 'viem/chains';
import { demoConfig } from '../scripts/security/demo-config.mjs';
import { readGateECredentialSnapshot, validateGateESnapshotFreshness } from '../scripts/security/gate-e-secure-bridge.mjs';
import { selectController, inspectSerial } from './serial-readiness.mjs';
import { preflightFile, sanitizePreflight, writeReport } from './evidence.mjs';

export function checkCredential(snapshot, now = BigInt(Math.floor(Date.now() / 1000))) {
  validateGateESnapshotFreshness(snapshot, { nowSeconds: () => now });
  const fail = reason => { throw new Error(reason); };
  if (snapshot.credentialName !== demoConfig.credential.name) fail('WRONG_CREDENTIAL');
  if (snapshot.state.status !== 2) fail('NOT_REGISTERED');
  if (snapshot.owner?.toLowerCase() !== demoConfig.expectedOwner.toLowerCase()) fail('WRONG_OWNER');
  if (snapshot.expiry <= now || snapshot.expiry <= snapshot.block.timestamp) fail('REGISTRY_EXPIRED');
  if (snapshot.resolver?.toLowerCase() !== demoConfig.expectedResolver.toLowerCase()) fail('WRONG_RESOLVER');
  if (!snapshot.access || typeof snapshot.access.active !== 'boolean' ||
      typeof snapshot.access.validUntil !== 'bigint') fail('ACCESS_RECORD_INVALID');
  if (snapshot.access.validUntil - now < demoConfig.validityMinimumRemainingSeconds ||
      snapshot.access.validUntil - snapshot.block.timestamp < demoConfig.validityMinimumRemainingSeconds) fail('ACCESS_VALIDITY_MARGIN');
  return snapshot.access.active ? 'ACTIVE' : 'INACTIVE';
}

const knownFailures = new Set(['WRONG_CREDENTIAL', 'NOT_REGISTERED', 'WRONG_OWNER', 'REGISTRY_EXPIRED',
  'WRONG_RESOLVER', 'ACCESS_RECORD_INVALID', 'ACCESS_VALIDITY_MARGIN', 'STALE_BLOCK', 'BLOCK_FROM_FUTURE',
  'BLOCK_TIMESTAMP_INVALID', 'LOCAL_CLOCK_INVALID', 'ENS_TIMEOUT', 'WRONG_CHAIN', 'RPC_SNAPSHOT_ERROR',
  'LOCAL_CONFIG_MISSING', 'LOCAL_CONFIG_INVALID', 'LOCAL_CONFIG_DRIFT', 'CH343_AMBIGUOUS', 'CH343_UNAVAILABLE',
  'SERIAL_BUSY_OR_UNAVAILABLE', 'SERIAL_ERROR', 'SERIAL_CLOSED', 'SERIAL_OPEN_TIMEOUT', 'SERIAL_CLOSE_TIMEOUT',
  'SERIAL_CLOSE_FAILED', 'PASSIVE_SERIAL_UNSUPPORTED', 'MANUAL_COLD_BOOT_REQUIRED', 'FIRMWARE_NOT_READY',
  'NONCE_MISMATCH', 'NONCE_READ_FAILED']);

function safeFailure(error) {
  const reason = error?.reason ?? error?.message;
  return knownFailures.has(reason) ? reason : 'CHECK_UNAVAILABLE';
}

async function bounded(operation, ms, reason) {
  let timer;
  try {
    return await Promise.race([Promise.resolve().then(operation), new Promise((_, reject) => {
      timer = setTimeout(() => reject(new Error(reason)), ms);
    })]);
  } finally { clearTimeout(timer); }
}

// Only public-client reads are accepted here. No signer, challenge store, or writer is constructed.
export async function runPreflight({ loadConfig, listSerial, probeSerial, readSnapshot, readNonces,
  now = () => BigInt(Math.floor(Date.now() / 1000)), writesPlanned = true, revision = 'UNAVAILABLE' }) {
  const checks = [];
  const check = async (name, operation) => {
    try { const detail = await operation(); checks.push({ name, result: 'PASS', detail }); return detail; }
    catch (error) { checks.push({ name, result: 'FAIL', detail: safeFailure(error) }); return undefined; }
  };
  const config = await check('LOCAL CONFIG', loadConfig);
  // Never include the loaded config in the returned report.
  if (config) checks.at(-1).detail = 'LOADED';
  const devices = await check('SERIAL', async () => {
    const found = selectController(await listSerial());
    if (!/^COM\d+$/i.test(found.path)) throw new Error('PASSIVE_SERIAL_UNSUPPORTED');
    return found.path;
  });
  if (devices) await check('HARDWARE READINESS', async () => {
    const result = await probeSerial(devices);
    if (result !== 'BOOT_OBSERVED') throw new Error(result);
    return 'BOOT_OBSERVED (I2C ACK implied by firmware readiness)';
  });
  if (config) {
    checks.push({ name: 'FALLBACK', result: 'INFO', detail: config.fallbackConfigured ? 'CONFIGURED' : 'NOT CONFIGURED' });
    await check('PRIMARY RPC / POLICY', async () => {
      const snapshot = await readSnapshot(config, demoConfig.credential.label);
      const policy = checkCredential(snapshot, now());
      checks.push({ name: 'CHAIN', result: 'INFO', detail: 'Sepolia 11155111' },
        { name: 'SNAPSHOT BLOCK', result: 'INFO', detail: String(snapshot.block.number) },
        { name: 'VALID UNTIL', result: 'INFO', detail: String(snapshot.access.validUntil) });
      return policy;
    });
    await check('DEV NONCES', async () => {
      const { latest, pending } = await bounded(() => readNonces(config), 8000, 'NONCE_READ_FAILED');
      if (!Number.isSafeInteger(latest) || !Number.isSafeInteger(pending) || latest < 0 || pending < 0) throw new Error('NONCE_READ_FAILED');
      checks.push({ name: 'DEV latest', result: 'INFO', detail: String(latest) },
        { name: 'DEV pending', result: 'INFO', detail: String(pending) });
      if (writesPlanned && latest !== pending) throw new Error('NONCE_MISMATCH');
      return writesPlanned ? 'NO PENDING WRITES' : 'WRITES NOT PLANNED';
    });
  }
  return { schemaVersion: 1, completedAt: new Date().toISOString(), credential: demoConfig.credential.name,
    result: checks.some(item => item.result === 'FAIL') ? 'FAIL' : 'PASS',
    revision: /^[0-9a-f]{40}$/.test(revision) ? revision : 'UNAVAILABLE', checks };
}

export async function loadLocalConfig() {
  let local;
  let nfc;
  try {
    // Runtime configuration loading only; values are never logged or persisted.
    local = { ...parseEnv(await readFile(new URL('../.env.local', import.meta.url), 'utf8')), ...process.env };
    nfc = { ...parseEnv(await readFile(new URL('../.env.nfc.local', import.meta.url), 'utf8')), ...process.env };
  } catch { throw new Error('LOCAL_CONFIG_MISSING'); }
  if (local.ENS_PARENT_NAME?.trim() !== 'demo-access.eth' || nfc.ENS_PARENT_NAME?.trim() !== 'demo-access.eth' ||
      !/^0x[0-9a-fA-F]{64}$/.test(local.DEV_PRIVATE_KEY ?? '')) throw new Error('LOCAL_CONFIG_INVALID');
  for (const key of ['SEPOLIA_RPC_URL']) {
    if ((local[key]?.trim() ?? '') !== (nfc[key]?.trim() ?? '')) throw new Error('LOCAL_CONFIG_DRIFT');
  }
  let devAddress;
  try { devAddress = privateKeyToAddress(local.DEV_PRIVATE_KEY); }
  catch { throw new Error('LOCAL_CONFIG_INVALID'); }
  const client = createPublicClient({ chain: sepolia,
    transport: http(local.SEPOLIA_RPC_URL?.trim() || undefined, { timeout: 3000, retryCount: 0 }) });
  return { client, devAddress, fallbackConfigured: Boolean(nfc.SEPOLIA_RPC_FALLBACK_URL?.trim()) };
}

export async function main(args = process.argv.slice(2)) {
  if (args.some(arg => !['--observe-boot', '--no-writes-planned'].includes(arg))) throw new Error('LOCAL_CONFIG_INVALID');
  const { SerialPort } = await import('serialport');
  let revision;
  try { revision = execFileSync('git', ['rev-parse', 'HEAD'], { encoding: 'utf8', timeout: 2000,
    cwd: new URL('..', import.meta.url), stdio: ['ignore', 'pipe', 'ignore'], windowsHide: true }).trim(); } catch { /* optional */ }
  console.log('Keep Seeker away. Passive serial observation only; no automatic reset.');
  if (args.includes('--observe-boot')) console.log('Observing boot for 30 seconds. If needed, use one short RST/EN press after a full cold boot.');
  const report = await runPreflight({ loadConfig: loadLocalConfig, revision,
    writesPlanned: !args.includes('--no-writes-planned'), listSerial: () => SerialPort.list(),
    probeSerial: path => inspectSerial({ SerialPort, path, observeMs: args.includes('--observe-boot') ? 30000 : 5000 }),
    readSnapshot: async (config, label) => (await readGateECredentialSnapshot({ primaryClient: config.client, label })).snapshot,
    readNonces: async ({ client, devAddress: address }) => {
      const [latest, pending] = await Promise.all(['latest', 'pending'].map(blockTag => client.getTransactionCount({ address, blockTag })));
      return { latest, pending };
    },
  });
  console.log(`SOURCE: ${report.revision}\nCREDENTIAL: ${report.credential}`);
  for (const check of report.checks) console.log(`${check.name}: ${check.result} — ${check.detail}`);
  console.log('MANUAL PHONE CHECK: Seeker unlocked; NFC ON; Internet available; ENSv2 Access Demo open; Privy authenticated.');
  console.log(`PRIVY WALLET shown in app must be ${demoConfig.expectedOwner}.`);
  if (report.checks.some(check => check.name === 'HARDWARE READINESS' && check.result === 'FAIL')) {
    console.log('HARDWARE READINESS: MANUAL COLD BOOT REQUIRED — follow DEMO_RUNBOOK.md; no tap until readiness is verified.');
  }
  await writeReport(preflightFile, sanitizePreflight(report));
  const failed = report.checks.find(check => check.result === 'FAIL');
  console.log(report.result === 'PASS' ? 'DEMO PREFLIGHT: PASS'
    : `DEMO PREFLIGHT: STOP — ${failed.detail}`);
  process.exitCode = report.result === 'PASS' ? 0 : 1;
  return report;
}

if (process.argv[1] && pathToFileURL(process.argv[1]).href === import.meta.url) {
  main().catch(() => { console.error('DEMO PREFLIGHT: STOP — CHECK_UNAVAILABLE'); process.exitCode = 1; });
}
