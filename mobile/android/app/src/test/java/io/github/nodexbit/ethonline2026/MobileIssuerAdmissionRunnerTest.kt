package io.github.nodexbit.ethonline2026

import io.privy.wallet.EmbeddedWalletException
import io.privy.wallet.ethereum.EthereumRpcRequest
import java.math.BigInteger
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.assertThrows
import org.junit.Test

class MobileIssuerAdmissionRunnerTest {
    @Test
    fun `checkbox confirmation triggers one readiness generation`() {
        val gate = MobileIssuerReadinessGate()

        assertEquals(1L, gate.confirmationChanged(true))
        assertTrue(gate.isCurrent(1L))
    }

    @Test
    fun `unchecked confirmation does not start readiness`() {
        val gate = MobileIssuerReadinessGate()

        assertNull(gate.confirmationChanged(false))
        assertFalse(gate.isCurrent(1L))
    }

    @Test
    fun `repeated checked event cannot create a duplicate concurrent readiness`() {
        val gate = MobileIssuerReadinessGate()

        val first = gate.confirmationChanged(true)
        val duplicate = gate.confirmationChanged(true)

        assertEquals(1L, first)
        assertNull(duplicate)
    }

    @Test
    fun `result becomes stale after confirmation is unchecked`() {
        val gate = MobileIssuerReadinessGate()
        val generation = gate.confirmationChanged(true)!!

        gate.confirmationChanged(false)

        assertFalse(gate.isCurrent(generation))
        assertFalse(gate.finish(generation))
    }

    @Test
    fun `wallet unavailable has a specific blocked reason`() {
        assertEquals(
            "WALLET_REQUIRED",
            MobileIssuerAdmissionRunner.admissionBlockReason(null, dedicatedIssuerConfirmed = true),
        )
    }

    @Test
    fun `quoted transaction hash is accepted`() {
        assertEquals(HASH, MobileIssuerAdmissionRunner.parseTransactionHash("\"$HASH\""))
    }

    @Test
    fun `malformed transaction hash is rejected`() {
        assertNull(MobileIssuerAdmissionRunner.parseTransactionHash("\"0x1234\""))
        assertNull(MobileIssuerAdmissionRunner.parseTransactionHash("{\"hash\":\"$HASH\"}"))
    }

    @Test
    fun `wrong chain blocks admission`() = runBlocking {
        val provider = FakeProvider(mapOf("eth_chainId" to listOf("\"0x1\"")))
        val result = runner(provider).inspect(ISSUER, true)

        assertEquals(MobileIssuerStatus.BLOCKED, result.status)
        assertEquals("WRONG_CHAIN", result.category)
        assertEquals(0, provider.sendCalls)
    }

    @Test
    fun `zero balance requires issuer funding and readiness never sends`() = runBlocking {
        val provider = readinessProvider(balance = "0x0")

        val result = runner(provider).inspect(ISSUER, true)

        assertEquals(MobileIssuerStatus.BLOCKED, result.status)
        assertEquals("ISSUER_FUNDING_REQUIRED", result.category)
        assertEquals(0, provider.sendCalls)
    }

    @Test
    fun `different latest and pending nonces block readiness`() = runBlocking {
        val provider = readinessProvider(latest = "0x7", pending = "0x8")

        val result = runner(provider).inspect(ISSUER, true)

        assertEquals(MobileIssuerStatus.BLOCKED, result.status)
        assertEquals("PENDING_TRANSACTION", result.category)
        assertEquals(0, provider.sendCalls)
    }

    @Test
    fun `valid readiness becomes READY and cannot submit`() = runBlocking {
        val provider = readinessProvider(balance = "0x1", latest = "0x7", pending = "0x7")
        val events = mutableListOf<MobileIssuerStageEvent>()

        val result = runner(provider).inspect(ISSUER, true, events::add)

        assertEquals(MobileIssuerStatus.READY, result.status)
        assertEquals(BigInteger.ONE, result.balanceWei)
        assertEquals(bi(7), result.latestNonce)
        assertEquals(bi(7), result.pendingNonce)
        assertEquals(
            MobileIssuerReadinessStage.entries.toList(),
            events.filter { it.outcome == MobileIssuerStageOutcome.PASS }.map { it.stage },
        )
        assertEquals(
            CapturedRpcRequest("eth_getBalance", listOf(ISSUER, "latest")),
            provider.rpcRequests[1],
        )
        assertEquals(
            CapturedRpcRequest("eth_getTransactionCount", listOf(ISSUER, "latest")),
            provider.rpcRequests[2],
        )
        assertEquals(
            CapturedRpcRequest("eth_getTransactionCount", listOf(ISSUER, "pending")),
            provider.rpcRequests[3],
        )
        assertEquals(0, provider.sendCalls)
    }

    @Test
    fun `Privy string and object parameters have distinct exact representations`() {
        assertEquals(ISSUER, MobileIssuerAdmissionRunner.jsonStringParam(ISSUER))
        assertEquals("latest", MobileIssuerAdmissionRunner.jsonStringParam("latest"))
        assertEquals(EXACT_TRANSACTION, MobileIssuerAdmissionRunner.jsonObjectParam(EXACT_TRANSACTION))
        assertFalse(MobileIssuerAdmissionRunner.jsonObjectParam(EXACT_TRANSACTION).startsWith('"'))
    }

    @Test
    fun `generic transaction builder emits explicit canonical Sepolia chain quantity`() {
        val transaction = EthereumTransactionJsonBuilder.build(
            EthereumTransactionPayload(
                from = ISSUER,
                to = ISSUER,
                valueWei = BigInteger.ZERO,
                data = "0x",
                chainId = MobileIssuerAdmissionRunner.SEPOLIA_CHAIN_ID,
            ),
        )

        assertEquals(EXACT_TRANSACTION, transaction)
        EthereumTransactionJsonBuilder.requireChainAgreement(
            transaction,
            MobileIssuerAdmissionRunner.SEPOLIA_CHAIN_ID,
        )
    }

