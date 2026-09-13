# Project

## Current LockENS product

LockENS is a Sepolia prototype for ENSv2-authoritative access passes. The final
demo uses one Android holder, three Android NFC gate stands, and one local Node
authoritative verifier. The holder presents a selected pass over HCE/NFC and
signs a fresh proof with a Privy embedded wallet. Node verifies holder control
offchain, reads current ENSv2 registration/ownership and PermissionedResolver
policy, then returns `ALLOW` or `DENY`. Only `ALLOW` opens the virtual gate
animation.

ENSv2 is the load-bearing credential and authorization state. Privy supplies
embedded wallet authentication/signing and approved write transport; it is not
the resource-policy engine. The local Node verifier is part of the final demo,
not a hidden production backend claim.

## Scope and demonstrated outcome

- Seven final passes across four holder wallets are registered/configured on
  Sepolia under the isolated issuer namespace.
- Staff, Visitor, Contractor, and showcase resource-policy outcomes were
  physically validated across Front Door, Lab, and Server Room Android gates.
- Wallet switching, pass discovery, selected-pass HCE, and stale-session
  invalidation were physically validated.
- Issuance and management flows are transaction guarded and evidence-backed.
- The final demo controls a virtual gate UI; no physical lock/relay actuator is
  claimed.

See [FINAL_DEMO_STATE.md](FINAL_DEMO_STATE.md) for public chain/evidence details
and [STATUS.md](STATUS.md) for the current release state.

## Security and trust boundary

Every grant requires a fresh one-use holder proof plus a coherent current ENSv2
snapshot. Registration, current owner, resolver provenance, validity, global
access, and selected resource policy must pass. Transport/RPC/state errors fail
closed. The prototype trusts the local PC, Node process, Android runtime, and
USB/NFC operating environment; it makes no production hardware-security,
relay-resistance, or universal-device claim.

## Alternative hardware route

The ESP32-S3 + PN532 implementation is retained as an experimental alternative
and as historical physical-protocol evidence. It is not the primary final
Android-gate demo. Its documented boot, wiring, I2C, ISO-DEP, and repeatability
limitations remain applicable, and there is no production-readiness claim.

## Feature freeze and non-goals

Feature freeze is active. Release work is limited to audits, privacy/security
remediation, documentation, screenshots/video, and submission materials.

Non-goals for this release include production access control, physical actuation,
offline/autonomous operation, a cloud verifier, new Solidity, additional sponsor
integrations, secure hardware enrollment, universal Android support, and new
wallet or protocol features.

Post-release ideas such as commercial-lock interoperability, richer credential
presentation, or additional account models are not implemented and must not be
read as current capability.
