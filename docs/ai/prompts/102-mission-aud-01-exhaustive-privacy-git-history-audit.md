# Project task packet 102: MISSION — AUD-01 EXHAUSTIVE PRIVACY / GIT HISTORY AUDIT

- Classification: **ORIGINAL / SANITIZED**
- Redactions applied: none
- Source: retained project-specific Control Tower packet in the local Codex record

---

> MISSION — AUD-01 EXHAUSTIVE PRIVACY / GIT HISTORY AUDIT
>
> This is a READ-ONLY inspection task.
>
> DO NOT modify any file.
> DO NOT modify Git history.
> DO NOT commit.
> DO NOT push.
> DO NOT change branches unless a read-only inspection genuinely requires it.
> DO NOT install anything.
> DO NOT create cleanup scripts yet.
> DO NOT expose secrets or sensitive values in the report.
> DO NOT perform blockchain writes.
> DO NOT change the current demo state.
>
> ============================================================
> TASK
> ====
>
> Perform an exhaustive privacy, secret, environment-data and publication-safety audit of the LOCAL LockENS repository and all Git history that could become public.
>
> The goal is to determine the COMPLETE sanitation scope before we authorize any history rewrite.
>
> Exact audited release candidate before remediation:
>
> d60572284854c005ab2d7c9f90f62da82ffc56b3
>
> A previous independent release audit found:
>
> 1. A real Windows absolute path containing the local username in:
>
>    * ANDROID_GATE_READER.md
>
> 2. Real ADB/device identifiers in:
>
>    * ANDROID_GATE_READER.md
>    * ANDROID_GATE_READER_PHYSICAL_REPORT.md
>
> 3. Those values also exist historically, including around commit:
>
>    * 734619c...
>
> 4. No confirmed seed/private key/API key/Privy secret/session token/OTP was found in the directed review, BUT the full historical object space was NOT exhaustively audited.
>
> We therefore need to close AUD-01 before publishing the repository.
>
> ============================================================
> WHY
> ===
>
> The repository is currently private but must become public for the ETHOnline 2026 / ENSv2 submission.
>
> A normal new commit or .gitignore does NOT remove values already present in Git history.
>
> We need one complete inventory before deciding the smallest safe rewrite strategy.
>
> Do not treat public blockchain addresses, transaction hashes, contract addresses, test vectors or protocol constants as secrets merely because they look hexadecimal.
>
> ============================================================
> SOURCE PRIORITY
> ===============
>
> 1. LOCAL working tree and LOCAL Git object/history state.
> 2. origin/main and other refs available locally.
> 3. Repository documentation.
> 4. Findings supplied above.
>
> Do not assume chat summaries override repository evidence.
>
> ============================================================
> SCOPE — REPOSITORY STATE
> ========================
>
> First establish and report, without changing anything:
>
> * current branch;
> * git status -sb;
> * HEAD SHA;
> * origin/main SHA;
> * whether HEAD == origin/main;
> * tracked remotes, but REDACT credentials/tokens if a remote URL contains any;
> * local branches;
> * remote-tracking branches;
> * tags;
> * stash existence/count;
> * whether there are untracked files;
> * whether worktree is clean;
> * whether there are additional worktrees;
> * whether refs exist that would accidentally publish sensitive history.
>
> If the worktree is not clean or HEAD != origin/main:
>
> STOP before any later remediation recommendation and clearly report the discrepancy.
>
> The inspection itself may continue read-only if safe.
>
> ============================================================
> SCOPE — CURRENT TREE
> ====================
>
> Inspect ALL tracked files in the current tree for potentially sensitive data.
>
> Search/classify at least:
>
> SECRET / CREDENTIAL
>
> * private keys
> * seed phrases / mnemonics
> * API keys
> * access tokens
> * refresh/session tokens
> * bearer tokens
> * OAuth secrets
> * Privy secrets
> * RPC provider secrets embedded in URLs
> * passwords
> * OTPs
> * auth cookies
> * keystore passwords
> * signing secrets
>
> PERSONAL / ENVIRONMENT
>
> * Windows usernames in absolute paths
> * home-directory paths
> * email addresses
> * personal names where unnecessary
> * phone numbers
> * account identifiers
> * local machine names
> * SSIDs
> * private LAN addresses when they represent real operational configuration
> * public IPs if tied to the developer environment
> * MAC addresses
> * Bluetooth identifiers
> * ADB serials/device IDs
> * hardware serial numbers
> * Android unique identifiers
> * USB IDs where device-specific
> * filesystem paths
> * local service credentials
>
> RUNTIME / BUILD ARTIFACTS
>
> * .env / .env.*
> * runtime state
> * generated credential files
> * logs
> * crash dumps
> * APK/AAB
> * build output
> * local databases
> * caches
> * credential exports
> * screenshots containing sensitive data
> * video or media accidentally tracked
>
> AI / DOCUMENTATION
>
> * copied conversations containing unrelated personal context
> * credentials in prompts
> * local paths/serials/IPs inside docs
> * account/login data
> * private context inappropriate for the public repository
>
> ============================================================
> SCOPE — COMPLETE GIT HISTORY
> ============================
>
> Audit ALL reachable commits and relevant Git objects/refs that could be exposed when this repository becomes public.
>
> Do not limit the inspection to main's current files.
>
> Inspect:
>
> * all commits reachable from all local branches;
> * all remote-tracking branches;
> * all tags;
> * commit messages;
> * historical versions of tracked files;
> * deleted historical files;
> * filenames and paths;
> * blobs;
> * refs;
> * stashes, if present;
> * reflog exposure implications where relevant locally;
> * other worktrees, if present.
>
> Also determine whether there are unreachable/dangling objects containing potentially sensitive material that matter for LOCAL cleanup, while distinguishing them from objects that GitHub would actually expose after the chosen publication/push strategy.
>
> Do not print the full contents of suspicious blobs.
>
> ============================================================
> SEARCH QUALITY
> ==============
>
> Use multiple complementary approaches rather than a single grep.
>
> At minimum combine:
>
> 1. filename/path inspection;
> 2. current-tree content search;
> 3. Git history/path search;
> 4. historical blob/content search;
> 5. commit-message search;
> 6. entropy/pattern-based reasoning where practical using existing tools only;
> 7. targeted searches based on known project technologies:
>
>    * Android / ADB
>    * Node
>    * Privy
>    * Ethereum
>    * Sepolia
>    * RPC
>    * Windows paths
>    * environment variables
>
> Do NOT install a secret-scanning dependency during this task.
>
> Use built-in Git, PowerShell, Node, Python already present, or other tools already available in the repository/environment if needed.
>
> ============================================================
> FALSE-POSITIVE RULES
> ====================
>
> Do NOT automatically classify these as secrets:
>
> * Ethereum wallet addresses;
> * deployed contract addresses;
> * transaction hashes;
> * block hashes;
> * Sepolia explorer evidence;
> * ABI;
> * AID/APDU constants;
> * deterministic test challenges;
> * protocol constants;
> * synthetic fixtures;
> * empty environment examples;
> * variable names such as DEV_PRIVATE_KEY without a value;
> * loopback localhost configuration;
> * example private IPs explicitly used as rejection/test fixtures;
> * dependency hashes;
> * Gradle wrapper JAR;
> * binary artwork merely because it is binary.
>
> Classify them according to context.
>
> ============================================================
> BINARY / MEDIA INSPECTION
> =========================
>
> Inventory tracked binary/media files.
>
> Without installing new software, inspect metadata where practical for:
>
> * creator/author metadata;
> * usernames;
> * filesystem paths;
> * GPS/location;
> * device model/serial;
> * software/account identifiers;
> * comments/descriptions.
>
> Do not use destructive metadata stripping.
>
> If metadata cannot be inspected reliably with existing tools, report that as an explicit coverage gap.
>
> ============================================================
> CLASSIFICATION
> ==============
>
> For every confirmed or credible finding classify it as:
>
> P0 — secret/credential or severe exposure requiring immediate action
> P1 — privacy/publication blocker before repo becomes public
> P2 — should clean/document but not necessarily publication blocking
> SAFE — intentional public technical/evidence data
> FALSE POSITIVE — matched pattern but harmless in context
>
> Also classify location:
>
> CURRENT TREE
> HISTORY ONLY
> BOTH
> LOCAL-ONLY / NOT VERSIONED
> UNKNOWN
>
> ============================================================
> REPORTING SAFETY
> ================
>
> NEVER reproduce a complete secret or potentially secret value.
>
> For sensitive values report only:
>
> * type;
> * file/path;
> * commit SHA(s) or range if relevant;
> * line number when current;
> * safe fingerprint, e.g. first 4 + last 4 characters ONLY when that itself is safe;
> * why it is sensitive;
> * whether it is current/history/both.
>
> For ADB/device identifiers, usernames, private IPs, emails, paths and similar privacy data:
>
> do not reproduce the full real value.
>
> Use placeholders such as:
>
> <WINDOWS_USERNAME>
> <GATE_SERIAL_1>
> <GATE_SERIAL_2>
> <PRIVATE_IP> <EMAIL>
>
> ============================================================
> NO-TOUCH
> ========
>
> Do NOT:
>
> * edit/redact files;
> * run git filter-repo;
> * run filter-branch;
> * run BFG;
> * run git gc/prune;
> * expire reflogs;
> * delete refs;
> * delete files;
> * rotate credentials;
> * create a backup branch/tag yet;
> * force push;
> * make GitHub public;
> * change LICENSE;
> * update README;
> * update AI documentation;
> * touch artwork;
> * perform tests unrelated to this audit.
>
> This task is DIAGNOSIS ONLY.
>
> ============================================================
> VALIDATION
> ==========
>
> Before finishing, cross-check that the audit has covered:
>
> [ ] current tracked tree
> [ ] all local branches
> [ ] remote-tracking branches
> [ ] tags
> [ ] commit messages
> [ ] historical file versions
> [ ] deleted historical files/blobs
> [ ] known Windows path finding
> [ ] known ADB serial findings
> [ ] secrets/credentials patterns
> [ ] environment data
> [ ] IP/network data
> [ ] emails/personal identifiers
> [ ] binary/media inventory
> [ ] metadata where tooling permits
> [ ] stashes
> [ ] additional worktrees
> [ ] dangling/unreachable-object relevance
> [ ] distinction between public blockchain evidence and secrets
> [ ] distinction between synthetic fixtures and real captured data
>
> If any checkbox cannot be completed, state exactly why.
>
> ============================================================
> ACCEPTANCE CRITERIA
> ===================
>
> PASS only if the final report gives us enough information to design ONE scoped sanitation operation without having to repeat discovery.
>
> The report must answer:
>
> 1. Is any REAL secret/credential confirmed anywhere?
> 2. Is credential rotation required?
> 3. What exact privacy-sensitive categories are confirmed?
> 4. Which files are affected in the current tree?
> 5. Which historical commits/blobs/paths are affected?
> 6. Which refs contain the affected history?
> 7. Are there sensitive values only in unreachable/local objects?
> 8. Are there binary metadata concerns?
> 9. What is SAFE and should NOT be removed?
> 10. What is the MINIMUM recommended sanitation strategy?
> 11. Will that strategy rewrite SHAs?
> 12. Which branches/tags/remotes would need coordinated handling?
> 13. What evidence should be retained privately before rewriting?
> 14. What must be revalidated after rewriting?
> 15. Is it safe to proceed to a remediation plan?
>
> ============================================================
> EVIDENCE REQUIRED
> =================
>
> Return this exact structure:
>
> # AUD-01 EXHAUSTIVE PRIVACY AUDIT
>
> ## REPOSITORY STATE
>
> * branch:
> * HEAD:
> * origin/main:
> * synchronized:
> * worktree:
> * local branches:
> * remote refs:
> * tags:
> * stashes:
> * additional worktrees:
>
> ## COVERAGE
>
> Table:
> Area | Method | Result | Coverage gap
>
> ## CONFIRMED FINDINGS
>
> Table:
> ID | Severity | Category | Location | File/Path | Commit(s) | Safe description | Required action
>
> Do not print sensitive values.
>
> ## POSSIBLE / NEEDS HUMAN REVIEW
>
> Same table structure.
>
> ## SAFE / FALSE POSITIVES
>
> Explain relevant matches that must NOT drive destructive cleanup.
>
> ## CURRENT TREE EXPOSURE
>
> Exact files requiring sanitation in the present version.
>
> ## HISTORICAL EXPOSURE
>
> Exact paths/commits/refs requiring history sanitation.
>
> ## BINARY / METADATA REVIEW
>
> ## SECRET ROTATION DECISION
>
> Explicitly:
> ROTATION REQUIRED / ROTATION NOT INDICATED / CANNOT DETERMINE
>
> With reasoning.
>
> ## MINIMUM SANITATION PLAN
>
> Design only. DO NOT EXECUTE.
>
> Include:
>
> * exact categories to redact;
> * whether file contents, filenames or commit messages need rewriting;
> * affected refs;
> * whether force-push would be required;
> * how to preserve legitimate development history;
> * what private mapping/evidence should be kept;
> * what local cleanup may eventually be necessary after remote sanitation.
>
> ## POST-REWRITE VALIDATION PLAN
>
> Design only. DO NOT EXECUTE.
>
> ## RESIDUAL RISKS
>
> ## VERDICT
>
> One of:
>
> AUD-01 PASS — READY TO PLAN REMEDIATION
>
> or
>
> AUD-01 INCOMPLETE — <reason>
>
> ============================================================
> STOP IF
> =======
>
> Immediately stop and report before doing anything else if:
>
> * you discover an actual private key, seed phrase, active API secret, session token, OTP or comparable credential;
> * repository state is unexpectedly dirty/divergent in a way that creates risk;
> * inspection would require installing software;
> * a command would modify Git state/history;
> * a command could expose full sensitive values in terminal output;
> * you cannot distinguish a suspected credential safely.
>
> If a secret is found, DO NOT print it and DO NOT rotate it yourself.
>
> FINAL REMINDER:
>
> READ ONLY.
> NO EDITS.
> NO HISTORY REWRITE.
> NO COMMIT.
> NO PUSH.
> NO INSTALLS.
> NO BLOCKCHAIN WRITES.
