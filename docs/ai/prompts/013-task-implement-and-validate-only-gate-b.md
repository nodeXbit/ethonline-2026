# Project task packet 013: TASK — Implement and validate ONLY Gate B:

- Classification: **ORIGINAL / SANITIZED**
- Redactions applied: personal filesystem path
- Source: retained project-specific Control Tower packet in the local Codex record

---

> TASK
>
> Implement and validate ONLY Gate B:
>
> native Android Privy embedded EOA
> → sign the exact ENSv2 Access EIP-712 v1 challenge
> → Node/viem recovers the signer
> → recovered address equals the Privy wallet address.
>
> No HCE.
> No NFC.
> No blockchain transaction.
> No ENS state change.
>
> WHY
>
> Gate A already proves:
>
> server-issued EIP-712 challenge
> → current-holder signature verification
> → one-shot consumption
> → existing ENSv2 authorization
> → ALLOW / DENY.
>
> Gate B must resolve one remaining empirical unknown:
>
> Does the current native Privy Android SDK/provider support
> eth_signTypedData_v4 for an EmbeddedEthereumWallet?
>
> CURRENT VERIFIED STATE
>
> Latest confirmed pushed Gate A checkpoint:
>
> 1e5d1b1ea7b52f95508b46328d5712f9d0b5402f
>
> Gate A:
> 79 tests passed.
>
> A validated Android skeleton was subsequently created under:
>
> mobile/android/
>
> with:
>
> applicationId:
> io.github.nodexbit.ethonline2026
>
> namespace:
> io.github.nodexbit.ethonline2026
>
> display name:
> ENSv2 Access Demo
>
> minSdk:
> 28
>
> targetSdk:
> 37
>
> The skeleton previously built successfully with :app:assembleDebug.
>
> IMPORTANT
>
> Its commit/push status has not been reported back to Control Tower.
>
> Therefore inspect the LOCAL repository first and resolve actual state from
> evidence.
>
> PRIVY DASHBOARD ALREADY CONFIGURED
>
> The user has created a Privy development app.
>
> The existing Default mobile app client has allowed app identifier:
>
> io.github.nodexbit.ethonline2026
>
> Email login is enabled.
>
> The user has access locally to:
>
> - Privy App ID
> - Privy mobile App Client ID
>
> The App Secret must NOT be used by Android.
>
> Do not ask the user to paste any Privy value into ChatGPT or this Codex chat.
>
> OFFICIAL PRIVY BEHAVIOR ALREADY VERIFIED
>
> Current Privy Android documentation states:
>
> - Android API 28+ required
> - Kotlin 2.1+ required
> - SDK is available from Maven Central
> - Privy initializes with PrivyConfig(appId, appClientId)
> - authenticated PrivyUser can call createEthereumWallet()
> - EmbeddedEthereumWallet exposes:
>   address
>   provider.request(EthereumRpcRequest)
> - provider.request is a generic Ethereum-style JSON-RPC request interface
>
> Official Android examples explicitly show personal_sign.
>
> Native Kotlin eth_signTypedData_v4 support remains empirically UNKNOWN.
>
> Do not assume success.
>
> PHASE 0 — REPOSITORY / SKELETON CHECKPOINT
>
> Run read-only:
>
> git status -sb
> git rev-parse HEAD
> git rev-parse origin/main
>
> Inspect mobile/android/.
>
> CASE A
>
> If the Android skeleton is already committed and:
>
> HEAD == origin/main
>
> continue.
>
> CASE B
>
> If HEAD == origin/main == 1e5d1b1... and the only uncommitted/untracked
> changes are exactly the previously validated mobile/android/ skeleton:
>
> 1. review the skeleton scope;
> 2. rerun its debug build;
> 3. confirm no credentials/secrets/build outputs are staged;
> 4. create a local checkpoint commit:
>
> feat: add native Android Gate B skeleton
>
> 5. push that skeleton commit to origin/main;
> 6. verify HEAD == origin/main and working tree clean;
> 7. continue Gate B.
>
> The user explicitly authorizes commit/push ONLY for the already validated
> Android skeleton in this case.
>
> Do not include Gate B Privy integration in that checkpoint.
>
> If repository state differs materially from A or B:
>
> STOP.
>
> PHASE 1 — PRIVY DEPENDENCY
>
> Inspect the current official Privy Android installation documentation and Maven
> metadata.
>
> Add only the official current stable Android dependency required for Privy.
>
> Expected artifact family:
>
> io.privy:privy-core
>
> Do not guess the version.
>
> Resolve and pin the current stable version from authoritative package metadata.
>
> Use:
> google()
> mavenCentral()
>
> Do not add unrelated wallet/web3 libraries.
>
> Do not add Compose just for Privy.
>
> Add Android INTERNET permission if required for Privy API access.
>
> No App Secret.
>
> PHASE 2 — LOCAL PRIVY CONFIGURATION
>
> Create a safe local configuration mechanism for:
>
> PRIVY_APP_ID
> PRIVY_APP_CLIENT_ID
>
> Requirements:
>
> - actual values must NOT be committed
> - actual values must NOT be printed in logs
> - App Secret must never be requested or loaded
> - committed repository may contain only variable names/placeholders/example
>   documentation if necessary
>
> Prefer a small ignored file under mobile/android/, for example:
>
> privy.local.properties
>
> with a committed safe example if useful:
>
> privy.local.properties.example
>
> The real local file should contain only:
>
> PRIVY_APP_ID=<user value>
> PRIVY_APP_CLIENT_ID=<user value>
>
> Wire those values into Android build/runtime configuration without exposing
> them through logs.
>
> App ID and Client ID are client configuration identifiers, not wallet private
> keys, but keep real project values local for this spike.
>
> If user input is needed:
>
> STOP at a clear manual checkpoint and tell the user exactly:
>
> 1. which file to open;
> 2. the two variable names to fill;
> 3. where each value is found in Privy;
> 4. that App Secret must NOT be entered.
>
> Do not inspect or echo the resulting values afterward.
>
> PHASE 3 — MINIMAL ANDROID UI
>
> Keep the UI deliberately primitive.
>
> It only needs enough controls/state for Gate B:
>
> - email address input
> - Send code
> - OTP code input
> - Log in
> - Create/reuse Ethereum wallet
> - display wallet address
> - Sign Gate B challenge
> - display PASS/FAIL/status
>
> Do not polish branding.
>
> Do not implement payment.
>
> Do not implement passkeys/MFA yet.
>
> Do not implement HCE/NFC.
>
> Use Kotlin/coroutines in the smallest conventional way.
>
> Initialize exactly one Privy instance using the Application context and on the
> main thread as required by current Privy docs.
>
> PHASE 4 — EMAIL AUTHENTICATION
>
> Implement the native Privy email OTP flow using the current official Android
> SDK APIs.
>
> Do not invent method names.
>
> Inspect the installed SDK/current docs and use the actual API.
>
> Requirements:
>
> email
> → send OTP
> → user manually enters OTP
> → authenticated Privy user.
>
> Do not automate or bypass OTP.
>
> Never log:
> - OTP
> - access token
> - refresh token
> - auth token
>
> Safe user-visible failures only.
>
> PHASE 5 — EMBEDDED EOA
>
> After authenticated state:
>
> - reuse an existing embedded Ethereum wallet if one exists;
> - otherwise call the official createEthereumWallet flow;
> - do not create additional wallets unnecessarily.
>
> Display only the public Ethereum address.
>
> No private-key export.
> No key material access.
>
> Record the public address for Gate B evidence.
>
> PHASE 6 — EXACT GATE B EIP-712 TEST VECTOR
>
> Gate A schema is immutable for this task.
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
> For Gate B only, use a deterministic TEST VECTOR so Android and Node can
> independently construct exactly the same typed data.
>
> This test vector is NOT an authorization challenge and must never be used by
> the live Gate A challenge store.
>
> Use clearly TEST-ONLY constants for:
>
> - credential bytes32
> - resource bytes32
> - nonce bytes32
> - expiresAt
>
> The purpose is only to prove Android signing interoperability.
>
> Keep the schema/encoding exactly compatible with Gate A.
>
> Do not weaken or change Gate A.
>
> PHASE 7 — eth_signTypedData_v4 EXPERIMENT
>
> This is the core of Gate B.
>
> Using the actual Privy EmbeddedEthereumWallet.provider.request API, attempt the
> standard EIP-712 JSON-RPC request:
>
> method:
> eth_signTypedData_v4
>
> with the standard Ethereum parameter semantics for:
>
> - wallet address
> - canonical typed-data JSON
>
> Inspect the actual EthereumRpcRequest API from the installed Privy SDK.
>
> Do not assume constructors/helpers that do not exist.
>
> Do NOT silently replace this with:
>
> personal_sign
> eth_sign
> a custom hash/signature
> server-side signing
>
> Those do not satisfy Gate B.
>
> If the SDK/provider rejects or does not support eth_signTypedData_v4:
>
> capture safe evidence:
> - method attempted
> - public error classification/message
> - SDK version
>
> Then STOP.
>
> That is a legitimate Gate B result.
>
> Do not redesign the project during this task.
>
> If it succeeds:
>
> obtain the normal public ECDSA signature.
>
> Sign exactly once unless a retry is explicitly needed because the first call
> failed before producing a signature.
>
> PHASE 8 — NODE/VIEM VERIFICATION
>
> Add the smallest Node verification fixture/script/test needed under the
> existing security area.
>
> Prefer reusing Gate A typed-data definitions rather than copying a divergent
> schema.
>
> The Node side must:
>
> 1. construct the exact same Gate B test typed data;
> 2. receive/use the signature produced by the Android Privy EOA;
> 3. recover signer with viem recoverTypedDataAddress;
> 4. compare recovered signer with the public Privy wallet address.
>
> Required final evidence:
>
> PRIVY WALLET:
> 0x...
>
> RECOVERED SIGNER:
> 0x...
>
> MATCH:
> PASS
>
> Do not commit the generated signature as permanent project data unless there
> is a strong reason.
>
> A temporary local evidence file or stdin/CLI argument is preferable and must
> be ignored if created.
>
> The signature is public proof data, not a private key, but avoid unnecessary
> permanent test artifacts.
>
> PHASE 9 — DEVICE VALIDATION
>
> Gate B requires a REAL Android-device run.
>
> Do not claim PASS from compilation only.
>
> If no device is currently connected:
>
> finish safe implementation/build first, then return a manual checkpoint telling
> the user exactly how to connect their Android device and authorize USB
> debugging.
>
> Use the existing adb at:
>
> [REDACTED: personal filesystem path]
>
> Do not modify global PATH just for this.
>
> Once a device is visible:
>
> - install debug APK through standard adb/Android Studio tooling
> - user performs email/OTP interaction manually
> - user triggers wallet creation/reuse
> - user triggers typed-data signing
>
> Codex may inspect only safe logs/output required for validation.
>
> Do not collect unrelated device information.
>
> Do not print auth tokens or Privy configuration.
>
> PHASE 10 — TESTS / VALIDATION
>
> Require:
>
> Android:
> - Gradle configuration valid
> - assembleDebug PASS
> - no real secret/App Secret embedded
> - only expected INTERNET permission added
> - physical-device runtime works through required steps
>
> Node:
> - existing canonical suite still passes:
>   node --test --test-isolation=none
> - Gate A remains unchanged/green
> - new Gate B verifier/test-vector checks pass
>
> Repository:
> - git diff --check
> - scoped status
> - safe secret scan
> - no environment/local Privy config staged
>
> No blockchain transaction.
> No ENS write.
> No NFC/HCE code.
>
> NO-TOUCH
>
> Do not modify unless absolutely required:
>
> - firmware/
> - scripts/nfc/
> - demo/
> - ENS contract/deployment code
> - Gate A security semantics
> - README.md
> - PROJECT.md
> - STATUS.md
> - DECISIONS.md
> - WORKLOG.md
>
> Do not implement:
>
> - HCE
> - APDUs
> - PN532 changes
> - USDC/payment
> - World
> - ERC-4337
> - ENS credential issuance for the Privy wallet
>
> Do not commit or push the Gate B implementation yet.
>
> Only the previously validated Android skeleton may be checkpointed/pushed in
> PHASE 0 if necessary.
>
> ACCEPTANCE CRITERIA
>
> Gate B PASS requires ALL:
>
> 1. Android project builds.
> 2. Real-device Privy email authentication succeeds.
> 3. Embedded Ethereum EOA exists.
> 4. Public wallet address is obtained.
> 5. Native Android provider successfully processes eth_signTypedData_v4.
> 6. Signature corresponds to exact Gate A-compatible EIP-712 schema.
> 7. Node/viem recoverTypedDataAddress succeeds.
> 8. Recovered address equals Privy embedded wallet address.
> 9. Existing 79-test Gate A/project suite remains green.
> 10. No private key/App Secret/auth token is exposed.
> 11. No blockchain transaction occurs.
> 12. No HCE/NFC functionality is added.
>
> STOP IF
>
> Stop immediately and report if:
>
> - repository baseline is unexpectedly dirty/divergent
> - Android skeleton cannot reproduce its build
> - official Privy dependency cannot resolve cleanly
> - local App ID/App Client ID configuration cannot be kept out of Git
> - email authentication requires an unexpected large integration
> - embedded wallet creation fails in a way that needs architectural redesign
> - eth_signTypedData_v4 is unsupported/rejected by native Privy Android
> - verification would require personal_sign/custom cryptography instead
> - any private key/App Secret is requested
> - scope starts expanding into HCE/NFC/payment/World/4337
>
> Do not work around an eth_signTypedData_v4 failure during this task.
>
> EVIDENCE REQUIRED
>
> Return:
>
> # GATE B PRIVY ANDROID SIGNING
>
> ## REPO BASELINE
> ## SKELETON CHECKPOINT
> ## PRIVY SDK
> - exact artifact/version
>
> ## LOCAL CONFIG
> - mechanism only
> - confirm actual values not committed/printed
>
> ## EMAIL AUTH
> ## EMBEDDED EOA
> ## EIP712 TEST VECTOR
> ## TYPED DATA RPC
>
> Explicitly state:
>
> eth_signTypedData_v4:
> SUPPORTED / UNSUPPORTED
>
> ## NODE RECOVERY
>
> Include:
>
> PRIVY WALLET:
> 0x...
>
> RECOVERED SIGNER:
> 0x...
>
> MATCH:
> PASS / FAIL
>
> ## ANDROID BUILD
> ## PHYSICAL DEVICE VALIDATION
> ## EXISTING TEST SUITE
> ## FILES CHANGED
> ## SECURITY CHECK
> ## GIT STATE
> ## NEXT ACTION
>
> Do not commit Gate B implementation.
>
> End exactly:
>
> GATE B: PASS
>
> or
>
> GATE B: STOP — <reason>
