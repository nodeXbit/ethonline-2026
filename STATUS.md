# Status

## Current objective

Preserve the completed Gate E INACTIVE physical validation and run a read-only renewal preflight for the existing REGISTERED INACTIVE `guest-001` record before seeking explicit Control Tower authorization for at most one resolver `setData` write.

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
- Secure holder-proof Gate A: PASS. Node now issues server-owned EIP-712 challenges, verifies the recovered signer against the current ENSv2 credential owner, consumes a valid holder proof exactly once before evaluating the shared `isAuthorized` policy, and rejects replay even when access later changes from DENY to ALLOW.
- Gate A validation passed 17 holder-proof tests and the complete 79-test repository suite.
- Privy Android signing Gate B: PASS. Native email OTP authentication and embedded Ethereum EOA creation/reuse succeeded on a real Solana Seeker used strictly as a standard Android device.
- Native `eth_signTypedData_v4` is empirically supported with `io.privy:privy-core:0.14.0`. The device signed the exact `ENSv2 Access` EIP-712 fixture, and Node/viem recovered the same public wallet address.
- Gate B validation passed the complete 82-test repository suite and performed zero blockchain writes.
- Android HCE protocol Gate C1: PASS. `HostApduService` is registered under the non-payment proprietary AID `F0454E5356324331`, with deterministic APDU v1 transporting the fixed 104-byte binary `AccessChallenge`.
- Gate C1 implements the explicit `IDLE` / `PROCESSING` / `READY` / `ERROR` session model, an exact 65-byte proof response, session reset behavior, and stale asynchronous completion protection behind a replaceable `ProofProvider`.
- Gate C1 Android JVM tests and debug assembly pass, and the complete Node suite remains green at 82/82.
- Privy HCE signing Gate C2: PASS. One application-scoped Privy instance and shared `HceApduProcessor` now serve both the Android activity harness and `HostApduService` through the production `PrivyProofProvider`.
- Gate C2 reconstructs the exact Gate A `ENSv2 Access` EIP-712 typed data from the binary `HceChallenge`, schedules `eth_signTypedData_v4` asynchronously, and preserves C1 stale-completion invalidation and the exact 65-byte proof contract.
- The production manual APDU harness passed on the Seeker. Privy wallet `0x3419148731087b970d2059C53780163B452D5FF7` produced a 65-byte proof, and Node recovered the same address (`MATCH: PASS`).
- Gate C2 validation passed all 21 Android JVM tests, Android debug assembly, and the complete 83-test Node suite.
- Physical PN532/HCE transport Gate D: PASS. A real ESP32-S3 using the installed Elechouse-compatible `PN532` / `PN532_I2C` stack completed ISO-DEP/APDU exchange with the Seeker `HostApduService`.
- TARGET, SELECT, SEND_CHALLENGE, PROCESSING, READY, and GET_SIGNATURE were physically validated with the frozen APDU v1 contract and deterministic 104-byte Gate C2 transport vector.
- The PN532 retrieved the real 65-byte Privy-produced Gate A-compatible signature, and Node/viem recovered the same embedded wallet `0x3419148731087b970d2059C53780163B452D5FF7` (`MATCH: PASS`).
- Gate D passed 2/2 complete physical sessions. The earlier GET_SIGNATURE failure is classified as transient/not reproduced after the unchanged implementation transported the full 67-byte response twice.
- Gate D requires no chunking. Final validation passed Node 83/83, Android 21/21, Android debug assembly, and firmware compilation.
- Gate D proves physical cryptographic proof transport. It does not yet prove ENS-based physical authorization.
- Gate E secure authorization implementation: PASS locally. The Node bridge issues a fresh Gate A challenge only after target activation, AID SELECT, and `WAITING_CHALLENGE`; transports the exact 104-byte challenge to the ESP32; expects an exact 65-byte physical proof; and reuses the existing Gate A verifier, current ENS owner comparison, coherent `readCredential` snapshot, and `isAuthorized` policy.
- Gate E replay-check support re-verifies the exact proof through the same in-memory challenge store and produces `REPLAYED_CHALLENGE` without another NFC operation, challenge, or blockchain write.
- The separate Gate E firmware is fail-closed and one-shot. Credential label/owner parameters support a second credential without changing the existing `cred-001` defaults.
- Gate E serial finalization now requires the exact matching firmware result after Node sends one `AUTHORIZATION=ALLOW` or `AUTHORIZATION=DENY`; the opposite result and a two-second confirmation timeout fail closed, and serial closes only after successful confirmation or terminal failure.
- Gate E local validation passes Node 104/104, Gate E 20/20, Android 21/21 plus debug assembly, and Gate E firmware compilation. Android production code, firmware, APDU v1, Gate A semantics, and ENS semantics remain unchanged.
- `guest-001.demo-access.eth` is REGISTERED to `0x3419148731087b970d2059C53780163B452D5FF7` with INACTIVE `access.v1`. Provisioning completed previously through `setData` transaction `0x34c47584a377bf6d77428d19a946a322d9d31fb040caf2bf40dc205bf97112bf` and `register` transaction `0x964e488bb056b86a72870251a46e91f569f4915fc102f2aeb573b71ae202f5da`.
- GATE E INACTIVE PHYSICAL: PASS. One fresh physical proof produced `ACCESS_DENIED`; Node sent `AUTHORIZATION=DENY` exactly once and captured firmware `AUTHORIZATION: DENY` before closing COM4. Reuse of the same proof produced `REPLAYED_CHALLENGE` / DENY without new NFC, signing, challenge issuance, ENS read, or blockchain write. UID was unused.
- GATE E ACTIVE PHYSICAL: PENDING.
- BATCH A — DETERMINISTIC READINESS: PASS. Added explicit scoped renew-inactive tooling, semantic no-op when validity is already sufficient, and ACTIVE renewal refusal without changing activation/deactivation semantics.
- Gate E now enforces a 50-second post-challenge total attempt deadline, an eight-second complete ENS verification budget, a two-second controller confirmation deadline, a 1,024-byte serial input bound, immediate terminal firmware STOP handling, and redacted physical-proof logging.
- Coherent ENS authorization now accepts blocks at most 60 seconds old with at most 15 seconds of future skew and may use one optional fallback only by restarting the complete `readCredential` snapshot; provider reads are never mixed within a snapshot.
- Batch A validation passed Node 134/134, scoped Gate E 42/42, Android 21/21 plus debug assembly, and Gate E firmware compilation.
- No live `guest-001` renewal has been executed.

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

