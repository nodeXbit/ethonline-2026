package io.github.nodexbit.ethonline2026

import java.math.BigInteger
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext

data class ContractTransactionRequest(
    val operationType: String,
    val from: String,
    val to: String,
    val data: String,
)

data class ContractTransactionResult(
    val status: MobileIssuerStatus,
    val category: String,
    val transactionHash: String? = null,
    val blockNumber: BigInteger? = null,
)

class ContractTransactionRunner(
    val identity: StudioRunnerIdentity,
    private val walletProvider: MobileIssuerWalletProvider,
    private val client: ReadOnlyEthereumRpcClient,
    private val engine: RecoverableTransactionEngine,
    private val wait: suspend (Long) -> Unit = { delay(it) },
    private val attempts: Int = 30,
    private val pollDelayMillis: Long = 1_500L,
) {
    private fun intent(request: ContractTransactionRequest) = TransactionIntent(
            operationType = request.operationType,
            walletAddress = request.from,
            chainId = IssuerSpace.chainId,
            targetAddress = request.to,
            valueWei = "0",
            dataSummary = CredentialAbi.calldataFingerprint(request.data),
        )

    fun requireReviewAvailable(request: ContractTransactionRequest, replacesOperationId: String? = null) {
        engine.requireReviewAvailable(intent(request), replacesOperationId)
    }

    fun create(request: ContractTransactionRequest, replacesOperationId: String? = null): PersistedTransactionOperation =
        engine.prepareReviewedOperation(intent(request), replacesOperationId)

    fun review(operationId: String): PersistedTransactionOperation {
        val current = engine.find(operationId) ?: error("TRANSACTION_OPERATION_NOT_FOUND")
        val reviewed = if (current.state == TransactionOperationState.DRAFT) {
            engine.transition(operationId, TransactionOperationState.READY_TO_REVIEW)
        } else {
            current
        }
        return if (reviewed.state == TransactionOperationState.READY_TO_REVIEW) {
            engine.transition(reviewed.operationId, TransactionOperationState.READY_TO_SUBMIT)
        } else {
            require(reviewed.state == TransactionOperationState.READY_TO_SUBMIT) {
                "TRANSACTION_OPERATION_NOT_READY"
            }
            reviewed
        }
    }

    suspend fun verifyNewSubmission(
        request: ContractTransactionRequest,
        preflight: suspend () -> Unit,
    ) {
        require(client.chainId() == BigInteger.valueOf(IssuerSpace.chainId)) { "WRONG_CHAIN" }
        val latest = client.transactionCount(request.from, "latest")
        val pending = client.transactionCount(request.from, "pending")
        require(latest == pending) { "PENDING_TRANSACTION" }
        preflight()
    }

    suspend fun submit(
        operationId: String,
        request: ContractTransactionRequest,
        preflight: suspend () -> Unit,
        permit: StudioWritePermit,
        onChanged: (PersistedTransactionOperation) -> Unit = {},
    ): ContractTransactionResult {
        val operation = requireOperation(operationId, request, TransactionOperationState.READY_TO_SUBMIT)
        try {
            permit.requireRunner(identity)
            permit.requireRequest(operationId, request)
            require(client.chainId() == BigInteger.valueOf(IssuerSpace.chainId)) { "WRONG_CHAIN" }
            val latest = client.transactionCount(request.from, "latest")
            val pending = client.transactionCount(request.from, "pending")
            require(latest == pending) { "PENDING_TRANSACTION" }
            preflight()
            val transactionJson = EthereumTransactionJsonBuilder.build(
                EthereumTransactionPayload(
                    request.from, request.to, BigInteger.ZERO, request.data,
                    BigInteger.valueOf(IssuerSpace.chainId),
                ),
            )
            EthereumTransactionJsonBuilder.requireChainAgreement(
                transactionJson, BigInteger.valueOf(IssuerSpace.chainId),
            )
            permit.requireRequest(operationId, request)
            permit.requireRunner(identity)
            val claimed = engine.claimProviderSubmission(operation.operationId, latest.toString(), pending.toString())
            val hash = withContext(NonCancellable) {
                try {
                    onChanged(claimed)
                    permit.requireRequest(operationId, request)
                    permit.requireRunner(identity)
                    engine.beginProviderInvocation(operationId)
                    // No product callbacks or suspension between the marker and this final identity check/send.
                    permit.requireRunner(identity)
                    val received = MobileIssuerAdmissionRunner.parseTransactionHash(
                        walletProvider.sendTransaction(MobileIssuerAdmissionRunner.jsonObjectParam(transactionJson)),
                    ) ?: error("UNKNOWN_TRANSACTION_RESULT")
                    engine.recordHash(operationId, received)
                    received
                } finally {
                    engine.finishProviderInvocation(operationId)
                }
            }
            onChanged(checkNotNull(engine.find(operationId)))
            return reconcile(operationId, request, hash, latest, onChanged)
        } catch (error: CancellationException) {
            if (engine.find(operationId)?.state == TransactionOperationState.SUBMISSION_CLAIMED) {
                engine.cancelUninvokedClaim(operationId)
            }
            throw error
        } catch (error: Throwable) {
            val current = engine.find(operationId) ?: throw error
            val category = safeCategory(error)
            if (current.state == TransactionOperationState.SUBMISSION_CLAIMED) {
                val cancelled = engine.cancelUninvokedClaim(operationId)
                // The product store may still be unavailable. The journal owns the safe outcome.
                runCatching { onChanged(cancelled) }
                return ContractTransactionResult(MobileIssuerStatus.FAILED, category)
            }
            if (current.state == TransactionOperationState.READY_TO_SUBMIT) {
                onChanged(
                    engine.recordPreSubmitFailure(
                        operationId, category, "CONTRACT_PREFLIGHT",
                        StudioErrors.exceptionClass(error), null,
                    ),
                )
                return ContractTransactionResult(MobileIssuerStatus.FAILED, category)
            }
            if (current.state !in TERMINAL && current.state != TransactionOperationState.UNKNOWN) {
                onChanged(engine.markUnknown(operationId, category, "CONTRACT_SUBMIT"))
            }
            return ContractTransactionResult(MobileIssuerStatus.UNKNOWN, category, current.txHash)
        }
    }

    suspend fun recover(
        operationId: String,
        request: ContractTransactionRequest,
        onChanged: (PersistedTransactionOperation) -> Unit = {},
    ): ContractTransactionResult {
        val operation = engine.find(operationId) ?: error("TRANSACTION_OPERATION_NOT_FOUND")
        requireOperation(operationId, request)
        if (engine.providerInvocationLive(operationId)) {
            return ContractTransactionResult(MobileIssuerStatus.UNKNOWN, "PROVIDER_REQUEST_OUTSTANDING", operation.txHash)
        }
        when (operation.state) {
            TransactionOperationState.SUBMISSION_CLAIMED -> {
                onChanged(engine.cancelUninvokedClaim(operationId))
                return ContractTransactionResult(MobileIssuerStatus.FAILED, "NO_BROADCAST_PROVEN")
            }
            TransactionOperationState.CONFIRMED -> return verifyConfirmed(operation, request)
            TransactionOperationState.REVERTED -> return ContractTransactionResult(MobileIssuerStatus.FAILED, "REVERTED_RECEIPT", operation.txHash)
            TransactionOperationState.NO_BROADCAST_PROVEN, TransactionOperationState.CANCELLED ->
                return ContractTransactionResult(MobileIssuerStatus.FAILED, "NO_BROADCAST_PROVEN")
            else -> Unit
        }
        var hash = operation.txHash
        if (hash == null) {
            if (operation.state in setOf(TransactionOperationState.DRAFT,
                    TransactionOperationState.READY_TO_REVIEW, TransactionOperationState.READY_TO_SUBMIT)) {
                // A background recovery must not cancel another session's reviewed preparation.
                return ContractTransactionResult(MobileIssuerStatus.FAILED, "REVIEW_REQUIRED")
            }
            // A previous process may have submitted remotely. Nonce equality cannot prove absence.
            try {
                require(client.chainId() == BigInteger.valueOf(IssuerSpace.chainId)) { "WRONG_CHAIN" }
                hash = discoverRecentHash(operation, request)
                if (hash != null) onChanged(engine.recordHash(operationId, hash))
            } catch (error: Throwable) {
                if (error is CancellationException) throw error
            }
            if (hash == null) {
                onChanged(engine.markUnknown(operationId, "SUBMISSION_OUTCOME_UNRESOLVED", "CONTRACT_RECOVERY"))
                return ContractTransactionResult(MobileIssuerStatus.UNKNOWN, "SUBMISSION_OUTCOME_UNRESOLVED")
            }
        }
        val preNonce = operation.preLatestNonce?.let(::BigInteger)
            ?: return ContractTransactionResult(MobileIssuerStatus.UNKNOWN, "RECOVERY_NONCE_MISSING", hash)
        return try {
            require(client.chainId() == BigInteger.valueOf(IssuerSpace.chainId)) { "WRONG_CHAIN" }
            reconcile(operationId, request, hash, preNonce, onChanged)
        } catch (error: CancellationException) {
            throw error
        } catch (error: Throwable) {
            val current = engine.find(operationId)!!
            if (current.state !in TERMINAL && current.state != TransactionOperationState.UNKNOWN) {
                onChanged(engine.markUnknown(operationId, safeCategory(error), "CONTRACT_RECOVERY"))
            }
            ContractTransactionResult(MobileIssuerStatus.UNKNOWN, safeCategory(error), hash)
        }
    }

    private suspend fun reconcile(
        operationId: String,
        request: ContractTransactionRequest,
        hash: String,
        preNonce: BigInteger,
        onChanged: (PersistedTransactionOperation) -> Unit,
    ): ContractTransactionResult {
        move(operationId, setOf(TransactionOperationState.HASH_RECEIVED, TransactionOperationState.UNKNOWN),
            TransactionOperationState.CONFIRMING, onChanged)
        repeat(attempts) { attempt ->
            val transaction = client.transactionByHash(hash)
            val receipt = client.receipt(hash)
            if (transaction != null) {
                validateTransaction(transaction, request, hash, preNonce)
            }
            if (transaction != null && receipt != null) {
                val (block, successful) = validateReceipt(receipt, transaction, request, hash)
                if (!successful) {
                    onChanged(engine.markReverted(operationId, block.toString()))
                    return ContractTransactionResult(MobileIssuerStatus.FAILED, "REVERTED_RECEIPT", hash, block)
                }
                move(operationId, setOf(TransactionOperationState.CONFIRMING),
                    TransactionOperationState.ONCHAIN_READBACK, onChanged)
                val expectedNonce = preNonce + BigInteger.ONE
                val latest = client.transactionCount(request.from, "latest")
                val pending = client.transactionCount(request.from, "pending")
                if (latest >= expectedNonce && pending >= latest) {
                    if (engine.find(operationId)?.state == TransactionOperationState.CONFIRMED) {
                        return ContractTransactionResult(MobileIssuerStatus.CONFIRMED, "CONFIRMED", hash, block)
                    }
                    val confirmed = engine.confirm(
                        operationId, block.toString(), latest.toString(), pending.toString(),
                    )
                    onChanged(confirmed)
                    return ContractTransactionResult(MobileIssuerStatus.CONFIRMED, "CONFIRMED", hash, block)
                }
            }
            if (attempt + 1 < attempts) wait(pollDelayMillis)
        }
        val current = engine.find(operationId)!!
        if (current.state !in TERMINAL && current.state != TransactionOperationState.UNKNOWN) {
            onChanged(engine.markUnknown(operationId, "READBACK_TIMEOUT", "CONTRACT_RECOVERY"))
        }
        return ContractTransactionResult(MobileIssuerStatus.UNKNOWN, "READBACK_TIMEOUT", hash)
    }

    private suspend fun verifyConfirmed(operation: PersistedTransactionOperation,
        request: ContractTransactionRequest): ContractTransactionResult = try {
        require(client.chainId() == BigInteger.valueOf(IssuerSpace.chainId)) { "WRONG_CHAIN" }
        val hash = checkNotNull(operation.txHash)
        val transaction = checkNotNull(client.transactionByHash(hash))
        val receipt = checkNotNull(client.receipt(hash))
        validateTransaction(transaction, request, hash, BigInteger(checkNotNull(operation.preLatestNonce)))
        val (block, successful) = validateReceipt(receipt, transaction, request, hash)
        require(successful && block == operation.receiptBlock?.let(::BigInteger)) { "RECEIPT_BLOCK_MISMATCH" }
        ContractTransactionResult(MobileIssuerStatus.CONFIRMED, "CONFIRMED", hash, block)
    } catch (error: CancellationException) { throw error
    } catch (error: Throwable) {
        // Preserve historical evidence; an inconsistent RPC response is never new confirmation.
        ContractTransactionResult(MobileIssuerStatus.UNKNOWN, safeCategory(error), operation.txHash)
    }

    private fun validateTransaction(
        value: ReadOnlyJsonValue.ObjectValue,
        request: ContractTransactionRequest,
        hash: String,
        nonce: BigInteger,
    ) {
        require(MobileIssuerAdmissionRunner.parseQuantity(field(value, "nonce")) == nonce) { "TRANSACTION_NONCE_MISMATCH" }
        require(MobileIssuerAdmissionRunner.parseQuantity(field(value, "chainId")) == BigInteger.valueOf(IssuerSpace.chainId)) { "WRONG_CHAIN" }
        require(field(value, "hash").equals(hash, true)) { "TRANSACTION_HASH_MISMATCH" }
        require(field(value, "from").equals(request.from, true)) { "TRANSACTION_FROM_MISMATCH" }
        require(field(value, "to").equals(request.to, true)) { "TRANSACTION_TO_MISMATCH" }
        require(MobileIssuerAdmissionRunner.parseQuantity(field(value, "value")) == BigInteger.ZERO) {
            "TRANSACTION_VALUE_MISMATCH"
        }
        require(field(value, "input").equals(request.data, true)) { "TRANSACTION_DATA_MISMATCH" }
    }

    private fun validateReceipt(
        value: ReadOnlyJsonValue.ObjectValue,
        transaction: ReadOnlyJsonValue.ObjectValue,
        request: ContractTransactionRequest,
        hash: String,
    ): Pair<BigInteger, Boolean> {
        require(field(value, "transactionHash").equals(hash, true)) { "RECEIPT_HASH_MISMATCH" }
        require(field(value, "blockHash").matches(Regex("^0x[0-9a-fA-F]{64}$")) &&
            field(value, "blockHash").equals(field(transaction, "blockHash"), true) &&
            field(value, "blockNumber").equals(field(transaction, "blockNumber"), true) &&
            field(value, "transactionIndex").equals(field(transaction, "transactionIndex"), true)
        ) { "RECEIPT_BLOCK_MISMATCH" }
        require(field(value, "from").equals(request.from, true)) { "RECEIPT_FROM_MISMATCH" }
        require(field(value, "to").equals(request.to, true)) { "RECEIPT_TO_MISMATCH" }
        val block = MobileIssuerAdmissionRunner.parseQuantity(field(value, "blockNumber"))
        require(block.signum() >= 0 && MobileIssuerAdmissionRunner.parseQuantity(field(value, "transactionIndex")).signum() >= 0) {
            "RECEIPT_BLOCK_MISMATCH"
        }
        val status = MobileIssuerAdmissionRunner.parseQuantity(field(value, "status"))
        require(status == BigInteger.ZERO || status == BigInteger.ONE) { "RECEIPT_STATUS_INVALID" }
        return block to (status == BigInteger.ONE)
    }

    private fun requireOperation(
        operationId: String,
        request: ContractTransactionRequest,
        state: TransactionOperationState? = null,
    ): PersistedTransactionOperation {
        val operation = engine.find(operationId) ?: error("TRANSACTION_OPERATION_NOT_FOUND")
        if (state != null) require(operation.state == state) { "TRANSACTION_OPERATION_NOT_READY" }
        require(operation.operationType == request.operationType) { "TRANSACTION_OPERATION_TYPE_MISMATCH" }
        require(operation.walletAddress.equals(request.from, true)) { "TRANSACTION_WALLET_MISMATCH" }
        require(operation.targetAddress.equals(request.to, true)) { "TRANSACTION_TARGET_MISMATCH" }
        require(operation.chainId == IssuerSpace.chainId && operation.valueWei == "0") { "TRANSACTION_CHAIN_OR_VALUE_MISMATCH" }
        require(operation.dataSummary == CredentialAbi.calldataFingerprint(request.data)) { "TRANSACTION_DATA_MISMATCH" }
        return operation
    }

    private fun move(
        operationId: String,
        expected: Set<TransactionOperationState>,
        target: TransactionOperationState,
        onChanged: (PersistedTransactionOperation) -> Unit,
    ) {
        if (engine.find(operationId)?.state in expected) onChanged(engine.transition(operationId, target))
    }

    private fun field(value: ReadOnlyJsonValue.ObjectValue, name: String): String =
        (value.fields[name] as? ReadOnlyJsonValue.StringValue)?.value ?: error("MALFORMED_$name")

    private fun safeCategory(error: Throwable) = StudioErrors.category(error)

    private suspend fun discoverRecentHash(operation: PersistedTransactionOperation, request: ContractTransactionRequest): String? {
        val nonce = operation.preLatestNonce?.let(::BigInteger) ?: return null
        val head = client.blockByNumber("latest", true) ?: return null
        val number = MobileIssuerAdmissionRunner.parseQuantity(field(head, "number"))
        repeat(12) { index ->
            val block = if (index == 0) head else client.blockByNumber("0x${(number - BigInteger.valueOf(index.toLong())).toString(16)}", true)
            val transactions = (block?.fields?.get("transactions") as? ReadOnlyJsonValue.ArrayValue)?.values.orEmpty()
            for (item in transactions) {
                val tx = item as? ReadOnlyJsonValue.ObjectValue ?: continue
                if (field(tx, "from").equals(request.from, true) &&
                    MobileIssuerAdmissionRunner.parseQuantity(field(tx, "nonce")) == nonce) {
                    val hash = field(tx, "hash")
                    validateTransaction(tx, request, hash, nonce)
                    return hash
                }
            }
        }
        return null
    }

    companion object {
        const val REGISTER_OPERATION = "STAFF_CREDENTIAL_REGISTER"
        const val RECORDS_OPERATION = "STAFF_CREDENTIAL_RECORDS"
        fun registerOperation(fullName: String) = StudioOperationIdentity.type(StudioActionType.ISSUE_REGISTER, fullName)
        fun recordsOperation(fullName: String) = StudioOperationIdentity.type(StudioActionType.ISSUE_CONFIGURE, fullName)
        private val TERMINAL = setOf(
            TransactionOperationState.CONFIRMED,
            TransactionOperationState.REVERTED,
            TransactionOperationState.NO_BROADCAST_PROVEN,
            TransactionOperationState.CANCELLED,
        )
    }
}
