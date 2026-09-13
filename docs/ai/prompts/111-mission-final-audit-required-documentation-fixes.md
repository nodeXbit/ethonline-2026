# Project task packet 111: MISSION — FINAL AUDIT REQUIRED DOCUMENTATION FIXES

- Classification: **ORIGINAL / SANITIZED**
- Redactions applied: none
- Source: retained project-specific Control Tower packet in the local Codex record

---

> MISSION — FINAL AUDIT REQUIRED DOCUMENTATION FIXES
>
> The final independent compliance review found exactly TWO remaining required documentation fixes.
>
> This is DOCUMENTATION ONLY.
>
> FEATURE FREEZE remains active.
>
> Expected current state:
>
> HEAD == origin/main ==
> 297633be23216a17ec84681423645ec57bebe491
>
> tracked worktree clean
> repository PRIVATE
>
> DO NOT modify application code.
> DO NOT modify tests.
> DO NOT modify blockchain state.
> DO NOT modify credentials.
> DO NOT rewrite Git history.
> DO NOT install anything.
> DO NOT make the repository public yet.
>
> ============================================================
> FIX 1 — CURRENT ANDROID DEMO RUNBOOK
> ====================================
>
> Problem:
>
> README.md currently links root DEMO_RUNBOOK.md among current/generic runtime references.
>
> docs/ai/SPECS.md describes DEMO_RUNBOOK.md as a generic public demo workflow.
>
> But root DEMO_RUNBOOK.md is the historical ESP32-S3/PN532 demo runbook, not the current final Android-gate runtime.
>
> This can mislead judges.
>
> Required remediation:
>
> 1. Create:
>
> ANDROID_GATE_DEMO_RUNBOOK.md
>
> This must be a PUBLIC, SANITIZED, role-based operational guide for the actual final demo:
>
> <HOLDER>
> → NFC/HCE
> → <FRONT_DOOR> / <LAB> / <SERVER_ROOM>
> → ADB reverse
> → Node pixel-gate bridge
> → RPC
> → ENSv2
> → ALLOW / DENY
>
> Do not include:
>
> * real ADB serials;
> * personal paths;
> * MAC addresses;
> * private IPs;
> * RPC credentials;
> * private keys;
> * private device-map contents.
>
> Document the verified restart sequence generically:
>
> 1. connect holder + three gates;
> 2. use the local private mapping to identify roles;
> 3. start exactly one:
>    `npm run pixel-gate:bridge`
> 4. confirm one local verifier listener;
> 5. configure:
>    `adb reverse tcp:8792 tcp:8792`
>    for each gate using placeholder/device-role syntax;
> 6. launch:
>    front-door
>    lab
>    server-room
> 7. verify each gate shows READY;
> 8. open holder My Keys;
> 9. select wallet/pass;
> 10. Refresh onchain;
> 11. wait for Ready to tap;
> 12. perform physical NFC.
>
> Explain:
>
> * opening Android apps alone does not reconstruct the PC runtime;
> * Node and ADB reverse are required;
> * gate authorization is Node-authoritative;
> * current ENSv2 state is read from Sepolia;
> * errors fail closed;
> * switching wallet/pass requires fresh holder selection/HCE state;
> * `gate:monitor` is historical and must not be presented as live telemetry for the current Android-gate flow.
>
> Keep it concise.
>
> 2. Update README.md:
>
> Replace the current operational reference to DEMO_RUNBOOK.md with:
>
> ANDROID_GATE_DEMO_RUNBOOK.md
>
> Keep root DEMO_RUNBOOK.md only as historical/alternate PN532 material.
>
> 3. Update docs/ai/SPECS.md:
>
> Describe:
>
> ANDROID_GATE_DEMO_RUNBOOK.md
> → current public final Android demo runtime.
>
> DEMO_RUNBOOK.md
> → historical ESP32-S3/PN532 public demo runbook retained as engineering evidence.
>
> 4. Add a prominent note near the top of root DEMO_RUNBOOK.md:
>
> HISTORICAL / ALTERNATE PN532 DEMO RUNBOOK
>
> State that the final ETHOnline release uses the Android Gate Stand architecture and link to:
>
> ANDROID_GATE_DEMO_RUNBOOK.md
>
> Do NOT rewrite its historical body as though it described the final demo.
>
> ============================================================
> FIX 2 — COMPLETE SPEC-DRIVEN PROMPT DISCLOSURE
> ==============================================
>
> Official ETHOnline 2026 rules state that a spec-driven workflow must include all spec files, prompts and planning artifacts in the submission repository.
>
> Current docs/ai/PROMPTS.md says it preserves selected/relevant excerpts.
>
> That is insufficiently literal for final compliance.
>
> Required remediation:
>
> Preserve ALL MATERIAL PROJECT-SPECIFIC AI TASK PACKETS / PROMPTS that actually directed implementation, review, validation, sanitation and release.
>
> Do NOT publish unrelated personal conversations.
>
> Do NOT publish secrets or privacy-sensitive operational values.
>
> Preferred structure:
>
> docs/ai/prompts/
> 01-ensv2-vertical.md
> 02-holder-proof.md
> 03-privy-signer.md
> ...
> final-release.md
>
> or another clear ordered structure.
>
> docs/ai/PROMPTS.md should become an INDEX explaining the corpus and redaction policy.
>
> For each available material project-specific packet:
>
> * reproduce the complete task packet rather than only an excerpt;
> * preserve the actual wording where available;
> * replace private content with explicit:
>   `[REDACTED: reason]`;
> * never invent missing text;
> * never describe reconstructed text as original;
> * if a genuinely unavailable historical prompt exists, identify it explicitly as unavailable and explain what tracked planning artifact captures the corresponding direction.
>
> At minimum cover the already identified material workstreams:
>
> * first ENSv2 Sepolia vertical;
> * Gate A holder proof;
> * Gate B Privy signer;
> * Gate C1 HCE;
> * Gate C2 Privy/HCE;
> * Gate E authoritative composition;
> * Android credential product vertical;
> * wallet experience foundation;
> * Pass Studio;
> * dynamic NFC/resource-aware gates;
> * four-device demo rig;
> * final demo state/feature freeze;
> * privacy audit;
> * history sanitation;
> * runtime restoration;
> * release documentation;
> * final compliance/remediation packets available in the project record.
>
> Include the exact three original image-generation prompts already preserved.
>
> Do NOT publish:
>
> * private keys;
> * credential-bearing RPC URLs;
> * real ADB serials;
> * real MAC;
> * personal filesystem paths;
> * OTP;
> * login/account material;
> * unrelated user conversation.
>
> Update AI_USAGE.md / docs/ai/SPECS.md only if necessary so their wording accurately reflects the resulting complete sanitized project-specific prompt corpus.
>
> Do not falsely claim that every conversational message from ChatGPT is published.
>
> The goal is:
>
> all project-specific spec-driven task packets/prompts needed to reconstruct how AI was directed are public and sanitized.
>
> ============================================================
> PRIVACY / QUALITY VALIDATION
> ============================
>
> Before staging:
>
> Verify all newly added/changed tracked documentation contains no:
>
> * personal Windows path;
> * real ADB/device serial;
> * real device MAC;
> * private LAN configuration;
> * credential-bearing RPC URL;
> * private key;
> * OTP/session token;
> * unrelated personal email/account information.
>
> Verify all Markdown links resolve.
>
> Verify the new Android runbook does NOT depend on ignored `.runtime` files being public.
>
> Verify old historical runbook is clearly historical.
>
> Verify README and SPECS point judges to the correct CURRENT runbook.
>
> Verify prompt files contain explicit redaction markers wherever private content was removed.
>
> Do not modify any code/build file.
>
> ============================================================
> GIT
> ===
>
> If all validation passes:
>
> Stage documentation only.
>
> Commit:
>
> docs: close final compliance gaps
>
> Push normally:
>
> git push origin main
>
> NO force push.
>
> Repository remains PRIVATE.
>
> After push verify:
>
> HEAD == origin/main
> worktree clean
>
> ============================================================
> OUTPUT
> ======
>
> Return:
>
> # FINAL COMPLIANCE FIX RESULT
>
> ## BASE STATE
>
> ## CURRENT ANDROID RUNBOOK
>
> * created:
> * README corrected:
> * SPECS corrected:
> * historical runbook labelled:
>
> ## AI PROMPT CORPUS
>
> * material task packets identified:
> * complete sanitized packets published:
> * unavailable packets:
> * reconstructed-as-original: NO
> * privacy redactions:
>
> ## PRIVACY CHECK
>
> ## GIT
>
> * commit:
> * HEAD:
> * origin/main:
> * clean:
> * repository visibility: PRIVATE
>
> ## REMAINING COMPLIANCE GAPS
>
> ## VERDICT
>
> One exact line:
>
> FINAL COMPLIANCE FIX PASS — READY TO MAKE REPOSITORY PUBLIC
>
> or
>
> FINAL COMPLIANCE FIX INCOMPLETE — <specific blocker>
>
> ============================================================
> STOP IF
> =======
>
> STOP before commit if:
>
> * complete material project-specific prompt packets cannot be recovered honestly;
> * sanitization would require guessing/reconstruction represented as original;
> * a privacy value would have to be published;
> * current Android runtime cannot be documented without private identifiers;
> * any non-documentation tracked file changes.
>
> No code changes.
> No blockchain writes.
> No installs.
> No Git history rewrite.
> No repository visibility change.
