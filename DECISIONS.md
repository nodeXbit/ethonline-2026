# Decisions

Accepted product and architecture decisions for ETHOnline 2026.

## D-025 - Preserve Studio transaction identity across asynchronous work

Accepted 2026-09-12. Studio uses a process-scoped wallet/provider generation and write lease through final confirmation. Durable provider-invocation markers distinguish proven-unsent preparations from ambiguous submissions; late hashes remain journaled after Activity cancellation. Recovery reconciles exact wallet, credential, session, calldata, transaction and receipt identities. A different pass cannot replace an unfinished preparation implicitly. Final authority and time checks run again before submission, and confirmed management work remains blocked until authoritative readback succeeds.

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

---

## D-012 — Holder-proof v1 uses server-issued, one-shot EIP-712 challenges

**Status:** Accepted
**Date:** 2026-09-09

### Decision

Use the `ENSv2 Access` version `1` EIP-712 domain on Sepolia (`chainId` 11155111) with this challenge:

```text
AccessChallenge(
  bytes32 credential,
  bytes32 resource,
  bytes32 nonce,
  uint64 expiresAt
)
```

`credential` is the canonical ENS namehash. `resource` binds the proof to one application-defined door/resource. The verifier generates an unpredictable nonce, applies a short expiry, and accepts only challenges present in its issued-challenge store.

At verification time, recover the signer from canonical server-stored typed data and require it to equal the current ENSv2 credential owner. After a valid current-holder proof, consume the challenge exactly once before evaluating the existing ENS access policy. The challenge remains consumed even when that policy returns DENY.

For the current hackathon Node verifier, an in-memory `PENDING` / `CONSUMED` store is sufficient.

### Why

Server issuance prevents holders from inventing challenges. Credential, resource, expiry, and one-shot nonce binding prevent cross-credential, cross-door, stale, and replayed proofs. Consuming before the policy result prevents a captured proof denied under inactive access from becoming valid after access is activated.

Authentication and authorization remain separate: EIP-712 proves wallet control, while current ENSv2 ownership and the existing `access.v1` policy remain the source of truth.

### Consequences

- Static NFC UID must not be an authorization factor in the future secure path.
- Ownership transfer invalidates signatures from the former owner.
- Persisted/distributed challenge storage is deferred until the verifier requires multiple processes or restart survival.
- Android, Privy signing, HCE, and APDU transport are not implemented by this decision or by Gate A.

### Revisit when

The verifier requires durable or distributed replay protection, or a later wallet type requires standard contract-wallet verification.

---

## D-011 — Privy Android embedded EOA is the first mobile signer

**Status:** Accepted
**Date:** 2026-09-10

### Decision

Use the native Privy Android embedded EOA as the first mobile holder-proof signer. The currently proven artifact is `io.privy:privy-core:0.14.0`, whose native provider empirically supports `eth_signTypedData_v4` in the tested real-device flow.

The Gate A `ENSv2 Access` EIP-712 schema remains canonical; mobile implementations must conform to it, and HCE transport must not change its cryptographic semantics.

### Why

Real-device email OTP authentication, embedded EOA creation/reuse, exact typed-data signing, and independent Node/viem recovery succeeded with the recovered signer equal to the Privy public wallet.

### Consequences

- App Secret must never enter mobile/client code.
- Real App ID and App Client ID remain ignored local configuration for the current spike.
- The Solana Seeker is only a generic Android test device; Seed Vault and the Solana stack are outside the current architecture.
- Wired ADB failure is not a reason to make ADB an application dependency. Gate C may use manual APK installation again if needed.
- Gate C is Android/HCE-first. Define and test deterministic APDU behavior before changing PN532 firmware.
- Static NFC UID remains excluded from secure authorization.
- This decision proves signing interoperability, not NFC/HCE transport, ENS credential ownership by the Privy wallet, payment, or Privy prize qualification.

### Revisit when

A later wallet model requires ERC-1271/6492, or a proven product requirement changes the mobile signer.

