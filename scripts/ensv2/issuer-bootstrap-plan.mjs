import { pathToFileURL } from 'node:url';
import {
  concatHex,
  createPublicClient,
  decodeAbiParameters,
  decodeFunctionData,
  encodeAbiParameters,
  encodeFunctionData,
  formatEther,
  getCreate2Address,
  http,
  isAddressEqual,
  keccak256,
  namehash,
  parseAbi,
  stringToHex,
  toHex,
  zeroAddress,
} from 'viem';
import { sepolia } from 'viem/chains';

export const CHAIN_ID = 11155111;
export const PINNED_ENSV2_COMMIT = '97a57293f3b4279d94b571e678edb53ce62638f4';
export const PARENT_NAME = 'demo-access.eth';
export const PARENT_LABEL = 'demo-access';
export const KEYS_LABEL = 'keys';
export const DEV = '0x4C60a5AD311510543B56d0408872A52e4AEEe19C';
export const ISSUER = '0xFa90e8301A22833B74378C5fA3a7c120Ac512685';
export const ETH_REGISTRY = '0xbdc85dd5b15d7ecb354cd7cb6f2c50b4f2c4f0e2';
export const R0 = '0x2d249472B83A453086254Acd8a42913D8e45a2Fd';
export const S0 = '0x0B723c0C2170F2ea508F2f4e3e122C6c0782744C';
export const FACTORY = '0x10dc6333cdfe1fcef624c6e0a8221b91804cd7ef';
export const USER_REGISTRY_IMPL = '0x624a25d67b59d587752ebec8dded8827dae52050';
export const PERMISSIONED_RESOLVER_IMPL = '0x9eae5c2730a7dd16bdd1dee6421a1b91e3b0365e';

export const AVAILABLE = 0;
export const REGISTERED = 2;
export const ROLE_REGISTRAR = 1n << 0n;
export const ROLE_RENEW = 1n << 16n;
export const ROLE_REGISTRAR_ADMIN = ROLE_REGISTRAR << 128n;
export const ROLE_RENEW_ADMIN = ROLE_RENEW << 128n;
export const ROLE_SET_TEXT = 1n << 4n;
export const ROLE_SET_DATA = 1n << 36n;
export const ROLE_SET_TEXT_ADMIN = ROLE_SET_TEXT << 128n;
export const ROLE_SET_DATA_ADMIN = ROLE_SET_DATA << 128n;
export const R1_ROOT_ROLES = ROLE_REGISTRAR | ROLE_REGISTRAR_ADMIN |
  ROLE_RENEW | ROLE_RENEW_ADMIN;
export const S1_ROOT_ROLES = ROLE_SET_TEXT | ROLE_SET_TEXT_ADMIN |
  ROLE_SET_DATA | ROLE_SET_DATA_ADMIN;
export const KEYS_OWNER_ROLES = ROLE_RENEW;

export const CREDENTIAL_HORIZON = BigInt(Date.parse('2026-10-31T22:59:59Z') / 1000);
export const RECOMMENDED_KEYS_EXPIRY = BigInt(Date.parse('2027-06-30T21:59:59Z') / 1000);
const MIN_PARENT_MARGIN = 30n * 24n * 60n * 60n;
const MAX_BLOCK_AGE = 120n;
const MAX_FUTURE_SKEW = 15n;

