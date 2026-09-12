package io.github.nodexbit.ethonline2026

import java.math.BigInteger
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.yield
import org.junit.Assert.*
import org.junit.Test

internal fun safetyPermit(operation: PersistedTransactionOperation, request: ContractTransactionRequest): StudioWritePermit {
    val coordinator = StudioWriteCoordinator()
    val binding = coordinator.select(request.from, "test-provider")
    return StudioWritePermit(coordinator, coordinator.acquire(binding), StudioWriteIntent(
        operation.operationId, "test-session", binding, IssuerSpace.chainId, IssuerSpace.fullName,
        request.operationType, request.to, "0", CredentialAbi.calldataFingerprint(request.data), request))
}

class StudioSafetyTest {
    private val now = BigInteger("1789180000")
    private val expiry = now + BigInteger.valueOf(3600)
    private val namespaceExpiry = expiry + BigInteger.valueOf(86400)
    private val hash = "0x" + "1".repeat(64)
    private val blockHash = "0x" + "2".repeat(64)
    private val other = "0x" + "3".repeat(40)

    @Test fun `overlapping failure cannot unlock owner or switch wallet`() {
        val coordinator = StudioWriteCoordinator()
        val binding = coordinator.select(IssuerSpace.issuer, "provider-A")
        val lease = coordinator.acquire(binding)
        assertTrue(runCatching { coordinator.acquire(binding) }.isFailure)
        assertFalse(coordinator.release(lease.copy(id = "failed-B")))
        assertTrue(coordinator.isBusy())
        assertTrue(runCatching { coordinator.select(other, "provider-B") }.isFailure)
        assertTrue(runCatching { coordinator.clearWallet() }.isFailure)
        assertEquals(binding, coordinator.select(binding.address, binding.providerId))
        assertTrue(coordinator.release(lease))
        val next = coordinator.acquire(binding)
        assertFalse(coordinator.release(lease))
        assertTrue(coordinator.isBusy())
        assertTrue(coordinator.release(next))
        assertEquals(other, coordinator.select(other, "provider-B").address)
    }

    @Test fun `process observer sees release after activity recreation and cannot poison lock`() {
        val coordinator = StudioWriteCoordinator()
        val binding = coordinator.select(IssuerSpace.issuer, "provider-A")
        var busy = false
        val stop = coordinator.observe { busy = coordinator.isBusy() }
        val first = coordinator.acquire(binding)
        assertTrue(busy)
        stop()
        coordinator.observe { busy = coordinator.isBusy() }
        coordinator.release(first)
        assertFalse(busy)
    }

    @Test fun `wallet changed during preflight prevents provider invocation`() = runBlocking {
        val f = Fixture()
        val permit = safetyPermit(f.operation, f.request)
        val result = f.runner.submit(f.operation.operationId, f.request, {
            permit.coordinator.release(permit.lease)
            permit.coordinator.select(other, "provider-B")
        }, permit)
        assertEquals(MobileIssuerStatus.FAILED, result.status)
        assertEquals(0, f.sends)
        assertEquals(TransactionOperationState.READY_TO_SUBMIT, f.engine.find(f.operation.operationId)?.state)
    }

    @Test fun `write permit binds exact operation chain target calldata and wallet`() {
        val f = Fixture()
        val permit = safetyPermit(f.operation, f.request)
        listOf(f.request.copy(from = other), f.request.copy(to = other),
            f.request.copy(data = "0xabcd"), f.request.copy(operationType = "other-action")).forEach {
            assertTrue(runCatching { permit.requireRequest(f.operation.operationId, it) }.isFailure)
        }
        assertTrue(runCatching { permit.copy(intent = permit.intent.copy(chainId = 1)).requireRequest(f.operation.operationId, f.request) }.isFailure)
        assertTrue(runCatching { permit.requireRequest("other-op", f.request) }.isFailure)
        assertTrue(runCatching { permit.copy(intent = permit.intent.copy(credential = "wrong.eth")).requireRequest(f.operation.operationId, f.request) }.isFailure)
    }

