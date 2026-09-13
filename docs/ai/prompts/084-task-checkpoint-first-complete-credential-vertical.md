# Project task packet 084: TASK — CHECKPOINT FIRST COMPLETE CREDENTIAL VERTICAL

- Classification: **ORIGINAL / SANITIZED**
- Redactions applied: none
- Source: retained project-specific Control Tower packet in the local Codex record

---

> TASK — CHECKPOINT FIRST COMPLETE CREDENTIAL VERTICAL
>
> MISSION
>
> The first real end-to-end credential vertical has physically PASSED.
>
> Do NOT implement any new feature.
>
> Do NOT perform any blockchain write.
>
> Do NOT work on automatic discovery yet.
>
> Do NOT work on NFC yet.
>
> Review, validate, document, commit and push the current completed work.
>
> ============================================================
> PHYSICAL END-TO-END EVIDENCE
> ============================================================
>
> Credential:
>
> staff-001.keys.demo-access.eth
>
> Issuer:
>
> 0xFa90e8301A22833B74378C5fA3a7c120Ac512685
>
> Holder:
>
> 0x3419148731087b970d2059C53780163B452D5FF7
>
> TX1 registration:
>
> 0x8858c291f14659ea8e323d1af988f4ee379c2a14dc5e2cbed4db592a04e6db58
>
> TX1 block:
>
> 11684957
>
> TX2 configuration:
>
> 0x57a5a4bf81fcede947b83bf55dcabe065540a1254fb2d9eb10677d391cf7bb11
>
> TX2 block:
>
> 11685150
>
> Issuer final nonce:
>
> 3 / 3
>
> Final credential state:
>
> REGISTERED
>
> Owner:
> 0x3419148731087b970d2059C53780163B452D5FF7
>
> Resolver:
> 0x20766FB21498a99350F922ee3163F5a95F354a7f
>
> Subregistry:
> zero
>
> Expiry:
> 1793487599
>
> Owner roles:
> 0
>
> Transferability:
> non-transferable
>
> Description:
> Staff Access Pass
>
> Avatar:
> unset
>
> access.v1:
> active = true
> validUntil = 1793487599
>
> Final Android issuance state:
>
> READY
>
> Physical holder validation:
>
> The holder logged in with:
>
> 0x3419148731087b970d2059C53780163B452D5FF7
>
> and My Keys successfully displayed:
>
> STAFF ACCESS
> staff-001.keys.demo-access.eth
> ALLOWED
> Valid until 31 Oct 2026
> Non-transferable
> Staff Access Pass
>
> Ownership was freshly validated onchain.
>
> Therefore:
>
> FIRST CREDENTIAL VERTICAL: PASS
>
> ============================================================
> PHASE 1 — REVIEW CURRENT WORKTREE
> ============================================================
>
> Inspect all changes since:
>
> ebd526aac8c61038b3318d16231e90877c63c58a
>
> Classify them.
>
> Expected work includes:
>
> - Web3j ABI dependency / exclusions
> - CredentialAbi
> - CredentialReader
> - CredentialPersistence
> - ContractTransactionRunner
> - staff issuance vertical
> - My Keys local index/import
> - issuer capability gating
> - polished product shell
> - Developer Diagnostics separation
> - P0 review/state fixes
> - post-TX1 recovery fixes
> - receipt-block-aware final readback
> - transaction-status presentation policy
> - CTA layout fix
> - tests
> - STATUS changes
>
> Require no unrelated HCE, Node, firmware or contract changes.
>
> Review for:
>
> secrets
> RPC credentials
> auth tokens
> raw signed transactions
> private keys
> unsafe retries
> duplicate write paths
> stale example.com artwork persistence
> accidental staff re-registration path
> accidental TX2 resend path
>
> ============================================================
> PHASE 2 — FINAL VALIDATION
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
> Use actual final test counts.
>
> No blockchain writes.
>
> Also perform fresh READ-ONLY staff verification proving the persisted public
> evidence is still correct.
>
> ============================================================
> PHASE 3 — DOCUMENTATION
> ============================================================
>
> Update dynamic project documents where genuinely required:
>
> STATUS.md
> WORKLOG.md
> DECISIONS.md
>
> Update PROJECT.md only if materially stale.
>
> Do not rewrite final README yet.
>
> Document succinctly:
>
> - first Android-issued credential vertical PASS;
> - exact TX1/TX2 evidence;
> - holder physically validated My Keys;
> - transaction recovery incidents and the general fixes learned:
>   - pure review boundary;
>   - confirmed operation idempotency;
>   - unset access.v1 handling;
>   - receipt-block-aware authoritative readback;
> - current limitations remain truthful.
>
> Explicitly record as NEXT PRODUCT P1:
>
> Automatic discovery of credentials belonging to the active wallet within the
> known product R1 namespace.
>
> Intended behavior:
>
> - scan known R1 registration/ownership events from a bounded known start block;
> - treat events only as candidate discovery;
> - perform fresh authoritative readback for current ownership/state;
> - partition by active wallet;
> - My Keys refreshes automatically;
> - manual entry becomes secondary "Add by ENS name" recovery flow.
>
> Do NOT claim this is implemented.
>
> Also record product-direction follow-ups, without implementing:
>
> - stacked pass presentation inspired by mobile wallet/pass apps;
> - selected pass as future NFC-active credential;
> - real artwork rendering;
> - startup Privy session hydration state;
> - configurable issuance templates.
>
> Do not let these obscure the next shipping-critical tasks.
>
> ============================================================
> PHASE 4 — COMMIT STRATEGY
> ============================================================
>
> This is substantial work.
>
> Prefer a small number of meaningful commits.
>
> A reasonable split:
>
> 1. implementation:
>    feat: add end-to-end ENSv2 credential issuance
>
> 2. product/recovery/UI if naturally separable:
>    feat: add credential wallet product experience
>
> 3. docs:
>    docs: checkpoint first credential vertical
>
> But inspect the actual diff and choose the smallest coherent history.
>
> Do not create many trivial commits.
>
> ============================================================
> PHASE 5 — PUSH
> ============================================================
>
> After validation and commits:
>
> If explicit push approval is required by the current safety gate:
>
> STOP and ask for it.
>
> Otherwise follow existing authorized repository workflow.
>
> After push require:
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
> ahead/behind 0/0
>
> ============================================================
> RETURN
> ============================================================
>
> # FIRST CREDENTIAL VERTICAL — CHECKPOINT
>
> ## ONCHAIN
>
> Credential:
>
> TX1:
>
> TX2:
>
> Holder:
>
> Final state:
>
> ## PHYSICAL PRODUCT
>
> Issuer flow:
>
> Holder My Keys:
>
> ## DIFF REVIEW
>
> ## TESTS
>
> Android:
> Node:
> assemble:
> diff check:
>
> ## SECURITY
>
> Secrets:
>
> Write retry safety:
>
> Confirmed-op recovery:
>
> ## DOCUMENTATION
>
> Files updated:
>
> P1 automatic discovery recorded:
> YES / NO
>
> ## GIT
>
> Commits:
>
> HEAD:
>
> origin/main:
>
> Status:
>
> If push approval is needed, stop with:
>
> FIRST CREDENTIAL VERTICAL: READY TO PUSH
>
> Otherwise end exactly:
>
> FIRST CREDENTIAL VERTICAL: PASS — CHECKPOINTED
