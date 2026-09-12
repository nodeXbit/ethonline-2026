# LockENS dynamic NFC and resource-aware virtual gates

## Physical NFC checkpoint - 2026-09-12

Dynamic NFC works physically end-to-end with `staff-001.keys.demo-access.eth` and virtual gate **Lab**. The authorized tap at 13:43:43-13:43:47 UTC completed discovery, the 109-byte challenge APDU, the 67-byte signature APDU response, the 65-byte holder proof, fresh authoritative verification and serial controller confirmation. Final decision: **ACCESS DENIED / RESOURCE_POLICY_MISSING**. Existing STAFF has no `resources.v1`; no resource policy was written. This is an authorization decision, not a transport failure.

One physical ESP32-S3/PN532 verifier simulates Front Door, Lab and Server Room. There is **no physical lock/relay actuator**. Pixel/Android Gate Reader fallback and monitor-animation work are **not implemented**.

PN532/I2C boot stability is **not fully characterized**. SDA recovery passed repeatedly; a ten-pass series was followed by a SCL/SAM failure. Later IRQ instrumentation has not reproduced that fault. Neither the electrical cause nor prolonged stability is established. The negative-length library defect remains unfixed.

Reference firmware: `.runtime/firmware-irq-cause/pn532_dynamic_access.ino.bin`, SHA-256 `4914015019c3659de25fd13d55ccb314b09ac4b0dfd7667f5407874e9b08ed9e`. Current-source recompilation reproduced that hash. Ignored binaries remain local; diagnostic patches and source fingerprints are preserved in [firmware/diagnostics](firmware/diagnostics/README.md).

Checkpoint validation: **246 Android unit tests**, **229 Node tests**, `assembleDebug`, dynamic and legacy firmware builds, and the standalone Gate Monitor test pass. The monitor test is included in the Node total. The rebuilt APK matches the installed APK: `45004d6ecd239cb1ac317dc9f684cfdc4bb3182e06ac527f67c11b11b00ce909`; no APK installation is needed.

See [checkpoint audit](PHYSICAL_NFC_CHECKPOINT.md), [sanitized physical evidence](docs/evidence/physical-nfc-2026-09-12.json), [test coordination](NFC_TEST_COORDINATION.md), and [boot/IRQ limitations](NFC_IRQ_CAUSE_INVESTIGATION.md). Checkpoint work performs no blockchain writes, real-wallet signatures, firmware flashes, credential changes, or new physical taps. Offline synthetic test cryptography is distinct from wallet signing; committed fixtures contain no serialized signatures.


## Dependency map and trust boundaries

```text
SelectedPassStore (active wallet + Sepolia)
  -> fresh validated My Keys snapshot
  -> NfcSelectionState + captured active Privy wallet/provider
  -> HceApduProcessor session identity
  -> GET_CREDENTIAL -> PN532/ESP32 -> opaque serial discovery
  -> DynamicGateBridge -> exact ENS hierarchy/provenance preflight
  -> latched virtual resource + fresh existing EIP-712 challenge
  -> SEND_CHALLENGE -> Privy proof -> GET_STATUS/GET_SIGNATURE
  -> final coherent ENS owner/access/resources reread + one-shot proof check
  -> existing Gate E serial authorization/confirmation state machine
  -> Gate Monitor final virtual grant/deny
```

Android presents identity and signs; Node independently decides authorization. PN532/ESP32 transports APDUs and serial data without ENS, RPC, wallet, policy, or UID authorization logic. Existing Gate E timing, whole-snapshot fallback, one-shot transport, and controller confirmation are reused.

The verifier pins Sepolia (11155111), `demo-access.eth -> R0 -> keys -> R1`, exact S1, registered/unexpired hierarchy, factory-verified R1/S1 implementations, and the direct credential's zero subregistry. Each read uses one block number and checks that block's hash again afterward. Final reads cannot regress behind the preflight block.

R0: `0x2d249472B83A453086254Acd8a42913D8e45a2Fd`

R1: `0x0AeB395be893149c0b60D9DAC3Ba55139D0dcA1a`

S1: `0x20766FB21498a99350F922ee3163F5a95F354a7f`

## Resource contract and Studio

Public source: `config/access-resources.json`. IDs are `keccak256(UTF8("lockens:resource:v1:" + slug))`. Android derives its build configuration from this file; both languages validate the derivation against shared fixtures.

| Slug | Display name | Resource ID |
| --- | --- | --- |
| front-door | Front Door | `0x42ec975e932957e2fe4f47cb698484ecd0ccd716ed14eb3d83afb5be796308a6` |
| lab | Lab | `0xa5463be76a1c842cab9c8fa2f5460c5f35fa716fa7ee1fdad33c97e6b5a0ea25` |
| server-room | Server Room | `0x0abcec70a5d096e96850983cba03efafdb54dc4671e461d56341c528c00e0d78` |

S1 `resources.v1` is canonical ABI encoding of a sorted, unique `bytes32[]` containing only configured IDs. No wildcard exists. Missing data denies with `RESOURCE_POLICY_MISSING`; an empty set denies with `RESOURCE_NOT_ALLOWED`; noncanonical/malformed/unknown-ID data denies with `RESOURCE_POLICY_INVALID`. Global `access.v1` remains separate and must be active with `validUntil >= now`.

