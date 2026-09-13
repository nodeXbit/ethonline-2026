# Project task packet 051: TASK — Resume the LIVE DEMO REHEARSAL only far enough to establish the INACTIVE

- Classification: **ORIGINAL / SANITIZED**
- Redactions applied: none
- Source: retained project-specific Control Tower packet in the local Codex record

---

> TASK
>
> Resume the LIVE DEMO REHEARSAL only far enough to establish the INACTIVE
> starting state.
>
> The previous Deactivate click has been authoritatively reconciled as:
>
> NO WRITE OBSERVED.
>
> Verified current state:
>
> guest-001.demo-access.eth
> REGISTERED
> expected owner/resolver
> access.active=true
> access.validUntil=1793487599
> policy=ALLOW
>
> DEV:
> latest/pending=22/22
>
> Confirmed rehearsal blockchain writes so far:
> 0
>
> Physical taps:
> 0
>
> Do NOT use Computer Use or browser automation.
>
> The USER will operate their normal local browser manually.
>
> This task authorizes at most ONE Sepolia blockchain transaction:
>
> guest-001 access.v1
> active=true -> active=false
>
> validUntil MUST remain exactly:
> 1793487599
>
> No other blockchain write is authorized.
>
> DO NOT:
>
> - click browser controls yourself
> - activate anything
> - run NFC
> - present the Seeker
> - cold boot hardware
> - start Gate E bridge
> - edit files
> - commit or push
> - retry a transaction after a hash exists
>
> ============================================================
> PHASE 1 — FRESH LOCAL DEMO SERVER
> ============================================================
>
> Check whether the finalized Batch B demo is currently available at:
>
> http://127.0.0.1:4173
>
> Use harmless GET requests only:
>
> GET /
> GET /api/credential
>
> If the existing server is healthy and reports:
>
> guest-001.demo-access.eth
>
> reuse it.
>
> Do NOT start a second instance.
>
> If no server is currently listening, start exactly ONE:
>
> npm run demo
>
> Then repeat harmless GET checks.
>
> Require:
>
> - loopback only
> - credential guest-001.demo-access.eth
> - expected owner
> - expected resolver
> - coherent ACTIVE/ALLOW state
>
> Do not make mutation HTTP requests yourself.
>
> ============================================================
> PHASE 2 — FRESH ONCHAIN SAFETY
> ============================================================
>
> Immediately before asking for the click, require read-only:
>
> repository clean
> HEAD == origin/main ==
> 1b1317e65106bb173d1eedc19ae32fcc34513651
>
> Sepolia chain ID:
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
> true
>
> access.validUntil:
> 1793487599
>
> policy:
> ALLOW
>
> DEV latest == pending == 22
>
> no pending transaction
>
> cred-001 identity intact
>
> If any state differs:
> STOP before the user clicks.
>
> ============================================================
> PHASE 3 — MANUAL USER CLICK
> ============================================================
>
> Once every precondition passes, tell the user exactly:
>
> OPEN http://127.0.0.1:4173 IN YOUR NORMAL BROWSER.
>
> VERIFY IT SHOWS:
>
> guest-001.demo-access.eth
> ENSv2 POLICY: ACTIVE / POLICY ALLOW
>
> THEN CLICK DEACTIVATE EXACTLY ONCE AND DO NOT CLICK IT AGAIN.
>
> The USER performs this manually.
>
> Codex does not use Computer Use.
>
> After the user confirms the click, continue using terminal/RPC evidence only.
>
> ============================================================
> PHASE 4 — TRANSACTION RECONCILIATION
> ============================================================
>
> Observe existing demo server output/runtime evidence for the transaction hash if
> available.
>
> Once a transaction hash exists:
>
> NEVER ask for another click.
> NEVER resubmit.
>
> Capture:
>
> tx hash
> tx nonce
> receipt status
> receipt block
>
> Verify the transaction is the expected guest-001 access.v1 deactivation.
>
> Require:
>
> receipt SUCCESS
>
> If receipt remains pending:
> wait/reconcile read-only only.
>
> No replacement transaction.
>
> ============================================================
> PHASE 5 — AUTHORITATIVE READBACK
> ============================================================
>
> Use the coherent ENS reader.
>
> Require:
>
> guest:
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
> false
>
> access.validUntil:
> 1793487599
>
> policy:
> DENY
>
> snapshot freshness:
> PASS
>
> Require DEV:
>
> latest/pending:
> 23 / 23
>
> no pending transaction
>
> Exactly one nonce-consuming transaction from the pre-click 22/22 baseline.
>
> ============================================================
> PHASE 6 — UI CHECK
> ============================================================
>
> Ask the user only to visually confirm in their normal browser that, after
> refresh/update, it displays:
>
> ENSv2 POLICY:
> INACTIVE / POLICY DENY
>
> Do not use Computer Use.
>
> Do not click anything else.
>
> ============================================================
> STOP
> ============================================================
>
> STOP after confirmed INACTIVE state.
>
> Do NOT yet:
>
> - cold boot
> - preflight hardware
> - present Seeker
> - run Gate E
> - activate
> - perform second blockchain write
>
> RETURN
>
> # REHEARSAL INACTIVE SETUP
>
> ## DEMO SERVER
>
> ## PRE-CLICK STATE
>
> ## MANUAL CLICK
>
> ## DEACTIVATION TX
>
> Include:
>
> hash
> nonce
> receipt block
> receipt status
>
> ## AUTHORITATIVE READBACK
>
> Require:
>
> active=false
> validUntil=1793487599
> policy=DENY
>
> ## NONCE
>
> Require:
>
> 22/22 -> 23/23
>
> ## UI
>
> Report user confirmation of INACTIVE / POLICY DENY.
>
> ## COUNTS
>
> Confirmed blockchain writes:
> 1
>
> Fresh physical taps:
> 0
>
> Challenges:
> 0
>
> Signatures:
> 0
>
> ## GIT STATE
>
> ## NEXT
>
> Cold boot + demo preflight + first fresh physical tap expecting confirmed DENY.
>
> End exactly:
>
> INACTIVE SETUP: PASS
>
> or
>
> INACTIVE SETUP: STOP — <exact reason>