1. Run the read-only renewal preflight for `guest-001`.
2. After explicit Control Tower authorization, permit at most one resolver `setData` write to renew the still-INACTIVE record.
3. Separately, after explicit authorization, validate the still-unproven ACTIVE physical ALLOW path with a fresh proof while keeping `cred-001`, NFC UID exclusion, APDU v1, and the one-shot Gate E session model unchanged.

## Blockers

- The safe renew-inactive tooling is implemented and validated locally, but its read-only `guest-001` preflight and any separately authorized resolver write have not yet been executed.
- Gate E ACTIVE/ALLOW physical validation remains pending explicit authorization after the validity risk is handled.
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

Gate A additionally proves the transport-independent holder model:

```text
server-issued EIP-712 challenge
  -> current-owner signature verification
  -> one-shot consumption
  -> existing ENSv2 access policy
  -> ALLOW / DENY
```

Gate B additionally proves native signing interoperability:

```text
Privy Android embedded EOA
  -> exact ENSv2 Access EIP-712 signature
  -> Node/viem recovery
  -> recovered signer equals Privy wallet
```

The real-device test used `io.privy:privy-core:0.14.0` on a Solana Seeker as a generic Android device. Native `eth_signTypedData_v4` is empirically supported, the recovered signer matched public wallet `0x3419148731087b970d2059C53780163B452D5FF7`, the full suite passes 82 tests, and the test caused zero blockchain writes.

Gate B proves signing interoperability only. It does not yet prove NFC/HCE transport, physical cryptographic access, ENS credential ownership by the Privy wallet, a payment flow, or Privy prize qualification.

Gate C1 additionally proves deterministic Android-side HCE transport semantics:

```text
SELECT proprietary non-payment AID
  -> receive 104-byte binary AccessChallenge
  -> PROCESSING / READY state machine
  -> return exact 65-byte proof payload
```

The implementation resets proof state on a new challenge, SELECT, or HCE deactivation and prevents stale provider completions from affecting newer sessions. Android JVM tests and `assembleDebug` pass, while the existing Node suite remains green at 82/82.

Gate C1 does not yet prove Privy signing from HCE, real phone-to-PN532 communication, or physical cryptographic authorization.

Gate C2 additionally proves the real signing integration through the production HCE path:

```text
manual APDU harness
  -> application-scoped HceApduProcessor
  -> production PrivyProofProvider
  -> asynchronous eth_signTypedData_v4
  -> exact 65-byte proof
  -> Node/viem recovered signer match
```

On the Seeker, public wallet `0x3419148731087b970d2059C53780163B452D5FF7` matched the recovered signer. Android passed 21/21 JVM tests and `assembleDebug`; Node passed 83/83 tests. The real signature is not stored in the repository.

Gate C2 proves real Privy signing through the production HCE processor/provider path. Gate D now additionally proves actual phone-to-PN532 NFC interoperability and physical transport of that cryptographic proof; ENS credential ownership and access authorization are not yet composed into this physical path.

Gate D proves the complete transport-only path:

```text
ESP32-S3 + Elechouse PN532 initiator
  -> real ISO-DEP target activation
  -> SELECT F0454E5356324331
  -> deterministic 104-byte Gate C2 challenge
  -> Android PROCESSING / READY
  -> real Privy 65-byte EIP-712 signature + 9000
  -> Node/viem recovered signer match
```

The unchanged APDU v1 implementation passed 2/2 complete physical sessions. Full-size 67-byte responses fit the validated transport without chunking. An earlier GET_SIGNATURE failure was not reproduced and remains classified as transient rather than justification for a speculative PN532/HAL workaround.

Gate D proves physical cryptographic proof transport. Gate E now additionally proves the INACTIVE ENS authorization path:

```text
one fresh physical Gate A proof
  -> recovered signer equals current guest-001 ENS owner
  -> challenge consumed
  -> current access.active == false
  -> ACCESS_DENIED
  -> one AUTHORIZATION=DENY command
  -> matching firmware AUTHORIZATION: DENY captured before serial close
  -> same-proof replay REPLAYED_CHALLENGE / DENY
```

The controlled run used one 104-byte challenge transport, one SEND_CHALLENGE, `PROCESSING -> READY`, one successful GET_SIGNATURE, and one 65-byte proof. It performed zero blockchain writes and did not use NFC UID. The ACTIVE/ALLOW physical path has not yet been proven.
