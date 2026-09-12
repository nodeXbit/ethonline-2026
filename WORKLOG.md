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

### Physical PN532/HCE transport Gate D milestone

- Added one-shot ESP32-S3 firmware using the exact installed Elechouse-compatible `PN532` / `PN532_I2C` stack over I2C on SDA GPIO17, SCL GPIO18, address `0x24`, with Serial0 at 115200 and PN532 firmware 1.6.
- Activated the Seeker as a real ISO14443A/ISO-DEP target and selected proprietary AID `F0454E5356324331`.
- Transported the exact deterministic Gate C2 104-byte binary challenge, observed asynchronous Privy signing through `PROCESSING -> READY`, and retrieved the exact 65-byte signature plus `9000`.
- Node/viem recovered `0x3419148731087b970d2059C53780163B452D5FF7`, matching the embedded Privy wallet in both successful physical sessions.
- Completed 2/2 full physical sessions with SEND_CHALLENGE sent once per session. The full 67-byte response is physically viable and no signature chunking is required.
- An earlier GET_SIGNATURE exchange failed once. Android telemetry later proved receipt in READY and production of 67 bytes ending `9000`; the unchanged firmware then succeeded twice, so the failure is classified transient/not reproduced.
- Temporary Android capability and transport telemetry was reverted after diagnosis; production Gate C1/C2 behavior and APDU v1 remain unchanged.
- Final validation passed firmware compilation, Android 21/21 JVM tests and debug assembly, and Node 83/83.
- Technical learning: do not redesign a protocol around one transient hardware failure. Instrument the failing boundary first; successful full-size reproduction proved the transport itself was viable.

### Gate E secure authorization implementation checkpoint

- Implemented the Gate E secure serial bridge: Node waits for physical target activation, successful AID SELECT, and `WAITING_CHALLENGE` before issuing a fresh Gate A challenge.
- Node sends the canonical 104-byte `credential[32] || resource[32] || nonce[32] || expiresAt[8 big-endian]` challenge to the firmware; the firmware returns an exact 65-byte proof to Node.
- One `IssuedChallengeStore` remains alive in the same Node process from issuance through existing Gate A verification and optional replay validation.
- Reused the existing coherent pinned-block `readCredential` snapshot and existing `isAuthorized` policy; current owner, registration, expiry, resolver provenance, `access.v1`, and authorization timestamp are not reimplemented.
- Added fail-closed serial/RPC handling, one-shot firmware behavior, duplicate-message protection, proof timeout handling, and `--check-replay` support.
- Added scoped credential label/owner parameters for `guest-001.demo-access.eth` and intended owner `0x3419148731087b970d2059C53780163B452D5FF7` without changing `cred-001` defaults or lifecycle.
- Added 16 tests across the Gate E bridge and separate-credential provisioning boundary. The complete Node suite passes 99/99.
- Android production code remained unchanged and passed 21/21 JVM tests plus debug assembly. Both Gate D and Gate E firmware sketches compile with the existing ESP32/PN532 toolchain.
- Performed zero blockchain writes. No live Gate E credential has been provisioned and no physical Gate E authorization has been run.
- Technical learning: transport identity and authorization remain separate. NFC transports a fresh signed proof; Gate A verifies wallet control; ENSv2 determines current credential ownership and access.
- Challenge issuance is intentionally delayed until the phone has activated and the application AID has been selected, preserving most of the short challenge TTL.

### Gate E provisioning and INACTIVE physical validation milestone