    @Test
    fun `wrong or missing payload chain is rejected locally`() {
        val wrong = EXACT_TRANSACTION.replace("0xaa36a7", "0x1")
        val missing = EXACT_TRANSACTION.replace(",\"chainId\":\"0xaa36a7\"", "")

        assertThrows(IllegalArgumentException::class.java) {
            EthereumTransactionJsonBuilder.requireChainAgreement(
                wrong,
                MobileIssuerAdmissionRunner.SEPOLIA_CHAIN_ID,
            )
        }
        assertThrows(IllegalArgumentException::class.java) {
            EthereumTransactionJsonBuilder.requireChainAgreement(
                missing,
                MobileIssuerAdmissionRunner.SEPOLIA_CHAIN_ID,
            )
        }
    }

    @Test
    fun `wrong payload chain fails before durable claim and provider send`() = runBlocking {
        val engine = transactionEngine()
        val operation = readyOperation(engine)
        val provider = FakeProvider(mapOf("eth_chainId" to listOf("\"0xaa36a7\"")))

        val result = runner(provider) { payload ->
            EthereumTransactionJsonBuilder.build(payload.copy(chainId = BigInteger.ONE))
        }.submitReviewedOperation(
            ISSUER,
            true,
            observer = MobileIssuerJournalObserver(engine, operation.operationId),
        )

        assertEquals("VALIDATE_INTENT", result.failure?.stage)
        assertEquals("TRANSACTION_CHAIN_ID_MISMATCH", result.failure?.message)
        assertEquals(TransactionOperationState.READY_TO_SUBMIT, engine.find(operation.operationId)?.state)
        assertNull(engine.find(operation.operationId)?.preLatestNonce)
        assertEquals(1, provider.switchCalls)
        assertEquals(0, provider.sendCalls)
    }

    @Test
    fun `missing payload chain fails before durable claim and provider send`() = runBlocking {
        val engine = transactionEngine()
        val operation = readyOperation(engine)
        val provider = FakeProvider(mapOf("eth_chainId" to listOf("\"0xaa36a7\"")))

        val result = runner(provider) { payload ->
            EthereumTransactionJsonBuilder.build(payload).replace(",\"chainId\":\"0xaa36a7\"", "")
        }.submitReviewedOperation(
            ISSUER,
            true,
            observer = MobileIssuerJournalObserver(engine, operation.operationId),
        )

        assertEquals("VALIDATE_INTENT", result.failure?.stage)
        assertEquals("TRANSACTION_CHAIN_ID_REQUIRED", result.failure?.message)
        assertEquals(TransactionOperationState.READY_TO_SUBMIT, engine.find(operation.operationId)?.state)
        assertEquals(0, provider.sendCalls)
    }

    @Test
    fun `exact Privy EthereumRpcRequest shapes match all M1 parameterized methods`() {
        val requests = listOf(
            EthereumRpcRequest("eth_getBalance", listOf(ISSUER, "latest")),
            EthereumRpcRequest("eth_getTransactionCount", listOf(ISSUER, "latest")),
            EthereumRpcRequest("eth_getTransactionCount", listOf(ISSUER, "pending")),
            EthereumRpcRequest("eth_getTransactionByHash", listOf(HASH)),
            EthereumRpcRequest("eth_getTransactionReceipt", listOf(HASH)),
            EthereumRpcRequest.ethSendTransaction(EXACT_TRANSACTION),
        )

        assertEquals(
            listOf(
                "eth_getBalance" to listOf(ISSUER, "latest"),
                "eth_getTransactionCount" to listOf(ISSUER, "latest"),
                "eth_getTransactionCount" to listOf(ISSUER, "pending"),
                "eth_getTransactionByHash" to listOf(HASH),
                "eth_getTransactionReceipt" to listOf(HASH),
                "eth_sendTransaction" to listOf(EXACT_TRANSACTION),
            ),
            requests.map { it.method to it.params },
        )
    }

    @Test
    fun `confirmation review contains every required transaction field`() {
        val summary = MobileIssuerAdmissionRunner.reviewSummary(ISSUER)

        assertTrue(summary.contains("Network: Sepolia (11155111)"))
        assertTrue(summary.contains("From: $ISSUER"))
        assertTrue(summary.contains("To: $ISSUER"))
        assertTrue(summary.contains("Action: Mobile issuer admission"))
        assertTrue(summary.contains("Value: 0 ETH"))
        assertTrue(summary.contains("Data: 0x"))
        assertTrue(summary.contains("Purpose: Verify Android mobile transaction transport"))
    }

    @Test
    fun `READY_TO_SUBMIT enables review and restores confirmed checkbox`() {
        assertTrue(MobileIssuerUiPolicy.reviewEnabled(TransactionOperationState.READY_TO_SUBMIT))
        assertTrue(MobileIssuerUiPolicy.restoreConfirmedCheckbox(TransactionOperationState.READY_TO_SUBMIT))
        assertFalse(MobileIssuerUiPolicy.issuerConfirmationEnabled(TransactionOperationState.READY_TO_SUBMIT))
    }

    @Test
    fun `READY_TO_REVIEW is internal and does not enable review`() {
        assertFalse(MobileIssuerUiPolicy.reviewEnabled(TransactionOperationState.READY_TO_REVIEW))
    }

    @Test
    fun `all states except READY_TO_SUBMIT disable review`() {
        TransactionOperationState.values()
            .filterNot { it == TransactionOperationState.READY_TO_SUBMIT }
            .forEach { state ->
                assertFalse("$state exposed review", MobileIssuerUiPolicy.reviewEnabled(state))
            }
    }

    @Test
    fun `opening review performs zero sends and does not mutate operation`() {
        val engine = transactionEngine()
        val operation = readyOperation(engine)
        val provider = FakeProvider(emptyMap())

        val summary = MobileIssuerReviewFlow.open(operation, ISSUER)

        assertTrue(summary.contains("SEPOLIA TESTNET"))
        assertEquals(0, provider.sendCalls)
        assertEquals(TransactionOperationState.READY_TO_SUBMIT, engine.find(operation.operationId)?.state)
    }

