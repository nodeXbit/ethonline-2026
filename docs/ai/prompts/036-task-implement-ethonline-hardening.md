# Project task packet 036: TASK — Implement ETHOnline hardening:

- Classification: **ORIGINAL / SANITIZED**
- Redactions applied: none
- Source: retained project-specific Control Tower packet in the local Codex record

---

> TASK
>
> Implement ETHOnline hardening:
>
> BATCH A — DETERMINISTIC READINESS
>
> This is one scoped implementation batch designed from the completed system
> audit and reconciled local handoff.
>
> DO NOT perform blockchain writes.
> DO NOT run physical NFC tests.
> DO NOT activate/deactivate/renew guest-001 onchain.
> DO NOT commit.
> DO NOT push.
> DO NOT install dependencies.
>
> TIMEBOX
>
> Prefer approximately 2–3 hours maximum.
>
> If any requested item requires a broad architecture rewrite, STOP and report
> rather than expanding scope.
>
> CURRENT PUSHED BASELINE
>
> Expected:
>
> HEAD == origin/main ==
> a45a7632a98c566cd373980f9860109e50f93a51
>
> Expected working tree:
> clean
>
> First run:
>
> git status -sb
> git rev-parse HEAD
> git rev-parse origin/main
>
> Require exact synchronization.
>
> If dirty/ahead/behind/divergent:
> STOP.
>
> CURRENT VERIFIED BASELINE
>
> Gate E INACTIVE physical validation is already PASS and checkpointed.
>
> Secure credential:
>
> guest-001.demo-access.eth
>
> Owner:
>
> 0x3419148731087b970d2059C53780163B452D5FF7
>
> Current access:
>
> active = false
>
> Current access.validUntil:
>
> 1789107864
>
> 2026-09-11 06:24:24 UTC
> 2026-09-11 08:24:24 Europe/Madrid
>
> Current tooling cannot extend this existing REGISTERED credential while
> preserving INACTIVE.
>
> Current regression baseline:
>
> Node:
> 104/104 PASS
>
> Gate E:
> 20/20 PASS
>
> Android:
> 21/21 PASS
> assembleDebug PASS
>
> Gate E firmware:
> compile PASS
>
> CURRENT STRONG COMPONENTS — PRESERVE
>
> - Gate A cryptographic semantics
> - IssuedChallengeStore
> - EIP-712 schema
> - challenge consumption ordering
> - replay behavior
> - current-owner comparison
> - existing isAuthorized
> - existing coherent pinned-block readCredential
> - APDU v1
> - Android Privy signer
> - Gate D firmware
> - existing physical resource ID
> - existing UserRegistry / PermissionedResolver architecture
>
> NO-TOUCH / DO NOT BUILD
>
> Do NOT:
>
> - replace PN532 library
> - move I2C to SPI
> - add PC/SC
> - add Redis
> - add database
> - add MQTT
> - add Docker/VPS
> - redesign APDU
> - add APDU chunking
> - add smart-wallet support
> - change Android
> - add session-ID protocol rewrite
> - add payment/Privy bounty work
> - expose secrets
>
> BATCH A HAS EXACTLY FOUR WORKSTREAMS:
>
> A. INACTIVE validity renewal
> B. deterministic attempt lifecycle
> C. serial input / terminal hardening
> D. RPC timeout / freshness / optional fallback
>
> Inspect current implementation first and keep changes minimal.
>
> ============================================================
> A — SAFE INACTIVE VALIDITY RENEWAL
> ============================================================
>
> PROBLEM
>
> Current behavior:
>
> - setup preserves a REGISTERED access record even if its deadline is unsuitable;
> - activate preserves validUntil while still future;
> - deactivate preserves validUntil;
> - there is no explicit safe way to extend validUntil while remaining INACTIVE.
>
> Do NOT change those established semantics implicitly.
>
> IMPLEMENT
>
> Add an explicit scoped operation for renewing the access validity of a
> REGISTERED credential while preserving:
>
> active == false
>
> Preferred user-facing command:
>
> npm run ensv2:access:renew-inactive -- \
>   --credential-label <label> \
>   --valid-until <unix-seconds>
>
> Follow existing CLI conventions if slightly different naming fits better.
>
> REQUIREMENTS
>
> The command/function must:
>
> 1. require an explicit credential label;
> 2. read the credential coherently using existing helpers;
> 3. require status REGISTERED;
> 4. require current access record decodes correctly;
> 5. require current access.active == false;
> 6. refuse to operate on ACTIVE access;
> 7. accept an explicit uint64 future validUntil;
> 8. require desired validUntil > current pinned block timestamp;
> 9. require desired validUntil <= registry expiry;
> 10. preserve:
>     - credential owner
>     - registry identity
>     - resolver
>     - active=false;
> 11. target only that credential node;
> 12. use the existing verified resolver;
> 13. reuse existing role/provenance checks;
> 14. reuse transaction simulation / nonce / pending guards / recovery conventions
>     already established in the ENS tooling;
> 15. perform semantic no-op if:
>     current active=false
>     AND current validUntil >= requested validUntil;
> 16. require at most ONE resolver setData write when a write is genuinely needed;
> 17. authoritative readback must prove:
>     active=false
>     validUntil == requested value.
>
> DO NOT execute live mode in this task.
>
> Do not touch cred-001.
>
> TESTS
>
> At minimum:
>
> - expired INACTIVE → renewal plan writes active=false + requested deadline
> - future but too-short INACTIVE → renewal required
> - already sufficient INACTIVE → no-op
> - ACTIVE → refuse, zero write
> - malformed record → refuse
> - AVAILABLE/unregistered → refuse
> - requested deadline in past → refuse
> - requested deadline above registry expiry → refuse
> - wrong credential remains untouched
> - transaction failure/recovery follows existing fail-safe patterns where
>   applicable
>
> Use deterministic fixtures, no blockchain writes.
>
> IMPORTANT
>
> Do not hard-code the eventual demo date into generic logic.
>
> The later live operation will choose the explicit horizon separately.
>
> ============================================================
> B — DETERMINISTIC ATTEMPT LIFECYCLE
> ============================================================
>
> PROBLEM
>
> Current system has useful individual timeouts but no single bounded
> post-challenge attempt budget.
>
> The critical property is:
>
> Once a fresh Gate A challenge has been issued, no late verifier result may
> eventually become a physical ALLOW outside the permitted attempt window.
>
> IMPLEMENT
>
> Create centralized Gate E timing constants/config rather than scattered magic
> numbers.
>
> Preferred initial defaults:
>
> CHALLENGE TTL:
> existing Gate A 60 seconds — unchanged.
>
> POST-CHALLENGE TOTAL DEADLINE:
> 50 seconds maximum.
>
> ENS/RPC verification budget:
> 8 seconds maximum.
>
> Firmware authorization confirmation:
> existing 2 seconds — preserve unless implementation evidence requires tiny
> adjustment.
>
> Existing proof timeout may remain <= the total attempt deadline.
>
> The total deadline begins when the Gate A challenge is issued, NOT while the
> operator is merely waiting to present the phone.
>
> REQUIREMENTS
>
> - before challenge issuance, normal controlled user presentation may wait;
> - once challenge exists, start one total deadline;
> - every later phase must fit inside remaining budget;
> - if deadline expires:
>   - no ALLOW;
>   - fail closed;
>   - terminate bridge cleanly;
>   - never issue another challenge automatically;
>   - never resend authorization;
> - late ENS resolution after deadline must be ignored;
> - late proof after deadline must be ignored;
> - late firmware confirmation after terminal timeout cannot convert failure into
>   success;
> - timers cleaned up exactly once on terminal state;
> - no overlapping completion paths.
>
> Do NOT modify Gate A's 60-second cryptographic TTL.
>
> TESTS
>
> Prove:
>
> - healthy flow fits deadline;
> - proof arrives after total deadline → no verification ALLOW;
> - ENS read resolves after deadline → no ALLOW;
> - ALLOW result computed after deadline → not sent;
> - timers do not cause double finalization;
> - replay behavior remains unchanged.
>
> Use fake clocks/timers where possible.
> Do not add slow real sleeps to tests.
>
> ============================================================
> C — SERIAL INPUT + FIRMWARE TERMINAL HARDENING
> ============================================================
>
> PROBLEM 1
>
> The Node pre-newline serial buffer is not explicitly bounded.
>
> IMPLEMENT
>
> Add a conservative application limit.
>
> Choose the smallest safe constant based on actual protocol line sizes.
>
> Expected legitimate machine messages are far below 1 KiB.
>
> A 1024-byte line/buffer limit is preferred unless current protocol evidence
> requires another value.
>
> REQUIREMENTS
>
> - if buffered input exceeds maximum before newline:
>   terminal fail-closed;
> - clear/bound memory;
> - never interpret a suffix as a fresh valid command after overflow;
> - never ALLOW;
> - no second challenge;
> - clean serial close.
>
> Test:
> - exact max acceptable behavior
> - over max
> - no newline flood
> - oversized PROOF
> - valid line after overflow cannot revive same attempt.
>
> PROBLEM 2
>
> Firmware can fail locally before producing PROOF, print a local DENY, and halt.
> Node currently may only discover this later through proof timeout.
>
> We need an explicit terminal failure indication.
>
> FIRST inspect the existing Gate E firmware outputs.
>
> Prefer reusing an existing unambiguous machine-readable terminal line if one
> already exists.
>
> Only if current output is not unambiguous enough, make the SMALLEST firmware
> protocol addition:
>
> GATE_E_STOP=<PUBLIC_REASON>
>
> Examples only if they match actual failure paths:
>
> PN532_INIT
> TARGET_ACTIVATION
> SELECT
> CHALLENGE
> ISO_DEP
> STATUS_TIMEOUT
> SIGNATURE
> AUTHORIZATION_TIMEOUT
> SERIAL_INPUT
>
> Do NOT expose secrets/signatures.
>
> If adding GATE_E_STOP:
>
> - emit exactly once on local terminal failure;
> - emit before/with existing human-readable DENY output;
> - leave existing fail-closed behavior intact;
> - do not change APDU bytes or NFC state semantics.
>
> NODE
>
> When a valid GATE_E_STOP is received at any non-terminal phase:
>
> - terminate attempt immediately;
> - classify origin as firmware/local transport failure;
> - do NOT wait for proof timeout;
> - do NOT perform ENS verification;
> - do NOT send AUTHORIZATION=DENY back to a firmware process that already
>   terminally failed;
> - close cleanly.
>
> Unknown stop reasons:
> fail closed as UNKNOWN_FIRMWARE_STOP, not ignore.
>
> An early firmware-local:
>
> AUTHORIZATION: DENY
>
> must NEVER be misclassified as confirmation of a later Node authorization.
>
> TESTS
>
> Prove:
>
> - local STOP before challenge
> - local STOP after challenge
> - local STOP while waiting proof
> - local STOP during verification
> - no ENS read after pre-verification STOP
> - no authorization write after STOP
> - exact one terminal completion
> - early AUTHORIZATION: DENY still cannot confirm a later command
> - unknown reason fails closed
> - existing happy DENY/ALLOW confirmation behavior preserved.
>
> If this requires a firmware edit:
>
> compile Gate E firmware after the change.
>
> Gate D firmware must remain unchanged.
>
> ============================================================
> D — RPC / ENS TIMEOUT, FRESHNESS, OPTIONAL FALLBACK
> ============================================================
>
> PRESERVE
>
> readCredential's current coherent single-block snapshot semantics.
>
> Do NOT rewrite the ENS reader.
>
> PROBLEM
>
> Gate E currently lacks explicit application-level:
> - ENS read timeout budget
> - block-age validation
> - optional second-provider recovery
>
> IMPLEMENT
>
> 1. RPC / ENS TIMEOUT
>
> A complete Gate E ENS snapshot used for authorization must finish within the
> remaining attempt budget and in all cases within the configured ENS budget
> (default 8 seconds).
>
> Timeout:
> fail closed.
> Never ALLOW from a promise that resolves later.
>
> 2. BLOCK FRESHNESS
>
> After obtaining the coherent snapshot, validate its pinned block timestamp.
>
> Default proposed limits:
>
> MAX_BLOCK_AGE_SECONDS = 60
> MAX_FUTURE_SKEW_SECONDS = 15
>
> Use the Node system clock only for this sanity/freshness check.
>
> If local clock makes the comparison invalid:
> fail closed with an explicit reason rather than weakening the check.
>
> Reject:
>
> - snapshot older than max age
> - snapshot implausibly in future beyond skew
> - missing/invalid timestamp.
>
> Do not alter isAuthorized.
>
> 3. OPTIONAL FALLBACK RPC
>
> Support one optional second Sepolia RPC configuration without requiring it.
>
> Preferred safe env/config convention:
>
> SEPOLIA_RPC_URL
> SEPOLIA_RPC_FALLBACK_URL
>
> Follow current repository configuration patterns.
>
> Never print either URL.
>
> If fallback is not configured:
> current primary-only behavior remains available and failures fail closed.
>
> CRITICAL SNAPSHOT RULE
>
> Do NOT use viem fallback in a way that can mix providers within one coherent
> snapshot.
>
> Correct behavior:
>
> Attempt primary:
>
> one client
> → one entire readCredential snapshot
>
> If that complete snapshot fails/times out BEFORE authorization:
>
> discard it.
>
> Then, if fallback is configured and sufficient attempt time remains:
>
> create/use fallback client
> → execute the ENTIRE readCredential operation again from the beginning.
>
> The successfully selected snapshot must come entirely from one provider.
>
> Do not switch provider after individual owner/resolver/policy calls.
>
> Do not retry indefinitely.
>
> At most:
>
> PRIMARY attempt
> +
> ONE FALLBACK attempt
>
> and both together remain inside the total attempt deadline.
>
> Wrong chain from either provider:
> that provider attempt fails.
>
> Freshness failure:
> may try the other provider once if configured and time remains.
>
> If both fail:
> DENY / terminal failure.
> Never cached ALLOW.
>
> TESTS
>
> At minimum:
>
> - healthy primary snapshot → one provider only
> - primary timeout → fallback entire snapshot
> - primary stale → fallback entire snapshot
> - primary wrong chain → fallback if configured
> - primary partially fails → discard and restart whole fallback snapshot
> - fallback absent → fail closed
> - fallback also fails → fail closed
> - snapshot calls never alternate providers internally
> - old block rejected
> - future block rejected
> - fresh block accepted
> - total attempt deadline still dominates RPC fallback
> - current-owner and access semantics remain existing Gate A behavior.
>
> Do not introduce quorum/consensus infrastructure.
>
> ============================================================
> CROSS-CUTTING FAIL-CLOSED RESULT MODEL
> ============================================================
>
> Do not redesign the public/demo UX yet.
>
> Internally preserve/distinguish at least:
>
> TRANSPORT_FAILURE
>
> VERIFIER_ALLOW
> VERIFIER_DENY
>
> CONTROLLER_CONFIRMED
> CONTROLLER_UNCONFIRMED
>
> A firmware transport failure must not be reported as an ENS policy DENY.
>
> A verifier ALLOW without controller confirmation must not be reported as a
> physically applied ALLOW.
>
> Keep changes scoped to existing result/error structures where possible.
>
> Do not build an event bus.
>
> ============================================================
> SECURITY / REGRESSION REQUIREMENTS
> ============================================================
>
> Gate A behavior must remain exactly:
>
> valid holder
> → consume challenge
> → apply ENS policy.
>
> INACTIVE valid holder:
> DENY + consumed.
>
> wrong holder:
> DENY + not consumed.
>
> replay:
> DENY.
>
> UID:
> never authorization input.
>
> No caching of ALLOW.
>
> No private keys, OTPs, Privy secrets or RPC credentials in:
> - source
> - tests
> - logs
> - returned report.
>
> Do not open/read secret env files.
>
> No live signature fixtures.
>
> No blockchain writes.
>
> ============================================================
> VALIDATION
> ============================================================
>
> Run all relevant tests.
>
> Require all previous tests remain green.
>
> Node:
>
> node --test --test-isolation=none
>
> Report new total.
>
> Gate E targeted tests:
> all PASS.
>
> Android:
>
> No Android source changes expected.
>
> Run:
> :app:testDebugUnitTest
> :app:assembleDebug
>
> Require:
> 21/21 PASS
> assembleDebug PASS.
>
> Firmware:
>
> If Gate E firmware changed:
> compile Gate E firmware using existing ESP32-S3 / Elechouse stack.
>
> Always confirm:
> Gate D firmware source unchanged.
>
> Run:
>
> git diff --check
>
> Perform scoped secret/config scan.
>
> Confirm:
> - no real proof/signature added
> - no env/local config staged
> - no blockchain writes
> - no physical NFC attempt
> - APDU v1 unchanged
> - Android unchanged
> - cred-001 tooling/default semantics unchanged
> - existing Gate E INACTIVE behavior preserved by tests.
>
> ============================================================
> NO COMMIT / NO PUSH
> ============================================================
>
> Do not commit.
> Do not push.
> Do not update STATUS/WORKLOG/DECISIONS/PROJECT/README yet.
>
> This task ends with local implementation and validation only.
>
> ============================================================
> STOP IF
> ============================================================
>
> STOP instead of expanding scope if:
>
> - renewal cannot be added without changing established activate/deactivate
>   semantics broadly;
> - readCredential cannot preserve a one-provider coherent snapshot;
> - RPC fallback would require mixing providers within one snapshot;
> - firmware terminal handling requires APDU changes;
> - Android changes become necessary;
> - session-ID/protocol redesign appears necessary;
> - a new dependency/service is required;
> - a private credential must be exposed;
> - code change starts becoming a large architectural refactor.
>
> ============================================================
> RETURN
> ============================================================
>
> # BATCH A — DETERMINISTIC READINESS
>
> ## BASELINE
>
> ## FILES CHANGED
>
> For every file explain WHY.
>
> ## A — INACTIVE VALIDITY RENEWAL
>
> Include:
> - exact proposed CLI
> - invariants
> - no-op behavior
> - maximum writes when later executed
> - tests
> - confirm NO write executed now
>
> ## B — ATTEMPT DEADLINES
>
> Include:
> - exact timing constants
> - when total timer starts
> - terminal behavior
> - late-result behavior
>
> ## C — SERIAL / FIRMWARE TERMINALS
>
> Include:
> - serial max size
> - whether firmware required modification
> - exact STOP protocol if added
> - behavior on local failure
> - happy-path confirmation preservation
>
> ## D — RPC / ENS
>
> Include:
> - timeout
> - freshness rules
> - fallback configuration
> - proof that one snapshot never mixes providers
>
> ## FAIL-CLOSED MODEL
>
> ## TESTS
>
> List new important deterministic cases.
>
> ## NODE VALIDATION
>
> ## ANDROID VALIDATION
>
> ## FIRMWARE VALIDATION
>
> ## SECURITY
>
> Explicitly confirm:
>
> - zero blockchain writes
> - zero physical NFC attempts
> - no secrets/signatures
> - APDU v1 unchanged
> - Gate A unchanged
> - readCredential coherence preserved
> - UID unused
> - cred-001 untouched
>
> ## GIT STATE
>
> No commit/push.
>
> ## LIVE MIGRATION PLAN — DO NOT EXECUTE
>
> Give the exact later read-only/preflight sequence for safely renewing:
>
> guest-001.demo-access.eth
>
> while keeping:
>
> active=false
>
> Do NOT select/send an onchain horizon automatically.
>
> Explain the parameters we must explicitly choose in Control Tower.
>
> ## NEXT
>
> State:
>
> Review/checkpoint Batch A, then perform a read-only renewal preflight before
> authorizing any guest-001 write.
>
> End exactly:
>
> BATCH A: PASS
>
> or
>
> BATCH A: STOP — <exact reason>