    @Test fun `delayed provider concurrent recovery never rearms and retains exactly one late hash`() = runBlocking {
        val f = Fixture(delayed = true)
        val sending = async { f.runner.submit(f.operation.operationId, f.request, {}, safetyPermit(f.operation, f.request)) }
        f.entered.await()
        repeat(3) {
            assertEquals("PROVIDER_REQUEST_OUTSTANDING", f.runner.recover(f.operation.operationId, f.request).category)
            assertTrue(runCatching { f.runner.create(f.request) }.isFailure)
            assertTrue(f.engine.providerInvocationLive(f.operation.operationId))
            assertTrue(runCatching { f.engine.proveNoBroadcast(f.operation.operationId, "1", "1") }.isFailure)
        }
        f.release.complete(Unit)
        assertEquals(MobileIssuerStatus.CONFIRMED, sending.await().status)
        assertEquals(hash, f.engine.find(f.operation.operationId)?.txHash)
        assertEquals(1, f.sends)
        assertEquals(1, f.engine.all().size)
        assertFalse(f.engine.providerInvocationLive(f.operation.operationId))
    }

    @Test fun `activity cancellation retains late hash in process shared journal`() = runBlocking {
        val f = Fixture(delayed = true)
        val sending = launch { f.runner.submit(f.operation.operationId, f.request, {}, safetyPermit(f.operation, f.request)) }
        f.entered.await()
        sending.cancel()
        yield()
        assertTrue(f.engine.providerInvocationLive(f.operation.operationId))
        f.release.complete(Unit)
        sending.join()
        assertEquals(hash, RecoverableTransactionEngine(TransactionJournal(f.store)).find(f.operation.operationId)?.txHash)
        assertFalse(f.engine.providerInvocationLive(f.operation.operationId))
        assertEquals(1, f.sends)
    }

    @Test fun `restart no hash unchanged nonces is UNKNOWN and bounded search cannot permit replacement`() = runBlocking {
        val f = Fixture()
        f.engine.beginSubmission(f.operation.operationId, "1", "1")
        val restarted = RecoverableTransactionEngine(TransactionJournal(f.store))
        val runner = f.runnerFor(restarted)
        repeat(2) { assertEquals(MobileIssuerStatus.UNKNOWN, runner.recover(f.operation.operationId, f.request).status) }
        assertEquals(TransactionOperationState.UNKNOWN, restarted.find(f.operation.operationId)?.state)
        assertTrue(runCatching { runner.create(f.request) }.isFailure)
        assertTrue(runCatching { restarted.proveNoBroadcast(f.operation.operationId, "1", "1") }.isFailure)
        assertEquals(0, f.sends)
        assertEquals(24, f.blocksRead)
    }

    @Test fun `bounded no hash search recovers only matching original transaction`() = runBlocking {
        val f = Fixture()
        f.engine.beginSubmission(f.operation.operationId, "1", "1")
        f.returned = true
        f.includeTransaction = true
        assertEquals(MobileIssuerStatus.CONFIRMED, f.runner.recover(f.operation.operationId, f.request).status)
        assertEquals(hash, f.engine.find(f.operation.operationId)?.txHash)
        assertEquals(0, f.sends)
    }

    @Test fun `confirmed nonce N recovers after N plus one and N plus two and restart`() = runBlocking {
        val f = Fixture()
        f.engine.beginSubmission(f.operation.operationId, "1", "1")
        f.engine.recordHash(f.operation.operationId, hash)
        f.returned = true
        val restarted = RecoverableTransactionEngine(TransactionJournal(f.store))
        assertEquals(MobileIssuerStatus.CONFIRMED, f.runnerFor(restarted).recover(f.operation.operationId, f.request).status)
        assertEquals("4", restarted.find(f.operation.operationId)?.postLatestNonce)
        assertEquals(0, f.sends)
    }

    @Test fun `unrelated receipt hash never confirms`() = runBlocking {
        val f = Fixture(); f.receiptHash = "0x" + "f".repeat(64)
        assertEquals(MobileIssuerStatus.UNKNOWN, f.submit().status)
        assertEquals(TransactionOperationState.UNKNOWN, f.engine.find(f.operation.operationId)?.state)
    }

