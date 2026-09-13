# Project task packet 070: TASK — ADD GENERIC READ-ONLY SEPOLIA RPC AND RECOVER EXISTING M1

- Classification: **ORIGINAL / SANITIZED**
- Redactions applied: none
- Source: retained project-specific Control Tower packet in the local Codex record

---

> TASK — ADD GENERIC READ-ONLY SEPOLIA RPC AND RECOVER EXISTING M1
>
> WHY
>
> M1 mobile transaction transport is physically PROVEN.
>
> Existing real Sepolia transaction:
>
> 0x6c4f42f2d368936d4aaf7edf3e0395c376f92b699563b34fee4ed053a0a53e32
>
> Authoritative external reconciliation already proved:
>
> chain:
> 11155111
>
> from:
> 0xFa90e8301A22833B74378C5fA3a7c120Ac512685
>
> to:
> same issuer
>
> value:
> 0
>
> input:
> 0x
>
> tx nonce:
> 0
>
> block:
> 11683226
>
> receipt:
> SUCCESS
>
> gas used:
> 21000
>
> issuer latest/pending nonce:
> 1 / 1
>
> issuer current balance:
> 0.019977050868725 ETH
>
> The Android journal has the tx hash but currently shows UNKNOWN because Privy
> 0.14.0 generic RPC cannot deserialize object-valued JSON-RPC results such as:
>
> eth_getTransactionByHash
> eth_getTransactionReceipt
>
> Its internal generic response model expects result:String.
>
> DO NOT submit another M1 transaction.
>
> ============================================================
> ARCHITECTURE DECISION
> ============================================================
>
> Separate responsibilities:
>
> PRIVY PROVIDER:
>
> - authentication
> - embedded wallet
> - switchChain
> - wallet signing
> - eth_signTypedData_v4
> - eth_sendTransaction
>
> NEW READ-ONLY SEPOLIA RPC CLIENT:
>
> - eth_chainId
> - eth_getBalance
> - eth_getTransactionCount
> - eth_getTransactionByHash
> - eth_getTransactionReceipt
> - eth_getBlockByNumber
> - eth_call
> - eth_estimateGas
>
> The read client MUST NOT contain signing functionality.
>
> It MUST NOT accept private keys.
>
> It MUST NOT expose eth_sendTransaction or eth_sendRawTransaction.
>
> This split should become the intended Android architecture for future ENS reads.
>
> ============================================================
> NO SECRET RPC
> ============================================================
>
> Do NOT compile the operator's Alchemy credential into the APK.
>
> Use an explicitly public, non-secret Sepolia RPC endpoint.
>
> Preferred primary candidate:
>
> https://rpc.sepolia.org
>
> Alternative public endpoint if needed:
>
> https://ethereum-sepolia-rpc.publicnode.com
>
> Before committing either as a default, verify read-only:
>
> eth_chainId == 0xaa36a7
>
> and successful object-valued:
>
> eth_getTransactionByHash
> eth_getTransactionReceipt
>
> for the known M1 hash.
>
> Do NOT send any transaction through these endpoints.
>
> If the primary endpoint is unreliable during inspection, evaluate the second.
>
> Keep endpoint configuration isolated so it can be replaced later without
> touching transaction logic.
>
> No credential-bearing endpoint.
>
> ============================================================
> IMPLEMENTATION
> ============================================================
>
> Build the smallest focused Android client.
>
> Prefer NO new dependency if Android/JDK existing primitives are sufficient.
>
> A design such as:
>
> ReadOnlyEthereumRpcClient
>
> is acceptable.
>
> It must:
>
> - use HTTPS only;
> - POST JSON-RPC 2.0;
> - generate request IDs;
> - support JSON string/object/array/null results;
> - detect JSON-RPC error objects;
> - impose connect/read/total timeout;
> - enforce bounded response size;
> - sanitize errors;
> - execute off main thread;
> - never log endpoint query credentials or raw HTTP metadata.
>
> Do NOT build a broad Web3 library.
>
> Do NOT implement ABI here.
>
> ============================================================
> API SURFACE
> ============================================================
>
> Expose a small generic read primitive plus typed helpers as justified.
>
> Conceptually:
>
> call(method, params)
>
> chainId()
>
> balance(address)
>
> transactionCount(address, tag)
>
> transactionByHash(hash)
>
> receipt(hash)
>
> blockByNumber(...)
>
> ethCall(...)
>
> estimateGas(...)
>
> Do not implement future ENS business logic yet.
>
> ============================================================
> READ-ONLY SECURITY BOUNDARY
> ============================================================
>
> The client must use an explicit allowlist of read methods.
>
> At minimum forbid:
>
> eth_sendTransaction
> eth_sendRawTransaction
> personal_sign
> eth_sign
> eth_signTypedData*
> wallet_*
> privy-specific wallet methods
>
> No method provided by user-controlled strings in the product UI.
>
> ============================================================
> TRANSACTION ENGINE INTEGRATION
> ============================================================
>
> Keep Privy for submission.
>
> After a tx hash is persisted:
>
> use the read-only RPC client for authoritative reconciliation.
>
> For M1:
>
> tx hash
> -> transactionByHash
> -> validate from/to/value/input/hash
> -> receipt
> -> status SUCCESS
> -> chain
> -> latest/pending nonce
> -> CONFIRMED
>
> The existing journal hash remains authoritative.
>
> Do NOT create a new M1 operation.
>
> Do NOT re-arm.
>
> Do NOT submit.
>
> ============================================================
> RECOVER THE EXISTING REAL M1
> ============================================================
>
> After implementation, the app must be able to restart with the EXISTING journal
> and EXISTING hash:
>
> 0x6c4f42f2d368936d4aaf7edf3e0395c376f92b699563b34fee4ed053a0a53e32
>
> and reconcile it automatically/read-only.
>
> Expected final journal state:
>
> CONFIRMED
>
> receiptBlock:
> 11683226
>
> pre nonce:
> 0
>
> post latest:
> 1
>
> post pending:
> 1
>
> No new transaction.
>
> No new operationId.
>
> ============================================================
> CONSISTENCY
> ============================================================
>
> Keep:
>
> wallet.provider.switchChain(EthereumChain.Sepolia)
>
> before wallet writes.
>
> For read-only RPC:
>
> require chainId == 11155111 before accepting results.
>
> Future transaction operations must ensure:
>
> Privy transaction chain
> ==
> read RPC chain
> ==
> 11155111
>
> If mismatch:
>
> fail closed.
>
> ============================================================
> FUTURE PRODUCT USE
> ============================================================
>
> Design this small client so it can later serve:
>
> - Wallet balance
> - Recent transaction reconciliation
> - R1/S1 provenance
> - credential owner
> - expiry
> - roles
> - access.v1
> - avatar
> - description
>
> Do NOT implement these product features in this task.
>
> ============================================================
> TESTS
> ============================================================
>
> Add tests for:
>
> JSON-RPC scalar result
>
> JSON-RPC object result
>
> JSON-RPC array result
>
> null result
>
> JSON-RPC error
>
> HTTP failure
>
> timeout
>
> oversized response
>
> wrong chain
>
> malformed JSON
>
> tx object parsing
>
> receipt parsing
>
> receipt reverted
>
> transaction from mismatch
>
> transaction to mismatch
>
> value mismatch
>
> input mismatch
>
> nonce reconciliation
>
> blocked write methods
>
> no endpoint credentials logged
>
> existing UNKNOWN/hash operation can become CONFIRMED
>
> CONFIRMED operation never resubmits
>
> No real transaction in tests.
>
> ============================================================
> READ-ONLY INTEGRATION TEST
> ============================================================
>
> It is authorized to perform read-only network validation against the selected
> PUBLIC Sepolia endpoint.
>
> Use the known public M1 tx hash.
>
> Require:
>
> chainId 11155111
>
> tx/receipt fields match the already-known authoritative result.
>
> No signing.
>
> No wallet write.
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
> No ENS writes.
>
> No blockchain writes.
>
> No HCE changes.
>
> No Node changes.
>
> No firmware changes.
>
> No commit/push.
>
> ============================================================
> RETURN
> ============================================================
>
> # ANDROID READ-ONLY RPC + M1 RECOVERY
>
> ## ARCHITECTURE
>
> Privy responsibilities:
>
> Read RPC responsibilities:
>
> ## PUBLIC RPC
>
> Selected endpoint:
>
> Why:
>
> Chain verification:
>
> No secret:
> YES
>
> ## CLIENT
>
> File:
>
> Dependencies added:
> YES / NO
>
> Allowed methods:
>
> Forbidden methods:
>
> Timeouts / bounds:
>
> ## EXISTING M1 READBACK
>
> Hash:
>
> Transaction readback:
> PASS / FAIL
>
> Receipt:
> PASS / FAIL
>
> Nonce:
> PASS / FAIL
>
> ## JOURNAL RECOVERY
>
> Before:
>
> After:
>
> New transaction:
> NO
>
> New operation:
> NO
>
> ## TESTS
>
> ## APK
>
> Path:
> SHA-256:
>
> ## SECURITY
>
> Blockchain writes:
> 0
>
> eth_sendTransaction:
> 0
>
> Secrets:
> 0
>
> ## MANUAL PHYSICAL RECOVERY
>
> Install over current app without clearing data.
>
> Open same issuer account.
>
> Do NOT re-arm.
>
> Do NOT press RUN.
>
> Existing operation must reconcile to CONFIRMED.
>
> End:
>
> M1 EXISTING TRANSACTION: READY FOR PHYSICAL RECOVERY
>
> or:
>
> M1 READ-ONLY RPC: STOP — <reason>
