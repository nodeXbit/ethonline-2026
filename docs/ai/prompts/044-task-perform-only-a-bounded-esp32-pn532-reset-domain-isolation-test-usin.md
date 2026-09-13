# Project task packet 044: TASK — Perform ONLY a bounded ESP32/PN532 reset-domain isolation test using the

- Classification: **ORIGINAL / SANITIZED**
- Redactions applied: none
- Source: retained project-specific Control Tower packet in the local Codex record

---

> TASK
>
> Perform ONLY a bounded ESP32/PN532 reset-domain isolation test using the
> committed Batch A Gate E firmware already flashed.
>
> Do NOT edit or flash firmware.
> Do NOT manipulate jumper wiring.
> Do NOT present the Seeker.
> Do NOT start Gate E bridge.
> Do NOT issue challenges.
> Do NOT perform blockchain writes.
> Do NOT change guest-001.
> Do NOT commit or push.
>
> WHY
>
> The early-startup-race hypothesis was rejected by authoritative local source:
>
> setup already provides approximately:
>
> 2 s sketch delay
> +
> PN532 begin
> +
> 500 ms PN532_I2C wakeup
>
> before the fatal 0x24 ACK probe.
>
> However the recurring NO ACK has appeared around flash/reset transitions.
>
> Before adding diagnostic firmware, distinguish:
>
> ESP32-only reset behavior
>
> from:
>
> complete ESP32 + PN532 power-cycle behavior.
>
> CURRENT STATE
>
> Repository expected clean:
>
> HEAD == origin/main ==
> 21041a26586bf615130708b5215c477d5c757aa5
>
> Committed Batch A firmware is already flashed.
>
> Current guest onchain state is expected ACTIVE, but must not be modified.
>
> Seeker stays away.
>
> ============================================================
> PHASE 1 — REPO SAFETY
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
> No source operations afterward.
>
> ============================================================
> PHASE 2 — FULL POWER CYCLE #1
> ============================================================
>
> The latest post-flash boot ended in:
>
> 0x24 NO ACK
>
> Do NOT press RST/EN first.
>
> Tell the user:
>
> 1. KEEP THE SEEKER AWAY.
> 2. Disconnect the single established CH343/COM4 USB connection completely.
> 3. Leave it disconnected for approximately 10 seconds.
> 4. Reconnect the SAME USB connector.
>
> Do not touch PN532 jumpers/wires.
>
> Identify the CH343 port again.
>
> Open/capture Serial0 at 115200.
>
> Observe the natural boot.
>
> Record:
>
> I2C 0x24:
> ACK / NO ACK
>
> PN532 firmware query:
> PASS / FAIL
>
> PN532 firmware:
> 1.6 / unavailable
>
> GATE_E_READY:
> YES / NO
>
> PRESENT_SEEKER:
> YES / NO
>
> If full power cycle #1 gives NO ACK:
>
> STOP.
>
> Do not continue to RST testing.
>
> This proves that ESP32-only reset is not sufficient to explain the problem.
>
> ============================================================
> PHASE 3 — ESP32 RST/EN RESET
> ============================================================
>
> Only if Full Power Cycle #1 passes:
>
> keep the phone away.
>
> Perform exactly ONE short RST/EN press.
>
> Do not press BOOT.
>
> Capture the next boot.
>
> Record the same fields.
>
> If this RST/EN boot fails while the full power-cycle boot passed:
>
> STOP after capturing it.
>
> Classify evidence as:
>
> RESET-DOMAIN SENSITIVE / ESP32-ONLY RESET SUSPECTED
>
> Do not fix anything yet.
>
> ============================================================
> PHASE 4 — FULL POWER CYCLE #2
> ============================================================
>
> Only if previous steps permit:
>
> disconnect the same USB power completely again for approximately 10 seconds.
>
> Reconnect.
>
> Capture one final natural boot.
>
> Record the same fields.
>
> No phone.
>
> ============================================================
> PHASE 5 — CLASSIFY
> ============================================================
>
> CASE A
>
> Full power cycle #1:
> PASS
>
> RST/EN:
> FAIL
>
> Full power cycle #2:
> PASS
>
> Classification:
>
> RESET-DOMAIN SENSITIVE
>
> The PN532 path is reliably recovered by complete power cycle but not by an
> ESP32-only reset.
>
> Do NOT implement hardware reset wiring yet.
>
> Recommended hackathon mitigation:
>
> full power-cycle before each controlled demo/run.
>
> CASE B
>
> All three:
> PASS
>
> Classification:
>
> TRANSIENT / NOT REPRODUCED
>
> Recommended operational precaution:
>
> use a full power-cycle before the important physical ACTIVE validation/demo.
>
> No firmware diagnostic required yet.
>
> CASE C
>
> Any full power cycle:
> NO ACK / firmware query FAIL
>
> Classification:
>
> PERSISTENT/INTERMITTENT HARDWARE-I2C ISSUE
>
> Then STOP.
>
> The next evidence-driven step may be timed I2C diagnostics or electrical power
> measurement, but do not perform it in this task.
>
> ============================================================
> FINAL SAFETY
> ============================================================
>
> Require:
>
> - zero firmware edits
> - zero flashes
> - zero NFC presentations
> - zero challenges
> - zero signatures
> - zero blockchain writes
> - guest ENS state untouched
> - git clean
>
> RETURN
>
> # PN532 RESET-DOMAIN ISOLATION
>
> ## REPO STATE
>
> ## FULL POWER CYCLE 1
>
> I2C 0x24:
> PN532 query:
> Firmware:
> GATE_E_READY:
> PRESENT_SEEKER:
>
> ## RST/EN BOOT
>
> Report same fields or NOT RUN.
>
> ## FULL POWER CYCLE 2
>
> Report same fields or NOT RUN.
>
> ## CLASSIFICATION
>
> Exactly one:
>
> RESET-DOMAIN SENSITIVE
> TRANSIENT / NOT REPRODUCED
> PERSISTENT/INTERMITTENT HARDWARE-I2C ISSUE
>
> ## OPERATIONAL MITIGATION
>
> ## SECURITY / STATE
>
> ## NEXT ACTION
>
> If all required full-power boots pass:
>
> next action is one controlled Gate E ACTIVE physical ALLOW attempt, beginning
> with a complete power cycle rather than RST/EN.
>
> If a full-power boot fails:
>
> next action is bounded hardware/I2C diagnosis; do not run Gate E.
>
> End:
>
> RESET DOMAIN: PASS
>
> or
>
> RESET DOMAIN: STOP — <reason>
