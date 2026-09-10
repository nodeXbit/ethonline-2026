import assert from 'node:assert/strict';
import test from 'node:test';
import { decodeFunctionData, encodeAbiParameters, encodeEventTopics, isAddressEqual, zeroAddress } from 'viem';
import { AVAILABLE, ETHRegistry, REGISTERED, VerifiableFactory, factoryAbi } from './contracts.mjs';
import {
  accessDuration, credentialIdentity, credentialNode, decodeAccess, encodeAccess, expectedRegistry,
  isAuthorized, PermissionedResolverImpl, predictResolver, readCredential, resolverAbi, resolverRoles,
} from './access-record.mjs';
import {
  accessTransactionState, isTransientRpcError, nextAccess, PendingAccessTransaction, publicErrorDetails,
  PreSubmissionRpcError, requireSameCredential, registrationExpiry, RevertedAccessTransaction,
  setupCredential, updateAccess,
} from './persistent-access.mjs';
import { normalizeUid, parseDetectionLine } from '../nfc/bridge.mjs';

const owner = '0x1111111111111111111111111111111111111111';
const resolver = '0x2222222222222222222222222222222222222222';
const parentOwner = '0x3333333333333333333333333333333333333333';
const transientRpcError = () => Object.assign(new Error('HTTP request failed.'), {
  name: 'HttpRequestError',
});

test('transient RPC classification excludes deterministic contract and ABI failures', () => {
  assert.equal(isTransientRpcError(transientRpcError()), true);
  assert.equal(isTransientRpcError({
    name: 'ContractFunctionExecutionError',
    cause: { name: 'ContractFunctionRevertedError', data: { errorName: 'Unauthorized' },
      cause: transientRpcError() },
  }), false);
  assert.equal(isTransientRpcError({ name: 'AbiDecodingDataSizeTooSmallError', code: 503 }), false);
});

test('access.v1 round trips inactive/active and uint64 boundaries', () => {
  for (const active of [false, true]) {
    for (const validUntil of [0n, 86400n, (1n << 64n) - 1n]) {
      assert.deepEqual(decodeAccess(encodeAccess({ active, validUntil })), { active, validUntil });
    }
  }
  assert.equal(decodeAccess('0x'), null);
  assert.equal(encodeAccess({ active: true, validUntil: 86400n }),
    `0x${'1'.padStart(64, '0')}${'15180'.padStart(64, '0')}`);
});

test('malformed data is an error, including noncanonical ABI padding', () => {
  const word = n => n.toString(16).padStart(64, '0');
  for (const value of [undefined, '0x01', `0x${'00'.repeat(65)}`,
    `0x${word(2)}${word(1)}`, `0x${word(1)}${word(1n << 64n)}`, `0x${'zz'.repeat(64)}`]) {
    assert.throws(() => decodeAccess(value));
  }
  assert.throws(() => encodeAccess({ active: true, validUntil: 1n << 64n }));
  assert.throws(() => encodeAccess({ active: true, validUntil: -1n }));
});

function fixture(overrides = {}) {
  return { state: { status: REGISTERED, tokenId: 7n, expiry: 1000n, resource: 9n },
    owner, resolver, tokenId: 7n, expiry: 1000n, subregistry: zeroAddress,
    block: { number: 42n, timestamp: 100n }, access: { active: true, validUntil: 101n }, ...overrides };
}

test('authorization requires all five conditions with strict expiry boundaries', () => {
  assert.equal(isAuthorized(fixture()), true);
  for (const changes of [{ state: { status: 0 } }, { state: { status: 1 } },
    { owner: zeroAddress }, { expiry: 100n }, { expiry: 99n }, { access: null },
    { access: { active: false, validUntil: 101n } },
    { access: { active: true, validUntil: 100n } }, { access: { active: true, validUntil: 99n } }]) {
    assert.equal(isAuthorized(fixture(changes)), false);
  }
});

