# Project task packet 066: DIAGNOSTIC TASK — M1 SUBMIT ONCE FAILED BEFORE TX HASH

- Classification: **ORIGINAL / SANITIZED**
- Redactions applied: none
- Source: retained project-specific Control Tower packet in the local Codex record

---

> DIAGNOSTIC TASK — M1 SUBMIT ONCE FAILED BEFORE TX HASH
>
> CORRECTION TO PREVIOUS PHYSICAL INTERPRETATION
>
> The human-consent boundary DID work.
>
> Actual physical sequence:
>
> 1. Operation reached READY_TO_SUBMIT.
> 2. User pressed RUN.
> 3. Review dialog DID appear with transaction information.
> 4. User pressed SUBMIT ONCE.
> 5. No transaction hash was obtained.
> 6. RecoverableTransactionEngine later reconciled onchain.
> 7. Issuer latest/pending nonce remained 0/0.
> 8. Issuer balance remained 0.02 ETH.
> 9. Operation became NO_BROADCAST_PROVEN.
>
> Therefore:
>
> - RUN did not submit directly.
> - SUBMIT ONCE was explicitly pressed.
> - The recovery engine behaved correctly after a real pre-hash send failure.
> - M1 transport itself is still NOT proven.
>
> DO NOT perform another transaction.
>
> DO NOT re-arm.
>
> DO NOT clear app data.
>
> DO NOT modify ENS/HCE/Node/firmware.
>
> ============================================================
> OBJECTIVE
> ============================================================
>
> Determine the exact cause of the physical:
>
> eth_sendTransaction
> → failure before valid tx hash
>
> using the current implementation, persisted journal and locally resolved Privy
> 0.14.0 behavior.
>
> This task is READ-ONLY unless a tiny diagnostic-only Android change is strictly
> necessary after inspection.
>
> No blockchain write is authorized.
>
> ============================================================
> PHASE 1 — INSPECT CURRENT PERSISTED ERROR MODEL
> ============================================================
>
> Inspect RecoverableTransactionEngine and MobileIssuerAdmissionRunner.
>
> Determine exactly what is persisted when:
>
> provider request throws before hash.
>
> Report whether the current failed operation stores:
>
> - safeErrorCategory
> - stage
> - sanitized provider message
> - exception class
> - pre nonce
> - operation intent digest
> - any other safe field
>
> Do not modify yet.
>
> ============================================================
> PHASE 2 — IDENTIFY THE EXACT SEND REQUEST
> ============================================================
>
> Show the exact conceptual request constructed for the physical M1.
>
> Require:
>
> method:
> eth_sendTransaction
>
> params shape as Privy 0.14.0 actually expects.
>
> Exact transaction object fields:
>
> from
> to
> value
> data
>
> Determine whether any of these were also supplied:
>
> gas
> gasPrice
> maxFeePerGas
> maxPriorityFeePerGas
> nonce
> chainId
>
> Do not guess.
>
> Compare the actual code against the locally inspected Privy implementation of:
>
> EthereumRpcRequest.ethSendTransaction(transactionJson)
>
> and the internal UnsignedEthereumTransaction parser.
>
> ============================================================
> PHASE 3 — CHECK ADDRESS / VALUE / DATA SERIALIZATION
> ============================================================
>
> Verify exact physical request representations:
>
> from:
> 0xFa90e8301A22833B74378C5fA3a7c120Ac512685
>
> to:
> 0xFa90e8301A22833B74378C5fA3a7c120Ac512685
>
> value:
> 0x0
>
> data:
> 0x
>
> Check whether Privy requires another canonical field name such as `input`
> instead of `data`, or supports both, from LOCAL 0.14.0 source/bytecode.
>
> Check casing/checksum behavior.
>
> Check whether transaction JSON is:
>
> raw JSON object string
>
> and NOT:
> - quoted object string;
> - double encoded JSON;
> - malformed List<String> param.
>
> We already found one real Privy parameter-serialization bug in read methods.
> Audit the send path equally carefully.
>
> ============================================================
> PHASE 4 — PRIVY UNSIGNED TRANSACTION REQUIREMENTS
> ============================================================
>
> Inspect the exact locally resolved:
>
> UnsignedEthereumTransaction
>
> serializer/model.
>
> Return all accepted/required fields and nullable/default behavior.
>
> Specifically answer whether M1 requires:
>
> - gas
> - nonce
> - gasPrice
> - maxFeePerGas
> - maxPriorityFeePerGas
> - chainId
>
> or whether the Privy provider populates them.
>
> Determine whether a 0-value self transaction with empty data is accepted by
> this SDK path.
>
> ============================================================
> PHASE 5 — RECOVER THE PHYSICAL ERROR IF POSSIBLE
> ============================================================
>
> Use only safe existing data.
>
> If the journal persisted a safe error category/message, report it.
>
> If sanitized Logcat data from the prior run can be inspected without exposing
> secrets, use only the M1Admission tagged lines.
>
> Do NOT dump generic app logs.
>
> Do NOT expose:
> - RPC endpoints
> - Privy auth
> - email/OTP
> - headers
> - tokens
> - provider internals containing secrets
>
> If the physical exception was not persisted and cannot be recovered, say so.
>
> ============================================================
> PHASE 6 — READ-ONLY FEASIBILITY
> ============================================================
>
> Do NOT submit.
>
> Using Privy/local code inspection, determine the exact corrected request for
> the next M1 attempt.
>
> If useful, add a pure/test-double test that passes the exact JSON through the
> same local serialization/parser path used by Privy.
>
> No real provider write.
>
> If Privy's internal parser can be invoked in a JVM/unit context safely, test
> that the proposed M1 transaction is accepted structurally.
>
> ============================================================
> PHASE 7 — TRANSACTION ENGINE REVIEW RESULT
> ============================================================
>
> Explicitly assess the transaction engine based on this physical failure.
>
> Expected conclusion if supported:
>
> Human consent:
> PASS
>
> Pre-hash persistence:
> PASS
>
> No blind retry:
> PASS
>
> Onchain no-broadcast reconciliation:
> PASS
>
> Explicit re-arm:
> PASS
>
> M1 transaction transport:
> FAIL / NOT YET PROVEN
>
> Do not conflate transport failure with transaction-engine recovery failure.
>
> ============================================================
> PHASE 8 — ONLY IF DIAGNOSTIC GAP EXISTS
> ============================================================
>
> If the real error cannot be recovered because the engine stores only a broad
> safeErrorCategory, propose the minimum improvement so the NEXT failure, if any,
> persists:
>
> - stage = PROVIDER_SEND
> - sanitized exception class
> - sanitized short message/category
>
> Do not store raw Throwable/provider response.
>
> Only implement this diagnostic persistence if needed and if it requires no
> architectural expansion.
>
> Still no blockchain write.
>
> ============================================================
> TESTS / VALIDATION
> ============================================================
>
> If code changes are made:
>
> .\gradlew.bat testDebugUnitTest
> .\gradlew.bat assembleDebug
>
> git diff --check
>
> No real eth_sendTransaction.
>
> ============================================================
> RETURN
> ============================================================
>
> # M1 PRE-HASH SEND FAILURE
>
> ## PHYSICAL FLOW
>
> Review visible:
> YES
>
> SUBMIT ONCE explicitly pressed:
> YES
>
> Human-consent boundary:
> PASS
>
> ## PERSISTED FAILURE EVIDENCE
>
> OperationId:
>
> Pre nonce:
>
> Failure stage:
>
> Safe error category:
>
> Safe message:
>
> ## EXACT ETH_SENDTRANSACTION REQUEST
>
> Method:
>
> Params:
>
> Transaction JSON:
>
> ## PRIVY 0.14.0 TRANSACTION MODEL
>
> Required fields:
>
> Optional fields:
>
> Provider-populated fields:
>
> ## ROOT CAUSE
>
> CONFIRMED / MOST LIKELY / UNKNOWN
>
> Exact reason:
>
> ## CORRECTED NEXT REQUEST
>
> Do not execute.
>
> ## TRANSACTION ENGINE PHYSICAL VERDICT
>
> Human review:
> PASS
>
> Durable pre-hash state:
> PASS / FAIL
>
> Hash safety:
> PASS / N/A
>
> No blind retry:
> PASS
>
> NO_BROADCAST reconciliation:
> PASS
>
> Recovery:
> PASS
>
> Mobile transaction transport:
> PASS / NOT YET PROVEN
>
> ## ANY CODE CHANGE
>
> ## TESTS
>
> ## NEXT ATTEMPT READINESS
>
> READY / NOT READY
>
> Do not re-arm or submit.
>
> If ready end:
>
> NEXT AUTHORIZATION REQUIRED:
> ONE NEW EXPLICITLY RE-ARMED M1 ATTEMPT ONLY.
