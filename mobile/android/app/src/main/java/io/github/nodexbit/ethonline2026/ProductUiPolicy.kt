package io.github.nodexbit.ethonline2026

enum class ProductDestination { MY_KEYS, ISSUER, SETTINGS, DIAGNOSTICS }

object ProductBrand {
    const val NAME = "LockENS"
    const val SUBTITLE = "Programmable access credentials powered by ENS"
    const val COPY_WALLET_DESCRIPTION = "Copy wallet address"
    const val NETWORK_DESCRIPTION = "Sepolia Testnet"
}

data class ProductContentPadding(val left: Int, val top: Int, val right: Int, val bottom: Int)

data class ReviewDialogActionPlan(
    val primaryLabel: String,
    val stackedFullWidth: Boolean,
    val minimumControlHeightDp: Int,
)

object ProductSpacing {
    const val CONTENT_GUTTER_DP = 24
    const val TOP_GUTTER_DP = 16
    const val BOTTOM_GUTTER_DP = 24
    const val CARD_GAP_DP = 16
    const val CONTROL_HEIGHT_DP = 52

    fun contentPadding(density: Float): ProductContentPadding = ProductContentPadding(
        left = (CONTENT_GUTTER_DP * density).toInt(),
        top = (TOP_GUTTER_DP * density).toInt(),
        right = (CONTENT_GUTTER_DP * density).toInt(),
        bottom = (BOTTOM_GUTTER_DP * density).toInt(),
    )

    fun viewportPadding(left: Int, top: Int, right: Int, bottom: Int): ProductContentPadding =
        ProductContentPadding(left, top, right, bottom)
}

object ReviewDialogPolicy {
    fun actionPlan(recordsOnly: Boolean) = ReviewDialogActionPlan(
        primaryLabel = if (recordsOnly) "Configure credential" else "Create credential",
        stackedFullWidth = true,
        minimumControlHeightDp = ProductSpacing.CONTROL_HEIGHT_DP,
    )
}

data class CredentialProgress(val step: Int, val title: String, val status: String) {
    fun visibleText(): String = "$title\n$step of 3 · $status"
}

data class StaffReviewPresentation(
    val credential: String,
    val recipient: String,
    val exactRecipient: String,
    val issuingWallet: String,
    val access: String,
    val expires: String,
    val exactExpiry: String,
    val utcExpiry: String,
    val transferability: String,
    val description: String,
    val artwork: String,
    val network: String,
    val transactionPurposes: List<String>,
)

data class CredentialReviewDraft(
    val avatarUri: String,
    val presentation: StaffReviewPresentation,
)

data class CredentialConfigurationReviewDraft(
    val review: CredentialReviewDraft,
    val calls: List<String>,
    val calldata: String,
    val purpose: String,
)

object CredentialReviewPolicy {
    fun prepare(avatarInput: String): CredentialReviewDraft {
        val avatar = CredentialValidation.normalizeAvatar(avatarInput)
        return CredentialReviewDraft(avatar, ProductShellPolicy.staffReview(avatar))
    }

    fun matchesCurrentArtwork(review: CredentialReviewDraft, avatarInput: String): Boolean =
        review.avatarUri == CredentialValidation.normalizeAvatar(avatarInput)
}

object CredentialConfigurationPolicy {
    const val PURPOSE = "Configure access and presentation for the already-created pass."

    fun prepare(session: IssuanceSession): CredentialConfigurationReviewDraft {
        require(session.state in setOf(IssuanceState.REGISTERED_CONFIGURING, IssuanceState.RECORDS_READY)) {
            "RECORDS_NOT_READY"
        }
        return build(session)
    }

    fun calldata(session: IssuanceSession): String = build(session).calldata

    private fun build(session: IssuanceSession): CredentialConfigurationReviewDraft {
        require(session.wallet.equals(IssuerSpace.issuer, true)) { "WRONG_ISSUER" }
        require(session.fullName == IssuerSpace.fullName) { "WRONG_CREDENTIAL" }
        require(session.holder.equals(IssuerSpace.STAFF_HOLDER, true)) { "WRONG_OWNER" }
        require(session.expiry == java.math.BigInteger.valueOf(IssuerSpace.STAFF_EXPIRY)) { "WRONG_REGISTRY_EXPIRY" }
        require(session.description == IssuerSpace.DEFAULT_DESCRIPTION) { "WRONG_DESCRIPTION" }
        require(session.avatarUri.isBlank()) { "RECOVERED_STAFF_ARTWORK_NOT_BLANK" }
        val calls = CredentialAbi.credentialRecords(
            session.fullName,
            session.avatarUri,
            session.description,
            session.expiry,
        )
        return CredentialConfigurationReviewDraft(
            review = CredentialReviewPolicy.prepare(session.avatarUri),
            calls = calls,
            calldata = CredentialAbi.multicall(calls),
            purpose = PURPOSE,
        )
    }
}