export const registryAbi = parseAbi([
  'function findTokenId(string label) view returns (uint256)',
  'function getOwner(uint256 anyId) view returns (address)',
  'function getSubregistry(string label) view returns (address)',
  'function getResolver(string label) view returns (address)',
  'function getState(uint256 anyId) view returns ((uint8 status, uint64 expiry, address latestOwner, uint256 tokenId, uint256 resource) state)',
  'function getExpiry(uint256 anyId) view returns (uint64)',
  'function getParent() view returns (address parent, string label)',
  'function roles(uint256 anyId, address account) view returns (uint256)',
  'function roleCount(uint256 anyId) view returns (uint256)',
  'function hasRoles(uint256 anyId, uint256 roleBitmap, address account) view returns (bool)',
  'function register(string label, address owner, address registry, address resolver, uint256 roleBitmap, uint64 expiry) returns (uint256)',
  'function renew(uint256 anyId, uint64 newExpiry)',
]);
export const userRegistryAbi = parseAbi([
  'function initialize(address rootAccount, uint256 roleBitmap)',
]);
export const resolverAbi = parseAbi([
  'function initialize(address admin, uint256 roleBitmap, bytes[] setters)',
  'function data(bytes32 node, string key) view returns (bytes)',
  'function roles(uint256 resource, address account) view returns (uint256)',
]);
export const factoryAbi = parseAbi([
  'function deployProxy(address implementation, uint256 salt, bytes data) returns (address proxy)',
  'function verifyContract(address proxy) view returns (address implementation)',
  'function proxyLogic() view returns (address)',
]);

const ACCESS_SCHEMA = [{ type: 'bool' }, { type: 'uint64' }];
const roleNames = Object.freeze({
  R1: ['ROLE_REGISTRAR', 'ROLE_REGISTRAR_ADMIN', 'ROLE_RENEW', 'ROLE_RENEW_ADMIN'],
  S1: ['ROLE_SET_TEXT', 'ROLE_SET_TEXT_ADMIN', 'ROLE_SET_DATA', 'ROLE_SET_DATA_ADMIN'],
  keys: ['ROLE_RENEW'],
});
const saltDomains = Object.freeze({
  r1: `ethonline-2026:sepolia:${CHAIN_ID}:${PARENT_NAME}:${KEYS_LABEL}:r1:${ISSUER.toLowerCase()}`,
  s1: `ethonline-2026:sepolia:${CHAIN_ID}:${PARENT_NAME}:${KEYS_LABEL}:s1:${ISSUER.toLowerCase()}`,
});

export function requireCondition(condition, message) {
  if (!condition) throw new Error(message);
}

export function bootstrapSalt(kind) {
  requireCondition(Object.hasOwn(saltDomains, kind), 'Salt kind must be r1 or s1.');
  return BigInt(keccak256(stringToHex(saltDomains[kind])));
}

export function predictProxy(deployer, salt, proxyLogic) {
  const outerSalt = keccak256(encodeAbiParameters(
    [{ type: 'address' }, { type: 'uint256' }], [deployer, salt],
  ));
  // Exact pinned CloneProxyBytecode.creationCode: 10-byte creation stub,
  // 45-byte EIP-1167 runtime, then the 32-byte derived salt.
  const creationCode = concatHex([
    '0x3d604d80600a3d3981f3363d3d373d3d3d363d73',
    proxyLogic,
    '0x5af43d82803e903d91602b57fd5bf3',
    outerSalt,
  ]);
  return {
    outerSalt,
    creationCode,
    address: getCreate2Address({ from: FACTORY, salt: outerSalt, bytecode: creationCode }),
  };
}

export function buildTransactions({ proxyLogic, expiry = RECOMMENDED_KEYS_EXPIRY } = {}) {
  requireCondition(Boolean(proxyLogic), 'Factory proxy logic is required.');
  const saltR1 = bootstrapSalt('r1');
  const saltS1 = bootstrapSalt('s1');
  const r1 = predictProxy(DEV, saltR1, proxyLogic);
  const s1 = predictProxy(DEV, saltS1, proxyLogic);
  const r1Initializer = encodeFunctionData({
    abi: userRegistryAbi,
    functionName: 'initialize',
    args: [ISSUER, R1_ROOT_ROLES],
  });
  const s1Initializer = encodeFunctionData({
    abi: resolverAbi,
    functionName: 'initialize',
    args: [ISSUER, S1_ROOT_ROLES, []],
  });
  const tx1 = encodeFunctionData({
    abi: factoryAbi,
    functionName: 'deployProxy',
    args: [USER_REGISTRY_IMPL, saltR1, r1Initializer],
  });
  const tx2 = encodeFunctionData({
    abi: factoryAbi,
    functionName: 'deployProxy',
    args: [PERMISSIONED_RESOLVER_IMPL, saltS1, s1Initializer],
  });
  const tx3 = encodeFunctionData({
    abi: registryAbi,
    functionName: 'register',
    args: [KEYS_LABEL, ISSUER, r1.address, s1.address, KEYS_OWNER_ROLES, expiry],
  });
  return {
    expiry,
    saltR1,
    saltS1,
    r1,
    s1,
    r1Initializer,
    s1Initializer,
    tx1,
    tx2,
    tx3,
  };
}