test('DENY -> ALLOW -> DENY preserves access expiry and credential identity', () => {
  const credential = fixture();
  let access = { active: false, validUntil: 200n };
  assert.equal(isAuthorized({ ...credential, access }), false);
  access = nextAccess('activate', access, 100n);
  assert.equal(access.validUntil, 200n);
  assert.equal(isAuthorized({ ...credential, access }), true);
  access = nextAccess('deactivate', access, 100n);
  assert.equal(access.validUntil, 200n);
  assert.equal(isAuthorized({ ...credential, access }), false);
  requireSameCredential(credential, { ...credential, access });
  assert.deepEqual(nextAccess('activate', access, 200n), { active: true, validUntil: 200n + accessDuration });
  assert.deepEqual(nextAccess('deactivate', access, 300n), access);
  assert.throws(() => nextAccess('activate', null, 100n));
  for (const changes of [{ owner: parentOwner }, { tokenId: 8n }, { resolver: parentOwner },
    { expiry: 999n }, { state: { ...credential.state, resource: 10n } }]) {
    assert.throws(() => requireSameCredential(credential, { ...credential, ...changes }));
  }
});

const logic = '0x4444444444444444444444444444444444444444';
const predicted = predictResolver(owner, logic).resolver;

// Public constants independently derived from the pinned CloneProxyBytecode assembly
// by applying its four mstore writes to an 87-byte buffer (not this helper's concat).
test('canonical prediction matches pinned factory creation-code vector', () => {
  const p = predictResolver(owner, logic);
  assert.equal(p.salt, BigInt('0x459dc478d7efb2bacddffcfd00cdfa9826abc93737276bc63edaa1978da93ba2'));
  assert.equal(p.outerSalt, '0x39f86a34899bd184be252fb2155422e9f11e1fe646d8507ecc6f4e5532fd7fa2');
  assert.equal(p.creationCode, '0x3d604d80600a3d3981f3363d3d373d3d3d363d7344444444444444444444444444444444444444445af43d82803e903d91602b57fd5bf339f86a34899bd184be252fb2155422e9f11e1fe646d8507ecc6f4e5532fd7fa2');
  assert.equal(p.resolver, '0xF1BE150644f5922A9FEAE13ad6eda4049334FC3f');
  assert.notEqual(predictResolver(parentOwner, logic).resolver, p.resolver);
  assert.notEqual(predictResolver(owner, parentOwner).resolver, p.resolver);
});

