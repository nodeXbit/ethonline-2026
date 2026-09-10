import { SerialPort } from 'serialport';
import { main } from '../scripts/security/gate-e-secure-bridge.mjs';
import { selectController } from './serial-readiness.mjs';
import { attemptFile, newAttempt, completeAttempt, writeReport } from './evidence.mjs';

// Resolve the actual established CH343 port; never inherit a stale COM number.
let bridgeStarted = false;
const start = newAttempt();
await writeReport(attemptFile, start);
try {
  const args = process.argv.slice(2);
  if (args.some(arg => arg !== '--check-replay')) throw new Error('Unsupported demo bridge option');
  const controller = selectController(await SerialPort.list());
  bridgeStarted = true;
  await main(['--port', controller.path, ...args], { preserveBoot: true });
} catch {
  // Gate E main records its own terminal outcome. Only discovery failures lack an attempt.
  console.error('DEMO BRIDGE: STOP — inspect sanitized attempt evidence and cold-boot runbook.');
  process.exitCode = 1;
}
  if (!bridgeStarted) await writeReport(attemptFile, completeAttempt(start, undefined));
