# Project task packet 017: TASK — Implement and validate ONLY Gate C2:

- Classification: **ORIGINAL / SANITIZED**
- Redactions applied: none
- Source: retained project-specific Control Tower packet in the local Codex record

---

> TASK
>
> Implement and validate ONLY Gate C2:
>
> connect the already-proven native Privy Android EIP-712 signer to the existing
> Gate C1 HCE ProofProvider abstraction while preserving APDU v1 exactly.
>
> Do NOT modify PN532 firmware.
> Do NOT perform a blockchain transaction.
> Do NOT change Gate A cryptographic semantics.
>
> WHY
>
> Already proven independently:
>
> Gate A:
> server-issued ENSv2 Access EIP-712 challenge
> → recovered signer
> → CURRENT ENS owner
> → one-shot holder proof
> → existing ENS authorization.
>
> Gate B:
> real Android Privy embedded Ethereum EOA
> → eth_signTypedData_v4
> → Node/viem recovered the same public wallet.
>
> Gate C1:
> deterministic Android HostApduService/APDU v1
> → SEND_CHALLENGE
> → PROCESSING
> → READY
> → GET_SIGNATURE
> → exact 65-byte proof.
>
> Gate C2 must combine Gate B signing with Gate C1 transport semantics.
>
> CURRENT CANONICAL STATE
>
> Expected pushed checkpoint:
>
> 95303eab67a93b1929eacf1c702cf2a3864e3bd9
>
> Expected:
>
> HEAD == origin/main
> tracked working tree clean
>
> Local ignored files such as:
>
> mobile/android/privy.local.properties
>
> may exist and must remain ignored.
>
> Before editing verify locally:
>
> git status -sb
> git rev-parse HEAD
> git rev-parse origin/main
>
> If tracked state is dirty/ahead/behind/divergent:
> STOP.
>
> BASELINE
>
> Before editing require:
>
> Node:
>
> node --test --test-isolation=none
>
> Expected:
> 82 passed
> 0 failed
>
> Android:
>
> :app:testDebugUnitTest
> :app:assembleDebug
>
> Both must pass.
>
> If baseline differs materially:
> STOP.
>
> APDU V1 IS FROZEN
>
> Do NOT change:
>
> AID:
> F0454E5356324331
>
> Challenge wire format:
>
> credential[32]
> || resource[32]
> || nonce[32]
> || expiresAt[8 unsigned big-endian]
>
> Total:
> 104 bytes
>
> Application commands:
>
> SEND_CHALLENGE
> CLA 80
> INS 10
> P1 01
> P2 00
>
> GET_STATUS
> CLA 80
> INS 20
> P1 01
> P2 00
>
> GET_SIGNATURE
> CLA 80
> INS 30
> P1 01
> P2 00
>
> States:
>
> IDLE
> PROCESSING
> READY
> ERROR
>
> Proof:
>
> exactly 65 signature bytes.
>
> Do not add JSON to NFC.
> Do not add wallet address to proof payload.
> Do not add chunking.
> Do not redesign status words.
>
> GATE A EIP-712 IS ALSO FROZEN
>
> Domain:
>
> name:
> ENSv2 Access
>
> version:
> 1
>
> chainId:
> 11155111
>
> No verifyingContract.
>
> Primary type:
>
> AccessChallenge(
>   bytes32 credential,
>   bytes32 resource,
>   bytes32 nonce,
>   uint64 expiresAt
> )
>
> Gate C2 Android signing must reproduce this schema exactly.
>
> Do not create a similar-but-different mobile schema.
>
> PHASE 1 — INSPECT CURRENT ANDROID ARCHITECTURE
>
> Read the relevant current code completely before editing:
>
> - MainActivity / current Privy integration
> - Gate B typed-data construction
> - ProofProvider
> - HceChallenge
> - HceApduProcessor
> - GateC1HostApduService
> - HCE tests
> - Android manifest/application setup
>
> Determine:
>
> 1. how the Privy instance is currently initialized;
> 2. how authenticated user state is accessed;
> 3. how the embedded Ethereum wallet is reused;
> 4. current ProofProvider interface semantics;
> 5. how asynchronous completion enters HceApduProcessor;
> 6. Android process/lifecycle relationship between Activity and HostApduService.
>
> Do not guess.
>
> PHASE 2 — SINGLE PRIVY / SESSION ARCHITECTURE
>
> Create the smallest safe application-scoped mechanism that allows the HCE
> proof provider to use the already authenticated Privy user/wallet.
>
> Requirements:
>
> - do not initialize competing Privy instances unnecessarily;
> - do not duplicate login state;
> - do not persist OTP;
> - do not persist Privy auth tokens manually;
> - do not access/export private keys;
> - do not place App ID/Client ID into source;
> - existing ignored local configuration remains the only local config source.
>
> Prefer normal application-process state / SDK-supported authenticated session
> behavior.
>
> Do NOT add a backend simply to share wallet state.
>
> If HostApduService lifecycle makes the current approach unsafe or impossible,
> STOP and explain before broad redesign.
>
> PHASE 3 — PRIVY PROOF PROVIDER
>
> Implement a production Privy-backed ProofProvider.
>
> Conceptually:
>
> HceChallenge
> → canonical ENSv2 Access EIP-712 typed data
> → current embedded Privy Ethereum wallet
> → EthereumRpcRequest.ethSignTypedDataV4(...)
> → 65-byte signature
> → ProofProvider completion
>
> Requirements:
>
> - reuse Gate B's already-proven signing mechanism;
> - reuse or centralize the Android typed-data builder rather than copy/paste a
>   divergent JSON representation;
> - exact Gate A domain/type/field semantics;
> - no personal_sign;
> - no eth_sign;
> - no custom hashing/signature scheme;
> - no signing server;
> - no App Secret.
>
> The provider must fail safely when:
>
> - user is not authenticated;
> - no embedded Ethereum wallet exists;
> - Privy signing fails;
> - returned signature is malformed;
> - returned signature is not exactly 65 bytes.
>
> Do not silently create new wallets from the HCE service.
>
> Wallet creation/reuse remains an explicit user/app setup action.
>
> PHASE 4 — ASYNC / GENERATION SAFETY
>
> Privy signing is asynchronous.
>
> Preserve C1 semantics:
>
> SEND_CHALLENGE
> → returns immediately
> → PROCESSING
>
> Privy signing completes later.
>
> Then:
>
> PROCESSING → READY
>
> or:
>
> PROCESSING → ERROR
>
> Requirements:
>
> - SEND_CHALLENGE APDU must never wait synchronously for remote Privy signing;
> - an old signing completion must not overwrite a newer challenge/session;
> - SELECT reset must invalidate older in-flight completion;
> - HCE deactivation must invalidate older in-flight completion;
> - new SEND_CHALLENGE must invalidate previous proof/completion;
> - GET_SIGNATURE before READY remains rejected;
> - successful proof must still be exactly 65 bytes.
>
> Use the generation/session protection already introduced in C1 rather than
> creating another competing mechanism.
>
> PHASE 5 — APP READINESS FOR HCE
>
> The app must make it understandable whether secure HCE signing can currently
> work.
>
> Add only minimal status if required, for example:
>
> HCE signer:
> READY
>
> or:
>
> HCE signer:
> LOGIN REQUIRED
>
> or:
>
> HCE signer:
> CREATE WALLET FIRST
>
> Do not redesign the UI.
>
> Do not expose:
> - OTP
> - Privy tokens
> - App ID
> - App Client ID
> - App Secret
> - private key.
>
> The user should authenticate/create or reuse the Privy wallet through the
> existing Gate B UI BEFORE attempting HCE proof signing.
>
> PHASE 6 — MANUAL IN-APP APDU HARNESS
>
> Because PN532 is deliberately deferred and wired adb is unreliable, add the
> smallest TEST/DEMO harness inside the Android app that exercises the SAME
> production HceApduProcessor + PrivyProofProvider without real NFC hardware.
>
> Do not implement a second fake flow.
>
> The manual harness should feed the same APDU v1 sequence to the actual
> processor:
>
> SELECT AID
> → SEND_CHALLENGE
> → GET_STATUS
> → wait/poll
> → GET_STATUS
> → GET_SIGNATURE
>
> using a deterministic TEST-ONLY 104-byte challenge compatible with Gate A.
>
> This harness exists only to validate Gate C2 on the physical Android device
> before PN532 integration.
>
> It must not bypass ProofProvider or call Privy directly around the HCE
> processor.
>
> Prefer one simple UI action such as:
>
> "Run Gate C2 HCE signing test"
>
> The resulting UI should display safe public evidence only:
>
> - wallet public address
> - final HCE state
> - signature length
> - signature/public copy action if already available
> - PASS/FAIL
>
> Do not expose credentials.
>
> Do not simulate READY.
>
> READY must result from the real PrivyProofProvider.
>
> PHASE 7 — NODE INTEROPERABILITY
>
> The real 65-byte signature produced through:
>
> manual APDU harness
> → HceApduProcessor
> → PrivyProofProvider
>
> must be verifiable on Node with viem.
>
> Reuse Gate A/Gate B canonical typed-data definitions.
>
> Do not redefine EIP-712 independently.
>
> The deterministic Gate C2 manual challenge must be representable on Node
> exactly.
>
> Add the smallest verifier/test/vector support necessary so the real output can
> be checked locally.
>
> Final empirical evidence must include:
>
> PRIVY WALLET:
> 0x...
>
> HCE SIGNATURE LENGTH:
> 65
>
> RECOVERED SIGNER:
> 0x...
>
> MATCH:
> PASS
>
> The real captured signature must not be committed.
>
> PHASE 8 — UNIT TESTS
>
> Keep all Gate C1 tests green.
>
> Add deterministic tests for the new integration boundaries without requiring
> live Privy network access.
>
> At minimum:
>
> 1. HceChallenge → canonical EIP-712 field mapping exact.
> 2. expiresAt unsigned big-endian decoding maps correctly into typed-data value.
> 3. successful mocked async signer:
>    PROCESSING → READY.
> 4. malformed non-65-byte signer output:
>    PROCESSING → ERROR.
> 5. signer failure:
>    PROCESSING → ERROR.
> 6. old signer completion after new challenge cannot install stale proof.
> 7. old signer completion after SELECT reset cannot install stale proof.
> 8. old signer completion after deactivation cannot install stale proof.
> 9. GET_SIGNATURE returns the exact signer-produced 65 bytes.
> 10. unauthenticated/no-wallet provider path fails safely.
> 11. no wallet is auto-created by the HCE provider.
> 12. APDU v1 byte contract remains unchanged.
> 13. Gate C1 deterministic tests remain green.
>
> Do not make unit tests depend on real Privy credentials or Internet.
>
> PHASE 9 — BUILD VALIDATION
>
> Require:
>
> Android:
>
> :app:testDebugUnitTest
> :app:assembleDebug
>
> Node:
>
> node --test --test-isolation=none
>
> All existing 82 Node tests must remain green plus any new scoped tests.
>
> Also:
>
> - git diff --check
> - scoped status
> - secret/config scan
> - privy.local.properties ignored and unstaged
> - no real signature staged
> - no App Secret
> - no blockchain write
> - no PN532/firmware modification
>
> PHASE 10 — REAL DEVICE VALIDATION
>
> Gate C2 does NOT pass from unit tests alone.
>
> Use the existing manually installed APK workflow if adb remains unavailable.
>
> Do not spend time fixing adb.
>
> If a new APK must be installed:
>
> - build it
> - give the exact APK path/hash
> - instruct the user to transfer it by USB file transfer
> - user installs it manually on the Seeker.
>
> Manual user actions should be only:
>
> 1. open app
> 2. log in via existing email OTP flow if session is absent
> 3. create/reuse existing embedded Ethereum wallet
> 4. press "Run Gate C2 HCE signing test"
> 5. copy only public wallet/signature evidence if required
>
> The user must never paste:
> - OTP
> - App ID
> - Client ID
> - App Secret
> - tokens
> - private key
>
> After real signing:
>
> verify the signature locally with Node/viem.
>
> Gate C2 PASS requires actual Privy signing through the HCE ProofProvider path,
> not the standalone Gate B signing button.
>
> NO-TOUCH
>
> Do NOT modify:
>
> - firmware/
> - scripts/nfc/
> - demo/
> - ENS registry/resolver/deployment code
> - Gate A holder-proof semantics
> - APDU v1
> - project documentation during implementation
> - local Privy credential values
>
> Do NOT implement:
>
> - PN532 exchange
> - physical NFC validation
> - blockchain transaction
> - ENS credential issuance to Privy wallet
> - payment/USDC
> - World
> - ERC-4337
> - Aliro
> - transferability
> - loyalty
>
> Do not commit.
> Do not push.
>
> STOP IF
>
> Stop if:
>
> - repo baseline is inconsistent;
> - existing Gate B Privy signing mechanism no longer works;
> - HCE service cannot safely access an authenticated Privy signer without a
>   broad architecture redesign;
> - Privy requires user interaction for every signature in a way incompatible
>   with the proposed asynchronous HCE flow;
> - asynchronous signing cannot be isolated from stale sessions safely;
> - preserving APDU v1 requires changing Gate A/B cryptographic semantics;
> - any private key/App Secret is required;
> - scope reaches PN532/physical NFC.
>
> EVIDENCE REQUIRED
>
> Return:
>
> # GATE C2 PRIVY HCE SIGNER
>
> ## BASELINE
> - HEAD
> - origin/main
> - status
> - Node baseline
> - Android baseline
>
> ## FILES CHANGED
>
> ## PRIVY SESSION ARCHITECTURE
>
> Explain how Activity/HCE service share safe authenticated signer access.
>
> ## CANONICAL TYPED DATA
>
> Confirm exact Gate A compatibility.
>
> ## PRIVY PROOF PROVIDER
>
> Explain:
> HceChallenge
> → typed data
> → eth_signTypedData_v4
> → 65 bytes.
>
> ## ASYNC SAFETY
>
> Explain generation/session invalidation.
>
> ## MANUAL APDU HARNESS
>
> Confirm it uses the actual production:
> HceApduProcessor + PrivyProofProvider
>
> and does not bypass the HCE protocol.
>
> ## UNIT TESTS
>
> List required cases PASS/FAIL.
>
> ## ANDROID BUILD
>
> ## NODE TESTS
>
> ## PHYSICAL DEVICE VALIDATION
>
> Report real Seeker result.
>
> Explicitly include:
>
> eth_signTypedData_v4:
> SUPPORTED / FAILED
>
> PRIVY WALLET:
> 0x...
>
> HCE SIGNATURE LENGTH:
> ...
>
> RECOVERED SIGNER:
> 0x...
>
> MATCH:
> PASS / FAIL
>
> ## SECURITY
>
> Confirm:
> - no UID authorization
> - no fake production signer
> - no private key
> - no App Secret
> - no OTP/token exposure
> - no blockchain writes
> - no PN532 changes
>
> ## WHAT THIS PROVES
>
> State exactly:
>
> Gate C2 proves that a real Privy embedded EOA can asynchronously produce the
> existing Gate A-compatible holder proof through the same HCE/APDU state
> machine that will later be driven by the physical PN532 reader.
>
> It does NOT yet prove phone-to-PN532 NFC interoperability.
>
> ## GIT STATE
>
> Do not commit.
>
> ## NEXT TASK
>
> Recommend ONLY Gate D:
>
> implement the minimal ESP32 + Elechouse PN532 APDU reader path and prove a real
> SELECT / SEND_CHALLENGE / GET_STATUS / GET_SIGNATURE exchange with the
> Seeker.
>
> End exactly:
>
> GATE C2: PASS
>
> or
>
> GATE C2: STOP — <reason>
