# Project task packet 053: TASK — Diagnose ONLY why the finalized Batch B preflight returned:

- Classification: **ORIGINAL / SANITIZED**
- Redactions applied: none
- Source: retained project-specific Control Tower packet in the local Codex record

---

> TASK
>
> Diagnose ONLY why the finalized Batch B preflight returned:
>
> HARDWARE READINESS: FAIL — MANUAL_COLD_BOOT_REQUIRED
>
> after a verified full cold boot and one RST/EN press.
>
> This is a READ-ONLY diagnosis first.
>
> DO NOT:
>
> - perform another cold boot
> - press RST/EN
> - present the Seeker
> - run Gate E bridge
> - perform blockchain writes
> - Activate/Deactivate
> - edit files until the source/runbook mismatch is proven
> - commit or push
> - flash firmware
>
> CURRENT STATE
>
> guest-001 remains:
>
> REGISTERED
> active=false
> validUntil=1793487599
> policy=DENY
>
> DEV nonce:
> 23/23
>
> Confirmed rehearsal blockchain writes:
> 1
>
> Physical taps:
> 0
>
> Repository expected:
>
> HEAD == origin/main ==
> 1b1317e65106bb173d1eedc19ae32fcc34513651
>
> working tree clean.
>
> ============================================================
> PHASE 1 — INSPECT EXACT PREFLIGHT SEMANTICS
> ============================================================
>
> Read completely:
>
> demo/preflight.mjs
> demo/serial-readiness.mjs
> DEMO_RUNBOOK.md
> package.json
>
> and any directly imported helper necessary to understand the flow.
>
> Do not inspect secret config contents.
>
> Trace exactly what happens for:
>
> npm run demo:preflight -- --observe-boot
>
> Answer from source:
>
> 1. At what point does it open the CH343 serial port?
> 2. Does it listen only for serial lines emitted AFTER the port is opened?
> 3. Can it recover firmware readiness lines that were emitted before it opened
>    COM?
> 4. What exact lines/states count as readiness success?
> 5. What timeout does it use?
> 6. What exact condition produces MANUAL_COLD_BOOT_REQUIRED?
> 7. Does --observe-boot itself instruct or expect the operator to reset/cold-boot
>    WHILE the observer is running?
> 8. Does DEMO_RUNBOOK.md currently tell the operator to cold boot BEFORE starting
>    preflight?
> 9. If so, is that ordering incompatible with the implementation?
> 10. Was the failed rehearsal sequence consistent with the current runbook, or
>     was the runbook executed incorrectly?
>
> ============================================================
> PHASE 2 — CLASSIFY
> ============================================================
>
> Choose exactly one.
>
> CASE A — WORKFLOW/RUNBOOK ORDERING DEFECT
>
> Use this only if source proves that:
>
> - observe-boot must already own/listen to COM when the boot/reset occurs;
> - previously emitted readiness lines cannot be recovered;
> - but the committed runbook tells the operator to complete cold boot/reset
>   before launching the observer, or otherwise makes the order ambiguous.
>
> Classification:
>
> BATCH B OPERABILITY DEFECT:
> BOOT OBSERVER ORDERING
>
> This is a real rehearsal blocker.
>
> Then design the smallest fix.
>
> Preferred operational sequence should be approximately:
>
> 1. Seeker away.
> 2. Complete power removal.
> 3. Verify CH343 disappears.
> 4. Wait ~10 s.
> 5. Start `demo:preflight -- --observe-boot` in a state that waits for CH343 /
>    serial readiness if the implementation supports this safely.
> 6. Reconnect CH343.
> 7. Once observer has COM open, perform one RST/EN if required to emit boot
>    telemetry.
> 8. Observer captures:
>    I2C 0x24 ACK
>    PN532 1.6
>    GATE_E_READY
>    PRESENT_SEEKER
> 9. Continue RPC/onchain preflight.
> 10. Release COM before bridge.
>
> BUT do not assume this exact sequence if current implementation cannot observe
> device enumeration/reconnection that way.
>
> Derive the minimal feasible sequence from actual code.
>
> If the implementation itself needs a tiny change to make this deterministic,
> propose that exact change.
>
> Do not implement yet in this task.
>
> CASE B — RUNBOOK AND IMPLEMENTATION ALREADY MATCH
>
> If the committed runbook already clearly requires starting the observer before
> the boot telemetry and the failed rehearsal simply executed it in the wrong
> order:
>
> Classification:
>
> REHEARSAL PROCEDURE ERROR
>
> State the exact correct sequence.
>
> Do not edit anything.
>
> CASE C — OBSERVER WAS ACTIVE AND STILL MISSED READINESS
>
> If evidence/source shows the observer was definitely listening before the
> RST/EN boot and nevertheless timed out:
>
> Classification:
>
> READINESS OBSERVATION FAILURE
>
> Identify the smallest next diagnostic boundary:
>
> - serial open timing
> - expected line mismatch
> - timeout
> - buffering
> - or actual firmware readiness failure
>
> Do not execute it yet.
>
> ============================================================
> PHASE 3 — CHECK WHETHER THIS INVALIDATES BATCH B CHECKPOINT
> ============================================================
>
> State explicitly:
>
> BATCH B CHECKPOINT:
> STILL VALID / REQUIRES SMALL FOLLOW-UP FIX
>
> If CASE A, expected answer:
>
> REQUIRES SMALL FOLLOW-UP FIX
>
> Do not call for redesign.
>
> ============================================================
> RETURN
> ============================================================
>
> # PREFLIGHT BOOT-OBSERVATION DIAGNOSIS
>
> ## SOURCE TRACE
>
> Exact sequence for:
>
> npm run demo:preflight -- --observe-boot
>
> ## READINESS SUCCESS CONDITION
>
> ## MANUAL_COLD_BOOT_REQUIRED CONDITION
>
> ## CURRENT RUNBOOK ORDER
>
> ## FAILED REHEARSAL ORDER
>
> ## CLASSIFICATION
>
> Exactly one:
>
> BATCH B OPERABILITY DEFECT: BOOT OBSERVER ORDERING
>
> REHEARSAL PROCEDURE ERROR
>
> READINESS OBSERVATION FAILURE
>
> ## ROOT CAUSE CONFIDENCE
>
> HIGH / MEDIUM / LOW
>
> Explain briefly.
>
> ## MINIMAL CORRECTION
>
> Do not implement.
>
> ## BATCH B CHECKPOINT
>
> STILL VALID
>
> or
>
> REQUIRES SMALL FOLLOW-UP FIX
>
> ## CURRENT REHEARSAL STATE
>
> Confirm:
>
> guest remains INACTIVE/DENY
> validUntil=1793487599
> nonce=23/23
> writes=1
> taps=0
>
> ## NEXT ACTION
>
> Give exactly the smallest next action.
>
> End:
>
> PREFLIGHT DIAGNOSIS: PASS
