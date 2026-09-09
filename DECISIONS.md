# Decisions

Accepted product and architecture decisions for ETHOnline 2026.

`STATUS.md` is authoritative for current implementation state. A decision listed here does not mean every planned layer is already implemented.

---

## D-001 — ENSv2 is the core credential layer

**Status:** Accepted
**Date:** 2026-09-09

### Decision

Use ENSv2 as the primary, load-bearing protocol for the programmable access credential.

### Why

The project already demonstrates a persistent tokenized ENSv2 subname, PermissionedResolver state, browser-controlled writes, and physical `DENY -> ALLOW -> DENY` behavior on Sepolia.

### Consequences

- ENS remains the primary sponsor.
- ENSv2 must remain authoritative, not cosmetic.
- Do not create a separate ERC-721 merely to make the credential look like an NFT.

### Revisit when

Only if ENSv2 becomes technically non-viable for the submission.

---

## D-002 — Persistent credential ownership and physical access are separate state

**Status:** Accepted
**Date:** 2026-09-09

### Decision

Keep credential ownership persistent while physical authorization changes independently through `access.v1`.

### Why

The verified lifecycle already proves that the credential can remain REGISTERED while access moves `DENY -> ALLOW -> DENY`.

This also leaves room for future history, loyalty, benefits, or other credential capabilities after a single access period ends.

### Consequences

Revoking physical access does not require unregistering or burning the credential.

### Revisit when

A future product requirement proves that strictly ephemeral credentials are preferable.

---

## D-003 — Static NFC UID is prototype-only

**Status:** Accepted
**Date:** 2026-09-09

### Decision

Never present a static NFC UID as secure proof of ENS credential ownership.

### Why

A static UID is clonable/replayable. It proves the hardware path works, not that the presenter controls the wallet/credential.

### Consequences

The current UID demo must retain an explicit security disclaimer until cryptographic holder proof is implemented.

### Revisit when

Do not revisit for a production-security claim.

---

## D-004 — Secure physical access should use fresh challenge-response

**Status:** Accepted
**Date:** 2026-09-09

### Decision

Target a fresh per-request challenge signed by the credential holder, using EIP-712 typed data unless an equivalent existing standard is required by the mobile credential layer.

### Why

A fresh nonce plus resource/time context binds the proof to one access attempt and prevents a static identifier from acting as the credential.

### Consequences

Preferred flow:

```text
verifier creates fresh challenge
  -> wallet/mobile signer signs
  -> verifier validates signature
  -> verifier checks ENSv2 authorization
  -> ALLOW / DENY
```

Replay resistance is part of the verifier design.

### Revisit when

A target platform requires another secure standard with equivalent guarantees.

---

## D-005 — Use the simplest signer first

**Status:** Accepted
**Date:** 2026-09-09

### Decision

For the first secure holder-proof vertical slice, use a simple embedded EOA/mobile signer before making an ERC-4337 smart wallet the ENS credential owner.

### Why

This is the shortest path to proving:

```text
wallet -> signature -> NFC/mobile transport -> verifier -> ENS
```

Smart-wallet ownership adds ERC-1271/6492 and more failure modes before the core proof exists.

### Consequences

- First prove EIP-712 + ECDSA holder verification.
- Add ERC-4337 later for batching, gas sponsorship, or UX if it materially improves the product.
- If a smart-contract wallet later owns the credential, use ERC-1271/6492 instead of inventing custom verification.

### Revisit when

The cryptographic access path already works or a concrete requirement makes smart-wallet ownership necessary.

---

## D-006 — USDC payment does not require Arc

**Status:** Accepted
**Date:** 2026-09-09

### Decision

If a real payment flow is implemented, keep it on the existing Sepolia-centered product path unless another network is technically required.

Do not add Arc solely to use USDC or EIP-712 patterns.

### Why

One-network architecture reduces integration and demo complexity.

### Consequences

- A testnet USDC payment can be used as the financial flow before/with credential issuance.
- No commercial credential price is decided.
- Any hackathon amount is a demo parameter, not a production price.
- More advanced payment-signature patterns are optional optimizations.

### Revisit when

A concrete sponsor/product requirement justifies another chain.

---

## D-007 — Secondary sponsors must improve the same product

**Status:** Accepted
**Date:** 2026-09-09

### Decision

Keep ENS primary. Treat Privy and World as strong secondary candidates only if their integrations are functional, load-bearing, and do not threaten completion.

### Why

They can improve one coherent flow:

- Privy: onboarding/wallet plus a real financial flow and potentially the access signer.
- World: liveness/eligibility/continuity before credential issuance or activation.

Other sponsor branches currently create more scope than product value.

### Consequences

Do not add Ledger, Chainlink, The Graph, Arc, Bazantic, or another sponsor merely to chase a prize.

### Revisit when

The core is submission-ready and verified time remains.

---

## D-008 — World Selfie Check is not legal KYC

**Status:** Accepted
**Date:** 2026-09-09

### Decision

If World is integrated, describe Selfie Check as a liveness/eligibility/continuity or abuse-prevention signal.

Do not claim it automatically performs legal hotel KYC or replaces legally required identity procedures.

### Why

The available feature and prize criteria do not justify that broader compliance claim.

### Consequences

A coherent flow is:

```text
request credential
  -> Selfie Check
  -> pass/fail materially gates issuance or activation
```

### Revisit when

A separate document-backed identity/compliance product is actually implemented and validated.

---

## D-009 — Reuse existing standards instead of rebuilding infrastructure

**Status:** Accepted
**Date:** 2026-09-09

### Decision

Prefer existing standards/platform capabilities:

- EIP-712 for typed signatures.
- Android HCE/APDUs for the mobile NFC prototype.
- ERC-1271/6492 if smart-contract wallets later own credentials.
- Aliro as a post-hackathon commercial interoperability direction.

### Why

The product value is the open programmable credential/authorization layer, not inventing new wallet, NFC, or commercial-lock standards.

### Consequences

Custom protocol work should be limited to the smallest demo-specific layer required for ETHOnline.

### Revisit when

An existing standard cannot satisfy a proven requirement.

---

## D-010 — ESP32 is the access controller; the PC bridge is prototype infrastructure

**Status:** Accepted
**Date:** 2026-09-09

### Decision

Keep the ESP32/PN532 path as the programmable physical verifier/controller. Treat the current PC bridge as development/demo infrastructure, not a permanent product requirement.

### Why

NFC utility apps can read/write compatible tags, but they do not replace an autonomous controller that generates challenges, evaluates authorization, and actuates a physical output.

The PC currently accelerates debugging and RPC/onchain access; the ESP32 can later communicate through embedded networking, a verifier service, local verification, or a commercial access standard.

### Consequences

Architecture/demo documentation must label the PC bridge as prototype infrastructure.

### Revisit when

Designing post-hackathon production hardware.

---

## D-011 — Price is not decided by the hackathon demo

**Status:** Accepted
**Date:** 2026-09-09

### Decision

Do not treat example values such as `5 USDC` as the commercial price of a credential.

### Why

The real business model and costs are not yet validated. A credential may also represent more than a one-time door key.

### Consequences

Any testnet USDC amount exists to demonstrate a real financial flow, not to establish product pricing.

### Revisit when

Post-hackathon product economics are researched with real deployment costs and customers.
