# Project task packet 087: TASK — MOVE PRIMARY WALLET SWITCHER INTO GLOBAL APP HEADER

- Classification: **ORIGINAL / SANITIZED**
- Redactions applied: none
- Source: retained project-specific Control Tower packet in the local Codex record

---

> TASK — MOVE PRIMARY WALLET SWITCHER INTO GLOBAL APP HEADER
>
> CONTEXT
>
> Pass Wallet Polish is functionally complete and physically reviewed.
>
> Do NOT start another redesign.
>
> One product-intent mismatch remains:
>
> The wallet selector was implemented primarily inside Settings.
>
> The user's intended UX was for the ACTIVE WALLET control at the TOP OF THE APP
> to be the primary wallet selector, similar to normal wallet applications.
>
> This is a small final interaction correction inside the current workstream.
>
> NO blockchain writes.
> NO signing.
> NO ENS changes.
> NO NFC/HCE.
> NO Node/firmware.
> NO artwork redesign.
>
> ============================================================
> OBJECTIVE
> ============================================================
>
> Turn the existing global top active-wallet chip/control into the primary
> interactive wallet selector.
>
> Current conceptual header:
>
> ENS Access
>
> Active wallet · 0x3419…5FF7       Sepolia
>
> Target conceptual behavior:
>
> ENS Access
>
> 0x3419…5FF7     Copy     ▼         Sepolia
>
> Exact composition may adapt to physical width.
>
> The user should immediately understand:
>
> - this is the currently active wallet;
> - it can be tapped to change wallet;
> - the address can be copied;
> - Sepolia is the current network context.
>
> ============================================================
> GLOBAL WALLET CONTROL
> ============================================================
>
> The control must appear consistently on authenticated product destinations:
>
> - My Keys
> - Issue/Issuer surface when capability exists
> - Settings
>
> It must use the existing ActiveWallet architecture.
>
> Do NOT duplicate wallet-selection state.
>
> Requirements:
>
> - truncated active address;
> - clear dropdown/expand affordance;
> - inline copy affordance;
> - wallet selector opens by tapping the main wallet area;
> - Copy has its own safe action/hit target and does not accidentally open selector;
> - accessible touch targets;
> - current active wallet obvious;
> - no Privy internal IDs.
>
> ============================================================
> WALLET SELECTOR
> ============================================================
>
> When opened:
>
> SELECT WALLET
>
> ✓ active wallet address
>   other wallet address
>   ...
>
> Each actual authenticated embedded Ethereum wallet must be selectable.
>
> At bottom:
>
> + Create new wallet
>
> If only one wallet exists:
>
> ✓ current wallet
>
> + Create new wallet
>
> Creating a wallet remains an explicit user action.
>
> Do NOT automatically create a wallet.
>
> Switching wallet must continue to:
>
> - update ActiveWallet;
> - refresh My Keys for the selected address;
> - restore that wallet's selected pass;
> - recalculate Can issue capability;
> - never require logout;
> - never perform a blockchain transaction.
>
> ============================================================
> SETTINGS AFTER THIS CHANGE
> ============================================================
>
> Do not keep two equally prominent wallet selectors.
>
> Settings should become the detailed account-management surface, while the global
> header is the fast switcher.
>
> Keep useful Settings functionality:
>
> Account
> full address/copy if useful
> Create/manage wallets if useful
> Switch account / Log out
> Network
> Developer options
>
> But simplify duplicated wallet-selection UI where appropriate.
>
> Do not remove functionality.
>
> The mental model should be:
>
> HEADER
> → quick active-wallet switch
>
> SETTINGS
> → account management/details
>
> ============================================================
> NETWORK
> ============================================================
>
> Keep current Sepolia Testnet presentation.
>
> Do NOT implement network switching.
>
> The existing compact Sepolia indicator may remain in the header.
>
> No fake interactive network control.
>
> ============================================================
> PHYSICAL LAYOUT
> ============================================================
>
> Use the real device width:
>
> 1200 px
> density 3.0
>
> Avoid overcrowding the header.
>
> If full text labels like "Copy" make the top bar too crowded, prefer a compact
> copy icon/vector with proper accessibility description.
>
> Do not reduce touch targets below accessibility-safe size.
>
> The address, copy action, dropdown affordance and Sepolia indicator must all fit
> comfortably.
>
> Inspect physically after implementation.
>
> ============================================================
> PRESERVE
> ============================================================
>
> Do not change:
>
> - session hydration;
> - Privy multi-wallet behavior;
> - R1 autodiscovery;
> - selected pass state;
> - pass artwork loading;
> - pass visual design;
> - stacking policy;
> - credential details;
> - issuer capability;
> - transaction/recovery semantics.
>
> ============================================================
> TESTS
> ============================================================
>
> Add/update focused tests for:
>
> - header shows current active wallet;
> - header wallet area opens selector;
> - inline copy does not change active wallet;
> - selector lists actual embedded wallets;
> - current wallet indicated;
> - selecting second wallet updates ActiveWallet;
> - switching back restores first wallet partition/selected pass;
> - Create new wallet action is present but never automatic;
> - header behavior consistent across My Keys / issuer-capable surface / Settings;
> - Settings no longer contains a confusing duplicate primary selector.
>
> Keep all existing tests green.
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
> git diff --check
>
> Install using adb install -r.
>
> Do not clear data.
>
> Physically verify:
>
> 1. My Keys header.
> 2. Wallet selector opened from header.
> 3. Select secondary wallet.
> 4. My Keys becomes correct for secondary wallet.
> 5. Switch back from header.
> 6. STAFF returns and remains selected.
> 7. Settings remains understandable and non-duplicative.
>
> ZERO blockchain writes.
> ZERO signatures.
>
> No commit/push yet.
>
> ============================================================
> RETURN
> ============================================================
>
> # GLOBAL WALLET SWITCHER — READY
>
> ## HEADER
>
> Before:
>
> After:
>
> Copy behavior:
>
> Selector behavior:
>
> ## WALLET SWITCH
>
> Primary -> secondary:
>
> Secondary -> primary:
>
> My Keys partition:
>
> Selected pass:
>
> Issuer capability:
>
> ## SETTINGS
>
> What remains:
>
> What duplication was removed:
>
> ## NETWORK
>
> Unchanged:
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
> Known visual issues:
>
> ## SECURITY
>
> Blockchain writes:
> 0
>
> Signatures:
> 0
>
> Wallets auto-created:
> 0
>
> ## GIT
>
> No commit/push.
>
> End exactly:
>
> GLOBAL WALLET SWITCHER: READY FOR PHYSICAL REVIEW
