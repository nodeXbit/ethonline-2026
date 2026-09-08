# Status

## Current objective

Define and implement the prototype mapping from a physical NFC identifier to the already validated ENSv2 credential state.

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
- First ENSv2 Sepolia vertical slice: DONE. `demo-access.eth` is attached to UserRegistry `0x2d249472B83A453086254Acd8a42913D8e45a2Fd`; register/read/unregister lifecycle passed with ENSv2 authoritative for authorization state.

## Next

1. Define and implement the prototype mapping from a physical NFC identifier to the already validated ENSv2 credential state.

## Blockers

None currently.

## Risks

- Schedule pressure after time consumed by setup and hardware validation.
- ENSv2 integration learning may delay the first vertical slice.
- A static NFC UID is clonable and is suitable only as a prototype identifier, not secure proof of ownership.
- ENSv2 must remain central and authoritative rather than becoming a cosmetic sponsor integration.
- The ENSv2 credential tokenId/resource is mutable across unregister; the logical ENS name/label is the stable application reference.
- The completed slice is a prototype and does not claim production security.
