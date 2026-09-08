import { pathToFileURL } from 'node:url';
import { createPublicClient, http, isAddressEqual, zeroAddress } from 'viem';
import { normalize } from 'viem/ens';
import { sepolia } from 'viem/chains';
import {
  ETHRegistry,
  REGISTERED,
  registryAbi,
  requireCondition,
} from '../ensv2/contracts.mjs';

const expectedRegistry = '0x2d249472B83A453086254Acd8a42913D8e45a2Fd';
const credentialLabel = 'cred-001';
const serialBaud = 115200;
const detectionPattern = /^ISO14443A tag detected; UID=(?:(?<uid4>(?:[0-9A-Fa-f]{2}:){3}[0-9A-Fa-f]{2}) \(4 bytes\)|(?<uid7>(?:[0-9A-Fa-f]{2}:){6}[0-9A-Fa-f]{2}) \(7 bytes\))$/;

export function normalizeUid(value) {
  const compact = value.trim().replace(/[:\s-]/g, '');
  requireCondition(/^(?:[0-9A-Fa-f]{8}|[0-9A-Fa-f]{14})$/.test(compact),
    'NFC UID must contain exactly 4 or 7 hexadecimal bytes.');
  return compact.toUpperCase().match(/.{2}/g).join(':');
}

export function parseDetectionLine(line) {
  const match = detectionPattern.exec(line.replace(/\r$/, ''));
  if (!match) return null;
  return normalizeUid(match.groups.uid4 ?? match.groups.uid7);
}

function parentDetails(rawName) {
  requireCondition(Boolean(rawName?.trim()), 'ENS_PARENT_NAME is required.');
  const parent = normalize(rawName.trim());
  const parts = parent.split('.');
  requireCondition(parts.length === 2 && parts[0] && parts[1] === 'eth',
    'ENS_PARENT_NAME must be a direct .eth second-level name.');
  return { parent, label: parts[0] };
}

async function closePort(port) {
  if (!port?.isOpen) return;
  await new Promise((resolve, reject) => {
    port.close(error => error ? reject(error) : resolve());
  });
}

async function waitForUid(portPath) {
  const { SerialPort } = await import('serialport');
  const port = new SerialPort({ path: portPath, baudRate: serialBaud });
  const uid = await new Promise((resolve, reject) => {
    let buffer = '';
    const fail = error => {
      port.off('data', receive);
      closePort(port).then(() => reject(error), () => reject(error));
    };
    const receive = chunk => {
      buffer += chunk.toString('utf8');
      let newline = buffer.indexOf('\n');
      while (newline !== -1) {
        const line = buffer.slice(0, newline);
        buffer = buffer.slice(newline + 1);
        const parsed = parseDetectionLine(line);
        if (parsed !== null) {
          port.off('error', fail);
          port.off('data', receive);
          resolve(parsed);
          return;
        }
        newline = buffer.indexOf('\n');
      }
    };
    port.once('error', fail);
    port.on('data', receive);
  });
  return { port, uid };
}

function monitorSerialErrors(port) {
  let fail;
  const promise = new Promise((_, reject) => {
    fail = reject;
    port.once('error', fail);
  });
  return { promise, stop: () => port.off('error', fail) };
}

async function readAuthorization(parentLabel) {
  const publicClient = createPublicClient({
    chain: sepolia,
    transport: http(process.env.SEPOLIA_RPC_URL?.trim() || undefined),
  });
  const chainId = await publicClient.getChainId();
  requireCondition(chainId === 11155111, 'RPC must be Sepolia (11155111).');

  const block = await publicClient.getBlock();
  const read = (address, functionName, args) => publicClient.readContract({
    address,
    abi: registryAbi,
    functionName,
    args,
    blockNumber: block.number,
  });
  const parentTokenId = await read(ETHRegistry, 'findTokenId', [parentLabel]);
  const [parentOwner, registry] = await Promise.all([
    read(ETHRegistry, 'getOwner', [parentTokenId]),
    read(ETHRegistry, 'getSubregistry', [parentLabel]),
  ]);
  requireCondition(!isAddressEqual(parentOwner, zeroAddress), 'Parent owner is the zero address.');
  requireCondition(isAddressEqual(registry, expectedRegistry),
    'Parent UserRegistry does not match the verified deployment.');
  const code = await publicClient.getCode({ address: registry, blockNumber: block.number });
  requireCondition(Boolean(code && code !== '0x'), 'UserRegistry bytecode is absent.');

  const tokenId = await read(registry, 'findTokenId', [credentialLabel]);
  const [state, owner, expiry] = await Promise.all([
    read(registry, 'getState', [tokenId]),
    read(registry, 'getOwner', [tokenId]),
    read(registry, 'getExpiry', [tokenId]),
  ]);
  requireCondition(state.tokenId === tokenId && state.expiry === expiry,
    'Inconsistent credential state reads.');

  return {
    block,
    registry,
    tokenId,
    state,
    owner,
    expiry,
    authorized: state.status === REGISTERED &&
      isAddressEqual(owner, parentOwner) && expiry > block.timestamp,
  };
}

let stage = 'configuration';
async function main() {
  const serialPort = process.env.NFC_SERIAL_PORT?.trim();
  requireCondition(Boolean(serialPort), 'NFC_SERIAL_PORT is required.');
  const configuredUid = normalizeUid(process.env.NFC_DEMO_UID ?? '');
  const { parent, label: parentLabel } = parentDetails(process.env.ENS_PARENT_NAME);

  stage = 'serial input';
  let port;
  try {
    const detected = await waitForUid(serialPort);
    port = detected.port;
    const serialErrors = monitorSerialErrors(port);
    try {
      console.log(`NFC UID: ${detected.uid}`);

      if (detected.uid !== configuredUid) {
        stage = 'serial close';
        await Promise.race([closePort(port), serialErrors.promise]);
        port = undefined;
        console.log('AUTHORIZATION: DENY');
        return;
      }

      console.log(`CREDENTIAL: ${credentialLabel}.${parent}`);
      stage = 'Sepolia authorization read';
      const result = await Promise.race([
        readAuthorization(parentLabel),
        serialErrors.promise,
      ]);
      console.log({
        blockNumber: result.block.number.toString(),
        registry: result.registry,
        tokenId: result.tokenId.toString(),
        status: result.state.status,
        owner: result.owner,
        expiry: result.expiry.toString(),
      });
      stage = 'serial close';
      await Promise.race([closePort(port), serialErrors.promise]);
      port = undefined;
      console.log(`AUTHORIZATION: ${result.authorized ? 'ALLOW' : 'DENY'}`);
    } finally {
      serialErrors.stop();
    }
  } finally {
    await closePort(port);
  }
}

const isDirectRun = process.argv[1] && pathToFileURL(process.argv[1]).href === import.meta.url;
if (isDirectRun) {
  main().catch(() => {
    console.error(`NFC bridge stopped during ${stage}. No authorization decision was produced.`);
    process.exitCode = 1;
  });
}
