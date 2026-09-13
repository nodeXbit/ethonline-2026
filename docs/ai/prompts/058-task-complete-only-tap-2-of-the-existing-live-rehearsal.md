# Project task packet 058: TASK — Complete ONLY TAP 2 of the existing live rehearsal:

- Classification: **ORIGINAL / SANITIZED**
- Redactions applied: none
- Source: retained project-specific Control Tower packet in the local Codex record

---

> TASK
>
> Complete ONLY TAP 2 of the existing live rehearsal:
>
> VALID HOLDER + ACTIVE POLICY
> -> PHYSICAL CONFIRMED ALLOW
> -> SAME-PROOF REPLAY DENY.
>
> The RPC credential incident is resolved.
>
> Verified replacement RPC status:
>
> - old credential rotated by user
> - replacement never printed
> - local config ignored
> - safe Sepolia connection PASS
> - chain 11155111
> - guest ACTIVE / ALLOW
> - DEV nonce 24/24
> - no pending transaction
> - leak source was AD-HOC DIAGNOSTIC ONLY
> - PROJECT RPC LOGGING FIX REQUIRED: NO
>
> One correct Batch B demo server is already running at:
>
> http://127.0.0.1:4173
>
> using the replacement RPC configuration.
>
> REUSE IT.
>
> Do NOT start another npm run demo instance.
>
> ============================================================
> CURRENT REHEARSAL STATE
> ============================================================
>
> guest-001.demo-access.eth
>
> status:
> REGISTERED
>
> owner:
> 0x3419148731087b970d2059C53780163B452D5FF7
>
> resolver:
> 0x0B723c0C2170F2ea508F2f4e3e122C6c0782744C
>
> access.active:
> true
>
> access.validUntil:
> 1793487599
>
> policy:
> ALLOW
>
> DEV:
> 24/24
>
> Confirmed rehearsal blockchain writes:
> 2
>
> Write 1:
> deactivation
>
> Write 2:
> activation
>
> Physical taps:
> 1
>
> TAP 1:
> valid holder + INACTIVE
> -> VERIFIER_DENY
> -> AUTHORIZATION=DENY
> -> firmware AUTHORIZATION: DENY
> -> CONTROLLER_CONFIRMED
>
> TAP 2:
> NOT RUN
>
> Repository expected:
>
> HEAD == origin/main ==
> 1b1317e65106bb173d1eedc19ae32fcc34513651
>
> working tree clean.
>
> THIS TASK AUTHORIZES:
>
> - ZERO blockchain writes
> - exactly ONE fresh physical NFC presentation
> - exactly ONE fresh Privy signature
> - same-proof in-memory replay checking after successful TAP 2
>
> DO NOT:
>
> - activate/deactivate/renew
> - click transaction controls
> - use Computer Use
> - edit files
> - commit/push
> - flash firmware
> - use raw uncaught RPC diagnostic commands
> - print any RPC URL or credential
> - perform a second physical tap
> - perform another fresh signature after TAP 2
> - issue a second fresh challenge after TAP 2
>
> ============================================================
> PHASE 1 — FRESH READ-ONLY BASELINE
> ============================================================
>
> Require:
>
> repository clean/synchronized
>
> Sepolia:
> 11155111
>
> primary RPC:
> PASS
>
> block freshness:
> PASS
>
> guest:
> REGISTERED
>
> owner:
> 0x3419148731087b970d2059C53780163B452D5FF7
>
> resolver:
> 0x0B723c0C2170F2ea508F2f4e3e122C6c0782744C
>
> registry expiry:
> valid
>
> access.active:
> true
>
> access.validUntil:
> 1793487599
>
> policy:
> ALLOW
>
> DEV:
> latest == pending == 24
>
> no pending transaction
>
> cred-001:
> intact
>
> All RPC errors must be sanitized.
>
> Never allow raw request metadata / RPC URLs to reach console output.
>
> ============================================================
> PHASE 2 — FULL COLD BOOT
> ============================================================
>
> Tell the user:
>
> KEEP THE SEEKER AWAY FROM THE PN532.
>
> Ensure:
>
> - Gate E bridge is not running
> - Serial Monitor is not running
> - no process owns CH343 except when explicitly required below
>
> Tell the user to:
>
> 1. disconnect ALL controller USB/power;
> 2. confirm CH343 disappears;
> 3. wait approximately 10 seconds;
> 4. reconnect ONLY the established CH343 serial/power path.
>
> Identify CH343 by device identity.
>
> Expected historical port:
> COM4
>
> Do NOT press RST/EN yet.
>
> Do NOT press BOOT.
>
> ============================================================
> PHASE 3 — CONTROLLED BOOT OBSERVATION
> ============================================================
>
> Prepare the user first:
>
> GET READY TO PRESS RST/EN ONCE, BUT DO NOT PRESS IT YET.
>
> Start exactly:
>
> npm run demo:preflight -- --observe-boot
>
> Do not restart the command.
>
> After approximately 6–8 seconds from command launch, while the SAME invocation
> is still running, tell the user exactly:
>
> PRESS RST/EN ONCE NOW.
>
> Require exactly one short RST/EN press.
>
> No BOOT press.
>
> The same preflight invocation must observe in order:
>
> PN532_FIRMWARE=1.6
> GATE_E_READY
> PRESENT_SEEKER
>
> Require:
>
> hardware readiness:
> BOOT_OBSERVED
>
> Then allow the SAME preflight invocation to complete its existing read-only
> checks.
>
> Require:
>
> canonical demo config PASS
> CH343 PASS
> Sepolia 11155111 PASS
> fresh block PASS
> guest REGISTERED
> expected owner/resolver
> registry expiry valid
> access validity >24h
> policy ACTIVE accepted
> DEV latest == pending == 24
> no pending transaction
>
> Require:
>
> DEMO PREFLIGHT: PASS
>
> Fallback may be NOT CONFIGURED.
>
> Require preflight fully releases COM afterward.
>
> If boot observation or preflight fails:
>
> STOP.
>
> Do not retry/reset again.
>
> ============================================================
> PHASE 4 — PHONE / SERIAL READINESS
> ============================================================
>
> Manual phone readiness:
>
> - Seeker unlocked
> - NFC ON
> - Internet available
> - ENSv2 Access Demo open
> - Privy authenticated
> - current/displayed wallet ==
>   0x3419148731087b970d2059C53780163B452D5FF7
>
> If OTP is required, user enters it privately.
>
> Never capture OTP.
>
> Confirm COM is free.
>
> Start exactly:
>
> npm run demo:bridge -- --check-replay
>
> Use the committed Batch B bridge.
>
> Require bridge successfully owns the intended CH343 port.
>
> Only when bridge is ready tell the user exactly:
>
> PRESENT THE SEEKER NOW AND HOLD IT STEADY UNTIL THE RESULT IS CONFIRMED.
>
> ============================================================
> PHASE 5 — TAP 2 PHYSICAL TRANSPORT
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
> Fresh challenge count for TAP 2:
> 1
>
> Credential:
> guest-001.demo-access.eth
>
> Resource:
> demo-access.eth:door-001
>
> Resource ID:
> 0xf2bde8f2654ee7267a06860eca03e0935d8818b006f5f51c2fce9d56a9b441cd
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
> Proof logging:
> REDACTED
>
> Raw proof/signature must never be printed or persisted.
>
> If any transport stage fails:
>
> STOP.
>
> No second presentation.
>
> ============================================================
> PHASE 6 — HOLDER VERIFICATION
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
> ============================================================
> PHASE 7 — ACTIVE ENS POLICY
> ============================================================
>
> Require one coherent fresh authorization snapshot:
>
> chain:
> 11155111
>
> provider:
> PRIMARY
>
> REGISTERED:
> YES
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
> true
>
> access.validUntil:
> 1793487599
>
> freshness:
> PASS
>
> Require:
>
> VERIFIER_ALLOW
>
> Require result inside committed Batch A budgets:
>
> ENS verification:
> <= 8 seconds
>
> post-challenge total attempt:
> <= 50 seconds
>
> No stale or late ALLOW accepted.
>
> ============================================================
> PHASE 8 — CONTROLLER ALLOW
> ============================================================
>
> Require Node sends exactly once:
>
> AUTHORIZATION=ALLOW
>
> Authorization command writes:
> 1
>
> Then require firmware subsequently emits:
>
> AUTHORIZATION: ALLOW
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
> bridge successful exit:
> PASS
>
> Do NOT infer physical success from VERIFIER_ALLOW alone.
>
> ============================================================
> PHASE 9 — SAME-PROOF REPLAY
> ============================================================
>
> Using the exact proof consumed during TAP 2 and the SAME IssuedChallengeStore,
> perform the committed replay check.
>
> DO NOT:
>
> - present the phone again
> - request another Privy signature
> - issue another fresh challenge
> - reset hardware
> - perform ENS mutation
> - broadcast anything
>
> Require:
>
> REPLAYED_CHALLENGE
>
> result:
> DENY
>
> Require zero additional:
>
> fresh challenges
> NFC presentations
> Privy signatures
> ENS reads
> blockchain writes
>
> The replay must be rejected before ENS evaluation as designed.
>
> ============================================================
> PHASE 10 — SANITIZED UI EVIDENCE
> ============================================================
>
> Use the existing advisory runtime evidence / harmless GET endpoints.
>
> No Computer Use.
>
> Require final demo semantics:
>
> ENSv2 POLICY:
> ACTIVE / POLICY ALLOW
>
> HOLDER VERIFIER:
> ALLOW
>
> PHYSICAL CONTROLLER:
> CONFIRMED ALLOW
>
> REPLAY:
> DENIED
>
> SYSTEM READINESS:
> PASS if the current evidence model preserves the successful preflight state.
>
> No raw proof/signature.
>
> Policy ACTIVE must remain visibly distinct from controller confirmation.
>
> ============================================================
> PHASE 11 — FINAL STATE / COUNTS
> ============================================================
>
> Read-only require:
>
> guest:
> REGISTERED
> expected owner/resolver
> access.active=true
> access.validUntil=1793487599
> policy=ALLOW
>
> cred-001:
> intact
>
> DEV:
> 24/24
>
> no pending transaction.
>
> Total rehearsal blockchain writes:
> 2
>
> New blockchain writes during this task:
> 0
>
> Total fresh physical taps:
> 2
>
> Total fresh challenges:
> 2
>
> Total fresh signatures:
> 2
>
> Replay additional challenge:
> 0
>
> Replay additional NFC:
> 0
>
> Replay additional signature:
> 0
>
> Git:
> clean
> HEAD == origin/main
>
> Runtime evidence may remain only in ignored runtime paths.
>
> ============================================================
> STOP
> ============================================================
>
> STOP after successful TAP 2 + replay verification.
>
> Do NOT:
>
> - deactivate guest afterward
> - run another tap
> - edit code/docs
> - commit/push
> - perform another rehearsal
>
> On ANY unexpected failure:
>
> STOP immediately.
>
> No retry.
>
> ============================================================
> RETURN
> ============================================================
>
> # REHEARSAL TAP 2 — ACTIVE ALLOW + REPLAY
>
> ## FRESH BASELINE
>
> ## COLD BOOT
>
> Report:
>
> CH343 disappeared:
> YES / NO
>
> Unpowered interval:
>
> Reconnected device / COM:
>
> ## CONTROLLED BOOT OBSERVATION
>
> RST/EN count:
> 1
>
> Reset timing after observer launch:
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
> DEMO PREFLIGHT:
> PASS / STOP
>
> ## PHONE / SERIAL READINESS
>
> ## PHYSICAL TRANSPORT
>
> TARGET_ACTIVATION:
> SELECT:
> WAITING_CHALLENGE:
> Fresh challenges:
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
> ## ENS ACTIVE POLICY
>
> Snapshot block:
> Freshness:
> access.active:
> access.validUntil:
> Verifier:
> ENS verification time:
> Total attempt time:
>
> Require:
>
> VERIFIER_ALLOW
>
> ## PHYSICAL CONTROLLER
>
> Node command:
> AUTHORIZATION=ALLOW
>
> Authorization writes:
> 1
>
> Firmware line:
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
> Result:
> REPLAYED_CHALLENGE / other
>
> Authorization:
> DENY / other
>
> Additional challenge:
> 0
>
> Additional NFC:
> 0
>
> Additional signature:
> 0
>
> Additional ENS read:
> 0
>
> ## FINAL UI
>
> Policy:
> Holder verifier:
> Physical controller:
> Replay:
> System readiness:
>
> ## REHEARSAL COUNTS
>
> Blockchain writes:
> 2
>
> Physical taps:
> 2
>
> Fresh challenges:
> 2
>
> Fresh signatures:
> 2
>
> ## FINAL ONCHAIN STATE
>
> ## NONCE
>
> Require:
> 24/24
>
> ## SECURITY / GIT
>
> Confirm:
>
> - no RPC credential exposure
> - no raw proof/signature persistence
> - no secrets
> - UID unused
> - zero new blockchain writes
> - no code edits
> - no firmware flash
> - git clean/synchronized
>
> ## REHEARSAL VERDICT
>
> If every required stage passed:
>
> READY TO RECORD: YES
>
> Otherwise:
>
> READY TO RECORD: NO
>
> ## NEXT
>
> Do not update docs or commit.
>
> End exactly:
>
> TAP 2 ACTIVE: PASS
>
> or
>
> TAP 2 ACTIVE: STOP — <exact reason>
