# Project task packet 045: TASK — Perform ONE controlled physical Gate E ACTIVE end-to-end validation using the

- Classification: **ORIGINAL / SANITIZED**
- Redactions applied: none
- Source: retained project-specific Control Tower packet in the local Codex record

---

> TASK
>
> Perform ONE controlled physical Gate E ACTIVE end-to-end validation using the
> committed Batch A system.
>
> This is a VALIDATION task.
>
> DO NOT edit code.
> DO NOT perform blockchain writes.
> DO NOT activate/deactivate/renew/register anything.
> DO NOT commit or push.
> DO NOT perform more than ONE fresh physical signing attempt.
>
> ZERO blockchain writes are authorized.
>
> ============================================================
> CURRENT VERIFIED BASELINE
> ============================================================
>
> Repository:
>
> HEAD == origin/main ==
> 21041a26586bf615130708b5215c477d5c757aa5
>
> Working tree:
> clean
>
> Batch A committed and pushed.
>
> Current guest credential:
>
> guest-001.demo-access.eth
>
> Owner:
>
> 0x3419148731087b970d2059C53780163B452D5FF7
>
> Resolver:
>
> 0x0B723c0C2170F2ea508F2f4e3e122C6c0782744C
>
> Expected access:
>
> active=true
> validUntil=1793487599
>
> Validity:
>
> 2026-10-31 23:59:59 Europe/Madrid
>
> Latest known ENS policy:
>
> ALLOW
>
> Latest known DEV nonce:
>
> 22 / 22
>
> Activation transaction:
>
> 0xafd5cb1c2ebe849ff65ed934ab2fb9da5eb1fe2df40370c3b1d43b25b97fa506
>
> Batch A firmware has been physically flashed.
>
> Latest verified cold boot:
>
> CH343 disappeared during full power loss: YES
> CH343 reconnected: COM4
> I2C 0x24: ACK
> PN532 query: PASS
> PN532 firmware: 1.6
> GATE_E_READY: YES
> PRESENT_SEEKER: YES
>
> Operational rule:
>
> FULL COLD BOOT BEFORE PHYSICAL GATE E SESSION.
>
> If the board is still powered, untouched, and waiting from that exact verified
> cold boot, that current ready state may be used.
>
> Do NOT reset/power-cycle merely for ritual.
>
> If that known-ready state has been lost or is uncertain, perform exactly one
> fresh full cold boot according to the established runbook before proceeding.
>
> ============================================================
> PHASE 1 — READ-ONLY STATE RECONSTRUCTION
> ============================================================
>
> Run:
>
> git status -sb
> git rev-parse HEAD
> git rev-parse origin/main
>
> Require:
>
> clean
> HEAD == origin/main ==
> 21041a26586bf615130708b5215c477d5c757aa5
>
> Read-only Sepolia preflight:
>
> - chain ID == 11155111
> - DEV latest == pending
> - no pending transaction
> - guest-001 REGISTERED
> - owner ==
>   0x3419148731087b970d2059C53780163B452D5FF7
> - expected resolver
> - registry expiry valid
> - access.active == true
> - access.validUntil == 1793487599
> - current policy authorization == ALLOW
> - Batch A snapshot freshness PASS
> - cred-001 identity intact
>
> Record fresh pre-run nonce.
>
> If guest is not ACTIVE/ALLOW:
> STOP.
>
> Do NOT repair it.
>
> ============================================================
> PHASE 2 — HARDWARE READINESS
> ============================================================
>
> KEEP THE SEEKER AWAY FROM THE PN532.
>
> Use:
>
> USB-Enhanced-SERIAL CH343
> historical port COM4
> Serial0 115200
>
> Identify by CH343 device, not COM number alone.
>
> If the previously verified cold-booted firmware is still running in its clean
> PRESENT_SEEKER state, preserve it.
>
> Otherwise perform ONE established cold boot:
>
> - remove all ESP32 power/USB
> - verify CH343 disappears
> - wait approximately 10 seconds
> - reconnect only established CH343 USB
> - use one short RST/EN press only if natural boot output is absent
>
> Require before phone presentation:
>
> I2C 0x24: ACK
> PN532 firmware query: PASS
> PN532 firmware: 1.6
> GATE_E_READY
> PRESENT_SEEKER
>
> No GATE_E_STOP.
>
> If initialization fails:
> STOP.
>
> Do not retry hardware repeatedly.
>
> ============================================================
> PHASE 3 — SERIAL OWNERSHIP
> ============================================================
>
> Before launching the Node bridge:
>
> Close any Serial Monitor/passive COM4 capture so that exactly ONE process owns
> the serial port.
>
> Confirm no other terminal/Arduino monitor has COM4 open.
>
> Start the committed Gate E bridge using the actual CLI syntax with replay
> checking enabled.
>
> Use the existing read-only RPC configuration.
>
> Do not print RPC URLs.
>
> Fallback RPC is optional; absence of fallback is acceptable for this run if
> the primary provider is healthy.
>
> If bridge startup/open fails:
> STOP.
>
> Do not alter drivers or software.
>
> ============================================================
> PHASE 4 — PHONE READINESS
> ============================================================
>
> Before asking the user to present the Seeker, require the user to have:
>
> - NFC enabled
> - Internet available
> - device unlocked
> - ENSv2 Access Demo open
> - authenticated Privy session
> - existing embedded Ethereum EOA available
>
> Expected wallet:
>
> 0x3419148731087b970d2059C53780163B452D5FF7
>
> Do NOT request or capture an OTP.
>
> If normal Privy login is required, the user may enter the OTP privately.
>
> Do not create another wallet.
>
> Only when firmware, bridge and phone are ready tell the user exactly:
>
> PRESENT THE SEEKER TO THE PN532 NOW AND HOLD IT STEADY IN ONE POSITION UNTIL I
> TELL YOU TO REMOVE IT.
>
> ============================================================
> PHASE 5 — PHYSICAL TRANSPORT
> ============================================================
>
> Perform exactly ONE physical presentation/signing attempt.
>
> Require:
>
> TARGET_ACTIVATION: PASS
>
> SELECT: PASS
>
> WAITING_CHALLENGE
>
> Only after WAITING_CHALLENGE may Node issue the fresh challenge.
>
> Require exactly:
>
> challenges issued:
> 1
>
> credential:
> guest-001.demo-access.eth
>
> resource:
> demo-access.eth:door-001
>
> resource ID:
> 0xf2bde8f2654ee7267a06860eca03e0935d8818b006f5f51c2fce9d56a9b441cd
>
> challenge:
> fresh server-issued secure nonce
>
> challenge payload:
> 104 bytes
>
> SEND_CHALLENGE:
> PASS
>
> SEND_CHALLENGE count:
> 1
>
> Then require:
>
> STATUS:
> PROCESSING → READY
> or direct READY if already complete
>
> GET_SIGNATURE:
> PASS
>
> PROOF LENGTH:
> 65 bytes
>
> Raw proof/signature content must NOT be printed or persistently logged.
>
> Require Batch A proof-log redaction to be effective.
>
> If firmware emits:
>
> GATE_E: STOP - ...
>
> or ISO-DEP fails, challenge deadline expires, proof timeout occurs, or
> signature transport fails:
>
> STOP immediately.
>
> Do not perform a second physical attempt.
>
> ============================================================
> PHASE 6 — HOLDER PROOF
> ============================================================
>
> Using the existing Gate A semantics require:
>
> - challenge known/server-issued
> - challenge not expired
> - credential binding valid
> - resource binding valid
> - signature cryptographically valid
>
> RECOVERED SIGNER:
>
> 0x3419148731087b970d2059C53780163B452D5FF7
>
> CURRENT ENS OWNER:
>
> 0x3419148731087b970d2059C53780163B452D5FF7
>
> OWNER MATCH:
> PASS
>
> CHALLENGE CONSUMED:
> YES
>
> Confirm consumption occurs according to existing Gate A ordering before final
> ENS access-policy result.
>
> ============================================================
> PHASE 7 — FRESH ACTIVE ENS AUTHORIZATION
> ============================================================
>
> Require the authorization snapshot used for this attempt:
>
> - comes entirely from ONE RPC provider
> - chain ID 11155111
> - pinned coherent block
> - Batch A block freshness PASS
> - registry status REGISTERED
> - owner == recovered signer
> - registry expiry valid
> - resolver expected
> - access.active == true
> - access.validUntil == 1793487599
> - access validity future
>
> Require:
>
> VERIFIER RESULT:
> ALLOW
>
> or the actual current equivalent internal classification:
>
> VERIFIER_ALLOW
>
> Require this result occurs within:
>
> - ENS budget <= 8 seconds
> - total post-challenge attempt deadline <= 50 seconds
>
> A stale/late result does NOT count.
>
> ============================================================
> PHASE 8 — PHYSICAL CONTROLLER ALLOW
> ============================================================
>
> Require Node writes exactly once:
>
> AUTHORIZATION=ALLOW
>
> Require:
>
> AUTHORIZATION COMMAND WRITES:
> 1
>
> Then keep serial open and require the firmware subsequently emits:
>
> AUTHORIZATION: ALLOW
>
> This line must occur AFTER Node's authorization command.
>
> Require:
>
> FIRMWARE CONFIRMATION:
> AUTHORIZATION: ALLOW
>
> CONFIRMATION MATCH:
> PASS
>
> CONTROLLER RESULT:
> CONTROLLER_CONFIRMED
>
> SERIAL CLOSED AFTER CONFIRMATION:
> PASS
>
> Bridge/process success:
> exit code 0 or current normal successful equivalent.
>
> Do NOT infer controller success merely from Node's ALLOW.
>
> The exact firmware ALLOW confirmation must be observed.
>
> ============================================================
> PHASE 9 — SAME-PROOF REPLAY
> ============================================================
>
> Using the SAME Node process / IssuedChallengeStore and the exact already
> consumed proof:
>
> perform the implemented replay check.
>
> Do NOT:
>
> - present the phone again
> - ask Privy to sign again
> - issue another challenge
> - reset the ESP32
> - send another physical proof
> - perform a blockchain write
>
> Require:
>
> REPLAYED_CHALLENGE
>
> Authorization:
> DENY
>
> Require:
>
> new challenges:
> 0
>
> additional NFC operations:
> 0
>
> additional signatures:
> 0
>
> additional ENS reads:
> 0 if replay is rejected before ENS lookup as designed
>
> blockchain writes:
> 0
>
> The previous physical ALLOW must not make the consumed proof reusable.
>
> ============================================================
> PHASE 10 — FINAL READ-ONLY STATE
> ============================================================
>
> After completion require:
>
> guest-001 remains:
>
> REGISTERED
> owner == Privy EOA
> access.active == true
> access.validUntil == 1793487599
> policy authorization == ALLOW
>
> cred-001 identity:
> intact
>
> DEV nonce:
>
> final latest/pending == pre-run latest/pending
>
> Expected:
> 22 / 22
>
> No pending transaction.
>
> Zero blockchain writes.
>
> Git remains clean/synchronized.
>
> ============================================================
> PHASE 11 — STOP
> ============================================================
>
> STOP after this one attempt.
>
> Do NOT:
>
> - deactivate guest-001
> - run another physical proof
> - edit anything
> - update docs
> - commit
> - push
>
> If ANY stage fails:
>
> return evidence and STOP.
>
> Do not debug/fix inside this validation run.
>
> ============================================================
> RETURN
> ============================================================
>
> # GATE E ACTIVE END-TO-END
>
> ## REPO / CHAIN PREFLIGHT
>
> ## GUEST ACTIVE BASELINE
>
> ## COLD-BOOT / PN532 READINESS
>
> ## SERIAL / PHONE READINESS
>
> ## FRESH CHALLENGE
>
> Report:
>
> challenge count
> credential binding
> resource binding
> 104-byte payload
> expiry/deadline validity
>
> Do not print nonce/signature unnecessarily.
>
> ## PHYSICAL TRANSPORT
>
> Report:
>
> TARGET_ACTIVATION
> SELECT
> WAITING_CHALLENGE
> SEND_CHALLENGE count
> STATUS
> GET_SIGNATURE
> PROOF LENGTH
> PROOF LOG REDACTION
>
> ## HOLDER PROOF
>
> Report:
>
> RECOVERED SIGNER
> CURRENT ENS OWNER
> OWNER MATCH
> CHALLENGE CONSUMED
>
> ## ENS ACTIVE POLICY
>
> Report:
>
> provider
> pinned block
> freshness
> registry status
> registry expiry
> access.active
> access.validUntil
> verification time/budget
>
> Require:
>
> VERIFIER RESULT:
> ALLOW
>
> ## PHYSICAL CONTROLLER
>
> Report exactly:
>
> Node command:
> AUTHORIZATION=ALLOW
>
> Authorization writes:
> 1
>
> Firmware line observed:
> AUTHORIZATION: ALLOW
>
> Confirmation match:
> PASS
>
> Controller:
> CONTROLLER_CONFIRMED
>
> Serial closed after confirmation:
> PASS
>
> ## REPLAY
>
> Require:
>
> REPLAYED_CHALLENGE
> DENY
>
> Confirm zero new:
>
> - challenges
> - NFC operations
> - signatures
> - blockchain writes
>
> ## DEADLINE / FAIL-CLOSED EVIDENCE
>
> Confirm the successful attempt completed inside the Batch A budgets and no
> late result path was used.
>
> ## NONCE / ONCHAIN STATE
>
> ## SECURITY
>
> Confirm:
>
> - UID unused
> - zero blockchain writes
> - no secret/OTP/private-key/RPC credential exposure
> - proof not printed/persisted
> - APDU v1 unchanged
> - no cached ALLOW
> - cred-001 unchanged
>
> ## GIT STATE
>
> ## WHAT THIS PROVES
>
> State precisely:
>
> A fresh physical EIP-712 proof produced by the Privy wallet currently owning
> guest-001.demo-access.eth was transported over Android HCE / ISO-DEP /
> PN532 / ESP32, verified by Gate A, evaluated against a fresh coherent ENSv2
> ACTIVE policy snapshot, delivered as ALLOW to the physical controller, and
> confirmed by that controller.
>
> The same consumed proof was rejected on replay.
>
> Do not claim production-grade physical security or relay resistance.
>
> ## NEXT ACTION
>
> If PASS:
>
> Checkpoint Gate E ACTIVE end-to-end evidence, then begin Batch B — Demo
> Reliability.
>
> End exactly:
>
> GATE E ACTIVE: PASS
>
> or
>
> GATE E ACTIVE: STOP — <exact stage/reason>
