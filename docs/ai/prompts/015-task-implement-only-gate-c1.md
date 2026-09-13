# Project task packet 015: TASK — Implement ONLY Gate C1:

- Classification: **ORIGINAL / SANITIZED**
- Redactions applied: none
- Source: retained project-specific Control Tower packet in the local Codex record

---

> TASK
>
> Implement ONLY Gate C1:
>
> a deterministic Android Host Card Emulation / ISO-DEP APDU protocol layer for
> the existing ENSv2 holder-proof architecture.
>
> Do NOT connect it to Privy signing yet.
> Do NOT modify PN532 firmware yet.
> Do NOT perform blockchain operations.
>
> WHY
>
> Already proven:
>
> Gate A:
> server-issued EIP-712 challenge
> → recover signer
> → CURRENT ENS owner
> → one-shot holder proof
> → ENS access policy.
>
> Gate B:
> real Privy Android embedded EOA
> → native eth_signTypedData_v4
> → Node/viem recovered the same address.
>
> Gate C1 must prove only that the Android application can model the NFC
> credential transport deterministically through Host Card Emulation.
>
> CURRENT EXPECTED REPOSITORY STATE
>
> Latest pushed checkpoint:
>
> 50225f11e3c550c72e2bb9740a1d4ca0da4dddc5
>
> Expected:
>
> HEAD == origin/main
> working tree clean
> except ignored local Privy/build configuration.
>
> Before editing run read-only:
>
> git status -sb
> git rev-parse HEAD
> git rev-parse origin/main
>
> If tracked state is dirty, ahead, behind or divergent:
> STOP.
>
> BASELINE VALIDATION
>
> Before editing:
>
> - run canonical Node suite:
>   node --test --test-isolation=none
> - require 82 passed, 0 failed
> - confirm existing Android debug build still succeeds
>
> If baseline differs materially:
> STOP.
>
> ARCHITECTURE BOUNDARY
>
> Do not change:
>
> - Gate A EIP-712 schema
> - Gate A challenge semantics
> - Gate B signing behavior
> - Privy authentication/wallet behavior
> - ENS authorization logic
>
> This task introduces only:
>
> Android HCE transport semantics.
>
> No PN532 code.
>
> PROTOCOL GOAL
>
> Use standard Android HostApduService.
>
> Create one application-specific AID.
>
> Choose one stable, private/demo AID appropriate for this prototype and document
> it in code.
>
> Do not use another vendor/payment application's registered AID.
>
> Minimal commands:
>
> 1. ISO7816 SELECT AID
>
> 2. SEND_CHALLENGE
>
> 3. GET_STATUS
>
> 4. GET_SIGNATURE
>
> The protocol should be versioned minimally so future incompatible changes can
> be rejected.
>
> Do not design a general-purpose smartcard protocol.
>
> CHALLENGE PAYLOAD
>
> Use the existing Gate A challenge fields, binary and fixed-length:
>
> credential:
> 32 bytes
>
> resource:
> 32 bytes
>
> nonce:
> 32 bytes
>
> expiresAt:
> 8 bytes unsigned big-endian
>
> Total challenge body:
> 104 bytes
>
> Do NOT transport JSON over NFC.
>
> Do NOT transport:
> - email
> - Privy IDs
> - wallet private data
> - ENS RPC data
>
> The challenge represents the server/verifier-issued values that will later be
> fed to the exact Gate A-compatible typed-data signer.
>
> For Gate C1 only, the payload is parsed and stored locally but is NOT signed by
> Privy.
>
> APDU COMMAND MODEL
>
> Design the smallest explicit CLA/INS/P1/P2/Lc/Data representation.
>
> Prefer a proprietary/demo CLA that does not conflict with ISO interindustry
> commands.
>
> SELECT must follow normal ISO7816 AID selection behavior.
>
> For application commands define stable instruction codes.
>
> The implementation must reject:
>
> - wrong CLA
> - unsupported INS
> - malformed Lc/data length
> - SEND_CHALLENGE before SELECT if your state model requires selection
> - challenge body not exactly 104 bytes
> - invalid protocol version where applicable
> - GET_SIGNATURE before proof is READY
>
> Use standard ISO7816 status words where suitable, for example:
>
> 9000 success
>
> 6A82 application/AID not found where applicable
>
> 6D00 INS not supported
>
> 6700 wrong length
>
> 6985 conditions not satisfied
>
> Use a small custom data byte before 9000 for application status only if useful.
>
> Do not invent dozens of error codes.
>
> STATE MACHINE
>
> Implement an explicit, testable session state.
>
> At minimum:
>
> IDLE
>
> CHALLENGE_ACCEPTED / PROCESSING
>
> READY
>
> ERROR if genuinely useful
>
> SELECT AID should initialize/reset a session safely.
>
> SEND_CHALLENGE:
>
> - requires valid selected app/session
> - parses the exact challenge
> - clears any prior proof
> - enters PROCESSING
> - returns immediately
>
> For Gate C1 provide a TEST-ONLY deterministic completion mechanism that changes:
>
> PROCESSING → READY
>
> and installs an obvious 65-byte test signature fixture.
>
> Do NOT make production HCE code use a fake signing key.
>
> Prefer dependency injection / proof provider abstraction so:
>
> today:
> DeterministicTestProofProvider
>
> later Gate C2:
> PrivyProofProvider
>
> The actual HostApduService should depend on the abstraction rather than contain
> test cryptography.
>
> GET_STATUS
>
> Return a compact deterministic state:
>
> PROCESSING
> READY
> ERROR
>
> No strings in the APDU.
>
> GET_SIGNATURE
>
> Only when READY:
>
> return exactly:
> 65 signature bytes
> + ISO7816 status word 9000
>
> No wallet address is required in the NFC proof because Node can recover the
> signer from the EIP-712 signature.
>
> After successfully returning the signature, decide and document whether the
> HCE session retains it until a new challenge/SELECT or clears it immediately.
>
> Choose the behavior that best avoids accidental replay while remaining easy
> for the reader to recover from one interrupted read.
>
> Keep this decision local to transport; Gate A remains responsible for
> cryptographic replay prevention.
>
> ANDROID HCE REGISTRATION
>
> Add the minimum Android configuration required for HCE:
>
> - NFC permission if required
> - android.hardware.nfc.hce feature declaration with appropriate required flag
> - HostApduService entry
> - exported/permission configuration required by Android
> - apduservice XML/AID registration
>
> Do not add payment-category behavior.
>
> Use category="other" or current Android equivalent suitable for a
> non-payment credential.
>
> Do not make this the default payment service.
>
> Do not add wallet/pass infrastructure.
>
> PURE APDU PROCESSOR
>
> Important:
>
> Do NOT put all protocol logic directly in HostApduService.
>
> Create a pure/testable APDU processor/state-machine class that:
>
> byte array in
> → byte array out
>
> The HostApduService should be a thin Android adapter.
>
> This allows protocol tests without NFC hardware.
>
> TESTS
>
> Add deterministic Android/JVM tests for at least:
>
> 1. correct SELECT AID → 9000
> 2. wrong AID → rejected
> 3. unsupported INS → 6D00
> 4. malformed challenge length → 6700
> 5. exact 104-byte SEND_CHALLENGE accepted
> 6. fields decode exactly:
>    - credential
>    - resource
>    - nonce
>    - uint64 expiresAt
> 7. SEND_CHALLENGE enters PROCESSING
> 8. GET_STATUS while processing returns PROCESSING
> 9. deterministic test proof completion enters READY
> 10. GET_STATUS returns READY
> 11. GET_SIGNATURE before READY → 6985
> 12. GET_SIGNATURE at READY returns exactly 65 proof bytes + 9000
> 13. new challenge clears previous proof
> 14. new SELECT/session reset cannot accidentally expose old proof
> 15. malformed APDU does not crash
> 16. sequential challenge sessions remain isolated
> 17. no Privy/real secret is used by HCE protocol tests
>
> If concurrency/reentrancy around service state can create a realistic issue,
> add the smallest relevant test; do not overengineer it.
>
> GATE A/B COMPATIBILITY
>
> Do not duplicate the EIP-712 schema in Android protocol semantics beyond the
> fixed field sizes/order required to transport it.
>
> Document the binary order clearly.
>
> If practical, add a small Node interoperability test/vector proving:
>
> Gate A challenge fields
> → binary Gate C challenge encoding
> → decode
> → exact original values
>
> Only do this if it can reuse existing dependencies and remain small.
>
> Do not add a new serialization library.
>
> MANUAL DEVICE VALIDATION
>
> Because wired adb remains unreliable on the Seeker, ADB is NOT required for
> Gate C1 PASS.
>
> Build the debug APK.
>
> If necessary, the user may manually install it again by USB file transfer.
>
> For Gate C1, real NFC exchange with PN532 is NOT required.
>
> Do not claim physical HCE transport has passed.
>
> Gate C1 proves:
>
> - Android HCE service is registered/buildable
> - APDU state machine is deterministic/tested
> - app remains installable
>
> Actual phone ↔ PN532 exchange belongs to Gate D after C2.
>
> NO-TOUCH
>
> Do not modify:
>
> - firmware/
> - scripts/nfc/
> - demo/
> - ENS deployment/write code
> - Gate A holder-proof semantics
> - Gate B verifier semantics
> - project-control documentation
> - Privy local configuration values
>
> Do not implement:
>
> - PN532 APDU firmware
> - actual NFC hardware exchange
> - Privy signing inside HostApduService
> - payment
> - World
> - ERC-4337
> - credential issuance
> - ENS transfer
> - Aliro
>
> Do not install new dependencies unless Android HCE implementation genuinely
> requires one.
>
> Standard Android SDK APIs require no new library.
>
> VALIDATION
>
> Require:
>
> Android:
> - unit/JVM HCE protocol tests PASS
> - :app:assembleDebug PASS
>
> Node:
> - node --test --test-isolation=none
> - existing 82 tests remain green
> - any small interoperability tests green
>
> Repository:
> - git diff --check
> - scoped git status
> - safe secret scan
> - local Privy properties still ignored/not staged
>
> No blockchain transaction.
> No real signing.
> No NFC hardware operation.
>
> Do not commit.
> Do not push.
>
> EVIDENCE REQUIRED
>
> Return:
>
> # GATE C1 ANDROID HCE PROTOCOL
>
> ## BASELINE
>
> - HEAD
> - origin/main
> - status
> - Node baseline test count
> - Android baseline build
>
> ## FILES CHANGED
>
> ## AID
>
> Give:
> - chosen AID
> - rationale
> - category
>
> ## APDU V1
>
> Give a compact table:
>
> COMMAND
> CLA
> INS
> DATA
> SUCCESS RESPONSE
>
> ## BINARY CHALLENGE
>
> Confirm exact byte order and total length.
>
> ## STATE MACHINE
>
> Explain:
>
> IDLE
> PROCESSING
> READY
>
> and reset behavior.
>
> ## HCE SERVICE
>
> Explain Android manifest/XML/service registration.
>
> ## PROOF PROVIDER ABSTRACTION
>
> Explain how Gate C1 test provider can later be replaced by Privy in Gate C2
> without changing APDU semantics.
>
> ## TESTS
>
> List required cases PASS/FAIL.
>
> ## BUILD
>
> ## EXISTING NODE TESTS
>
> ## SECURITY BOUNDARY
>
> Explicitly confirm:
>
> - no UID authorization
> - no real signing
> - no private keys
> - no Privy identifiers exposed
> - no blockchain writes
> - no PN532 change
>
> ## WHAT THIS PROVES
>
> State clearly:
>
> Gate C1 proves deterministic Android-side HCE protocol implementation.
>
> It does NOT prove real phone-to-reader NFC communication yet.
>
> ## GIT STATE
>
> Do not commit.
>
> ## NEXT TASK
>
> Recommend ONLY Gate C2:
>
> wire the already-proven Privy EIP-712 signer into this HCE proof-provider
> interface while preserving the APDU V1 contract.
>
> End exactly:
>
> GATE C1: PASS
>
> or
>
> GATE C1: STOP — <reason>
>
> STOP IF
>
> Stop if:
>
> - repo baseline differs unexpectedly
> - Android build has regressed
> - HCE requires changing Gate A/B cryptographic semantics
> - the 104-byte challenge cannot be represented by the proposed APDU model
>   without a material redesign
> - a new dependency appears necessary for basic HCE
> - correct service registration cannot be achieved with standard Android APIs
> - scope starts expanding into PN532 or real NFC exchange