data class SafeActionFailure(
    val action: String,
    val stage: String,
    val category: String,
    val humanMessage: String,
    val exceptionClass: String,
) {
    fun diagnosticText(): String = buildString {
        append("LAST ACTION FAILURE\n")
        append("Action: $action\n")
        append("Stage: $stage\n")
        append("Category: $category\n")
        append("Class: $exceptionClass")
    }
}

object SafeActionFailurePolicy {
    private val SAFE_CODE = Regex("^[A-Z][A-Z0-9_]{1,95}$")
    private val SECRET_MARKER = Regex("(?i)(https?://|authorization|bearer|token|secret|private|email|otp|signature)")

    fun from(action: String, stage: String, error: Throwable): SafeActionFailure {
        val exception = error::class.simpleName?.take(64) ?: "Error"
        val rawCategory = error.message?.trim()
        val category = rawCategory
            ?.takeIf { SAFE_CODE.matches(it) && !SECRET_MARKER.containsMatchIn(it) }
            ?: exception.replace(Regex("[^A-Za-z0-9]+"), "_").uppercase().take(96)
        val safeStage = stage.takeIf(SAFE_CODE::matches) ?: "ACTION"
        val safeAction = action.replace(Regex("[^A-Za-z0-9 ._-]+"), "").trim().take(64).ifBlank { "Operation" }
        val human = when (safeStage) {
            "POST_TX1_FINALIZATION" ->
                "Credential creation is confirmed. Setup is incomplete; reopen Issuer and use Resume setup."
            "TX2_REVIEW_PREFLIGHT" ->
                "Could not verify the existing credential for setup. No configuration transaction was started."
            "TX2_SUBMISSION" ->
                "Could not complete credential setup safely. Check Developer Diagnostics before trying again."
            "FINAL_READBACK" ->
                "Verification is temporarily unavailable. TX2 is confirmed; retry verification without resending it."
            else -> "$safeAction could not be completed safely. Check Developer Diagnostics for details."
        }
        return SafeActionFailure(safeAction, safeStage, category, human, exception)
    }
}

object ProductShellPolicy {
    private val TECHNICAL_TERMS = listOf("calldata", "role bitmap", "nonce", "operation id", "uuid")

    fun loggedOutVisible(authenticated: Boolean): Boolean = !authenticated

    fun defaultDestination(authenticated: Boolean): ProductDestination? =
        if (authenticated) ProductDestination.MY_KEYS else null

    fun destinations(authenticated: Boolean, issuerCapability: Boolean): Set<ProductDestination> = when {
        !authenticated -> emptySet()
        issuerCapability -> setOf(
            ProductDestination.MY_KEYS,
            ProductDestination.ISSUER,
            ProductDestination.SETTINGS,
        )
        else -> setOf(ProductDestination.MY_KEYS, ProductDestination.SETTINGS)
    }

    fun diagnosticsInline(): Boolean = false

    fun diagnosticsReachableFromSettings(authenticated: Boolean): Boolean = authenticated

    fun resumeSetupVisible(state: IssuanceState): Boolean = state in setOf(
        IssuanceState.REGISTERED_CONFIGURING,
        IssuanceState.RECORDS_READY,
    )

    fun retryVerificationVisible(state: IssuanceState): Boolean =
        state == IssuanceState.AUTHORITATIVE_READBACK

    fun credentialVerificationMessage(state: IssuanceState): String = when (state) {
        IssuanceState.READY -> "Credential ready"
        IssuanceState.AUTHORITATIVE_READBACK -> "Verifying onchain"
        else -> humanIssuanceStatus(state)
    }

    fun createCredentialVisible(state: IssuanceState): Boolean = state in setOf(
        IssuanceState.DRAFT,
        IssuanceState.REGISTER_READY,
    )

    fun compactAddress(address: String): String {
        if (address.length < 12) return address
        return "${address.take(6)}…${address.takeLast(4)}"
    }

    fun identityLabel(address: String, issuerCapability: Boolean): String =
        WalletCapabilityPresentation.identity(address)

    fun staffReview(avatarUri: String): StaffReviewPresentation = StaffReviewPresentation(
        credential = IssuerSpace.fullName,
        recipient = compactAddress(IssuerSpace.STAFF_HOLDER),
        exactRecipient = IssuerSpace.STAFF_HOLDER,
        issuingWallet = IssuerSpace.issuer,
        access = "Allowed",
        expires = "31 Oct 2026, 23:59",
        exactExpiry = "31 Oct 2026, 23:59:59 Europe/Madrid",
        utcExpiry = "2026-10-31T22:59:59Z",
        transferability = "Non-transferable",
        description = IssuerSpace.DEFAULT_DESCRIPTION,
        artwork = avatarUri.ifBlank { "Not set" },
        network = "Sepolia",
        transactionPurposes = listOf(
            "Create the access pass.",
            "Configure its access and presentation records.",
        ),
    )

