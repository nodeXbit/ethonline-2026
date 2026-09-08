import { createPublicClient, createWalletClient, http, isAddressEqual, parseAbi } from 'viem';
import { privateKeyToAccount } from 'viem/accounts';
import { normalize } from 'viem/ens';
import { sepolia } from 'viem/chains';

// Sepolia Beta: recheck addresses AND deployment ABIs before future use.
// Verified 2026-09-08 against https://docs.ens.domains/learn/deployments/
// Official artifacts (ETHRegistry.json, UserRegistryImpl.json, VerifiableFactory.json):
// https://github.com/ensdomains/contracts-v2/tree/97a57293f3b4279d94b571e678edb53ce62638f4/contracts/deployments/sepolia
// Status and role constants verified in src/registry/interfaces/IPermissionedRegistry.sol
// and src/registry/libraries/RegistryRolesLib.sol at that same commit.
export const ETHRegistry = '0xbdc85dd5b15d7ecb354cd7cb6f2c50b4f2c4f0e2';
export const UserRegistryImpl = '0x624a25d67b59d587752ebec8dded8827dae52050';
export const VerifiableFactory = '0x10dc6333cdfe1fcef624c6e0a8221b91804cd7ef';
export const ROLE_REGISTRAR = 1n << 0n;
export const ROLE_UNREGISTER = 1n << 12n;
export const ROLE_SET_SUBREGISTRY = 1n << 20n;
export const AVAILABLE = 0;
export const REGISTERED = 2; // RESERVED is 1.

export const registryAbi = parseAbi([
  'function findTokenId(string label) view returns (uint256)',
  'function getOwner(uint256 anyId) view returns (address)',
  'function getSubregistry(string label) view returns (address)',
  'function hasRoles(uint256 anyId, uint256 roleBitmap, address account) view returns (bool)',
  'function setSubregistry(uint256 anyId, address registry)',
  'function getState(uint256 anyId) view returns ((uint8 status, uint64 expiry, address latestOwner, uint256 tokenId, uint256 resource) state)',
  'function getExpiry(uint256 anyId) view returns (uint64)',
  'function register(string label, address owner, address registry, address resolver, uint256 roleBitmap, uint64 expiry) returns (uint256)',
  'function unregister(uint256 anyId)',
]);
export const userRegistryAbi = parseAbi([
  'function initialize(address rootAccount, uint256 roleBitmap)',
]);
export const factoryAbi = parseAbi([
  // Salt is uint256 in this deployment, not bytes32.
  'function deployProxy(address implementation, uint256 salt, bytes data) returns (address proxy)',
  'event ProxyDeployed(address indexed sender, address indexed proxyAddress, uint256 salt, address implementation)',
]);

export function requireCondition(condition, message) {
  if (!condition) throw new Error(message);
}

export async function connect() {
  const rawName = process.env.ENS_PARENT_NAME?.trim();
  requireCondition(Boolean(rawName), 'ENS_PARENT_NAME is required.');
  const parent = normalize(rawName);
  const parts = parent.split('.');
  requireCondition(parts.length === 2 && parts[0] && parts[1] === 'eth',
    'ENS_PARENT_NAME must be a direct .eth second-level name.');
  const key = process.env.DEV_PRIVATE_KEY;
  requireCondition(/^0x[0-9a-fA-F]{64}$/.test(key ?? ''), 'DEV_PRIVATE_KEY must be configured.');
  const account = privateKeyToAccount(key);
  const transport = http(process.env.SEPOLIA_RPC_URL?.trim() || undefined);
  const publicClient = createPublicClient({ chain: sepolia, transport });
  const chainId = await publicClient.getChainId();
  requireCondition(chainId === 11155111, 'RPC must be Sepolia (11155111).');
  const walletClient = createWalletClient({ account, chain: sepolia, transport });
  console.log({ account: account.address, chainId, parent });
  return { account, publicClient, walletClient, parent, label: parts[0] };
}

export async function requireCode(client, address) {
  const code = await client.getCode({ address });
  requireCondition(Boolean(code && code !== '0x'), 'Required contract bytecode is absent.');
}

export async function parentState(client, label, account) {
  const block = await client.getBlock();
  const read = (functionName, args) => client.readContract({
    address: ETHRegistry, abi: registryAbi, functionName, args, blockNumber: block.number,
  });
  const tokenId = await read('findTokenId', [label]);
  const owner = await read('getOwner', [tokenId]);
  requireCondition(isAddressEqual(owner, account.address), 'DEV wallet must own the parent.');
  const subregistry = await read('getSubregistry', [label]);
  return { tokenId, subregistry };
}

export async function send(context, contract) {
  const { request, result } = await context.publicClient.simulateContract({
    ...contract, account: context.account,
  });
  const hash = await context.walletClient.writeContract(request);
  console.log({ operation: contract.functionName, transactionHash: hash });
  const receipt = await context.publicClient.waitForTransactionReceipt({ hash });
  requireCondition(receipt.status === 'success', 'Transaction receipt was not successful.');
  console.log({ operation: contract.functionName, transactionHash: receipt.transactionHash,
    blockNumber: receipt.blockNumber.toString() });
  return { result, receipt };
}