---

## D-013 — Gate C APDU v1 separates HCE transport from signing

**Status:** Accepted
**Date:** 2026-09-10

### Decision

Fix Gate C APDU v1 for the current prototype around the proprietary, non-payment AID `F0454E5356324331` (`F0` + ASCII `ENSV2C1`, category `other`).

Transport the Gate A challenge without JSON in this exact 104-byte wire format:

```text
credential[32] || resource[32] || nonce[32] || expiresAt[8 unsigned big-endian]
```

The proof wire payload is exactly 65 signature bytes. `ProofProvider` separates HCE transport/state from the signing implementation, and Gate C2 must preserve this APDU contract.

A READY proof may be reread during the same selected transport session so an interrupted GET_SIGNATURE can be retried. A new challenge, SELECT, or HCE deactivation clears it.

### Why

The smallest deterministic protocol is sufficient to transport the existing Gate A-compatible holder proof without duplicating EIP-712 semantics or coupling Android NFC callbacks to Privy. Session generation binding prevents stale asynchronous signing completion from exposing a proof for a newer challenge.

Gate A remains authoritative for cryptographic one-shot replay protection; HCE is only the transport layer.

### Consequences

- Production Gate C1 contains no fake signing and remains PROCESSING until a real provider is connected.
- Gate C2 replaces the pending provider with the already-proven Privy EIP-712 signer without changing APDU v1.
- Static NFC UID remains outside authorization.
- PN532 firmware and physical phone-to-reader validation are deferred until Gate C2 passes.
- Gate C1 does not claim real NFC interoperability or physical cryptographic authorization.

### Revisit when

Only if physical interoperability proves an APDU-level incompatibility that cannot be resolved while preserving the current contract.

---

## D-014 — Gate C2 uses one application-scoped Privy HCE signer

**Status:** Accepted
**Date:** 2026-09-10

### Decision

Use one application-scoped Privy SDK instance and one shared `HceApduProcessor` for both the Android activity harness and `HostApduService`. `PrivyProofProvider` is the production HCE signer.

The user must authenticate and explicitly create or reuse an embedded Ethereum wallet before holder-proof use. The HCE service reads an existing SDK-managed session/wallet and never creates a wallet.

Gate A remains the canonical EIP-712 definition, and Gate C APDU v1 remains frozen. Signing is asynchronous:

```text
SEND_CHALLENGE -> PROCESSING -> READY / ERROR
```

C1 generation/session invalidation remains authoritative for stale completions after a newer challenge, SELECT/reset, or HCE deactivation. Only an exact 65-byte signature can enter READY.

The wallet address is not transported over NFC because the verifier recovers the signer from the proof. The manual in-app APDU harness is validation infrastructure using the production processor/provider, not a separate authorization path.

### Why

The real Seeker validation proved that the production HCE path can asynchronously invoke Privy `eth_signTypedData_v4` and return an exact 65-byte Gate A-compatible proof whose Node-recovered signer matches the Privy wallet.

Keeping session ownership application-scoped avoids competing Privy instances or duplicated login state. Keeping signing behind `ProofProvider` preserves the deterministic NFC transport contract.

### Consequences

- No OTP, auth token, private key, or App Secret is manually persisted or exposed.
- Static NFC UID remains excluded from secure authorization.
- Real phone-to-PN532 interoperability remains unproven until Gate D.
- Gate C2 does not prove physical cryptographic access, ENS credential ownership by the Privy wallet, payment, Privy prize qualification, World, or ERC-4337.
- Gate D must preserve APDU v1 and initially prove transport only, without blockchain/ENS authorization.

### Revisit when

Only if Android process behavior or physical reader interoperability demonstrates that the shared application-scoped architecture cannot preserve the frozen APDU contract.

---

## D-015 â€” Gate D keeps the ESP32 as one-shot NFC/APDU transport

**Status:** Accepted
**Date:** 2026-09-10

### Decision

