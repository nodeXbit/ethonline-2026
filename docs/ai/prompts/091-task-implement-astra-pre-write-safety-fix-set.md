# Project task packet 091: TASK — IMPLEMENT ASTRA PRE-WRITE SAFETY FIX SET

- Classification: **ORIGINAL / SANITIZED**
- Redactions applied: none
- Source: retained project-specific Control Tower packet in the local Codex record

---

> TASK — IMPLEMENT ASTRA PRE-WRITE SAFETY FIX SET
>
> CONTEXT
>
> Your adversarial review concluded:
>
> REAL STUDIO WRITES: STOP
>
> The ENSv2 semantics, ABI, templates and live simulations were sound.
>
> The blockers are application coordination / recovery.
>
> You are now authorized to IMPLEMENT the required fixes and selected small
> hardening items.
>
> NO real blockchain writes.
>
> NO signing.
>
> NO real credential creation.
>
> NO management transaction.
>
> NO commit/push yet.
>
> Preserve the existing uncommitted Pass Studio worktree.
>
> ============================================================
> MISSION
> ============================================================
>
> Fix completely:
>
> P0/P1:
> W01
> R01
> I01
> R02
> M01
> T01
> V01
>
> Also fix now because they are small/security-adjacent and touch the same seams:
>
> A01
> D01
> R03
> R04
> S01
>
> Do NOT implement C01 scoped EAC capabilities yet.
>
> Do NOT expand product scope.
>
> ============================================================
> NON-NEGOTIABLE ARCHITECTURE
> ============================================================
>
> Do NOT replace:
>
> RecoverableTransactionEngine
> ContractTransactionRunner
> CredentialReader
> R1/S1
> Privy integration
> active-wallet architecture
> automatic discovery
> Pass Studio product model
>
> Strengthen them.
>
> No weaker parallel transaction path.
>
> ============================================================
> W01 — EXCLUSIVE ACTION / WALLET BINDING
> ============================================================
>
> Problem:
>
> Concurrent actions can re-enable wallet selection while another submission is
> still active, allowing the UI wallet to differ from the captured submitting
> provider.
>
> Fix systematically.
>
> Requirements:
>
> - one exclusive write-action coordinator per app/process;
> - while a write reaches final confirmation/preflight/submission/finalization:
>   - wallet switching disabled;
>   - conflicting Studio management controls disabled;
>   - another action cannot independently call setBusy(false) and unlock them;
> - action owns a unique lease/token/generation;
> - only the action holding that lease may release the busy/write lock;
> - Review remains read-only and need not hold the write lock;
> - final confirmation freezes:
>   - wallet address;
>   - wallet/provider identity;
>   - chain;
>   - credential;
>   - operation intent;
> - immediately before provider send, re-assert active wallet/provider generation
>   still matches the frozen intent;
> - mismatch => STOP before provider send.
>
> Async callbacks must not rely on mutable global active-wallet state.
>
> Tests:
> - overlapping action failure cannot unlock A;
> - wallet switch impossible during A;
> - stale callback cannot release newer action;
> - active wallet changed before final send => no provider invocation.
>
> ============================================================
> R01 — LIVE PROVIDER REQUEST / FALSE NO_BROADCAST
> ============================================================
>
> Problem:
>
> A provider invocation may still be alive while recovery observes unchanged
> nonces and incorrectly marks NO_BROADCAST_PROVEN, permitting a replacement.
>
> Required invariant:
>
> A live in-process provider invocation can NEVER be finalized as
> NO_BROADCAST_PROVEN.
>
> Track provider invocation lifecycle explicitly.
>
> If send has crossed the persisted SUBMITTING_NO_HASH boundary and provider call
> is still outstanding:
>
> state remains submitting/uncertain.
>
> Do not run no-broadcast proof that permits re-arm.
>
> If a late hash returns:
>
> persist it if it matches the active operation, even if reconciliation has run.
>
> Never produce HASH_NOT_EXPECTED merely because an unsafe recovery transitioned
> the operation while provider work was alive.
>
> Across PROCESS RESTART:
>
> there is no live in-memory provider handle.
>
> But unchanged nonces from one observation alone are not sufficient proof that a
> previous send cannot still appear.
>
> Use the strongest practical bounded reconciliation supported by current
> architecture.
>
> Prefer conservative UNKNOWN over unsafe NO_BROADCAST_PROVEN.
>
> Explicit re-arm only after genuinely safe proof.
>
> Tests must reproduce Astra's delayed-provider case and prove:
> - no replacement permitted;
> - late hash retained;
> - exactly one operation/send.
>
> ============================================================
> I01 — IMMUTABLE ISSUANCE SESSION IDENTITY
> ============================================================
>
> Remove correctness dependence on mutable global `activeKey`.
>
> Reads/lookups must be side-effect-free.
>
> Every transition must receive immutable identity sufficient to target exactly
> one session, e.g.:
>
> wallet
> chain
> normalized fullName
> sessionId / operation identity as appropriate
>
> Functions such as:
>
> registerConfirmed
> recordsConfirmed
> ready
> recordsFailed
> etc.
>
> must not mutate "whatever session is currently selected."
>
> Async callbacks carry frozen session identity.
>
> Support explicit enumeration/selection of incomplete sessions.
>
> A callback for A can NEVER advance B.
>
> Wallet-only lookup cannot silently return the last-saved unrelated credential.
>
> Preserve existing historical staff session/journal.
>
> Tests:
> - A callback while B is selected;
> - two incomplete credentials same wallet;
> - wallet switch;
> - restart;
> - TX1 A confirmed while B draft exists.
>
> ============================================================
> R02 — JOURNAL ↔ SESSION CRASH RECOVERY
> ============================================================
>
> Recovery must reconcile the product/session state FROM authoritative journal
> state, not assume the two stores were updated together.
>
> Handle every meaningful crash boundary:
>
> session READY_TO_* vs engine SUBMITTING_NO_HASH
>
> HASH_RECEIVED
>
> CONFIRMING
>
> CONFIRMED
>
> REVERTED
>
> UNKNOWN
>
> NO_BROADCAST_PROVEN
>
> including crash between any two persistence writes.
>
> Requirements:
>
> - idempotent recovery;
> - repeated recovery safe;
> - terminal engine states reflected correctly into issuance session;
> - TX1 revert leads to a usable truthful failed/review/retry path according to
>   safe transaction semantics;
> - TX2 revert invokes equivalent of recordsFailed;
> - NO_BROADCAST_PROVEN does not re-prove itself from an unsupported state;
> - confirmed TX1 never re-registers;
> - confirmed TX2 never reconfigures.
>
> Build a clear reconciliation table in code/tests.
>
> ============================================================
> M01 — MANAGEMENT SESSION PER OPERATION
> ============================================================
>
> Current wallet-only management-session slot is insufficient.
>
> Persist management intent/finalization by stable operation/session ID.
>
> A confirmed-but-not-authoritatively-verified management action remains durable
> until final readback succeeds or reaches a truthful recoverable state.
>
> Operation B must not overwrite A's finalization data.
>
> Clearing/finalizing must be conditional on exact operation ID.
>
> Block new conflicting mutation of the same credential/resource while a
> confirmed operation is awaiting authoritative verification.
>
> If non-conflicting concurrent management is not explicitly designed and proven,
> serialize management writes globally for this sprint.
>
> Prefer simpler safe serialization.
>
> Tests:
> - A confirmed/readback fails;
> - B attempted;
> - restart;
> - A finalizes;
> - no A/B session overwrite.
>
> ============================================================
> T01 — FRESH LIFECYCLE / TIME VALIDATION
> ============================================================
>
> At FINAL confirmation, rebuild validation from fresh chain state and fresh block
> timestamp.
>
> Do not rely on Review-time validity.
>
> For issue configuration and management:
>
> registrationExpiry
> accessValidUntil
> current block time
>
> must be checked again.
>
> Restore:
> - active=true only if accessValidUntil > fresh chain time.
>
> Access validity update:
> - obey fresh current registration expiry;
> - accessValidUntil <= registrationExpiry.
>
> Issue TX2:
> - access validity still future/currently meaningful according to intended
>   semantics at submission;
> - if the user waited until it expired, require re-review/update.
>
> Product state must distinguish:
>
> configured active flag
>
> from:
>
> currently valid access.
>
> Do not label a pass "Allowed" solely because active=true when validUntil has
> expired.
>
> Derive human status from:
> registration validity
> AND active
> AND accessValidUntil.
>
> Use chain/block timestamp where authorization matters.
>
> ============================================================
> V01 — LEGITIMATE VISITOR CHANGES BETWEEN TX1 / TX2
> ============================================================
>
> VISITOR is transferable after TX1.
>
> Do not assume original recipient + original bit-156 state must remain unchanged
> forever before TX2.
>
> After TX1 confirmation, before TX2:
>
> freshly read:
> - current owner;
> - current roles;
> - resolver;
> - expiry;
> - registration state.
>
> If current owner/roles differ from originally reviewed issuance:
>
> DO NOT silently continue.
>
> Present a specific "Credential changed after registration" reconciliation
> state.
>
> Require fresh human review acknowledging current authoritative owner/roles.
>
> The resolver records to be configured remain explicit/frozen/reviewed.
>
> Do NOT automatically modify transferability.
>
> If bit 156 was self-revoked:
> reflect actual non-transferability.
>
> If owner transferred:
> configuration may proceed only after explicit fresh review and all other
> invariants remain valid.
>
> Never re-register.
>
> Tests:
> - VISITOR transfer after TX1;
> - self-revoke bit after TX1;
> - fresh review required;
> - TX2 can safely configure acknowledged current credential;
> - old recipient assumptions never silently persist.
>
> ============================================================
> A01 — ADDRESS INPUT SAFETY
> ============================================================
>
> Ethereum address behavior:
>
> - fully lowercase input: accept if otherwise valid, checksum for display;
> - fully uppercase input: handle according to standard accepted
>   non-checksummed semantics if current libraries support it safely;
> - mixed-case input MUST have a valid checksum;
> - invalid mixed-case checksum => reject.
>
> Never silently "repair" invalid mixed-case input into a different-looking
> checksummed address.
>
> Review shows full exact checksummed recipient.
>
> ============================================================
> D01 — STRICT DATE / DST PARSING
> ============================================================
>
> Remove SMART normalization.
>
> Invalid calendar dates:
> reject.
>
> Madrid DST gap:
> do not silently move time forward.
>
> DST ambiguous overlap:
> require deterministic explicit handling / offset choice or reject ambiguity with
> human guidance.
>
> Normal UI should display:
> local date/time + Europe/Madrid
> and exact UTC in Review details.
>
> Management old → new timestamps should be human-readable, not raw epoch-only.
>
> Keep exact integer seconds internally.
>
> ============================================================
> R03 — RECOVERY AFTER LATER NONCE ADVANCEMENT
> ============================================================
>
> Do not require current wallet latest/pending nonce == preNonce + 1 forever for an
> already-known transaction.
>
> Once hash/original tx is known, validate:
>
> recorded tx nonce
> original tx intent
> receipt
>
> and tolerate later legitimate nonce advancement.
>
> Nonce checks remain useful for no-hash reconciliation, not as a permanent
> receipt validity condition.
>
> Tests:
> operation nonce N confirmed;
> wallet later sends N+1/N+2;
> restart;
> N still recovers/finalizes correctly.
>
> ============================================================
> R04 — RECEIPT IDENTITY LINKAGE
> ============================================================
>
> Strengthen receipt validation.
>
> Require at least:
>
> receipt.transactionHash == recorded hash
>
> receipt status expected
>
> receipt block linkage coherent with fetched original transaction
>
> original transaction:
> chain context
> from
> to
> value
> input/calldata
> nonce
> hash
>
> match the persisted intent/evidence.
>
> Do not accept an unrelated receipt with superficially matching fields.
>
> Handle RPC inconsistency as UNKNOWN / unavailable, not success.
>
> ============================================================
> S01 — SANITIZED ERROR CATEGORIES
> ============================================================
>
> Never derive persisted categories from arbitrary exception text.
>
> Use a finite allowlisted enum/category set.
>
> Persist:
> action
> stage
> allowlisted category
> safe exception class if useful
>
> Normal UI:
> safe actionable messages.
>
> Diagnostics:
> sanitized category/stage/class.
>
> Never persist/log arbitrary provider text that might contain:
> token
> URL credential
> email/OTP
> secret
> signed payload
>
> ============================================================
> COORDINATION DESIGN RULE
> ============================================================
>
> After this fix there should be ONE consistent concept:
>
> WRITE INTENT
>
> with frozen:
>
> operationId/sessionId
> wallet
> provider identity/generation
> chain
> credential
> action
> target
> value
> calldata fingerprint
> reviewed business intent
>
> All async work refers to this identity.
>
> Avoid mutable "current" globals for security-critical completion callbacks.
>
> Do not over-engineer into a new framework; introduce the smallest coherent
> identity object/seams needed.
>
> ============================================================
> EXISTING STAFF REGRESSION
> ============================================================
>
> ZERO blockchain writes.
>
> Fresh read-only verify existing:
>
> staff-001.keys.demo-access.eth
>
> remains unchanged.
>
> Its historical TX1/TX2 journal/recovery must still load.
>
> My Keys/autodiscovery/selected pass unchanged.
>
> ============================================================
> TEST STRATEGY
> ============================================================
>
> Add deterministic tests specifically reproducing every Astra finding.
>
> Mandatory:
>
> W01 overlapping action race
>
> W01 wallet-switch attempt
>
> R01 delayed provider hash
>
> R01 concurrent recovery
>
> I01 A callback mutating B
>
> I01 multiple incomplete sessions
>
> R02 crash boundary matrix
>
> R02 TX1 revert
>
> R02 TX2 revert
>
> R02 NO_BROADCAST recovery
>
> M01 confirmed management + failed readback + B attempt
>
> T01 review before expiry / confirm after expiry
>
> T01 active=true past validity displays not Allowed
>
> V01 transfer after TX1
>
> V01 bit revocation after TX1
>
> A01 invalid mixed-case checksum
>
> D01 Feb 30
>
> D01 DST gap/overlap
>
> R03 later nonce advancement
>
> R04 wrong receipt hash
>
> R04 block/tx mismatch
>
> S01 provider message containing fake secret does not persist it
>
> Existing tests must remain green.
>
> ============================================================
> READ-ONLY LIVE VALIDATION
> ============================================================
>
> After implementation/tests:
>
> repeat the safe synthetic Studio simulations for:
>
> STAFF
> VISITOR
> CONTRACTOR
>
> No broadcast.
>
> Re-read staff-001.
>
> No real management operation.
>
> No signature.
>
> ============================================================
> PHYSICAL DEVICE — NO WRITE
> ============================================================
>
> Build/install with adb -r.
>
> Do not clear data.
>
> Inspect:
>
> Studio
> Review
> wallet switching outside writes
>
> Use test/debug-only mechanisms for asynchronous race validation where needed.
>
> DO NOT press any final write button.
>
> ============================================================
> SECOND INTERNAL ADVERSARIAL PASS
> ============================================================
>
> Before returning PASS:
>
> review your OWN fix diff adversarially.
>
> Specifically verify no fix introduced:
>
> deadlock that permanently locks wallet UI
>
> lost late hash
>
> ability to use stale provider
>
> orphaned sessions
>
> automatic retry
>
> session migration loss
>
> historical staff journal corruption
>
> Do not merely rely on new tests.
>
> ============================================================
> VALIDATION
> ============================================================
>
> Run:
>
> Android tests
>
> assembleDebug
>
> Node tests
>
> git diff --check
>
> Use actual counts.
>
> No commit/push.
>
> ============================================================
> RETURN
> ============================================================
>
> # LOCKENS PASS STUDIO — ASTRA SAFETY FIX
>
> ## ORIGINAL FINDINGS
>
> W01:
> FIXED / NOT FIXED
>
> R01:
> FIXED / NOT FIXED
>
> I01:
> FIXED / NOT FIXED
>
> R02:
> FIXED / NOT FIXED
>
> M01:
> FIXED / NOT FIXED
>
> T01:
> FIXED / NOT FIXED
>
> V01:
> FIXED / NOT FIXED
>
> ## HARDENING
>
> A01:
>
> D01:
>
> R03:
>
> R04:
>
> S01:
>
> ## WRITE IDENTITY MODEL
>
> ## RECOVERY MODEL
>
> Include the journal/session reconciliation table.
>
> ## VISITOR INTERMEDIATE-STATE MODEL
>
> ## TIME / ACCESS MODEL
>
> ## TESTS
>
> New adversarial tests:
>
> Android total:
>
> Node total:
>
> assemble:
>
> diff check:
>
> ## READ-ONLY LIVE SIMULATION
>
> Staff:
>
> Visitor:
>
> Contractor:
>
> Existing staff:
>
> ## PHYSICAL DEVICE
>
> Final write pressed:
> NO
>
> ## SECURITY
>
> Blockchain writes:
> 0
>
> Signatures:
> 0
>
> Secrets:
> 0
>
> ## GIT
>
> No commit/push.
>
> ## VERDICT
>
> REAL STUDIO WRITES:
>
> GO
> GO WITH REMAINING FIXES
> STOP
>
> If GO, end exactly:
>
> ASTRA SAFETY FIX COMPLETE — READY FOR INDEPENDENT RE-AUDIT