// No RPC, keys, or real signing client: simulate complete confirmed chain transitions.
function chain(options = {}) {
  const state = {
    status: AVAILABLE, deployed: false, access: '0x', expiry: 0n, holder: owner,
    parentOwner: owner, parentExpiry: 40000000n, parentResolver: zeroAddress,
    pointer: predicted, implementation: PermissionedResolverImpl,
    timestamp: 100n, blockNumber: 42n, pending: false, ...options,
  };
  const writes = [];
  const writeAttempts = [];
  const calls = [];
  const requests = [];
  const nonceCalls = { latest: 0, pending: 0 };
  let lastReceipt;
  const equals = isAddressEqual;
  const client = {
    getChainId: async () => state.chainId ?? 11155111,
    getBlock: async () => {
      if (writes.length === 0 && state.credentialReadFailures > 0) {
        state.credentialReadFailures--;
        throw transientRpcError();
      }
      if (writes.length > 0 && state.readbackFailures > 0) {
        state.readbackFailures--;
        throw Error('Temporary credential readback failure');
      }
      return { number: state.blockNumber, timestamp: state.timestamp };
    },
    getTransactionCount: async ({ blockTag }) => {
      nonceCalls[blockTag]++;
      const failureKey = blockTag === 'latest' ? 'latestNonceFailures' : 'pendingNonceFailures';
      const failureCallKey = blockTag === 'latest'
        ? 'latestNonceFailureOnCall' : 'pendingNonceFailureOnCall';
      if (state[failureCallKey] === nonceCalls[blockTag] || state[failureKey] > 0) {
        state[failureKey]--;
        throw transientRpcError();
      }
      return blockTag === 'pending' && state.pending ? 1 : 0;
    },
    getCode: async args => {
      if (args.blockNumber !== undefined) assert.equal(args.blockNumber, state.blockNumber);
      if (equals(args.address, predicted) || equals(args.address, resolver)) return state.deployed ? '0x1234' : '0x';
      return '0x1234';
    },
    readContract: async args => {
      assert.equal(args.blockNumber, state.blockNumber);
      calls.push(args.functionName);
      if (state.rpcFailure) throw Error('RPC failure');
      if (equals(args.address, VerifiableFactory)) {
        if (args.functionName === 'proxyLogic') return logic;
        assert.equal(args.functionName, 'verifyContract');
        if (state.provenanceFailure) throw Error('VerificationFailed');
        return state.implementation;
      }
      if (equals(args.address, predicted) || equals(args.address, resolver)) {
        assert.equal(args.functionName, 'data');
        assert.deepEqual(args.args, [state.credentialNode ?? credentialNode, 'access.v1']);
        return state.access;
      }
      const parent = equals(args.address, ETHRegistry);
      const registryValid = state.status === REGISTERED && state.expiry > state.timestamp;
      switch (args.functionName) {
        case 'getSubregistry': return parent ? expectedRegistry : zeroAddress;
        case 'findTokenId': return 7n;
        case 'getState': return { status: state.status, tokenId: 7n, resource: 9n, expiry: state.expiry };
        case 'getOwner': return parent ? state.parentOwner : registryValid ? state.holder : zeroAddress;
        case 'getExpiry': return parent ? state.parentExpiry : state.expiry;
        case 'getResolver':
          if (!parent && state.forbidResolverRead) throw Error('Resolver must not be read');
          return parent ? state.parentResolver : state.pointer;
        default: throw Error('Unexpected read');
      }
    },
    simulateContract: async request => {
      calls.push('simulate:' + request.functionName);
      requests.push(request);
      if (request.functionName === 'setData' && state.simulationFailures > 0) {
        state.simulationFailures--;
        throw transientRpcError();
      }
      if (request.functionName === 'setData' && state.noControl) throw Error('Missing setData role');
      if (request.functionName === 'register' && state.noRegistrar) throw Error('Missing registrar role');
      if (request.functionName === 'deployProxy') {
        assert.equal(state.deployed, false, 'Never redeploy an existing candidate');
        const initializer = decodeFunctionData({ abi: resolverAbi, data: request.args[2] });
        assert.deepEqual(initializer.args, [owner, resolverRoles, []]);
        if (state.pendingAfterSimulation) state.pending = true;
        return { request, result: state.predictionMismatch ? resolver : predicted };
      }
      if (state.pendingAfterSimulation) state.pending = true;
      return { request, result: request.functionName === 'register' ? 7n : undefined };
    },
    waitForTransactionReceipt: async () => {
      if (state.waitReceiptFailures > 0) {
        state.waitReceiptFailures--;
        throw Error('Temporary receipt RPC failure');
      }
      if (state.interruptAfter === writes.length) throw Error('Process interrupted after confirmed write');
      return lastReceipt;
    },
    getTransactionReceipt: async () => {
      if (state.recoveryReceiptFailures > 0) {
        state.recoveryReceiptFailures--;
        throw Error('Receipt not available');
      }
      return lastReceipt;
    },
  };
  const context = { account: { address: owner }, publicClient: client,
    walletClient: { writeContract: async request => {
      writeAttempts.push(request.functionName);
      if (state.writeFailures > 0) {
        state.writeFailures--;
        throw transientRpcError();
      }
      assert.equal(state.pending, false);
      writes.push(request.functionName);
      state.blockNumber++;
      state.timestamp++;
      const hash = '0x' + writes.length.toString(16).padStart(64, '0');
      const logs = [];
      if (request.functionName === 'deployProxy') {
        state.deployed = true;
        logs.push({ address: VerifiableFactory,
          topics: encodeEventTopics({ abi: factoryAbi, eventName: 'ProxyDeployed',
            args: { sender: owner, proxyAddress: predicted } }),
          data: encodeAbiParameters([{ type: 'uint256' }, { type: 'address' }],
            [predictResolver(owner, logic).salt, PermissionedResolverImpl]) });
      } else if (request.functionName === 'setData') {
        if (!state.ignoreAccessWrite) state.access = request.args[2];
      } else if (request.functionName === 'register') {
        assert.equal(decodeAccess(state.access)?.active, false, 'Must initialize inactive BEFORE registration');
        assert.ok(decodeAccess(state.access).validUntil > state.timestamp);
        assert.equal(request.args[4], 0n);
        state.status = REGISTERED;
        state.holder = request.args[1];
        state.expiry = request.args[5];
        state.pointer = request.args[3];
      } else throw Error('Unexpected write');
      lastReceipt = { status: state.receiptStatus ?? 'success', blockNumber: state.blockNumber,
        transactionHash: hash, logs };
      return hash;
    } },
  };
  return { state, writes, writeAttempts, calls, requests, client, context };
}

const registered = { status: REGISTERED, deployed: true, expiry: 1000000n };
const inactive = encodeAccess({ active: false, validUntil: 90000n });
const active = encodeAccess({ active: true, validUntil: 90000n });

