import { readFile } from 'node:fs/promises';
import { pathToFileURL } from 'node:url';
import { createPublicClient, http, isAddress, isAddressEqual, namehash, zeroAddress } from 'viem';
import { sepolia } from 'viem/chains';
import {
  CHAIN_ID,
  DEV,
  ETH_REGISTRY,
  FACTORY,
  ISSUER,
  KEYS_OWNER_ROLES,
  PERMISSIONED_RESOLVER_IMPL,
  PINNED_ENSV2_COMMIT,
  R0,
  R1_ROOT_ROLES,
  REGISTERED,
  S0,
  S1_ROOT_ROLES,
  USER_REGISTRY_IMPL,
  decodeAccess,
  factoryAbi,
  registryAbi,
  resolverAbi,
} from './issuer-bootstrap-plan.mjs';

const CONFIG_URL = new URL('../../config/issuer-space.json', import.meta.url);
const EXPECTED_NAMESPACE = 'keys.demo-access.eth';
const EXPECTED_EXPIRY = 1814392799n;
const EXPECTED_R1 = '0x0AeB395be893149c0b60D9DAC3Ba55139D0dcA1a';
const EXPECTED_S1 = '0x20766FB21498a99350F922ee3163F5a95F354a7f';
const EXPECTED_TX1 = '0x632c6d6f9c04a381f763013bf304d30c15bcccc440b41df9350829cf6e952253';
const EXPECTED_TX2 = '0x2e6e8f830527df1aa384fe09c0a51f27c8e8d7e2b0b3862a9c646aa29d38d94c';
const EXPECTED_TX3 = '0x5997d2a1ccbaf146de00dbddef79c49f00fedefd02fdb2399386910a1840230b';
const GUEST_OWNER = '0x3419148731087b970d2059C53780163B452D5FF7';
const MAX_BLOCK_AGE = 120n;
const MAX_FUTURE_SKEW = 15n;

function requireCondition(condition, message) {
  if (!condition) throw new Error(message);
}

function sameAddress(actual, expected, label) {
  requireCondition(isAddress(actual) && isAddressEqual(actual, expected), `${label} mismatch.`);
}

function roleHex(value) {
  return `0x${value.toString(16)}`;
}

export async function loadIssuerSpaceConfig(url = CONFIG_URL) {
  return JSON.parse(await readFile(url, 'utf8'));
}

export function validateIssuerSpaceConfig(config) {
  requireCondition(config && typeof config === 'object' && !Array.isArray(config),
    'Issuer space config must be an object.');
  requireCondition(config.version === 1, 'Config version mismatch.');
  requireCondition(config.chainId === CHAIN_ID && config.network === 'sepolia',
    'Config chain mismatch.');
  requireCondition(config.namespace === EXPECTED_NAMESPACE &&
    config.parentNamespace === 'demo-access.eth', 'Config namespace mismatch.');
  sameAddress(config.issuerAddress, ISSUER, 'Issuer');
  sameAddress(config.ethRegistry, ETH_REGISTRY, 'ETHRegistry');
  sameAddress(config.parentRegistry, R0, 'Parent registry');
  sameAddress(config.issuerRegistry, EXPECTED_R1, 'Issuer registry');
  sameAddress(config.issuerResolver, EXPECTED_S1, 'Issuer resolver');
  sameAddress(config.verifiableFactory, FACTORY, 'Factory');
  sameAddress(config.userRegistryImplementation, USER_REGISTRY_IMPL,
    'UserRegistry implementation');
  sameAddress(config.permissionedResolverImplementation, PERMISSIONED_RESOLVER_IMPL,
    'PermissionedResolver implementation');
  requireCondition(config.namespaceExpiry === Number(EXPECTED_EXPIRY), 'Namespace expiry mismatch.');
  requireCondition(config.roles?.issuerRegistryRoot === roleHex(R1_ROOT_ROLES) &&
    config.roles?.issuerResolverRoot === roleHex(S1_ROOT_ROLES) &&
    config.roles?.namespaceOwner === roleHex(KEYS_OWNER_ROLES), 'Role bitmap mismatch.');
  requireCondition(config.bootstrap?.deployerAddress &&
    isAddressEqual(config.bootstrap.deployerAddress, DEV), 'Bootstrap deployer mismatch.');
  requireCondition(config.bootstrap.r1DeploymentTx === EXPECTED_TX1 &&
    config.bootstrap.s1DeploymentTx === EXPECTED_TX2 &&
    config.bootstrap.namespaceRegistrationTx === EXPECTED_TX3,
  'Bootstrap transaction evidence mismatch.');
  requireCondition(config.bootstrap.r1DeploymentBlock === 11683691 &&
    config.bootstrap.s1DeploymentBlock === 11683692 &&
    config.bootstrap.namespaceRegistrationBlock === 11683693,
  'Bootstrap block evidence mismatch.');
  requireCondition(config.source?.ensContractsV2Commit === PINNED_ENSV2_COMMIT,
    'Pinned ENSv2 source mismatch.');
  return config;
}