Use the installed Elechouse-compatible `PN532` / `PN532_I2C` stack for Gate D and keep Gate C APDU v1 frozen. The validated 104-byte challenge and 65-byte proof fit the physical transport, so signature chunking is not required for this prototype.

The ESP32 remains NFC/APDU transport only at this stage. Privy signing stays on Android, and Node remains the future verifier and onchain reader. NFC UID is excluded from secure authorization.

Gate D firmware is deliberately one-shot and requires reset before another signing session, preventing accidental repeated signatures while a phone remains on the antenna.

### Why

Two complete ESP32-S3 + PN532 + Seeker ISO-DEP sessions transported a real Privy-produced 65-byte Gate A-compatible signature, and Node recovered the same embedded wallet. The unchanged implementation successfully carried the full 67-byte response twice without chunking.

One earlier GET_SIGNATURE failure was transient and not reproduced. A speculative PN532/HAL workaround is not accepted without a reproducible failure and stronger low-level evidence.

### Consequences

- Preserve APDU v1 and the installed PN532 libraries.
- Do not add chunking for the current challenge/proof sizes.
- Keep ENS/RPC/blockchain authorization outside the ESP32 transport layer for Gate D.
- Gate D proves physical proof transport, not ENS-based physical authorization.
- Gate E must compose a fresh verifier-issued Gate A challenge with current ENSv2 ownership and `access.v1` authorization.

### Revisit when

A reproducible physical transport limitation produces specific evidence that the current contract cannot be carried safely.

---

## D-016 — Gate E composes holder proof and current ENS authorization without changing transport semantics

**Status:** Accepted
**Date:** 2026-09-10

### Decision

Use a separate credential for Gate E live validation: `guest-001.demo-access.eth`, planned owner `0x3419148731087b970d2059C53780163B452D5FF7`. Keep `cred-001.demo-access.eth` unchanged as the prior validated demo/reference credential.

Use `demo-access.eth:door-001` as the physical resource identifier and derive its bytes32 value deterministically with `keccak256`.

Node remains the challenge issuer, in-memory challenge store, Gate A verifier, and current ENS reader. The ESP32 remains NFC/APDU plus line-oriented serial transport only. It has no ENS, RPC, wallet, key, or UID authorization responsibility.

Issue the Gate A challenge only after ordered `TARGET_ACTIVATION: PASS`, `SELECT: PASS`, and `WAITING_CHALLENGE` messages. Gate A remains the source of truth for challenge issuance, signature recovery, consumption, and replay semantics. Existing `readCredential` remains the coherent current ENS snapshot, and existing `isAuthorized` remains the only ENS access policy.

Gate E firmware is one-shot per reset/session. Replay validation submits the exact consumed proof to the same Node store without new NFC work or a new challenge. No Android production or APDU v1 change is required.

### Why

Authentication, transport, and authorization stay separate: NFC transports the proof, Gate A proves current wallet control, and ENSv2 determines current credential ownership and access. Delaying issuance preserves the short challenge TTL, while one-shot consumption prevents later activation from making a captured denied proof valid.

### Consequences

- NFC UID has no role in secure authorization.
- A valid current-holder proof is consumed before the later ENS policy result, including INACTIVE/DENY.
- Wrong signer, malformed proof, timeout, and ENS read failure fail closed.
- Credential label/owner parameters default to `cred-001` and the DEV account when omitted, preserving the established flow.
- Provisioning `guest-001` does not transfer, unregister, or modify `cred-001`.
- Live provisioning requires separate explicit authorization after this code checkpoint.
- Local implementation success must not be described as live secure ENS authorization success.

### Revisit when

Only if live physical validation exposes a concrete transport or coherent-read defect that cannot be addressed within the frozen APDU v1 and Gate A semantics.

---

## D-017 — Require physical-controller confirmation before Gate E serial finalization

**Status:** Accepted
**Date:** 2026-09-10

### Decision

