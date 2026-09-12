package io.github.nodexbit.ethonline2026

import java.math.BigInteger
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

/** Exercises the runner and persisted stores used by Activity final-confirmation paths. */
class StudioCoordinationTest {
    private val walletA = IssuerSpace.issuer
    private val walletB = "0x3333333333333333333333333333333333333333"

    @Test fun `retained A runner rejects valid B permit before either provider is invoked then fresh B sends once`() = runBlocking {
        val coordinator = StudioWriteCoordinator()
        val a = coordinator.select(walletA, "provider-A")
        val f = Fixture(a)
        val b = coordinator.select(walletB, "provider-B")
        val request = request(StudioActionType.ISSUE_REGISTER, "pass-b", walletB)
        val operation = f.prepare(request)
        val permit = permit(coordinator, operation, request)
        val result = f.runner.submit(operation.operationId, request, {}, permit)
        assertEquals("WALLET_CHANGED_REVIEW_AGAIN", result.category)
        assertEquals(MobileIssuerStatus.FAILED, result.status)
        assertEquals(TransactionOperationState.READY_TO_SUBMIT, f.engine.find(operation.operationId)?.state)
        assertEquals(0, f.sends)
        val freshB = Fixture(b)
        assertEquals(0, freshB.sends)
        coordinator.release(permit.lease)
        val freshOperation = freshB.prepare(request)
        val freshPermit = permit(coordinator, freshOperation, request)
        assertEquals(MobileIssuerStatus.CONFIRMED,
            freshB.runner.submit(freshOperation.operationId, request, {}, freshPermit).status)
        assertEquals(0, f.sends)
        assertEquals(1, freshB.sends)
        assertTrue(coordinator.release(freshPermit.lease))
        assertFalse(coordinator.isBusy())
    }

    @Test fun `A B A selection invalidates old A runner and old lease even for same address`() = runBlocking {
        val coordinator = StudioWriteCoordinator()
        val original = coordinator.select(walletA, "provider-A")
        val f = Fixture(original)
        val staleLease = coordinator.acquire(original)
        coordinator.release(staleLease)
        coordinator.select(walletB, "provider-B")
        val returned = coordinator.select(walletA, "provider-A")
        assertNotEquals(original, returned)
        val request = request(StudioActionType.ISSUE_REGISTER, "pass-a")
        val operation = f.prepare(request)
        val permit = permit(coordinator, operation, request)
        assertFalse(coordinator.release(staleLease))
        assertEquals("WALLET_CHANGED_REVIEW_AGAIN", f.runner.submit(operation.operationId, request, {}, permit).category)
        assertEquals(0, f.sends)
        coordinator.release(permit.lease)
        val fresh = Fixture(returned)
        val next = fresh.prepare(request)
        val nextPermit = permit(coordinator, next, request)
        assertEquals(MobileIssuerStatus.CONFIRMED, fresh.runner.submit(next.operationId, request, {}, nextPermit).status)
        assertEquals(1, fresh.sends)
        coordinator.release(nextPermit.lease)
        Unit
    }

    @Test fun `runner chain mismatch cannot invoke provider`() = runBlocking {
        val coordinator = StudioWriteCoordinator()
        val binding = coordinator.select(walletA, "provider-A")
        val f = Fixture(binding, chainId = 1)
        val request = request(StudioActionType.ISSUE_REGISTER, "pass-a")
        val operation = f.prepare(request)
        val permit = permit(coordinator, operation, request)
        assertEquals(MobileIssuerStatus.FAILED, f.runner.submit(operation.operationId, request, {}, permit).status)
        assertEquals(0, f.sends)
        coordinator.release(permit.lease)
        Unit
    }

    @Test fun `callback invalidation after claim is caught at send boundary and remains proven unsent`() = runBlocking {
        val coordinator = StudioWriteCoordinator()
        val f = Fixture(coordinator.select(walletA, "provider-A"))
        val request = request(StudioActionType.ISSUE_REGISTER, "pass-a")
        val operation = f.prepare(request)
        val permit = permit(coordinator, operation, request)
        val result = f.runner.submit(operation.operationId, request, {}, permit) {
            if (it.state == TransactionOperationState.SUBMISSION_CLAIMED) {
                coordinator.release(permit.lease)
                coordinator.select(walletB, "provider-B")
            }
        }
        assertEquals("WALLET_CHANGED_REVIEW_AGAIN", result.category)
        assertEquals(0, f.sends)
        assertEquals(TransactionOperationState.NO_BROADCAST_PROVEN, f.engine.find(operation.operationId)?.state)
        assertFalse(coordinator.isBusy())
    }

