# Project task packet 038: TASK — Perform ONLY the read-only live renewal preflight for:

- Classification: **ORIGINAL / SANITIZED**
- Redactions applied: none
- Source: retained project-specific Control Tower packet in the local Codex record

---

> TASK
>
> Perform ONLY the read-only live renewal preflight for:
>
> guest-001.demo-access.eth
>
> DO NOT broadcast any blockchain transaction.
>
> DO NOT activate/deactivate the credential.
> DO NOT run NFC/hardware.
> DO NOT edit files.
> DO NOT commit or push.
>
> CONTROL TOWER HAS EXPLICITLY CHOSEN THE TARGET VALIDITY
>
> Europe/Madrid:
>
> 2026-10-31 23:59:59
>
> UTC:
>
> 2026-10-31 22:59:59
>
> Unix uint64:
>
> 1793487599
>
> This is the ONLY requested renewal target for this preflight.
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
> First verify:
>
> git status -sb
> git rev-parse HEAD
> git rev-parse origin/main
>
> If not clean/synchronized:
> STOP.
>
> NO SOURCE EDITS.
>
> ============================================================
> PHASE 1 — CHAIN / NONCE / RPC SAFETY
> ============================================================
>
> Read-only verify:
>
> - chain ID == 11155111
> - current pinned block number/timestamp
> - DEV latest nonce
> - DEV pending nonce
> - require latest == pending
> - require no unexplained pending transaction
>
> Do not print RPC URLs.
>
> Report only:
>
> PRIMARY RPC:
> AVAILABLE / FAILED
>
> FALLBACK RPC:
> CONFIGURED / NOT CONFIGURED
>
> Do not expose endpoint values.
>
> This preflight does not require fallback to be configured if the primary
> healthy path passes.
>
> ============================================================
> PHASE 2 — CURRENT GUEST STATE
> ============================================================
>
> Use the existing coherent pinned-block credential reader.
>
> Read:
>
> guest-001.demo-access.eth
>
> Require:
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
> registry expiry:
> VALID
>
> access record:
> validly decoded
>
> access.active:
> false
>
> Record current:
>
> access.validUntil
>
> and its exact UTC + Europe/Madrid date/time.
>
> If access.active is true:
> STOP.
>
> If owner/resolver/status differs:
> STOP.
>
> ============================================================
> PHASE 3 — TARGET DEADLINE VALIDATION
> ============================================================
>
> Validate exact requested deadline:
>
> 1793487599
>
> Require:
>
> requested validUntil
> >
> current pinned block timestamp
>
> and:
>
> requested validUntil
> <=
> guest registry expiry
>
> Report the margin between:
>
> requested deadline
> and
> registry expiry
>
> in days/hours.
>
> Do not substitute or automatically choose another deadline.
>
> If invalid:
> STOP.
>
> ============================================================
> PHASE 4 — CRED-001 SAFETY
> ============================================================
>
> Read-only confirm:
>
> cred-001.demo-access.eth
>
> identity invariants remain intact:
>
> - REGISTERED
> - established DEV owner
> - established token/resource
> - established resolver
> - registry expiry valid
>
> Its access policy does not need to be active.
>
> The renewal command must not target this credential.
>
> ============================================================
> PHASE 5 — OPERATOR / RESOLVER SAFETY
> ============================================================
>
> Using existing project checks, verify:
>
> - resolver bytecode/provenance still valid
> - expected PermissionedResolver implementation
> - DEV still has the required setData capability on the guest node
> - guest owner does NOT have the configured administrative resolver capability
>
> Do not modify roles.
>
> ============================================================
> PHASE 6 — RUN THE EXACT READ-ONLY PREFLIGHT
> ============================================================
>
> Run exactly:
>
> npm run ensv2:access:renew-inactive -- --credential-label guest-001 --valid-until 1793487599 --preflight
>
> This command MUST NOT broadcast.
>
> Capture safe output/evidence.
>
> Require it resolves one of:
>
> A. WRITE REQUIRED
>
> Expected normal case.
>
> or:
>
> B. NO-OP
>
> Only valid if current validUntil is already >= 1793487599.
>
> If WRITE REQUIRED, require:
>
> - target node is guest-001 only
> - resolver is expected resolver
> - key is access.v1
> - encoded proposed record decodes exactly to:
>   active=false
>   validUntil=1793487599
> - transaction simulation PASS
> - maximum future writes: exactly 1
> - no transaction hash exists
> - nonce unchanged
>
> If NO-OP:
> prove current deadline already satisfies the target.
>
> Any ACTIVE refusal, malformed record, simulation failure, wrong credential,
> wrong resolver, nonce issue or provenance failure:
> STOP.
>
> ============================================================
> PHASE 7 — BROADCAST IMPOSSIBILITY CHECK
> ============================================================
>
> After the preflight:
>
> require:
>
> DEV latest nonce == preflight-start latest nonce
>
> DEV pending nonce == preflight-start pending nonce
>
> No tx hash produced.
>
> No receipt produced.
>
> No blockchain write.
>
> guest-001 onchain state remains unchanged.
>
> This is mandatory evidence.
>
> ============================================================
> PHASE 8 — EXPECTED POST-WRITE STATE
> ============================================================
>
> Do NOT create this state now.
>
> State the expected result if a later separately authorized write succeeds:
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
> validUntil Europe/Madrid:
> 2026-10-31 23:59:59
>
> authorization:
> DENY
>
> Credential identity must remain unchanged.
>
> ============================================================
> PHASE 9 — EXACT LATER WRITE BUDGET
> ============================================================
>
> If WRITE REQUIRED:
>
> state:
>
> RENEWAL WRITE BUDGET: 1
>
> Future authorized operation would be exactly one:
>
> PermissionedResolver.setData
>
> for guest-001 access.v1 only.
>
> Do NOT execute it.
>
> If NO-OP:
>
> state:
>
> RENEWAL WRITE BUDGET: 0
>
> ============================================================
> FINAL SAFETY
> ============================================================
>
> Require:
>
> - git still clean
> - HEAD == origin/main
> - cred-001 unchanged
> - guest remains INACTIVE
> - nonce unchanged
> - no pending tx
> - zero broadcasts
> - zero hardware/NFC operations
>
> RETURN
>
> # GUEST-001 RENEWAL PREFLIGHT
>
> ## REPO STATE
>
> ## RPC / CHAIN
>
> ## NONCE
>
> ## CURRENT GUEST STATE
>
> ## TARGET VALIDITY
>
> Include:
>
> Unix:
> 1793487599
>
> Madrid:
> 2026-10-31 23:59:59
>
> UTC:
> 2026-10-31 22:59:59
>
> ## CRED-001 SAFETY
>
> ## RESOLVER / PERMISSIONS
>
> ## RENEW-INACTIVE PREFLIGHT
>
> Report:
>
> WRITE REQUIRED / NO-OP / FAIL
>
> ## SIMULATION
>
> ## EXPECTED POST-WRITE STATE
>
> ## RENEWAL WRITE BUDGET
>
> State exact:
> 0 or 1
>
> ## FINAL SAFETY CHECK
>
> ## USER ACTION REQUIRED
>
> State:
>
> No write has been authorized or executed.
>
> End exactly:
>
> RENEWAL PREFLIGHT: PASS
>
> or
>
> RENEWAL PREFLIGHT: STOP — <exact reason>
