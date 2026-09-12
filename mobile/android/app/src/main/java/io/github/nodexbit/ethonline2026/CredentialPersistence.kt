package io.github.nodexbit.ethonline2026

import java.math.BigInteger
import java.util.UUID

fun interface StringStateStore {
    fun save(value: String)
}

interface LoadableStringStateStore : StringStateStore {
    fun load(): String?
}

data class CredentialReference(val chainId: Long, val fullName: String)

class LocalCredentialIndex(
    private val store: LoadableStringStateStore,
    private val limitPerWallet: Int = 20,
) {
    private val entries = decode(store.load()).toMutableList()

    @Synchronized
    fun add(wallet: String, reference: CredentialReference): Boolean {
        val normalizedWallet = wallet.lowercase()
        val normalizedName = CredentialValidation.normalizeFullName(reference.fullName)
        if (entries.any { it.wallet == normalizedWallet && it.chainId == reference.chainId && it.fullName == normalizedName }) {
            return false
        }
        require(entries.count { it.wallet == normalizedWallet } < limitPerWallet) { "CREDENTIAL_INDEX_LIMIT" }
        entries += Entry(normalizedWallet, reference.chainId, normalizedName)
        persist()
        return true
    }

    @Synchronized
    fun list(wallet: String): List<CredentialReference> = entries
        .filter { it.wallet == wallet.lowercase() }
        .map { CredentialReference(it.chainId, it.fullName) }

    private fun persist() = store.save(entries.joinToString("\n") {
        "${it.wallet}|${it.chainId}|${escape(it.fullName)}"
    })

    private fun decode(raw: String?): List<Entry> = raw.orEmpty().lineSequence()
        .filter(String::isNotBlank)
        .mapNotNull { line ->
            val fields = line.split('|')
            if (fields.size != 3) null else fields[1].toLongOrNull()?.let { Entry(fields[0], it, unescape(fields[2])) }
        }
        .toList()

    private data class Entry(val wallet: String, val chainId: Long, val fullName: String)
}

enum class IssuanceState {
    DRAFT,
    REGISTER_READY,
    REGISTER_SUBMITTED,
    REGISTER_CONFIRMED,
    REGISTERED_CONFIGURING,
    RECORDS_READY,
    RECORDS_SUBMITTED,
    RECORDS_CONFIRMED,
    AUTHORITATIVE_READBACK,
    READY,
}

data class IssuanceSession(
    val wallet: String,
    val fullName: String,
    val holder: String,
    val expiry: BigInteger,
    val avatarUri: String,
    val description: String,
    val state: IssuanceState,
    val registerOperationId: String? = null,
    val recordsOperationId: String? = null,
    val template: PassTemplate = PassTemplate.STAFF,
    val accessActive: Boolean = true,
    val accessValidUntil: BigInteger = expiry,
    val transferable: Boolean = false,
    val roleBitmap: BigInteger = if (transferable) StudioRoles.CAN_TRANSFER_ADMIN else BigInteger.ZERO,
    val sessionId: String = UUID.randomUUID().toString(),
    val acknowledgedOwner: String? = null,
    val acknowledgedRoles: BigInteger? = null,
    val acknowledgedTransferable: Boolean? = null,
    val allowedResources: Set<String>? = null,
) {
    val identity: IssuanceIdentity get() = IssuanceIdentity(wallet.lowercase(), fullName, sessionId, IssuerSpace.chainId)
    val configurationOwner: String get() = acknowledgedOwner ?: holder
    val configurationRoles: BigInteger get() = acknowledgedRoles ?: roleBitmap
    val configurationTransferable: Boolean get() = acknowledgedTransferable ?: transferable
}

data class IssuanceIdentity(val wallet: String, val fullName: String, val sessionId: String, val chainId: Long)

enum class IssuanceRecoveryAction { NONE, RECOVER_REGISTER, RESUME_RECORDS, RECOVER_RECORDS, READBACK, DISPLAY_READY }

class IssuanceCoordinator(private val store: LoadableStringStateStore) {
    private val sessions = decodeAll(store.load()).associateByTo(linkedMapOf()) { key(it.wallet, it.fullName) }

    @Synchronized
    fun current(wallet: String): IssuanceSession? = list(wallet).singleOrNull()

    @Synchronized
    fun current(wallet: String, fullName: String): IssuanceSession? =
        sessions[key(wallet, CredentialValidation.normalizeFullName(fullName))]

    @Synchronized
    fun list(wallet: String): List<IssuanceSession> = sessions.values.filter { it.wallet.equals(wallet, true) }

