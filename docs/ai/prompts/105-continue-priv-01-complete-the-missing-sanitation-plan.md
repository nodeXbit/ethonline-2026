# Project task packet 105: CONTINUE PRIV-01 — COMPLETE THE MISSING SANITATION PLAN

- Classification: **ORIGINAL / SANITIZED**
- Redactions applied: none
- Source: retained project-specific Control Tower packet in the local Codex record

---

> CONTINUE PRIV-01 — COMPLETE THE MISSING SANITATION PLAN
>
> Your previous response reconfirmed AUD-01 findings but did NOT complete the requested PRIV-01 planning task.
>
> DO NOT repeat the privacy audit.
>
> The following findings are now accepted and frozen:
>
> * HEAD == origin/main == d60572284854c005ab2d7c9f90f62da82ffc56b3
> * clean worktree
> * no exposed credentials in Git
> * rotation not indicated
> * mandatory main sanitation scope is exactly the five already identified paths
> * local refs/codex cleanup may be deferred until after sanitized main is pushed
>
> This remains READ-ONLY / PLAN-ONLY.
>
> DO NOT edit.
> DO NOT rewrite history.
> DO NOT create backups yet.
> DO NOT commit.
> DO NOT push.
> DO NOT install anything.
> DO NOT delete refs.
> DO NOT touch credentials or blockchain state.
>
> ============================================================
> COMPLETE ONLY THE MISSING PRIV-01 PLAN
> ======================================
>
> 1. Determine whether `git filter-repo` is ALREADY installed and available.
>
> Report:
>
> * Git version
> * `git filter-repo` availability
> * any other already-installed suitable rewrite tooling
>
> DO NOT install anything.
>
> If `git filter-repo` is unavailable, design the safest alternative but do not execute it.
>
> 2. Design the exact smallest rewrite of `main`.
>
> Requirements:
>
> * exact-value replacement only;
> * keep all five files;
> * do not squash commits;
> * preserve commit messages;
> * preserve authorship/timestamps as far as technically possible;
> * preserve chronological development evidence;
> * do not touch refs/codex during this main rewrite;
> * do not touch ignored `.env` files;
> * do not rewrite unrelated safe data.
>
> 3. Design the private pre-rewrite checkpoint.
>
> Use the directory OUTSIDE the repository:
>
> ..\LockENS-private-evidence\git-audit\
>
> Evaluate and specify an exact `main`-only Git bundle or equivalent recovery artifact that:
>
> * preserves old main at d60572284854c005ab2d7c9f90f62da82ffc56b3;
> * excludes ignored working-tree `.env` files;
> * excludes refs/codex;
> * stays private/local.
>
> Also specify a small text manifest containing:
>
> * old final SHA;
> * affected historical commits;
> * replacement categories;
> * rewrite date/time;
> * new final SHA after execution;
> * old→new commit mapping if produced.
>
> DO NOT create anything yet.
>
> 4. Design the sensitive replacement mechanism.
>
> The future execution must be able to replace:
>
> * username-bearing Windows path;
> * ADB serial #1;
> * ADB serial #2;
> * ESP32 ROM MAC;
>
> WITHOUT printing those real values in the report or commands returned to ChatGPT.
>
> Explain:
>
> * where a temporary replacement-expression/file would live OUTSIDE the repository;
> * how Codex would populate it locally without echoing values;
> * how the rewrite tool would consume it;
> * how it would be deleted after successful validation.
>
> DO NOT create it now.
>
> 5. Provide the EXACT FUTURE EXECUTION COMMANDS.
>
> For every command show:
>
> COMMAND
> CLASS: READ / WRITE / DESTRUCTIVE
> PURPOSE
> EXPECTED EFFECT
>
> Do not substitute conceptual prose for commands.
>
> Sensitive real values must NEVER appear in those command examples.
>
> 6. Explain SHA impact.
>
> Identify:
>
> * earliest affected commit;
> * whether every descendant commit through HEAD receives a new SHA;
> * whether unaffected commits before the earliest modified commit retain their SHA;
> * whether commit messages/authorship/timestamps change;
> * expected new final candidate behavior.
>
> 7. Explain tool side effects.
>
> Especially for `git filter-repo`, if selected:
>
> * effect on `origin`;
> * effect on working tree;
> * effect on ignored files;
> * commit-map/ref-map artifacts;
> * reflogs;
> * original refs;
> * refs/codex;
> * need for force push.
>
> Do not assume defaults: verify against the installed tool/help if available.
>
> 8. Design exact POST-REWRITE validation.
>
> Before any push, commands must verify:
>
> * sensitive Windows path absent from current main;
> * both ADB serials absent from current main;
> * ESP32 ROM MAC absent from current main;
> * all four sensitive values absent from every rewritten main commit/blob;
> * no secret accidentally introduced;
> * five affected files still exist where expected;
> * commit progression remains understandable;
> * worktree clean;
> * `.env.local` and `.env.nfc.local` still exist locally, remain ignored and untracked WITHOUT printing contents;
> * HEAD differs from old final SHA as expected;
> * Node tests PASS;
> * Android tests/build PASS;
> * appropriate firmware compile validation PASS.
>
> 9. Design the remote update.
>
> Provide the exact intended command.
>
> Requirements:
>
> * ONLY `main`;
> * NEVER `--mirror`;
> * NEVER `--all`;
> * prefer `--force-with-lease` if technically correct;
> * explain how the lease is made safe after a history rewrite;
> * verify remote state immediately before push;
> * verify GitHub points to the expected new SHA afterward.
>
> Repository stays PRIVATE during this operation.
>
> 10. Design the later local cleanup.
>
> Separately describe how, AFTER successful sanitized-main remote update, we eventually clean:
>
> * refs/codex;
> * reflogs;
> * unreachable objects;
>
> before ever creating all-ref bundles, mirrors, or sharing `.git`.
>
> DO NOT execute cleanup now.
>
> 11. Recovery.
>
> Provide explicit procedures for:
>
> A. rewrite fails locally before push;
> B. validation fails locally before push;
> C. force push is rejected;
> D. remote push succeeds but a subsequent problem is discovered.
>
> The private main-only checkpoint must make recovery possible.
>
> ============================================================
> OUTPUT EXACTLY
> ==============
>
> # PRIV-01 HISTORY SANITATION PLAN
>
> ## AVAILABLE TOOLING
>
> Git version:
> Preferred tool:
> Available:
> Why:
>
> ## PRE-REWRITE PRIVATE CHECKPOINT
>
> ## REPLACEMENT METHOD
>
> ## EXACT EXECUTION SEQUENCE
>
> For each step:
> Command:
> Class:
> Purpose:
> Expected effect:
>
> ## EXPECTED SHA IMPACT
>
> ## TOOL SIDE EFFECTS
>
> ## POST-REWRITE LOCAL VALIDATION
>
> ## TEST / BUILD VALIDATION
>
> ## REMOTE UPDATE PLAN
>
> ## LOCAL CODEX-REF CLEANUP PLAN
>
> ## RECOVERY PLAN
>
> ## RISKS
>
> ## EXECUTION READINESS
>
> End with exactly one:
>
> READY FOR CONTROL-TOWER REVIEW
>
> or
>
> NOT READY — <specific reason>
>
> ============================================================
> STOP IF
> =======
>
> Stop if:
>
> * repo state no longer matches the clean audited SHA;
> * a suitable plan requires installing software;
> * a safe main-only rewrite cannot be designed;
> * new sensitive history is discovered.
>
> READ ONLY.
> PLAN ONLY.
> NO EDITS.
> NO REWRITE.
> NO BACKUP CREATION.
> NO PUSH.
> NO INSTALL.
> NO BLOCKCHAIN WRITES.
