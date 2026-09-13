# Project task packet 104: MISSION — PRIV-01 HISTORY SANITATION PLAN

- Classification: **ORIGINAL / SANITIZED**
- Redactions applied: none
- Source: retained project-specific Control Tower packet in the local Codex record

---

> MISSION — PRIV-01 HISTORY SANITATION PLAN
>
> AUD-01 is complete.
>
> VERDICT:
>
> AUD-01 PASS — READY TO PLAN REMEDIATION
>
> This task is PLAN / READ-ONLY ONLY.
>
> DO NOT perform the history rewrite.
> DO NOT edit files.
> DO NOT commit.
> DO NOT push.
> DO NOT create branches or tags.
> DO NOT delete refs or objects.
> DO NOT rotate credentials.
> DO NOT change blockchain state.
> DO NOT make the repository public.
>
> ============================================================
> CURRENT VERIFIED STATE
> ======================
>
> Branch:
> main
>
> HEAD:
> d60572284854c005ab2d7c9f90f62da82ffc56b3
>
> origin/main:
> d60572284854c005ab2d7c9f90f62da82ffc56b3
>
> Worktree:
> clean
>
> AUD-01 concluded:
>
> ROTATION NOT INDICATED BY AUDIT EVIDENCE
>
> No secret/credential was found exposed in tracked files, reachable history, refs/codex, or dangling blobs.
>
> Real operational secrets remain correctly contained in ignored local environment files and MUST NOT be changed.
>
> ============================================================
> MANDATORY PUBLIC-MAIN SANITATION SCOPE
> ======================================
>
> Exactly these five paths are currently known to require history sanitation:
>
> 1. ANDROID_GATE_READER.md
>
>    * username-bearing Windows path
>    * real ADB/device serial
>    * current + history
>
> 2. ANDROID_GATE_READER_PHYSICAL_REPORT.md
>
>    * two real ADB/device serials
>    * current + history
>
> 3. GATE_STAND.md
>
>    * username-bearing Windows path
>    * history only
>
> 4. NFC_PORT_DIAGNOSTIC_RESULT.md
>
>    * real ESP32 ROM MAC
>    * current + history
>
> 5. WORKLOG.md
>
>    * same real ESP32 ROM MAC
>    * current + history
>
> First known affected commits:
>
> Windows/ADB group:
> 734619cd2ee7c4886f0bcdf5b2710827a518c157
>
> Historical GATE_STAND path:
> 37331ab9eaee070553a8ea0cce01936395a9ce46
>
> ESP32 MAC group:
> ec82f3f578b610ff3978de10d197f1032e449809
>
> Required replacement semantics:
>
> * absolute username-bearing Windows paths
>   → repository-relative or neutral portable paths
>
> * real ADB/device serial #1
>   → <GATE_SERIAL_1>
>
> * real ADB/device serial #2
>   → <GATE_SERIAL_2>
>
> * real ESP32 ROM MAC
>   → <DEVICE_MAC>
>
> Do not expose the original values in your report.
>
> ============================================================
> LOCAL-ONLY STATE
> ================
>
> There are 30 refs/codex/... tree refs plus unreachable/local objects containing generated build files and username-bearing paths.
>
> AUD-01 established:
>
> * normal `git push origin main` does NOT push them;
> * changing GitHub visibility cannot upload objects/refs never pushed;
> * they must eventually be cleaned before any mirror push, `--all` bundle, or sharing `.git`;
> * their cleanup can occur after sanitized main is pushed.
>
> Do NOT include those refs in the public-history rewrite unless there is a concrete reason.
>
> ============================================================
> TASK 1 — DETERMINE AVAILABLE REWRITE TOOLING
> ============================================
>
> READ ONLY.
>
> Determine:
>
> * Git version;
> * whether `git filter-repo` is already available;
> * whether any other suitable history-rewrite tool is already available.
>
> DO NOT install anything.
>
> Prefer the safest modern approach.
>
> Do NOT choose deprecated `git filter-branch` merely because it ships with Git.
>
> If `git filter-repo` is unavailable, explain the safest fallback.
>
> Do not execute any rewrite command.
>
> ============================================================
> TASK 2 — DESIGN THE SMALLEST SAFE REWRITE
> =========================================
>
> Design a procedure that:
>
> * rewrites only the sensitive content necessary;
> * preserves filenames;
> * preserves commit messages;
> * preserves commit authorship/timestamps as far as the chosen tool permits;
> * preserves meaningful development progression;
> * does not squash history;
> * does not remove legitimate blockchain evidence;
> * does not remove safe device models or Android versions;
> * does not touch operational `.env` files;
> * does not rewrite more refs than necessary;
> * results in a sanitized `main`.
>
> Prefer exact-value replacement over deleting entire files/history where safe.
>
> The original sensitive values must NOT be copied into ChatGPT-facing output.
>
> If an exact-value replacement file is required:
>
> * design how Codex can create/use it locally during the later authorized execution;
> * it must never enter the repository;
> * it must never be committed;
> * it must not be printed in reports;
> * define where it temporarily lives;
> * define how it is securely removed after successful validation.
>
> DO NOT create it in this planning task.
>
> ============================================================
> TASK 3 — PRIVATE PRE-REWRITE EVIDENCE
> =====================================
>
> Design the minimum useful recovery/evidence checkpoint BEFORE rewriting.
>
> We have a separate private directory outside the repository:
>
> ..\LockENS-private-evidence\
>
> The plan may use:
>
> ..\LockENS-private-evidence\git-audit\
>
> Do NOT copy the whole working directory because it contains ignored operational secrets.
>
> Evaluate whether we should create a `main`-only Git bundle before rewrite.
>
> The backup/evidence must:
>
> * preserve the audited release candidate privately;
> * contain no ignored `.env` working-tree files;
> * avoid including refs/codex unless genuinely needed;
> * not be pushed anywhere;
> * allow recovery of the old main history if the rewrite fails.
>
> Also preserve, in a safe text report:
>
> * old final SHA;
> * affected old commits;
> * intended replacement categories;
> * timestamp of rewrite;
> * new final SHA after execution;
> * old→new commit mapping if generated by the tool.
>
> Do NOT create these artifacts yet.
>
> ============================================================
> TASK 4 — ANTICIPATE TOOL SIDE EFFECTS
> =====================================
>
> For the preferred rewrite tool explicitly address:
>
> * whether it removes or modifies `origin`;
> * whether it creates commit maps;
> * whether it rewrites only the specified refs or more;
> * whether reflogs/original refs remain;
> * whether working-tree files change;
> * whether ignored `.env` files are touched;
> * whether force-push will be necessary;
> * exact force-push form recommended;
> * how to avoid accidentally pushing refs/codex;
> * how to avoid a mirror/all-refs push.
>
> We will NOT use:
>
> `git push --mirror`
>
> or an indiscriminate all-refs push.
>
> ============================================================
> TASK 5 — VALIDATION DESIGN
> ==========================
>
> Design the exact post-rewrite validation BEFORE any push.
>
> It must prove:
>
> 1. current tree no longer contains:
>
>    * username-bearing Windows path;
>    * real ADB serials;
>    * real ESP32 ROM MAC.
>
> 2. rewritten reachable `main` history no longer contains them.
>
> 3. no secret/credential was accidentally introduced.
>
> 4. commit progression remains understandable.
>
> 5. expected project files remain present.
>
> 6. worktree is clean.
>
> 7. operational ignored environment files still exist locally and remain ignored, WITHOUT printing their contents.
>
> 8. Node tests still pass.
>
> 9. Android tests/build still pass where appropriate.
>
> 10. firmware compile validation is preserved or rerun where proportionate.
>
> 11. final documentation remains internally coherent.
>
> 12. a NEW final candidate SHA is recorded.
>
> 13. no public push happens until all validation passes.
>
> ============================================================
> TASK 6 — REMOTE UPDATE DESIGN
> =============================
>
> Design the remote step but DO NOT execute it.
>
> Because public `origin/main` currently contains unsanitized history, after local validation we expect a coordinated history replacement.
>
> Specify:
>
> * final synchronization check immediately before push;
> * exact branch/ref to push;
> * safe force-push mechanism;
> * why `--force-with-lease` is preferred if applicable;
> * how to verify GitHub points to the expected new SHA;
> * how to verify old sensitive values are no longer available through the public branch history after publication;
> * what limitations remain because hosting providers may retain internal caches/objects temporarily.
>
> Repository visibility must remain PRIVATE until sanitation, documentation fixes and final validation are finished.
>
> ============================================================
> TASK 7 — LOCAL POST-PUSH CLEANUP
> ================================
>
> Design a separate later cleanup for:
>
> * refs/codex;
> * reflogs;
> * unreachable objects;
> * generated build-history blobs.
>
> This cleanup must happen before:
>
> * Git bundles that include all refs;
> * mirror operations;
> * sharing `.git`.
>
> Determine whether it should happen immediately after successful rewritten-main push or later in the release-remediation sequence.
>
> DO NOT perform it now.
>
> ============================================================
> NO-TOUCH
> ========
>
> Do not change:
>
> * application code;
> * blockchain configuration;
> * wallets;
> * credentials;
> * credential state;
> * ENSv2 state;
> * Privy configuration;
> * Android behavior;
> * Node behavior;
> * firmware;
> * artwork;
> * LICENSE;
> * README;
> * AI disclosure;
> * GitHub visibility.
>
> Those are outside this planning task.
>
> ============================================================
> OUTPUT
> ======
>
> Return exactly:
>
> # PRIV-01 HISTORY SANITATION PLAN
>
> ## CURRENT STATE CONFIRMATION
>
> ## AVAILABLE TOOLING
>
> Include:
> Preferred tool:
> Available:
> Why:
>
> ## RECOMMENDED STRATEGY
>
> Explain the rewrite at a conceptual level.
>
> ## PRE-REWRITE PRIVATE CHECKPOINT
>
> Exact artifacts and locations, but DO NOT create them.
>
> ## EXACT EXECUTION SEQUENCE
>
> Numbered operational sequence.
>
> For every command that will eventually be run:
>
> * show the command;
> * explain what it does;
> * mark it READ / WRITE / DESTRUCTIVE;
> * DO NOT execute it.
>
> Do not include real sensitive values in command examples.
>
> ## REPLACEMENT METHOD
>
> Explain how exact local values will be transformed without exposing them.
>
> ## EXPECTED SHA IMPACT
>
> Which part of history is expected to receive new SHAs.
>
> ## TOOL SIDE EFFECTS
>
> ## POST-REWRITE LOCAL VALIDATION
>
> Exact checks and commands.
>
> ## TEST / BUILD VALIDATION
>
> ## REMOTE UPDATE PLAN
>
> Exact intended push command, but DO NOT execute it.
>
> ## LOCAL CODEX-REF CLEANUP PLAN
>
> Separate from main rewrite.
>
> ## RECOVERY PLAN
>
> What to do if validation fails before push.
> What to do if remote update fails.
>
> ## RISKS
>
> P0 / P1 / P2 as applicable.
>
> ## EXECUTION READINESS
>
> One exact line:
>
> READY FOR CONTROL-TOWER REVIEW
>
> or
>
> NOT READY — <reason>
>
> ============================================================
> STOP IF
> =======
>
> Stop and report if:
>
> * repository state no longer matches the audited clean SHA;
> * deciding the strategy requires installing a tool;
> * the rewrite cannot be safely scoped to main;
> * you discover new sensitive history not covered by AUD-01;
> * preserving development history would require a substantially different approach.
>
> READ ONLY.
> PLAN ONLY.
> NO EDITS.
> NO HISTORY REWRITE.
> NO PUSH.
> NO INSTALLS.
> NO BLOCKCHAIN WRITES.
