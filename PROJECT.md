# Project

- Goal: build a valid, functional, deployed, and explainable ETHGlobal prototype that connects an ENSv2-managed digital access credential to a physical NFC interaction.
- Problem: owners and operators of physical spaces need a simple way to grant temporary access with authoritative credential state that is open, inspectable, and easy to update.
- Initial user: the owner or operator of a physical space granting temporary access.
- Product direction: an open physical-access credential prototype in which ENSv2 on Sepolia is the authoritative state and an ESP32-S3 with a PN532 bridges that state to a physical NFC interaction.
- Core flow: an operator activates or deactivates access in the browser; the local Node server updates PermissionedResolver `access.v1` on Sepolia; a user presents a fresh wallet-signed proof through Android HCE and the ESP32/PN532; the secure bridge verifies current holder control and reads the same authoritative ENSv2 state to produce an access decision.
- Primary sponsor: ENS / ENSv2.
- MVP: one end-to-end Sepolia vertical slice demonstrating ENSv2-authoritative credential state and a physical ISO14443A NFC interaction through the ESP32-S3 and PN532.
- Verified architecture: browser demo -> local Node server -> ENSv2 PermissionedResolver `access.v1` -> persistent UserRegistry credential; and fresh Node challenge -> ESP32/PN532 -> Android HCE/Privy proof -> Node Gate A verification -> coherent current ENSv2 read -> one serial authorization command -> matching firmware confirmation. Gate E INACTIVE/DENY is physically verified; ACTIVE/ALLOW remains pending.
- Verified lifecycle: the same NFC tag produced `DENY → ALLOW → DENY` while the credential remained REGISTERED with unchanged owner, tokenId, resolver, and registry expiry.
- Future layers: presentation/collectible behavior, dynamic metadata, loyalty, transferability, and programmable benefits may build on the persistent credential, but none is implemented yet.
- Non-goals: production-grade access control, complex infrastructure, multiple superficial sponsor integrations, and unrelated Hermes/Knowledge OS work.
- NFC security boundary: a static NFC UID is acceptable only as a prototype/demo identifier. It is clonable and must not be represented as secure proof of credential ownership or as anti-cloning security.

## Sponsor strategy

- Primary: ENS / ENSv2. ENSv2 remains load-bearing and authoritative.
- Strong secondary candidate: Privy, only if implemented as a real product layer. The intended fit is embedded-wallet onboarding plus a real USDC financial flow for credential issuance; the same signer may also become the cryptographic access signer.
- Strong secondary candidate: World, only if Selfie Check materially gates credential issuance or activation as a liveness/eligibility/continuity signal.
- No secondary sponsor should be added if it weakens the ENS core, the demo, or submission readiness.
- Arc is not required merely to use USDC or EIP-712 patterns.
- Ledger, Chainlink, The Graph, Bazantic, and other sponsors remain off the critical path unless a later, already-complete core makes a genuinely load-bearing integration realistic.

## Security upgrade direction

The fresh cryptographic holder-proof architecture and physical Gate E INACTIVE/DENY path are verified. Explicit renew-inactive tooling and deterministic Gate E timing, serial, and coherent-snapshot bounds are implemented and locally validated; no live guest renewal has been executed. The immediate objective is a read-only `guest-001` renewal preflight, followed only with explicit Control Tower authorization by at most one resolver `setData` write. Physical ACTIVE/ALLOW remains the next unproven Gate E result.

Implemented local composition:

```text
door/verifier creates fresh challenge
  -> wallet/mobile signer signs EIP-712 typed data
  -> signature travels over the NFC/mobile presentation path
  -> verifier checks the signer
  -> verifier checks authoritative ENSv2 access state
  -> ALLOW / DENY
```

Start with a simple embedded EOA signer before making an ERC-4337 smart wallet the credential owner. If smart-wallet ownership is added later, use standard ERC-1271/6492 verification rather than custom signature rules.

## Planned product layers

High-value layers already identified, but not yet implemented:

- Further Privy onboarding/product UX beyond the implemented embedded-wallet signer.
- Real USDC credential-issuance payment flow.
- Physical validation of the remaining Gate E ACTIVE/ALLOW path.
- Dynamic credential branding/metadata.
- Credential transferability.
- World Selfie Check before issuance/activation.
- ERC-4337 smart-wallet UX after the simpler signer path works.
- Loyalty, benefits, and consumable promotions built on the persistent credential model.

Post-hackathon/commercial direction:

- Aliro-compatible interoperability rather than inventing a new commercial lock standard.
- Matter/commercial-lock integration only when justified by product requirements.
- iOS contactless/HCE exploration after the Android/mobile proof is stable.

## Pricing note

No commercial credential price has been chosen.

Any testnet USDC amount used during ETHOnline is a demo/payment-flow parameter, not a production pricing decision.

