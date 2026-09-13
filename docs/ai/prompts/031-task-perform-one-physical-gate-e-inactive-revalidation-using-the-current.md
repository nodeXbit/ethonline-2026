# Project task packet 031: TASK — Perform ONE physical Gate E INACTIVE revalidation using the current

- Classification: **ORIGINAL / SANITIZED**
- Redactions applied: none
- Source: retained project-specific Control Tower packet in the local Codex record

---

> TASK
>
> Perform ONE physical Gate E INACTIVE revalidation using the current
> uncommitted serial-finalization fix.
>
> ZERO blockchain writes are authorized.
>
> Do NOT activate or deactivate guest-001.
> Do NOT change ENS state.
> Do NOT edit code before the run.
> Do NOT commit or push.
> Do NOT run more than one fresh physical signing session.
>
> CURRENT LOCAL STATE
>
> Expected pushed baseline:
>
> HEAD == origin/main ==
> b6159981f1e01195df9183ecc9a535a072edf2c7
>
> Expected intentional unstaged modifications only:
>
> scripts/security/gate-e-secure-bridge.mjs
> scripts/security/gate-e-secure-bridge.test.mjs
>
> These contain the already-validated serial-finalization fix.
>
> Expected validation for that fix:
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
> Gate E firmware:
> unchanged
> compile PASS
>
> The fix requires the Node bridge to wait for the exact matching firmware
> terminal line after sending the authorization decision:
>
> Node sends:
> AUTHORIZATION=DENY
>
> Firmware replies:
> AUTHORIZATION: DENY
>
> Only after that matching confirmation may the serial session finalize.
>
> PHASE 1 — REPOSITORY SAFETY
>
> Run read-only:
>
> git status -sb
> git rev-parse HEAD
> git rev-parse origin/main
> git diff --check
>
> Require:
>
> HEAD == origin/main ==
> b6159981f1e01195df9183ecc9a535a072edf2c7
>
> and only the two expected unstaged bridge/test modifications.
>
> If any unexpected change exists:
> STOP.
>
> Do not discard the serial-finalization fix.
>
> PHASE 2 — LIVE READ-ONLY ENS SAFETY
>
> Before hardware interaction:
>
> 1. verify chain ID 11155111;
> 2. verify DEV latest nonce == pending nonce;
> 3. require no pending transaction;
> 4. read guest-001.demo-access.eth through the existing coherent read path;
> 5. require:
>    - REGISTERED
>    - owner ==
>      0x3419148731087b970d2059C53780163B452D5FF7
>    - expected resolver
>    - registry expiry valid
>    - access.active == false
>    - access.validUntil still future
>    - authorization currently DENY;
> 6. confirm cred-001 identity invariants remain intact.
>
> If guest-001 is no longer valid INACTIVE state:
> STOP.
>
> Do not repair state.
>
> Record the fresh pre-run DEV nonce.
>
> PHASE 3 — FIRMWARE / SERIAL READINESS
>
> Use the unchanged committed:
>
> firmware/pn532_secure_access/pn532_secure_access.ino
>
> If the ESP32 is already running the exact Gate E firmware from the previous
> physical test and this can be established confidently, do not reflash merely
> for ritual.
>
> Otherwise compile and flash the unchanged Gate E sketch to ESP32-S3 on the
> currently confirmed port.
>
> No firmware source edit.
>
> Prepare the Node bridge using the current working-tree serial-finalization fix.
>
> Replay checking must be enabled.
>
> Use the actual committed/current CLI syntax from source.
>
> PHASE 4 — PHONE READINESS
>
> Tell the user only when terminal/bridge setup is ready:
>
> Prepare the Seeker:
>
> - NFC ON
> - Internet available
> - device unlocked
> - ENSv2 Access Demo open
> - authenticated Privy session
> - existing embedded Ethereum wallet available
>
> Expected wallet:
>
> 0x3419148731087b970d2059C53780163B452D5FF7
>
> Do not request OTP.
>
> If login is required, allow the normal user-entered OTP flow without capturing
> or printing it.
>
> Do not create another wallet.
>
> PHASE 5 — START BRIDGE
>
> Start ONE live Gate E bridge attempt with replay validation enabled.
>
> The same Node process must own the same IssuedChallengeStore from issuance
> through:
>
> - first verification
> - authorization decision
> - firmware confirmation
> - replay check
>
> Require normal readiness.
>
> When the system is actually waiting for the phone, tell the user exactly:
>
> PRESENT THE SEEKER TO THE PN532 NOW AND HOLD IT STEADY UNTIL I TELL YOU TO REMOVE IT
>
> Do not ask the user to relay serial text manually if Codex can observe it
> directly.
>
> PHASE 6 — ONE FRESH PHYSICAL PROOF
>
> Require in order:
>
> TARGET_ACTIVATION: PASS
>
> SELECT: PASS
>
> WAITING_CHALLENGE
>
> Only then issue exactly ONE fresh Gate A challenge.
>
> Require:
>
> - credential binding:
>   guest-001.demo-access.eth
> - resource binding:
>   demo-access.eth:door-001
> - resource bytes32:
>   0xf2bde8f2654ee7267a06860eca03e0935d8818b006f5f51c2fce9d56a9b441cd
> - fresh secure nonce
> - expiry valid
>
> Node sends exactly one 104-byte challenge.
>
> Firmware:
>
> SEND_CHALLENGE: PASS
>
> SEND_CHALLENGE must occur exactly once.
>
> Then require:
>
> STATUS:
> PROCESSING → READY
> or READY if already completed
>
> GET_SIGNATURE:
> PASS
>
> PROOF LENGTH:
> 65 bytes
>
> Proof must arrive through the physical PN532 ↔ Seeker path.
>
> No manual signature copy.
>
> PHASE 7 — HOLDER + ENS VERIFICATION
>
> Require evidence from existing Gate A + coherent live ENS read:
>
> - stored challenge known
> - challenge unexpired
> - credential binding valid
> - resource binding valid
> - signature valid
>
> RECOVERED SIGNER:
>
> 0x3419148731087b970d2059C53780163B452D5FF7
>
> CURRENT ENS OWNER:
>
> 0x3419148731087b970d2059C53780163B452D5FF7
>
> OWNER MATCH:
> PASS
>
> CHALLENGE CONSUMED:
> YES
>
> Then require current policy:
>
> credential:
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
> Decision reason:
> ACCESS_DENIED / existing equivalent meaning valid holder but policy denied.
>
> Require:
>
> FINAL AUTHORIZATION:
> DENY — INACTIVE ENS POLICY
>
> The DENY must NOT be caused by:
>
> - wrong signer
> - expired challenge
> - wrong binding
> - malformed proof
> - replay
> - RPC failure.
>
> PHASE 8 — PHYSICAL CONTROLLER CONFIRMATION
>
> This phase is the exact regression target.
>
> Require Node sends exactly once:
>
> AUTHORIZATION=DENY
>
> Then the serial-finalization fix must keep COM4 open.
>
> Require the firmware emits:
>
> AUTHORIZATION: DENY
>
> Require Node observes the exact matching firmware line BEFORE detaching
> listeners or closing COM4.
>
> Record explicitly:
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
> YES
>
> Do not accept inference.
>
> The literal firmware line must be observed by the bridge.
>
> If the opposite line appears:
> STOP fail-closed.
>
> If FIRMWARE_CONFIRMATION_TIMEOUT occurs:
> STOP.
>
> Do not repeat the physical signing session.
>
> PHASE 9 — SAME-PROOF REPLAY
>
> Using the same Node process/store and exact already-consumed proof:
>
> perform the implemented --check-replay validation.
>
> No new NFC interaction.
> No new signing.
> No new challenge.
> No ESP32 reset.
> No new proof.
>
> Require:
>
> REPLAYED_CHALLENGE
>
> Authorization:
> DENY
>
> Also require:
>
> new challenges:
> 0
>
> additional NFC/signing:
> 0
>
> blockchain writes:
> 0
>
> If replay is recognized before a second ENS read as designed, record that.
>
> PHASE 10 — FINAL READ-ONLY SAFETY
>
> After the physical run verify:
>
> guest-001 still:
>
> REGISTERED
> owner == Privy EOA
> access.active == false
> authorization == DENY
>
> cred-001 identity intact.
>
> DEV latest/pending nonce:
>
> must equal the pre-run nonce
> and have no pending transaction.
>
> Require exactly zero blockchain writes.
>
> Git must still contain only the existing two uncommitted finalization-fix files.
>
> PHASE 11 — STOP
>
> After evidence collection:
>
> STOP.
>
> Do NOT:
>
> - activate guest-001
> - perform another physical proof
> - modify code
> - commit
> - push
> - update project docs yet
>
> RETURN
>
> # GATE E INACTIVE PHYSICAL REVALIDATION
>
> ## REPO STATE
>
> ## LIVE ENS PREFLIGHT
>
> ## FIRMWARE / PHONE READINESS
>
> ## FRESH CHALLENGE
>
> ## PHYSICAL TRANSPORT
>
> Report:
>
> TARGET ACTIVATION
> SELECT
> WAITING_CHALLENGE
> SEND_CHALLENGE count
> STATUS
> GET_SIGNATURE
> PROOF LENGTH
>
> ## HOLDER PROOF
>
> Report:
>
> RECOVERED SIGNER
> CURRENT ENS OWNER
> OWNER MATCH
> CHALLENGE CONSUMED
>
> ## ENS POLICY
>
> Report:
>
> status
> registry expiry
> access.active
> access.validUntil
> denial reason
>
> Require:
>
> FINAL AUTHORIZATION:
> DENY — INACTIVE ENS POLICY
>
> ## PHYSICAL CONTROLLER RESULT
>
> Report exactly:
>
> Node command:
> AUTHORIZATION=DENY
>
> Firmware line observed:
> AUTHORIZATION: DENY
>
> Confirmation match:
> PASS
>
> Serial closed only after firmware confirmation:
> PASS
>
> ## REPLAY CHECK
>
> Require:
>
> REPLAYED_CHALLENGE
> DENY
>
> Confirm zero:
>
> - new challenge
> - new NFC operation
> - new signature
> - blockchain writes
>
> ## NONCE / ONCHAIN STATE
>
> ## SECURITY
>
> Confirm:
>
> - UID unused
> - no secret/OTP/private-key exposure
> - proof not committed/stored
> - APDU v1 unchanged
> - cred-001 unchanged
> - zero blockchain writes
>
> ## GIT STATE
>
> ## NEXT ACTION
>
> State only:
>
> Checkpoint the serial-finalization fix and Gate E INACTIVE physical evidence,
> then authorize exactly one guest-001 activation transaction.
>
> End exactly:
>
> GATE E INACTIVE: PASS
>
> or
>
> GATE E INACTIVE: STOP — <exact reason>