    @Test
    fun `final submission runner remains the only send entry point`() = runBlocking {
        val engine = transactionEngine()
        val operation = readyOperation(engine)
        val provider = FakeProvider(
            mapOf(
                "eth_chainId" to listOf("\"0xaa36a7\"", "\"0xaa36a7\""),
                "eth_getTransactionCount:latest" to listOf("\"0x7\"", "\"0x8\""),
                "eth_getTransactionCount:pending" to listOf("\"0x7\"", "\"0x8\""),
                "eth_getTransactionByHash" to listOf(transactionJson()),
                "eth_getTransactionReceipt" to listOf(receiptJson()),
            ),
        )

        MobileIssuerReviewFlow.open(operation, ISSUER)
        assertEquals(0, provider.sendCalls)

        runner(provider).submitReviewedOperation(
            ISSUER,
            dedicatedIssuerConfirmed = true,
            observer = MobileIssuerJournalObserver(engine, operation.operationId),
        )

        assertEquals(1, provider.sendCalls)
    }

    @Test
    fun `switchChain failure identifies SWITCH_CHAIN`() = runBlocking {
        assertStageFailure(
            readinessProvider(switchFailure = embeddedFailure()),
            MobileIssuerReadinessStage.SWITCH_CHAIN,
        )
    }

    @Test
    fun `chainId failure identifies READ_CHAIN_ID`() = runBlocking {
        assertStageFailure(
            readinessProvider(failures = mapOf("eth_chainId" to embeddedFailure())),
            MobileIssuerReadinessStage.READ_CHAIN_ID,
        )
    }

    @Test
    fun `balance failure identifies READ_BALANCE`() = runBlocking {
        assertStageFailure(
            readinessProvider(failures = mapOf("eth_getBalance" to embeddedFailure())),
            MobileIssuerReadinessStage.READ_BALANCE,
        )
    }

    @Test
    fun `latest nonce failure identifies READ_NONCE_LATEST`() = runBlocking {
        assertStageFailure(
            readinessProvider(
                failures = mapOf("eth_getTransactionCount:latest" to embeddedFailure()),
            ),
            MobileIssuerReadinessStage.READ_NONCE_LATEST,
        )
    }

    @Test
    fun `pending nonce failure identifies READ_NONCE_PENDING`() = runBlocking {
        assertStageFailure(
            readinessProvider(
                failures = mapOf("eth_getTransactionCount:pending" to embeddedFailure()),
            ),
            MobileIssuerReadinessStage.READ_NONCE_PENDING,
        )
    }

    @Test
    fun `known holder wallet is blocked as issuer before provider access`() = runBlocking {
        val provider = FakeProvider(emptyMap())
        val result = runner(provider).inspect(MobileIssuerAdmissionRunner.KNOWN_HOLDER, true)

        assertEquals(MobileIssuerStatus.BLOCKED, result.status)
        assertEquals("KNOWN_HOLDER_BLOCKED", result.category)
        assertTrue(provider.requests.isEmpty())
    }

    @Test
    fun `transaction from or to mismatch is rejected`() {
        val wrong = "0x2222222222222222222222222222222222222222"
        val error = assertThrows(ReadbackFailure::class.java) {
            MobileIssuerAdmissionRunner.validateTransaction(transactionJson(from = wrong), HASH, ISSUER)
        }
        assertEquals("TRANSACTION_PARTY_MISMATCH", error.category)
    }

    @Test
    fun `nonzero transaction value is rejected`() {
        val error = assertThrows(ReadbackFailure::class.java) {
            MobileIssuerAdmissionRunner.validateTransaction(transactionJson(value = "0x1"), HASH, ISSUER)
        }
        assertEquals("NONZERO_VALUE", error.category)
    }

    @Test
    fun `nonempty transaction input is rejected`() {
        val error = assertThrows(ReadbackFailure::class.java) {
            MobileIssuerAdmissionRunner.validateTransaction(transactionJson(input = "0x00"), HASH, ISSUER)
        }
        assertEquals("NONEMPTY_DATA", error.category)
    }

    @Test
    fun `reverted receipt is rejected`() {
        val error = assertThrows(ReadbackFailure::class.java) {
            MobileIssuerAdmissionRunner.validateReceipt(receiptJson(status = "0x0"), ISSUER)
        }
        assertEquals("REVERTED_RECEIPT", error.category)
    }

    @Test
    fun `malformed receipt is rejected`() {
        val error = assertThrows(ReadbackFailure::class.java) {
            MobileIssuerAdmissionRunner.validateReceipt("{\"status\":\"0x1\"}", ISSUER)
        }
        assertTrue(error.category.startsWith("MALFORMED_"))
    }

    @Test
    fun `nonce reconciliation requires both counts to advance exactly once`() {
        assertTrue(MobileIssuerAdmissionRunner.nonceReconciled(bi(7), bi(8), bi(8)))
        assertFalse(MobileIssuerAdmissionRunner.nonceReconciled(bi(7), bi(7), bi(8)))
        assertFalse(MobileIssuerAdmissionRunner.nonceReconciled(bi(7), bi(9), bi(9)))
    }

    @Test
    fun `successful exact readback confirms M1`() = runBlocking {
        val engine = transactionEngine()
        val operation = readyOperation(engine)
        val provider = FakeProvider(
            mapOf(
                "eth_chainId" to listOf("\"0xaa36a7\"", "\"0xaa36a7\""),
                "eth_getTransactionCount:latest" to listOf("\"0x7\"", "\"0x8\""),
                "eth_getTransactionCount:pending" to listOf("\"0x7\"", "\"0x8\""),
                "eth_getTransactionByHash" to listOf(transactionJson()),
                "eth_getTransactionReceipt" to listOf(receiptJson()),
            ),
            sendResponse = "\"$HASH\"",
        )

        val result = runner(provider).submitReviewedOperation(
            ISSUER,
            dedicatedIssuerConfirmed = true,
            observer = MobileIssuerJournalObserver(engine, operation.operationId),
        )

        assertEquals(MobileIssuerStatus.CONFIRMED, result.status)
        assertEquals(bi(7), result.evidence?.preNonce)
        assertEquals(bi(8), result.evidence?.postLatestNonce)
        assertEquals(1, provider.sendCalls)
        assertEquals(1, provider.switchCalls)
        assertEquals(EXACT_TRANSACTION, provider.sentTransactions.single())
        assertEquals(
            CapturedRpcRequest("eth_sendTransaction", listOf(EXACT_TRANSACTION)),
            provider.rpcRequests[3],
        )
        assertEquals(
            CapturedRpcRequest("eth_getTransactionByHash", listOf(HASH)),
            provider.rpcRequests[4],
        )
        assertEquals(
            CapturedRpcRequest("eth_getTransactionReceipt", listOf(HASH)),
            provider.rpcRequests[5],
        )
    }