Editable issuance presets: STAFF = all three resources; VISITOR = Front Door; CONTRACTOR = Lab. TX2's existing S1 multicall includes optional avatar, description, `access.v1`, and `resources.v1`. Selected resources survive transaction recovery and are checked in final readback. Historical issuance sessions retain their original calldata instead of silently gaining permissions.

Manage Resources uses the existing safe management engine with distinct `RESOURCE_POLICY_UPDATE`, SET_DATA authority, human old/new resource review, fresh preflight comparison, wallet/write-lease binding, exact calldata and receipt linkage, recovery, and final authoritative readback. No management action was submitted during this task.

Read-only live verification at Sepolia block 11687400 confirmed that existing staff is registered and has no resource policy. Resource-aware verification therefore denies it with `RESOURCE_POLICY_MISSING`. A later, separately authorized Studio write is needed for a real resource-aware ALLOW.

## HCE and proof protocol

| Operation | Bytes |
| --- | --- |
| AID | `F0454E5356324331` |
| SELECT AID | `00 A4 04 00 08 F0 45 4E 53 56 32 43 31` |
| GET_CREDENTIAL | `80 40 01 00` |
| SEND_CHALLENGE | `80 10 01 00 68` + 104-byte body |
| GET_STATUS | `80 20 01 00` |
| GET_SIGNATURE | `80 30 01 00` |

GET_CREDENTIAL returns 1..64 normalized UTF-8 name bytes plus `90 00`. Existing APDU error words remain in use; unavailable identity fails closed with `69 85`. No selected pass, stale validation, wrong chain, or invalidated ownership yields no identity. My Keys shows NFC readiness and the selected full name.

SELECT latches the published wallet/provider, credential, and chain. A wallet/selection/session change invalidates subsequent APDUs and late signer callbacks. Dynamic signing uses the captured active embedded wallet, not the first wallet. Android requires GET_CREDENTIAL before signing, exact selected namehash, a configured resource, a future expiry no more than 60 seconds away, one challenge per session, and a bounded nonce replay guard. It does not decide resource authorization.

The existing body remains `credential[32] || resource[32] || nonce[32] || expiresAt[8 unsigned big-endian]`. Existing EIP-712 domain `ENSv2 Access`, version `1`, Sepolia and AccessChallenge types are unchanged. The signature binds credential, resource, nonce, expiry and domain. Node accepts only its own pending challenge and consumes a valid current-holder proof once before evaluating access/resource denial.

| Preserved bound | Value |
| --- | --- |
| Challenge TTL | 60 seconds |
| Post-challenge total | 50 seconds |
| Proof receive | 30 seconds |
| ENS verification, including fallback | 8 seconds |
| Controller confirmation | 2 seconds |
| Maximum block age | 60 seconds |
| Maximum future block skew | 15 seconds |

Dynamic serial adds a 120-second overall waiting deadline. Firmware retains bounded challenge waiting and 20-second status polling. Local NFC selection publication and snapshot age are bounded to 60 seconds; refresh My Keys if readiness expires.

## Firmware and serial

`firmware/pn532_dynamic_access/pn532_dynamic_access.ino` selects the dynamic build of the shared existing transport sketch. After SELECT it performs GET_CREDENTIAL, bounds the response, checks `90 00`, and emits opaque hex. Legacy `firmware/pn532_secure_access` remains a separate build.

Dynamic startup marker: `LOCKENS_DYNAMIC_V1_READY`. Discovery: `CREDENTIAL_V1=0x<lowercase UTF-8 hex>` (1..64 decoded bytes). Existing `CHALLENGE=0x...`, `PROOF=0x...`, `AUTHORIZATION=ALLOW`/`AUTHORIZATION=DENY` commands and exact `AUTHORIZATION: ALLOW`/`AUTHORIZATION: DENY` confirmation remain. Node strictly decodes UTF-8, bounds lines, rejects duplicates/order errors, and never logs proof bytes to the monitor. Dynamic mode rejects legacy firmware instead of falling back.

One firmware session runs per reset. Wire uses SDA 17/SCL 18 and a 128-byte buffer; serial is 115200 baud. Opening the serial monitor is not proof of a successful NFC exchange. Controller confirmation is serial evidence only.

## Monitor and offline simulation

```powershell
node demo/gate-monitor.mjs
node scripts/security/resource-simulation.mjs
```

Monitor: `http://127.0.0.1:8790`. Default mode is preview only, with physical attempts disabled. It uses plain HTML/CSS/JS, a loopback-only server, strict Host/Origin checks, bounded reason codes, and no external frontend dependency. It shows current and next resource, credential, holder, registration, global access, resource policy, proof, controller, and final decision. Gate changes during an attempt affect the next attempt only. ACCESS GRANTED requires exact controller confirmation; no prior result carries into a new attempt.

For a later explicitly approved physical proof session, after flashing:

```powershell
node --env-file=.env.nfc.local demo/gate-monitor.mjs --port COM4 --physical-proof-approved --http-port 8791
```

Use the existing ignored local environment file; do not put RPC credentials in tracked files. Open `http://127.0.0.1:8791`, choose the gate, start an attempt, then reset the ESP32 so Node observes its dynamic-ready marker before presenting the phone. The firmware halts after each outcome; start another attempt and reset for another tap. Port COM4 was discovered read-only; confirm it still identifies the same ESP32 before future flashing.

The physical switch permits real holder signing and must not be enabled during read-only review. This task did not enable it or open a physical serial proof session.

## Validation and matrix

Android: **244 JVM tests passed**, debug assembly passed, final APK installed with `adb install -r`. Node: **224 tests passed**, zero failures/skips. Both firmware targets compile for `esp32:esp32:esp32s3`; dynamic program size 305441 bytes, legacy 305033 bytes. Firmware constants, APDU commands, response bounds, and serial frames are checked by Node tests; no separate native firmware test suite exists. `git diff --check` passed.

Shared `fixtures/lockens-access-v1.properties` covers resource hashes, ABI encoding, credential namehash, challenge bytes, EIP-712 digest, APDU and serial vectors. No private key or signature is serialized into that file; Android transport tests use opaque bytes, while offline Node tests exercise synthetic cryptography.

| Offline fixture | Result |
| --- | --- |
| STAFF -> Lab | ALLOW + simulated controller confirmation |
| VISITOR -> Lab | DENY / RESOURCE_NOT_ALLOWED |
| CONTRACTOR -> Lab (suspended) | DENY / ACCESS_SUSPENDED |
| STAFF -> Server Room | ALLOW + simulated controller confirmation |
| VISITOR -> Front Door | ALLOW + simulated controller confirmation |
| Same consumed proof | DENY / REPLAY |
| Wrong holder/credential | DENY / HOLDER_MISMATCH |

Adversarial coverage includes wallet/selection changes before and during signing, chain/namehash/resource substitution, expired/repeated nonces, untrusted names, registry/resolver/provenance mismatch, malformed policy, stale/future/reorged snapshots, owner/policy changes after challenge, concurrent proof consumption, controller timeout, late discovery, gate changes during an attempt, and Studio resource recovery/TOCTOU.

Monitor HTTP/state tests passed. A browser surface was unavailable in this environment, so rendered browser QA is still pending. No dynamic physical success is claimed, and a real multi-pass selection test still requires multiple real owned credentials.

## Build artifacts and controlled flash handoff

Builds were performed with Arduino CLI and the installed ESP32 toolchain. Repeat from the repository root:

```powershell
arduino-cli compile --fqbn esp32:esp32:esp32s3 --output-dir .runtime/firmware-dynamic firmware/pn532_dynamic_access
arduino-cli compile --fqbn esp32:esp32:esp32s3 --output-dir .runtime/firmware-legacy firmware/pn532_secure_access
```

Dynamic application artifact: `.runtime/firmware-dynamic/pn532_dynamic_access.ino.bin` (305584 bytes). The same directory contains bootloader, partition and merged images. Android artifact: `mobile/android/app/build/outputs/apk/debug/app-debug.apk`.

Validated artifact SHA-256 values:

- Dynamic application: `e14e61d27ca7725d0e8a0912af8ccb3168d6fca3650b11d69c22aec2cfb7f513`
- Installed debug APK: `07c742314ed40354f6d6aa2783d5fb86426368755d2321a866b0bc828319719a`

**STOP before flashing.** Firmware flash is required for dynamic discovery and was not authorized for automatic execution in this task. For a later explicitly authorized flash, close any serial client and run:

```powershell
arduino-cli upload --fqbn esp32:esp32:esp32s3 --port COM4 --input-dir .runtime/firmware-dynamic firmware/pn532_dynamic_access
```

Then follow the monitor sequence above under separate physical-signature authorization. First verify the selected existing staff identity and expected missing-policy DENY. Do not create credentials or assign resources just to make a test green without a separately reviewed and authorized write.

Legacy regression: build/use the original secure-access sketch, original Gate E bridge, and the explicit Android diagnostics legacy NFC checkbox. Dynamic product mode never switches to that path automatically.

## Implementation files

- Public contract/vectors: `config/access-resources.json`, `fixtures/lockens-access-v1.properties`, `scripts/security/access-resources.mjs`, `resource-vectors.mjs`.
- Gate: `scripts/security/resource-verifier.mjs`, `dynamic-gate-bridge.mjs`, `resource-simulation.mjs` and their tests; `demo/gate-monitor.mjs`, its test and `demo/gate-public/*`.
- Firmware: `firmware/pn532_dynamic_access/*`, guarded dynamic additions to `firmware/pn532_secure_access/pn532_secure_access.ino`.
- Android: `AccessResources`, `NfcSelectionState`, HCE processor/service/signer, application/Activity selection wiring, Studio draft/persistence/ABI/reader/safety/UI, build config and resource/HCE tests.
- Handoff: this runbook, README, STATUS, WORKLOG, DECISIONS, and package scripts.