    fun reviewContainsTechnicalFields(review: StaffReviewPresentation): Boolean {
        val visible = listOf(
            review.credential,
            review.recipient,
            review.exactRecipient,
            review.issuingWallet,
            review.access,
            review.expires,
            review.exactExpiry,
            review.utcExpiry,
            review.transferability,
            review.description,
            review.artwork,
            review.network,
            review.transactionPurposes.joinToString(" "),
        ).joinToString(" ").lowercase()
        return TECHNICAL_TERMS.any(visible::contains)
    }

    fun humanIssuanceStatus(state: IssuanceState): String = when (state) {
        IssuanceState.DRAFT, IssuanceState.REGISTER_READY -> "Preparing"
        IssuanceState.REGISTER_SUBMITTED -> "Creating credential"
        IssuanceState.REGISTER_CONFIRMED -> "Registration confirmed"
        IssuanceState.REGISTERED_CONFIGURING -> "Credential created\nSetup incomplete"
        IssuanceState.RECORDS_READY, IssuanceState.RECORDS_SUBMITTED -> "Configuring credential"
        IssuanceState.RECORDS_CONFIRMED, IssuanceState.AUTHORITATIVE_READBACK -> "Verifying onchain"
        IssuanceState.READY -> "Credential ready"
    }

    fun issuanceProgress(
        session: IssuanceSession?,
        registerOperation: PersistedTransactionOperation? = null,
        recordsOperation: PersistedTransactionOperation? = null,
    ): CredentialProgress? = when (session?.state) {
        null, IssuanceState.DRAFT -> null
        IssuanceState.REGISTER_READY -> registerOperation
            ?.takeUnless { it.state == TransactionOperationState.DRAFT }
            ?.let { CredentialProgress(1, "Creating credential", humanTransactionStatus(it)) }
        IssuanceState.REGISTER_SUBMITTED,
        IssuanceState.REGISTER_CONFIRMED,
        -> CredentialProgress(
            1,
            "Creating credential",
            registerOperation?.let(::humanTransactionStatus) ?: "Status unavailable",
        )
        IssuanceState.REGISTERED_CONFIGURING -> if (recordsOperation != null) {
            CredentialProgress(2, "Configuring access", humanTransactionStatus(recordsOperation))
        } else {
            CredentialProgress(2, "Credential created · Setup incomplete", "Ready to configure")
        }
        IssuanceState.RECORDS_READY -> recordsOperation
            ?.takeUnless { it.state == TransactionOperationState.DRAFT }
            ?.let { CredentialProgress(2, "Configuring access", humanTransactionStatus(it)) }
        IssuanceState.RECORDS_SUBMITTED,
        IssuanceState.RECORDS_CONFIRMED,
        -> CredentialProgress(
            2,
            "Configuring access",
            recordsOperation?.let(::humanTransactionStatus) ?: "Status unavailable",
        )
        IssuanceState.AUTHORITATIVE_READBACK ->
            CredentialProgress(3, "Verifying onchain", "Confirming")
        IssuanceState.READY ->
            CredentialProgress(3, "Credential ready", "Confirmed")
    }

    fun humanTransactionStatus(operation: PersistedTransactionOperation): String = when (operation.state) {
        TransactionOperationState.DRAFT,
        TransactionOperationState.READY_TO_REVIEW,
        TransactionOperationState.READY_TO_SUBMIT,
        -> if (operation.safeErrorCategory == null) "Waiting for wallet approval" else "Status unavailable"
        TransactionOperationState.SUBMITTING_NO_HASH -> "Submitting — do not retry"
        TransactionOperationState.HASH_RECEIVED -> "Submitted"
        TransactionOperationState.CONFIRMING,
        TransactionOperationState.ONCHAIN_READBACK,
        -> "Confirming"
        TransactionOperationState.CONFIRMED -> "Confirmed"
        TransactionOperationState.REVERTED -> "Failed / Reverted"
        TransactionOperationState.UNKNOWN -> if (operation.txHash != null) {
            "Status unknown — checking existing attempt"
        } else {
            "Status unavailable"
        }
        TransactionOperationState.NO_BROADCAST_PROVEN -> "Previous attempt was not broadcast"
        TransactionOperationState.CANCELLED,
        TransactionOperationState.LEGACY_ATTEMPT_REQUIRES_RECONCILIATION,
        -> "Status unavailable"
    }
}
