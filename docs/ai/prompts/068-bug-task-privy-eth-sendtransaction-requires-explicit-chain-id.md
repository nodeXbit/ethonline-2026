# Project task packet 068: BUG TASK — PRIVY ETH_SENDTRANSACTION REQUIRES EXPLICIT CHAIN ID

- Classification: **ORIGINAL / SANITIZED**
- Redactions applied: none
- Source: retained project-specific Control Tower packet in the local Codex record

---

> BUG TASK — PRIVY ETH_SENDTRANSACTION REQUIRES EXPLICIT CHAIN ID
>
> Physical-device evidence has now identified the exact provider failure.
>
> Observed final state:
>
> failureStage:
> PROVIDER_SEND
>
> category:
> EMBEDDED_WALLET_EXCEPTION
>
> exception:
> EmbeddedWalletException
>
> message:
>
> Chain ID is required for eth_sendTransaction
>
> No transaction hash was returned.
>
> Read-only reconciliation confirms:
>
> issuer:
> 0xFa90e8301A22833B74378C5fA3a7c120Ac512685
>
> Sepolia balance:
> 0.02 ETH
>
> latest nonce:
> 0
>
> pending nonce:
> 0
>
> Therefore:
>
> NO TRANSACTION WAS BROADCAST.
>
> Do NOT execute another blockchain transaction in this task.
>
> Do NOT re-arm physically.
>
> Do NOT change transaction-engine recovery semantics.
>
> Do NOT modify ENS/HCE/Node/firmware.
>
> ============================================================
> OBJECTIVE
> ============================================================
>
> Fix ONLY the Privy transaction-object construction so eth_sendTransaction
> contains the explicit chainId format required by the locally resolved Privy
> 0.14.0 transaction model.
>
> The current physical transaction payload was:
>
> {
>   "from":"0xFa90e8301A22833B74378C5fA3a7c120Ac512685",
>   "to":"0xFa90e8301A22833B74378C5fA3a7c120Ac512685",
>   "value":"0x0",
>   "data":"0x"
> }
>
> Privy physically rejected it with:
>
> Chain ID is required for eth_sendTransaction
>
> ============================================================
> PHASE 1 — VERIFY EXACT LOCAL CHAINID REPRESENTATION
> ============================================================
>
> Inspect the exact locally resolved Privy 0.14.0:
>
> UnsignedEthereumTransaction
>
> serializer
>
> EthereumRpcRequest.ethSendTransaction()
>
> and the Wallet API conversion path.
>
> Determine exactly:
>
> - chainId field type;
> - whether JSON expects:
>   "0xaa36a7"
>   OR
>   "11155111"
>   OR
>   numeric 11155111
>   OR another representation;
> - whether it must be a string or JSON number;
> - whether the field name is exactly "chainId";
> - whether switchChain is still required in addition to payload chainId.
>
> Do not infer from generic Ethereum JSON-RPC alone.
>
> Use the local artifact as source of truth.
>
> ============================================================
> PHASE 2 — CORRECT TRANSACTION BUILDER
> ============================================================
>
> Make the smallest change to the canonical transaction-object builder.
>
> The M1 transaction must still contain exactly:
>
> from:
> 0xFa90e8301A22833B74378C5fA3a7c120Ac512685
>
> to:
> same issuer
>
> value:
> 0x0
>
> data:
> 0x
>
> chainId:
> EXACT LOCAL PRIVY-REQUIRED SEPOLIA REPRESENTATION
>
> No:
>
> nonce
> gas
> gasLimit
> gasPrice
> maxFeePerGas
> maxPriorityFeePerGas
> type
>
> unless local Privy evidence proves one is also mandatory.
>
> Do not add fields speculatively.
>
> ============================================================
> PHASE 3 — PRESERVE DOUBLE CHAIN SAFETY
> ============================================================
>
> Keep:
>
> wallet.provider.switchChain(EthereumChain.Sepolia)
>
> and:
>
> eth_chainId == 0xaa36a7
>
> before submission.
>
> Additionally require that the transaction payload chainId represents the SAME:
>
> 11155111
>
> So the app checks both:
>
> provider selected chain
> AND
> transaction-declared chain
>
> They must agree.
>
> A mismatch must fail before SUBMITTING_NO_HASH.
>
> ============================================================
> PHASE 4 — GENERIC TRANSACTION ENGINE IMPLICATION
> ============================================================
>
> This is not only an M1 detail.
>
> Future operations:
>
> - native ETH send
> - R1.register
> - S1.multicall
> - suspend / restore
> - metadata edit
> - credential transfer
>
> will all require the same explicit chain identity.
>
> Put chainId in the reusable transaction intent/builder rather than hardcoding an
> M1-only string if the current architecture supports that without broad refactor.
>
> Do NOT implement those future operations now.
>
> ============================================================
> PHASE 5 — REVIEW UI
> ============================================================
>
> The human review already shows:
>
> Network:
> Sepolia (11155111)
>
> Keep that.
>
> Developer diagnostics may additionally show:
>
> Transaction chainId:
> 11155111
>
> Do not expose hex-vs-decimal serialization details in normal UI.
>
> ============================================================
> PHASE 6 — TESTS
> ============================================================
>
> Add exact golden tests proving:
>
> 1. M1 transaction JSON contains explicit chainId.
> 2. Exact serialized representation matches Privy local model.
> 3. Provider selected chain == payload chain.
> 4. Wrong payload chain is rejected before submission.
> 5. Missing payload chainId is rejected locally by our builder/validator.
> 6. switchChain remains required.
> 7. Existing from/to/value/data remain unchanged.
> 8. Transaction engine recovery tests remain green.
> 9. No real provider send occurs in tests.
>
> If safely possible, pass the exact corrected transaction JSON through the same
> locally resolved Privy serializer/model used in the previous structural test.
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
> No real blockchain writes.
>
> No real eth_sendTransaction.
>
> No commit/push.
>
> ============================================================
> RETURN
> ============================================================
>
> # M1 EXPLICIT CHAIN ID FIX
>
> ## ROOT CAUSE
>
> Physical provider error:
>
> ## PRIVY LOCAL CHAINID FORMAT
>
> Field:
> Type:
> Exact Sepolia representation:
>
> switchChain still required:
> YES / NO
>
> ## CORRECTED M1 TRANSACTION JSON
>
> Show sanitized exact structure.
>
> ## GENERIC TRANSACTION BUILDER IMPACT
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
> Real blockchain writes:
> 0
>
> Real eth_sendTransaction:
> 0
>
> ## MANUAL RETEST PLAN
>
> Install over current app without clearing data.
>
> Historical failed operation remains NO_BROADCAST_PROVEN.
>
> Explicitly re-arm a NEW operation.
>
> Reach READY_TO_SUBMIT.
>
> Review exact transaction.
>
> Press SUBMIT ONCE exactly once.
>
> If another failure occurs:
> STOP and report persisted stage/category/message.
>
> Do not automatically retry.
>
> End:
>
> M1 CHAIN ID FIX: READY FOR ONE PHYSICAL ATTEMPT
