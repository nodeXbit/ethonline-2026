# Project task packet 086: TASK — PASS WALLET PRODUCT POLISH

- Classification: **ORIGINAL / SANITIZED**
- Redactions applied: none
- Source: retained project-specific Control Tower packet in the local Codex record

---

> TASK — PASS WALLET PRODUCT POLISH
>
> MISSION
>
> Perform one final focused product-UX pass on the checkpointed Android wallet.
>
> This task is PRESENTATION / INTERACTION only.
>
> Improve:
>
> 1. human active-wallet selector;
> 2. network presentation;
> 3. real credential artwork rendering;
> 4. full visual pass treatment;
> 5. overlapping/selectable multi-pass stack.
>
> Preserve:
>
> - Privy session;
> - active-wallet architecture;
> - multi-wallet behavior;
> - R1 automatic discovery;
> - authoritative credential reads;
> - issuer capability model;
> - selected-pass persistence;
> - all transaction/recovery semantics.
>
> NO blockchain writes.
>
> NO signing.
>
> NO HCE/NFC changes.
>
> NO Node/firmware changes.
>
> NO new credentials.
>
> ============================================================
> BASELINE
> ============================================================
>
> Require clean synchronized baseline:
>
> HEAD == origin/main ==
> e4ae08c35d4848e5bc426803791160771f79255e
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
> STOP if baseline differs.
>
> ============================================================
> PRODUCT TARGET
> ============================================================
>
> The Android app should now feel like a credential/pass wallet.
>
> Primary mental model:
>
> ACTIVE WALLET
>     ↓
> OWNED PASSES
>     ↓
> SELECTED PASS
>
> Wallet infrastructure should be understandable but visually subordinate to the
> passes themselves.
>
> Do not redesign ENS architecture.
>
> ============================================================
> PHASE 1 — SETTINGS ACTIVE WALLET CONTROL
> ============================================================
>
> Current wallet management is functionally correct but should become more
> intuitive.
>
> Create one prominent compact active-wallet control.
>
> Conceptually:
>
> ACCOUNT
>
> ┌────────────────────────────────┐
> │ 0x3419…5FF7               Copy │
> │ Active wallet                  │
> │                              ▼ │
> └────────────────────────────────┘
>
> Exact visual treatment may adapt to the existing Android UI stack.
>
> Requirements:
>
> - truncated active address prominently readable;
> - inline copy-address action;
> - whole wallet-selector area clearly tappable;
> - visual dropdown/expand affordance;
> - accessible touch targets;
> - current wallet obvious;
> - no raw Privy IDs.
>
> When opened, show all actual authenticated embedded Ethereum wallets:
>
> ✓ active wallet address
>   other wallet address
>   ...
>
> Then at the bottom:
>
> + Create new wallet
>
> If there is only one wallet:
>
> ✓ current wallet
>
> + Create new wallet
>
> Wallet creation remains explicit user action.
>
> Codex/tests MUST NOT create another real wallet.
>
> Selecting another existing wallet must continue to use the current safe
> ActiveWallet architecture.
>
> ============================================================
> PHASE 2 — CAPABILITY PRESENTATION
> ============================================================
>
> Do not label a wallet globally as Issuer or Holder.
>
> Within account/wallet presentation, show capability badges only when useful:
>
> Can issue
>
> Cannot issue passes
>
> Issuer access unavailable
>
> according to the existing ALLOWED / DENIED / UNAVAILABLE capability.
>
> Do not overfill the wallet picker.
>
> My Keys remains available regardless of issuer capability.
>
> ============================================================
> PHASE 3 — NETWORK PRESENTATION
> ============================================================
>
> Do NOT implement a network selector.
>
> Current product supports the configured Sepolia deployment only.
>
> Present it cleanly as:
>
> NETWORK
>
> Ethereum icon / mark
> Sepolia Testnet
>
> Optionally show:
>
> Chain ID 11155111
>
> only in secondary details.
>
> The row must NOT look tappable/selectable if it cannot change network.
>
> Use a small local vector/drawable for an Ethereum-style network mark if needed.
>
> Do not add a network/image/icon dependency just for this.
>
> Architecture may remain compatible with a future network selector, but do not
> fake one.
>
> ============================================================
> PHASE 4 — ARTWORK SEMANTICS
> ============================================================
>
> Treat the onchain credential avatar/artwork URI as the PASS ARTWORK.
>
> It is NOT the holder portrait.
>
> Visual model:
>
> credential artwork
> → primary visual identity / background/hero of the pass
>
> Future holder portrait:
> separate concept, NOT implemented here.
>
> Existing STAFF has blank artwork and must therefore render a polished fallback.
>
> Do not invent onchain artwork for STAFF.
>
> ============================================================
> PHASE 5 — SAFE ARTWORK LOADING
> ============================================================
>
> Implement real pass-artwork rendering for credentials whose authoritative
> avatar/artwork URI is nonblank.
>
> Support at minimum:
>
> https://
>
> Evaluate whether existing code can safely support:
>
> ipfs://
>
> without adding a fragile dependency.
>
> If IPFS requires a gateway, isolate that translation cleanly and fail to the
> placeholder if unavailable.
>
> Do not change onchain URI values.
>
> Requirements for remote images:
>
> - asynchronous / never block UI thread;
> - bounded connect/read timeout;
> - bounded download size;
> - image content only;
> - safe decode;
> - no HTML rendering;
> - reject unsafe schemes;
> - graceful failure;
> - no credential/auth headers sent;
> - simple bounded cache if useful.
>
> Prefer existing Android primitives if sufficient.
>
> Add a small mature image-loading dependency ONLY if inspection proves doing so
> is materially safer/smaller than hand-rolling the network/cache path.
>
> If adding one dependency, explain why and pin it.
>
> Do not add a broad networking stack.
>
> ============================================================
> PHASE 6 — FULL PASS VISUAL DESIGN
> ============================================================
>
> Make credentials feel like digital passes rather than database cards.
>
> The artwork should visually fill or dominate the credential surface.
>
> For artwork credentials:
>
> ┌─────────────────────────────────┐
> │                                 │
> │          PASS ARTWORK           │
> │                                 │
> │ STAFF ACCESS           ALLOWED  │
> │ staff-001.keys...               │
> │ Valid until 31 Oct 2026         │
> └─────────────────────────────────┘
>
> Exact composition can adapt.
>
> Requirements:
>
> - pass type/status readable over variable images;
> - use restrained overlay/scrim where required for contrast;
> - ENS name still available;
> - status does not rely only on color;
> - expiry visible;
> - selected state understandable;
> - no important ownership/technical clutter on main card.
>
> For blank artwork:
>
> render a deliberate deterministic fallback, not a broken-image box.
>
> Do not turn the entire card into a tiny thumbnail plus text list.
>
> ============================================================
> PHASE 7 — OPTIONAL SMALL PORTRAIT SLOT
> ============================================================
>
> The user suggested a small portrait/photo region.
>
> Do NOT incorrectly reuse pass artwork for this.
>
> For this task:
>
> either:
>
> A. keep a neutral deterministic credential/icon/initials element;
>
> or
>
> B. omit the portrait slot entirely if it makes the pass cleaner.
>
> Do NOT add another onchain field.
>
> Do NOT claim holder-editable portrait exists yet.
>
> Record holder avatar / delegated metadata as future work only.
>
> ============================================================
> PHASE 8 — MULTI-PASS STACK
> ============================================================
>
> The wallet foundation already supports selected-pass state.
>
> Implement a physical-wallet-like stacked pass presentation for 2+ credentials.
>
> Do not clone Apple Wallet trade dress exactly.
>
> Use the interaction principle:
>
> - selected pass is foreground/expanded;
> - non-selected passes remain partially visible behind/below;
> - enough header/artwork/status remains visible to identify them;
> - tapping another pass selects and promotes it;
> - scrolling/tapping remains accessible;
> - selected pass persists via existing SelectedPass state.
>
> For a single pass:
>
> show it normally without artificial empty stacked layers.
>
> ============================================================
> MULTIPLE PASS VALIDATION
> ============================================================
>
> Production My Keys must show only real discovered/imported owned passes.
>
> Do NOT inject fake credentials into production state.
>
> Because currently only one real STAFF pass exists, validate multi-pass layout
> using test fixtures or a DEBUG/diagnostic-only render path if physical layout
> inspection genuinely requires it.
>
> Any fixture preview must be:
>
> - clearly Developer Diagnostics only;
> - never part of My Keys authoritative data;
> - unable to sign/write;
> - removable later.
>
> Prefer automated layout/state tests if physical fixture UI is unnecessary.
>
> Real physical stacked-pass validation will occur when VISITOR / CONTRACTOR
> exist.
>
> ============================================================
> PHASE 9 — SELECTED PASS VISUAL STATE
> ============================================================
>
> Make the selected pass state human-readable.
>
> Do not say:
>
> NFC Active
>
> because HCE has not been connected yet.
>
> Acceptable language:
>
> Selected
>
> Selected pass
>
> or purely clear foreground interaction if textual label is unnecessary.
>
> Selected pass still must satisfy:
>
> freshly owned by active wallet
>
> If ownership becomes invalid:
>
> existing foundation must clear/invalidate selection.
>
> Do not change that security behavior.
>
> ============================================================
> PHASE 10 — DETAILS
> ============================================================
>
> Tapping/opening a selected pass may expose credential details using current
> data:
>
> ENS name
>
> Description
>
> Access state
>
> Registration/expiry
>
> Transferability
>
> Current owner
>
> Issuer/provenance if useful
>
> Artwork URI
>
> Technical details may remain progressively disclosed.
>
> Do not add blockchain functionality.
>
> ============================================================
> PHASE 11 — PRESERVE EXISTING PRODUCT
> ============================================================
>
> Must preserve physically validated behavior:
>
> - no Login flash;
> - multi-wallet selection;
> - wallet partitioning;
> - automatic R1 discovery;
> - STAFF automatically appears for holder;
> - STAFF excluded for non-owner wallet;
> - selected pass persists;
> - issuer capability recalculates;
> - Add by ENS name fallback;
> - Developer Diagnostics separation;
> - issuance/recovery.
>
> Do not touch transaction semantics.
>
> ============================================================
> PHASE 12 — NO SCOPE CREEP
> ============================================================
>
> Do NOT implement:
>
> - NFC/HCE dynamic pass;
> - gates/resources;
> - VISITOR;
> - CONTRACTOR;
> - transfers;
> - generic issuer templates;
> - holder metadata delegation;
> - network switching;
> - ETH wallet/send/receive;
> - backend/indexer;
> - image upload;
> - IPFS upload service.
>
> This is wallet presentation only.
>
> ============================================================
> PHASE 13 — TESTS
> ============================================================
>
> Cover at least:
>
> wallet selector with one wallet
>
> wallet selector with multiple wallets
>
> active indicator
>
> wallet selection callback/model
>
> create-wallet action exists but is not automatic
>
> inline address copy policy
>
> network row is non-interactive
>
> blank artwork fallback
>
> safe HTTPS artwork URI
>
> unsafe artwork schemes rejected
>
> artwork load failure fallback
>
> selected single pass
>
> multi-pass stacking order
>
> tap pass changes selected state
>
> selected foreground survives rerender/restart model
>
> wallet switch displays that wallet's selected pass
>
> invalid ownership still invalidates selection
>
> product state never uses debug fixture as authoritative pass
>
> existing wallet-foundation and credential tests stay green
>
> No blockchain writes.
>
> ============================================================
> PHASE 14 — PHYSICAL DEVICE AUDIT
> ============================================================
>
> Build/install:
>
> adb install -r
>
> Do not clear data.
>
> Use unlocked physical device.
>
> Inspect:
>
> Settings wallet selector closed
>
> wallet selector opened
>
> copy-address affordance
>
> Sepolia network presentation
>
> My Keys with real STAFF
>
> selected-state appearance
>
> single-pass artwork fallback
>
> light/dark handling where practical
>
> If a second real wallet already exists, switch between them safely and verify
> selector behavior.
>
> Do not create another wallet automatically.
>
> For multi-pass layout:
>
> use automated fixture validation or clearly isolated diagnostics preview only if
> needed.
>
> Do not mutate blockchain.
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
> No commit/push.
>
> ============================================================
> RETURN
> ============================================================
>
> # PASS WALLET POLISH — READY
>
> ## SETTINGS WALLET PICKER
>
> Closed:
>
> Opened:
>
> Create wallet action:
>
> Physical:
>
> ## NETWORK
>
> Presentation:
>
> Interactive:
> NO
>
> ## ARTWORK
>
> Semantics:
>
> HTTPS:
>
> IPFS:
>
> Fallback:
>
> Dependencies added:
>
> ## PASS DESIGN
>
> Single pass:
>
> Artwork pass:
>
> ## MULTI-PASS STACK
>
> Behavior:
>
> Selection:
>
> Production fake credentials:
> NO
>
> How multi-pass was validated:
>
> ## SELECTED PASS
>
> Visual state:
>
> Persistence unchanged:
>
> HCE:
> NOT CONNECTED
>
> ## DETAILS
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
> Known remaining visual issues:
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
> Secrets:
> 0
>
> ## GIT
>
> No commit/push.
>
> End exactly:
>
> PASS WALLET POLISH: READY FOR PHYSICAL REVIEW
