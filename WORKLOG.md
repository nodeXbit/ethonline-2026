# Worklog

## 2026-09-05

Preflight development environment completed.
No product code has been written yet.

## 2026-09-07

- Validated ESP32-S3 serial output and programming with a minimal serial test sketch.
- Validated PN532 communication over I2C and detected a physical ISO14443A tag, including successful UID reading.
- Confirmed wiring and address: GPIO17 is SDA, GPIO18 is SCL, and the PN532 responds at I2C address `0x24`.
- Identified the initial failure as an HSU/I2C interface mismatch: the PN532 hardware/interface selection and sketch transport must both use I2C.
- Confirmed that custom ESP32-S3 I2C pins require `Wire.setPins(17, 18)` before `nfc.begin()`, because the Elechouse I2C transport calls `Wire.begin()` without pin arguments.
- Identified an Elechouse library behavior in which `PN532::SAMConfig()` treats the valid zero-payload SAMConfiguration response as failure. Kept the library unchanged and added a sketch-level command/response workaround that accepts a validated zero-length payload.
- Hardware feasibility result: PASS for PC -> ESP32-S3 -> PN532 over I2C -> physical ISO14443A tag.
- Decided that static NFC UID can serve only as a clonable prototype/demo identifier. Cryptographic NFC challenge-response and Android HCE remain outside the MVP critical path and may be revisited as security hardening if time permits.

## 2026-09-08

- Completed the ENSv2 Sepolia vertical slice for `demo-access.eth` with UserRegistry `0x2d249472B83A453086254Acd8a42913D8e45a2Fd`.
- Attached the parent with `setSubregistry` transaction `0x5c9c54c1873260be56e91a0c8f9adfd37479a92e9c5cf05a864fc2aa0e7f2ab8` (block `11663664`), after proxy deployment transaction `0xb820d86648c1201f59c5f9966adfbee22cb0fee5772a5da3ff39166d88d0657a` (block `11663662`).
- Registered `cred-001.demo-access.eth` with transaction `0xde2447b4146cb1687428e43abf51dac3f748be6dc52f744474a48e1bbbe60dd9` (block `11663688`), read `REGISTERED` with the DEV wallet as owner, and produced `AUTHORIZATION: ALLOW`.
- Unregistered it with transaction `0x65c2d6a6b8ad86d365541d57a26b83d3222ffce2eadb42f84bcdd0f91ac35d91` (block `11663690`), read `AVAILABLE` with zero owner, and produced `AUTHORIZATION: DENY`.
- Key learning: ENSv2 is verified as load-bearing authorization state. The credential tokenId/resource changed after unregister, so the logical ENS name/label is the stable application reference.

## 2026-09-09

- Completed the physical NFC → ENSv2 authorization demo with physical UID `91:2D:E3:06`: ISO14443A tag → ESP32-S3 + PN532 over I2C → 115200-baud serial UID → Node PC bridge → Sepolia ENSv2 read.
- The bridge resolved `demo-access.eth` to UserRegistry `0x2d249472B83A453086254Acd8a42913D8e45a2Fd` and mapped the UID to `cred-001.demo-access.eth`.
- Revoked read at block `11663898`: status `AVAILABLE` (0), zero owner, and result `AUTHORIZATION: DENY`.
- Issued with transaction `0x965260d7a766e0d0bbaa0abe110487723a229cd06d81b88acf770b42a5a98d35` in block `11663902`; result `CREDENTIAL STATE: ACTIVE`. The same physical tag then read at block `11663904` as `REGISTERED` (2), owner `0x4C60a5AD311510543B56d0408872A52e4AEEe19C`, and `AUTHORIZATION: ALLOW`.
- Revoked with transaction `0xd538c72e6ec5a59e4c7722db0c4ce63a98731f832a76d351fa68c5c7b9803c69` in block `11663907`; result `CREDENTIAL STATE: REVOKED`. The same physical tag then read at block `11663909` as `AVAILABLE` (0), zero owner, and `AUTHORIZATION: DENY`.
- The logical ENS label remained `cred-001.demo-access.eth`, while the current tokenId changed after unregister (`...167041` → `...167042`); tokenId must therefore be rediscovered and is not a permanent application identifier.
- The static NFC UID is only a clonable prototype/demo identifier, not proof of possession or production security.
- Conclusion: ENSv2 is verified as load-bearing authorization state in the physical demo. The next product question is a persistent credential lifecycle that can survive after physical access expires rather than treating unregister/burn as the final model.

