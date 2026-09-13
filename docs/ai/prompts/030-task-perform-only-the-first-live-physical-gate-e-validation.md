# Project task packet 030: TASK — Perform ONLY the first live physical Gate E validation:

- Classification: **ORIGINAL / SANITIZED**
- Redactions applied: none
- Source: retained project-specific Control Tower packet in the local Codex record

---

> TASK
>
> Perform ONLY the first live physical Gate E validation:
>
> Privy-owned guest-001 credential
> + fresh server-issued physical holder proof
> + current ENSv2 INACTIVE policy
> → physical DENY.
>
> Also verify replay rejection using the SAME consumed proof.
>
> ZERO blockchain writes are authorized.
>
> Do NOT activate or deactivate anything.
> Do NOT provision anything.
> Do NOT modify ENS state.
> Do NOT commit or push.
>
> CURRENT VERIFIED ONCHAIN STATE
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
> Token ID:
> 9797450251103362342421957848157950028437726839506502066019935098029798850560
>
> Resource:
> 9797450251103362342421957848157950028437726839506502066019935098029798850560
>
> Resolver:
> 0x0B723c0C2170F2ea508F2f4e3e122C6c0782744C
>
> Registry expiry:
> 1820557476
>
> access.active:
> false
>
> access.validUntil:
> 1789107864
>
> Authorization:
> DENY
>
> Provisioning verification block:
> 11673176
>
> Privy embedded EOA:
>
> 0x3419148731087b970d2059C53780163B452D5FF7
>
> Provisioning transactions already completed:
>
> setData:
> 0x34c47584a377bf6d77428d19a946a322d9d31fb040caf2bf40dc205bf97112bf
>
> register:
> 0x964e488bb056b86a72870251a46e91f569f4915fc102f2aeb573b71ae202f5da
>
> Post-provision DEV nonce:
> 20 / 20
>
> No pending transaction.
>
> CURRENT REPOSITORY
>
> Expected:
>
> HEAD == origin/main ==
> b6159981f1e01195df9183ecc9a535a072edf2c7
>
> Working tree:
> clean
>
> Gate E implementation is already committed and pushed.
>
> PHASE 1 — READ-ONLY SAFETY CHECK
>
> Before hardware work:
>
> 1. verify git state remains clean/synchronized;
> 2. verify chain ID 11155111;
> 3. verify DEV latest == pending nonce;
> 4. require no pending transaction;
> 5. read guest-001 using the existing coherent credential reader;
> 6. require:
>    - REGISTERED
>    - owner == Privy EOA
>    - resolver expected
>    - registry expiry valid
>    - access.active == false
>    - access.validUntil still future
>    - authorization DENY;
> 7. confirm cred-001 identity remains intact.
>
> If guest-001 is no longer INACTIVE or its access record has expired:
> STOP.
>
> Do not change state.
>
> PHASE 2 — CONFIRM LIVE BRIDGE CONFIGURATION
>
> Inspect the committed Gate E bridge/CLI only as necessary to determine the
> exact invocation for:
>
> credential:
> guest-001.demo-access.eth
>
> resource:
> demo-access.eth:door-001
>
> physical serial device:
> the current ESP32 port
>
> replay check:
> enabled
>
> Do not guess CLI flags.
>
> Use the actual committed implementation.
>
> The live process must NOT:
>
> - create a wallet client
> - load/use DEV_PRIVATE_KEY
> - sign a blockchain transaction
> - call any ENS write function.
>
> It may use the existing dedicated Sepolia RPC configuration read-only.
>
> Do not print RPC URL/config.
>
> PHASE 3 — GATE E FIRMWARE
>
> Use the committed:
>
> firmware/pn532_secure_access/pn532_secure_access.ino
>
> Compile it with the already validated ESP32-S3 / Elechouse toolchain.
>
> Confirm the current ESP32 serial port read-only.
>
> Flash the Gate E firmware.
>
> This firmware upload is authorized.
>
> No source edit is authorized.
>
> After flash, prepare direct serial/bridge orchestration.
>
> PHASE 4 — PHONE READINESS
>
> Before starting the one physical proof, tell the user to prepare the Seeker:
>
> - NFC ON
> - Internet available
> - device unlocked
> - ENSv2 Access Demo open
> - Privy session authenticated
> - existing Ethereum wallet available
>
> Expected wallet:
>
> 0x3419148731087b970d2059C53780163B452D5FF7
>
> If session/login is missing, guide only the normal manual email OTP login.
>
> Never request or capture the OTP.
>
> Do not create a different wallet.
>
> PHASE 5 — START LIVE GATE E BRIDGE
>
> Start the committed Gate E secure bridge with replay checking enabled using
> the exact actual CLI syntax discovered from source.
>
> The same Node process must own the same Gate A IssuedChallengeStore from
> issuance through verification and replay check.
>
> Require firmware/serial readiness equivalent to:
>
> GATE_E_READY
> PRESENT_SEEKER
>
> Tell the user exactly:
>
> PRESENT THE SEEKER TO THE PN532 NOW AND HOLD IT STEADY UNTIL THE RESULT IS SHOWN
>
> PHASE 6 — PHYSICAL HOLDER PROOF
>
> Require the real flow:
>
> TARGET_ACTIVATION: PASS
> SELECT: PASS
> WAITING_CHALLENGE
>
> Only then:
>
> Node issues one fresh Gate A challenge.
>
> Require:
>
> - server-issued challenge
> - fresh cryptographic nonce
> - credential == guest-001 namehash
> - resource == demo-access.eth:door-001 resource ID
> - expiry currently valid
>
> Node sends exactly one 104-byte challenge to ESP32.
>
> Then require:
>
> SEND_CHALLENGE: PASS
>
> No second SEND_CHALLENGE.
>
> Then:
>
> STATUS:
> PROCESSING → READY
> or direct READY if signing completes before first poll.
>
> GET_SIGNATURE:
> PASS
>
> Require:
> 65-byte proof.
>
> The proof must come from the physical PN532 ↔ Seeker HCE path.
>
> Do not manually copy the signature.
>
> PHASE 7 — HOLDER VERIFICATION + ENS DENY
>
> Node must verify using the existing Gate A verifier and live coherent ENS read.
>
> Require evidence that:
>
> 1. challenge is known/server-issued;
> 2. challenge is not expired;
> 3. credential matches;
> 4. resource matches;
> 5. signature cryptographically verifies;
> 6. recovered signer is:
>
>    0x3419148731087b970d2059C53780163B452D5FF7
>
> 7. CURRENT ENS owner is:
>
>    0x3419148731087b970d2059C53780163B452D5FF7
>
> 8. signer == CURRENT ENS owner;
> 9. valid holder proof transitions challenge to CONSUMED;
> 10. current ENS policy reads access.active == false;
> 11. final authorization is DENY.
>
> Require the bridge sends:
>
> AUTHORIZATION=DENY
>
> and firmware prints:
>
> AUTHORIZATION: DENY
>
> This must be an ENS policy denial.
>
> It must NOT be caused by:
> - wrong signer
> - unknown challenge
> - expired challenge
> - wrong resource
> - malformed proof
> - RPC failure.
>
> PHASE 8 — SAME-PROOF REPLAY
>
> Using the already implemented replay-check path in the SAME Node process:
>
> verify the exact same:
> - challenge
> - nonce
> - physical signature
>
> again.
>
> Do NOT:
>
> - perform another NFC operation
> - issue another challenge
> - ask Privy to sign again
> - reset ESP32
> - read/send a new proof.
>
> Require:
>
> REPLAYED_CHALLENGE
>
> and:
>
> DENY.
>
> Confirm:
>
> - zero new challenges;
> - zero additional NFC signing operations;
> - zero blockchain writes.
>
> If existing implementation avoids a second ENS read after recognizing replay,
> record that evidence.
>
> PHASE 9 — FINAL ONCHAIN / NONCE CHECK
>
> Read-only require:
>
> guest-001 still:
>
> REGISTERED
> owner = Privy EOA
> access.active = false
> AUTHORIZATION = DENY
>
> DEV latest/pending nonce must be unchanged from the fresh pre-run value.
>
> No pending transactions.
>
> cred-001 unchanged.
>
> PHASE 10 — CLEANUP
>
> Stop the Node bridge/serial capture cleanly.
>
> Do not modify code.
> Do not update docs yet.
> Do not commit/push.
>
> RETURN
>
> # GATE E INACTIVE PHYSICAL DENY
>
> ## REPO / CHAIN PREFLIGHT
>
> ## GUEST-001 BASELINE
>
> ## PHONE / FIRMWARE READINESS
>
> ## FRESH CHALLENGE
>
> Include:
> - credential binding
> - resource binding
> - expiry validity
> - do NOT print secret data (nonce is public proof metadata but no need to
>   expose it unless useful)
>
> ## PHYSICAL TRANSPORT
>
> Report:
>
> TARGET ACTIVATION
> SELECT
> WAITING_CHALLENGE
> SEND_CHALLENGE
> STATUS
> GET_SIGNATURE
> PROOF LENGTH
>
> ## HOLDER PROOF
>
> Report:
>
> RECOVERED SIGNER:
> 0x...
>
> CURRENT ENS OWNER:
> 0x...
>
> OWNER MATCH:
> PASS / FAIL
>
> CHALLENGE CONSUMED:
> YES / NO
>
> ## ENS POLICY
>
> Report:
>
> credential status
> registry expiry validity
> access.active
> access.validUntil validity
> final reason
>
> Require:
>
> FINAL AUTHORIZATION:
> DENY — INACTIVE ENS POLICY
>
> ## FIRMWARE RESULT
>
> Require:
>
> AUTHORIZATION: DENY
>
> ## REPLAY CHECK
>
> Require:
>
> REPLAYED_CHALLENGE
> DENY
>
> Confirm:
> - no new challenge
> - no new NFC/signing
> - no blockchain write
>
> ## NONCE / STATE ACCOUNTING
>
> ## SECURITY
>
> Confirm:
> - UID unused
> - zero blockchain writes
> - no private key/App Secret/OTP/token exposure
> - no signature committed/stored
> - APDU v1 unchanged
> - cred-001 unchanged
>
> ## GIT STATE
>
> ## NEXT ACTION
>
> State only:
>
> Authorize one guest-001 activation write, then repeat with a fresh physical
> challenge expecting ALLOW.
>
> End exactly:
>
> GATE E INACTIVE: PASS
>
> or
>
> GATE E INACTIVE: STOP — <exact reason>
