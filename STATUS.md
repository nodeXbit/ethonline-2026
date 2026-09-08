# Status

## Current objective

Design and validate the smallest persistent-credential access lifecycle where the ENSv2 credential can survive after physical access expires, instead of using unregister/burn as the final product model.

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
- Physical NFC → ENSv2 authorization vertical slice: PASS. The same physical tag (`91:2D:E3:06`) produced `DENY → ALLOW → DENY` solely from ENSv2 credential state for `cred-001.demo-access.eth`.

## Next

1. Design and validate the smallest persistent-credential access lifecycle where the ENSv2 credential can survive after physical access expires, instead of using unregister/burn as the final product model.

## Blockers

None currently.

## Risks

- Schedule pressure after time consumed by setup and hardware validation.
- ENSv2 integration learning may delay the first vertical slice.
- A static NFC UID is clonable and is suitable only as a prototype identifier, not secure proof of ownership.
- ENSv2 must remain central and authoritative rather than becoming a cosmetic sponsor integration.
- The ENSv2 credential tokenId/resource is mutable across unregister; the logical ENS name/label is the stable application reference.
- The completed slice is a prototype and does not claim production security.