export function assertCalldataRoundTrip(plan) {
  const initR1 = decodeFunctionData({ abi: userRegistryAbi, data: plan.r1Initializer });
  const initS1 = decodeFunctionData({ abi: resolverAbi, data: plan.s1Initializer });
  const tx1 = decodeFunctionData({ abi: factoryAbi, data: plan.tx1 });
  const tx2 = decodeFunctionData({ abi: factoryAbi, data: plan.tx2 });
  const tx3 = decodeFunctionData({ abi: registryAbi, data: plan.tx3 });
  requireCondition(initR1.functionName === 'initialize' &&
    isAddressEqual(initR1.args[0], ISSUER) && initR1.args[1] === R1_ROOT_ROLES,
  'R1 initializer round-trip mismatch.');
  requireCondition(initS1.functionName === 'initialize' &&
    isAddressEqual(initS1.args[0], ISSUER) && initS1.args[1] === S1_ROOT_ROLES &&
    initS1.args[2].length === 0, 'S1 initializer round-trip mismatch.');
  requireCondition(tx1.functionName === 'deployProxy' &&
    isAddressEqual(tx1.args[0], USER_REGISTRY_IMPL) && tx1.args[1] === plan.saltR1 &&
    tx1.args[2] === plan.r1Initializer, 'TX1 calldata round-trip mismatch.');
  requireCondition(tx2.functionName === 'deployProxy' &&
    isAddressEqual(tx2.args[0], PERMISSIONED_RESOLVER_IMPL) && tx2.args[1] === plan.saltS1 &&
    tx2.args[2] === plan.s1Initializer, 'TX2 calldata round-trip mismatch.');
  requireCondition(tx3.functionName === 'register' && tx3.args[0] === KEYS_LABEL &&
    isAddressEqual(tx3.args[1], ISSUER) && isAddressEqual(tx3.args[2], plan.r1.address) &&
    isAddressEqual(tx3.args[3], plan.s1.address) && tx3.args[4] === KEYS_OWNER_ROLES &&
    tx3.args[5] === plan.expiry, 'TX3 calldata round-trip mismatch.');
  return true;
}

export function assertKeysAvailable(snapshot) {
  requireCondition(snapshot.state.status === AVAILABLE, 'keys is not AVAILABLE.');
  requireCondition(snapshot.state.expiry === 0n, 'keys has stale expiry state.');
  requireCondition(isAddressEqual(snapshot.state.latestOwner, zeroAddress),
    'keys has stale/current owner state.');
  requireCondition(isAddressEqual(snapshot.owner, zeroAddress), 'keys owner is not zero.');
  requireCondition(isAddressEqual(snapshot.subregistry, zeroAddress), 'keys subregistry is not zero.');
  requireCondition(isAddressEqual(snapshot.resolver, zeroAddress), 'keys resolver is not zero.');
  requireCondition(snapshot.roleCount === 0n, 'keys has stale role assignments.');
  return true;
}

export function expiryPlan(ancestorExpiries, recommended = RECOMMENDED_KEYS_EXPIRY) {
  requireCondition(Array.isArray(ancestorExpiries) && ancestorExpiries.length > 0,
    'At least one ancestor expiry is required.');
  const finite = ancestorExpiries.filter(expiry => expiry > 0n);
  requireCondition(finite.length > 0, 'No finite ancestor expiry is available.');
  const maximum = finite.reduce((a, b) => a < b ? a : b);
  requireCondition(recommended > CREDENTIAL_HORIZON,
    'Recommended keys expiry does not cover the credential horizon.');
  requireCondition(recommended < maximum, 'Recommended keys expiry is not below its ancestors.');
  const margin = maximum - recommended;
  requireCondition(margin >= MIN_PARENT_MARGIN, 'Recommended keys expiry lacks safety margin.');
  return { maximum, recommended, margin };
}

