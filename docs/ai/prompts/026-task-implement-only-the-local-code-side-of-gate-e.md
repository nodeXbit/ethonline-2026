# Project task packet 026: TASK — Implement ONLY the local/code side of Gate E:

- Classification: **ORIGINAL / SANITIZED**
- Redactions applied: none
- Source: retained project-specific Control Tower packet in the local Codex record

---

> TASK
>
> Implement ONLY the local/code side of Gate E:
>
> fresh server-issued Gate A challenge
> → serial transport to ESP32
> → physical HCE proof transport
> → proof returned to Node
> → existing Gate A current-holder verification
> → CURRENT ENSv2 ownership/access authorization
> → ALLOW / DENY returned to the ESP32.
>
> DO NOT perform blockchain writes yet.
>
> Do NOT provision/register/activate/deactivate any credential during this task.
>
> WHY
>
> The following boundaries are already independently proven:
>
> Gate A:
> server-issued EIP-712 challenge
> → signer recovery
> → CURRENT ENS owner comparison
> → one-shot challenge consumption
> → existing ENS access policy.
>
> Gate B:
> real Privy Android embedded EOA
> → eth_signTypedData_v4
> → recovered signer == Privy wallet.
>
> Gate C:
> Privy signing through the production Android HCE processor.
>
> Gate D:
> ESP32-S3 + Elechouse PN532
> ↔ Seeker HostApduService
> → real 104-byte challenge transport
> → real 65-byte Privy signature transport
> → Node recovery
> → 2/2 physical PASS.
>
> Gate E must COMPOSE these existing pieces.
>
> It must not introduce another cryptographic protocol or another ENS
> authorization implementation.
>
> CURRENT CANONICAL REPOSITORY
>
> Expected pushed checkpoint:
>
> c3e5f1be8ec83387f50b16e34cfb2241238ca856
>
> Before editing run:
>
> git status -sb
> git rev-parse HEAD
> git rev-parse origin/main
>
> Require:
>
> HEAD == origin/main ==
> c3e5f1be8ec83387f50b16e34cfb2241238ca856
>
> tracked working tree clean.
>
> Ignored local configuration/build outputs are acceptable.
>
> If local state differs materially:
> STOP.
>
> BASELINE VALIDATION
>
> Before editing require:
>
> Node:
>
> node --test --test-isolation=none
>
> Expected:
> 83 passed
> 0 failed
>
> Android:
>
> :app:testDebugUnitTest
> Expected:
> 21/21 PASS
>
> :app:assembleDebug
> Expected:
> PASS
>
> Gate D firmware compile:
> PASS.
>
> If baseline differs materially:
> STOP.
>
> PHASE 1 — INSPECT EXISTING COMPOSITION POINTS
>
> Read relevant current code completely before editing.
>
> At minimum inspect:
>
> scripts/security/holder-proof.mjs
> scripts/ensv2/access-record.mjs
> the actual current ENS credential-state/read helper
> current persistent-access helpers
> current ENS config/env-example conventions
> scripts/security Gate B/C verifier helpers
> firmware/pn532_hce_apdu/pn532_hce_apdu.ino
>
> Inspect package.json only as required for command conventions.
>
> Determine exactly:
>
> 1. how Gate A issues/stores challenges;
> 2. how verifyAccessAttempt receives current credential state;
> 3. how isAuthorized is reused;
> 4. how current ENS reads are pinned/coherently obtained;
> 5. how credential name/namehash are currently constructed;
> 6. whether existing persistent credential tooling can target a different:
>    - credential name
>    - credential owner
>    without modifying cred-001;
> 7. how Gate D currently sends its deterministic challenge;
> 8. what serial tooling already exists through the installed serialport package.
>
> DO NOT inspect private environment-file contents.
>
> Do not duplicate any of these mechanisms unnecessarily.
>
> PHASE 2 — GATE E CREDENTIAL PLAN
>
> Gate E must NOT reuse or transfer:
>
> cred-001.demo-access.eth
>
> That credential remains intact.
>
> Default new secure credential:
>
> guest-001.demo-access.eth
>
> Expected owner:
>
> 0x3419148731087b970d2059C53780163B452D5FF7
>
> This is the already-verified public Privy embedded EOA.
>
> For this implementation task:
>
> DO NOT register it.
> DO NOT set resolver data.
> DO NOT activate it.
> DO NOT send transactions.
>
> Only determine the smallest existing/proposed provisioning path.
>
> Prefer reusing current idempotent ENSv2 persistent-credential tooling.
>
> If existing tooling can cleanly provision a separate credential and owner
> through parameters/config:
> reuse it.
>
> If existing tooling is hard-coded specifically to cred-001 and a broad refactor
> would be required:
> STOP and report the issue before changing the existing persistent demo.
>
> A very small scoped wrapper/parameter export is acceptable only if the existing
> helpers already support the underlying operation cleanly.
>
> Do not mutate the old credential to save work.
>
> PHASE 3 — RESOURCE ID
>
> Gate E needs one stable physical resource identifier.
>
> Use one deterministic application value, unless the repository already has a
> canonical resource convention.
>
> Preferred default:
>
> keccak256(
>   UTF-8 "demo-access.eth:door-001"
> )
>
> using existing viem utilities.
>
> Expose/document both:
>
> human name:
> demo-access.eth:door-001
>
> bytes32 resource ID:
> derived deterministically at runtime/test time.
>
> Do not invent a database/resource registry.
>
> The ESP32 and Android do not need to interpret the human-readable resource;
> they transport/sign the bytes32 value.
>
> PHASE 4 — FRESH CHALLENGE TIMING
>
> Do NOT embed a deterministic Gate D challenge in Gate E runtime firmware.
>
> The same Node process that later verifies the proof must:
>
> 1. own one IssuedChallengeStore;
> 2. wait until the physical target is activated and SELECT has succeeded;
> 3. only then issue the fresh Gate A challenge;
> 4. serialize its exact 104-byte fields;
> 5. send them to the ESP32.
>
> This preserves the in-memory issued-challenge security model and minimizes TTL
> loss while the user positions the phone.
>
> Use Gate A's existing default TTL unless a concrete current implementation
> constraint requires otherwise.
>
> Do not create a second nonce implementation.
>
> Production/default nonce generation must remain Gate A's secure random
> generation.
>
> PHASE 5 — SERIAL CONTROL PROTOCOL
>
> Gate E keeps Node as network/onchain verifier.
>
> ESP32 is only:
>
> NFC/APDU controller
> +
> serial transport endpoint.
>
> Implement the smallest line-oriented serial control protocol needed.
>
> Exact token names may follow repository conventions, but behavior should be
> equivalent to:
>
> ESP32 → Node:
>
> GATE_E_READY
> PRESENT_SEEKER
> TARGET_ACTIVATION: PASS
> SELECT: PASS
> WAITING_CHALLENGE
>
> Node → ESP32:
>
> CHALLENGE=0x<exactly 208 ASCII hex characters>
>
> The 208 hex chars represent exactly the 104 Gate A bytes:
>
> credential[32]
> || resource[32]
> || nonce[32]
> || expiresAt[8 unsigned big-endian]
>
> ESP32 validates:
> - prefix
> - ASCII hex
> - exact decoded length 104
>
> Then performs frozen APDU v1:
>
> SEND_CHALLENGE
> GET_STATUS
> GET_SIGNATURE
>
> ESP32 → Node:
>
> PROOF=0x<exactly 130 ASCII hex characters>
>
> representing exactly 65 signature bytes.
>
> Do not use the NFC UID in this protocol.
>
> After Node verifies:
>
> Node → ESP32:
>
> AUTHORIZATION=ALLOW
>
> or:
>
> AUTHORIZATION=DENY
>
> ESP32 prints a final human-readable result:
>
> AUTHORIZATION: ALLOW
>
> or:
>
> AUTHORIZATION: DENY
>
> Then the Gate E firmware/session terminates and requires reset for another
> attempt.
>
> No automatic second signing session.
>
> Do not send detailed blockchain policy data to the ESP32.
>
> PHASE 6 — GATE E FIRMWARE
>
> Preserve:
>
> firmware/pn532_hce_apdu/pn532_hce_apdu.ino
>
> as Gate D evidence.
>
> Create a separate Gate E firmware sketch, preferably:
>
> firmware/pn532_secure_access/pn532_secure_access.ino
>
> Reuse the already-proven:
>
> - Elechouse PN532 / PN532_I2C stack
> - GPIO17/GPIO18
> - PN532 0x24
> - Serial0 115200
> - target activation
> - SELECT
> - SEND_CHALLENGE
> - GET_STATUS polling
> - GET_SIGNATURE handling
> - 65-byte proof validation
>
> Change only what Gate E requires:
>
> deterministic firmware challenge
> → challenge received from Node over serial.
>
> Flow:
>
> boot
> → GATE_E_READY
> → PRESENT_SEEKER
> → target activation
> → SELECT
> → WAITING_CHALLENGE
> → receive exactly one fresh challenge from Node
> → SEND_CHALLENGE once
> → PROCESSING/READY
> → GET_SIGNATURE
> → send proof to Node
> → wait for AUTHORIZATION
> → print final ALLOW/DENY
> → halt.
>
> FAIL CLOSED.
>
> On malformed challenge, timeout, APDU error or malformed authorization:
> DENY/STOP.
>
> Never resend SEND_CHALLENGE automatically.
>
> Do not add:
> - Wi-Fi
> - RPC
> - ENS
> - private keys
> - UID authorization
> to ESP32.
>
> PHASE 7 — NODE SECURE BRIDGE
>
> Create the smallest Node orchestrator, following existing naming conventions.
>
> Preferred conceptual responsibility:
>
> GateESecureBridge
>
> One process must:
>
> 1. open the ESP32 serial port;
> 2. wait for WAITING_CHALLENGE;
> 3. issue the Gate A challenge through the existing challenge store;
> 4. send its canonical 104-byte representation;
> 5. receive the exact 65-byte proof;
> 6. verify it through existing Gate A verifyAccessAttempt;
> 7. use a REAL current ENS credential read dependency in live mode;
> 8. send ALLOW/DENY to ESP32;
> 9. exit after one attempt.
>
> Do not copy Gate A's:
> - EIP-712 definitions
> - nonce store
> - signature recovery
> - consumption semantics
> - isAuthorized logic.
>
> Reuse them.
>
> TESTABILITY
>
> Separate pure/orchestration logic from actual SerialPort and live ENS transport
> through dependency injection where useful.
>
> Unit tests must not require:
> - COM4
> - PN532
> - Privy
> - real RPC.
>
> LIVE MODE
>
> The eventual live bridge will use:
>
> credential:
> guest-001.demo-access.eth
>
> resource:
> demo-access.eth:door-001
>
> and current Sepolia ENS state.
>
> Do not execute live mode during this task if it would require the new
> credential to exist.
>
> PHASE 8 — COHERENT CURRENT ENS SNAPSHOT
>
> This is security-sensitive.
>
> At proof verification time:
>
> recovered signer
> must be compared to CURRENT credential owner
>
> and access policy must use a coherent current ENS snapshot.
>
> Prefer reusing the existing credential read that already pins state to one
> block.
>
> Require one snapshot/block context for:
>
> - registration status
> - owner
> - registry expiry
> - resolver/access.v1 state
> - block timestamp used by authorization
>
> Do NOT independently read owner at one arbitrary block and policy at another if
> the existing helper can provide one coherent snapshot.
>
> If current code does not provide a reusable coherent snapshot and fixing that
> requires a broad ENS refactor:
> STOP.
>
> Do not invent duplicate reads.
>
> PHASE 9 — AUTHORIZATION SEMANTICS
>
> Gate E must preserve Gate A exactly.
>
> Valid current-holder proof:
>
> - challenge server-issued
> - challenge PENDING
> - not expired
> - credential/resource match
> - signature valid
> - recovered signer == CURRENT ENS owner
>
> THEN:
>
> challenge becomes CONSUMED exactly once
>
> THEN:
>
> existing ENS policy determines:
>
> ALLOW or DENY.
>
> Therefore:
>
> valid holder + ACTIVE policy
> → ALLOW
>
> valid holder + INACTIVE policy
> → DENY
> → challenge consumed
>
> replay same consumed proof
> → DENY / REPLAYED_CHALLENGE
>
> wrong signer
> → DENY
> → challenge NOT consumed
>
> Do not change this ordering.
>
> PHASE 10 — REPLAY VALIDATION SUPPORT
>
> For Gate E validation we need to prove physical proof replay rejection without
> requiring another NFC signing operation.
>
> Add a validation-only mechanism, preferably a CLI flag such as:
>
> --check-replay
>
> After the first real verification result, while the same Node process/store is
> still alive:
>
> call the existing Gate A verification again with the exact same:
> - challenge identifier/nonce
> - signature
>
> Expected:
>
> REPLAYED_CHALLENGE
> DENY
>
> This must:
>
> - perform zero NFC operations
> - perform zero blockchain writes
> - create no new challenge
> - never send ALLOW on replay.
>
> Do not make replay checking mandatory production behavior if it unnecessarily
> pollutes the normal bridge.
>
> PHASE 11 — PRIVY-OWNED CREDENTIAL PROVISIONING SUPPORT
>
> Do NOT perform writes.
>
> Inspect the existing setup/activation/deactivation tooling.
>
> We will later need a controlled live sequence:
>
> A. provision:
>
> guest-001.demo-access.eth
>
> owner:
>
> 0x3419148731087b970d2059C53780163B452D5FF7
>
> using the existing demo-access.eth UserRegistry and verified resolver.
>
> Initial access:
> INACTIVE / DENY
>
> B. activate:
> one existing validated resolver write
>
> C. deactivate:
> one existing validated resolver write.
>
> If current helpers already support this cleanly:
> document the exact future commands/config path.
>
> If they require only a SMALL safe parameterization:
> implement and test that parameterization.
>
> Do NOT execute it.
>
> Do NOT unregister/transfer/change cred-001.
>
> Do NOT hard-code the real Privy wallet into generic reusable ENS helpers if it
> can be passed as a public parameter.
>
> PHASE 12 — TESTS
>
> Add deterministic tests for the new Node/serial bridge boundaries.
>
> At minimum prove:
>
> 1. WAITING_CHALLENGE causes exactly one Gate A challenge issuance.
>
> 2. challenge serial payload:
>    - ASCII hex
>    - exact 104 decoded bytes
>    - exact credential bytes32
>    - exact resource bytes32
>    - exact nonce
>    - exact expiresAt big-endian.
>
> 3. no challenge is issued before WAITING_CHALLENGE.
>
> 4. duplicate WAITING_CHALLENGE cannot issue multiple concurrent challenges for
>    one bridge attempt.
>
> 5. exact 65-byte PROOF is accepted.
>
> 6. malformed/non-hex/wrong-length proof fails closed.
>
> 7. correct holder + mocked ACTIVE coherent ENS state:
>    → ALLOW
>    → AUTHORIZATION=ALLOW sent once.
>
> 8. correct holder + mocked INACTIVE ENS state:
>    → DENY
>    → AUTHORIZATION=DENY
>    → challenge CONSUMED.
>
> 9. replay flag:
>    first valid proof result
>    → second same proof
>    → REPLAYED_CHALLENGE / DENY
>    → zero new challenge.
>
> 10. wrong holder:
>     → DENY
>     → no ALLOW.
>
> 11. expired challenge:
>     → DENY.
>
> 12. RPC/read failure:
>     → fail closed DENY/error
>     → never ALLOW.
>
> 13. timeout waiting for proof:
>     → fail closed
>     → no second SEND_CHALLENGE/challenge issuance.
>
> 14. serial duplicate/malformed messages cannot create a second authorization
>     attempt.
>
> 15. NFC UID does not occur as an input to secure authorization.
>
> 16. existing Gate A tests remain unchanged/green.
>
> Do not use real Privy signatures in committed tests.
>
> Use deterministic TEST ONLY accounts/fixtures.
>
> PHASE 13 — FIRMWARE VALIDATION
>
> Compile the new Gate E firmware with the exact existing:
>
> - ESP32 core
> - Elechouse PN532 libraries
> - hardware configuration.
>
> Do not flash/run it physically yet unless compilation itself requires the
> existing normal workflow.
>
> No physical Gate E validation during this implementation task.
>
> Gate D firmware must remain unchanged and still compile.
>
> PHASE 14 — ANDROID
>
> Expected Android production change for Gate E:
>
> NONE.
>
> Gate C2 already signs arbitrary canonical HceChallenge values received through
> APDU.
>
> Run:
>
> :app:testDebugUnitTest
> :app:assembleDebug
>
> Require:
> 21/21 PASS
> build PASS.
>
> If Gate E appears to require changing Android/APDU v1:
> STOP.
>
> PHASE 15 — VALIDATION
>
> Require:
>
> Node:
> node --test --test-isolation=none
>
> All previous 83 tests plus new Gate E tests PASS.
>
> Android:
> 21/21 PASS
> assembleDebug PASS.
>
> Firmware:
> Gate D compile PASS
> Gate E compile PASS.
>
> Repository:
>
> - git diff --check
> - scoped status
> - safe secret scan
> - no real signature
> - no App ID / Client ID / App Secret
> - no local env/config staged
> - no blockchain transaction.
>
> NO-TOUCH
>
> Do not modify unless a tiny explicitly justified reuse export is required:
>
> - Gate A cryptographic semantics
> - Gate B/C Android signing semantics
> - APDU v1
> - Gate D firmware
> - existing cred-001 lifecycle
> - demo UI
> - README
> - PROJECT.md
> - STATUS.md
> - WORKLOG.md
> - DECISIONS.md
>
> Do not implement:
>
> - payment/USDC
> - World
> - ERC-4337
> - Aliro
> - loyalty
> - transferability
> - lock relay
> - Wi-Fi on ESP32
> - smart-wallet signatures
> - blockchain writes.
>
> DO NOT COMMIT.
> DO NOT PUSH.
>
> STOP IF
>
> Stop if:
>
> - repo state differs unexpectedly;
> - current ENS credential read cannot be reused coherently without broad
>   refactoring;
> - provisioning a second Privy-owned credential requires mutating cred-001;
> - Gate E would require APDU v1 changes;
> - Android changes are required for arbitrary fresh challenge signing;
> - in-memory Gate A store cannot remain in the same process from issuance through
>   verification;
> - serial architecture would require putting ENS/network logic on ESP32;
> - any private key/App Secret must be exposed;
> - scope expands beyond Gate E.
>
> RETURN
>
> # GATE E SECURE AUTHORIZATION IMPLEMENTATION
>
> ## BASELINE
> - HEAD
> - origin/main
> - status
> - baseline tests/builds
>
> ## FILES CHANGED
>
> ## SECURE CREDENTIAL PLAN
>
> Explicitly state:
>
> credential:
> guest-001.demo-access.eth
>
> intended owner:
> 0x3419148731087b970d2059C53780163B452D5FF7
>
> and whether current tooling can provision it safely without touching cred-001.
>
> NO writes performed.
>
> ## RESOURCE ID
>
> Human identifier plus computed bytes32.
>
> ## SERIAL PROTOCOL
>
> Compact table:
>
> DIRECTION
> MESSAGE
> MEANING
>
> ## FRESH CHALLENGE FLOW
>
> Show exactly when the Gate A challenge is issued.
>
> ## GATE E FIRMWARE
>
> ## NODE SECURE BRIDGE
>
> ## ENS SNAPSHOT COMPOSITION
>
> State exactly which existing read/policy helpers are reused.
>
> Confirm no duplicate authorization implementation.
>
> ## REPLAY SEMANTICS
>
> ## TESTS
>
> List required cases PASS/FAIL.
>
> ## ANDROID VALIDATION
> ## FIRMWARE VALIDATION
> ## NODE TESTS
>
> ## SECURITY
>
> Confirm:
> - UID not used
> - no private key
> - no Privy secrets
> - no real signature committed
> - no blockchain writes
> - cred-001 unchanged
> - APDU v1 unchanged.
>
> ## LIVE PROVISIONING PLAN
>
> Do NOT execute it.
>
> List the exact smallest future write sequence required to create the
> Privy-owned credential and establish INACTIVE baseline.
>
> State expected number/type of transactions.
>
> Then list the later validation writes needed for:
> INACTIVE → ACTIVE → INACTIVE.
>
> Do not authorize them yourself.
>
> ## GIT STATE
>
> Do not commit.
>
> ## SAFE LIVE VALIDATION NEXT
>
> Specify the future physical validation sequence:
>
> 1. INACTIVE → physical proof → DENY
> 2. activate
> 3. fresh physical proof → ALLOW
> 4. same proof replay → DENY
> 5. deactivate
> 6. fresh physical proof → DENY
>
> ## NEXT ACTION
>
> Checkpoint Gate E implementation before any live write.
>
> End exactly:
>
> GATE E IMPLEMENTATION: PASS
>
> or
>
> GATE E IMPLEMENTATION: STOP — <reason>