    @Test fun `A TX1 final confirmation preserves B TX1 preparation byte for byte`() = conflictScenario(StudioActionType.ISSUE_REGISTER)
    @Test fun `A TX2 final confirmation preserves B TX1 preparation byte for byte`() = conflictScenario(StudioActionType.ISSUE_CONFIGURE)
    @Test fun `management A final confirmation preserves issue B preparation byte for byte`() = conflictScenario(StudioActionType.ACCESS_SUSPEND)
    @Test fun `management A final confirmation preserves management B preparation byte for byte`() =
        conflictScenario(StudioActionType.ACCESS_SUSPEND, managementB = true)

    private fun conflictScenario(action: StudioActionType, managementB: Boolean = false) = runBlocking {
        val coordinator = StudioWriteCoordinator()
        val f = Fixture(coordinator.select(walletA, "provider-A"))
        val issueStore = ProductStore()
        val managementStore = ProductStore()
        val issuance = IssuanceCoordinator(issueStore)
        val management = ManagementSessionCoordinator(managementStore)
        val draft = PassDraft(PassTemplate.STAFF, "pass-b", IssuerSpace.STAFF_HOLDER,
            BigInteger.valueOf(IssuerSpace.STAFF_EXPIRY), true, BigInteger.valueOf(IssuerSpace.STAFF_EXPIRY), false, "B", "")
        val requestB = if (managementB) request(StudioActionType.ACCESS_SUSPEND, "pass-b") else
            request(StudioActionType.ISSUE_REGISTER, "pass-b").copy(data = CredentialAbi.register(draft.label,
                draft.recipient, IssuerSpace.resolver, draft.registrationExpiry, draft.roleBitmap))
        val operationB = f.prepare(requestB)
        if (managementB) management.save(ManagementSession(walletA, draft.fullName, operationB.operationId,
            ManagementMutation(StudioActionType.ACCESS_SUSPEND, requestB.to, requestB.data, "Allowed", "Suspended")))
        else issuance.beginRegister(walletA, draft, operationB.operationId)
        val before = Triple(f.store.value, issueStore.value, managementStore.value)
        val requestA = request(action, "pass-a")
        assertTrue(runCatching { f.runner.requireReviewAvailable(requestA) }.exceptionOrNull() is StudioOperationConflict)
        assertTrue(runCatching { f.runner.create(requestA) }.exceptionOrNull() is StudioOperationConflict)
        assertEquals(before, Triple(f.store.value, issueStore.value, managementStore.value))
        // Same production sequence as Activity: pending-management gate, preflight, runner.create.
        val failure = runCatching {
            management.requireNoPending()
            f.runner.verifyNewSubmission(requestA) {}
            f.runner.create(requestA)
        }.exceptionOrNull() ?: error("A_MUST_BE_BLOCKED")
        val human = SafeActionFailurePolicy.from("Create pass", "ACTION", failure).humanMessage
        assertTrue(human.startsWith("Another pass has an unfinished transaction."))
        assertTrue(human.contains(draft.fullName))
        assertEquals(before, Triple(f.store.value, issueStore.value, managementStore.value))
        // Even background recovery cannot silently cancel B's review.
        assertEquals("REVIEW_REQUIRED", f.runner.recover(operationB.operationId, requestB).category)
        assertEquals(before, Triple(f.store.value, issueStore.value, managementStore.value))
        assertEquals(0, f.sends)
        // The exact production action used by the explicit B cancellation control.
        StudioPreparedOperationResolver(f.engine, issuance, management).cancel(operationB)
        assertEquals(TransactionOperationState.CANCELLED, f.engine.find(operationB.operationId)?.state)
        if (!managementB) assertEquals(IssuanceState.DRAFT, issuance.current(walletA, draft.fullName)?.state)
        management.requireNoPending()
        assertEquals(TransactionOperationState.DRAFT, f.runner.create(requestA).state)
        assertEquals(0, f.sends) // Re-review is required; cancellation never auto-sends A.
    }

    @Test fun `replacement cannot name another credential or action and leaves journal unchanged`() {
        val f = Fixture(StudioWalletBinding(walletA, "provider-A", 1))
        val requestB = request(StudioActionType.ISSUE_REGISTER, "pass-b")
        val b = f.prepare(requestB)
        val before = f.store.value
        for (a in listOf(request(StudioActionType.ISSUE_REGISTER, "pass-a"), request(StudioActionType.ISSUE_CONFIGURE, "pass-b"))) {
            assertTrue(runCatching { f.runner.create(a, b.operationId) }.isFailure)
            assertEquals(before, f.store.value)
        }
    }

