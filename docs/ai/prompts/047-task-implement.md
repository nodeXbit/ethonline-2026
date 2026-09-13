# Project task packet 047: TASK — Implement:

- Classification: **ORIGINAL / SANITIZED**
- Redactions applied: none
- Source: retained project-specific Control Tower packet in the local Codex record

---

> TASK
>
> Implement:
>
> BATCH B — DEMO RELIABILITY
>
> Goal:
>
> Turn the already-proven Gate E secure path into a deterministic, understandable
> and recordable local hackathon demo.
>
> This is NOT a protocol-security redesign.
>
> DO NOT perform blockchain writes.
> DO NOT run physical NFC tests.
> DO NOT flash firmware.
> DO NOT commit or push.
> DO NOT deploy publicly.
> DO NOT install dependencies unless a concrete blocker is found; prefer zero new
> dependencies.
>
> TIMEBOX
>
> Aim for approximately 2–3 hours.
>
> Stop instead of expanding into a broad frontend/platform rewrite.
>
> ============================================================
> CURRENT VERIFIED BASELINE
> ============================================================
>
> Expected repository:
>
> HEAD == origin/main ==
> 496ac4577ec479c8e4b284a566d85053d90b841e
>
> Working tree:
> clean.
>
> Verify before editing:
>
> git status -sb
> git rev-parse HEAD
> git rev-parse origin/main
>
> If state differs:
> STOP.
>
> Gate E secure path is already physically validated.
>
> Canonical secure demo credential:
>
> guest-001.demo-access.eth
>
> Owner:
>
> 0x3419148731087b970d2059C53780163B452D5FF7
>
> Physical resource:
>
> demo-access.eth:door-001
>
> Resource bytes32:
>
> 0xf2bde8f2654ee7267a06860eca03e0935d8818b006f5f51c2fce9d56a9b441cd
>
> Current intended validity:
>
> 1793487599
> 2026-10-31 23:59:59 Europe/Madrid
>
> Current onchain state is expected ACTIVE/ALLOW, but do not require/live-query
> that merely to begin coding.
>
> Proven physical behavior:
>
> valid holder + INACTIVE
> → physical DENY
>
> valid holder + ACTIVE
> → physical ALLOW
>
> same consumed proof
> → REPLAYED_CHALLENGE / DENY
>
> Gate E Batch A already provides:
>
> - 50s post-challenge attempt deadline
> - 8s ENS verification budget
> - 2s controller-confirmation timeout
> - block freshness
> - optional whole-snapshot RPC fallback
> - bounded serial input
> - firmware GATE_E STOP handling
> - proof log redaction
> - physical controller confirmation
>
> PRESERVE ALL OF THIS.
>
> ============================================================
> FIRST — INSPECT EXISTING DEMO
> ============================================================
>
> Before editing, read completely the current:
>
> - package.json
> - demo/server.mjs
> - demo frontend HTML/CSS/JS and related files
> - Gate E bridge CLI
> - Gate E config
> - relevant ENS access helpers
> - .gitignore
> - current demo-related tests
> - current local config/example conventions
>
> Determine:
>
> 1. why the browser demo currently targets cred-001;
> 2. how activate/deactivate currently select the credential;
> 3. what /api/credential actually represents;
> 4. how server-side DEV signing remains protected;
> 5. how Gate E currently exposes sanitized terminal outcomes;
> 6. the smallest safe way to show Gate E attempt evidence in the browser;
> 7. whether a runtime-file convention already exists.
>
> Do not inspect secret env file contents.
>
> ============================================================
> BATCH B SCOPE
> ============================================================
>
> Implement only these four coherent pieces:
>
> A. one canonical demo configuration;
> B. deterministic pre-demo health check;
> C. sanitized Gate E attempt evidence;
> D. local demo UI alignment.
>
> No public deployment yet.
> No README rewrite yet.
> No physical rehearsal yet.
>
> ============================================================
> A — CANONICAL DEMO CONFIG
> ============================================================
>
> PROBLEM
>
> The historical browser demo uses cred-001 while the secure Gate E flow uses
> guest-001.
>
> Create the smallest demo-specific shared configuration so demo UI/server,
> preflight and Gate E demo commands cannot silently drift.
>
> Do NOT change generic persistent-access defaults from cred-001 merely to achieve
> this.
>
> Canonical demo values:
>
> credential:
> guest-001.demo-access.eth
>
> credential label:
> guest-001
>
> expected owner:
> 0x3419148731087b970d2059C53780163B452D5FF7
>
> resource human id:
> demo-access.eth:door-001
>
> resource bytes32:
> 0xf2bde8f2654ee7267a06860eca03e0935d8818b006f5f51c2fce9d56a9b441cd
>
> chain:
> Sepolia 11155111
>
> expected resolver:
> 0x0B723c0C2170F2ea508F2f4e3e122C6c0782744C
>
> Do not put secrets in this config.
>
> Prefer a small module under an appropriate demo/security path.
>
> Tests must prove the server/preflight use guest-001 and not cred-001.
>
> ============================================================
> B — TWO-MINUTE PRE-DEMO HEALTH CHECK
> ============================================================
>
> Add one obvious operator command:
>
> npm run demo:preflight
>
> It must be READ-ONLY.
>
> It must NEVER:
>
> - sign an Ethereum transaction
> - broadcast
> - activate/deactivate/renew
> - issue a Gate A access challenge
> - request a Privy signature
> - perform an NFC authorization attempt
>
> Goal:
>
> catch predictable demo failures before the tap.
>
> Automate all host/onchain checks that are safely machine-checkable.
>
> At minimum check:
>
> REPOSITORY / LOCAL CONFIG
>
> - required local config exists without printing contents
> - expected application/demo configuration loads
> - no need to require git clean for ordinary operation, but report current
>   source revision if straightforward
>
> SERIAL
>
> - enumerate serial devices
> - find the established CH343 device by VID/PID/descriptor where possible
> - report actual COM port rather than assuming COM4
> - require exactly one intended device when safely identifiable
> - detect if serial port cannot be opened because another process owns it
> - open/close safely without sending Gate E protocol data
>
> Do not reset hardware automatically.
>
> PN532 / FIRMWARE
>
> There is no reliable host-side PN532 check without observing a boot.
>
> Implement the smallest useful readiness mode.
>
> Preferred approach:
>
> demo:preflight may wait for / observe the committed firmware readiness lines if
> the controller has just been cold-booted:
>
> I2C 0x24 ACK
> PN532 firmware 1.6
> GATE_E_READY
> PRESENT_SEEKER
>
> If the current firmware output cannot be observed without a fresh boot, report:
>
> HARDWARE READINESS:
> MANUAL COLD BOOT REQUIRED
>
> and give the exact runbook rather than fabricating PASS.
>
> Do NOT add new firmware behavior for preflight unless absolutely necessary.
>
> ONCHAIN / RPC
>
> Use Batch A rules.
>
> Require:
>
> - primary RPC available
> - fallback status reported as CONFIGURED / NOT CONFIGURED, never URL
> - chain ID 11155111
> - current block passes freshness
> - guest REGISTERED
> - guest owner == expected Privy EOA
> - resolver expected
> - registry expiry valid
> - access record valid
> - validUntil has comfortable margin
>
> Define a demo safety margin.
>
> Preferred:
>
> VALIDITY_MINIMUM_REMAINING_SECONDS = 86400
>
> because the current configured deadline is much longer.
>
> If less than 24h remains:
> PRECHECK FAIL.
>
> Report current:
>
> POLICY:
> ACTIVE / INACTIVE
>
> Do NOT require policy to be ACTIVE because the demo may deliberately start
> INACTIVE.
>
> NONCE / WRITES
>
> Report:
>
> DEV latest
> DEV pending
>
> Require latest == pending if demo writes are planned.
>
> The health check itself performs zero writes.
>
> ANDROID / PRIVY
>
> Host cannot reliably introspect the current Seeker Privy session without ADB /
> new infrastructure.
>
> Do NOT fake automation.
>
> Print a compact MANUAL PHONE CHECK:
>
> - Seeker unlocked
> - NFC ON
> - Internet available
> - ENSv2 Access Demo open
> - Privy authenticated
> - wallet shown/known as expected address
>
> If the existing Android app already exposes the current wallet visibly, reuse
> that fact in the runbook.
>
> Do not modify Android solely for preflight unless the wallet/current-login
> state is genuinely not visible and a tiny UI-only change is justified.
>
> Avoid Android changes by default.
>
> OUTPUT
>
> Return a compact PASS/FAIL report suitable for running immediately before
> recording.
>
> Require total automated runtime under approximately 120 seconds in healthy
> conditions.
>
> Final result:
>
> DEMO PREFLIGHT: PASS
>
> or:
>
> DEMO PREFLIGHT: STOP — <reason>
>
> Also write a sanitized local report if useful for the UI.
>
> No secrets, proof, nonce challenge or RPC URL.
>
> ============================================================
> C — SANITIZED LAST GATE E ATTEMPT
> ============================================================
>
> PROBLEM
>
> The browser currently cannot distinguish the physical verifier/controller
> result from the onchain policy.
>
> Add the smallest cross-process local evidence mechanism.
>
> Preferred default if no better existing mechanism exists:
>
> an atomically-written JSON file under an ignored runtime directory, for example:
>
> .runtime/gate-e-last-attempt.json
>
> Add runtime directory to .gitignore.
>
> Do NOT add a database.
>
> Do NOT make Gate E authorization depend on the UI or status file.
>
> Reporting failure must never change ALLOW/DENY semantics.
>
> The bridge should best-effort record sanitized attempt state.
>
> Useful fields may include:
>
> schemaVersion
> attemptId
> startedAt
> completedAt
> credential
> resource
> transportResult
> holderResult
> recoveredSigner
> currentEnsOwner
> ownerMatch
> verifierResult
> verifierReason
> snapshotBlock
> controllerDecision
> controllerConfirmation
> replayResult
>
> Public wallet addresses and block numbers are acceptable.
>
> NEVER STORE:
>
> - raw signature/proof
> - raw PROOF= line
> - nonce
> - private key
> - RPC URL
> - OTP
> - Privy token
> - App Secret
> - full typed-data payload
>
> At the beginning of a new bridge attempt, stale previous success must not appear
> as current.
>
> Prefer writing:
>
> state = IN_PROGRESS
>
> or clearing the previous terminal result before challenge issuance.
>
> Terminal examples must preserve distinctions:
>
> TRANSPORT_FAILURE
>
> VERIFIER_DENY
>
> VERIFIER_ALLOW
>
> CONTROLLER_CONFIRMED
>
> CONTROLLER_UNCONFIRMED
>
> A verifier ALLOW with controller timeout must visibly remain:
>
> VERIFIER_ALLOW
> +
> CONTROLLER_UNCONFIRMED
>
> not physical success.
>
> A firmware transport failure must not become policy DENY.
>
> The runtime write must be:
>
> - local only
> - ignored by Git
> - atomic enough to avoid partial JSON reads
> - best-effort / non-authoritative
>
> Tests must inspect report objects and prove forbidden fields are never present.
>
> ============================================================
> D — LOCAL DEMO UI
> ============================================================
>
> Keep:
>
> 127.0.0.1 local-only security boundary.
>
> Do NOT expose 0.0.0.0.
> Do NOT add CORS.
> Do NOT turn the privileged demo server into a public service.
>
> Align the local UI with the secure demo credential:
>
> guest-001.demo-access.eth
>
> The existing activate/deactivate controls should operate guest-001 when used
> through this demo.
>
> Preserve:
>
> - server-side private key only
> - exact Host/Origin protections
> - idempotent writes
> - nonce guards
> - post-submit recovery
> - no optimistic success UI
> - current transaction safety
>
> Do not weaken those protections.
>
> VISUAL MODEL
>
> The main page must clearly separate THREE concepts.
>
> 1. ONCHAIN POLICY
>
> Display:
>
> credential
> owner
> ACTIVE / INACTIVE
> validUntil
> policy result
>
> Label it explicitly as:
>
> ENSv2 POLICY
>
> Do not label ACTIVE as “door open”.
>
> 2. LAST HOLDER VERIFICATION
>
> Display:
>
> NOT RUN
> ALLOW
> DENY
> TRANSPORT FAILURE
> IN PROGRESS
>
> As applicable.
>
> Show:
>
> recovered signer
> owner match
> reason
> snapshot block
>
> when available.
>
> Label:
>
> HOLDER VERIFIER
>
> 3. PHYSICAL CONTROLLER
>
> Display separately:
>
> NOT RUN
> CONFIRMED ALLOW
> CONFIRMED DENY
> UNCONFIRMED
> TRANSPORT FAILURE
>
> Label:
>
> PHYSICAL CONTROLLER
>
> A verifier ALLOW must NOT make controller green unless controller confirmation
> exists.
>
> REPLAY
>
> If last attempt performed replay checking, display succinctly:
>
> REPLAY:
> DENIED / NOT CHECKED
>
> Do not display proof bytes.
>
> PREFLIGHT
>
> If a sanitized last preflight report is available, expose a small:
>
> SYSTEM READINESS:
> PASS / FAIL / NOT RUN
>
> with timestamp.
>
> Do not build a complex dashboard.
>
> DESIGN
>
> Preserve vanilla HTML/CSS/JS and existing visual style unless a modest cleanup
> improves demo readability.
>
> Optimize for a 2–4 minute recorded demo:
>
> large readable states
> clear ACTIVE/INACTIVE
> clear ALLOW/DENY
> clear controller confirmation
>
> Avoid developer-jargon overload in the primary visible cards.
>
> Detailed technical metadata can be secondary/smaller.
>
> ============================================================
> DEMO OPERATOR COMMANDS
> ============================================================
>
> After inspection, add only genuinely useful aliases.
>
> Preferred shape if compatible:
>
> npm run demo
> npm run demo:preflight
> npm run demo:bridge
>
> Optionally:
>
> npm run demo:activate
> npm run demo:deactivate
>
> only if these safely reuse the exact existing transaction paths and make the
> rehearsal materially simpler.
>
> Do NOT create duplicate ENS write implementations.
>
> No command may embed private configuration.
>
> ============================================================
> COLD-BOOT RUNBOOK
> ============================================================
>
> Add a concise dedicated runbook, for example:
>
> DEMO_RUNBOOK.md
>
> This is allowed in Batch B.
>
> Keep it operational, not an architecture essay.
>
> Exact startup sequence:
>
> 1. Seeker away from PN532.
> 2. Disconnect all controller USB/power.
> 3. Verify CH343 disappears.
> 4. Wait approximately 10 seconds.
> 5. Reconnect only established CH343 path.
> 6. If passive boot output is absent, one short RST/EN press; never BOOT unless
>    actual upload recovery requires it.
> 7. Require:
>    I2C 0x24 ACK
>    PN532 1.6
>    GATE_E_READY
>    PRESENT_SEEKER.
> 8. Run demo preflight.
> 9. Close any competing serial monitor.
> 10. Start demo bridge.
> 11. Present Seeker only when explicitly requested.
> 12. Hold steady through controller result.
>
> Include STOP conditions.
>
> Also include a short recovery rule:
>
> If physical attempt fails:
> - remove phone
> - stop bridge
> - do not reuse proof/challenge
> - perform full cold boot
> - preflight again
> - start a new attempt
>
> No “retry until works”.
>
> ============================================================
> TESTS
> ============================================================
>
> Add deterministic tests for:
>
> CANONICAL CONFIG
>
> - demo uses guest-001
> - expected owner/resource correct
> - historical cred-001 defaults outside demo remain unchanged
>
> PREFLIGHT
>
> - healthy chain/credential
> - INACTIVE is acceptable
> - ACTIVE is acceptable
> - wrong owner fails
> - wrong resolver fails
> - expired registry fails
> - near-expiry access validity fails
> - stale block fails
> - nonce mismatch fails when writes planned
> - fallback status does not leak URLs
> - missing CH343 fails or clearly reports hardware unavailable
> - busy serial port fails
> - zero blockchain writes
> - no challenge issuance
>
> ATTEMPT REPORT
>
> - IN_PROGRESS clears stale success
> - physical ALLOW requires controller confirmed
> - verifier ALLOW + controller timeout remains unconfirmed
> - policy DENY vs transport failure stay distinct
> - replay denial represented correctly
> - no signature/proof/nonce/secret fields
> - malformed/partial report fails safely in UI/server
>
> DEMO SERVER/UI
>
> - /api/credential uses guest-001
> - activate/deactivate target guest only
> - Host/Origin protections unchanged
> - attempt endpoint never returns forbidden fields
> - policy/verifier/controller remain distinct
> - no CORS
> - local bind unchanged
>
> All existing tests must remain green.
>
> ============================================================
> VALIDATION
> ============================================================
>
> Run:
>
> node --test --test-isolation=none
>
> Report total.
>
> Run any targeted demo tests separately.
>
> Android source should preferably remain unchanged.
>
> If Android unchanged, normal regression:
>
> :app:testDebugUnitTest
> :app:assembleDebug
>
> Require existing 21/21 + build PASS.
>
> Firmware should remain unchanged during Batch B.
>
> Confirm current Gate E firmware source unchanged from the Gate E checkpoint.
>
> Start the local demo server and manually smoke-test in browser:
>
> - page loads
> - guest-001 shown
> - onchain policy shown
> - holder verifier separate
> - controller separate
> - no raw proof
> - activate/deactivate buttons visually ready but DO NOT click them during this
>   implementation task
> - refresh preserves correct sanitized last-attempt semantics if runtime data
>   exists
>
> Do NOT generate fake successful physical evidence for the production UI.
>
> For UI smoke testing, deterministic TEST data may be injected only through test
> fixtures or explicit dev/test mode that cannot be confused with live evidence.
>
> Do not leave fake runtime evidence after validation.
>
> Also require:
>
> git diff --check
> secret/config scan
> runtime files ignored
> no real proofs/signatures
> zero blockchain writes
> zero NFC attempts
> zero firmware flashes
>
> ============================================================
> NO COMMIT / NO PUSH
> ============================================================
>
> Do not commit.
> Do not push.
>
> Do not update:
>
> STATUS.md
> WORKLOG.md
> DECISIONS.md
> PROJECT.md
> README.md
>
> during this implementation task.
>
> DEMO_RUNBOOK.md is part of Batch B implementation and may be created.
>
> ============================================================
> STOP IF
> ============================================================
>
> STOP rather than expand scope if:
>
> - server would need to become public
> - Gate E auth must depend on UI/runtime status reporting
> - a DB/event bus/new service becomes necessary
> - Android protocol changes appear necessary
> - firmware/APDU changes appear necessary
> - generic cred-001 behavior must be broken
> - demo activate/deactivate would bypass existing safe transaction handling
> - raw proof/signature would need persistence
> - new authentication/security infrastructure becomes necessary
>
> ============================================================
> RETURN
> ============================================================
>
> # BATCH B — DEMO RELIABILITY
>
> ## BASELINE
>
> ## FILES CHANGED
>
> Explain each file and why.
>
> ## CANONICAL DEMO CONFIG
>
> Confirm:
>
> guest-001.demo-access.eth
>
> and that cred-001 reference behavior remains intact.
>
> ## DEMO PREFLIGHT
>
> Include:
>
> exact command
> automated checks
> manual phone checks
> healthy runtime target
> zero-write guarantee
>
> ## ATTEMPT EVIDENCE
>
> Include exact sanitized schema.
>
> Confirm forbidden fields.
>
> ## DEMO UI
>
> Describe:
>
> ENSv2 POLICY
> HOLDER VERIFIER
> PHYSICAL CONTROLLER
> REPLAY
> SYSTEM READINESS
>
> ## LOCAL SECURITY BOUNDARY
>
> Confirm loopback-only and existing mutation protections preserved.
>
> ## DEMO COMMANDS
>
> ## COLD-BOOT RUNBOOK
>
> ## TESTS
>
> ## NODE VALIDATION
>
> ## ANDROID VALIDATION
>
> ## FIRMWARE REGRESSION
>
> ## MANUAL BROWSER SMOKE TEST
>
> ## SECURITY
>
> Confirm:
>
> - zero blockchain writes
> - zero NFC attempts
> - zero hardware flashes
> - no proof/signature persisted
> - no secrets
> - no CORS
> - loopback only
> - Gate A/APDU/firmware authorization unchanged
> - UID unused
>
> ## GIT STATE
>
> No commit/push.
>
> ## REHEARSAL PLAN — DO NOT EXECUTE
>
> Design the exact shortest controlled rehearsal that validates the same flow we
> will record.
>
> Preferred narrative:
>
> initial policy INACTIVE
> → fresh physical holder proof
> → DENY
>
> activate guest through the local demo control
> → confirmed receipt/readback
>
> fresh physical proof
> → ALLOW
> → controller confirmed
>
> same proof replay
> → DENY
>
> Then optionally return guest to INACTIVE after recording/rehearsal.
>
> Specify exact number of required blockchain writes and physical taps.
>
> Do not execute them.
>
> ## NEXT
>
> Review/checkpoint Batch B before live rehearsal.
>
> End exactly:
>
> BATCH B: PASS
>
> or
>
> BATCH B: STOP — <exact reason>
