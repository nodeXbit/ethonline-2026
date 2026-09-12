package io.github.nodexbit.ethonline2026

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ProductUiPolicyTest {
    @Test
    fun `LockENS brand and header accessibility labels are exact`() {
        assertEquals("LockENS", ProductBrand.NAME)
        assertEquals("Programmable access credentials powered by ENS", ProductBrand.SUBTITLE)
        assertEquals("Copy wallet address", ProductBrand.COPY_WALLET_DESCRIPTION)
        assertEquals("Sepolia Testnet", ProductBrand.NETWORK_DESCRIPTION)
        assertFalse(NetworkPresentationPolicy.configured().interactive)
    }

    @Test
    fun `logged out shell contains no product destinations`() {
        assertTrue(ProductShellPolicy.loggedOutVisible(authenticated = false))
        assertTrue(ProductShellPolicy.destinations(authenticated = false, issuerCapability = true).isEmpty())
        assertNull(ProductShellPolicy.defaultDestination(authenticated = false))
    }

    @Test
    fun `authenticated holder defaults to My Keys without issuer navigation`() {
        val destinations = ProductShellPolicy.destinations(authenticated = true, issuerCapability = false)
        assertEquals(ProductDestination.MY_KEYS, ProductShellPolicy.defaultDestination(authenticated = true))
        assertEquals(setOf(ProductDestination.MY_KEYS, ProductDestination.SETTINGS), destinations)
        assertFalse(ProductDestination.STUDIO in destinations)
    }

    @Test
    fun `issuer navigation requires fresh onchain capability result`() {
        val holder = ProductShellPolicy.destinations(authenticated = true, issuerCapability = false)
        val issuer = ProductShellPolicy.destinations(authenticated = true, issuerCapability = true)
        assertFalse(ProductDestination.STUDIO in holder)
        assertTrue(ProductDestination.STUDIO in issuer)
        assertEquals("Active wallet · 0xFa90…2685", ProductShellPolicy.identityLabel(IssuerSpace.issuer, true))
        assertEquals("Active wallet · 0x3419…5FF7", ProductShellPolicy.identityLabel(IssuerSpace.STAFF_HOLDER, false))
    }

    @Test
    fun `diagnostics are reachable from settings but never inline`() {
        val normal = ProductShellPolicy.destinations(authenticated = true, issuerCapability = true)
        assertFalse(ProductDestination.DIAGNOSTICS in normal)
        assertFalse(ProductShellPolicy.diagnosticsInline())
        assertTrue(ProductShellPolicy.diagnosticsReachableFromSettings(authenticated = true))
        assertFalse(ProductShellPolicy.diagnosticsReachableFromSettings(authenticated = false))
    }

    @Test
    fun `staff review exposes exact human values and no technical fields`() {
        val review = ProductShellPolicy.staffReview("")
        assertEquals("staff-001.keys.demo-access.eth", review.credential)
        assertEquals("0x3419…5FF7", review.recipient)
        assertEquals("0x3419148731087b970d2059C53780163B452D5FF7", review.exactRecipient)
        assertEquals("0xFa90e8301A22833B74378C5fA3a7c120Ac512685", review.issuingWallet)
        assertEquals("Allowed", review.access)
        assertEquals("31 Oct 2026, 23:59", review.expires)
        assertEquals("31 Oct 2026, 23:59:59 Europe/Madrid", review.exactExpiry)
        assertEquals("2026-10-31T22:59:59Z", review.utcExpiry)
        assertEquals("Non-transferable", review.transferability)
        assertEquals("Staff Access Pass", review.description)
        assertEquals("Not set", review.artwork)
        assertEquals("Sepolia", review.network)
        assertEquals(
            listOf("Create the access pass.", "Configure its access and presentation records."),
            review.transactionPurposes,
        )
        assertFalse(ProductShellPolicy.reviewContainsTechnicalFields(review))
    }

    @Test
    fun `review and back cycles are pure and artwork remains editable`() {
        val engine = RecoverableTransactionEngine(TransactionJournal(MemoryJournal()))
        val first = CredentialReviewPolicy.prepare("")
        assertNull(ProductShellPolicy.issuanceProgress(null))
        assertNull(engine.latestForWallet(IssuerSpace.issuer, ContractTransactionRunner.REGISTER_OPERATION))

        val changed = CredentialReviewPolicy.prepare("https://example.com/staff.png")
        val repeated = CredentialReviewPolicy.prepare("https://example.com/staff.png")
        assertNotEquals(first.avatarUri, changed.avatarUri)
        assertEquals(changed, repeated)
        assertTrue(CredentialReviewPolicy.matchesCurrentArtwork(changed, "https://example.com/staff.png"))
        assertFalse(CredentialReviewPolicy.matchesCurrentArtwork(first, "https://example.com/staff.png"))
        assertNull(engine.latestForWallet(IssuerSpace.issuer, ContractTransactionRunner.REGISTER_OPERATION))
    }

    @Test
    fun `two transaction states stay human and resume setup remains reachable`() {
        assertEquals("Preparing", ProductShellPolicy.humanIssuanceStatus(IssuanceState.REGISTER_READY))
        assertEquals("Creating credential", ProductShellPolicy.humanIssuanceStatus(IssuanceState.REGISTER_SUBMITTED))
        assertEquals("Credential created\nSetup incomplete", ProductShellPolicy.humanIssuanceStatus(IssuanceState.REGISTERED_CONFIGURING))
        assertEquals("Configuring credential", ProductShellPolicy.humanIssuanceStatus(IssuanceState.RECORDS_SUBMITTED))
        assertEquals("Verifying onchain", ProductShellPolicy.humanIssuanceStatus(IssuanceState.AUTHORITATIVE_READBACK))
        assertEquals("Credential ready", ProductShellPolicy.humanIssuanceStatus(IssuanceState.READY))
        assertTrue(ProductShellPolicy.resumeSetupVisible(IssuanceState.REGISTERED_CONFIGURING))
        assertTrue(ProductShellPolicy.resumeSetupVisible(IssuanceState.RECORDS_READY))
        assertFalse(ProductShellPolicy.resumeSetupVisible(IssuanceState.REGISTER_READY))
        assertFalse(ProductShellPolicy.createCredentialVisible(IssuanceState.REGISTERED_CONFIGURING))
        assertTrue(ProductShellPolicy.createCredentialVisible(IssuanceState.REGISTER_READY))
    }

    @Test
    fun `safe action failures retain useful diagnostics without provider secrets`() {
        val finalization = SafeActionFailurePolicy.from(
            "Create credential", "POST_TX1_FINALIZATION", IllegalArgumentException("ILLEGAL_ISSUANCE_TRANSITION"),
        )
        assertEquals("POST_TX1_FINALIZATION", finalization.stage)
        assertEquals("ILLEGAL_ISSUANCE_TRANSITION", finalization.category)
        assertEquals("IllegalArgumentException", finalization.exceptionClass)
        assertTrue(finalization.humanMessage.contains("Pass creation is confirmed"))

        val secret = SafeActionFailurePolicy.from(
            "Resume setup", "TX2_REVIEW_PREFLIGHT",
            IllegalArgumentException("https://rpc.example/?token=do-not-expose"),
        )
        val visible = secret.diagnosticText() + secret.humanMessage
        assertFalse(visible.contains("rpc.example"))
        assertFalse(visible.contains("do-not-expose"))
        assertEquals("VALIDATION_FAILED", secret.category)
    }

    @Test
    fun `ready replaces stale failure and verification retry is read only presentation`() {
        val stale = SafeActionFailurePolicy.from(
            "Configure credential", "TX2_SUBMISSION", IllegalArgumentException("WRONG_DESCRIPTION"),
        ).humanMessage
        assertTrue(ProductShellPolicy.retryVerificationVisible(IssuanceState.AUTHORITATIVE_READBACK))
        assertFalse(ProductShellPolicy.retryVerificationVisible(IssuanceState.READY))
        assertEquals("Verifying onchain", ProductShellPolicy.credentialVerificationMessage(
            IssuanceState.AUTHORITATIVE_READBACK,
        ))
        assertEquals("Credential ready", ProductShellPolicy.credentialVerificationMessage(IssuanceState.READY))
        assertNotEquals(stale, ProductShellPolicy.credentialVerificationMessage(IssuanceState.READY))
    }

    @Test
    fun `long configure action uses accessible stacked full width layout`() {
        val plan = ReviewDialogPolicy.actionPlan(recordsOnly = true)
        assertEquals("Configure pass", plan.primaryLabel)
        assertTrue(plan.stackedFullWidth)
        assertEquals(ProductSpacing.CONTROL_HEIGHT_DP, plan.minimumControlHeightDp)
    }

    @Test
    fun `physical spacing adds one consistent gutter outside system bars`() {
        val content = ProductSpacing.contentPadding(density = 3f)
        val viewport = ProductSpacing.viewportPadding(0, 110, 0, 72)
        assertEquals(ProductContentPadding(72, 48, 72, 72), content)
        assertEquals(ProductContentPadding(0, 110, 0, 72), viewport)
        assertEquals(52, ProductSpacing.CONTROL_HEIGHT_DP)
    }

    @Test
    fun `credential progress is absent before create and transaction certainty is authoritative`() {
        assertNull(ProductShellPolicy.issuanceProgress(session(IssuanceState.DRAFT)))
        assertNull(ProductShellPolicy.issuanceProgress(
            session(IssuanceState.REGISTER_READY, "register"),
            operation(TransactionOperationState.DRAFT),
        ))
        assertEquals(
            "Credential created · Setup incomplete\n2 of 3 · Ready to configure",
            ProductShellPolicy.issuanceProgress(session(IssuanceState.REGISTERED_CONFIGURING))!!.visibleText(),
        )
        assertEquals(
            "Credential ready\n3 of 3 · Confirmed",
            ProductShellPolicy.issuanceProgress(session(IssuanceState.READY))!!.visibleText(),
        )
        assertEquals("Submitting — do not retry", ProductShellPolicy.humanTransactionStatus(
            operation(TransactionOperationState.SUBMITTING_NO_HASH),
        ))
        assertEquals("Submitted", ProductShellPolicy.humanTransactionStatus(
            operation(TransactionOperationState.HASH_RECEIVED, TX_HASH),
        ))
        assertEquals("Confirming", ProductShellPolicy.humanTransactionStatus(
            operation(TransactionOperationState.CONFIRMING, TX_HASH),
        ))
        assertEquals("Confirmed", ProductShellPolicy.humanTransactionStatus(
            operation(TransactionOperationState.CONFIRMED, TX_HASH),
        ))
        assertEquals("Failed / Reverted", ProductShellPolicy.humanTransactionStatus(
            operation(TransactionOperationState.REVERTED, TX_HASH),
        ))
        assertEquals("Previous attempt was not broadcast", ProductShellPolicy.humanTransactionStatus(
            operation(TransactionOperationState.NO_BROADCAST_PROVEN),
        ))
        val unknown = operation(TransactionOperationState.UNKNOWN, TX_HASH)
        assertEquals("Status unknown — checking existing attempt", ProductShellPolicy.humanTransactionStatus(unknown))
        assertEquals(
            "Creating credential\n1 of 3 · Status unknown — checking existing attempt",
            ProductShellPolicy.issuanceProgress(
                session(IssuanceState.REGISTER_SUBMITTED, "register"),
                unknown,
            )!!.visibleText(),
        )
        assertEquals("Status unavailable", ProductShellPolicy.humanTransactionStatus(
            operation(TransactionOperationState.UNKNOWN),
        ))
    }

    private fun session(state: IssuanceState, registerOperationId: String? = null) = IssuanceSession(
        wallet = IssuerSpace.issuer,
        fullName = IssuerSpace.fullName,
        holder = IssuerSpace.STAFF_HOLDER,
        expiry = java.math.BigInteger.valueOf(IssuerSpace.STAFF_EXPIRY),
        avatarUri = "",
        description = IssuerSpace.DEFAULT_DESCRIPTION,
        state = state,
        registerOperationId = registerOperationId,
    )

    private fun operation(state: TransactionOperationState, hash: String? = null) = PersistedTransactionOperation(
        operationId = "register",
        operationType = ContractTransactionRunner.REGISTER_OPERATION,
        walletAddress = IssuerSpace.issuer,
        chainId = IssuerSpace.chainId,
        targetAddress = IssuerSpace.registry,
        valueWei = "0",
        dataSummary = "fixture",
        txHash = hash,
        state = state,
        createdAt = 1,
        updatedAt = 1,
    )

    private class MemoryJournal : TransactionJournalStore {
        private var value: String? = null
        override fun load(): String? = value
        override fun save(serializedJournal: String) { value = serializedJournal }
    }

    companion object {
        private const val TX_HASH = "0x1111111111111111111111111111111111111111111111111111111111111111"
    }
}
