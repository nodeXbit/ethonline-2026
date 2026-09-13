# Project task packet 081: TASK — FIX POST-TX1 RECOVERY AND PREPARE EXISTING STAFF FOR TX2 REVIEW

- Classification: **ORIGINAL / SANITIZED**
- Redactions applied: none
- Source: retained project-specific Control Tower packet in the local Codex record

---

> TASK — FIX POST-TX1 RECOVERY AND PREPARE EXISTING STAFF FOR TX2 REVIEW
>
> WHY
>
> The first real staff registration transaction has CONFIRMED onchain.
>
> This is no longer a fresh credential-creation flow.
>
> Existing authoritative credential:
>
> staff-001.keys.demo-access.eth
>
> Registration tx:
>
> 0x8858c291f14659ea8e323d1af988f4ee379c2a14dc5e2cbed4db592a04e6db58
>
> Receipt:
> SUCCESS
>
> Block:
> 11684957
>
> Issuer nonce:
> 1 -> 2
>
> Current issuer latest/pending:
> 2 / 2
>
> Current persisted issuance state:
>
> REGISTERED_CONFIGURING
>
> Registration operation:
>
> c9085610-c3d6-4d6b-9534-f55d94580e23
>
> Registration transaction state:
>
> CONFIRMED
>
> Records operation:
> NONE
>
> Records transaction:
> NONE
>
> TX2 has NOT been broadcast.
>
> ============================================================
> MISSION
> ============================================================
>
> Fix the LOCAL post-TX1 failure/recovery path.
>
> Then install the APK over the existing application and physically prove that the
> EXISTING persisted credential can reach a correct TX2 configuration REVIEW.
>
> STOP BEFORE ANY TX2 SUBMISSION.
>
> ZERO blockchain writes in this task.
>
> ZERO signatures.
>
> NEVER call R1.register again.
>
> ============================================================
> AUTHORITATIVE EXISTING ONCHAIN STATE
> ============================================================
>
> staff-001:
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
> 0x0000000000000000000000000000000000000000
>
> expiry:
>
> 1793487599
>
> owner roles:
>
> 0
>
> transferability:
>
> non-transferable
>
> Current S1 state:
>
> description:
> empty / not set
>
> avatar:
> empty / not set
>
> access.v1:
> raw 0x / not set
>
> Do not mutate any of this during implementation/validation.
>
> ============================================================
> NO-TOUCH
> ============================================================
>
> Do NOT:
>
> call register
> re-arm registration
> create another registration operation
> delete the confirmed registration journal
> clear application data
> uninstall the app
> reset issuance state
> change R1/S1 configuration
> change CredentialAbi semantics
> change Privy
> change Web3j
> change HCE/NFC
> change Node
> change firmware
> implement automatic discovery
> make credential fields generic/editable
> implement artwork rendering
> fix startup hydration flash
> rename Issuer
> commit
> push
>
> Those product items are separate follow-up work.
>
> ============================================================
> PHASE 1 — SOURCE-LEVEL ROOT CAUSE ANALYSIS
> ============================================================
>
> The original IllegalArgumentException was not retained.
>
> Known facts:
>
> - TX1 had already confirmed.
> - registerConfirmed() persisted REGISTERED_CONFIGURING.
> - a later exception was caught by runAction().
> - no TX2 operation was created.
> - no TX2 transaction was sent.
>
> Inspect the exact path AFTER successful:
>
> registerConfirmed()
>
> through:
>
> post-registration handling
> rendering
> records preparation
> state restoration
>
> Identify every realistic IllegalArgumentException source.
>
> Do not invent a root cause.
>
> If a deterministic source can be proven through code/test reproduction:
>
> document and fix it.
>
> If the original throw cannot be proven uniquely:
>
> fix any demonstrated invalid state transition/rendering assumption and ensure
> the persisted REGISTERED_CONFIGURING state can be restored safely.
>
> ============================================================
> PHASE 2 — EXISTING-STATE RECOVERY CONTRACT
> ============================================================
>
> The app MUST treat the existing state:
>
> registration CONFIRMED
> +
> issuance REGISTERED_CONFIGURING
> +
> no records operation
>
> as a normal recoverable condition.
>
> On startup/re-render it must show humanly:
>
> Credential created
> Setup incomplete
>
> and offer:
>
> Resume setup
>
> It must NOT:
>
> show Create credential as the valid next transaction action
>
> call registration again
>
> create another registration transaction draft
>
> require re-arming TX1
>
> discard the confirmed registration
>
> ============================================================
> PHASE 3 — RESUME SETUP MUST BE READ-ONLY UNTIL FINAL CONFIGURE
> ============================================================
>
> Pressing:
>
> Resume setup
>
> must initially perform only:
>
> fresh read-only onchain verification
> local validation
> exact TX2 calldata construction
> eth_call simulation
> human configuration review
>
> It MUST NOT submit TX2 merely by opening Resume setup.
>
> The persistence boundary should mirror the P0 fix:
>
> Resume setup
>     ↓
> pure configuration review
>     ↓
> Back
> or
> Configure credential
>
> Only FINAL:
>
> Configure credential
>
> may establish the persistent TX2 operation and enter wallet approval.
>
> ============================================================
> PHASE 4 — TX2 EXACT INTENT
> ============================================================
>
> Because Artwork for the real credential was intentionally left blank, expected
> TX2 must contain:
>
> S1.multicall(
>   [
>     setText(
>       node,
>       "description",
>       "Staff Access Pass"
>     ),
>
>     setData(
>       node,
>       "access.v1",
>       abi.encode(
>         true,
>         uint64(1793487599)
>       )
>     )
>   ]
> )
>
> Avatar:
>
> NO avatar setter.
>
> Do not silently reuse the temporary physical test value:
>
> https://example.com/staff.png
>
> That URI MUST NOT become part of TX2.
>
> Require the existing persisted real issuance data to represent Artwork as blank.
>
> If any stale local temporary artwork could affect TX2:
>
> STOP and report it rather than writing.
>
> ============================================================
> PHASE 5 — TX2 PREFLIGHT
> ============================================================
>
> Before showing a ready-to-submit configuration review, read-only verify:
>
> chain:
> 11155111
>
> issuer:
> 0xFa90e8301A22833B74378C5fA3a7c120Ac512685
>
> issuer latest == pending == 2
>
> staff:
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
> expiry:
> 1793487599
>
> roles:
> 0
>
> description currently unset
>
> avatar currently unset
>
> access.v1 currently unset
>
> issuer still has required S1 authority
>
> S1 provenance matches config
>
> exact S1.multicall simulation:
> PASS
>
> No write.
>
> ============================================================
> PHASE 6 — SAFE ACTION ERROR OBSERVABILITY
> ============================================================
>
> The previous broad runAction catch discarded the useful origin/message and left:
>
> Operation failed (IllegalArgumentException).
>
> Improve this minimally.
>
> Do NOT expose raw provider errors or secrets.
>
> Preserve a sanitized action-level error model containing enough information for
> future diagnosis, e.g.:
>
> action
> safe stage/category
> safe human message
> exception class for diagnostics
>
> Normal product UI should receive an actionable safe message.
>
> Developer Diagnostics may show sanitized technical stage/category/class.
>
> Never log:
>
> auth tokens
> OTP/email secrets
> private keys
> raw signed tx
> RPC secret URLs
> holder proofs/signatures
>
> Do not build a large logging framework.
>
> ============================================================
> PHASE 7 — TESTS
> ============================================================
>
> Add/update tests for the EXACT real recovery scenario:
>
> registration op CONFIRMED
>
> issuance REGISTERED_CONFIGURING
>
> no records operation
>
> no records hash
>
> restart/re-render
>
> Expected:
>
> Resume setup available
>
> no registration retry
>
> no new registration draft
>
> Resume setup creates only an in-memory/read-only TX2 review
>
> Back from TX2 review is side-effect free
>
> opening review repeatedly does not create records operations
>
> final Configure boundary is the only place that may create records transaction
> intent
>
> TX2 contains description + access.v1 only
>
> blank avatar omits setText("avatar")
>
> temporary example.com artwork cannot leak into the recovered real credential
>
> existing transaction-engine recovery tests remain green
>
> No real chain write in tests.
>
> ============================================================
> PHASE 8 — VALIDATION
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
> ============================================================
> PHASE 9 — PHYSICAL RECOVERY
> ============================================================
>
> Install with adb install -r.
>
> Do not clear data.
>
> Device may remain unlocked.
>
> Open ENS Access.
>
> Require it recovers the EXISTING authenticated issuer/session and the persisted
> staff state.
>
> Known startup login flash may still exist; it is not part of this task.
>
> Navigate:
>
> Issuer
> → Resume setup
>
> This physical action is authorized ONLY if code inspection proves it is
> read-only until the separate final Configure credential button.
>
> Inspect the TX2 review.
>
> DO NOT press:
>
> Configure credential
>
> Do not approve any wallet action.
>
> No signing.
>
> No broadcast.
>
> ============================================================
> TX2 REVIEW MUST SHOW
> ============================================================
>
> Existing credential:
>
> staff-001.keys.demo-access.eth
>
> Recipient:
>
> 0x3419148731087b970d2059C53780163B452D5FF7
>
> Description:
>
> Staff Access Pass
>
> Access:
>
> Allowed
>
> Valid until:
>
> 31 Oct 2026, 23:59:59 Europe/Madrid
>
> Artwork:
>
> Not set
>
> Network:
>
> Sepolia
>
> Purpose:
>
> Configure access and presentation for the already-created pass.
>
> It should make clear the credential itself is already created and this is NOT
> another registration.
>
> ============================================================
> RETURN
> ============================================================
>
> # STAFF POST-TX1 RECOVERY — READY
>
> ## EXISTING TX1
>
> Hash:
> State:
> Block:
> Issuer nonce:
>
> ## ROOT CAUSE
>
> Proven exact cause:
> YES / NO
>
> Cause/finding:
>
> Fix:
>
> ## RECOVERY
>
> Persisted issuance before:
>
> After restart:
>
> Registration retried:
> NO
>
> ## RESUME SETUP
>
> Read-only before final Configure:
> YES / NO
>
> TX2 operation created before final Configure:
> YES / NO
>
> ## TX2 INTENT
>
> Target:
>
> Calls:
>
> Description:
>
> Access:
>
> Avatar setter:
> YES / NO
>
> Simulation:
>
> ## ERROR OBSERVABILITY
>
> Previous:
>
> Now:
>
> Sensitive data exposure:
> NO
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
> Existing state recovered:
>
> Resume setup opened:
>
> TX2 review inspected:
>
> Configure credential pressed:
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
> POST-TX1 RECOVERY:
> PASS / STOP
>
> If PASS end exactly:
>
> TX2: READY FOR PHYSICAL REVIEW — NOT SUBMITTED