    @Test
    fun `existing real UNKNOWN hash is recovered through separate read-only provider`() = runBlocking {
        val engine = RecoverableTransactionEngine(
            TransactionJournal(MemoryStore()),
            newOperationId = { "existing-real-m1" },
        )
        val created = engine.create(
            TransactionIntent(
                MobileIssuerJournalObserver.OPERATION_TYPE,
                PHYSICAL_ISSUER,
                11155111L,
                PHYSICAL_ISSUER,
                "0",
                "0x",
            ),
        )
        engine.transition(created.operationId, TransactionOperationState.READY_TO_REVIEW)
        engine.transition(created.operationId, TransactionOperationState.READY_TO_SUBMIT)
        engine.beginSubmission(created.operationId, "0", "0")
        engine.recordHash(created.operationId, REAL_HASH)
        engine.markUnknown(created.operationId, "READBACK_PRIVY_API_EXCEPTION")
        val walletProvider = FakeProvider(emptyMap())
        val readProvider = FakeProvider(
            mapOf(
                "eth_chainId" to listOf("\"0xaa36a7\"", "\"0xaa36a7\""),
                "eth_getTransactionByHash" to listOf(realTransactionJson()),
                "eth_getTransactionReceipt" to listOf(realReceiptJson()),
                "eth_getTransactionCount:latest" to listOf("\"0x1\""),
                "eth_getTransactionCount:pending" to listOf("\"0x1\""),
            ),
        )
        val runner = MobileIssuerAdmissionRunner(
            provider = walletProvider,
            readProvider = readProvider,
            wait = {},
            readbackAttempts = 1,
            reconciliationAttempts = 1,
            pollDelayMillis = 0,
        )

        val result = runner.reconcile(
            PHYSICAL_ISSUER,
            REAL_HASH,
            BigInteger.ZERO,
            MobileIssuerJournalObserver(engine, created.operationId),
        )

        assertEquals(MobileIssuerStatus.CONFIRMED, result.status)
        val recovered = engine.find(created.operationId)!!
        assertEquals(TransactionOperationState.CONFIRMED, recovered.state)
        assertEquals(REAL_HASH, recovered.txHash)
        assertEquals("11683226", recovered.receiptBlock)
        assertEquals("0", recovered.preLatestNonce)
        assertEquals("1", recovered.postLatestNonce)
        assertEquals("1", recovered.postPendingNonce)
        assertEquals(0, walletProvider.sendCalls)
        assertTrue(walletProvider.requests.isEmpty())
        assertEquals(
            listOf(
                "eth_chainId",
                "eth_getTransactionByHash",
                "eth_getTransactionReceipt",
                "eth_chainId",
                "eth_getTransactionCount",
                "eth_getTransactionCount",
            ),
            readProvider.requests,
        )
    }

    @Test
    fun `observed real transaction and receipt shapes pass strict M1 validation`() {
        MobileIssuerAdmissionRunner.validateTransaction(
            realTransactionJson(),
            REAL_HASH,
            PHYSICAL_ISSUER,
        )

        val receipt = MobileIssuerAdmissionRunner.validateReceipt(realReceiptJson(), PHYSICAL_ISSUER)

        assertEquals(BigInteger("11683226"), receipt.blockNumber)
    }

    @Test
    fun `journaled M1 persists submitting before send and confirmation evidence`() = runBlocking {
        val engine = transactionEngine()
        val operation = readyOperation(engine)
        val provider = FakeProvider(
            mapOf(
                "eth_chainId" to listOf("\"0xaa36a7\"", "\"0xaa36a7\""),
                "eth_getTransactionCount:latest" to listOf("\"0x7\"", "\"0x8\""),
                "eth_getTransactionCount:pending" to listOf("\"0x7\"", "\"0x8\""),
                "eth_getTransactionByHash" to listOf(transactionJson()),
                "eth_getTransactionReceipt" to listOf(receiptJson()),
            ),
            beforeSend = {
                assertEquals(
                    TransactionOperationState.SUBMITTING_NO_HASH,
                    engine.find(operation.operationId)?.state,
                )
            },
        )

        val result = runner(provider).submitReviewedOperation(
            ISSUER,
            dedicatedIssuerConfirmed = true,
            observer = MobileIssuerJournalObserver(engine, operation.operationId),
        )

        assertEquals(MobileIssuerStatus.CONFIRMED, result.status)
        val persisted = engine.find(operation.operationId)!!
        assertEquals(TransactionOperationState.CONFIRMED, persisted.state)
        assertEquals(HASH, persisted.txHash)
        assertEquals("42", persisted.receiptBlock)
        assertEquals("7", persisted.preLatestNonce)
        assertEquals("8", persisted.postLatestNonce)
        assertEquals("8", persisted.postPendingNonce)
    }

