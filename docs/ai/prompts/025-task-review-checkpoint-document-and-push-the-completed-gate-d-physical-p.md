# Project task packet 025: TASK — Review, checkpoint, document and push the completed Gate D physical PN532/HCE

- Classification: **ORIGINAL / SANITIZED**
- Redactions applied: none
- Source: retained project-specific Control Tower packet in the local Codex record

---

> TASK
>
> Review, checkpoint, document and push the completed Gate D physical PN532/HCE
> transport milestone.
>
> The user explicitly authorizes:
>
> - scoped read-only review
> - commit of the existing Gate D firmware
> - scoped STATUS.md / WORKLOG.md / DECISIONS.md updates
> - documentation commit
> - push to origin/main
>
> Do NOT implement Gate E yet.
> Do NOT change APDU v1.
> Do NOT modify Android production code.
>
> CURRENT VERIFIED PUSHED BASE
>
> 5cb7d3561492afeb5b9477b32ab9dd34688df7d6
>
> Expected current tracked/untracked state after diagnostic cleanup:
>
> - no Android production diff
> - only Gate D implementation remains:
>   firmware/pn532_hce_apdu/pn532_hce_apdu.ino
>
> Before any write, run:
>
> git status -sb
> git rev-parse HEAD
> git rev-parse origin/main
>
> Require:
>
> HEAD == origin/main ==
> 5cb7d3561492afeb5b9477b32ab9dd34688df7d6
>
> If other unexpected tracked/untracked changes exist:
> STOP.
>
> VERIFIED GATE D EVIDENCE
>
> Hardware:
>
> - ESP32-S3
> - Elechouse-compatible PN532 + PN532_I2C
> - I2C SDA GPIO17
> - I2C SCL GPIO18
> - PN532 address 0x24
> - Serial0 115200
> - PN532 firmware 1.6
>
> Frozen APDU v1:
>
> AID:
> F0454E5356324331
>
> Challenge:
> credential[32]
> || resource[32]
> || nonce[32]
> || expiresAt[8 unsigned big-endian]
>
> 104 bytes total.
>
> Proof:
> 65-byte ECDSA signature.
>
> No chunking.
>
> PHYSICAL SESSION 1 — PASS
>
> TARGET ACTIVATION: PASS
> SELECT: PASS
> SEND_CHALLENGE: PASS
> STATUS PROCESSING: PASS
> STATUS READY: PASS
> GET_SIGNATURE: PASS
> SIGNATURE LENGTH: 65
>
> Android telemetry confirmed:
>
> GET_SIGNATURE received: YES
> processor state: READY
> Host response length: 67
> Host status: 9000
>
> Node recovered signer:
>
> 0x3419148731087b970d2059C53780163B452D5FF7
>
> MATCH: PASS
>
> PHYSICAL SESSION 2 — PASS
>
> TARGET ACTIVATION: PASS
> SELECT: PASS
> SEND_CHALLENGE: PASS
> STATUS PROCESSING → READY
> GET_SIGNATURE: PASS
> SIGNATURE_LEN=65
> ASCII/hex/65-byte structural validation: PASS
>
> Node recovered signer:
>
> 0x3419148731087b970d2059C53780163B452D5FF7
>
> MATCH: PASS
>
> SEND_CHALLENGE was sent exactly once.
>
> REPEATABILITY
>
> 2/2 complete physical sessions PASS.
>
> Earlier GET_SIGNATURE failure:
>
> TRANSIENT / NOT REPRODUCED
>
> The unchanged permanent implementation subsequently transported the same
> full-size response successfully twice.
>
> Do NOT introduce a speculative fix.
>
> TEMPORARY DIAGNOSTICS
>
> All temporary Android HCE capability/transport telemetry changes were already
> reverted after diagnosis.
>
> Confirm there is no remaining Android diagnostic diff.
>
> PRE-COMMIT REVIEW
>
> Read the Gate D firmware fully.
>
> Confirm:
>
> 1. exact Elechouse-compatible PN532 / PN532_I2C stack is used;
> 2. Adafruit_PN532 is not introduced;
> 3. existing GPIO17/GPIO18 I2C wiring is preserved;
> 4. APDU v1 AID and command bytes are unchanged;
> 5. challenge is exactly the Gate C2 deterministic transport vector;
> 6. challenge binary encoding is exactly 104 bytes;
> 7. SEND_CHALLENGE occurs once per session;
> 8. status polling does not resend SEND_CHALLENGE;
> 9. GET_SIGNATURE is requested only after READY;
> 10. response requires exactly 65 proof bytes + 9000;
> 11. serial signature output is strict ASCII hexadecimal;
> 12. firmware is one-shot and requires reset for another session;
> 13. NFC UID has no authorization role;
> 14. no private key/Privy secret/token/OTP/config is present;
> 15. no ENS/RPC/blockchain logic exists in firmware;
> 16. no installed PN532 library was modified;
> 17. no chunking or APDU v1 change was introduced.
>
> If a concrete defect is found:
> STOP.
>
> Do not perform stylistic refactors.
>
> VALIDATION
>
> Re-run safe validation:
>
> Node:
> node --test --test-isolation=none
> Require:
> 83 passed
> 0 failed
>
> Android:
> :app:testDebugUnitTest
> Require:
> 21/21 PASS
>
> :app:assembleDebug
> Require:
> PASS
>
> Firmware:
> compile the Gate D sketch using the already-established local toolchain.
> No need to run another physical signing session just for checkpointing.
>
> Also require:
>
> - git diff --check
> - firmware whitespace check
> - safe secret scan
> - no real transported signature in source/repo
> - no local Privy configuration staged
> - no Android diagnostic diff
> - no blockchain transaction
>
> IMPLEMENTATION COMMIT
>
> Stage ONLY:
>
> firmware/pn532_hce_apdu/pn532_hce_apdu.ino
>
> Commit:
>
> feat: add physical PN532 HCE proof transport
>
> Do not include documentation in this commit.
>
> DOCUMENTATION UPDATE
>
> After implementation commit, update only:
>
> STATUS.md
> WORKLOG.md
> DECISIONS.md
>
> Modify PROJECT.md only if an existing architecture statement is now factually
> wrong.
>
> Do not update README.md yet.
>
> STATUS.md
>
> Record Gate D as PASS:
>
> - real ESP32-S3 + Elechouse PN532 ↔ Seeker ISO-DEP/APDU interoperability
> - TARGET / SELECT / SEND_CHALLENGE / PROCESSING / READY / GET_SIGNATURE
>   physically validated
> - real Privy-produced Gate A-compatible signature transported
> - exact signature length 65 bytes
> - Node recovered the same embedded Privy wallet
> - repeatability 2/2
> - APDU v1 unchanged
> - chunking not required
> - earlier GET_SIGNATURE failure classified transient/not reproduced
> - Node 83/83 PASS
> - Android 21/21 PASS/build PASS
>
> State clearly:
>
> Gate D proves physical cryptographic proof transport.
>
> It does NOT yet prove ENS-based physical authorization.
>
> Set next objective:
>
> Gate E — replace the deterministic Gate D transport vector with a fresh
> server-issued Gate A challenge and compose the physically transported proof
> with CURRENT ENSv2 ownership and access.v1 authorization to produce physical
> ALLOW / DENY without using NFC UID for authorization.
>
> WORKLOG.md
>
> Record concise Gate D evidence:
>
> - exact hardware stack
> - real ISO-DEP activation
> - SELECT AID
> - 104-byte challenge transport
> - asynchronous Privy signing
> - 65-byte signature retrieval
> - Node recovery match
> - 2/2 physical repeatability
> - original GET_SIGNATURE transient and diagnostic process
> - full-size 67-byte response physically proven
> - no chunking required
> - temporary Android diagnostics reverted
>
> Technical learning:
>
> Do not redesign a protocol around a single transient hardware failure.
> Instrument the failing boundary first; successful full-size reproduction proved
> the transport itself was viable.
>
> DECISIONS.md
>
> Record:
>
> - Gate D uses Elechouse-compatible PN532 / PN532_I2C.
> - APDU v1 remains frozen.
> - Current 104-byte challenge and 65-byte proof fit the validated transport.
> - Signature chunking is not required for this prototype.
> - ESP32 is NFC/APDU transport only at this stage.
> - Privy signing remains on Android.
> - Node remains the future verifier/onchain reader.
> - NFC UID is excluded from secure authorization.
> - One-shot firmware behavior prevents accidental repeated signing sessions.
> - Original GET_SIGNATURE failure is treated as transient unless recurrence
>   produces stronger evidence.
> - No speculative PN532/HAL workaround is accepted without reproducible
>   evidence.
>
> Do not claim ENS physical authorization yet.
>
> DOCUMENTATION VALIDATION
>
> Before docs commit:
>
> - inspect documentation diff
> - git diff --check
> - confirm no real signature
> - confirm no App ID / Client ID / App Secret
> - confirm no OTP/token/private data
> - confirm no local environment/config staged
> - confirm implementation firmware unchanged after its commit
>
> DOCUMENTATION COMMIT
>
> Commit:
>
> docs: record physical HCE transport Gate D
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
> # GATE D CHECKPOINT
>
> ## REVIEW
> ## FIRMWARE BUILD
> ## ANDROID TESTS
> ## NODE TESTS
> ## IMPLEMENTATION COMMIT
> ## DOCUMENTATION COMMIT
> ## PUSH
> ## PHYSICAL EVIDENCE
> ## SECURITY
> ## FINAL GIT STATE
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
> - physical sessions 2/2 PASS
> - transported proof length 65
> - Privy wallet == recovered signer
> - APDU v1 unchanged
> - chunking NOT REQUIRED
> - NFC UID unused for authorization
> - no real signature committed
> - no Privy secret/config values committed
> - zero blockchain writes
> - Node 83/83 PASS
> - Android 21/21 PASS
> - local main == origin/main
>
> End exactly:
>
> GATE D CHECKPOINT: PASS
>
> or
>
> GATE D CHECKPOINT: STOP — <reason>
