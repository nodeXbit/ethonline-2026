# Project task packet 023: TASK — Diagnose ONLY the Gate D GET_SIGNATURE physical failure.

- Classification: **ORIGINAL / SANITIZED**
- Redactions applied: none
- Source: retained project-specific Control Tower packet in the local Codex record

---

> TASK
>
> Diagnose ONLY the Gate D GET_SIGNATURE physical failure.
>
> Do not implement Gate E.
> Do not change APDU v1.
> Do not add chunking.
> Do not switch PN532 libraries.
> Do not commit or push.
>
> CURRENT VERIFIED PHYSICAL PATH
>
> A real Seeker + ESP32-S3 + Elechouse PN532 session reached:
>
> TARGET ACTIVATION: PASS
> SELECT: PASS
> SEND_CHALLENGE: PASS
> STATUS: PROCESSING
> STATUS: READY
> GET_SIGNATURE: FAIL
>
> GET_SIGNATURE request:
>
> 80 30 01 00
>
> Expected Android APDU response:
>
> 65 signature bytes
> + 90 00
>
> Total:
> 67 bytes
>
> Firmware caller response buffer:
> 68 bytes
>
> The Elechouse PN532 inDataExchange() call returned false.
>
> No signature was obtained.
> No second SEND_CHALLENGE occurred.
>
> GOAL
>
> Determine exactly which boundary failed:
>
> 1. PN532 did not transmit GET_SIGNATURE to Android
> 2. Android HostApduService received it but processor rejected it
> 3. Android produced the correct 67-byte response but PN532/HAL failed to
>    receive/return it
> 4. RF/ISO-DEP link deactivated during the exchange
> 5. another specific evidenced cause
>
> Do not guess.
>
> CURRENT REPO
>
> Expected:
>
> HEAD == origin/main ==
> 5cb7d3561492afeb5b9477b32ab9dd34688df7d6
>
> Expected existing uncommitted work:
>
> - firmware/pn532_hce_apdu/
> - temporary Android HCE diagnostic edit
>
> Preserve both.
>
> PHASE 1 — EXACT LOCAL ELECHOUSE SOURCE AUDIT
>
> Before editing anything, inspect the exact installed local source used by the
> firmware:
>
> - PN532.h
> - PN532.cpp
> - PN532_I2C.h
> - PN532_I2C.cpp
>
> Trace the exact implementation of:
>
> PN532::inDataExchange(...)
> → HAL writeCommand(...)
> → HAL readResponse(...)
>
> For the installed code, document every condition that causes
> inDataExchange() to return false.
>
> Resolve specifically:
>
> - internal PN532 packet-buffer size
> - caller response-buffer semantics
> - whether readResponse first receives into an internal buffer
> - maximum internal response size
> - getResponseLength/readResponse behavior
> - I2C request length for a 67-byte APDU response
> - exact negative/error return values available from the HAL
> - whether PN532 status/MI bits are handled
> - whether the current boolean API hides an actionable error
>
> Re-evaluate the earlier claim that the response is guaranteed to fit.
>
> The earlier feasibility calculation is NOT authoritative if the actual
> inDataExchange/readResponse implementation reveals another internal limit.
>
> Do not edit the installed library.
>
> PHASE 2 — ANDROID-SIDE TRANSPORT TELEMETRY
>
> Because adb/logcat is unavailable, extend the EXISTING temporary Android
> diagnostics minimally.
>
> Do not alter HCE behavior or APDU responses.
>
> Record application-scoped, non-sensitive diagnostic state for the most recent
> physical HCE session:
>
> - last APDU command received:
>   SELECT / SEND_CHALLENGE / GET_STATUS / GET_SIGNATURE / OTHER
> - GET_SIGNATURE received:
>   YES / NO
> - processor state when GET_SIGNATURE was received:
>   IDLE / PROCESSING / READY / ERROR
> - response produced by HostApduService:
>   response byte length only
> - response trailing status word:
>   e.g. 9000 / other
> - HCE deactivation callback observed:
>   YES / NO
> - Android deactivation reason if Android exposes a safe integer/enum
> - session/generation identifier if already available and non-sensitive
>
> Do NOT display:
> - signature bytes
> - Privy identifiers
> - OTP/tokens
> - wallet secrets
> - App ID/Client ID
> - private key.
>
> The diagnostic must not affect timing or mutate protocol state beyond normal
> telemetry.
>
> Do not change:
> - AID
> - commands
> - response bytes
> - ProofProvider
> - signing semantics.
>
> PHASE 3 — BUILD
>
> Run:
>
> Android:
> :app:testDebugUnitTest
> :app:assembleDebug
>
> Require existing 21/21 tests remain PASS.
>
> Node:
>
> node --test --test-isolation=none
>
> Require:
> 83/83 PASS.
>
> Return APK path and SHA-256 for manual update.
>
> Do not commit.
>
> PHASE 4 — REAL PHYSICAL DIAGNOSTIC
>
> After user installs the diagnostic APK, orchestrate ONE fresh physical Gate D
> session.
>
> Before session:
>
> - user opens app
> - authenticated Privy wallet is ready
> - NFC on
> - Internet available
> - phone unlocked
>
> Reset ESP32 Gate D firmware.
>
> When:
>
> PRESENT_SEEKER
>
> tell user:
>
> PRESENT THE SEEKER TO THE PN532 NOW AND HOLD IT STEADY
>
> Run normally through:
>
> TARGET
> SELECT
> SEND_CHALLENGE
> STATUS
>
> If READY is reached, perform exactly ONE GET_SIGNATURE.
>
> Do not resend SEND_CHALLENGE.
>
> After GET_SIGNATURE fails or succeeds, tell user to remove the phone and open
> the on-device HCE diagnostics.
>
> Collect the Android telemetry.
>
> DECISION MATRIX
>
> CASE A
>
> Android says:
>
> GET_SIGNATURE received = NO
>
> Then the failure is before HostApduService.
>
> Investigate:
> - RF link loss
> - PN532 transmission
> - ISO-DEP session state
>
> Do not debug response buffers yet.
>
> CASE B
>
> Android says:
>
> GET_SIGNATURE received = YES
> processor state = READY
> response length = 67
> status = 9000
>
> Then Android/C1/C2 are proven correct for the physical command.
>
> The failure is PN532/Elechouse receive-side.
>
> Proceed to PHASE 5.
>
> CASE C
>
> GET_SIGNATURE received = YES
> but processor state/response differs from READY / 67 / 9000
>
> Then diagnose the Android processor/session issue.
>
> Do not change PN532 library.
>
> CASE D
>
> Android reports deactivation immediately before/during GET_SIGNATURE.
>
> Treat RF/session loss as primary evidence.
>
> PHASE 5 — PN532 RECEIVE-SIDE DIAGNOSTIC
>
> Only if CASE B is proven.
>
> Do NOT change APDU v1 and do NOT change installed library files.
>
> Using the exact local library source, find the smallest way to expose the
> hidden inDataExchange failure reason.
>
> Preferred options, in order:
>
> A. Existing PN532 debug/error facility that can be enabled without modifying
>    library semantics.
>
> B. A tiny Gate-D-only diagnostic wrapper/helper using existing public HAL/API
>    capabilities.
>
> C. If neither is possible, a temporary LOCAL diagnostic patch to the installed
>    library is permitted ONLY after explicitly reporting why A/B cannot expose
>    the cause.
>
> If C is necessary:
>
> - do not commit the library patch
> - record the exact original file/hash first
> - change diagnostics only, not behavior
> - restore the library byte-for-byte afterward.
>
> The goal is to obtain evidence such as:
>
> - write/ACK failure
> - timeout
> - invalid frame
> - PN532_NO_SPACE
> - PN532 status code
> - I2C requested length
> - actual PN532 frame length
>
> Do not "fix" anything yet.
>
> PHASE 6 — OPTIONAL SHORT-RESPONSE CONTROL
>
> Only if Android is confirmed to return 67 bytes and the low-level failure still
> does not identify the cause:
>
> design ONE temporary controlled experiment to determine whether response size
> is the trigger.
>
> Do not change permanent APDU v1.
>
> The experiment may use a diagnostic-only Android build in which the same
> GET_SIGNATURE command returns a deterministic shorter payload after READY.
>
> Test a minimal useful boundary, for example:
>
> 32 bytes + 9000
>
> versus the real:
>
> 65 bytes + 9000.
>
> This is diagnostic code only.
>
> Do not call the shortened value a signature.
> Do not feed it to Node verification.
> Revert the diagnostic behavior after the test.
>
> Interpretation:
>
> short response PASS
> 67-byte response FAIL
> → response-size/receive path proven
>
> short response also FAIL
> → response length is not sufficient explanation.
>
> Do not run a large length sweep unless one additional boundary is genuinely
> needed.
>
> PHASE 7 — ONLY THEN RECOMMEND FIX
>
> After obtaining evidence, recommend the smallest fix.
>
> Possible outcomes may include:
>
> - firmware buffer/error-handling fix
> - I2C/HAL receive handling fix
> - library/API limitation
> - RF/session handling
> - APDU response-size constraint
>
> Do not implement the fix unless:
>
> - root cause is specific and evidenced
> - it does NOT change Gate A/B/C crypto semantics
> - it does NOT require switching PN532 libraries.
>
> If the only viable fix requires changing frozen APDU v1, such as response
> chunking:
>
> STOP and report that conclusion to Control Tower.
>
> Do not introduce chunking inside this task.
>
> NO-TOUCH
>
> Do not modify:
>
> - Gate A holder-proof semantics
> - Gate B signing semantics
> - permanent Gate C APDU v1
> - ENS
> - scripts/nfc/
> - demo/
> - Privy local config values
>
> No blockchain writes.
> No Gate E.
> No commit.
> No push.
>
> RETURN
>
> # GET_SIGNATURE FAILURE DIAGNOSIS
>
> ## LOCAL ELECHOUSE SOURCE
>
> Include exact relevant buffer/error behavior from the installed source.
>
> ## ANDROID TELEMETRY
> ## PHYSICAL REPRODUCTION
> ## FAILURE BOUNDARY
> ## PN532 LOW-LEVEL ERROR
> ## RESPONSE-SIZE CONTROL
>
> If not needed, say NOT RUN and why.
>
> ## ROOT CAUSE
>
> One evidenced cause, or UNKNOWN if evidence is still insufficient.
>
> ## SMALLEST FIX
>
> Do not implement a protocol-breaking fix automatically.
>
> ## APDU V1 IMPACT
>
> UNCHANGED
>
> or
>
> CHANGE REQUIRED — <why>
>
> ## FILES CHANGED
>
> Separate:
> - real Gate D implementation
> - temporary diagnostics.
>
> ## SECURITY
> ## GIT STATE
> ## NEXT ACTION
>
> End exactly:
>
> DIAGNOSIS: PASS
>
> if root cause is sufficiently established and a bounded fix is known,
>
> or:
>
> DIAGNOSIS: STOP — <specific unresolved boundary>
