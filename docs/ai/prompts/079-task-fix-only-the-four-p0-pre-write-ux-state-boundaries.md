# Project task packet 079: TASK — FIX ONLY THE FOUR P0 PRE-WRITE UX / STATE BOUNDARIES

- Classification: **ORIGINAL / SANITIZED**
- Redactions applied: none
- Source: retained project-specific Control Tower packet in the local Codex record

---

> TASK — FIX ONLY THE FOUR P0 PRE-WRITE UX / STATE BOUNDARIES
>
> CONTEXT
>
> The independent physical UX audit is complete.
>
> DO NOT broaden the product in this task.
>
> DO NOT implement automatic discovery yet.
>
> DO NOT make the staff template generic yet.
>
> DO NOT rename/restructure the product beyond what is necessary for the P0 fixes.
>
> DO NOT perform any blockchain write.
>
> DO NOT sign anything.
>
> The physical audit found four P0 issues that MUST be fixed before the first real
> staff credential write.
>
> ============================================================
> OBJECTIVE
> ============================================================
>
> Fix exactly:
>
> UX-01
> pre-action creation progress is shown before Create.
>
> UX-02
> Review persists/locks an unsent issuance draft and Back is not semantically
> reversible.
>
> UX-03
> final review does not provide enough exact recipient/issuer/expiry information.
>
> UX-04
> no-hash and UNKNOWN transaction states can be presented as Submitted.
>
> Preserve all existing visual polish and transaction safety.
>
> ============================================================
> P0-01 — NO CREATION PROGRESS BEFORE CREATE
> ============================================================
>
> Before the user presses the FINAL:
>
> Create credential
>
> there must be no active transaction-progress presentation.
>
> DRAFT / editable screen:
>
> no creation progress.
>
> Review screen:
>
> no creation progress.
>
> Returning Back from Review:
>
> no creation progress.
>
> Do not display:
>
> Creating credential
> 1 of 3
> Waiting for approval
>
> merely because a draft/review exists.
>
> A neutral read-only validation message is acceptable only if actually useful,
> but prefer no progress component before submission.
>
> ============================================================
> P0-02 — MAKE REVIEW PURE AND REVERSIBLE
> ============================================================
>
> Current audit finding:
>
> reviewCredential() performs read-only preflight but also creates/persists
> issuance/transaction draft state and locks Artwork.
>
> This must change.
>
> Required semantic boundary:
>
> EDITABLE FORM
>     ↓
> Review credential
>     ↓
> PURE REVIEW
>     ↓
> Back
>     ↓
> EDITABLE FORM
>
> Review may:
>
> - validate local input;
> - perform read-only preflight;
> - build exact ABI/calldata in memory if useful;
> - simulate read-only;
> - show the preview.
>
> Review MUST NOT:
>
> - create a persistent transaction operation;
> - persist an irreversible issuance session;
> - lock Artwork;
> - move the issuance workflow into a submitted/active state;
> - prevent changing currently editable draft input.
>
> Back must leave the user able to edit Artwork again without:
> ISSUANCE_ARTWORK_ALREADY_LOCKED
> or equivalent.
>
> Do not add a complex Discard Draft feature if pure Review removes the need.
>
> ============================================================
> FINAL CREATE BOUNDARY
> ============================================================
>
> Only when the user presses:
>
> Create credential
>
> may the application establish the persistent issuance intent / transaction
> operation required by the existing recovery model.
>
> At that moment:
>
> 1. rerun fresh safety preflight;
> 2. reverify issuer authority;
> 3. reverify Sepolia;
> 4. latest/pending nonce agreement;
> 5. verify staff-001 still AVAILABLE;
> 6. verify namespace / R1 / S1 / provenance;
> 7. verify exact intended form values;
> 8. simulate the exact register call;
> 9. atomically establish the persistent operation according to the existing
>    transaction-engine safety model;
> 10. proceed to wallet approval/submission.
>
> Do not rely on the earlier Review preflight being fresh enough.
>
> Preserve:
>
> one attempt per operation
> hash persistence
> no blind retry
> restart recovery
> NO_BROADCAST_PROVEN semantics
> UNKNOWN reconciliation
>
> ============================================================
> P0-03 — EXACT FINAL REVIEW DETAILS
> ============================================================
>
> Keep the polished compact review.
>
> But before final Create, the user must be able to verify exact irreversible
> details.
>
> Normal summary may continue to show truncated addresses.
>
> Add a clear expandable/details section or equivalent existing-native treatment
> that shows:
>
> Credential:
> staff-001.keys.demo-access.eth
>
> Recipient:
> FULL checksummed
> 0x3419148731087b970d2059C53780163B452D5FF7
>
> Issuing wallet:
> FULL checksummed
> 0xFa90e8301A22833B74378C5fA3a7c120Ac512685
>
> Access:
> Allowed
>
> Expiry:
> 31 Oct 2026, 23:59:59 Europe/Madrid
>
> Also show exact UTC value in details if useful:
> 2026-10-31T22:59:59Z
>
> Transferability:
> Non-transferable
>
> Network:
> Sepolia
>
> Explain humanly:
>
> Creation requires two Sepolia transactions:
>
> 1. Create the access pass.
> 2. Configure its access and presentation records.
>
> Do not expose:
>
> raw calldata
> role bitmaps
> operation UUID
> nonces
>
> in normal review.
>
> Technical diagnostics remain elsewhere.
>
> ============================================================
> P0-04 — AUTHORITATIVE TRANSACTION STATUS MAPPING
> ============================================================
>
> Visible transaction status must never claim more certainty than the persisted
> transaction operation proves.
>
> When a transaction operation exists, it is the authoritative source for the
> visible transaction status.
>
> Required distinctions:
>
> Before final Create:
> NO transaction progress.
>
> User has pressed Create but wallet/provider approval has not reached the
> persisted submission boundary:
> Waiting for wallet approval
> (use only if this distinction is genuinely observable)
>
> SUBMITTING_NO_HASH:
> Submitting — do not retry
>
> Do NOT call this Submitted.
>
> HASH_RECEIVED:
> Submitted
>
> CONFIRMING:
> Confirming
>
> UNKNOWN with hash:
> Status unknown — checking existing attempt
>
> Do NOT overwrite this with Submitted from the coarse issuance coordinator.
>
> CONFIRMED:
> Confirmed
>
> REVERTED:
> Failed / Reverted
>
> NO_BROADCAST_PROVEN:
> Previous attempt was not broadcast
>
> Only offer the existing explicitly safe re-arm path where appropriate.
>
> If RPC is unavailable/ambiguous:
> Status unavailable
>
> Never show Failed unless failure/revert is proven.
>
> ============================================================
> SOURCE OF TRUTH / RENDER ORDER
> ============================================================
>
> Audit the current rendering path that allowed the coarse issuance state to
> overwrite a more accurate transaction-engine status.
>
> Fix the model systematically.
>
> Do not patch strings screen-by-screen.
>
> Define one safe presentation derivation/policy for transaction status.
>
> Issuance step may describe:
>
> Creating pass
> Configuring access
> Verifying onchain
>
> But transaction certainty must come from the current persisted transaction
> operation where one exists.
>
> ============================================================
> PHYSICAL UX PRESERVATION
> ============================================================
>
> Preserve the current successful physical polish:
>
> - 24dp gutter;
> - correct WindowInsets;
> - My Keys layout;
> - credential card language;
> - Issuer layout;
> - Review layout;
> - Settings;
> - separate Developer Diagnostics;
> - light/dark resources.
>
> Do not reopen the visual redesign.
>
> ============================================================
> NOT IN THIS TASK
> ============================================================
>
> Do NOT implement audit P1/P2 items yet:
>
> - generic/editable credential creator;
> - automatic R1 discovery / eth_getLogs;
> - rename Issuer;
> - capability tri-state;
> - new safe-error taxonomy beyond what P0 requires;
> - multi-wallet selector;
> - artwork rendering;
> - Wallet tab;
> - QR;
> - transfer;
> - NFC;
> - Gate UI.
>
> Do not change:
>
> CredentialAbi semantics
> R1/S1 configuration
> Privy version
> Web3j dependency
> HCE/NFC
> Node
> firmware
>
> ============================================================
> TESTS
> ============================================================
>
> Add/update focused tests proving:
>
> 1. DRAFT shows no transaction progress.
>
> 2. Opening Review shows no transaction progress.
>
> 3. Review creates no persisted transaction operation.
>
> 4. Review does not lock Artwork.
>
> 5. Review → Back → change Artwork → Review succeeds.
>
> 6. Multiple Review/Back cycles remain side-effect free.
>
> 7. Final Create reruns fresh preflight.
>
> 8. Persistent issuance/transaction state begins only at the final Create
>    boundary.
>
> 9. Review details expose:
>    - exact recipient;
>    - exact issuer;
>    - timezone;
>    - network;
>    - two transaction purposes.
>
> 10. SUBMITTING_NO_HASH != Submitted.
>
> 11. HASH_RECEIVED == Submitted.
>
> 12. UNKNOWN/hash cannot be overwritten by coarse issuance state.
>
> 13. CONFIRMING / CONFIRMED / REVERTED / NO_BROADCAST_PROVEN retain truthful
>     human mappings.
>
> 14. Existing duplicate-prevention and recovery tests remain green.
>
> No real blockchain writes in tests.
>
> ============================================================
> VALIDATION
> ============================================================
>
> Run:
>
> from mobile/android:
>
> .\gradlew.bat testDebugUnitTest
> .\gradlew.bat assembleDebug
>
> from repo root:
>
> node --test
> git diff --check
>
> Install resulting APK over the current app with adb -r.
>
> Do NOT clear data.
>
> With the unlocked physical device, safely inspect:
>
> Issuer before Review
>
> Review
>
> Back to Issuer
>
> If safe, modify Artwork locally and reopen Review to prove it is not locked.
>
> DO NOT press final Create credential.
>
> No blockchain write.
>
> No signing.
>
> ============================================================
> RETURN
> ============================================================
>
> # P0 PRE-WRITE UX FIXES — READY
>
> ## UX-01
>
> Before:
> After:
> Physical verification:
>
> ## UX-02
>
> Previous persistence boundary:
>
> New persistence boundary:
>
> Review side effects:
>
> Review → Back → edit test:
>
> ## UX-03
>
> Exact review details now available:
>
> ## UX-04
>
> Transaction presentation mapping:
>
> Explain source-of-truth precedence.
>
> ## FILES
>
> ## TESTS
>
> Android:
> Node:
> assemble:
> diff check:
>
> ## PHYSICAL DEVICE
>
> Screens inspected:
>
> Review/Back/edit cycle:
>
> Final Create pressed:
> NO
>
> ## BLOCKCHAIN / SIGNING
>
> Writes:
> 0
>
> Signatures:
> 0
>
> ## APK
>
> Path:
> SHA-256:
>
> ## GIT
>
> No commit/push.
>
> ## VERDICT
>
> P0 FIX SET:
> PASS / STOP
>
> If PASS end exactly:
>
> STAFF WRITE: READY FOR FINAL PHYSICAL PRE-WRITE REVIEW
>
> Do not create staff-001.
