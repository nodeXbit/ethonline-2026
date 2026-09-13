# Project task packet 052: TASK — Continue the LIVE DEMO REHEARSAL ONLY through TAP 1 — INACTIVE physical DENY.

- Classification: **ORIGINAL / SANITIZED**
- Redactions applied: none
- Source: retained project-specific Control Tower packet in the local Codex record

---

> TASK
>
> Continue the LIVE DEMO REHEARSAL ONLY through TAP 1 — INACTIVE physical DENY.
>
> CURRENT AUTHORITATIVE STATE
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
> false
>
> access.validUntil:
> 1793487599
>
> policy:
> DENY
>
> DEV nonce:
> 23 / 23
>
> Confirmed rehearsal blockchain writes so far:
> 1
>
> That write was the successful guest deactivation:
>
> 0x70a942ab82ff6eb7c2032bd4bff8d142b35187d748ccd5da6013f280c5ac96df
>
> Do NOT click Deactivate again.
>
> Existing Batch B demo server is already running at:
>
> http://127.0.0.1:4173
>
> Reuse it.
>
> Do NOT start another npm run demo instance.
>
> This task authorizes:
>
> - zero blockchain writes
> - exactly ONE fresh physical NFC presentation/signature attempt
>
> DO NOT:
>
> - activate guest
> - deactivate again
> - perform any blockchain mutation
> - edit files
> - commit/push
> - flash firmware
> - use Computer Use
> - perform a second physical attempt
> - issue another challenge after a failure
>
> ============================================================
> PHASE 1 — FRESH READ-ONLY SAFETY
> ============================================================
>
> Require:
>
> repository clean
>
> HEAD == origin/main ==
> 1b1317e65106bb173d1eedc19ae32fcc34513651
>
> Sepolia chain:
> 11155111
>
> DEV:
> latest == pending == 23
> no pending transaction
>
> guest:
> REGISTERED
> expected owner
> expected resolver
> registry expiry valid
> access.active == false
> access.validUntil == 1793487599
> policy == DENY
>
> snapshot freshness:
> PASS
>
> cred-001 identity:
> intact
>
> Use harmless GET /api/credential if useful.
>
> No mutation HTTP request.
>
> ============================================================
> PHASE 2 — COLD BOOT + READINESS
> ============================================================
>
> KEEP THE SEEKER AWAY FROM THE PN532.
>
> Follow the CURRENT committed DEMO_RUNBOOK.md and preflight implementation.
>
> First inspect only enough to determine the exact correct ordering between:
>
> - npm run demo:preflight -- --observe-boot
> - controller power removal/reconnect
> - optional RST/EN
>
> Do not guess the ordering.
>
> The purpose is to ensure the finalized preflight actually observes the firmware
> boot readiness lines if its implementation requires being active during boot.
>
> Then guide the user one physical action at a time.
>
> Required cold-boot facts:
>
> - remove all controller USB/power
> - verify CH343 disappears
> - approximately 10 seconds unpowered
> - reconnect only the established CH343 path
> - identify by CH343 device identity
> - if passive boot output is absent, at most ONE short RST/EN press
> - never press BOOT
>
> Require observed readiness:
>
> I2C 0x24:
> ACK
>
> PN532 firmware:
> 1.6
>
> GATE_E_READY:
> YES
>
> PRESENT_SEEKER:
> YES
>
> No GATE_E STOP.
>
> If cold boot/readiness fails:
> STOP.
>
> Do not retry.
>
> ============================================================
> PHASE 3 — FINALIZED DEMO PREFLIGHT
> ============================================================
>
> Run the correct committed Batch B preflight sequence.
>
> Expected operator command where applicable:
>
> npm run demo:preflight -- --observe-boot
>
> Require:
>
> DEMO PREFLIGHT: PASS
>
> Require automated checks include:
>
> - canonical guest config
> - CH343 discovery
> - serial availability
> - Sepolia chain 11155111
> - fresh block
> - guest REGISTERED
> - expected owner/resolver
> - registry expiry valid
> - access validity > 24h
> - INACTIVE policy accepted
> - DEV latest == pending == 23
> - no pending transaction
>
> Fallback RPC may be NOT CONFIGURED.
>
> Require zero writes and zero challenge issuance.
>
> Manual phone readiness:
>
> - Seeker unlocked
> - NFC ON
> - Internet available
> - ENSv2 Access Demo open
> - Privy authenticated
> - displayed/current wallet ==
>   0x3419148731087b970d2059C53780163B452D5FF7
>
> If OTP is needed, user enters it privately.
>
> Do not capture OTP.
>
> ============================================================
> PHASE 4 — SERIAL OWNERSHIP
> ============================================================
>
> Ensure preflight/serial observer has fully released the CH343 port.
>
> No Arduino Serial Monitor or other terminal may own it.
>
> Start the finalized bridge:
>
> npm run demo:bridge
>
> Use actual committed CLI semantics.
>
> Do NOT enable replay checking for this first INACTIVE tap unless required by the
> committed command.
>
> Require bridge successfully owns the intended CH343 port.
>
> Only when bridge and firmware are ready tell the user exactly:
>
> PRESENT THE SEEKER NOW AND HOLD IT STEADY IN ONE POSITION UNTIL THE RESULT IS
> CONFIRMED.
>
> ============================================================
> PHASE 5 — TAP 1 PHYSICAL TRANSPORT
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
> Fresh Gate A challenges issued:
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
> SEND_CHALLENGE count:
> 1
>
> Require:
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
> Do not print/store raw proof.
>
> If any transport stage fails:
> STOP.
>
> No second tap.
>
> ============================================================
> PHASE 6 — HOLDER VERIFICATION
> ============================================================
>
> Require:
>
> cryptographic signature:
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
> This must establish valid holder possession before policy denial.
>
> ============================================================
> PHASE 7 — INACTIVE POLICY DENY
> ============================================================
>
> Use a fresh coherent ENS snapshot.
>
> Require:
>
> chain:
> 11155111
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
> false
>
> access.validUntil:
> 1793487599
>
> freshness:
> PASS
>
> Require verifier outcome:
>
> VERIFIER_DENY
>
> Reason must be:
>
> POLICY INACTIVE
>
> or exact committed equivalent.
>
> This must NOT be classified as:
>
> TRANSPORT_FAILURE
>
> ============================================================
> PHASE 8 — PHYSICAL CONTROLLER DENY
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
> controller result:
> CONTROLLER_CONFIRMED
>
> serial closed only after matching confirmation:
> PASS
>
> Do not infer physical success merely from Node's DENY command.
>
> ============================================================
> PHASE 9 — UI / RUNTIME EVIDENCE
> ============================================================
>
> After bridge completion, use harmless GET/read-only evidence or ask the user to
> refresh/observe their normal browser.
>
> Require semantic distinction:
>
> ENSv2 POLICY:
> INACTIVE / POLICY DENY
>
> HOLDER VERIFIER:
> DENY
>
> The UI/reason should make clear that the holder was valid but policy was
> inactive, if the current presentation supports that detail.
>
> PHYSICAL CONTROLLER:
> CONFIRMED DENY
>
> It must NOT display:
>
> TRANSPORT FAILURE
>
> No proof/signature bytes visible.
>
> Do not click any UI mutation button.
>
> ============================================================
> PHASE 10 — FINAL STATE
> ============================================================
>
> Read-only require:
>
> guest remains:
> REGISTERED
> active=false
> validUntil=1793487599
> policy DENY
>
> DEV nonce remains:
>
> 23 / 23
>
> No pending transaction.
>
> Blockchain writes during THIS task:
> 0
>
> Total confirmed rehearsal blockchain writes remains:
> 1
>
> Physical taps during rehearsal:
> 1
>
> Fresh challenges:
> 1
>
> Fresh signatures:
> 1
>
> Git:
> clean and synchronized
>
> ============================================================
> STOP
> ============================================================
>
> STOP after confirmed TAP 1 DENY.
>
> Do NOT:
>
> - Activate guest
> - perform TAP 2
> - replay anything
> - cold boot again
> - edit docs/code
> - commit/push
>
> On any unexpected failure:
> STOP immediately.
>
> No retry.
>
> ============================================================
> RETURN
> ============================================================
>
> # REHEARSAL TAP 1 — INACTIVE DENY
>
> ## READ-ONLY BASELINE
>
> ## COLD BOOT
>
> ## DEMO PREFLIGHT
>
> ## PHONE / SERIAL READINESS
>
> ## PHYSICAL TRANSPORT
>
> Report:
>
> TARGET_ACTIVATION
> SELECT
> WAITING_CHALLENGE
> challenge count
> SEND_CHALLENGE count
> STATUS
> GET_SIGNATURE
> proof length
> redaction
>
> ## HOLDER VERIFICATION
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
> freshness:
> verifier result:
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
> Confirmation:
> PASS
>
> Controller:
> CONTROLLER_CONFIRMED
>
> ## UI
>
> Require:
>
> POLICY = INACTIVE/DENY
> HOLDER VERIFIER = DENY
> PHYSICAL CONTROLLER = CONFIRMED DENY
>
> and no transport-failure misclassification.
>
> ## COUNTS
>
> Total rehearsal blockchain writes:
> 1
>
> New blockchain writes this task:
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
> 23 / 23
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
