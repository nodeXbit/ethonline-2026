package io.github.nodexbit.ethonline2026

import java.util.UUID

enum class TransactionOperationState {
    DRAFT,
    READY_TO_REVIEW,
    READY_TO_SUBMIT,
    SUBMITTING_NO_HASH,
    HASH_RECEIVED,
    CONFIRMING,
    ONCHAIN_READBACK,
    CONFIRMED,
    REVERTED,
    UNKNOWN,
    NO_BROADCAST_PROVEN,
    CANCELLED,
    LEGACY_ATTEMPT_REQUIRES_RECONCILIATION,
}

data class TransactionIntent(
    val operationType: String,
    val walletAddress: String,
    val chainId: Long,
    val targetAddress: String,
    val valueWei: String,
    val dataSummary: String,
)

data class PersistedTransactionOperation(
    val operationId: String,
    val operationType: String,
    val walletAddress: String,
    val chainId: Long,
    val targetAddress: String,
    val valueWei: String,
    val dataSummary: String,
    val preLatestNonce: String? = null,
    val prePendingNonce: String? = null,
    val txHash: String? = null,
    val state: TransactionOperationState,
    val receiptBlock: String? = null,
    val createdAt: Long,
    val updatedAt: Long,
    val safeErrorCategory: String? = null,
    val postLatestNonce: String? = null,
    val postPendingNonce: String? = null,
    val failureStage: String? = null,
    val safeExceptionClass: String? = null,
    val safeErrorMessage: String? = null,
)

interface TransactionJournalStore {
    fun load(): String?
    fun save(serializedJournal: String)
}

class TransactionJournal(private val store: TransactionJournalStore) {
    private val operations = decode(store.load())

    @Synchronized
    fun all(): List<PersistedTransactionOperation> = operations.toList()

    @Synchronized
    fun find(operationId: String): PersistedTransactionOperation? =
        operations.firstOrNull { it.operationId == operationId }

    @Synchronized
    fun latestForWallet(
        walletAddress: String,
        operationType: String? = null,
    ): PersistedTransactionOperation? =
        operations
            .filter {
                it.walletAddress.equals(walletAddress, ignoreCase = true) &&
                    (operationType == null || it.operationType == operationType)
            }
            .maxByOrNull { it.createdAt }

    @Synchronized
    fun activeForWallet(walletAddress: String): PersistedTransactionOperation? =
        operations
            .filter { it.walletAddress.equals(walletAddress, ignoreCase = true) && !it.state.isTerminal() }
            .maxByOrNull { it.createdAt }

    @Synchronized
    fun put(operation: PersistedTransactionOperation): PersistedTransactionOperation {
        val persisted = operations.toMutableList()
        val index = persisted.indexOfFirst { it.operationId == operation.operationId }
        if (index >= 0) persisted[index] = operation else persisted += operation
        store.save(encode(persisted))
        operations.clear()
        operations.addAll(persisted)
        return operation
    }

    private fun TransactionOperationState.isTerminal(): Boolean = this in setOf(
        TransactionOperationState.CONFIRMED,
        TransactionOperationState.REVERTED,
        TransactionOperationState.NO_BROADCAST_PROVEN,
        TransactionOperationState.CANCELLED,
    )

    private fun encode(values: List<PersistedTransactionOperation>): String = values.joinToString("\n") { value ->
        listOf(
            value.operationId,
            value.operationType,
            value.walletAddress,
            value.chainId.toString(),
            value.targetAddress,
            value.valueWei,
            value.dataSummary,
            nullable(value.preLatestNonce),
            nullable(value.prePendingNonce),
            nullable(value.txHash),
            value.state.name,
            nullable(value.receiptBlock),
            value.createdAt.toString(),
            value.updatedAt.toString(),
            nullable(value.safeErrorCategory),
            nullable(value.postLatestNonce),
            nullable(value.postPendingNonce),
            nullable(value.failureStage),
            nullable(value.safeExceptionClass),
            nullable(value.safeErrorMessage),
        ).joinToString("|") { escape(it) }
    }

