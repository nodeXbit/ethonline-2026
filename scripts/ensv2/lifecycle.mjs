import { isAddressEqual, zeroAddress } from 'viem';
import {
  ETHRegistry, AVAILABLE, REGISTERED, registryAbi, connect, requireCode,
  parentState, requireCondition, send,
} from './contracts.mjs';

const label = 'cred-001';
let stage = 'configuration and parent checks';
async function main() {
  const context = await connect();
  const { publicClient, account } = context;
  await requireCode(publicClient, ETHRegistry);
  const { subregistry } = await parentState(publicClient, context.label, account);
  requireCondition(!isAddressEqual(subregistry, zeroAddress), 'Run setup first.');
  await requireCode(publicClient, subregistry);
  console.log({ proxy: subregistry, credential: `${label}.${context.parent}` });

  async function snapshot() {
    // Pin all reads to one latest block; refind the ID because unregister regenerates it.
    const block = await publicClient.getBlock();
    const read = (functionName, args) => publicClient.readContract({
      address: subregistry, abi: registryAbi, functionName, args, blockNumber: block.number,
    });
    const tokenId = await read('findTokenId', [label]);
    const [state, owner, expiry, pointer] = await Promise.all([
      read('getState', [tokenId]), read('getOwner', [tokenId]), read('getExpiry', [tokenId]),
      publicClient.readContract({ address: ETHRegistry, abi: registryAbi,
        functionName: 'getSubregistry', args: [context.label], blockNumber: block.number }),
    ]);
    requireCondition(isAddressEqual(pointer, subregistry), 'Parent subregistry changed.');
    requireCondition(state.tokenId === tokenId && state.expiry === expiry, 'Inconsistent state reads.');
    console.log({ blockNumber: block.number.toString(), timestamp: block.timestamp.toString(),
      tokenId: tokenId.toString(), status: state.status, owner, expiry: expiry.toString(),
      latestOwner: state.latestOwner, resource: state.resource.toString() });
    const authorized = state.status === REGISTERED &&
      isAddressEqual(owner, account.address) && expiry > block.timestamp;
    return { tokenId, state, owner, expiry, block, authorized };
  }

  stage = 'initial AVAILABLE check';
  const before = await snapshot();
  requireCondition(before.state.status === AVAILABLE, 'Credential must be AVAILABLE before starting.');
  stage = 'register';
  const latest = await publicClient.getBlock();
  const registration = await send(context, { address: subregistry, abi: registryAbi,
    functionName: 'register', args: [label, account.address, zeroAddress, zeroAddress, 0n,
      latest.timestamp + 24n * 60n * 60n] });

  stage = 'registered state verification';
  const active = await snapshot();
  requireCondition(active.block.number >= registration.receipt.blockNumber, 'Read predates registration.');
  requireCondition(active.authorized, 'Registered state must authorize the DEV wallet.');
  console.log('AUTHORIZATION: ALLOW');

  stage = 'unregister';
  const revocation = await send(context, { address: subregistry, abi: registryAbi,
    functionName: 'unregister', args: [active.tokenId] });
  stage = 'revoked state verification';
  const revoked = await snapshot();
  requireCondition(revoked.block.number >= revocation.receipt.blockNumber, 'Read predates revocation.');
  requireCondition(revoked.state.status === AVAILABLE && isAddressEqual(revoked.owner, zeroAddress)
    && !revoked.authorized, 'Revoked state must be AVAILABLE with no owner.');
  console.log('AUTHORIZATION: DENY');
  console.log({ registerTransaction: registration.receipt.transactionHash,
    unregisterTransaction: revocation.receipt.transactionHash });
}

main().catch(() => {
  // A failed read is not evidence of revocation. Never print DENY on an RPC error.
  // Suppress raw errors to avoid exposing RPC credentials or configuration.
  console.error(`Lifecycle stopped during ${stage}. Inspect the public transaction evidence before retrying; the credential may still be registered.`);
  process.exitCode = 1;
});
