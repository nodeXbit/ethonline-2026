# Project task packet 041: TASK — Execute ONLY the previously preflighted guest-001 activation on Sepolia and

- Classification: **ORIGINAL / SANITIZED**
- Redactions applied: none
- Source: retained project-specific Control Tower packet in the local Codex record

---

> TASK
>
> Execute ONLY the previously preflighted guest-001 activation on Sepolia and
> perform authoritative readback.
>
> BY PASTING THIS TASK, THE USER AUTHORIZES AT MOST ONE BLOCKCHAIN TRANSACTION.
>
> AUTHORIZED STATE CHANGE
>
> guest-001.demo-access.eth
>
> access.active:
> false → true
>
> access.validUntil:
> must remain exactly 1793487599
>
> Authorized write:
>
> at most ONE PermissionedResolver.setData transaction targeting guest-001
> access.v1.
>
> NO OTHER BLOCKCHAIN WRITE IS AUTHORIZED.
>
> DO NOT:
>
> - renew validity again
> - deactivate anything
> - modify cred-001
> - register/unregister/transfer
> - modify roles
> - deploy contracts
> - run NFC
> - flash firmware
> - present the Seeker
> - edit source
> - update docs
> - commit
> - push
>
> CURRENT EXPECTED REPOSITORY
>
> HEAD == origin/main ==
> 21041a26586bf615130708b5215c477d5c757aa5
>
> Working tree:
> clean
>
> CURRENT VERIFIED STATE FROM READ-ONLY PREFLIGHT
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
> deadline:
> 2026-10-31 23:59:59 Europe/Madrid
>
> Expected activation target:
>
> active=true
> validUntil=1793487599
>
> Simulation:
> PASS
>
> Expected activation writes:
> 1
>
> Latest known DEV nonce:
> 21 / 21
>
> Do not rely blindly on stale state.
>
> ============================================================
> PHASE 1 — FRESH SAFETY CHECK
> ============================================================
>
> Immediately before any write:
>
> 1. require git clean and HEAD == origin/main;
> 2. require chain ID 11155111;
> 3. query DEV latest and pending nonce;
> 4. require latest == pending;
> 5. require no pending transaction;
> 6. coherently read guest-001;
> 7. require:
>    - REGISTERED
>    - owner ==
>      0x3419148731087b970d2059C53780163B452D5FF7
>    - expected resolver
>    - registry expiry valid
>    - access.active == false
>    - access.validUntil == 1793487599;
> 8. require Batch A snapshot freshness PASS;
> 9. verify resolver provenance;
> 10. verify DEV setData capability;
> 11. verify guest owner still lacks administrative resolver capability;
> 12. verify cred-001 identity remains intact.
>
> If guest is already ACTIVE or any relevant state differs:
>
> STOP.
>
> Do not send a transaction.
>
> Record fresh baseline latest/pending nonce.
>
> ============================================================
> PHASE 2 — FRESH EXACT SIMULATION
> ============================================================
>
> Using the exact existing activation implementation/call, simulate immediately
> before broadcast:
>
> PermissionedResolver.setData
>
> guest-001 node
> key:
> access.v1
>
> decoded target:
>
> active=true
> validUntil=1793487599
>
> Account:
> DEV public address
>
> Require:
>
> SIMULATION: PASS
>
> Confirm:
>
> - only guest node targeted
> - active=true
> - validUntil exactly 1793487599
> - no owner/resolver/registry changes
> - maximum write count remains 1
>
> If simulation fails:
> STOP.
>
> No broadcast during this phase.
>
> ============================================================
> PHASE 3 — ONE AUTHORIZED ACTIVATION
> ============================================================
>
> Run the exact established activation CLI:
>
> npm run ensv2:access:activate -- --credential-label guest-001 --credential-owner 0x3419148731087b970d2059C53780163B452D5FF7
>
> Authorize exactly ONE possible setData broadcast.
>
> Requirements:
>
> - submit once only;
> - capture tx hash;
> - capture nonce;
> - wait for confirmed successful receipt;
> - capture receipt block;
> - do not send a replacement after a hash exists;
> - do not retry broadcast.
>
> If transaction status becomes uncertain after obtaining a hash:
>
> use existing receipt/state recovery only.
>
> DO NOT resubmit.
>
> If receipt reverts:
> STOP.
>
> ============================================================
> PHASE 4 — AUTHORITATIVE READBACK
> ============================================================
>
> After successful receipt, use the coherent credential reader.
>
> Require:
>
> credential:
> guest-001.demo-access.eth
>
> status:
> REGISTERED
>
> owner:
> 0x3419148731087b970d2059C53780163B452D5FF7
>
> token/resource:
> unchanged
>
> resolver:
> 0x0B723c0C2170F2ea508F2f4e3e122C6c0782744C
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
> Madrid expiry:
> 2026-10-31 23:59:59
>
> authorization:
> ALLOW
>
> Require the verification snapshot to pass Batch A freshness checks.
>
> Record:
>
> - verification block
> - verification timestamp
> - token/resource identity
> - final policy result
>
> IMPORTANT:
>
> This proves ENS policy ACTIVE/ALLOW.
>
> Do NOT describe it as physical Gate E ALLOW yet.
>
> ============================================================
> PHASE 5 — CRED-001 SAFETY
> ============================================================
>
> Read-only verify cred-001 established identity remains intact.
>
> Confirm this write affected only guest-001 access.v1.
>
> ============================================================
> PHASE 6 — NONCE ACCOUNTING
> ============================================================
>
> Record:
>
> baseline latest/pending nonce
>
> activation transaction nonce
>
> final latest/pending nonce
>
> Require:
>
> - exactly one nonce-consuming transaction
> - nonce increase exactly +1
> - final latest == pending
> - no pending transaction
> - no unexplained gap
>
> TRANSACTION COUNT:
> exactly 1
>
> If more than one transaction appears:
> STOP / critical failure.
>
> ============================================================
> PHASE 7 — FINAL STOP
> ============================================================
>
> After authoritative ACTIVE readback:
>
> STOP.
>
> Do NOT:
>
> - flash Batch A firmware
> - run Gate E bridge
> - present the phone
> - run replay
> - deactivate
> - update docs
> - commit/push
>
> RETURN
>
> # GUEST-001 ACTIVATION
>
> ## REPO / CHAIN SAFETY
>
> ## FRESH STATE
>
> ## SIMULATION
>
> ## AUTHORIZED WRITE
>
> Include:
>
> contract
> function
> tx hash
> nonce
> receipt block
> receipt status
>
> Do not expose secret configuration.
>
> ## AUTHORITATIVE ACTIVE READBACK
>
> Include:
>
> status
> owner
> token/resource
> resolver
> registryExpiry
> access.active
> access.validUntil
> Madrid expiry
> authorization
> verification block/timestamp
>
> Require:
>
> access.active=true
> access.validUntil=1793487599
> authorization=ALLOW
>
> ## CRED-001 SAFETY
>
> ## NONCE ACCOUNTING
>
> ## TRANSACTION COUNT
>
> Require exactly:
> 1
>
> ## SECURITY
>
> Confirm:
>
> - only guest access.v1 changed
> - no renewal/deactivation/registration/etc.
> - no resubmission
> - no secret exposure
> - zero NFC operations
> - zero hardware flashes
> - UID irrelevant
>
> ## GIT STATE
>
> ## NEXT ACTION
>
> State only:
>
> Flash the committed Batch A Gate E firmware, run a boot/readiness preflight,
> then perform one fresh physical ACTIVE proof expecting end-to-end ALLOW and
> same-proof replay DENY.
>
> End exactly:
>
> ACTIVATION: PASS
>
> or
>
> ACTIVATION: STOP — <exact reason>