    private fun decode(serialized: String?): MutableList<PersistedTransactionOperation> {
        if (serialized.isNullOrBlank()) return mutableListOf()
        return serialized.lineSequence().map { line ->
                val fields = line.split('|').map(::unescape)
                require(fields.size == 17 || fields.size == 20)
                PersistedTransactionOperation(
                    operationId = fields[0],
                    operationType = fields[1],
                    walletAddress = fields[2],
                    chainId = fields[3].toLong(),
                    targetAddress = fields[4],
                    valueWei = fields[5],
                    dataSummary = fields[6],
                    preLatestNonce = nonNull(fields[7]),
                    prePendingNonce = nonNull(fields[8]),
                    txHash = nonNull(fields[9]),
                    state = TransactionOperationState.valueOf(fields[10]),
                    receiptBlock = nonNull(fields[11]),
                    createdAt = fields[12].toLong(),
                    updatedAt = fields[13].toLong(),
                    safeErrorCategory = nonNull(fields[14]),
                    postLatestNonce = nonNull(fields[15]),
                    postPendingNonce = nonNull(fields[16]),
                    failureStage = fields.getOrNull(17)?.let(::nonNull),
                    safeExceptionClass = fields.getOrNull(18)?.let(::nonNull),
                    safeErrorMessage = fields.getOrNull(19)?.let(::nonNull),
                )
        }.toMutableList()
    }

    private fun nullable(value: String?): String = value?.let { "v$it" } ?: "n"
    private fun nonNull(value: String): String? = if (value == "n") null else value.removePrefix("v")
    private fun escape(value: String): String = value
        .replace("%", "%25")
        .replace("|", "%7C")
        .replace("\r", "%0D")
        .replace("\n", "%0A")

    private fun unescape(value: String): String = value
        .replace("%0A", "\n")
        .replace("%0D", "\r")
        .replace("%7C", "|")
        .replace("%25", "%")
}

