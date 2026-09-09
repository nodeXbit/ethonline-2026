import { readFile } from 'node:fs/promises';
import { createServer } from 'node:http';
import { pathToFileURL } from 'node:url';
import { createPublicClient, http } from 'viem';
import { sepolia } from 'viem/chains';
import { readCredential, credentialLabel, parentName } from '../scripts/ensv2/access-record.mjs';
import { connect, requireCondition } from '../scripts/ensv2/contracts.mjs';
import {
  accessStateAchieved, publicErrorDetails, updateAccess,
} from '../scripts/ensv2/persistent-access.mjs';

export const demoHost = '127.0.0.1';
export const demoPort = 4173;
export const demoAuthority = `${demoHost}:${demoPort}`;
export const demoOrigin = `http://${demoAuthority}`;

const staticRoutes = new Map([
  ['/', { file: new URL('./public/index.html', import.meta.url), type: 'text/html; charset=utf-8' }],
  ['/index.html', { file: new URL('./public/index.html', import.meta.url), type: 'text/html; charset=utf-8' }],
  ['/styles.css', { file: new URL('./public/styles.css', import.meta.url), type: 'text/css; charset=utf-8' }],
  ['/app.js', { file: new URL('./public/app.js', import.meta.url), type: 'text/javascript; charset=utf-8' }],
  ['/access-action.js', { file: new URL('./public/access-action.js', import.meta.url),
    type: 'text/javascript; charset=utf-8' }],
]);

const statusNames = ['AVAILABLE', 'RESERVED', 'REGISTERED'];

const commonHeaders = {
  'Content-Security-Policy': "default-src 'self'; script-src 'self'; style-src 'self'; connect-src 'self'; img-src 'self' data:; object-src 'none'; base-uri 'none'; form-action 'none'; frame-ancestors 'none'",
  'X-Content-Type-Options': 'nosniff',
  'Referrer-Policy': 'no-referrer',
};

function sendResponse(response, status, body, headers = {}) {
  response.writeHead(status, { ...commonHeaders, ...headers });
  response.end(body);
}

function sendJson(response, status, value) {
  sendResponse(response, status, JSON.stringify(value), {
    'Content-Type': 'application/json; charset=utf-8',
    'Cache-Control': 'no-store',
  });
}

function redactUrls(value) {
  return String(value).split(/\r?\n/, 1)[0]
    .replace(/https?:\/\/\S+/gi, '[redacted URL]')
    .slice(0, 240);
}

export function safePublicError(error, {
  code = 'UNEXPECTED_ERROR', stage = 'request', message = 'The request could not be completed.',
} = {}) {
  const details = publicErrorDetails(error, stage);
  const result = { code, message: redactUrls(details.message || message), stage };
  if (/^0x[0-9a-fA-F]{64}$/.test(details.transactionHash ?? '')) {
    result.transactionHash = details.transactionHash;
  }
  return result;
}

export function publicCredential(snapshot) {
  const numericStatus = Number(snapshot.state.status);
  return {
    name: `${credentialLabel}.${parentName}`,
    status: statusNames[numericStatus] ?? `UNKNOWN(${numericStatus})`,
    owner: snapshot.owner,
    registryExpiry: snapshot.expiry.toString(),
    tokenId: snapshot.tokenId.toString(),
    resolver: snapshot.resolver,
    access: snapshot.access === null ? null : {
      active: snapshot.access.active,
      validUntil: snapshot.access.validUntil.toString(),
    },
    authorization: snapshot.authorized ? 'ALLOW' : 'DENY',
  };
}

function isWriteRoute(path) {
  return path === '/api/activate' || path === '/api/deactivate';
}

function validateWriteRequest(request) {
  if (request.headers.host !== demoAuthority || request.headers.origin !== demoOrigin) {
    return { status: 403, code: 'FORBIDDEN', message: 'Invalid local Host or Origin.' };
  }
  const contentLength = request.headers['content-length'];
  if (request.headers['transfer-encoding'] !== undefined ||
      (contentLength !== undefined && (!/^\d+$/.test(contentLength) || Number(contentLength) > 0))) {
    return { status: 400, code: 'BODY_NOT_ALLOWED', message: 'Request body is not allowed.' };
  }
  return null;
}

