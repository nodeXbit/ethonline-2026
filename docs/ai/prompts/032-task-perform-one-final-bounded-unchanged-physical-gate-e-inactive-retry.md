# Project task packet 032: TASK — Perform ONE final bounded unchanged physical Gate E INACTIVE retry.

- Classification: **ORIGINAL / SANITIZED**
- Redactions applied: none
- Source: retained project-specific Control Tower packet in the local Codex record

---

> TASK
>
> Perform ONE final bounded unchanged physical Gate E INACTIVE retry.
>
> ZERO blockchain writes are authorized.
>
> Do NOT edit code before this attempt.
> Do NOT activate/deactivate guest-001.
> Do NOT commit or push.
> Do NOT perform more than one fresh physical signing attempt.
>
> IMPORTANT PHYSICAL CONTROL
>
> The previous run started target activation while the Seeker was already inside
> the PN532 field.
>
> For this retry, the Seeker MUST remain physically away from the PN532 until the
> bridge and firmware are fully ready and the user is explicitly told to present
> it.
>
> Do not start the signing attempt while the phone is already in NFC range.
>
> CURRENT EXPECTED REPOSITORY
>
> HEAD == origin/main ==
> b6159981f1e01195df9183ecc9a535a072edf2c7
>
> Expected unstaged files only:
>
> scripts/security/gate-e-secure-bridge.mjs
> scripts/security/gate-e-secure-bridge.test.mjs
>
> These contain the already-tested serial-finalization fix.
>
> Expected local validation:
>
> Node 104/104 PASS
> Gate E 20/20 PASS
> Android 21/21 PASS
> Gate E firmware compile PASS
>
> PHASE 1 — SAFETY
>
> Read-only confirm:
>
> git status -sb
> git rev-parse HEAD
> git rev-parse origin/main
> git diff --check
>
> Require only the expected two modifications.
>
> Read-only Sepolia require:
>
> - chainId 11155111
> - DEV latest == pending
> - no pending tx
> - guest-001 REGISTERED
> - owner ==
>   0x3419148731087b970d2059C53780163B452D5FF7
> - expected resolver
> - registry expiry valid
> - access.active == false
> - access.validUntil future
> - current authorization DENY
> - cred-001 identity intact
>
> Record pre-run DEV nonce.
>
> No state change.
>
> PHASE 2 — RESET SESSION WITH PHONE AWAY
>
> Tell the user FIRST:
>
> KEEP THE SEEKER AWAY FROM THE PN532 UNTIL I EXPLICITLY ASK YOU TO PRESENT IT.
>
> Require acknowledgement through normal interaction before physical target
> activation.
>
> Reset/reboot the ESP32 once so the one-shot Gate E firmware starts from a clean
> session.
>
> Use the unchanged Gate E firmware already installed if confidently confirmed.
>
> Require:
>
> GATE_E_READY
> PRESENT_SEEKER
>
> Start the current working-tree Node bridge with --check-replay using the actual
> CLI syntax.
>
> The Seeker must still be outside NFC range.
>
> Only when serial listeners, bridge state, and firmware are ready tell the user
> exactly:
>
> PRESENT THE SEEKER TO THE PN532 NOW AND HOLD IT STEADY IN ONE POSITION UNTIL I
> TELL YOU TO REMOVE IT.
>
> PHASE 3 — ONE PHYSICAL ATTEMPT
>
> Require:
>
> TARGET_ACTIVATION: PASS
> SELECT: PASS
> WAITING_CHALLENGE
>
> Only then issue ONE fresh Gate A challenge.
>
> Require:
>
> credential:
> guest-001.demo-access.eth
>
> resource:
> demo-access.eth:door-001
>
> resource bytes32:
> 0xf2bde8f2654ee7267a06860eca03e0935d8818b006f5f51c2fce9d56a9b441cd
>
> challenge count:
> 1
>
> 104-byte payload count:
> 1
>
> Then:
>
> SEND_CHALLENGE: PASS
> count exactly 1
>
> STATUS:
> PROCESSING → READY
> or direct READY
>
> GET_SIGNATURE:
> PASS
>
> PROOF LENGTH:
> 65
>
> Do not move/remove the phone until after the firmware terminal authorization
> result has been observed.
>
> If any ISO-DEP exchange fails:
>
> STOP immediately.
>
> Do not retry.
> Do not change code.
> Do not run a second physical session.
>
> Record the exact command/stage that failed.
>
> PHASE 4 — VALID HOLDER + INACTIVE POLICY
>
> Using existing Gate A + coherent ENS read require:
>
> RECOVERED SIGNER:
> 0x3419148731087b970d2059C53780163B452D5FF7
>
> CURRENT ENS OWNER:
> 0x3419148731087b970d2059C53780163B452D5FF7
>
> OWNER MATCH:
> PASS
>
> CHALLENGE CONSUMED:
> YES
>
> Then require:
>
> guest status:
> REGISTERED
>
> registry expiry:
> VALID
>
> access.active:
> false
>
> access.validUntil:
> VALID
>
> reason:
> ACCESS_DENIED
>
> FINAL AUTHORIZATION:
> DENY — INACTIVE ENS POLICY
>
> Do not accept DENY from:
>
> PROOF_TIMEOUT
> ISO_DEP_ERROR
> INVALID_SIGNATURE
> WRONG_HOLDER
> EXPIRED_CHALLENGE
> RPC_ERROR
> or any other failure reason.
>
> PHASE 5 — SERIAL FINALIZATION REGRESSION
>
> Require Node writes exactly once:
>
> AUTHORIZATION=DENY
>
> The firmware must then emit AFTER that command:
>
> AUTHORIZATION: DENY
>
> The current serial-finalization fix must observe that exact matching line
> before closing COM4.
>
> Require explicit ordered evidence:
>
> 1. Node authorization command sent
> 2. firmware terminal confirmation received
> 3. confirmation matched
> 4. bridge finalized
> 5. serial closed
>
> Report:
>
> AUTHORIZATION COMMAND WRITES: 1
>
> FIRMWARE CONFIRMATION AFTER NODE COMMAND:
> AUTHORIZATION: DENY
>
> CONFIRMATION MATCH:
> PASS
>
> SERIAL CLOSED AFTER CONFIRMATION:
> PASS
>
> An earlier local firmware DENY does NOT count as this confirmation.
>
> PHASE 6 — REPLAY
>
> Using the exact consumed proof and SAME Node store:
>
> require:
>
> REPLAYED_CHALLENGE
> DENY
>
> Require zero:
>
> - new challenges
> - NFC operations
> - signing operations
> - ENS writes
>
> No second physical attempt.
>
> PHASE 7 — FINAL SAFETY
>
> Read-only confirm:
>
> guest-001 still REGISTERED / INACTIVE / DENY
> cred-001 intact
> DEV nonce unchanged
> no pending tx
> zero blockchain writes
>
> Git state must remain the same two unstaged files only.
>
> STOP.
>
> Do not activate.
> Do not commit/push yet.
>
> RETURN
>
> # GATE E INACTIVE CONTROLLED RETRY
>
> ## REPO / ENS PREFLIGHT
> ## CONTROLLED PHONE PRESENTATION
> ## PHYSICAL TRANSPORT
> ## HOLDER PROOF
> ## ENS POLICY
> ## PHYSICAL CONTROLLER CONFIRMATION
> ## REPLAY
> ## NONCE / ONCHAIN STATE
> ## SECURITY
> ## GIT STATE
> ## NEXT ACTION
>
> For PASS require all:
>
> - one controlled physical attempt
> - proof length 65
> - recovered signer == current ENS owner
> - challenge consumed
> - access.active == false
> - ACCESS_DENIED
> - Node sends AUTHORIZATION=DENY exactly once
> - firmware emits AUTHORIZATION: DENY after that Node command
> - bridge observes it before closing serial
> - replay == REPLAYED_CHALLENGE / DENY
> - zero blockchain writes
>
> End exactly:
>
> GATE E INACTIVE: PASS
>
> or
>
> GATE E INACTIVE: STOP — <exact reason>
