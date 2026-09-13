# LockENS

LockENS is a Sepolia prototype for wallet-held access passes whose current
authorization is resolved through ENSv2. A pass can remain registered to its
holder while global access, resource permissions, validity, and transferability
change independently.

The final demo shows one Android holder and three Android NFC gate stands. The
holder selects a pass, presents it over Android HCE/NFC, and signs a fresh
resource-bound proof with an embedded Privy wallet. A local Node service verifies
holder control offchain, reads current ENSv2 state on Sepolia, evaluates the
resource policy, and returns `ALLOW` or `DENY`. The virtual gate animation opens
only after authoritative Node `ALLOW`.

ENSv2 is load-bearing: the UserRegistry establishes current credential
registration and ownership, while the PermissionedResolver carries the access
and resource policy used in every decision. Privy provides embedded-wallet
onboarding and signing; it is not the resource-authorization engine.

## Current demo architecture

```text
Android holder
  -> NFC / Android HCE
  -> Android NFC gate
  -> local Node authoritative verifier
  -> Sepolia RPC
  -> ENSv2 UserRegistry + PermissionedResolver / resource policy
  -> ALLOW / DENY
  -> virtual gate UI
```

Node issues and verifies a one-use EIP-712 holder challenge offchain, then reads
a coherent current ENSv2 snapshot. An active credential is not sufficient by
itself: registration, ownership, validity, resolver provenance, global access,
and the selected gate's resource permission must all pass. A missing bridge,
RPC failure, stale state, invalid proof, or policy denial fails closed.

## What the demo demonstrates

- Physical NFC between the holder and Android gate readers.
- Wallet-specific pass discovery, multi-wallet selection, selected-pass HCE,
  and invalidation when wallet/pass state changes.
- Current ENSv2 ownership and policy reads on Sepolia.
- Resource-specific outcomes across Front Door, Lab, and Server Room.
- An authoritative Node decision before any virtual opening animation.
- Issuance and management review flows backed by previously confirmed public
  Sepolia transactions; no new transaction is required to inspect the source.

The final physical matrix includes Staff grants at Lab and Server Room, a
Visitor denial at Lab and grant at Front Door, and a suspended Contractor denial
at Lab. Full public state and transaction evidence are in
[FINAL_DEMO_STATE.md](FINAL_DEMO_STATE.md).

## Reproduce the software checks

### Requirements

- Node.js 24 or newer.
- Java 17 or newer for Gradle. Android sources target Java 17.
- Android SDK with compile SDK 37 for Android builds.
- Existing npm and Gradle dependencies/caches, or network access to obtain them
  in a normal development environment.
- Optional firmware validation: Arduino CLI with the ESP32 core and compatible
  PN532/Wire libraries.

Install Node dependencies from the lockfile in a normal development checkout:

```powershell
npm ci
```

Run the complete Node suite:

```powershell
node --test --test-isolation=none
```

Run Android unit tests and build the debug APK:

```powershell
cd mobile/android
./gradlew testDebugUnitTest assembleDebug
```

On Windows use `gradlew.bat`. If the Android SDK is not globally configured, set
`ANDROID_HOME`/`ANDROID_SDK_ROOT` or use an ignored `local.properties` file.

The frozen release validation passed 238/238 Node tests, 261 Android unit tests,
Android `assembleDebug`, and compilation of the three approved ESP32-S3
firmware sketches.

## Local configuration and operation

Copy `.env.example` or `.env.nfc.example` to the corresponding ignored local
file and replace only the documented placeholders. Android Privy configuration
uses `mobile/android/privy.local.properties.example`; the local copy is ignored.
Private keys, credential-bearing RPC endpoints, application credentials, and
device mappings must never be committed.

Compilation and source inspection do not require the developer's issuer private
key. Public judges can build the Android app, run tests, inspect public Sepolia
configuration, and review the architecture without authority to issue or modify
credentials. Operating the exact live demo additionally requires local Privy
client configuration, a Sepolia RPC endpoint, the local Node verifier, the
existing demo devices, and the already-issued passes. Issuer-authorized writes
are a separate, explicitly controlled operation.

The final local verifier entry point is:

```powershell
npm run pixel-gate:bridge
```

Generic setup and architecture references are documented in
[ANDROID_GATE_READER.md](ANDROID_GATE_READER.md), [GATE_STAND.md](GATE_STAND.md),
and [ANDROID_GATE_DEMO_RUNBOOK.md](ANDROID_GATE_DEMO_RUNBOOK.md). The root
[DEMO_RUNBOOK.md](DEMO_RUNBOOK.md) is retained only for the historical/alternate
ESP32-S3 + PN532 workflow. Device-specific recording-day data stays under ignored
`.runtime/` files.

## Evidence and limitations

Verified release evidence includes the final resource-policy matrix, physical
NFC grant/deny cases, multi-wallet selection, HCE freshness/invalidation, the
test/build results above, and current ENSv2/Sepolia configuration. The
ESP32-S3 + PN532 implementation remains an alternative experimental hardware
route with its own boot, wiring, and interoperability limitations; it is not the
primary final demo gate.

LockENS remains a Sepolia prototype:

- The UI controls a virtual gate animation, not a physical lock or actuator.
- The final architecture depends on a local PC running the Node verifier.
- Validation covers the documented Android device matrix, not universal Android
  compatibility.
- The system is not production-ready, fully decentralized, offline/autonomous,
  or a secure hardware-enrollment system.
- Revocation is reflected when the verifier obtains current authoritative state;
  no instant atomic physical-revocation claim is made.
- The alternate PN532 route is not claimed as universally stable or
  production-ready.

Earlier checkpoint reports are retained as historical engineering evidence and
may describe missing policy, pending Android fallback work, or the experimental
PN532 path before the final Android-gate demo was completed. They are not the
current product state. Start with this README, [STATUS.md](STATUS.md), and
[FINAL_DEMO_STATE.md](FINAL_DEMO_STATE.md).

## License, assets, and AI disclosure

Original LockENS software is available under the [MIT License](LICENSE).
Third-party artwork remains under its source license and is not relicensed by
MIT; see [ATTRIBUTIONS.md](ATTRIBUTIONS.md). AI-assisted development, human
control, asset generation, specifications, and sanitized prompt evidence are
documented in [AI_USAGE.md](AI_USAGE.md), [docs/ai/SPECS.md](docs/ai/SPECS.md),
and [docs/ai/PROMPTS.md](docs/ai/PROMPTS.md).