test('shared updateAccess confirms INACTIVE -> ACTIVE -> INACTIVE with persistent identity', async () => {
  const c = chain({ ...registered, access: inactive });
  const before = await readCredential(c.client, { includeParent: true });
  const activated = await updateAccess(c.context, 'activate');
  assert.equal(activated.changed, true);
  assert.equal(activated.credential.authorized, true);
  assert.equal(activated.credential.access.active, true);
  assert.match(activated.transactionHash, /^0x[0-9a-f]{64}$/);
  requireSameCredential(before, activated.credential);

  const deactivated = await updateAccess(c.context, 'deactivate');
  assert.equal(deactivated.changed, true);
  assert.equal(deactivated.credential.authorized, false);
  assert.equal(deactivated.credential.access.active, false);
  requireSameCredential(activated.credential, deactivated.credential);
  assert.deepEqual(c.writes, ['setData', 'setData']);
});

test('sequential activation and deactivation are semantic no-ops after the first write', async () => {
  const activating = chain({ ...registered, access: inactive });
  assert.equal((await updateAccess(activating.context, 'activate')).changed, true);
  const activeNoOp = await updateAccess(activating.context, 'activate');
  assert.equal(activeNoOp.changed, false);
  assert.equal(activeNoOp.transactionHash, null);
  assert.deepEqual(activating.writes, ['setData']);

  const deactivating = chain({ ...registered, access: active });
  assert.equal((await updateAccess(deactivating.context, 'deactivate')).changed, true);
  const inactiveNoOp = await updateAccess(deactivating.context, 'deactivate');
  assert.equal(inactiveNoOp.changed, false);
  assert.equal(inactiveNoOp.transactionHash, null);
  assert.deepEqual(deactivating.writes, ['setData']);
});

test('activation renews an active but expired access deadline exactly once', async () => {
  const c = chain({ ...registered, access: encodeAccess({ active: true, validUntil: 100n }) });
  const result = await updateAccess(c.context, 'activate');
  assert.equal(result.changed, true);
  assert.equal(result.credential.access.active, true);
  assert.ok(result.credential.access.validUntil > result.credential.block.timestamp);
  assert.deepEqual(c.writes, ['setData']);
});

test('updateAccess cannot report confirmed state without requested readback', async () => {
  const stale = chain({ ...registered, access: inactive, ignoreAccessWrite: true });
  const unresolved = await updateAccess(stale.context, 'activate', {
    recoveryDelays: [0], sleep: async () => {},
  });
  assert.equal(unresolved.pending, true);
  assert.equal(unresolved.transactionState, accessTransactionState.confirmedSuccess);
  assert.equal(unresolved.credential, undefined);
  assert.deepEqual(stale.writes, ['setData']);

  const failed = chain({ ...registered, access: inactive, noControl: true });
  await assert.rejects(updateAccess(failed.context, 'activate'));
  assert.deepEqual(failed.writes, []);
});

test('receipt RPC failure recovers by hash without a second write', async () => {
  const c = chain({ ...registered, access: inactive, waitReceiptFailures: 1 });
  const result = await updateAccess(c.context, 'activate', {
    recoveryDelays: [0], sleep: async () => {},
  });
  assert.equal(result.recovered, true);
  assert.equal(result.pending, false);
  assert.equal(result.transactionState, accessTransactionState.confirmedSuccess);
  assert.equal(result.credential.authorized, true);
  assert.deepEqual(c.writes, ['setData']);
});

test('confirmed receipt recovers from a temporary credential readback failure', async () => {
  const c = chain({ ...registered, access: inactive, readbackFailures: 1 });
  const result = await updateAccess(c.context, 'activate', {
    recoveryDelays: [0], sleep: async () => {},
  });
  assert.equal(result.recovered, true);
  assert.equal(result.transactionState, accessTransactionState.confirmedSuccess);
  assert.equal(result.credential.authorized, true);
  assert.deepEqual(c.writes, ['setData']);
});

test('exhausted receipt recovery returns pending evidence without resubmission', async () => {
  const c = chain({ ...registered, access: inactive, waitReceiptFailures: 1,
    recoveryReceiptFailures: 2 });
  const result = await updateAccess(c.context, 'activate', {
    recoveryDelays: [0, 0], sleep: async () => {},
  });
  assert.equal(result.pending, true);
  assert.equal(result.transactionState, accessTransactionState.confirmationUnknown);
  assert.equal(result.requestedState, 'ACTIVE');
  assert.match(result.transactionHash, /^0x[0-9a-f]{64}$/);
  assert.deepEqual(c.writes, ['setData']);
});

