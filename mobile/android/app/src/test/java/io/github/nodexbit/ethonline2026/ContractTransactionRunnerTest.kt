package io.github.nodexbit.ethonline2026

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ContractTransactionRunnerTest {
    @Test
    fun `final create verifies before persistence and reruns fresh preflight at submit`() = runBlocking {
        val wallet = FakeWallet()
        val engine = RecoverableTransactionEngine(TransactionJournal(MemoryJournal()))
        val runner = runner(wallet, engine)
        val request = registerRequest()
        var preflightCalls = 0

        runner.verifyNewSubmission(request) { preflightCalls += 1 }
        assertNull(engine.latestForWallet(IssuerSpace.issuer, ContractTransactionRunner.REGISTER_OPERATION))

        val operation = runner.create(request)
        runner.review(operation.operationId)
        val result = runner.submit(operation.operationId, request, permit = safetyPermit(operation, request), preflight = {
            preflightCalls += 1
            error("STOP_BEFORE_WRITE")
        })

        assertEquals(2, preflightCalls)
        assertEquals(MobileIssuerStatus.FAILED, result.status)
        assertEquals(0, wallet.sendCalls)
        assertNotNull(engine.find(operation.operationId))
    }

    @Test
    fun `failed safety preflight records failure and never asks Privy to write`() = runBlocking {
        val wallet = FakeWallet()
        val engine = RecoverableTransactionEngine(TransactionJournal(MemoryJournal()))
        val runner = runner(wallet, engine)
        val request = registerRequest()
        val operation = runner.create(request)
        runner.review(operation.operationId)
        val result = runner.submit(operation.operationId, request, permit = safetyPermit(operation, request), preflight = { error("NOT_AVAILABLE") })
        assertEquals(MobileIssuerStatus.FAILED, result.status)
        assertEquals(0, wallet.sendCalls)
        val persisted = engine.find(operation.operationId)!!
        assertEquals(TransactionOperationState.READY_TO_SUBMIT, persisted.state)
        assertNotNull(persisted.safeErrorCategory)
    }

    @Test
    fun `no-hash restart stays uncertain and blocks replacement`() = runBlocking {
        val wallet = FakeWallet()
        val engine = RecoverableTransactionEngine(TransactionJournal(MemoryJournal()))
        val runner = runner(wallet, engine)
        val request = registerRequest()
        val operation = runner.create(request).let { runner.review(it.operationId) }
        engine.beginSubmission(operation.operationId, "1", "1")

        val recovered = runner.recover(operation.operationId, request)
        assertEquals(MobileIssuerStatus.UNKNOWN, recovered.status)
        assertEquals("SUBMISSION_OUTCOME_UNRESOLVED", recovered.category)
        assertEquals(TransactionOperationState.UNKNOWN, engine.find(operation.operationId)?.state)
        assertEquals(0, wallet.sendCalls)
        assertEquals(0, wallet.switchCalls)

        assertTrue(runCatching { runner.create(request) }.isFailure)
    }

    private fun runner(wallet: FakeWallet, engine: RecoverableTransactionEngine) = ContractTransactionRunner(
            identity = StudioRunnerIdentity(StudioWalletBinding(IssuerSpace.issuer, "test-provider", 1), IssuerSpace.chainId),
            walletProvider = wallet,
            client = ReadOnlyEthereumRpcClient(
                transport = ReadOnlyRpcTransport { request, _ ->
                    val id = Regex("\"id\":(\\d+)").find(request)!!.groupValues[1]
                    val result = if (request.contains("eth_chainId")) "\"0xaa36a7\"" else "\"0x1\""
                    "{\"jsonrpc\":\"2.0\",\"id\":$id,\"result\":$result}"
                },
            ),
            engine = engine,
            wait = {},
            attempts = 1,
        )

    private fun registerRequest() = ContractTransactionRequest(
            ContractTransactionRunner.REGISTER_OPERATION,
            IssuerSpace.issuer,
            IssuerSpace.registry,
            CredentialAbi.register(
                IssuerSpace.STAFF_LABEL,
                IssuerSpace.STAFF_HOLDER,
                IssuerSpace.resolver,
                java.math.BigInteger.valueOf(IssuerSpace.STAFF_EXPIRY),
            ),
        )

    private class FakeWallet : MobileIssuerWalletProvider {
        var sendCalls = 0
        var switchCalls = 0
        override suspend fun switchToSepolia() { switchCalls += 1 }
        override suspend fun sendTransaction(transactionJson: String): String {
            sendCalls += 1
            return "0x${"1".repeat(64)}"
        }
    }

    private class MemoryJournal : TransactionJournalStore {
        private var value: String? = null
        override fun load(): String? = value
        override fun save(serializedJournal: String) { value = serializedJournal }
    }
}