    @Test
    fun `send exception before hash persists UNKNOWN and cannot resubmit`() = runBlocking {
        val engine = transactionEngine()
        val operation = readyOperation(engine)
        val provider = FakeProvider(
            mapOf(
                "eth_chainId" to listOf("\"0xaa36a7\""),
                "eth_getTransactionCount:latest" to listOf("\"0x0\""),
                "eth_getTransactionCount:pending" to listOf("\"0x0\""),
            ),
            sendFailure = EmbeddedWalletException("send failed"),
        )

        val result = runner(provider).submitReviewedOperation(
            ISSUER,
            dedicatedIssuerConfirmed = true,
            observer = MobileIssuerJournalObserver(engine, operation.operationId),
        )

        assertEquals(MobileIssuerStatus.UNKNOWN, result.status)
        val failed = engine.find(operation.operationId)!!
        assertEquals(TransactionOperationState.UNKNOWN, failed.state)
        assertNull(failed.txHash)
        assertEquals("PROVIDER_SEND", failed.failureStage)
        assertEquals("EMBEDDED_WALLET_EXCEPTION", failed.safeErrorCategory)
        assertEquals("EmbeddedWalletException", failed.safeExceptionClass)
        assertEquals("send failed", failed.safeErrorMessage)
        assertEquals("0", failed.preLatestNonce)
        assertEquals("0", failed.prePendingNonce)

        val reconciled = engine.proveNoBroadcast(operation.operationId, "0", "0")
        assertEquals(TransactionOperationState.NO_BROADCAST_PROVEN, reconciled.state)
        assertEquals("PROVIDER_SEND", reconciled.failureStage)
        assertEquals("send failed", reconciled.safeErrorMessage)
        assertThrows(IllegalArgumentException::class.java) {
            engine.beginSubmission(operation.operationId, "0", "0")
        }
        Unit
    }

    @Test
    fun `exact M1 JSON is accepted by the locally resolved Privy unsigned transaction parser`() {
        val transactionClass = Class.forName(
            "io.privy.wallet.walletApi.rpc.ethereum.UnsignedEthereumTransaction",
        )
        val companion = transactionClass.getField("Companion").get(null)
        @Suppress("UNCHECKED_CAST")
        val serializer = companion.javaClass.getDeclaredMethod("serializer").apply {
            isAccessible = true
        }.invoke(companion) as KSerializer<Any>
        val transactionJson = EthereumTransactionJsonBuilder.build(
            EthereumTransactionPayload(
                PHYSICAL_ISSUER,
                PHYSICAL_ISSUER,
                BigInteger.ZERO,
                "0x",
                MobileIssuerAdmissionRunner.SEPOLIA_CHAIN_ID,
            ),
        )

        val parsed = Json { ignoreUnknownKeys = true }.decodeFromString(serializer, transactionJson)

        assertEquals(PHYSICAL_ISSUER, transactionClass.getMethod("getFrom").invoke(parsed))
        assertEquals(PHYSICAL_ISSUER, transactionClass.getMethod("getTo").invoke(parsed))
        assertEquals("0x", transactionClass.getMethod("getData").invoke(parsed))
        val value = transactionClass.getMethod("getValue").invoke(parsed)
        assertEquals("0x0", value.javaClass.getMethod("getHexString").invoke(value))
        val chainId = transactionClass.getMethod("getChainId").invoke(parsed)
        assertEquals("0xaa36a7", chainId.javaClass.getMethod("getHexString").invoke(chainId))
        assertEquals(11155111, chainId.javaClass.getMethod("getNumber").invoke(chainId))
        assertEquals(
            JsonPrimitive("0xaa36a7"),
            Json.encodeToJsonElement(serializer, parsed).jsonObject["chainId"],
        )
        val walletApiTransaction = Class.forName(
            "io.privy.wallet.walletApi.rpc.ethereum.UnsignedEthereumTransactionKt",
        ).getMethod("toWalletApi", transactionClass).invoke(null, parsed)
        assertEquals(11155111, walletApiTransaction.javaClass.getMethod("getChainId").invoke(walletApiTransaction))
        assertNull(transactionClass.getMethod("getNonce").invoke(parsed))
        assertNull(transactionClass.getMethod("getGasLimit").invoke(parsed))
        assertNull(transactionClass.getMethod("getGasPrice").invoke(parsed))
        assertNull(transactionClass.getMethod("getType").invoke(parsed))
        assertNull(transactionClass.getMethod("getMaxFeePerGas").invoke(parsed))
        assertNull(transactionClass.getMethod("getMaxPriorityFeePerGas").invoke(parsed))
    }

    @Test
    fun `malformed returned hash persists UNKNOWN without a hash`() = runBlocking {
        val engine = transactionEngine()
        val operation = readyOperation(engine)
        val provider = FakeProvider(
            mapOf(
                "eth_chainId" to listOf("\"0xaa36a7\""),
                "eth_getTransactionCount:latest" to listOf("\"0x0\""),
                "eth_getTransactionCount:pending" to listOf("\"0x0\""),
            ),
            sendResponse = "\"0x1234\"",
        )

        runner(provider).submitReviewedOperation(
            ISSUER,
            dedicatedIssuerConfirmed = true,
            observer = MobileIssuerJournalObserver(engine, operation.operationId),
        )

        val persisted = engine.find(operation.operationId)!!
        assertEquals(TransactionOperationState.UNKNOWN, persisted.state)
        assertEquals("UNKNOWN_TRANSACTION_RESULT", persisted.safeErrorCategory)
        assertNull(persisted.txHash)
    }

    @Test
    fun `reverted receipt persists terminal REVERTED state`() = runBlocking {
        val engine = transactionEngine()
        val operation = readyOperation(engine)
        val provider = FakeProvider(
            mapOf(
                "eth_chainId" to listOf("\"0xaa36a7\""),
                "eth_getTransactionCount:latest" to listOf("\"0x0\""),
                "eth_getTransactionCount:pending" to listOf("\"0x0\""),
                "eth_getTransactionByHash" to listOf(transactionJson()),
                "eth_getTransactionReceipt" to listOf(receiptJson(status = "0x0")),
            ),
        )

        val result = runner(provider).submitReviewedOperation(
            ISSUER,
            dedicatedIssuerConfirmed = true,
            observer = MobileIssuerJournalObserver(engine, operation.operationId),
        )

        assertEquals(MobileIssuerStatus.FAILED, result.status)
        assertEquals(TransactionOperationState.REVERTED, engine.find(operation.operationId)?.state)
        assertEquals(HASH, engine.find(operation.operationId)?.txHash)
    }