    @Test fun `confirmed historical receipt is revalidated without rewriting journal`() = runBlocking {
        val f = Fixture()
        assertEquals(MobileIssuerStatus.CONFIRMED, f.submit().status)
        val before = f.store.value
        val restarted = f.runnerFor(RecoverableTransactionEngine(TransactionJournal(f.store)))
        assertEquals(MobileIssuerStatus.CONFIRMED, restarted.recover(f.operation.operationId, f.request).status)
        f.receiptHash = "0x" + "f".repeat(64)
        assertEquals(MobileIssuerStatus.UNKNOWN, restarted.recover(f.operation.operationId, f.request).status)
        assertEquals(before, f.store.value)
        assertEquals(1, f.sends)
    }

    @Test fun `failed product callback after hash cannot lose durable journal hash`() = runBlocking {
        val f = Fixture()
        val result = f.runner.submit(f.operation.operationId, f.request, {}, safetyPermit(f.operation, f.request)) {
            if (it.state == TransactionOperationState.HASH_RECEIVED) error("PRODUCT_STORE_UNAVAILABLE")
        }
        assertEquals(MobileIssuerStatus.UNKNOWN, result.status)
        val restarted = RecoverableTransactionEngine(TransactionJournal(f.store))
        assertEquals(hash, restarted.find(f.operation.operationId)?.txHash)
        assertEquals(MobileIssuerStatus.CONFIRMED, f.runnerFor(restarted).recover(f.operation.operationId, f.request).status)
        assertEquals(1, f.sends)
    }

    @Test fun `receipt block hash number and transaction index mismatch never confirm`() = runBlocking {
        for (field in listOf("blockHash", "blockNumber", "transactionIndex")) {
            val f = Fixture(); f.badReceiptField = field
            assertEquals(field, MobileIssuerStatus.UNKNOWN, f.submit().status)
        }
    }

    @Test fun `transaction wrong nonce chain from to value input and hash never confirm`() = runBlocking {
        for (field in listOf("nonce", "chainId", "from", "to", "value", "input", "hash")) {
            val f = Fixture(); f.badTransactionField = field
            assertEquals(field, MobileIssuerStatus.UNKNOWN, f.submit().status)
        }
    }

    @Test fun `receipt invalid status stays unknown but zero is terminal reverted`() = runBlocking {
        val bad = Fixture(); bad.receiptStatus = "0x2"
        assertEquals(MobileIssuerStatus.UNKNOWN, bad.submit().status)
        val reverted = Fixture(); reverted.receiptStatus = "0x0"
        assertEquals(MobileIssuerStatus.FAILED, reverted.submit().status)
        assertEquals(TransactionOperationState.REVERTED, reverted.engine.find(reverted.operation.operationId)?.state)
        repeat(2) { assertEquals("REVERTED_RECEIPT", reverted.runner.recover(reverted.operation.operationId, reverted.request).category) }
    }

    @Test fun `arbitrary provider secret never persists in category class or message`() = runBlocking {
        val secret = "fake-secret https://user:password@example.test token=FAKE email=person@example.test OTP=123456 signed=0xabcdef"
        val f = Fixture(); f.providerError = secret
        val result = f.submit()
        assertEquals("PROVIDER_OR_OPERATION_UNAVAILABLE", result.category)
        assertFalse(f.store.value.orEmpty().contains("fake-secret"))
        assertFalse(f.store.value.orEmpty().contains("password"))
        assertFalse(f.store.value.orEmpty().contains("example.test"))
        val failure = SafeActionFailurePolicy.from("Create credential", "TX1_SUBMISSION", RuntimeException(secret))
        assertFalse(failure.diagnosticText().contains(secret))
    }

