# Project task packet 057: TASK — Handle ONLY the exposed Sepolia RPC credential before TAP 2.

- Classification: **ORIGINAL / SANITIZED**
- Redactions applied: none
- Source: retained project-specific Control Tower packet in the local Codex record

---

> TASK
>
> Handle ONLY the exposed Sepolia RPC credential before TAP 2.
>
> Treat the credential-bearing RPC URL that appeared in diagnostic tool output as
> COMPROMISED.
>
> DO NOT perform TAP 2.
> DO NOT perform NFC.
> DO NOT perform blockchain writes.
> DO NOT activate/deactivate/renew.
> DO NOT flash/reset hardware.
> DO NOT print the old or new RPC URL.
> DO NOT print API keys/tokens.
> DO NOT put a secret in a shell command line.
> DO NOT commit/push yet.
> DO NOT use Computer Use.
>
> CURRENT AUTHORITATIVE REHEARSAL STATE
>
> guest-001.demo-access.eth:
>
> REGISTERED
> active=true
> validUntil=1793487599
> policy=ALLOW
>
> DEV:
> 24/24
>
> Confirmed rehearsal writes:
> 2
>
> Physical taps:
> 1
>
> TAP 1:
> physical CONFIRMED DENY
>
> TAP 2:
> NOT RUN
>
> Repository expected clean at:
>
> 1b1317e65106bb173d1eedc19ae32fcc34513651
>
> ============================================================
> PHASE 1 — SAFE SECRET LOCATION / PROVIDER IDENTIFICATION
> ============================================================
>
> Read-only inspect HOW RPC configuration is loaded.
>
> Determine:
>
> - exact environment variable name used for primary Sepolia RPC;
> - optional fallback variable name;
> - exact local ignored config file/location supplying it, if applicable;
> - whether that file is ignored by Git;
> - provider HOSTNAME only.
>
> CRITICAL:
>
> NEVER print:
>
> - full RPC URL
> - URL path
> - URL query
> - API key
> - token
> - username/password
> - secret fragment
>
> If provider identification requires parsing the current URL, parse locally and
> return ONLY hostname, e.g.:
>
> provider.example.com
>
> Do not echo the source string.
>
> Also determine safely whether fallback:
>
> - is not configured;
> - uses the same credential/provider;
> - or is independent.
>
> Return only:
>
> PRIMARY PROVIDER HOSTNAME:
> <hostname>
>
> PRIMARY LOCAL CONFIG LOCATION:
> <path / environment only>
>
> FALLBACK:
> NOT CONFIGURED / SAME PROVIDER / INDEPENDENT
>
> Do not reveal values.
>
> ============================================================
> PHASE 2 — IDENTIFY THE LEAK BOUNDARY
> ============================================================
>
> Read-only inspect the exact command/path that produced the unsanitized RPC error
> during activation verification.
>
> Also inspect relevant committed error handling in:
>
> - demo server
> - demo preflight
> - Gate E bridge
> - ENS/RPC helper paths directly involved
>
> Determine whether the credential-bearing URL was exposed by:
>
> A. an ad-hoc diagnostic/tool command used during this rehearsal;
>
> or
>
> B. committed application/demo code that can emit raw RPC errors containing the
> URL.
>
> Do NOT deliberately trigger another real RPC failure with the current secret.
>
> If testing logging behavior is necessary, use dummy/non-secret placeholder URLs
> only.
>
> Classify:
>
> LEAK SOURCE:
> AD-HOC DIAGNOSTIC ONLY
>
> or
>
> LEAK SOURCE:
> COMMITTED PROJECT ERROR PATH
>
> or
>
> LEAK SOURCE:
> UNCERTAIN
>
> If COMMITTED PROJECT ERROR PATH or UNCERTAIN:
>
> DO NOT use the replacement credential in that path yet.
>
> Pause after rotation for a scoped redaction fix.
>
> ============================================================
> PHASE 3 — STOP PROCESSES USING OLD CREDENTIAL
> ============================================================
>
> Identify only project processes that have loaded the current RPC configuration.
>
> The existing demo server may be one.
>
> Gracefully stop ONLY relevant ETHOnline demo Node processes that need a restart
> to load the replacement credential.
>
> Do not blanket-kill node.exe.
>
> Do not touch unrelated processes.
>
> Do not stop hardware power merely for this credential rotation.
>
> ============================================================
> PHASE 4 — PAUSE FOR USER ROTATION
> ============================================================
>
> PAUSE and tell the user:
>
> 1. Open the dashboard for the provider hostname identified in Phase 1.
> 2. Revoke/regenerate/delete the exposed RPC/API credential.
> 3. Create a replacement credential if required.
> 4. DO NOT paste the replacement credential into Codex or ChatGPT.
> 5. Manually edit the exact ignored local config location identified in Phase 1.
> 6. Replace ONLY the primary Sepolia RPC value.
> 7. Save the file.
> 8. Tell Codex only:
>
>    RPC ROTATED AND LOCAL CONFIG UPDATED
>
> Do not ask the user for the secret value.
>
> Do not continue until the user confirms.
>
> ============================================================
> PHASE 5 — LOCAL SECRET SAFETY AFTER USER CONFIRMATION
> ============================================================
>
> After confirmation:
>
> verify WITHOUT PRINTING CONTENTS that:
>
> - required RPC configuration exists;
> - the config file remains ignored/untracked;
> - git status does not expose/stage it;
> - no committed file contains the replacement value;
> - no runtime/log file contains the replacement value.
>
> Do not run:
>
> env
> set
> Get-ChildItem Env:
> cat <secret-file>
> type <secret-file>
>
> or anything else that dumps secrets.
>
> ============================================================
> PHASE 6 — SAFE RPC VALIDATION
> ============================================================
>
> Validate the replacement primary RPC without printing its URL.
>
> Use a purpose-built/safe invocation that:
>
> - loads the value internally;
> - performs READ-ONLY calls only;
> - catches errors without serializing the full error/request/config object;
> - prints only sanitized facts.
>
> Require:
>
> RPC CONNECTION:
> PASS
>
> CHAIN ID:
> 11155111
>
> LATEST BLOCK:
> available
>
> BLOCK FRESHNESS:
> PASS
>
> Then read guest coherently and require:
>
> guest REGISTERED
> expected owner
> expected resolver
> active=true
> validUntil=1793487599
> policy ALLOW
>
> DEV:
> latest == pending == 24
>
> No pending transaction.
>
> Do not broadcast anything.
>
> If validation fails:
>
> print only a sanitized error category/message with all URLs/credentials removed.
>
> ============================================================
> PHASE 7 — DECIDE WHETHER A CODE FIX IS REQUIRED
> ============================================================
>
> If Phase 2 classified:
>
> AD-HOC DIAGNOSTIC ONLY
>
> require:
>
> PROJECT RPC LOGGING FIX REQUIRED:
> NO
>
> Record the unsafe diagnostic pattern so it is not used again.
>
> Do not edit source.
>
> If Phase 2 classified:
>
> COMMITTED PROJECT ERROR PATH
>
> or
>
> UNCERTAIN
>
> require:
>
> PROJECT RPC LOGGING FIX REQUIRED:
> YES
>
> Do NOT proceed to TAP 2.
>
> Return the exact minimal files/error boundary requiring redaction.
>
> Do not implement the fix in this task.
>
> ============================================================
> PHASE 8 — RESTART SAFE LOCAL DEMO
> ============================================================
>
> Only if:
>
> - rotation confirmed;
> - replacement RPC validated;
> - PROJECT RPC LOGGING FIX REQUIRED = NO.
>
> Start exactly one fresh:
>
> npm run demo
>
> using the replacement local configuration.
>
> Require:
>
> 127.0.0.1 only
>
> GET /
> PASS
>
> GET /api/credential
> PASS
>
> guest:
> guest-001.demo-access.eth
>
> policy:
> ACTIVE / ALLOW
>
> Do not click mutation buttons.
>
> Leave one correct demo server running for TAP 2 preparation.
>
> ============================================================
> PHASE 9 — FINAL SECURITY CHECK
> ============================================================
>
> Require:
>
> - old RPC credential revoked/rotated by user
> - replacement never printed
> - replacement not committed
> - config ignored
> - no blockchain writes
> - no NFC
> - guest still ACTIVE/ALLOW
> - DEV nonce 24/24
> - git clean
>
> Do not claim the old credential is revoked unless user explicitly confirmed the
> provider-side rotation step.
>
> ============================================================
> RETURN
> ============================================================
>
> # RPC CREDENTIAL ROTATION
>
> ## PROVIDER
>
> Hostname only:
>
> ## LOCAL CONFIG
>
> Path only:
> Variable name:
>
> No value.
>
> ## LEAK SOURCE
>
> Exactly one:
>
> AD-HOC DIAGNOSTIC ONLY
>
> COMMITTED PROJECT ERROR PATH
>
> UNCERTAIN
>
> Explain the exact boundary without reproducing any URL.
>
> ## PROVIDER ROTATION
>
> User confirmed:
> YES / NO
>
> Old credential revoked/regenerated:
> YES / NO / USER CONFIRMATION ONLY
>
> ## REPLACEMENT SECRET SAFETY
>
> Config ignored:
> Replacement printed:
> Replacement committed:
>
> Require replacement printed = NO
> Require replacement committed = NO
>
> ## SAFE RPC VALIDATION
>
> Connection:
> Chain:
> Block freshness:
> Guest:
> Policy:
> Nonce:
>
> ## PROJECT RPC LOGGING FIX REQUIRED
>
> YES / NO
>
> ## DEMO SERVER
>
> Restarted with replacement credential:
> YES / NO
>
> ## REHEARSAL STATE
>
> guest:
> ACTIVE / ALLOW
>
> validUntil:
> 1793487599
>
> DEV nonce:
> 24/24
>
> writes:
> 2
>
> physical taps:
> 1
>
> ## SECURITY / GIT
>
> ## NEXT
>
> If fix required = NO:
>
> TAP 2 preparation: controlled cold boot/boot-observation preflight followed by
> one ACTIVE physical ALLOW attempt with same-proof replay DENY.
>
> If fix required = YES:
>
> Return to Control Tower for one narrowly scoped RPC-error redaction fix before
> TAP 2.
>
> End exactly:
>
> RPC ROTATION: PASS
>
> or
>
> RPC ROTATION: STOP — <exact reason>
