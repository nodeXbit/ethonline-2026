# Project task packet 007: TASK — Diagnose and fix the duplicate/sequential write problem discovered during the

- Classification: **ORIGINAL / SANITIZED**
- Redactions applied: none
- Source: retained project-specific Control Tower packet in the local Codex record

---

> TASK
>
> Diagnose and fix the duplicate/sequential write problem discovered during the
> interactive ENSv2 demo validation.
>
> The user must NOT act as a terminal relay.
>
> Do the safe diagnosis, local reproduction and code fix yourself.
>
> DO NOT SEND ANY BLOCKCHAIN TRANSACTION.
>
> INCIDENT
>
> Interactive validation stopped because the UI was not sufficiently responsive
> and more than one sequential transaction was submitted.
>
> Confirmed activation:
>
> 0xeb1e9a6fefb6d22df1eb37e0fadd6170c454abc27ea714969a077107fdd20b82
> block 11664610
>
> Later confirmed transactions:
>
> 0xedf845c3fa77d441383d313cc6f3609cd39110e276d4823ff206dcbdbffe1eaf8
> block 11664634
>
> 0xbb84d471f4ad153cf9363fbc37ecee1d2b5e9914e2a6c8e19f2fd14ae11df283d
> block 11664649
>
> Submitted hash seen without an observed receipt:
>
> 0x1d1ae5ee9730b3e5f29f58dd139a6e3e04213590c2fc7edbd9c97526933bf584f
>
> Final observed authoritative state:
>
> REGISTERED
> access.active = false
> AUTHORIZATION = DENY
>
> Local UI checkpoint:
>
> 580e4f2
> feat: add interactive ENSv2 credential demo
>
> It has NOT been pushed.
>
> PHASE 1 — READ-ONLY ONCHAIN INCIDENT RECONSTRUCTION
>
> Using public/read-only Sepolia RPC only:
>
> 1. Inspect receipts/status for all four transaction hashes.
> 2. Determine exact DEV latest and pending nonce.
> 3. Confirm whether any transaction is currently pending.
> 4. Decode/classify the transactions if useful:
>
>    * activate?
>    * deactivate?
>    * duplicate?
> 5. Reconstruct the current credential/access state.
> 6. Do not broadcast anything.
>
> If any transaction is still pending, clearly flag it.
>
> PHASE 2 — LOCAL ROOT-CAUSE DIAGNOSIS
>
> Inspect:
>
> * demo/public/index.html
> * demo/public/app.js
> * demo/server.mjs
> * scripts/ensv2/persistent-access.mjs
> * relevant tests
>
> Determine exactly how multiple sequential POST/write calls could occur.
>
> Specifically inspect for:
>
> * button type / form submit behavior
> * duplicate event listeners
> * click + submit paths
> * missing synchronous in-flight guard
> * guard being cleared before UI authoritative refresh completes
> * server write lock lifetime
> * behavior when POST succeeds but subsequent GET fails/delays
> * whether activate can write when access is already effectively ACTIVE
> * whether deactivate can write when access.active is already false
> * whether repeated sequential same-target requests still call setData
> * any automatic retry behavior
>
> Do not guess.
> Identify the concrete path(s).
>
> PHASE 3 — REQUIRED SAFETY MODEL
>
> Implement defense in depth.
>
> A. SERVER-SIDE SEMANTIC IDEMPOTENCY
>
> Before any write, read authoritative current state.
>
> For ACTIVATE:
>
> * if registration is valid
> * access.active == true
> * access.validUntil > current pinned block timestamp
> * authorization already ALLOW
>
> then DO NOT write.
>
> Return a successful no-op response with:
>
> changed: false
> transactionHash: null
> credential: authoritative public state
>
> If active=true but access.validUntil has expired, activation is NOT a no-op;
> the validated renewal path may write a new future deadline.
>
> For DEACTIVATE:
>
> If access record exists and access.active == false:
>
> DO NOT write.
>
> Return:
>
> changed: false
> transactionHash: null
> credential: authoritative public state
>
> Otherwise perform the validated deactivation write.
>
> No repeated request for an already-achieved target state may create another
> transaction.
>
> B. SERVER WRITE LOCK
>
> Keep the existing in-process write lock.
>
> A second overlapping request must remain HTTP 409.
>
> The lock must cover:
>
> * pre-write read
> * transaction simulation/submission
> * receipt
> * verified readback
> * response construction
>
> C. CLIENT IN-FLIGHT GUARD
>
> In app.js:
>
> * set an inFlight flag synchronously at the beginning of the action handler
>   before the first await
> * ignore subsequent activation/deactivation attempts while true
> * disable the action button immediately
> * ensure buttons are type="button" unless form behavior is explicitly needed
> * ensure there is only one event path that can invoke an action
> * clear inFlight only after the UI has a safe confirmed/recovered state
>
> D. CONFIRMED POST RESPONSE
>
> The POST response already contains verified ENSv2 credential state.
>
> After successful POST:
>
> 1. immediately render the returned confirmed credential state;
> 2. display transaction confirmation when changed=true;
> 3. if changed=false, display an "already in requested state" result;
> 4. then perform GET /api/credential as reconciliation.
>
> The card must NOT remain stale while waiting for the reconciliation GET.
>
> If reconciliation GET fails after a successful POST:
>
> * retain the verified state returned by the POST
> * show a warning that refresh/reconciliation failed
> * do NOT revert the visual card to the old state
> * do NOT automatically retry the POST.
>
> E. CLEAR PENDING UX
>
> During action:
>
> Show an unmistakable persistent message:
>
> ACTIVATING ON SEPOLIA — DO NOT CLICK AGAIN
>
> or
>
> DEACTIVATING ON SEPOLIA — DO NOT CLICK AGAIN
>
> Button disabled immediately.
>
> Do not imply success until confirmed readback.
>
> PHASE 4 — TESTS
>
> Add regression tests without real transactions.
>
> At minimum prove:
>
> 1. Two overlapping activate requests:
>
>    * first reaches mocked write
>    * second gets 409
>    * only one write
>
> 2. Two sequential activate requests:
>
>    * first changes INACTIVE → ACTIVE
>    * second observes already ACTIVE
>    * second produces changed:false
>    * total writes = 1
>
> 3. Two sequential deactivate requests:
>
>    * first ACTIVE → INACTIVE
>    * second no-op
>    * total writes = 1
>
> 4. Activate when active=true but deadline expired:
>
>    * performs one renewal write
>    * is not treated as no-op
>
> 5. Double client action invocation while inFlight:
>
>    * only one POST is initiated, if practical to test at current abstraction.
>
> 6. Successful POST + failed reconciliation GET:
>
>    * confirmed POST credential state remains displayed logically
>    * no second POST/retry is triggered.
>
> 7. Button/form markup cannot produce both submit and click write paths.
>
> 8. Existing security/origin/body/route tests still pass.
>
> Do not introduce a framework just to test browser behavior.
>
> PHASE 5 — VALIDATION
>
> Run:
>
> * syntax checks
> * full local test suite
> * git diff --check
> * safe server tests
> * no-touch audit
> * secret/public asset scan
>
> Do not access private key contents.
> Do not send transactions.
> Do not commit.
> Do not push.
>
> PHASE 6 — INCIDENT / FIX REPORT
>
> Return:
>
> # DUPLICATE WRITE DIAGNOSIS
>
> ## ONCHAIN TRANSACTION RECONSTRUCTION
>
> ## CURRENT NONCE / PENDING STATE
>
> ## ROOT CAUSE
>
> ## SERVER IDEMPOTENCY FIX
>
> ## CLIENT DUPLICATE-PREVENTION FIX
>
> ## PENDING / CONFIRMATION UX
>
> ## REGRESSION TESTS
>
> ## VALIDATION
>
> ## FILES CHANGED
>
> ## SAFE NEXT VALIDATION
>
> ## USER ACTION REQUIRED
>
> USER ACTION REQUIRED must contain only genuinely manual actions.
>
> End exactly:
>
> REVIEW: PASS
>
> or
>
> REVIEW: STOP
