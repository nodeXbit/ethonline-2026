package io.github.nodexbit.ethonline2026

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class RecoverableTransactionEngineTest {
    @Test
    fun `state transitions are explicit and illegal jumps fail`() {
        val fixture = fixture()
        val operation = fixture.engine.create(intent())

        assertThrows(IllegalArgumentException::class.java) {
            fixture.engine.transition(operation.operationId, TransactionOperationState.CONFIRMED)
        }
        fixture.engine.transition(operation.operationId, TransactionOperationState.READY_TO_REVIEW)
        fixture.engine.transition(operation.operationId, TransactionOperationState.READY_TO_SUBMIT)
        assertEquals(
            TransactionOperationState.READY_TO_SUBMIT,
            fixture.engine.find(operation.operationId)?.state,
        )
    }

    @Test
    fun `journal persists and reloads every required field`() {
        val store = MemoryStore()
        val first = fixture(store)
        val operation = ready(first.engine)
        first.engine.beginSubmission(operation.operationId, "7", "7")
        first.engine.recordHash(operation.operationId, HASH)

        val reloaded = fixture(store).engine.find(operation.operationId)!!

        assertEquals("M1_MOBILE_ISSUER_ADMISSION", reloaded.operationType)
        assertEquals(ISSUER, reloaded.walletAddress)
        assertEquals(11155111L, reloaded.chainId)
        assertEquals(ISSUER, reloaded.targetAddress)
        assertEquals("0", reloaded.valueWei)
        assertEquals("0x", reloaded.dataSummary)
        assertEquals("7", reloaded.preLatestNonce)
        assertEquals("7", reloaded.prePendingNonce)
        assertEquals(HASH, reloaded.txHash)
        assertEquals(TransactionOperationState.HASH_RECEIVED, reloaded.state)
    }

    @Test
    fun `journal reload remains compatible with existing seventeen-field records`() {
        val serialized = listOf(
            "legacy-operation",
            "M1_MOBILE_ISSUER_ADMISSION",
            ISSUER,
            "11155111",
            ISSUER,
            "0",
            "0x",
            "v0",
            "v0",
            "n",
            "NO_BROADCAST_PROVEN",
            "n",
            "100",
            "101",
            "n",
            "n",
            "n",
        ).joinToString("|")

        val reloaded = fixture(MemoryStore(serialized)).engine.find("legacy-operation")!!

        assertEquals(TransactionOperationState.NO_BROADCAST_PROVEN, reloaded.state)
        assertNull(reloaded.failureStage)
        assertNull(reloaded.safeExceptionClass)
        assertNull(reloaded.safeErrorMessage)
    }

    @Test
    fun `SUBMITTING_NO_HASH is durable before send can run`() {
        val fixture = fixture()
        val operation = ready(fixture.engine)
        var sendObservedPersistedState = false

        fixture.engine.beginSubmission(operation.operationId, "0", "0")
        sendObservedPersistedState = fixture.engine.find(operation.operationId)?.state ==
            TransactionOperationState.SUBMITTING_NO_HASH

        assertTrue(sendObservedPersistedState)
        assertNull(fixture.engine.find(operation.operationId)?.txHash)
    }

    @Test
    fun `hash is durable immediately after return`() {
        val store = MemoryStore()
        val fixture = fixture(store)
        val operation = ready(fixture.engine)
        fixture.engine.beginSubmission(operation.operationId, "0", "0")

        fixture.engine.recordHash(operation.operationId, HASH)

        val reloaded = fixture(store).engine.find(operation.operationId)!!
        assertEquals(HASH, reloaded.txHash)
        assertEquals(TransactionOperationState.HASH_RECEIVED, reloaded.state)
    }

    @Test
    fun `restart after SUBMITTING_NO_HASH preserves recovery state`() {
        val store = MemoryStore()
        val first = fixture(store)
        val operation = ready(first.engine)
        first.engine.beginSubmission(operation.operationId, "0", "0")

        val reloaded = fixture(store).engine.latestForWallet(ISSUER)!!

        assertEquals(TransactionOperationState.SUBMITTING_NO_HASH, reloaded.state)
        assertNull(reloaded.txHash)
    }

    @Test
    fun `restart after HASH_RECEIVED preserves hash and never permits resubmit`() {
        val store = MemoryStore()
        val first = fixture(store)
        val operation = ready(first.engine)
        first.engine.beginSubmission(operation.operationId, "0", "0")
        first.engine.recordHash(operation.operationId, HASH)
        val restarted = fixture(store).engine

        assertThrows(IllegalArgumentException::class.java) {
            restarted.beginSubmission(operation.operationId, "0", "0")
        }
        assertEquals(HASH, restarted.find(operation.operationId)?.txHash)
    }

    @Test
    fun `UNKNOWN never permits resubmit`() {
        val fixture = fixture()
        val operation = ready(fixture.engine)
        fixture.engine.beginSubmission(operation.operationId, "0", "0")
        fixture.engine.markUnknown(operation.operationId, "SUBMISSION_FAILURE")

        assertThrows(IllegalArgumentException::class.java) {
            fixture.engine.beginSubmission(operation.operationId, "0", "0")
        }
    }

    @Test
    fun `unchanged latest and pending nonce proves no broadcast`() {
        val fixture = fixture()
        val operation = ready(fixture.engine)
        fixture.engine.beginSubmission(operation.operationId, "0", "0")
        fixture.engine.markUnknown(operation.operationId, "SEND_EXCEPTION")

        val reconciled = fixture.engine.proveNoBroadcast(operation.operationId, "0", "0")

        assertEquals(TransactionOperationState.NO_BROADCAST_PROVEN, reconciled.state)
    }

    @Test
    fun `changed nonce cannot prove no broadcast`() {
        val fixture = fixture()
        val operation = ready(fixture.engine)
        fixture.engine.beginSubmission(operation.operationId, "0", "0")

        val reconciled = fixture.engine.proveNoBroadcast(operation.operationId, "1", "1")

        assertEquals(TransactionOperationState.UNKNOWN, reconciled.state)
        assertEquals("NONCE_CHANGED_NO_BROADCAST_NOT_PROVEN", reconciled.safeErrorCategory)
    }

    @Test
    fun `explicit rearm creates new operation and retains history`() {
        val fixture = fixture(ids = ArrayDeque(listOf("old", "new")))
        val historical = ready(fixture.engine)
        fixture.engine.beginSubmission(historical.operationId, "0", "0")
        fixture.engine.proveNoBroadcast(historical.operationId, "0", "0")

        val rearmed = fixture.engine.rearm(historical.operationId, intent())

        assertNotEquals(historical.operationId, rearmed.operationId)
        assertEquals(TransactionOperationState.DRAFT, rearmed.state)
        assertEquals(2, fixture.engine.all().size)
        assertEquals(
            TransactionOperationState.NO_BROADCAST_PROVEN,
            fixture.engine.find(historical.operationId)?.state,
        )
    }

    @Test
    fun `no broadcast history requires explicit rearm`() {
        val fixture = fixture()
        val historical = ready(fixture.engine)
        fixture.engine.beginSubmission(historical.operationId, "0", "0")
        fixture.engine.proveNoBroadcast(historical.operationId, "0", "0")

        val error = assertThrows(IllegalArgumentException::class.java) {
            fixture.engine.create(intent())
        }

        assertEquals("REARM_REQUIRED", error.message)
    }

    @Test
    fun `legacy boolean migration with zero nonces proves no broadcast`() {
        val fixture = fixture()
        val legacy = fixture.engine.migrateLegacyAttemptIfNeeded(
            intent(),
            legacyAttempted = true,
            alreadyMigrated = false,
        )!!

        val reconciled = fixture.engine.proveNoBroadcast(legacy.operationId, "0", "0")

        assertEquals(TransactionOperationState.NO_BROADCAST_PROVEN, reconciled.state)
        assertEquals("0", reconciled.preLatestNonce)
    }

    @Test
    fun `legacy migration with ambiguous nonce remains UNKNOWN`() {
        val fixture = fixture()
        val legacy = fixture.engine.migrateLegacy(intent())

        val reconciled = fixture.engine.proveNoBroadcast(legacy.operationId, "1", "1")

        assertEquals(TransactionOperationState.UNKNOWN, reconciled.state)
    }

    @Test
    fun `legacy boolean is ignored after migration`() {
        val fixture = fixture()

        val operation = fixture.engine.migrateLegacyAttemptIfNeeded(
            intent(),
            legacyAttempted = true,
            alreadyMigrated = true,
        )

        assertNull(operation)
        assertTrue(fixture.engine.all().isEmpty())
    }

    @Test
    fun `Sepolia explorer seam accepts only a transaction hash`() {
        assertEquals(
            "https://sepolia.etherscan.io/tx/$HASH",
            RecoverableTransactionEngine.sepoliaExplorerUrl(HASH),
        )
        assertThrows(IllegalArgumentException::class.java) {
            RecoverableTransactionEngine.sepoliaExplorerUrl("0x1234")
        }
    }

    @Test
    fun `only one active operation is allowed per wallet`() {
        val fixture = fixture()
        fixture.engine.create(intent())

        val error = assertThrows(IllegalArgumentException::class.java) {
            fixture.engine.create(intent())
        }

        assertEquals("ACTIVE_OPERATION_EXISTS", error.message)
    }

    @Test
    fun `confirmation persists public evidence`() {
        val store = MemoryStore()
        val fixture = fixture(store)
        val operation = ready(fixture.engine)
        fixture.engine.beginSubmission(operation.operationId, "7", "7")
        fixture.engine.recordHash(operation.operationId, HASH)
        fixture.engine.transition(operation.operationId, TransactionOperationState.CONFIRMING)
        fixture.engine.transition(operation.operationId, TransactionOperationState.ONCHAIN_READBACK)

        fixture.engine.confirm(operation.operationId, "42", "8", "8")

        val reloaded = fixture(store).engine.find(operation.operationId)!!
        assertEquals(TransactionOperationState.CONFIRMED, reloaded.state)
        assertEquals("42", reloaded.receiptBlock)
        assertEquals("8", reloaded.postLatestNonce)
        assertEquals("8", reloaded.postPendingNonce)
    }

    private fun ready(engine: RecoverableTransactionEngine): PersistedTransactionOperation {
        val operation = engine.create(intent())
        engine.transition(operation.operationId, TransactionOperationState.READY_TO_REVIEW)
        return engine.transition(operation.operationId, TransactionOperationState.READY_TO_SUBMIT)
    }

    private fun fixture(
        store: MemoryStore = MemoryStore(),
        ids: ArrayDeque<String> = ArrayDeque(listOf("operation-1", "operation-2")),
    ): Fixture {
        var timestamp = 100L
        val journal = TransactionJournal(store)
        return Fixture(
            RecoverableTransactionEngine(
                journal,
                now = { timestamp++ },
                newOperationId = { ids.removeFirst() },
            ),
        )
    }

    private fun intent() = TransactionIntent(
        operationType = "M1_MOBILE_ISSUER_ADMISSION",
        walletAddress = ISSUER,
        chainId = 11155111L,
        targetAddress = ISSUER,
        valueWei = "0",
        dataSummary = "0x",
    )

    private data class Fixture(val engine: RecoverableTransactionEngine)

    private class MemoryStore(var value: String? = null) : TransactionJournalStore {
        override fun load(): String? = value
        override fun save(serializedJournal: String) {
            value = serializedJournal
        }
    }

    companion object {
        private const val ISSUER = "0x1111111111111111111111111111111111111111"
        private const val HASH = "0xaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
    }
}