After Node computes a Gate E decision, send exactly one `AUTHORIZATION=ALLOW` or `AUTHORIZATION=DENY` command and keep the serial transport attached until the firmware emits the exact matching `AUTHORIZATION: ALLOW` or `AUTHORIZATION: DENY` terminal line.

Use a bounded two-second local serial timeout. A contradictory terminal line is a protocol mismatch and fails closed. A timeout reports that the command was sent but confirmation was not observed; it must never be interpreted as ALLOW. Unrelated or premature serial lines do not satisfy confirmation.

Do not resend authorization or repeat challenge issuance, NFC transport, signing, proof verification, replay verification, or ENS reads while awaiting this delivery confirmation.

### Why

The first successful physical Gate E INACTIVE authorization proved the entire cryptographic and ENS DENY path, and Node sent `AUTHORIZATION=DENY`, but Node closed COM4 before reliably capturing the firmware's existing `AUTHORIZATION: DENY` line. A subsequent controlled run with the corrected lifecycle captured that matching line before close and preserved same-proof replay denial.

### Consequences

- Gate E INACTIVE physical authorization is proven end to end, including controller receipt: PASS.
- Gate E ACTIVE physical authorization was still pending at this checkpoint; D-019 records its later end-to-end PASS.
- Firmware, Android, APDU v1, Gate A, and ENS authorization semantics remain unchanged.
- `guest-001` remained REGISTERED and INACTIVE during this validation; the run used zero blockchain writes and did not use NFC UID. D-019 records the later authorized renewal, activation, and ACTIVE physical validation.

### Revisit when

Measured serial behavior requires a different bounded timeout, or a documented controller protocol replaces the current line confirmation. Do not weaken exact-match or fail-closed behavior.

---

## D-018 — Gate E deterministic readiness uses bounded, coherent attempts

**Status:** Accepted
**Date:** 2026-09-10

### Decision

Keep the Gate A cryptographic challenge TTL at 60 seconds and bound every Gate E attempt to 50 seconds beginning immediately after challenge issuance. Within that lifetime, allow at most eight seconds for the complete ENS verification and two seconds for firmware confirmation. Accept a pinned block at most 60 seconds old and at most 15 seconds in the future.

Bound serial input to 1,024 bytes before newline and poison the current attempt permanently on overflow. Treat firmware STOP as immediate, public terminal telemetry containing only stage/reason information. Do not redesign APDU v1, session IDs, or the PN532 protocol.

Make validity extension an explicit renew-inactive operation. It must preserve `active=false`, refuse ACTIVE credentials, and be a semantic no-op when the existing deadline is sufficient. Activation and deactivation semantics remain unchanged.

An optional RPC fallback may retry only by restarting the entire pinned-block `readCredential` snapshot on the fallback client. Do not use transparent per-call multi-provider fallback or combine owner, resolver, and policy values from different providers.

### Why

The cryptographic challenge must outlive the deliberately shorter physical attempt, while every async result and controller command remains subject to one terminal deadline. A complete snapshot belongs to one provider/client and one pinned block; partial provider recovery would make the authorization evidence incoherent.

Explicit renewal prevents an administrative validity change from being coupled accidentally to ordinary access-state controls. Bounded serial memory and immediate public STOP telemetry keep firmware failures deterministic and fail closed.

### Consequences

- Late proof, late ENS completion, or a computed ALLOW that reaches the deadline cannot authorize or be sent to firmware.
- A failed primary snapshot is discarded in full before the optional fallback begins.
- Read-only renewal preflight may read and simulate but cannot broadcast or enter post-broadcast recovery.
- Gate E INACTIVE physical was PASS and ACTIVE remained pending at this checkpoint; D-019 records the later ACTIVE end-to-end PASS without changing these bounds.

### Revisit when

Measured end-to-end behavior provides evidence that a bound must change without weakening fail-closed, whole-snapshot, or one-shot semantics.

---

## D-019 — Close Gate E secure authorization and move to demo reliability

**Status:** Accepted
**Date:** 2026-09-10

### Decision

