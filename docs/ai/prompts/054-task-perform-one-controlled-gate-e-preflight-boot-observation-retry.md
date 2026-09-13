# Project task packet 054: TASK — Perform ONE controlled Gate E preflight boot-observation retry.

- Classification: **ORIGINAL / SANITIZED**
- Redactions applied: none
- Source: retained project-specific Control Tower packet in the local Codex record

---

> TASK
>
> Perform ONE controlled Gate E preflight boot-observation retry.
>
> Purpose:
>
> Determine whether the previous MANUAL_COLD_BOOT_REQUIRED result was caused by
> RST/EN occurring outside or too late in the active serial observation window.
>
> This is NOT an NFC attempt.
>
> DO NOT:
>
> - perform another full cold boot
> - disconnect USB/power
> - perform blockchain writes
> - activate/deactivate guest
> - present the Seeker
> - start Gate E bridge
> - edit files
> - flash firmware
> - commit or push
> - use Computer Use
> - press BOOT
> - perform more than ONE RST/EN reset
>
> CURRENT AUTHORITATIVE STATE
>
> guest-001.demo-access.eth:
>
> REGISTERED
> active=false
> validUntil=1793487599
> policy=DENY
>
> DEV nonce:
> 23/23
>
> Confirmed rehearsal writes:
> 1
>
> Physical taps:
> 0
>
> Repository expected clean at:
>
> 1b1317e65106bb173d1eedc19ae32fcc34513651
>
> ============================================================
> PHASE 1 — SAFETY
> ============================================================
>
> Read-only confirm:
>
> git status -sb
> git rev-parse HEAD
> git rev-parse origin/main
>
> Require clean/synchronized.
>
> Read-only require:
>
> DEV latest == pending == 23
> guest still INACTIVE
> validUntil == 1793487599
> policy DENY
>
> KEEP THE SEEKER AWAY FROM THE PN532.
>
> Ensure no Serial Monitor, Gate E bridge or other process owns the CH343 port.
>
> Do not start another demo server if the existing HTTP server is still running;
> the HTTP server may remain running because it does not need to own COM4.
>
> ============================================================
> PHASE 2 — PREPARE OPERATOR
> ============================================================
>
> Before starting the preflight, tell the user:
>
> GET READY TO PRESS RST/EN ONCE, BUT DO NOT PRESS IT YET.
>
> Explain:
>
> - after the command starts, wait for my explicit instruction;
> - press RST/EN only once;
> - do not press BOOT;
> - keep the Seeker away.
>
> ============================================================
> PHASE 3 — START OBSERVER
> ============================================================
>
> Start exactly:
>
> npm run demo:preflight -- --observe-boot
>
> Do not restart the command.
>
> The source establishes:
>
> - CH343 enumeration occurs first;
> - serial open timeout is 5 seconds;
> - once opened, readiness observation lasts 30 seconds.
>
> After the command has remained alive for approximately 6–8 seconds from launch,
> it is safely beyond the five-second serial-open timeout.
>
> At that point, and still well within the 30-second observation window, tell the
> user exactly:
>
> PRESS RST/EN ONCE NOW.
>
> The user performs one short RST/EN press.
>
> Do not request another reset.
>
> ============================================================
> PHASE 4 — OBSERVE RESULT
> ============================================================
>
> Let the SAME preflight invocation finish.
>
> Require the observer to receive, in order:
>
> PN532_FIRMWARE=1.6
> GATE_E_READY
> PRESENT_SEEKER
>
> Benign surrounding lines are acceptable.
>
> If it receives:
>
> GATE_E: STOP ...
>
> capture the public stage/reason.
>
> If it returns MANUAL_COLD_BOOT_REQUIRED again:
>
> do not retry.
>
> If the existing implementation exposes any sanitized observed serial lines or
> serial-readiness reason, record them.
>
> Do not modify code merely to obtain more logging in this task.
>
> ============================================================
> PHASE 5 — PREFLIGHT RESULT
> ============================================================
>
> If readiness succeeds, allow the same preflight to complete its existing
> read-only RPC/onchain checks.
>
> Require:
>
> canonical guest config PASS
> Sepolia 11155111 PASS
> fresh block PASS
> guest REGISTERED
> expected owner/resolver
> validUntil >24h
> policy INACTIVE accepted
> DEV latest == pending == 23
> no pending transaction
>
> Require:
>
> DEMO PREFLIGHT: PASS
>
> Zero challenge issuance.
> Zero blockchain writes.
>
> ============================================================
> PHASE 6 — CLASSIFY
> ============================================================
>
> CASE A
>
> The controlled reset produces:
>
> PN532_FIRMWARE=1.6
> GATE_E_READY
> PRESENT_SEEKER
>
> and:
>
> DEMO PREFLIGHT: PASS
>
> Classification:
>
> BOOT OBSERVATION: PASS
>
> Operational conclusion:
>
> For demo sessions, start `demo:preflight -- --observe-boot` first and perform
> one RST/EN during its active observation window when boot telemetry needs to be
> captured.
>
> No source fix required yet.
>
> CASE B
>
> The reset produces an explicit:
>
> GATE_E: STOP - ...
>
> Classification:
>
> FIRMWARE READINESS FAILURE
>
> Record exact stage/reason.
>
> Do not retry.
>
> CASE C
>
> The command again returns:
>
> MANUAL_COLD_BOOT_REQUIRED
>
> despite the reset being deliberately performed 6–8 seconds after launch.
>
> Classification:
>
> SERIAL OBSERVATION FAILURE REPRODUCED
>
> This is sufficient evidence for a small follow-up diagnostic/instrumentation
> task.
>
> Do not edit in this task.
>
> ============================================================
> PHASE 7 — SAFETY
> ============================================================
>
> Require:
>
> new blockchain writes:
> 0
>
> physical taps:
> 0
>
> challenges:
> 0
>
> signatures:
> 0
>
> RST/EN presses:
> exactly 1
>
> source changes:
> 0
>
> guest remains INACTIVE/DENY
> nonce remains 23/23
> git clean
>
> ============================================================
> RETURN
> ============================================================
>
> # CONTROLLED BOOT OBSERVATION
>
> ## BASELINE
>
> ## OBSERVER START
>
> Record approximate elapsed time before reset.
>
> ## RST/EN
>
> Count:
> 1
>
> Approximate timing after command launch:
>
> ## SERIAL RESULT
>
> Report whether each was observed:
>
> PN532_FIRMWARE=1.6:
> YES / NO
>
> GATE_E_READY:
> YES / NO
>
> PRESENT_SEEKER:
> YES / NO
>
> GATE_E STOP:
> NONE / <stage and reason>
>
> ## PREFLIGHT
>
> DEMO PREFLIGHT:
> PASS / STOP
>
> Reason:
>
> ## CLASSIFICATION
>
> Exactly one:
>
> BOOT OBSERVATION: PASS
>
> FIRMWARE READINESS FAILURE
>
> SERIAL OBSERVATION FAILURE REPRODUCED
>
> ## REHEARSAL STATE
>
> guest:
> INACTIVE / DENY
>
> validUntil:
> 1793487599
>
> nonce:
> 23/23
>
> writes:
> 1
>
> taps:
> 0
>
> ## SECURITY / GIT
>
> ## NEXT
>
> If BOOT OBSERVATION PASS:
>
> Resume TAP 1 immediately using this freshly validated hardware state. Do not
> cold boot again.
>
> If SERIAL OBSERVATION FAILURE REPRODUCED:
>
> Return to Control Tower for the smallest serial-observer instrumentation fix.
>
> If FIRMWARE READINESS FAILURE:
>
> Return exact STOP evidence.
>
> End exactly:
>
> CONTROLLED BOOT: PASS
>
> or
>
> CONTROLLED BOOT: STOP — <exact reason>
