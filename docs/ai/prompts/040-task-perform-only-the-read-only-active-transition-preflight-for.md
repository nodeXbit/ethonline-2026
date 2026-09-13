# Project task packet 040: TASK — Perform ONLY the read-only ACTIVE-transition preflight for:

- Classification: **ORIGINAL / SANITIZED**
- Redactions applied: none
- Source: retained project-specific Control Tower packet in the local Codex record

---

> TASK
>
> Perform ONLY the read-only ACTIVE-transition preflight for:
>
> guest-001.demo-access.eth
>
> DO NOT broadcast a blockchain transaction.
> DO NOT activate/deactivate anything.
> DO NOT run NFC/hardware.
> DO NOT flash firmware.
> DO NOT edit files.
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
> CURRENT VERIFIED ONCHAIN STATE
>
> Credential:
>
> guest-001.demo-access.eth
>
> Expected owner:
>
> 0x3419148731087b970d2059C53780163B452D5FF7
>
> Expected resolver:
>
> 0x0B723c0C2170F2ea508F2f4e3e122C6c0782744C
>
> Current expected access state after the completed renewal:
>
> active=false
> validUntil=1793487599
>
> Deadline:
>
> 2026-10-31 23:59:59 Europe/Madrid
> 2026-10-31 22:59:59 UTC
>
> Latest known DEV nonce after renewal:
>
> 21 / 21
>
> Do not trust stale values blindly.
> Re-read current state.
>
> ============================================================
> PHASE 1 — REPO / CHAIN / NONCE
> ============================================================
>
> Verify read-only:
>
> git status -sb
> git rev-parse HEAD
> git rev-parse origin/main
>
> Require clean/synchronized.
>
> Then require:
>
> - chain ID == 11155111
> - primary RPC healthy
> - current pinned block/timestamp
> - DEV latest nonce == pending nonce
> - no pending transaction
>
> Do not print RPC URL.
>
> Record the fresh starting nonce.
>
> ============================================================
> PHASE 2 — CURRENT GUEST STATE
> ============================================================
>
> Using the existing coherent pinned-block reader require:
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
> access.validUntil:
> 1793487599
>
> authorization:
> DENY
>
> Also require the snapshot passes current Batch A freshness checks.
>
> If active is already true:
> STOP.
>
> If validUntil is not exactly 1793487599:
> STOP.
>
> ============================================================
> PHASE 3 — IDENTITY / PERMISSION SAFETY
> ============================================================
>
> Confirm read-only:
>
> - guest token/resource identity unchanged
> - resolver provenance valid
> - expected PermissionedResolver implementation
> - DEV has required setData capability
> - guest owner does NOT have the administrative resolver capability
> - cred-001 established identity remains intact
>
> No role changes.
>
> ============================================================
> PHASE 4 — INSPECT ACTUAL ACTIVATION PATH
> ============================================================
>
> Inspect the CURRENT committed activation implementation.
>
> Determine the exact CLI and exact call it would use for guest-001.
>
> Do NOT guess command flags.
>
> Confirm from source that for:
>
> active=false
> validUntil=1793487599
> and deadline still future
>
> activation will request exactly:
>
> active=true
> validUntil=1793487599
>
> It must PRESERVE the renewed deadline.
>
> It must NOT:
>
> - create a new 24-hour deadline
> - modify owner
> - modify token/resource
> - modify resolver
> - modify registry expiry
> - modify cred-001
> - require more than one transaction.
>
> If current implementation would change validUntil unexpectedly:
> STOP.
>
> ============================================================
> PHASE 5 — READ-ONLY ACTIVATION SIMULATION
> ============================================================
>
> Use the safest existing read-only path.
>
> If the committed activate CLI has an explicit non-broadcast/preflight mode,
> use it.
>
> If it does not, use the existing helpers/publicClient simulation directly to
> simulate the exact activation call.
>
> DO NOT add code just to create a preflight command.
>
> Simulate exactly one:
>
> PermissionedResolver.setData
>
> for:
>
> guest-001 node
> key:
> access.v1
>
> target decoded state:
>
> active=true
> validUntil=1793487599
>
> Account context:
>
> DEV public address.
>
> Require:
>
> SIMULATION:
> PASS
>
> Confirm proposed encoded data decodes exactly to:
>
> active=true
> validUntil=1793487599
>
> Require:
>
> EXPECTED ACTIVATION WRITES:
> 1
>
> No transaction hash.
> No receipt.
> No broadcast.
>
> ============================================================
> PHASE 6 — EXPECTED POST-ACTIVATION STATE
> ============================================================
>
> Do NOT create this state now.
>
> State the exact expected authoritative readback after a future separately
> authorized activation write:
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
> authorization:
> ALLOW
>
> Explain that ALLOW here means the ENS policy state expected from readCredential /
> isAuthorized.
>
> It does NOT yet prove a physical Gate E ALLOW.
>
> ============================================================
> PHASE 7 — NONCE / BROADCAST SAFETY
> ============================================================
>
> After simulation require:
>
> DEV latest nonce ==
> starting latest nonce
>
> DEV pending nonce ==
> starting pending nonce
>
> No pending tx.
> No tx hash.
> No receipt.
> Zero broadcasts.
>
> Guest must remain:
>
> active=false
> validUntil=1793487599
> DENY
>
> ============================================================
> PHASE 8 — NEXT PHYSICAL REQUIREMENT
> ============================================================
>
> Inspect only enough committed Batch A firmware state to remind us whether the
> ESP32 currently requires reflashing before the future physical ACTIVE test.
>
> Batch A changed:
>
> firmware/pn532_secure_access/pn532_secure_access.ino
>
> to add terminal:
>
> GATE_E: STOP - <STAGE>: <PUBLIC_REASON>
>
> Do NOT flash it now.
>
> State:
>
> FIRMWARE REFLASH REQUIRED BEFORE PHYSICAL ALLOW:
> YES / NO
>
> based on whether the currently running physical board can be proven to already
> contain the Batch A firmware.
>
> Do not assume it does.
>
> ============================================================
> FINAL
> ============================================================
>
> No writes.
> No hardware.
> No source edits.
>
> RETURN
>
> # GUEST-001 ACTIVATION PREFLIGHT
>
> ## REPO / CHAIN
>
> ## NONCE
>
> ## CURRENT GUEST STATE
>
> ## IDENTITY / PERMISSIONS
>
> ## ACTIVATION SEMANTICS
>
> Include exact actual CLI/path.
>
> Confirm:
>
> current:
> active=false
> validUntil=1793487599
>
> simulated target:
> active=true
> validUntil=1793487599
>
> ## SIMULATION
>
> PASS / FAIL
>
> ## EXPECTED ACTIVATION WRITE BUDGET
>
> State exact:
> 0 or 1
>
> Expected normal result:
> 1
>
> ## EXPECTED POST-ACTIVATION STATE
>
> ## BROADCAST SAFETY
>
> ## FIRMWARE READINESS
>
> Report:
>
> FIRMWARE REFLASH REQUIRED BEFORE PHYSICAL ALLOW:
> YES / NO
>
> ## USER ACTION REQUIRED
>
> State:
>
> No activation write has been authorized or executed.
>
> End exactly:
>
> ACTIVATION PREFLIGHT: PASS
>
> or
>
> ACTIVATION PREFLIGHT: STOP — <exact reason>
