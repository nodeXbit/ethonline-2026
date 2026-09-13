# Project task packet 046: TASK — Checkpoint ONLY the completed Gate E ACTIVE end-to-end physical validation and

- Classification: **ORIGINAL / SANITIZED**
- Redactions applied: none
- Source: retained project-specific Control Tower packet in the local Codex record

---

> TASK
>
> Checkpoint ONLY the completed Gate E ACTIVE end-to-end physical validation and
> the associated already-completed onchain preparation evidence.
>
> This is a documentation/state checkpoint.
>
> The user explicitly authorizes:
>
> - read-only repository/state review
> - scoped STATUS.md / WORKLOG.md / DECISIONS.md updates
> - PROJECT.md only if existing state claims are now stale
> - one documentation commit
> - push to origin/main
>
> DO NOT:
>
> - modify production/test code
> - modify firmware
> - run NFC again
> - perform blockchain writes
> - deactivate guest-001
> - renew or activate anything
> - update README yet
> - add Batch B implementation
> - commit unrelated files
>
> CURRENT EXPECTED REPOSITORY
>
> HEAD == origin/main ==
> 21041a26586bf615130708b5215c477d5c757aa5
>
> Working tree:
> clean
>
> Verify:
>
> git status -sb
> git rev-parse HEAD
> git rev-parse origin/main
>
> If state differs:
> STOP.
>
> ============================================================
> EVIDENCE TO RECORD
> ============================================================
>
> SECURE CREDENTIAL
>
> guest-001.demo-access.eth
>
> Owner:
>
> 0x3419148731087b970d2059C53780163B452D5FF7
>
> Resolver:
>
> 0x0B723c0C2170F2ea508F2f4e3e122C6c0782744C
>
> Token/resource:
>
> 9797450251103362342421957848157950028437726839506502066019935098029798850560
>
> Registry expiry:
>
> 1820557476
>
> ============================================================
> VALIDITY RENEWAL
> ============================================================
>
> Completed authorized guest-only validity renewal:
>
> tx:
> 0xecbf343cc3c6a26a599bc46c789d273e33354b950767765c27b4dba0b0508e4e
>
> nonce:
> 20
>
> result:
>
> access.active=false
> access.validUntil=1793487599
>
> Deadline:
>
> 2026-10-31 23:59:59 Europe/Madrid
>
> The renewal preserved INACTIVE and did not touch credential identity or
> cred-001.
>
> ============================================================
> ACTIVATION
> ============================================================
>
> Completed authorized activation:
>
> tx:
> 0xafd5cb1c2ebe849ff65ed934ab2fb9da5eb1fe2df40370c3b1d43b25b97fa506
>
> nonce:
> 21
>
> result:
>
> access.active=true
> access.validUntil=1793487599
>
> Authoritative onchain policy after activation:
>
> ALLOW
>
> Activation preserved the renewed validity deadline.
>
> ============================================================
> BATCH A FIRMWARE / COLD BOOT
> ============================================================
>
> Committed Batch A firmware was flashed successfully.
>
> Established hardware:
>
> - ESP32-S3
> - Elechouse-compatible PN532 / PN532_I2C
> - SDA GPIO17
> - SCL GPIO18
> - I2C address 0x24
> - Serial0 115200
> - CH343 / COM4
> - PN532 firmware 1.6
>
> Batch A firmware includes:
>
> GATE_E: STOP - <STAGE>: <PUBLIC_REASON>
>
> before firmware-local AUTHORIZATION: DENY.
>
> Verified complete cold boot:
>
> - all USB/power removed
> - CH343 disappearance observed
> - approximately 10-second unpowered interval
> - established CH343 path reconnected
> - one RST/EN press was required because passive capture had no output
> - I2C 0x24 ACK
> - PN532 query PASS
> - PN532 firmware 1.6
> - GATE_E_READY
> - PRESENT_SEEKER
>
> Operational demo rule:
>
> FULL COLD BOOT BEFORE PHYSICAL GATE E SESSION.
>
> Do not claim this explains the underlying intermittent PN532 cause.
>
> ============================================================
> GATE E ACTIVE PHYSICAL PASS
> ============================================================
>
> One controlled physical attempt using committed Batch A code/firmware.
>
> Pre-run:
>
> - Sepolia 11155111
> - guest REGISTERED
> - expected owner/resolver
> - access.active=true
> - access.validUntil=1793487599
> - policy ALLOW
> - DEV nonce 22/22
> - no pending tx
>
> Physical path:
>
> - TARGET_ACTIVATION PASS
> - SELECT PASS
> - WAITING_CHALLENGE
> - exactly one fresh Gate A challenge
> - credential:
>   guest-001.demo-access.eth
> - resource:
>   demo-access.eth:door-001
> - resource ID:
>   0xf2bde8f2654ee7267a06860eca03e0935d8818b006f5f51c2fce9d56a9b441cd
> - exact 104-byte challenge payload
> - SEND_CHALLENGE exactly once
> - PROCESSING -> READY
> - GET_SIGNATURE PASS
> - proof length 65 bytes
> - raw proof redacted from logs
>
> Holder proof:
>
> Recovered signer:
>
> 0x3419148731087b970d2059C53780163B452D5FF7
>
> Current ENS owner:
>
> 0x3419148731087b970d2059C53780163B452D5FF7
>
> OWNER MATCH:
> PASS
>
> Challenge consumed:
> YES
>
> ENS authorization:
>
> - coherent one-provider snapshot
> - freshness PASS
> - REGISTERED
> - registry expiry valid
> - access.active=true
> - access.validUntil=1793487599
> - VERIFIER_ALLOW
> - verification completed inside Batch A ENS/attempt budgets
>
> Physical controller:
>
> Node sent exactly once:
>
> AUTHORIZATION=ALLOW
>
> Firmware subsequently emitted:
>
> AUTHORIZATION: ALLOW
>
> Confirmation:
> PASS
>
> Controller:
> CONTROLLER_CONFIRMED
>
> Serial closed only after matching confirmation.
>
> Bridge exit:
> success.
>
> Replay:
>
> same consumed proof
> -> REPLAYED_CHALLENGE
> -> DENY
>
> Replay required:
>
> - zero new challenges
> - zero NFC operations
> - zero signatures
> - zero ENS reads
> - zero blockchain writes
>
> Physical ACTIVE run itself:
>
> zero blockchain writes.
>
> Post-run:
>
> DEV nonce:
> 22/22
>
> guest still ACTIVE / ALLOW.
>
> cred-001 identity intact.
>
> ============================================================
> WHAT GATE E NOW PROVES
> ============================================================
>
> Record precisely:
>
> Gate E end-to-end secure authorization is physically validated.
>
> A fresh EIP-712 proof produced by the Privy wallet currently owning
> guest-001.demo-access.eth is physically transported through Android HCE /
> ISO-DEP / PN532 / ESP32, verified by Gate A, evaluated against current coherent
> ENSv2 ownership and access policy, and the resulting decision is delivered to
> and confirmed by the physical controller.
>
> Validated policy behavior now includes:
>
> valid holder + INACTIVE
> -> physical DENY
>
> valid holder + ACTIVE
> -> physical ALLOW
>
> consumed proof replay
> -> DENY
>
> Static NFC UID has no authorization role.
>
> ============================================================
> LIMITATIONS TO RECORD
> ============================================================
>
> Do NOT oversell.
>
> Record explicitly:
>
> - prototype trusts local Node/USB/controller environment;
> - no relay-resistance claim;
> - no production hardware security claim;
> - no physical lock/relay actuator is currently part of the validated path;
> - physical hardware has shown intermittent PN532/I2C/ISO-DEP failures;
> - current operational mitigation includes controlled setup and full cold boot;
> - fallback RPC is optional and was not configured in the successful ACTIVE run;
> - successful ACTIVE run used PRIMARY RPC.
>
> ============================================================
> STATUS.md
> ============================================================
>
> Set:
>
> GATE E ACTIVE END-TO-END:
> PASS
>
> GATE E SECURE PATH:
> PASS
>
> Batch A:
> PASS
>
> Record current onchain state:
>
> guest-001:
> ACTIVE
> validUntil=1793487599
> policy ALLOW
>
> State next objective:
>
> BATCH B — DEMO RELIABILITY
>
> Goals:
>
> - sub-2-minute deterministic pre-demo health check
> - align demo with guest-001
> - distinguish POLICY / VERIFIER / CONTROLLER
> - preserve proof redaction
> - operational cold-boot/runbook
> - controlled repeatability/revocation rehearsal
>
> ============================================================
> WORKLOG.md
> ============================================================
>
> Record concise chronological evidence:
>
> 1. guest renewal
> 2. activation
> 3. Batch A firmware flash
> 4. verified cold-boot readiness
> 5. one complete physical ACTIVE ALLOW
> 6. controller confirmation
> 7. replay DENY
> 8. zero physical-run writes
>
> Technical lessons:
>
> - physical authorization is not proven until controller confirmation;
> - policy ACTIVE is not itself proof of holder possession;
> - NFC transports proof; Node performs blockchain authorization;
> - whole-snapshot ENS freshness and attempt deadlines protect against late/stale
>   ALLOW;
> - cold boot is currently an operational reliability measure, not a proven
>   hardware root-cause fix.
>
> ============================================================
> DECISIONS.md
> ============================================================
>
> Record accepted decisions:
>
> - Gate E secure path is now technically closed.
> - guest-001 is the canonical secure demo credential.
> - validity horizon is 2026-10-31 23:59:59 Europe/Madrid.
> - static UID excluded from authorization.
> - Batch A timing/freshness/STOP rules remain required.
> - full cold boot is the current pre-session hardware runbook.
> - physical success requires controller confirmation.
> - replay rejection remains mandatory.
> - no PC/SC / PN532-library / APDU redesign is justified.
> - next work is demo reliability, not secure-protocol expansion.
>
> ============================================================
> PROJECT.md
> ============================================================
>
> Update only if current statements still claim:
>
> - Gate E ACTIVE is pending
> - Gate E secure authorization is unproven
> - guest provisioning/activation is pending
>
> Keep architecture description stable.
>
> ============================================================
> VALIDATION
> ============================================================
>
> Before commit:
>
> - inspect documentation diff
> - git diff --check
> - ensure only expected project-control docs changed
> - no real proof/signature
> - no OTP/token
> - no private key
> - no RPC URL
> - no local config
> - public tx hashes/addresses are acceptable evidence
> - no source changes
> - no blockchain writes
>
> No need to rerun full software suites solely for documentation if source tree is
> unchanged.
>
> ============================================================
> COMMIT
> ============================================================
>
> Commit:
>
> docs: record Gate E active end-to-end validation
>
> Push to origin/main.
>
> ============================================================
> FINAL
> ============================================================
>
> Run:
>
> git status -sb
> git rev-parse HEAD
> git rev-parse origin/main
>
> Require:
>
> working tree clean
> HEAD == origin/main
>
> RETURN
>
> # GATE E FINAL CHECKPOINT
>
> ## DOCS REVIEW
> ## COMMIT
> ## PUSH
> ## SECURE PATH EVIDENCE
> ## ONCHAIN STATE
> ## PHYSICAL ALLOW
> ## REPLAY
> ## LIMITATIONS
> ## FINAL GIT STATE
> ## NEXT OBJECTIVE
>
> Include documentation commit SHA.
>
> End exactly:
>
> GATE E CHECKPOINT: PASS
>
> or
>
> GATE E CHECKPOINT: STOP — <reason>
