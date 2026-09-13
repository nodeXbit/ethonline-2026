# Project task packet 067: TASK — FIX PRE-SUBMIT BOUNDARY AND REMOVE REDUNDANT GLOBAL ONE-SHOT LOGIC

- Classification: **ORIGINAL / SANITIZED**
- Redactions applied: none
- Source: retained project-specific Control Tower packet in the local Codex record

---

> TASK — FIX PRE-SUBMIT BOUNDARY AND REMOVE REDUNDANT GLOBAL ONE-SHOT LOGIC
>
> Do NOT perform a real blockchain transaction.
>
> Do NOT re-arm on the device.
>
> Do NOT call eth_sendTransaction against a real provider.
>
> The latest physical result proved:
>
> - human review appeared;
> - user explicitly pressed SUBMIT ONCE;
> - operation remained READY_TO_SUBMIT;
> - issuer nonce remained 0/0;
> - issuer balance remained 0.02 ETH;
> - no transaction was broadcast.
>
> The diagnostic classified:
>
> PRE-SUBMIT VALIDATION FAILURE.
>
> ============================================================
> IMPORTANT ARCHITECTURAL CORRECTION
> ============================================================
>
> Do NOT persist SUBMITTING_NO_HASH before read-only provider preflight.
>
> Correct semantics:
>
> READY_TO_SUBMIT
>
> -> local immutable-intent validation
>
> -> read-only provider preflight:
>    chain
>    wallet
>    latest nonce
>    pending nonce
>
> If ANY of those fail:
>
> - operation remains READY_TO_SUBMIT;
> - ZERO send occurs;
> - persist/display a sanitized PRE-SUBMIT failure stage/category;
> - user may retry the review later after the blocking condition is resolved.
>
> Only AFTER all read-only preflight passes:
>
> atomically persist:
>
> READY_TO_SUBMIT
> -> SUBMITTING_NO_HASH
>
> including the fresh pre-submit nonces.
>
> Then, and only then:
>
> provider.sendTransaction()
>
> The transition to SUBMITTING_NO_HASH is the durable send-claim.
>
> ============================================================
> PRIMARY SUSPECT — LEGACY IN-MEMORY ONE-SHOT GUARD
> ============================================================
>
> Audit the current MobileIssuerAdmissionRunner in-memory submission guard.
>
> The application previously used a one-shot M1 design.
>
> We now have a recoverable transaction engine where:
>
> - each operation has a unique operationId;
> - READY_TO_SUBMIT -> SUBMITTING_NO_HASH is atomic;
> - hash-bearing/UNKNOWN operations cannot resubmit;
> - NO_BROADCAST_PROVEN can explicitly create a NEW operation.
>
> Therefore a process-wide/global M1 one-shot guard is likely redundant and may
> incorrectly block a newly re-armed operation.
>
> Determine exactly:
>
> - guard type / field;
> - lifecycle;
> - when it becomes claimed;
> - whether RE-ARM resets it;
> - whether Activity/app process persistence affects it;
> - whether it is the likely reason the physical operation exited before
>   beginSubmission().
>
> If the transaction engine already supplies the required atomicity:
>
> REMOVE the redundant global one-shot guard from the normal M1 submission path.
>
> Do not weaken per-operation submission protection.
>
> The safety property must become:
>
> ONE SUBMISSION ATTEMPT PER OPERATION
>
> not:
>
> ONE SUBMISSION ATTEMPT PER APP PROCESS / INSTALLATION.
>
> ============================================================
> PRE-SUBMIT STAGES
> ============================================================
>
> Create explicit sanitized stages such as:
>
> VALIDATE_OPERATION
> VALIDATE_INTENT
> VALIDATE_WALLET
> SWITCH_CHAIN
> READ_CHAIN_ID
> READ_NONCE_LATEST
> READ_NONCE_PENDING
> CLAIM_SUBMISSION
>
> For any failure before CLAIM_SUBMISSION:
>
> operation state stays:
> READY_TO_SUBMIT
>
> persist safe diagnostic fields:
>
> failureStage
> safeErrorCategory
> sanitized short message
>
> The normal UI must show a human-readable failure.
>
> Developer diagnostics may show the stage/category.
>
> Do NOT overwrite the error immediately by rendering generic READY_TO_SUBMIT.
>
> ============================================================
> SUBMIT BOUNDARY
> ============================================================
>
> Implement one clear function conceptually:
>
> submitReviewedOperation(reviewIntent)
>
> Required sequence:
>
> 1. Find exact operationId.
> 2. Require state == READY_TO_SUBMIT.
> 3. Verify immutable review digest / intent.
> 4. Verify current authenticated wallet.
> 5. switchChain(Sepolia).
> 6. require eth_chainId == 11155111.
> 7. read latest nonce.
> 8. read pending nonce.
> 9. require latest == pending.
> 10. atomically persist:
>
>     state = SUBMITTING_NO_HASH
>     preLatestNonce = latest
>     prePendingNonce = pending
>     clear previous pre-submit diagnostic
>
> 11. ONLY NOW invoke provider.sendTransaction().
>
> If step 10 cannot persist:
>
> NO provider send.
>
> ============================================================
> AFTER PROVIDER SEND
> ============================================================
>
> Keep existing engine rules:
>
> If valid hash returned:
>
> persist HASH_RECEIVED + hash immediately.
>
> Never automatically resubmit hash-bearing operation.
>
> If provider throws before hash:
>
> persist:
>
> failureStage = PROVIDER_SEND
> safe exception/category/message
>
> then bounded no-broadcast reconciliation.
>
> If unchanged latest/pending and no hash:
>
> NO_BROADCAST_PROVEN
>
> Explicit re-arm required for another operation.
>
> ============================================================
> VISIBLE UX
> ============================================================
>
> For a pre-submit failure, do NOT show only:
>
> READY_TO_SUBMIT
>
> Show a human-readable status such as:
>
> Could not prepare the transaction.
>
> [technical details]
> Stage: READ_NONCE_PENDING
> Category: ...
>
> The operation may remain READY_TO_SUBMIT internally.
>
> This distinction is intentional:
>
> technical state:
> READY_TO_SUBMIT
>
> current blocking diagnostic:
> PRE_SUBMIT_ERROR
>
> ============================================================
> DIAGNOSTIC / PRODUCT SEPARATION
> ============================================================
>
> Do not redesign the product UI now.
>
> But create/retain a clear mapping seam:
>
> Internal state/category
> -> human-readable status
>
> Later Developer Diagnostics will expose:
>
> - operationId
> - technical state
> - failureStage
> - category
> - tx hash
> - block
> - nonces
>
> Normal product UI will not expose those by default.
>
> ============================================================
> TESTS
> ============================================================
>
> Add tests for:
>
> 1. Re-armed NEW operation is not blocked by a previous operation's in-memory
>    guard.
>
> 2. Two submissions of the SAME operation cannot occur.
>
> 3. Two different operationIds can each be submitted once when independently
>    authorized.
>
> 4. Local intent failure:
>    - stays READY_TO_SUBMIT
>    - visible/persisted error
>    - zero provider send.
>
> 5. Wallet mismatch:
>    same.
>
> 6. switchChain failure:
>    same.
>
> 7. wrong chain:
>    same.
>
> 8. latest/pending mismatch:
>    same.
>
> 9. read nonce exception:
>    same.
>
> 10. successful preflight:
>     atomically transitions to SUBMITTING_NO_HASH.
>
> 11. persistence failure during CLAIM_SUBMISSION:
>     zero provider sends.
>
> 12. provider send is impossible before SUBMITTING_NO_HASH persists.
>
> 13. provider throw after SUBMITTING_NO_HASH:
>     preserves PROVIDER_SEND diagnostic
>     -> reconciliation.
>
> 14. valid hash:
>     persists HASH_RECEIVED immediately.
>
> 15. pre-submit error is NOT overwritten by generic state rendering.
>
> 16. review/cancel still causes zero sends.
>
> No real provider transaction in tests.
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
> git diff --check
>
> Inspect diff.
>
> No HCE.
> No Node.
> No firmware.
> No ENS.
> No dependency changes expected.
> No commit/push.
>
> ============================================================
> RETURN
> ============================================================
>
> # M1 PRE-SUBMIT BOUNDARY FIX
>
> ## EXACT ROOT CAUSE
>
> Was the old in-memory one-shot guard responsible:
> YES / NO
>
> If NO, exact failing condition:
>
> ## OLD GUARD AUDIT
>
> Scope:
> Lifecycle:
> Why still needed / why removed:
>
> ## FINAL SUBMISSION SEQUENCE
>
> Show exact ordered stages.
>
> ## PRE-SUBMIT ERROR MODEL
>
> ## TRANSACTION ENGINE SAFETY
>
> One attempt per operation:
> PASS / FAIL
>
> Different re-armed operation can submit:
> PASS / FAIL
>
> Provider send only after SUBMITTING_NO_HASH persisted:
> PASS / FAIL
>
> ## UI ERROR PRESERVATION
>
> ## TESTS
>
> ## APK
>
> Path:
> SHA-256:
>
> ## SECURITY
>
> Real blockchain writes:
> 0
>
> Real eth_sendTransaction:
> 0
>
> ## MANUAL RETEST
>
> Install over current app without clearing data.
>
> The current NO_BROADCAST_PROVEN operation remains historical.
>
> Explicitly RE-ARM once.
>
> Reach READY_TO_SUBMIT.
>
> Open review.
>
> Press SUBMIT ONCE once.
>
> If a PRE-SUBMIT failure occurs:
> STOP and report its exact persisted stage/category.
>
> If provider send is reached:
> the engine will now preserve PROVIDER_SEND evidence if it fails.
>
> Do not automatically retry.
>
> End:
>
> M1 PRE-SUBMIT FIX: READY FOR ONE PHYSICAL ATTEMPT
>
> or:
>
> M1 PRE-SUBMIT FIX: STOP — <reason>
