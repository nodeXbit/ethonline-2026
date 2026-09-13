# Project task packet 092: TASK — INDEPENDENT RE-AUDIT OF PASS STUDIO SAFETY FIX

- Classification: **ORIGINAL / SANITIZED**
- Redactions applied: none
- Source: retained project-specific Control Tower packet in the local Codex record

---

> TASK — INDEPENDENT RE-AUDIT OF PASS STUDIO SAFETY FIX
>
> MISSION
>
> Independently verify the uncommitted LockENS Pass Studio + Astra safety fix.
>
> DO NOT modify code.
>
> DO NOT perform blockchain writes.
>
> DO NOT sign anything.
>
> DO NOT commit or push.
>
> This is NOT a broad architecture review.
>
> Timebox toward answering one question:
>
> ARE REAL STUDIO WRITES SAFE ENOUGH FOR CONTROLLED TESTING?
>
> ============================================================
> BASELINE
> ============================================================
>
> Pushed baseline:
>
> 09e8153a01bca6283a773d41c77572442b742337
>
> The worktree intentionally contains:
>
> - LockENS Pass Studio implementation;
> - the subsequent Astra safety fix.
>
> The previous Astra fix report claims:
>
> W01 FIXED
> R01 FIXED
> I01 FIXED
> R02 FIXED
> M01 FIXED
> T01 FIXED
> V01 FIXED
>
> and hardening:
>
> A01
> D01
> R03
> R04
> S01
>
> Current reported validation:
>
> Android:
> 210 passed
>
> Node:
> 173 passed
>
> assemble:
> PASS
>
> diff check:
> PASS
>
> No real Studio writes have occurred.
>
> ============================================================
> REVIEW PRINCIPLE
> ============================================================
>
> Do not trust the previous Astra report.
>
> Reconstruct the fix from the actual diff.
>
> Attempt to DISPROVE each claimed fix.
>
> Do not recommend product expansion.
>
> Do not review cosmetic UX unless it affects irreversible write trust.
>
> ============================================================
> 1 — W01 WALLET / ACTION COORDINATION
> ============================================================
>
> Try to find any interleaving where:
>
> - wallet A review leads to provider B;
> - UI shows B while A submits;
> - overlapping action releases another action's lock;
> - stale callback releases a newer lease;
> - wallet switching occurs after final preflight but before provider send.
>
> Inspect the lease/generation implementation and all release paths:
>
> success
> exception
> cancellation
> timeout
> restart
> early return
>
> Require:
>
> only lease owner can release.
>
> If any plausible provider/wallet mismatch remains:
>
> STOP.
>
> ============================================================
> 2 — R01 LATE HASH / NO-BROADCAST
> ============================================================
>
> Reproduce or reason through:
>
> SUBMITTING_NO_HASH
> +
> provider call still live
> +
> concurrent reconciliation
> +
> unchanged nonces
> +
> late hash
>
> Require:
>
> NO_BROADCAST_PROVEN impossible while live provider exists.
>
> Late matching hash must be retained.
>
> After process death:
>
> verify reconciliation is conservative enough that one unchanged nonce read cannot
> permit unsafe replacement.
>
> Try repeated recovery calls.
>
> Try provider returning after timeout/cancellation boundaries.
>
> ============================================================
> 3 — I01 SESSION ISOLATION
> ============================================================
>
> Attempt:
>
> credential A
> credential B
> same wallet
>
> and:
>
> credential A
> wallet A
> credential B
> wallet B
>
> Inspect every transition/callback.
>
> Require no security-critical mutation through:
>
> global activeKey
> current credential
> last selected session
> wallet-only lookup
>
> Try:
>
> A callback after B selected
> A restart while B draft exists
> TX1 A confirmed while B is reviewed
> TX2 A finalizes while B active
> wallet switch during recovery
>
> Any A→B state mutation is STOP.
>
> ============================================================
> 4 — R02 CRASH MATRIX
> ============================================================
>
> Review the reconciliation table against actual code.
>
> Try every journal state relevant to:
>
> TX1
> TX2
>
> including persistence disagreement between:
>
> session
> journal
>
> Require idempotency.
>
> Especially verify:
>
> TX1 REVERTED
> TX2 REVERTED
> NO_BROADCAST_PROVEN
> UNKNOWN
> CONFIRMED
> crash before/after session transition
>
> Confirmed transaction must never resend.
>
> ============================================================
> 5 — M01 MANAGEMENT DURABILITY
> ============================================================
>
> Scenario:
>
> management A receives CONFIRMED
> authoritative readback fails
> user attempts management B
> restart
>
> Require:
>
> A intent remains durable.
>
> B cannot overwrite/clear A.
>
> Finalization targets operation ID exactly.
>
> No second mutation begins while A's result is unresolved unless explicitly and
> safely designed.
>
> ============================================================
> 6 — T01 TIME VALIDATION
> ============================================================
>
> Try:
>
> Review Restore while valid.
>
> Wait/pass fresh chain time beyond accessValidUntil.
>
> Final confirmation.
>
> Require:
> NO send.
>
> Try issue TX2 after access validity has expired.
>
> Try changing registration expiry underneath a reviewed management action.
>
> Require final preflight uses fresh block timestamp/current registration.
>
> Also verify UI status:
>
> active=true
> validUntil past
>
> must NOT display Allowed.
>
> ============================================================
> 7 — V01 VISITOR INTERMEDIATE CHANGES
> ============================================================
>
> After synthetic TX1:
>
> A. transfer to new owner
> B. self-revoke transfer-admin bit
>
> Then attempt TX2 continuation.
>
> Require:
>
> explicit change state
> fresh review
> current owner/roles displayed
> no silent original-recipient assumptions
> no re-registration
>
> Verify newly acknowledged current state cannot accidentally alter the reviewed
> resolver records.
>
> ============================================================
> 8 — HARDENING SPOT CHECKS
> ============================================================
>
> A01:
> invalid mixed-case checksum rejected.
>
> D01:
> Feb 30 rejected.
> Madrid DST gap rejected.
> Madrid overlap handled explicitly.
>
> R03:
> known tx still finalizes after nonce advances multiple times.
>
> R04:
> wrong receipt hash rejected.
> wrong block linkage rejected.
> RPC inconsistency => UNKNOWN, not CONFIRMED.
>
> S01:
> fake provider exception containing:
> SECRET_API_KEY_123
> must not persist/log that content.
>
> ============================================================
> 9 — WRITE IDENTITY MODEL
> ============================================================
>
> Inspect the frozen WriteIntent identity.
>
> Require that security-critical async work is bound to:
>
> operation/session identity
> wallet
> provider generation
> chain
> credential
> action
> target
> value
> calldata fingerprint
> reviewed business intent
>
> Look for any mutable object/reference that can change after final confirmation.
>
> ============================================================
> 10 — TRANSACTION ENGINE REGRESSION
> ============================================================
>
> Verify that the safety fix did NOT introduce:
>
> deadlock leaving writes permanently disabled
>
> lease never released after safe failure
>
> automatic retry
>
> lost late hash
>
> inability to recover historical staff TX1/TX2
>
> journal migration corruption
>
> cross-wallet recovery blockage
>
> unsafe cancellation semantics
>
> ============================================================
> 11 — EXISTING STAFF
> ============================================================
>
> Read-only verify:
>
> staff-001.keys.demo-access.eth
>
> still unchanged.
>
> Historical confirmed journal/session must still load safely.
>
> Issuer nonce must remain 3.
>
> No write.
>
> ============================================================
> 12 — READ-ONLY LIVE SIMULATION
> ============================================================
>
> Repeat only the highest-value simulations needed to verify no regression:
>
> fresh synthetic STAFF
> fresh synthetic VISITOR
> fresh synthetic CONTRACTOR
>
> No broadcasts.
>
> Confirm:
>
> register intent
> role bitmap
> configuration intent
> suspended contractor readiness
>
> Do not spend time redoing already-settled broad ENS research.
>
> ============================================================
> 13 — TEST QUALITY
> ============================================================
>
> Inspect the 31 new adversarial tests.
>
> Determine whether they genuinely reproduce the original failures or merely test
> helper abstractions.
>
> Identify any important source-level interleaving still uncovered.
>
> Temporary ignored scripts are allowed.
>
> Do not modify repository tests.
>
> ============================================================
> FINDING FORMAT
> ============================================================
>
> Only report material findings.
>
> For each:
>
> ID
> Severity P0/P1/P2/P3
> Evidence
> Concrete failure scenario
> Required before controlled real write: YES/NO
> Smallest fix
>
> ============================================================
> RETURN
> ============================================================
>
> # LOCKENS PASS STUDIO — INDEPENDENT ASTRA RE-AUDIT
>
> ## VERDICT
>
> CONTROLLED REAL STUDIO WRITES:
>
> GO
> GO WITH REQUIRED FIXES
> STOP
>
> ## W01
>
> ## R01
>
> ## I01
>
> ## R02
>
> ## M01
>
> ## T01
>
> ## V01
>
> ## HARDENING
>
> A01:
> D01:
> R03:
> R04:
> S01:
>
> ## WRITE IDENTITY
>
> ## RECOVERY
>
> ## ADVERSARIAL TEST QUALITY
>
> ## LIVE READ-ONLY CHECK
>
> Staff:
> Visitor:
> Contractor:
> Existing staff:
>
> ## REMAINING MATERIAL FINDINGS
>
> ## REQUIRED FIXES BEFORE WRITE
>
> Only true blockers.
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
> Files modified:
> NONE
>
> Commit:
> NONE
>
> Push:
> NONE
>
> End exactly:
>
> INDEPENDENT RE-AUDIT COMPLETE — NO IMPLEMENTATION PERFORMED
