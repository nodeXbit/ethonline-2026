import { fileURLToPath } from 'node:url';
import { verifyGateBSignature } from './gate-b-verify.mjs';

export async function verifyGateC2Signature({ walletAddress, signature }) {
  const result = await verifyGateBSignature({ walletAddress, signature });
  return { ...result, signatureLength: (signature.length - 2) / 2 };
}

async function main() {
  const [walletAddress, signature] = process.argv.slice(2);
  if (!walletAddress || !signature) {
    throw new Error('Usage: node scripts/security/gate-c2-verify.mjs <privy-wallet> <hce-signature>');
  }
  const result = await verifyGateC2Signature({ walletAddress, signature });
  console.log(`PRIVY WALLET:\n${result.walletAddress}`);
  console.log(`HCE SIGNATURE LENGTH:\n${result.signatureLength}`);
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
