// Offline deterministic test fixtures only. No project wallet/configuration is loaded.
import { mkdir, writeFile } from 'node:fs/promises';
import { pathToFileURL } from 'node:url';
import { hashTypedData, namehash } from 'viem';
import { resources, encodeResources } from './access-resources.mjs';
import { buildAccessChallengeTypedData } from './holder-proof.mjs';
import { serializeChallenge } from './gate-e-secure-bridge.mjs';

export async function resourceVectors() {
  const name = 'staff-001.keys.demo-access.eth';
  const challenge = { credential: namehash(name), resource: resources[1].resourceId,
    nonce: `0x${'5a'.repeat(32)}`, expiresAt: 1_800_000_060n };
  // Public test address only; these committed vectors contain no signatures or private material.
  const holderAddress = '0x19E7E376E7C213B7E7e7e46cc70A5dD086DAff2A';
  const typedData = buildAccessChallengeTypedData(challenge);
  const payload = serializeChallenge(challenge).slice('CHALLENGE='.length);
  return {
    'version': '1', 'chainId': '11155111', 'domain.name': 'ENSv2 Access', 'domain.version': '1',
    ...Object.fromEntries(resources.map(r => [`resource.${r.slug}`, r.resourceId])),
    'policy.empty': encodeResources([]), 'policy.front': encodeResources([resources[0].resourceId]),
    'policy.all': encodeResources(resources.map(r => r.resourceId)),
    'credential.name': name, 'credential.node': challenge.credential,
    'challenge.bytes': payload, 'challenge.now': '1800000000', 'challenge.expiry': String(challenge.expiresAt),
    'challenge.digest': hashTypedData(typedData), 'holder.address': holderAddress,
    'apdu.select': '00a4040008f0454e5356324331', 'apdu.credential': '80400100',
    'apdu.credentialResponse': Buffer.from(name).toString('hex') + '9000',
    'apdu.challenge': `8010010068${payload.slice(2)}`,
    'serial.ready': 'LOCKENS_DYNAMIC_V1_READY',
    'serial.credential': `CREDENTIAL_V1=0x${Buffer.from(name).toString('hex')}`,
    'serial.challenge': serializeChallenge(challenge),
    'serial.allow': 'AUTHORIZATION=ALLOW', 'serial.allowConfirmed': 'AUTHORIZATION: ALLOW',
    'serial.deny': 'AUTHORIZATION=DENY', 'serial.denyConfirmed': 'AUTHORIZATION: DENY',
  };
}
export const vectorText = vectors => Object.entries(vectors).map(([key, value]) => `${key}=${value}\n`).join('');
if (process.argv[1] && pathToFileURL(process.argv[1]).href === import.meta.url) {
  await mkdir(new URL('../../fixtures/', import.meta.url), { recursive: true });
  await writeFile(new URL('../../fixtures/lockens-access-v1.properties', import.meta.url), vectorText(await resourceVectors()));
  console.log('Public resource/APDU/challenge/serial vectors generated offline.');
}
