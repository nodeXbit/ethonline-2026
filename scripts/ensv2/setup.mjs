import { randomBytes } from 'node:crypto';
import { encodeFunctionData, isAddressEqual, parseEventLogs, zeroAddress } from 'viem';
import {
  ETHRegistry, UserRegistryImpl, VerifiableFactory, ROLE_REGISTRAR, ROLE_UNREGISTER,
  ROLE_SET_SUBREGISTRY,
  registryAbi, userRegistryAbi, factoryAbi, connect, requireCode, parentState,
  requireCondition, send,
} from './contracts.mjs';

const missingSetSubregistryRole = 'DEV wallet lacks ROLE_SET_SUBREGISTRY on the parent.';
let stage = 'configuration and Sepolia checks';
async function main() {
  const context = await connect();
  const { publicClient, account, label } = context;
  await Promise.all([ETHRegistry, UserRegistryImpl, VerifiableFactory]
    .map(address => requireCode(publicClient, address)));
  stage = 'parent ownership and existing subregistry checks';
  const parent = await parentState(publicClient, label, account);
  // No saved deployment identity: treat every existing pointer as unexpected.
  // Rerunning after successful setup deliberately stops without writing.
  requireCondition(isAddressEqual(parent.subregistry, zeroAddress), 'Parent already has a subregistry.');
  stage = 'parent ROLE_SET_SUBREGISTRY permission check';
  const canSetSubregistry = await publicClient.readContract({
    address: ETHRegistry, abi: registryAbi, functionName: 'hasRoles',
    args: [parent.tokenId, ROLE_SET_SUBREGISTRY, account.address],
  });
  requireCondition(canSetSubregistry, missingSetSubregistryRole);

  const salt = BigInt(`0x${randomBytes(32).toString('hex')}`);
  const data = encodeFunctionData({
    abi: userRegistryAbi, functionName: 'initialize',
    args: [account.address, ROLE_REGISTRAR | ROLE_UNREGISTER],
  });
  stage = 'UserRegistry proxy deployment';
  const { result: proxy, receipt } = await send(context, {
    address: VerifiableFactory, abi: factoryAbi, functionName: 'deployProxy',
    args: [UserRegistryImpl, salt, data],
  });
  const events = parseEventLogs({ abi: factoryAbi, eventName: 'ProxyDeployed',
    logs: receipt.logs.filter(log => isAddressEqual(log.address, VerifiableFactory)), strict: true });
  requireCondition(events.length === 1 &&
    isAddressEqual(events[0].args.proxyAddress, proxy) &&
    isAddressEqual(events[0].args.sender, account.address) &&
    isAddressEqual(events[0].args.implementation, UserRegistryImpl) &&
    events[0].args.salt === salt, 'Proxy deployment event mismatch.');
  await requireCode(publicClient, proxy);
  console.log({ proxy });

  stage = 'connecting the proxy to the parent';
  // Recheck ownership and pointer after mining; never intentionally overwrite one.
  const current = await parentState(publicClient, label, account);
  requireCondition(isAddressEqual(current.subregistry, zeroAddress), 'Parent subregistry changed.');
  await send(context, { address: ETHRegistry, abi: registryAbi,
    functionName: 'setSubregistry', args: [current.tokenId, proxy] });
  const connected = await publicClient.readContract({ address: ETHRegistry,
    abi: registryAbi, functionName: 'getSubregistry', args: [label] });
  requireCondition(isAddressEqual(connected, proxy), 'Parent subregistry readback mismatch.');
  console.log({ parent: context.parent, proxy: connected });
}

main().catch(error => {
  // Do not dump viem errors: RPC URLs can contain credentials.
  if (error instanceof Error && error.message === missingSetSubregistryRole) {
    console.error(`Setup stopped: ${missingSetSubregistryRole}`);
    process.exitCode = 1;
    return;
  }
  console.error(`Setup stopped during ${stage}. Check configuration, ownership, permissions, and public transaction evidence.`);
  process.exitCode = 1;
});
