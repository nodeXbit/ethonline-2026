import { pathToFileURL } from 'node:url';
import {
  createPublicClient, encodeFunctionData, http, isAddressEqual, parseEventLogs, zeroAddress,
} from 'viem';
import { sepolia } from 'viem/chains';
import {
  AVAILABLE, REGISTERED, ETHRegistry, VerifiableFactory, connect, factoryAbi,
  requireCode, requireCondition, send,
} from './contracts.mjs';
import {
  accessDuration, accessKey, accessRegistryAbi, canonicalResolver, credentialLabel,
  credentialNode, encodeAccess, expectedRegistry, parentName, readAccess,
  PermissionedResolverImpl, readCredential, resolverAbi, resolverRoles, verifyResolver,
} from './access-record.mjs';

export function nextAccess(action, access, timestamp) {
  requireCondition(access !== null, 'access.v1 is not configured; inspect setup evidence.');
  requireCondition(action === 'activate' || action === 'deactivate', 'Invalid access action.');
  return { active: action === 'activate', validUntil:
    action === 'activate' && access.validUntil <= timestamp
      ? timestamp + accessDuration : access.validUntil };
}

function requireRegistered(snapshot) {
  requireCondition(snapshot.state.status === REGISTERED &&
    !isAddressEqual(snapshot.owner, zeroAddress) && snapshot.expiry > snapshot.block.timestamp,
  'Credential must be REGISTERED, owned and unexpired.');
}

export function requireSameCredential(before, after) {
  requireRegistered(after);
  requireCondition(before.tokenId === after.tokenId && before.state.resource === after.state.resource &&
    before.expiry === after.expiry && isAddressEqual(before.owner, after.owner) &&
    isAddressEqual(before.resolver, after.resolver) && isAddressEqual(before.subregistry, after.subregistry),
  'Credential identity changed during access update.');
}

function printSnapshot(s) {
  console.log({
    credential: `${credentialLabel}.${parentName}`,
    status: ['AVAILABLE', 'RESERVED', 'REGISTERED'][s.state.status] ?? s.state.status,
    owner: s.owner, registryExpiry: s.expiry.toString(), tokenId: s.tokenId.toString(),
    resource: s.state.resource.toString(), resolver: s.resolver,
    'access.active': s.access?.active ?? 'NOT CONFIGURED',
    'access.validUntil': s.access?.validUntil.toString() ?? 'NOT CONFIGURED',
    blockNumber: s.block.number.toString(), timestamp: s.block.timestamp.toString(),
  });
  console.log(`AUTHORIZATION: ${s.authorized ? 'ALLOW' : 'DENY'}`);
}

const dataCall = (resolver, access) => ({
  address: resolver, abi: resolverAbi, functionName: 'setData',
  args: [credentialNode, accessKey, encodeAccess(access)],
});

let stage = 'configuration';
const pendingMessage = 'DEV has unresolved pending transactions. Wait for their result before rerunning setup.';
export class PendingSetupTransaction extends Error {
  constructor() { super(pendingMessage); }
}

export async function requireNoPending(client, address) {
  const [latest, pending] = await Promise.all([
    client.getTransactionCount({ address, blockTag: 'latest' }),
    client.getTransactionCount({ address, blockTag: 'pending' }),
  ]);
  if (latest !== pending) throw new PendingSetupTransaction();
}

// Separate from the unchanged legacy send helper: validate the exact simulation result
// before submitting its request, and guard every setup write against unknown pending work.
async function setupSend(context, contract, predictedResolver) {
  const { publicClient: client, account, walletClient } = context;
  let hash;
  try {
    await requireNoPending(client, account.address);
    const { request, result } = await client.simulateContract({ ...contract, account });
    if (predictedResolver) requireCondition(isAddressEqual(result, predictedResolver),
      'Simulated proxy does not match deterministic prediction.');
    await requireNoPending(client, account.address);
    hash = await walletClient.writeContract(request);
    console.log({ operation: contract.functionName, transactionHash: hash });
    const receipt = await client.waitForTransactionReceipt({ hash });
    requireCondition(receipt.status === 'success', 'Setup transaction receipt failed.');
    return { result, receipt };
  } catch (error) {
    // Preserve only public transaction context for the top-level safe error reporter.
    if (error && typeof error === 'object') {
      try {
        error.persistentContext = { operation: contract.functionName, transactionHash: hash };
      } catch {
        // Never replace the original failure if an RPC library freezes its error object.
      }
    }
    throw error;
  }
}

const safeMessage = value => String(value).split(/\r?\n/, 1)[0]
  .replace(/https?:\/\/\S+/gi, '[redacted URL]');

