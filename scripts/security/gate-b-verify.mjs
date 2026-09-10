import { readFile } from 'node:fs/promises';
import { fileURLToPath } from 'node:url';
import { isAddress, isAddressEqual, recoverTypedDataAddress } from 'viem';
import { buildAccessChallengeTypedData } from './holder-proof.mjs';

export const gateBTypedDataPath = fileURLToPath(new URL(
  '../../mobile/android/app/src/main/assets/gate-b-typed-data.json',
  import.meta.url,
));

export async function loadGateBTypedData() {
  const androidTypedData = JSON.parse(await readFile(gateBTypedDataPath, 'utf8'));
  const canonical = buildAccessChallengeTypedData(androidTypedData.message);
  return { androidTypedData, canonical };
}

export async function verifyGateBSignature({ walletAddress, signature }) {
  if (!isAddress(walletAddress)) throw new TypeError('Privy wallet must be an Ethereum address.');
  if (!/^0x[0-9a-fA-F]{130}$/.test(signature)) {
    throw new TypeError('Signature must be 65-byte hex.');
  }
  const { canonical } = await loadGateBTypedData();
  const recoveredSigner = await recoverTypedDataAddress({ ...canonical, signature });
  return {
    walletAddress,
    recoveredSigner,
    matches: isAddressEqual(walletAddress, recoveredSigner),
  };
}

async function main() {
  const [walletAddress, signature] = process.argv.slice(2);
  if (!walletAddress || !signature) {
    throw new Error('Usage: node scripts/security/gate-b-verify.mjs <privy-wallet> <signature>');
  }
  const result = await verifyGateBSignature({ walletAddress, signature });
  console.log(`PRIVY WALLET:\n${result.walletAddress}`);
  console.log(`RECOVERED SIGNER:\n${result.recoveredSigner}`);
  console.log(`MATCH:\n${result.matches ? 'PASS' : 'FAIL'}`);
  if (!result.matches) process.exitCode = 1;
}

if (process.argv[1] && fileURLToPath(import.meta.url) === process.argv[1]) {
  main().catch(error => {
    console.error(error.message);
    process.exitCode = 1;
  });
}
