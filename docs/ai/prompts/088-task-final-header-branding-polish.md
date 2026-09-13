# Project task packet 088: TASK — FINAL HEADER BRANDING POLISH

- Classification: **ORIGINAL / SANITIZED**
- Redactions applied: none
- Source: retained project-specific Control Tower packet in the local Codex record

---

> TASK — FINAL HEADER BRANDING POLISH
>
> CONTEXT
>
> Pass Wallet Polish and the global wallet switcher are functionally complete.
>
> Before checkpoint, make three final small product-branding/header adjustments.
>
> NO blockchain changes.
> NO wallet-state changes.
> NO ENS changes.
> NO artwork redesign.
> NO NFC/HCE.
> NO Node/firmware.
> NO commit/push yet.
>
> ============================================================
> 1 — PRODUCT BRAND
> ============================================================
>
> The project/product name is now:
>
> LockENS
>
> Use this exact capitalization.
>
> Replace the user-facing product title:
>
> ENS Access
>
> with:
>
> LockENS
>
> at least in:
>
> - authenticated global header;
> - logged-out authentication screen;
> - RESTORING_SESSION / startup shell;
> - Android launcher/app label if it currently exposes ENS Access.
>
> Do NOT rename:
>
> - Java/Kotlin package names;
> - ENS namespaces;
> - config keys;
> - contract concepts;
> - historical technical documentation merely for branding.
>
> Where a short explanatory subtitle exists or is useful, wording such as:
>
> Programmable access credentials powered by ENS
>
> is acceptable.
>
> Do not clutter the authenticated header with a subtitle.
>
> ============================================================
> 2 — COPY ICON IN GLOBAL WALLET CONTROL
> ============================================================
>
> Replace the visible text action:
>
> Copy
>
> in the GLOBAL HEADER wallet control with a compact copy/clipboard icon.
>
> Requirements:
>
> - use an existing local/vector Android drawable or add a tiny local vector;
> - no icon dependency;
> - proper contentDescription/accessibility label:
>   "Copy wallet address";
> - accessible hit target;
> - tapping the icon copies the validated public wallet address;
> - tapping it MUST NOT open the wallet selector;
> - wallet selector main area remains separately tappable.
>
> Settings may also use the icon if that makes the product more coherent, but do
> not remove useful address-copy functionality.
>
> ============================================================
> 3 — ETHEREUM ICON NEXT TO SEPOLIA
> ============================================================
>
> The global header currently shows:
>
> Sepolia
>
> Add the existing Ethereum network mark/icon next to it.
>
> Reuse the already implemented local drawable:
>
> ic_ethereum_network.xml
>
> if appropriate.
>
> Conceptually:
>
> ◇ Sepolia
>
> or:
>
> [Ethereum mark] Sepolia
>
> Requirements:
>
> - compact;
> - visually aligned;
> - does not make the network control look interactive;
> - network remains static;
> - no dropdown indicator;
> - no network switching;
> - accessible label should still communicate:
>   "Sepolia Testnet".
>
> Keep the detailed Settings network card:
>
> Sepolia Testnet
> Ethereum · Chain ID 11155111
>
> unchanged unless a tiny consistency adjustment is necessary.
>
> ============================================================
> PHYSICAL HEADER CONSTRAINT
> ============================================================
>
> Current physical device:
>
> 1200 px width
> density 3.0
>
> The header must comfortably contain:
>
> LockENS
>
> active-wallet selector:
> address + copy icon + dropdown affordance
>
> Ethereum icon + Sepolia
>
> Do not shrink text excessively.
>
> Maintain safe touch targets and current gutters.
>
> If necessary, use icons rather than text to preserve space.
>
> ============================================================
> PRESERVE
> ============================================================
>
> Do not change:
>
> - ActiveWallet behavior;
> - multi-wallet switching;
> - wallet selector contents;
> - Create new wallet;
> - My Keys discovery;
> - selected pass;
> - capability calculation;
> - pass artwork;
> - pass stack;
> - Settings behavior;
> - transactions/recovery.
>
> ============================================================
> TESTS
> ============================================================
>
> Add/update only focused tests as useful:
>
> - LockENS shown as product brand;
> - copy icon action remains isolated from wallet selector;
> - Ethereum icon/network presentation remains non-interactive;
> - accessibility descriptions exist.
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
> Install with:
>
> adb install -r
>
> Do not clear data.
>
> Physically inspect:
>
> 1. My Keys header.
> 2. Header wallet selector.
> 3. Copy icon behavior.
> 4. Settings.
> 5. Cold-start/restoring state if safely observable.
>
> ZERO blockchain writes.
> ZERO signatures.
>
> ============================================================
> RETURN
> ============================================================
>
> # LOCKENS HEADER POLISH — READY
>
> ## BRAND
>
> Header:
> Login:
> Startup:
> App label:
>
> ## WALLET CONTROL
>
> Copy icon:
> Accessibility:
> Selector behavior:
>
> ## NETWORK
>
> Ethereum mark:
> Sepolia presentation:
> Interactive:
> NO
>
> ## PHYSICAL
>
> Header fit:
> Screens inspected:
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
> Blockchain writes:
> 0
> Signatures:
> 0
>
> ## GIT
>
> No commit/push.
>
> End exactly:
>
> LOCKENS HEADER: READY FOR PHYSICAL REVIEW