async function readEntry(client, address, label, blockNumber) {
  const read = (functionName, args) => client.readContract({
    address, abi: registryAbi, functionName, args, blockNumber,
  });
  const tokenId = await read('findTokenId', [label]);
  const [state, owner, expiry, subregistry, resolver] = await Promise.all([
    read('getState', [tokenId]),
    read('getOwner', [tokenId]),
    read('getExpiry', [tokenId]),
    read('getSubregistry', [label]),
    read('getResolver', [label]),
  ]);
  requireCondition(state.tokenId === tokenId && state.expiry === expiry,
    `Inconsistent ${label} reads.`);
  return { tokenId, state, owner, expiry, subregistry, resolver };
}

async function noncePair(client, address) {
  const [latest, pending] = await Promise.all([
    client.getTransactionCount({ address, blockTag: 'latest' }),
    client.getTransactionCount({ address, blockTag: 'pending' }),
  ]);
  return { latest, pending };
}

async function legacyEntry(client, label, blockNumber) {
  const entry = await readEntry(client, R0, label, blockNumber);
  const rawAccess = await client.readContract({
    address: S0,
    abi: resolverAbi,
    functionName: 'data',
    args: [namehash(`${label}.demo-access.eth`), 'access.v1'],
    blockNumber,
  });
  return { ...entry, access: decodeAccess(rawAccess) };
}

