# Project task packet 094: TASK — FIX FINAL THREE STUDIO COORDINATION DEFECTS

- Classification: **ORIGINAL / SANITIZED**
- Redactions applied: none
- Source: retained project-specific Control Tower packet in the local Codex record

---

> TASK — FIX FINAL THREE STUDIO COORDINATION DEFECTS
>
> CONTEXT
>
> Independent Astra re-audit concluded:
>
> CONTROLLED REAL STUDIO WRITES: STOP
>
> Only two blockers remain, plus one closely related availability defect:
>
> RA-01
> Actual runner/provider identity is not enforced at send.
>
> RA-02
> Confirming credential A can implicitly cancel credential B's prepared operation.
>
> RA-03
> A known pre-provider failure can become permanently UNKNOWN even though provider
> invocation provably never started.
>
> All other previously reviewed P0/P1 findings passed the independent re-audit.
>
> MISSION
>
> Implement ONLY RA-01, RA-02 and RA-03.
>
> Do not redesign Studio.
>
> Do not change ENSv2 semantics.
>
> Do not broaden product scope.
>
> NO real blockchain writes.
> NO signatures.
> NO commit/push yet.
>
> ============================================================
> RA-01 — BIND ACTUAL RUNNER / PROVIDER AT SEND BOUNDARY
> ============================================================
>
> PROVEN FAILURE
>
> A stale Activity may retain runner/provider A.
>
> Global coordinator may now legitimately issue a permit for active wallet B.
>
> The current permit validation can succeed for B while the retained runner invokes
> provider A.
>
> Readback catches TRANSACTION_FROM_MISMATCH only AFTER provider invocation.
>
> This is unacceptable.
>
> REQUIRED INVARIANT
>
> Immediately before ANY provider invocation:
>
> permit identity
> ==
> runner identity
> ==
> actual captured provider wallet identity/generation
> ==
> frozen reviewed write intent
>
> There must be no path where:
>
> permit B + runner/provider A
>
> can invoke the provider.
>
> ============================================================
> RUNNER IDENTITY
> ============================================================
>
> Bind every ContractTransactionRunner instance immutably to explicit identity
> created alongside its actual Privy wallet/provider.
>
> At minimum bind:
>
> wallet address
> provider identity/generation token
> chain
>
> Do NOT infer runner identity later from mutable Activity/global state.
>
> The runner must expose or internally validate this immutable identity.
>
> ============================================================
> SEND-BOUNDARY CHECK
> ============================================================
>
> Before provider.send:
>
> require exact equality between:
>
> WriteIntent wallet
> WriteIntent provider generation
> Write permit wallet
> Write permit provider generation
> Runner wallet
> Runner provider generation
>
> If ANY mismatch:
>
> - provider invocation count MUST remain zero;
> - operation must transition truthfully to a safe pre-send outcome;
> - no ambiguous broadcast state;
> - user must re-review using current wallet/provider.
>
> Do not rely on post-send FROM mismatch detection as the protection.
>
> Keep post-send intent verification as defense in depth.
>
> ============================================================
> STALE ACTIVITY
> ============================================================
>
> Audit the Activity/provider lifecycle that allowed an old Activity to retain a
> stale runner.
>
> Use the smallest safe solution.
>
> Options may include:
>
> - force Activity runner reconstruction/synchronization when active wallet
>   generation changes;
> - invalidate stale runner explicitly;
> - immutable runner check at submission boundary.
>
> The send-boundary check is mandatory even if UI synchronization is improved.
>
> Do NOT rely only on manifest singleTask/singleTop changes.
>
> ============================================================
> RA-01 TESTS
> ============================================================
>
> Reproduce the independent re-audit probe exactly:
>
> runner/provider A retained
>
> global active binding → B
>
> valid permit for B
>
> attempt submission through stale A runner
>
> Require:
>
> provider A send count == 0
> provider B send count == 0 unless explicitly using B runner
> safe mismatch result before send
>
> Then test:
>
> fresh B runner + B permit
> → exactly one provider B invocation in fake/test environment.
>
> Also:
>
> wallet switches A→B→A
> stale callbacks/runners cannot submit after their generation is invalidated.
>
> ============================================================
> RA-02 — NEVER CANCEL ANOTHER CREDENTIAL IMPLICITLY
> ============================================================
>
> PROVEN FAILURE
>
> createReviewedOperation currently performs wallet-wide cancellation of prepared
> operations.
>
> Confirming credential A can therefore:
>
> B journal:
> READY_TO_SUBMIT → CANCELLED
>
> B issuance:
> REGISTER_READY → DRAFT
>
> without any explicit user action on B.
>
> REQUIRED INVARIANT
>
> An action for credential/session A may NEVER mutate credential/session B.
>
> ============================================================
> PREPARED OPERATION POLICY
> ============================================================
>
> When final-confirming A:
>
> If there is another prepared/incomplete operation B for the same wallet:
>
> DO NOT cancel B automatically.
>
> Prefer:
>
> BLOCK A
>
> and return a clear human state:
>
> "Another pass has an unfinished transaction."
>
> Provide enough information to resume/resolve B.
>
> For this sprint, serialization is preferable to implicit cancellation.
>
> Explicit cancellation of an UNSENT operation may exist only through an action
> targeting that exact operation/session.
>
> Do not add broad wallet-wide cleanup.
>
> ============================================================
> OPERATION REPLACEMENT
> ============================================================
>
> If replacing/rebuilding an unsent reviewed operation for the SAME credential and
> SAME intended action is genuinely needed:
>
> target the exact operation ID/session ID.
>
> Never iterate through and cancel unrelated prepared operations merely because
> the wallet matches.
>
> ============================================================
> RA-02 TESTS
> ============================================================
>
> Reproduce:
>
> same wallet
>
> credential B:
> durable READY_TO_SUBMIT operation
>
> credential A:
> user final-confirms
>
> Require:
>
> A does NOT mutate B.
>
> Expected safe behavior:
>
> A is blocked
> B remains byte-for-byte/session-state equivalent
>
> Then explicitly resume/cancel B by B's identity and prove A can proceed only
> after the conflict is resolved.
>
> Test:
>
> credential A TX1 vs B TX1
> A TX2 vs B TX1
> management A vs issue B
> management A vs management B
>
> No cross-session mutation.
>
> ============================================================
> RA-03 — PROVEN PRE-PROVIDER FAILURE
> ============================================================
>
> PROVEN FAILURE
>
> Submission state is persisted before:
>
> onChanged(claimed)
>
> If that product-persistence callback throws:
>
> provider invocation never begins
>
> but recovery later treats SUBMITTING_NO_HASH conservatively as potentially
> broadcast forever.
>
> This is safe but can permanently brick writes.
>
> Fix without weakening R01.
>
> ============================================================
> PROVIDER INVOCATION EVIDENCE
> ============================================================
>
> Persist or otherwise durably distinguish at least:
>
> SUBMISSION_CLAIMED
> PROVIDER_INVOCATION_STARTED
>
> The exact naming may follow current state design.
>
> Critical requirement:
>
> If failure occurs BEFORE provider invocation begins and the application has
> durable proof of that fact:
>
> it may safely transition to a non-broadcast/cancellable outcome.
>
> If provider invocation has begun:
>
> retain existing conservative semantics.
>
> Never infer "provider never invoked" merely because no hash exists.
>
> ============================================================
> PERSISTENCE ORDER
> ============================================================
>
> Design the ordering carefully.
>
> We need a crash-safe distinction between:
>
> A.
> operation claimed
> provider definitely NOT invoked
>
> and:
>
> B.
> provider may have been invoked
>
> The marker indicating provider invocation may occur immediately before entering
> the provider call, but reason explicitly about the crash window.
>
> If an atomic perfect distinction is impossible with current architecture:
>
> prefer conservative UNKNOWN for the ambiguous crash boundary.
>
> Only grant safe cancellation where durable evidence proves no provider
> invocation was possible.
>
> Do NOT weaken late-hash retention.
>
> ============================================================
> RA-03 EXPECTED BEHAVIOR
> ============================================================
>
> Case:
>
> claim persisted
> product callback fails
> provider send count = 0
>
> Require:
>
> safe recoverable pre-send failure
> no permanent UNKNOWN
> new operation only after explicit re-review/re-arm according to existing UX
>
> Case:
>
> provider invocation marker persisted
> process dies before hash
>
> Require:
>
> UNKNOWN / reconciliation
> NO blind retry.
>
> ============================================================
> RA-03 TESTS
> ============================================================
>
> Reproduce the previous probe:
>
> product persistence callback throws before provider invocation.
>
> Require:
>
> provider invocation count 0
>
> recovery recognizes proven pre-provider failure
>
> no permanent UNKNOWN
>
> no unsafe auto-send
>
> Then test:
>
> crash/failure after provider-invocation marker
> before returned hash
>
> Require conservative UNKNOWN.
>
> Delayed hash test from R01 must remain green.
>
> ============================================================
> DO NOT REGRESS PREVIOUS FIXES
> ============================================================
>
> Re-run and preserve:
>
> W01 lease ownership
>
> R01 live-provider late hash
>
> I01 explicit session identity
>
> R02 crash matrix
>
> M01 management durability
>
> T01 fresh time
>
> V01 visitor intermediate-state handling
>
> A01
>
> D01
>
> R03
>
> R04
>
> S01
>
> Do not modify them unless strictly necessary for these three fixes.
>
> ============================================================
> WRITE-IDENTITY CONSOLIDATION
> ============================================================
>
> After RA-01, there should be no ambiguity about these identities:
>
> ACTIVE UI WALLET
>
> FROZEN WRITE WALLET
>
> WRITE PERMIT WALLET
>
> RUNNER WALLET
>
> ACTUAL PROVIDER WALLET
>
> All five must resolve to the same immutable identity/generation at send.
>
> Add one helper/invariant assertion if that makes the property obvious and
> testable.
>
> Do not create a large framework.
>
> ============================================================
> PHYSICAL UI BEHAVIOR
> ============================================================
>
> No real write.
>
> Install APK with adb -r.
>
> Inspect:
>
> Studio
> wallet switching
> unfinished-operation conflict presentation if safely reproducible using
> test/debug state
>
> The user must receive a human message for RA-02, not an exception class.
>
> No final transaction action on real chain.
>
> ============================================================
> ADVERSARIAL SELF-REVIEW
> ============================================================
>
> Before returning:
>
> attempt to reproduce RA-01 and RA-02 using the exact independent re-audit
> interleavings.
>
> Do not substitute helper-only tests.
>
> Attempt RA-03 around persistence/crash boundaries.
>
> Check specifically that the fixes did NOT introduce:
>
> wallet permanently locked
>
> operation permanently locked
>
> implicit cancellation elsewhere
>
> provider marker that loses late hashes
>
> automatic retry
>
> historical staff journal incompatibility
>
> ============================================================
> VALIDATION
> ============================================================
>
> Run full:
>
> Android unit tests
>
> assembleDebug
>
> Node tests
>
> git diff --check
>
> Use actual counts.
>
> Repeat read-only Studio simulations if the transaction-runner diff could affect
> encoding/submission validation.
>
> Re-read existing staff.
>
> Issuer nonce must remain 3.
>
> No real write.
>
> ============================================================
> RETURN
> ============================================================
>
> # LOCKENS PASS STUDIO — FINAL COORDINATION FIX
>
> ## RA-01
>
> Previous exploit:
>
> Fix:
>
> Runner identity:
>
> Send-boundary invariant:
>
> Exact adversarial reproduction:
>
> Result:
>
> ## RA-02
>
> Previous cross-session mutation:
>
> Fix:
>
> Conflict behavior:
>
> Exact reproduction:
>
> Result:
>
> ## RA-03
>
> Previous stranded state:
>
> Provider invocation evidence:
>
> Crash semantics:
>
> Exact reproduction:
>
> Result:
>
> ## REGRESSION
>
> W01:
> R01:
> I01:
> R02:
> M01:
> T01:
> V01:
>
> A01:
> D01:
> R03:
> R04:
> S01:
>
> ## TESTS
>
> New targeted tests:
>
> Android total:
>
> Node total:
>
> assemble:
>
> diff check:
>
> ## PHYSICAL
>
> APK installed:
>
> Real final-write button pressed:
> NO
>
> ## EXISTING STAFF
>
> Onchain:
>
> Historical journal:
>
> Issuer nonce:
>
> ## SECURITY
>
> Blockchain writes:
> 0
>
> Signatures:
> 0
>
> Secrets:
> 0
>
> ## GIT
>
> No commit/push.
>
> ## VERDICT
>
> CONTROLLED REAL STUDIO WRITES:
>
> GO
> GO WITH REQUIRED FIXES
> STOP
>
> If GO end exactly:
>
> FINAL COORDINATION FIX COMPLETE — READY FOR THIRD-PARTY SPOT CHECK