// Avoid viem's full error rendering because it can include a credential-bearing RPC URL.
// Contract error names/arguments, stages and transaction hashes are public chain data.
export function publicErrorDetails(error, failureStage = stage) {
  const chain = [];
  for (let current = error; current && chain.length < 12; current = current.cause) chain.push(current);
  const custom = chain.find(item => item?.data?.errorName);
  const context = chain.find(item => item?.persistentContext)?.persistentContext;
  const transactionHash = context?.transactionHash ??
    chain.map(item => item?.transactionHash ?? item?.receipt?.transactionHash)
      .find(value => /^0x[0-9a-fA-F]{64}$/.test(value ?? ''));
  const report = {
    stage: failureStage,
    operation: context?.operation,
    error: chain.find(item => item?.name && item.name !== 'Error')?.name ?? error?.name ?? 'Error',
    message: safeMessage(chain.find(item => item?.shortMessage)?.shortMessage ??
      error?.message ?? 'Unknown failure.'),
    customError: custom?.data.errorName,
    customErrorArgs: custom?.data.args,
    rpcCode: chain.find(item => item?.code !== undefined)?.code,
    transactionHash,
  };
  return Object.fromEntries(Object.entries(report).filter(([, value]) => value !== undefined));
}

export function registrationExpiry(snapshot, accessDeadline = snapshot.block.timestamp + accessDuration) {
  const year = snapshot.block.timestamp + 365n * accessDuration;
  const expiry = year < snapshot.parentExpiry ? year : snapshot.parentExpiry;
  requireCondition(expiry > snapshot.block.timestamp + accessDuration && expiry > accessDeadline &&
    expiry <= snapshot.parentExpiry, 'Parent expiry cannot cover persistent registration beyond access expiry.');
  return expiry;
}

export function setupAccess(status, access, timestamp) {
  // A registered credential's valid record is never reset, including ACTIVE or expired access.
  if (status === REGISTERED && access !== null) return access;
  if (access?.active === false && access.validUntil > timestamp) return access;
  return { active: false, validUntil: access && access.validUntil > timestamp
    ? access.validUntil : timestamp + accessDuration };
}

const sameAccess = (a, b) => a?.active === b?.active && a?.validUntil === b?.validUntil;

// Each iteration reconstructs the next step from confirmed chain state. No local checkpoint.
export async function setupCredential(context) {
  const { publicClient: client, account } = context;
  requireCondition(await client.getChainId() === sepolia.id, 'RPC must be Sepolia.');
  await Promise.all([ETHRegistry, expectedRegistry, VerifiableFactory, PermissionedResolverImpl]
    .map(address => requireCode(client, address)));
  let minimumBlock = 0n;
  for (;;) {
    stage = 'setup state discovery';
    const before = await readCredential(client, { includeParent: true });
    requireCondition(before.block.number >= minimumBlock, 'Setup snapshot predates confirmed transaction.');
    if (before.state.status === REGISTERED) {
      requireRegistered(before);
      requireCondition(isAddressEqual(before.owner, account.address), 'Registered credential is not held by DEV.');
      requireCondition(!isAddressEqual(before.resolver, zeroAddress), 'Registered credential has no resolver.');
      // readCredential already verifies the authoritative resolver and strictly decodes the record.
      if (before.access !== null) return before;
      stage = 'registered missing-record recovery';
      const access = setupAccess(REGISTERED, null, before.block.timestamp);
      const transaction = await setupSend(context, dataCall(before.resolver, access));
      const after = await readCredential(client, { includeParent: true });
      requireCondition(after.block.number >= transaction.receipt.blockNumber, 'Recovery readback predates write.');
      requireSameCredential(before, after);
      requireCondition(sameAccess(after.access, access), 'Recovery access readback mismatch.');
      return after;
    }

    requireCondition(before.state.status === AVAILABLE, 'Setup requires AVAILABLE or REGISTERED credential.');
    requireCondition(isAddressEqual(before.parentOwner, account.address), 'DEV must own parent before registration.');
    let expiry = registrationExpiry(before);
    let resolver = before.parentResolver;
    let prediction;
    stage = 'resolver discovery';
    if (isAddressEqual(resolver, zeroAddress)) {
      prediction = await canonicalResolver(client, account.address, before.block.number);
      resolver = prediction.resolver;
    }
    const code = await client.getCode({ address: resolver, blockNumber: before.block.number });
    const deployed = Boolean(code && code !== '0x');
    // A nonzero parent pointer must be proven; never replace an unknown parent resolver.
    requireCondition(prediction || deployed, 'Parent resolver bytecode is absent.');
    if (deployed) await verifyResolver(client, resolver, before.block.number);
    const access = deployed ? await readAccess(client, resolver, before.block.number) : null;
    const inactive = setupAccess(AVAILABLE, access, before.block.timestamp);
    expiry = registrationExpiry(before, inactive.validUntil);
    const registration = { address: expectedRegistry, abi: accessRegistryAbi, functionName: 'register',
      args: [credentialLabel, account.address, zeroAddress, resolver, 0n, expiry] };
    // Check registry authority before any deployment or record write. register stores the
    // resolver pointer without calling it, so this is valid even for an undeployed candidate.
    await client.simulateContract({ ...registration, account });
    if (!deployed) {
      stage = 'canonical deployment';
      const data = encodeFunctionData({ abi: resolverAbi, functionName: 'initialize',
        args: [account.address, resolverRoles, []] });
      const deployment = await setupSend(context, { address: VerifiableFactory, abi: factoryAbi,
        functionName: 'deployProxy', args: [PermissionedResolverImpl, prediction.salt, data] }, resolver);
      const events = parseEventLogs({ abi: factoryAbi, eventName: 'ProxyDeployed', strict: true,
        logs: deployment.receipt.logs.filter(log => isAddressEqual(log.address, VerifiableFactory)) });
      requireCondition(events.length === 1 && isAddressEqual(events[0].args.proxyAddress, resolver) &&
        isAddressEqual(events[0].args.sender, account.address) && events[0].args.salt === prediction.salt &&
        isAddressEqual(events[0].args.implementation, PermissionedResolverImpl), 'ProxyDeployed mismatch.');
      await verifyResolver(client, resolver, deployment.receipt.blockNumber);
      minimumBlock = deployment.receipt.blockNumber;
      continue;
    }

    // Prove DEV control even when preserving an existing inactive record.
    await client.simulateContract({ ...dataCall(resolver, inactive), account });
    if (!sameAccess(access, inactive)) {
      stage = 'inactive record initialization';
      const initialization = await setupSend(context, dataCall(resolver, inactive));
      const stored = await readAccess(client, resolver, initialization.receipt.blockNumber);
      requireCondition(sameAccess(stored, inactive), 'Inactive initialization readback mismatch.');
      minimumBlock = initialization.receipt.blockNumber;
      continue;
    }

    // Only an explicitly inactive, future record reaches this write. Earlier writes are
    // followed by fresh snapshots above, so recovery uses the same path as first setup.
    stage = 'persistent registration';
    const registered = await setupSend(context, registration);
    const after = await readCredential(client, { includeParent: true });
    requireRegistered(after);
    requireCondition(after.block.number >= registered.receipt.blockNumber &&
      after.tokenId === registered.result && isAddressEqual(after.owner, account.address) &&
      isAddressEqual(after.resolver, resolver) && isAddressEqual(after.subregistry, zeroAddress) &&
      after.expiry === expiry && after.expiry <= after.parentExpiry && sameAccess(after.access, inactive),
    'Persistent registration readback mismatch.');
    return after;
  }
}

