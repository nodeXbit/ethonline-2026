# Project task packet 059: TASK — Perform ONLY the read-only admission inspection for the new ENSv2 mobile issuer

- Classification: **ORIGINAL / SANITIZED**
- Redactions applied: none
- Source: retained project-specific Control Tower packet in the local Codex record

---

> TASK
>
> Perform ONLY the read-only admission inspection for the new ENSv2 mobile issuer
> architecture.
>
> DO NOT edit files.
> DO NOT install dependencies.
> DO NOT perform blockchain writes.
> DO NOT deploy contracts.
> DO NOT run NFC.
> DO NOT flash firmware.
> DO NOT commit or push.
> DO NOT expose secrets.
>
> This task decides whether we may proceed to the separately authorized mobile
> transaction proof.
>
> EXPECTED PUSHED BASELINE
>
> HEAD == origin/main ==
> 1b1317e65106bb173d1eedc19ae32fcc34513651
>
> Current supplied onchain/runtime state after the final rehearsal:
>
> guest-001.demo-access.eth:
> REGISTERED
> ACTIVE / ALLOW
> validUntil=1793487599
>
> DEV:
> nonce 24/24
> no pending transaction
>
> The existing legacy fallback MUST remain untouched.
>
> ============================================================
> PRODUCT LOCK
> ============================================================
>
> We intend to add, not replace:
>
> demo-access.eth
> └── keys.demo-access.eth
>     └── NEW UserRegistry R1 controlled by a dedicated Privy issuer EOA I
>         ├── staff-001.keys.demo-access.eth
>         ├── visitor-001.keys.demo-access.eth
>         └── contractor-001.keys.demo-access.eth
>
> A NEW PermissionedResolver S1 will be controlled by I.
>
> Legacy:
>
> guest-001.demo-access.eth
> R0
> S0
>
> must remain untouched.
>
> Reference architecture/specification is available in:
>
> ENSV2_PRODUCT_LOCK_IMPLEMENTATION_PACKET.md
>
> Treat it as DESIGN, not executable authority.
>
> Verify all integration details locally.
>
> ============================================================
> PHASE 1 — REPOSITORY / WORKSPACE
> ============================================================
>
> Run read-only:
>
> git fetch origin
> git status -sb
> git rev-parse HEAD
> git rev-parse origin/main
>
> Require:
>
> clean
> HEAD == origin/main ==
> 1b1317e65106bb173d1eedc19ae32fcc34513651
>
> Inspect AGENTS.md before anything else.
>
> If local state differs:
> STOP.
>
> ============================================================
> PHASE 2 — PRIVY 0.14.0 ACTUAL API
> ============================================================
>
> Inspect the exact locally resolved Privy Android 0.14.0 artifact/source/API used
> by this project.
>
> Do not rely only on online docs.
>
> Determine exactly:
>
> - concrete EthereumRpcRequest constructor/factory shape;
> - EthereumRpcResponse shape;
> - whether wallet.provider.request supports:
>   - eth_sendTransaction
>   - eth_call
>   - eth_getTransactionReceipt
>   - eth_getTransactionCount
>   - eth_chainId
>   - eth_getBlockByNumber
>   or whether some reads require a separate RPC client;
> - exact way chainId is specified for Sepolia;
> - whether provider chain switching/configuration is required;
> - exact response format for eth_sendTransaction;
> - error/result types;
> - whether transaction user confirmation is automatic or exposed;
> - whether existing app integration can reuse the same wallet/provider.
>
> DO NOT make any request that creates a transaction.
>
> Existing eth_signTypedData_v4 behavior must remain untouched.
>
> ============================================================
> PHASE 3 — RPC DESIGN
> ============================================================
>
> Determine the smallest secure read strategy for Android.
>
> Priority:
>
> A. If the authenticated Privy Ethereum provider safely supports the required
>    read-only JSON-RPC methods, prefer reusing it.
>
> B. Otherwise identify the smallest distributable read-only Sepolia RPC
>    configuration.
>
> NEVER compile the operator's credential-bearing Alchemy URL into the APK.
>
> Do not print current local RPC URLs or credentials.
>
> Return only the architecture choice.
>
> ============================================================
> PHASE 4 — ABI DEPENDENCY
> ============================================================
>
> Inspect current Gradle dependency graph and Android toolchain.
>
> Evaluate ONLY, do not install:
>
> org.web3j:abi:4.12.3-android
>
> Determine:
>
> - whether Maven/Gradle coordinates resolve in the existing repositories;
> - expected transitive dependencies;
> - compatibility with minSdk 28 / current Java/AGP;
> - whether the ABI module alone supplies:
>   FunctionEncoder
>   FunctionReturnDecoder
>   Address
>   Uint64 / Uint256
>   DynamicBytes
>   DynamicArray
>   Utf8String
>   Function
>   tuple decoding required for getState;
> - whether importing only this module is sufficient.
>
> If another existing project dependency can already encode ABI safely, report it
> instead.
>
> Do not install anything.
>
> ============================================================
> PHASE 5 — CURRENT CONTRACT / ABI FACTS
> ============================================================
>
> Inspect the project's pinned ENSv2 artifacts/source references.
>
> Verify from the exact pinned deployment/code:
>
> VerifiableFactory:
>
> deployProxy(address implementation,uint256 salt,bytes data)
>
> verifyContract(address proxy) -> address implementation
>
> UserRegistry:
>
> initialize(address rootAccount,uint256 roleBitmap)
>
> register(
>   string label,
>   address owner,
>   address registry,
>   address resolver,
>   uint256 roleBitmap,
>   uint64 expiry
> )
>
> PermissionedResolver:
>
> initialize(
>   address admin,
>   uint256 roleBitmap,
>   bytes[] setters
> )
>
> setText
> setData
> multicall
> authorizeTextRoles
>
> Registry safeTransferFrom ABI / inheritance.
>
> Do not trust guessed ABIs.
>
> ============================================================
> PHASE 6 — ROLE BITMAP REVIEW
> ============================================================
>
> Verify exact current ENSv2 role constants.
>
> Proposed R1 ROOT roles for issuer I:
>
> ROLE_REGISTRAR
> ROLE_REGISTRAR_ADMIN
> ROLE_RENEW
> ROLE_RENEW_ADMIN
>
> Proposed S1 ROOT roles for issuer I:
>
> ROLE_SET_TEXT
> ROLE_SET_TEXT_ADMIN
> ROLE_SET_DATA
> ROLE_SET_DATA_ADMIN
>
> For keys.demo-access.eth in R0:
>
> evaluate MINIMUM privilege.
>
> Explicitly determine whether:
>
> ROLE_RENEW only
>
> is sufficient for I to renew keys, and whether ROLE_RENEW_ADMIN adds any
> necessary hackathon capability.
>
> Prefer least privilege.
>
> Do not modify roles.
>
> ============================================================
> PHASE 7 — BOOTSTRAP FEASIBILITY
> ============================================================
>
> Read-only confirm from committed helpers/artifacts that the intended sequence is
> structurally valid:
>
> 1. Factory.deployProxy UserRegistryImpl initialized to I.
> 2. Factory.deployProxy PermissionedResolverImpl initialized to I.
> 3. R0.register(
>      "keys",
>      I,
>      R1,
>      S1,
>      keysOwnerRoles,
>      keysExpiry
>    )
>
> Verify whether register directly establishes the subregistry/resolver pointers
> without another setSubregistry/setResolver transaction.
>
> No simulation requiring a signer yet.
>
> No broadcast.
>
> ============================================================
> PHASE 8 — ANDROID INTEGRATION SURFACE
> ============================================================
>
> Inspect current:
>
> MainActivity
> GateBApplication
> PrivyProofProvider
> HCE classes
> Gradle files
> tests
>
> Identify exact integration points for:
>
> - dedicated issuer account detection;
> - transaction admission proof;
> - future CredentialRepository;
> - future IssuanceCoordinator;
> - future selectedCredential store.
>
> Do not design a broad Android refactor.
>
> Native Views remain.
>
> ============================================================
> PHASE 9 — ISSUER WALLET OPERATION
> ============================================================
>
> State exactly how a DEDICATED Privy issuer EOA should be established for the
> hackathon.
>
> Do not request OTP or login in this read-only task.
>
> Determine:
>
> - whether a separate Privy user/login is the cleanest path;
> - whether the app can switch logout/login between issuer and holder accounts;
> - what public issuer address must be captured before bootstrap;
> - what Sepolia ETH funding will be required conceptually.
>
> Never request/export private keys or seed phrases.
>
> ============================================================
> PHASE 10 — ADMISSION PLAN
> ============================================================
>
> If everything above is feasible, design ONLY the next separately authorized
> proof:
>
> M1 — MOBILE TRANSACTION TRANSPORT
>
> Dedicated issuer I:
>
> eth_sendTransaction
>
> Sepolia
>
> from I
> to I
> value 0
> data 0x
>
> Require:
>
> user intent
> exact chain
> tx hash
> receipt success
> nonce reconciliation
>
> No contract deployment yet.
>
> Then define M2, but DO NOT execute:
>
> M2 — FIRST REAL MOBILE CREDENTIAL VERTICAL
>
> after separately authorized bootstrap:
>
> Android issuer
> → R1.register one credential
> → S1.multicall records
> → receipts
> → authoritative readback
>
> Do not build full UI before M1 and M2 prove viable.
>
> ============================================================
> RETURN
> ============================================================
>
> # MOBILE ISSUER ADMISSION — READ ONLY
>
> ## REPO STATE
>
> ## PRIVY 0.14.0 ACTUAL API
>
> Exact request construction:
> Exact chain handling:
> eth_sendTransaction support:
> Read RPC support:
> Response shape:
> Errors:
>
> ## ANDROID RPC STRATEGY
>
> Chosen:
> PRIVY PROVIDER / SEPARATE PUBLIC READ RPC
>
> Reason:
>
> Confirm operator Alchemy secret will NOT be compiled into APK.
>
> ## ABI STRATEGY
>
> Current capabilities:
> web3j ABI artifact compatibility:
> Recommended dependency:
> Installation required later:
> YES / NO
>
> ## CONTRACT ABI VERIFICATION
>
> Factory:
> Registry:
> Resolver:
> Transfer:
>
> ## ROLE REVIEW
>
> R1 issuer:
> S1 issuer:
> keys owner minimum roles:
>
> Explicit verdict on ROLE_RENEW_ADMIN for keys.
>
> ## BOOTSTRAP STRUCTURE
>
> Expected writes:
> 3 / other
>
> Need setSubregistry afterward:
> YES / NO
>
> Need setResolver afterward:
> YES / NO
>
> Need new Solidity:
> YES / NO
>
> ## ANDROID INTEGRATION POINTS
>
> ## ISSUER ACCOUNT PLAN
>
> No secrets.
>
> ## M1 PLAN
>
> Exact mobile transaction proof.
>
> ## M2 PLAN
>
> Exact first real credential vertical.
>
> DO NOT EXECUTE.
>
> ## CODEX ESTIMATED IMPLEMENTATION SURFACE AFTER ADMISSION
>
> ## STOP CONDITIONS
>
> ## VERDICT
>
> MOBILE ISSUER ADMISSION:
> PASS / STOP
>
> If PASS, end with:
>
> NEXT AUTHORIZATION REQUIRED:
> ONE M1 MOBILE ZERO-VALUE SEPOLIA TRANSACTION ONLY.
>
> No edits, installs, writes or commits in this task.