    @Test
    fun `post-submit timeout is UNKNOWN and same operation cannot submit twice`() = runBlocking {
        val engine = transactionEngine()
        val operation = readyOperation(engine)
        val provider = FakeProvider(
            mapOf(
                "eth_chainId" to listOf("\"0xaa36a7\"", "\"0xaa36a7\""),
                "eth_getTransactionCount:latest" to listOf("\"0x7\"", "\"0x7\""),
                "eth_getTransactionCount:pending" to listOf("\"0x7\"", "\"0x7\""),
                "eth_getTransactionByHash" to listOf("null"),
                "eth_getTransactionReceipt" to listOf("null"),
            ),
            sendResponse = "\"$HASH\"",
        )
        val runner = runner(provider, readbackAttempts = 1)

        val observer = MobileIssuerJournalObserver(engine, operation.operationId)
        val first = runner.submitReviewedOperation(ISSUER, true, observer = observer)
        val second = runner.submitReviewedOperation(ISSUER, true, observer = observer)

        assertEquals(MobileIssuerStatus.UNKNOWN, first.status)
        assertEquals("POST_SUBMIT_READBACK_TIMEOUT", first.category)
        assertEquals(MobileIssuerStatus.FAILED, second.status)
        assertTrue(second.category.startsWith("VALIDATE_OPERATION_"))
        assertEquals(1, provider.sendCalls)
    }

    private fun runner(
        provider: MobileIssuerProvider,
        readbackAttempts: Int = 1,
        transactionJsonBuilder: (EthereumTransactionPayload) -> String = EthereumTransactionJsonBuilder::build,
    ) =
        MobileIssuerAdmissionRunner(
            provider = provider,
            readProvider = provider,
            wait = {},
            readbackAttempts = readbackAttempts,
            reconciliationAttempts = 1,
            pollDelayMillis = 0,
            transactionJsonBuilder = transactionJsonBuilder,
        )

    private fun transactionEngine(
        store: TransactionJournalStore = MemoryStore(),
        ids: ArrayDeque<String> = ArrayDeque(listOf("m1-test-operation", "m1-test-operation-2")),
    ): RecoverableTransactionEngine = RecoverableTransactionEngine(
        TransactionJournal(store),
        newOperationId = { ids.removeFirst() },
    )

    private fun readyOperation(
        engine: RecoverableTransactionEngine,
        chainId: Long = 11155111L,
    ): PersistedTransactionOperation {
        val operation = engine.create(
            TransactionIntent(
                MobileIssuerJournalObserver.OPERATION_TYPE,
                ISSUER,
                chainId,
                ISSUER,
                "0",
                "0x",
            ),
        )
        engine.transition(operation.operationId, TransactionOperationState.READY_TO_REVIEW)
        return engine.transition(operation.operationId, TransactionOperationState.READY_TO_SUBMIT)
    }

    private fun m1Intent() = TransactionIntent(
        MobileIssuerJournalObserver.OPERATION_TYPE,
        ISSUER,
        11155111L,
        ISSUER,
        "0",
        "0x",
    )

    private fun successfulProvider(preNonce: Long) = FakeProvider(
        mapOf(
            "eth_chainId" to listOf("\"0xaa36a7\"", "\"0xaa36a7\""),
            "eth_getTransactionCount:latest" to listOf(
                "\"0x${preNonce.toString(16)}\"",
                "\"0x${(preNonce + 1).toString(16)}\"",
            ),
            "eth_getTransactionCount:pending" to listOf(
                "\"0x${preNonce.toString(16)}\"",
                "\"0x${(preNonce + 1).toString(16)}\"",
            ),
            "eth_getTransactionByHash" to listOf(transactionJson()),
            "eth_getTransactionReceipt" to listOf(receiptJson()),
        ),
    )

    private fun readinessProvider(
        balance: String = "0x1",
        latest: String = "0x0",
        pending: String = "0x0",
        failures: Map<String, Throwable> = emptyMap(),
        switchFailure: Throwable? = null,
    ) = FakeProvider(
        mapOf(
            "eth_chainId" to listOf("\"0xaa36a7\""),
            "eth_getBalance" to listOf("\"$balance\""),
            "eth_getTransactionCount:latest" to listOf("\"$latest\""),
            "eth_getTransactionCount:pending" to listOf("\"$pending\""),
        ),
        failures = failures,
        switchFailure = switchFailure,
    )

    private suspend fun assertStageFailure(
        provider: FakeProvider,
        expectedStage: MobileIssuerReadinessStage,
    ) {
        val events = mutableListOf<MobileIssuerStageEvent>()

        val result = runner(provider).inspect(ISSUER, true, events::add)

        assertEquals(MobileIssuerStatus.BLOCKED, result.status)
        assertEquals(expectedStage, result.diagnostic?.stage)
        assertEquals("EMBEDDED_WALLET_EXCEPTION", result.diagnostic?.category)
        assertNull(result.diagnostic?.code)
        assertEquals("RPC error: method not supported", result.diagnostic?.message)
        assertEquals(expectedStage, events.single { it.outcome == MobileIssuerStageOutcome.FAIL }.stage)
        assertEquals(0, provider.sendCalls)
    }

    private suspend fun assertPreSubmitFailure(
        provider: FakeProvider,
        expectedStage: String,
        expectedCategory: String,
    ) {
        val engine = transactionEngine()
        val operation = readyOperation(engine)

        val result = runner(provider).submitReviewedOperation(
            ISSUER,
            true,
            observer = MobileIssuerJournalObserver(engine, operation.operationId),
        )

        assertEquals(MobileIssuerStatus.FAILED, result.status)
        assertEquals(expectedStage, result.failure?.stage)
        assertEquals(expectedCategory, result.failure?.category)
        assertEquals(TransactionOperationState.READY_TO_SUBMIT, engine.find(operation.operationId)?.state)
        assertEquals(expectedStage, engine.find(operation.operationId)?.failureStage)
        assertEquals(0, provider.sendCalls)
    }