export function decodeAccess(value) {
  if (value === '0x') return null;
  requireCondition(/^0x[0-9a-fA-F]{128}$/.test(value), 'Malformed access.v1 data.');
  const [active, validUntil] = decodeAbiParameters(ACCESS_SCHEMA, value);
  requireCondition(encodeAbiParameters(ACCESS_SCHEMA, [active, validUntil]) === value.toLowerCase(),
    'Noncanonical access.v1 data.');
  return { active, validUntil };
}

function asHex(value) {
  return `0x${value.toString(16)}`;
}

function asBytes32(value) {
  return toHex(value, { size: 32 });
}

function jsonValue(value) {
  if (typeof value === 'bigint') return value.toString();
  return value;
}

function iso(seconds) {
  return new Date(Number(seconds) * 1000).toISOString();
}

function madrid(seconds) {
  return new Intl.DateTimeFormat('en-GB', {
    timeZone: 'Europe/Madrid', dateStyle: 'full', timeStyle: 'long', hourCycle: 'h23',
  }).format(new Date(Number(seconds) * 1000));
}

async function readEntry(client, address, label, blockNumber) {
  const read = (functionName, args) => client.readContract({
    address, abi: registryAbi, functionName, args, blockNumber,
  });
  const tokenId = await read('findTokenId', [label]);
  const [state, owner, expiry, subregistry, resolver, roleCount] = await Promise.all([
    read('getState', [tokenId]),
    read('getOwner', [tokenId]),
    read('getExpiry', [tokenId]),
    read('getSubregistry', [label]),
    read('getResolver', [label]),
    read('roleCount', [tokenId]),
  ]);
  requireCondition(state.tokenId === tokenId && state.expiry === expiry,
    `Inconsistent ${label} registry reads.`);
  return { tokenId, state, owner, expiry, subregistry, resolver, roleCount };
}

async function readLegacy(client, label, blockNumber) {
  const entry = await readEntry(client, R0, label, blockNumber);
  let access = null;
  if (isAddressEqual(entry.resolver, S0)) {
    const value = await client.readContract({
      address: S0,
      abi: resolverAbi,
      functionName: 'data',
      args: [namehash(`${label}.${PARENT_NAME}`), 'access.v1'],
      blockNumber,
    });
    access = decodeAccess(value);
  }
  return { ...entry, access };
}

async function requireCode(client, address, blockNumber, label) {
  const code = await client.getCode({ address, blockNumber });
  requireCondition(Boolean(code && code !== '0x'), `${label} code is absent.`);
  return (code.length - 2) / 2;
}

