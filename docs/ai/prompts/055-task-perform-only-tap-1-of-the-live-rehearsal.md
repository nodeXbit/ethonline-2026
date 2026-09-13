# Project task packet 055: TASK — Perform ONLY TAP 1 of the live rehearsal:

- Classification: **ORIGINAL / SANITIZED**
- Redactions applied: none
- Source: retained project-specific Control Tower packet in the local Codex record

---

> TASK
>
> Perform ONLY TAP 1 of the live rehearsal:
>
> VALID HOLDER + INACTIVE POLICY -> PHYSICAL CONFIRMED DENY.
>
> A successful controlled boot observation has JUST completed.
>
> DO NOT cold boot again.
> DO NOT press RST/EN again.
> DO NOT press BOOT.
> DO NOT flash firmware.
>
> CURRENT VERIFIED HARDWARE STATE
>
> The immediately preceding:
>
> npm run demo:preflight -- --observe-boot
>
> completed successfully after one controlled RST/EN press.
>
> Observed:
>
> PN532_FIRMWARE=1.6
> GATE_E_READY
> PRESENT_SEEKER
>
> DEMO PREFLIGHT:
> PASS
>
> COM4 was released cleanly afterward.
>
> Use this freshly validated hardware state directly.
>
> CURRENT AUTHORITATIVE REHEARSAL STATE
>
> guest-001.demo-access.eth
>
> REGISTERED
> owner:
> 0x3419148731087b970d2059C53780163B452D5FF7
>
> resolver:
> 0x0B723c0C2170F2ea508F2f4e3e122C6c0782744C
>
> access.active:
> false
>
> access.validUntil:
> 1793487599
>
> policy:
> DENY
>
> DEV nonce:
> 23/23
>
> Confirmed blockchain writes in rehearsal:
> 1
>
> That write was the already-confirmed deactivation.
>
> Physical taps so far:
> 0
>
> Repository:
>
> HEAD == origin/main ==
> 1b1317e65106bb173d1eedc19ae32fcc34513651
>
> working tree clean.
>
> THIS TASK AUTHORIZES:
>
> - ZERO blockchain writes
> - exactly ONE fresh physical NFC presentation/signature attempt
>
> DO NOT:
>
> - Activate
> - Deactivate
> - perform any blockchain mutation
> - edit files
> - commit/push
> - use Computer Use
> - run a second tap
> - retry after failure
> - issue another challenge after failure
>
> ============================================================
> PHASE 1 — FRESH READ-ONLY CHECK
> ============================================================
>
> Do only the minimum fresh verification before opening the bridge:
>
> - git clean/synchronized
> - Sepolia 11155111
> - guest REGISTERED
> - expected owner/resolver
> - access.active == false
> - access.validUntil == 1793487599
> - policy == DENY
> - DEV latest == pending == 23
> - no pending transaction
>
> Do NOT rerun boot observation.
>
> Do NOT reset hardware.
>
> Keep Seeker away.
>
> ============================================================
> PHASE 2 — SERIAL OWNERSHIP
> ============================================================
>
> Confirm the completed preflight released COM4.
>
> Ensure no Serial Monitor or other process owns CH343.
>
> Do not start another demo server if the existing one is already running.
>
> Start exactly:
>
> npm run demo:bridge
>
> Use the committed Batch B command.
>
> Do NOT enable replay checking for TAP 1 unless the committed bridge requires it.
>
> Require the bridge successfully opens the established CH343 device.
>
> Only when bridge state is ready tell the user exactly:
>
> PRESENT THE SEEKER NOW AND HOLD IT STEADY UNTIL THE RESULT IS CONFIRMED.
>
> ============================================================
> PHASE 3 — ONE PHYSICAL TAP
> ============================================================
>
> Perform exactly ONE fresh physical presentation.
>
> Require:
>
> TARGET_ACTIVATION:
> PASS
>
> SELECT:
> PASS
>
> WAITING_CHALLENGE:
> YES
>
> Fresh challenges:
> 1
>
> Credential:
> guest-001.demo-access.eth
>
> Resource:
> demo-access.eth:door-001
>
> Challenge payload:
> 104 bytes
>
> SEND_CHALLENGE:
> PASS
>
> SEND_CHALLENGE count:
> 1
>
> STATUS:
> PROCESSING -> READY
> or valid direct READY equivalent
>
> GET_SIGNATURE:
> PASS
>
> Proof length:
> 65 bytes
>
> Proof log:
> REDACTED
>
> If any transport stage fails:
>
> STOP immediately.
>
> No second tap.
>
> ============================================================
> PHASE 4 — HOLDER PROOF
> ============================================================
>
> Require:
>
> signature:
> VALID
>
> Recovered signer:
>
> 0x3419148731087b970d2059C53780163B452D5FF7
>
> Current ENS owner:
>
> 0x3419148731087b970d2059C53780163B452D5FF7
>
> OWNER MATCH:
> PASS
>
> Challenge consumed:
> YES
>
> The holder must be valid before policy evaluation denies access.
>
> ============================================================
> PHASE 5 — ENS POLICY DENY
> ============================================================
>
> Require fresh coherent ENS snapshot:
>
> chain:
> 11155111
>
> status:
> REGISTERED
>
> owner:
> expected
>
> resolver:
> expected
>
> registry expiry:
> valid
>
> access.active:
> false
>
> access.validUntil:
> 1793487599
>
> freshness:
> PASS
>
> Require:
>
> VERIFIER_DENY
>
> Reason:
>
> POLICY INACTIVE
>
> or exact committed equivalent.
>
> This MUST NOT be classified as:
>
> TRANSPORT_FAILURE
>
> ============================================================
> PHASE 6 — CONTROLLER CONFIRMATION
> ============================================================
>
> Require Node sends exactly once:
>
> AUTHORIZATION=DENY
>
> Authorization command writes:
> 1
>
> Then require firmware subsequently emits:
>
> AUTHORIZATION: DENY
>
> Require:
>
> confirmation match:
> PASS
>
> controller:
> CONTROLLER_CONFIRMED
>
> serial closed after matching confirmation:
> PASS
>
> Node DENY alone is not enough.
>
> The firmware confirmation must be observed.
>
> ============================================================
> PHASE 7 — SANITIZED DEMO EVIDENCE
> ============================================================
>
> After bridge completion, use the existing read-only runtime/demo evidence path.
>
> No browser automation.
>
> Require the demo evidence represents:
>
> ENSv2 POLICY:
> INACTIVE / DENY
>
> HOLDER VERIFIER:
> DENY
>
> Reason:
> policy inactive / equivalent
>
> PHYSICAL CONTROLLER:
> CONFIRMED DENY
>
> It must NOT report:
>
> TRANSPORT FAILURE
>
> No proof/signature bytes may appear.
>
> ============================================================
> PHASE 8 — FINAL READ-ONLY STATE
> ============================================================
>
> Require:
>
> guest remains REGISTERED
>
> access.active:
> false
>
> access.validUntil:
> 1793487599
>
> policy:
> DENY
>
> DEV:
> 23/23
>
> no pending transaction
>
> Blockchain writes during this task:
> 0
>
> Total confirmed rehearsal blockchain writes:
> 1
>
> Physical taps:
> 1
>
> Fresh challenges:
> 1
>
> Fresh signatures:
> 1
>
> Git remains clean/synchronized.
>
> ============================================================
> STOP
> ============================================================
>
> STOP immediately after TAP 1 is fully verified.
>
> DO NOT:
>
> - activate guest
> - perform TAP 2
> - replay proof
> - reset hardware
> - cold boot
> - edit docs/code
> - commit/push
>
> On any failure:
> STOP.
> No retry.
>
> ============================================================
> RETURN
> ============================================================
>
> # REHEARSAL TAP 1 — INACTIVE DENY
>
> ## FRESH BASELINE
>
> ## SERIAL / BRIDGE
>
> ## PHYSICAL TRANSPORT
>
> TARGET_ACTIVATION:
> SELECT:
> WAITING_CHALLENGE:
> Challenges:
> SEND_CHALLENGE:
> STATUS:
> GET_SIGNATURE:
> Proof length:
> Redaction:
>
> ## HOLDER PROOF
>
> Recovered signer:
> Current ENS owner:
> Owner match:
> Challenge consumed:
>
> ## ENS POLICY
>
> access.active:
> access.validUntil:
> snapshot freshness:
> verifier:
> reason:
>
> Require:
>
> VERIFIER_DENY
> POLICY INACTIVE
>
> ## PHYSICAL CONTROLLER
>
> Node command:
> AUTHORIZATION=DENY
>
> Authorization writes:
> 1
>
> Firmware line:
> AUTHORIZATION: DENY
>
> Confirmation match:
> PASS
>
> Controller:
> CONTROLLER_CONFIRMED
>
> ## DEMO EVIDENCE
>
> Policy:
> Holder verifier:
> Physical controller:
>
> ## COUNTS
>
> Total rehearsal blockchain writes:
> 1
>
> New blockchain writes:
> 0
>
> Physical taps:
> 1
>
> Fresh challenges:
> 1
>
> Fresh signatures:
> 1
>
> ## FINAL ONCHAIN STATE
>
> ## NONCE
>
> Require:
> 23/23
>
> ## SECURITY / GIT
>
> ## NEXT
>
> If PASS:
>
> Return to Control Tower to separately authorize the single activation
> transaction before TAP 2.
>
> End exactly:
>
> TAP 1 INACTIVE: PASS
>
> or
>
> TAP 1 INACTIVE: STOP — <exact reason>