    @Synchronized
    fun get(identity: IssuanceIdentity): IssuanceSession {
        require(identity.chainId == IssuerSpace.chainId) { "WRONG_CHAIN" }
        val value = sessions[key(identity.wallet, identity.fullName)] ?: error("ISSUANCE_SESSION_REQUIRED")
        require(value.sessionId == identity.sessionId) { "ISSUANCE_IDENTITY_MISMATCH" }
        return value
    }

    @Synchronized
    fun start(wallet: String, avatarUri: String): IssuanceSession = save(newSession(wallet, avatarUri))

    @Synchronized
    fun beginRegister(wallet: String, avatarUri: String, operationId: String): IssuanceSession = save(
        newSession(wallet, avatarUri).copy(
            state = IssuanceState.REGISTER_READY,
            registerOperationId = operationId,
        ),
    )

    @Synchronized
    fun beginRegister(wallet: String, draft: PassDraft, operationId: String): IssuanceSession = save(
        IssuanceSession(
            wallet = CredentialValidation.requireAddress(wallet),
            fullName = draft.fullName,
            holder = CredentialValidation.requireNonZeroAddress(draft.recipient),
            expiry = draft.registrationExpiry,
            avatarUri = CredentialValidation.normalizeAvatar(draft.artworkUri),
            description = draft.description,
            state = IssuanceState.REGISTER_READY,
            registerOperationId = operationId,
            template = draft.template,
            accessActive = draft.accessActive,
            accessValidUntil = draft.accessValidUntil,
            transferable = draft.transferable,
            roleBitmap = draft.roleBitmap,
            allowedResources = draft.allowedResources,
        ),
    )

    private fun newSession(wallet: String, avatarUri: String) =
        IssuanceSession(
            wallet = CredentialValidation.requireAddress(wallet),
            fullName = IssuerSpace.fullName,
            holder = IssuerSpace.STAFF_HOLDER,
            expiry = BigInteger.valueOf(IssuerSpace.STAFF_EXPIRY),
            avatarUri = CredentialValidation.normalizeAvatar(avatarUri),
            description = IssuerSpace.DEFAULT_DESCRIPTION,
            state = IssuanceState.DRAFT,
        )

    @Synchronized
    fun registerReady(identity: IssuanceIdentity, operationId: String): IssuanceSession = update(identity, setOf(IssuanceState.DRAFT)) {
        it.copy(state = IssuanceState.REGISTER_READY, registerOperationId = operationId)
    }

    @Synchronized
    fun registerSubmitted(identity: IssuanceIdentity): IssuanceSession = update(identity, setOf(IssuanceState.REGISTER_READY, IssuanceState.REGISTER_SUBMITTED)) {
        it.copy(state = IssuanceState.REGISTER_SUBMITTED)
    }

    @Synchronized
    fun registerNotBroadcast(identity: IssuanceIdentity): IssuanceSession = update(identity, setOf(IssuanceState.REGISTER_SUBMITTED)) {
        it.copy(state = IssuanceState.DRAFT, registerOperationId = null)
    }

    @Synchronized
    fun registerConfirmed(identity: IssuanceIdentity): IssuanceSession {
        val current = get(identity)
        return when (current.state) {
            IssuanceState.REGISTER_SUBMITTED,
            IssuanceState.REGISTER_CONFIRMED,
            -> save(current.copy(state = IssuanceState.REGISTERED_CONFIGURING))
            IssuanceState.REGISTERED_CONFIGURING -> current
            else -> throw IllegalArgumentException("ILLEGAL_ISSUANCE_TRANSITION_${current.state}")
        }
    }

    @Synchronized
    fun recordsReady(identity: IssuanceIdentity, operationId: String, reviewed: IssuanceSession? = null): IssuanceSession =
        update(identity, setOf(IssuanceState.REGISTERED_CONFIGURING)) { current ->
            require(reviewed == null || reviewed.identity == identity) { "WRITE_INTENT_MISMATCH" }
            // Acknowledgment, revised validity and operation linkage are one persistence write.
            current.copy(state = IssuanceState.RECORDS_READY, recordsOperationId = operationId,
                acknowledgedOwner = reviewed?.configurationOwner ?: current.acknowledgedOwner,
                acknowledgedRoles = reviewed?.configurationRoles ?: current.acknowledgedRoles,
                acknowledgedTransferable = reviewed?.configurationTransferable ?: current.acknowledgedTransferable,
                accessValidUntil = reviewed?.accessValidUntil ?: current.accessValidUntil)
        }