test('pending DEV nonce blocks a new access action before or after simulation', async () => {
  for (const pendingState of [{ pending: true }, { pendingAfterSimulation: true }]) {
    const c = chain({ ...registered, access: inactive, ...pendingState });
    await assert.rejects(updateAccess(c.context, 'activate'), PendingAccessTransaction);
    assert.deepEqual(c.writes, []);
  }
});

test('transient credential, nonce and simulation failures retry before one write', async () => {
  for (const failures of [
    { credentialReadFailures: 1 },
    { latestNonceFailures: 1 },
    { pendingNonceFailures: 1 },
    { latestNonceFailureOnCall: 2 },
    { pendingNonceFailureOnCall: 2 },
    { simulationFailures: 1 },
  ]) {
    const c = chain({ ...registered, access: inactive, ...failures });
    const result = await updateAccess(c.context, 'activate', {
      preSubmissionRetryOptions: { retryDelays: [0, 0], sleep: async () => {} },
    });
    assert.equal(result.changed, true);
    assert.deepEqual(c.writeAttempts, ['setData']);
    assert.deepEqual(c.writes, ['setData']);
  }
});

test('exhausted transient pre-submission failure is retryable and writes nothing', async () => {
  const c = chain({ ...registered, access: inactive, simulationFailures: 3 });
  await assert.rejects(updateAccess(c.context, 'activate', {
    preSubmissionRetryOptions: { retryDelays: [0, 0, 0], sleep: async () => {} },
  }), error => {
    assert.ok(error instanceof PreSubmissionRpcError);
    assert.equal(error.stage, 'access update simulation');
    assert.equal(error.noTransactionSent, true);
    assert.equal(error.transactionHash, undefined);
    return true;
  });
  assert.deepEqual(c.writeAttempts, []);
  assert.deepEqual(c.writes, []);
});

test('deterministic simulation failure is not retried', async () => {
  const c = chain({ ...registered, access: inactive, noControl: true });
  await assert.rejects(updateAccess(c.context, 'activate', {
    preSubmissionRetryOptions: { retryDelays: [0, 0, 0], sleep: async () => {} },
  }), /Missing setData role/);
  assert.equal(c.calls.filter(call => call === 'simulate:setData').length, 1);
  assert.deepEqual(c.writeAttempts, []);
});

test('writeContract transport failure is never automatically retried', async () => {
  const c = chain({ ...registered, access: inactive, writeFailures: 1 });
  await assert.rejects(updateAccess(c.context, 'activate', {
    preSubmissionRetryOptions: { retryDelays: [0, 0, 0], sleep: async () => {} },
  }), error => error.name === 'HttpRequestError');
  assert.deepEqual(c.writeAttempts, ['setData']);
  assert.deepEqual(c.writes, []);
});

test('confirmed reverted access transaction retains its hash without resubmission', async () => {
  const c = chain({ ...registered, access: inactive, receiptStatus: 'reverted' });
  await assert.rejects(updateAccess(c.context, 'activate'), error => {
    assert.ok(error instanceof RevertedAccessTransaction);
    assert.match(error.persistentContext.transactionHash, /^0x[0-9a-f]{64}$/);
    return true;
  });
  assert.deepEqual(c.writes, ['setData']);
});

test('fresh setup deploys, initializes inactive, then registers', async () => {
  const c = chain();
  const result = await setupCredential(c.context);
  assert.deepEqual(c.writes, ['deployProxy', 'setData', 'register']);
  assert.equal(result.state.status, REGISTERED);
  assert.equal(result.access.active, false);
  assert.equal(result.authorized, false);
});

test('existing canonical proxy is verified and reused without deployment', async () => {
  const c = chain({ deployed: true });
  await setupCredential(c.context);
  assert.deepEqual(c.writes, ['setData', 'register']);
  assert.ok(c.calls.includes('verifyContract'));
  assert.ok(!c.calls.includes('simulate:deployProxy'));
});

test('existing parent resolver is verified and reused without canonical deployment', async () => {
  const c = chain({ deployed: true, parentResolver: resolver });
  const result = await setupCredential(c.context);
  assert.deepEqual(c.writes, ['setData', 'register']);
  assert.equal(result.resolver, resolver);
  assert.ok(!c.calls.includes('proxyLogic'));
});

