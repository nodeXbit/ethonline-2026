# LockENS physical NFC checkpoint

## Scope and audit

Previous pushed baseline: `20c2dd9866f8c48bc0611dee34b9b272906a3429`. This checkpoint contains the current Dynamic NFC/Virtual Gates implementation and existing reliability/diagnostic work, not new features. User explicitly authorized commit and push to origin/main. No Pixel Reader, animations, transport changes, negative-length repair or onchain policy writes are part of checkpointing.

A = production implementation; B = physically exercised reliability improvement; C = monitor coordination/UI; D = preserved diagnostics/evidence; E = documentation; F = excluded generated/debug material; G = unrelated/suspicious. No unrelated G change was identified. Every changed/new nonignored file is classified below; multiple categories indicate an existing mixed-purpose file rather than artificial commit splitting.

Excluded without deletion: `.runtime/`, private `.env`/Android local properties, Android APK/build outputs, and generated `firmware/*/build/` binaries/maps/options. The previously untracked Arduino outputs are now explicitly ignored. Existing diagnostic sketches and sanitized reports are retained. Raw physical proofs/signatures and full payload logs remain excluded. A serialized synthetic fixture signature was removed; opaque Android test bytes preserve transport assertions without cryptographic signing. Fixture LF is pinned; patch whitespace attributes preserve upstream context exactly. No transport behavior changed in this housekeeping.

## Validation of actual source

- Android `testDebugUnitTest`: 246 tests, 0 failures/errors/skips.
- `assembleDebug`: PASS. APK SHA-256 `45004d6ecd239cb1ac317dc9f684cfdc4bb3182e06ac527f67c11b11b00ce909`, exactly matching read-only hash of installed Seeker base.apk. No installation performed.
- Node full suite `node --test --test-isolation=none`: 229 tests, 0 failures/skips. Offline synthetic test cryptography uses no real wallet, credentials or network writes.
- Gate Monitor standalone suite: 1/1; included in the full Node count. Dynamic bridge coverage also passes in that full suite.
- Dynamic firmware: PASS, program 307945 bytes, globals 23504; output 308096 bytes. Reference SHA-256 reproduced exactly.
- Legacy firmware: PASS, program 305149 bytes, globals 23144. This is a source build check, not a new physical legacy validation.
- Three archived diagnostic patches apply cleanly and reproduce normalized reference source hashes. Dependency portability limitation: local PN532 libraries have source fingerprints but no established upstream revision.
- `git diff --check` and staged diff check: PASS.

Initial validation issues were tooling/fixture formatting: Android required explicit ANDROID_HOME; Windows CRLF in the shared fixture broke two Node vector comparisons. Both were corrected and validations rerun. No production behavior was changed to make tests pass.

## Physical evidence preserved

Historical authorized tap, 2026-09-12 13:43:43-13:43:47 UTC: `staff-001.keys.demo-access.eth` -> virtual Lab. GET_CREDENTIAL succeeds; SEND_CHALLENGE is 109 bytes and succeeds; Android returns a 67-byte signature APDU, yielding a 65-byte holder proof. Holder verified, registration valid, access.v1 Allowed, proof fresh, controller confirmed. Final **ACCESS DENIED / RESOURCE_POLICY_MISSING**. Existing STAFF has no resources.v1. This is an authorization decision, not transport failure. No lock/relay actuator exists.

Reference firmware `.runtime/firmware-irq-cause/pn532_dynamic_access.ino.bin` SHA-256: `4914015019c3659de25fd13d55ccb314b09ac4b0dfd7667f5407874e9b08ed9e`.

Sanitized events: [physical evidence](docs/evidence/physical-nfc-2026-09-12.json). Diagnostics: [source patches](firmware/diagnostics/README.md). Full raw local artifacts are intentionally not in Git.

Boot stability is not fully characterized: ten recorded successful boots were followed by a SCL/SAM failure. SDA recovery is physically exercised; SCL cause and durable recovery remain unresolved. Subsequent IRQ diagnostic boots and a complete NFC exchange passed, but the earlier fault has not been reproduced with its IRQ cause recorded. Pixel/Android Gate Reader fallback is NOT implemented.

## Security and checkpoint boundaries

No real secrets or serialized signatures are admitted in this checkpoint. The scan's credential-shaped URL finding is an existing explicitly fake example.test redaction test, not an account credential. Public chain addresses/hashes, source hashes and reference firmware hashes are not secrets. Test accounts are deliberately synthetic.

Checkpoint operations: blockchain writes 0; real-wallet signatures 0; physical taps 0; firmware flashes 0; APK installations 0. assembleDebug uses the normal local Android debug APK signing process; offline test cryptography is not a real-wallet signature. No commits are cryptographically signed. The final commit ID and remote equality are reported after push rather than self-embedded in this file.