Treat Gate E secure authorization as technically closed after physical validation of valid-holder INACTIVE/DENY, valid-holder ACTIVE/ALLOW, exact controller confirmation, and consumed-proof replay DENY.

Use `guest-001.demo-access.eth` as the canonical secure demo credential. Preserve its owner `0x3419148731087b970d2059C53780163B452D5FF7`, expected resolver, and renewed validity horizon of 2026-10-31 23:59:59 Europe/Madrid. Keep static NFC UID excluded from authorization.

Retain all Batch A timing, freshness, public firmware STOP, proof-redaction, one-shot, controller-confirmation, and replay-rejection requirements. Require a full cold boot before a physical Gate E session using the established CH343/PN532 readiness sequence. This is an operational reliability measure and not a claim about the root cause of intermittent hardware failures.

Do not redesign APDU v1, adopt PC/SC, change the installed PN532 library, or expand the secure protocol without new concrete evidence. The next work is Batch B — Demo Reliability.

### Why

One controlled physical ACTIVE attempt transported a fresh 104-byte challenge and redacted 65-byte Privy proof through Android HCE / ISO-DEP / PN532 / ESP32. Gate A recovered the current ENS owner, consumed the challenge, and a fresh coherent PRIMARY-provider ENSv2 snapshot returned `VERIFIER_ALLOW`. Node sent `AUTHORIZATION=ALLOW` once, firmware later emitted the exact matching `AUTHORIZATION: ALLOW`, and the same consumed proof was rejected as `REPLAYED_CHALLENGE` without another physical or chain operation.

### Consequences

- Gate E secure path: PASS. Batch A: PASS.
- Physical success continues to require exact controller confirmation; Node's policy result alone is insufficient.
- The prototype trusts its local Node/USB/controller environment and makes no relay-resistance or production hardware-security claim.
- No physical lock or relay actuator is part of the validated path.
- Intermittent PN532/I2C/ISO-DEP failures remain an operational risk; controlled setup and full cold boot are the current mitigation.
- The successful ACTIVE run used PRIMARY RPC with no fallback configured.
- Demo reliability, outcome clarity, runbook quality, and controlled rehearsal now take priority over secure-protocol expansion.

### Revisit when

New reproducible evidence requires a security or transport change, or a production controller/actuator replaces the current prototype boundary.

---

## D-020 — Close Batch B with separate policy, verifier and controller evidence

**Status:** Accepted
**Date:** 2026-09-10

### Decision

Use `guest-001.demo-access.eth` as the only canonical secure demo credential. Preserve generic `cred-001` reference behavior. The UI must separate ENSv2 POLICY, HOLDER VERIFIER and PHYSICAL CONTROLLER; verifier ALLOW never implies physical success without matching controller confirmation. Transport failures and replay results remain distinct from policy decisions.

Runtime evidence is local, advisory and non-authoritative. Atomically replaced, ignored JSON reports expose only sanitized metadata, never proof/signature bytes, challenge nonces, typed data, secrets or RPC URLs. New attempts replace old success with IN_PROGRESS. Reporting failures do not change authorization.

Keep the loopback-only server as the local security boundary, preserving exact mutation Host/Origin checks, server-side signing, existing safe transaction paths and no CORS. Keep vanilla HTML/CSS/JS and local files: no database, event bus, new service or framework migration.

Full cold boot remains the pre-session operational rule in `DEMO_RUNBOOK.md`. Passive boot observation cannot certify a phone session or replace missing hardware evidence; those checks remain explicit and manual where necessary.

### Why

The demo must make an onchain permission visibly different from proof of holder control and physical controller confirmation. The final browser review passed at 1366x900 normal zoom with all three cards visible together and no physical success implied by ACTIVE/POLICY ALLOW.

### Consequences

- BATCH B — DEMO RELIABILITY: PASS. Existing Gate E security and APDU v1 remain unchanged.
- LIVE DEMO REHEARSAL is next; it has not yet been executed with the finalized Batch B UX.
- No new dependencies, public deployment or speculative infrastructure are needed for this checkpoint.
- Batch B is closed unless rehearsal discovers a real blocker.

