# Project task packet 061: BUG TASK — M1 ADMISSION READINESS DOES NOT RUN ON DEVICE

- Classification: **ORIGINAL / SANITIZED**
- Redactions applied: none
- Source: retained project-specific Control Tower packet in the local Codex record

---

> BUG TASK — M1 ADMISSION READINESS DOES NOT RUN ON DEVICE
>
> Do NOT perform any blockchain transaction.
>
> Do NOT press or programmatically invoke the M1 send action.
>
> The APK built successfully and the dedicated issuer Privy wallet exists on the
> physical Android device.
>
> Observed physical behavior:
>
> The M1 section renders correctly.
>
> It shows:
>
> CURRENT WALLET:
> a dedicated wallet distinct from the known holder
>
> NETWORK:
> Not verified
>
> BALANCE:
> Not available
>
> STATUS:
> BLOCKED - DEDICATED_ISSUER_CONFIRMATION_REQUIRED
>
> After the user checks:
>
> "I confirm this separate login is the new dedicated issuer account"
>
> the expected read-only admission does NOT occur.
>
> NETWORK remains:
> Not verified
>
> BALANCE remains:
> Not available
>
> No READY or ISSUER_FUNDING_REQUIRED state appears.
>
> This is a real physical-device integration failure.
>
> ============================================================
> SCOPE
> ============================================================
>
> Diagnose and fix ONLY the M1 read-only readiness wiring.
>
> Authorized:
>
> - inspect current M1 diff
> - inspect MainActivity event/listener wiring
> - inspect MobileIssuerAdmissionRunner
> - inspect lifecycle/coroutine/state update behavior
> - add focused logging that is sanitized
> - make the minimum Android code fix
> - add/update tests
> - build APK
>
> NOT AUTHORIZED:
>
> - blockchain writes
> - eth_sendTransaction
> - ENS writes
> - dependency installation
> - Web3j
> - HCE changes
> - NFC tests
> - Node changes
> - firmware changes
> - UI redesign
> - commit
> - push
> - secrets in logs
>
> ============================================================
> FIRST — INSPECT, DO NOT GUESS
> ============================================================
>
> Trace the exact runtime path from:
>
> checkbox checked
>
> to:
>
> admission readiness invocation
>
> to:
>
> wallet.provider.switchChain(EthereumChain.Sepolia)
>
> to read-only:
>
> eth_chainId
> eth_getBalance
> eth_getTransactionCount latest
> eth_getTransactionCount pending
>
> to:
>
> UI state update/render.
>
> Determine which stage is not occurring.
>
> Explicitly inspect:
>
> 1. checkbox listener registration;
> 2. whether the checked state actually triggers refresh/admission;
> 3. whether the runner instance exists at that time;
> 4. whether the authenticated wallet/provider is available;
> 5. coroutine scope / lifecycle ownership;
> 6. main-thread UI updates;
> 7. whether an exception is swallowed;
> 8. whether the UI is rendered from stale state after the runner updates;
> 9. whether a recreated Activity loses the issuer confirmation/readiness state.
>
> Do not infer success from unit tests.
>
> ============================================================
> SANITIZED DEVICE DIAGNOSTICS
> ============================================================
>
> If runtime diagnostics are needed, logs may contain ONLY:
>
> - event names
> - public wallet address shortened or full
> - chain ID
> - public balance
> - nonce numbers
> - admission stage
> - sanitized exception class/category
>
> NEVER log:
>
> - email
> - OTP
> - auth tokens
> - RPC URLs
> - HTTP metadata
> - provider internals
> - secrets
>
> Add temporary structured tags if useful, for example:
>
> M1_CONFIRMATION_CHANGED
> M1_READINESS_STARTED
> M1_CHAIN_SWITCH_OK
> M1_CHAIN_READ_OK
> M1_BALANCE_READ_OK
> M1_NONCE_READ_OK
> M1_READINESS_READY
> M1_READINESS_BLOCKED
> M1_READINESS_ERROR
>
> Do not dump raw exception objects.
>
> ============================================================
> EXPECTED PRODUCT BEHAVIOR
> ============================================================
>
> When the checkbox becomes checked:
>
> 1. disable the M1 send button;
> 2. show a visible state such as CHECKING;
> 3. start exactly one read-only readiness evaluation;
> 4. switch the provider to Sepolia;
> 5. require eth_chainId == 0xaa36a7;
> 6. read balance;
> 7. read latest nonce;
> 8. read pending nonce;
> 9. require latest == pending;
> 10. require wallet != known holder;
> 11. update the UI on the main thread.
>
> Result must become one of:
>
> READY
>
> or
>
> ISSUER_FUNDING_REQUIRED
>
> or a specific sanitized BLOCKED/ERROR reason.
>
> It must NOT remain indefinitely at:
>
> Not verified / Not available
>
> after confirmation.
>
> Unchecking the issuer confirmation must:
>
> - cancel/invalidate any pending readiness result;
> - disable submission;
> - return to confirmation-required state.
>
> Avoid duplicate concurrent readiness requests when the checkbox is toggled.
>
> ============================================================
> NO TRANSACTION GUARANTEE
> ============================================================
>
> The readiness path MUST NOT be capable of calling:
>
> eth_sendTransaction
>
> The send operation remains behind the separate explicit RUN action plus final
> confirmation.
>
> Add/retain a test proving readiness evaluation cannot submit.
>
> ============================================================
> TESTS
> ============================================================
>
> Add or update tests for at least:
>
> - checkbox confirmation triggers readiness once;
> - unchecked does not run readiness;
> - re-check does not create duplicate concurrent runs;
> - wallet unavailable -> blocked reason;
> - wrong chain -> blocked;
> - zero balance -> ISSUER_FUNDING_REQUIRED;
> - latest != pending -> blocked;
> - valid reads -> READY;
> - stale async result after uncheck is ignored;
> - readiness path performs zero sendTransaction calls.
>
> Run:
>
> .\gradlew.bat testDebugUnitTest
> .\gradlew.bat assembleDebug
>
> and:
>
> git diff --check
>
> ============================================================
> RETURN
> ============================================================
>
> # M1 READINESS BUG
>
> ## ROOT CAUSE
>
> Exact cause, with file/function.
>
> ## FIX
>
> Exact minimal change.
>
> ## SAFETY
>
> Confirm readiness path:
> READ-ONLY
>
> eth_sendTransaction reachable from readiness:
> NO
>
> ## TESTS
>
> ## APK
>
> Give exact new APK path and SHA-256.
>
> ## GIT
>
> No commit/push.
>
> ## MANUAL RETEST
>
> Give exact phone steps.
>
> End:
>
> M1 READINESS FIX: READY FOR PHYSICAL RETEST
>
> or
>
> M1 READINESS FIX: STOP — <exact reason>
