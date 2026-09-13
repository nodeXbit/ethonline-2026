# Project task packet 095: TASK — BUILD LOCKENS DYNAMIC NFC + RESOURCE-AWARE VIRTUAL GATES

- Classification: **ORIGINAL / SANITIZED**
- Redactions applied: none
- Source: retained project-specific Control Tower packet in the local Codex record

---

> TASK — BUILD LOCKENS DYNAMIC NFC + RESOURCE-AWARE VIRTUAL GATES
>
> MISSION
>
> Build the next complete LockENS vertical:
>
> SELECTED PASS
> → Android HCE
> → PN532
> → ESP32
> → Node physical verifier
> → resource-bound holder challenge
> → ENSv2 credential/resource policy
> → virtual gate
> → ALLOW / DENY
> → Gate Monitor
>
> This is intentionally a large cross-stack implementation task.
>
> Use GPT-6 Astra reasoning to inspect the existing implementation first and then
> make the smallest coherent changes necessary.
>
> NO real ENS/blockchain writes.
>
> NO wallet signatures except deterministic/local test fixtures.
>
> NO physical firmware flashing automatically.
>
> NO real credential creation.
>
> NO commit/push of the NEW NFC/gate implementation.
>
> ============================================================
> PHASE 0 — CHECKPOINT CURRENT PASS STUDIO + SAFETY WORK
> ============================================================
>
> Current pushed baseline before Pass Studio:
>
> 09e8153a01bca6283a773d41c77572442b742337
>
> Current worktree intentionally contains:
>
> - LockENS Pass Studio;
> - Studio UX refinements;
> - Astra safety fixes;
> - final coordination fixes.
>
> Previous independent review found RA-01 / RA-02 / RA-03.
> They have now been fixed and targeted adversarial tests report PASS.
>
> Before touching NFC/Node/firmware:
>
> 1. inspect the ENTIRE current diff;
> 2. run the full relevant validation;
> 3. review for secrets/unrelated changes;
> 4. update STATUS / WORKLOG / DECISIONS only as genuinely needed;
> 5. create a coherent checkpoint commit or small coherent commit set;
> 6. push;
> 7. verify clean synchronization.
>
> I EXPLICITLY AUTHORIZE:
>
> - committing the CURRENT Studio + safety work only;
> - pushing those checkpoint commits to origin/main;
> - only if validation is green and the diff is scoped.
>
> Suggested messages:
>
> feat: add secure LockENS Pass Studio
>
> fix: harden Studio transaction coordination
>
> or a smaller coherent history if preferable.
>
> After push require:
>
> git fetch origin
> git status -sb
> git rev-parse HEAD
> git rev-parse origin/main
>
> Require:
>
> clean
> HEAD == origin/main
> ahead/behind 0/0
>
> Record the resulting SHA as:
>
> NFC_BASELINE_SHA
>
> Only then continue.
>
> If checkpoint/push/synchronization fails:
>
> STOP.
> Do not begin NFC implementation.
>
> ============================================================
> CURRENT PRODUCT MODEL
> ============================================================
>
> LockENS now has:
>
> - Privy authentication;
> - multiple embedded wallets;
> - active wallet selection;
> - automatic R1 credential discovery;
> - My Keys;
> - selected pass persisted by wallet/chain;
> - Pass Studio;
> - R1 UserRegistry;
> - S1 PermissionedResolver;
> - transaction recovery;
> - read-only RPC;
> - existing physical holder-proof / Gate stack.
>
> Selected Pass is currently LOCAL product state only.
>
> HCE has NOT yet been connected to Selected Pass.
>
> ============================================================
> AUTHORITATIVE ENSV2 CONFIG
> ============================================================
>
> Chain:
>
> Sepolia
> 11155111
>
> Namespace:
>
> keys.demo-access.eth
>
> Parent R0:
>
> 0x2d249472B83A453086254Acd8a42913D8e45a2Fd
>
> R1:
>
> 0x0AeB395be893149c0b60D9DAC3Ba55139D0dcA1a
>
> S1:
>
> 0x20766FB21498a99350F922ee3163F5a95F354a7f
>
> Issuer/root controller:
>
> 0xFa90e8301A22833B74378C5fA3a7c120Ac512685
>
> Existing real pass:
>
> staff-001.keys.demo-access.eth
>
> Do NOT modify it onchain in this task.
>
> ============================================================
> EXISTING PHYSICAL SECURITY — PRESERVE
> ============================================================
>
> Inspect exact current implementation before editing.
>
> Known prior security properties that MUST NOT regress include:
>
> - fresh challenge;
> - replay protection;
> - bounded challenge/proof TTL;
> - post-challenge timing bounds;
> - ENS read timeout;
> - coherent onchain snapshot;
> - max block age;
> - future-skew rejection;
> - fail closed;
> - controller confirmation;
> - replayed proof DENY;
> - no blockchain logic in PN532/ESP32;
> - firmware serial confirmation only;
> - no claim of physical relay/lock actuator.
>
> Do not trust this summary over source.
> Reconstruct exact current state.
>
> ============================================================
> PHASE 1 — RECONSTRUCT COMPLETE CURRENT ACCESS STACK
> ============================================================
>
> READ FIRST.
>
> Inspect:
>
> ANDROID:
> - HCE service;
> - AID / APDU constants;
> - holder-proof signer;
> - selected-pass persistence;
> - active-wallet provider;
> - Privy signing;
> - current challenge parser;
> - current status/signature buffers.
>
> NODE:
> - Gate A / holder-proof verifier;
> - Gate E;
> - ENS reads;
> - replay cache;
> - challenge creation;
> - policy;
> - serial bridge;
> - controller confirmation;
> - CLI/UI if any.
>
> FIRMWARE:
> - PN532 APDU sequence;
> - ISO-DEP handling;
> - ESP32 serial protocol;
> - current challenge/signature transport.
>
> CONFIG:
> - ENS deployment;
> - current resource representation;
> - timing/security constants.
>
> TESTS:
> - Android HCE;
> - Node holder proof;
> - replay;
> - firmware/APDU fixtures.
>
> Return internally a dependency map before editing.
>
> Do NOT change architecture blindly.
>
> ============================================================
> PHASE 2 — DYNAMIC CREDENTIAL APDU
> ============================================================
>
> Current/expected AID:
>
> F0454E5356324331
>
> Current APDUs are expected to include:
>
> SEND_CHALLENGE
> 80 10 01 00
>
> GET_STATUS
> 80 20 01 00
>
> GET_SIGNATURE
> 80 30 01 00
>
> Add:
>
> GET_CREDENTIAL
> 80 40 01 00
>
> same AID / protocol version where safely compatible.
>
> Response:
>
> UTF-8 normalized full ENS credential name
> followed by normal success status word.
>
> Recommended maximum credential-name payload:
> 64 bytes
>
> Use existing APDU error/status conventions.
>
> Do not invent silent fallback behavior.
>
> ============================================================
> HCE SELECTED-PASS RULE
> ============================================================
>
> GET_CREDENTIAL must expose the currently selected LockENS pass for:
>
> active wallet
> +
> Sepolia
> +
> SelectedPassStore
>
> Requirements:
>
> - selected pass must belong to current active wallet according to current local
>   validated product state;
> - HCE session latches:
>   wallet identity
>   selected credential
>   chain
> - later APDUs in the same NFC session use the latched identity;
> - active wallet / selected pass changing mid-session cannot silently switch the
>   credential being proven;
> - no selected pass => fail closed;
> - invalidated ownership => fail closed;
> - never fall back silently to guest-001 or another credential.
>
> Do NOT remove the existing legacy/fallback demo code unless required.
> Preserve it as an explicitly separate compatibility path.
>
> Dynamic mode must never silently degrade to the legacy credential.
>
> ============================================================
> PHASE 3 — CREDENTIAL-FIRST READER FLOW
> ============================================================
>
> Physical verifier flow should become conceptually:
>
> SELECT AID
>
> GET_CREDENTIAL
> → selected full ENS name
>
> Gate validates credential candidate enough to proceed
>
> Gate creates challenge bound to:
> credential
> +
> resource
> +
> fresh nonce
> +
> expiry
>
> SEND_CHALLENGE
>
> GET_STATUS
>
> GET_SIGNATURE
>
> final verification
>
> Do not issue/consume a challenge before the discovered credential has passed the
> required structural/provenance checks.
>
> No challenge should be wasted on malformed/untrusted arbitrary names where
> avoidable.
>
> ============================================================
> PHASE 4 — EXACT ENS CREDENTIAL VALIDATION
> ============================================================
>
> A phone-provided ENS name is UNTRUSTED DISCOVERY DATA.
>
> Node must independently validate:
>
> - normalized name;
> - direct child of keys.demo-access.eth;
> - expected hierarchy;
> - demo-access.eth → existing R0;
> - keys.demo-access.eth → exact R1;
> - resolver == exact S1;
> - expected R1/S1 provenance;
> - REGISTERED;
> - unexpired registration;
> - current owner;
> - no unsupported subregistry where not expected.
>
> Do not trust:
>
> phone
> event cache
> local pass UI
> historical owner
>
> as authority.
>
> ============================================================
> PHASE 5 — RESOURCE MODEL
> ============================================================
>
> Implement explicit resources.
>
> For this sprint support exactly:
>
> FRONT_DOOR
> LAB
> SERVER_ROOM
>
> Human names:
>
> Front Door
> Lab
> Server Room
>
> Create a PUBLIC configuration file, e.g.:
>
> config/access-resources.json
>
> Use repo conventions if a better path exists.
>
> Each resource must have:
>
> slug
> displayName
> deterministic bytes32 resourceId
>
> Canonical ID derivation:
>
> keccak256(
>   UTF-8(
>     "lockens:resource:v1:" + canonicalSlug
>   )
> )
>
> Use lowercase canonical slugs:
>
> front-door
> lab
> server-room
>
> Generate/validate the exact bytes32 IDs in both Node and Android tests.
>
> Do not hard-code inconsistent copies across languages.
>
> ============================================================
> PHASE 6 — ONCHAIN RESOURCE POLICY
> ============================================================
>
> Use the existing PermissionedResolver S1.
>
> NO new Solidity.
>
> Define a versioned data record:
>
> resources.v1
>
> Canonical value:
>
> ABI encoding of a sorted unique bytes32[] set of allowed resource IDs.
>
> Record semantics:
>
> resources.v1
> → which LockENS physical resources this pass may access.
>
> Global access remains separately controlled by:
>
> access.v1
>
> Decision model:
>
> registered
> AND registration unexpired
> AND current holder proven
> AND access.v1.active
> AND access.v1.validUntil >= current time
> AND requested resourceId ∈ resources.v1
>
> => eligible for ALLOW
>
> Missing / empty / malformed resources.v1:
>
> DENY in RESOURCE-AWARE mode.
>
> Do not interpret missing resources as "all resources."
>
> Do not use wildcard permissions in this sprint.
>
> ============================================================
> LEGACY COMPATIBILITY
> ============================================================
>
> Existing staff-001 currently has no resources.v1 record.
>
> Do NOT mutate it onchain in this task.
>
> Preserve existing legacy verifier behavior separately where needed for regression.
>
> In resource-aware virtual-gate mode:
>
> staff-001 without resources.v1
> must fail closed with a clear reason:
>
> RESOURCE_POLICY_MISSING
>
> Later Studio management can assign resources.
>
> Do not silently treat legacy access.v1 as permission for every gate.
>
> ============================================================
> PHASE 7 — PASS STUDIO RESOURCE INTEGRATION
> ============================================================
>
> Extend Pass Studio WITHOUT weakening its recently hardened transaction model.
>
> PassDraft gains:
>
> allowedResources
>
> Human UI:
>
> Resources
>
> ☑ Front Door
> ☑ Lab
> ☑ Server Room
>
> Templates default to:
>
> STAFF:
> Front Door
> Lab
> Server Room
>
> VISITOR:
> Front Door
>
> CONTRACTOR:
> Lab
>
> These are PRESETS and remain editable.
>
> For new issuance TX2:
>
> S1.multicall includes:
>
> optional avatar
> description
> access.v1
> resources.v1
>
> resources.v1 must use canonical sorted unique bytes32[] encoding.
>
> ============================================================
> MANAGE RESOURCE POLICY
> ============================================================
>
> Studio → Manage Pass:
>
> Resources
> [ Manage ]
>
> Authorized SET_DATA wallet can update resources.v1.
>
> Review MUST show:
>
> old human resource names
> →
> new human resource names
>
> Use existing safe management transaction engine.
>
> Add a distinct action type such as:
>
> RESOURCE_POLICY_UPDATE
>
> No real write in this task.
>
> Do not weaken:
>
> wallet binding
> write lease
> late-hash handling
> session identity
> crash recovery
> receipt linkage
> fresh-time checks.
>
> ============================================================
> PHASE 8 — RESOURCE-AWARE HOLDER CHALLENGE
> ============================================================
>
> Inspect the CURRENT holder challenge schema.
>
> Known SEND_CHALLENGE payload length has historically been 104 bytes.
>
> Do NOT change protocol/version unnecessarily.
>
> If the current challenge already contains:
>
> credential/resource/nonce/expiry
>
> reuse it exactly and make resource semantics real.
>
> If it does NOT:
>
> design the smallest backwards-compatible v2 necessary.
>
> The holder proof MUST be cryptographically bound to:
>
> selected credential identity
> resourceId
> fresh random nonce
> challenge expiry
>
> plus current domain/chain binding already present.
>
> A proof for:
>
> Front Door
>
> MUST NOT be valid for:
>
> Lab
>
> or:
>
> Server Room.
>
> A proof for credential A MUST NOT validate as credential B.
>
> ============================================================
> ANDROID CHALLENGE VALIDATION
> ============================================================
>
> Before signing:
>
> Android must verify:
>
> - challenge protocol/domain/version;
> - challenge credential == latched selected credential;
> - challenge credential node/namehash exact;
> - resourceId is a known configured LockENS resource;
> - challenge freshness/expiry;
> - chain expected;
> - any existing anti-replay/session checks.
>
> Do not trust arbitrary reader-provided credential substitution.
>
> Whether the selected pass currently permits that resource is NOT an Android
> authorization decision.
>
> The authoritative Gate/ENS policy decides ALLOW/DENY.
>
> Android may sign a valid challenge for a resource that ultimately denies access.
>
> ============================================================
> PHASE 9 — HOLDER SIGNING
> ============================================================
>
> Reuse current holder-proof signing infrastructure.
>
> No new signature scheme unless proven necessary.
>
> Bind signing to the latched active wallet/provider.
>
> A wallet switch during an NFC session:
>
> must not cause signing with a different wallet.
>
> Either complete against the latched wallet or fail closed according to the
> existing provider/session model.
>
> No raw private keys.
>
> No secrets.
>
> ============================================================
> PHASE 10 — FINAL NODE VERIFICATION
> ============================================================
>
> After receiving proof:
>
> perform authoritative final verification.
>
> At minimum:
>
> 1. challenge fresh;
> 2. nonce not replayed;
> 3. challenge resource == latched virtual gate resource;
> 4. challenge credential == discovered credential;
> 5. recovered signer == CURRENT credential owner;
> 6. credential still REGISTERED;
> 7. registration not expired;
> 8. resolver == S1;
> 9. provenance exact;
> 10. access.v1 valid/active/current;
> 11. resources.v1 valid;
> 12. current resource ∈ resources.v1.
>
> Use a coherent fresh block/snapshot where supported.
>
> Preserve existing fail-closed freshness protections.
>
> Re-read current owner/policy after proof so a TOCTOU ownership/policy change does
> not silently ALLOW using stale pre-challenge state.
>
> ============================================================
> PHASE 11 — VIRTUAL GATES
> ============================================================
>
> The same physical:
>
> ESP32
> +
> PN532
> +
> serial bridge
>
> must simulate multiple logical doors.
>
> Implement gate profiles:
>
> Front Door
> Lab
> Server Room
>
> The active virtual gate defines the resourceId used in the next challenge.
>
> Changing gate profile:
>
> - permitted while idle;
> - must not mutate an already-issued challenge;
> - current session latches resourceId;
> - changing the UI during an active session affects only the NEXT session, or is
>   temporarily disabled.
>
> Do not require three PN532 readers.
>
> Make clear in UI/docs:
>
> one physical reference verifier
> simulating multiple logical resources.
>
> ============================================================
> PHASE 12 — GATE MONITOR
> ============================================================
>
> Build or extend the existing monitor using the smallest existing project stack.
>
> Do NOT add a large frontend framework.
>
> Prefer the existing Node/UI mechanism if one exists.
>
> Required UI:
>
> LOCKENS GATE MONITOR
>
> Virtual Gate
> [ Front Door | Lab | Server Room ]
>
> Current resource:
> Lab
>
> Status:
>
> Waiting for pass
>
> then:
>
> Credential identified
> staff-001.keys.demo-access.eth
>
> Holder
> Verified / Failed
>
> Registration
> Valid / Expired / Invalid
>
> Global access
> Allowed / Suspended / Expired
>
> Resource policy
> Lab ✓ / ✕
>
> Proof
> Fresh / Replay / Invalid
>
> Controller
> Confirmed / Not confirmed
>
> Final:
>
> ACCESS GRANTED
>
> or:
>
> ACCESS DENIED
> <reason>
>
> ============================================================
> CONTROLLER / ACTUATOR TRUTHFULNESS
> ============================================================
>
> Current ESP32 confirmation is serial/controller confirmation only.
>
> Do NOT claim a physical relay/door actuator.
>
> If ALLOW:
>
> display:
>
> Controller confirmed
> Access granted
>
> An optional door/open animation is allowed ONLY after controller confirmation,
> but label it as the virtual gate state.
>
> README/demo language must remain truthful:
>
> reference physical verifier
> virtual gate
> no real door actuator.
>
> ============================================================
> PHASE 13 — DENY REASONS
> ============================================================
>
> Use stable human + technical categories.
>
> At minimum:
>
> NO_SELECTED_CREDENTIAL
> INVALID_CREDENTIAL_NAME
> WRONG_NAMESPACE
> WRONG_REGISTRY
> WRONG_RESOLVER
> CREDENTIAL_NOT_REGISTERED
> REGISTRATION_EXPIRED
> HOLDER_MISMATCH
> CHALLENGE_EXPIRED
> REPLAY
> INVALID_SIGNATURE
> ACCESS_SUSPENDED
> ACCESS_EXPIRED
> RESOURCE_POLICY_MISSING
> RESOURCE_NOT_ALLOWED
> RPC_UNAVAILABLE
> CONTROLLER_NOT_CONFIRMED
>
> Do not leak provider/RPC secret text.
>
> Normal monitor shows human reason.
>
> Diagnostics may show bounded category.
>
> ============================================================
> PHASE 14 — FIRMWARE / PN532
> ============================================================
>
> Keep ESP32/PN532 responsibilities minimal:
>
> ISO-DEP/APDU transport
> serial framing
> controller confirmation
>
> NO:
>
> ENS reads
> RPC
> policy decisions
> resource-policy parsing
> Ethereum signature validation
>
> Add only the protocol support actually required for GET_CREDENTIAL / dynamic
> challenge transport.
>
> Inspect the current firmware before deciding whether changes are necessary.
>
> Do not flash the device automatically.
>
> Build/compile validation is authorized.
>
> ============================================================
> PHASE 15 — SERIAL PROTOCOL
> ============================================================
>
> Preserve compatibility where practical.
>
> If the serial protocol must add:
>
> credential discovery
> resource/gate information
> new status categories
>
> version it or make extension unambiguous.
>
> Do not break the existing fallback verifier silently.
>
> Add golden fixtures for all new frames.
>
> ============================================================
> PHASE 16 — LEGACY FALLBACK
> ============================================================
>
> The existing guest-001 physical path remains a fallback/regression path.
>
> Do not destroy it.
>
> Dynamic selected-pass mode must be explicit.
>
> If GET_CREDENTIAL is unsupported:
>
> dynamic mode must NOT silently switch to guest-001.
>
> Legacy mode may remain explicitly selectable for regression.
>
> ============================================================
> PHASE 17 — ANDROID PRODUCT UX
> ============================================================
>
> Selected pass in My Keys becomes the credential intended for NFC.
>
> Now that HCE is actually connected, the product may truthfully indicate:
>
> Ready to tap
>
> only when:
>
> - active wallet session available;
> - selected pass exists;
> - selected pass is still freshly owned according to current app state;
> - HCE service ready.
>
> Do not say:
>
> Access guaranteed
>
> because the gate/resource policy may still deny.
>
> If no pass selected:
>
> show:
> Select a pass to use NFC
>
> Do not add broad UI redesign.
>
> ============================================================
> PHASE 18 — RESOURCE DISPLAY IN PASS DETAILS
> ============================================================
>
> Pass details may display authoritative resources.v1 as:
>
> Access to
>
> Front Door
> Lab
> Server Room
>
> For missing resources.v1:
>
> No resource policy configured
>
> Do not imply it can open all doors.
>
> This read remains authoritative from S1.
>
> ============================================================
> PHASE 19 — TEST VECTORS / CROSS-LANGUAGE CONSISTENCY
> ============================================================
>
> Create golden shared vectors for:
>
> resource slug
> resourceId
>
> resources.v1 ABI bytes
>
> credential namehash
>
> challenge bytes
>
> typed-data/signing digest if current design exposes it safely
>
> APDU GET_CREDENTIAL command/response
>
> serial frames
>
> Use fixtures so Android and Node cannot silently disagree.
>
> Do not include private keys/secrets in repo fixtures.
>
> Use deterministic public test addresses/signatures only where already consistent
> with project test policy.
>
> ============================================================
> PHASE 20 — ANDROID TESTS
> ============================================================
>
> Cover at least:
>
> GET_CREDENTIAL exact APDU
>
> no selected pass
>
> wrong AID/version
>
> selected pass response
>
> 64-byte bound
>
> wallet switch before session
>
> wallet switch during latched session
>
> selection change mid-session
>
> challenge credential mismatch
>
> challenge unknown resource
>
> expired challenge
>
> resource IDs
>
> resources.v1 encoding/decoding
>
> template resource defaults
>
> resource editing
>
> Ready to tap state
>
> no ownership => no HCE credential
>
> existing HCE tests remain green
>
> ============================================================
> PHASE 21 — NODE TESTS
> ============================================================
>
> Cover at least:
>
> resource config golden IDs
>
> resources.v1 decode
>
> missing policy DENY
>
> malformed policy DENY
>
> Front allowed
>
> Lab denied
>
> suspended credential DENY
>
> expired access DENY
>
> registration expired DENY
>
> wrong owner
>
> credential direct-child validation
>
> wrong R1
>
> wrong S1
>
> challenge credential mismatch
>
> challenge resource mismatch
>
> resource replay isolation
>
> same proof different resource DENY
>
> same proof same resource replay DENY
>
> owner changes between challenge and final read
>
> policy changes between challenge and final read
>
> RPC unavailable fail closed
>
> controller not confirmed
>
> existing secure timing/replay suite remains green
>
> ============================================================
> PHASE 22 — FIRMWARE TEST / BUILD
> ============================================================
>
> Compile current target firmware.
>
> Run any existing host/native tests.
>
> Validate new APDU/serial fixtures.
>
> No flash.
>
> Return exact binary/build output path if applicable.
>
> ============================================================
> PHASE 23 — END-TO-END SIMULATED MATRIX
> ============================================================
>
> No blockchain write.
>
> Using fixtures/simulated onchain snapshots, demonstrate:
>
> STAFF:
> active
> resources = Front + Lab + Server
>
> LAB
> → ALLOW
>
> VISITOR:
> active
> resources = Front
>
> LAB
> → DENY RESOURCE_NOT_ALLOWED
>
> CONTRACTOR:
> suspended
> resources = Lab
>
> LAB
> → DENY ACCESS_SUSPENDED
>
> Also:
>
> STAFF
> SERVER_ROOM
> → ALLOW
>
> VISITOR
> FRONT_DOOR
> → ALLOW
>
> Replay:
> → DENY
>
> Wrong selected credential:
> → DENY
>
> ============================================================
> PHASE 24 — PHYSICAL READ-ONLY / LEGACY TESTING
> ============================================================
>
> Physical testing is authorized only where it causes:
>
> NO ENS write
> NO wallet transaction
> NO firmware flash unless separately authorized.
>
> You may install Android APK using adb -r.
>
> You may run Node Gate/Monitor.
>
> If CURRENT firmware already supports the necessary dynamic protocol without
> flashing, perform safe physical taps.
>
> If new firmware is required:
>
> STOP before flashing and return exact flash instructions/build artifact.
>
> Do not perform holder signatures during physical testing if the user has not
> explicitly approved that specific physical proof run.
>
> Read-only UI/APDU inspection is allowed.
>
> ============================================================
> NO-TOUCH / SCOPE CONTROL
> ============================================================
>
> Do NOT implement:
>
> Studio Permissions grant/revoke
>
> actual VISITOR transfer
>
> unregister
>
> root governance
>
> new Solidity
>
> network switching
>
> ETH wallet/send/receive
>
> backend/indexer
>
> real lock actuator
>
> QR
>
> holder delegated metadata
>
> unless strictly required by this architecture, which is not expected.
>
> ============================================================
> VALIDATION
> ============================================================
>
> Run all relevant:
>
> Android unit tests
> assembleDebug
>
> Node tests
>
> firmware build/tests
>
> git diff --check
>
> Use actual test counts.
>
> No commit/push of the NEW NFC/gate implementation.
>
> ============================================================
> FINAL ADVERSARIAL SELF-REVIEW
> ============================================================
>
> Before returning:
>
> try to break:
>
> selected-pass/wallet binding
>
> credential discovery trust boundary
>
> resource challenge binding
>
> replay isolation
>
> TOCTOU owner/policy
>
> virtual-gate latching
>
> legacy fallback separation
>
> resources.v1 fail-closed semantics
>
> Studio resource write integration
>
> serial/APDU framing
>
> Do not return PASS merely because tests pass.
>
> ============================================================
> RETURN
> ============================================================
>
> # LOCKENS DYNAMIC NFC + VIRTUAL GATES — IMPLEMENTATION
>
> ## CHECKPOINT BASELINE
>
> Studio checkpoint commit(s):
>
> NFC_BASELINE_SHA:
>
> HEAD/origin synchronized before NFC work:
> YES / NO
>
> ## ARCHITECTURE
>
> ## RESOURCE MODEL
>
> Config:
>
> Resource IDs:
>
> resources.v1:
>
> Missing-policy behavior:
>
> ## STUDIO RESOURCE INTEGRATION
>
> Issue:
>
> Manage:
>
> Writes performed:
> 0
>
> ## ANDROID HCE
>
> AID:
>
> GET_CREDENTIAL:
>
> Selected-pass binding:
>
> Wallet binding:
>
> Challenge validation:
>
> ## HOLDER PROOF
>
> Credential binding:
>
> Resource binding:
>
> Replay:
>
> TTL:
>
> ## PN532 / ESP32
>
> Changes:
>
> Firmware build:
>
> Flash performed:
> NO
>
> ## SERIAL
>
> ## NODE VERIFIER
>
> Credential validation:
>
> Owner validation:
>
> access.v1:
>
> resources.v1:
>
> TOCTOU:
>
> ## VIRTUAL GATES
>
> Front Door:
>
> Lab:
>
> Server Room:
>
> Session latching:
>
> ## GATE MONITOR
>
> ## DENY REASONS
>
> ## TESTS
>
> Android:
>
> Node:
>
> Firmware:
>
> diff check:
>
> ## SIMULATED MATRIX
>
> STAFF → LAB:
>
> VISITOR → LAB:
>
> CONTRACTOR → LAB:
>
> STAFF → SERVER ROOM:
>
> VISITOR → FRONT DOOR:
>
> Replay:
>
> ## PHYSICAL DEVICE
>
> APK installed:
>
> Dynamic physical tap performed:
> YES / NO
>
> Firmware flash required:
> YES / NO
>
> ## SECURITY
>
> Blockchain writes:
> 0
>
> Wallet transactions:
> 0
>
> Secrets:
> 0
>
> Legacy fallback preserved:
> YES / NO
>
> ## FILES
>
> ## GIT
>
> New NFC/gate work:
> UNCOMMITTED
>
> ## VERDICT
>
> DYNAMIC NFC + VIRTUAL GATES:
>
> READY FOR PHYSICAL FLASH / TEST
> STOP
>
> If ready, end exactly:
>
> LOCKENS PHYSICAL ACCESS SYSTEM: READY FOR CONTROLLED PHYSICAL TEST
