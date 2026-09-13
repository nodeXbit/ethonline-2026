# Project task packet 024: TASK — Perform ONE bounded unchanged Gate D physical repeatability session.

- Classification: **ORIGINAL / SANITIZED**
- Redactions applied: none
- Source: retained project-specific Control Tower packet in the local Codex record

---

> TASK
>
> Perform ONE bounded unchanged Gate D physical repeatability session.
>
> Do not modify code before the run.
>
> CURRENT EVIDENCE
>
> The original Gate D run failed transiently at GET_SIGNATURE.
>
> A later diagnostic reproduction using the unchanged permanent APDU v1 and
> unchanged Gate D firmware completed successfully:
>
> TARGET ACTIVATION: PASS
> SELECT: PASS
> SEND_CHALLENGE: PASS
> STATUS PROCESSING: PASS
> STATUS READY: PASS
> GET_SIGNATURE: PASS
> SIGNATURE LENGTH: 65
> Node recovered signer:
> 0x3419148731087b970d2059C53780163B452D5FF7
> MATCH: PASS
>
> Android telemetry for that successful session confirmed:
>
> GET_SIGNATURE received: YES
> processor state: READY
> Host response length: 67
> status: 9000
>
> Therefore:
>
> - 67-byte APDU responses are physically viable
> - firmware 68-byte caller capacity is sufficient
> - chunking is NOT justified
> - APDU v1 must remain unchanged
>
> The root cause of the original failed GET_SIGNATURE remains unknown and is
> currently classified as a transient RF/ISO-DEP/PN532 HAL event.
>
> CURRENT REPOSITORY
>
> Expected:
>
> HEAD == origin/main ==
> 5cb7d3561492afeb5b9477b32ab9dd34688df7d6
>
> Expected uncommitted work includes:
>
> - firmware/pn532_hce_apdu/pn532_hce_apdu.ino
> - temporary Android transport diagnostic changes
>
> Do not commit.
> Do not push.
>
> PHASE 1 — READ-ONLY STATE
>
> Confirm:
>
> git status -sb
> git rev-parse HEAD
> git rev-parse origin/main
>
> Preserve the existing uncommitted Gate D firmware and diagnostics.
>
> Do not edit them before the repeatability run.
>
> PHASE 2 — PHONE READINESS
>
> Tell the user to:
>
> 1. open ENSv2 Access Demo
> 2. keep NFC enabled
> 3. keep Internet available
> 4. keep the Seeker unlocked
> 5. ensure Privy is authenticated
> 6. ensure the existing embedded Ethereum wallet is available
>
> Expected public wallet:
>
> 0x3419148731087b970d2059C53780163B452D5FF7
>
> Do not request OTP or any Privy configuration.
>
> PHASE 3 — RESET GATE D FIRMWARE
>
> Use the existing unchanged:
>
> firmware/pn532_hce_apdu/pn532_hce_apdu.ino
>
> Do not rebuild/change protocol constants.
>
> Reset/reboot the ESP32.
>
> Capture serial.
>
> Require:
>
> GATE_D_READY
> PRESENT_SEEKER
>
> Tell the user exactly:
>
> PRESENT THE SEEKER TO THE PN532 NOW AND HOLD IT STEADY UNTIL I TELL YOU TO REMOVE IT
>
> PHASE 4 — ONE COMPLETE PHYSICAL SESSION
>
> Run exactly one session.
>
> Require:
>
> TARGET ACTIVATION:
> PASS
>
> SELECT:
> PASS
>
> SEND_CHALLENGE:
> PASS
>
> After SEND_CHALLENGE:
> never resend it during this session.
>
> GET_STATUS:
> PROCESSING and/or READY
>
> READY:
> PASS
>
> GET_SIGNATURE:
> PASS
>
> Require:
>
> SIGNATURE_LEN=65
>
> Capture the strict:
>
> GATE_D_SIGNATURE=0x...
>
> directly from serial.
>
> Do not route the signature through ChatGPT.
>
> Validate locally:
>
> - ASCII hex only
> - 130 hex characters after 0x
> - 65 decoded bytes.
>
> PHASE 5 — NODE RECOVERY
>
> Use the transported signature with:
>
> node scripts/security/gate-c2-verify.mjs <wallet> <signature>
>
> Expected wallet:
>
> 0x3419148731087b970d2059C53780163B452D5FF7
>
> Require:
>
> RECOVERED SIGNER:
> 0x3419148731087b970d2059C53780163B452D5FF7
>
> MATCH:
> PASS
>
> No ENS reads.
> No blockchain writes.
>
> PHASE 6A — IF THE SESSION PASSES
>
> If every stage passes:
>
> classify:
>
> - successful physical sessions: 2
>   (the previous full diagnostic reproduction + this unchanged repeatability
>   session)
>
> - original GET_SIGNATURE failure:
>   TRANSIENT / NOT REPRODUCED
>
> Do NOT implement a speculative PN532 fix.
>
> Do NOT add chunking.
>
> Then perform cleanup.
>
> TEMPORARY ANDROID DIAGNOSTIC CLEANUP
>
> The Android HCE/transport telemetry was created only to isolate the physical
> failure.
>
> Prefer reverting all diagnostic-only Android changes back to the pushed Gate C2
> behavior unless a diagnostic element has clear continuing product/demo value.
>
> In particular remove/revert:
>
> - misleading dynamic-AID diagnostic label
> - temporary HCE transport telemetry UI/state
> - diagnostic-only instrumentation
>
> Do NOT revert or alter any committed Gate C1/C2 production functionality.
>
> After cleanup the intended uncommitted Gate D implementation should preferably
> be limited to:
>
> firmware/pn532_hce_apdu/pn532_hce_apdu.ino
>
> If some tiny non-diagnostic Android change is genuinely required for Gate D,
> explain it rather than keeping diagnostics by accident.
>
> FINAL VALIDATION AFTER CLEANUP
>
> Android:
>
> :app:testDebugUnitTest
> require:
> 21/21 PASS
>
> :app:assembleDebug
> require:
> PASS
>
> Node:
>
> node --test --test-isolation=none
>
> require:
> 83/83 PASS
>
> Also:
>
> - git diff --check
> - safe secret scan
> - no real signature stored/staged
> - local Privy config remains ignored
> - APDU v1 unchanged
> - firmware diff scoped
> - no blockchain writes
>
> Then return a complete Gate D PASS report.
>
> PHASE 6B — IF THE SESSION FAILS
>
> If any stage fails:
>
> STOP the physical run.
>
> Do not retry repeatedly.
>
> Identify exact stage:
>
> TARGET_ACTIVATION
> SELECT
> SEND_CHALLENGE
> STATUS
> GET_SIGNATURE
> SIGNATURE_LENGTH
> NODE_RECOVERY
>
> If GET_SIGNATURE / PN532 exchange fails again:
>
> use the existing Android telemetry to determine whether:
>
> - GET_SIGNATURE reached HostApduService
> - processor was READY
> - Android produced 67 bytes / 9000
>
> Then instrument ONLY the Gate-D PN532 receive path to expose the hidden
> inDataExchange/HAL failure.
>
> Preferred order:
>
> 1. existing library diagnostics if sufficient
> 2. Gate-D-only wrapper/helper
> 3. temporary diagnostic-only installed-library instrumentation as last resort
>
> If installed-library instrumentation is required:
>
> - record original file hashes
> - diagnostics only
> - do not change behavior
> - restore library byte-for-byte afterward
> - do not commit the library change
>
> Do not introduce:
>
> - chunking
> - APDU changes
> - library replacement
> - Gate E
> - ENS integration
>
> unless Control Tower explicitly approves after reviewing the evidence.
>
> RETURN IF PASS
>
> # GATE D PHYSICAL PN532 HCE TRANSPORT
>
> ## BASELINE
> ## SESSION 1 EVIDENCE
>
> Summarize the previous successful diagnostic physical session.
>
> ## SESSION 2
>
> Report:
>
> TARGET ACTIVATION
> SELECT
> SEND_CHALLENGE
> STATUS
> GET_SIGNATURE
> SIGNATURE LENGTH
> NODE RECOVERY
>
> ## REPEATABILITY
>
> State:
>
> 2/2 successful complete physical sessions
>
> and classify the original failed GET_SIGNATURE as:
>
> TRANSIENT / NOT REPRODUCED
>
> ## APDU V1
>
> Confirm unchanged.
>
> ## ANDROID DIAGNOSTIC CLEANUP
> ## FINAL ANDROID TESTS
> ## FINAL NODE TESTS
> ## SECURITY
> ## FILES CHANGED
> ## GIT STATE
> ## WHAT THIS PROVES
> ## NEXT TASK
>
> WHAT THIS PROVES:
>
> Gate D proves real physical ISO-DEP/APDU interoperability between the
> ESP32-S3 + Elechouse PN532 and the Seeker HostApduService, including transport
> of a real Privy-produced 65-byte Gate A-compatible EIP-712 signature and Node
> recovery of the same embedded wallet.
>
> It still does NOT prove ENS-based physical authorization.
>
> NEXT TASK:
>
> Gate E only.
>
> End exactly:
>
> GATE D: PASS
>
> RETURN IF FAIL
>
> Return the same relevant evidence and end:
>
> GATE D: STOP — <exact stage/reason>
>
> Do not commit.
> Do not push.
