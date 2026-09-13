# Project task packet 009: TASK — Diagnose and fix the pre-submission HTTP failure from the latest interactive

- Classification: **ORIGINAL / SANITIZED**
- Redactions applied: none
- Source: retained project-specific Control Tower packet in the local Codex record

---

> TASK
>
> Diagnose and fix the pre-submission HTTP failure from the latest interactive
> demo validation.
>
> The user should NOT execute diagnostic commands.
>
> DO NOT SEND ANY BLOCKCHAIN TRANSACTION.
>
> INCIDENT
>
> Browser action:
>
> Activate access
>
> Browser displayed:
>
> HTTP request failed.
>
> Verified afterward:
>
> DEV latest nonce: 16
> DEV pending nonce: 16
> no pending transaction
> no transactionHash produced
> credential remains REGISTERED
> access remains INACTIVE
> authorization remains DENY
>
> Therefore the failure occurred BEFORE blockchain submission.
>
> Current local HEAD:
>
> c3d4c7d
> fix: recover submitted demo transactions safely
>
> Working tree was clean after validation.
>
> GOAL
>
> Identify the exact failing stage and make safe pre-submission RPC failures
> recoverable without risking duplicate writes.
>
> Do not modify ENSv2 architecture.
>
> PHASE 1 — EXACT REQUEST PATH
>
> Inspect:
>
> demo/public/app.js
> demo/public/access-action.js
> demo/server.mjs
> scripts/ensv2/persistent-access.mjs
> scripts/ensv2/contracts.mjs
> relevant tests
>
> Trace exactly:
>
> browser click
> → fetch POST
> → Host/Origin/body validation
> → write lock
> → current credential read
> → latest/pending nonce
> → simulation
> → second nonce guard
> → writeContract
>
> Identify every point BEFORE writeContract that can produce the browser's
> generic "HTTP request failed."
>
> Also inspect how the browser handles non-2xx JSON responses.
>
> Determine whether it discards useful server error information.
>
> PHASE 2 — REPRODUCE WITHOUT BLOCKCHAIN WRITES
>
> Use the existing dependency-injected/mocked server test architecture.
>
> Reproduce the exact browser request shape:
>
> POST /api/activate
>
> Host:
> 127.0.0.1:4173
>
> Origin:
> http://127.0.0.1:4173
>
> empty body
>
> Verify request validation itself succeeds.
>
> Inject failures separately into:
>
> * credential read
> * latest nonce read
> * pending nonce read
> * simulation
> * second nonce read
> * write preparation BEFORE actual submission
>
> Determine what HTTP status/body each produces and what the client displays.
>
> Do not invoke a real writeContract.
>
> PHASE 3 — ERROR VISIBILITY
>
> The UI must never collapse a safe structured API error into only:
>
> HTTP request failed.
>
> For non-2xx API responses:
>
> * parse the safe JSON error body when available
> * display its public message/stage
> * preserve current confirmed credential card
> * allow retry only when no transactionHash exists
> * never expose raw Error, stack, RPC URL, environment or secret
>
> For example:
>
> Sepolia RPC temporarily unavailable before transaction submission.
> No transaction was sent. Retry is safe.
>
> or equivalent concise wording.
>
> PHASE 4 — SAFE PRE-SUBMISSION RETRY
>
> Add bounded retry/backoff ONLY around operations that are guaranteed
> read-only / non-broadcasting before writeContract.
>
> Candidates include:
>
> * authoritative credential reads
> * latest/pending nonce reads
> * simulateContract
> * other public RPC preflight reads
>
> Do NOT wrap writeContract in an automatic retry.
>
> Rules:
>
> * small bounded number of attempts
> * short backoff appropriate for local demo
> * classify obviously deterministic contract/application failures as
>   non-retryable
> * only transient transport/RPC failures should retry
> * if exhausted, return a safe pre-submission error stating that no
>   transaction was sent
>
> Once writeContract is invoked and a hash may exist, the existing
> post-submission recovery state machine remains authoritative.
>
> Do not blur the pre/post-submission boundary.
>
> PHASE 5 — RPC CONFIGURATION AUDIT
>
> Inspect how SEPOLIA_RPC_URL is selected.
>
> Important current project fact:
>
> SEPOLIA_RPC_URL has historically been left empty, using viem's/default public
> Sepolia transport fallback.
>
> Determine whether the repeated transient failures observed in this project are
> consistent with relying on that public fallback.
>
> Do not access .env.local contents.
>
> Report whether, for demo reliability, Control Tower should configure a
> dedicated Sepolia RPC endpoint in the existing SEPOLIA_RPC_URL variable.
>
> Do not add a provider SDK.
>
> Do not hardcode an API key or endpoint.
>
> The implementation must continue to work with either:
>
> * supplied SEPOLIA_RPC_URL
> * existing fallback
>
> PHASE 6 — CLIENT RETRY SAFETY
>
> Confirm:
>
> pre-submission failure with no transactionHash:
> → button can be safely re-enabled after bounded server retry exhausts
>
> post-submission failure with transactionHash:
> → button remains in recovery path
> → no new POST
>
> These must remain visibly different.
>
> PHASE 7 — REGRESSION TESTS
>
> Add tests covering at minimum:
>
> 1. exact browser POST Host/Origin/body accepted
> 2. safe structured server error is rendered instead of generic
>    "HTTP request failed"
> 3. transient credential-read RPC failure recovers
> 4. transient nonce RPC failure recovers
> 5. transient simulation RPC failure recovers
> 6. retries exhausted before submission:
>
>    * zero writes
>    * no transactionHash
>    * safe retryable error
> 7. deterministic simulation/contract failure is not repeatedly retried
> 8. writeContract itself is never automatically retried
> 9. existing semantic idempotency tests pass
> 10. existing post-submit hash recovery tests pass
> 11. security/origin/body tests pass
>
> PHASE 8 — VALIDATION
>
> Run:
>
> * syntax checks
> * full local suite
> * git diff --check
> * public asset secret scan
> * no-touch audit
>
> Do not:
>
> * access environment file contents
> * send blockchain transactions
> * install dependencies
> * commit
> * push
>
> RETURN
>
> # PRE-SUBMISSION FAILURE DIAGNOSIS
>
> ## EXACT FAILURE CANDIDATES
>
> ## LOCAL REPRODUCTION
>
> ## ROOT CAUSE
>
> ## SAFE RPC RETRY
>
> ## CLIENT ERROR UX
>
> ## RPC CONFIGURATION RECOMMENDATION
>
> ## REGRESSION TESTS
>
> ## VALIDATION
>
> ## FILES CHANGED
>
> ## SAFE NEXT STEP
>
> End exactly:
>
> REVIEW: PASS
>
> or
>
> REVIEW: STOP
