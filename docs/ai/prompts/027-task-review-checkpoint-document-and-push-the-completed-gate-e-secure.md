# Project task packet 027: TASK — Review, checkpoint, document and push the completed Gate E secure

- Classification: **ORIGINAL / SANITIZED**
- Redactions applied: none
- Source: retained project-specific Control Tower packet in the local Codex record

---

> TASK
>
> Review, checkpoint, document and push the completed Gate E secure
> authorization implementation.
>
> The user explicitly authorizes:
>
> - scoped read-only review
> - commit of the existing Gate E implementation
> - scoped project-state documentation updates
> - documentation commit
> - push to origin/main
>
> DO NOT execute any blockchain write.
>
> Do NOT provision guest-001 yet.
>
> Do NOT run live Gate E physical authorization yet.
>
> CURRENT PUSHED BASELINE
>
> Expected:
>
> c3e5f1be8ec83387f50b16e34cfb2241238ca856
>
> CURRENT GATE E IMPLEMENTATION
>
> Expected changed files:
>
> package.json
>
> scripts/ensv2/access-record.mjs
> scripts/ensv2/access-record.test.mjs
> scripts/ensv2/persistent-access.mjs
>
> scripts/security/gate-e-secure-bridge.mjs
> scripts/security/gate-e-secure-bridge.test.mjs
>
> firmware/pn532_secure_access/pn532_secure_access.ino
>
> Verified implementation result:
>
> Node:
> 99 passed
> 0 failed
>
> Android:
> 21/21 PASS
> assembleDebug PASS
>
> Firmware:
> Gate D compile PASS
> Gate E compile PASS
>
> No blockchain writes.
>
> SECURE GATE E DESIGN
>
> Credential planned for live validation:
>
> guest-001.demo-access.eth
>
> Intended owner:
>
> 0x3419148731087b970d2059C53780163B452D5FF7
>
> Existing cred-001.demo-access.eth must remain unchanged.
>
> Resource:
>
> human identifier:
> demo-access.eth:door-001
>
> bytes32:
>
> 0xf2bde8f2654ee7267a06860eca03e0935d8818b006f5f51c2fce9d56a9b441cd
>
> derived using:
>
> keccak256(stringToHex("demo-access.eth:door-001"))
>
> Gate E uses:
>
> - existing Gate A IssuedChallengeStore
> - existing issueAccessChallenge
> - existing verifyAccessAttempt
> - existing isAuthorized
> - existing coherent readCredential snapshot
> - existing Gate C/D APDU v1
> - existing Privy Android signer
>
> No UID authorization.
>
> PRE-COMMIT REPOSITORY CHECK
>
> Run:
>
> git status -sb
> git rev-parse HEAD
> git rev-parse origin/main
>
> Require:
>
> HEAD == origin/main ==
> c3e5f1be8ec83387f50b16e34cfb2241238ca856
>
> and only the expected Gate E scoped changes.
>
> Ignored local configuration/build outputs are acceptable.
>
> If repository state differs materially:
> STOP.
>
> PRE-COMMIT REVIEW
>
> Read every Gate E changed file completely.
>
> Review specifically:
>
> 1. Gate A challenge issuance is reused rather than duplicated.
>
> 2. The same in-memory IssuedChallengeStore survives from:
>    challenge issuance
>    through proof verification
>    and optional replay validation.
>
> 3. No challenge is issued before:
>
>    TARGET_ACTIVATION: PASS
>    SELECT: PASS
>    WAITING_CHALLENGE
>
> 4. Duplicate WAITING_CHALLENGE input cannot issue another active challenge.
>
> 5. challenge serialization is exactly:
>
>    credential[32]
>    || resource[32]
>    || nonce[32]
>    || expiresAt[8 big-endian]
>
>    = 104 bytes.
>
> 6. resource bytes32 matches:
>
>    keccak256(stringToHex("demo-access.eth:door-001"))
>
> 7. firmware validates exactly 104 challenge bytes.
>
> 8. firmware sends SEND_CHALLENGE exactly once.
>
> 9. firmware accepts exactly 65 proof bytes.
>
> 10. UID does not influence authorization.
>
> 11. Node receives the physical proof and calls the existing Gate A verifier.
>
> 12. current signer comparison comes from CURRENT ENS credential owner.
>
> 13. ENS access policy still comes only from existing isAuthorized.
>
> 14. readCredential provides one coherent pinned snapshot for:
>     - status
>     - current owner
>     - registry expiry
>     - resolver
>     - access.v1
>     - authorization timestamp.
>
> 15. valid current-holder proof is consumed BEFORE later ENS policy ALLOW/DENY.
>
> 16. INACTIVE policy still consumes a valid holder challenge.
>
> 17. --check-replay verifies the same proof again through the same store and
>     returns REPLAYED_CHALLENGE without:
>     - new NFC work
>     - new challenge
>     - blockchain write.
>
> 18. wrong signer does not become ALLOW.
>
> 19. RPC/read failure fails closed.
>
> 20. serial timeout/malformed input fails closed.
>
> 21. Gate D firmware is unchanged.
>
> 22. APDU v1 is unchanged.
>
> 23. Android production code is unchanged.
>
> 24. existing cred-001 defaults remain unchanged.
>
> 25. --credential-label / --credential-owner parameterization cannot silently
>     change cred-001 when omitted.
>
> 26. guest-001 provisioning does not require transferring/unregistering
>     cred-001.
>
> 27. no private key, App Secret, Privy config, OTP, token or real signature
>     exists in the changes.
>
> 28. no blockchain write executes merely by importing/testing the new modules.
>
> If a concrete correctness/security defect is found:
> STOP.
> Do not commit.
>
> Do not perform stylistic refactors.
>
> VALIDATION BEFORE IMPLEMENTATION COMMIT
>
> Node:
>
> node --test --test-isolation=none
>
> Require:
>
> 99 passed
> 0 failed
>
> Android:
>
> :app:testDebugUnitTest
>
> Require:
>
> 21/21 PASS
>
> :app:assembleDebug
>
> Require:
>
> PASS
>
> Firmware:
>
> - compile Gate D sketch
> - compile Gate E sketch
>
> Require both PASS using the existing toolchain and Elechouse-compatible
> PN532 stack.
>
> Also:
>
> - git diff --check
> - firmware whitespace validation
> - safe secret/config scan
> - no real signature staged
> - no environment/local Privy configuration staged
> - no blockchain transaction
> - confirm cred-001 was not changed onchain or in defaults
>
> IMPLEMENTATION COMMIT
>
> Stage ONLY the Gate E implementation files.
>
> Expected:
>
> package.json
> scripts/ensv2/access-record.mjs
> scripts/ensv2/access-record.test.mjs
> scripts/ensv2/persistent-access.mjs
> scripts/security/gate-e-secure-bridge.mjs
> scripts/security/gate-e-secure-bridge.test.mjs
> firmware/pn532_secure_access/pn532_secure_access.ino
>
> Commit:
>
> feat: add secure ENS holder authorization bridge
>
> Do NOT include project-control documentation in this commit.
>
> After commit confirm:
>
> - implementation tree clean except future documentation changes
> - no local config included
> - no signature included
> - no blockchain write occurred
>
> DOCUMENTATION UPDATE
>
> Update only:
>
> STATUS.md
> WORKLOG.md
> DECISIONS.md
>
> Modify PROJECT.md only if an existing architecture statement has become
> factually wrong.
>
> Do not modify README.md yet.
>
> STATUS.md
>
> Record:
>
> Gate E implementation:
> PASS locally / LIVE VALIDATION PENDING
>
> Evidence:
>
> - fresh Gate A challenge generated only after physical SELECT/readiness
> - exact 104-byte challenge sent Node → ESP32
> - physical proof expected as exact 65 bytes ESP32 → Node
> - existing Gate A verifier reused
> - current ENS owner check reused
> - existing coherent ENS snapshot reused
> - existing isAuthorized reused
> - replay-check support implemented
> - second credential parameters added without changing cred-001 defaults
> - Gate E firmware fail-closed and one-shot
> - Node 99/99 PASS
> - Android 21/21 PASS/build PASS
> - Gate D and Gate E firmware compile PASS
>
> State explicitly:
>
> No live Gate E credential has yet been provisioned.
>
> Planned secure credential:
>
> guest-001.demo-access.eth
>
> intended owner:
>
> 0x3419148731087b970d2059C53780163B452D5FF7
>
> Current next objective:
>
> Perform read-only Gate E live preflight, then provision the separate
> Privy-owned credential in INACTIVE state with explicitly authorized Sepolia
> writes.
>
> After provisioning, validate:
>
> INACTIVE → physical DENY
> ACTIVE → physical ALLOW
> same-proof replay → DENY
> INACTIVE again → physical DENY
>
> WORKLOG.md
>
> Record concise implementation evidence:
>
> - Gate E secure serial bridge
> - fresh challenge timing after SELECT
> - 104-byte Node → firmware challenge
> - 65-byte firmware → Node proof
> - same Gate A challenge store from issuance through verification
> - coherent ENS snapshot reuse
> - isAuthorized reuse
> - fail-closed serial/RPC behavior
> - replay-check support
> - separate guest credential parameterization
> - 16 new tests
> - 99 final Node tests
> - Android unchanged and green
> - both firmware sketches compile
> - zero blockchain writes
>
> Technical learning:
>
> Transport identity and authorization remain separate:
>
> NFC transports a fresh signed proof;
> Gate A verifies wallet control;
> ENSv2 determines current credential ownership and access.
>
> Also record:
>
> challenge issuance is intentionally delayed until the phone has activated and
> the application AID has been selected, preserving most of the short challenge
> TTL.
>
> DECISIONS.md
>
> Record accepted Gate E implementation decisions:
>
> - Secure credential for live validation will be separate from cred-001.
> - Planned name:
>   guest-001.demo-access.eth
> - Planned owner:
>   0x3419148731087b970d2059C53780163B452D5FF7
> - cred-001 remains unchanged as prior validated demo/reference credential.
> - physical resource:
>   demo-access.eth:door-001
> - resource ID derived deterministically with keccak256.
> - Node remains challenge issuer, challenge store, verifier and ENS reader.
> - ESP32 remains NFC/APDU + serial transport only.
> - challenge is issued only after TARGET/SELECT/WAITING_CHALLENGE.
> - Gate A remains source of truth for challenge/replay/signature semantics.
> - existing readCredential remains source of coherent ENS state.
> - existing isAuthorized remains source of ENS access policy.
> - UID has no role in secure authorization.
> - Gate E firmware is one-shot per reset/session.
> - replay validation reuses the exact consumed proof and same store.
> - no Android/APDU change is required for Gate E.
> - live provisioning requires separate explicit authorization after code
>   checkpoint.
>
> Do not claim live secure ENS authorization has passed yet.
>
> DOCUMENTATION VALIDATION
>
> Before documentation commit:
>
> - inspect documentation diff
> - git diff --check
> - confirm no real signature
> - confirm no OTP/token
> - confirm no App ID / Client ID / App Secret
> - confirm no private key
> - confirm no local env/config staged
> - confirm implementation files unchanged after implementation commit
>
> DOCUMENTATION COMMIT
>
> Commit:
>
> docs: record secure authorization Gate E implementation
>
> PUSH
>
> Push both commits to origin/main.
>
> FINAL VERIFICATION
>
> Run:
>
> git status -sb
> git rev-parse HEAD
> git rev-parse origin/main
>
> Require:
>
> working tree clean excluding ignored local configuration/build outputs
> HEAD == origin/main
>
> RETURN
>
> # GATE E IMPLEMENTATION CHECKPOINT
>
> ## REVIEW
> ## NODE TESTS
> ## ANDROID TESTS
> ## FIRMWARE BUILDS
> ## IMPLEMENTATION COMMIT
> ## DOCUMENTATION COMMIT
> ## PUSH
> ## SECURITY
> ## FINAL GIT STATE
> ## LIVE VALIDATION READINESS
> ## NEXT OBJECTIVE
>
> Include:
>
> - implementation commit SHA
> - documentation commit SHA
> - pushed range
>
> Confirm explicitly:
>
> - Node 99/99 PASS
> - Android 21/21 PASS
> - Android build PASS
> - Gate D firmware compile PASS
> - Gate E firmware compile PASS
> - Gate A semantics reused
> - isAuthorized reused
> - coherent ENS read reused
> - APDU v1 unchanged
> - cred-001 unchanged
> - UID unused
> - no real signature committed
> - no secret/config committed
> - zero blockchain writes
> - local main == origin/main
>
> End exactly:
>
> GATE E IMPLEMENTATION CHECKPOINT: PASS
>
> or
>
> GATE E IMPLEMENTATION CHECKPOINT: STOP — <reason>
