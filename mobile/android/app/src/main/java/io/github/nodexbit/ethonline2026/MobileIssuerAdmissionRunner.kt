package io.github.nodexbit.ethonline2026

import java.math.BigDecimal
import java.math.BigInteger
import java.math.RoundingMode
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay

interface MobileIssuerReadProvider {
    suspend fun request(method: String, params: List<String> = emptyList()): String
}

interface MobileIssuerWalletProvider {
    suspend fun switchToSepolia()
    suspend fun sendTransaction(transactionJson: String): String
}

interface MobileIssuerProvider : MobileIssuerReadProvider, MobileIssuerWalletProvider

class ReadOnlyMobileIssuerProvider(
    private val client: ReadOnlyEthereumRpcClient,
) : MobileIssuerReadProvider {
    override suspend fun request(method: String, params: List<String>): String =
        client.rawStringParams(method, params)
}

interface MobileIssuerTransactionObserver {
    fun validateOperation() = Unit
    fun validateIntent(
        chainId: BigInteger,
        valueWei: BigInteger,
        dataSummary: String,
    ) = Unit
    fun validateWallet(walletAddress: String) = Unit
    fun preSubmitFailure(failure: MobileIssuerSafeFailure) = Unit
    fun claimSubmission(preLatestNonce: BigInteger, prePendingNonce: BigInteger): Unit =
        error("TRANSACTION_JOURNAL_REQUIRED")
    fun hashReceived(transactionHash: String) = Unit
    fun confirming() = Unit
    fun onchainReadback() = Unit
    fun confirmed(evidence: MobileIssuerEvidence) = Unit
    fun reverted(category: String) = Unit
    fun unknown(category: String) = Unit
    fun unknown(failure: MobileIssuerSafeFailure) = unknown(failure.category)

    companion object {
        val NONE = object : MobileIssuerTransactionObserver {}
    }
}

enum class MobileIssuerStatus {
    READY,
    BLOCKED,
    SUBMITTING,
    CONFIRMED,
    FAILED,
    UNKNOWN,
}

enum class MobileIssuerReadinessStage {
    SWITCH_CHAIN,
    READ_CHAIN_ID,
    READ_BALANCE,
    READ_NONCE_LATEST,
    READ_NONCE_PENDING,
}

enum class MobileIssuerStageOutcome {
    START,
    PASS,
    FAIL,
}

data class MobileIssuerFailureDiagnostic(
    val stage: MobileIssuerReadinessStage,
    val category: String,
    val code: String? = null,
    val message: String? = null,
)

data class MobileIssuerStageEvent(
    val stage: MobileIssuerReadinessStage,
    val outcome: MobileIssuerStageOutcome,
    val diagnostic: MobileIssuerFailureDiagnostic? = null,
)

class MobileIssuerReadinessGate {
    private var generation = 0L
    private var inFlight = false

    @Synchronized
    fun confirmationChanged(checked: Boolean): Long? {
        if (!checked) {
            generation += 1
            inFlight = false
            return null
        }
        if (inFlight) return null
        generation += 1
        inFlight = true
        return generation
    }

    @Synchronized
    fun isCurrent(candidate: Long): Boolean = inFlight && candidate == generation

    @Synchronized
    fun finish(candidate: Long): Boolean {
        if (!isCurrent(candidate)) return false
        inFlight = false
        return true
    }
}

data class MobileIssuerInspection(
    val status: MobileIssuerStatus,
    val issuer: String,
    val chainId: BigInteger? = null,
    val balanceWei: BigInteger? = null,
    val latestNonce: BigInteger? = null,
    val pendingNonce: BigInteger? = null,
    val category: String,
    val diagnostic: MobileIssuerFailureDiagnostic? = null,
) {
    val canSubmit: Boolean get() = status == MobileIssuerStatus.READY
}

data class MobileIssuerEvidence(
    val issuer: String,
    val transactionHash: String,
    val blockNumber: BigInteger,
    val preNonce: BigInteger,
    val postLatestNonce: BigInteger,
    val postPendingNonce: BigInteger,
)

data class MobileIssuerResult(
    val status: MobileIssuerStatus,
    val category: String,
    val evidence: MobileIssuerEvidence? = null,
    val failure: MobileIssuerSafeFailure? = null,
)

data class MobileIssuerSafeFailure(
    val stage: String,
    val category: String,
    val exceptionClass: String? = null,
    val message: String? = null,
)

data class EthereumTransactionPayload(
    val from: String,
    val to: String,
    val valueWei: BigInteger,
    val data: String,
    val chainId: BigInteger,
)

