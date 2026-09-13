# Project task packet 034: TASK — Perform ONE controlled Gate E INACTIVE physical validation now that PN532 boot

- Classification: **ORIGINAL / SANITIZED**
- Redactions applied: none
- Source: retained project-specific Control Tower packet in the local Codex record

---

> TASK
>
> Perform ONE controlled Gate E INACTIVE physical validation now that PN532 boot
> initialization has recovered.
>
> ZERO blockchain writes are authorized.
>
> Do NOT edit code.
> Do NOT activate/deactivate guest-001.
> Do NOT commit or push.
> Do NOT perform more than one physical signing attempt.
>
> CURRENT HARDWARE STATE
>
> The latest clean boot on the established CH343 / COM4 path passed:
>
> I2C 0x24: ACK
> PN532 firmware query: PASS
> PN532 firmware: 1.6
> GATE_E_READY: YES
> PRESENT_SEEKER: YES
>
> The previous NO ACK is classified:
>
> TRANSIENT / NOT REPRODUCED
>
> Do not investigate cables, COM3, or the second USB further unless a new
> reproducible hardware failure occurs.
>
> CURRENT REPOSITORY
>
> Expected:
>
> HEAD == origin/main ==
> b6159981f1e01195df9183ecc9a535a072edf2c7
>
> Expected unstaged modifications only:
>
> scripts/security/gate-e-secure-bridge.mjs
> scripts/security/gate-e-secure-bridge.test.mjs
>
> These contain the tested serial-finalization fix.
>
> Expected tests:
>
> Node:
> 104/104 PASS
>
> Gate E:
> 20/20 PASS
>
> Android:
> 21/21 PASS
> assembleDebug PASS
>
> PHASE 1 — READ-ONLY SAFETY
>
> Confirm:
>
> git status -sb
> git rev-parse HEAD
> git rev-parse origin/main
> git diff --check
>
> Require only the two expected unstaged files.
>
> Read-only Sepolia require:
>
> - chainId 11155111
> - DEV latest nonce == pending nonce
> - no pending transaction
> - guest-001.demo-access.eth REGISTERED
> - owner ==
>   0x3419148731087b970d2059C53780163B452D5FF7
> - expected resolver
> - registry expiry valid
> - access.active == false
> - access.validUntil future
> - current authorization DENY
> - cred-001 identity intact
>
> If guest-001 is not valid INACTIVE:
> STOP.
>
> Record pre-run nonce.
>
> PHASE 2 — PHYSICAL SESSION PREPARATION
>
> Tell the user:
>
> KEEP THE SEEKER AWAY FROM THE PN532 UNTIL I EXPLICITLY TELL YOU TO PRESENT IT.
>
> Use the established COM4 / CH343 serial path at 115200.
>
> Use the unchanged Gate E firmware already installed.
>
> If a fresh reset is needed, instruct one short press of RST/EN, NOT BOOT.
>
> Require normal initialization:
>
> I2C 0x24: ACK
> PN532 firmware: 1.6
> GATE_E_READY
> PRESENT_SEEKER
>
> Do not present the phone yet.
>
> Start the current working-tree Gate E bridge with replay checking enabled using
> the actual CLI syntax.
>
> Ensure serial listeners and bridge state are ready before asking for the phone.
>
> PHASE 3 — USER PRESENTATION
>
> Only when everything is ready, tell the user exactly:
>
> PRESENT THE SEEKER TO THE PN532 NOW AND HOLD IT STEADY IN ONE POSITION UNTIL I
> TELL YOU TO REMOVE IT.
>
> The user should keep:
>
> - NFC ON
> - Internet available
> - device unlocked
> - ENSv2 Access Demo open
> - existing Privy session/wallet available
>
> Expected wallet:
>
> 0x3419148731087b970d2059C53780163B452D5FF7
>
> Do not request or capture OTP.
>
> PHASE 4 — ONE FRESH PHYSICAL PROOF
>
> Require:
>
> TARGET_ACTIVATION: PASS
> SELECT: PASS
> WAITING_CHALLENGE
>
> Only after WAITING_CHALLENGE:
>
> issue exactly ONE fresh Gate A challenge.
>
> Require:
>
> credential:
> guest-001.demo-access.eth
>
> resource:
> demo-access.eth:door-001
>
> resource ID:
> 0xf2bde8f2654ee7267a06860eca03e0935d8818b006f5f51c2fce9d56a9b441cd
>
> challenge count:
> 1
>
> challenge payload:
> exactly 104 bytes
>
> Then require:
>
> SEND_CHALLENGE: PASS
> count exactly 1
>
> STATUS:
> PROCESSING → READY
> or READY
>
> GET_SIGNATURE:
> PASS
>
> PROOF LENGTH:
> 65 bytes
>
> Do not manually copy the signature.
>
> If ISO-DEP fails at any point:
>
> STOP.
>
> Do not retry a second physical signing session.
>
> PHASE 5 — HOLDER VERIFICATION
>
> Using existing Gate A require:
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
> Require that the proof passed:
>
> - known issued challenge
> - expiry
> - credential binding
> - resource binding
> - signature validation
> - current-owner match
>
> PHASE 6 — ENS POLICY
>
> Require current coherent ENS snapshot:
>
> status:
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
> Require:
>
> FINAL AUTHORIZATION:
> DENY — INACTIVE ENS POLICY
>
> This must NOT be a denial due to:
>
> PROOF_TIMEOUT
> ISO_DEP_ERROR
> INVALID_SIGNATURE
> WRONG_HOLDER
> EXPIRED_CHALLENGE
> RPC_ERROR
>
> PHASE 7 — PHYSICAL CONTROLLER CONFIRMATION
>
> Require Node sends exactly once:
>
> AUTHORIZATION=DENY
>
> Then require firmware emits AFTER that Node command:
>
> AUTHORIZATION: DENY
>
> The serial-finalization fix must observe this line before closing COM4.
>
> Require ordered evidence:
>
> AUTHORIZATION COMMAND WRITES:
> 1
>
> FIRMWARE CONFIRMATION:
> AUTHORIZATION: DENY
>
> CONFIRMATION MATCH:
> PASS
>
> SERIAL CLOSED AFTER CONFIRMATION:
> PASS
>
> An earlier firmware-local DENY does NOT count.
>
> PHASE 8 — REPLAY
>
> Using the same already-consumed proof and SAME Node store:
>
> require:
>
> REPLAYED_CHALLENGE
> DENY
>
> Require:
>
> new challenges:
> 0
>
> additional NFC operations:
> 0
>
> additional signatures:
> 0
>
> blockchain writes:
> 0
>
> No second physical attempt.
>
> PHASE 9 — FINAL SAFETY
>
> Read-only confirm:
>
> guest-001 remains REGISTERED / INACTIVE / DENY
> cred-001 intact
> DEV nonce unchanged
> no pending transaction
> zero blockchain writes
>
> Git must still contain only the same two unstaged finalization-fix files.
>
> STOP.
>
> Do NOT activate.
> Do NOT commit yet.
>
> RETURN
>
> # GATE E INACTIVE PHYSICAL PASS ATTEMPT
>
> ## REPO / ENS PREFLIGHT
> ## PN532 / SERIAL READINESS
> ## CONTROLLED PHONE PRESENTATION
> ## FRESH CHALLENGE
> ## PHYSICAL TRANSPORT
> ## HOLDER PROOF
> ## ENS POLICY
> ## PHYSICAL CONTROLLER CONFIRMATION
> ## REPLAY
> ## NONCE / STATE
> ## SECURITY
> ## GIT STATE
> ## NEXT ACTION
>
> For PASS require:
>
> - proof length 65
> - recovered signer == current ENS owner
> - challenge consumed
> - access.active == false
> - ACCESS_DENIED
> - Node sends AUTHORIZATION=DENY exactly once
> - firmware emits AUTHORIZATION: DENY afterward
> - bridge captures it before closing serial
> - replay == REPLAYED_CHALLENGE / DENY
> - zero blockchain writes
>
> NEXT ACTION if PASS:
>
> Checkpoint the serial-finalization fix and Gate E INACTIVE physical evidence,
> then authorize exactly one guest-001 activation write.
>
> End exactly:
>
> GATE E INACTIVE: PASS
>
> or
>
> GATE E INACTIVE: STOP — <exact reason>