class RecoverableTransactionEngine(
    private val journal: TransactionJournal,
    private val now: () -> Long = System::currentTimeMillis,
    private val newOperationId: () -> String = { UUID.randomUUID().toString() },
) {
    fun all(): List<PersistedTransactionOperation> = journal.all()
    fun find(operationId: String): PersistedTransactionOperation? = journal.find(operationId)
    fun latestForWallet(
        walletAddress: String,
        operationType: String? = null,
    ): PersistedTransactionOperation? = journal.latestForWallet(walletAddress, operationType)

    @Synchronized
    fun create(intent: TransactionIntent): PersistedTransactionOperation {
        val latestMatchingOperation = journal.latestForWallet(intent.walletAddress, intent.operationType)
        require(latestMatchingOperation?.state != TransactionOperationState.NO_BROADCAST_PROVEN) {
            "REARM_REQUIRED"
        }
        return createInternal(intent)
    }

    private fun createInternal(intent: TransactionIntent): PersistedTransactionOperation {
        require(journal.activeForWallet(intent.walletAddress) == null) {
            "ACTIVE_OPERATION_EXISTS"
        }
        val timestamp = now()
        return journal.put(
            PersistedTransactionOperation(
                operationId = newOperationId(),
                operationType = intent.operationType,
                walletAddress = intent.walletAddress,
                chainId = intent.chainId,
                targetAddress = intent.targetAddress,
                valueWei = intent.valueWei,
                dataSummary = intent.dataSummary,
                state = TransactionOperationState.DRAFT,
                createdAt = timestamp,
                updatedAt = timestamp,
            ),
        )
    }

    @Synchronized
    fun migrateLegacy(intent: TransactionIntent): PersistedTransactionOperation {
        journal.latestForWallet(intent.walletAddress, intent.operationType)?.let { return it }
        require(journal.activeForWallet(intent.walletAddress) == null) {
            "ACTIVE_OPERATION_EXISTS"
        }
        val timestamp = now()
        return journal.put(
            PersistedTransactionOperation(
                operationId = newOperationId(),
                operationType = intent.operationType,
                walletAddress = intent.walletAddress,
                chainId = intent.chainId,
                targetAddress = intent.targetAddress,
                valueWei = intent.valueWei,
                dataSummary = intent.dataSummary,
                preLatestNonce = "0",
                prePendingNonce = "0",
                state = TransactionOperationState.LEGACY_ATTEMPT_REQUIRES_RECONCILIATION,
                createdAt = timestamp,
                updatedAt = timestamp,
                safeErrorCategory = "LEGACY_BOOLEAN_WITHOUT_TRANSACTION_EVIDENCE",
            ),
        )
    }

    fun migrateLegacyAttemptIfNeeded(
        intent: TransactionIntent,
        legacyAttempted: Boolean,
        alreadyMigrated: Boolean,
    ): PersistedTransactionOperation? = if (legacyAttempted && !alreadyMigrated) {
        migrateLegacy(intent)
    } else {
        null
    }

    @Synchronized
    fun transition(operationId: String, target: TransactionOperationState): PersistedTransactionOperation {
        val current = required(operationId)
        require(target in legalTransitions.getValue(current.state)) {
            "ILLEGAL_TRANSACTION_STATE_TRANSITION_${current.state}_TO_$target"
        }
        return journal.put(
            current.copy(
                state = target,
                updatedAt = now(),
                safeErrorCategory = null,
                failureStage = null,
                safeExceptionClass = null,
                safeErrorMessage = null,
            ),
        )
    }

    @Synchronized
    fun beginSubmission(
        operationId: String,
        preLatestNonce: String,
        prePendingNonce: String,
    ): PersistedTransactionOperation {
        val current = required(operationId)
        require(current.state == TransactionOperationState.READY_TO_SUBMIT) { "OPERATION_NOT_READY_TO_SUBMIT" }
        require(preLatestNonce == prePendingNonce) { "PENDING_TRANSACTION" }
        return journal.put(
            current.copy(
                state = TransactionOperationState.SUBMITTING_NO_HASH,
                preLatestNonce = preLatestNonce,
                prePendingNonce = prePendingNonce,
                updatedAt = now(),
                safeErrorCategory = null,
                failureStage = null,
                safeExceptionClass = null,
                safeErrorMessage = null,
            ),
        )
    }

    @Synchronized
    fun recordPreSubmitFailure(
        operationId: String,
        safeCategory: String,
        failureStage: String,
        safeExceptionClass: String? = null,
        safeErrorMessage: String? = null,
    ): PersistedTransactionOperation {
        val current = required(operationId)
        require(current.state == TransactionOperationState.READY_TO_SUBMIT) {
            "PRE_SUBMIT_FAILURE_REQUIRES_READY_TO_SUBMIT"
        }
        return journal.put(
            current.copy(
                updatedAt = now(),
                safeErrorCategory = safeCategory.take(96),
                failureStage = failureStage.take(48),
                safeExceptionClass = safeExceptionClass?.take(64),
                safeErrorMessage = safeErrorMessage?.take(160),
            ),
        )
    }

    @Synchronized
    fun recordHash(operationId: String, txHash: String): PersistedTransactionOperation {
        require(HASH.matches(txHash)) { "MALFORMED_TRANSACTION_HASH" }
        val current = required(operationId)
        require(current.state == TransactionOperationState.SUBMITTING_NO_HASH) { "HASH_NOT_EXPECTED" }
        require(current.txHash == null) { "HASH_ALREADY_RECORDED" }
        return journal.put(
            current.copy(
                txHash = txHash.lowercase(),
                state = TransactionOperationState.HASH_RECEIVED,
                updatedAt = now(),
            ),
        )
    }

    @Synchronized
    fun markUnknown(
        operationId: String,
        safeCategory: String,
        failureStage: String? = null,
        safeExceptionClass: String? = null,
        safeErrorMessage: String? = null,
    ): PersistedTransactionOperation {
        val current = required(operationId)
        require(current.state !in terminalStates) { "TERMINAL_OPERATION" }
        return journal.put(
            current.copy(
                state = TransactionOperationState.UNKNOWN,
                updatedAt = now(),
                safeErrorCategory = safeCategory.take(96),
                failureStage = failureStage?.take(48),
                safeExceptionClass = safeExceptionClass?.take(64),
                safeErrorMessage = safeErrorMessage?.take(160),
            ),
        )
    }

    @Synchronized
    fun proveNoBroadcast(
        operationId: String,
        latestNonce: String,
        pendingNonce: String,
    ): PersistedTransactionOperation {
        val current = required(operationId)
        require(current.txHash == null) { "HASH_EXISTS" }
        require(current.state in noHashRecoveryStates) { "NO_BROADCAST_PROOF_NOT_ALLOWED" }
        val unchanged = latestNonce == current.preLatestNonce && pendingNonce == current.prePendingNonce
        return journal.put(
            current.copy(
                state = if (unchanged) {
                    TransactionOperationState.NO_BROADCAST_PROVEN
                } else {
                    TransactionOperationState.UNKNOWN
                },
                updatedAt = now(),
                safeErrorCategory = if (unchanged) {
                    current.safeErrorCategory
                } else {
                    "NONCE_CHANGED_NO_BROADCAST_NOT_PROVEN"
                },
            ),
        )
    }

    @Synchronized
    fun confirm(
        operationId: String,
        receiptBlock: String,
        postLatestNonce: String,
        postPendingNonce: String,
    ): PersistedTransactionOperation {
        val current = required(operationId)
        require(current.txHash != null) { "CONFIRMATION_REQUIRES_HASH" }
        require(current.state == TransactionOperationState.ONCHAIN_READBACK) { "READBACK_NOT_COMPLETE" }
        return journal.put(
            current.copy(
                state = TransactionOperationState.CONFIRMED,
                receiptBlock = receiptBlock,
                postLatestNonce = postLatestNonce,
                postPendingNonce = postPendingNonce,
                updatedAt = now(),
                safeErrorCategory = null,
            ),
        )
    }

    @Synchronized
    fun markReverted(operationId: String, receiptBlock: String?): PersistedTransactionOperation {
        val current = required(operationId)
        require(current.txHash != null) { "REVERTED_REQUIRES_HASH" }
        return journal.put(
            current.copy(
                state = TransactionOperationState.REVERTED,
                receiptBlock = receiptBlock,
                updatedAt = now(),
                safeErrorCategory = "REVERTED_RECEIPT",
            ),
        )
    }

    @Synchronized
    fun rearm(
        historicalOperationId: String,
        intent: TransactionIntent,
    ): PersistedTransactionOperation {
        val historical = required(historicalOperationId)
        require(historical.state == TransactionOperationState.NO_BROADCAST_PROVEN) {
            "REARM_REQUIRES_NO_BROADCAST_PROOF"
        }
        require(historical.walletAddress.equals(intent.walletAddress, ignoreCase = true)) { "REARM_WALLET_MISMATCH" }
        return createInternal(intent)
    }

    private fun required(operationId: String): PersistedTransactionOperation =
        journal.find(operationId) ?: error("TRANSACTION_OPERATION_NOT_FOUND")

    companion object {
        private val HASH = Regex("^0x[0-9a-fA-F]{64}$")
        private val terminalStates = setOf(
            TransactionOperationState.CONFIRMED,
            TransactionOperationState.REVERTED,
            TransactionOperationState.NO_BROADCAST_PROVEN,
            TransactionOperationState.CANCELLED,
        )
        private val noHashRecoveryStates = setOf(
            TransactionOperationState.SUBMITTING_NO_HASH,
            TransactionOperationState.UNKNOWN,
            TransactionOperationState.LEGACY_ATTEMPT_REQUIRES_RECONCILIATION,
        )
        private val legalTransitions = mapOf(
            TransactionOperationState.DRAFT to setOf(
                TransactionOperationState.READY_TO_REVIEW,
                TransactionOperationState.CANCELLED,
            ),
            TransactionOperationState.READY_TO_REVIEW to setOf(
                TransactionOperationState.READY_TO_SUBMIT,
                TransactionOperationState.CANCELLED,
            ),
            TransactionOperationState.READY_TO_SUBMIT to setOf(
                TransactionOperationState.SUBMITTING_NO_HASH,
                TransactionOperationState.CANCELLED,
            ),
            TransactionOperationState.SUBMITTING_NO_HASH to setOf(TransactionOperationState.UNKNOWN),
            TransactionOperationState.HASH_RECEIVED to setOf(
                TransactionOperationState.CONFIRMING,
                TransactionOperationState.UNKNOWN,
            ),
            TransactionOperationState.CONFIRMING to setOf(
                TransactionOperationState.ONCHAIN_READBACK,
                TransactionOperationState.REVERTED,
                TransactionOperationState.UNKNOWN,
            ),
            TransactionOperationState.ONCHAIN_READBACK to setOf(
                TransactionOperationState.CONFIRMED,
                TransactionOperationState.REVERTED,
                TransactionOperationState.UNKNOWN,
            ),
            TransactionOperationState.UNKNOWN to setOf(TransactionOperationState.CONFIRMING),
            TransactionOperationState.CONFIRMED to emptySet(),
            TransactionOperationState.REVERTED to emptySet(),
            TransactionOperationState.NO_BROADCAST_PROVEN to emptySet(),
            TransactionOperationState.CANCELLED to emptySet(),
            TransactionOperationState.LEGACY_ATTEMPT_REQUIRES_RECONCILIATION to emptySet(),
        )

        fun sepoliaExplorerUrl(transactionHash: String): String {
            require(HASH.matches(transactionHash)) { "MALFORMED_TRANSACTION_HASH" }
            return "https://sepolia.etherscan.io/tx/${transactionHash.lowercase()}"
        }
    }
}
