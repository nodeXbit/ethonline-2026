# Project task packet 107: MISSION — EXECUTE PRIV-01 APPROVED HISTORY SANITATION

- Classification: **ORIGINAL / SANITIZED**
- Redactions applied: personal filesystem path
- Source: retained project-specific Control Tower packet in the local Codex record

---

> MISSION — EXECUTE PRIV-01 APPROVED HISTORY SANITATION
>
> CONTROL TOWER AUTHORIZATION:
>
> Execute the previously reviewed PRIV-01 history sanitation plan, subject to the modifications and stop conditions below.
>
> This is an explicitly authorized WRITE / HISTORY-REWRITE task.
>
> FEATURE FREEZE remains active.
>
> ============================================================
> PRECONDITION
> ============
>
> Expected source repository state:
>
> branch:
> main
>
> HEAD:
> d60572284854c005ab2d7c9f90f62da82ffc56b3
>
> origin/main:
> d60572284854c005ab2d7c9f90f62da82ffc56b3
>
> tracked worktree:
> clean
>
> The final demo runtime has just been independently restored and validated without code changes:
>
> * Node bridge PASS
> * 3/3 ADB reverses PASS
> * holder HCE PASS
> * RPC PASS
> * ENSv2 reads PASS
> * STAFF → LAB = ALLOW
> * VISITOR → LAB = DENY / RESOURCE_NOT_ALLOWED
> * showcase-all → all three gates = ALLOW
> * blockchain writes = ZERO
> * code change required = NO
>
> Do NOT alter that application state intentionally during this task.
>
> ============================================================
> APPROVED SANITATION SCOPE
> =========================
>
> Rewrite only the privacy-sensitive historical content already approved in:
>
> 1. ANDROID_GATE_READER.md
> 2. ANDROID_GATE_READER_PHYSICAL_REPORT.md
> 3. GATE_STAND.md
> 4. NFC_PORT_DIAGNOSTIC_RESULT.md
> 5. WORKLOG.md
>
> Approved transformations:
>
> * username-bearing Windows ADB path
>   → portable `adb`
>
> * real ADB/device serial #1
>   → `<GATE_SERIAL_1>`
>
> * real ADB/device serial #2
>   → `<GATE_SERIAL_2>`
>
> * real ESP32 ROM MAC
>   → `<DEVICE_MAC>`
>
> Do not expose original values in terminal summaries or final report.
>
> Do not sanitize optional COM labels, device models, Android versions, timestamps or timezone in this task.
>
> ============================================================
> APPROVED STRATEGY
> =================
>
> Use the previously designed:
>
> PRIVATE MAIN-ONLY BUNDLE
> → DISPOSABLE MAIN-ONLY CLONE
> → LOCAL EXACT-VALUE EXTRACTION
> → DETERMINISTIC PYTHON/GIT-PLUMBING REWRITE
> → LOCAL VALIDATION
> → TESTS/BUILDS
> → EXPLICIT FORCE-WITH-LEASE OF MAIN ONLY
> → REMOTE VERIFICATION
>
> The operational source repository must remain untouched until the sanitized remote `main` has been successfully validated.
>
> ============================================================
> CONTROL-TOWER MODIFICATIONS TO THE PLAN
> =======================================
>
> MODIFICATION 1:
>
> DO NOT run `git gc --prune=now`, reflog expiry, or old-object cleanup in the disposable rewrite clone BEFORE the remote push.
>
> Those steps are not required to safely push the sanitized `main` ref and add unnecessary destructive work before publication.
>
> The old history is allowed to remain locally in the disposable private clone during validation.
>
> MODIFICATION 2:
>
> DO NOT delete `refs/codex/...`, expire source-repository reflogs, prune source objects, or run source-repository GC during this task.
>
> That cleanup is explicitly deferred.
>
> We will address local Git housekeeping separately after the release/submission flow or before any operation that would share `.git`, mirror refs, or create an all-ref bundle.
>
> MODIFICATION 3:
>
> After the successful remote sanitized-main update, synchronize the original operational repository to the new `main`, BUT preserve all ignored operational files and do not clean Codex refs.
>
> ============================================================
> PHASE 1 — RECONFIRM STATE
> =========================
>
> Before creating anything:
>
> * verify clean tracked worktree;
> * verify HEAD equals the old audited SHA;
> * verify local origin/main equals the old audited SHA;
> * fetch origin;
> * verify remote main still equals old audited SHA;
> * verify 52 commits and linear history.
>
> STOP on any discrepancy.
>
> ============================================================
> PHASE 2 — PRIVATE RECOVERY CHECKPOINT
> =====================================
>
> Create the approved private evidence under:
>
> [REDACTED: personal filesystem path]
>
> Create and VERIFY:
>
> * main-only Git bundle;
> * safe rewrite manifest;
> * pre-rewrite commit metadata;
> * pre-rewrite tree;
> * later old→new commit map.
>
> The bundle must contain exactly old `refs/heads/main`.
>
> It must NOT contain:
>
> * `.env.local`
> * `.env.nfc.local`
> * `.runtime`
> * node_modules
> * build outputs
> * refs/codex
>
> Do not copy the complete working directory.
>
> ============================================================
> PHASE 3 — DISPOSABLE REWRITE
> ============================
>
> Create the approved disposable main-only clone from the private bundle.
>
> Remove its bundle-backed `origin`.
>
> Verify it contains only:
>
> refs/heads/main
>
> and no:
>
> refs/codex
> refs/original
> other branches/tags
>
> Generate the private exact-value replacement file outside both repositories using the previously approved structural extraction.
>
> Do not print extracted sensitive values.
>
> Safe output may report only:
>
> replacement_rules=4
>
> Create the previously reviewed deterministic rewrite helper.
>
> Execute it ONLY in the disposable clone.
>
> Expected:
>
> * commits 1–45 unchanged;
> * commits 46–52 rewritten;
> * total commits still 52;
> * no merge commits;
> * only authorized file blobs altered;
> * authorship, committer metadata, timestamps and messages preserved;
> * new final SHA produced.
>
> ============================================================
> PHASE 4 — LOCAL SANITATION VALIDATION
> =====================================
>
> Perform ALL previously designed validations, including:
>
> 1. original four sensitive values absent from every blob reachable from rewritten main;
>
> 2. generic username-bearing Windows paths absent from authorized affected history;
>
> 3. real MAC pattern removed from authorized affected history;
>
> 4. placeholders present where expected;
>
> 5. five authorized files remain;
>
> 6. first 45 commit SHAs unchanged;
>
> 7. final seven commit SHAs changed;
>
> 8. commit messages unchanged;
>
> 9. author/committer metadata and timestamps unchanged;
>
> 10. filenames unchanged;
>
> 11. all unrelated blobs byte-identical;
>
> 12. total commit count remains 52;
>
> 13. linear topology remains;
>
> 14. credential-pattern scan PASS;
>
> 15. rewritten worktree clean;
>
> 16. source `.env.local` and `.env.nfc.local` still:
>
>     * exist;
>     * are ignored;
>     * are untracked;
>     * are NOT read/printed.
>
> Do not push unless every validation passes.
>
> ============================================================
> PHASE 5 — TEST / BUILD VALIDATION
> =================================
>
> Run the approved validation against the sanitized candidate.
>
> NODE:
>
> Run the complete existing Node suite.
>
> Expected prior baseline:
> 238/238
>
> If using the approved temporary `node_modules` junction:
>
> * verify it really is a junction before deletion;
> * remove only the junction;
> * never delete the source dependency directory.
>
> ANDROID:
>
> Run offline:
>
> testDebugUnitTest
> assembleDebug
>
> Do not download/install dependencies.
>
> If offline resolution fails due to missing cached dependency:
>
> STOP rather than changing dependency state.
>
> FIRMWARE:
>
> Because source firmware blobs are required to remain unchanged, either:
>
> A. rerun the three existing compile targets using already installed tooling/libraries;
>
> or, only if the existing Arduino CLI/toolchain is unexpectedly unavailable:
>
> B. clearly report that source blobs are byte-identical to the previously validated release and retain prior compile evidence.
>
> Do not install anything.
> Do not flash hardware.
>
> ============================================================
> PHASE 6 — RECORD NEW CANDIDATE
> ==============================
>
> If and only if ALL sanitation and validation checks pass:
>
> * record new final SHA;
> * complete private manifest;
> * verify commit-map;
> * preserve old bundle privately.
>
> Do NOT delete the replacement-expression file yet.
>
> ============================================================
> PHASE 7 — REMOTE LEASE CHECK
> ============================
>
> Repository must remain PRIVATE.
>
> Add an explicit GitHub publication remote to the disposable clone as previously planned.
>
> Immediately query:
>
> refs/heads/main
>
> using `git ls-remote`.
>
> Remote `main` MUST still equal:
>
> d60572284854c005ab2d7c9f90f62da82ffc56b3
>
> If not:
>
> STOP.
>
> Do NOT retry with plain `--force`.
>
> ============================================================
> PHASE 8 — PUSH SANITIZED MAIN ONLY
> ==================================
>
> Push ONLY:
>
> refs/heads/main:refs/heads/main
>
> using an explicit lease against the old SHA:
>
> --force-with-lease="refs/heads/main:d60572284854c005ab2d7c9f90f62da82ffc56b3"
>
> NEVER use:
>
> --force
> --mirror
> --all
> --tags
> wildcard refspecs
>
> After push:
>
> * query GitHub remote main;
> * verify it equals the new candidate SHA.
>
> ============================================================
> PHASE 9 — FRESH REMOTE VALIDATION
> =================================
>
> Create a NEW temporary single-branch clone from GitHub `main`.
>
> Do not reuse the rewrite clone as proof of remote sanitation.
>
> Against that fresh clone verify:
>
> * expected new SHA;
> * 52 commits;
> * privacy-sensitive values/patterns absent;
> * credential-pattern scan PASS;
> * five required files present;
> * no unexpected branches/tags/refs in the fetched normal branch view;
> * worktree clean.
>
> Do not make repository public.
>
> ============================================================
> PHASE 10 — SYNCHRONIZE OPERATIONAL SOURCE REPOSITORY
> ====================================================
>
> Only after fresh remote validation PASS:
>
> Return to:
>
> [REDACTED: personal filesystem path]
>
> Require clean tracked worktree.
>
> Fetch sanitized main.
>
> Verify fetched origin/main equals new SHA.
>
> Move local main to the sanitized SHA using the safest deterministic operation already designed.
>
> After synchronization verify:
>
> * HEAD == origin/main == new SHA;
> * tracked worktree clean;
> * `.env.local` still exists and remains ignored/untracked;
> * `.env.nfc.local` still exists and remains ignored/untracked;
> * `.runtime` remains intact;
> * no operational secret is printed.
>
> DO NOT:
>
> * delete refs/codex;
> * expire reflogs;
> * run gc;
> * prune objects.
>
> ============================================================
> PHASE 11 — DELETE TEMPORARY REPLACEMENT FILE
> ============================================
>
> Only after:
>
> * remote main PASS;
> * fresh clone PASS;
> * source repository synchronized PASS;
>
> perform the previously designed best-effort overwrite/delete of:
>
> replace-text.txt
>
> Do not delete:
>
> * private recovery bundle;
> * commit map;
> * safe manifest;
> * safe metadata evidence.
>
> ============================================================
> NO-TOUCH
> ========
>
> DO NOT modify:
>
> * application code;
> * Node behavior;
> * Android behavior;
> * firmware source;
> * blockchain configuration;
> * wallets;
> * credentials;
> * ENSv2 state;
> * Privy configuration;
> * artwork;
> * LICENSE;
> * README beyond the exact historic privacy replacement that may already affect authorized docs;
> * AI documentation;
> * submission material.
>
> DO NOT:
>
> * make GitHub public;
> * create blockchain transactions;
> * install software;
> * rotate secrets;
> * delete Codex refs;
> * prune the source repository.
>
> ============================================================
> STOP IF
> =======
>
> STOP immediately if:
>
> * repo state differs from the audited old SHA before execution;
> * bundle verification fails;
> * extraction does not produce exactly four mappings;
> * an original value appears outside the authorized five-path sanitation scope in reachable main;
> * more than seven commits need rewriting unexpectedly;
> * an unrelated blob changes;
> * authorship/message/timestamp preservation fails;
> * any credential-pattern candidate needs human review;
> * Node tests fail;
> * Android tests/build fail;
> * firmware source differs unexpectedly;
> * remote main moved;
> * force-with-lease is rejected;
> * fresh remote validation fails;
> * synchronization would risk ignored operational files.
>
> Never bypass a STOP condition by weakening validation.
>
> ============================================================
> FINAL OUTPUT
> ============
>
> Return:
>
> # PRIV-01 EXECUTION RESULT
>
> ## PRECONDITION
>
> PASS / FAIL
>
> ## PRIVATE RECOVERY CHECKPOINT
>
> * bundle:
> * verified:
> * old SHA:
>
> ## REWRITE
>
> * rewritten commits:
> * unchanged commits:
> * new SHA:
> * authorized files changed:
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
> * lease old SHA:
> * push:
> * remote new SHA:
> * fresh clone validation:
>
> ## SOURCE REPOSITORY RESYNC
>
> * HEAD:
> * origin/main:
> * clean:
> * ignored env preserved:
> * runtime preserved:
> * Codex refs preserved:
>
> ## PRIVATE EVIDENCE
>
> List safe artifacts retained.
>
> ## DEFERRED CLEANUP
>
> Explicitly state:
>
> * refs/codex cleanup deferred;
> * reflog/source GC deferred;
> * no mirror/all-ref operation permitted.
>
> ## CODE / BLOCKCHAIN CHANGES
>
> Expected:
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