- Provisioned `guest-001.demo-access.eth` previously as a separate REGISTERED credential owned by Privy wallet `0x3419148731087b970d2059C53780163B452D5FF7`, without modifying `cred-001`.
- The provisioning writes were `setData` transaction `0x34c47584a377bf6d77428d19a946a322d9d31fb040caf2bf40dc205bf97112bf` and `register` transaction `0x964e488bb056b86a72870251a46e91f569f4915fc102f2aeb573b71ae202f5da`.
- Completed one controlled physical INACTIVE run: TARGET activation and SELECT passed; Node issued one fresh challenge; the firmware transported one 104-byte challenge and sent SEND_CHALLENGE exactly once; Android progressed `PROCESSING -> READY`; GET_SIGNATURE returned one 65-byte proof.
- Node recovered the current ENS owner, consumed the challenge, read `access.active == false`, and returned `ACCESS_DENIED`. NFC UID was not used.
- The first successful INACTIVE proof exposed a serial-finalization race: Node sent `AUTHORIZATION=DENY` but detached before the firmware terminal line could be captured reliably.
- Fixed only the Node serial lifecycle. It now sends the authorization command exactly once, waits up to two seconds for the exact matching `AUTHORIZATION: ALLOW` or `AUTHORIZATION: DENY` firmware line, fails closed on an opposite result or timeout, and closes serial afterward. It does not issue another challenge, repeat NFC/signing/proof verification, reread ENS, or resend authorization.
- Repeated the bounded physical INACTIVE validation after the fix. Node sent `AUTHORIZATION=DENY` once, firmware emitted `AUTHORIZATION: DENY`, and the bridge captured it before COM4 closed.
- Re-verifying the same proof in the same challenge store returned `REPLAYED_CHALLENGE` / DENY without another challenge, NFC operation, signature, ENS read, or blockchain write.
- Validation passes Node 104/104, scoped Gate E 20/20, Android 21/21, Android `assembleDebug`, and Gate E firmware compilation. The physical validation itself performed zero blockchain writes.
- GATE E INACTIVE PHYSICAL: PASS. GATE E ACTIVE PHYSICAL: PENDING.
- Immediate P0: `guest-001` has `access.validUntil = 1789107864` (2026-09-11 08:24:24 Europe/Madrid), while current tooling cannot safely renew an existing REGISTERED INACTIVE record and preserve INACTIVE.

### Batch A — deterministic readiness checkpoint

- Added an explicit, credential-scoped renew-inactive operation. Administrative validity extension is no longer hidden inside activate/deactivate; sufficient validity is a semantic no-op and ACTIVE credentials are refused.
- Proved from control flow and tests that read-only preflight may read and simulate but cannot call `writeContract`, enter transaction-hash recovery, broadcast, or increment the account nonce. No live `guest-001` renewal was executed.
- Kept Gate A's 60-second challenge TTL while bounding the complete Gate E lifetime after challenge issuance to 50 seconds, including an eight-second ENS budget and two-second firmware-confirmation budget.
- Terminal state and deadline guards prevent a late proof, late ENS result, or already-computed late ALLOW from escaping after the total deadline.
- Firmware-local STOP now terminates Node immediately, physical-proof content is redacted, and the serial parser permanently poisons an attempt after its 1,024-byte pre-newline bound is exceeded.
- RPC recovery uses at most one optional fallback and restarts the entire coherent pinned-block snapshot on that provider; it never mixes owner, resolver, or policy reads across providers.
- Fresh validation passed Node 134/134, scoped Gate E 42/42, Android 21/21 plus debug assembly, and Gate E firmware compilation. Android source, Gate A, Gate D, APDU v1, and established `cred-001` behavior remained unchanged.
- GATE E INACTIVE PHYSICAL: PASS. GATE E ACTIVE PHYSICAL: PENDING. This checkpoint performed zero blockchain writes and zero physical NFC attempts.

### Gate E ACTIVE end-to-end physical validation

- Renewed only `guest-001` validity while it remained INACTIVE in transaction `0xecbf343cc3c6a26a599bc46c789d273e33354b950767765c27b4dba0b0508e4e` at nonce 20. The write preserved credential identity and `cred-001`, setting `access.validUntil = 1793487599` (2026-10-31 23:59:59 Europe/Madrid).
- Activated `guest-001` in transaction `0xafd5cb1c2ebe849ff65ed934ab2fb9da5eb1fe2df40370c3b1d43b25b97fa506` at nonce 21. The write preserved the renewed deadline and produced authoritative ACTIVE/ALLOW policy.
- Flashed the committed Batch A firmware, including public `GATE_E: STOP - <STAGE>: <PUBLIC_REASON>` telemetry before firmware-local DENY, and verified a complete cold boot: all power removed, CH343 disappearance observed, approximately ten seconds unpowered, established CH343/COM4 reconnected, and one RST/EN press required after passive capture produced no output. I2C `0x24` ACK, PN532 query PASS/version 1.6, `GATE_E_READY`, and `PRESENT_SEEKER` followed.
- Completed one controlled physical ACTIVE attempt. TARGET activation and SELECT passed; Node issued exactly one fresh Gate A challenge for `guest-001.demo-access.eth` and `demo-access.eth:door-001`; one 104-byte payload traversed HCE/ISO-DEP; Android progressed `PROCESSING -> READY`; GET_SIGNATURE returned one 65-byte proof whose raw value remained redacted.
- Gate A recovered `0x3419148731087b970d2059C53780163B452D5FF7`, matched the current ENS owner, consumed the challenge, and evaluated a fresh coherent PRIMARY-provider snapshot as `VERIFIER_ALLOW` inside the Batch A freshness and deadline budgets. No fallback RPC was configured.
- Node wrote `AUTHORIZATION=ALLOW` exactly once. Firmware subsequently emitted the matching `AUTHORIZATION: ALLOW`; the controller result was `CONTROLLER_CONFIRMED`, and serial closed only after confirmation.
- The same consumed proof returned `REPLAYED_CHALLENGE` / DENY in the same process with zero new challenges, NFC operations, signatures, ENS reads, or blockchain writes.
- The physical ACTIVE run itself performed zero blockchain writes. DEV nonce remained 22/22 with no pending transaction; `guest-001` remained ACTIVE/ALLOW and `cred-001` identity remained intact.
- Technical lessons: physical authorization is not proven until controller confirmation; ACTIVE policy alone is not holder possession; NFC transports proof while Node performs blockchain authorization; whole-snapshot freshness and attempt deadlines reject stale or late ALLOW; full cold boot is currently an operational reliability measure, not a proven fix for intermittent PN532/I2C/ISO-DEP failures.