### Revisit when

A concrete rehearsal regression or security issue requires a scoped correction. Do not reopen visual polish or protocol design without such evidence.

---

## D-021 — Separate Android wallet writes from public blockchain reads

**Status:** Accepted
**Date:** 2026-09-11

### Decision

Treat Android as the primary credential-wallet and issuer surface. Keep Privy responsible for authentication, the embedded wallet, `switchChain`, signing and wallet writes. Route Android blockchain reads through a separate credential-free HTTPS Sepolia client with an explicit read-method allowlist, bounded timeouts and response size, and sanitized errors.

Use an operation-scoped persistent transaction journal for mobile writes. Persist `SUBMITTING_NO_HASH` before calling the wallet provider, persist a returned transaction hash immediately, never blindly retry after an ambiguous result, and permit re-arm only after unchanged latest/pending nonces prove no broadcast. Existing hashes must reconcile on restart without creating a new operation.

### Why

Privy Android 0.14.0 successfully signed and broadcast the physical M1 transaction but its generic response model could not deserialize object-valued transaction and receipt results. The separate public read client reconciled the existing hash to `CONFIRMED` on the physical device without another submission.

### Consequences

- Mobile issuer admission is closed: transaction transport, persistent recovery and read-only reconciliation are PASS.
- M1 transaction `0x6c4f42f2d368936d4aaf7edf3e0395c376f92b699563b34fee4ed053a0a53e32` is confirmed in block `11683226`; issuer nonce advanced `0 -> 1`.
- The read client cannot sign or expose `eth_sendTransaction` / `eth_sendRawTransaction`; wallet writes remain behind Privy and explicit human review.
- No operator RPC credential is compiled into Android.
- The existing `guest-001` physical fallback remains untouched.
- The next architecture target is the isolated issuer namespace `keys.demo-access.eth` with R1 and S1. This decision does not authorize that work.

### Revisit when

A production RPC strategy, multi-chain requirement, or smart-wallet architecture requires a different read boundary without weakening transaction recovery guarantees.

---

## D-022 — Isolate mobile-issued credentials under an issuer-controlled ENSv2 namespace

**Status:** Accepted
**Date:** 2026-09-11

### Decision

Use the live `keys.demo-access.eth` branch for credentials issued by the dedicated Android Privy issuer `0xFa90e8301A22833B74378C5fA3a7c120Ac512685`. Its R1 UserRegistry is `0x0AeB395be893149c0b60D9DAC3Ba55139D0dcA1a`; its S1 PermissionedResolver is `0x20766FB21498a99350F922ee3163F5a95F354a7f`. Both are deterministic proxies deployed through the verified factory and controlled at root by the issuer, not DEV.

Grant the issuer R1 root REGISTRAR/RENEW roles and their admin counterparts, and S1 root SET_TEXT/SET_DATA roles and their admin counterparts. On the `keys` entry inside R0, grant the issuer only ROLE_RENEW. Preserve `demo-access.eth -> R0`, S0, `guest-001`, and `cred-001` as the established fallback branch.

Treat `config/issuer-space.json` as the public authoritative bootstrap configuration. The next implementation target is one complete `staff-001.keys.demo-access.eth` credential vertical. This decision does not create that credential or authorize any additional chain write.

### Why

The isolated branch separates issuer administration from the legacy operator-controlled demo path while allowing the Android product surface to register credentials and manage narrowly scoped text/data records. DEV was needed only to deploy and attach the namespace because it controls R0; it received no root authority in R1 or S1.

### Consequences