test('pre-initialized inactive record is preserved without another setData', async () => {
  const c = chain({ deployed: true, access: inactive });
  const result = await setupCredential(c.context);
  assert.deepEqual(c.writes, ['register']);
  assert.equal(result.access.validUntil, 90000n);
});

test('pre-registration ACTIVE or stale data is made inactive before registering', async () => {
  for (const access of [active, encodeAccess({ active: false, validUntil: 100n }),
    encodeAccess({ active: true, validUntil: 99n })]) {
    const c = chain({ deployed: true, access });
    const result = await setupCredential(c.context);
    assert.deepEqual(c.writes, ['setData', 'register']);
    assert.equal(result.access.active, false);
    assert.ok(result.access.validUntil > result.block.timestamp);
  }
});

test('REGISTERED INACTIVE and ACTIVE reruns preserve all state with zero writes', async () => {
  for (const access of [inactive, active, encodeAccess({ active: true, validUntil: 1n })]) {
    const c = chain({ ...registered, access });
    const first = await setupCredential(c.context);
    const second = await setupCredential(c.context);
    assert.deepEqual(c.writes, []);
    assert.equal(c.state.access, access);
    requireSameCredential(first, second);
  }
});

test('REGISTERED missing record recovers inactive and subsequent setup does not write', async () => {
  const c = chain(registered);
  await setupCredential(c.context);
  await setupCredential(c.context);
  assert.deepEqual(c.writes, ['setData']);
  assert.equal(decodeAccess(c.state.access).active, false);
});

test('confirmed interruptions after each write resume to READY without duplicate writes', async () => {
  for (const interruptAfter of [1, 2, 3]) {
    const c = chain({ interruptAfter });
    await assert.rejects(setupCredential(c.context), /interrupted/);
    c.state.interruptAfter = undefined;
    const ready = await setupCredential(c.context);
    await setupCredential(c.context);
    assert.equal(ready.state.status, REGISTERED);
    assert.deepEqual(c.writes, ['deployProxy', 'setData', 'register']);
  }
});

test('prediction mismatch, unproven proxy, missing control, and pending work stop before writes', async () => {
  for (const options of [{ predictionMismatch: true }, { deployed: true, provenanceFailure: true },
    { deployed: true, implementation: resolver }, { deployed: true, noControl: true },
    { noRegistrar: true }, { pending: true }, { pendingAfterSimulation: true }, { chainId: 1 }]) {
    const c = chain(options);
    await assert.rejects(setupCredential(c.context));
    assert.deepEqual(c.writes, []);
  }
});

test('pending guard also protects record initialization and registration recovery', async () => {
  for (const options of [{ deployed: true }, { deployed: true, access: inactive }, registered]) {
    const c = chain({ ...options, pending: true });
    await assert.rejects(setupCredential(c.context), /Wait for their result/);
    assert.deepEqual(c.writes, []);
  }
});

test('registered setup rejects wrong holder or zero resolver without writes', async () => {
  for (const changes of [{ holder: parentOwner }, { pointer: zeroAddress }]) {
    const c = chain({ ...registered, ...changes });
    await assert.rejects(setupCredential(c.context));
    assert.deepEqual(c.writes, []);
  }
});

test('malformed access records block both available and registered recovery', async () => {
  for (const status of [AVAILABLE, REGISTERED]) {
    const c = chain({ ...registered, status, access: '0xab' });
    await assert.rejects(setupCredential(c.context), /Malformed/);
    assert.deepEqual(c.writes, []);
  }
});

test('expiry invariants replace fixed multi-day threshold', () => {
  const base = { block: { timestamp: 100n }, parentExpiry: 86500n };
  assert.throws(() => registrationExpiry(base));
  assert.equal(registrationExpiry({ ...base, parentExpiry: 86501n }), 86501n);
  assert.throws(() => registrationExpiry({ ...base, parentExpiry: 90000n }, 90000n));
  assert.equal(registrationExpiry({ ...base, parentExpiry: 999999999n }), 100n + 365n * accessDuration);
});

