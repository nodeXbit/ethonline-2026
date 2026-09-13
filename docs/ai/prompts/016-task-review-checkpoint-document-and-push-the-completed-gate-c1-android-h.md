# Project task packet 016: TASK — Review, checkpoint, document and push the completed Gate C1 Android HCE/APDU

- Classification: **ORIGINAL / SANITIZED**
- Redactions applied: none
- Source: retained project-specific Control Tower packet in the local Codex record

---

> TASK
>
> Review, checkpoint, document and push the completed Gate C1 Android HCE/APDU
> protocol milestone.
>
> The user explicitly authorizes:
>
> - scoped read-only review
> - commit of the existing Gate C1 implementation
> - scoped STATUS.md / WORKLOG.md / DECISIONS.md updates
> - documentation commit
> - push to origin/main
>
> Do NOT implement Gate C2 yet.
> Do NOT modify PN532 firmware.
> Do NOT perform any NFC hardware exchange.
>
> CURRENT VERIFIED BASELINE
>
> Expected pushed base:
>
> 50225f11e3c550c72e2bb9740a1d4ca0da4dddc5
>
> Expected Gate C1 changes only in the previously reported Android files.
>
> Verified before checkpoint:
>
> Node:
> 82 passed
> 0 failed
>
> Android:
> :app:testDebugUnitTest PASS
> :app:assembleDebug PASS
>
> No blockchain writes.
> No Privy signing during Gate C1.
> No PN532/firmware changes.
>
> GATE C1 PROTOCOL
>
> AID:
>
> F0454E5356324331
>
> Interpretation:
> proprietary/demo F0 prefix + ASCII ENSV2C1
>
> Category:
> other / non-payment
>
> Application APDUs:
>
> SELECT AID
> CLA 00
> INS A4
> P1 04
> P2 00
>
> SEND_CHALLENGE
> CLA 80
> INS 10
> P1 01
> P2 00
> Lc 68 hex = 104 decimal bytes
>
> GET_STATUS
> CLA 80
> INS 20
> P1 01
> P2 00
>
> GET_SIGNATURE
> CLA 80
> INS 30
> P1 01
> P2 00
>
> Challenge binary order:
>
> credential[32]
> || resource[32]
> || nonce[32]
> || expiresAt[8 unsigned big-endian]
>
> Total:
> 104 bytes
>
> Proof:
> 65-byte ECDSA signature
>
> States:
> IDLE
> PROCESSING
> READY
> ERROR
>
> Status bytes:
> 00 IDLE
> 01 PROCESSING
> 02 READY
> 03 ERROR
>
> Important transport decision:
>
> A successfully produced signature remains readable during the current selected
> session so an interrupted GET_SIGNATURE can be retried.
>
> It is cleared by:
> - new challenge
> - new SELECT/session reset
> - HCE deactivation
>
> Gate A remains responsible for cryptographic one-shot/replay protection.
>
> PRE-COMMIT REVIEW
>
> First run:
>
> git status -sb
> git rev-parse HEAD
> git rev-parse origin/main
>
> Require:
>
> HEAD == origin/main == 50225f11e3c550c72e2bb9740a1d4ca0da4dddc5
>
> and only the expected Gate C1 scoped changes.
>
> If repo state differs materially:
> STOP.
>
> Read all Gate C1 changed files completely.
>
> Review specifically:
>
> 1. HostApduService is a thin Android adapter.
> 2. APDU parsing/state logic lives in the independently testable processor.
> 3. SELECT correctly resets the session.
> 4. HCE deactivation clears session/proof state.
> 5. challenge length is exactly 104 bytes.
> 6. challenge byte order exactly matches the reported protocol.
> 7. uint64 expiresAt is interpreted consistently and safely.
> 8. wrong CLA/INS/length/version are rejected deterministically.
> 9. GET_SIGNATURE cannot expose proof before READY.
> 10. proof length must be exactly 65 bytes.
> 11. a new challenge cannot expose a stale proof.
> 12. stale asynchronous provider completion cannot overwrite a newer session.
> 13. deterministic fake proof provider exists only in test code.
> 14. production Gate C1 does NOT contain fake cryptographic signing.
> 15. production PendingProofProvider performs no signing.
> 16. HCE registration uses category=other / non-payment.
> 17. no UID participates in authorization.
> 18. no Privy App ID / Client ID / App Secret is exposed.
> 19. no private key exists.
> 20. no Gate A/B semantics were changed.
> 21. firmware/scripts/nfc remain untouched.
>
> If a concrete protocol/security defect is found:
> STOP before committing.
>
> Do not perform stylistic refactors.
>
> VALIDATION
>
> Rerun:
>
> Android:
> - :app:testDebugUnitTest
> - :app:assembleDebug
>
> Node:
> - node --test --test-isolation=none
>
> Require:
> 82 passed
> 0 failed
>
> Also:
>
> - git diff --check
> - scoped secret/config scan
> - confirm privy.local.properties remains ignored and unstaged
> - confirm build outputs remain ignored
> - confirm firmware/ and scripts/nfc/ unchanged
>
> IMPLEMENTATION COMMIT
>
> Stage ONLY Gate C1 implementation files.
>
> Commit:
>
> feat: add Android HCE access proof protocol
>
> Do not include documentation in this commit.
>
> DOCUMENTATION
>
> Then update only:
>
> STATUS.md
> WORKLOG.md
> DECISIONS.md
>
> Do not modify README.md yet.
> Do not modify PROJECT.md unless an existing statement becomes factually wrong.
>
> STATUS.md
>
> Record Gate C1 as PASS:
>
> - Android HostApduService registered
> - non-payment proprietary AID
> - deterministic APDU v1
> - 104-byte binary AccessChallenge transport
> - IDLE / PROCESSING / READY state model
> - exact 65-byte proof response
> - reset/stale-completion protection
> - Android unit tests PASS
> - Android debug build PASS
> - Node 82/82 remains green
>
> State clearly:
>
> Gate C1 proves deterministic Android-side HCE protocol behavior only.
>
> It does NOT yet prove:
> - Privy signing from HCE
> - real phone ↔ PN532 communication
> - physical cryptographic authorization
>
> Set next objective:
>
> Gate C2 — connect the already-proven Privy Android EIP-712 signer to the HCE
> ProofProvider abstraction while preserving APDU v1 and Gate A/B semantics.
>
> PN532 firmware must remain untouched during Gate C2.
>
> WORKLOG.md
>
> Record concise evidence:
>
> - Gate C1 AID
> - 104-byte binary challenge format
> - 65-byte proof response
> - APDU state machine
> - HostApduService thin adapter
> - ProofProvider abstraction
> - stale async completion protection
> - Android JVM tests/build PASS
> - Node 82/82 unchanged
> - no physical NFC claim yet
>
> Technical learning:
>
> NFC/HCE is only the transport layer.
>
> Replay security remains in Gate A, and cryptographic signing remains separated
> behind the ProofProvider interface.
>
> DECISIONS.md
>
> Record:
>
> - Gate C APDU v1 is fixed for the current prototype.
> - AID F0454E5356324331 is prototype/non-payment.
> - challenge wire format is:
>   credential[32] || resource[32] || nonce[32] || expiresAt[8 big-endian]
> - signature wire payload is 65 bytes.
> - no JSON is transported over NFC.
> - ProofProvider separates HCE transport from signing implementation.
> - GET_SIGNATURE proof may be reread during the same selected transport session.
> - new challenge / SELECT / HCE deactivation clears proof state.
> - Gate A remains authoritative for replay protection.
> - Gate C2 must preserve the APDU contract.
> - PN532 firmware is deferred until C2 passes.
>
> Do not claim physical NFC interoperability yet.
>
> DOCUMENTATION VALIDATION
>
> Before docs commit:
>
> - inspect docs diff
> - git diff --check
> - ensure no Privy identifiers/secrets/signatures appear
> - ensure ignored local config is not staged
> - confirm implementation files unchanged after their commit
>
> DOCUMENTATION COMMIT
>
> Commit:
>
> docs: record Android HCE Gate C1
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
> working tree clean excluding ignored local config/build outputs
> HEAD == origin/main
>
> RETURN
>
> # GATE C1 CHECKPOINT
>
> ## REVIEW
> ## ANDROID TESTS
> ## ANDROID BUILD
> ## NODE TESTS
> ## IMPLEMENTATION COMMIT
> ## DOCUMENTATION COMMIT
> ## PUSH
> ## SECURITY
> ## FINAL GIT STATE
> ## NEXT OBJECTIVE
>
> Include:
>
> - implementation SHA
> - documentation SHA
> - pushed range
>
> Confirm:
>
> - Android HCE tests pass
> - Android assembleDebug passes
> - Node 82/82 passes
> - APDU v1 unchanged
> - no fake signing in production code
> - no Privy secret/config committed
> - no blockchain write
> - no PN532/NFC hardware modification
> - local main == origin/main
>
> End exactly:
>
> GATE C1 CHECKPOINT: PASS
>
> or
>
> GATE C1 CHECKPOINT: STOP — <reason>