    @Test fun `A callback cannot mutate B across selection wallet change and restart`() {
        val store = MemoryStore()
        val coordinator = IssuanceCoordinator(store)
        val a = coordinator.beginRegister(IssuerSpace.issuer, draft("visitor-a"), "op-a")
        val b = coordinator.beginRegister(IssuerSpace.issuer, draft("visitor-b"), "op-b")
        coordinator.current(IssuerSpace.issuer, b.fullName)
        coordinator.current(other)
        coordinator.reconcile(a.identity, operation(a, false, TransactionOperationState.CONFIRMED))
        assertEquals(IssuanceState.REGISTER_READY, coordinator.get(b.identity).state)
        assertNull(coordinator.current(IssuerSpace.issuer))
        val restarted = IssuanceCoordinator(store)
        assertEquals(2, restarted.list(IssuerSpace.issuer).size)
        assertEquals(IssuanceState.REGISTERED_CONFIGURING, restarted.get(a.identity).state)
        assertEquals(b, restarted.get(b.identity))
        assertTrue(runCatching { restarted.get(a.identity.copy(wallet = other)) }.isFailure)
        assertTrue(runCatching { restarted.get(a.identity.copy(sessionId = "stale")) }.isFailure)
    }

    @Test fun `journal session crash matrix is idempotent for every state and both transactions`() {
        for (records in listOf(false, true)) for (state in TransactionOperationState.entries) {
            val store = MemoryStore()
            var coordinator = IssuanceCoordinator(store)
            var session = coordinator.beginRegister(IssuerSpace.issuer, draft(), "register")
            if (records) {
                session = coordinator.reconcile(session.identity, operation(session, false, TransactionOperationState.CONFIRMED))
                session = coordinator.recordsReady(session.identity, "records")
            }
            // Journal advanced; product persisted only READY. Restart before reconciliation.
            val journalStore = JournalStore()
            TransactionJournal(journalStore).put(operation(session, records, state))
            coordinator = IssuanceCoordinator(store)
            val authoritative = TransactionJournal(journalStore).all().single()
            val result = coordinator.reconcile(session.identity, authoritative)
            val expected = when (state) {
                TransactionOperationState.CONFIRMED -> if (records) IssuanceState.AUTHORITATIVE_READBACK else IssuanceState.REGISTERED_CONFIGURING
                TransactionOperationState.REVERTED, TransactionOperationState.NO_BROADCAST_PROVEN,
                TransactionOperationState.CANCELLED -> if (records) IssuanceState.REGISTERED_CONFIGURING else IssuanceState.DRAFT
                TransactionOperationState.DRAFT, TransactionOperationState.READY_TO_REVIEW,
                TransactionOperationState.READY_TO_SUBMIT, TransactionOperationState.SUBMISSION_CLAIMED ->
                    if (records) IssuanceState.RECORDS_READY else IssuanceState.REGISTER_READY
                else -> if (records) IssuanceState.RECORDS_SUBMITTED else IssuanceState.REGISTER_SUBMITTED
            }
            assertEquals("records=$records state=$state", expected, result.state)
            assertEquals(result, coordinator.reconcile(session.identity, authoritative))
            assertEquals(result, IssuanceCoordinator(store).get(session.identity))
        }
    }

    @Test fun `terminal no broadcast recovery never tries to prove itself again`() = runBlocking {
        val f = Fixture()
        val terminal = f.operation.copy(state = TransactionOperationState.NO_BROADCAST_PROVEN)
        val store = JournalStore(); TransactionJournal(store).put(terminal)
        val runner = f.runnerFor(RecoverableTransactionEngine(TransactionJournal(store)))
        repeat(3) { assertEquals("NO_BROADCAST_PROVEN", runner.recover(terminal.operationId, f.request).category) }
        assertEquals(0, f.blocksRead)
        assertEquals(0, f.sends)
    }

    @Test fun `confirmed TX1 callback never rolls back confirmed TX2 and ready never reconfigures`() {
        val coordinator = IssuanceCoordinator(MemoryStore())
        var session = coordinator.beginRegister(IssuerSpace.issuer, draft(), "register")
        val register = operation(session, false, TransactionOperationState.CONFIRMED)
        coordinator.reconcile(session.identity, register)
        session = coordinator.recordsReady(session.identity, "records")
        val records = operation(session, true, TransactionOperationState.CONFIRMED)
        coordinator.reconcile(session.identity, records)
        val ready = coordinator.ready(session.identity)
        assertEquals(ready, coordinator.reconcile(session.identity, register))
        assertEquals(ready, coordinator.reconcile(session.identity, records))
        assertEquals(IssuanceRecoveryAction.DISPLAY_READY, coordinator.recoveryAction(ready, register, records))
    }

