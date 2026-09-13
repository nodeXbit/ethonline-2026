# Project task packet 076: TASK — POLISHED PRODUCT UI PASS BEFORE STAFF CREATION

- Classification: **ORIGINAL / SANITIZED**
- Redactions applied: none
- Source: retained project-specific Control Tower packet in the local Codex record

---

> TASK — POLISHED PRODUCT UI PASS BEFORE STAFF CREATION
>
> CONTEXT
>
> The staff credential vertical is functionally complete and fully tested.
>
> Physical inspection shows the app still looks like a development harness.
>
> Control Tower has decided to do ONE polished UI/UX pass BEFORE the first real
> staff credential is created.
>
> This is intentional.
>
> The goal is not merely to hide diagnostics.
>
> The goal is to make the Android application look like a credible, polished
> hackathon product suitable for the final demo video while preserving all
> existing blockchain logic exactly.
>
> NO blockchain writes.
>
> NO NFC/HCE changes.
>
> NO ENS transaction/ABI changes.
>
> NO backend.
>
> NO architectural rewrite.
>
> ============================================================
> PRODUCT FEEL
> ============================================================
>
> Target feel:
>
> modern
> minimal
> professional
> Ethereum-native without looking like a crypto dashboard
> clear physical-access / credential identity
> high contrast
> comfortable spacing
> strong card-based credential presentation
> demo-video friendly
>
> Avoid:
>
> generic gray test-harness appearance
> dense technical text
> raw addresses everywhere
> over-decoration
> gradients everywhere
> cyberpunk clichés
> fake wallet balances/data
> unnecessary animations
>
> ============================================================
> VISUAL DIRECTION
> ============================================================
>
> Create a small coherent visual language using the CURRENT Android UI stack.
>
> Do NOT migrate to Compose or add a large UI framework.
>
> Prefer existing/native Material/AppCompat capabilities already available.
>
> Use:
>
> - consistent spacing scale
> - clear heading/body hierarchy
> - rounded cards where available
> - restrained elevation
> - meaningful status chips/badges
> - truncated addresses
> - clear primary/secondary actions
> - good empty states
> - compact network indicator
> - consistent section padding
> - accessible touch targets
> - sensible dark/light handling if current app supports it
>
> Do not introduce arbitrary dependencies merely for appearance.
>
> ============================================================
> APP SHELL
> ============================================================
>
> Normal authenticated app should feel like three conceptual areas:
>
> MY KEYS
> ISSUER
> SETTINGS
>
> Do NOT implement Wallet yet.
>
> Use the simplest navigation suitable for the existing UI technology:
>
> tabs, segmented controls, bottom navigation, or equivalent.
>
> Choose the approach that requires the smallest safe change while looking
> polished.
>
> MY KEYS is default.
>
> ISSUER only appears or enables when fresh onchain authority confirms it.
>
> SETTINGS contains account/switch-account and Developer Diagnostics.
>
> ============================================================
> TOP AREA
> ============================================================
>
> Replace the current "ENSv2 Access Demo" test-harness feeling.
>
> Preferred product title:
>
> ENS Access
>
> Small secondary network indicator:
>
> Sepolia
>
> Authenticated identity should be compact, for example:
>
> Issuer
> 0xFa90…2685
>
> or:
>
> Holder
> 0x3419…5FF7
>
> Do not display giant full addresses in the main shell.
>
> Full address can remain available in details/copy action.
>
> ============================================================
> LOGGED OUT EXPERIENCE
> ============================================================
>
> When logged out, present a clean sign-in screen.
>
> Suggested hierarchy:
>
> ENS Access
>
> Programmable credentials powered by ENS
>
> Email
> [________________]
>
> [ Send code ]
>
> Verification code
> [______]
>
> [ Sign in ]
>
> Do not show:
>
> My Keys
> Issuer
> M1
> Gate B
> Gate C2
> raw wallet controls
> Developer Diagnostics
>
> before authentication.
>
> Keep exact existing Privy behavior.
>
> ============================================================
> MY KEYS
> ============================================================
>
> Empty state should look intentional.
>
> Suggested:
>
> MY KEYS
>
> Your credentials will appear here.
>
> [ Import credential ]
>
> Do not leave the credential name text field permanently dominating the screen.
>
> When Import is selected, reveal a compact input/dialog/sheet using the current
> UI capabilities.
>
> Existing ownership verification remains exact.
>
> ============================================================
> CREDENTIAL CARD DESIGN
> ============================================================
>
> Implement a reusable visual card for credentials.
>
> For STAFF conceptually show:
>
> STAFF ACCESS
>
> staff-001.keys.demo-access.eth
>
> [ ALLOWED ]
>
> Valid until
> 31 Oct 2026
>
> Non-transferable
>
> Description:
> Staff Access Pass
>
> Artwork area:
> - image if a valid supported URI is available and rendering is already safe;
> - otherwise use a polished deterministic placeholder.
>
> Do NOT add a new network image stack merely for this task if one does not
> already exist.
>
> The placeholder should still make the card visually deliberate.
>
> Do not invent onchain artwork data.
>
> ============================================================
> ISSUER
> ============================================================
>
> Make Create Credential feel like a product flow, not a form dump.
>
> Visually group:
>
> IDENTITY
> Name
> ENS name
>
> RECIPIENT
> Recipient address
>
> POLICY
> Expires
> Transferability
> Initial access
>
> PRESENTATION
> Artwork URI
> Description
>
> Primary button:
>
> Review credential
>
> Use human-readable values first.
>
> Technical exact values remain available only in details/diagnostics where
> useful.
>
> ============================================================
> REVIEW CREDENTIAL
> ============================================================
>
> This screen matters because it will be recorded and used for a real write.
>
> Make it visually strong and extremely clear.
>
> Heading:
>
> Review credential
>
> Show a preview card.
>
> Then:
>
> Credential
> staff-001.keys.demo-access.eth
>
> Recipient
> 0x3419…5FF7
>
> Access
> Allowed
>
> Expires
> 31 Oct 2026, 23:59
>
> Transferability
> Non-transferable
>
> Description
> Staff Access Pass
>
> Artwork
> Not set
> (if blank)
>
> Network
> Sepolia
>
> Then a clear primary CTA:
>
> Create credential
>
> and secondary:
>
> Back
>
> Do NOT expose normal users to:
>
> calldata
> role bitmaps
> nonce
> operation UUID
> internal enums
>
> Preserve the exact existing transaction review/safety semantics underneath.
>
> ============================================================
> TWO-TRANSACTION UX
> ============================================================
>
> Do not hide the fact that creation requires blockchain confirmation, but do not
> show raw internal states.
>
> Human states should be:
>
> Preparing
>
> Creating credential
>
> Registration confirmed
>
> Configuring credential
>
> Verifying onchain
>
> Credential ready
>
> If registration succeeds and records are incomplete:
>
> Credential created
> Setup incomplete
>
> [ Resume setup ]
>
> Do not call register again.
>
> Underlying state machine remains unchanged.
>
> ============================================================
> SETTINGS
> ============================================================
>
> Settings should contain:
>
> Account
>
> Current wallet
> full address + Copy
>
> Switch account / Log out
>
> Network
> Sepolia
>
> Developer options
> [ Developer diagnostics ]
>
> Do not display M1 admission or HCE/Gate controls elsewhere.
>
> ============================================================
> DEVELOPER DIAGNOSTICS
> ============================================================
>
> Preserve all existing diagnostics.
>
> Move:
>
> M1 admission
> HCE signer
> Gate B
> Gate C2
> raw technical transaction/journal state
> signature copy tooling
>
> into a separate Developer Diagnostics screen/surface.
>
> This surface may remain visually utilitarian.
>
> Normal product path must not depend on it.
>
> ============================================================
> BRANDING / ASSETS
> ============================================================
>
> Do not block on custom illustration work.
>
> If the app currently has no polished iconography/assets:
>
> - use built-in/vector icons already available;
> - create very small vector/shape resources locally if needed;
> - do not add a giant icon library.
>
> A custom STAFF artwork asset is NOT required yet.
>
> The credential card may use a tasteful deterministic placeholder until real
> artwork is chosen.
>
> ============================================================
> ACCESSIBILITY / DEMO QUALITY
> ============================================================
>
> Ensure:
>
> text does not clip on the physical device
>
> long ENS names wrap/truncate intentionally
>
> addresses are readable
>
> buttons have clear enabled/disabled states
>
> important status does not rely only on color
>
> screenshots look coherent at the device's current resolution
>
> no giant vertical dead zones
>
> no developer text bleeding into product surfaces
>
> ============================================================
> NO SCOPE CREEP
> ============================================================
>
> Do NOT:
>
> change ENS logic
> change transaction engine
> change Privy
> change Web3j dependency
> change ABI
> touch HCE/NFC
> touch Node verifier
> touch firmware
> implement Wallet
> implement QR
> implement transfer
> implement avatar upload
> add animations beyond trivial existing-native transitions
> rewrite architecture
>
> ============================================================
> TESTS
> ============================================================
>
> Preserve all current tests.
>
> Add/update focused UI/state tests for:
>
> logged out shell
>
> issuer shell
>
> holder shell
>
> navigation visibility
>
> issuer gated by onchain authority
>
> diagnostics reachable but not inline
>
> staff review displays exact human values
>
> resume setup remains reachable
>
> no technical/raw fields in normal review
>
> No real blockchain writes.
>
> ============================================================
> VALIDATION
> ============================================================
>
> Run:
>
> .\gradlew.bat testDebugUnitTest
> .\gradlew.bat assembleDebug
>
> node --test
>
> git diff --check
>
> No commit/push.
>
> Return APK path and SHA-256.
>
> ============================================================
> RETURN
> ============================================================
>
> # POLISHED PRODUCT UI — READY
>
> ## DESIGN DIRECTION
>
> ## LOGGED OUT
>
> ## MY KEYS
>
> ## CREDENTIAL CARD
>
> ## ISSUER
>
> ## REVIEW
>
> ## SETTINGS
>
> ## DEVELOPER DIAGNOSTICS
>
> ## LOGIC PRESERVED
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
> ## APK
>
> Path:
> SHA-256:
>
> ## BLOCKCHAIN WRITES
>
> 0
>
> ## GIT
>
> No commit/push.
>
> End exactly:
>
> POLISHED UI: READY FOR PHYSICAL REVIEW