object EthereumTransactionJsonBuilder {
    private val ADDRESS = Regex("^0x[0-9a-fA-F]{40}$")
    private val DATA = Regex("^0x(?:[0-9a-fA-F]{2})*$")
    private val CHAIN_ID_FIELD = Regex("\"chainId\"\\s*:\\s*\"(0x[0-9a-fA-F]+)\"")

    fun build(payload: EthereumTransactionPayload): String {
        require(ADDRESS.matches(payload.from)) { "INVALID_FROM_ADDRESS" }
        require(ADDRESS.matches(payload.to)) { "INVALID_TO_ADDRESS" }
        require(payload.valueWei.signum() >= 0) { "NEGATIVE_TRANSACTION_VALUE" }
        require(DATA.matches(payload.data)) { "INVALID_TRANSACTION_DATA" }
        require(payload.chainId.signum() > 0) { "CHAIN_ID_REQUIRED" }
        require(payload.chainId <= BigInteger.valueOf(Int.MAX_VALUE.toLong())) { "UNSUPPORTED_CHAIN_ID" }
        return "{\"from\":\"${payload.from}\",\"to\":\"${payload.to}\"," +
            "\"value\":\"${quantity(payload.valueWei)}\",\"data\":\"${payload.data}\"," +
            "\"chainId\":\"${quantity(payload.chainId)}\"}"
    }

    fun requireChainAgreement(transactionJson: String, selectedChainId: BigInteger) {
        val matches = CHAIN_ID_FIELD.findAll(transactionJson).toList()
        require(matches.size == 1) { "TRANSACTION_CHAIN_ID_REQUIRED" }
        val payloadChainId = MobileIssuerAdmissionRunner.parseQuantity(matches.single().groupValues[1])
        require(payloadChainId == selectedChainId) { "TRANSACTION_CHAIN_ID_MISMATCH" }
    }

    private fun quantity(value: BigInteger): String = "0x${value.toString(16)}"
}

object MobileIssuerUiPolicy {
    fun reviewEnabled(state: TransactionOperationState?): Boolean =
        state == TransactionOperationState.READY_TO_SUBMIT

    fun issuerConfirmationEnabled(state: TransactionOperationState?): Boolean =
        state == null || state == TransactionOperationState.DRAFT

    fun restoreConfirmedCheckbox(state: TransactionOperationState?): Boolean =
        state == TransactionOperationState.READY_TO_SUBMIT

    fun preSubmitFailureText(failure: MobileIssuerSafeFailure): String = buildString {
        append("STATUS:\nCould not prepare the transaction.\n")
        append("Stage: ${failure.stage}\n")
        append("Category: ${failure.category}")
        failure.message?.let { append("\nMessage: $it") }
        append("\n\nReview remains available. No transaction was submitted.")
    }

    fun operationStatusText(operation: PersistedTransactionOperation): String? {
        if (operation.state != TransactionOperationState.READY_TO_SUBMIT) return null
        val stage = operation.failureStage
            ?: return "STATUS:\nREADY TO SUBMIT\nOperation: ${operation.operationId}"
        return preSubmitFailureText(
            MobileIssuerSafeFailure(
                stage = stage,
                category = operation.safeErrorCategory ?: "ERROR",
                exceptionClass = operation.safeExceptionClass,
                message = operation.safeErrorMessage,
            ),
        )
    }
}

object MobileIssuerReviewFlow {
    fun open(
        operation: PersistedTransactionOperation,
        walletAddress: String,
    ): String {
        require(MobileIssuerUiPolicy.reviewEnabled(operation.state)) { "M1_REVIEW_NOT_AVAILABLE" }
        require(operation.operationType == MobileIssuerJournalObserver.OPERATION_TYPE) {
            "M1_OPERATION_TYPE_MISMATCH"
        }
        require(operation.walletAddress.equals(walletAddress, ignoreCase = true)) { "M1_WALLET_MISMATCH" }
        require(operation.targetAddress.equals(walletAddress, ignoreCase = true)) { "M1_TARGET_MISMATCH" }
        require(operation.chainId == MobileIssuerAdmissionRunner.SEPOLIA_CHAIN_ID.toLong()) {
            "M1_CHAIN_MISMATCH"
        }
        require(operation.valueWei == "0" && operation.dataSummary == "0x") { "M1_INTENT_MISMATCH" }
        return MobileIssuerAdmissionRunner.reviewSummary(walletAddress)
    }
}