    @Test fun `management confirmed failed readback blocks B survives restart and clears exact A only`() = runBlocking {
        val store = MemoryStore()
        var coordinator = ManagementSessionCoordinator(store)
        val a = ManagementSession(IssuerSpace.issuer, IssuerSpace.fullName, "op-a",
            PassManagementPolicy.presentation(snapshot(), "Updated", null))
        coordinator.save(a)
        val operation = PersistedTransactionOperation(a.operationId,
            StudioOperationIdentity.type(a.mutation.action, a.fullName), a.wallet, IssuerSpace.chainId,
            a.mutation.target, "0", CredentialAbi.calldataFingerprint(a.mutation.calldata),
            state = TransactionOperationState.CONFIRMED, receiptBlock = "10", createdAt = 1, updatedAt = 2)
        val unavailable = ManagementFinalReadbackReconciler(read = { snapshot().copy(readStatus = CredentialReadStatus.UNKNOWN) },
            wait = {}, attempts = 1)
        assertTrue(runCatching { unavailable.finalize(a, operation, coordinator) }.isFailure)
        val b = a.copy(operationId = "op-b", fullName = "visitor-b.${IssuerSpace.namespace}")
        assertTrue(runCatching { coordinator.save(b) }.isFailure)
        assertTrue(runCatching { coordinator.requireNoPending() }.isFailure)
        coordinator = ManagementSessionCoordinator(store)
        assertEquals(listOf(a), coordinator.pending())
        coordinator.clear("op-b")
        assertEquals(listOf(a), coordinator.pending())
        ManagementFinalReadbackReconciler(read = { snapshot().copy(description = "Updated") },
            wait = {}, attempts = 1).finalize(a, operation, coordinator)
        coordinator.requireNoPending()
        coordinator.save(b)
        coordinator.clear("op-a")
        assertEquals(listOf(b), ManagementSessionCoordinator(store).pending())
    }

    @Test fun `historical 9 and 14 field staff sessions migrate without identity or record loss`() {
        val legacy = listOf(IssuerSpace.issuer, IssuerSpace.fullName, IssuerSpace.STAFF_HOLDER,
            IssuerSpace.STAFF_EXPIRY.toString(), "", IssuerSpace.DEFAULT_DESCRIPTION,
            "REGISTER_READY", "historical-register", "")
        for (fields in listOf(legacy, legacy + listOf("STAFF", "true", IssuerSpace.STAFF_EXPIRY.toString(), "false", "0"))) {
            val store = MemoryStore().apply { value = fields.joinToString("|") }
            val first = IssuanceCoordinator(store)
            val session = first.current(IssuerSpace.issuer)!!
            assertEquals(session, IssuanceCoordinator(store).get(session.identity))
            first.registerSubmitted(session.identity)
            val migrated = IssuanceCoordinator(store).get(session.identity)
            assertEquals(session.identity, migrated.identity)
            assertEquals(session.holder, migrated.holder)
            assertEquals(session.expiry, migrated.expiry)
            assertEquals("historical-register", migrated.registerOperationId)
            assertEquals(18, store.value!!.split('|').size)
        }
    }

    @Test fun `TX2 acknowledgement revised validity and operation linkage persist atomically`() {
        val store = MemoryStore()
        val coordinator = IssuanceCoordinator(store)
        var initial = coordinator.beginRegister(IssuerSpace.issuer, draft(), "register")
        initial = coordinator.reconcile(initial.identity, operation(initial, false, TransactionOperationState.CONFIRMED))
        val reviewed = initial.copy(acknowledgedOwner = other, acknowledgedRoles = BigInteger.ZERO,
            acknowledgedTransferable = false, accessValidUntil = expiry)
        store.fail = true
        assertTrue(runCatching { coordinator.recordsReady(initial.identity, "records-new", reviewed) }.isFailure)
        assertEquals(initial, coordinator.get(initial.identity))
        assertEquals(initial, IssuanceCoordinator(store).get(initial.identity))
        store.fail = false
        coordinator.recordsReady(initial.identity, "records-new", reviewed)
        val restarted = IssuanceCoordinator(store)
        val restored = restarted.get(initial.identity)
        assertEquals(other, restored.configurationOwner)
        assertEquals(expiry, restored.accessValidUntil)
        assertEquals("records-new", restored.recordsOperationId)
        assertEquals(IssuanceState.RECORDS_SUBMITTED,
            restarted.reconcile(restored.identity, operation(restored, true, TransactionOperationState.SUBMITTING_NO_HASH)).state)
    }

