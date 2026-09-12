package io.github.nodexbit.ethonline2026

import java.math.BigInteger

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
)

enum class IssuanceRecoveryAction { NONE, RECOVER_REGISTER, RESUME_RECORDS, RECOVER_RECORDS, READBACK, DISPLAY_READY }

class IssuanceCoordinator(private val store: LoadableStringStateStore) {
    private var session: IssuanceSession? = decode(store.load())

    @Synchronized
    fun current(wallet: String): IssuanceSession? = session?.takeIf { it.wallet.equals(wallet, true) }

    @Synchronized
    fun start(wallet: String, avatarUri: String): IssuanceSession = save(newSession(wallet, avatarUri))

    @Synchronized
    fun beginRegister(wallet: String, avatarUri: String, operationId: String): IssuanceSession = save(
        newSession(wallet, avatarUri).copy(
            state = IssuanceState.REGISTER_READY,
            registerOperationId = operationId,
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
    fun registerReady(operationId: String): IssuanceSession = update(setOf(IssuanceState.DRAFT)) {
        it.copy(state = IssuanceState.REGISTER_READY, registerOperationId = operationId)
    }

    @Synchronized
    fun registerSubmitted(): IssuanceSession = update(setOf(IssuanceState.REGISTER_READY, IssuanceState.REGISTER_SUBMITTED)) {
        it.copy(state = IssuanceState.REGISTER_SUBMITTED)
    }

    @Synchronized
    fun registerConfirmed(): IssuanceSession {
        val current = checkNotNull(session) { "ISSUANCE_SESSION_REQUIRED" }
        return when (current.state) {
            IssuanceState.REGISTER_SUBMITTED,
            IssuanceState.REGISTER_CONFIRMED,
            -> save(current.copy(state = IssuanceState.REGISTERED_CONFIGURING))
            IssuanceState.REGISTERED_CONFIGURING -> current
            else -> throw IllegalArgumentException("ILLEGAL_ISSUANCE_TRANSITION_${current.state}")
        }
    }

    @Synchronized
    fun recordsReady(operationId: String): IssuanceSession = update(setOf(IssuanceState.REGISTERED_CONFIGURING)) {
        it.copy(state = IssuanceState.RECORDS_READY, recordsOperationId = operationId)
    }

    @Synchronized
    fun recordsSubmitted(): IssuanceSession = update(setOf(IssuanceState.RECORDS_READY, IssuanceState.RECORDS_SUBMITTED)) {
        it.copy(state = IssuanceState.RECORDS_SUBMITTED)
    }

    @Synchronized
    fun recordsFailed(): IssuanceSession = update(setOf(IssuanceState.RECORDS_READY, IssuanceState.RECORDS_SUBMITTED)) {
        it.copy(state = IssuanceState.REGISTERED_CONFIGURING)
    }

    @Synchronized
    fun recordsConfirmed(): IssuanceSession = update(setOf(IssuanceState.RECORDS_SUBMITTED)) {
        it.copy(state = IssuanceState.RECORDS_CONFIRMED)
    }

    @Synchronized
    fun beginReadback(): IssuanceSession {
        val current = checkNotNull(session) { "ISSUANCE_SESSION_REQUIRED" }
        return when (current.state) {
            IssuanceState.RECORDS_CONFIRMED -> save(current.copy(state = IssuanceState.AUTHORITATIVE_READBACK))
            IssuanceState.AUTHORITATIVE_READBACK,
            IssuanceState.READY,
            -> current
            else -> throw IllegalArgumentException("ILLEGAL_ISSUANCE_TRANSITION_${current.state}")
        }
    }

    @Synchronized
    fun ready(): IssuanceSession {
        val current = checkNotNull(session) { "ISSUANCE_SESSION_REQUIRED" }
        return when (current.state) {
            IssuanceState.AUTHORITATIVE_READBACK -> save(current.copy(state = IssuanceState.READY))
            IssuanceState.READY -> current
            else -> throw IllegalArgumentException("ILLEGAL_ISSUANCE_TRANSITION_${current.state}")
        }
    }

    fun recoveryAction(
        value: IssuanceSession,
        register: PersistedTransactionOperation?,
        records: PersistedTransactionOperation?,
    ): IssuanceRecoveryAction = when {
        value.state == IssuanceState.READY -> IssuanceRecoveryAction.DISPLAY_READY
        value.state == IssuanceState.RECORDS_SUBMITTED && records?.state == TransactionOperationState.CONFIRMED ->
            IssuanceRecoveryAction.READBACK
        value.state == IssuanceState.REGISTER_SUBMITTED && register?.state == TransactionOperationState.CONFIRMED ->
            IssuanceRecoveryAction.RESUME_RECORDS
        value.state == IssuanceState.REGISTER_CONFIRMED && register?.state == TransactionOperationState.CONFIRMED ->
            IssuanceRecoveryAction.RESUME_RECORDS
        value.state == IssuanceState.RECORDS_CONFIRMED || value.state == IssuanceState.AUTHORITATIVE_READBACK ->
            IssuanceRecoveryAction.READBACK
        value.state == IssuanceState.RECORDS_SUBMITTED && records?.txHash != null -> IssuanceRecoveryAction.RECOVER_RECORDS
        value.state == IssuanceState.REGISTERED_CONFIGURING -> IssuanceRecoveryAction.RESUME_RECORDS
        value.state == IssuanceState.REGISTER_SUBMITTED && register?.txHash != null -> IssuanceRecoveryAction.RECOVER_REGISTER
        else -> IssuanceRecoveryAction.NONE
    }

    private fun update(allowed: Set<IssuanceState>, change: (IssuanceSession) -> IssuanceSession): IssuanceSession {
        val current = checkNotNull(session) { "ISSUANCE_SESSION_REQUIRED" }
        require(current.state in allowed) { "ILLEGAL_ISSUANCE_TRANSITION_${current.state}" }
        return save(change(current))
    }

    private fun save(value: IssuanceSession): IssuanceSession {
        session = value
        store.save(encode(value))
        return value
    }

    private fun encode(value: IssuanceSession) = listOf(
        value.wallet, value.fullName, value.holder, value.expiry.toString(), value.avatarUri,
        value.description, value.state.name, value.registerOperationId.orEmpty(), value.recordsOperationId.orEmpty(),
    ).joinToString("|") { escape(it) }

    private fun decode(raw: String?): IssuanceSession? {
        if (raw.isNullOrBlank()) return null
        val fields = raw.split('|').map(::unescape)
        if (fields.size != 9) return null
        return runCatching {
            IssuanceSession(
                fields[0], fields[1], fields[2], BigInteger(fields[3]), fields[4], fields[5],
                IssuanceState.valueOf(fields[6]), fields[7].ifBlank { null }, fields[8].ifBlank { null },
            )
        }.getOrNull()
    }
}

private fun escape(value: String) = value.replace("%", "%25").replace("|", "%7C").replace("\n", "%0A")
private fun unescape(value: String) = value.replace("%0A", "\n").replace("%7C", "|").replace("%25", "%")