    @Synchronized
    fun recordsSubmitted(identity: IssuanceIdentity): IssuanceSession = update(identity, setOf(IssuanceState.RECORDS_READY, IssuanceState.RECORDS_SUBMITTED)) {
        it.copy(state = IssuanceState.RECORDS_SUBMITTED)
    }

    @Synchronized
    fun recordsFailed(identity: IssuanceIdentity): IssuanceSession = update(identity, setOf(IssuanceState.RECORDS_READY, IssuanceState.RECORDS_SUBMITTED)) {
        it.copy(state = IssuanceState.REGISTERED_CONFIGURING)
    }

    @Synchronized
    fun recordsConfirmed(identity: IssuanceIdentity): IssuanceSession = update(identity, setOf(IssuanceState.RECORDS_SUBMITTED)) {
        it.copy(state = IssuanceState.RECORDS_CONFIRMED)
    }

    @Synchronized
    fun beginReadback(identity: IssuanceIdentity): IssuanceSession {
        val current = get(identity)
        return when (current.state) {
            IssuanceState.RECORDS_CONFIRMED -> save(current.copy(state = IssuanceState.AUTHORITATIVE_READBACK))
            IssuanceState.AUTHORITATIVE_READBACK,
            IssuanceState.READY,
            -> current
            else -> throw IllegalArgumentException("ILLEGAL_ISSUANCE_TRANSITION_${current.state}")
        }
    }

    @Synchronized
    fun ready(identity: IssuanceIdentity): IssuanceSession {
        val current = get(identity)
        return when (current.state) {
            IssuanceState.AUTHORITATIVE_READBACK -> save(current.copy(state = IssuanceState.READY))
            IssuanceState.READY -> current
            else -> throw IllegalArgumentException("ILLEGAL_ISSUANCE_TRANSITION_${current.state}")
        }
    }

    /** Journal -> product reconciliation. Never infers broadcast outcome from product state. */
    @Synchronized
    fun reconcile(identity: IssuanceIdentity, operation: PersistedTransactionOperation): IssuanceSession {
        val value = get(identity)
        val records = operation.operationId == value.recordsOperationId
        require(records || operation.operationId == value.registerOperationId) { "ISSUANCE_OPERATION_MISMATCH" }
        require(operation.walletAddress.equals(value.wallet, true) && operation.chainId == identity.chainId) { "ISSUANCE_OPERATION_MISMATCH" }
        val action = if (records) StudioActionType.ISSUE_CONFIGURE else StudioActionType.ISSUE_REGISTER
        val legacy = if (records) ContractTransactionRunner.RECORDS_OPERATION else ContractTransactionRunner.REGISTER_OPERATION
        require(operation.operationType == StudioOperationIdentity.type(action, value.fullName) ||
            (value.fullName == IssuerSpace.fullName && operation.operationType == legacy)) { "ISSUANCE_OPERATION_MISMATCH" }
        require(operation.targetAddress.equals(if (records) IssuerSpace.resolver else IssuerSpace.registry, true)) { "ISSUANCE_OPERATION_MISMATCH" }
        val calldata = if (records) CredentialConfigurationPolicy.calldata(value) else CredentialAbi.register(
            value.fullName.removeSuffix(".${IssuerSpace.namespace}"), value.holder, IssuerSpace.resolver, value.expiry, value.roleBitmap)
        require(operation.valueWei == "0" && operation.dataSummary == CredentialAbi.calldataFingerprint(calldata)) { "WRITE_INTENT_MISMATCH" }
        // A later phase must not be rolled back by an idempotent TX1 callback.
        if (!records && value.state in setOf(IssuanceState.RECORDS_READY, IssuanceState.RECORDS_SUBMITTED,
                IssuanceState.RECORDS_CONFIRMED, IssuanceState.AUTHORITATIVE_READBACK, IssuanceState.READY)) return value
        val next = when (operation.state) {
            TransactionOperationState.CONFIRMED -> if (records) {
                if (value.state == IssuanceState.READY) IssuanceState.READY else IssuanceState.AUTHORITATIVE_READBACK
            } else IssuanceState.REGISTERED_CONFIGURING
            TransactionOperationState.REVERTED, TransactionOperationState.CANCELLED,
            TransactionOperationState.NO_BROADCAST_PROVEN -> if (records) IssuanceState.REGISTERED_CONFIGURING else IssuanceState.DRAFT
            TransactionOperationState.DRAFT, TransactionOperationState.READY_TO_REVIEW,
            TransactionOperationState.READY_TO_SUBMIT, TransactionOperationState.SUBMISSION_CLAIMED ->
                if (records) IssuanceState.RECORDS_READY else IssuanceState.REGISTER_READY
            else -> if (records) IssuanceState.RECORDS_SUBMITTED else IssuanceState.REGISTER_SUBMITTED
        }
        return if (next == value.state) value else save(value.copy(state = next))
    }

