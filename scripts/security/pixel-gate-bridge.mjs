import { randomBytes } from 'node:crypto';
import { createServer } from 'node:http';
import { pathToFileURL } from 'node:url';
import { concatHex, createPublicClient, http, numberToHex } from 'viem';
import { sepolia } from 'viem/chains';
import { IssuedChallengeStore, issueAccessChallenge } from './holder-proof.mjs';
import { resourceBySlug } from './access-resources.mjs';
import {
  credentialCandidate, readResourceCredential, verifyResourceProof,
} from './resource-verifier.mjs';

export const pixelGatePort = 8792;
export const pixelGateChallengeTtlSeconds = 55n;
const maxBodyBytes = 2_048;
const signaturePattern = /^0x[0-9a-fA-F]{130}$/;

function challengeHex(challenge) {
  return concatHex([
    challenge.credential,
    challenge.resource,
    challenge.nonce,
    numberToHex(challenge.expiresAt, { size: 8 }),
  ]);
}

function publicResult(result) {
  const reason = typeof result.reason === 'string' ? result.reason : 'VERIFIER_ERROR';
  return {
    allowed: result.allowed === true,
    reason,
    checks: {
      holder: result.checks?.holder ?? 'Not checked',
      registration: result.checks?.registration ?? 'Not checked',
      globalAccess: result.checks?.globalAccess ?? 'Not checked',
      resourcePolicy: reason === 'RESOURCE_POLICY_MISSING'
        ? 'Missing' : (result.checks?.resourcePolicy ?? 'Not checked'),
      proof: result.checks?.proof ?? 'Not checked',
    },
    ...(typeof result.snapshotBlock === 'string' ? { snapshotBlock: result.snapshotBlock } : {}),
  };
}

function exactObject(value, keys) {
  return value && typeof value === 'object' && !Array.isArray(value) &&
    Object.keys(value).length === keys.length && keys.every(key => Object.hasOwn(value, key));
}

async function readJson(req) {
  if (req.headers['transfer-encoding']) throw Error('MALFORMED_INPUT');
  const declared = Number(req.headers['content-length']);
  if (!Number.isSafeInteger(declared) || declared < 2 || declared > maxBodyBytes) throw Error('MALFORMED_INPUT');
  const chunks = [];
  let size = 0;
  for await (const chunk of req) {
    size += chunk.length;
    if (size > maxBodyBytes) throw Error('MALFORMED_INPUT');
    chunks.push(chunk);
  }
  if (size !== declared) throw Error('MALFORMED_INPUT');
  try { return JSON.parse(Buffer.concat(chunks).toString('utf8')); }
  catch { throw Error('MALFORMED_INPUT'); }
}

export function createPixelGateService({
  store = new IssuedChallengeStore(),
  now = () => BigInt(Math.floor(Date.now() / 1_000)),
  random = randomBytes,
  readSnapshot,
  sessionRandom = randomBytes,
} = {}) {
  if (typeof readSnapshot !== 'function') throw Error('READ_SNAPSHOT_REQUIRED');
  const sessions = new Map();

  function start({ resource: resourceSlug, credential: credentialName }) {
    const resource = resourceBySlug(resourceSlug);
    const credential = credentialCandidate(credentialName);
    const challenge = issueAccessChallenge({
      store, credential: credential.node, resource: resource.resourceId,
      // Leave bounded room for small reader/holder clock skew while preserving
      // the holder's existing hard maximum of 60 seconds.
      ttlSeconds: pixelGateChallengeTtlSeconds, now, randomBytes: random,
    });
    const idBytes = sessionRandom(16);
    if (!(idBytes instanceof Uint8Array) || idBytes.length !== 16) throw Error('SESSION_RANDOM_INVALID');
    const sessionId = Buffer.from(idBytes).toString('hex');
    if (sessions.has(sessionId)) throw Error('SESSION_ID_COLLISION');
    sessions.set(sessionId, { state: 'PENDING', credential, resource, challenge });
    return {
      sessionId,
      challenge: challengeHex(challenge),
      expiresAt: challenge.expiresAt.toString(),
      resource: { slug: resource.slug, displayName: resource.displayName, resourceId: resource.resourceId },
    };
  }

  async function complete({ sessionId, signature }) {
    if (typeof sessionId !== 'string' || !/^[0-9a-f]{32}$/.test(sessionId) ||
        typeof signature !== 'string' || !signaturePattern.test(signature)) {
      return { status: 400, body: { error: 'MALFORMED_INPUT' } };
    }
    const session = sessions.get(sessionId);
    if (!session) return { status: 404, body: { error: 'UNKNOWN_SESSION' } };
    if (session.state !== 'PENDING') return { status: 409, body: { error: 'REPLAY' } };
    session.state = 'COMPLETING';
    try {
      const result = await verifyResourceProof({
        store,
        challenge: session.challenge,
        signature: signature.toLowerCase(),
        credential: session.credential,
        resourceId: session.resource.resourceId,
        readSnapshot: () => readSnapshot(session.credential.name),
        now,
      });
      session.state = 'COMPLETE';
      return { status: 200, body: publicResult(result) };
    } catch {
      session.state = 'COMPLETE';
      return { status: 200, body: publicResult({ allowed: false, reason: 'RPC_UNAVAILABLE' }) };
    }
  }

  return { start, complete };
}