### Batch B — demo reliability final checkpoint

- Reviewed all 16 implementation files and found no P0/P1 regression requiring a fix. Implementation commit: `8a1b4bd52b25e988a4d39e630d1839860d78808c` (`feat: add deterministic demo reliability workflow`).
- Added canonical guest-001 configuration, read-only preflight with optional passive boot observation, CH343-discovering demo bridge/replay commands, sanitized advisory runtime reports, and `DEMO_RUNBOOK.md`.
- Aligned demo activate/deactivate with guest through the existing safe ENS transaction paths. Preserved generic cred-001 defaults, Gate A/ENS authorization, Batch A deadlines/freshness, exact controller confirmation, APDU v1, firmware and Android.
- Separated ENSv2 POLICY, HOLDER VERIFIER and PHYSICAL CONTROLLER in the UI, with replay and SYSTEM READINESS. A small HTML/CSS refinement keeps the three primary cards together on desktop and places policy-only clarification beside ACTIVE/POLICY ALLOW.
- Final validation from the resulting implementation tree: `node --test --test-isolation=none` passed 163/163; targeted demo/server/reliability tests passed 52/52; `git diff --check` passed. Previous Batch B Android 21/21 and `assembleDebug` PASS were retained without rebuilding unchanged sources. Firmware remains unchanged from the validated Gate E checkpoint.
- Final user-supplied ChatGPT Work browser validation: PASS at 1366x900 normal zoom. Guest name and all three cards were readable together; controller stayed NOT RUN without evidence, recovered signer showed an em dash, and no proof/signature bytes were visible. No button was clicked and browser review modified no files/data.
- Security review confirmed ignored runtime paths, no runtime files or local configuration staged, no real proof/signature or secrets added, and no fabricated physical-success evidence. Staged implementation content matched the reviewed tree exactly.
- Batch B implementation/review used zero blockchain writes, zero NFC attempts and zero firmware flashes; no dependency installation or public deployment. The earlier authoritative read-only Batch B API observation was guest ACTIVE/ALLOW through `1793487599`; checkpoint review did not query or mutate onchain state.
- BATCH B — DEMO RELIABILITY: PASS. Next objective: LIVE DEMO REHEARSAL. No physical rehearsal has yet been executed with the finalized Batch B UX; Batch B stays closed unless rehearsal reveals a real blocker.

## 2026-09-11

### Mobile issuer admission M1 closure

- Funded dedicated Privy issuer `0xFa90e8301A22833B74378C5fA3a7c120Ac512685` in Sepolia transaction `0x388a8817a207ebeea6cfc4e2d7d3573f97522cebc31c44d866ad63cd0a929b53`.
- Physically submitted exactly one zero-value M1 self-transaction: `0x6c4f42f2d368936d4aaf7edf3e0395c376f92b699563b34fee4ed053a0a53e32`, confirmed in block `11683226`; issuer nonce advanced from `0` to `1`.
- Added an operation-scoped persistent transaction journal and explicit review/submission state machine. `SUBMITTING_NO_HASH` is durable before provider send, returned hashes are persisted immediately, ambiguous outcomes reconcile without blind resubmission, and re-arm requires read-only proof that no broadcast occurred.
- Split Android provider responsibilities: Privy handles authentication, embedded-wallet signing, chain switching and writes; a credential-free HTTPS Sepolia client handles allowlisted scalar/object reads with bounded timeouts, bounded responses and sanitized errors.
- The physical app restarted with the existing M1 hash, reconciled transaction/receipt/nonces through the public read client, and displayed `Confirmed`. No second M1 operation or transaction was created.
- M1 transaction transport, persistent recovery and public read-only reconciliation: PASS. No ENS write, NFC run or firmware flash occurred during M1.
- The existing `guest-001` physical fallback remains untouched. The next architecture target is the isolated `keys.demo-access.eth` namespace with R1 and S1; it was not started in this checkpoint.

