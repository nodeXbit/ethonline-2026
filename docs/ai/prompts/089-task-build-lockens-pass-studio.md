# Project task packet 089: TASK — BUILD LOCKENS PASS STUDIO

- Classification: **ORIGINAL / SANITIZED**
- Redactions applied: none
- Source: retained project-specific Control Tower packet in the local Codex record

---

> TASK — BUILD LOCKENS PASS STUDIO
>
> MISSION
>
> Turn the current fixed STAFF issuance demo into a real, capability-driven
> LockENS Studio.
>
> Build:
>
> 1. configurable pass issuance;
> 2. STAFF / VISITOR / CONTRACTOR templates;
> 3. dynamic ENSv2 capability gating;
> 4. management of existing passes:
>    - Suspend / Restore
>    - Change access validity
>    - Extend registration expiry
>    - Change artwork
>    - Change description
>
> DO NOT perform any real blockchain write.
>
> DO NOT create VISITOR or CONTRACTOR yet.
>
> DO NOT modify HCE/NFC.
>
> DO NOT modify Node/firmware.
>
> DO NOT redeploy R1/S1.
>
> DO NOT change root governance.
>
> STOP before any physical transaction.
>
> ============================================================
> BASELINE
> ============================================================
>
> Expected clean synchronized baseline:
>
> HEAD == origin/main ==
> 09e8153a01bca6283a773d41c77572442b742337
>
> Run:
>
> git fetch origin
> git status -sb
> git rev-parse HEAD
> git rev-parse origin/main
>
> Inspect AGENTS.md.
>
> Require exact synchronization.
>
> ============================================================
> EXISTING INFRASTRUCTURE — MUST PRESERVE
> ============================================================
>
> Namespace:
>
> keys.demo-access.eth
>
> Issuer namespace owner:
>
> 0xFa90e8301A22833B74378C5fA3a7c120Ac512685
>
> R1 UserRegistry:
>
> 0x0AeB395be893149c0b60D9DAC3Ba55139D0dcA1a
>
> S1 PermissionedResolver:
>
> 0x20766FB21498a99350F922ee3163F5a95F354a7f
>
> Namespace expiry:
>
> 1814392799
>
> Existing real credential:
>
> staff-001.keys.demo-access.eth
>
> It is a regression fixture and MUST NOT be modified during implementation.
>
> Existing real STAFF:
>
> owner:
> 0x3419148731087b970d2059C53780163B452D5FF7
>
> registration expiry:
> 1793487599
>
> transferable:
> false
>
> description:
> Staff Access Pass
>
> access:
> active
>
> Existing issuance/recovery, automatic discovery, selected-pass and wallet
> behavior MUST remain valid.
>
> ============================================================
> PRODUCT INFORMATION ARCHITECTURE
> ============================================================
>
> Replace the user-facing navigation concept:
>
> Issuer
>
> with:
>
> Studio
>
> Studio is NOT a user identity.
>
> It is a capability-driven workspace.
>
> Conceptually:
>
> MY KEYS | STUDIO | SETTINGS
>
> Studio appears only when the ACTIVE wallet has at least one relevant verified
> ENSv2 management capability.
>
> A wallet may simultaneously:
>
> - own passes in My Keys;
> - use Studio;
> - have only some Studio capabilities.
>
> Do not classify users globally as Holder or Issuer.
>
> ============================================================
> CAPABILITY MODEL
> ============================================================
>
> Use fresh onchain roles, not hard-coded addresses, as authority.
>
> Model at minimum:
>
> canIssue
> → required R1 REGISTRAR capability
>
> canRenew
> → required R1 RENEW capability
>
> canManageAccess
> → required S1 SET_DATA capability
>
> canManagePresentation
> → required S1 SET_TEXT capability
>
> Admin/delegation capabilities may be READ and represented internally if useful,
> but DO NOT implement Permissions grant/revoke transactions in this task.
>
> Studio sections/actions must be individually gated.
>
> Example:
>
> wallet with only SET_TEXT
> → may edit artwork/description
> → may NOT issue
> → may NOT suspend
>
> Capability read failure:
> UNAVAILABLE, not DENIED.
>
> ============================================================
> STUDIO HOME
> ============================================================
>
> Human product surface:
>
> STUDIO
>
> [ Issue Pass ]
>
> Manage Passes
> <existing product passes>
>
> Optionally show compact capability information such as:
>
> Can issue
> Can manage access
> Can manage presentation
> Can extend registrations
>
> Do not expose raw role bitmaps in normal UI.
>
> ============================================================
> PASS DRAFT MODEL
> ============================================================
>
> Replace the hard-coded STAFF creation constants with the smallest reusable
> PassDraft model.
>
> Required business fields:
>
> template
> label
> recipient
> registrationExpiry
> accessActive
> accessValidUntil
> transferable
> description
> artworkUri
>
> Derived/internal:
>
> full ENS name
> namespace
> R1
> S1
> chain
> roleBitmap
> namehash/node
> calldata
>
> Use integer-safe timestamp handling.
>
> Do NOT use floating point for uint64/uint256 values.
>
> ============================================================
> LABEL / NAME
> ============================================================
>
> User edits ONLY the label, e.g.:
>
> visitor-001
>
> App derives:
>
> visitor-001.keys.demo-access.eth
>
> Validate:
>
> - normalized supported label;
> - no dots / no attempt to escape namespace;
> - sane length;
> - currently AVAILABLE;
> - not existing/expired/reused in an unsafe way.
>
> Do not implement unregister/reissue.
>
> ============================================================
> RECIPIENT
> ============================================================
>
> Recipient is editable.
>
> Accept a valid Ethereum address.
>
> Normalize safely/checksum for display.
>
> Review must show:
>
> full exact recipient
> and compact recipient in normal summary.
>
> Do not allow invalid/zero address unless pinned ENS semantics explicitly justify
> it, which is not expected here.
>
> ============================================================
> EXPIRY MODEL
> ============================================================
>
> Registration expiry is editable.
>
> Require:
>
> future timestamp
>
> and:
>
> registrationExpiry < namespaceExpiry
>
> with a reasonable safety margin where appropriate.
>
> Access valid-until is separately editable or clearly derived by default.
>
> Default:
>
> accessValidUntil = registrationExpiry
>
> Allow access validity to be earlier than registry expiry.
>
> Never allow access validity beyond registry expiry.
>
> Display human local date/time plus exact UTC in review details.
>
> ============================================================
> TRANSFERABILITY
> ============================================================
>
> Treat transferability as an ISSUANCE-TIME property.
>
> Pinned ENSv2 source must reconfirm the exact role constant before use.
>
> Expected model from prior verified architecture:
>
> non-transferable:
> roleBitmap = 0
>
> transferable:
> ROLE_CAN_TRANSFER_ADMIN
>
> Verify against pinned source/artifact.
>
> Do NOT implement changing transferability after issuance.
>
> Do NOT implement actual transfer in this task.
>
> ============================================================
> TEMPLATES
> ============================================================
>
> Templates are presets, NOT immutable hard-coded credentials.
>
> Implement:
>
> STAFF
> VISITOR
> CONTRACTOR
>
> Suggested defaults:
>
> STAFF:
> - Allowed
> - Non-transferable
> - description "Staff Access Pass"
>
> VISITOR:
> - Allowed
> - Transferable
> - shorter default validity
> - description "Visitor Pass"
>
> CONTRACTOR:
> - Suspended initially
> - Non-transferable
> - description "Contractor Access Pass"
>
> Every normal business field remains editable after choosing the preset,
> subject to validation.
>
> Changing template updates sensible defaults but does not secretly override
> user-edited values during Review/Create.
>
> Use a clear reset/apply-template action if needed.
>
> ============================================================
> ARTWORK
> ============================================================
>
> Artwork URI remains editable.
>
> Support current:
>
> https://
> ipfs://
>
> Do not upload files in this task.
>
> Existing safe artwork rendering infrastructure remains unchanged.
>
> After issuance, authoritative avatar URI should render as the full pass artwork.
>
> ============================================================
> GENERIC ISSUANCE — TX1
> ============================================================
>
> Reuse the EXISTING recoverable transaction infrastructure.
>
> Do NOT create a second write engine.
>
> Generic TX1:
>
> R1.register(
>     label,
>     recipient,
>     zeroSubregistry,
>     S1,
>     computedRoleBitmap,
>     registrationExpiry
> )
>
> Before final submission require fresh:
>
> - active wallet provider;
> - Sepolia;
> - wallet capability;
> - latest == pending;
> - label AVAILABLE;
> - namespace valid;
> - R1/S1 provenance;
> - expiry bounds;
> - exact eth_call simulation.
>
> Review must remain pure.
>
> Persistent operation begins only at FINAL Create boundary.
>
> ============================================================
> GENERIC ISSUANCE — TX2
> ============================================================
>
> After confirmed registration:
>
> S1.multicall(
>     [
>       optional avatar setText,
>       description setText,
>       access.v1 setData
>     ]
> )
>
> access.v1:
>
> abi.encode(
>     accessActive,
>     uint64(accessValidUntil)
> )
>
> Blank artwork:
> omit avatar setter.
>
> Do not re-register after confirmed TX1.
>
> Resume setup must remain records-only.
>
> READY only after exact authoritative readback.
>
> Generalize the existing proven STAFF recovery semantics rather than replacing
> them.
>
> ============================================================
> ISSUANCE SESSION IDENTITY
> ============================================================
>
> Current implementation evolved from a fixed staff vertical.
>
> Refactor only as much as required so issuance state is correctly scoped to the
> credential/full name being created.
>
> A confirmed or ambiguous operation for credential A must never be interpreted
> as credential B.
>
> Do not key long-lived authority only on tokenId.
>
> Use stable credential/full-name intent plus transaction evidence.
>
> Preserve historical staff journal/session.
>
> Do not corrupt or migrate existing confirmed evidence unnecessarily.
>
> ============================================================
> MANAGE PASSES DISCOVERY
> ============================================================
>
> Studio must be able to manage product credentials even when the active wallet
> does NOT own them.
>
> Use the existing known-R1 event discovery to obtain product credential
> candidates.
>
> Then perform current authoritative readback.
>
> Manage list is NOT "My Keys".
>
> My Keys:
> passes owned by active wallet.
>
> Studio Manage:
> passes in product R1 that current active wallet has authority to modify.
>
> For hackathon scale, scanning the known R1 is sufficient.
>
> ============================================================
> MANAGE PASS — HUMAN UI
> ============================================================
>
> Opening a pass in Studio should show:
>
> Credential
> full ENS name
>
> Owner
>
> Registration
> status
> expiry
>
> Access
> Allowed / Suspended
> valid until
>
> Transferability
> Transferable / Non-transferable
>
> Presentation
> description
> artwork
>
> Then show ONLY actions permitted by current wallet capability.
>
> ============================================================
> MANAGE — SUSPEND / RESTORE
> ============================================================
>
> Using S1 access.v1.
>
> Suspend:
>
> preserve current accessValidUntil
> set:
> active = false
>
> Restore:
>
> set:
> active = true
> preserve current accessValidUntil
>
> Require valid unexpired access validity for Restore.
>
> If accessValidUntil is already expired:
> do not misleadingly restore to a currently valid state.
> Require/change validity first.
>
> Each action:
>
> Review
> → final explicit confirmation
> → existing recoverable transaction infrastructure
> → receipt
> → authoritative readback
>
> Do NOT execute physically in this task.
>
> ============================================================
> MANAGE — CHANGE ACCESS VALIDITY
> ============================================================
>
> Allow authorized S1 SET_DATA wallet to change:
>
> access.v1.validUntil
>
> and preserve chosen active state unless user intentionally changes it.
>
> Require:
>
> future/supported value as appropriate
> accessValidUntil <= registrationExpiry
>
> Human review shows old → new.
>
> ============================================================
> MANAGE — CHANGE PRESENTATION
> ============================================================
>
> Allow authorized S1 SET_TEXT wallet to update:
>
> avatar
> description
>
> Prefer one S1.multicall when both change together.
>
> Allow changing one independently.
>
> Review old → new.
>
> Blank artwork may intentionally clear avatar only if pinned resolver semantics
> and UI explicitly support "Remove artwork".
>
> Do not accidentally clear it because an input failed to load.
>
> ============================================================
> MANAGE — EXTEND REGISTRATION
> ============================================================
>
> Reconfirm the exact pinned R1 renew ABI and semantics.
>
> Allow only if active wallet has required RENEW authority.
>
> Registration expiry may ONLY increase.
>
> Never present reduction as available.
>
> Require:
>
> newExpiry > currentExpiry
> newExpiry < namespaceExpiry
>
> After renew, authoritative readback must prove exact new expiry.
>
> If accessValidUntil is lower, do NOT silently extend access.
>
> Registration and access policy remain distinct.
>
> ============================================================
> TRANSACTION SAFETY FOR MANAGEMENT
> ============================================================
>
> Every write action must use the proven safe semantics:
>
> - pure Review;
> - fresh preflight at final confirmation;
> - one attempt;
> - persist no-hash boundary before provider send;
> - hash persisted immediately;
> - no blind retry;
> - receipt reconciliation;
> - receipt-block-aware readback;
> - restart recovery;
> - UNKNOWN never blindly resubmits.
>
> Do not weaken this for "small" management operations.
>
> ============================================================
> ACTION JOURNAL / RECENT HISTORY
> ============================================================
>
> If existing transaction journal can naturally distinguish management actions,
> use clear action types such as:
>
> ISSUE_REGISTER
> ISSUE_CONFIGURE
> ACCESS_SUSPEND
> ACCESS_RESTORE
> ACCESS_VALIDITY
> PRESENTATION_UPDATE
> REGISTRATION_RENEW
>
> Do not build a new activity/indexing system.
>
> ============================================================
> UI / TERMINOLOGY
> ============================================================
>
> Use:
>
> Studio
>
> Issue Pass
>
> Manage Passes
>
> Access:
> Allowed / Suspended
>
> Registration:
> Valid until ...
>
> Access valid until:
> ...
>
> Transferability:
> ...
>
> Avoid normal-user terms:
>
> REGISTRAR
> SET_DATA
> roleBitmap
> multicall
> uint64
>
> Those stay in diagnostics/details only.
>
> ============================================================
> EXISTING STAFF REGRESSION
> ============================================================
>
> staff-001 must remain:
>
> REGISTERED
> owner unchanged
> ALLOWED
> non-transferable
> same expiry
> same description
> no artwork
>
> It must:
>
> still autodiscover for its owner;
> still render in My Keys;
> remain selected where applicable;
> appear in Studio Manage for wallets with relevant authority.
>
> No blockchain write to it.
>
> ============================================================
> NO PERMISSIONS WRITE YET
> ============================================================
>
> Do NOT implement:
>
> grant roles
> revoke roles
> admin changes
> root governance
>
> But structure capability checks so a later:
>
> Studio → Permissions
>
> can be added without redesign.
>
> ============================================================
> NO OTHER SCOPE
> ============================================================
>
> Do NOT implement:
>
> unregister
> label reuse
> actual VISITOR transfer
> holder metadata delegation
> resource-aware gates
> NFC/HCE
> firmware
> Node verifier
> network switching
> ETH send/receive
> backend/indexer
> image upload
>
> ============================================================
> TESTS
> ============================================================
>
> Cover at least:
>
> STUDIO CAPABILITIES
> - no capabilities → Studio hidden/unavailable
> - issue only
> - access only
> - presentation only
> - renew only
> - combined capabilities
> - capability unavailable
>
> PASS DRAFT
> - template defaults
> - template fields editable
> - derived full ENS name
> - invalid labels
> - invalid recipient
> - expiry bounds
> - access validity bounds
> - transferable exact role bitmap
> - non-transferable exact role bitmap
>
> GENERIC ISSUE
> - register ABI golden with arbitrary valid draft
> - configure ABI golden
> - optional artwork
> - blank artwork omitted
> - Allowed
> - Suspended
> - pure review
> - final preflight
> - per-credential operation isolation
> - TX1 confirmed + TX2 resume
> - receipt-block readback
> - no duplicate retries
>
> MANAGE
> - discovered pass not owned by manager can still be managed when authority exists
> - no authority hides actions
> - suspend preserves validUntil
> - restore preserves validUntil
> - expired access cannot misleadingly restore
> - access validity update
> - description update
> - artwork update
> - combined presentation update
> - renew only extends
> - renew cannot reduce
> - namespace expiry ceiling
>
> REGRESSION
> - real staff fixture remains readable
> - My Keys ownership model unchanged
> - wallet switching unchanged
> - autodiscovery unchanged
> - artwork rendering unchanged
> - selected pass unchanged
> - no HCE changes
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
> No commit/push.
>
> ============================================================
> PHYSICAL READ-ONLY REVIEW
> ============================================================
>
> Install using:
>
> adb install -r
>
> Do not clear data.
>
> Use the current active issuer/root-controller wallet if available.
>
> ZERO blockchain writes.
>
> Physically inspect:
>
> Studio navigation
>
> Studio capability presentation
>
> Issue Pass
>
> STAFF template
>
> VISITOR template
>
> CONTRACTOR template
>
> editing:
> label
> recipient
> expiry
> access
> transferability
> description
> artwork
>
> derived ENS name
>
> Review for a fresh AVAILABLE test label
>
> Back/edit/review purity
>
> Manage Passes
>
> existing staff-001 detail
>
> available management actions
>
> DO NOT press any final write confirmation.
>
> No wallet approval.
>
> No signing.
>
> No transaction.
>
> ============================================================
> RETURN
> ============================================================
>
> # LOCKENS PASS STUDIO — READY
>
> ## STUDIO
>
> Navigation:
>
> Capability gating:
>
> ## ISSUE PASS
>
> Editable fields:
>
> Derived fields:
>
> Templates:
>
> ## STAFF TEMPLATE
>
> ## VISITOR TEMPLATE
>
> ## CONTRACTOR TEMPLATE
>
> ## GENERIC ISSUANCE
>
> TX1:
>
> TX2:
>
> Recovery:
>
> ## MANAGE PASSES
>
> Discovery:
>
> Authority:
>
> ## SUSPEND / RESTORE
>
> ## ACCESS VALIDITY
>
> ## PRESENTATION
>
> Artwork:
>
> Description:
>
> ## EXTEND REGISTRATION
>
> Pinned ABI:
>
> Behavior:
>
> ## EXISTING STAFF REGRESSION
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
> Final transaction action pressed:
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
> ## SECURITY
>
> Secrets:
> 0
>
> ## GIT
>
> No commit/push.
>
> ## VERDICT
>
> PASS STUDIO:
> PASS / STOP
>
> If PASS end exactly:
>
> PASS STUDIO: READY FOR CONTROLLED REAL CREDENTIAL TESTS
