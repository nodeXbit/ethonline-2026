# Status

## Current objective

Establish the first ENSv2 Sepolia vertical slice, keeping ENSv2 authoritative for digital access credential state and connecting it to the validated ESP32-S3 + PN532 NFC path.

## Done

- ChatGPT Project configured
- GitHub repository created and connected
- Local repository cloned
- VS Code ready
- Git ready
- Node.js 24 ready
- npm ready
- Codex ready
- dedicated DEV wallet ready
- Hardware feasibility: PASS (PC -> ESP32-S3 -> PN532 over I2C -> physical ISO14443A tag).
- Project direction selected: open physical-access credential prototype backed by ENSv2 on Sepolia.
- Primary sponsor selected: ENS / ENSv2.
- Local ESP32-S3 serial test and PN532 NFC tag-reading firmware completed.

## Next

1. Confirm the smallest ENSv2 Sepolia write/read path required for credential state.
2. Implement and verify that ENSv2 vertical slice independently of the hardware loop.
3. Define the prototype mapping between an NFC UID identifier and ENSv2 credential state.
4. Connect the validated NFC interaction to the ENSv2-backed access decision.

## Blockers

None currently.

## Risks

- Schedule pressure after time consumed by setup and hardware validation.
- ENSv2 integration learning may delay the first vertical slice.
- A static NFC UID is clonable and is suitable only as a prototype identifier, not secure proof of ownership.
- ENSv2 must remain central and authoritative rather than becoming a cosmetic sponsor integration.