test('scoped setup can provision a separate label and owner without changing cred-001 defaults', async () => {
  const guestOwner = '0x3419148731087b970d2059C53780163B452D5FF7';
  const guest = credentialIdentity('guest-001');
  const c = chain({ credentialNode: guest.node, deployed: true, parentResolver: resolver });
  const result = await setupCredential(c.context, {
    credentialLabel: guest.label,
    credentialOwner: guestOwner,
  });
  const registration = c.requests.find(request => request.functionName === 'register');
  assert.deepEqual(registration.args.slice(0, 2), [guest.label, guestOwner]);
  assert.equal(result.credentialName, guest.name);
  assert.equal(result.credentialNode, guest.node);
  assert.notEqual(result.credentialNode, credentialNode);
  assert.ok(isAddressEqual(result.owner, guestOwner));
  assert.equal(result.access.active, false);
  assert.equal(registration.args[3], resolver);
  assert.deepEqual(c.writes, ['setData', 'register']);
});

test('persistent failures expose safe stage, operation, custom error and submitted hash', () => {
  const transactionHash = `0x${'ab'.repeat(32)}`;
  const cause = {
    name: 'ContractFunctionRevertedError',
    code: -32000,
    shortMessage: 'The contract function reverted. See https://rpc.example/key-secret for details.',
    data: { errorName: 'EACUnauthorizedAccountRoles', args: [9n, 1n << 36n, owner] },
    persistentContext: { operation: 'setData', transactionHash },
  };
  const report = publicErrorDetails({
    name: 'ContractFunctionExecutionError',
    shortMessage: 'The contract function "setData" reverted via https://rpc.example/key-secret', cause,
  }, 'inactive record initialization');
  assert.deepEqual(report, {
    stage: 'inactive record initialization', operation: 'setData',
    error: 'ContractFunctionExecutionError',
    message: 'The contract function "setData" reverted via [redacted URL]',
    customError: 'EACUnauthorizedAccountRoles',
    customErrorArgs: [9n, 1n << 36n, owner], rpcCode: -32000, transactionHash,
  });
});

test('short parent lifetime uses invariants and insufficient lifetime stops before deployment', async () => {
  const allowed = chain({ parentExpiry: 200000n });
  assert.equal((await setupCredential(allowed.context)).expiry, 200000n);
  const insufficient = chain({ parentExpiry: 86500n });
  await assert.rejects(setupCredential(insufficient.context), /Parent expiry/);
  assert.deepEqual(insufficient.writes, []);
});

test('bridge/inspect discover resolver and do not require parent-owner equality', async () => {
  const c = chain({ ...registered, access: active, parentOwner });
  assert.equal((await readCredential(c.client)).authorized, true);
  assert.ok(!c.calls.includes('proxyLogic'));
});

test('registry invalidity returns DENY without even reading resolver', async () => {
  for (const changes of [{ status: AVAILABLE }, { status: 1 }, { expiry: 100n },
    { expiry: 99n }, { holder: zeroAddress }]) {
    const c = chain({ ...registered, ...changes, forbidResolverRead: true, provenanceFailure: true });
    assert.equal((await readCredential(c.client)).authorized, false);
    assert.ok(!c.calls.includes('getResolver'));
    assert.ok(!c.calls.includes('verifyContract'));
    assert.ok(!c.calls.includes('data'));
  }
});

test('zero resolver, missing record, inactive and expired access are normal DENY', async () => {
  for (const changes of [{ pointer: zeroAddress }, { access: '0x' }, { access: inactive },
    { access: encodeAccess({ active: true, validUntil: 100n }) }]) {
    const c = chain({ ...registered, ...changes });
    assert.equal((await readCredential(c.client)).authorized, false);
  }
});

test('RPC failure, malformed data, missing code, provenance and implementation failures are ERROR', async () => {
  for (const changes of [{ rpcFailure: true }, { access: '0xab' }, { deployed: false },
    { provenanceFailure: true }, { implementation: resolver }]) {
    const c = chain({ ...registered, ...changes });
    await assert.rejects(readCredential(c.client));
  }
});

test('existing NFC UID parsing remains intact', () => {
  assert.equal(normalizeUid('91-2d-e3-06'), '91:2D:E3:06');
  assert.equal(parseDetectionLine('ISO14443A tag detected; UID=91:2D:E3:06 (4 bytes)'), '91:2D:E3:06');
  assert.equal(parseDetectionLine('ISO14443A tag detected; UID=01:02:03:04:05:06:07 (7 bytes)'), '01:02:03:04:05:06:07');
  assert.equal(parseDetectionLine('unrelated serial input'), null);
});
