# Project task packet 075: TASK — BUILD FIRST END-TO-END ANDROID CREDENTIAL VERTICAL

- Classification: **ORIGINAL / SANITIZED**
- Redactions applied: none
- Source: retained project-specific Control Tower packet in the local Codex record

---

> TASK — BUILD FIRST END-TO-END ANDROID CREDENTIAL VERTICAL
>
> MISSION
>
> Build ONE complete product vertical:
>
> Android issuer
> → create staff-001.keys.demo-access.eth
> → configure its ENSv2 records
> → authoritative readback
> → recipient holder imports/receives it
> → credential appears in My Keys.
>
> Do NOT implement dynamic NFC yet.
>
> Do NOT create visitor-001 or contractor-001.
>
> Do NOT perform the real staff credential transactions automatically.
>
> Implementation/build/tests are authorized.
>
> Real blockchain writes must happen only through the finished Android product UI
> after a separate manual user review.
>
> ============================================================
> BASELINE
> ============================================================
>
> Expected synchronized baseline:
>
> HEAD == origin/main ==
> ebd526aac8c61038b3318d16231e90877c63c58a
>
> Worktree clean.
>
> Before edits:
>
> git fetch origin
> git status -sb
> git rev-parse HEAD
> git rev-parse origin/main
>
> Require exact synchronization.
>
> Inspect AGENTS.md.
>
> STOP if state differs.
>
> ============================================================
> AUTHORITATIVE ISSUER CONFIG
> ============================================================
>
> Use:
>
> config/issuer-space.json
>
> Expected:
>
> chainId:
> 11155111
>
> namespace:
> keys.demo-access.eth
>
> issuer:
>
> 0xFa90e8301A22833B74378C5fA3a7c120Ac512685
>
> R1:
>
> 0x0AeB395be893149c0b60D9DAC3Ba55139D0dcA1a
>
> S1:
>
> 0x20766FB21498a99350F922ee3163F5a95F354a7f
>
> Do not duplicate these constants independently if config can be consumed or
> safely generated into Android.
>
> No RPC secrets.
>
> ============================================================
> TARGET CREDENTIAL
> ============================================================
>
> Label:
>
> staff-001
>
> Full name:
>
> staff-001.keys.demo-access.eth
>
> Recipient / holder:
>
> 0x3419148731087b970d2059C53780163B452D5FF7
>
> Registry expiry:
>
> 1793487599
>
> Human display:
>
> 2026-10-31 23:59:59 Europe/Madrid
>
> Transferable:
>
> NO
>
> R1 owner roleBitmap:
>
> 0
>
> Resolver:
>
> S1
>
> Subregistry:
>
> zero address
>
> Initial access:
>
> ALLOWED
>
> access.v1:
>
> active = true
> validUntil = 1793487599
>
> Description default:
>
> Staff Access Pass
>
> Avatar:
>
> User-editable URI field.
>
> Support validation for:
>
> https://
>
> ipfs://
>
> Do not require an avatar value to implement/build the vertical.
>
> Do not accept file:, javascript:, private-network proxying or arbitrary HTML.
>
> If avatar is blank during creation, either:
>
> A. omit setText("avatar") cleanly,
>
> or
>
> B. set an empty string only if the pinned resolver semantics and UI treatment
>    make that preferable.
>
> Choose the smaller truthful implementation and document it.
>
> Do NOT invent a fake/broken image URI.
>
> ============================================================
> PRODUCT REQUIREMENT
> ============================================================
>
> The user must be able to complete ordinary credential creation without:
>
> Codex
> ChatGPT
> CLI
> developer scripts
> DEV private key
>
> The app itself must:
>
> - build calldata;
> - present human review;
> - submit through the authenticated issuer Privy wallet;
> - persist/recover transaction progress;
> - reconcile receipts through the read-only RPC client;
> - verify final ENSv2 state.
>
> ============================================================
> PHASE 1 — INSPECT CURRENT ANDROID INFRASTRUCTURE
> ============================================================
>
> Inspect exact current:
>
> RecoverableTransactionEngine
>
> ReadOnlyEthereumRpcClient
>
> MobileIssuerAdmissionRunner
>
> MainActivity
>
> GateBApplication
>
> Privy wallet integration
>
> Gradle dependencies
>
> Do not redesign what already works.
>
> Identify the smallest seams for:
>
> - generic contract transaction submission;
> - ABI encoding;
> - credential state reading;
> - credential local index;
> - issuer capability detection.
>
> ============================================================
> PHASE 2 — ABI DEPENDENCY
> ============================================================
>
> Previous admission work determined native Android currently lacks a contract ABI
> encoder.
>
> Locally reverify before edit, then add ONE mature dependency if still required.
>
> Expected choice from prior inspection:
>
> org.web3j:core:4.12.3-android
>
> Reason:
>
> the abi-only AAR lacked required support modules in practice.
>
> Do NOT update:
>
> Privy
> AGP
> Gradle
> Java
> Android SDK
>
> Do NOT add a second Ethereum client stack for network transport.
>
> web3j is for ABI encode/decode only.
>
> Read transport stays:
>
> ReadOnlyEthereumRpcClient
>
> Write transport stays:
>
> Privy provider.
>
> ============================================================
> PHASE 3 — ENS ABI LAYER
> ============================================================
>
> Create the smallest focused ABI layer.
>
> Exact class/file names may adapt to repo style.
>
> It must encode/decode only what this vertical needs.
>
> Required writes:
>
> R1.register(
>     "staff-001",
>     holder,
>     zeroAddress,
>     S1,
>     0,
>     1793487599
> )
>
> S1.multicall(
>     [
>       optional setText(node, "avatar", avatarUri),
>       setText(node, "description", "Staff Access Pass"),
>       setData(
>         node,
>         "access.v1",
>         abi.encode(true, uint64(1793487599))
>       )
>     ]
> )
>
> Do not write a general-purpose Ethereum ABI framework.
>
> Need read encoding/decoding for authoritative state:
>
> R1 findTokenId / state / owner / expiry / resolver / roles as supported by the
> pinned ABI.
>
> S1:
>
> text avatar
> text description
> data access.v1
>
> Factory/provenance can use config + existing helpers/read calls as needed.
>
> Create golden ABI tests against Node/viem fixtures.
>
> Important:
>
> uint64 and uint256 must never pass through floating point.
>
> ============================================================
> PHASE 4 — CREDENTIAL READ MODEL
> ============================================================
>
> Create a reusable read-only model conceptually equivalent to:
>
> CredentialSnapshot
>
> Required fields:
>
> fullName
>
> namehash/node
>
> registry
>
> resolver
>
> tokenId as CURRENT technical value, never permanent identity
>
> status
>
> owner
>
> registryExpiry
>
> transferable
>
> accessActive
>
> accessValidUntil
>
> avatarUri
>
> description
>
> snapshotBlock
>
> snapshotTimestamp
>
> readStatus
>
> Read all authorization-sensitive state from one coherent/pinned block where
> possible.
>
> For UI state:
>
> RPC failure/stale state => UNKNOWN
>
> Never display a cached ALLOWED as authoritative after refresh failure.
>
> ============================================================
> PHASE 5 — ISSUER CAPABILITY CHECK
> ============================================================
>
> When logged in as:
>
> 0xFa90e8301A22833B74378C5fA3a7c120Ac512685
>
> read onchain authority.
>
> The UI may show Issuer/Create controls only when current onchain state confirms
> the required capability.
>
> Do not rely solely on matching the configured issuer address.
>
> At minimum confirm the connected account can perform the required R1/S1
> operations using roles/simulation/read-only checks.
>
> Ordinary holder:
>
> 0x341914...
>
> must NOT gain issuer UI authority merely by manipulating frontend state.
>
> ============================================================
> PHASE 6 — CREATE CREDENTIAL UX
> ============================================================
>
> Do not build final visual polish yet.
>
> But build a HUMAN product flow, not another test harness.
>
> Required form:
>
> CREATE CREDENTIAL
>
> Name
> staff-001
>
> Resulting ENS name
> staff-001.keys.demo-access.eth
>
> Recipient
> 0x3419148731087b970d2059C53780163B452D5FF7
>
> Expires
> human date + underlying timestamp
>
> Transferable
> No
>
> Artwork URI
> editable
>
> Description
> Staff Access Pass
>
> Initial access
> Allowed
>
> [ REVIEW ]
>
> Before transaction submission show a human review.
>
> Do NOT expose:
>
> raw calldata
> operation UUID
> role bitmaps
>
> in normal review.
>
> Technical details may be collapsible.
>
> ============================================================
> PHASE 7 — TWO-TRANSACTION ISSUANCE COORDINATOR
> ============================================================
>
> Build a small IssuanceCoordinator over the existing transaction engine.
>
> Lifecycle:
>
> DRAFT
>
> → REGISTER_READY
>
> → REGISTER_SUBMITTED
>
> → REGISTER_CONFIRMED
>
> → REGISTERED_CONFIGURING
>
> → RECORDS_READY
>
> → RECORDS_SUBMITTED
>
> → RECORDS_CONFIRMED
>
> → AUTHORITATIVE_READBACK
>
> → READY
>
> Important:
>
> R1 registration and S1 records are two independent blockchain transactions.
>
> If register succeeds and records fail:
>
> DO NOT call register again.
>
> Persist:
>
> REGISTERED_CONFIGURING
>
> Allow:
>
> Resume setup
>
> which only submits the S1 multicall.
>
> Use existing RecoverableTransactionEngine semantics:
>
> - explicit review;
> - one attempt per operation;
> - hash persistence;
> - receipt reconciliation;
> - no blind retry;
> - restart recovery.
>
> Do not create a second weaker transaction system.
>
> ============================================================
> PHASE 8 — WRITE SAFETY
> ============================================================
>
> Before R1.register submission:
>
> require:
>
> authenticated wallet == issuer I
>
> Privy selected chain == Sepolia
>
> read RPC chain == Sepolia
>
> issuer latest == pending
>
> staff-001 is AVAILABLE
>
> keys namespace is still valid
>
> R1/S1 provenance matches config
>
> credential expiry < keys expiry
>
> simulated call succeeds
>
> Before S1 multicall:
>
> require:
>
> staff-001 REGISTERED
>
> current owner == intended holder
>
> resolver == S1
>
> registry expiry exact
>
> register receipt confirmed
>
> issuer authority in S1 still valid
>
> simulated multicall succeeds
>
> No automatic retries.
>
> ============================================================
> PHASE 9 — AUTHORITATIVE READBACK
> ============================================================
>
> Credential becomes READY only when fresh readback proves:
>
> full name:
>
> staff-001.keys.demo-access.eth
>
> status:
> REGISTERED
>
> owner:
> 0x3419148731087b970d2059C53780163B452D5FF7
>
> resolver:
> S1
>
> subregistry:
> zero
>
> registry expiry:
> 1793487599
>
> owner role bitmap:
> 0
>
> transferable:
> false
>
> description:
> Staff Access Pass
>
> avatar:
> exact entered URI, or blank/absent according to implemented behavior
>
> access.active:
> true
>
> access.validUntil:
> 1793487599
>
> R1/S1 provenance:
> expected
>
> No READY from receipt alone.
>
> ============================================================
> PHASE 10 — LOCAL CREDENTIAL INDEX / MY KEYS
> ============================================================
>
> Implement the smallest truthful local index.
>
> Persist only references, conceptually:
>
> chainId
> fullName
>
> Partition associations by wallet.
>
> Do NOT persist ownership/access/expiry as authority.
>
> MY KEYS must refresh each known credential onchain.
>
> A credential belongs in the active holder's owned view only if fresh readback
> shows:
>
> current owner == connected wallet
>
> For this hackathon:
>
> allow:
>
> IMPORT CREDENTIAL
>
> by normalized ENS full name.
>
> No indexer.
>
> No reverse-ENS enumeration.
>
> No claim that My Keys lists every ENS name owned globally.
>
> Bound known list reasonably, e.g. 20.
>
> ============================================================
> PHASE 11 — RECEIVE / IMPORT FIRST VERTICAL
> ============================================================
>
> After staff creation is complete, issuer UI must provide at minimum:
>
> Credential created
>
> staff-001.keys.demo-access.eth
>
> [ Copy credential name ]
>
> A QR can be deferred if it materially adds time.
>
> Then user can:
>
> logout issuer
>
> login holder account:
>
> 0x3419148731087b970d2059C53780163B452D5FF7
>
> My Keys
> → Import credential
>
> paste:
>
> staff-001.keys.demo-access.eth
>
> The app verifies:
>
> owner == logged holder
>
> then displays the card.
>
> If owner does not match:
>
> do not add to owned My Keys.
>
> ============================================================
> PHASE 12 — FIRST MY KEYS CARD
> ============================================================
>
> Minimum product card:
>
> STAFF PASS
>
> staff-001.keys.demo-access.eth
>
> Artwork:
> render URI if supported and safe
> otherwise placeholder
>
> Access:
> Allowed
>
> Expires:
> 31 Oct 2026
>
> Transferability:
> Non-transferable
>
> Do not show raw wallet address prominently.
>
> Credential detail may show:
>
> Owner
> Issuer
> Registration
> Access
> Transferability
> Description
> Artwork URI
> Technical details
>
> No NFC button required in this batch unless already trivial.
>
> Dynamic NFC is the NEXT batch.
>
> ============================================================
> PHASE 13 — HUMAN VS DIAGNOSTIC UI
> ============================================================
>
> Do not polish the whole app.
>
> But enforce the separation:
>
> NORMAL PRODUCT:
>
> My Keys
> Issuer/create flow
>
> DEVELOPER DIAGNOSTICS:
>
> M1 admission
> Gate B/C2 tests
> raw technical states
> operation IDs
> nonces
>
> It is acceptable for diagnostics to remain visually plain.
>
> Do not remove useful diagnostics.
>
> Do not expose raw signature/proof copying in the normal product path.
>
> ============================================================
> PHASE 14 — NO REAL STAFF WRITE DURING AUTOMATED WORK
> ============================================================
>
> Implementation/testing may use mocks/fixtures/read-only simulation.
>
> Do NOT submit:
>
> R1.register staff-001
>
> or:
>
> S1.multicall staff records
>
> from Codex/terminal.
>
> The real credential MUST be created from the physical Android UI by the user
> after:
>
> tests/build pass
> APK installed
> issuer logged in
> human review displayed.
>
> ============================================================
> PHASE 15 — TESTS
> ============================================================
>
> Tests must cover at least:
>
> ABI golden register
>
> ABI golden setText
>
> ABI golden setData access.v1
>
> ABI golden multicall
>
> uint64 expiry exact
>
> non-transferable roleBitmap == 0
>
> credential name validation
>
> expiry bounds
>
> issuer authority positive
>
> holder authority negative
>
> staff availability
>
> two-step issuance state machine
>
> register confirmed + records fail => resume records only
>
> restart recovery after register
>
> restart recovery after records hash
>
> authoritative final readback
>
> wrong owner
>
> wrong resolver
>
> wrong registry/provenance
>
> expired namespace/credential
>
> access malformed
>
> RPC UNKNOWN does not display ALLOWED
>
> local index duplicates
>
> wallet switch
>
> import not-owned credential rejected
>
> owned credential displayed
>
> product UI does not expose issuer controls to ordinary holder
>
> No real chain write in tests.
>
> ============================================================
> PHASE 16 — VALIDATION
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
> Inspect dependency diff.
>
> No secret in APK/config.
>
> No operator Alchemy endpoint.
>
> No HCE/Node/firmware changes.
>
> No commit/push yet.
>
> ============================================================
> PHASE 17 — STOP BEFORE REAL CREATION
> ============================================================
>
> After green build/tests:
>
> STOP.
>
> Return the APK and exact manual user steps.
>
> Do NOT create staff.
>
> Do NOT commit yet.
>
> We want physical validation before checkpoint.
>
> ============================================================
> RETURN
> ============================================================
>
> # STAFF CREDENTIAL VERTICAL — IMPLEMENTATION READY
>
> ## FILES
>
> ## DEPENDENCY
>
> ## ABI
>
> ## ISSUER AUTHORITY
>
> ## ISSUANCE STATE MACHINE
>
> ## TRANSACTION SAFETY
>
> ## CREDENTIAL READ MODEL
>
> ## LOCAL INDEX
>
> ## MY KEYS
>
> ## PRODUCT UI
>
> ## DIAGNOSTICS SEPARATION
>
> ## TESTS
>
> Node:
> Android:
> assemble:
> diff check:
>
> ## APK
>
> Path:
> SHA-256:
>
> ## REAL STAFF WRITE
>
> Performed:
> NO
>
> ## MANUAL TEST PLAN
>
> Give exact simple human steps:
>
> A. issuer login
> B. create/review staff credential
> C. transaction 1
> D. transaction 2
> E. READY
> F. logout/login holder
> G. import credential
> H. My Keys card
>
> Include STOP conditions.
>
> ## SECURITY
>
> ## GIT
>
> No commit/push.
>
> End exactly:
>
> STAFF VERTICAL: READY FOR PHYSICAL CREATION
>
> or:
>
> STAFF VERTICAL: STOP — <reason>
