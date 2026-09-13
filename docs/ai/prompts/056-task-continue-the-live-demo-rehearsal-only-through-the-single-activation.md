# Project task packet 056: TASK — Continue the LIVE DEMO REHEARSAL ONLY through the single activation transaction.

- Classification: **ORIGINAL / SANITIZED**
- Redactions applied: none
- Source: retained project-specific Control Tower packet in the local Codex record

---

> TASK
>
> Continue the LIVE DEMO REHEARSAL ONLY through the single activation transaction.
>
> TAP 1 has already passed:
>
> valid holder
> + current ENS owner match
> + INACTIVE policy
> -> VERIFIER_DENY
> -> AUTHORIZATION=DENY
> -> firmware AUTHORIZATION: DENY
> -> CONTROLLER_CONFIRMED
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
> 23/23
>
> Confirmed rehearsal blockchain writes:
> 1
>
> Confirmed physical taps:
> 1
>
> The existing write was the successful deactivation:
>
> 0x70a942ab82ff6eb7c2032bd4bff8d142b35187d748ccd5da6013f280c5ac96df
>
> Repository expected:
>
> HEAD == origin/main ==
> 1b1317e65106bb173d1eedc19ae32fcc34513651
>
> working tree clean.
>
> THIS TASK AUTHORIZES AT MOST ONE SEPOLIA BLOCKCHAIN TRANSACTION:
>
> guest-001 access.v1
>
> active:
> false -> true
>
> validUntil:
> MUST remain exactly 1793487599
>
> No other blockchain write is authorized.
>
> The USER must click Activate manually in their normal browser.
>
> DO NOT use Computer Use.
>
> DO NOT:
>
> - click Activate autonomously
> - click Deactivate
> - perform NFC
> - present the Seeker
> - reset/cold boot hardware
> - start Gate E bridge
> - edit files
> - commit/push
> - renew validity
> - resend after a transaction hash exists
>
> ============================================================
> PHASE 1 — DEMO SERVER
> ============================================================
>
> Check the existing Batch B server using harmless GET requests only:
>
> GET http://127.0.0.1:4173/
> GET http://127.0.0.1:4173/api/credential
>
> If the existing correct server is running, reuse it.
>
> Do NOT start a second server.
>
> If no server is listening, start exactly one:
>
> npm run demo
>
> Require:
>
> credential:
> guest-001.demo-access.eth
>
> expected owner/resolver
>
> current policy:
> INACTIVE / DENY
>
> No mutation HTTP request may be sent by Codex.
>
> ============================================================
> PHASE 2 — FRESH PRE-CLICK SAFETY
> ============================================================
>
> Immediately before asking the user to click, require read-only:
>
> git clean/synchronized
>
> Sepolia:
> 11155111
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
> latest == pending == 23
>
> no pending transaction
>
> cred-001:
> intact
>
> If any relevant state differs:
> STOP before the click.
>
> ============================================================
> PHASE 3 — USER ACTIVATION
> ============================================================
>
> Once all checks pass, tell the user exactly:
>
> OPEN THE EXISTING LOCAL DEMO IN YOUR NORMAL BROWSER.
>
> VERIFY:
>
> ENSv2 POLICY:
> INACTIVE / POLICY DENY
>
> THEN CLICK ACTIVATE EXACTLY ONCE.
>
> DO NOT CLICK IT AGAIN.
>
> The user performs the click manually.
>
> Codex does not use browser automation.
>
> ============================================================
> PHASE 4 — TRANSACTION EVIDENCE
> ============================================================
>
> After the user confirms the single click:
>
> capture the resulting public transaction hash from the existing server/runtime
> output if available.
>
> Once a hash exists:
>
> NEVER request another click.
> NEVER resubmit.
> NEVER replace the transaction.
>
> Verify:
>
> sender:
> DEV
>
> destination:
> expected PermissionedResolver
>
> function:
> setData
>
> node:
> guest-001.demo-access.eth
>
> key:
> access.v1
>
> decoded value:
> active=true
> validUntil=1793487599
>
> Capture:
>
> transaction hash
> transaction nonce
> receipt block
> receipt status
>
> Require:
>
> receipt:
> SUCCESS
>
> If pending or uncertain:
>
> receipt/state reconciliation only.
>
> No resubmission.
>
> ============================================================
> PHASE 5 — AUTHORITATIVE ACTIVE READBACK
> ============================================================
>
> After confirmed receipt use the coherent ENS reader.
>
> Require:
>
> status:
> REGISTERED
>
> owner:
> unchanged
>
> token/resource:
> unchanged
>
> resolver:
> unchanged
>
> registry expiry:
> unchanged and valid
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
> snapshot freshness:
> PASS
>
> Require DEV:
>
> latest/pending:
> 24/24
>
> no pending transaction
>
> Nonce increase from the pre-click state:
>
> exactly +1
>
> ============================================================
> PHASE 6 — UI
> ============================================================
>
> Ask the user to visually confirm in their normal browser:
>
> ENSv2 POLICY:
> ACTIVE / POLICY ALLOW
>
> Also require the UI still does NOT imply physical access has been confirmed.
>
> The previous physical evidence from TAP 1 may remain visible as historical last
> attempt evidence if that is the committed UI behavior.
>
> Do not fabricate a new physical ALLOW.
>
> Do not click anything else.
>
> ============================================================
> PHASE 7 — COUNTS
> ============================================================
>
> Require total rehearsal accounting:
>
> Confirmed blockchain writes:
> 2
>
> Write 1:
> deactivation only
>
> Write 2:
> activation only
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
> No NFC occurred during this activation task.
>
> ============================================================
> STOP
> ============================================================
>
> STOP after ACTIVE / ALLOW is authoritatively verified.
>
> Do NOT:
>
> - cold boot
> - run preflight
> - start bridge
> - perform TAP 2
> - replay anything
> - deactivate afterward
> - edit docs/code
> - commit/push
>
> ============================================================
> RETURN
> ============================================================
>
> # REHEARSAL ACTIVATION
>
> ## PRE-CLICK STATE
>
> ## MANUAL CLICK
>
> ## ACTIVATION TX
>
> Hash:
> Nonce:
> Receipt block:
> Receipt status:
>
> Decoded mutation:
>
> active=true
> validUntil=1793487599
>
> ## AUTHORITATIVE READBACK
>
> Status:
> Owner:
> Resolver:
> Token/resource:
> Registry expiry:
> access.active:
> access.validUntil:
> Policy:
> Snapshot freshness:
>
> Require:
>
> ACTIVE / ALLOW
>
> ## NONCE
>
> Require:
>
> 23/23 -> 24/24
>
> ## UI
>
> Require user confirmation:
>
> ENSv2 POLICY:
> ACTIVE / POLICY ALLOW
>
> Physical confirmation must NOT be inferred from policy alone.
>
> ## COUNTS
>
> Confirmed rehearsal blockchain writes:
> 2
>
> Physical taps:
> 1
>
> Challenges:
> 1
>
> Signatures:
> 1
>
> ## SECURITY / GIT
>
> Confirm:
>
> - no extra blockchain write
> - no NFC
> - no raw proof/signature
> - no code edits
> - no firmware flash
> - git clean/synchronized
>
> ## NEXT
>
> If PASS:
>
> Cold boot + controlled boot-observation preflight + TAP 2 ACTIVE with
> --check-replay, expecting controller-confirmed ALLOW and same-proof replay DENY.
>
> End exactly:
>
> REHEARSAL ACTIVATION: PASS
>
> or
>
> REHEARSAL ACTIVATION: STOP — <exact reason>
