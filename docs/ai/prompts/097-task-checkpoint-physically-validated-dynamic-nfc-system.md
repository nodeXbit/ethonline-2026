# Project task packet 097: TASK — CHECKPOINT PHYSICALLY VALIDATED DYNAMIC NFC SYSTEM

- Classification: **ORIGINAL / SANITIZED**
- Redactions applied: none
- Source: retained project-specific Control Tower packet in the local Codex record

---

> TASK — CHECKPOINT PHYSICALLY VALIDATED DYNAMIC NFC SYSTEM
>
> MISSION
>
> Create a clean Git/GitHub checkpoint of the CURRENT physically validated
> Dynamic NFC + Virtual Gates state before any Pixel-reader or monitor-animation
> work begins.
>
> DO NOT implement new features.
>
> DO NOT perform blockchain writes.
>
> DO NOT sign anything.
>
> DO NOT flash firmware.
>
> DO NOT install a different APK unless validation proves the currently installed
> artifact is inconsistent with the worktree.
>
> ============================================================
> KNOWN PHYSICAL RESULT
> ============================================================
>
> The current system has physically completed:
>
> staff-001.keys.demo-access.eth
> → Virtual Gate Lab
> → GET_CREDENTIAL success
> → 109-byte SEND_CHALLENGE success
> → Android holder processing
> → 67-byte APDU signature response
> → 65-byte holder proof
> → holder verified
> → registration valid
> → access.v1 Allowed
> → proof fresh
> → controller confirmed
> → ACCESS DENIED / RESOURCE_POLICY_MISSING
>
> This was an authorization decision, not a transport failure.
>
> Current reference firmware:
>
> .runtime/firmware-irq-cause/pn532_dynamic_access.ino.bin
>
> SHA-256:
>
> 4914015019c3659de25fd13d55ccb314b09ac4b0dfd7667f5407874e9b08ed9e
>
> Do not commit .runtime artifacts if they are intentionally ignored.
>
> ============================================================
> BASELINE
> ============================================================
>
> Previous pushed baseline:
>
> 20c2dd9866f8c48bc0611dee34b9b272906a3429
>
> Inspect the entire current worktree before doing anything.
>
> ============================================================
> PHASE 1 — DIFF AUDIT
> ============================================================
>
> Classify every tracked change into:
>
> A. Dynamic NFC / Virtual Gates production implementation
> B. physically validated reliability improvement
> C. Gate Monitor coordination / UI
> D. diagnostics useful enough to preserve
> E. documentation
> F. temporary/debug-only change that should NOT enter the checkpoint
> G. unrelated/suspicious change
>
> Do not discard anything automatically.
>
> If an F/G item exists, report it before removing/reverting it unless it is
> obviously generated/ignored.
>
> Check for secrets.
>
> No .env, auth token, OTP, raw proof/signature or private material may enter Git.
>
> ============================================================
> PHASE 2 — CANONICAL DOCS
> ============================================================
>
> Review whether these need updating to reflect the REAL current state:
>
> STATUS.md
> PROJECT.md
> DECISIONS.md
> WORKLOG.md
> README.md
>
> Only make scoped documentation corrections where necessary.
>
> The truthful state must say:
>
> - Dynamic NFC works physically end-to-end.
> - One physical verifier simulates virtual resources.
> - Existing staff currently has no resources.v1.
> - Physical result achieved:
>   RESOURCE_POLICY_MISSING.
> - No physical lock/relay actuator exists.
> - PN532/I2C boot stability is not fully characterized.
> - Current reference firmware/hash is recorded.
> - Pixel Reader fallback is NOT implemented yet.
>
> Do not overstate stability.
>
> ============================================================
> PHASE 3 — VALIDATION
> ============================================================
>
> Run the full relevant validations using actual current source:
>
> Android unit tests
> assembleDebug
>
> Node full test suite where allowed by current project policy
>
> dynamic firmware build
>
> legacy firmware build
>
> Gate Monitor tests
>
> git diff --check
>
> Use actual counts.
>
> Do not generate real wallet signatures merely to increase test count.
>
> ============================================================
> PHASE 4 — GIT CHECKPOINT
> ============================================================
>
> If and only if:
>
> - diff is scoped;
> - tests/builds pass;
> - no secrets;
> - documentation is truthful;
>
> create a coherent checkpoint.
>
> Prefer 1–2 commits maximum.
>
> Example structure if the diff naturally supports it:
>
> feat: add dynamic LockENS NFC virtual gates
>
> docs: record physical NFC validation
>
> Do not force two commits if one coherent commit is safer.
>
> I AUTHORIZE commit and push for this CURRENT validated work only.
>
> Push to origin/main.
>
> ============================================================
> PHASE 5 — VERIFY SYNCHRONIZATION
> ============================================================
>
> Run:
>
> git fetch origin
> git status -sb
> git rev-parse HEAD
> git rev-parse origin/main
>
> Require:
>
> clean worktree
> HEAD == origin/main
> ahead/behind 0/0
>
> Return final SHA as:
>
> PHYSICAL_NFC_BASELINE_SHA
>
> ============================================================
> DO NOT DO
> ============================================================
>
> Do NOT:
>
> - implement Pixel Reader Mode;
> - change PN532 transport;
> - fix the negative-length library bug;
> - investigate SCL;
> - add resources.v1 onchain;
> - change STAFF;
> - implement animations;
> - create credentials;
> - make any blockchain write.
>
> ============================================================
> RETURN
> ============================================================
>
> # LOCKENS PHYSICAL NFC — CHECKPOINT
>
> ## DIFF AUDIT
>
> Production:
>
> Reliability:
>
> Diagnostics preserved:
>
> Excluded:
>
> Documentation:
>
> ## VALIDATION
>
> Android:
>
> assemble:
>
> Node:
>
> Firmware dynamic:
>
> Firmware legacy:
>
> Monitor:
>
> diff check:
>
> ## PHYSICAL EVIDENCE PRESERVED
>
> Credential:
>
> Resource:
>
> Holder proof:
>
> Global access:
>
> Resource policy:
>
> Final decision:
>
> Reference firmware SHA-256:
>
> ## GIT
>
> Commit(s):
>
> PHYSICAL_NFC_BASELINE_SHA:
>
> HEAD:
>
> origin/main:
>
> status:
>
> ## SECURITY
>
> Secrets:
> 0
>
> Blockchain writes:
> 0
>
> Signatures:
> 0
>
> Firmware flashes:
> 0
>
> End exactly:
>
> PHYSICAL NFC CHECKPOINT COMPLETE — READY FOR ANDROID GATE READER FALLBACK