    @Test fun `restore reviewed before expiry cannot submit after fresh chain expiry`() {
        val before = snapshot().copy(accessActive = false, accessValidUntil = now + BigInteger.TEN)
        val mutation = PassManagementPolicy.access(before, true, before.accessValidUntil!!, now)
        StudioManagementPreflight.validate(before, mutation, before, namespaceExpiry)
        assertTrue(runCatching { StudioManagementPreflight.validate(before, mutation,
            before.copy(snapshotTimestamp = now + BigInteger.TEN), namespaceExpiry) }.isFailure)
    }

    @Test fun `fresh registration expiry constrains access update and all management lifecycle`() {
        val before = snapshot()
        val mutation = PassManagementPolicy.access(before, true, expiry, now)
        assertTrue(runCatching { StudioManagementPreflight.validate(before, mutation,
            before.copy(registryExpiry = expiry - BigInteger.ONE), namespaceExpiry) }.isFailure)
        val presentation = PassManagementPolicy.presentation(before, "Updated", null)
        assertTrue(runCatching { StudioManagementPreflight.validate(before, presentation,
            before.copy(snapshotTimestamp = expiry), namespaceExpiry) }.isFailure)
    }

    @Test fun `active flag past validity or registration expiry is never Allowed`() {
        val pass = snapshot().copy(accessActive = true, accessValidUntil = now)
        assertEquals("Access expired", StudioAccessPolicy.status(pass))
        assertFalse(pass.authoritativeAllowed)
        assertEquals("Registration expired", StudioAccessPolicy.status(pass.copy(registryExpiry = now)))
        assertEquals("Suspended", StudioAccessPolicy.status(snapshot().copy(accessActive = false)))
        assertTrue(snapshot().authoritativeAllowed)
    }

    @Test fun `TX2 final confirmation rechecks time and allows explicit validity review`() {
        val session = session().copy(accessValidUntil = now + BigInteger.TEN)
        val reviewed = StudioConfigurationPolicy.review(session, snapshot())
        val late = snapshot().copy(snapshotTimestamp = now + BigInteger.TEN)
        assertTrue(runCatching { StudioConfigurationPolicy.requireAcknowledged(reviewed, late) }.isFailure)
        val updated = StudioConfigurationPolicy.review(reviewed, late, expiry)
        StudioConfigurationPolicy.requireAcknowledged(updated, late)
        assertEquals(expiry, updated.accessValidUntil)
    }

    @Test fun `VISITOR transfer after TX1 requires explicit authoritative owner review`() {
        val session = session()
        val transferred = snapshot().copy(owner = other)
        assertTrue(runCatching { StudioConfigurationPolicy.requireAcknowledged(session, transferred) }.isFailure)
        assertTrue(SafeActionFailurePolicy.from("Configure credential", "TX2_SUBMISSION",
            IllegalArgumentException("CREDENTIAL_CHANGED_AFTER_REGISTRATION")).humanMessage
            .startsWith("Credential changed after registration"))
        val reviewed = StudioConfigurationPolicy.review(session, transferred)
        StudioConfigurationPolicy.requireAcknowledged(reviewed, transferred)
        val configuration = CredentialConfigurationPolicy.prepare(reviewed)
        assertEquals(other, configuration.review.presentation.exactRecipient)
        assertEquals(CredentialConfigurationPolicy.calldata(session), configuration.calldata)
        assertEquals(session.holder, reviewed.holder)
    }