class MobileIssuerJournalObserver(
    private val engine: RecoverableTransactionEngine,
    private val operationId: String,
    private val onChanged: (PersistedTransactionOperation) -> Unit = {},
) : MobileIssuerTransactionObserver {
    override fun validateOperation() {
        val operation = engine.find(operationId) ?: error("M1_OPERATION_NOT_FOUND")
        require(operation.state == TransactionOperationState.READY_TO_SUBMIT) {
            "M1_OPERATION_NOT_READY_TO_SUBMIT"
        }
        require(operation.operationType == OPERATION_TYPE) { "M1_OPERATION_TYPE_MISMATCH" }
    }

    override fun validateIntent(
        chainId: BigInteger,
        valueWei: BigInteger,
        dataSummary: String,
    ) {
        val operation = engine.find(operationId) ?: error("M1_OPERATION_NOT_FOUND")
        require(operation.chainId == chainId.toLong()) { "M1_CHAIN_MISMATCH" }
        require(operation.targetAddress.equals(operation.walletAddress, ignoreCase = true)) {
            "M1_TARGET_MISMATCH"
        }
        require(operation.valueWei == valueWei.toString() && operation.dataSummary == dataSummary) {
            "M1_INTENT_MISMATCH"
        }
    }

    override fun validateWallet(walletAddress: String) {
        val operation = engine.find(operationId) ?: error("M1_OPERATION_NOT_FOUND")
        require(operation.walletAddress.equals(walletAddress, ignoreCase = true)) { "M1_WALLET_MISMATCH" }
    }

    override fun preSubmitFailure(failure: MobileIssuerSafeFailure) = changed(
        engine.recordPreSubmitFailure(
            operationId = operationId,
            safeCategory = failure.category,
            failureStage = failure.stage,
            safeExceptionClass = failure.exceptionClass,
            safeErrorMessage = failure.message,
        ),
    )

    override fun claimSubmission(preLatestNonce: BigInteger, prePendingNonce: BigInteger) {
        changed(
            engine.beginSubmission(
                operationId,
                preLatestNonce.toString(),
                prePendingNonce.toString(),
            ),
        )
    }

    override fun hashReceived(transactionHash: String) =
        changed(engine.recordHash(operationId, transactionHash))

    override fun confirming() = moveIfCurrent(
        setOf(TransactionOperationState.HASH_RECEIVED, TransactionOperationState.UNKNOWN),
        TransactionOperationState.CONFIRMING,
    )

    override fun onchainReadback() = moveIfCurrent(
        setOf(TransactionOperationState.CONFIRMING),
        TransactionOperationState.ONCHAIN_READBACK,
    )

    override fun confirmed(evidence: MobileIssuerEvidence) = changed(
        engine.confirm(
            operationId,
            evidence.blockNumber.toString(),
            evidence.postLatestNonce.toString(),
            evidence.postPendingNonce.toString(),
        ),
    )

    override fun reverted(category: String) = changed(engine.markReverted(operationId, receiptBlock = null))

    override fun unknown(category: String) {
        val operation = engine.find(operationId) ?: return
        if (operation.state != TransactionOperationState.UNKNOWN && operation.state !in terminalStates) {
            changed(engine.markUnknown(operationId, category))
        }
    }

    override fun unknown(failure: MobileIssuerSafeFailure) {
        val operation = engine.find(operationId) ?: return
        if (operation.state != TransactionOperationState.UNKNOWN && operation.state !in terminalStates) {
            changed(
                engine.markUnknown(
                    operationId = operationId,
                    safeCategory = failure.category,
                    failureStage = failure.stage,
                    safeExceptionClass = failure.exceptionClass,
                    safeErrorMessage = failure.message,
                ),
            )
        }
    }

    private fun moveIfCurrent(
        expected: Set<TransactionOperationState>,
        target: TransactionOperationState,
    ) {
        val operation = engine.find(operationId) ?: return
        if (operation.state in expected) changed(engine.transition(operationId, target))
    }

    private fun changed(operation: PersistedTransactionOperation) {
        onChanged(operation)
    }

    companion object {
        const val OPERATION_TYPE = "M1_MOBILE_ISSUER_ADMISSION"
        private val terminalStates = setOf(
            TransactionOperationState.CONFIRMED,
            TransactionOperationState.REVERTED,
            TransactionOperationState.NO_BROADCAST_PROVEN,
            TransactionOperationState.CANCELLED,
        )
    }
}

