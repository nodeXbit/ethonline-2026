import { pathToFileURL } from 'node:url';
import {
  createPublicClient, encodeFunctionData, http, isAddress, isAddressEqual, parseEventLogs, zeroAddress,
} from 'viem';
import { sepolia } from 'viem/chains';
import {
  AVAILABLE, REGISTERED, ETHRegistry, VerifiableFactory, connect, factoryAbi,
  requireCode, requireCondition, send,
} from './contracts.mjs';
import {
  accessDuration, accessKey, accessRegistryAbi, canonicalResolver, credentialIdentity, credentialLabel,
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
    credential: s.credentialName,
    status: ['AVAILABLE', 'RESERVED', 'REGISTERED'][s.state.status] ?? s.state.status,
    owner: s.owner, registryExpiry: s.expiry.toString(), tokenId: s.tokenId.toString(),
    resource: s.state.resource.toString(), resolver: s.resolver,
    'access.active': s.access?.active ?? 'NOT CONFIGURED',
    'access.validUntil': s.access?.validUntil.toString() ?? 'NOT CONFIGURED',
    blockNumber: s.block.number.toString(), timestamp: s.block.timestamp.toString(),
  });
  console.log(`AUTHORIZATION: ${s.authorized ? 'ALLOW' : 'DENY'}`);
}

const dataCall = (resolver, access, node = credentialNode) => ({
  address: resolver, abi: resolverAbi, functionName: 'setData',
  args: [node, accessKey, encodeAccess(access)],
});

function targetCredential(context, options = {}) {
  const identity = credentialIdentity(options.credentialLabel ?? credentialLabel);
  const owner = options.credentialOwner ?? context.account.address;
  requireCondition(isAddress(owner), 'Credential owner must be an Ethereum address.');
  return { ...identity, owner };
}

const maxUint64 = (1n << 64n) - 1n;

export function nextInactiveRenewal(snapshot, validUntil) {
  requireRegistered(snapshot);
  requireCondition(snapshot.access !== null, 'access.v1 is not configured; inspect setup evidence.');
  requireCondition(snapshot.access.active === false,
    'Inactive renewal refuses an ACTIVE access record.');
  requireCondition(typeof validUntil === 'bigint' && validUntil >= 0n && validUntil <= maxUint64,
    'validUntil must be an explicit uint64 Unix timestamp.');
  requireCondition(validUntil > snapshot.block.timestamp,
    'Requested validUntil must be later than the pinned block timestamp.');
  requireCondition(validUntil <= snapshot.expiry,
    'Requested validUntil must not exceed the credential registry expiry.');
  if (snapshot.access.validUntil >= validUntil) return snapshot.access;
  return { active: false, validUntil };
}

let stage = 'configuration';
const pendingMessage = 'DEV has unresolved pending transactions. Wait for their result before rerunning setup.';
export class PendingSetupTransaction extends Error {
  constructor() { super(pendingMessage); }
}

export const accessTransactionState = Object.freeze({
  beforeSubmission: 'BEFORE_SUBMISSION',
  submitted: 'SUBMITTED',
  confirmedSuccess: 'CONFIRMED_SUCCESS',
  confirmedRevert: 'CONFIRMED_REVERT',
  confirmationUnknown: 'CONFIRMATION_UNKNOWN',
});

export class PendingAccessTransaction extends Error {
  constructor() {
    super('A previous Sepolia transaction is still pending. Wait for confirmation.');
  }
}

export class RevertedAccessTransaction extends Error {
  constructor(transactionHash) {
    super('The submitted Sepolia access transaction reverted.');
    this.transactionState = accessTransactionState.confirmedRevert;
    this.persistentContext = { operation: 'setData', transactionHash };
  }
}

const transientRpcErrorNames = new Set([
  'HttpRequestError', 'RpcRequestError', 'TimeoutError', 'WebSocketRequestError',
]);