### ENSv2 issuer namespace bootstrap

- Added and checkpointed the deterministic read-only issuer bootstrap planner at commit `35ff5ea13e00f0538f7d2d93356edf0abe5bb271` before execution.
- Deployed R1 UserRegistry `0x0AeB395be893149c0b60D9DAC3Ba55139D0dcA1a` in transaction `0x632c6d6f9c04a381f763013bf304d30c15bcccc440b41df9350829cf6e952253`, block `11683691`. Factory provenance resolves to UserRegistryImpl, issuer root roles match the approved REGISTRAR/RENEW bitmap, and DEV root roles are zero.
- Deployed S1 PermissionedResolver `0x20766FB21498a99350F922ee3163F5a95F354a7f` in transaction `0x2e6e8f830527df1aa384fe09c0a51f27c8e8d7e2b0b3862a9c646aa29d38d94c`, block `11683692`. Factory provenance resolves to PermissionedResolverImpl, issuer root roles match the approved SET_TEXT/SET_DATA bitmap, DEV root roles are zero, and initializer setters were empty.
- Registered `keys.demo-access.eth` in transaction `0x5997d2a1ccbaf146de00dbddef79c49f00fedefd02fdb2399386910a1840230b`, block `11683693`, owned by the issuer with R1/S1 pointers, expiry `1814392799`, and ROLE_RENEW only.
- Exactly three authorized blockchain writes occurred, sequentially with receipt/readback verification and no retry. DEV nonce advanced `25 -> 28`; issuer nonce remained `1` because it performed no bootstrap write.
- Fresh post-write verification preserved `demo-access.eth -> R0`, S0 provenance, `guest-001`, and `cred-001`. Android/HCE, Gate E, firmware, and the Node verifier were unchanged.
- Added public authoritative issuer namespace configuration and a read-only drift/onchain verifier. No RPC URL, key, Privy data, local path, or credential was added.
- Next objective: design and separately authorize one complete `staff-001.keys.demo-access.eth` credential vertical. No staff/visitor/contractor credential or final issuer UI exists yet.

## 2026-09-12

### First Android-issued credential vertical checkpoint

- Built and physically completed the Android issuer flow for `staff-001.keys.demo-access.eth`, with My Keys, issuer capability gating, a product/diagnostics separation, explicit two-step review, persistent transaction recovery, and authoritative final readback.
- TX1 registration `0x8858c291f14659ea8e323d1af988f4ee379c2a14dc5e2cbed4db592a04e6db58` confirmed in block `11684957`. TX2 configuration `0x57a5a4bf81fcede947b83bf55dcabe065540a1254fb2d9eb10677d391cf7bb11` confirmed in block `11685150`. Issuer nonce finished at `3 / 3`.
- Final onchain state is REGISTERED to `0x3419148731087b970d2059C53780163B452D5FF7`, with S1 resolver, zero subregistry, expiry `1793487599`, owner roles `0`, non-transferability, description `Staff Access Pass`, no avatar record, and active `access.v1` through `1793487599`.
- The holder physically logged in with the owner wallet and My Keys displayed STAFF ACCESS, the full ENS name, ALLOWED, the 31 Oct 2026 validity, non-transferability, and description. Android recovered to final issuance state `READY`.
- Two recovery incidents established general safeguards: reviews must be pure until their final CTA; already-confirmed operations and issuance transitions must be idempotent; unset `access.v1` is valid between TX1 and TX2; and final readiness must use bounded authoritative readback at or after the confirmed receipt block.
- Final checkpoint review found no unrelated HCE, Node, firmware, or contract changes; no secret, RPC credential, auth token, raw signed transaction, or private key was added. Blank artwork remains omitted in production, and ambiguous/confirmed operations cannot blindly resend either registration or configuration.
- Final validation passed Android JVM tests 139/139, Node tests 173/173, Android debug assembly, `git diff --check`, and a fresh read-only Sepolia reconciliation at block `11685453`.
- Next product P1 is automatic active-wallet discovery inside the known R1 namespace: bounded event scan for candidates, fresh authoritative ownership/state readback, wallet partitioning, automatic My Keys refresh, and manual **Add by ENS name** as a secondary recovery flow. It is not implemented.
- Later product direction: stacked wallet-style passes, selected-pass NFC activation, real artwork, startup Privy session hydration, and configurable issuance templates.