    @Test
    fun `runtime wallet must match the persisted approved intent`() = runBlocking {
        val engine = transactionEngine()
        val operation = readyOperation(engine)
        val provider = FakeProvider(emptyMap())

        val result = runner(provider).submitReviewedOperation(
            "0x2222222222222222222222222222222222222222",
            dedicatedIssuerConfirmed = true,
            observer = MobileIssuerJournalObserver(engine, operation.operationId),
        )

        assertEquals(MobileIssuerStatus.FAILED, result.status)
        assertTrue(result.category.startsWith("VALIDATE_WALLET_"))
        assertTrue(provider.requests.isEmpty())
        assertEquals(0, provider.sendCalls)
        val persisted = engine.find(operation.operationId)!!
        assertEquals("VALIDATE_WALLET", result.failure?.stage)
        assertEquals(TransactionOperationState.READY_TO_SUBMIT, persisted.state)
        assertEquals("VALIDATE_WALLET", persisted.failureStage)
    }

    @Test
    fun `local intent failure stays READY_TO_SUBMIT and is visible without sending`() = runBlocking {
        val engine = transactionEngine()
        val operation = readyOperation(engine, chainId = 1L)
        val provider = FakeProvider(emptyMap())

        val result = runner(provider).submitReviewedOperation(
            ISSUER,
            true,
            observer = MobileIssuerJournalObserver(engine, operation.operationId),
        )

        val persisted = engine.find(operation.operationId)!!
        assertEquals("VALIDATE_INTENT", result.failure?.stage)
        assertEquals(TransactionOperationState.READY_TO_SUBMIT, persisted.state)
        assertEquals("VALIDATE_INTENT", persisted.failureStage)
        assertEquals(0, provider.sendCalls)
        assertTrue(MobileIssuerUiPolicy.reviewEnabled(persisted.state))
        val rendered = MobileIssuerUiPolicy.operationStatusText(persisted)!!
        assertTrue(rendered.contains("Could not prepare"))
        assertTrue(rendered.contains("Stage: VALIDATE_INTENT"))
        assertFalse(rendered.contains("READY TO SUBMIT"))
    }

    @Test
    fun `switch failure stays retryable at exact pre-submit stage`() = runBlocking {
        val provider = FakeProvider(emptyMap(), switchFailure = embeddedFailure())
        assertPreSubmitFailure(
            provider = provider,
            expectedStage = "SWITCH_CHAIN",
            expectedCategory = "EMBEDDED_WALLET_EXCEPTION",
        )
        assertEquals(1, provider.switchCalls)
    }

    @Test
    fun `wrong chain stays retryable without sending`() = runBlocking {
        assertPreSubmitFailure(
            provider = FakeProvider(mapOf("eth_chainId" to listOf("\"0x1\""))),
            expectedStage = "READ_CHAIN_ID",
            expectedCategory = "WRONG_CHAIN",
        )
    }

    @Test
    fun `nonce mismatch stays retryable without sending`() = runBlocking {
        assertPreSubmitFailure(
            provider = FakeProvider(
                mapOf(
                    "eth_chainId" to listOf("\"0xaa36a7\""),
                    "eth_getTransactionCount:latest" to listOf("\"0x0\""),
                    "eth_getTransactionCount:pending" to listOf("\"0x1\""),
                ),
            ),
            expectedStage = "READ_NONCE_PENDING",
            expectedCategory = "PENDING_TRANSACTION",
        )
    }

    @Test
    fun `latest nonce exception stays retryable at exact stage`() = runBlocking {
        assertPreSubmitFailure(
            provider = FakeProvider(
                mapOf("eth_chainId" to listOf("\"0xaa36a7\"")),
                failures = mapOf("eth_getTransactionCount:latest" to embeddedFailure()),
            ),
            expectedStage = "READ_NONCE_LATEST",
            expectedCategory = "EMBEDDED_WALLET_EXCEPTION",
        )
    }

    @Test
    fun `claim persistence failure keeps READY_TO_SUBMIT and makes send unreachable`() = runBlocking {
        val store = FailingStore()
        val engine = transactionEngine(store = store)
        val operation = readyOperation(engine)
        store.failSaves = true
        val provider = FakeProvider(
            mapOf(
                "eth_chainId" to listOf("\"0xaa36a7\""),
                "eth_getTransactionCount:latest" to listOf("\"0x0\""),
                "eth_getTransactionCount:pending" to listOf("\"0x0\""),
            ),
        )

        val result = runner(provider).submitReviewedOperation(
            ISSUER,
            true,
            observer = MobileIssuerJournalObserver(engine, operation.operationId),
        )

        assertEquals("CLAIM_SUBMISSION", result.failure?.stage)
        assertEquals(TransactionOperationState.READY_TO_SUBMIT, engine.find(operation.operationId)?.state)
        assertEquals(0, provider.sendCalls)
    }

    @Test
    fun `rearmed operation is not blocked by an earlier operation in the same runner`() = runBlocking {
        val engine = transactionEngine(ids = ArrayDeque(listOf("old-operation", "rearmed-operation")))
        val old = readyOperation(engine)
        engine.beginSubmission(old.operationId, "0", "0")
        engine.proveNoBroadcast(old.operationId, "0", "0")
        val rearmed = engine.rearm(old.operationId, m1Intent())
        engine.transition(rearmed.operationId, TransactionOperationState.READY_TO_REVIEW)
        engine.transition(rearmed.operationId, TransactionOperationState.READY_TO_SUBMIT)
        val provider = successfulProvider(preNonce = 0)

        val result = runner(provider).submitReviewedOperation(
            ISSUER,
            true,
            observer = MobileIssuerJournalObserver(engine, rearmed.operationId),
        )

        assertEquals(MobileIssuerStatus.CONFIRMED, result.status)
        assertEquals(1, provider.sendCalls)
    }

