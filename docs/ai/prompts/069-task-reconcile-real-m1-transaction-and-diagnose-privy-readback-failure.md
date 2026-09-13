# Project task packet 069: TASK — RECONCILE REAL M1 TRANSACTION AND DIAGNOSE PRIVY READBACK FAILURE

- Classification: **ORIGINAL / SANITIZED**
- Redactions applied: none
- Source: retained project-specific Control Tower packet in the local Codex record

---

> TASK — RECONCILE REAL M1 TRANSACTION AND DIAGNOSE PRIVY READBACK FAILURE
>
> A real Android/Privy transaction now exists.
>
> DO NOT perform another transaction.
> DO NOT re-arm M1.
> DO NOT call eth_sendTransaction.
> DO NOT perform any ENS write.
> DO NOT modify journal state before diagnosis.
> DO NOT clear app data.
>
> ============================================================
> PHYSICAL RESULT
> ============================================================
>
> Issuer:
>
> 0xFa90e8301A22833B74378C5fA3a7c120Ac512685
>
> App state after SUBMIT ONCE:
>
> Could not determine transaction status
>
> Stage/category:
>
> READBACK_PRIVY_API_EXCEPTION
>
> The UI exposes:
>
> VIEW TRANSACTION IN EXPLORER
>
> Known transaction hash:
>
> 0x6c4f42f2d368936d4aaf7edf3e0395c376f92b699563b34fee4ed053a0a53e32
>
> The transaction is visible in the Sepolia explorer.
>
> This means a valid tx hash was returned and persisted.
>
> ============================================================
> OBJECTIVE A — AUTHORITATIVE READ-ONLY RECONCILIATION
> ============================================================
>
> Using the project's SAFE read-only Sepolia path, with no credential-bearing RPC
> URL printed, read exactly:
>
> eth_chainId
>
> eth_getTransactionByHash(
>   0x6c4f42f2d368936d4aaf7edf3e0395c376f92b699563b34fee4ed053a0a53e32
> )
>
> eth_getTransactionReceipt(
>   same hash
> )
>
> issuer:
> eth_getTransactionCount latest
> eth_getTransactionCount pending
> eth_getBalance latest
>
> Return public fields only.
>
> Require:
>
> chainId == 11155111
>
> transaction hash ==
> expected hash
>
> from ==
> 0xFa90e8301A22833B74378C5fA3a7c120Ac512685
>
> to ==
> same issuer
>
> value ==
> 0
>
> input ==
> empty / 0x
>
> transaction chain semantics ==
> Sepolia
>
> receipt status ==
> SUCCESS
>
> Capture:
>
> block number
> transaction nonce
> gas used
> effective gas price if available
>
> Require issuer latest/pending nonces reconcile appropriately.
>
> Do not infer PASS merely because explorer has a page.
>
> ============================================================
> OBJECTIVE B — INSPECT PERSISTED JOURNAL SEMANTICS
> ============================================================
>
> Inspect code and, if safely accessible, only this M1 operation's journal record.
>
> Determine:
>
> state
>
> txHash
>
> preLatestNonce
> prePendingNonce
>
> receiptBlock
>
> failureStage
> safeErrorCategory
> safeException
> safeMessage
>
> The transaction hash MUST remain authoritative and the operation MUST NOT become
> retryable.
>
> If device-private journal is unavailable, inspect code and report that runtime
> journal fields cannot be read.
>
> ============================================================
> OBJECTIVE C — DIAGNOSE READBACK_PRIVY_API_EXCEPTION
> ============================================================
>
> Trace the exact mobile code after HASH_RECEIVED.
>
> Identify every Privy request made during readback, in order.
>
> Likely candidates include:
>
> eth_getTransactionByHash
> eth_getTransactionReceipt
> eth_chainId
> eth_getTransactionCount
>
> For each show:
>
> method
> exact params List<String>
> expected Privy serialization
> parser used
> failure mapping
>
> Determine which call can generate:
>
> READBACK_PRIVY_API_EXCEPTION
>
> Do not guess.
>
> ============================================================
> PARAM SERIALIZATION AUDIT
> ============================================================
>
> We previously discovered that Privy List<String> parameters must NOT contain
> manual quote characters.
>
> Audit the current post-hash methods specifically.
>
> Correct conceptual parameter for a tx hash should be the plain Kotlin string:
>
> 0x6c4f42...
>
> not:
>
> "\"0x6c4f42...\""
>
> Confirm the exact current implementation.
>
> Also verify the transaction/receipt response parser against actual Privy
> EthereumRpcResponse.data behavior.
>
> ============================================================
> READBACK ARCHITECTURE
> ============================================================
>
> If Privy generic RPC supports these reads correctly after fixing a local bug:
>
> KEEP Privy provider for mobile readback.
>
> If the physical evidence proves a particular read method is unsupported or
> semantically incompatible:
>
> report that precisely.
>
> Do NOT add a second RPC provider yet.
>
> ============================================================
> M1 VERDICT
> ============================================================
>
> Explicitly separate:
>
> MOBILE TRANSACTION TRANSPORT
>
> from:
>
> APP RECONCILIATION
>
> If authoritative onchain data proves the transaction succeeded:
>
> Mobile transaction transport:
> PASS
>
> App reconciliation:
> PASS / FIX REQUIRED
>
> Do not call the overall M1 fully complete until the app can recover/reconcile
> the persisted hash by itself.
>
> ============================================================
> ONLY AFTER ROOT CAUSE
> ============================================================
>
> If the readback bug is a small local serialization/parser defect, implement the
> minimum Android-only fix.
>
> It must support recovery of the ALREADY EXISTING tx hash.
>
> Do NOT send another M1.
>
> After installing the fixed APK, app restart/reconciliation should convert the
> existing operation to CONFIRMED using the same hash.
>
> No new blockchain write is permitted.
>
> Add tests using the actual observed transaction/receipt JSON shape where useful.
>
> ============================================================
> VALIDATION IF CODE CHANGES
> ============================================================
>
> .\gradlew.bat testDebugUnitTest
> .\gradlew.bat assembleDebug
>
> git diff --check
>
> Real blockchain writes:
> 0
>
> Real eth_sendTransaction:
> 0
>
> ============================================================
> RETURN
> ============================================================
>
> # M1 REAL TRANSACTION RECONCILIATION
>
> ## AUTHORITATIVE ONCHAIN RESULT
>
> Chain:
>
> Hash:
>
> From:
>
> To:
>
> Value:
>
> Input:
>
> Transaction nonce:
>
> Block:
>
> Receipt:
>
> Gas used:
>
> Issuer latest nonce:
>
> Issuer pending nonce:
>
> Issuer balance:
>
> ## MOBILE TRANSACTION TRANSPORT
>
> PASS / FAIL
>
> ## APP JOURNAL
>
> State:
>
> Persisted hash:
>
> Failure stage:
>
> ## PRIVY READBACK FAILURE
>
> Exact failing method:
>
> Exact request params:
>
> Exact root cause:
>
> ## FIX
>
> If needed.
>
> ## TESTS
>
> ## APK
>
> If changed:
>
> Path:
> SHA-256:
>
> ## PHYSICAL RECOVERY PLAN
>
> Must reuse the EXISTING hash.
>
> NO new transaction.
>
> ## FINAL VERDICT
>
> M1 TRANSPORT:
> PASS / FAIL
>
> M1 RECONCILIATION:
> PASS / FIX REQUIRED
>
> End with one of:
>
> M1 EXISTING TRANSACTION: READY FOR APP RECOVERY
>
> or
>
> M1 EXISTING TRANSACTION: FULLY RECONCILED