export function isTransientRpcError(error) {
  const chain = [];
  for (let current = error; current && chain.length < 12; current = current.cause) chain.push(current);
  if (chain.some(item => item?.data?.errorName || /Revert|Abi|Invalid/.test(item?.name ?? ''))) {
    return false;
  }
  for (const current of chain) {
    if (transientRpcErrorNames.has(current.name)) return true;
    if ([429, 502, 503, 504, -32005].includes(current.code)) return true;
  }
  return false;
}

export class PreSubmissionRpcError extends Error {
  constructor(failureStage) {
    super('Sepolia RPC temporarily unavailable before transaction submission. No transaction was sent. Retry is safe.');
    this.name = 'PreSubmissionRpcError';
    this.stage = failureStage;
    this.retryable = true;
    this.noTransactionSent = true;
    this.transactionState = accessTransactionState.beforeSubmission;
  }
}

const defaultPreSubmissionRetryDelays = [0, 150, 400];

export async function retryPreSubmissionRpc(operation, {
  stage: failureStage, retryDelays = defaultPreSubmissionRetryDelays, sleep = delay,
} = {}) {
  for (let attempt = 0; attempt < retryDelays.length; attempt++) {
    if (retryDelays[attempt] > 0) await sleep(retryDelays[attempt]);
    try {
      return await operation();
    } catch (error) {
      if (!isTransientRpcError(error)) throw error;
      if (attempt === retryDelays.length - 1) throw new PreSubmissionRpcError(failureStage);
    }
  }
  throw new PreSubmissionRpcError(failureStage);
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

async function requireNoPendingAccess(client, address, retryOptions, guardStage) {
  const latest = await retryPreSubmissionRpc(
    () => client.getTransactionCount({ address, blockTag: 'latest' }),
    { ...retryOptions, stage: `${guardStage} latest nonce read` });
  const pending = await retryPreSubmissionRpc(
    () => client.getTransactionCount({ address, blockTag: 'pending' }),
    { ...retryOptions, stage: `${guardStage} pending nonce read` });
  if (pending > latest) throw new PendingAccessTransaction();
  requireCondition(pending === latest, 'Unexpected DEV transaction count state.');
}

export function accessStateAchieved(snapshot, action) {
  requireCondition(action === 'activate' || action === 'deactivate',
    'Access action must be activate or deactivate.');
  requireRegistered(snapshot);
  requireCondition(!isAddressEqual(snapshot.resolver, zeroAddress), 'Credential has no resolver.');
  requireCondition(snapshot.access !== null, 'access.v1 is not configured; inspect setup evidence.');
  if (action === 'deactivate') return snapshot.access.active === false;
  return snapshot.access.active === true && snapshot.access.validUntil > snapshot.block.timestamp &&
    snapshot.authorized === true;
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

function verifyAccessUpdate(before, after, access, minimumBlock) {
  requireCondition(after.block.number >= minimumBlock, 'Readback predates update.');
  requireSameCredential(before, after);
  requireCondition(sameAccess(after.access, access), 'Access readback mismatch.');
}

const defaultRecoveryDelays = [0, 250, 750];
const delay = milliseconds => new Promise(resolve => setTimeout(resolve, milliseconds));

async function recoverSubmittedAccess(context, {
  transactionHash, before, access, receipt: knownReceipt,
  credentialLabel: label = credentialLabel,
  recoveryDelays = defaultRecoveryDelays, sleep = delay,
}) {
  let receipt = knownReceipt;
  for (const milliseconds of recoveryDelays) {
    if (milliseconds > 0) await sleep(milliseconds);
    if (!receipt) {
      try {
        receipt = await context.publicClient.getTransactionReceipt({ hash: transactionHash });
      } catch {
        continue;
      }
    }
    if (receipt.status !== 'success') throw new RevertedAccessTransaction(transactionHash);
    try {
      const after = await readCredential(context.publicClient, { includeParent: true, label });
      verifyAccessUpdate(before, after, access, receipt.blockNumber);
      console.log({ operation: 'setData', transactionHash,
        blockNumber: receipt.blockNumber.toString(), recovered: true });
      return {
        changed: true,
        recovered: true,
        pending: false,
        transactionState: accessTransactionState.confirmedSuccess,
        transactionHash,
        blockNumber: receipt.blockNumber,
        credential: after,
      };
    } catch {
      // A successful receipt can precede a temporarily available authoritative readback.
    }
  }
  return {
    changed: true,
    pending: true,
    transactionState: receipt
      ? accessTransactionState.confirmedSuccess
      : accessTransactionState.confirmationUnknown,
    transactionHash,
    requestedState: access.active ? 'ACTIVE' : 'INACTIVE',
  };
}

// Each iteration reconstructs the next step from confirmed chain state. No local checkpoint.
export async function setupCredential(context, options = {}) {
  const { publicClient: client, account } = context;
  const target = targetCredential(context, options);
  requireCondition(await client.getChainId() === sepolia.id, 'RPC must be Sepolia.');
  await Promise.all([ETHRegistry, expectedRegistry, VerifiableFactory, PermissionedResolverImpl]
    .map(address => requireCode(client, address)));
  let minimumBlock = 0n;
  for (;;) {
    stage = 'setup state discovery';
    const before = await readCredential(client, { includeParent: true, label: target.label });
    requireCondition(before.block.number >= minimumBlock, 'Setup snapshot predates confirmed transaction.');
    if (before.state.status === REGISTERED) {
      requireRegistered(before);
      requireCondition(isAddressEqual(before.owner, target.owner),
        'Registered credential is not held by the intended owner.');
      requireCondition(!isAddressEqual(before.resolver, zeroAddress), 'Registered credential has no resolver.');
      // readCredential already verifies the authoritative resolver and strictly decodes the record.
      if (before.access !== null) return before;
      stage = 'registered missing-record recovery';
      const access = setupAccess(REGISTERED, null, before.block.timestamp);
      const transaction = await setupSend(context, dataCall(before.resolver, access, target.node));
      const after = await readCredential(client, { includeParent: true, label: target.label });
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
    const access = deployed ? await readAccess(client, resolver, before.block.number, target.node) : null;
    const inactive = setupAccess(AVAILABLE, access, before.block.timestamp);
    expiry = registrationExpiry(before, inactive.validUntil);
    const registration = { address: expectedRegistry, abi: accessRegistryAbi, functionName: 'register',
      args: [target.label, target.owner, zeroAddress, resolver, 0n, expiry] };
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
    await client.simulateContract({ ...dataCall(resolver, inactive, target.node), account });
    if (!sameAccess(access, inactive)) {
      stage = 'inactive record initialization';
      const initialization = await setupSend(context, dataCall(resolver, inactive, target.node));
      const stored = await readAccess(client, resolver, initialization.receipt.blockNumber, target.node);
      requireCondition(sameAccess(stored, inactive), 'Inactive initialization readback mismatch.');
      minimumBlock = initialization.receipt.blockNumber;
      continue;
    }

    // Only an explicitly inactive, future record reaches this write. Earlier writes are
    // followed by fresh snapshots above, so recovery uses the same path as first setup.
    stage = 'persistent registration';
    const registered = await setupSend(context, registration);
    const after = await readCredential(client, { includeParent: true, label: target.label });
    requireRegistered(after);
    requireCondition(after.block.number >= registered.receipt.blockNumber &&
      after.tokenId === registered.result && isAddressEqual(after.owner, target.owner) &&
      isAddressEqual(after.resolver, resolver) && isAddressEqual(after.subregistry, zeroAddress) &&
      after.expiry === expiry && after.expiry <= after.parentExpiry && sameAccess(after.access, inactive),
    'Persistent registration readback mismatch.');
    return after;
  }
}

async function updateAccessRecord(context, {
  target, requestedAccess, isAchieved, recoveryOptions,
}) {
  stage = 'access update preflight';
  const preSubmissionRetryOptions = recoveryOptions.preSubmissionRetryOptions ?? {};
  const before = await retryPreSubmissionRpc(
    () => readCredential(context.publicClient, { includeParent: true, label: target.label }),
    { ...preSubmissionRetryOptions, stage: 'access update credential read' });
  const access = requestedAccess(before);
  if (isAchieved(before, access)) {
    return {
      changed: false,
      writeRequired: false,
      preflight: Boolean(recoveryOptions.preflightOnly),
      recovered: false,
      pending: false,
      transactionState: accessTransactionState.beforeSubmission,
      transactionHash: null,
      blockNumber: before.block.number,
      credential: before,
    };
  }
  stage = 'access update pending nonce guard';
  await requireNoPendingAccess(context.publicClient, context.account.address,
    preSubmissionRetryOptions, 'initial');
  stage = 'simulated access update';
  const { request } = await retryPreSubmissionRpc(
    () => context.publicClient.simulateContract({
      ...dataCall(before.resolver, access, target.node), account: context.account,
    }), { ...preSubmissionRetryOptions, stage: 'access update simulation' });
  await requireNoPendingAccess(context.publicClient, context.account.address,
    preSubmissionRetryOptions, 'post-simulation');
  if (recoveryOptions.preflightOnly) {
    return {
      changed: false,
      writeRequired: true,
      preflight: true,
      requestedAccess: access,
      recovered: false,
      pending: false,
      transactionState: accessTransactionState.beforeSubmission,
      transactionHash: null,
      blockNumber: before.block.number,
      credential: before,
    };
  }
  stage = 'access update submission';
  const transactionHash = await context.walletClient.writeContract(request);
  console.log({ operation: 'setData', transactionHash });
  stage = 'access transaction receipt';
  let receipt;
  try {
    receipt = await context.publicClient.waitForTransactionReceipt({ hash: transactionHash });
  } catch {
    return recoverSubmittedAccess(context, {
      transactionHash, before, access, ...recoveryOptions,
    });
  }
  if (receipt.status !== 'success') throw new RevertedAccessTransaction(transactionHash);
  console.log({ operation: 'setData', transactionHash,
    blockNumber: receipt.blockNumber.toString() });
  stage = 'access and persistent identity readback';
  let after;
  try {
    after = await readCredential(context.publicClient, { includeParent: true, label: target.label });
    verifyAccessUpdate(before, after, access, receipt.blockNumber);
  } catch {
    return recoverSubmittedAccess(context, {
      transactionHash, before, access, receipt, ...recoveryOptions,
    });
  }
  return {
    changed: true,
    recovered: false,
    pending: false,
    transactionState: accessTransactionState.confirmedSuccess,
    transactionHash,
    blockNumber: receipt.blockNumber,
    credential: after,
  };
}

export async function updateAccess(context, action, recoveryOptions = {}) {
  requireCondition(action === 'activate' || action === 'deactivate',
    'Access action must be activate or deactivate.');
  const target = targetCredential(context, recoveryOptions);
  return updateAccessRecord(context, {
    target,
    recoveryOptions,
    requestedAccess: before => {
      requireCondition(isAddressEqual(before.owner, target.owner),
        'Credential is not held by the intended owner.');
      return nextAccess(action, before.access, before.block.timestamp);
    },
    isAchieved: before => accessStateAchieved(before, action),
  });
}

export async function renewInactiveAccess(context, validUntil, recoveryOptions = {}) {
  requireCondition(Boolean(recoveryOptions.credentialLabel),
    'Inactive renewal requires an explicit --credential-label.');
  requireCondition(recoveryOptions.credentialOwner === undefined,
    'Inactive renewal preserves the authoritative owner; do not pass --credential-owner.');
  const target = credentialIdentity(recoveryOptions.credentialLabel);
  return updateAccessRecord(context, {
    target,
    recoveryOptions,
    requestedAccess: before => nextInactiveRenewal(before, validUntil),
    isAchieved: (before, access) => before.access?.active === false &&
      before.access.validUntil === access.validUntil,
  });
}

export function parseCredentialOptions(args = []) {
  const options = {};
  for (let index = 0; index < args.length; index++) {
    const flag = args[index];
    if (flag === '--preflight') {
      options.preflightOnly = true;
      continue;
    }
    const value = args[index + 1];
    requireCondition(Boolean(value), `${flag ?? 'Option'} requires a value.`);
    index++;
    if (flag === '--credential-label') options.credentialLabel = value;
    else if (flag === '--credential-owner') options.credentialOwner = value;
    else if (flag === '--valid-until') {
      requireCondition(/^\d+$/.test(value), '--valid-until must be uint64 Unix seconds.');
      options.validUntil = BigInt(value);
      requireCondition(options.validUntil <= maxUint64,
        '--valid-until must be uint64 Unix seconds.');
    }
    else throw new Error(`Unknown option: ${flag}`);
  }
  return options;
}

export async function main(action = process.argv[2], args = process.argv.slice(3)) {
  requireCondition(['setup', 'activate', 'deactivate', 'renew-inactive', 'inspect'].includes(action),
    'Command must be setup, activate, deactivate, renew-inactive or inspect.');
  const credentialOptions = parseCredentialOptions(args);
  if (action === 'renew-inactive') {
    requireCondition(Boolean(credentialOptions.credentialLabel),
      'Inactive renewal requires an explicit --credential-label.');
    requireCondition(credentialOptions.validUntil !== undefined,
      'Inactive renewal requires an explicit --valid-until.');
    requireCondition(credentialOptions.credentialOwner === undefined,
      'Inactive renewal preserves the authoritative owner; do not pass --credential-owner.');
  } else {
    requireCondition(credentialOptions.preflightOnly === undefined,
      '--preflight is only supported for renew-inactive.');
  }
  // No signing connection or prior resolver knowledge is needed for inspect.
  if (action === 'inspect') {
    const client = createPublicClient({ chain: sepolia,
      transport: http(process.env.SEPOLIA_RPC_URL?.trim() || undefined) });
    requireCondition(await client.getChainId() === sepolia.id, 'RPC must be Sepolia.');
    stage = 'read-only inspection';
    printSnapshot(await readCredential(client, { label: credentialOptions.credentialLabel }));
    return;
  }
  const context = await connect();
  requireCondition(context.parent === parentName, 'This demo requires demo-access.eth.');
  if (action === 'setup') {
    const ready = await setupCredential(context, credentialOptions);
    printSnapshot(ready);
    console.log('PERSISTENT CREDENTIAL: READY');
    console.log(`ACCESS STATE: ${ready.access.active ? 'ACTIVE' : 'INACTIVE'}`);
    return;
  }

  if (action === 'renew-inactive') {
    const result = await renewInactiveAccess(context, credentialOptions.validUntil, credentialOptions);
    printSnapshot(result.credential);
    if (credentialOptions.preflightOnly) {
      console.log(`ACCESS RENEWAL PREFLIGHT: ${result.writeRequired ? 'WRITE REQUIRED' : 'NO-OP'}`);
      if (result.writeRequired) {
        console.log(`REQUESTED access.validUntil: ${result.requestedAccess.validUntil.toString()}`);
      }
      return;
    }
    console.log(`ACCESS RENEWAL: ${result.changed ? 'UPDATED' : 'NO-OP'}`);
    return;
  }

  const result = await updateAccess(context, action, credentialOptions);
  printSnapshot(result.credential);
  console.log(`ACCESS STATE: ${result.credential.access.active ? 'ACTIVE' : 'INACTIVE'}`);
}

if (process.argv[1] && pathToFileURL(process.argv[1]).href === import.meta.url) {
  main().catch(error => {
    console.error(error instanceof PendingSetupTransaction ? error.message :
      'Persistent access ERROR', publicErrorDetails(error));
    process.exitCode = 1;
  });
}