export async function main(action = process.argv[2]) {
  requireCondition(['setup', 'activate', 'deactivate', 'inspect'].includes(action),
    'Command must be setup, activate, deactivate or inspect.');
  // No signing connection or prior resolver knowledge is needed for inspect.
  if (action === 'inspect') {
    const client = createPublicClient({ chain: sepolia,
      transport: http(process.env.SEPOLIA_RPC_URL?.trim() || undefined) });
    requireCondition(await client.getChainId() === sepolia.id, 'RPC must be Sepolia.');
    stage = 'read-only inspection';
    printSnapshot(await readCredential(client));
    return;
  }
  const context = await connect();
  requireCondition(context.parent === parentName, 'This demo requires demo-access.eth.');
  if (action === 'setup') {
    const ready = await setupCredential(context);
    printSnapshot(ready);
    console.log('PERSISTENT CREDENTIAL: READY');
    console.log(`ACCESS STATE: ${ready.access.active ? 'ACTIVE' : 'INACTIVE'}`);
    return;
  }

  stage = 'access update preflight';
  const before = await readCredential(context.publicClient, { includeParent: true });
  requireRegistered(before);
  requireCondition(!isAddressEqual(before.resolver, zeroAddress), 'Credential has no resolver.');
  const access = nextAccess(action, before.access, before.block.timestamp);
  stage = 'simulated access update and receipt';
  const transaction = await send(context, dataCall(before.resolver, access));
  stage = 'access and persistent identity readback';
  const after = await readCredential(context.publicClient, { includeParent: true });
  requireCondition(after.block.number >= transaction.receipt.blockNumber, 'Readback predates update.');
  requireSameCredential(before, after);
  requireCondition(sameAccess(after.access, access), 'Access readback mismatch.');
  printSnapshot(after);
  console.log(`ACCESS STATE: ${access.active ? 'ACTIVE' : 'INACTIVE'}`);
}

if (process.argv[1] && pathToFileURL(process.argv[1]).href === import.meta.url) {
  main().catch(error => {
    console.error(error instanceof PendingSetupTransaction ? error.message :
      'Persistent access ERROR', publicErrorDetails(error));
    process.exitCode = 1;
  });
}
