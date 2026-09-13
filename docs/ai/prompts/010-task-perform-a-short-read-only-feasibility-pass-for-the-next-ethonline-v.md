# Project task packet 010: TASK — Perform a SHORT, READ-ONLY feasibility pass for the next ETHOnline vertical

- Classification: **ORIGINAL / SANITIZED**
- Redactions applied: none
- Source: retained project-specific Control Tower packet in the local Codex record

---

> TASK
>
> Perform a SHORT, READ-ONLY feasibility pass for the next ETHOnline vertical
> slice:
>
> Privy Android embedded EOA
> → fresh EIP-712 holder challenge
> → Android HCE
> → ESP32-S3 + PN532
> → Node verifier
> → ENSv2 owner + access state
> → physical ALLOW / DENY.
>
> TIMEBOX
>
> Maximum target: 60 minutes.
>
> Do not turn this into a complete architecture specification.
>
> The purpose is only to resolve the blockers required to start Gate A.
>
> DO NOT:
>
> * edit files
> * install anything
> * send blockchain transactions
> * inspect environment-file contents
> * commit
> * push
> * change NFC libraries
> * design unrelated future features
>
> WHY
>
> The current prototype already proves real:
>
> persistent ENSv2 credential
> → access state changes
> → physical NFC DENY → ALLOW → DENY.
>
> But the current static NFC UID is clonable.
>
> The next intended security boundary is cryptographic proof that the presenter
> controls the wallet that owns the ENSv2 credential.
>
> CURRENT CANONICAL REPO STATE
>
> Latest verified pushed checkpoint:
>
> 676e12766e3a1928664aba8b55000a6ffdd67b38
>
> Immediately after that push:
>
> git status -sb
>
> ## main...origin/main
>
> git rev-parse HEAD
> 676e12766e3a1928664aba8b55000a6ffdd67b38
>
> git rev-parse origin/main
> 676e12766e3a1928664aba8b55000a6ffdd67b38
>
> Treat this as canonical unless your new LOCAL read-only inspection shows
> otherwise.
>
> IMPORTANT PN532 CORRECTION
>
> The successful ESP32-S3 + PN532 I2C hardware spike did NOT use
> Adafruit_PN532.
>
> It uses Elechouse-compatible libraries:
>
> * PN532
> * PN532_I2C
>
> Adafruit_PN532 may exist locally but must NOT be used as evidence for the
> current implementation.
>
> Therefore:
>
> * inspect the exact Elechouse-compatible source/version actually used by the
>   validated firmware
> * determine actual relevant APDU / inDataExchange TX and RX limits
> * determine whether Android HCE / ISO-DEP exchange is viable with that exact
>   implementation
> * treat signature chunking as UNKNOWN until this inspection is complete
>
> Do NOT add chunking merely because an Ethereum signature is 65 bytes.
>
> If the actual transport fits the required proof safely, prefer the simpler
> protocol.
>
> If it does not, state the real constraint and only then recommend chunking.
>
> CURRENT PRODUCT DIRECTION
>
> Target secure flow:
>
> fresh verifier challenge
> → embedded wallet signs typed data
> → proof transported over NFC/HCE
> → verifier recovers signer
> → signer must match current ENSv2 credential owner
> → verifier checks current ENSv2 access state
> → ALLOW / DENY.
>
> Static NFC UID must not be an authorization factor in this future secure path.
>
> The current PC/Node process may remain the network/onchain verifier.
>
> Do not move RPC/ENS verification into ESP32 during this slice.
>
> PHASE 1 — REPO STATE
>
> Run read-only inspection.
>
> At minimum:
>
> git status -sb
> git rev-parse HEAD
> git rev-parse origin/main
>
> Inspect relevant project state from:
>
> * AGENTS.md
> * STATUS.md
> * PROJECT.md
> * DECISIONS.md
>
> Only read more files if required for the feasibility questions.
>
> Require either:
>
> HEAD == origin/main == 676e127...
>
> or explain precisely why current evidence differs.
>
> If workspace is dirty, ahead, behind or divergent:
>
> STOP.
>
> Do not modify anything.
>
> PHASE 2 — ANDROID TOOLCHAIN
>
> Read-only determine whether this Windows machine currently has enough tooling
> to start a native Android Kotlin prototype.
>
> Check only what matters:
>
> * Java/JDK
> * Android SDK
> * adb
> * Gradle / Android project tooling
> * Android Studio if discoverable without broad filesystem scanning
> * connected Android device if safely discoverable
>
> Do not install anything.
>
> Return:
>
> AVAILABLE
> MISSING
> UNKNOWN
>
> for each material requirement.
>
> Identify the minimum missing installation, if any.
>
> Do not propose unrelated tooling.
>
> PHASE 3 — PRIVY ANDROID SIGNING FEASIBILITY
>
> Determine, using current official documentation if needed, whether the native
> Privy Android SDK can support the minimum required path:
>
> authenticate user
> → embedded EVM EOA
> → obtain/use wallet provider
> → sign EIP-712 typed data
> → Node/viem can recover and verify the same address.
>
> We only need feasibility, not integration code.
>
> Resolve:
>
> * whether native Android is supported
> * whether embedded EVM wallet is supported
> * whether typed-data signing is supported
> * the relevant high-level API/mechanism
> * any authentication or wallet-creation prerequisite that materially affects
>   Gate B
>
> If documentation is ambiguous on a blocker, say UNKNOWN.
>
> Do not infer undocumented SDK behavior.
>
> PHASE 4 — ACTUAL ELECHOUSE PN532 / HCE FEASIBILITY
>
> Inspect the exact firmware and library used by the validated hardware spike.
>
> Relevant areas include the successful ESP32-S3 + PN532 I2C firmware and the
> actual PN532 / PN532_I2C source it includes.
>
> Determine only:
>
> 1. Which concrete PN532 implementation/version is actually compiled.
>
> 2. Whether it exposes the initiator/peer exchange necessary to communicate
>    with an Android HCE ISO-DEP target.
>
> 3. Actual relevant TX/RX buffer or command-size limits imposed by:
>
>    * the library
>    * its inDataExchange-style API
>    * or any obvious current firmware constraint.
>
> 4. Whether I2C transport changes feasibility.
>
> 5. Whether a likely EIP-712 proof can:
>
>    * fit in a straightforward exchange, or
>    * will require multiple APDUs/chunking.
>
> Do not fully design the APDU protocol.
>
> A minimal conclusion is enough:
>
> SINGLE EXCHANGE LIKELY VIABLE
>
> or
>
> CHUNKING REQUIRED
>
> or
>
> UNKNOWN — <specific reason>
>
> Do not switch libraries.
>
> PHASE 5 — MINIMAL EIP-712 V1
>
> Specify only the minimum typed challenge necessary to start Gate A.
>
> It must bind:
>
> * credential identity
> * resource/door identity
> * fresh unpredictable nonce
> * expiry
>
> Use Sepolia domain separation.
>
> Recommend the smallest sensible EIP-712 domain and primary type.
>
> For example, resolve:
>
> Domain:
>
> * name
> * version
> * chainId
> * whether verifyingContract is useful or unnecessary
>
> Message:
>
> * credential
> * resource
> * nonce
> * expiresAt
>
> Do not add fields without a concrete security need.
>
> Explain in a few lines:
>
> * how nonce is generated
> * how short the validity window should conceptually be
> * how verifier rejects replay
> * why signer must be compared with the current ENSv2 owner at verification
>   time
>
> Do not design production nonce infrastructure.
>
> PHASE 6 — GATE A GO / NO-GO
>
> Gate A will be pure Node work only:
>
> * create fresh EIP-712 challenge
> * sign with a deterministic local TEST key fixture
> * recover signer with viem
> * reject wrong signer
> * reject wrong resource
> * reject expired challenge
> * reject replayed nonce
> * compose signer verification with existing ENSv2 owner/access checks
>
> No Privy.
> No Android.
> No NFC.
> No real transaction.
>
> Determine whether the information gathered is sufficient to start Gate A
> without inventing unresolved protocol details.
>
> BIGGEST UNKNOWN
>
> Name exactly ONE biggest remaining risk after this pass.
>
> Do not generate a long risk register.
>
> STOP IF
>
> Stop the feasibility pass if any of these is established:
>
> * local repo is not clean/synchronized
> * native Privy Android cannot perform the required typed-data signing
> * the actual Elechouse PN532 implementation cannot plausibly communicate with
>   Android HCE / ISO-DEP
> * Android tooling is absent enough that starting the mobile path would require
>   an unexpectedly large setup
> * a fundamental constraint prevents fresh/replay-protected holder proof
>
> Do not redesign around the blocker during this task.
>
> RETURN
>
> # SECURE HOLDER PROOF FEASIBILITY
>
> ## REPO STATE
>
> Canonical expected checkpoint and actual local result.
>
> ## ANDROID TOOLCHAIN
>
> Only relevant available/missing components.
>
> ## PRIVY SIGNING
>
> PASS / STOP / UNKNOWN plus concise evidence.
>
> ## ELECHOUSE PN532 + HCE
>
> Exact library evidence, exchange capability and observed limits.
>
> Explicitly state:
>
> SIGNATURE CHUNKING:
> NOT REQUIRED / REQUIRED / UNKNOWN
>
> Do not cite Adafruit_PN532 limits as evidence.
>
> ## MINIMAL EIP712 V1
>
> Compact domain + type + replay model.
>
> ## BIGGEST UNKNOWN
>
> Exactly one.
>
> ## GATE A READINESS
>
> READY / BLOCKED
>
> Explain why in no more than a few bullets.
>
> ## NEXT IMPLEMENTATION TASK
>
> If READY, define ONLY the Gate A implementation task at a high level.
>
> Do not implement it.
>
> End exactly:
>
> FEASIBILITY: PASS
>
> or
>
> FEASIBILITY: STOP — <reason>