### Persistent physical credential milestone

- Chose a two-layer ENSv2 model: UserRegistry provides persistent ownership of `cred-001.demo-access.eth`, while PermissionedResolver `access.v1` independently controls whether the physical credential authorizes entry.
- Reused the verified PermissionedResolver at `0x0B723c0C2170F2ea508F2f4e3e122C6c0782744C` with UserRegistry `0x2d249472B83A453086254Acd8a42913D8e45a2Fd` under `demo-access.eth`.
- Initialized inactive access with `0xfdee98ad2bb5d646ae1a5ce940a858970759b2cb6bab0615cae57b89c00a4f69`, then registered the persistent credential with `0x60f6125afe15d6383728cbc4cf019926afe8e62927d41fb91e1fef54543d029a`.
- Activated access with `0xe2385b730a8ee48c7d9fbc7ecf862d5022ea8883997a56375f1d05738a066335` in block `11664354`, and deactivated it with `0x0e3ab168faa2fffe67bb0d0cda2222726fb58f8890bd90c1f82108bd0fce712b` in block `11664363`.
- Presented the same physical NFC tag (`91:2D:E3:06`) for the complete lifecycle and observed `DENY → ALLOW → DENY`, driven by `access.active: false → true → false`.
- Preserved REGISTERED status, owner `0x4C60a5AD311510543B56d0408872A52e4AEEe19C`, tokenId `111633085976721986886445685281703791217854403259346662013299173065803998167042`, resolver, and registry expiry `1820447664` throughout.
- Confirmed that rerunning persistent setup while ACTIVE was idempotent: it sent no transaction, did not re-register the credential, and did not reset access.
- Recovery/debugging lesson: reconstruct public nonces, receipts, resolver provenance, roles, and pinned-block state before retrying. Safe errors must retain stage, operation, custom error/RPC code, and any public transaction hash without exposing RPC credentials.
- Conclusion: ENSv2 is load-bearing for physical authorization, while the credential survives access revocation. Ownership lifetime and access lifetime are now separate.

### Interactive visual demo milestone

- Implemented a local Node.js 24 browser demo whose digital key reads authoritative ENSv2 state and invokes only fixed access actions for `cred-001.demo-access.eth`; the DEV key remains server-side.
- Early manual validation exposed duplicate-write risk around slow or failed RPC responses. Added semantic idempotency, a write lock, client in-flight protection, and pending-nonce guards.
- Established explicit pre- and post-submission boundaries: safe reads and simulation receive bounded retries, while submitted hashes are retained for read-only receipt/readback recovery and are never automatically resubmitted.
- A transient public Sepolia RPC failure produced an unhelpful pre-submission HTTP error. Structured public errors now identify the safe failing stage and whether no transaction was sent. A dedicated RPC was then selected through local `SEPOLIA_RPC_URL`; no endpoint or key is committed.
- Activation `0x40997865f355673a84e30c020f04631e2bac34ccefd91996a70bd70f1ebeed05`, nonce `16`, block `11668615`, produced `ACTIVE / ALLOW`. Its repeated action returned `changed:false`, `transactionHash:null`, and no additional transaction.
- Deactivation `0x5d0cfe3348509ae69ca719f8a0f1105d04a12445a10cd7a86ca0432419c428a9`, nonce `17`, block `11668637`, produced `INACTIVE / DENY`. Its repeated action returned `changed:false`, `transactionHash:null`; final latest/pending nonce was `18 / 18`.
- The same physical tag `91:2D:E3:06` produced `DENY -> ALLOW -> DENY`. Visual QA confirmed immediate clear transitions, understandable pending feedback, one click per action, no manual refresh, and a credential card that never disappeared.
- Identity remained REGISTERED with unchanged owner `0x4C60a5AD311510543B56d0408872A52e4AEEe19C`, tokenId `111633085976721986886445685281703791217854403259346662013299173065803998167042`, resolver `0x0B723c0C2170F2ea508F2f4e3e122C6c0782744C`, and registry expiry `1820447664`.
- Main technical learning: visible state must always follow authoritative ENSv2 readback, and writes need distinct pre-submission retry and post-submission hash-recovery boundaries.