class MobileIssuerAdmissionRunner(
    private val provider: MobileIssuerWalletProvider,
    private val readProvider: MobileIssuerReadProvider,
    private val wait: suspend (Long) -> Unit = { delay(it) },
    private val readbackAttempts: Int = 30,
    private val reconciliationAttempts: Int = 12,
    private val pollDelayMillis: Long = 1_500,
    private val transactionJsonBuilder: (EthereumTransactionPayload) -> String =
        EthereumTransactionJsonBuilder::build,
) {
    suspend fun inspect(
        issuer: String,
        dedicatedIssuerConfirmed: Boolean,
        onStage: (MobileIssuerStageEvent) -> Unit = {},
    ): MobileIssuerInspection {
        admissionBlockReason(issuer, dedicatedIssuerConfirmed)?.let { return blocked(issuer, it) }

        val switchResult = readinessCall(MobileIssuerReadinessStage.SWITCH_CHAIN, onStage) {
            provider.switchToSepolia()
        }
        if (switchResult is ReadinessCall.Failure) return blocked(issuer, switchResult.diagnostic)

        val chainResult = readinessCall(MobileIssuerReadinessStage.READ_CHAIN_ID, onStage) {
            parseQuantity(readProvider.request("eth_chainId"))
        }
        if (chainResult is ReadinessCall.Failure) return blocked(issuer, chainResult.diagnostic)
        val chainId = (chainResult as ReadinessCall.Success).value
        if (chainId != SEPOLIA_CHAIN_ID) return blocked(issuer, "WRONG_CHAIN", chainId)

        val balanceResult = readinessCall(MobileIssuerReadinessStage.READ_BALANCE, onStage) {
            parseQuantity(
                readProvider.request(
                    "eth_getBalance",
                    listOf(jsonStringParam(issuer), jsonStringParam("latest")),
                ),
            )
        }
        if (balanceResult is ReadinessCall.Failure) {
            return blocked(issuer, balanceResult.diagnostic, chainId = chainId)
        }
        val balance = (balanceResult as ReadinessCall.Success).value

        val latestResult = readinessCall(MobileIssuerReadinessStage.READ_NONCE_LATEST, onStage) {
            transactionCount(issuer, "latest")
        }
        if (latestResult is ReadinessCall.Failure) {
            return blocked(issuer, latestResult.diagnostic, chainId, balance)
        }
        val latest = (latestResult as ReadinessCall.Success).value

        val pendingResult = readinessCall(MobileIssuerReadinessStage.READ_NONCE_PENDING, onStage) {
            transactionCount(issuer, "pending")
        }
        if (pendingResult is ReadinessCall.Failure) {
            return blocked(issuer, pendingResult.diagnostic, chainId, balance, latest)
        }
        val pending = (pendingResult as ReadinessCall.Success).value

        if (latest != pending) {
            return blocked(issuer, "PENDING_TRANSACTION", chainId, balance, latest, pending)
        }
        if (balance == BigInteger.ZERO) {
            return blocked(issuer, "ISSUER_FUNDING_REQUIRED", chainId, balance, latest, pending)
        }
        return MobileIssuerInspection(
            MobileIssuerStatus.READY,
            issuer,
            chainId,
            balance,
            latest,
            pending,
            "READY_GAS_VALIDATED_BY_PROVIDER_AT_SUBMISSION",
        )
    }

    private suspend fun <T> readinessCall(
        stage: MobileIssuerReadinessStage,
        onStage: (MobileIssuerStageEvent) -> Unit,
        operation: suspend () -> T,
    ): ReadinessCall<T> {
        onStage(MobileIssuerStageEvent(stage, MobileIssuerStageOutcome.START))
        return try {
            val value = operation()
            onStage(MobileIssuerStageEvent(stage, MobileIssuerStageOutcome.PASS))
            ReadinessCall.Success(value)
        } catch (error: CancellationException) {
            throw error
        } catch (error: Throwable) {
            val diagnostic = safeDiagnostic(stage, error)
            onStage(MobileIssuerStageEvent(stage, MobileIssuerStageOutcome.FAIL, diagnostic))
            ReadinessCall.Failure(diagnostic)
        }
    }

    suspend fun submitReviewedOperation(
        issuer: String,
        dedicatedIssuerConfirmed: Boolean,
        onStatus: (MobileIssuerStatus) -> Unit = {},
        observer: MobileIssuerTransactionObserver,
    ): MobileIssuerResult {
        try {
            observer.validateOperation()
        } catch (error: CancellationException) {
            throw error
        } catch (error: Throwable) {
            return failBeforeSubmission(observer, safeFailure("VALIDATE_OPERATION", error))
        }

        try {
            observer.validateIntent(SEPOLIA_CHAIN_ID, BigInteger.ZERO, "0x")
        } catch (error: CancellationException) {
            throw error
        } catch (error: Throwable) {
            return failBeforeSubmission(observer, safeFailure("VALIDATE_INTENT", error))
        }

        if (!ADDRESS.matches(issuer) || issuer.equals(KNOWN_HOLDER, ignoreCase = true)) {
            return failBeforeSubmission(
                observer,
                MobileIssuerSafeFailure("VALIDATE_WALLET", "ISSUER_NOT_ADMITTED"),
            )
        }
        if (!dedicatedIssuerConfirmed) {
            return failBeforeSubmission(
                observer,
                MobileIssuerSafeFailure(
                    "VALIDATE_WALLET",
                    "DEDICATED_ISSUER_CONFIRMATION_REQUIRED",
                    message = "Confirm the dedicated issuer wallet before submitting.",
                ),
            )
        }
        try {
            observer.validateWallet(issuer)
        } catch (error: CancellationException) {
            throw error
        } catch (error: Throwable) {
            return failBeforeSubmission(observer, safeFailure("VALIDATE_WALLET", error))
        }

        val transactionJson = try {
            transactionJsonBuilder(
                EthereumTransactionPayload(
                    from = issuer,
                    to = issuer,
                    valueWei = BigInteger.ZERO,
                    data = "0x",
                    chainId = SEPOLIA_CHAIN_ID,
                ),
            )
        } catch (error: Throwable) {
            return failBeforeSubmission(observer, safeFailure("VALIDATE_INTENT", error))
        }

        try {
            provider.switchToSepolia()
        } catch (error: CancellationException) {
            throw error
        } catch (error: Throwable) {
            return failBeforeSubmission(observer, safeFailure("SWITCH_CHAIN", error))
        }

        val chainId = try {
            parseQuantity(readProvider.request("eth_chainId"))
        } catch (error: CancellationException) {
            throw error
        } catch (error: Throwable) {
            return failBeforeSubmission(observer, safeFailure("READ_CHAIN_ID", error))
        }
        if (chainId != SEPOLIA_CHAIN_ID) {
            return failBeforeSubmission(
                observer,
                MobileIssuerSafeFailure("READ_CHAIN_ID", "WRONG_CHAIN", message = "Sepolia network required."),
            )
        }
        try {
            EthereumTransactionJsonBuilder.requireChainAgreement(transactionJson, chainId)
        } catch (error: Throwable) {
            return failBeforeSubmission(observer, safeFailure("VALIDATE_INTENT", error))
        }

        val latest = try {
            transactionCount(issuer, "latest")
        } catch (error: CancellationException) {
            throw error
        } catch (error: Throwable) {
            return failBeforeSubmission(observer, safeFailure("READ_NONCE_LATEST", error))
        }
        val pending = try {
            transactionCount(issuer, "pending")
        } catch (error: CancellationException) {
            throw error
        } catch (error: Throwable) {
            return failBeforeSubmission(observer, safeFailure("READ_NONCE_PENDING", error))
        }
        if (latest != pending) {
            return failBeforeSubmission(
                observer,
                MobileIssuerSafeFailure(
                    "READ_NONCE_PENDING",
                    "PENDING_TRANSACTION",
                    message = "Latest and pending nonce differ.",
                ),
            )
        }

        try {
            observer.claimSubmission(latest, pending)
        } catch (error: CancellationException) {
            throw error
        } catch (error: Throwable) {
            return failBeforeSubmission(observer, safeFailure("CLAIM_SUBMISSION", error))
        }
        onStatus(MobileIssuerStatus.SUBMITTING)

        val transactionHash = try {
            val hash = parseTransactionHash(provider.sendTransaction(jsonObjectParam(transactionJson)))
            if (hash == null) {
                observer.unknown(
                    MobileIssuerSafeFailure(
                        stage = "PROVIDER_SEND_RESPONSE",
                        category = "UNKNOWN_TRANSACTION_RESULT",
                    ),
                )
                return MobileIssuerResult(MobileIssuerStatus.UNKNOWN, "UNKNOWN_TRANSACTION_RESULT")
            }
            observer.hashReceived(hash)
            hash
        } catch (error: Throwable) {
            val failure = safeFailure("PROVIDER_SEND", error)
            observer.unknown(failure)
            return MobileIssuerResult(
                MobileIssuerStatus.UNKNOWN,
                "${failure.stage}_${failure.category}",
            )
        }

        return reconcileSubmitted(issuer, transactionHash, latest, observer)
    }

    suspend fun reconcile(
        issuer: String,
        transactionHash: String,
        preNonce: BigInteger,
        observer: MobileIssuerTransactionObserver = MobileIssuerTransactionObserver.NONE,
    ): MobileIssuerResult {
        if (!ADDRESS.matches(issuer) || parseTransactionHash(transactionHash) == null) {
            return MobileIssuerResult(MobileIssuerStatus.UNKNOWN, "INVALID_RECOVERY_EVIDENCE")
        }
        try {
            provider.switchToSepolia()
            if (parseQuantity(readProvider.request("eth_chainId")) != SEPOLIA_CHAIN_ID) {
                observer.unknown("RECOVERY_WRONG_CHAIN")
                return MobileIssuerResult(MobileIssuerStatus.UNKNOWN, "RECOVERY_WRONG_CHAIN")
            }
        } catch (error: Throwable) {
            val category = safeCategory("RECOVERY_PREFLIGHT", error)
            observer.unknown(category)
            return MobileIssuerResult(MobileIssuerStatus.UNKNOWN, category)
        }
        return reconcileSubmitted(issuer, transactionHash.lowercase(), preNonce, observer)
    }

    suspend fun reconcileNoBroadcast(
        issuer: String,
    ): Pair<BigInteger, BigInteger> {
        provider.switchToSepolia()
        require(parseQuantity(readProvider.request("eth_chainId")) == SEPOLIA_CHAIN_ID) { "WRONG_CHAIN" }
        var observed: Pair<BigInteger, BigInteger>? = null
        repeat(reconciliationAttempts) { attempt ->
            val current = transactionCount(issuer, "latest") to transactionCount(issuer, "pending")
            val first = observed
            if (first != null && current != first) return current
            observed = current
            if (attempt + 1 < reconciliationAttempts) wait(pollDelayMillis)
        }
        return checkNotNull(observed)
    }

    private suspend fun reconcileSubmitted(
        issuer: String,
        transactionHash: String,
        preNonce: BigInteger,
        observer: MobileIssuerTransactionObserver,
    ): MobileIssuerResult {

        var transactionSeen = false
        var receipt: ReceiptReadback? = null
        try {
            observer.confirming()
            for (attempt in 0 until readbackAttempts) {
                val transactionData = readProvider.request(
                    "eth_getTransactionByHash",
                    listOf(jsonStringParam(transactionHash)),
                )
                if (!isNullResult(transactionData)) {
                    validateTransaction(transactionData, transactionHash, issuer)
                    transactionSeen = true
                }
                val receiptData = readProvider.request(
                    "eth_getTransactionReceipt",
                    listOf(jsonStringParam(transactionHash)),
                )
                if (!isNullResult(receiptData)) {
                    receipt = validateReceipt(receiptData, issuer)
                }
                if (transactionSeen && receipt != null) {
                    observer.onchainReadback()
                    break
                }
                if (attempt + 1 < readbackAttempts) wait(pollDelayMillis)
            }
        } catch (error: ReadbackFailure) {
            if (error.category == "REVERTED_RECEIPT") observer.reverted(error.category)
            else observer.unknown(error.category)
            return MobileIssuerResult(MobileIssuerStatus.FAILED, error.category)
        } catch (error: Throwable) {
            val category = safeCategory("READBACK", error)
            observer.unknown(category)
            return MobileIssuerResult(MobileIssuerStatus.UNKNOWN, category)
        }
        if (!transactionSeen || receipt == null) {
            observer.unknown("POST_SUBMIT_READBACK_TIMEOUT")
            return MobileIssuerResult(MobileIssuerStatus.UNKNOWN, "POST_SUBMIT_READBACK_TIMEOUT")
        }

        try {
            if (parseQuantity(readProvider.request("eth_chainId")) != SEPOLIA_CHAIN_ID) {
                observer.unknown("POST_CONFIRMATION_CHAIN_MISMATCH")
                return MobileIssuerResult(MobileIssuerStatus.UNKNOWN, "POST_CONFIRMATION_CHAIN_MISMATCH")
            }
            val expectedNonce = preNonce + BigInteger.ONE
            repeat(reconciliationAttempts) { attempt ->
                val latest = transactionCount(issuer, "latest")
                val pending = transactionCount(issuer, "pending")
                if (latest == expectedNonce && pending == expectedNonce) {
                    val evidence = MobileIssuerEvidence(
                        issuer,
                        transactionHash,
                        receipt!!.blockNumber,
                        preNonce,
                        latest,
                        pending,
                    )
                    observer.confirmed(evidence)
                    return MobileIssuerResult(MobileIssuerStatus.CONFIRMED, "M1_CONFIRMED", evidence)
                }
                if (attempt + 1 < reconciliationAttempts) wait(pollDelayMillis)
            }
        } catch (error: Throwable) {
            val category = safeCategory("RECONCILIATION", error)
            observer.unknown(category)
            return MobileIssuerResult(MobileIssuerStatus.UNKNOWN, category)
        }
        observer.unknown("NONCE_RECONCILIATION_TIMEOUT")
        return MobileIssuerResult(MobileIssuerStatus.UNKNOWN, "NONCE_RECONCILIATION_TIMEOUT")
    }

    private suspend fun transactionCount(address: String, blockTag: String): BigInteger =
        parseQuantity(
            readProvider.request(
                "eth_getTransactionCount",
                listOf(jsonStringParam(address), jsonStringParam(blockTag)),
            ),
        )

    private fun failBeforeSubmission(
        observer: MobileIssuerTransactionObserver,
        failure: MobileIssuerSafeFailure,
    ): MobileIssuerResult {
        try {
            observer.preSubmitFailure(failure)
        } catch (_: Throwable) {
            // The original failure is the useful UI diagnostic. A failed journal write
            // cannot advance the operation, so provider submission remains unreachable.
        }
        return MobileIssuerResult(
            status = MobileIssuerStatus.FAILED,
            category = "${failure.stage}_${failure.category}",
            failure = failure,
        )
    }

    private fun blocked(
        issuer: String,
        category: String,
        chainId: BigInteger? = null,
        balance: BigInteger? = null,
        latestNonce: BigInteger? = null,
        pendingNonce: BigInteger? = null,
        diagnostic: MobileIssuerFailureDiagnostic? = null,
    ) = MobileIssuerInspection(
        MobileIssuerStatus.BLOCKED,
        issuer,
        chainId,
        balance,
        latestNonce,
        pendingNonce,
        category,
        diagnostic,
    )

    private fun blocked(
        issuer: String,
        diagnostic: MobileIssuerFailureDiagnostic,
        chainId: BigInteger? = null,
        balance: BigInteger? = null,
        latestNonce: BigInteger? = null,
        pendingNonce: BigInteger? = null,
    ) = blocked(
        issuer,
        diagnostic.category,
        chainId,
        balance,
        latestNonce,
        pendingNonce,
        diagnostic,
    )

    companion object {
        const val KNOWN_HOLDER = "0x3419148731087b970d2059C53780163B452D5FF7"
        val SEPOLIA_CHAIN_ID: BigInteger = BigInteger("11155111")
        private val ADDRESS = Regex("^0x[0-9a-fA-F]{40}$")
        private val HASH = Regex("^0x[0-9a-fA-F]{64}$")
        private val QUANTITY = Regex("^0x(?:0|[1-9a-fA-F][0-9a-fA-F]*)$")

        fun admissionBlockReason(issuer: String?, dedicatedIssuerConfirmed: Boolean): String? = when {
            issuer == null -> "WALLET_REQUIRED"
            !ADDRESS.matches(issuer) -> "INVALID_WALLET"
            issuer.equals(KNOWN_HOLDER, ignoreCase = true) -> "KNOWN_HOLDER_BLOCKED"
            !dedicatedIssuerConfirmed -> "DEDICATED_ISSUER_CONFIRMATION_REQUIRED"
            else -> null
        }

        fun parseTransactionHash(raw: String): String? {
            val candidate = parseJsonStringOrBare(raw) ?: return null
            return candidate.takeIf(HASH::matches)?.lowercase()
        }

        fun parseQuantity(raw: String): BigInteger {
            val candidate = parseJsonStringOrBare(raw)
                ?: throw IllegalArgumentException("Malformed JSON-RPC quantity")
            require(QUANTITY.matches(candidate)) { "Malformed JSON-RPC quantity" }
            return BigInteger(candidate.drop(2), 16)
        }

        fun formatBalance(balanceWei: BigInteger?): String {
            if (balanceWei == null) return "Not available"
            val eth = BigDecimal(balanceWei).movePointLeft(18).setScale(8, RoundingMode.DOWN).stripTrailingZeros()
            return "${eth.toPlainString()} Sepolia ETH (${balanceWei} wei)"
        }

        fun reviewSummary(issuer: String): String =
            "SEPOLIA TESTNET\n\n" +
                "Network: Sepolia (11155111)\n" +
                "From: $issuer\n" +
                "To: $issuer\n" +
                "Action: Mobile issuer admission\n" +
                "Value: 0 ETH\n" +
                "Data: 0x\n" +
                "Estimated / expected network cost: gas only; provider-estimated at submission\n" +
                "Purpose: Verify Android mobile transaction transport"

        fun validateTransaction(json: String, expectedHash: String, issuer: String) {
            val hash = requiredHexField(json, "hash")
            val from = requiredHexField(json, "from")
            val to = requiredHexField(json, "to")
            val value = requiredHexField(json, "value")
            val input = requiredHexField(json, "input")
            if (!hash.equals(expectedHash, true)) throw ReadbackFailure("TRANSACTION_HASH_MISMATCH")
            if (!from.equals(issuer, true) || !to.equals(issuer, true)) {
                throw ReadbackFailure("TRANSACTION_PARTY_MISMATCH")
            }
            if (parseQuantity(value) != BigInteger.ZERO) throw ReadbackFailure("NONZERO_VALUE")
            if (input != "0x") throw ReadbackFailure("NONEMPTY_DATA")
        }

        fun validateReceipt(json: String, issuer: String): ReceiptReadback {
            val status = requiredHexField(json, "status")
            val from = requiredHexField(json, "from")
            val to = requiredHexField(json, "to")
            val blockNumber = requiredHexField(json, "blockNumber")
            if (!from.equals(issuer, true) || !to.equals(issuer, true)) {
                throw ReadbackFailure("RECEIPT_PARTY_MISMATCH")
            }
            if (parseQuantity(status) != BigInteger.ONE) throw ReadbackFailure("REVERTED_RECEIPT")
            return ReceiptReadback(parseQuantity(blockNumber))
        }

        fun nonceReconciled(preNonce: BigInteger, latest: BigInteger, pending: BigInteger): Boolean =
            latest == preNonce + BigInteger.ONE && pending == preNonce + BigInteger.ONE

        private fun requiredHexField(json: String, name: String): String {
            val pattern = Regex("\"${Regex.escape(name)}\"\\s*:\\s*\"(0x[0-9a-fA-F]*)\"")
            val values = pattern.findAll(json.trim()).map { it.groupValues[1] }.toList()
            if (values.size != 1) throw ReadbackFailure("MALFORMED_${name.uppercase()}")
            return values.single()
        }

        private fun parseJsonStringOrBare(raw: String): String? {
            val value = raw.trim()
            if (value.startsWith('"') && value.endsWith('"') && value.length >= 2) {
                val inner = value.substring(1, value.length - 1)
                return inner.takeIf { '\\' !in it && '"' !in it }
            }
            return value.takeIf { it.startsWith("0x") }
        }

        private fun isNullResult(raw: String): Boolean = raw.trim() == "null"

        fun jsonStringParam(value: String): String = value

        fun jsonObjectParam(json: String): String {
            val value = json.trim()
            require(value.startsWith('{') && value.endsWith('}')) { "Expected JSON object parameter" }
            return value
        }

        private fun safeCategory(stage: String, error: Throwable): String {
            return "${stage}_${safeErrorType(error)}"
        }

        fun safeFailure(stage: String, error: Throwable): MobileIssuerSafeFailure =
            MobileIssuerSafeFailure(
                stage = stage.replace(Regex("[^A-Za-z0-9_]+"), "_").take(48),
                category = safeErrorType(error),
                exceptionClass = error::class.simpleName
                    ?.replace(Regex("[^A-Za-z0-9_.]+"), "_")
                    ?.take(64),
                message = safeThrowableMessage(error),
            )

        private fun safeDiagnostic(
            stage: MobileIssuerReadinessStage,
            error: Throwable,
        ): MobileIssuerFailureDiagnostic = MobileIssuerFailureDiagnostic(
            stage = stage,
            category = safeErrorType(error),
            message = safeThrowableMessage(error),
        )

        private fun safeErrorType(error: Throwable): String {
            val type = error::class.simpleName?.take(48) ?: "Error"
            return type
                .replace(Regex("([a-z0-9])([A-Z])"), "$1_$2")
                .replace(Regex("[^A-Za-z0-9_]+"), "_")
                .trim('_')
                .uppercase()
                .ifEmpty { "ERROR" }
        }

        private fun safeThrowableMessage(error: Throwable): String? {
            val normalized = error.message
                ?.replace(Regex("[\\r\\n\\t]+"), " ")
                ?.replace(Regex("\\s+"), " ")
                ?.trim()
                ?.takeIf { it.isNotEmpty() && it.length <= 160 }
                ?: return null
            val unsafe = Regex(
                "(?i)(https?://|wss?://|authorization|bearer|auth[_ -]?token|request headers?|" +
                    "provider internals?|iframe|endpoint|api[_ -]?key|otp|one.time|email|[A-Z0-9._%+-]+@[A-Z0-9.-]+)",
            )
            if (unsafe.containsMatchIn(normalized)) return null
            return normalized.replace(Regex("0x[0-9a-fA-F]{16,}"), "<hex>")
        }
    }
}

private sealed interface ReadinessCall<out T> {
    data class Success<T>(val value: T) : ReadinessCall<T>
    data class Failure(val diagnostic: MobileIssuerFailureDiagnostic) : ReadinessCall<Nothing>
}

data class ReceiptReadback(val blockNumber: BigInteger)

class ReadbackFailure(val category: String) : IllegalArgumentException(category)
