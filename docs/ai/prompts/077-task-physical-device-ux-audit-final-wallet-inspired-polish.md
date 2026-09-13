# Project task packet 077: TASK — PHYSICAL-DEVICE UX AUDIT + FINAL WALLET-INSPIRED POLISH

- Classification: **ORIGINAL / SANITIZED**
- Redactions applied: none
- Source: retained project-specific Control Tower packet in the local Codex record

---

> TASK — PHYSICAL-DEVICE UX AUDIT + FINAL WALLET-INSPIRED POLISH
>
> CONTEXT
>
> The polished product shell is implemented and installed on the physical Android
> device.
>
> The device is now UNLOCKED and ADB-authorized.
>
> Physical first impression is positive, but the user has already noticed:
>
> - some content feels too close to the physical screen edges;
> - overall UX can still become substantially more wallet-like and intuitive;
> - we want ONE final serious UX refinement before the first real staff
>   credential transaction.
>
> Do not perform any blockchain write.
>
> Do not sign anything.
>
> Do not create staff-001.
>
> Do not change blockchain/ENS/Privy/ABI/transaction semantics.
>
> This is UI/UX only.
>
> ============================================================
> PHASE 0 — INSPECT THE ACTUAL PHYSICAL DEVICE
> ============================================================
>
> Use the existing local Android SDK adb.
>
> Do not ask the user to manually provide screenshots unless ADB inspection fails.
>
> Find/use adb from the existing SDK.
>
> Confirm exactly one expected physical device is attached.
>
> Collect:
>
> - physical screen resolution;
> - density;
> - Android/system bar information relevant to layout;
> - current app screenshot;
> - UI hierarchy / bounds.
>
> Use read-only commands such as:
>
> adb devices
> adb shell wm size
> adb shell wm density
> adb exec-out screencap -p
> adb shell uiautomator dump
>
> Store temporary screenshots/dumps only under ignored .runtime/.
>
> Inspect the actual rendered PNGs if supported.
>
> Also inspect UI hierarchy bounds numerically.
>
> Specifically audit:
>
> - distance from physical left/right edges;
> - status bar inset;
> - navigation/gesture inset;
> - long ENS-name wrapping;
> - cards touching edges;
> - input/button width;
> - CTA placement;
> - vertical rhythm;
> - clipping;
> - text density;
> - touch targets.
>
> If the app currently uses edge-to-edge rendering, verify WindowInsets are applied
> correctly.
>
> Do NOT disable modern edge-to-edge behavior merely as a shortcut if correct
> insets solve it.
>
> ============================================================
> SAFE PHYSICAL NAVIGATION
> ============================================================
>
> ADB interaction may navigate ONLY through non-transactional UI to inspect:
>
> MY KEYS
> ISSUER
> REVIEW CREDENTIAL
> SETTINGS
> DEVELOPER DIAGNOSTICS
>
> It may press Review credential because Review itself causes no blockchain write.
>
> It MUST NOT press:
>
> Create credential
> Configure credential
> Resume setup
> any Privy approval
> any signing action
> Gate signing controls
> M1 write controls
>
> If safe navigation cannot be established confidently from the UI hierarchy,
> STOP that navigation step and ask the user to open that screen manually.
>
> ZERO blockchain writes.
>
> ZERO signatures.
>
> ============================================================
> UX REFERENCE DIRECTION
> ============================================================
>
> Do not clone another application's trade dress.
>
> Use established wallet interaction patterns selectively.
>
> PHANTOM-LIKE PRINCIPLES:
>
> - wallet/account identity compact at the top;
> - current account context immediately understandable;
> - primary owned assets/collectibles dominate Home;
> - actions and settings do not overwhelm content;
> - intentional empty states.
>
> ZERION-LIKE PRINCIPLES:
>
> - human-readable language first;
> - user understands expected result before signing;
> - state and activity are understandable without blockchain vocabulary;
> - progressive disclosure for technical information.
>
> RAINBOW-LIKE PRINCIPLES:
>
> - strong but restrained visual personality;
> - friendly rather than enterprise/admin-tool appearance;
> - crypto complexity hidden from the main experience.
>
> ENS ACCESS MUST STILL HAVE ITS OWN IDENTITY.
>
> The product is a credential/key wallet, not a token portfolio.
>
> ============================================================
> INFORMATION ARCHITECTURE
> ============================================================
>
> Treat MY KEYS as the equivalent of wallet Home.
>
> Main authenticated experience:
>
> MY KEYS
> ISSUER (authorized issuer only)
> SETTINGS
>
> No Wallet tab yet.
>
> Do not create functionality that does not exist.
>
> ============================================================
> MY KEYS — TARGET EXPERIENCE
> ============================================================
>
> The first impression should be:
>
> this is where my digital keys live.
>
> Use an intentional top section containing compact:
>
> ENS Access
>
> account identity / role
>
> Sepolia indicator
>
> Then credential content.
>
> Avoid making the wallet address the hero.
>
> EMPTY STATE:
>
> Use deliberate whitespace and a visual credential/pass placeholder or suitable
> existing local vector treatment.
>
> Human copy similar to:
>
> No credentials yet
>
> Credentials you receive or import will appear here.
>
> Primary:
> Import credential
>
> Avoid permanent raw input clutter.
>
> ============================================================
> CREDENTIAL AS THE HERO OBJECT
> ============================================================
>
> Credentials should feel more like:
>
> digital access cards / passes
>
> and less like:
>
> database rows.
>
> The STAFF card should have clear hierarchy:
>
> STAFF ACCESS
>
> staff-001.keys.demo-access.eth
>
> ALLOWED
>
> valid until 31 Oct 2026
>
> Non-transferable
>
> Artwork/placeholder
>
> Do not try to fit every technical property on the card.
>
> Tap/detail can expose more.
>
> Status must remain legible without relying on color alone.
>
> ============================================================
> ISSUER — TARGET EXPERIENCE
> ============================================================
>
> The current long form can be improved.
>
> Make it feel like creating a credential rather than filling in a developer
> configuration form.
>
> Prioritize:
>
> Credential identity
> Recipient
> Access policy
> Presentation
>
> Use card/section grouping and progressive disclosure.
>
> The fixed staff vertical is acceptable for this hackathon.
>
> Do not build a generic credential-template system now.
>
> Consider keeping a visible preview of the resulting credential if it improves
> the composition without major complexity.
>
> Primary CTA should remain visually anchored and obvious:
>
> Review credential
>
> Avoid placing the CTA or fields against the screen edge.
>
> ============================================================
> REVIEW — MOST IMPORTANT SCREEN
> ============================================================
>
> This is a wallet-equivalent transaction review.
>
> It should answer immediately:
>
> WHAT am I creating?
> WHO receives it?
> WHAT access does it grant?
> WHEN does it expire?
> CAN it be transferred?
> WHICH network?
> WHAT happens next?
>
> Prefer outcome-first language.
>
> Conceptual layout:
>
> Review credential
>
> [ STAFF PASS PREVIEW ]
>
> staff-001.keys.demo-access.eth
>
> Recipient
> 0x3419…5FF7
>
> Access
> Allowed
>
> Valid until
> 31 Oct 2026 · 23:59
>
> Transfer
> Non-transferable
>
> Network
> Sepolia
>
> Then explain compactly:
>
> Creation uses two Sepolia transactions:
> 1. Create credential
> 2. Configure access
>
> Do NOT introduce technical contract terminology here.
>
> Primary:
> Create credential
>
> Secondary:
> Back
>
> Preserve the exact existing underlying two-transaction safety model.
>
> ============================================================
> TRANSACTION PROGRESS UX
> ============================================================
>
> Map technical state into a small understandable progress experience:
>
> 1  Creating credential
> 2  Configuring access
> 3  Verifying onchain
>
> Human states:
>
> Waiting for approval
> Submitted
> Confirming
> Confirmed
>
> Never say "finalized" unless the actual code proves finality.
>
> Hash/explorer access may live under Details.
>
> If status cannot be determined:
>
> Status unavailable
>
> not Failed unless failure is proven.
>
> ============================================================
> SETTINGS
> ============================================================
>
> Follow familiar wallet settings hierarchy:
>
> Account
> Network
> Developer options
>
> Account:
> compact role/name
> full address
> Copy address
> Switch account / Log out
>
> Network:
> Sepolia
>
> Developer options:
> Developer diagnostics
>
> Diagnostics must remain visually subordinate.
>
> ============================================================
> PHYSICAL SPACING REQUIREMENT
> ============================================================
>
> Use the actual device inspection to define a consistent safe layout.
>
> Do not hard-code arbitrary fixes screen by screen.
>
> Create/reuse a small spacing policy.
>
> Target a comfortable horizontal content gutter appropriate for the physical
> device, likely around the normal Android 16–24dp range, but derive/finalize it
> from existing components and screenshot inspection.
>
> All cards, labels, text fields and CTAs must respect the same content gutter.
>
> Account for system WindowInsets separately.
>
> Buttons must not visually touch the display edge.
>
> No important text may render under system UI.
>
> ============================================================
> VISUAL SYSTEM
> ============================================================
>
> Keep the current restrained purple direction if it works physically.
>
> Refine rather than restart.
>
> Create consistency for:
>
> page title
> section title
> body
> secondary text
> credential cards
> status badges
> primary CTA
> secondary CTA
> text fields
> network chip
> account chip
>
> Do not add a UI/design-system dependency.
>
> Do not add network image libraries.
>
> Do not create flashy gradients/cyberpunk motifs.
>
> Light and dark themes must remain coherent.
>
> ============================================================
> PHYSICAL ITERATION
> ============================================================
>
> After implementing:
>
> build debug APK
>
> install with adb install -r
>
> DO NOT clear app data.
>
> With the device still unlocked, inspect the real rendered app again.
>
> Capture the same screens.
>
> Compare BEFORE vs AFTER for:
>
> edge spacing
> hierarchy
> readability
> density
> card composition
> CTA placement
> system insets
>
> If there is an obvious physical clipping/padding defect, fix it within this same
> task.
>
> Do not begin another conceptual redesign.
>
> ============================================================
> NO-TOUCH
> ============================================================
>
> Do not change:
>
> CredentialAbi
> ENS write semantics
> CredentialReader authorization semantics
> RecoverableTransactionEngine semantics
> Privy version/integration
> Web3j
> R1/S1 configuration
> credential constants
> HCE/NFC
> Node verifier
> firmware
>
> No blockchain writes.
>
> No signing.
>
> No staff creation.
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
> Preserve all existing tests.
>
> Add only small UI-policy/state tests when useful.
>
> No commit/push.
>
> ============================================================
> RETURN
> ============================================================
>
> # PHYSICAL UX POLISH — READY
>
> ## DEVICE
>
> Resolution:
> Density:
> Insets/findings:
>
> ## PHYSICAL BEFORE AUDIT
>
> List concrete observed issues.
>
> ## DESIGN DECISIONS
>
> Explain what was borrowed conceptually from wallet UX patterns and why.
>
> ## MY KEYS
>
> ## CREDENTIAL CARD
>
> ## ISSUER
>
> ## REVIEW
>
> ## TRANSACTION PROGRESS
>
> ## SETTINGS
>
> ## EDGE / INSET FIX
>
> Explain exact systematic fix.
>
> ## PHYSICAL AFTER AUDIT
>
> Screens inspected:
> Remaining known visual issues:
>
> ## LOGIC UNCHANGED
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
> ## SECURITY
>
> Blockchain writes:
> 0
>
> Signatures:
> 0
>
> ## GIT
>
> No commit/push.
>
> End exactly:
>
> PHYSICAL UX: READY FOR FINAL USER REVIEW
