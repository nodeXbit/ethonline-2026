# Project task packet 008: TASK — Fix only the remaining post-submission recovery gap in the interactive demo.

- Classification: **ORIGINAL / SANITIZED**
- Redactions applied: none
- Source: retained project-specific Control Tower packet in the local Codex record

---

> TASK
>
> Fix only the remaining post-submission recovery gap in the interactive demo.
>
> DO NOT SEND ANY BLOCKCHAIN TRANSACTION.
>
> VERIFIED CURRENT STATE
>
> The duplicate-write/idempotency fixes work.
>
> Live validation proved:
>
> * real activate: exactly one transaction
> * repeated activate: changed:false, zero additional transactions
> * real deactivate: exactly one transaction
> * repeated deactivate: changed:false, zero additional transactions
>
> The remaining failure mode is:
>
> transaction is successfully submitted and mined
> → receipt/readback RPC temporarily fails
> → server returns HTTP error
> → browser retains stale pre-write credential state.
>
> Incident deactivation:
>
> 0x24ccb6e2c072512f1213d9f5006cd4bd3e29856c07f307db8a8ad7aa40f89fd7
>
> nonce 15
> block 11664773
> receipt success
> final ENSv2 state INACTIVE / DENY
>
> But the server lost confirmation after submission because RPC receipt/readback
> failed.
>
> GOAL
>
> Once a transaction hash exists, the system must NEVER invite or perform a
> second write merely because confirmation/readback failed.
>
> The client must enter a RECOVERING/PENDING state and resolve the existing
> transaction/state using read-only operations.
>
> PHASE 1 — INSPECT CURRENT WRITE PIPELINE
>
> Inspect:
>
> * demo/server.mjs
> * demo/public/app.js
> * demo/public/access-action.js
> * scripts/ensv2/persistent-access.mjs
> * scripts/ensv2/contracts.mjs
> * relevant tests
>
> Map exactly:
>
> simulate
> → submit
> → transaction hash obtained
> → waitForTransactionReceipt
> → credential readback
> → HTTP response
>
> Identify where the transaction hash becomes durable and where later failures
> currently collapse into generic WRITE_FAILED.
>
> PHASE 2 — REQUIRED TRANSACTION STATE MODEL
>
> Explicitly distinguish:
>
> BEFORE_SUBMISSION
> SUBMITTED
> CONFIRMED_SUCCESS
> CONFIRMED_REVERT
> CONFIRMATION_UNKNOWN
>
> Rules:
>
> 1. BEFORE_SUBMISSION failure
>
>    * normal safe error
>    * no transaction hash
>    * user may retry later.
>
> 2. SUBMITTED
>
>    * transactionHash exists
>    * NEVER resubmit automatically.
>
> 3. CONFIRMED_SUCCESS
>
>    * receipt success
>    * perform authoritative ENSv2 readback
>    * return confirmed credential.
>
> 4. CONFIRMED_REVERT
>
>    * return failure including public transaction hash
>    * never retry automatically.
>
> 5. CONFIRMATION_UNKNOWN
>
>    * RPC/receipt/readback failed after transaction hash exists
>    * do NOT report ordinary action failure
>    * do NOT invite another POST
>    * enter recovery flow.
>
> PHASE 3 — SERVER RECOVERY
>
> Implement a bounded read-only recovery mechanism after submission.
>
> Prefer reusing the submitted transactionHash.
>
> When receipt/readback fails after submission:
>
> A. Preserve transactionHash.
>
> B. Attempt bounded recovery using read-only calls:
>
> * getTransactionReceipt(transactionHash)
> * if not yet available, retry with small bounded backoff
> * once receipt exists:
>
>   * require status success
>   * read authoritative credential
>   * verify requested target state and identity invariants.
>
> C. Do not call writeContract again.
>
> If bounded recovery succeeds:
> return normal HTTP 200:
>
> {
> changed: true,
> recovered: true,
> transactionHash: "0x...",
> credential: authoritativeState
> }
>
> If the transaction is still unresolved after the bounded server recovery
> window:
>
> return HTTP 202, not 500:
>
> {
> pending: true,
> transactionHash: "0x...",
> requestedState: "ACTIVE|INACTIVE"
> }
>
> No secret or raw RPC information.
>
> If receipt is confirmed reverted:
> return safe failure with transactionHash.
>
> PHASE 4 — PENDING NONCE GUARD
>
> Before any NEW write action:
>
> Compare DEV account latest and pending nonce.
>
> If pending > latest:
>
> reject the new write with HTTP 409 and a safe message equivalent to:
>
> "A previous Sepolia transaction is still pending. Wait for confirmation."
>
> This prevents duplicate submission after reload/restart while an earlier
> transaction remains unresolved.
>
> Do not attempt transaction replacement.
>
> PHASE 5 — CLIENT RECOVERY
>
> If POST returns HTTP 200 confirmed:
>
> * render returned credential immediately
> * reconcile with GET as already implemented.
>
> If POST returns HTTP 202 pending:
>
> * keep last confirmed credential appearance
> * disable the action button
> * display clearly:
>
> TRANSACTION SUBMITTED — WAITING FOR SEPOLIA CONFIRMATION
>
> * show shortened transaction hash
> * do NOT issue another POST
> * poll read-only reconciliation only.
>
> Use either:
>
> * existing GET /api/credential where sufficient, or
> * a narrowly scoped GET recovery/status endpoint if transaction receipt
>   knowledge is required.
>
> Do not introduce a generic transaction endpoint.
>
> Polling must:
>
> * never perform writes
> * use bounded/backoff timing
> * stop once authoritative credential reaches requested state
> * render that state
> * then re-enable controls.
>
> If polling cannot resolve after a reasonable demo timeout:
>
> show:
>
> "Transaction submitted. Confirmation is taking longer than expected.
> Reload to reconcile from ENSv2."
>
> Keep transaction hash visible.
>
> Do not label the action failed.
>
> A page reload must always reconstruct current credential state from ENSv2.
>
> PHASE 6 — READBACK FAILURE AFTER CONFIRMED RECEIPT
>
> Handle separately:
>
> receipt status success
> → credential readback temporarily fails.
>
> The transaction is already confirmed.
>
> Do NOT return WRITE_FAILED.
>
> Attempt bounded readback retry.
>
> If still unavailable:
> return/recover as a submitted-confirmed transaction requiring read-only state
> reconciliation.
>
> No additional transaction.
>
> PHASE 7 — UI LATENCY
>
> Improve clarity without faking state.
>
> While waiting:
>
> ACTIVATING ON SEPOLIA…
> or
> DEACTIVATING ON SEPOLIA…
>
> After hash exists:
>
> TRANSACTION SUBMITTED
> Waiting for confirmation…
>
> After receipt but readback is pending:
>
> CONFIRMED ON SEPOLIA
> Refreshing credential state…
>
> Do not render target ACTIVE/INACTIVE until authoritative state is available.
>
> PHASE 8 — TESTS
>
> Add transaction-free regression tests for at least:
>
> 1. submit → receipt/readback success
>    → 200 confirmed
>    → one write
>
> 2. submit → first receipt RPC failure → recovery receipt success
>    → 200
>    → recovered:true
>    → one write
>
> 3. submit → receipt success → first credential readback failure → retry success
>    → 200
>    → one write
>
> 4. submit → bounded receipt recovery exhausted
>    → HTTP 202
>    → transactionHash returned
>    → one write
>    → no automatic retry
>
> 5. HTTP 202 client path
>    → zero extra POSTs
>    → only read-only reconciliation.
>
> 6. pending nonce before a new action
>    → HTTP 409
>    → zero writes.
>
> 7. confirmed reverted receipt
>    → safe error with transactionHash
>    → zero resubmissions.
>
> 8. reload/current GET after previously completed transaction
>    → authoritative current ENSv2 state.
>
> 9. existing semantic idempotency tests continue to pass.
>
> 10. existing Host/Origin/body/write-lock/security tests continue to pass.
>
> PHASE 9 — ERROR MODEL
>
> Preserve useful safe public evidence:
>
> * stage
> * transactionHash
> * public error code/message
>
> Never serialize:
>
> * raw viem Error
> * RPC URL
> * environment
> * private key
> * stack trace
>
> PHASE 10 — VALIDATION
>
> Run safe local validation only:
>
> * syntax checks
> * full tests
> * git diff --check
> * no-touch audit
> * public secret scan
>
> Do NOT:
>
> * broadcast transactions
> * run live UI writes
> * access private key contents
> * modify firmware
> * modify ENS architecture
> * add dependencies
> * commit
> * push
>
> SCOPE
>
> Modify only files required in the current demo/write-recovery layer.
>
> Do not touch:
>
> * firmware/
> * STATUS.md
> * WORKLOG.md
> * PROJECT.md
> * README.md
> * NFC bridge
> * account abstraction
> * Aliro
> * transfers
> * metadata
> * loyalty
> * Solidity
>
> RETURN
>
> # POST-SUBMISSION RECOVERY FIX
>
> ## ROOT FAILURE PATH
>
> ## TRANSACTION STATE MODEL
>
> ## SERVER RECOVERY
>
> ## PENDING NONCE GUARD
>
> ## CLIENT RECOVERY
>
> ## UX STATES
>
> ## REGRESSION TESTS
>
> ## VALIDATION
>
> ## FILES CHANGED
>
> ## SAFE LIVE REVALIDATION
>
> ## RISKS
>
> End exactly:
>
> REVIEW: PASS
>
> or
>
> REVIEW: STOP