    @Test fun `explicit same credential and action re-review replaces only named unsent operation`() {
        val f = Fixture(StudioWalletBinding(walletA, "provider-A", 1))
        val request = request(StudioActionType.ISSUE_REGISTER, "pass-a")
        val original = f.prepare(request)
        val replacement = f.runner.create(request.copy(data = "0xabcd"), original.operationId)
        assertNotEquals(original.operationId, replacement.operationId)
        assertEquals(TransactionOperationState.CANCELLED, f.engine.find(original.operationId)?.state)
        assertEquals(TransactionOperationState.DRAFT, replacement.state)
        assertEquals(0, f.sends)
        // A stale cancellation review cannot touch the new operation.
        assertTrue(runCatching { f.engine.cancelPreparedOperation(original) }.isFailure)
        assertEquals(replacement, f.engine.find(replacement.operationId))
    }

    @Test fun `failed claimed product callback has zero sends and durable no broadcast after restart`() = runBlocking {
        val coordinator = StudioWriteCoordinator()
        val f = Fixture(coordinator.select(walletA, "provider-A"))
        val request = request(StudioActionType.ISSUE_REGISTER, "pass-a")
        val operation = f.prepare(request)
        val permit = permit(coordinator, operation, request)
        val result = f.runner.submit(operation.operationId, request, {}, permit) { error("PRODUCT_PERSISTENCE_FAILED") }
        assertEquals(MobileIssuerStatus.FAILED, result.status)
        assertEquals(0, f.sends)
        assertTrue(coordinator.release(permit.lease))
        assertFalse(coordinator.isBusy())
        val restarted = f.restart()
        assertEquals(TransactionOperationState.NO_BROADCAST_PROVEN, restarted.engine.find(operation.operationId)?.state)
        repeat(3) { assertEquals("NO_BROADCAST_PROVEN", restarted.runner.recover(operation.operationId, request).category) }
        assertEquals(TransactionOperationState.DRAFT, restarted.runner.create(request).state)
        assertEquals(0, restarted.sends)
    }

    @Test fun `same-session TX2 re-review replaces exact preparation and persists revised configuration`() = runBlocking {
        val coordinator = StudioWriteCoordinator()
        val f = Fixture(coordinator.select(walletA, "provider-A"))
        val issuance = IssuanceCoordinator(ProductStore())
        val draft = PassDraft(PassTemplate.STAFF, "pass-a", IssuerSpace.STAFF_HOLDER,
            BigInteger.valueOf(IssuerSpace.STAFF_EXPIRY), true, BigInteger.valueOf(IssuerSpace.STAFF_EXPIRY), false, "A", "")
        val register = request(StudioActionType.ISSUE_REGISTER, "pass-a").copy(data = CredentialAbi.register(
            draft.label, draft.recipient, IssuerSpace.resolver, draft.registrationExpiry, draft.roleBitmap))
        val tx1 = f.prepare(register)
        val session = issuance.beginRegister(walletA, draft, tx1.operationId)
        val permit = permit(coordinator, tx1, register)
        assertEquals(MobileIssuerStatus.CONFIRMED, f.runner.submit(tx1.operationId, register, {}, permit) {
            issuance.reconcile(session.identity, it)
        }.status)
        coordinator.release(permit.lease)
        val registered = issuance.get(session.identity)
        val records = request(StudioActionType.ISSUE_CONFIGURE, "pass-a").copy(data = CredentialConfigurationPolicy.calldata(registered))
        val old = f.prepare(records)
        val ready = issuance.recordsReady(session.identity, old.operationId, registered)
        val reviewed = ready.copy(accessValidUntil = ready.accessValidUntil - BigInteger.valueOf(60))
        val next = f.runner.create(records.copy(data = CredentialConfigurationPolicy.calldata(reviewed)), ready.recordsOperationId)
        issuance.reconcile(session.identity, checkNotNull(f.engine.find(old.operationId)))
        issuance.recordsReady(session.identity, next.operationId, reviewed)
        assertEquals(session.identity, issuance.get(session.identity).identity)
        assertEquals(next.operationId, issuance.get(session.identity).recordsOperationId)
        assertEquals(reviewed.accessValidUntil, issuance.get(session.identity).accessValidUntil)
        assertEquals(TransactionOperationState.CONFIRMED, f.engine.find(tx1.operationId)?.state)
        assertEquals(1, f.sends) // Only the fake TX1; rebuilding TX2 never sends it.
    }

