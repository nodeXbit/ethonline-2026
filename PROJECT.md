# Project

- Goal: build a valid, functional, deployed, and explainable ETHGlobal prototype that connects an ENSv2-managed digital access credential to a physical NFC interaction.
- Problem: owners and operators of physical spaces need a simple way to grant temporary access with authoritative credential state that is open, inspectable, and easy to update.
- Initial user: the owner or operator of a physical space granting temporary access.
- Product direction: an open physical-access credential prototype in which ENSv2 on Sepolia is the authoritative state and an ESP32-S3 with a PN532 bridges that state to a physical NFC interaction.
- Core flow: an operator activates or deactivates access in the browser; the local Node server updates PermissionedResolver `access.v1` on Sepolia; a user presents a fresh wallet-signed proof through Android HCE and the ESP32/PN532; the secure bridge verifies current holder control and reads the same authoritative ENSv2 state to produce an access decision.
- Primary sponsor: ENS / ENSv2.
- MVP: one end-to-end Sepolia vertical slice demonstrating ENSv2-authoritative credential state and a physical ISO14443A NFC interaction through the ESP32-S3 and PN532.
- Verified architecture: browser demo -> local Node server -> ENSv2 PermissionedResolver `access.v1` -> persistent UserRegistry credential; and fresh Node challenge -> ESP32/PN532 -> Android HCE/Privy proof -> Node Gate A verification -> coherent current ENSv2 read -> one serial authorization command -> matching firmware confirmation. Android is the primary issuer/holder product surface: Privy handles authentication/signing/writes while a separate allowlisted public Sepolia client handles blockchain reads and recoverable transaction reconciliation. The dedicated Android issuer `0xFa90e8301A22833B74378C5fA3a7c120Ac512685` controls the live isolated `keys.demo-access.eth` namespace through R1/S1 and has physically issued `staff-001.keys.demo-access.eth`; the holder validated it in My Keys. Gate E remains the physically verified reference verifier path for both INACTIVE/DENY and ACTIVE/ALLOW.
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

The fresh cryptographic holder-proof architecture and physical Gate E INACTIVE/DENY and ACTIVE/ALLOW paths are verified. `guest-001.demo-access.eth` remains the canonical secure physical fallback, with validity renewed through 2026-10-31 23:59:59 Europe/Madrid and last observed ACTIVE/ALLOW policy. Mobile issuer admission is physically verified for dedicated issuer `0xFa90e8301A22833B74378C5fA3a7c120Ac512685`, including M1 restart recovery through the public read-only RPC boundary. Its isolated `keys.demo-access.eth` namespace is live through R1 `0x0AeB395be893149c0b60D9DAC3Ba55139D0dcA1a` and S1 `0x20766FB21498a99350F922ee3163F5a95F354a7f`. The issuer physically created `staff-001.keys.demo-access.eth` in two confirmed transactions, and the holder validated the current credential in My Keys.

The local demo now provides read-only preflight and boot observation, automatic CH343 discovery, sanitized advisory attempt evidence, and distinct ENSv2 POLICY / HOLDER VERIFIER / PHYSICAL CONTROLLER displays. Policy ALLOW never implies confirmed physical access. The loopback-only server and existing transaction safeguards remain the security boundary; `DEMO_RUNBOOK.md` defines cold boot and controlled recovery. Final desktop visual validation passed at 1366x900 normal zoom.

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

- Automatic discovery of active-wallet credentials within the known product R1 namespace, using bounded event scanning only for candidates and fresh authoritative ownership/state readback for display.
- Stacked wallet-style pass presentation, with a selected pass becoming the future NFC-active credential.
- Real artwork rendering, startup Privy session hydration state, and configurable issuance templates.
- Further Privy onboarding/product UX beyond the implemented embedded-wallet signer.
- Real USDC credential-issuance payment flow.
- Controlled Gate E repeatability/revocation rehearsal using the completed pre-demo health check and finalized local UI.
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