    @Test fun `VISITOR self revoke after TX1 shows actual non transferability and configures only records`() {
        val session = session()
        val revoked = snapshot().copy(ownerRoleBitmap = BigInteger.ZERO, transferable = false)
        assertTrue(runCatching { StudioConfigurationPolicy.requireAcknowledged(session, revoked) }.isFailure)
        val reviewed = StudioConfigurationPolicy.review(session, revoked)
        StudioConfigurationPolicy.requireAcknowledged(reviewed, revoked)
        assertFalse(reviewed.configurationTransferable)
        assertEquals(BigInteger.ZERO, reviewed.configurationRoles)
        assertEquals(CredentialConfigurationPolicy.calldata(session), CredentialConfigurationPolicy.prepare(reviewed).calldata)
    }

    @Test fun `lowercase uppercase valid checksum accepted invalid mixed checksum rejected`() {
        val address = IssuerSpace.STAFF_HOLDER
        assertEquals(address, CredentialValidation.requireNonZeroAddress(address.lowercase()))
        assertEquals(address, CredentialValidation.requireNonZeroAddress("0x" + address.drop(2).uppercase()))
        assertEquals(address, CredentialValidation.requireNonZeroAddress(address))
        val invalid = address.replaceFirst("D", "d")
        assertNotEquals(address, invalid)
        assertTrue(runCatching { CredentialValidation.requireNonZeroAddress(invalid) }.isFailure)
    }

    @Test fun `strict calendar rejects Feb 30 and Madrid DST gap and overlap`() {
        assertEquals("INVALID_CALENDAR_DATE", runCatching { StudioTime.parse("2026-02-30 12:00") }.exceptionOrNull()?.message)
        assertEquals("DST_GAP_CHOOSE_ANOTHER_TIME", runCatching { StudioTime.parse("2026-03-29 02:30") }.exceptionOrNull()?.message)
        assertEquals("DST_OVERLAP_CHOOSE_ANOTHER_TIME", runCatching { StudioTime.parse("2026-10-25 02:30") }.exceptionOrNull()?.message)
        val normal = StudioTime.parse("2026-09-12 12:30")
        assertEquals("2026-09-12 12:30", StudioTime.input(normal))
        assertTrue(StudioTime.describe(normal).contains("Europe/Madrid"))
        assertTrue(StudioTime.describe(normal).contains("2026-09-12T10:30:00Z"))
    }

    private fun draft(label: String = "visitor-safety") = PassDraft(PassTemplate.VISITOR, label,
        IssuerSpace.STAFF_HOLDER, expiry, true, expiry - BigInteger.valueOf(60), true, "Visitor Access Pass", "")
    private fun session() = IssuanceSession(IssuerSpace.issuer, IssuerSpace.fullName, IssuerSpace.STAFF_HOLDER,
        expiry, "", "Visitor Access Pass", IssuanceState.REGISTERED_CONFIGURING,
        registerOperationId = "register", template = PassTemplate.VISITOR, transferable = true)
    private fun snapshot() = CredentialSnapshot(IssuerSpace.fullName, CredentialAbi.nodeHex(IssuerSpace.fullName),
        IssuerSpace.registry, IssuerSpace.resolver, IssuerSpace.ZERO_ADDRESS, BigInteger.ONE,
        CredentialRegistryStatus.REGISTERED, IssuerSpace.STAFF_HOLDER, expiry, StudioRoles.CAN_TRANSFER_ADMIN,
        true, true, expiry - BigInteger.valueOf(60), "", "Visitor Access Pass", BigInteger.TEN, now,
        CredentialReadStatus.FRESH, true)
    private fun operation(session: IssuanceSession, records: Boolean, state: TransactionOperationState) =
        PersistedTransactionOperation(if (records) session.recordsOperationId!! else session.registerOperationId!!,
            StudioOperationIdentity.type(if (records) StudioActionType.ISSUE_CONFIGURE else StudioActionType.ISSUE_REGISTER, session.fullName),
            session.wallet, IssuerSpace.chainId, if (records) IssuerSpace.resolver else IssuerSpace.registry,
            "0", CredentialAbi.calldataFingerprint(if (records) CredentialConfigurationPolicy.calldata(session) else
                CredentialAbi.register(session.fullName.removeSuffix(".${IssuerSpace.namespace}"), session.holder,
                    IssuerSpace.resolver, session.expiry, session.roleBitmap)), state = state, createdAt = 1, updatedAt = 2)
    private class MemoryStore : LoadableStringStateStore {
        var value: String? = null
        var fail = false
        override fun load() = value
        override fun save(value: String) { check(!fail); this.value = value }
    }
    private class JournalStore : TransactionJournalStore {
        var value: String? = null
        override fun load() = value
        override fun save(serializedJournal: String) { value = serializedJournal }
    }