### Targeted sponsor and architecture delta

- Reopened Workshop Radar only for a targeted delta scan; broad ideation remained closed.
- Reconfirmed ENS as the primary, load-bearing sponsor based on the already working ENSv2 Sepolia implementation.
- Reclassified Privy as a strong secondary candidate only if the product adds a real financial flow. Proposed coherent flow: embedded wallet/onboarding -> testnet USDC payment -> ENSv2 credential issuance. Embedded wallet/account abstraction by itself is not treated as sufficient prize qualification.
- Reclassified World as a strong secondary candidate if Selfie Check materially gates credential issuance or activation. Product language must treat Selfie Check as liveness/eligibility/continuity/abuse-prevention evidence, not as legal KYC.
- Confirmed that Arc is not required merely to use USDC or EIP-712 patterns. Keep the current Sepolia-centered architecture unless a concrete requirement justifies another chain.
- Identified the next security hardening target: a fresh EIP-712 challenge-response so the wallet/mobile signer proves control of the credential instead of relying on a clonable NFC UID.
- Chose the simplest signer path first: embedded EOA/mobile signer before ERC-4337 smart-wallet ownership. If smart-wallet ownership is added later, use ERC-1271/6492 verification standards.
- Identified existing platform/industry technology to reuse rather than rebuild: Android HCE/APDUs for mobile NFC presentation and Aliro as a post-hackathon commercial interoperability direction.
- Clarified that the ESP32 is the programmable door/access controller and the PN532 is the NFC radio/reader. NFC utility apps are debugging/tag tools, not replacements for an autonomous verifier that generates challenges, checks authorization, and actuates hardware.
- Clarified that the current PC bridge accelerates development/debugging but is not a permanent product requirement.
- No commercial price for a credential was selected. Any testnet USDC amount used in the demo is a payment-flow parameter, not a production pricing claim.
- Submission deadline reconfirmed from ETHGlobal email: Sunday, September 13 at 12:00 ET / 18:00 CEST. Demo-video work must begin before the final hours.

### Secure holder-proof Gate A milestone

- Clarified the canonical baseline: the full command `node --test --test-isolation=none` discovers 62 existing tests; the earlier 39 count covered only `scripts/ensv2/access-record.test.mjs`.
- Implemented a server-issued `ENSv2 Access` EIP-712 challenge with credential namehash, resource binding, verifier-generated random nonce, short expiry, and an in-memory `PENDING` / `CONSUMED` store.
- Verification reconstructs typed data from the stored challenge, recovers the signer, and compares it with the current ENSv2 credential owner at verification time.
- A valid current-holder proof is consumed exactly once before the existing `isAuthorized` ENS policy runs, including when that later policy returns DENY.
- Confirmed that a valid proof presented while access is inactive remains consumed: changing mocked access to active still returns `REPLAYED_CHALLENGE`, never ALLOW.
- Added 17 deterministic holder-proof tests; the complete suite now passes 79 tests.
- Added no dependency, accessed no real private key, and performed no blockchain write.
- Technical learning: authentication proof and authorization policy are separate. EIP-712 proves control of a wallet, while current ENSv2 ownership and `access.v1` remain authoritative for authorization.

## 2026-09-10

### Privy Android signing Gate B milestone