    @Test
    fun `two distinct operation ids can each submit once with one runner`() = runBlocking {
        val engine = transactionEngine(ids = ArrayDeque(listOf("operation-1", "operation-2")))
        val first = readyOperation(engine)
        val provider = FakeProvider(
            mapOf(
                "eth_chainId" to List(4) { "\"0xaa36a7\"" },
                "eth_getTransactionCount:latest" to listOf("\"0x0\"", "\"0x1\"", "\"0x1\"", "\"0x2\""),
                "eth_getTransactionCount:pending" to listOf("\"0x0\"", "\"0x1\"", "\"0x1\"", "\"0x2\""),
                "eth_getTransactionByHash" to listOf(transactionJson(), transactionJson()),
                "eth_getTransactionReceipt" to listOf(receiptJson(), receiptJson()),
            ),
        )
        val admissionRunner = runner(provider)

        val firstResult = admissionRunner.submitReviewedOperation(
            ISSUER,
            true,
            observer = MobileIssuerJournalObserver(engine, first.operationId),
        )
        val second = readyOperation(engine)
        val secondResult = admissionRunner.submitReviewedOperation(
            ISSUER,
            true,
            observer = MobileIssuerJournalObserver(engine, second.operationId),
        )

        assertEquals(MobileIssuerStatus.CONFIRMED, firstResult.status)
        assertEquals(MobileIssuerStatus.CONFIRMED, secondResult.status)
        assertEquals(2, provider.sendCalls)
    }

    @Test
    fun `no broadcast reconciliation samples nonces for a bounded interval`() = runBlocking {
        val provider = FakeProvider(
            mapOf(
                "eth_chainId" to listOf("\"0xaa36a7\""),
                "eth_getTransactionCount:latest" to List(3) { "\"0x0\"" },
                "eth_getTransactionCount:pending" to List(3) { "\"0x0\"" },
            ),
        )
        val runner = MobileIssuerAdmissionRunner(
            provider = provider,
            readProvider = provider,
            wait = {},
            reconciliationAttempts = 3,
            pollDelayMillis = 0,
        )

        val nonces = runner.reconcileNoBroadcast(ISSUER)

        assertEquals(bi(0) to bi(0), nonces)
        assertEquals(3, provider.requests.count { it == "eth_getTransactionCount" } / 2)
        assertEquals(0, provider.sendCalls)
    }

    private fun embeddedFailure() = EmbeddedWalletException("RPC error: method not supported")

    private fun transactionJson(
        from: String = ISSUER,
        to: String = ISSUER,
        value: String = "0x0",
        input: String = "0x",
    ) = "{\"hash\":\"$HASH\",\"from\":\"$from\",\"to\":\"$to\",\"value\":\"$value\",\"input\":\"$input\"}"

    private fun receiptJson(status: String = "0x1") =
        "{\"status\":\"$status\",\"from\":\"$ISSUER\",\"to\":\"$ISSUER\",\"blockNumber\":\"0x2a\"}"

    private fun realTransactionJson() =
        "{\"hash\":\"$REAL_HASH\",\"from\":\"$PHYSICAL_ISSUER\",\"to\":\"$PHYSICAL_ISSUER\"," +
            "\"value\":\"0x0\",\"input\":\"0x\",\"nonce\":\"0x0\"," +
            "\"blockNumber\":\"0xb2459a\",\"chainId\":\"0xaa36a7\",\"type\":\"0x2\"}"

    private fun realReceiptJson() =
        "{\"transactionHash\":\"$REAL_HASH\",\"status\":\"0x1\"," +
            "\"from\":\"$PHYSICAL_ISSUER\",\"to\":\"$PHYSICAL_ISSUER\"," +
            "\"blockNumber\":\"0xb2459a\",\"gasUsed\":\"0x5208\"," +
            "\"effectiveGasPrice\":\"0x41230b9f\"}"

    private fun bi(value: Long) = BigInteger.valueOf(value)

    private open class MemoryStore(var value: String? = null) : TransactionJournalStore {
        override fun load(): String? = value
        override fun save(serializedJournal: String) {
            value = serializedJournal
        }
    }

    private class FailingStore : MemoryStore() {
        var failSaves = false
        override fun save(serializedJournal: String) {
            if (failSaves) error("journal unavailable")
            super.save(serializedJournal)
        }
    }

    private class FakeProvider(
        responses: Map<String, List<String>>,
        private val sendResponse: String = "\"$HASH\"",
        private val failures: Map<String, Throwable> = emptyMap(),
        private val switchFailure: Throwable? = null,
        private val sendFailure: Throwable? = null,
        private val beforeSend: () -> Unit = {},
    ) : MobileIssuerProvider {
        private val queues = responses.mapValues { ArrayDeque(it.value) }.toMutableMap()
        val requests = mutableListOf<String>()
        val rpcRequests = mutableListOf<CapturedRpcRequest>()
        val sentTransactions = mutableListOf<String>()
        var sendCalls = 0
        var switchCalls = 0

        override suspend fun switchToSepolia() {
            switchCalls += 1
            switchFailure?.let { throw it }
        }

        override suspend fun request(method: String, params: List<String>): String {
            requests += method
            rpcRequests += CapturedRpcRequest(method, params)
            val key = if (method == "eth_getTransactionCount") {
                "$method:${params.last().removeSurrounding("\"")}"
            } else {
                method
            }
            failures[key]?.let { throw it }
            return queues[key]?.removeFirstOrNull() ?: error("Missing fake response for $key")
        }

        override suspend fun sendTransaction(transactionJson: String): String {
            beforeSend()
            sendCalls += 1
            sentTransactions += transactionJson
            rpcRequests += CapturedRpcRequest("eth_sendTransaction", listOf(transactionJson))
            sendFailure?.let { throw it }
            return sendResponse
        }
    }

    private data class CapturedRpcRequest(val method: String, val params: List<String>)

    companion object {
        private const val ISSUER = "0x1111111111111111111111111111111111111111"
        private const val PHYSICAL_ISSUER = "0xFa90e8301A22833B74378C5fA3a7c120Ac512685"
        private const val REAL_HASH =
            "0x6c4f42f2d368936d4aaf7edf3e0395c376f92b699563b34fee4ed053a0a53e32"
        private const val HASH = "0xaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
        private const val EXACT_TRANSACTION =
            "{\"from\":\"$ISSUER\",\"to\":\"$ISSUER\",\"value\":\"0x0\",\"data\":\"0x\"," +
                "\"chainId\":\"0xaa36a7\"}"
    }
}