    @Test fun `failed claim persistence remains safely prepared without invoking provider`() = runBlocking {
        val coordinator = StudioWriteCoordinator()
        val f = Fixture(coordinator.select(walletA, "provider-A"))
        val request = request(StudioActionType.ISSUE_REGISTER, "pass-a")
        val operation = f.prepare(request)
        val permit = permit(coordinator, operation, request)
        f.store.failState = TransactionOperationState.SUBMISSION_CLAIMED
        assertEquals(MobileIssuerStatus.FAILED, f.runner.submit(operation.operationId, request, {}, permit).status)
        assertEquals(0, f.sends)
        assertEquals(TransactionOperationState.READY_TO_SUBMIT, f.restart().engine.find(operation.operationId)?.state)
        coordinator.release(permit.lease)
        Unit
    }

    @Test fun `process death after durable claim but before marker recovers proven no broadcast`() = runBlocking {
        val f = Fixture(StudioWalletBinding(walletA, "provider-A", 1))
        val request = request(StudioActionType.ISSUE_REGISTER, "pass-a")
        val operation = f.prepare(request)
        f.engine.claimProviderSubmission(operation.operationId, "1", "1")
        val restarted = f.restart()
        assertEquals("NO_BROADCAST_PROVEN", restarted.runner.recover(operation.operationId, request).category)
        assertEquals(0, f.sends)
        assertEquals(0, restarted.sends)
        assertEquals(TransactionOperationState.DRAFT, restarted.runner.create(request).state)
    }

    @Test fun `process death after invocation marker before hash stays unknown and cannot rearm`() = runBlocking {
        val f = Fixture(StudioWalletBinding(walletA, "provider-A", 1))
        val request = request(StudioActionType.ISSUE_REGISTER, "pass-a")
        val operation = f.prepare(request)
        f.engine.claimProviderSubmission(operation.operationId, "1", "1")
        f.engine.beginProviderInvocation(operation.operationId)
        val restarted = f.restart()
        repeat(3) { assertEquals(MobileIssuerStatus.UNKNOWN, restarted.runner.recover(operation.operationId, request).status) }
        assertTrue(runCatching { restarted.runner.create(request) }.isFailure)
        assertTrue(runCatching { restarted.engine.cancelUninvokedClaim(operation.operationId) }.isFailure)
        assertTrue(runCatching { restarted.engine.proveNoBroadcast(operation.operationId, "1", "1") }.isFailure)
        assertEquals(0, restarted.sends)
    }

    @Test fun `failed provider marker persistence never sends and safely resolves the claim`() = runBlocking {
        val coordinator = StudioWriteCoordinator()
        val f = Fixture(coordinator.select(walletA, "provider-A"))
        val request = request(StudioActionType.ISSUE_REGISTER, "pass-a")
        val operation = f.prepare(request)
        val permit = permit(coordinator, operation, request)
        f.store.failState = TransactionOperationState.SUBMITTING_NO_HASH
        assertEquals(MobileIssuerStatus.FAILED, f.runner.submit(operation.operationId, request, {}, permit).status)
        assertEquals(0, f.sends)
        assertEquals(TransactionOperationState.NO_BROADCAST_PROVEN, f.restart().engine.find(operation.operationId)?.state)
        coordinator.release(permit.lease)
        Unit
    }

    @Test fun `failed safe-cancellation persistence retains durable claim for restart recovery`() = runBlocking {
        val coordinator = StudioWriteCoordinator()
        val f = Fixture(coordinator.select(walletA, "provider-A"))
        val request = request(StudioActionType.ISSUE_REGISTER, "pass-a")
        val operation = f.prepare(request)
        val permit = permit(coordinator, operation, request)
        f.store.failState = TransactionOperationState.NO_BROADCAST_PROVEN
        assertTrue(runCatching { f.runner.submit(operation.operationId, request, {}, permit) {
            if (it.state == TransactionOperationState.SUBMISSION_CLAIMED) error("PRODUCT_PERSISTENCE_FAILED")
        } }.isFailure)
        coordinator.release(permit.lease)
        assertEquals(0, f.sends)
        assertEquals(TransactionOperationState.SUBMISSION_CLAIMED, f.restart().engine.find(operation.operationId)?.state)
        f.store.failState = null
        assertEquals("NO_BROADCAST_PROVEN", f.restart().runner.recover(operation.operationId, request).category)
    }

