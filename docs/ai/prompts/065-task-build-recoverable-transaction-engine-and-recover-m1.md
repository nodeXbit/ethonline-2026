# Project task packet 065: TASK — BUILD RECOVERABLE TRANSACTION ENGINE AND RECOVER M1

- Classification: **ORIGINAL / SANITIZED**
- Redactions applied: none
- Source: retained project-specific Control Tower packet in the local Codex record

---

> TASK — BUILD RECOVERABLE TRANSACTION ENGINE AND RECOVER M1
>
> WHY
>
> The current M1 one-shot harness exposed a product-level design flaw:
>
> submission_attempted=true is persisted before provider submission, but no hash,
> stage, nonce or recovery state is persisted.
>
> Physical/onchain reconciliation proved:
>
> issuer:
> 0xFa90e8301A22833B74378C5fA3a7c120Ac512685
>
> Sepolia balance:
> 0.02 ETH
>
> latest nonce:
> 0
>
> pending nonce:
> 0
>
> M1 outgoing transactions:
> 0
>
> Therefore:
>
> NO TRANSACTION WAS OBSERVED.
>
> The current ONE_SHOT_ALREADY_USED state is a local false terminal state.
>
> Do NOT patch this with "clear the boolean".
>
> Build the smallest reusable transaction state machine that can later support:
>
> - M1
> - native ETH send
> - credential register
> - resolver multicall
> - suspend / restore
> - metadata edits
> - credential transfer
>
> Only M1 must be integrated now.
>
> ============================================================
> CONTEXT
> ============================================================
>
> Current repository has uncommitted scoped M1 Android changes.
>
> Legacy holder proof / HCE / Node / ENS / firmware are untouched.
>
> Do not lose or overwrite existing M1 work.
>
> Expected pushed base remains:
>
> 1b1317e65106bb173d1eedc19ae32fcc34513651
>
> Do not reset the worktree.
>
> Do not commit/push.
>
> No blockchain write is authorized during implementation or automated tests.
>
> ============================================================
> SCOPE
> ============================================================
>
> AUTHORIZED:
>
> - refactor the M1 transaction submission state into a reusable Android
>   transaction engine/journal;
> - migrate the old Boolean M1 state safely;
> - add persistent transaction state;
> - add recovery/reconciliation;
> - improve M1 transaction review UI;
> - add tests;
> - build APK.
>
> NOT AUTHORIZED:
>
> - actual M1 retry yet;
> - any eth_sendTransaction against a real provider during automated work;
> - ENS writes;
> - R1/S1 bootstrap;
> - Web3j installation;
> - credential UI;
> - HCE/NFC;
> - Node;
> - firmware;
> - dependency upgrades;
> - commit/push.
>
> ============================================================
> TRANSACTION MODEL
> ============================================================
>
> Create a small persisted operation model.
>
> Exact class/file names may adapt to the existing Android structure, but behavior
> must follow this contract.
>
> Required persisted fields:
>
> operationId
> operationType
> walletAddress
> chainId
>
> targetAddress
> valueWei
> callDigest or canonical data summary
>
> preLatestNonce
> prePendingNonce
>
> txHash if obtained
>
> state
>
> receiptBlock if confirmed
>
> createdAt
> updatedAt
>
> safeErrorCategory if applicable
>
> Never persist:
>
> private keys
> auth tokens
> RPC endpoints
> raw signed transaction
> OTP/email
> unredacted provider internals
>
> ============================================================
> REQUIRED STATES
> ============================================================
>
> At minimum:
>
> DRAFT
>
> READY_TO_REVIEW
>
> READY_TO_SUBMIT
>
> SUBMITTING_NO_HASH
>
> HASH_RECEIVED
>
> CONFIRMING
>
> ONCHAIN_READBACK
>
> CONFIRMED
>
> REVERTED
>
> UNKNOWN
>
> NO_BROADCAST_PROVEN
>
> CANCELLED
>
> You may introduce one additional migration state if necessary.
>
> State transitions must be explicit and tested.
>
> ============================================================
> CORE SAFETY RULES
> ============================================================
>
> 1. Only one write operation may be active per wallet.
>
> 2. Before submit:
>    require exact wallet;
>    Sepolia chain;
>    latest == pending;
>    operation summary matches approved intent.
>
> 3. Persist:
>
>    SUBMITTING_NO_HASH
>    + pre nonce
>    + exact operation identity
>
>    BEFORE invoking provider send.
>
> 4. Immediately when a valid hash returns:
>
>    persist HASH_RECEIVED + txHash
>
>    BEFORE doing any further RPC work.
>
> 5. Once a txHash exists:
>
>    NEVER automatically submit again.
>
> 6. After hash:
>
>    reconcile transaction
>    receipt
>    chain
>    nonce
>    product readback when applicable.
>
> 7. UNKNOWN never automatically resubmits.
>
> 8. App restart must load and reconcile incomplete operations.
>
> 9. A send failure before hash does NOT automatically become retryable.
>
> It must first prove NO_BROADCAST.
>
> ============================================================
> NO-BROADCAST PROOF
> ============================================================
>
> For M1, NO_BROADCAST_PROVEN requires bounded read-only reconciliation showing:
>
> latest nonce == preLatestNonce
>
> pending nonce == prePendingNonce
>
> and no persisted tx hash.
>
> Balance may be used as supporting evidence but nonce is authoritative for this
> zero-value self transaction.
>
> After NO_BROADCAST_PROVEN:
>
> do NOT automatically retry.
>
> Require an explicit user action:
>
> RE-ARM M1
>
> which creates a NEW operationId.
>
> The historical failed operation remains in the journal.
>
> ============================================================
> LEGACY ONE-SHOT MIGRATION
> ============================================================
>
> Current private preferences contain:
>
> mobile_issuer_admission
> submission_attempted=true
>
> but no tx hash/stage/preNonce.
>
> For the current issuer only, migration must NOT simply erase this.
>
> On app startup / explicit reconcile:
>
> recognize:
>
> LEGACY_ATTEMPT_REQUIRES_RECONCILIATION
>
> Perform read-only reconciliation.
>
> Known physical/onchain evidence currently expects:
>
> issuer latest == pending == 0
>
> If the device also sees 0/0:
>
> classify the legacy attempt as:
>
> NO_BROADCAST_PROVEN
>
> Then show:
>
> Previous admission attempt did not reach Sepolia.
>
> [ Re-arm admission ]
>
> Do not re-arm automatically.
>
> If nonce is no longer 0 or state is ambiguous:
>
> UNKNOWN
>
> and no retry button.
>
> Once migrated successfully, the old Boolean must no longer control normal
> transaction behavior.
>
> ============================================================
> M1 REVIEW UI
> ============================================================
>
> Before user submission, show explicitly:
>
> SEPOLIA TESTNET
>
> Network:
> Sepolia (11155111)
>
> From:
> issuer full/short address
>
> To:
> same issuer
>
> Action:
> Mobile issuer admission
>
> Value:
> 0 ETH
>
> Data:
> 0x
>
> Estimated / expected network cost:
> if safely available
>
> Purpose:
> Verify Android mobile transaction transport
>
> The submit control must say clearly:
>
> SUBMIT ONCE
>
> The UI must make clear this is an M1 developer admission operation, not a normal
> product transfer.
>
> ============================================================
> M1 RESULT UI
> ============================================================
>
> Show product-readable states:
>
> Preparing
> Submitting
> Transaction submitted
> Confirming
> Confirmed
> Failed
> Could not determine transaction status
>
> Technical details collapsed may show:
>
> operationId
> tx hash
> block
> pre nonce
> post nonce
> safe stage/category
>
> Explorer button when tx hash exists.
>
> No raw provider data.
>
> ============================================================
> M1 RECONCILIATION
> ============================================================
>
> Once tx hash exists:
>
> eth_getTransactionByHash:
>
> require:
> hash
> from == issuer
> to == issuer
> value == 0
> input/data empty
>
> receipt:
>
> status success
>
> chain:
>
> still Sepolia
>
> bounded nonce reconciliation:
>
> latest == pending == preNonce + 1
>
> Then:
>
> CONFIRMED
>
> Persist all public evidence needed to survive restart.
>
> ============================================================
> GENERIC DESIGN
> ============================================================
>
> Do not make the engine M1-specific internally.
>
> A future operation should be able to describe:
>
> operationType
> target
> value
> data/call digest
> review summary
> confirmation policy
> readback callback/policy
>
> However:
>
> DO NOT implement credential transactions yet.
>
> Avoid speculative framework design.
>
> Small reusable abstraction only.
>
> ============================================================
> RECENT ACTIVITY FOUNDATION
> ============================================================
>
> The journal should be structurally suitable for future:
>
> Wallet → Recent Activity
>
> Do not build the final Wallet UI now.
>
> It must be possible later to render:
>
> operation type
> status
> timestamp
> tx hash
> explorer link
>
> from the journal.
>
> ============================================================
> EXPLORER
> ============================================================
>
> Add a helper/design seam for Sepolia transaction explorer URLs.
>
> Do not embed explorer pages or WebView.
>
> Opening the browser from confirmed M1 may be implemented if trivial.
>
> ============================================================
> TESTS
> ============================================================
>
> Tests must cover at least:
>
> state transition legality
>
> persist/reload journal
>
> SUBMITTING_NO_HASH persisted before send
>
> hash persisted immediately
>
> app restart after SUBMITTING_NO_HASH
>
> app restart after HASH_RECEIVED
>
> hash => never resubmit
>
> UNKNOWN => never resubmit
>
> NO_BROADCAST proof with unchanged latest/pending
>
> nonce changed => cannot classify no-broadcast
>
> explicit re-arm creates NEW operationId
>
> historical failed operation remains stored
>
> legacy submission_attempted=true migration
>
> legacy 0/0 => NO_BROADCAST_PROVEN
>
> legacy ambiguous nonce => UNKNOWN
>
> wrong wallet
>
> wrong chain
>
> latest != pending
>
> send exception before hash
>
> malformed hash
>
> reverted receipt
>
> transaction content mismatch
>
> nonce reconciliation
>
> M1 CONFIRMED persistence
>
> confirmation UI includes:
>
> Network
> From
> To
> Value
> Data
> Purpose
>
> readiness still performs zero sends
>
> No test may perform a real blockchain transaction.
>
> ============================================================
> VALIDATION
> ============================================================
>
> Run:
>
> .\gradlew.bat testDebugUnitTest
> .\gradlew.bat assembleDebug
>
> From repo root:
>
> git diff --check
>
> Inspect diff.
>
> Require:
>
> Android-only transaction/M1 scope.
>
> No HCE change.
>
> No Node change.
>
> No firmware change.
>
> No ENS change.
>
> No dependency addition unless absolutely unavoidable; expect none.
>
> No secrets.
>
> ============================================================
> MANUAL RETEST — READ ONLY FIRST
> ============================================================
>
> After building:
>
> STOP before any real send.
>
> Return APK path + SHA-256.
>
> User will install over current app, keeping app data.
>
> Expected first physical result:
>
> legacy attempt is detected
>
> → read-only reconciliation
>
> → current issuer 0/0 causes:
>
> NO_BROADCAST_PROVEN
>
> UI shows:
>
> Previous admission attempt did not reach Sepolia.
>
> [ Re-arm admission ]
>
> The USER may press Re-arm only after Control Tower reviews the physical result.
>
> Re-arm itself MUST NOT submit a transaction.
>
> After re-arm, readiness may return to READY.
>
> DO NOT authorize SUBMIT ONCE in this task.
>
> ============================================================
> RETURN
> ============================================================
>
> # RECOVERABLE TRANSACTION ENGINE — M1 INTEGRATION
>
> ## ROOT DESIGN
>
> ## FILES
>
> ## PERSISTED MODEL
>
> ## STATE MACHINE
>
> ## LEGACY MIGRATION
>
> ## NO-BROADCAST RECONCILIATION
>
> ## M1 REVIEW UX
>
> ## RECOVERY UX
>
> ## RECENT ACTIVITY FOUNDATION
>
> ## TESTS
>
> ## APK
>
> Path:
> SHA-256:
>
> ## GIT
>
> ## SECURITY
>
> Real blockchain writes:
> 0
>
> Real eth_sendTransaction:
> 0
>
> ## MANUAL READ-ONLY RETEST
>
> Exact device steps.
>
> End:
>
> TRANSACTION ENGINE: READY FOR PHYSICAL RECOVERY TEST
>
> or:
>
> TRANSACTION ENGINE: STOP — <reason>