export function createPixelGateServer({ service }) {
  if (!service) throw Error('SERVICE_REQUIRED');
  return createServer(async (req, res) => {
    const address = res.socket.localAddress;
    const localPort = res.socket.localPort;
    const expectedHost = `127.0.0.1:${localPort}`;
    const send = (status, body) => {
      res.writeHead(status, {
        'Content-Type': 'application/json',
        'Cache-Control': 'no-store',
        'X-Content-Type-Options': 'nosniff',
      });
      res.end(JSON.stringify(body));
    };
    if (address !== '127.0.0.1' || req.headers.host !== expectedHost) {
      return send(403, { error: 'LOCAL_HOST_REQUIRED' });
    }
    if (req.method !== 'POST') return send(405, { error: 'METHOD_NOT_ALLOWED' });
    try {
      const body = await readJson(req);
      if (req.url === '/gate/sessions') {
        if (!exactObject(body, ['resource', 'credential']) ||
            typeof body.resource !== 'string' || typeof body.credential !== 'string') {
          return send(400, { error: 'MALFORMED_INPUT' });
        }
        return send(201, service.start(body));
      }
      const match = /^\/gate\/sessions\/([0-9a-f]{32})\/complete$/.exec(req.url ?? '');
      if (!match) return send(404, { error: 'NOT_FOUND' });
      if (!exactObject(body, ['signature']) || typeof body.signature !== 'string') {
        return send(400, { error: 'MALFORMED_INPUT' });
      }
      const result = await service.complete({ sessionId: match[1], signature: body.signature });
      return send(result.status, result.body);
    } catch (error) {
      const reason = ['UNKNOWN_RESOURCE', 'INVALID_CREDENTIAL_NAME', 'WRONG_NAMESPACE']
        .includes(error?.message) || error?.reason ? (error.reason ?? error.message) : 'MALFORMED_INPUT';
      return send(400, { error: reason });
    }
  });
}

function productionSnapshotReader() {
  const urls = [process.env.SEPOLIA_RPC_URL?.trim(), process.env.SEPOLIA_RPC_FALLBACK_URL?.trim()]
    .filter(Boolean);
  const clients = (urls.length ? urls : [undefined]).map(url => createPublicClient({
    chain: sepolia,
    transport: http(url, { retryCount: 0, timeout: 8_000 }),
  }));
  return async name => {
    let failure;
    for (const client of clients) {
      try { return await readResourceCredential(client, name); }
      catch (error) { failure = error; }
    }
    throw failure ?? Error('RPC_UNAVAILABLE');
  };
}

export async function main(args = process.argv.slice(2)) {
  let port = pixelGatePort;
  for (let index = 0; index < args.length; index++) {
    if (args[index] === '--port') port = Number(args[++index]);
    else throw Error('INVALID_ARGUMENT');
  }
  if (!Number.isInteger(port) || port < 1 || port > 65535) throw Error('INVALID_ARGUMENT');
  const service = createPixelGateService({ readSnapshot: productionSnapshotReader() });
  const server = createPixelGateServer({ service });
  await new Promise(resolve => server.listen(port, '127.0.0.1', resolve));
  console.log(`LockENS Pixel Gate bridge: http://127.0.0.1:${port}`);
  console.log('Pixel is untrusted transport; Node remains the authoritative verifier.');
  return server;
}

if (process.argv[1] && pathToFileURL(process.argv[1]).href === import.meta.url) {
  main().catch(() => { console.error('Pixel Gate bridge could not start.'); process.exitCode = 1; });
}