- R1 deployment transaction `0x632c6d6f9c04a381f763013bf304d30c15bcccc440b41df9350829cf6e952253` confirmed in block `11683691`.
- S1 deployment transaction `0x2e6e8f830527df1aa384fe09c0a51f27c8e8d7e2b0b3862a9c646aa29d38d94c` confirmed in block `11683692`.
- Namespace registration transaction `0x5997d2a1ccbaf146de00dbddef79c49f00fedefd02fdb2399386910a1840230b` confirmed in block `11683693` with expiry `1814392799`.
- Android with Privy is the intended primary issuer/holder surface; ESP32/PN532 remains the reference physical verifier.
- No staff, visitor, or contractor credential exists yet. No final issuer UI or dynamic physical credential discovery is claimed.

### Revisit when

The first credential vertical has an exact lifecycle, record schema, authorization plan, and verification contract ready for separate approval.

---

## D-023 — Make confirmed mobile issuance recovery idempotent and receipt-aware

**Status:** Accepted
**Date:** 2026-09-12

### Decision

Treat the first Android-issued credential vertical as complete after the physical two-transaction flow and holder validation of `staff-001.keys.demo-access.eth`.

Keep every transaction review pure until its final explicit CTA. Once a registration or configuration operation is confirmed, recovery transitions are idempotent and must never resubmit that write. Treat unset `access.v1` as the expected state after registration and before configuration. Declare issuance `READY` only after a bounded authoritative readback whose snapshot block is at or after the confirmed TX2 receipt block and whose complete credential invariants match.

Make automatic discovery within the known product R1 namespace the next product P1. Event scans provide candidates only; fresh readback determines current ownership and state, results are partitioned by the active wallet, and manual **Add by ENS name** remains a secondary recovery path.

### Why

The physical TX1/TX2 run exposed recovery boundaries that successful happy-path unit tests alone could not prove. Separating intent review, durable operation state, confirmed-receipt recovery, and authoritative post-receipt state prevents accidental re-registration, configuration resend, and premature READY presentation.

### Consequences

- TX1 `0x8858c291f14659ea8e323d1af988f4ee379c2a14dc5e2cbed4db592a04e6db58` confirmed in block `11684957`; TX2 `0x57a5a4bf81fcede947b83bf55dcabe065540a1254fb2d9eb10677d391cf7bb11` confirmed in block `11685150`.
- The holder physically validated the current owned credential in My Keys, and Android recovered to `READY` without another write.
- Automatic discovery, selected-pass NFC behavior, real artwork rendering, startup Privy hydration UI, configurable templates, and stacked pass presentation remain unimplemented.

### Revisit when

The bounded R1 event start block and reorg/rescan policy are specified for automatic discovery, or provider behavior requires stronger receipt-finality handling.

---

## D-024 — Connect selected passes to resource-aware virtual gates next

**Status:** Accepted
**Date:** 2026-09-12

### Decision

Close the Pass Wallet Polish workstream with LockENS branding, global wallet selection, automatic R1 discovery, multi-wallet ownership partitioning, selected-pass persistence, and artwork rendering infrastructure marked PASS.

Treat a real multi-pass physical stack as not yet validated until one wallet owns 2+ real credentials. Keep selected-pass HCE integration explicitly not implemented. The next architecture sequence is `Selected Pass -> Dynamic HCE -> resource-aware challenge -> virtual gate profiles -> ENS policy -> ALLOW / DENY`.

Model Front Door, Lab, and Server Room as distinct logical resources that may share the same physical ESP32/PN532 rig. This decision records the intended resource model but does not implement profiles, protocol changes, or policy changes.

### Why

The physical product review passed the single-pass wallet experience and preserved existing HCE/NFC behavior. Binding the persisted product selection to physical presentation requires an explicit resource context and a separately reviewed protocol boundary.

### Consequences

- Pass Wallet Polish, LockENS branding, global wallet selection, automatic discovery, multi-wallet behavior, selected-pass persistence, and artwork infrastructure are checkpointed as PASS.
- No HCE, Node, firmware, blockchain-write, or transaction-semantic change is part of this checkpoint.
- Dynamic HCE and resource-aware virtual gates remain future work requiring separate implementation and validation.

### Revisit when

The selected-pass-to-HCE contract and resource identifier are specified, or 2+ real owned credentials are available for physical stack validation.