export function createDemoHandler({ readState, writeAccess, loadStatic = readFile }) {
  let writePending = false;

  return async function demoHandler(request, response) {
    const path = request.url;
    const staticRoute = staticRoutes.get(path);
    if (staticRoute) {
      if (request.method !== 'GET') {
        sendJson(response, 405, { error: { code: 'METHOD_NOT_ALLOWED',
          message: 'Only GET is allowed for this route.', stage: 'routing' } });
        return;
      }
      try {
        const body = await loadStatic(staticRoute.file);
        sendResponse(response, 200, body, { 'Content-Type': staticRoute.type,
          'Cache-Control': 'no-cache' });
      } catch {
        sendJson(response, 500, { error: { code: 'STATIC_READ_FAILED',
          message: 'The local UI asset could not be read.', stage: 'static asset' } });
      }
      return;
    }

    if (path === '/api/credential') {
      if (request.method !== 'GET') {
        sendJson(response, 405, { error: { code: 'METHOD_NOT_ALLOWED',
          message: 'Only GET is allowed for this route.', stage: 'routing' } });
        return;
      }
      try {
        sendJson(response, 200, publicCredential(await readState()));
      } catch (error) {
        sendJson(response, 502, { error: safePublicError(error, {
          code: 'READ_FAILED', stage: 'Sepolia credential read',
        }) });
      }
      return;
    }

    if (isWriteRoute(path)) {
      if (request.method !== 'POST') {
        sendJson(response, 405, { error: { code: 'METHOD_NOT_ALLOWED',
          message: 'Only POST is allowed for this route.', stage: 'routing' } });
        return;
      }
      const invalid = validateWriteRequest(request);
      if (invalid) {
        request.resume();
        sendJson(response, invalid.status, { error: {
          code: invalid.code, message: invalid.message, stage: 'request validation',
        } });
        return;
      }
      if (writePending) {
        sendJson(response, 409, { error: { code: 'WRITE_PENDING',
          message: 'Another Sepolia access update is still pending.', stage: 'write lock' } });
        return;
      }

      writePending = true;
      const action = path === '/api/activate' ? 'activate' : 'deactivate';
      try {
        const before = await readState();
        const result = accessStateAchieved(before, action)
          ? { changed: false, transactionHash: null, credential: before }
          : await writeAccess(action);
        sendJson(response, 200, {
          changed: result.changed ?? true,
          transactionHash: result.transactionHash,
          credential: publicCredential(result.credential),
        });
      } catch (error) {
        const conflict = /Credential must be REGISTERED|access\.v1 is not configured/.test(
          String(error?.message ?? ''));
        sendJson(response, conflict ? 409 : 500, { error: safePublicError(error, {
          code: conflict ? 'INCOMPATIBLE_STATE' : 'WRITE_FAILED', stage: 'Sepolia access update',
        }) });
      } finally {
        writePending = false;
      }
      return;
    }

    sendJson(response, 404, { error: { code: 'NOT_FOUND',
      message: 'Route not found.', stage: 'routing' } });
  };
}

export function createProductionDependencies() {
  let publicClient;
  async function client() {
    if (!publicClient) {
      publicClient = createPublicClient({ chain: sepolia,
        transport: http(process.env.SEPOLIA_RPC_URL?.trim() || undefined) });
      requireCondition(await publicClient.getChainId() === sepolia.id,
        'RPC must be Sepolia (11155111).');
    }
    return publicClient;
  }
  return {
    readState: async () => readCredential(await client()),
    writeAccess: async action => {
      const context = await connect();
      requireCondition(context.parent === parentName, 'This demo requires demo-access.eth.');
      return updateAccess(context, action);
    },
  };
}

export async function startDemoServer() {
  const server = createServer(createDemoHandler(createProductionDependencies()));
  await new Promise((resolve, reject) => {
    server.once('error', reject);
    server.listen(demoPort, demoHost, resolve);
  });
  console.log(`ENSv2 credential demo: ${demoOrigin}`);
  return server;
}

if (process.argv[1] && pathToFileURL(process.argv[1]).href === import.meta.url) {
  startDemoServer().catch(error => {
    console.error('Demo server failed to start', safePublicError(error, {
      code: 'START_FAILED', stage: 'server startup',
    }));
    process.exitCode = 1;
  });
}
