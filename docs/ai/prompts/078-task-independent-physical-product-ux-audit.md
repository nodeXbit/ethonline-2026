# Project task packet 078: TASK — INDEPENDENT PHYSICAL PRODUCT / UX AUDIT

- Classification: **ORIGINAL / SANITIZED**
- Redactions applied: none
- Source: retained project-specific Control Tower packet in the local Codex record

---

> TASK — INDEPENDENT PHYSICAL PRODUCT / UX AUDIT
>
> MISSION
>
> Do NOT implement anything.
>
> Do NOT change code.
>
> Do NOT perform blockchain writes.
>
> Do NOT sign anything.
>
> Do NOT create staff-001.
>
> Perform an independent usability/product audit of the CURRENT Android app as
> rendered on the unlocked physical device.
>
> The purpose is to identify product/UX problems that the user and Control Tower
> may not have noticed.
>
> Do not assume the current information architecture, labels, controls, flows,
> or screen states are correct just because they were previously specified.
>
> Do not merely validate prior requirements.
>
> Challenge them where appropriate.
>
> ============================================================
> CONTEXT
> ============================================================
>
> The app is functionally advanced:
>
> - Privy authentication / embedded wallets
> - recoverable Sepolia transactions
> - ENSv2 issuer namespace
> - R1 UserRegistry
> - S1 PermissionedResolver
> - credential read model
> - issuer capability checks
> - My Keys
> - credential creation vertical
> - Settings
> - Developer Diagnostics
>
> No real staff credential has been created yet.
>
> Current branch contains uncommitted UI + staff vertical work.
>
> Do not edit it during this audit.
>
> ============================================================
> OBSERVED USER FEEDBACK — EVIDENCE, NOT PRESCRIPTIONS
> ============================================================
>
> The user reports:
>
> 1. The latest physical UI looks substantially better overall.
>
> 2. Some product behavior is still unclear.
>
> 3. On the screen currently called "Issuer", most credential fields appear
>    non-editable; the Artwork URI appears editable.
>
> 4. Pressing "Review credential" correctly opens a separate review surface with
>    information plus Back and Create credential actions.
>
> 5. However, on the Issuer screen itself, below Review credential, there is also
>    a section displaying:
>
>    "Creating credential"
>    "1 of 3 · Waiting for approval"
>
>    before the user has chosen Create credential.
>
> 6. The user is unsure whether "Issuer" is the right user-facing concept/label.
>
> 7. On My Keys there is an Import credential action, but the user questions
>    whether a wallet should discover credentials owned by the active address
>    automatically instead of requiring manual import.
>
> These are observations/questions.
>
> Do NOT assume the proposed solution is:
> - make every field editable;
> - remove Import;
> - rename Issuer;
> - change the state machine.
>
> Determine the correct product behavior from the actual app, architecture,
> constraints, and user mental model.
>
> ============================================================
> PHYSICAL AUDIT
> ============================================================
>
> Use ADB read-only inspection against the unlocked authorized physical device.
>
> Inspect screenshots and UI hierarchy where useful.
>
> Navigate safely through non-transactional product surfaces:
>
> - logged-out/authentication state if safely reachable without destroying state;
> - My Keys;
> - Issuer/current creation surface;
> - Review credential;
> - Settings;
> - Developer Diagnostics.
>
> Do NOT press any action that:
>
> - creates a credential;
> - signs;
> - broadcasts;
> - sends a transaction;
> - configures records;
> - resumes a write;
> - triggers holder proof.
>
> If an action's safety is uncertain, do not press it.
>
> ============================================================
> AUDIT AS A REAL USER
> ============================================================
>
> Evaluate the product without relying on knowledge of the source code first.
>
> Ask:
>
> - What does a first-time user think this product does?
> - What is the primary object: wallet, credential, key, access pass?
> - Is the active account model understandable?
> - Is issuer vs holder understandable?
> - Does the product explain what "Issuer" means?
> - Are actions discoverable?
> - Are controls that look editable actually editable?
> - Are read-only values visually distinguishable from inputs?
> - Are states shown only when contextually relevant?
> - Does any progress UI imply an action has started when it has not?
> - Does Back behave predictably?
> - Is there an obvious way to abandon an unsubmitted draft/review?
> - Is there an obvious difference between draft, review, submitted, confirmed,
>   configuring and ready?
> - Are blockchain concepts leaking unnecessarily into normal UX?
> - Does My Keys behave like users expect a wallet/credential wallet to behave?
> - Is manual Import conceptually correct as primary behavior, fallback behavior,
>   or neither?
> - What happens when the active wallet changes?
> - Is one address treated as if it were the entire wallet/account model?
> - Are there assumptions that work only for the current demo credential?
> - Can a normal issuer understand what can/cannot be changed?
> - Is the creation surface actually a credential creator or merely a fixed demo
>   template presented as one?
> - Are expiry/access/transferability/recipient concepts understandable?
> - Does the review give enough confidence before a write?
> - Is the two-transaction nature explained at the right time?
> - Are recovery states actionable?
> - Does Settings follow normal wallet expectations?
> - Are Developer Diagnostics sufficiently separated?
> - Would a judge understand the product without verbal explanation?
>
> ============================================================
> CODE-AWARE SECOND PASS
> ============================================================
>
> After the user-first physical audit, inspect the relevant implementation.
>
> Determine WHY each notable behavior exists.
>
> Do not edit.
>
> Specifically inspect:
>
> - UI state derivation;
> - current issuer creation model;
> - draft/review/progress state boundaries;
> - transaction state mapping;
> - credential local index;
> - My Keys import/discovery behavior;
> - account/wallet partitioning;
> - issuer capability gating;
> - any hard-coded demo assumptions.
>
> Distinguish:
>
> INTENTIONAL DEMO CONSTRAINT
>
> from:
>
> ACCIDENTAL UX LIMITATION
>
> from:
>
> ARCHITECTURAL LIMITATION
>
> from:
>
> BUG / STATE PRESENTATION BUG
>
> ============================================================
> CREDENTIAL DISCOVERY ANALYSIS
> ============================================================
>
> Analyze, read-only, what the current architecture can truthfully support for
> automatic credential discovery.
>
> Do not implement it.
>
> Evaluate at least:
>
> A. Current local explicit-import index.
>
> B. Discovering credentials issued by our known R1 through relevant ENSv2 /
>    registry events plus authoritative current-state readback.
>
> C. Any existing contract enumeration capability, if present.
>
> D. Need for an indexer/backend only if truly necessary.
>
> E. Behavior with multiple active wallet addresses/accounts.
>
> Clarify the difference between:
>
> "discover every ENS asset globally owned by this address"
>
> and:
>
> "discover credentials belonging to this product/issuer namespace."
>
> Do not claim capability that current ENSv2 contracts do not provide.
>
> ============================================================
> CREATE-CREDENTIAL PRODUCT MODEL
> ============================================================
>
> Audit whether the current creation surface should be:
>
> - a fixed demo template;
> - a configurable template;
> - a genuinely generic credential creator;
> - or another model.
>
> Consider hackathon scope and actual ENS capabilities.
>
> Identify which fields SHOULD be:
>
> editable
>
> fixed by namespace/product
>
> derived automatically
>
> advanced/hidden
>
> Do not change anything yet.
>
> ============================================================
> NAMING / INFORMATION ARCHITECTURE
> ============================================================
>
> Audit user-facing labels independently.
>
> Examples to question, not necessarily replace:
>
> Issuer
> My Keys
> Import credential
> Create credential
> Access
> Transferability
>
> Recommend alternatives only where they materially improve comprehension.
>
> Avoid jargon for normal users while preserving technical accuracy.
>
> ============================================================
> PRIORITIZATION
> ============================================================
>
> Every finding must be classified:
>
> P0 — misleading/dangerous before a real blockchain write
>
> P1 — core product usability problem before demo
>
> P2 — useful improvement if time permits
>
> P3 — post-hackathon
>
> Also classify each as:
>
> BUG
> STATE PRESENTATION
> UX
> PRODUCT GAP
> ARCHITECTURE
> POLISH
>
> Estimate:
>
> SMALL
> MEDIUM
> LARGE
>
> for implementation effort.
>
> ============================================================
> DO NOT OVER-SCOPE
> ============================================================
>
> This is an AUDIT.
>
> Do not:
>
> - redesign the entire product;
> - implement findings;
> - add dependencies;
> - modify Gradle;
> - edit docs;
> - change tests;
> - commit;
> - push.
>
> Do not treat every possible wallet feature as required.
>
> The output should help Control Tower decide the smallest set of changes needed
> before staff-001 is created.
>
> ============================================================
> RETURN
> ============================================================
>
> # PHYSICAL PRODUCT / UX AUDIT
>
> ## EXECUTIVE VERDICT
>
> Is the app ready to create the first real staff credential now?
>
> YES / NO / YES WITH REQUIRED FIXES
>
> Why:
>
> ## USER MENTAL MODEL
>
> What the app currently communicates:
>
> What it should communicate:
>
> Main mismatch:
>
> ## CRITICAL FINDINGS
>
> For each:
>
> ID:
> Severity:
> Type:
> Observed behavior:
> Why it matters:
> Likely cause:
> Recommended product behavior:
> Estimated effort:
>
> ## ISSUER / CREATION AUDIT
>
> Current model:
>
> Is "Issuer" a good user-facing label?
>
> Editable vs fixed fields:
>
> Draft/review/progress findings:
>
> ## MY KEYS / DISCOVERY AUDIT
>
> Current behavior:
>
> What users reasonably expect:
>
> What can be supported today without an indexer:
>
> What requires future infrastructure:
>
> Role of manual import:
>
> Multi-wallet/account implications:
>
> ## TRANSACTION / RECOVERY UX
>
> ## SETTINGS / ACCOUNT MODEL
>
> ## INFORMATION ARCHITECTURE
>
> ## DEMO / JUDGE READABILITY
>
> What would confuse a judge:
>
> What is already strong:
>
> ## HIDDEN ASSUMPTIONS
>
> List hard-coded/demo-specific assumptions that are currently visible as if
> they were general product behavior.
>
> ## PRIORITIZED FIX SET
>
> MINIMUM BEFORE STAFF WRITE:
>
> Only P0/P1 items truly required.
>
> DO NOT include optional polish here.
>
> AFTER STAFF / BEFORE VIDEO:
>
> POST-HACKATHON:
>
> ## PHYSICAL EVIDENCE
>
> Screens inspected:
>
> Device:
>
> No write actions performed:
>
> ## CODE INSPECTION
>
> Relevant files reviewed:
>
> No modifications:
>
> ## BLOCKCHAIN / SIGNING
>
> Writes:
> 0
>
> Signatures:
> 0
>
> ## GIT
>
> No changes.
>
> End exactly:
>
> UX AUDIT COMPLETE — NO IMPLEMENTATION PERFORMED