export async function verifyIssuerSpace(client, config, now = BigInt(Math.floor(Date.now() / 1000))) {
  validateIssuerSpaceConfig(config);
  requireCondition(await client.getChainId() === config.chainId, 'RPC chain mismatch.');
  const block = await client.getBlock({ blockTag: 'latest' });
  requireCondition(block.number !== null && block.timestamp <= now + MAX_FUTURE_SKEW &&
    now - block.timestamp <= MAX_BLOCK_AGE, 'Latest block is not fresh.');
  const [r1Code, s1Code, r1Implementation, s1Implementation, s0Implementation,
    r1IssuerRoles, r1DevRoles, s1IssuerRoles, s1DevRoles, keys, parent, guest, cred,
    devNonce, issuerNonce] = await Promise.all([
    client.getCode({ address: config.issuerRegistry, blockNumber: block.number }),
    client.getCode({ address: config.issuerResolver, blockNumber: block.number }),
    client.readContract({ address: FACTORY, abi: factoryAbi, functionName: 'verifyContract',
      args: [config.issuerRegistry], blockNumber: block.number }),
    client.readContract({ address: FACTORY, abi: factoryAbi, functionName: 'verifyContract',
      args: [config.issuerResolver], blockNumber: block.number }),
    client.readContract({ address: FACTORY, abi: factoryAbi, functionName: 'verifyContract',
      args: [S0], blockNumber: block.number }),
    client.readContract({ address: config.issuerRegistry, abi: resolverAbi, functionName: 'roles',
      args: [0n, ISSUER], blockNumber: block.number }),
    client.readContract({ address: config.issuerRegistry, abi: resolverAbi, functionName: 'roles',
      args: [0n, DEV], blockNumber: block.number }),
    client.readContract({ address: config.issuerResolver, abi: resolverAbi, functionName: 'roles',
      args: [0n, ISSUER], blockNumber: block.number }),
    client.readContract({ address: config.issuerResolver, abi: resolverAbi, functionName: 'roles',
      args: [0n, DEV], blockNumber: block.number }),
    readEntry(client, R0, 'keys', block.number),
    readEntry(client, ETH_REGISTRY, 'demo-access', block.number),
    legacyEntry(client, 'guest-001', block.number),
    legacyEntry(client, 'cred-001', block.number),
    noncePair(client, DEV),
    noncePair(client, ISSUER),
  ]);
  requireCondition(Boolean(r1Code && r1Code !== '0x') && Boolean(s1Code && s1Code !== '0x'),
    'Issuer proxy code is absent.');
  sameAddress(r1Implementation, USER_REGISTRY_IMPL, 'R1 provenance');
  sameAddress(s1Implementation, PERMISSIONED_RESOLVER_IMPL, 'S1 provenance');
  sameAddress(s0Implementation, PERMISSIONED_RESOLVER_IMPL, 'S0 provenance');
  requireCondition(r1IssuerRoles === R1_ROOT_ROLES && r1DevRoles === 0n,
    'R1 root roles mismatch.');
  requireCondition(s1IssuerRoles === S1_ROOT_ROLES && s1DevRoles === 0n,
    'S1 root roles mismatch.');
  requireCondition(keys.state.status === REGISTERED &&
    isAddressEqual(keys.owner, ISSUER) &&
    isAddressEqual(keys.subregistry, config.issuerRegistry) &&
    isAddressEqual(keys.resolver, config.issuerResolver) &&
    keys.expiry === EXPECTED_EXPIRY, 'keys registration mismatch.');
  const keysRoles = await client.readContract({
    address: R0, abi: registryAbi, functionName: 'roles',
    args: [keys.tokenId, ISSUER], blockNumber: block.number,
  });
  requireCondition(keysRoles === KEYS_OWNER_ROLES, 'keys owner roles mismatch.');
  requireCondition(parent.state.status === REGISTERED &&
    isAddressEqual(parent.subregistry, R0), 'demo-access.eth hierarchy changed.');
  requireCondition(guest.state.status === REGISTERED && isAddressEqual(guest.owner, GUEST_OWNER) &&
    isAddressEqual(guest.resolver, S0) && isAddressEqual(guest.subregistry, zeroAddress) &&
    guest.expiry === 1820557476n && guest.access?.active === true &&
    guest.access.validUntil === 1793487599n, 'guest-001 changed.');
  requireCondition(cred.state.status === REGISTERED && isAddressEqual(cred.owner, DEV) &&
    isAddressEqual(cred.resolver, S0) && isAddressEqual(cred.subregistry, zeroAddress) &&
    cred.expiry === 1820447664n && cred.access?.active === false &&
    cred.access.validUntil === 1788998040n, 'cred-001 changed.');
  requireCondition(devNonce.latest === devNonce.pending, 'DEV has a pending transaction.');
  requireCondition(issuerNonce.latest === 1 && issuerNonce.pending === 1,
    'Issuer nonce changed or has a pending transaction.');
  return { block, keys, keysRoles, r1Implementation, s1Implementation,
    r1IssuerRoles, r1DevRoles, s1IssuerRoles, s1DevRoles, devNonce, issuerNonce };
}

async function main() {
  let stage = 'configuration';
  try {
    const config = validateIssuerSpaceConfig(await loadIssuerSpaceConfig());
    const client = createPublicClient({ chain: sepolia,
      transport: http(process.env.SEPOLIA_RPC_URL?.trim() || undefined) });
    stage = 'fresh read-only onchain verification';
    const result = await verifyIssuerSpace(client, config);
    console.log(JSON.stringify({ status: 'PASS', chainId: config.chainId,
      block: result.block.number.toString(), namespace: config.namespace,
      issuerRegistry: config.issuerRegistry, issuerResolver: config.issuerResolver,
      owner: result.keys.owner, expiry: result.keys.expiry.toString(),
      ownerRoles: result.keysRoles.toString(), devNonce: result.devNonce,
      issuerNonce: result.issuerNonce }));
  } catch (_error) {
    console.error(`Issuer space verification stopped during ${stage}. No transaction was sent.`);
    process.exitCode = 1;
  }
}

const invokedPath = process.argv[1] ? pathToFileURL(process.argv[1]).href : '';
if (import.meta.url === invokedPath) await main();
