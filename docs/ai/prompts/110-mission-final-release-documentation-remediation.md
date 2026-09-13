# Project task packet 110: MISSION — FINAL RELEASE DOCUMENTATION REMEDIATION

- Classification: **ORIGINAL / SANITIZED**
- Redactions applied: personal filesystem path
- Source: retained project-specific Control Tower packet in the local Codex record

---

> MISSION — FINAL RELEASE DOCUMENTATION REMEDIATION
>
> FEATURE FREEZE remains active.
>
> Current sanitized release base:
>
> e7ce904c7ff4156ed7b7aecd115e54c86fa4a6df
>
> Expected:
>
> HEAD == origin/main == exact SHA above
> clean tracked worktree
> GitHub repository remains PRIVATE
>
> PRIV-01 is complete.
>
> No application code, blockchain state, credentials, firmware source or product behavior may change.
>
> ============================================================
> TASK
> ====
>
> Close the remaining release/documentation findings in one coherent documentation-only increment:
>
> * LIC-01
> * DOC-01
> * CLAIM-01
> * AI-01
> * AST-01
> * release-state documentation consistency
> * private demo runbook discrepancy
>
> Do not add features.
>
> ============================================================
> OFFICIAL ETHONLINE 2026 AI REQUIREMENT
> ======================================
>
> Current official rules require transparent AI attribution:
>
> * document where/how AI tools were used;
> * identify code/files/assets generated or assisted by AI;
> * meaningful human involvement must be clear;
> * if a spec-driven workflow was used, include the relevant specs, prompts and planning artifacts so judges can understand how AI was directed.
>
> Do not fabricate original prompts.
>
> Do not claim a specific model was used unless supported by evidence.
>
> Do not publish unrelated personal conversation content, credentials, OTPs, account information or secrets.
>
> ============================================================
> 1 — RECONFIRM REPOSITORY STATE
> ==============================
>
> Before editing:
>
> * git status -sb
> * git rev-parse HEAD
> * git rev-parse origin/main
>
> Expected exact SHA:
>
> e7ce904c7ff4156ed7b7aecd115e54c86fa4a6df
>
> STOP on any discrepancy.
>
> ============================================================
> 2 — LICENSE
> ===========
>
> Add a root:
>
> LICENSE
>
> Use the MIT License for original LockENS software.
>
> Copyright line:
>
> Copyright (c) 2026 nodeXbit
>
> Do NOT imply that third-party photographs/assets are relicensed under MIT.
>
> README and attribution documentation must make this distinction clear.
>
> ============================================================
> 3 — ATTRIBUTIONS / ASSET PROVENANCE
> ===================================
>
> Create:
>
> ATTRIBUTIONS.md
>
> Use repository evidence and authoritative source URLs already documented/recoverable.
>
> Cover at minimum the four known demo artwork sources:
>
> * Standard-lock-key.jpg
> * Front_door_(23200352782).jpg
> * Stop_sign.png
> * Server_racks,*one_empty*-_IMG_3636.jpg
>
> For each record:
>
> * asset/use;
> * original creator when known;
> * exact source page URL;
> * original license/public-domain status;
> * required attribution;
> * whether LockENS cropped/resized/converted/otherwise modified it.
>
> For the server-rack image specifically preserve its CC BY 2.0 obligations:
>
> * creator credit;
> * source link;
> * CC BY 2.0 license link;
> * indication of modifications if applicable.
>
> Also identify the three locally packaged Gate Stand scene backgrounds.
>
> For original/AI-assisted assets:
>
> * state the actual provenance supported by evidence;
> * distinguish human-created vs AI-assisted/generated if known;
> * preserve C2PA provenance;
> * do not invent a tool/model if evidence does not establish it.
>
> Do NOT replace artwork.
> Do NOT perform blockchain writes.
>
> ============================================================
> 4 — README — ONE CURRENT STORY
> ==============================
>
> Rewrite/polish README.md so a judge sees ONE current LockENS product rather than mixed historical checkpoints.
>
> The beginning must answer quickly:
>
> * What is LockENS?
> * What problem does it solve?
> * What does the demo actually demonstrate?
> * Why ENSv2 is load-bearing?
> * What role Privy plays?
>
> Current demo architecture must be described as:
>
> Android holder
> → NFC / Android HCE
> → Android NFC gate
> → local Node authoritative verifier
> → Sepolia RPC
> → ENSv2 UserRegistry + PermissionedResolver / resource policy
> → ALLOW / DENY
> → virtual gate UI
>
> Be precise:
>
> * Node verifies holder proof offchain.
> * Node reads current ENSv2 state on Sepolia.
> * ENSv2 resource policy participates in authorization.
> * Privy provides embedded wallets/signing; Privy is not the resource authorization engine.
> * Gate opens only after authoritative Node ALLOW.
>
> Do NOT claim:
>
> * physical door actuation;
> * production readiness;
> * fully decentralized architecture;
> * offline/autonomous operation;
> * universal Android compatibility;
> * secure hardware gate enrollment;
> * PN532 universal/stable production readiness;
> * instant atomic physical revocation;
> * “onchain signature verification” if verification is actually offchain.
>
> Use wording such as:
>
> “virtual gate animation opens after authoritative ALLOW”
>
> rather than implying a physical lock actuates.
>
> Describe the final demo as:
>
> “one Android holder and three Android NFC gate stands”
>
> without making device brands central to the product narrative.
>
> Move/model-specific validation can live in a technical appendix/section.
>
> ============================================================
> 5 — REMOVE README CONTRADICTIONS
> ================================
>
> Remove or clearly mark historical states such as:
>
> RESOURCE_POLICY_MISSING
> pending Android fallback
> old architecture checkpoints
> legacy/current ambiguity
>
> The README must not make a judge think the final demo is unfinished.
>
> ESP32-S3 + PN532 remains an alternative/experimental hardware route, clearly separate from the primary final Android-gate demo.
>
> Do not delete historical reports merely because they document earlier checkpoints.
>
> Label historical material as historical where necessary.
>
> ============================================================
> 6 — CURRENT SETUP / REPRODUCIBILITY
> ===================================
>
> Document accurately:
>
> * Node >=24
> * Node test command uses `node --test` / actual current command, not a nonexistent `npm test`
> * Android Java/SDK requirements supported by current Gradle configuration
> * build/test commands
> * `.env.example` usage
> * private environment values never committed
> * compiling/testing is distinct from operating as an authorized issuer
> * local Node is part of the final demo architecture
> * public judges do NOT need the developer's issuer credentials to inspect/build the source
>
> Do not expose:
>
> * personal paths
> * device serials
> * real MACs
> * RPC credentials
> * private keys
> * operational private network data
>
> ============================================================
> 7 — DEMO / EVIDENCE / LIMITATIONS
> =================================
>
> README should concisely summarize verified final evidence:
>
> * physical NFC demonstrated;
> * final resource policy cases;
> * multi-wallet selection;
> * HCE invalidation/state handling;
> * Node test count where current evidence supports it;
> * Android test/build validation;
> * Sepolia / ENSv2 configuration/evidence;
> * firmware compile evidence as an alternative route.
>
> Keep limitations explicit:
>
> * Sepolia prototype;
> * virtual gate UI, not a physical actuator;
> * local PC Node dependency;
> * tested device matrix, not universal Android compatibility;
> * alternate PN532 route has separate limitations.
>
> Do not overclaim.
>
> ============================================================
> 8 — AI TRANSPARENCY
> ===================
>
> Create:
>
> AI_USAGE.md
> docs/ai/SPECS.md
> docs/ai/PROMPTS.md
>
> AI_USAGE.md must explain:
>
> TOOLS
>
> * ChatGPT used as control tower for planning, research, architecture discussion, review, debugging strategy, release/compliance work and learning support.
> * Codex used for repository inspection, scoped implementation, tests, builds, local operational validation and documentation changes.
> * Mention other AI tools ONLY if repository/project evidence shows they materially contributed.
>
> HUMAN CONTRIBUTION
>
> Make clear that the participant performed/controlled:
>
> * product selection and scope;
> * trade-off decisions;
> * approval of significant changes;
> * wallet/blockchain action authorization;
> * physical hardware/NFC setup and validation;
> * manual observation of demo results;
> * release decisions;
> * final narration and submission responsibility.
>
> AI CONTRIBUTION MAP
>
> Map AI assistance at a useful file/layer level:
>
> * Android
> * Node
> * firmware
> * tests
> * documentation
> * planning/release
> * artwork/assets where supported
>
> Do not falsely say code was manually authored if AI assisted substantially.
>
> Do not falsely imply the project was autonomously built by AI.
>
> ============================================================
> 9 — SPECS / PROMPTS / PLANNING ARTIFACTS
> ========================================
>
> `docs/ai/SPECS.md`:
>
> Create an INDEX to the genuine project specifications/planning artifacts already present in the repository, including as applicable:
>
> * PROJECT.md
> * AGENTS.md
> * DECISIONS.md
> * WORKLOG.md
> * STATUS.md
> * FINAL_DEMO_STATE.md
> * test plans
> * architecture/security/audit documents
> * implementation plans and handoff documents that genuinely directed work
>
> Explain the role of each category.
>
> Do NOT duplicate entire files unnecessarily.
>
> `docs/ai/PROMPTS.md`:
>
> Preserve the genuine material implementation/review task packets available in the Codex conversation/context for this project where they can be reproduced accurately.
>
> Requirements:
>
> * exact/relevant project task packets where available;
> * remove/redact secrets, real device identifiers, personal paths and unrelated personal context;
> * mark redactions explicitly;
> * distinguish:
>   ORIGINAL / SANITIZED
>   from
>   RECONSTRUCTED SUMMARY
>
> Do NOT manufacture an “original prompt” from memory.
>
> If full historical ChatGPT control-tower prompts are not accessible to Codex:
>
> state that limitation accurately in `docs/ai/PROMPTS.md`.
>
> Create a clearly marked section indicating which additional project-specific ChatGPT prompt artifacts must still be supplied by the Control Tower before final public release.
>
> Do not claim AI compliance PASS if those artifacts are genuinely still missing.
>
> ============================================================
> 10 — PROJECT CONTROL DOCS
> =========================
>
> Inspect:
>
> PROJECT.md
> AGENTS.md
> STATUS.md
> DECISIONS.md
> WORKLOG.md
> FINAL_DEMO_STATE.md
>
> Keep all of them unless there is a concrete release/privacy reason not to.
>
> At minimum update STATUS.md so it reflects:
>
> * FEATURE FREEZE
> * sanitized history completed
> * current SHA after this documentation commit will be new
> * final runtime preflight PASS
> * privacy audit PASS
> * remaining work: final compliance check, public visibility, video, submission
>
> Add a concise WORKLOG entry describing:
>
> * privacy audit;
> * controlled history sanitation;
> * runtime restoration root cause;
> * validation outcome;
>
> without restoring redacted identifiers.
>
> Only modify PROJECT.md / DECISIONS.md / AGENTS.md if evidence shows they are materially inconsistent with the final release.
>
> Do not churn them cosmetically.
>
> ============================================================
> 11 — PRIVATE DEMO RUNBOOK
> =========================
>
> `.runtime/DEMO_RUNBOOK.md` is ignored/private and was found to contain an outdated device-role mapping.
>
> The authoritative current mapping is:
>
> .runtime/demo-device-map.json
>
> Update the PRIVATE runbook locally so future demo/video startup cannot repeat that mistake.
>
> Requirements:
>
> * use role labels rather than publishing real serials;
> * explicitly state `demo-device-map.json` is authoritative;
> * preserve the verified restart sequence:
>   Node bridge
>   → 3 ADB reverses
>   → correct gate profiles
>   → holder wallet/pass
>   → Refresh onchain
>   → Ready to tap
>
> Also document that the startup script may stop on a harmless “activity already in foreground” ADB warning and that the validated manual equivalent exists.
>
> This file remains ignored and must NOT be staged/committed.
>
> ============================================================
> 12 — PRIVACY RECHECK
> ====================
>
> Before staging:
>
> Search all changed TRACKED documentation for:
>
> [REDACTED: personal filesystem path]
> * real device serials
> * MAC addresses
> * credential-bearing RPC URLs
> * private key patterns
> * OTP/token patterns
> * unnecessary personal emails
> * private LAN configuration
>
> Do not print sensitive matches.
>
> Known public/test data and synthetic fixtures must be context-classified, not blindly removed.
>
> ============================================================
> 13 — VALIDATION
> ===============
>
> Verify:
>
> * README describes only the final current demo;
> * LICENSE exists;
> * ATTRIBUTIONS.md exists;
> * AI_USAGE.md exists;
> * docs/ai/SPECS.md exists;
> * docs/ai/PROMPTS.md exists;
> * internal Markdown links used are valid;
> * no tracked private identifiers were reintroduced;
> * no application/code/build configuration changed;
> * ignored `.runtime/DEMO_RUNBOOK.md` is updated but remains untracked/ignored;
> * worktree diff contains documentation-only tracked changes.
>
> Show a concise `git diff --stat` and changed tracked path list.
>
> Do not dump giant full diffs unless needed.
>
> ============================================================
> 14 — COMMIT / PUSH AUTHORIZATION
> ================================
>
> If ALL acceptance criteria pass:
>
> Stage ONLY the intended tracked documentation files.
>
> DO NOT stage `.runtime`.
>
> Commit with:
>
> docs: prepare final public release
>
> Then push normally:
>
> git push origin main
>
> NO force push.
>
> After push verify:
>
> HEAD == origin/main
>
> and worktree tracked state is clean.
>
> Repository remains PRIVATE.
>
> ============================================================
> ACCEPTANCE CRITERIA
> ===================
>
> PASS requires:
>
> * MIT license for original software;
> * third-party assets remain under their own licenses;
> * artwork attribution complete enough for public release;
> * current architecture unambiguous;
> * claims bounded by evidence;
> * README reproducible and coherent;
> * AI usage honestly documented;
> * genuine specs/planning indexed;
> * prompt coverage accurately represented without fabrication;
> * project control docs consistent;
> * private demo runbook corrected;
> * privacy recheck PASS;
> * docs-only commit/push PASS;
> * GitHub still PRIVATE.
>
> ============================================================
> STOP IF
> =======
>
> STOP before commit if:
>
> * AI prompt requirement cannot be represented honestly;
> * an asset license/source cannot be established;
> * resolving README contradictions would require changing code;
> * a secret/privacy exposure is discovered;
> * tracked state includes code changes;
> * repo is no longer clean/synchronized at task start;
> * any uncertain provenance would require inventing facts.
>
> Return the blocker rather than guessing.
>
> ============================================================
> EVIDENCE
> ========
>
> Return:
>
> # FINAL RELEASE DOCUMENTATION RESULT
>
> ## BASE STATE
>
> ## FILES ADDED
>
> ## FILES UPDATED
>
> ## README / CLAIMS
>
> ## LICENSE
>
> ## ATTRIBUTIONS
>
> ## AI TRANSPARENCY
>
> Explicitly state whether prompt/spec coverage is COMPLETE or STILL REQUIRES CONTROL-TOWER MATERIAL.
>
> ## PROJECT CONTROL DOCS
>
> ## PRIVATE RUNBOOK
>
> * updated:
> * ignored:
> * staged: NO
>
> ## PRIVACY RECHECK
>
> ## GIT
>
> * commit:
> * HEAD:
> * origin/main:
> * clean:
> * repository visibility: PRIVATE
>
> ## REMAINING REQUIRED FIXES
>
> ## VERDICT
>
> One exact line:
>
> RELEASE DOCS PASS — READY FOR FINAL AUDIT
>
> or
>
> RELEASE DOCS INCOMPLETE — <specific blocker>