- Established the native Android skeleton and toolchain with application ID `io.github.nodexbit.ethonline2026`, then configured the matching Privy mobile client locally.
- Integrated `io.privy:privy-core:0.14.0` with ignored local App ID/App Client ID configuration; no App Secret entered mobile code or Git.
- Completed real-device native email OTP login on a Solana Seeker used only as a generic Android test device, then created or reused embedded Ethereum EOA `0x3419148731087b970d2059C53780163B452D5FF7`.
- Signed the exact Gate A-compatible `ENSv2 Access` EIP-712 test fixture through native `eth_signTypedData_v4`; Node/viem recovered the same public address.
- Confirmed `eth_signTypedData_v4` real-device support and passed the complete 82-test suite with zero blockchain writes.
- Used manual APK installation because wired ADB authorization on the Seeker never surfaced the RSA confirmation dialog. This was a workflow workaround, not an application security dependency.
- An early public-signature transfer introduced Cyrillic Unicode characters. Structural validation rejected it before cryptographic recovery; a byte-preserving transfer then passed. Public signatures are not secrets, but their transport must preserve exact bytes.
- Technical learning: SDK/API availability in source is insufficient evidence; signing support had to be exercised through the real provider on a physical device.

### Android HCE/APDU Gate C1 milestone

- Registered a thin Android `HostApduService` under proprietary, non-payment AID `F0454E5356324331` (`F0` + ASCII `ENSV2C1`, category `other`).
- Fixed APDU v1 around SELECT, SEND_CHALLENGE, GET_STATUS, and GET_SIGNATURE without changing Gate A/B cryptographic semantics.
- Defined the 104-byte binary challenge as `credential[32] || resource[32] || nonce[32] || expiresAt[8 unsigned big-endian]`; no JSON or identity metadata is transported over NFC.
- Defined the successful proof payload as exactly 65 signature bytes followed by ISO 7816 status `9000`.
- Implemented the independently testable `IDLE` / `PROCESSING` / `READY` / `ERROR` state machine outside the Android service adapter.
- Added a `ProofProvider` boundary: production Gate C1 performs no signing, while deterministic proof completion and the obvious 65-byte fixture exist only in JVM test code.
- A new challenge, SELECT, or HCE deactivation clears proof state. Generation binding prevents a stale asynchronous provider completion from overwriting a newer session.
- Android JVM tests and `:app:assembleDebug` passed; the complete Node suite remained green at 82/82.
- No phone-to-reader NFC exchange was performed or claimed. Gate C1 proves deterministic Android-side HCE protocol behavior only.
- Technical learning: NFC/HCE is only the transport layer. Replay security remains authoritative in Gate A, while cryptographic signing remains separated behind `ProofProvider` for Gate C2.

### Privy HCE signer Gate C2 milestone

- Reused one application-scoped Privy SDK instance and one shared `HceApduProcessor` for both the activity harness and Android `HostApduService`; no competing login/session state was introduced.
- Added the production `PrivyProofProvider`, which reads an existing authenticated embedded Ethereum wallet, reconstructs canonical Gate A typed data from `HceChallenge`, calls `eth_signTypedData_v4`, and accepts only an exact 65-byte result.
- Centralized the Android `ENSv2 Access` version `1`, Sepolia chain ID `11155111`, `AccessChallenge` typed-data builder. The APDU v1 AID, commands, 104-byte challenge, states, and proof format remain unchanged.
- Preserved asynchronous behavior: SEND_CHALLENGE returns in PROCESSING while signing continues, and the reader/harness polls GET_STATUS until READY or ERROR.
- Preserved C1 generation/session invalidation for a newer challenge, SELECT/reset, and HCE deactivation. Missing authentication/wallet, signing failure, or malformed output fails safely without creating a wallet.
- The manual in-app APDU harness exercised the production processor/provider path on a real Solana Seeker. Native `eth_signTypedData_v4` returned a 65-byte signature for public wallet `0x3419148731087b970d2059C53780163B452D5FF7`.
- Node/viem recovered `0x3419148731087b970d2059C53780163B452D5FF7` from that public proof (`MATCH: PASS`). The real signature was not stored in the repository.
- Android passed 21 JVM tests and `:app:assembleDebug`; the complete Node suite passed 83/83.
- No PN532 exchange occurred during Gate C2. Real phone-to-PN532 interoperability remains Gate D.
- Technical learning: asynchronous cryptographic signing and NFC transport are separated by the HCE state machine. SEND_CHALLENGE does not wait for network signing; the reader polls GET_STATUS until READY.