    fun recoveryAction(value: IssuanceSession, register: PersistedTransactionOperation?,
        records: PersistedTransactionOperation?): IssuanceRecoveryAction = when {
        value.state == IssuanceState.READY -> IssuanceRecoveryAction.DISPLAY_READY
        records?.state == TransactionOperationState.CONFIRMED -> IssuanceRecoveryAction.READBACK
        records != null && records.state !in retryable -> IssuanceRecoveryAction.RECOVER_RECORDS
        register?.state == TransactionOperationState.CONFIRMED -> IssuanceRecoveryAction.RESUME_RECORDS
        register != null && register.state !in retryable -> IssuanceRecoveryAction.RECOVER_REGISTER
        else -> IssuanceRecoveryAction.NONE
    }

    private fun update(identity: IssuanceIdentity, allowed: Set<IssuanceState>, change: (IssuanceSession) -> IssuanceSession): IssuanceSession {
        val current = get(identity)
        require(current.state in allowed) { "ILLEGAL_ISSUANCE_TRANSITION" }
        return save(change(current))
    }

    private fun save(value: IssuanceSession): IssuanceSession {
        val next = LinkedHashMap(sessions)
        next[key(value.wallet, value.fullName)] = value
        store.save(next.values.joinToString("\n", transform = ::encode))
        sessions.clear()
        sessions.putAll(next)
        return value
    }

    private val retryable = setOf(TransactionOperationState.REVERTED, TransactionOperationState.CANCELLED,
        TransactionOperationState.NO_BROADCAST_PROVEN)

    private fun key(wallet: String, fullName: String) = "${wallet.lowercase()}|${fullName.lowercase()}"

    private fun encode(value: IssuanceSession) = listOf(
        value.wallet, value.fullName, value.holder, value.expiry.toString(), value.avatarUri,
        value.description, value.state.name, value.registerOperationId.orEmpty(), value.recordsOperationId.orEmpty(),
        value.template.name, value.accessActive.toString(), value.accessValidUntil.toString(),
        value.transferable.toString(), value.roleBitmap.toString(), value.sessionId,
        value.acknowledgedOwner.orEmpty(), value.acknowledgedRoles?.toString().orEmpty(),
        value.acknowledgedTransferable?.toString().orEmpty(),
        value.allowedResources?.let(AccessResources::encode).orEmpty(),
    ).joinToString("|") { escape(it) }

    private fun decodeAll(raw: String?): List<IssuanceSession> = raw.orEmpty().lineSequence()
        .filter(String::isNotBlank)
        .mapNotNull(::decode)
        .toList()

    private fun decode(raw: String): IssuanceSession? {
        val fields = raw.split('|').map(::unescape)
        if (fields.size !in setOf(9, 14, 18, 19)) return null
        return runCatching {
            IssuanceSession(
                fields[0], fields[1], fields[2], BigInteger(fields[3]), fields[4], fields[5],
                IssuanceState.valueOf(fields[6]), fields[7].ifBlank { null }, fields[8].ifBlank { null },
                template = fields.getOrNull(9)?.let(PassTemplate::valueOf) ?: PassTemplate.STAFF,
                accessActive = fields.getOrNull(10)?.toBooleanStrictOrNull() ?: true,
                accessValidUntil = fields.getOrNull(11)?.let(::BigInteger) ?: BigInteger(fields[3]),
                transferable = fields.getOrNull(12)?.toBooleanStrictOrNull() ?: false,
                roleBitmap = fields.getOrNull(13)?.let(::BigInteger) ?: BigInteger.ZERO,
                sessionId = fields.getOrNull(14) ?: "legacy:${fields[0].lowercase()}:${fields[1]}:${fields[7]}",
                acknowledgedOwner = fields.getOrNull(15)?.takeIf(String::isNotBlank),
                acknowledgedRoles = fields.getOrNull(16)?.takeIf(String::isNotBlank)?.let(::BigInteger),
                acknowledgedTransferable = fields.getOrNull(17)?.takeIf(String::isNotBlank)?.toBooleanStrict(),
                allowedResources = fields.getOrNull(18)?.takeIf(String::isNotBlank)?.let(AccessResources::decode),
            )
        }.getOrNull()
    }
}

private fun escape(value: String) = value.replace("%", "%25").replace("|", "%7C").replace("\n", "%0A")
private fun unescape(value: String) = value.replace("%0A", "\n").replace("%7C", "|").replace("%25", "%")
