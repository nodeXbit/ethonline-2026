package io.github.nodexbit.ethonline2026

import java.math.BigInteger
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CredentialVerticalTest {
    @Test
    fun `expiry bounds reject expired and namespace overflow`() {
        assertTrue(runCatching { CredentialValidation.validateExpiry(BigInteger.TEN, BigInteger.TEN) }.isFailure)
        assertTrue(
            runCatching {
                CredentialValidation.validateExpiry(
                    BigInteger.valueOf(IssuerSpace.namespaceExpiry), BigInteger.ONE,
                )
            }.isFailure,
        )
        CredentialValidation.validateExpiry(BigInteger.valueOf(IssuerSpace.STAFF_EXPIRY), BigInteger.ONE)
    }

    @Test
    fun `issuer authority controls are positive only for confirmed capability`() {
        val positive = authority(IssuerSpace.issuer)
        val holder = authority(IssuerSpace.STAFF_HOLDER)
        val missingRole = authority(IssuerSpace.issuer, registryRoles = BigInteger.ZERO)
        assertTrue(positive.allowed)
        assertEquals("NOT_CONFIGURED_ISSUER", holder.category)
        assertEquals("R1_AUTHORITY_MISSING", missingRole.category)
        assertTrue(CredentialProductPolicy.issuerControlsVisible(positive))
        assertFalse(CredentialProductPolicy.issuerControlsVisible(holder))
        assertFalse(CredentialProductPolicy.issuerControlsVisible(missingRole))
    }

    @Test
    fun `staff availability requires a fresh AVAILABLE snapshot`() {
        CredentialProductPolicy.requireAvailable(snapshot(status = CredentialRegistryStatus.AVAILABLE))
        assertTrue(runCatching { CredentialProductPolicy.requireAvailable(snapshot()) }.isFailure)
        assertTrue(
            runCatching {
                CredentialProductPolicy.requireAvailable(snapshot(status = CredentialRegistryStatus.AVAILABLE, read = CredentialReadStatus.UNKNOWN))
            }.isFailure,
        )
    }

    @Test
    fun `two step issuance persists and records failure resumes records only`() {
        val store = MemoryStore()
        val coordinator = IssuanceCoordinator(store)
        coordinator.start(IssuerSpace.issuer, "")
        coordinator.registerReady("register-op")
        coordinator.registerSubmitted()
        val configuring = coordinator.registerConfirmed()
        assertEquals(IssuanceState.REGISTERED_CONFIGURING, configuring.state)
        assertEquals("register-op", configuring.registerOperationId)
        coordinator.recordsReady("records-op")
        coordinator.recordsSubmitted()
        val resumed = coordinator.recordsFailed()
        assertEquals(IssuanceState.REGISTERED_CONFIGURING, resumed.state)
        assertEquals("register-op", resumed.registerOperationId)
        assertEquals("records-op", resumed.recordsOperationId)
    }

    @Test
    fun `confirmed real staff recovery stays read only until final configure`() {
        val issuanceStore = MemoryStore()
        val first = IssuanceCoordinator(issuanceStore)
        first.start(IssuerSpace.issuer, "")
        first.registerReady(REGISTER_OPERATION_ID)
        first.registerSubmitted()
        first.registerConfirmed()

        val journal = TransactionJournal(MemoryJournal())
        journal.put(confirmedRegisterOperation())
        val engine = RecoverableTransactionEngine(journal, now = { 2L }, newOperationId = { "records-op" })
        val restored = IssuanceCoordinator(issuanceStore)
        val session = restored.current(IssuerSpace.issuer)!!

        assertEquals(IssuanceState.REGISTERED_CONFIGURING, session.state)
        assertEquals(IssuanceRecoveryAction.RESUME_RECORDS, restored.recoveryAction(
            session, engine.find(REGISTER_OPERATION_ID), null,
        ))
        assertEquals("Credential created\nSetup incomplete", ProductShellPolicy.humanIssuanceStatus(session.state))
        assertTrue(ProductShellPolicy.resumeSetupVisible(session.state))
        assertFalse(ProductShellPolicy.createCredentialVisible(session.state))
        assertEquals(session, restored.registerConfirmed())

        val firstReview = CredentialConfigurationPolicy.prepare(session)
        val repeatedReview = CredentialConfigurationPolicy.prepare(session)
        assertEquals(firstReview, repeatedReview)
        assertEquals(CredentialConfigurationPolicy.PURPOSE, firstReview.purpose)
        assertEquals(2, firstReview.calls.size)
        assertEquals(
            CredentialAbi.setText(
                CredentialAbi.namehash(IssuerSpace.fullName), "description", IssuerSpace.DEFAULT_DESCRIPTION,
            ),
            firstReview.calls[0],
        )
        assertEquals(
            CredentialAbi.setData(
                CredentialAbi.namehash(IssuerSpace.fullName),
                IssuerSpace.ACCESS_KEY,
                CredentialAbi.accessValue(true, BigInteger.valueOf(IssuerSpace.STAFF_EXPIRY)),
            ),
            firstReview.calls[1],
        )
        assertFalse(firstReview.calldata.contains("68747470733a2f2f6578616d706c652e636f6d2f73746166662e706e67"))
        assertTrue(runCatching {
            CredentialConfigurationPolicy.prepare(session.copy(avatarUri = "https://example.com/staff.png"))
        }.isFailure)
        assertEquals(1, engine.all().size)
        assertNull(engine.latestForWallet(IssuerSpace.issuer, ContractTransactionRunner.RECORDS_OPERATION))

        val recordsOperation = engine.create(TransactionIntent(
            operationType = ContractTransactionRunner.RECORDS_OPERATION,
            walletAddress = IssuerSpace.issuer,
            chainId = IssuerSpace.chainId,
            targetAddress = IssuerSpace.resolver,
            valueWei = "0",
            dataSummary = CredentialAbi.calldataFingerprint(firstReview.calldata),
        ))
        assertEquals("records-op", recordsOperation.operationId)
        assertEquals(TransactionOperationState.DRAFT, recordsOperation.state)
        assertEquals(2, engine.all().size)
    }

    @Test
    fun `review is memory only and final create begins persistent issuance state`() {
        val issuanceStore = MemoryStore()
        val coordinator = IssuanceCoordinator(issuanceStore)
        val engine = RecoverableTransactionEngine(TransactionJournal(MemoryJournal()))

        val review = CredentialReviewPolicy.prepare("https://example.com/first.png")
        CredentialReviewPolicy.prepare("https://example.com/second.png")
        assertNull(coordinator.current(IssuerSpace.issuer))
        assertNull(engine.latestForWallet(IssuerSpace.issuer, ContractTransactionRunner.REGISTER_OPERATION))

        val request = ContractTransactionRequest(
            ContractTransactionRunner.REGISTER_OPERATION,
            IssuerSpace.issuer,
            IssuerSpace.registry,
            CredentialAbi.register(
                IssuerSpace.STAFF_LABEL,
                IssuerSpace.STAFF_HOLDER,
                IssuerSpace.resolver,
                BigInteger.valueOf(IssuerSpace.STAFF_EXPIRY),
            ),
        )
        val operation = engine.create(TransactionIntent(
            operationType = request.operationType,
            walletAddress = request.from,
            chainId = IssuerSpace.chainId,
            targetAddress = request.to,
            valueWei = "0",
            dataSummary = CredentialAbi.calldataFingerprint(request.data),
        ))
        val session = coordinator.beginRegister(IssuerSpace.issuer, review.avatarUri, operation.operationId)

        assertEquals(IssuanceState.REGISTER_READY, session.state)
        assertEquals(review.avatarUri, session.avatarUri)
        assertEquals(operation.operationId, session.registerOperationId)
        assertNotNull(engine.find(operation.operationId))
    }

    @Test
    fun `restart recovery distinguishes register and records hashes`() {
        val store = MemoryStore()
        val first = IssuanceCoordinator(store)
        val registerSession = first.start(IssuerSpace.issuer, "").let {
            first.registerReady("register-op").let { first.registerSubmitted() }
        }
        val reloaded = IssuanceCoordinator(store)
        val registerOperation = operation("register-op", ContractTransactionRunner.REGISTER_OPERATION)
        assertEquals(
            IssuanceRecoveryAction.RECOVER_REGISTER,
            reloaded.recoveryAction(registerSession, registerOperation, null),
        )

        first.registerConfirmed()
        first.recordsReady("records-op")
        val recordsSession = first.recordsSubmitted()
        val recordsOperation = operation("records-op", ContractTransactionRunner.RECORDS_OPERATION)
        assertEquals(
            IssuanceRecoveryAction.RECOVER_RECORDS,
            IssuanceCoordinator(store).recoveryAction(recordsSession, registerOperation, recordsOperation),
        )
    }

    @Test
    fun `authoritative readback rejects every security mismatch`() {
        val expected = expectation()
        CredentialReadPolicy.requireReady(snapshot(), expected)
        val mismatches = listOf(
            snapshot(owner = "0x0000000000000000000000000000000000000001"),
            snapshot(resolver = "0x0000000000000000000000000000000000000001"),
            snapshot(registry = "0x0000000000000000000000000000000000000001"),
            snapshot(provenance = false),
            snapshot(expiry = BigInteger.ONE),
            snapshot(access = false),
            snapshot(accessUntil = BigInteger.ONE),
            snapshot(snapshotTime = BigInteger.valueOf(IssuerSpace.STAFF_EXPIRY + 1)),
        )
        mismatches.forEach { assertTrue(runCatching { CredentialReadPolicy.requireReady(it, expected) }.isFailure) }
    }

    @Test
    fun `final readback waits for receipt block before evaluating invariants`() = runBlocking {
        val receiptBlock = BigInteger.valueOf(11_685_150L)
        val stale = snapshot(
            description = "",
            access = false,
            accessUntil = BigInteger.ZERO,
            snapshotBlock = receiptBlock - BigInteger.ONE,
        )
        val eligible = snapshot(snapshotBlock = receiptBlock)
        var reads = 0
        var waits = 0
        val recovered = CredentialFinalReadbackReconciler(
            read = {
                reads += 1
                if (reads == 1) stale else eligible
            },
            wait = { waits += 1 },
            attempts = 2,
        ).reconcile(expectation(), receiptBlock)

        assertEquals(receiptBlock, recovered.snapshotBlock)
        assertEquals(2, reads)
        assertEquals(1, waits)

        val behind = runCatching {
            CredentialFinalReadbackReconciler(read = { stale }, wait = {}, attempts = 1)
                .reconcile(expectation(), receiptBlock)
        }.exceptionOrNull()
        assertEquals("READBACK_BEHIND_CONFIRMED_RECEIPT", behind?.message)
        assertFalse(behind?.message.orEmpty().contains("WRONG_DESCRIPTION"))

        CredentialFinalReadbackReconciler(read = { eligible }, wait = {}, attempts = 1)
            .reconcile(expectation(), receiptBlock)
        CredentialFinalReadbackReconciler(
            read = { snapshot(snapshotBlock = receiptBlock + BigInteger.ONE) }, wait = {}, attempts = 1,
        ).reconcile(expectation(), receiptBlock)

        val wrongAtEligibleBlock = runCatching {
            CredentialFinalReadbackReconciler(
                read = { snapshot(description = "", snapshotBlock = receiptBlock) }, wait = {}, attempts = 1,
            ).reconcile(expectation(), receiptBlock)
        }.exceptionOrNull()
        assertEquals("WRONG_DESCRIPTION", wrongAtEligibleBlock?.message)
    }

    @Test
    fun `confirmed records restart recovers read only to idempotent READY`() = runBlocking {
        val issuanceStore = MemoryStore()
        val first = IssuanceCoordinator(issuanceStore)
        first.start(IssuerSpace.issuer, "")
        first.registerReady(REGISTER_OPERATION_ID)
        first.registerSubmitted()
        first.registerConfirmed()
        first.recordsReady(RECORDS_OPERATION_ID)
        first.recordsSubmitted()
        first.recordsConfirmed()
        first.beginReadback()

        val journalStore = MemoryJournal()
        val journal = TransactionJournal(journalStore)
        journal.put(confirmedRegisterOperation())
        journal.put(confirmedRecordsOperation())
        val restoredJournal = TransactionJournal(journalStore)
        val engine = RecoverableTransactionEngine(restoredJournal)
        val restored = IssuanceCoordinator(issuanceStore)
        val session = restored.current(IssuerSpace.issuer)!!
        val records = engine.find(RECORDS_OPERATION_ID)

        assertEquals(IssuanceState.AUTHORITATIVE_READBACK, session.state)
        assertEquals(TransactionOperationState.CONFIRMED, records?.state)
        assertEquals("11685150", records?.receiptBlock)
        assertEquals(IssuanceRecoveryAction.READBACK, restored.recoveryAction(
            session, engine.find(REGISTER_OPERATION_ID), records,
        ))
        val minimumBlock = CredentialFinalReadbackPolicy.confirmedReceiptBlock(session, records)
        assertEquals(BigInteger.valueOf(11_685_150L), minimumBlock)

        var readCalls = 0
        CredentialFinalReadbackReconciler(
            read = {
                readCalls += 1
                snapshot(snapshotBlock = minimumBlock)
            },
            wait = {},
            attempts = 1,
        ).reconcile(expectation(), minimumBlock)
        val ready = restored.ready()
        assertEquals(IssuanceState.READY, ready.state)
        assertEquals(ready, restored.ready())
        assertEquals(ready, restored.beginReadback())
        assertEquals(1, readCalls)
        assertEquals(2, engine.all().size)
        assertEquals(1, engine.all().count { it.operationType == ContractTransactionRunner.RECORDS_OPERATION })
    }

    @Test
    fun `malformed access and RPC failure never display ALLOWED`() = runBlocking {
        assertNull(CredentialRecordDecoding.access(ByteArray(0)))
        assertTrue(runCatching { CredentialAbi.decodeAccess(ByteArray(63)) }.isFailure)
        assertTrue(runCatching { CredentialRecordDecoding.access(ByteArray(63)) }.isFailure)
        assertTrue(runCatching { CredentialAbi.decodeAccess(ByteArray(64) { 2 }) }.isFailure)
        val unknown = CredentialReader(
            ReadOnlyEthereumRpcClient(transport = ReadOnlyRpcTransport { _, _ -> error("offline") }),
        ).read(IssuerSpace.fullName)
        assertEquals(CredentialReadStatus.UNKNOWN, unknown.readStatus)
        assertFalse(unknown.authoritativeAllowed)
    }

    @Test
    fun `local index deduplicates and partitions wallet switches`() {
        val store = MemoryStore()
        val index = LocalCredentialIndex(store)
        val reference = CredentialReference(IssuerSpace.chainId, IssuerSpace.fullName)
        assertTrue(index.add(IssuerSpace.STAFF_HOLDER, reference))
        assertFalse(index.add(IssuerSpace.STAFF_HOLDER.lowercase(), reference))
        assertEquals(1, index.list(IssuerSpace.STAFF_HOLDER).size)
        assertTrue(index.list(IssuerSpace.issuer).isEmpty())
        assertEquals(1, LocalCredentialIndex(store).list(IssuerSpace.STAFF_HOLDER).size)
    }

    @Test
    fun `import rejects wrong owner and owned credential is displayable only when fresh`() {
        assertTrue(CredentialProductPolicy.ownedBy(snapshot(), IssuerSpace.STAFF_HOLDER))
        assertFalse(CredentialProductPolicy.ownedBy(snapshot(), IssuerSpace.issuer))
        assertFalse(
            CredentialProductPolicy.ownedBy(
                snapshot(read = CredentialReadStatus.UNKNOWN), IssuerSpace.STAFF_HOLDER,
            ),
        )
    }

    private fun expectation() = CredentialExpectation(
        IssuerSpace.fullName,
        IssuerSpace.STAFF_HOLDER,
        IssuerSpace.resolver,
        BigInteger.valueOf(IssuerSpace.STAFF_EXPIRY),
        IssuerSpace.DEFAULT_DESCRIPTION,
        "",
    )

    private fun authority(
        account: String,
        registryRoles: BigInteger = IssuerSpace.registryRootRoles,
    ) = IssuerAuthorityPolicy.evaluate(
        account = account,
        registryRoles = registryRoles,
        resolverRoles = IssuerSpace.resolverRootRoles,
        provenanceMatches = true,
        namespaceStatus = BigInteger.valueOf(2),
        namespaceOwner = IssuerSpace.issuer,
        namespaceRegistry = IssuerSpace.registry,
        namespaceResolver = IssuerSpace.resolver,
        namespaceExpiry = BigInteger.valueOf(IssuerSpace.namespaceExpiry),
        now = BigInteger.ONE,
        blockNumber = BigInteger.ONE,
    )

    private fun snapshot(
        status: CredentialRegistryStatus = CredentialRegistryStatus.REGISTERED,
        read: CredentialReadStatus = CredentialReadStatus.FRESH,
        owner: String = IssuerSpace.STAFF_HOLDER,
        resolver: String = IssuerSpace.resolver,
        registry: String = IssuerSpace.registry,
        expiry: BigInteger = BigInteger.valueOf(IssuerSpace.STAFF_EXPIRY),
        access: Boolean = true,
        accessUntil: BigInteger = BigInteger.valueOf(IssuerSpace.STAFF_EXPIRY),
        provenance: Boolean = true,
        snapshotTime: BigInteger = BigInteger.ONE,
        snapshotBlock: BigInteger = BigInteger.ONE,
        description: String = IssuerSpace.DEFAULT_DESCRIPTION,
    ) = CredentialSnapshot(
        fullName = IssuerSpace.fullName,
        node = CredentialAbi.nodeHex(IssuerSpace.fullName),
        registry = registry,
        resolver = resolver,
        subregistry = IssuerSpace.ZERO_ADDRESS,
        tokenId = BigInteger.TEN,
        status = status,
        owner = owner,
        registryExpiry = expiry,
        ownerRoleBitmap = BigInteger.ZERO,
        transferable = false,
        accessActive = access,
        accessValidUntil = accessUntil,
        avatarUri = "",
        description = description,
        snapshotBlock = snapshotBlock,
        snapshotTimestamp = snapshotTime,
        readStatus = read,
        provenanceMatches = provenance,
    )

    private fun operation(id: String, type: String) = PersistedTransactionOperation(
        operationId = id,
        operationType = type,
        walletAddress = IssuerSpace.issuer,
        chainId = IssuerSpace.chainId,
        targetAddress = IssuerSpace.registry,
        valueWei = "0",
        dataSummary = "fixture",
        preLatestNonce = "1",
        prePendingNonce = "1",
        txHash = "0x${"1".repeat(64)}",
        state = TransactionOperationState.HASH_RECEIVED,
        createdAt = 1,
        updatedAt = 1,
    )

    private fun confirmedRegisterOperation() = PersistedTransactionOperation(
        operationId = REGISTER_OPERATION_ID,
        operationType = ContractTransactionRunner.REGISTER_OPERATION,
        walletAddress = IssuerSpace.issuer,
        chainId = IssuerSpace.chainId,
        targetAddress = IssuerSpace.registry,
        valueWei = "0",
        dataSummary = "confirmed-real-registration",
        preLatestNonce = "1",
        prePendingNonce = "1",
        txHash = REGISTER_TX_HASH,
        state = TransactionOperationState.CONFIRMED,
        receiptBlock = "11684957",
        createdAt = 1,
        updatedAt = 1,
        postLatestNonce = "2",
        postPendingNonce = "2",
    )

    private fun confirmedRecordsOperation() = PersistedTransactionOperation(
        operationId = RECORDS_OPERATION_ID,
        operationType = ContractTransactionRunner.RECORDS_OPERATION,
        walletAddress = IssuerSpace.issuer,
        chainId = IssuerSpace.chainId,
        targetAddress = IssuerSpace.resolver,
        valueWei = "0",
        dataSummary = "confirmed-real-configuration",
        preLatestNonce = "2",
        prePendingNonce = "2",
        txHash = RECORDS_TX_HASH,
        state = TransactionOperationState.CONFIRMED,
        receiptBlock = "11685150",
        createdAt = 2,
        updatedAt = 2,
        postLatestNonce = "3",
        postPendingNonce = "3",
    )

    private class MemoryStore(var value: String? = null) : LoadableStringStateStore {
        override fun load(): String? = value
        override fun save(value: String) { this.value = value }
    }

    private class MemoryJournal : TransactionJournalStore {
        private var value: String? = null
        override fun load(): String? = value
        override fun save(serializedJournal: String) { value = serializedJournal }
    }

    private companion object {
        const val REGISTER_OPERATION_ID = "c9085610-c3d6-4d6b-9534-f55d94580e23"
        const val REGISTER_TX_HASH =
            "0x8858c291f14659ea8e323d1af988f4ee379c2a14dc5e2cbed4db592a04e6db58"
        const val RECORDS_OPERATION_ID = "585f5dec-8a8d-42de-bf00-3bfee084087a"
        const val RECORDS_TX_HASH =
            "0x57a5a4bf81fcede947b83bf55dcabe065540a1254fb2d9eb10677d391cf7bb11"
    }
}
