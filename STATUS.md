# Status

## Current objective

Timebox the secure holder-proof vertical slice: mobile/Privy embedded EOA signer -> fresh EIP-712 challenge -> NFC/mobile transport -> verifier -> authoritative ENSv2 authorization.

Do not add Privy payment, World, ERC-4337, or another sponsor layer until this security boundary is either validated or explicitly stopped by the timebox.

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

## Sponsor / architecture delta

- ENS remains the primary, load-bearing sponsor.
- Privy is now a strong secondary candidate if a real financial flow is implemented; embedded wallet/account abstraction alone is not treated as prize qualification.
- World is now a strong secondary candidate if Selfie Check materially gates credential issuance/activation; it must not be described as legal KYC.
- USDC and EIP-712 patterns can be used without deploying on Arc.
- Existing standards/capabilities identified for reuse rather than reinvention: EIP-712, Android HCE/APDUs, ERC-1271/6492 for later smart-wallet ownership, and Aliro as a post-hackathon commercial interoperability direction.
- The PC bridge is explicitly prototype/development infrastructure, not a permanent product requirement.
- No commercial credential price has been selected; any testnet USDC amount is a demo-flow parameter.

## Event / submission state

- Project submissions are open.
- Submission deadline confirmed by ETHGlobal email: Sunday, September 13 at 12:00 ET / 18:00 CEST.
- The demo video is now an operational priority and should not be left to the final hours.
- If stake recovery matters and no Project Check-in has yet been completed, that remains an administrative item to verify separately.

## Next

1. Execute the secure mobile/NFC challenge-response spike with a strict stop-loss.
2. If PASS, integrate the proof with the existing ENSv2 authorization path.
3. If the spike exceeds the stop-loss, preserve the current verified ENSv2 + physical demo, document the UID security boundary clearly, and shift to deployment/demo/submission hardening.
4. Only after the core is stable, evaluate Privy payment/onboarding and World Selfie Check as secondary sponsor layers.

## Blockers

- Secure wallet/mobile proof of credential control over the physical NFC path is not yet validated.
- World Selfie Check sandbox/access is an external dependency only if World is selected as a secondary sponsor.

## Risks

- Hard deadline and shrinking buffer before Sunday, September 13 at 12:00 ET / 18:00 CEST.
- Scope creep from trying to integrate multiple sponsors before the secure/core flow is stable.
- A static NFC UID is clonable and is suitable only as a prototype identifier, not secure proof of ownership.
- Android HCE/mobile signing can create disproportionate debugging risk; use a stop-loss.
- ENSv2 must remain central and authoritative rather than becoming a cosmetic sponsor integration.
- The ENSv2 credential tokenId/resource can be mutable across unregister; the logical ENS name/label is the stable application reference.
- Demo video, public deployment, README, and submission polish cannot be deferred to the final hours.
- The completed slice is a prototype and does not claim production security.

## Latest validation

The current verified product slice remains:

```text
persistent ENSv2 credential
        +
independent PermissionedResolver access.v1 state
        +
physical NFC path
        ↓
DENY -> ALLOW -> DENY
```

The credential remained REGISTERED with unchanged owner, tokenId, resolver, and registry expiry during the persistent-access validation.

This proves programmable physical authorization backed by real ENSv2 state.

It does not yet prove secure possession/control of the credential over NFC.