    private inner class Fixture(val delayed: Boolean = false) {
        val store = JournalStore()
        val engine = RecoverableTransactionEngine(TransactionJournal(store))
        var sends = 0
        var returned = false
        var blocksRead = 0
        var includeTransaction = false
        var providerError: String? = null
        var receiptHash = hash
        var receiptStatus = "0x1"
        var badReceiptField: String? = null
        var badTransactionField: String? = null
        val entered = CompletableDeferred<Unit>()
        val release = CompletableDeferred<Unit>()
        val request = ContractTransactionRequest(ContractTransactionRunner.registerOperation(IssuerSpace.fullName),
            IssuerSpace.issuer, IssuerSpace.registry, "0x1234")
        val provider = object : MobileIssuerWalletProvider {
            override suspend fun switchToSepolia() = Unit
            override suspend fun sendTransaction(transactionJson: String): String {
                sends++
                entered.complete(Unit)
                if (delayed) release.await()
                providerError?.let { throw RuntimeException(it) }
                returned = true
                return hash
            }
        }
        val client = ReadOnlyEthereumRpcClient(transport = ReadOnlyRpcTransport { body, _ ->
            val id = Regex("\"id\":(\\d+)").find(body)!!.groupValues[1]
            val result = when {
                body.contains("eth_chainId") -> "\"0xaa36a7\""
                body.contains("eth_getTransactionCount") -> if (returned) "\"0x4\"" else "\"0x1\""
                body.contains("eth_getTransactionByHash") -> if (returned) tx() else "null"
                body.contains("eth_getTransactionReceipt") -> if (returned) receipt() else "null"
                body.contains("eth_getBlockByNumber") -> {
                    blocksRead++
                    "{\"number\":\"0x100\",\"transactions\":[${if (includeTransaction) tx() else ""}]}"
                }
                else -> error("UNEXPECTED_READ_ONLY_RPC")
            }
            "{\"jsonrpc\":\"2.0\",\"id\":$id,\"result\":$result}"
        })
        val runner = runnerFor(engine)
        val operation = runner.create(request).let { runner.review(it.operationId) }
        fun runnerFor(value: RecoverableTransactionEngine) = ContractTransactionRunner(
            StudioRunnerIdentity(StudioWalletBinding(IssuerSpace.issuer, "test-provider", 1), IssuerSpace.chainId),
            provider, client, value, wait = {}, attempts = 1)
        suspend fun submit() = runner.submit(operation.operationId, request, {}, safetyPermit(operation, request))
        fun tx(): String = json(linkedMapOf("hash" to hash, "from" to request.from, "to" to request.to,
            "value" to "0x0", "input" to request.data, "nonce" to "0x1", "chainId" to "0xaa36a7",
            "blockHash" to blockHash, "blockNumber" to "0x100", "transactionIndex" to "0x0"), badTransactionField)
        fun receipt(): String = json(linkedMapOf("transactionHash" to receiptHash, "from" to request.from,
            "to" to request.to, "blockHash" to blockHash, "blockNumber" to "0x100",
            "transactionIndex" to "0x0", "status" to receiptStatus), badReceiptField)
        fun json(fields: LinkedHashMap<String, String>, bad: String?): String {
            if (bad != null) fields[bad] = "0x9"
            return fields.entries.joinToString(",", "{", "}") { "\"${it.key}\":\"${it.value}\"" }
        }
    }
}
