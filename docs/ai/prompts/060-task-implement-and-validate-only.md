# Project task packet 060: TASK — Implement and validate ONLY:

- Classification: **ORIGINAL / SANITIZED**
- Redactions applied: none
- Source: retained project-specific Control Tower packet in the local Codex record

---

> TASK
>
> Implement and validate ONLY:
>
> M1 — MOBILE ISSUER TRANSACTION TRANSPORT
>
> This task MAY make a very small scoped Android code change.
>
> This task MAY perform exactly ONE blockchain write, but ONLY after:
>
> 1. code/build/tests pass;
> 2. a dedicated issuer Privy account is logged in;
> 3. Sepolia is confirmed;
> 4. the wallet has enough Sepolia ETH;
> 5. the user explicitly presses the one-shot M1 action in the Android UI.
>
> No other blockchain write is authorized.
>
> DO NOT:
>
> - deploy R1
> - deploy S1
> - register keys.demo-access.eth
> - call any ENS contract write
> - install Web3j
> - modify HCE
> - run NFC
> - modify firmware
> - modify Node verifier
> - change guest-001
> - change R0/S0
> - commit
> - push
> - expose secrets
> - compile any operator Alchemy credential into the APK
> - automatically resubmit a transaction
> - run M1 from the existing holder account
>
> ============================================================
> VERIFIED ADMISSION FACTS
> ============================================================
>
> Read-only admission already PASSed.
>
> Repository expected:
>
> HEAD == origin/main ==
> 1b1317e65106bb173d1eedc19ae32fcc34513651
>
> worktree clean.
>
> Local Privy version:
>
> io.privy:privy-core:0.14.0
>
> Locally verified API:
>
> EthereumRpcRequest(
>     method: String,
>     params: List<String>
> )
>
> EthereumRpcRequest.ethSendTransaction(
>     transactionJson: String
> )
>
> Provider chain selection:
>
> wallet.provider.switchChain(EthereumChain.Sepolia)
>
> Sepolia:
>
> 11155111
> 0xaa36a7
>
> provider.request supports generic read JSON-RPC.
>
> Use the Privy provider.
>
> DO NOT add another RPC client.
>
> DO NOT use the operator Alchemy endpoint.
>
> ============================================================
> IMPORTANT CORRECTION TO THE PREVIOUS M1 PLAN
> ============================================================
>
> A transaction receipt does NOT itself prove transaction value/input and does not
> contain a chainId.
>
> Therefore M1 must verify:
>
> CHAIN:
>
> eth_chainId before submission
> ==
> 0xaa36a7
>
> and again after confirmation.
>
> TRANSACTION:
>
> after obtaining tx hash, call:
>
> eth_getTransactionByHash
>
> through the same Privy provider.
>
> Require authoritative transaction readback:
>
> from == I
> to == I
> value == 0x0
> input == 0x
> hash == submitted hash
>
> Then independently require receipt success.
>
> Do not describe this as "receipt chain verification".
>
> ============================================================
> PHASE 1 — REVERIFY LOCAL STATE
> ============================================================
>
> Read-only first:
>
> git status -sb
> git rev-parse HEAD
> git rev-parse origin/main
>
> Require exact clean baseline.
>
> Inspect AGENTS.md.
>
> STOP if baseline differs.
>
> ============================================================
> PHASE 2 — IMPLEMENT MINIMAL M1 HARNESS
> ============================================================
>
> Make the smallest Android-only change.
>
> Prefer:
>
> NEW focused class such as:
>
> MobileIssuerAdmissionRunner.kt
>
> plus minimal MainActivity integration.
>
> Do NOT build the future IssuanceCoordinator yet.
>
> Do NOT install ABI libraries.
>
> Do NOT refactor Android architecture.
>
> Reuse:
>
> - application-scoped Privy instance
> - existing authenticated user
> - existing embedded Ethereum wallet/provider
>
> Leave:
>
> PrivyProofProvider
> HCE
> typed-data signing
>
> untouched.
>
> ============================================================
> M1 UI
> ============================================================
>
> Add a clearly isolated temporary/product-development section:
>
> MOBILE ISSUER ADMISSION
>
> Show only public information:
>
> Current wallet:
> 0x...
>
> Network:
> Sepolia
>
> Balance:
> <public balance>
>
> Status:
> READY / BLOCKED / SUBMITTING / CONFIRMED / FAILED / UNKNOWN
>
> Button:
>
> RUN ZERO-VALUE SEPOLIA ADMISSION
>
> Before the button becomes enabled require:
>
> - authenticated wallet exists
> - this login is explicitly being used as the NEW DEDICATED ISSUER account
> - switchChain(EthereumChain.Sepolia) succeeded
> - eth_chainId == 0xaa36a7
> - latest nonce == pending nonce
> - balance is sufficient for a normal self-transaction
>
> Do not estimate/fabricate sufficient balance.
>
> If balance is insufficient:
>
> STOP M1.
>
> Return the public issuer address I so it can be funded separately.
>
> Do NOT fund it automatically.
>
> ============================================================
> DEDICATED ISSUER ACCOUNT
> ============================================================
>
> The user will manually log in using a separate Privy identity intended to become
> issuer I.
>
> Never request or display:
>
> - OTP
> - email
> - private key
> - recovery material
> - seed
>
> The public embedded Ethereum address of that account becomes candidate:
>
> I
>
> Do NOT hard-code the existing holder:
>
> 0x3419148731087b970d2059C53780163B452D5FF7
>
> as issuer.
>
> If the logged wallet equals the known holder wallet:
>
> BLOCK THE M1 BUTTON.
>
> Require a distinct issuer wallet.
>
> ============================================================
> EXACT M1 TRANSACTION
> ============================================================
>
> Immediately before submit:
>
> wallet.provider.switchChain(EthereumChain.Sepolia)
>
> Require again:
>
> eth_chainId == 0xaa36a7
>
> Read:
>
> latest nonce
> pending nonce
>
> Require equality.
>
> Capture:
>
> preNonce
>
> Construct EXACT transaction JSON:
>
> {
>   "from": "<I>",
>   "to": "<I>",
>   "value": "0x0",
>   "data": "0x"
> }
>
> No arbitrary fields unless the Privy provider requires them.
>
> Do not manually set nonce unless required by the local API.
>
> Do not manually set chainId inside EthereumRpcRequest if chain selection is
> provider state.
>
> Do not add contract data.
>
> Show an explicit final confirmation in the UI immediately before submission:
>
> Sepolia
> From: I
> To: I
> Value: 0 ETH
> Purpose: Mobile issuer admission
> Cost: gas only
>
> Submission must require a deliberate user button press.
>
> Then call exactly once:
>
> EthereumRpcRequest.ethSendTransaction(transactionJson)
>
> ============================================================
> TRANSACTION HASH
> ============================================================
>
> Parse EthereumRpcResponse.data defensively.
>
> Accept only one valid 32-byte transaction hash.
>
> If result shape is unknown:
>
> STATUS = UNKNOWN
> STOP
>
> Do not retry.
>
> Never dump the complete response object if it may contain transport metadata.
>
> ============================================================
> TRANSACTION READBACK
> ============================================================
>
> Using the same selected Sepolia Privy provider:
>
> poll:
>
> eth_getTransactionByHash
>
> Require:
>
> hash == submitted hash
> from == I
> to == I
> value == 0x0
> input == 0x
>
> Treat:
>
> input == "0x"
> or another canonical empty-data representation only if verified against the
> actual RPC response format.
>
> No guessing.
>
> ============================================================
> RECEIPT
> ============================================================
>
> Poll:
>
> eth_getTransactionReceipt
>
> with bounded timeout/backoff.
>
> Require:
>
> receipt exists
> status == 0x1
> from == I
> to == I
>
> Capture:
>
> blockNumber
>
> If timeout occurs after a tx hash exists:
>
> STATUS = UNKNOWN
>
> Do NOT resubmit.
>
> ============================================================
> POST-CONFIRMATION
> ============================================================
>
> Re-read:
>
> eth_chainId
>
> Require:
>
> 0xaa36a7
>
> Re-read:
>
> latest nonce
> pending nonce
>
> RPC/provider indexing may lag briefly.
>
> Use a bounded reconciliation poll rather than requiring the first immediate
> read to have advanced.
>
> Require eventually:
>
> latest == preNonce + 1
> pending == preNonce + 1
>
> If not reconciled within the bounded window:
>
> STATUS = UNKNOWN
>
> Do NOT submit another transaction.
>
> ============================================================
> BALANCE / GAS
> ============================================================
>
> Use:
>
> eth_getBalance
>
> through the Privy Sepolia provider.
>
> Do not require a large arbitrary threshold.
>
> Before enabling submit, determine whether the wallet has enough balance for the
> provider's transaction path.
>
> If reliable gas estimation is straightforward through:
>
> eth_estimateGas
>
> you may use it read-only.
>
> Otherwise simply BLOCK if balance is zero and rely on the provider for gas
> validation.
>
> Do not add a gas-management subsystem.
>
> ============================================================
> SANITIZATION
> ============================================================
>
> Visible/loggable:
>
> - public issuer address
> - Sepolia chain ID
> - balance
> - nonce counts
> - tx hash
> - block number
> - success/failure category
>
> Never print:
>
> - OTP
> - email
> - RPC endpoint
> - Privy auth token
> - private/recovery material
> - raw provider internals
> - HTTP request metadata
>
> Errors must be reduced to:
>
> error class
> +
> short sanitized message/category
>
> ============================================================
> TESTS
> ============================================================
>
> Add focused pure/JVM tests where practical for:
>
> - quoted tx hash parsing
> - malformed tx hash
> - wrong chain
> - from/to mismatch
> - nonzero value
> - nonempty data
> - reverted receipt
> - receipt malformed
> - nonce reconciliation
> - UNKNOWN after post-submit timeout
> - prevention of second submission
> - known holder wallet blocked as issuer
>
> Do not attempt a network transaction from JVM tests.
>
> ============================================================
> VALIDATION BEFORE REAL M1
> ============================================================
>
> Run:
>
> cd mobile/android
> .\gradlew.bat testDebugUnitTest
> .\gradlew.bat assembleDebug
>
> Require PASS.
>
> Also from repository root:
>
> git diff --check
>
> Inspect diff.
>
> Require:
>
> - Android-only scoped M1 changes
> - no dependency changes unless absolutely required
> - no secrets
> - no HCE change
> - no ENS/Node/firmware change
>
> Do NOT commit.
>
> ============================================================
> MANUAL EXECUTION BOUNDARY
> ============================================================
>
> After build PASS:
>
> STOP AUTOMATED EXECUTION.
>
> Return exact APK path and instructions for the user.
>
> The USER will:
>
> 1. install the APK manually;
> 2. log in privately with the dedicated issuer Privy account;
> 3. never reveal OTP/email;
> 4. observe its public wallet address;
> 5. check that M1 shows Sepolia and sufficient balance;
> 6. press the one-shot M1 button exactly once.
>
> If insufficient balance:
>
> DO NOT PRESS.
>
> STOP and report:
>
> ISSUER FUNDING REQUIRED
>
> plus the public issuer address only.
>
> ============================================================
> AFTER USER PRESS
> ============================================================
>
> Use only the app's implemented admission flow.
>
> Do not run an ad-hoc RPC diagnostic that could expose endpoint metadata.
>
> Require app result:
>
> M1 CONFIRMED
>
> with:
>
> issuer public address I
> chainId 11155111
> tx hash
> receipt block
> pre nonce
> post latest nonce
> post pending nonce
>
> No other write.
>
> ============================================================
> FINAL VALIDATION
> ============================================================
>
> After M1 attempt:
>
> git status -sb
>
> No unrelated changes.
>
> No ENS state mutation expected.
>
> guest-001 / R0 / S0 untouched.
>
> Do not run NFC.
>
> Do not flash.
>
> Do not commit/push.
>
> ============================================================
> RETURN BEFORE THE USER TRANSACTION
> ============================================================
>
> # M1 MOBILE ISSUER — IMPLEMENTATION READY
>
> ## CODE CHANGES
>
> ## TESTS
>
> ## APK
>
> ## CURRENT WALLET REQUIREMENT
>
> Dedicated issuer account required:
> YES
>
> Existing holder blocked:
> YES
>
> ## MANUAL STEPS
>
> Exact install/login/button instructions.
>
> ## SECURITY
>
> ## GIT
>
> End:
>
> READY FOR USER M1: YES
>
> or:
>
> READY FOR USER M1: NO — <reason>
>
> ============================================================
> RETURN AFTER THE USER TRANSACTION
> ============================================================
>
> # M1 MOBILE ISSUER — RESULT
>
> Issuer I:
>
> Chain:
>
> Transaction:
>
> Block:
>
> Pre nonce:
>
> Post latest nonce:
>
> Post pending nonce:
>
> Transaction readback:
> PASS / FAIL
>
> Receipt:
> PASS / FAIL
>
> M1:
> PASS / STOP
>
> Blockchain writes during M1:
> MUST equal 1 if PASS.
>
> ENS writes:
> 0
>
> NFC activity:
> 0
>
> Firmware activity:
> 0
>
> Git:
>
> If fully successful end exactly:
>
> M1 MOBILE TRANSACTION: PASS
>
> Otherwise:
>
> M1 MOBILE TRANSACTION: STOP — <exact reason>
