# Project task packet 080: TASK — FORENSIC READ-ONLY RECOVERY AFTER FIRST STAFF CREATE ATTEMPT

- Classification: **ORIGINAL / SANITIZED**
- Redactions applied: none
- Source: retained project-specific Control Tower packet in the local Codex record

---

> TASK — FORENSIC READ-ONLY RECOVERY AFTER FIRST STAFF CREATE ATTEMPT
>
> DO NOT IMPLEMENT ANYTHING YET.
>
> DO NOT perform any blockchain write.
>
> DO NOT sign anything.
>
> DO NOT press Resume setup.
>
> DO NOT retry registration.
>
> The user physically pressed the FINAL "Create credential" once.
>
> Observed physical result:
>
> Top-level UI error:
>
> Operation failed
> (IllegalArgumentException).
>
> But the issuance UI simultaneously advanced to:
>
> Credential created · Setup incomplete
> 2 of 3 · Ready to configure
>
> and now exposes:
>
> Resume setup
>
> This strongly suggests registration may have succeeded and a later local step
> failed, but DO NOT assume this.
>
> Determine the exact persisted and onchain truth.
>
> ============================================================
> PRIMARY OBJECTIVE
> ============================================================
>
> Answer with authoritative evidence:
>
> 1. Was an R1.register transaction broadcast?
>
> 2. If yes:
>    - exact tx hash
>    - receipt status
>    - block
>    - transaction nonce
>    - gas
>
> 3. Does staff-001.keys.demo-access.eth currently exist onchain?
>
> 4. If it exists, verify exact:
>    - status
>    - owner
>    - resolver
>    - subregistry
>    - registry expiry
>    - owner roles / transferability
>
> 5. Did any S1 configuration transaction get broadcast?
>
> 6. What is the issuer latest/pending nonce now?
>
> 7. What exact local journal / issuance state is persisted?
>
> 8. What exact IllegalArgumentException occurred and where?
>
> 9. What is the safe next action?
>
> ZERO writes.
>
> ============================================================
> EXPECTED PRE-ATTEMPT STATE
> ============================================================
>
> Issuer:
>
> 0xFa90e8301A22833B74378C5fA3a7c120Ac512685
>
> Pre-attempt issuer nonce:
>
> 1 / 1
>
> Target:
>
> staff-001.keys.demo-access.eth
>
> Expected holder:
>
> 0x3419148731087b970d2059C53780163B452D5FF7
>
> R1:
>
> 0x0AeB395be893149c0b60D9DAC3Ba55139D0dcA1a
>
> S1:
>
> 0x20766FB21498a99350F922ee3163F5a95F354a7f
>
> Expected expiry:
>
> 1793487599
>
> Expected role bitmap:
>
> 0
>
> Artwork intended for the real credential:
>
> BLANK / NOT SET
>
> Description intended:
>
> Staff Access Pass
>
> Initial access intended:
>
> Allowed
>
> ============================================================
> PHASE 1 — PRESERVE STATE
> ============================================================
>
> Do not clear app data.
>
> Do not uninstall.
>
> Do not mutate SharedPreferences/files/journal.
>
> Do not re-arm anything.
>
> Do not press Resume setup.
>
> Use ADB read-only inspection only.
>
> Confirm current worktree state but DO NOT modify it.
>
> ============================================================
> PHASE 2 — LOCAL PERSISTED STATE
> ============================================================
>
> Use safe read-only ADB / run-as inspection if supported by the debug build.
>
> Inspect the application persistence relevant to:
>
> IssuanceCoordinator/session
>
> RecoverableTransactionEngine journal
>
> credential draft/session
>
> Find:
>
> current issuance state
>
> registration operation ID
>
> registration tx hash if present
>
> registration transaction state
>
> records/config operation ID if any
>
> records tx hash if any
>
> pre/post nonces
>
> error category/message/stage
>
> timestamps
>
> Do not print auth tokens or other sensitive app data.
>
> If persistence is binary or unsafe to dump broadly, inspect only through existing
> safe app/debug helpers or targeted files.
>
> ============================================================
> PHASE 3 — FRESH ONCHAIN READBACK
> ============================================================
>
> Using existing safe read-only Sepolia infrastructure:
>
> verify chain 11155111
>
> read issuer:
>
> latest nonce
> pending nonce
> balance
>
> Read staff-001 from R1.
>
> If REGISTERED, verify:
>
> owner ==
> 0x3419148731087b970d2059C53780163B452D5FF7
>
> resolver ==
> S1
>
> subregistry ==
> zero address
>
> expiry ==
> 1793487599
>
> owner roles ==
> 0
>
> Read current S1 records for staff:
>
> description
>
> avatar
>
> access.v1
>
> Distinguish:
>
> not set
>
> from:
>
> set but malformed
>
> from:
>
> configured correctly
>
> ============================================================
> PHASE 4 — TRANSACTION EVIDENCE
> ============================================================
>
> If a registration hash exists:
>
> read original transaction and receipt.
>
> Require:
>
> from == issuer
>
> to == R1
>
> decode input and verify exact intended register call.
>
> Report:
>
> hash
> nonce
> block
> receipt status
> gas used
>
> If issuer nonce advanced from 1 to 2 but journal hash is missing, investigate
> carefully and locate the transaction through the strongest safe evidence
> available.
>
> If issuer nonce is 3 or higher, investigate whether an unexpected second write
> occurred.
>
> Do not guess.
>
> ============================================================
> PHASE 5 — DID TX2 OCCUR?
> ============================================================
>
> Determine authoritatively whether any S1 write was broadcast after registration.
>
> Evidence may include:
>
> journal
>
> issuer nonce
>
> transaction history available from known hashes
>
> S1 records
>
> Do not infer only from the UI step number.
>
> Return:
>
> TX2 BROADCAST:
> YES / NO / UNKNOWN
>
> If YES, fully reconcile it.
>
> ============================================================
> PHASE 6 — ROOT CAUSE ILLEGALARGUMENTEXCEPTION
> ============================================================
>
> Use:
>
> ADB logcat if the relevant exception remains available
>
> source inspection
>
> persisted error data
>
> to identify the exact throw site.
>
> Return:
>
> exception message if safely available
>
> source file
>
> function
>
> line or approximate location
>
> stage in workflow
>
> whether it occurred:
>
> before TX1 broadcast
> during TX1
> after TX1 receipt
> during transition to configuration
> during TX2 preflight
> during UI rendering
> elsewhere
>
> Explain why the UI can simultaneously show:
>
> Operation failed (IllegalArgumentException)
>
> and:
>
> Credential created · Setup incomplete
> Ready to configure
>
> Do not fix it yet.
>
> ============================================================
> PHASE 7 — OTHER OBSERVED PRODUCT FACTS
>
> READ-ONLY CONFIRMATION ONLY
> ============================================================
>
> While inspecting current code, confirm briefly:
>
> A. Credential creation fields:
> Which are currently hard-coded/fixed and which are editable?
>
> B. Artwork:
> - Is the URI written to S1 when provided?
> - Does My Keys currently fetch/render the actual remote artwork?
> - Or does it always use the deterministic placeholder?
>
> C. App startup session:
> Investigate the reported behavior:
>
> On cold/restart the Login screen appears briefly, then the authenticated product
> screen returns.
>
> Determine whether the app renders an unauthenticated state before Privy session
> hydration completes.
>
> Do not fix any of these in this task.
>
> ============================================================
> RETURN
> ============================================================
>
> # STAFF FIRST CREATE — FORENSIC REPORT
>
> ## LOCAL STATE
>
> Issuance state:
>
> Registration operation:
>
> Registration hash:
>
> Registration tx state:
>
> Records operation:
>
> Records hash:
>
> Persisted error:
>
> ## ISSUER ONCHAIN
>
> Latest nonce:
>
> Pending nonce:
>
> Balance:
>
> ## STAFF ONCHAIN
>
> Status:
>
> Owner:
>
> Resolver:
>
> Subregistry:
>
> Expiry:
>
> Owner roles:
>
> ## TX1 — REGISTRATION
>
> Broadcast:
> YES / NO / UNKNOWN
>
> Hash:
>
> Nonce:
>
> Block:
>
> Receipt:
>
> Gas:
>
> Decoded intent:
>
> TX1 VERDICT:
> CONFIRMED / REVERTED / NO BROADCAST / UNKNOWN
>
> ## TX2 — CONFIGURATION
>
> Broadcast:
> YES / NO / UNKNOWN
>
> Hash:
>
> Receipt:
>
> Current description:
>
> Current avatar:
>
> Current access.v1:
>
> ## ILLEGALARGUMENTEXCEPTION
>
> Exact origin:
>
> Stage:
>
> Root cause:
>
> Why UI reached step 2:
>
> ## PRODUCT FACT CONFIRMATION
>
> Fixed creation fields:
>
> Editable fields:
>
> Artwork persistence:
>
> Artwork rendering:
>
> Startup login flash cause:
>
> ## SAFE NEXT ACTION
>
> Do NOT execute it.
>
> State exactly one:
>
> A. Fix local post-TX1 bug, then Resume setup / TX2 only.
>
> B. Reconcile an unknown existing transaction.
>
> C. Registration never broadcast; repair then prepare a fresh safe registration.
>
> D. Another condition — explain.
>
> ## SECURITY
>
> Writes during investigation:
> 0
>
> Signatures:
> 0
>
> Secrets exposed:
> 0
>
> End exactly:
>
> FORENSIC RECOVERY COMPLETE — NO NEW WRITE PERFORMED