async function main() {
  let stage = 'configuration';
  try {
    const client = createPublicClient({
      chain: sepolia,
      transport: http(process.env.SEPOLIA_RPC_URL?.trim() || undefined),
    });
    stage = 'fresh Sepolia baseline';
    requireCondition(await client.getChainId() === CHAIN_ID, 'RPC is not Sepolia.');
    const block = await client.getBlock({ blockTag: 'latest' });
    requireCondition(block.number !== null, 'Latest block is not numbered.');
    const now = BigInt(Math.floor(Date.now() / 1000));
    requireCondition(block.timestamp <= now + MAX_FUTURE_SKEW && now - block.timestamp <= MAX_BLOCK_AGE,
      'Latest block is not fresh.');
    const [devLatest, devPending, issuerLatest, issuerPending, issuerBalance] = await Promise.all([
      client.getTransactionCount({ address: DEV, blockTag: 'latest' }),
      client.getTransactionCount({ address: DEV, blockTag: 'pending' }),
      client.getTransactionCount({ address: ISSUER, blockTag: 'latest' }),
      client.getTransactionCount({ address: ISSUER, blockTag: 'pending' }),
      client.getBalance({ address: ISSUER, blockNumber: block.number }),
    ]);
    requireCondition(devLatest === devPending, 'DEV has a pending transaction.');
    requireCondition(issuerLatest === 1 && issuerPending === 1,
      'Issuer latest/pending nonce is not exactly 1/1.');

    stage = 'contract code and provenance';
    const addresses = {
      ETHRegistry: ETH_REGISTRY,
      R0,
      S0,
      VerifiableFactory: FACTORY,
      UserRegistryImpl: USER_REGISTRY_IMPL,
      PermissionedResolverImpl: PERMISSIONED_RESOLVER_IMPL,
    };
    const codeSizes = Object.fromEntries(await Promise.all(Object.entries(addresses).map(
      async ([label, address]) => [label, await requireCode(client, address, block.number, label)],
    )));
    const [r0Implementation, s0Implementation, proxyLogic] = await Promise.all([
      client.readContract({ address: FACTORY, abi: factoryAbi, functionName: 'verifyContract',
        args: [R0], blockNumber: block.number }),
      client.readContract({ address: FACTORY, abi: factoryAbi, functionName: 'verifyContract',
        args: [S0], blockNumber: block.number }),
      client.readContract({ address: FACTORY, abi: factoryAbi, functionName: 'proxyLogic',
        blockNumber: block.number }),
    ]);
    requireCondition(isAddressEqual(r0Implementation, USER_REGISTRY_IMPL),
      'R0 provenance/implementation mismatch.');
    requireCondition(isAddressEqual(s0Implementation, PERMISSIONED_RESOLVER_IMPL),
      'S0 provenance/implementation mismatch.');
    codeSizes.FactoryProxyLogic = await requireCode(client, proxyLogic, block.number,
      'Factory proxy logic');

    stage = 'hierarchy and legacy fallback';
    const parent = await readEntry(client, ETH_REGISTRY, PARENT_LABEL, block.number);
    requireCondition(parent.state.status === REGISTERED && isAddressEqual(parent.owner, DEV) &&
      isAddressEqual(parent.subregistry, R0), 'demo-access.eth hierarchy changed.');
    const [rootRegistry, rootLabel] = await client.readContract({
      address: ETH_REGISTRY, abi: registryAbi, functionName: 'getParent', blockNumber: block.number,
    });
    requireCondition(rootLabel === 'eth' && !isAddressEqual(rootRegistry, zeroAddress),
      'ETHRegistry parent hierarchy is unexpected.');
    codeSizes.RootRegistry = await requireCode(client, rootRegistry, block.number, 'RootRegistry');
    const rootEth = await readEntry(client, rootRegistry, rootLabel, block.number);
    requireCondition(rootEth.state.status === REGISTERED &&
      isAddressEqual(rootEth.subregistry, ETH_REGISTRY), 'RootRegistry eth hierarchy changed.');
    const [r0ParentRegistry, r0ParentLabel] = await client.readContract({
      address: R0, abi: registryAbi, functionName: 'getParent', blockNumber: block.number,
    });

    const [guest, cred] = await Promise.all([
      readLegacy(client, 'guest-001', block.number),
      readLegacy(client, 'cred-001', block.number),
    ]);
    requireCondition(guest.state.status === REGISTERED &&
      isAddressEqual(guest.owner, '0x3419148731087b970d2059C53780163B452D5FF7') &&
      isAddressEqual(guest.resolver, S0) && isAddressEqual(guest.subregistry, zeroAddress) &&
      guest.expiry === 1820557476n && guest.access?.active === true &&
      guest.access.validUntil === 1793487599n, 'guest-001 baseline changed.');
    requireCondition(cred.state.status === REGISTERED && isAddressEqual(cred.owner, DEV) &&
      isAddressEqual(cred.resolver, S0) && isAddressEqual(cred.subregistry, zeroAddress) &&
      cred.expiry === 1820447664n && cred.access !== null, 'cred-001 baseline changed.');

    stage = 'keys availability';
    const keys = await readEntry(client, R0, KEYS_LABEL, block.number);
    assertKeysAvailable(keys);
    const devCanRegister = await client.readContract({
      address: R0, abi: registryAbi, functionName: 'hasRoles',
      args: [0n, ROLE_REGISTRAR, DEV], blockNumber: block.number,
    });
    requireCondition(devCanRegister, 'DEV lacks R0 ROLE_REGISTRAR.');

    stage = 'expiry planning';
    const expiry = expiryPlan([parent.expiry, rootEth.expiry]);

    stage = 'salts, addresses, and calldata';
    const plan = buildTransactions({ proxyLogic, expiry: expiry.recommended });
    assertCalldataRoundTrip(plan);
    requireCondition(!isAddressEqual(plan.r1.address, plan.s1.address),
      'R1 and S1 predicted addresses collide.');
    const [r1Code, s1Code] = await Promise.all([
      client.getCode({ address: plan.r1.address, blockNumber: block.number }),
      client.getCode({ address: plan.s1.address, blockNumber: block.number }),
    ]);
    requireCondition(!r1Code || r1Code === '0x', 'Predicted R1 already has code.');
    requireCondition(!s1Code || s1Code === '0x', 'Predicted S1 already has code.');

    stage = 'read-only simulation and gas estimation';
    const tx1Request = { address: FACTORY, abi: factoryAbi, functionName: 'deployProxy',
      args: [USER_REGISTRY_IMPL, plan.saltR1, plan.r1Initializer], account: DEV };
    const tx2Request = { address: FACTORY, abi: factoryAbi, functionName: 'deployProxy',
      args: [PERMISSIONED_RESOLVER_IMPL, plan.saltS1, plan.s1Initializer], account: DEV };
    const tx3Request = { address: R0, abi: registryAbi, functionName: 'register',
      args: [KEYS_LABEL, ISSUER, plan.r1.address, plan.s1.address, KEYS_OWNER_ROLES,
        expiry.recommended], account: DEV };
    const tx1Simulation = await client.simulateContract({ ...tx1Request, blockNumber: block.number });
    const tx2Simulation = await client.simulateContract({ ...tx2Request, blockNumber: block.number });
    const tx3Simulation = await client.simulateContract({ ...tx3Request, blockNumber: block.number });
    requireCondition(isAddressEqual(tx1Simulation.result, plan.r1.address),
      'TX1 simulation returned a different proxy.');
    requireCondition(isAddressEqual(tx2Simulation.result, plan.s1.address),
      'TX2 simulation returned a different proxy.');
    requireCondition(typeof tx3Simulation.result === 'bigint',
      'TX3 simulation did not return a token ID.');
    const [tx1Gas, tx2Gas, tx3Gas] = await Promise.all([
      client.estimateContractGas(tx1Request),
      client.estimateContractGas(tx2Request),
      client.estimateContractGas(tx3Request),
    ]);

    const output = {
      generatedAt: new Date().toISOString(),
      chain: {
        id: CHAIN_ID,
        blockNumber: block.number,
        blockTimestamp: block.timestamp,
        blockIso: iso(block.timestamp),
        ageSeconds: now - block.timestamp,
      },
      accounts: {
        DEV: { address: DEV, latestNonce: devLatest, pendingNonce: devPending },
        issuer: { address: ISSUER, latestNonce: issuerLatest, pendingNonce: issuerPending,
          balanceWei: issuerBalance, balanceEth: formatEther(issuerBalance) },
      },
      contracts: { pinnedEnsv2Commit: PINNED_ENSV2_COMMIT, ...addresses,
        proxyLogic, rootRegistry, codeSizes,
        r0Implementation, s0Implementation },
      hierarchy: {
        rootLabel,
        rootEthExpiry: rootEth.expiry,
        parentName: PARENT_NAME,
        parentTokenId: parent.tokenId,
        parentOwner: parent.owner,
        parentExpiry: parent.expiry,
        parentSubregistry: parent.subregistry,
        parentResolver: parent.resolver,
        r0DeclaredParent: r0ParentRegistry,
        r0DeclaredParentLabel: r0ParentLabel,
      },
      legacy: {
        guest001: { status: guest.state.status, tokenId: guest.tokenId, resource: guest.state.resource,
          owner: guest.owner, expiry: guest.expiry, resolver: guest.resolver,
          subregistry: guest.subregistry, access: guest.access },
        cred001: { status: cred.state.status, tokenId: cred.tokenId, resource: cred.state.resource,
          owner: cred.owner, expiry: cred.expiry, resolver: cred.resolver,
          subregistry: cred.subregistry, access: cred.access },
      },
      keysAvailability: { available: true, tokenId: keys.tokenId, resource: keys.state.resource,
        expiry: keys.expiry, latestOwner: keys.state.latestOwner, owner: keys.owner,
        subregistry: keys.subregistry, resolver: keys.resolver, roleCount: keys.roleCount },
      roles: {
        R1: { decimal: R1_ROOT_ROLES, hex: asHex(R1_ROOT_ROLES), names: roleNames.R1 },
        S1: { decimal: S1_ROOT_ROLES, hex: asHex(S1_ROOT_ROLES), names: roleNames.S1 },
        keysOwner: { decimal: KEYS_OWNER_ROLES, hex: asHex(KEYS_OWNER_ROLES),
          names: roleNames.keys, renewAdminIncluded: false },
      },
      expiry: {
        nearestAncestor: expiry.maximum,
        maximumLegal: expiry.maximum,
        recommended: expiry.recommended,
        utc: iso(expiry.recommended),
        madrid: madrid(expiry.recommended),
        safetyMarginSeconds: expiry.margin,
        safetyMarginDays: Number(expiry.margin) / 86400,
      },
      salts: {
        R1: { domain: saltDomains.r1, decimal: plan.saltR1, hex: asBytes32(plan.saltR1) },
        S1: { domain: saltDomains.s1, decimal: plan.saltS1, hex: asBytes32(plan.saltS1) },
      },
      expectedAddresses: { R1: plan.r1.address, S1: plan.s1.address,
        deployer: DEV, factory: FACTORY, proxyLogic, existingCode: false },
      transactions: {
        TX1: { target: FACTORY, function: 'deployProxy', implementation: USER_REGISTRY_IMPL,
          salt: plan.saltR1, initializer: plan.r1Initializer, calldata: plan.tx1,
          calldataDigest: keccak256(plan.tx1), simulation: 'PASS', result: tx1Simulation.result,
          gasEstimate: tx1Gas },
        TX2: { target: FACTORY, function: 'deployProxy', implementation: PERMISSIONED_RESOLVER_IMPL,
          salt: plan.saltS1, initializer: plan.s1Initializer, setters: [], calldata: plan.tx2,
          calldataDigest: keccak256(plan.tx2), simulation: 'PASS', result: tx2Simulation.result,
          gasEstimate: tx2Gas },
        TX3: { target: R0, function: 'register', label: KEYS_LABEL, owner: ISSUER,
          subregistry: plan.r1.address, resolver: plan.s1.address, roles: KEYS_OWNER_ROLES,
          expiry: expiry.recommended, calldata: plan.tx3, calldataDigest: keccak256(plan.tx3),
          simulation: 'PASS', resultTokenId: tx3Simulation.result, gasEstimate: tx3Gas },
      },
      totalEstimatedGas: tx1Gas + tx2Gas + tx3Gas,
      security: { blockchainWrites: 0, signing: 0, secretsPrinted: 0 },
    };
    console.log(JSON.stringify(output, (_key, value) => jsonValue(value), 2));
  } catch (_error) {
    // Raw transport errors can contain credential-bearing RPC URLs.
    console.error(`Issuer bootstrap planner stopped during ${stage}. No transaction was sent.`);
    process.exitCode = 1;
  }
}

const invokedPath = process.argv[1] ? pathToFileURL(process.argv[1]).href : '';
if (import.meta.url === invokedPath) await main();
