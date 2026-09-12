package io.github.nodexbit.ethonline2026

import java.math.BigInteger
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay

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
    private val walletProvider: MobileIssuerWalletProvider,
    private val client: ReadOnlyEthereumRpcClient,
    private val engine: RecoverableTransactionEngine,
    private val wait: suspend (Long) -> Unit = { delay(it) },
    private val attempts: Int = 30,
    private val pollDelayMillis: Long = 1_500L,
) {
    fun create(request: ContractTransactionRequest): PersistedTransactionOperation = engine.create(
        TransactionIntent(
            operationType = request.operationType,
            walletAddress = request.from,
            chainId = IssuerSpace.chainId,
            targetAddress = request.to,
            valueWei = "0",
            dataSummary = CredentialAbi.calldataFingerprint(request.data),
        ),
    )

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
        walletProvider.switchToSepolia()
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
        onChanged: (PersistedTransactionOperation) -> Unit = {},
    ): ContractTransactionResult {
        val operation = requireOperation(operationId, request, TransactionOperationState.READY_TO_SUBMIT)
        try {
            walletProvider.switchToSepolia()
            require(client.chainId() == BigInteger.valueOf(IssuerSpace.chainId)) { "WRONG_CHAIN" }
            val latest = client.transactionCount(request.from, "latest")
            val pending = client.transactionCount(request.from, "pending")
            require(latest == pending) { "PENDING_TRANSACTION" }
            preflight()
            val claimed = engine.beginSubmission(operation.operationId, latest.toString(), pending.toString())
            onChanged(claimed)
            val transactionJson = EthereumTransactionJsonBuilder.build(
                EthereumTransactionPayload(
                    request.from, request.to, BigInteger.ZERO, request.data,
                    BigInteger.valueOf(IssuerSpace.chainId),
                ),
            )
            EthereumTransactionJsonBuilder.requireChainAgreement(
                transactionJson, BigInteger.valueOf(IssuerSpace.chainId),
            )
            val hash = MobileIssuerAdmissionRunner.parseTransactionHash(
                walletProvider.sendTransaction(MobileIssuerAdmissionRunner.jsonObjectParam(transactionJson)),
            ) ?: error("UNKNOWN_TRANSACTION_RESULT")
            onChanged(engine.recordHash(operationId, hash))
            return reconcile(operationId, request, hash, latest, onChanged)
        } catch (error: CancellationException) {
            throw error
        } catch (error: Throwable) {
            val current = engine.find(operationId) ?: throw error
            val category = safeCategory(error)
            if (current.state == TransactionOperationState.READY_TO_SUBMIT) {
                onChanged(
                    engine.recordPreSubmitFailure(
                        operationId, category, "CONTRACT_PREFLIGHT",
                        error::class.simpleName, safeMessage(error),
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
        val hash = operation.txHash ?: return ContractTransactionResult(
            MobileIssuerStatus.UNKNOWN, "RECOVERY_HASH_MISSING",
        )
        val preNonce = operation.preLatestNonce?.let(::BigInteger)
            ?: return ContractTransactionResult(MobileIssuerStatus.UNKNOWN, "RECOVERY_NONCE_MISSING", hash)
        return try {
            walletProvider.switchToSepolia()
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
        var transactionSeen = false
        repeat(attempts) { attempt ->
            val transaction = client.transactionByHash(hash)
            val receipt = client.receipt(hash)
            if (transaction != null) {
                validateTransaction(transaction, request, hash)
                transactionSeen = true
            }
            if (transactionSeen && receipt != null) {
                val (block, successful) = validateReceipt(receipt, request)
                if (!successful) {
                    onChanged(engine.markReverted(operationId, block.toString()))
                    return ContractTransactionResult(MobileIssuerStatus.FAILED, "REVERTED_RECEIPT", hash, block)
                }
                move(operationId, setOf(TransactionOperationState.CONFIRMING),
                    TransactionOperationState.ONCHAIN_READBACK, onChanged)
                val expectedNonce = preNonce + BigInteger.ONE
                val latest = client.transactionCount(request.from, "latest")
                val pending = client.transactionCount(request.from, "pending")
                if (latest == expectedNonce && pending == expectedNonce) {
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

    private fun validateTransaction(
        value: ReadOnlyJsonValue.ObjectValue,
        request: ContractTransactionRequest,
        hash: String,
    ) {
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
        request: ContractTransactionRequest,
    ): Pair<BigInteger, Boolean> {
        require(field(value, "from").equals(request.from, true)) { "RECEIPT_FROM_MISMATCH" }
        require(field(value, "to").equals(request.to, true)) { "RECEIPT_TO_MISMATCH" }
        val block = MobileIssuerAdmissionRunner.parseQuantity(field(value, "blockNumber"))
        return block to (MobileIssuerAdmissionRunner.parseQuantity(field(value, "status")) == BigInteger.ONE)
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

    private fun safeCategory(error: Throwable) = error.message
        ?.replace(Regex("[^A-Za-z0-9_]+"), "_")?.uppercase()?.take(80)
        ?: (error::class.simpleName ?: "ERROR").uppercase()

    private fun safeMessage(error: Throwable) = error.message
        ?.take(120)
        ?.takeUnless { Regex("(?i)(https?://|authorization|bearer|api[_ -]?key|email|otp)").containsMatchIn(it) }

    companion object {
        const val REGISTER_OPERATION = "STAFF_CREDENTIAL_REGISTER"
        const val RECORDS_OPERATION = "STAFF_CREDENTIAL_RECORDS"
        private val TERMINAL = setOf(
            TransactionOperationState.CONFIRMED,
            TransactionOperationState.REVERTED,
            TransactionOperationState.NO_BROADCAST_PROVEN,
            TransactionOperationState.CANCELLED,
        )
    }
}
