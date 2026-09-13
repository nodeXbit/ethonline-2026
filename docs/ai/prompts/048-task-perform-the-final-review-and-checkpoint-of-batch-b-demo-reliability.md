# Project task packet 048: TASK — Perform the FINAL REVIEW AND CHECKPOINT of Batch B — Demo Reliability.

- Classification: **ORIGINAL / SANITIZED**
- Redactions applied: none
- Source: retained project-specific Control Tower packet in the local Codex record

---

> TASK
>
> Perform the FINAL REVIEW AND CHECKPOINT of Batch B — Demo Reliability.
>
> The implementation and manual browser validation are complete.
>
> The user authorizes:
>
> - read-only review of all current local changes
> - validation commands
> - scoped fixes ONLY if a concrete regression/security issue is discovered
> - one implementation commit
> - project-control documentation updates
> - one documentation commit
> - push both commits to origin/main
>
> DO NOT:
>
> - perform blockchain writes
> - activate/deactivate/renew credentials
> - run NFC
> - present the Seeker
> - flash firmware
> - deploy publicly
> - update README
> - add new Batch B features
> - redesign UI
> - install new dependencies
> - broaden scope
>
> ============================================================
> EXPECTED BASELINE
> ============================================================
>
> Last pushed baseline before Batch B:
>
> 496ac4577ec479c8e4b284a566d85053d90b841e
>
> Current local work intentionally contains Batch B changes.
>
> First inspect:
>
> git status -sb
> git diff --stat
> git diff
>
> Do not assume files from prior reports are the complete diff.
>
> ============================================================
> MANUAL VISUAL EVIDENCE
> ============================================================
>
> The final ChatGPT Work browser review was performed at:
>
> 1366 × 900
> normal zoom
>
> Result:
>
> PASS
>
> Observed:
>
> - guest-001.demo-access.eth visible and legible
> - ENSv2 POLICY visible
> - HOLDER VERIFIER visible
> - PHYSICAL CONTROLLER visible
> - the three primary cards visible together in three columns
> - ACTIVE / POLICY ALLOW clearly represented as policy state
> - nearby clarification requires controller confirmation
> - ACTIVE is not presented as confirmed physical opening
> - PHYSICAL CONTROLLER remains NOT RUN without physical evidence
> - no proof/signature bytes visible
> - Recovered signer displays —
> - primary text readable without zoom
> - no buttons clicked
> - no files/data modified by browser review
>
> Treat this as the required manual browser smoke PASS.
>
> Do not request another visual redesign.
>
> ============================================================
> PHASE 1 — REVIEW THE ACTUAL DIFF
> ============================================================
>
> Review every changed/untracked file.
>
> Expected Batch B areas include, but are not limited to:
>
> - scripts/security/demo-config.mjs
> - demo/server.mjs
> - demo/preflight.mjs
> - demo/serial-readiness.mjs
> - demo/bridge.mjs
> - scripts/security/gate-e-secure-bridge.mjs
> - demo/evidence.mjs
> - demo/public/*
> - demo/server.test.mjs
> - demo/reliability.test.mjs
> - package.json
> - .gitignore
> - DEMO_RUNBOOK.md
>
> Confirm from the actual diff:
>
> 1. canonical demo credential is guest-001.demo-access.eth;
> 2. generic historical cred-001 behavior outside the demo remains intact;
> 3. preflight is read-only;
> 4. preflight does not issue Gate A challenges;
> 5. preflight does not sign/broadcast;
> 6. runtime evidence cannot affect authorization;
> 7. evidence reporting is best-effort/non-authoritative;
> 8. stale success is cleared/IN_PROGRESS at new attempt start;
> 9. raw proof/signature/challenge nonce/typed data/secrets/RPC URLs are excluded;
> 10. VERIFIER_ALLOW cannot become physical success without controller
>     confirmation;
> 11. transport failure remains distinct from policy DENY;
> 12. replay state is distinct;
> 13. loopback-only server boundary remains;
> 14. Host/Origin mutation protections remain;
> 15. no CORS was introduced;
> 16. activate/deactivate reuse existing safe ENS transaction paths;
> 17. no duplicate authorization-policy implementation was introduced;
> 18. APDU v1 unchanged;
> 19. firmware unchanged;
> 20. Android unchanged unless the actual diff proves otherwise.
>
> If a concrete P0/P1 defect is found:
> fix only that defect, then revalidate.
>
> Do not perform polish beyond the validated UI.
>
> ============================================================
> PHASE 2 — SECURITY REVIEW
> ============================================================
>
> Check:
>
> - .runtime is ignored;
> - runtime evidence contains no secret fields;
> - no real proof/signature has entered Git;
> - no OTP;
> - no private key;
> - no RPC URL/credential;
> - no Privy secret/token;
> - no local config accidentally staged;
> - no generated runtime report staged;
> - no fabricated physical success evidence staged.
>
> Inspect staged content again before commit.
>
> ============================================================
> PHASE 3 — FINAL VALIDATION
> ============================================================
>
> Run the relevant final validation from the resulting tree.
>
> At minimum:
>
> node --test --test-isolation=none
>
> Record exact total.
>
> Run targeted demo/reliability tests if useful separately and record exact total.
>
> Run:
>
> git diff --check
>
> Because Android and firmware should be unchanged, do not waste time rebuilding
> them again unless the actual diff shows they changed or previous validation can
> no longer be trusted.
>
> Record the already-completed Batch B validation evidence:
>
> Android:
> 21/21 PASS
> assembleDebug PASS
>
> Firmware:
> unchanged from Gate E validated checkpoint
>
> Manual browser:
> PASS at 1366x900 normal zoom
>
> Do not click activate/deactivate as part of validation.
>
> Require:
>
> blockchain writes = 0
> NFC attempts = 0
> firmware flashes = 0
>
> ============================================================
> PHASE 4 — IMPLEMENTATION COMMIT
> ============================================================
>
> If review and validation PASS:
>
> stage ONLY Batch B implementation files.
>
> Do NOT stage project-control docs yet.
>
> Inspect:
>
> git diff --cached --stat
> git diff --cached
>
> Commit with:
>
> feat: add deterministic demo reliability workflow
>
> Record commit SHA.
>
> Do not push yet if project-control docs still need updating.
>
> ============================================================
> PHASE 5 — PROJECT STATE DOCUMENTATION
> ============================================================
>
> Now update only the project-control docs that need the new state:
>
> STATUS.md
> WORKLOG.md
> DECISIONS.md
> PROJECT.md
>
> Do NOT update README.md yet.
>
> STATUS.md should record:
>
> BATCH B — DEMO RELIABILITY:
> PASS
>
> Capabilities now available:
>
> - canonical guest-001 demo configuration
> - npm run demo
> - npm run demo:preflight
> - optional boot observation preflight mode
> - npm run demo:bridge
> - replay-check bridge mode
> - sanitized last-attempt evidence
> - separate ENSv2 POLICY / HOLDER VERIFIER / PHYSICAL CONTROLLER UI
> - SYSTEM READINESS
> - dedicated cold-boot/demo runbook
> - final manual visual PASS at 1366x900
>
> Record that:
>
> - Gate E security path remains unchanged
> - no physical rehearsal has yet been executed with the finalized Batch B UX
> - current guest state remains whatever authoritative read-only state is observed
>   if checked; do not mutate it
>
> Next objective:
>
> LIVE DEMO REHEARSAL
>
> WORKLOG.md:
>
> Record concise Batch B implementation and validation evidence.
>
> Include final Node test count from this run and manual visual PASS.
>
> DECISIONS.md:
>
> Record:
>
> - guest-001 is the only canonical secure demo credential;
> - UI must separate POLICY, HOLDER VERIFIER and PHYSICAL CONTROLLER;
> - verifier ALLOW never implies physical success;
> - runtime evidence is advisory/local/non-authoritative;
> - loopback server remains the security boundary;
> - cold boot is the pre-session operational rule;
> - no database/event bus/framework migration;
> - Batch B is closed unless rehearsal discovers a real blocker.
>
> PROJECT.md:
>
> Update only stale product/current-state statements.
>
> Do not turn it into a worklog.
>
> ============================================================
> PHASE 6 — DOCUMENTATION COMMIT
> ============================================================
>
> Review docs diff.
>
> Run:
>
> git diff --check
>
> Stage only expected project-control docs.
>
> Commit:
>
> docs: checkpoint Batch B demo reliability
>
> Record commit SHA.
>
> ============================================================
> PHASE 7 — PUSH / FINAL SYNC
> ============================================================
>
> Push to origin/main.
>
> Then run:
>
> git status -sb
> git rev-parse HEAD
> git rev-parse origin/main
>
> Require:
>
> working tree clean
> HEAD == origin/main
>
> No pending untracked runtime evidence.
>
> ============================================================
> RETURN
> ============================================================
>
> # BATCH B FINAL CHECKPOINT
>
> ## DIFF REVIEW
>
> List actual implementation files and summarize security-sensitive findings.
>
> ## SECURITY REVIEW
>
> ## NODE VALIDATION
>
> Include exact full-suite count.
>
> ## PREVIOUS REGRESSION EVIDENCE
>
> Android:
> Firmware:
>
> ## MANUAL BROWSER VALIDATION
>
> Record:
>
> 1366x900
> normal zoom
> PASS
>
> and the key policy/controller distinction.
>
> ## IMPLEMENTATION COMMIT
>
> SHA:
> message:
>
> ## PROJECT DOCS UPDATED
>
> ## DOCUMENTATION COMMIT
>
> SHA:
> message:
>
> ## PUSH
>
> ## FINAL GIT STATE
>
> Require clean and synchronized.
>
> ## BATCH B RESULT
>
> State:
>
> BATCH B — DEMO RELIABILITY: PASS
>
> ## NEXT OBJECTIVE
>
> LIVE DEMO REHEARSAL
>
> Do NOT execute the rehearsal.
>
> End exactly:
>
> BATCH B CHECKPOINT: PASS
>
> or
>
> BATCH B CHECKPOINT: STOP — <exact reason>
