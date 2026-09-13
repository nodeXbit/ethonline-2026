# Project task packet 043: TASK — Diagnose and, ONLY IF CONFIRMED BY SOURCE, harden the recurring PN532

- Classification: **ORIGINAL / SANITIZED**
- Redactions applied: none
- Source: retained project-specific Control Tower packet in the local Codex record

---

> TASK
>
> Diagnose and, ONLY IF CONFIRMED BY SOURCE, harden the recurring PN532
> initialization race seen immediately after ESP32 flash/reset.
>
> This is a firmware-initialization task only.
>
> DO NOT:
>
> - run Gate E NFC
> - present the Seeker
> - perform blockchain writes
> - change ENS state
> - change APDU v1
> - change PN532 library files
> - change Android
> - change Node Gate E semantics
> - change pins/interface
> - replace cables
> - switch I2C/SPI
> - commit or push
>
> CURRENT REPOSITORY
>
> Expected:
>
> HEAD == origin/main ==
> 21041a26586bf615130708b5215c477d5c757aa5
>
> Working tree clean.
>
> CURRENT FAILURE
>
> The committed Batch A firmware compiled/flashed successfully to ESP32-S3 via
> CH343 / COM4.
>
> Immediately after upload/reset:
>
> I2C 0x24: NO ACK
> PN532 firmware query: FAIL
> GATE_E_READY: NO
>
> Firmware correctly emitted:
>
> GATE_E: STOP - INITIALIZATION: PN532 did not ACK at I2C address 0x24
> AUTHORIZATION: DENY
>
> Zero NFC and zero blockchain writes.
>
> HISTORICAL EVIDENCE
>
> The exact ESP32/PN532 hardware and wiring has previously completed:
>
> - PN532 firmware 1.6 detection
> - Gate D physical proof transport 2/2
> - Gate E physical INACTIVE proof
> - later normal boot with:
>   0x24 ACK
>   firmware 1.6
>   GATE_E_READY
>   PRESENT_SEEKER
>
> Therefore do NOT assume permanent wiring failure.
>
> EXTERNAL REFERENCE TO VERIFY AGAINST LOCAL SOURCE
>
> The common Elechouse PN532_I2C implementation has:
>
> PN532_I2C::wakeup()
> {
>     delay(500);
> }
>
> with the comment that this waits until the PN532 is ready.
>
> The Elechouse examples/documentation use library initialization before firmware
> query.
>
> LOCAL SOURCE IS AUTHORITATIVE.
>
> ============================================================
> PHASE 1 — INSPECT EXACT CURRENT INITIALIZATION
> ============================================================
>
> Read completely:
>
> firmware/pn532_secure_access/pn532_secure_access.ino
>
> and the exact installed local:
>
> PN532.h
> PN532.cpp
> PN532_I2C.h
> PN532_I2C.cpp
>
> Do NOT modify library files.
>
> Trace exact order from setup() through:
>
> - Wire.begin
> - custom/raw I2C 0x24 ACK probe
> - PN532_I2C begin/wakeup
> - PN532 begin
> - getFirmwareVersion
> - SAM/RF initialization
>
> Determine:
>
> 1. whether the raw 0x24 probe occurs BEFORE any library readiness/wakeup delay;
> 2. whether local PN532_I2C::wakeup() actually delays approximately 500 ms;
> 3. whether nfc.begin() invokes interface begin/wakeup, and in what order;
> 4. whether the current raw probe can fail during a legitimate PN532 startup
>    window and terminate before the library gets its normal readiness handling;
> 5. whether flash/reset can reset/power-cycle ESP32 and PN532 differently enough
>    to make this timing observable;
> 6. whether any existing delay/readiness mechanism is already present and simply
>    misplaced.
>
> Also compare Gate D initialization.
>
> Do not speculate where source can answer.
>
> ============================================================
> PHASE 2 — DECISION
> ============================================================
>
> CASE A — STARTUP RACE IS SUPPORTED
>
> If current firmware performs its fatal raw 0x24 ACK check before the established
> PN532 readiness/wakeup period, classify:
>
> ROOT CAUSE HYPOTHESIS:
> EARLY_FATAL_I2C_PROBE / PN532 STARTUP READINESS RACE
>
> Then implement the SMALLEST robust fix.
>
> Preferred shape:
>
> - preserve the raw ACK diagnostic;
> - do not make the first immediate NO ACK fatal;
> - allow PN532 startup stabilization first;
> - then perform a small bounded readiness sequence.
>
> Prefer one of these, based on actual library semantics:
>
> A. library begin/wakeup first, then diagnostic ACK check;
>
> or
>
> B. explicit bounded startup delay/readiness probe before fatal ACK decision.
>
> Do NOT stack redundant delays if the library already supplies the correct one.
>
> If bounded probing is useful, use a small deterministic policy such as:
>
> - total startup allowance roughly <= 1 second;
> - only a few attempts;
> - short spacing;
> - PASS on first valid ACK;
> - after budget expires:
>   existing GATE_E_STOP INITIALIZATION + DENY.
>
> Do not retry indefinitely.
>
> Do not hide a genuinely disconnected PN532.
>
> CASE B — SOURCE DOES NOT SUPPORT THIS
>
> If the fatal raw probe already occurs AFTER adequate startup/wakeup handling:
>
> DO NOT EDIT.
>
> Return the exact initialization ordering and STOP for a different hardware-level
> diagnosis.
>
> ============================================================
> PHASE 3 — TESTABILITY / TELEMETRY
> ============================================================
>
> If CASE A is implemented:
>
> Keep public serial output concise but make boot diagnosis observable.
>
> It should distinguish:
>
> PN532 readiness recovered after bounded startup wait
>
> from:
>
> PN532 never ACKed within startup budget
>
> Do not expose secrets.
>
> Do not add a complex state machine.
>
> ============================================================
> PHASE 4 — VALIDATION
> ============================================================
>
> If firmware changed:
>
> 1. compile Gate E firmware;
> 2. confirm Gate D firmware unchanged;
> 3. git diff --check;
> 4. secret scan;
> 5. no APDU changes;
> 6. no Node/Android changes.
>
> Do NOT run NFC.
>
> Then perform a bounded hardware validation specifically for boot reliability:
>
> Flash the updated Gate E firmware ONCE.
>
> After flash, observe the automatic post-upload boot.
>
> Require:
>
> I2C 0x24: ACK
> PN532 firmware query: PASS
> PN532 firmware: 1.6
> GATE_E_READY
> PRESENT_SEEKER
>
> Keep Seeker away.
>
> If post-flash boot passes, then perform TWO additional ordinary RST/EN resets,
> one at a time, observing boot only.
>
> Require all three total boot observations:
>
> 1. post-flash boot
> 2. RST/EN boot #1
> 3. RST/EN boot #2
>
> to reach:
>
> PN532 1.6
> GATE_E_READY
> PRESENT_SEEKER
>
> No NFC interaction.
>
> If any one fails:
> STOP with exact stage.
>
> Do not keep resetting until success.
>
> ============================================================
> SECURITY / SCOPE
> ============================================================
>
> Require:
>
> - zero blockchain writes
> - zero NFC presentations
> - zero challenges/signatures
> - no library file modifications
> - no APDU change
> - no pin/interface change
> - no cable manipulation required
> - repository modifications limited to Gate E firmware only if CASE A justified
>
> ============================================================
> RETURN
> ============================================================
>
> # PN532 STARTUP HARDENING
>
> ## SOURCE TRACE
>
> Show exact initialization order.
>
> ## LOCAL LIBRARY READINESS
>
> State whether local PN532_I2C wakeup has a delay and whether current firmware
> reaches it before the fatal probe.
>
> ## ROOT CAUSE ASSESSMENT
>
> CONFIRMED / SUPPORTED / NOT SUPPORTED
>
> Do not overclaim certainty.
>
> ## CHANGE
>
> If any.
>
> ## BOOT POLICY
>
> Include bounded timing/retry behavior.
>
> ## BUILD
>
> ## POST-FLASH BOOT
>
> ## RESET BOOT 1
>
> ## RESET BOOT 2
>
> ## RELIABILITY RESULT
>
> Require 3/3 only if fix was implemented.
>
> ## SECURITY
>
> ## GIT STATE
>
> No commit/push.
>
> ## NEXT ACTION
>
> If 3/3 passes:
>
> Checkpoint the scoped startup hardening, then perform one controlled Gate E
> ACTIVE physical ALLOW attempt.
>
> If source does not support the timing hypothesis:
>
> return the next smallest evidence-driven diagnostic, but do not execute it.
>
> End exactly:
>
> PN532 STARTUP: PASS
>
> or
>
> PN532 STARTUP: STOP — <exact reason>
