# Project task packet 108: CONTINUE PRIV-01 — AUTHORIZE SIXTH PATH AND RESUME EXECUTION

- Classification: **ORIGINAL / SANITIZED**
- Redactions applied: none
- Source: retained project-specific Control Tower packet in the local Codex record

---

> CONTINUE PRIV-01 — AUTHORIZE SIXTH PATH AND RESUME EXECUTION
>
> The previous STOP was correct.
>
> No rewrite occurred.
> No push occurred.
> No source-repository change occurred.
> No code/blockchain change occurred.
>
> Current verified state remains:
>
> HEAD == origin/main ==
> d60572284854c005ab2d7c9f90f62da82ffc56b3
>
> tracked worktree:
> clean
>
> The verified private recovery bundle already exists and is valid.
>
> ============================================================
> CONTROL TOWER DECISION
> ======================
>
> Authorize the newly discovered sixth path:
>
> NFC_AB_TEST_PLAN.md
>
> Reason:
>
> ADB serial #2 appears there in reachable main and must receive the same approved neutral replacement:
>
> <GATE_SERIAL_2>
>
> The mandatory known sanitation scope is therefore now:
>
> 1. ANDROID_GATE_READER.md
> 2. ANDROID_GATE_READER_PHYSICAL_REPORT.md
> 3. GATE_STAND.md
> 4. NFC_PORT_DIAGNOSTIC_RESULT.md
> 5. WORKLOG.md
> 6. NFC_AB_TEST_PLAN.md
>
> Approved sensitive categories remain ONLY:
>
> * username-bearing Windows ADB path
> * ADB/device serial #1
> * ADB/device serial #2
> * ESP32 ROM MAC
>
> No credential rotation is indicated.
>
> ============================================================
> PHASE 0 — FINAL EXACT-VALUE CONFINEMENT CHECK
> =============================================
>
> Before creating a replacement file or performing any rewrite:
>
> Re-extract the same four sensitive values in memory using the already reviewed structural method.
>
> Do NOT print them.
>
> Search ALL blobs/paths reachable from old `main` for exact occurrences of each of the four values.
>
> Return internally a path set per value.
>
> Expected authorized union:
>
> ANDROID_GATE_READER.md
> ANDROID_GATE_READER_PHYSICAL_REPORT.md
> GATE_STAND.md
> NFC_PORT_DIAGNOSTIC_RESULT.md
> WORKLOG.md
> NFC_AB_TEST_PLAN.md
>
> Historical absence of a path in some commits is normal.
>
> If the union contains ANY additional path:
>
> STOP immediately.
>
> Report only:
>
> * unexpected path;
> * which safe category matched:
>   WINDOWS_PATH / SERIAL_1 / SERIAL_2 / DEVICE_MAC;
> * first affected commit;
> * current/history/both.
>
> Never print the matched value.
>
> If the complete union is exactly the six authorized paths:
>
> continue automatically with the approved rewrite.
>
> Safe output:
>
> exact_value_scope=PASS
> authorized_paths=6
>
> ============================================================
> REUSE EXISTING PRIVATE CHECKPOINT
> =================================
>
> Do NOT unnecessarily recreate the verified bundle.
>
> First verify that the existing bundle still:
>
> * exists;
> * verifies successfully;
> * contains exactly old refs/heads/main;
> * resolves main to the old audited SHA.
>
> Reuse it if PASS.
>
> The existing disposable rewrite clone may also be reused ONLY IF:
>
> * it remains at the old SHA;
> * its worktree/index are clean;
> * it contains only refs/heads/main;
> * no replacement file exists;
> * no rewrite helper exists;
> * no commit map exists;
> * no rewritten commit/ref was created.
>
> If any of those conditions fail:
>
> discard only the disposable clone using the previously reviewed guarded deletion procedure and recreate it from the verified bundle.
>
> Do not touch the operational source repository.
>
> ============================================================
> UPDATED REWRITE TARGETS
> =======================
>
> Update the approved rewrite helper TARGETS only to include:
>
> "NFC_AB_TEST_PLAN.md"
>
> The complete target tuple becomes:
>
> ANDROID_GATE_READER.md
> ANDROID_GATE_READER_PHYSICAL_REPORT.md
> GATE_STAND.md
> NFC_PORT_DIAGNOSTIC_RESULT.md
> WORKLOG.md
> NFC_AB_TEST_PLAN.md
>
> No other file is authorized.
>
> The four literal replacement rules remain unchanged.
>
> ============================================================
> EXPECTED HISTORY IMPACT
> =======================
>
> The earliest affected commit remains:
>
> ec82f3f578b610ff3978de10d197f1032e449809
>
> Therefore expected topology remains:
>
> * commits 1–45 unchanged;
> * commits 46–52 rewritten;
> * 7 rewritten commits total;
> * 52 commits total;
> * no squashing;
> * no merge creation;
> * metadata/messages preserved.
>
> If this expectation changes:
>
> STOP.
>
> ============================================================
> EXECUTION
> =========
>
> After exact-value confinement PASS:
>
> Resume the previously approved PRIV-01 execution plan with the Control Tower modifications already established:
>
> 1. reuse/verify private main-only recovery bundle;
> 2. verify/recreate disposable main-only clone;
> 3. generate the four-rule private replacement file;
> 4. create deterministic rewrite helper with SIX authorized paths;
> 5. rewrite only commits 46–52;
> 6. perform complete local privacy validation;
> 7. prove unrelated blobs unchanged;
> 8. run credential-pattern scan;
> 9. run Node suite;
> 10. run Android tests/build offline;
> 11. run firmware validation as previously approved;
> 12. record new candidate SHA;
> 13. verify remote main still equals old SHA;
> 14. push ONLY main using explicit force-with-lease;
> 15. fresh-clone remote validation;
> 16. synchronize operational source repo to sanitized main;
> 17. preserve ignored env/runtime files;
> 18. delete temporary replacement file only after all remote/source validation passes.
>
> ============================================================
> CONTROL-TOWER MODIFICATIONS STILL APPLY
> =======================================
>
> DO NOT before/during this release task:
>
> * delete refs/codex;
> * expire source-repository reflogs;
> * run source-repository gc/prune;
> * perform mirror/all-ref operations.
>
> Do NOT run unnecessary pre-push gc/prune in the disposable clone.
>
> Those cleanups remain deferred.
>
> ============================================================
> UPDATED VALIDATION
> ==================
>
> Validation must prove all four original values are absent from ALL blobs reachable from rewritten main.
>
> Also specifically verify:
>
> NFC_AB_TEST_PLAN.md
>
> * remains present;
> * contains the neutral <GATE_SERIAL_2> placeholder where applicable;
> * has no original serial #2 in any rewritten revision.
>
> All six authorized file paths may change only where one of the four approved exact literal replacements applies.
>
> All unrelated file blobs must remain byte-identical.
>
> ============================================================
> STOP IF
> =======
>
> STOP if:
>
> * exact-value confinement finds a seventh path;
> * repo state changed;
> * bundle verification fails;
> * disposable clone contains unexpected state;
> * rewrite affects more than seven commits;
> * an unrelated blob changes;
> * metadata/message preservation fails;
> * privacy validation fails;
> * credential scan requires human review;
> * tests/builds fail;
> * remote main moved;
> * force-with-lease fails;
> * fresh remote validation fails.
>
> Do not weaken validation to continue.
>
> ============================================================
> FINAL OUTPUT
> ============
>
> Return:
>
> # PRIV-01 EXECUTION RESULT — SIX-PATH SCOPE
>
> ## FINAL CONFINEMENT CHECK
>
> * exact values checked:
> * authorized paths:
> * unexpected paths:
> * result:
>
> ## PRIVATE CHECKPOINT
>
> * existing bundle reused:
> * verified:
> * old SHA:
>
> ## REWRITE
>
> * rewritten commits:
> * unchanged commits:
> * new SHA:
> * authorized paths changed:
>
> ## PRIVACY VALIDATION
>
> Table:
> Check | Result
>
> ## TEST / BUILD VALIDATION
>
> * Node:
> * Android tests:
> * Android build:
> * Firmware:
>
> ## REMOTE UPDATE
>
> * push:
> * remote new SHA:
> * fresh clone:
>
> ## SOURCE REPOSITORY
>
> * HEAD:
> * origin/main:
> * clean:
> * env preserved:
> * runtime preserved:
>
> ## DEFERRED CLEANUP
>
> * refs/codex: deferred
> * source reflog/gc: deferred
> * mirror/all-ref operations: forbidden
>
> ## CODE / BLOCKCHAIN CHANGES
>
> NONE
>
> ## VERDICT
>
> One exact line:
>
> PRIV-01 PASS — SANITIZED MAIN READY FOR RELEASE DOCUMENTATION
>
> or
>
> PRIV-01 FAIL — <specific blocker>
