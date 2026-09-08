import {
  concatHex, decodeAbiParameters, encodeAbiParameters, getCreate2Address,
  isAddressEqual, keccak256, namehash, parseAbi, stringToHex, zeroAddress,
} from 'viem';
import { ETHRegistry, REGISTERED, registryAbi, requireCondition, VerifiableFactory } from './contracts.mjs';

export const expectedRegistry = '0x2d249472B83A453086254Acd8a42913D8e45a2Fd';
export const parentName = 'demo-access.eth';
export const parentLabel = 'demo-access';
export const credentialLabel = 'cred-001';
export const credentialNode = namehash(`${credentialLabel}.${parentName}`);
export const accessKey = 'access.v1';
export const accessDuration = 86400n;
export const PermissionedResolverImpl = '0x9eae5c2730a7dd16bdd1dee6421a1b91e3b0365e';
export const resolverRoles = (1n << 36n) | (1n << 164n);

// Verified 2026-09-09 against official Sepolia artifacts linked by ENS deployments docs:
// https://github.com/ensdomains/contracts-v2/tree/97a57293f3b4279d94b571e678edb53ce62638f4/contracts/deployments/sepolia
// The deployed factory takes ONE address and returns the current implementation.
export const resolverAbi = parseAbi([
  'function initialize(address admin, uint256 roleBitmap, bytes[] setters)',
  'function setData(bytes32 node, string key, bytes value)',
  'function data(bytes32 node, string key) view returns (bytes)',
]);
export const provenanceAbi = parseAbi([
  'function verifyContract(address proxy) view returns (address implementation)',
  'function proxyLogic() view returns (address)',
]);
export const accessRegistryAbi = [...registryAbi, ...parseAbi([
  'function getResolver(string label) view returns (address)',
])];
const schema = [{ type: 'bool' }, { type: 'uint64' }];

export function encodeAccess({ active, validUntil }) {
  requireCondition(typeof active === 'boolean' && typeof validUntil === 'bigint',
    'Invalid access.v1 value types.');
  return encodeAbiParameters(schema, [active, validUntil]);
}

export function decodeAccess(value) {
  if (value === '0x') return null;
  requireCondition(typeof value === 'string' && /^0x[0-9a-fA-F]{128}$/.test(value),
    'Malformed access.v1 length or hex.');
  const [active, validUntil] = decodeAbiParameters(schema, value);
  // Reject noncanonical booleans, uint64 padding, and trailing bytes.
  requireCondition(encodeAccess({ active, validUntil }) === value.toLowerCase(),
    'Noncanonical access.v1 encoding.');
  return { active, validUntil };
}

export function resolverSalt(owner) {
  return BigInt(keccak256(encodeAbiParameters(
    [{ type: 'bytes32' }, { type: 'address' }, { type: 'uint256' }],
    [keccak256(stringToHex('OwnedResolver')), owner, 0n],
  )));
}

export function predictResolver(owner, proxyLogic) {
  const salt = resolverSalt(owner);
  const outerSalt = keccak256(encodeAbiParameters(
    [{ type: 'address' }, { type: 'uint256' }], [owner, salt],
  ));
  // Exact CloneProxyBytecode.creationCode from the pinned factory artifact's metadata:
  // 10-byte creation stub, 45-byte clone runtime, then 32-byte outerSalt (87 total).
  const creationCode = concatHex([
    '0x3d604d80600a3d3981f3363d3d373d3d3d363d73', proxyLogic,
    '0x5af43d82803e903d91602b57fd5bf3', outerSalt,
  ]);
  return { salt, outerSalt, creationCode, resolver: getCreate2Address({
    from: VerifiableFactory, salt: outerSalt, bytecode: creationCode,
  }) };
}

export async function canonicalResolver(client, owner, blockNumber) {
  const proxyLogic = await client.readContract({ address: VerifiableFactory,
    abi: provenanceAbi, functionName: 'proxyLogic', blockNumber });
  requireCondition(!isAddressEqual(proxyLogic, zeroAddress), 'Factory proxy logic is zero.');
  const code = await client.getCode({ address: proxyLogic, blockNumber });
  requireCondition(Boolean(code && code !== '0x'), 'Factory proxy logic bytecode is absent.');
  return predictResolver(owner, proxyLogic);
}

export async function verifyResolver(client, resolver, blockNumber) {
  requireCondition(!isAddressEqual(resolver, zeroAddress), 'Credential resolver is zero.');
  const code = await client.getCode({ address: resolver, blockNumber });
  requireCondition(Boolean(code && code !== '0x'), 'Resolver bytecode is absent.');
  const implementation = await client.readContract({
    address: VerifiableFactory, abi: provenanceAbi, functionName: 'verifyContract',
    args: [resolver], blockNumber,
  });
  requireCondition(isAddressEqual(implementation, PermissionedResolverImpl),
    'Resolver factory provenance or implementation mismatch.');
}

export function isAuthorized({ state, owner, expiry, block, access }) {
  return state.status === REGISTERED && !isAddressEqual(owner, zeroAddress) &&
    expiry > block.timestamp && access?.active === true && access.validUntil > block.timestamp;
}

export async function readAccess(client, resolver, blockNumber) {
  return decodeAccess(await client.readContract({
    address: resolver, abi: resolverAbi, functionName: 'data',
    args: [credentialNode, accessKey], blockNumber,
  }));
}

// All chain reads, including provenance and resolver data, use the same block.
export async function readCredential(client, { includeParent = false } = {}) {
  const block = await client.getBlock();
  requireCondition(block.number !== null, 'Cannot pin pending block.');
  const read = (address, functionName, args) => client.readContract({
    address, abi: accessRegistryAbi, functionName, args, blockNumber: block.number,
  });
  const registry = await read(ETHRegistry, 'getSubregistry', [parentLabel]);
  requireCondition(isAddressEqual(registry, expectedRegistry), 'Unexpected parent UserRegistry.');
  const code = await client.getCode({ address: registry, blockNumber: block.number });
  requireCondition(Boolean(code && code !== '0x'), 'UserRegistry bytecode is absent.');
  const tokenId = await read(registry, 'findTokenId', [credentialLabel]);
  const [state, owner, expiry] = await Promise.all([
    read(registry, 'getState', [tokenId]), read(registry, 'getOwner', [tokenId]),
    read(registry, 'getExpiry', [tokenId]),
  ]);
  requireCondition(state.tokenId === tokenId && state.expiry === expiry, 'Inconsistent credential state.');
  let resolver = zeroAddress;
  let subregistry = zeroAddress;
  let access = null;
  // Registry invalidity is DENY without touching resolver code or records.
  if (state.status === REGISTERED && !isAddressEqual(owner, zeroAddress) && expiry > block.timestamp) {
    resolver = await read(registry, 'getResolver', [credentialLabel]);
    if (!isAddressEqual(resolver, zeroAddress)) {
      await verifyResolver(client, resolver, block.number);
      access = await readAccess(client, resolver, block.number);
    }
    if (includeParent) subregistry = await read(registry, 'getSubregistry', [credentialLabel]);
  }
  const result = { block, registry, tokenId, state, owner, expiry, resolver, subregistry, access };
  if (includeParent) {
    const parentTokenId = await read(ETHRegistry, 'findTokenId', [parentLabel]);
    [result.parentOwner, result.parentExpiry, result.parentResolver] = await Promise.all([
      read(ETHRegistry, 'getOwner', [parentTokenId]), read(ETHRegistry, 'getExpiry', [parentTokenId]),
      read(ETHRegistry, 'getResolver', [parentLabel]),
    ]);
  }
  return { ...result, authorized: isAuthorized(result) };
}
