# Status

## Current objective

Perform sponsor-qualification and 30-second demo audit, then choose the smallest remaining improvement required for submission quality.

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
- Persistent physical credential slice: PASS. UserRegistry ownership kept `cred-001.demo-access.eth` REGISTERED while PermissionedResolver `access.v1` independently changed physical authorization from inactive to active to inactive.
- Persistent identity invariants were preserved throughout validation: owner, tokenId, resolver, and registry expiry did not change.
- The physical NFC UID remains a clonable prototype/demo identifier, not production proof of possession or anti-cloning security.
- Interactive visual demo: PASS. The browser UI drives real ENSv2 `access.v1` state through one-click ACTIVE/INACTIVE actions while the persistent credential remains visible and REGISTERED.
- Semantic no-op protection, client in-flight locking, pending-nonce checks, pre-submit RPC retries, and post-submit hash recovery prevent duplicate access writes across failure boundaries.
- Final dedicated-RPC validation reproduced NFC `DENY -> ALLOW -> DENY`, preserved credential identity, required no manual browser refresh, and ended with no pending DEV transaction.
- A dedicated Sepolia RPC configured through the existing local environment improved demo reliability; no endpoint or key is stored in the repository.

## Next

1. Perform sponsor-qualification and 30-second demo audit, then choose the smallest remaining improvement required for submission quality.

## Blockers

None currently.

## Risks

- Schedule pressure after time consumed by setup and hardware validation.
- ENSv2 integration learning may delay the first vertical slice.
- A static NFC UID is clonable and is suitable only as a prototype identifier, not secure proof of ownership.
- ENSv2 must remain central and authoritative rather than becoming a cosmetic sponsor integration.
- The ENSv2 credential tokenId/resource is mutable across unregister; the logical ENS name/label is the stable application reference.
- The completed slice is a prototype and does not claim production security.
