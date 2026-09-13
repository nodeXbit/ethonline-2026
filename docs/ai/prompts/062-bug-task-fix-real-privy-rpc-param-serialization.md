# Project task packet 062: BUG TASK — FIX REAL PRIVY RPC PARAM SERIALIZATION

- Classification: **ORIGINAL / SANITIZED**
- Redactions applied: none
- Source: retained project-specific Control Tower packet in the local Codex record

---

> BUG TASK — FIX REAL PRIVY RPC PARAM SERIALIZATION
>
> Physical device evidence:
>
> SWITCH_CHAIN: PASS
>
> READ_CHAIN_ID: PASS
>
> Network displayed:
> Sepolia (11155111)
>
> READ_BALANCE: FAIL
>
> Exact terminal state:
>
> BLOCKED - READ_BALANCE - EMBEDDED_WALLET_EXCEPTION
>
> Sanitized message:
>
> RPC error: Invalid params
>
> No blockchain transaction was submitted.
>
> Blockchain writes remain 0.
>
> The dedicated issuer public address displayed by the app is:
>
> 0xFa90e8301A22833B74378C5fA3a7c120AC512685
>
> Do NOT perform any blockchain write.
>
> Do NOT call eth_sendTransaction.
>
> Do NOT add another RPC provider.
>
> Do NOT add Alchemy/operator credentials.
>
> The evidence now proves:
>
> - switchChain(EthereumChain.Sepolia) works on the physical device;
> - eth_chainId works through the Privy provider;
> - the generic RPC path is reachable;
> - the first parametrized RPC request, eth_getBalance, is rejected with
>   Invalid params.
>
> ============================================================
> OBJECTIVE
> ============================================================
>
> Determine and fix the exact JSON parameter serialization required by the
> locally resolved Privy 0.14.0:
>
> EthereumRpcRequest(
>     method: String,
>     params: List<String>
> )
>
> Do not guess.
>
> Inspect the local implementation / bytecode and any available usage examples.
>
> Compare:
>
> - parameter strings before Privy serialization;
> - final JSON structure Privy constructs if determinable;
> - Ethereum JSON-RPC expected parameter shapes.
>
> ============================================================
> METHODS TO AUDIT
> ============================================================
>
> Audit every parametrized method used or planned in M1:
>
> eth_getBalance(address, blockTag)
>
> eth_getTransactionCount(address, blockTag)
>
> eth_getTransactionByHash(txHash)
>
> eth_getTransactionReceipt(txHash)
>
> eth_estimateGas(transactionObject) if currently used
>
> eth_sendTransaction(transactionObject)
>
> The fix must establish ONE correct helper/pattern for:
>
> A. JSON string parameter
>
> B. JSON object parameter
>
> so we do not fix balance and then fail again at nonce/receipt.
>
> ============================================================
> IMPORTANT LOCAL API FACT
>
> Prior inspection concluded:
>
> EthereumRpcRequest params are List<String>
>
> and each entry is intended to represent JSON content.
>
> Reverify what that means in the actual implementation.
>
> Specifically determine whether a JSON string parameter must be supplied as:
>
> "\"0xabc...\""
>
> rather than:
>
> "0xabc..."
>
> and similarly:
>
> "\"latest\""
>
> rather than:
>
> "latest"
>
> Also verify transaction JSON objects must remain raw JSON objects rather than
> quoted JSON strings.
>
> Do not apply this assumed fix until the local implementation confirms it.
>
> ============================================================
> IMPLEMENTATION
> ============================================================
>
> Make the smallest change.
>
> Prefer explicit helpers such as conceptually:
>
> jsonStringParam(value)
>
> jsonObjectParam(json)
>
> or another clear equivalent supported by the local API.
>
> Do not scatter manual quoting throughout the runner.
>
> Ensure:
>
> eth_getBalance:
>   address = JSON string
>   block tag = JSON string
>
> eth_getTransactionCount:
>   address = JSON string
>   block tag = JSON string
>
> eth_getTransactionByHash:
>   tx hash = JSON string
>
> eth_getTransactionReceipt:
>   tx hash = JSON string
>
> eth_sendTransaction:
>   transaction = JSON object
>
> No blockchain call is authorized during this task.
>
> ============================================================
> TEST QUALITY
> ============================================================
>
> The previous mocks did not detect this real-device serialization defect.
>
> Improve tests so they assert the EXACT EthereumRpcRequest method and parameter
> strings produced before the provider call.
>
> Add golden assertions for:
>
> eth_getBalance
>
> eth_getTransactionCount latest
>
> eth_getTransactionCount pending
>
> eth_getTransactionByHash
>
> eth_getTransactionReceipt
>
> eth_sendTransaction
>
> Tests must prove string params and object params are encoded differently where
> required.
>
> Do not execute eth_sendTransaction.
>
> ============================================================
> READINESS RETEST SCOPE
> ============================================================
>
> After the fix, readiness remains read-only:
>
> switchChain
> eth_chainId
> eth_getBalance
> eth_getTransactionCount latest
> eth_getTransactionCount pending
>
> No send path.
>
> Expected physical result after reinstall:
>
> either:
>
> READY
>
> or:
>
> ISSUER_FUNDING_REQUIRED
>
> or the next exact stage-specific failure.
>
> ============================================================
> VALIDATION
> ============================================================
>
> Run:
>
> .\gradlew.bat testDebugUnitTest
> .\gradlew.bat assembleDebug
>
> git diff --check
>
> No commit/push.
>
> No dependency changes.
>
> ============================================================
> RETURN
> ============================================================
>
> # M1 RPC PARAMETER BUG
>
> ## ROOT CAUSE
>
> Show the exact wrong request parameter representation.
>
> ## CORRECT PRIVY REPRESENTATION
>
> JSON string parameter:
>
> JSON object parameter:
>
> ## METHODS FIXED/AUDITED
>
> eth_getBalance:
> eth_getTransactionCount:
> eth_getTransactionByHash:
> eth_getTransactionReceipt:
> eth_sendTransaction:
>
> ## TESTS
>
> Include exact parameter-shape tests.
>
> ## APK
>
> Path:
> SHA-256:
>
> ## SAFETY
>
> Blockchain writes:
> 0
>
> eth_sendTransaction called:
> NO
>
> ## MANUAL RETEST
>
> Install over current APK.
> Keep existing issuer login/data.
> Check issuer confirmation once.
> Do NOT press RUN.
>
> Report resulting terminal readiness state.
>
> End:
>
> M1 RPC PARAM FIX: READY FOR PHYSICAL RETEST