## File classification

| File | Category | Rationale |
|---|---|---|
| `.gitattributes` | F exclusion / validation | Ignore generated Arduino artifacts; preserve exact fixture/patch line endings |
| `.gitignore` | F exclusion / validation | Ignore generated Arduino artifacts; preserve exact fixture/patch line endings |
| `DECISIONS.md` | E | Documentation; historical reports retained, canonical state corrected |
| `DYNAMIC_NFC_RUNBOOK.md` | E | Documentation; historical reports retained, canonical state corrected |
| `NFC_AB_A1_RESULT.md` | E | Documentation; historical reports retained, canonical state corrected |
| `NFC_AB_A2_BOOT_RESULT.md` | E | Documentation; historical reports retained, canonical state corrected |
| `NFC_AB_TEST_PLAN.md` | E | Documentation; historical reports retained, canonical state corrected |
| `NFC_BOOT_STABILITY_SERIES.md` | E | Documentation; historical reports retained, canonical state corrected |
| `NFC_BUS_CLEAR_CANDIDATE.md` | E | Documentation; historical reports retained, canonical state corrected |
| `NFC_DRIVER_STATE_DIAGNOSTIC.md` | E | Documentation; historical reports retained, canonical state corrected |
| `NFC_FIX_VALIDATION.md` | E | Documentation; historical reports retained, canonical state corrected |
| `NFC_IDF_0X103_INVESTIGATION.md` | E | Documentation; historical reports retained, canonical state corrected |
| `NFC_IRQ_CAUSE_INVESTIGATION.md` | E | Documentation; historical reports retained, canonical state corrected |
| `NFC_LINE_DIAGNOSTIC.md` | E | Documentation; historical reports retained, canonical state corrected |
| `NFC_PHYSICAL_TEST_REPORT.md` | E | Documentation; historical reports retained, canonical state corrected |
| `NFC_PORT_DIAGNOSTIC_RESULT.md` | E | Documentation; historical reports retained, canonical state corrected |
| `NFC_READ_ONLY_AUDIT.md` | E | Documentation; historical reports retained, canonical state corrected |
| `NFC_RECOVERED_BUS_COMPARISON.md` | E | Documentation; historical reports retained, canonical state corrected |
| `NFC_SAM_IDF_DIAGNOSTIC.md` | E | Documentation; historical reports retained, canonical state corrected |
| `NFC_SAM_STARTUP.md` | E | Documentation; historical reports retained, canonical state corrected |
| `NFC_SAM_WIRE_DIAGNOSTIC.md` | E | Documentation; historical reports retained, canonical state corrected |
| `NFC_SCL_LOW_INVESTIGATION.md` | E | Documentation; historical reports retained, canonical state corrected |
| `NFC_STARTUP_STABILIZATION.md` | E | Documentation; historical reports retained, canonical state corrected |
| `NFC_TEST_COORDINATION.md` | E | Documentation; historical reports retained, canonical state corrected |
| `PROJECT.md` | E | Documentation; historical reports retained, canonical state corrected |
| `README.md` | E | Documentation; historical reports retained, canonical state corrected |
| `STATUS.md` | E | Documentation; historical reports retained, canonical state corrected |
| `WORKLOG.md` | E | Documentation; historical reports retained, canonical state corrected |
| `config/access-resources.json` | A | Dynamic NFC/resource implementation, build integration or offline tests |
| `demo/gate-monitor.mjs` | C | Local virtual-gate monitor, coordination/UI and regression coverage |
| `demo/gate-monitor.test.mjs` | C | Local virtual-gate monitor, coordination/UI and regression coverage |
| `demo/gate-public/app.js` | C | Local virtual-gate monitor, coordination/UI and regression coverage |
| `demo/gate-public/index.html` | C | Local virtual-gate monitor, coordination/UI and regression coverage |
| `demo/gate-public/style.css` | C | Local virtual-gate monitor, coordination/UI and regression coverage |
| `docs/evidence/physical-nfc-2026-09-12.json` | D | Sanitized historical physical event evidence, no signature/nonce payload |
| `firmware/diagnostics/PN532_I2C.cpp.patch` | D | Existing diagnostic source/patches and reference fingerprints |
| `firmware/diagnostics/README.md` | E | Documentation; historical reports retained, canonical state corrected |
| `firmware/diagnostics/Wire.cpp.patch` | D | Existing diagnostic source/patches and reference fingerprints |
| `firmware/diagnostics/i2c_master.c.patch` | D | Existing diagnostic source/patches and reference fingerprints |
| `firmware/diagnostics/reference-manifest.json` | D | Existing diagnostic source/patches and reference fingerprints |
| `firmware/pn532_dynamic_access/pn532_dynamic_access.ino` | A | Dynamic NFC/resource implementation, build integration or offline tests |
| `firmware/pn532_line_diagnostic/pn532_line_diagnostic.ino` | D | Existing diagnostic source/patches and reference fingerprints |
| `firmware/pn532_secure_access/pn532_bus_recovery.h` | B | Physically exercised SDA recovery; SCL limitations documented |
| `firmware/pn532_secure_access/pn532_secure_access.ino` | A/B/D | Dynamic discovery, validated boot responses and bounded diagnostics; legacy still builds |
| `fixtures/lockens-access-v1.properties` | A | Dynamic NFC/resource implementation, build integration or offline tests |
| `mobile/android/app/build.gradle.kts` | A | Dynamic NFC/resource implementation, build integration or offline tests |
| `mobile/android/app/src/main/java/io/github/nodexbit/ethonline2026/AccessResources.kt` | A | Dynamic NFC/resource implementation, build integration or offline tests |
| `mobile/android/app/src/main/java/io/github/nodexbit/ethonline2026/CredentialAbi.kt` | A | Dynamic NFC/resource implementation, build integration or offline tests |
| `mobile/android/app/src/main/java/io/github/nodexbit/ethonline2026/CredentialPersistence.kt` | A | Dynamic NFC/resource implementation, build integration or offline tests |
| `mobile/android/app/src/main/java/io/github/nodexbit/ethonline2026/CredentialReader.kt` | A | Dynamic NFC/resource implementation, build integration or offline tests |
| `mobile/android/app/src/main/java/io/github/nodexbit/ethonline2026/GateBApplication.kt` | A | Dynamic NFC/resource implementation, build integration or offline tests |
| `mobile/android/app/src/main/java/io/github/nodexbit/ethonline2026/MainActivity.kt` | A | Dynamic NFC/resource implementation, build integration or offline tests |
| `mobile/android/app/src/main/java/io/github/nodexbit/ethonline2026/PassStudio.kt` | A | Dynamic NFC/resource implementation, build integration or offline tests |
| `mobile/android/app/src/main/java/io/github/nodexbit/ethonline2026/ProductUiPolicy.kt` | A | Dynamic NFC/resource implementation, build integration or offline tests |
| `mobile/android/app/src/main/java/io/github/nodexbit/ethonline2026/StudioSafety.kt` | A | Dynamic NFC/resource implementation, build integration or offline tests |
| `mobile/android/app/src/main/java/io/github/nodexbit/ethonline2026/hce/GateC1HostApduService.kt` | A/D | Dynamic/legacy service selection and DEBUG-only bounded metadata logs |
| `mobile/android/app/src/main/java/io/github/nodexbit/ethonline2026/hce/HceApduProcessor.kt` | A | Dynamic NFC/resource implementation, build integration or offline tests |
| `mobile/android/app/src/main/java/io/github/nodexbit/ethonline2026/hce/NfcSelectionState.kt` | A | Dynamic NFC/resource implementation, build integration or offline tests |
| `mobile/android/app/src/main/java/io/github/nodexbit/ethonline2026/hce/PrivyProofProvider.kt` | A | Dynamic NFC/resource implementation, build integration or offline tests |
| `mobile/android/app/src/test/java/io/github/nodexbit/ethonline2026/StudioResourceTest.kt` | A | Dynamic NFC/resource implementation, build integration or offline tests |
| `mobile/android/app/src/test/java/io/github/nodexbit/ethonline2026/StudioSafetyTest.kt` | A | Dynamic NFC/resource implementation, build integration or offline tests |
| `mobile/android/app/src/test/java/io/github/nodexbit/ethonline2026/hce/DynamicHceTest.kt` | A | Dynamic NFC/resource implementation, build integration or offline tests |
| `package.json` | A | Dynamic NFC/resource implementation, build integration or offline tests |
| `scripts/security/access-resources.mjs` | A | Dynamic NFC/resource implementation, build integration or offline tests |
| `scripts/security/dynamic-gate-bridge.mjs` | A | Dynamic NFC/resource implementation, build integration or offline tests |
| `scripts/security/dynamic-gate-bridge.test.mjs` | A | Dynamic NFC/resource implementation, build integration or offline tests |
| `scripts/security/resource-simulation.mjs` | A | Dynamic NFC/resource implementation, build integration or offline tests |
| `scripts/security/resource-vectors.mjs` | A | Dynamic NFC/resource implementation, build integration or offline tests |
| `scripts/security/resource-verifier.mjs` | A | Dynamic NFC/resource implementation, build integration or offline tests |
| `scripts/security/resource-verifier.test.mjs` | A | Dynamic NFC/resource implementation, build integration or offline tests |
| `PHYSICAL_NFC_CHECKPOINT.md` | E | Checkpoint inventory, evidence and validation record |
