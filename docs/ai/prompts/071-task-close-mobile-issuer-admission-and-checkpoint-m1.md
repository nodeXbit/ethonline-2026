# Project task packet 071: TASK — CLOSE MOBILE ISSUER ADMISSION AND CHECKPOINT M1

- Classification: **ORIGINAL / SANITIZED**
- Redactions applied: none
- Source: retained project-specific Control Tower packet in the local Codex record

---

> TASK — CLOSE MOBILE ISSUER ADMISSION AND CHECKPOINT M1
>
> M1 has now physically PASSED.
>
> Physical Android evidence:
>
> Issuer:
> 0xFa90e8301A22833B74378C5fA3a7c120Ac512685
>
> Real Sepolia transaction:
>
> 0x6c4f42f2d368936d4aaf7edf3e0395c376f92b699563b34fee4ed053a0a53e32
>
> Block:
> 11683226
>
> Transaction:
> SUCCESS
>
> Pre nonce:
> 0
>
> Post nonce:
> 1
>
> The latest APK physically restarted/reconciled the EXISTING persisted hash and
> displayed:
>
> Confirmed
>
> No second M1 transaction was sent.
>
> ============================================================
> OBJECTIVE
> ============================================================
>
> Close the Mobile Issuer Admission phase.
>
> Perform:
>
> 1. final diff/security review;
> 2. full relevant validation;
> 3. minimal project-document updates;
> 4. commit;
> 5. push;
> 6. final synchronization verification.
>
> Do NOT add new features.
>
> Do NOT perform blockchain writes.
>
> Do NOT run NFC.
>
> Do NOT flash firmware.
>
> Do NOT start R1/S1 bootstrap yet.
>
> ============================================================
> PHASE 1 — REVIEW CURRENT DIFF
> ============================================================
>
> Inspect every uncommitted change since:
>
> 1b1317e65106bb173d1eedc19ae32fcc34513651
>
> Verify the diff contains only the intended Android M1 / reusable transaction
> infrastructure work.
>
> Expected themes:
>
> - MobileIssuerAdmissionRunner
> - RecoverableTransactionEngine
> - ReadOnlyEthereumRpcClient
> - MainActivity M1 integration
> - tests
>
> Confirm no unexpected changes in:
>
> - HCE
> - holder-proof
> - Node verifier
> - firmware
> - ENS contracts/scripts
> - RPC secret configuration
> - Gradle dependencies unless already explicitly justified
>
> Review for:
>
> - secrets
> - RPC credentials
> - raw proof/signature persistence
> - auth tokens
> - email/OTP logging
> - private keys
> - unsafe arbitrary RPC write methods
> - accidental automatic retries
> - transaction resubmission after hash
> - operator Alchemy URL in APK
>
> ============================================================
> PHASE 2 — ARCHITECTURE CHECK
> ============================================================
>
> Require final architecture:
>
> PRIVY PROVIDER:
>
> - authentication
> - embedded wallet
> - switchChain
> - eth_signTypedData_v4
> - eth_sendTransaction
>
> READ-ONLY PUBLIC SEPOLIA RPC:
>
> - scalar/object blockchain reads
> - tx/receipt reconciliation
> - balance
> - nonce
> - blocks
> - eth_call
> - eth_estimateGas
>
> Require read client:
>
> - HTTPS
> - read-method allowlist
> - no eth_sendTransaction
> - no eth_sendRawTransaction
> - no signing
> - no secret endpoint
> - bounded timeout
> - bounded response
> - sanitized errors
>
> TRANSACTION ENGINE:
>
> - operation-scoped
> - persistent journal
> - explicit human review
> - SUBMITTING_NO_HASH before provider send
> - hash persisted immediately
> - no blind retry
> - UNKNOWN requires reconciliation
> - NO_BROADCAST requires proof + explicit re-arm
> - restart recovery
> - confirmed activity history foundation
>
> ============================================================
> PHASE 3 — VALIDATION
> ============================================================
>
> From mobile/android:
>
> .\gradlew.bat testDebugUnitTest
> .\gradlew.bat assembleDebug
>
> From repo root:
>
> node --test
> git diff --check
>
> Use actual resulting test counts.
>
> Do not weaken/remove existing tests.
>
> Do not make network writes.
>
> ============================================================
> PHASE 4 — DOCUMENT CURRENT VERIFIED STATE
> ============================================================
>
> Update only the dynamic project documents that genuinely need this checkpoint:
>
> STATUS.md
> WORKLOG.md
> DECISIONS.md
>
> Update PROJECT.md only if its architecture/product description is now materially
> wrong.
>
> Do not rewrite README yet; final product/UI still changes.
>
> Document succinctly:
>
> MOBILE ISSUER ADMISSION: PASS
>
> Issuer:
> 0xFa90e8301A22833B74378C5fA3a7c120Ac512685
>
> Funding tx:
> 0x388a8817a207ebeea6cfc4e2d7d3573f97522cebc31c44d866ad63cd0a929b53
>
> M1 tx:
> 0x6c4f42f2d368936d4aaf7edf3e0395c376f92b699563b34fee4ed053a0a53e32
>
> M1 block:
> 11683226
>
> nonce:
> 0 -> 1
>
> M1 transaction transport:
> PASS
>
> persistent hash/recovery:
> PASS
>
> read-only public RPC reconciliation:
> PASS
>
> No ENS writes occurred during M1.
>
> Also document the product architecture decision:
>
> - Android is primary credential wallet / issuer surface.
> - Privy handles wallet signing/writes.
> - public read-only RPC handles Android blockchain reads.
> - existing guest-001 physical fallback remains untouched.
> - next architecture target is isolated issuer namespace:
>   keys.demo-access.eth + R1 + S1.
>
> Do not include secrets.
>
> ============================================================
> PHASE 5 — COMMIT
> ============================================================
>
> If and only if:
>
> all validation passes
> diff is scoped
> no secrets
> docs accurate
>
> create one meaningful implementation checkpoint commit.
>
> Suggested message:
>
> feat: add recoverable mobile transaction infrastructure
>
> If docs are substantial enough to warrant a separate docs checkpoint, prefer:
>
> implementation commit first
>
> then:
>
> docs: checkpoint mobile issuer admission
>
> Do not create many trivial commits.
>
> ============================================================
> PHASE 6 — PUSH
> ============================================================
>
> Push main to origin only after successful commit validation.
>
> Then require:
>
> git fetch origin
> git status -sb
> git rev-parse HEAD
> git rev-parse origin/main
>
> Require:
>
> clean
> HEAD == origin/main
>
> Return exact final commit SHA(s).
>
> ============================================================
> RETURN
> ============================================================
>
> # MOBILE ISSUER ADMISSION — CLOSED
>
> ## VERIFIED M1
>
> Issuer:
>
> Funding tx:
>
> M1 tx:
>
> Block:
>
> Nonce:
>
> Transport:
>
> Recovery:
>
> Read RPC:
>
> ## FINAL ARCHITECTURE
>
> ## DIFF REVIEW
>
> ## TESTS
>
> Node:
>
> Android:
>
> assembleDebug:
>
> diff check:
>
> ## SECURITY
>
> Secrets:
>
> Write retries:
>
> RPC boundary:
>
> Legacy fallback:
>
> ## DOCUMENTATION
>
> Files updated:
>
> ## GIT
>
> Implementation commit:
>
> Docs commit:
>
> HEAD:
>
> origin/main:
>
> Status:
>
> ## NEXT ARCHITECTURE TARGET
>
> Do NOT execute it.
>
> Describe only:
>
> R1
> S1
> keys.demo-access.eth
>
> End exactly:
>
> MOBILE ISSUER ADMISSION: PASS — CHECKPOINTED
