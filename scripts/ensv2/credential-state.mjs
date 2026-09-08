import { isAddressEqual, zeroAddress } from 'viem';
import {
  ETHRegistry,
  AVAILABLE,
  REGISTERED,
  registryAbi,
  connect,
  parentState,
  requireCode,
  requireCondition,
  send,
} from './contracts.mjs';

const expectedRegistry = '0x2d249472B83A453086254Acd8a42913D8e45a2Fd';
const label = 'cred-001';
const action = process.argv[2];
let stage = 'configuration and Sepolia checks';

async function main() {
  requireCondition(action === 'issue' || action === 'revoke',
    'Action must be issue or revoke.');
  const context = await connect();
  const { account, publicClient } = context;
  await requireCode(publicClient, ETHRegistry);

  stage = 'parent and UserRegistry preflight';
  const { subregistry } = await parentState(publicClient, context.label, account);
  requireCondition(isAddressEqual(subregistry, expectedRegistry),
    'Parent UserRegistry does not match the verified deployment.');
  await requireCode(publicClient, subregistry);

  async function snapshot() {
    const block = await publicClient.getBlock();
    const read = (address, functionName, args) => publicClient.readContract({
      address,
      abi: registryAbi,
      functionName,
      args,
      blockNumber: block.number,
    });
    const pointer = await read(ETHRegistry, 'getSubregistry', [context.label]);
    requireCondition(isAddressEqual(pointer, expectedRegistry),
      'Parent UserRegistry changed during the operation.');
    const tokenId = await read(subregistry, 'findTokenId', [label]);
    const [state, owner, expiry] = await Promise.all([
      read(subregistry, 'getState', [tokenId]),
      read(subregistry, 'getOwner', [tokenId]),
      read(subregistry, 'getExpiry', [tokenId]),
    ]);
    requireCondition(state.tokenId === tokenId && state.expiry === expiry,
      'Inconsistent credential state reads.');
    return { block, tokenId, state, owner, expiry };
  }

  stage = `${action} state preflight`;
  const before = await snapshot();

  if (action === 'issue') {
    requireCondition(before.state.status === AVAILABLE,
      'Credential must be AVAILABLE before issue.');
    const requestedExpiry = before.block.timestamp + 24n * 60n * 60n;
    stage = 'issue transaction';
    const transaction = await send(context, {
      address: subregistry,
      abi: registryAbi,
      functionName: 'register',
      args: [label, account.address, zeroAddress, zeroAddress, 0n, requestedExpiry],
    });
    stage = 'issued state verification';
    const after = await snapshot();
    requireCondition(after.block.number >= transaction.receipt.blockNumber,
      'Readback predates the issue transaction.');
    requireCondition(after.state.status === REGISTERED &&
      isAddressEqual(after.owner, account.address) &&
      after.expiry === requestedExpiry && after.expiry > after.block.timestamp,
    'Issued credential state is not active as requested.');
    console.log({
      transactionHash: transaction.receipt.transactionHash,
      blockNumber: transaction.receipt.blockNumber.toString(),
    });
    console.log('CREDENTIAL STATE: ACTIVE');
    return;
  }

  requireCondition(before.state.status === REGISTERED,
    'Credential must be REGISTERED before revoke.');
  stage = 'revoke transaction';
  const transaction = await send(context, {
    address: subregistry,
    abi: registryAbi,
    functionName: 'unregister',
    args: [before.tokenId],
  });
  stage = 'revoked state verification';
  const after = await snapshot();
  requireCondition(after.block.number >= transaction.receipt.blockNumber,
    'Readback predates the revoke transaction.');
  requireCondition(after.state.status === AVAILABLE &&
    isAddressEqual(after.owner, zeroAddress),
  'Revoked credential state is not AVAILABLE with zero owner.');
  console.log({
    transactionHash: transaction.receipt.transactionHash,
    blockNumber: transaction.receipt.blockNumber.toString(),
  });
  console.log('CREDENTIAL STATE: REVOKED');
}

main().catch(() => {
  // Do not dump viem errors: RPC URLs can contain credentials.
  console.error(`Credential state operation stopped during ${stage}.`);
  process.exitCode = 1;
});
