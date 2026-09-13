# Project task packet 050: TASK — Perform ONLY post-Deactivate transaction reconciliation.

- Classification: **ORIGINAL / SANITIZED**
- Redactions applied: none
- Source: retained project-specific Control Tower packet in the local Codex record

---

> TASK
>
> Perform ONLY post-Deactivate transaction reconciliation.
>
> The user clicked Deactivate exactly ONCE.
>
> Browser Computer Use then stopped before receipt/readback could be verified.
>
> DO NOT click Deactivate again.
> DO NOT click Activate.
> DO NOT use Computer Use.
> DO NOT send any POST/mutation HTTP request.
> DO NOT perform any blockchain write.
> DO NOT run NFC.
> DO NOT reset/flash hardware.
> DO NOT edit files.
> DO NOT commit or push.
> DO NOT stop the existing demo server.
>
> This task is READ-ONLY reconciliation.
>
> KNOWN PRE-CLICK BASELINE
>
> guest-001.demo-access.eth:
>
> REGISTERED
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
> DEV nonce before rehearsal:
> 22 / 22
>
> Repository:
>
> HEAD == origin/main ==
> 1b1317e65106bb173d1eedc19ae32fcc34513651
>
> ============================================================
> PHASE 1 — DO NOT TOUCH THE BROWSER
> ============================================================
>
> Do not use browser automation.
>
> Leave the existing local demo server running.
>
> If useful, harmless GET requests only are allowed:
>
> GET http://127.0.0.1:4173/api/credential
>
> No POST.
>
> ============================================================
> PHASE 2 — NONCE RECONCILIATION
> ============================================================
>
> Read DEV account:
>
> latest nonce
> pending nonce
>
> Record exact values.
>
> Interpret carefully.
>
> Potential expected patterns include:
>
> 22 / 22
> -> no nonce-consuming transaction currently observed
>
> 22 / 23
> -> a nonce-22 transaction may still be pending
>
> 23 / 23
> -> nonce 22 has been mined/consumed
>
> Do not infer the exact transaction solely from nonce.
>
> No transaction may be sent.
>
> ============================================================
> PHASE 3 — FIND EXISTING TX EVIDENCE
> ============================================================
>
> Read-only inspect evidence already produced by the existing demo process/session.
>
> If the server terminal/output contains a transaction hash from the single
> Deactivate click, capture that PUBLIC tx hash.
>
> Do not expose secrets or unrelated process arguments.
>
> If a hash is found:
>
> - query its transaction
> - verify sender is DEV
> - verify nonce
> - verify destination is expected PermissionedResolver
> - verify the transaction corresponds to the guest-001 access.v1 mutation
>   if safely decodable
> - query receipt
> - NEVER replace or resubmit it
>
> If no hash can be recovered, continue using nonce + authoritative state.
>
> ============================================================
> PHASE 4 — AUTHORITATIVE GUEST READBACK
> ============================================================
>
> Use the existing coherent read-only credential path.
>
> Require fresh Sepolia chain 11155111 snapshot.
>
> Record:
>
> status
> owner
> token/resource
> resolver
> registry expiry
> access.active
> access.validUntil
> policy authorization
> snapshot block/timestamp/freshness
>
> Identity must remain unchanged.
>
> validUntil must remain exactly:
>
> 1793487599
>
> ============================================================
> PHASE 5 — CLASSIFY EXACTLY ONE CASE
> ============================================================
>
> CASE A — DEACTIVATION CONFIRMED
>
> Require strong evidence such as:
>
> - receipt SUCCESS for the single click transaction, and/or
> - DEV nonce mined exactly once from 22 -> 23
> - authoritative guest readback:
>   active=false
>   validUntil=1793487599
>   policy DENY
>
> Require final latest == pending and no pending transaction.
>
> Classification:
>
> DEACTIVATION: CONFIRMED
>
> Do NOT continue rehearsal in this task.
>
> Next step will begin from INACTIVE with cold boot/preflight/TAP 1.
>
> CASE B — TRANSACTION STILL PENDING / UNCERTAIN
>
> Examples:
>
> - latest=22, pending=23
> - known tx hash without final receipt
> - inconsistent provider evidence
>
> Classification:
>
> DEACTIVATION: PENDING / UNCERTAIN
>
> If a tx hash exists, use receipt/state polling only for a bounded period.
>
> NO RESUBMISSION.
>
> If it remains uncertain:
> STOP.
>
> CASE C — NO WRITE OCCURRED
>
> Require:
>
> latest=22
> pending=22
> no tx hash/evidence
> authoritative guest remains:
> active=true
> validUntil=1793487599
> policy ALLOW
>
> Classification:
>
> DEACTIVATION: NO WRITE OBSERVED
>
> Do NOT click again in this task.
>
> Return to Control Tower for a separately authorized new click.
>
> CASE D — UNEXPECTED STATE
>
> Any other combination, for example:
>
> - nonce consumed but guest unchanged
> - validUntil changed
> - owner/resolver changed
> - more than one nonce consumed
> - unexpected pending transaction
>
> Classification:
>
> DEACTIVATION: STOP — UNEXPECTED STATE
>
> ============================================================
> PHASE 6 — SAFETY
> ============================================================
>
> Require:
>
> blockchain writes during THIS reconciliation:
> 0
>
> new transaction clicks:
> 0
>
> physical taps:
> 0
>
> fresh challenges:
> 0
>
> fresh signatures:
> 0
>
> source edits:
> 0
>
> Git remains clean.
>
> ============================================================
> RETURN
> ============================================================
>
> # POST-DEACTIVATE RECONCILIATION
>
> ## NONCE
>
> Pre-click baseline:
> 22 / 22
>
> Current latest/pending:
>
> ## EXISTING TX EVIDENCE
>
> Tx hash:
> <hash / none found>
>
> Nonce:
> <value / unknown>
>
> Receipt:
> SUCCESS / PENDING / REVERTED / NOT FOUND / UNKNOWN
>
> ## AUTHORITATIVE GUEST STATE
>
> Status:
> Owner:
> Resolver:
> Token/resource:
> Registry expiry:
> access.active:
> access.validUntil:
> Policy:
> Snapshot:
>
> ## CLASSIFICATION
>
> Exactly one:
>
> DEACTIVATION: CONFIRMED
>
> DEACTIVATION: PENDING / UNCERTAIN
>
> DEACTIVATION: NO WRITE OBSERVED
>
> DEACTIVATION: STOP — UNEXPECTED STATE
>
> ## REHEARSAL COUNTS
>
> Confirmed blockchain writes so far:
> 0 or 1
>
> Transaction clicks:
> 1
>
> Physical taps:
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
> ## NEXT ACTION
>
> If CONFIRMED:
>
> Resume rehearsal from cold boot + preflight + TAP 1 INACTIVE.
> Never click Deactivate again.
>
> If NO WRITE OBSERVED:
>
> Return to Control Tower before any new transaction click.
>
> If PENDING/UNCERTAIN:
>
> No resubmission. Continue receipt/readback reconciliation only.
>
> End exactly:
>
> POST-DEACTIVATE: PASS — CONFIRMED
>
> or
>
> POST-DEACTIVATE: PASS — NO WRITE
>
> or
>
> POST-DEACTIVATE: STOP — <exact reason>
