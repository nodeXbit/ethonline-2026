# Project task packet 083: TASK — RECOVER CONFIRMED TX2 TO READY + FIX CONFIGURE CTA CLIPPING

- Classification: **ORIGINAL / SANITIZED**
- Redactions applied: none
- Source: retained project-specific Control Tower packet in the local Codex record

---

> TASK — RECOVER CONFIRMED TX2 TO READY + FIX CONFIGURE CTA CLIPPING
>
> CONTEXT
>
> Both real staff credential transactions are now CONFIRMED.
>
> NO further blockchain transaction is required for staff-001.
>
> This task is LOCAL / READ-ONLY recovery plus one UI layout fix.
>
> ZERO blockchain writes.
>
> ZERO signatures.
>
> NEVER resend TX1.
>
> NEVER resend TX2.
>
> ============================================================
> AUTHORITATIVE REAL STATE
> ============================================================
>
> Credential:
>
> staff-001.keys.demo-access.eth
>
> TX1 registration:
>
> 0x8858c291f14659ea8e323d1af988f4ee379c2a14dc5e2cbed4db592a04e6db58
>
> TX1:
> CONFIRMED
>
> TX2 configuration:
>
> 0x57a5a4bf81fcede947b83bf55dcabe065540a1254fb2d9eb10677d391cf7bb11
>
> TX2:
> CONFIRMED
>
> TX2 receipt block:
>
> 11685150
>
> Issuer current latest/pending nonce:
>
> 3 / 3
>
> Persisted issuance state:
>
> AUTHORITATIVE_READBACK
>
> Persisted records operation:
>
> 585f5dec-8a8d-42de-bf00-3bfee084087a
>
> Records transaction state:
>
> CONFIRMED
>
> ============================================================
> CURRENT ONCHAIN STAFF STATE
> ============================================================
>
> At fresh pinned block >= 11685150:
>
> status:
> REGISTERED
>
> owner:
>
> 0x3419148731087b970d2059C53780163B452D5FF7
>
> resolver:
>
> 0x20766FB21498a99350F922ee3163F5a95F354a7f
>
> subregistry:
>
> zero address
>
> expiry:
>
> 1793487599
>
> roles:
>
> 0
>
> description:
>
> Staff Access Pass
>
> avatar:
>
> unset / empty
>
> access.v1:
>
> active = true
> validUntil = 1793487599
>
> provenance:
>
> PASS
>
> Therefore the credential is already:
>
> FULLY CONFIGURED ONCHAIN.
>
> ============================================================
> PROVEN FAILURE
> ============================================================
>
> Final CredentialReadPolicy.requireReady() previously rejected the snapshot as:
>
> WRONG_DESCRIPTION
>
> Strong evidence demonstrates a read-after-confirmation race.
>
> TX2 was mined in block:
>
> 11685150
>
> At block:
>
> 11685149
>
> description/access were still unset.
>
> At block:
>
> 11685150
>
> both records were correctly configured.
>
> The final readback used a new "latest" snapshot but did not require:
>
> snapshotBlock >= confirmed TX2 receipt block
>
> Therefore an RPC lagging by one block could produce a false invariant mismatch.
>
> ============================================================
> OBJECTIVE A — RECEIPT-BLOCK-AWARE AUTHORITATIVE READBACK
> ============================================================
>
> Fix this systematically.
>
> Do NOT add an arbitrary fixed sleep as the correctness mechanism.
>
> When finalizing a confirmed transaction:
>
> knownReceiptBlock = persisted receipt block
>
> Before evaluating final credential invariants:
>
> require the authoritative read snapshot block to satisfy:
>
> snapshotBlock >= knownReceiptBlock
>
> If the current RPC endpoint reports a latest block below knownReceiptBlock:
>
> do NOT evaluate credential invariants yet.
>
> Classify as temporary read propagation / unavailable state.
>
> Perform a small bounded read-only reconciliation strategy.
>
> Use sensible existing timeout/polling infrastructure where possible.
>
> Do not create an aggressive loop.
>
> No write.
>
> No signing.
>
> Once an eligible block >= knownReceiptBlock is available:
>
> perform the credential reads coherently at one pinned block.
>
> Then evaluate exact READY invariants.
>
> ============================================================
> IMPORTANT DISTINCTION
> ============================================================
>
> Case A:
>
> snapshot block < receipt block
>
> This is NOT a credential mismatch.
>
> Do not produce:
>
> WRONG_DESCRIPTION
> WRONG_ACCESS
> etc.
>
> It means the read source has not reached the proven receipt block yet.
>
> Case B:
>
> snapshot block >= receipt block
> but values are still wrong
>
> This IS a real authoritative mismatch and should remain fail-closed / not READY.
>
> Preserve that security property.
>
> ============================================================
> OBJECTIVE B — RESTART RECOVERY
> ============================================================
>
> The existing real persisted state must recover safely.
>
> On startup / issuer state restoration:
>
> records transaction:
> CONFIRMED
>
> issuance:
> AUTHORITATIVE_READBACK
>
> credential:
> already fully configured
>
> The application must perform READ-ONLY final reconciliation.
>
> It must NEVER create another records transaction.
>
> It must NEVER call S1.multicall again.
>
> If authoritative readback passes:
>
> transition idempotently to:
>
> READY
>
> This must work if:
>
> - the app remained open;
> - the app was restarted;
> - the previous final readback failed;
> - READY finalization is called more than once.
>
> READY finalization should be idempotent.
>
> ============================================================
> OBJECTIVE C — CLEAR STALE FAILURE PRESENTATION
> ============================================================
>
> Once successful authoritative recovery proves READY:
>
> clear/replace the stale action-level message:
>
> Could not complete credential setup safely.
>
> Normal product UI should instead show:
>
> Credential ready
>
> The previous sanitized diagnostic may remain in Developer Diagnostics/history if
> useful, but it must not continue to look like the CURRENT credential result.
>
> Do not erase transaction/journal history.
>
> ============================================================
> OBJECTIVE D — READ-ONLY RETRY UX
> ============================================================
>
> If authoritative readback cannot currently complete because the RPC is
> temporarily unavailable or behind:
>
> the credential must remain:
>
> Verifying onchain
> or
> Verification unavailable
>
> Offer a safe action such as:
>
> Retry verification
>
> ONLY if useful.
>
> That action must be provably READ-ONLY.
>
> It must NEVER resend TX2.
>
> If automatic bounded retry is enough, do not add unnecessary UI.
>
> ============================================================
> OBJECTIVE E — CONFIGURE BUTTON CLIPPING
> ============================================================
>
> Fix the demonstrated physical layout defect.
>
> Observed:
>
> Screen:
> 1200 x 2670
>
> Dialog:
> [48,214][1152,2494]
>
> Action row:
> [108,2290][1092,2446]
>
> Configure button:
> [609,2303][1083,2446]
>
> System insets are NOT the cause.
>
> The longer Configure credential label is clipped inside the half-width fixed
> action row.
>
> Fix systematically.
>
> Do not solve by shrinking text excessively.
>
> Preferred principles:
>
> - CTA text fully visible;
> - accessible control height;
> - current horizontal gutter preserved;
> - both Back and primary action clearly usable.
>
> If two equal half-width buttons do not fit comfortably, use a better native
> composition, for example:
>
> primary full-width button
> +
> secondary Back action
>
> or another small layout compatible with the existing visual system.
>
> Use physical screenshot/bounds validation.
>
> ============================================================
> NO-TOUCH
> ============================================================
>
> Do NOT:
>
> send TX1
> send TX2
> create any new blockchain transaction
> re-arm any confirmed operation
> clear app data
> uninstall app
> reset issuance
> make the credential creator generic
> implement automatic discovery
> implement artwork rendering
> fix session/login flash
> rename Issuer
> implement NFC
> change HCE
> change Node
> change firmware
> commit
> push
>
> ============================================================
> TESTS
> ============================================================
>
> Add/update tests proving:
>
> 1. confirmed TX2 receipt block is persisted and available to final readback.
>
> 2. snapshot block below receipt block:
>    - is NOT evaluated as a credential mismatch;
>    - cannot produce WRONG_DESCRIPTION merely from stale pre-TX block state;
>    - remains read-only.
>
> 3. snapshot block equal to receipt block:
>    accepted for authoritative evaluation.
>
> 4. snapshot block above receipt block:
>    accepted.
>
> 5. snapshot >= receipt but wrong description:
>    still fails closed.
>
> 6. AUTHORITATIVE_READBACK + records CONFIRMED on restart:
>    performs read-only recovery.
>
> 7. successful recovery:
>    transitions to READY.
>
> 8. repeated READY recovery/finalization:
>    idempotent.
>
> 9. no new records operation is created.
>
> 10. no write transport is invoked during final recovery.
>
> 11. stale product error is no longer presented as current after READY.
>
> 12. existing duplicate-prevention and UNKNOWN tests remain green.
>
> 13. UI policy/layout test for long primary Configure label where practical.
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
> ZERO real blockchain writes.
>
> ============================================================
> PHYSICAL RECOVERY
> ============================================================
>
> Install using:
>
> adb install -r
>
> Do not clear data.
>
> Open the existing app.
>
> The existing authenticated issuer, journal, TX1 and TX2 state must remain.
>
> No transaction button should need to be pressed.
>
> Allow the app to reconcile the EXISTING confirmed TX2.
>
> Require:
>
> issuer nonce remains 3 / 3
>
> no new hash
>
> no new transaction operation
>
> credential transitions to:
>
> READY
>
> Inspect physically:
>
> Issuer / current credential state
>
> Configure CTA layout if the recovery path still exposes the relevant review in a
> safe non-writing test/mocked path.
>
> Do NOT press any final transaction action.
>
> ============================================================
> RETURN
> ============================================================
>
> # STAFF FINAL READBACK RECOVERY — READY
>
> ## EXISTING TX1
>
> Hash:
> State:
>
> ## EXISTING TX2
>
> Hash:
> State:
> Receipt block:
>
> ## READBACK FIX
>
> Previous behavior:
>
> New minimum-block rule:
>
> Below-receipt behavior:
>
> At/above-receipt behavior:
>
> ## RESTART RECOVERY
>
> Before:
>
> After:
>
> New TX operation created:
> YES / NO
>
> Blockchain call:
> READS ONLY / WRITE
>
> ## CREDENTIAL
>
> Final app state:
>
> Onchain snapshot block:
>
> Description:
>
> Access:
>
> READY:
> YES / NO
>
> ## STALE ERROR
>
> Previous:
>
> After recovery:
>
> ## BUTTON CLIPPING
>
> Previous bounds/problem:
>
> Layout fix:
>
> Physical result:
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
> APK installed over existing data:
>
> Existing journal preserved:
>
> Issuer nonce:
>
> New tx hash:
> NONE
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
> FINAL READBACK RECOVERY:
> PASS / STOP
>
> If PASS end exactly:
>
> STAFF CREDENTIAL: READY — EXISTING TX1/TX2 RECONCILED
