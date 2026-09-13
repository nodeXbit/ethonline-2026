# Project task packet 085: TASK — BUILD WALLET EXPERIENCE FOUNDATION

- Classification: **ORIGINAL / SANITIZED**
- Redactions applied: none
- Source: retained project-specific Control Tower packet in the local Codex record

---

> TASK — BUILD WALLET EXPERIENCE FOUNDATION
>
> MISSION
>
> Build one coherent Android wallet-experience foundation on top of the now
> checkpointed first credential vertical.
>
> This task combines related product work intentionally:
>
> 1. correct session hydration;
> 2. explicit active-wallet model;
> 3. Privy multi-wallet support if confirmed by the exact installed SDK;
> 4. automatic credential discovery inside the known product R1;
> 5. wallet/pass-style My Keys presentation;
> 6. selected-pass state for the future NFC integration.
>
> NO blockchain writes.
>
> NO signing.
>
> NO HCE/NFC implementation yet.
>
> NO Node/firmware changes.
>
> ============================================================
> BASELINE
> ============================================================
>
> Expected:
>
> HEAD == origin/main ==
> 6e2f23fe724c3e180b98a6f9b4bc7cfc19c8661e
>
> Worktree clean.
>
> Require:
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
> EXISTING REAL CREDENTIAL
> ============================================================
>
> Known R1:
>
> 0x0AeB395be893149c0b60D9DAC3Ba55139D0dcA1a
>
> Known holder:
>
> 0x3419148731087b970d2059C53780163B452D5FF7
>
> Existing credential:
>
> staff-001.keys.demo-access.eth
>
> It is:
>
> REGISTERED
> ALLOWED
> non-transferable
> fully configured
>
> Do not modify it.
>
> ============================================================
> ARCHITECTURAL TARGET
> ============================================================
>
> The product model must become:
>
> PRIVY USER
>     ↓
> one or more embedded Ethereum wallets
>     ↓
> ACTIVE WALLET
>     ↓
> {
>   owned ENS Access passes,
>   onchain issuer capability,
>   selected pass
> }
>
> "Holder" and "Issuer" must NOT be exclusive user identities.
>
> A wallet may simultaneously:
>
> - own passes;
> - have issuer capability;
> - later have other ENSv2 permissions.
>
> Use capability-based presentation.
>
> ============================================================
> PHASE 1 — VERIFY EXACT PRIVY MULTI-WALLET API
> ============================================================
>
> Before changing wallet behavior, inspect the exact locally resolved Privy
> Android SDK version and APIs.
>
> Current known dependency was previously:
>
> io.privy:privy-core:0.14.0
>
> Reconfirm locally.
>
> Determine exact supported APIs for:
>
> - enumerating embedded Ethereum wallets;
> - creating an additional Ethereum wallet;
> - selecting/using a specific wallet/provider;
> - stable wallet identity/address;
> - any maximum-wallet behavior relevant to the SDK.
>
> Do not rely on latest online documentation if the locally pinned SDK differs.
>
> Do not upgrade Privy.
>
> If multiple embedded Ethereum wallets are NOT safely supported by the pinned
> version:
>
> do not improvise.
>
> STOP the multi-wallet subphase and report exact limitation, but other independent
> phases may proceed only if they do not bake in a wrong single-wallet
> architecture.
>
> ============================================================
> PHASE 2 — ACTIVE WALLET MODEL
> ============================================================
>
> Remove firstOrNull-style assumptions from normal product architecture.
>
> Create the smallest explicit ActiveWallet model.
>
> Requirements:
>
> - selected wallet must be one of the authenticated user's actual embedded
>   Ethereum wallets;
> - selection persisted safely by stable public wallet identity/address;
> - invalid/stale selection falls back safely;
> - no private key material;
> - wallet switch performs no blockchain transaction;
> - transaction operations remain partitioned by wallet address;
> - My Keys remains partitioned by wallet;
> - selected pass remains partitioned by wallet;
> - issuer capability is recalculated for the active wallet.
>
> Do NOT migrate/rewrite RecoverableTransactionEngine.
>
> Integrate through small seams.
>
> ============================================================
> PHASE 3 — SESSION HYDRATION
> ============================================================
>
> Fix the cold-start Login flash.
>
> Use at least three product states:
>
> RESTORING_SESSION
> AUTHENTICATED
> UNAUTHENTICATED
>
> On startup:
>
> do not render the Login form until Privy has actually determined there is no
> authenticated user.
>
> RESTORING_SESSION should show a clean ENS Access loading/splash state.
>
> If a session exists:
>
> restore account
> restore active wallet
> then show product shell.
>
> Do not weaken authentication.
>
> ============================================================
> PHASE 4 — MULTI-WALLET PRODUCT UI
> ============================================================
>
> If supported by the pinned SDK:
>
> Settings → Wallets
>
> Show each embedded Ethereum wallet:
>
> truncated address
> active indicator
>
> Allow:
>
> Select wallet
>
> Provide:
>
> Create another wallet
>
> BUT:
>
> creating a real additional wallet is a USER action.
>
> Codex/tests must NOT create a real new Privy wallet automatically.
>
> No wallet should be labeled globally as:
>
> Holder
> Issuer
>
> Instead show dynamic capabilities, e.g.:
>
> Active wallet
> 0x...
>
> Can issue passes
> if verified onchain.
>
> Do not expose internal Privy IDs unnecessarily.
>
> ============================================================
> PHASE 5 — R1 AUTOMATIC DISCOVERY
> ============================================================
>
> My Keys must automatically discover credentials belonging to the ACTIVE wallet
> inside the known ENS Access R1.
>
> This is NOT global ENS enumeration.
>
> Known product scope only:
>
> keys.demo-access.eth
> R1:
> 0x0AeB395be893149c0b60D9DAC3Ba55139D0dcA1a
>
> Use public configuration as the authoritative R1/deployment context.
>
> Add strict READ-ONLY support for:
>
> eth_getLogs
>
> to ReadOnlyEthereumRpcClient.
>
> No write methods.
>
> ============================================================
> DISCOVERY MODEL
> ============================================================
>
> Use R1 registration events to discover candidate credential labels/token IDs
> from the known bounded R1 history.
>
> Events are DISCOVERY ONLY.
>
> They are NOT current authority.
>
> For every candidate, perform fresh authoritative CredentialReader readback.
>
> Include in active wallet My Keys only if current onchain state proves:
>
> current owner == active wallet
>
> and the credential belongs to expected R1/S1/provenance.
>
> Current ownership/state wins over historical event ownership.
>
> This naturally supports future transfers because every registered label remains
> a candidate but the current owner is reread.
>
> Do not require a backend/indexer.
>
> ============================================================
> LOG SCANNING SAFETY
> ============================================================
>
> Use bounded/chunked eth_getLogs queries if required by public RPC reliability.
>
> Use the known R1 deployment/bootstrap block from public configuration.
>
> Bound:
>
> candidate count
> response size
> scan ranges
> timeouts
>
> Deduplicate candidates.
>
> RPC failure:
>
> must not silently present stale discovery as authoritative.
>
> Existing already-read credentials may display an explicit stale/unavailable
> state according to current safe read policy.
>
> ============================================================
> PHASE 6 — LOCAL DISCOVERY CACHE
> ============================================================
>
> Cache only non-authoritative discovery references/checkpoints if useful:
>
> chain
> R1
> label/full name
> last scanned block
>
> Do NOT cache ownership/access as authority.
>
> On wallet switch:
>
> re-filter/re-read for the new active address.
>
> Manual existing import remains supported but becomes SECONDARY:
>
> Add by ENS name
>
> It is a recovery/manual-add mechanism.
>
> It must not be necessary for normal known-R1 passes.
>
> ============================================================
> PHASE 7 — AUTOMATIC REFRESH
> ============================================================
>
> Run safe discovery/readback when appropriate:
>
> - entering My Keys;
> - after session restoration;
> - after active-wallet change;
> - returning to foreground if sufficiently stale;
> - manual refresh.
>
> Avoid redundant aggressive polling.
>
> Do not scan continuously.
>
> ============================================================
> PHASE 8 — PASS WALLET PRESENTATION
> ============================================================
>
> Refine My Keys from independent database-like cards toward a pass-wallet
> experience inspired by mobile pass wallets WITHOUT cloning Apple trade dress.
>
> When multiple credentials exist:
>
> show vertically stacked/overlapping pass cards where practical.
>
> Each card should remain identifiable.
>
> Selected card expands/becomes foreground.
>
> Minimum visible summary:
>
> pass type/name
> ENS credential name
> status
> validity
>
> Details may show:
>
> description
> transferability
> owner
> issuer/provenance
> technical details
>
> The existing STAFF credential must look good as the single-pass case.
>
> Do not invent credentials for production state.
>
> Tests/previews may use fixtures.
>
> ============================================================
> PHASE 9 — SELECTED PASS
> ============================================================
>
> Introduce a LOCAL product concept:
>
> Selected pass
>
> This is preparation for future dynamic NFC.
>
> Selecting a credential in My Keys should persist, partitioned by active wallet:
>
> chainId
> credential fullName
>
> Only an actually discovered/imported and freshly owned credential may become
> selected.
>
> If ownership later changes or credential disappears:
>
> clear/invalidate selection.
>
> Do NOT change HCE behavior yet.
>
> Do NOT claim the selected pass is already presented over NFC.
>
> The future NFC workstream will consume this state.
>
> UI may say:
>
> Selected
>
> but not:
>
> NFC active
>
> until HCE is integrated.
>
> ============================================================
> PHASE 10 — CAPABILITY MODEL
> ============================================================
>
> Replace exclusive Holder/Issuer account labeling.
>
> At minimum model issuer capability as:
>
> ALLOWED
> DENIED
> UNAVAILABLE
>
> If active wallet has required R1/S1 authority:
>
> show:
>
> Can issue
>
> and enable Issue/Create Pass destination.
>
> If denied:
>
> My Keys still works.
>
> If unavailable due RPC:
>
> do not falsely call the account a Holder.
>
> Show a neutral state/retry.
>
> A wallet may own passes AND have Can issue.
>
> ============================================================
> PHASE 11 — EXISTING ISSUANCE
> ============================================================
>
> Do NOT generalize the staff credential template in this task.
>
> Do not create new credentials.
>
> The existing issuance flow must continue to work against whichever active wallet
> actually has issuer capability.
>
> Do not silently allow an arbitrary active wallet to issue.
>
> ============================================================
> PHASE 12 — TESTS
> ============================================================
>
> Cover at least:
>
> session restoring does not flash Login
>
> authenticated restore
>
> unauthenticated restore
>
> multiple-wallet enumeration model
>
> active wallet persistence
>
> invalid selected wallet fallback
>
> wallet switch partitions My Keys
>
> wallet switch recalculates issuer capability
>
> eth_getLogs strict allowlist
>
> R1 event decoding
>
> log candidate deduplication
>
> current owner overrides historical owner
>
> staff automatically discovered for holder
>
> staff excluded for another wallet
>
> RPC discovery failure behavior
>
> manual Add by ENS name remains functional
>
> stack/single-pass UI policy
>
> selected-pass persistence
>
> selection invalidated on ownership mismatch
>
> issuer capability ALLOWED/DENIED/UNAVAILABLE
>
> a wallet can own credentials and Can issue simultaneously
>
> no blockchain writes
>
> Existing 139+ Android and 173 Node tests must remain green.
>
> ============================================================
> PHASE 13 — PHYSICAL VALIDATION
> ============================================================
>
> Build/install with:
>
> adb install -r
>
> Do not clear data.
>
> ZERO blockchain writes.
>
> First test with EXISTING holder:
>
> 0x3419148731087b970d2059C53780163B452D5FF7
>
> Prove staff appears in My Keys WITHOUT entering its ENS name.
>
> If existing manual index would mask the test, use the smallest SAFE local
> reference-only mechanism/test setup to prove discovery itself, without deleting
> wallet/auth/transaction state.
>
> Do not mutate blockchain.
>
> Then inspect:
>
> session startup
> My Keys
> selected STAFF
> Settings / wallet area
> capability presentation
>
> Do NOT create a real additional Privy wallet automatically.
>
> Return exact instructions if a USER-CREATED second wallet is needed for final
> physical validation.
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
> No commit/push yet.
>
> ============================================================
> RETURN
> ============================================================
>
> # WALLET EXPERIENCE FOUNDATION — READY
>
> ## PRIVY API
>
> Exact version:
>
> Multi-wallet support:
>
> APIs used:
>
> ## SESSION HYDRATION
>
> Before:
> After:
> Physical:
>
> ## ACTIVE WALLET
>
> Model:
> Persistence:
> Partitioning:
>
> ## MULTI-WALLET
>
> Implemented:
> YES / NO
>
> Real additional wallet created by Codex:
> NO
>
> ## R1 DISCOVERY
>
> Source block:
> Event:
> Candidates:
> Authority:
> RPC behavior:
>
> ## STAFF PHYSICAL AUTODISCOVERY
>
> Manual ENS entry:
> NO
>
> Discovered:
> YES / NO
>
> Ownership verified:
> YES / NO
>
> ## MY KEYS
>
> Stack behavior:
>
> ## SELECTED PASS
>
> Persistence:
> Ownership invalidation:
> HCE modified:
> NO
>
> ## CAPABILITIES
>
> Issuer model:
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
> ## SECURITY
>
> Blockchain writes:
> 0
>
> Signatures:
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
> WALLET FOUNDATION: READY FOR PHYSICAL REVIEW
