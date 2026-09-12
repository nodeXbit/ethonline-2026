// Offline fixture rehearsal only: synthetic accounts and in-memory ENS snapshots.
import { pathToFileURL } from 'node:url';
import { privateKeyToAccount } from 'viem/accounts';
import { zeroAddress } from 'viem';
import { credentialCandidate, issuer, validateCredentialSnapshot, verifyResourceProof } from './resource-verifier.mjs';
import { resourceBySlug, resources } from './access-resources.mjs';
import { DynamicGateBridge, GateMonitorState, VirtualGateProfiles } from './dynamic-gate-bridge.mjs';
import { IssuedChallengeStore, issueAccessChallenge, buildAccessChallengeTypedData } from './holder-proof.mjs';

export async function simulateResourceSession({ template = 'STAFF', gate = 'lab', ownerChanged = false,
  policyChanged = false, confirm = true, wrongCredential = false, onPreflight, snapshotChange,
  onState = () => {} } = {}) {
  const now = 1_800_000_000n;
  const holder = privateKeyToAccount(`0x${'11'.repeat(32)}`);
  const credential = credentialCandidate(`${template.toLowerCase()}-001.${issuer.namespace}`);
  const store = new IssuedChallengeStore();
  const profiles = new VirtualGateProfiles(); profiles.select(gate);
  const monitor = new GateMonitorState(profiles); monitor.begin();
  const snapshot = { credentialName: credential.name, credentialNode: credential.node,
    registry: issuer.issuerRegistry, resolver: issuer.issuerResolver, subregistry: zeroAddress,
    provenanceMatches: true, state: { status: 2 }, owner: holder.address, expiry: now + 600n,
    block: { number: 100n, timestamp: now }, access: { active: template !== 'CONTRACTOR', validUntil: now + 300n },
    parent: { state: { status: 2 }, subregistry: issuer.parentRegistry, expiry: now + 1000n },
    namespace: { state: { status: 2 }, subregistry: issuer.issuerRegistry, resolver: issuer.issuerResolver, expiry: now + 1000n },
    resourcePolicy: template === 'STAFF' ? resources.map(r => r.resourceId) : [resourceBySlug(template === 'VISITOR' ? 'front-door' : 'lab').resourceId],
  };
  const frames = [], timers = []; let issued = 0, finalReads = 0;
  const verify = args => verifyResourceProof({ ...args, store, now: () => now, readSnapshot: async () => {
    finalReads++;
    return { ...snapshot, ...(ownerChanged ? { owner: issuer.issuerAddress } : {}),
      ...(policyChanged ? { resourcePolicy: [] } : {}), ...snapshotChange };
  } });
  const bridge = new DynamicGateBridge({ profiles, onEvent: event => monitor.event(event),
    nowMs: () => 0, setTimer: (callback, delay) => { timers.push({ callback, delay }); return timers.length - 1; }, clearTimer: () => {},
    sendLine: async line => { frames.push(line); },
    discover: async candidate => { validateCredentialSnapshot(snapshot, candidate, now); await onPreflight?.(profiles); },
    issue: (candidate, resource) => { issued++; return issueAccessChallenge({ store, credential: candidate.node, resource: resource.resourceId, now: () => now }); },
    verify,
  });
  bridge.waitForCompletion().catch(() => {});
  await bridge.receiveLine('LOCKENS_DYNAMIC_V1_READY');
  await bridge.receiveLine('TARGET_ACTIVATION: PASS');
  await bridge.receiveLine('SELECT: PASS');
  await bridge.receiveLine(`CREDENTIAL_V1=0x${Buffer.from(credential.name).toString('hex')}`);
  await bridge.receiveLine('WAITING_CHALLENGE');
  const signingChallenge = wrongCredential ? { ...bridge.challenge, credential: credentialCandidate(`wrong-001.${issuer.namespace}`).node } : bridge.challenge;
  const signature = await holder.signTypedData(buildAccessChallengeTypedData(signingChallenge));
  await bridge.receiveLine(`PROOF=${signature}`);
  const beforeConfirmation = monitor.publicState(); onState(beforeConfirmation);
  if (confirm) await bridge.receiveLine(`AUTHORIZATION: ${bridge.result.allowed ? 'ALLOW' : 'DENY'}`);
  else timers.find(t => t.delay === 2000).callback();
  try { monitor.complete(await bridge.waitForCompletion()); } catch (error) { monitor.fail(error, bridge.result); }
  const replay = await verify({ challenge: bridge.challenge, signature, credential, resourceId: resourceBySlug(gate).resourceId });
  return { template, gate, result: bridge.result, monitor: monitor.publicState(), beforeConfirmation,
    replay, issued, finalReads, frames: frames.filter(frame => !frame.startsWith('CHALLENGE=')) };
}

export async function simulatedMatrix() {
  const rows = [];
  for (const [template, gate] of [['STAFF', 'lab'], ['VISITOR', 'lab'], ['CONTRACTOR', 'lab'], ['STAFF', 'server-room'], ['VISITOR', 'front-door']]) {
    const run = await simulateResourceSession({ template, gate });
    rows.push({ template, gate, decision: run.monitor.final, reason: run.result.reason, replay: run.replay.reason });
  }
  const wrong = await simulateResourceSession({ wrongCredential: true });
  rows.push({ template: 'WRONG_SELECTED_CREDENTIAL', gate: 'lab', decision: wrong.monitor.final, reason: wrong.result.reason });
  return rows;
}
if (process.argv[1] && pathToFileURL(process.argv[1]).href === import.meta.url) {
  console.log(JSON.stringify({ simulated: true, blockchainWrites: 0, physicalSignatures: 0, rows: await simulatedMatrix() }, null, 2));
}
