import { createServer } from 'node:http';
import { readFile } from 'node:fs/promises';
import { pathToFileURL } from 'node:url';
import { createPublicClient, http } from 'viem';
import { sepolia } from 'viem/chains';
import { GateMonitorState, runDynamicSerial } from '../scripts/security/dynamic-gate-bridge.mjs';

const assets = new Map([['/', ['index.html', 'text/html']], ['/app.js', ['app.js', 'text/javascript']], ['/style.css', ['style.css', 'text/css']]]);
export function createGateMonitorServer({ monitor = new GateMonitorState(), runAttempt } = {}) {
  const server = createServer(async (req, res) => {
    res.setHeader('Content-Security-Policy', "default-src 'self'; style-src 'self'; script-src 'self'; frame-ancestors 'none'; base-uri 'none'");
    res.setHeader('X-Content-Type-Options', 'nosniff');
    res.setHeader('Cache-Control', 'no-store');
    const local = `127.0.0.1:${server.address().port}`;
    const send = (status, value) => { res.writeHead(status, { 'Content-Type': 'application/json' }); res.end(JSON.stringify(value)); };
    if (req.headers.host !== local) return send(403, { error: 'LOCAL_HOST_REQUIRED' });
    if (req.method === 'POST') {
      if (req.headers.origin !== `http://${local}` || req.headers['transfer-encoding'] ||
          (req.headers['content-length'] && req.headers['content-length'] !== '0')) return send(403, { error: 'LOCAL_REQUEST_REQUIRED' });
      if (req.url.startsWith('/gate/')) {
        try {
          monitor.profiles.select(req.url.slice(6));
          if (!monitor.busy && !monitor.attempt.final) monitor.reset();
          return send(200, monitor.publicState());
        }
        catch { return send(400, { error: 'UNKNOWN_RESOURCE' }); }
      }
      if (req.url === '/attempt') {
        if (!runAttempt) return send(409, { error: 'PHYSICAL_PROOF_NOT_ENABLED' });
        if (monitor.busy) return send(409, { error: 'SESSION_BUSY' });
        // runAttempt acquires the session synchronously before its first await.
        void runAttempt(monitor).catch(() => monitor.fail({ reason: 'TRANSPORT_FAILURE' }));
        return send(202, monitor.publicState());
      }
      return send(404, { error: 'NOT_FOUND' });
    }
    if (req.method !== 'GET') return send(405, { error: 'METHOD_NOT_ALLOWED' });
    if (req.url === '/state') return send(200, { ...monitor.publicState(), physicalEnabled: Boolean(runAttempt) });
    const asset = assets.get(req.url);
    if (!asset) return send(404, { error: 'NOT_FOUND' });
    try { res.writeHead(200, { 'Content-Type': asset[1] }); res.end(await readFile(new URL(`./gate-public/${asset[0]}`, import.meta.url))); }
    catch { res.end(); }
  });
  return server;
}

export async function main(args = process.argv.slice(2)) {
  let port = 8790, serialPort, physicalApproved = false;
  for (let i = 0; i < args.length; i++) {
    if (args[i] === '--port') serialPort = args[++i];
    else if (args[i] === '--http-port') port = Number(args[++i]);
    else if (args[i] === '--physical-proof-approved') physicalApproved = true;
    else throw Error('INVALID_ARGUMENT');
  }
  if (!Number.isInteger(port) || port < 1 || port > 65535 || Boolean(serialPort) !== physicalApproved) throw Error('INVALID_ARGUMENT');
  const client = url => createPublicClient({ chain: sepolia, transport: http(url || undefined, { retryCount: 0, timeout: 8_000 }) });
  const server = createGateMonitorServer({ runAttempt: physicalApproved ? monitor => runDynamicSerial({ monitor,
    portPath: serialPort, primaryClient: client(process.env.SEPOLIA_RPC_URL?.trim()),
    fallbackClient: process.env.SEPOLIA_RPC_FALLBACK_URL ? client(process.env.SEPOLIA_RPC_FALLBACK_URL.trim()) : undefined,
  }) : undefined });
  await new Promise(resolve => server.listen(port, '127.0.0.1', resolve));
  console.log(`LockENS Gate Monitor: http://127.0.0.1:${port}`);
  console.log('One reference physical verifier; virtual gates; no door actuator.');
  return server;
}
if (process.argv[1] && pathToFileURL(process.argv[1]).href === import.meta.url) {
  main().catch(() => { console.error('Gate Monitor could not start.'); process.exitCode = 1; });
}
