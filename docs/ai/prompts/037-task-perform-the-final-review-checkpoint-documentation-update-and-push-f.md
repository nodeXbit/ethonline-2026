# Project task packet 037: TASK — Perform the final review, checkpoint, documentation update and push for:

- Classification: **ORIGINAL / SANITIZED**
- Redactions applied: none
- Source: retained project-specific Control Tower packet in the local Codex record

---

> TASK
>
> Perform the final review, checkpoint, documentation update and push for:
>
> BATCH A — DETERMINISTIC READINESS
>
> The user explicitly authorizes:
>
> - read/review of the current Batch A diff
> - implementation commit
> - scoped project-state documentation update
> - documentation commit
> - push to origin/main
>
> DO NOT perform blockchain writes.
> DO NOT run physical NFC tests.
> DO NOT renew/activate/deactivate guest-001.
> DO NOT flash hardware.
> DO NOT make unrelated improvements.
>
> CURRENT EXPECTED PUSHED BASE
>
> HEAD == origin/main ==
> a45a7632a98c566cd373980f9860109e50f93a51
>
> EXPECTED LOCAL BATCH A FILES
>
> package.json
> scripts/ensv2/persistent-access.mjs
> scripts/ensv2/access-record.test.mjs
> scripts/security/gate-e-config.mjs
> scripts/security/gate-e-secure-bridge.mjs
> scripts/security/gate-e-secure-bridge.test.mjs
> firmware/pn532_secure_access/pn532_secure_access.ino
>
> No other changed/untracked files expected.
>
> CURRENT REPORTED VALIDATION
>
> Node:
> 134/134 PASS
>
> Gate E:
> 42/42 PASS
>
> Android:
> 21/21 PASS
> assembleDebug PASS
>
> Gate E firmware:
> compile PASS
>
> No blockchain writes.
> No physical NFC attempts.
>
> ============================================================
> PHASE 1 — REPOSITORY CHECK
> ============================================================
>
> Run:
>
> git status -sb
> git rev-parse HEAD
> git rev-parse origin/main
> git diff --check
>
> Require:
>
> HEAD == origin/main ==
> a45a7632a98c566cd373980f9860109e50f93a51
>
> and exactly the expected Batch A files.
>
> If any unexpected state exists:
> STOP.
>
> ============================================================
> PHASE 2 — FULL DIFF REVIEW
> ============================================================
>
> Read every changed/new Batch A file completely.
>
> Review behavior, not style.
>
> Require all of the following.
>
> A — RENEW-INACTIVE
>
> Confirm:
>
> 1. CLI requires explicit credential label.
> 2. CLI requires explicit valid-until.
> 3. --credential-owner is rejected.
> 4. REGISTERED required.
> 5. active=false required.
> 6. ACTIVE refuses with zero write.
> 7. malformed/unregistered refuses.
> 8. desired deadline must be:
>    > pinned block timestamp
>    <= registry expiry.
> 9. sufficient existing deadline is semantic NO-OP.
> 10. write-required path can perform at most ONE setData transaction.
> 11. write preserves active=false.
> 12. owner/token/resource/resolver/registry identity are not altered.
> 13. readback requires exact requested deadline.
>
> CRITICAL PREFLIGHT REVIEW
>
> Prove from control flow that:
>
> --preflight
>
> cannot:
>
> - call writeContract
> - send a transaction
> - increment nonce
> - enter a broadcast/recovery path that assumes a tx hash
>
> It may simulate/read.
>
> If --preflight could broadcast under any branch:
> STOP.
>
> B — DEADLINE
>
> Confirm:
>
> - Gate A cryptographic TTL remains 60s.
> - total Gate E post-challenge deadline = 50s.
> - timer begins only after challenge issuance.
> - ENS verification budget = 8s maximum.
> - controller confirmation = 2s.
> - late proof cannot ALLOW.
> - late ENS result cannot ALLOW.
> - already-computed ALLOW after deadline cannot be sent.
> - exactly one terminal finalization.
> - timers cleaned once.
>
> C — SERIAL / FIRMWARE
>
> Confirm:
>
> - maximum pre-newline buffer = 1024 bytes.
> - overflow poisons the current attempt.
> - suffix after overflow cannot revive protocol parsing.
> - no ALLOW after overflow.
> - proof log content is redacted.
> - firmware STOP contains only public reason/state information.
> - STOP occurs before the ambiguous local AUTHORIZATION: DENY.
> - Node treats STOP as terminal.
> - Node performs no later ENS read/authorization write after STOP when it occurs
>   before those operations.
> - early firmware-local DENY cannot satisfy later Node confirmation.
> - APDU v1 bytes/state behavior unchanged.
>
> D — RPC / ENS
>
> This is CRITICAL.
>
> Confirm that a coherent authorization snapshot is always produced as:
>
> ONE client
> → entire readCredential execution
> → one pinned-block coherent result
>
> If primary fails:
>
> discard complete primary attempt
>
> then:
>
> ONE fallback client
> → entire readCredential execution FROM THE BEGINNING.
>
> Prove no implementation can do:
>
> owner from provider A
> resolver/policy from provider B.
>
> Confirm:
>
> - fallback optional
> - at most primary + one fallback
> - no indefinite retry
> - both URLs never logged
> - total ENS operation remains <= 8s and <= remaining total attempt deadline
> - stale block >60s rejected
> - exact 60s boundary accepted
> - future skew >15s rejected
> - local-clock invalidity fails closed
> - wrong chain fails provider attempt
> - no cached ALLOW.
>
> If snapshot-provider mixing is possible:
> STOP.
>
> ============================================================
> PHASE 3 — REGRESSION VALIDATION
> ============================================================
>
> Run fresh:
>
> node --test --test-isolation=none
>
> Require:
> 134 passed
> 0 failed
>
> Target Gate E suite:
> 42/42 PASS
>
> Android:
>
> :app:testDebugUnitTest
> :app:assembleDebug
>
> Require:
> 21/21 PASS
> assembleDebug PASS
>
> Firmware:
>
> compile:
>
> firmware/pn532_secure_access/pn532_secure_access.ino
>
> Require PASS.
>
> Also confirm:
>
> firmware/pn532_hce_apdu/pn532_hce_apdu.ino
> unchanged from pushed Gate D evidence.
>
> Run:
>
> git diff --check
>
> Scoped secret/config scan.
>
> Require:
>
> - no private key
> - no OTP
> - no Privy secret/config
> - no RPC URLs
> - no real physical proof/signature
> - no blockchain write
> - no NFC test
> - Android source unchanged
> - APDU v1 unchanged
> - Gate A source/semantics unchanged
> - cred-001 established defaults/behavior preserved.
>
> ============================================================
> PHASE 4 — IMPLEMENTATION COMMIT
> ============================================================
>
> Stage ONLY the seven Batch A implementation files:
>
> package.json
> scripts/ensv2/persistent-access.mjs
> scripts/ensv2/access-record.test.mjs
> scripts/security/gate-e-config.mjs
> scripts/security/gate-e-secure-bridge.mjs
> scripts/security/gate-e-secure-bridge.test.mjs
> firmware/pn532_secure_access/pn532_secure_access.ino
>
> Commit:
>
> feat: harden Gate E deterministic readiness
>
> Do not include project-state docs.
>
> ============================================================
> PHASE 5 — DOCUMENTATION
> ============================================================
>
> Update only:
>
> STATUS.md
> WORKLOG.md
> DECISIONS.md
>
> Update PROJECT.md only if an existing architecture/state statement is now
> factually stale.
>
> Do NOT update README yet.
>
> STATUS.md
>
> Record:
>
> BATCH A — DETERMINISTIC READINESS: PASS
>
> Implemented:
>
> - explicit scoped renew-inactive tooling
> - no-op when validity already sufficient
> - ACTIVE renewal refusal
> - 50s post-challenge total attempt deadline
> - 8s complete ENS verification budget
> - 2s controller confirmation deadline
> - 1024-byte serial input bound
> - immediate firmware terminal STOP handling
> - physical proof logging redacted
> - maximum block age 60s
> - maximum future skew 15s
> - optional whole-snapshot RPC fallback
> - no provider mixing inside a coherent readCredential snapshot
>
> Validation:
>
> - Node 134/134
> - Gate E 42/42
> - Android 21/21 + assemble PASS
> - Gate E firmware compile PASS
>
> State clearly:
>
> NO live guest renewal has been executed yet.
>
> Current next action:
>
> read-only renewal preflight for guest-001, then explicit Control Tower
> authorization for at most one resolver setData write.
>
> Also state:
>
> Gate E INACTIVE physical = PASS
> Gate E ACTIVE physical = PENDING.
>
> WORKLOG.md
>
> Record concise Batch A implementation evidence and key lessons:
>
> - administrative validity extension is explicit rather than hidden inside
>   activate/deactivate;
> - total attempt lifetime is shorter than Gate A TTL;
> - late ALLOW cannot escape after attempt deadline;
> - firmware-local failure terminates Node immediately;
> - serial memory is bounded;
> - RPC recovery restarts the full coherent snapshot on a different provider
>   rather than mixing provider reads.
>
> DECISIONS.md
>
> Record accepted architectural decisions:
>
> - keep Gate A 60s challenge TTL;
> - Gate E post-challenge budget 50s;
> - ENS budget 8s;
> - firmware confirmation 2s;
> - block age <=60s;
> - future skew <=15s;
> - 1024-byte serial line/buffer bound;
> - explicit renew-inactive operation;
> - activation/deactivation semantics remain unchanged;
> - optional fallback restarts whole snapshot;
> - no transparent per-call multi-provider fallback;
> - firmware STOP is public terminal telemetry only;
> - no APDU/session-ID/PN532 redesign.
>
> Do NOT claim live ACTIVE/ALLOW.
>
> ============================================================
> PHASE 6 — DOCS COMMIT + PUSH
> ============================================================
>
> Before docs commit:
>
> - inspect diff
> - git diff --check
> - no secrets/config
> - implementation files unchanged after implementation commit
>
> Commit docs:
>
> docs: record deterministic readiness hardening
>
> Push both commits to origin/main.
>
> ============================================================
> PHASE 7 — FINAL
> ============================================================
>
> Run:
>
> git status -sb
> git rev-parse HEAD
> git rev-parse origin/main
>
> Require:
>
> working tree clean
> HEAD == origin/main
>
> No blockchain write.
>
> RETURN
>
> # BATCH A CHECKPOINT
>
> ## REVIEW
> ## PREFLIGHT SAFETY REVIEW
> ## SNAPSHOT COHERENCE REVIEW
> ## NODE TESTS
> ## ANDROID TESTS
> ## FIRMWARE BUILD
> ## IMPLEMENTATION COMMIT
> ## DOCUMENTATION COMMIT
> ## PUSH
> ## SECURITY
> ## FINAL GIT STATE
> ## NEXT OBJECTIVE
>
> Include:
>
> - implementation commit SHA
> - documentation commit SHA
> - pushed range
>
> Confirm:
>
> - --preflight cannot broadcast
> - whole-snapshot fallback only
> - late ALLOW impossible after total deadline
> - Node 134/134
> - Gate E 42/42
> - Android 21/21
> - Gate E compile PASS
> - APDU v1 unchanged
> - Gate A unchanged
> - Gate D unchanged
> - zero blockchain writes
> - zero physical NFC attempts
> - main == origin/main
>
> NEXT OBJECTIVE:
>
> Read-only guest-001 renewal preflight.
>
> End exactly:
>
> BATCH A CHECKPOINT: PASS
>
> or
>
> BATCH A CHECKPOINT: STOP — <reason>
