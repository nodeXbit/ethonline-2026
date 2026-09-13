# Project task packet 049: TASK — Perform ONE complete LIVE DEMO REHEARSAL using the finalized Batch B workflow.

- Classification: **ORIGINAL / SANITIZED**
- Redactions applied: none
- Source: retained project-specific Control Tower packet in the local Codex record

---

> TASK
>
> Perform ONE complete LIVE DEMO REHEARSAL using the finalized Batch B workflow.
>
> This is VALIDATION ONLY.
>
> DO NOT edit files.
> DO NOT fix bugs during the run.
> DO NOT commit or push.
> DO NOT flash firmware.
> DO NOT change architecture.
> DO NOT install anything.
>
> BY PASTING THIS TASK, THE USER AUTHORIZES AT MOST TWO SEPOLIA BLOCKCHAIN
> TRANSACTIONS, BOTH THROUGH THE EXISTING LOCAL DEMO UI:
>
> WRITE 1
> guest-001 access.v1:
> active=true -> active=false
> validUntil MUST remain 1793487599
>
> WRITE 2
> guest-001 access.v1:
> active=false -> active=true
> validUntil MUST remain 1793487599
>
> NO OTHER BLOCKCHAIN WRITE IS AUTHORIZED.
>
> The USER must personally click the Deactivate and Activate UI buttons when
> explicitly instructed.
>
> Codex must NOT autonomously click either transaction button.
>
> At most TWO fresh physical NFC presentations are authorized:
>
> TAP 1:
> INACTIVE policy -> expected physical DENY
>
> TAP 2:
> ACTIVE policy -> expected physical ALLOW
>
> The replay check after TAP 2 MUST reuse the already-consumed proof in memory and
> must NOT cause a third NFC presentation or signature.
>
> ============================================================
> EXPECTED REPOSITORY BASELINE
> ============================================================
>
> Expected pushed checkpoint:
>
> 1b1317e65106bb173d1eedc19ae32fcc34513651
>
> First run read-only:
>
> git fetch origin
> git status -sb
> git rev-parse HEAD
> git rev-parse origin/main
>
> Require:
>
> working tree clean
> HEAD == origin/main ==
> 1b1317e65106bb173d1eedc19ae32fcc34513651
>
> If not:
> STOP.
>
> ============================================================
> CURRENT EXPECTED ONCHAIN BASELINE
> ============================================================
>
> Do not trust stale values.
>
> Read coherently before doing anything.
>
> Expected:
>
> credential:
> guest-001.demo-access.eth
>
> owner:
> 0x3419148731087b970d2059C53780163B452D5FF7
>
> resolver:
> 0x0B723c0C2170F2ea508F2f4e3e122C6c0782744C
>
> validUntil:
> 1793487599
>
> current expected policy:
> ACTIVE / ALLOW
>
> DEV expected nonce:
> 22 / 22
>
> Require:
>
> Sepolia chain 11155111
> REGISTERED
> expected owner
> expected resolver
> registry expiry valid
> access record valid
> validUntil exactly 1793487599
> block freshness PASS
> latest nonce == pending nonce
> no pending transaction
>
> cred-001 identity must remain intact.
>
> If current state differs materially:
> STOP.
>
> ============================================================
> PHASE 1 — START LOCAL DEMO
> ============================================================
>
> Run the finalized Batch B local server:
>
> npm run demo
>
> Capture the actual loopback URL.
>
> Expected historical URL:
> http://127.0.0.1:4173
>
> Do not expose 0.0.0.0.
>
> Tell the user to open the local demo in their normal local browser.
>
> Require UI shows:
>
> guest-001.demo-access.eth
>
> ENSv2 POLICY:
> ACTIVE / POLICY ALLOW
>
> HOLDER VERIFIER:
> NOT RUN
>
> PHYSICAL CONTROLLER:
> NOT RUN
>
> or equivalent correct initial sanitized state.
>
> Do not fabricate runtime evidence.
>
> ============================================================
> PHASE 2 — SET REHEARSAL START STATE TO INACTIVE
> ============================================================
>
> This is rehearsal SETUP, not the first physical interaction.
>
> Immediately before the write re-check:
>
> DEV latest == pending
> no pending transaction
>
> Tell the user exactly:
>
> CLICK DEACTIVATE ONCE NOW.
>
> The user performs the click manually.
>
> Do not click for them.
>
> Observe the existing safe transaction flow.
>
> Require exactly ONE transaction.
>
> Require:
>
> receipt SUCCESS
> guest remains REGISTERED
> owner unchanged
> resolver unchanged
> token/resource unchanged
> registry expiry unchanged
> access.active == false
> access.validUntil == 1793487599
> policy authorization == DENY
>
> Capture:
>
> tx hash
> tx nonce
> receipt block
>
> Require DEV nonce increases by exactly +1 and settles latest == pending.
>
> Do not perform a second click.
>
> Require browser now clearly shows:
>
> ENSv2 POLICY:
> INACTIVE / POLICY DENY
>
> This completes rehearsal setup.
>
> ============================================================
> PHASE 3 — VERIFIED COLD BOOT
> ============================================================
>
> Tell the user:
>
> KEEP THE SEEKER AWAY FROM PN532.
>
> Follow DEMO_RUNBOOK.md exactly.
>
> Complete power removal:
>
> disconnect all controller USB/power
>
> Require CH343 disappears.
>
> Wait approximately 10 seconds.
>
> Reconnect only the established CH343 serial path.
>
> Identify CH343 by device identity rather than assuming COM4.
>
> If passive boot output does not appear, ONE short RST/EN press is allowed.
>
> Never press BOOT unless upload recovery were required, which it is not here.
>
> Require:
>
> I2C 0x24 ACK
> PN532 firmware 1.6
> GATE_E_READY
> PRESENT_SEEKER
>
> If readiness fails:
> STOP.
>
> No repeated resets.
>
> ============================================================
> PHASE 4 — FINALIZED DEMO PREFLIGHT
> ============================================================
>
> Run the finalized Batch B preflight using the appropriate boot-observation mode
> for the established runbook.
>
> Prefer the committed operator command:
>
> npm run demo:preflight -- --observe-boot
>
> Use actual committed semantics.
>
> Require automated healthy result within the intended <120 second budget.
>
> Require:
>
> canonical guest config PASS
> CH343 PASS
> serial availability PASS
> Sepolia 11155111 PASS
> fresh RPC block PASS
> guest REGISTERED
> expected owner
> expected resolver
> registry expiry PASS
> validUntil > 24h
> policy INACTIVE accepted
> DEV latest == pending
> no pending transaction
>
> Fallback may report NOT CONFIGURED.
>
> Require:
>
> DEMO PREFLIGHT: PASS
>
> Manual phone check:
>
> Seeker unlocked
> NFC ON
> Internet available
> ENSv2 Access Demo open
> Privy authenticated
> displayed/current wallet ==
> 0x3419148731087b970d2059C53780163B452D5FF7
>
> If Privy login is required, user enters OTP privately.
>
> Do not capture OTP.
>
> ============================================================
> PHASE 5 — TAP 1: VALID HOLDER + INACTIVE
> ============================================================
>
> Ensure any preflight serial observer has closed COM.
>
> Require exactly one process will own CH343 for Gate E.
>
> Start finalized bridge WITHOUT needing replay validation for this first policy
> DENY unless the committed CLI requires it:
>
> npm run demo:bridge
>
> Use actual committed syntax.
>
> Only once bridge/firmware are ready tell the user exactly:
>
> PRESENT THE SEEKER NOW AND HOLD IT STEADY UNTIL THE RESULT IS CONFIRMED.
>
> Perform exactly ONE fresh physical tap.
>
> Require transport:
>
> TARGET_ACTIVATION PASS
> SELECT PASS
> WAITING_CHALLENGE
> exactly 1 fresh challenge
> 104-byte challenge
> SEND_CHALLENGE exactly once
> PROCESSING -> READY
> GET_SIGNATURE PASS
> proof length 65 bytes
> proof redacted
>
> Require holder:
>
> recovered signer ==
> 0x3419148731087b970d2059C53780163B452D5FF7
>
> CURRENT ENS owner ==
> same address
>
> owner match PASS
>
> challenge consumed YES
>
> Require fresh policy:
>
> access.active == false
> access.validUntil == 1793487599
>
> VERIFIER:
> DENY
>
> Reason:
> policy inactive
>
> This is NOT a transport failure.
>
> Require Node sends exactly once:
>
> AUTHORIZATION=DENY
>
> Require firmware subsequently emits:
>
> AUTHORIZATION: DENY
>
> Require:
>
> controller confirmation MATCH
> CONTROLLER_CONFIRMED
> serial closes after confirmation
>
> Remove phone after result.
>
> Require UI runtime evidence distinguishes:
>
> ENSv2 POLICY:
> INACTIVE / DENY
>
> HOLDER VERIFIER:
> DENY because policy is inactive
> with valid recovered/current owner evidence where displayed
>
> PHYSICAL CONTROLLER:
> CONFIRMED DENY
>
> Do not run another physical attempt if TAP 1 fails.
>
> STOP on failure.
>
> ============================================================
> PHASE 6 — ACTIVATE THROUGH FINAL DEMO UI
> ============================================================
>
> Only after TAP 1 fully passes.
>
> No phone near PN532.
>
> Immediately before write 2 require:
>
> DEV latest == pending
> no pending transaction
> guest still INACTIVE
> validUntil == 1793487599
>
> Tell the user exactly:
>
> CLICK ACTIVATE ONCE NOW.
>
> The user performs the click manually.
>
> Do not click for them.
>
> Require exactly ONE activation transaction.
>
> Require:
>
> receipt SUCCESS
> status REGISTERED
> owner unchanged
> token/resource unchanged
> resolver unchanged
> registry expiry unchanged
> access.active == true
> access.validUntil == 1793487599
> policy authorization == ALLOW
>
> Capture:
>
> tx hash
> nonce
> receipt block
>
> Require total rehearsal blockchain writes so far:
>
> 2
>
> Require final DEV nonce after activation settles latest == pending.
>
> Browser must show:
>
> ENSv2 POLICY:
> ACTIVE / POLICY ALLOW
>
> and must still NOT imply physical controller confirmation from policy alone.
>
> ============================================================
> PHASE 7 — PREPARE TAP 2
> ============================================================
>
> Use the current committed runbook conservatively.
>
> Perform a new full cold boot before TAP 2 so the second physical sequence starts
> from known hardware readiness.
>
> Again require:
>
> CH343 disappears on power removal
> approximately 10 seconds unpowered
> reconnect established CH343 path
> one RST/EN only if passive output absent
>
> Require:
>
> 0x24 ACK
> PN532 1.6
> GATE_E_READY
> PRESENT_SEEKER
>
> Run demo preflight again.
>
> Require:
>
> DEMO PREFLIGHT: PASS
> policy ACTIVE
> nonce settled
> owner/resolver/validity correct
>
> Do not mutate anything.
>
> ============================================================
> PHASE 8 — TAP 2: VALID HOLDER + ACTIVE
> ============================================================
>
> Close any preflight serial observer.
>
> Start:
>
> npm run demo:bridge -- --check-replay
>
> Use actual committed syntax.
>
> When ready tell user:
>
> PRESENT THE SEEKER NOW AND HOLD IT STEADY UNTIL THE RESULT IS CONFIRMED.
>
> Perform exactly the SECOND and FINAL fresh NFC presentation.
>
> Require:
>
> TARGET_ACTIVATION PASS
> SELECT PASS
> WAITING_CHALLENGE
> exactly one new fresh challenge for TAP 2
> 104-byte challenge
> SEND_CHALLENGE once
> PROCESSING -> READY
> GET_SIGNATURE PASS
> proof 65 bytes
> proof redacted
>
> Holder verification:
>
> recovered signer ==
> current ENS owner ==
> 0x3419148731087b970d2059C53780163B452D5FF7
>
> owner match PASS
> challenge consumed YES
>
> Fresh coherent ENS policy:
>
> REGISTERED
> expected resolver
> registry expiry valid
> access.active == true
> access.validUntil == 1793487599
> freshness PASS
>
> Require:
>
> VERIFIER_ALLOW
>
> Require Node writes exactly once to controller:
>
> AUTHORIZATION=ALLOW
>
> Require firmware line:
>
> AUTHORIZATION: ALLOW
>
> Require:
>
> confirmation match PASS
> CONTROLLER_CONFIRMED
> serial closed after confirmation
>
> Remove phone.
>
> ============================================================
> PHASE 9 — SAME-PROOF REPLAY
> ============================================================
>
> Using the exact proof consumed in TAP 2 and the same IssuedChallengeStore:
>
> require:
>
> REPLAYED_CHALLENGE
> DENY
>
> Require ZERO additional:
>
> fresh challenges
> NFC presentations
> Privy signatures
> ENS reads
> blockchain writes
>
> The user must NOT present the phone again.
>
> UI should expose succinctly:
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
> Policy ACTIVE alone must remain visually distinct from controller confirmation.
>
> ============================================================
> PHASE 10 — FINAL STATE / COUNTS
> ============================================================
>
> Do not perform a final deactivation.
>
> Leave guest ACTIVE after successful rehearsal.
>
> Final authoritative read-only state:
>
> guest REGISTERED
> expected owner
> expected resolver
> access.active == true
> access.validUntil == 1793487599
> policy ALLOW
>
> cred-001 intact.
>
> Require transaction accounting from this rehearsal:
>
> WRITE 1:
> deactivate only
>
> WRITE 2:
> activate only
>
> TOTAL BLOCKCHAIN WRITES:
> 2
>
> No other tx.
>
> Require physical accounting:
>
> fresh physical taps:
> 2
>
> fresh signatures:
> 2
>
> TAP 1:
> confirmed DENY
>
> TAP 2:
> confirmed ALLOW
>
> replay:
> DENY without new signature
>
> Final DEV latest == pending.
> No pending transaction.
>
> Git:
>
> clean
> HEAD == origin/main
> no source modifications
>
> Runtime evidence may exist only under ignored .runtime paths.
>
> ============================================================
> FAILURE POLICY
> ============================================================
>
> On ANY unexpected failure:
>
> STOP.
>
> Do NOT:
>
> retry a tap
> click a transaction button twice
> repair state by extra transaction
> reset repeatedly
> edit code
> change configuration
> debug expansively
> commit anything
>
> Capture the exact failing boundary and return to Control Tower.
>
> ============================================================
> RETURN
> ============================================================
>
> # LIVE DEMO REHEARSAL
>
> ## REPO BASELINE
>
> ## STARTING ONCHAIN STATE
>
> ## SETUP DEACTIVATION
>
> Report:
>
> tx hash
> nonce
> receipt
> readback
> policy INACTIVE/DENY
>
> ## COLD BOOT 1
>
> ## PREFLIGHT 1
>
> ## TAP 1 — INACTIVE
>
> Report:
>
> transport
> holder/current owner match
> policy
> verifier result
> Node command
> firmware confirmation
> controller result
>
> Require:
>
> PHYSICAL DENY: PASS
>
> ## UI AFTER DENY
>
> ## REHEARSAL ACTIVATION
>
> Report:
>
> tx hash
> nonce
> receipt
> readback
> policy ACTIVE/ALLOW
>
> ## COLD BOOT 2
>
> ## PREFLIGHT 2
>
> ## TAP 2 — ACTIVE
>
> Report:
>
> transport
> holder/current owner match
> fresh policy
> VERIFIER_ALLOW
> Node command
> firmware confirmation
> CONTROLLER_CONFIRMED
>
> Require:
>
> PHYSICAL ALLOW: PASS
>
> ## REPLAY
>
> Require:
>
> REPLAYED_CHALLENGE
> DENY
>
> ## UI FINAL STATE
>
> Require policy/verifier/controller/replay remain semantically distinct.
>
> ## COUNTS
>
> Blockchain writes:
> 2
>
> Fresh physical taps:
> 2
>
> Fresh challenges:
> 2
>
> Fresh signatures:
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
> ## FINAL ONCHAIN STATE
>
> ## NONCE ACCOUNTING
>
> ## SECURITY
>
> Confirm:
>
> no secrets
> no raw proof persisted
> UID unused
> no extra blockchain writes
> no code edits
> no firmware flash
>
> ## GIT STATE
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
> LIVE REHEARSAL: PASS
>
> or
>
> LIVE REHEARSAL: STOP — <exact failing boundary/reason>
