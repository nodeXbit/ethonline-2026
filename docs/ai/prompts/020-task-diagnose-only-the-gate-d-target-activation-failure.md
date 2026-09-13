# Project task packet 020: TASK — Diagnose ONLY the Gate D TARGET_ACTIVATION failure.

- Classification: **ORIGINAL / SANITIZED**
- Redactions applied: none
- Source: retained project-specific Control Tower packet in the local Codex record

---

> TASK
>
> Diagnose ONLY the Gate D TARGET_ACTIVATION failure.
>
> Do not continue the APDU transport implementation yet.
>
> Current failure:
>
> ESP32-S3 + Elechouse PN532 initializes successfully
> → GATE_D_READY
> → PRESENT_SEEKER
> → inListPassiveTarget()
> → no Android target detected.
>
> Therefore:
>
> SELECT was never sent.
> SEND_CHALLENGE was never sent.
> Privy signing was never invoked.
>
> The existing Gate D firmware is currently uncommitted and must be preserved.
>
> CURRENT EXPECTED REPO
>
> HEAD == origin/main ==
> 5cb7d3561492afeb5b9477b32ab9dd34688df7d6
>
> Expected only untracked implementation:
>
> firmware/pn532_hce_apdu/
>
> Do not commit or push.
>
> GOAL
>
> Determine whether the failure is:
>
> A. NFC disabled / unavailable on device
> B. FEATURE_NFC_HOST_CARD_EMULATION absent
> C. HCE service/AID not registered at runtime
> D. Android routing/session prerequisite
> E. PN532 RF activation / positioning issue
>
> Do not debug SELECT or later APDUs until a target activates.
>
> PHASE 1 — PRESERVE BASELINE
>
> Read-only confirm:
>
> git status -sb
> git rev-parse HEAD
> git rev-parse origin/main
>
> Do not modify Gate D firmware during initial diagnosis.
>
> Inspect the already-pushed Android HCE configuration:
>
> - AndroidManifest.xml
> - gate_c1_apdu_service.xml
> - GateC1HostApduService
> - AID registration
>
> Confirm from source:
>
> AID:
> F0454E5356324331
>
> category:
> other
>
> Whether:
> android:requireDeviceUnlock
> is explicitly configured, and what its effective behavior is.
>
> Do not change anything yet.
>
> PHASE 2 — RUNTIME HCE DIAGNOSTICS
>
> Because wired adb is unavailable, add the smallest temporary/scoped
> user-visible diagnostic to the existing Android app.
>
> It must report ONLY non-sensitive Android capability/configuration:
>
> - NFC adapter present: YES / NO
> - NFC currently enabled: YES / NO
> - PackageManager FEATURE_NFC: YES / NO
> - PackageManager FEATURE_NFC_HOST_CARD_EMULATION: YES / NO
> - application HCE service component enabled: YES / NO
> - whether CardEmulation APIs can see the expected service/AID, where the
>   installed Android API exposes a safe query for this
> - expected AID:
>   F0454E5356324331
>
> If available through standard Android APIs without special permissions, also
> report the CATEGORY_OTHER selection/routing mode or whether this component is
> the active/default handler for the AID.
>
> Do not guess API methods.
> Inspect the installed Android SDK and use only supported APIs.
>
> Do not display:
> - Privy identifiers
> - wallet credentials
> - OTP/tokens
> - signatures
> - private data.
>
> Keep this diagnostic visually minimal.
>
> Do not redesign the app.
>
> PHASE 3 — BUILD
>
> Run:
>
> :app:testDebugUnitTest
> :app:assembleDebug
>
> Require all existing 21 Android JVM tests to remain green.
>
> Run:
>
> node --test --test-isolation=none
>
> Require:
> 83/83 PASS.
>
> Return the new APK path and SHA-256.
>
> Do not commit.
>
> PHASE 4 — MANUAL DEVICE CHECKPOINT
>
> After the APK builds, STOP for the user to install it manually on the Seeker.
>
> Tell the user exactly:
>
> 1. update the existing APK by USB file transfer;
> 2. open ENSv2 Access Demo;
> 3. keep the screen unlocked;
> 4. ensure system NFC is enabled;
> 5. open the new HCE diagnostics view;
> 6. report ONLY the YES/NO/status results shown there.
>
> Do NOT ask the user to present the phone to the PN532 yet.
>
> PHASE 5 — DECISION RULE
>
> After receiving device diagnostics:
>
> CASE A
>
> FEATURE_NFC_HOST_CARD_EMULATION = NO
>
> → STOP Gate D on Seeker.
>
> Do not attempt firmware fixes.
>
> Recommend testing the same Android APK on the user's Pixel/GrapheneOS device
> as the next isolated compatibility check.
>
> CASE B
>
> NFC adapter present = YES
> NFC enabled = NO
>
> → user enables NFC.
> → rerun diagnostics.
> → no code change.
>
> CASE C
>
> NFC = YES
> HCE feature = YES
> but service/AID is not registered/routable
>
> → identify the exact Android registration issue.
> → propose the smallest Android-only fix.
> → do not touch firmware yet.
>
> CASE D
>
> NFC = YES
> HCE = YES
> service/AID registration = healthy
>
> → Android capability is no longer the primary suspect.
>
> Then perform one bounded physical activation-only experiment:
>
> - Gate D firmware reset
> - user holds unlocked Seeker steadily against PN532
> - test only whether inListPassiveTarget() returns a target
>
> Do NOT proceed to SELECT in that diagnostic run if a firmware switch/temporary
> activation-only path is useful.
>
> If target activates:
> TARGET_ACTIVATION PASS
> and normal Gate D can resume.
>
> If target still does not activate:
> investigate RF/antenna/reader activation next.
>
> PHYSICAL ISOLATION IF NEEDED
>
> Only if Android diagnostics are completely healthy and target activation still
> fails:
>
> 1. reconfirm the same PN532 can still detect a known ISO14443A physical tag
>    using the previously validated UID firmware or a read-only equivalent;
> 2. do not authorize from that UID;
> 3. use this only to prove the PN532 RF field/antenna still works.
>
> Do not switch PN532 libraries.
>
> Do not change APDU v1.
>
> Do not add chunking.
>
> NO-TOUCH
>
> Do not modify:
>
> - Gate A semantics
> - Gate B signing semantics
> - APDU v1
> - scripts/ensv2/
> - scripts/nfc/
> - demo/
> - Privy local values
>
> Do not:
> - send blockchain transactions
> - implement ENS authorization
> - implement Gate E
> - commit
> - push
>
> RETURN FIRST AT THE MANUAL CHECKPOINT:
>
> # HCE TARGET ACTIVATION DIAGNOSTIC
>
> ## ANDROID CONFIG REVIEW
> ## DIAGNOSTIC IMPLEMENTATION
> ## ANDROID TESTS
> ## ANDROID BUILD
> ## NODE TESTS
> ## APK
> ## USER ACTION REQUIRED
>
> End:
>
> TARGET DIAGNOSTIC: AWAITING DEVICE RESULTS
>
> Do not continue further until the user reports the on-device diagnostic
> results.
