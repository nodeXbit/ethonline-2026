# Project task packet 039: TASK — Execute ONLY the previously preflighted guest-001 INACTIVE validity renewal on

- Classification: **ORIGINAL / SANITIZED**
- Redactions applied: none
- Source: retained project-specific Control Tower packet in the local Codex record

---

> TASK
>
> Execute ONLY the previously preflighted guest-001 INACTIVE validity renewal on
> Sepolia, then verify authoritative readback.
>
> BY PASTING THIS TASK, THE USER AUTHORIZES AT MOST ONE BLOCKCHAIN TRANSACTION.
>
> AUTHORIZED WRITE
>
> Exactly one possible:
>
> PermissionedResolver.setData
>
> for:
>
> guest-001.demo-access.eth
>
> key:
>
> access.v1
>
> target decoded state:
>
> active=false
> validUntil=1793487599
>
> No other blockchain write is authorized.
>
> DO NOT activate guest-001.
> DO NOT deactivate anything.
> DO NOT modify cred-001.
> DO NOT register/unregister/transfer anything.
> DO NOT deploy.
> DO NOT modify roles.
> DO NOT run NFC/hardware.
> DO NOT edit source.
> DO NOT commit or push.
>
> CURRENT EXPECTED REPOSITORY
>
> HEAD == origin/main ==
>
> 21041a26586bf615130708b5215c477d5c757aa5
>
> Working tree:
> clean
>
> Verify before any write.
>
> If repository state differs:
> STOP.
>
> PREVIOUS READ-ONLY PREFLIGHT
>
> Credential:
>
> guest-001.demo-access.eth
>
> Status:
> REGISTERED
>
> Owner:
> 0x3419148731087b970d2059C53780163B452D5FF7
>
> Resolver:
> 0x0B723c0C2170F2ea508F2f4e3e122C6c0782744C
>
> Current access:
>
> active=false
> validUntil=1789107864
>
> Target:
>
> active=false
> validUntil=1793487599
>
> Target date:
>
> 2026-10-31 23:59:59 Europe/Madrid
> 2026-10-31 22:59:59 UTC
>
> Preflight result:
>
> WRITE REQUIRED
>
> Simulation:
> PASS
>
> Expected maximum writes:
> 1
>
> Previous DEV nonce:
> 20 / 20
>
> Do not blindly rely on old state.
> Perform a fresh safety check first.
>
> ============================================================
> PHASE 1 — FRESH READ-ONLY SAFETY
> ============================================================
>
> Immediately before execution verify:
>
> 1. chain ID == 11155111;
> 2. HEAD == origin/main and git clean;
> 3. DEV latest nonce;
> 4. DEV pending nonce;
> 5. require latest == pending;
> 6. require no pending transaction;
> 7. read guest-001 coherently;
> 8. require:
>    - REGISTERED
>    - owner ==
>      0x3419148731087b970d2059C53780163B452D5FF7
>    - expected resolver
>    - registry expiry valid
>    - access.active == false;
> 9. verify target 1793487599 remains:
>    - > pinned block timestamp
>    - <= registry expiry;
> 10. verify resolver provenance;
> 11. verify DEV setData capability;
> 12. verify guest owner still lacks the configured resolver administrative role;
> 13. verify cred-001 identity remains intact.
>
> If any relevant state differs:
> STOP.
>
> Record fresh baseline latest/pending nonce.
>
> ============================================================
> PHASE 2 — RE-RUN EXACT PREFLIGHT
> ============================================================
>
> Immediately before the write run exactly:
>
> npm run ensv2:access:renew-inactive -- --credential-label guest-001 --valid-until 1793487599 --preflight
>
> Require:
>
> WRITE REQUIRED
>
> or:
>
> NO-OP
>
> If NO-OP because another actor has already established validUntil >=
> 1793487599:
>
> DO NOT send a transaction.
>
> Proceed directly to authoritative readback and report transaction count 0.
>
> If WRITE REQUIRED:
>
> require fresh simulation PASS.
>
> Confirm again:
>
> decoded proposed state:
>
> active=false
> validUntil=1793487599
>
> Maximum writes:
> 1
>
> No broadcast from --preflight.
>
> If simulation fails:
> STOP.
>
> ============================================================
> PHASE 3 — ONE AUTHORIZED WRITE
> ============================================================
>
> Only if still WRITE REQUIRED, run exactly:
>
> npm run ensv2:access:renew-inactive -- --credential-label guest-001 --valid-until 1793487599
>
> Authorize no other command capable of writing.
>
> Requirements:
>
> - existing transaction safety path only;
> - simulate before submission according to current tooling;
> - submit exactly once;
> - capture transaction hash;
> - capture nonce;
> - wait for confirmed successful receipt;
> - capture block number;
> - do not resubmit after a transaction hash exists.
>
> If submission status becomes uncertain after obtaining a hash:
>
> use existing receipt/state recovery logic.
>
> DO NOT broadcast a replacement transaction.
>
> If receipt reverts:
> STOP.
>
> ============================================================
> PHASE 4 — AUTHORITATIVE READBACK
> ============================================================
>
> Using the existing coherent credential reader, require:
>
> guest-001.demo-access.eth
>
> status:
> REGISTERED
>
> owner:
> 0x3419148731087b970d2059C53780163B452D5FF7
>
> tokenId/resource:
> unchanged from established guest identity
>
> resolver:
> 0x0B723c0C2170F2ea508F2f4e3e122C6c0782744C
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
> Madrid:
> 2026-10-31 23:59:59
>
> Authorization:
> DENY
>
> Record coherent verification block and timestamp.
>
> This is the mandatory success condition.
>
> If active becomes true:
> STOP and report a critical invariant failure.
>
> ============================================================
> PHASE 5 — CRED-001 SAFETY
> ============================================================
>
> Read cred-001 again.
>
> Require its established identity invariants remain unchanged.
>
> No access-state requirement beyond not having been targeted by this operation.
>
> ============================================================
> PHASE 6 — NONCE ACCOUNTING
> ============================================================
>
> Record:
>
> baseline latest/pending nonce
>
> write nonce if a transaction occurred
>
> final latest/pending nonce
>
> If one write occurred:
>
> require nonce increase exactly +1.
>
> If NO-OP:
>
> require nonce increase 0.
>
> In all cases:
>
> latest == pending
> no unexplained nonce gap
> no pending transaction
>
> TRANSACTION COUNT must be:
> 0 or 1
>
> Never >1.
>
> ============================================================
> PHASE 7 — STOP
> ============================================================
>
> After successful readback:
>
> STOP.
>
> Do NOT:
>
> - activate guest-001
> - run Gate E bridge
> - flash ESP32
> - present Seeker
> - perform physical proof
> - update docs
> - commit
> - push
>
> RETURN
>
> # GUEST-001 INACTIVE RENEWAL
>
> ## REPO / CHAIN SAFETY
>
> ## FRESH PREFLIGHT
>
> Report:
>
> WRITE REQUIRED / NO-OP
>
> ## WRITE
>
> If sent include:
>
> - contract
> - function
> - tx hash
> - nonce
> - block
> - receipt status
>
> If no-op:
> state explicitly no transaction sent.
>
> ## AUTHORITATIVE GUEST READBACK
>
> Include:
>
> status
> owner
> tokenId/resource
> resolver
> registryExpiry
> access.active
> access.validUntil
> Madrid deadline
> authorization
> verification block
>
> Require:
>
> access.active=false
> access.validUntil=1793487599
> authorization=DENY
>
> ## CRED-001 SAFETY
>
> ## NONCE ACCOUNTING
>
> ## TRANSACTION COUNT
>
> State exactly:
> 0 or 1
>
> ## SECURITY
>
> Confirm:
>
> - no private key printed
> - no RPC URL printed
> - no Privy secret/config printed
> - no guest activation
> - no cred-001 mutation
> - no extra transaction
> - no transaction resubmission
> - no NFC/hardware operation
>
> ## GIT STATE
>
> ## NEXT ACTION
>
> State only:
>
> Perform a fresh read-only ACTIVE-transition preflight before separately
> authorizing one guest-001 activation transaction.
>
> End exactly:
>
> RENEWAL: PASS
>
> or
>
> RENEWAL: STOP — <exact reason>