    @Test fun `cancellation thrown before provider marker retains safe durable outcome`() = runBlocking {
        val coordinator = StudioWriteCoordinator()
        val f = Fixture(coordinator.select(walletA, "provider-A"))
        val request = request(StudioActionType.ISSUE_REGISTER, "pass-a")
        val operation = f.prepare(request)
        val permit = permit(coordinator, operation, request)
        assertTrue(runCatching { f.runner.submit(operation.operationId, request, {}, permit) {
            if (it.state == TransactionOperationState.SUBMISSION_CLAIMED) throw CancellationException("CANCELLED")
        } }.exceptionOrNull() is CancellationException)
        coordinator.release(permit.lease)
        assertEquals(0, f.sends)
        assertEquals(TransactionOperationState.NO_BROADCAST_PROVEN, f.restart().engine.find(operation.operationId)?.state)
    }

    private fun request(action: StudioActionType, label: String, wallet: String = walletA) = ContractTransactionRequest(
        StudioOperationIdentity.type(action, "$label.${IssuerSpace.namespace}"), wallet,
        if (action == StudioActionType.ISSUE_REGISTER) IssuerSpace.registry else IssuerSpace.resolver, "0x1234")

    private fun permit(coordinator: StudioWriteCoordinator, op: PersistedTransactionOperation, request: ContractTransactionRequest): StudioWritePermit {
        val binding = checkNotNull(coordinator.currentWallet())
        return StudioWritePermit(coordinator, coordinator.acquire(binding), StudioWriteIntent(op.operationId,
            "session:${request.operationType}", binding, IssuerSpace.chainId, request.operationType.substringAfter(':'),
            request.operationType, request.to, "0", CredentialAbi.calldataFingerprint(request.data), request))
    }

    private class ProductStore : LoadableStringStateStore {
        var value: String? = null
        override fun load() = value
        override fun save(value: String) { this.value = value }
    }
    private class JournalStore : TransactionJournalStore {
        var value: String? = null
        var failState: TransactionOperationState? = null
        override fun load() = value
        override fun save(serializedJournal: String) {
            check(failState == null || !serializedJournal.contains("|${failState!!.name}|")) { "PERSISTENCE_FAILED" }
            value = serializedJournal
        }
    }

    private class Fixture(val binding: StudioWalletBinding, val store: JournalStore = JournalStore(), val chainId: Long = IssuerSpace.chainId) {
        val engine = RecoverableTransactionEngine(TransactionJournal(store))
        var sends = 0
        private var sent: Map<String, String>? = null
        private val hash = "0x" + "1".repeat(64)
        private val blockHash = "0x" + "2".repeat(64)
        private val provider = object : MobileIssuerWalletProvider {
            override suspend fun switchToSepolia() = Unit
            override suspend fun sendTransaction(transactionJson: String): String {
                sends++
                // The provider remains bound to this fixture's wallet, independently of request.from.
                sent = Regex("\"([a-zA-Z]+)\"\\s*:\\s*\"([^\"]*)\"").findAll(transactionJson)
                    .associate { it.groupValues[1] to it.groupValues[2] }
                return hash
            }
        }
        private val client = ReadOnlyEthereumRpcClient(transport = ReadOnlyRpcTransport { body, _ ->
            val id = Regex("\"id\":(\\d+)").find(body)!!.groupValues[1]
            fun field(name: String) = sent!!.getValue(name)
            val result = when {
                body.contains("eth_chainId") -> "\"0xaa36a7\""
                body.contains("eth_getTransactionCount") -> if (sent == null) "\"0x1\"" else "\"0x2\""
                body.contains("eth_getBlockByNumber") -> "{\"number\":\"0x100\",\"transactions\":[]}"
                body.contains("eth_getTransactionByHash") -> if (sent == null) "null" else
                    "{\"hash\":\"$hash\",\"from\":\"${binding.address}\",\"to\":\"${field("to")}\",\"value\":\"0x0\",\"input\":\"${field("data")}\",\"nonce\":\"0x1\",\"chainId\":\"0xaa36a7\",\"blockHash\":\"$blockHash\",\"blockNumber\":\"0x100\",\"transactionIndex\":\"0x0\"}"
                body.contains("eth_getTransactionReceipt") -> if (sent == null) "null" else
                    "{\"transactionHash\":\"$hash\",\"from\":\"${binding.address}\",\"to\":\"${field("to")}\",\"status\":\"0x1\",\"blockHash\":\"$blockHash\",\"blockNumber\":\"0x100\",\"transactionIndex\":\"0x0\"}"
                else -> error("UNEXPECTED_RPC")
            }
            "{\"jsonrpc\":\"2.0\",\"id\":$id,\"result\":$result}"
        })
        val runner = ContractTransactionRunner(StudioRunnerIdentity(binding, chainId), provider, client, engine, wait = {}, attempts = 1)
        fun prepare(request: ContractTransactionRequest) = runner.create(request).let { runner.review(it.operationId) }
        fun restart() = Fixture(binding, store, chainId)
    }
}
