package io.github.nodexbit.ethonline2026

import java.math.BigInteger
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.ResolverStyle
import java.util.UUID

data class StudioWalletBinding(val address: String, val providerId: String, val generation: Long)
data class StudioWriteLease(val id: String, val wallet: StudioWalletBinding)

/** Construct alongside the captured wallet/provider, never from a later selection. */
data class StudioRunnerIdentity(val wallet: StudioWalletBinding, val chainId: Long)

class StudioOperationConflict(val operation: PersistedTransactionOperation) :
    IllegalStateException("ANOTHER_PASS_UNFINISHED")

/** Only the operation explicitly reviewed for cancellation and its exact links may change. */
class StudioPreparedOperationResolver(
    private val engine: RecoverableTransactionEngine,
    private val issuance: IssuanceCoordinator,
    private val management: ManagementSessionCoordinator,
) {
    fun cancel(reviewed: PersistedTransactionOperation) {
        val cancelled = engine.cancelPreparedOperation(reviewed)
        issuance.list(reviewed.walletAddress).filter {
            it.registerOperationId == reviewed.operationId || it.recordsOperationId == reviewed.operationId
        }.forEach { issuance.reconcile(it.identity, cancelled) }
        management.pending(reviewed.walletAddress).filter { it.operationId == reviewed.operationId }
            .forEach { management.clear(it.operationId) }
    }
}

/** A single process owns wallet selection and the complete final-confirmation lifetime. */
class StudioWriteCoordinator {
    private var wallet: StudioWalletBinding? = null
    private var generation = 0L
    private var lease: StudioWriteLease? = null
    private val observers = mutableSetOf<() -> Unit>()
    @Synchronized fun observe(observer: () -> Unit): () -> Unit {
        observers += observer
        observer()
        return { synchronized(this) { observers -= observer } }
    }
    private fun changed() { observers.toList().forEach { runCatching { it() } } }
    @Synchronized fun currentWallet(): StudioWalletBinding? = wallet
    @Synchronized fun isBusy(): Boolean = lease != null
    @Synchronized fun select(address: String, providerId: String): StudioWalletBinding {
        if (wallet?.address.equals(address, true) && wallet?.providerId == providerId) return wallet!!
        check(lease == null) { "WRITE_IN_PROGRESS" }
        return StudioWalletBinding(address, providerId, ++generation).also { wallet = it }
    }
    @Synchronized fun clearWallet() {
        check(lease == null) { "WRITE_IN_PROGRESS" }
        wallet = null
        generation++
    }
    @Synchronized fun acquire(binding: StudioWalletBinding): StudioWriteLease {
        check(lease == null) { "WRITE_IN_PROGRESS" }
        check(wallet == binding) { "WALLET_CHANGED_REVIEW_AGAIN" }
        return StudioWriteLease(UUID.randomUUID().toString(), binding).also { lease = it; changed() }
    }
    @Synchronized fun requireCurrent(value: StudioWriteLease) {
        check(lease == value && wallet == value.wallet) { "WALLET_CHANGED_REVIEW_AGAIN" }
    }
    @Synchronized fun requireActiveWallet(binding: StudioWalletBinding) {
        check(lease?.wallet == binding && wallet == binding) { "WALLET_CHANGED_REVIEW_AGAIN" }
    }
    @Synchronized fun release(value: StudioWriteLease): Boolean {
        if (lease != value) return false
        lease = null
        changed()
        return true
    }
}

data class StudioWriteIntent(
    val operationId: String,
    val sessionId: String,
    val wallet: StudioWalletBinding,
    val chainId: Long,
    val credential: String,
    val action: String,
    val target: String,
    val value: String,
    val calldataFingerprint: String,
    val reviewedBusinessIntent: Any,
)

data class StudioWritePermit(
    val coordinator: StudioWriteCoordinator,
    val lease: StudioWriteLease,
    val intent: StudioWriteIntent,
) {
    fun requireRunner(runner: StudioRunnerIdentity) {
        coordinator.requireCurrent(lease)
        check(intent.wallet == lease.wallet && runner.wallet == lease.wallet &&
            runner.chainId == intent.chainId && runner.chainId == IssuerSpace.chainId) {
            "WALLET_CHANGED_REVIEW_AGAIN"
        }
    }

    fun requireRequest(operationId: String, request: ContractTransactionRequest) {
        coordinator.requireCurrent(lease)
        val credential = if (request.operationType in setOf(ContractTransactionRunner.REGISTER_OPERATION,
                ContractTransactionRunner.RECORDS_OPERATION)) IssuerSpace.fullName else request.operationType.substringAfter(':')
        check(intent.wallet == lease.wallet && intent.operationId == operationId &&
            intent.sessionId.isNotBlank() && intent.credential == credential &&
            intent.wallet.address.equals(request.from, true) && intent.chainId == IssuerSpace.chainId &&
            intent.action == request.operationType && intent.target.equals(request.to, true) &&
            intent.value == "0" && intent.calldataFingerprint == CredentialAbi.calldataFingerprint(request.data)
        ) { "WRITE_INTENT_MISMATCH" }
    }
}

/** Shared stores prevent an old Activity's late hash being overwritten by a new Activity's cache. */
object StudioRuntime {
    val writes = StudioWriteCoordinator()
    private var engine: RecoverableTransactionEngine? = null
    private var issuance: IssuanceCoordinator? = null
    private var management: ManagementSessionCoordinator? = null
    @Synchronized fun engine(create: () -> RecoverableTransactionEngine) = engine ?: create().also { engine = it }
    @Synchronized fun issuance(create: () -> IssuanceCoordinator) = issuance ?: create().also { issuance = it }
    @Synchronized fun management(create: () -> ManagementSessionCoordinator) = management ?: create().also { management = it }
}

object StudioTime {
    val zone: ZoneId = ZoneId.of("Europe/Madrid")
    private val input = DateTimeFormatter.ofPattern("uuuu-MM-dd HH:mm").withResolverStyle(ResolverStyle.STRICT)
    private val display = DateTimeFormatter.ofPattern("d MMM uuuu, HH:mm:ss")
    fun parse(value: String): BigInteger {
        val local = try { LocalDateTime.parse(value.trim(), input) } catch (_: Exception) {
            throw IllegalArgumentException("INVALID_CALENDAR_DATE")
        }
        val offsets = zone.rules.getValidOffsets(local)
        require(offsets.isNotEmpty()) { "DST_GAP_CHOOSE_ANOTHER_TIME" }
        require(offsets.size == 1) { "DST_OVERLAP_CHOOSE_ANOTHER_TIME" }
        return BigInteger.valueOf(local.toEpochSecond(offsets.single()))
    }
    fun input(value: BigInteger): String = input.format(Instant.ofEpochSecond(value.longValueExact()).atZone(zone))
    fun describe(value: BigInteger?): String = value?.let {
        val instant = Instant.ofEpochSecond(it.longValueExact())
        "${display.format(instant.atZone(zone))} Europe/Madrid (UTC: $instant)"
    } ?: "Not set"
}

object StudioAccessPolicy {
    fun status(value: CredentialSnapshot): String = when {
        value.readStatus != CredentialReadStatus.FRESH || !value.provenanceMatches ||
            value.snapshotTimestamp == null -> "Unavailable"
        value.status != CredentialRegistryStatus.REGISTERED || value.registryExpiry == null ||
            value.registryExpiry <= value.snapshotTimestamp -> "Registration expired"
        value.accessActive == null || value.accessValidUntil == null -> "Not configured"
        value.accessValidUntil <= value.snapshotTimestamp -> "Access expired"
        value.accessActive -> "Allowed"
        else -> "Suspended"
    }
    fun requireRegistered(value: CredentialSnapshot) {
        require(value.readStatus == CredentialReadStatus.FRESH && value.provenanceMatches &&
            value.status == CredentialRegistryStatus.REGISTERED && value.snapshotTimestamp != null &&
            value.registryExpiry != null && value.registryExpiry > value.snapshotTimestamp &&
            value.registry.equals(IssuerSpace.registry, true) && value.resolver.equals(IssuerSpace.resolver, true) &&
            value.subregistry.equals(IssuerSpace.ZERO_ADDRESS, true)) { "PASS_STATE_UNAVAILABLE" }
    }
}

object StudioConfigurationPolicy {
    fun review(session: IssuanceSession, current: CredentialSnapshot,
        validUntil: BigInteger = session.accessValidUntil): IssuanceSession {
        StudioAccessPolicy.requireRegistered(current)
        require(current.fullName == session.fullName && current.registryExpiry == session.expiry) { "CREDENTIAL_CHANGED_AFTER_REGISTRATION" }
        require(validUntil > checkNotNull(current.snapshotTimestamp) && validUntil <= checkNotNull(current.registryExpiry)) { "ACCESS_EXPIRED_REVIEW_AGAIN" }
        return session.copy(acknowledgedOwner = checkNotNull(current.owner),
            acknowledgedRoles = checkNotNull(current.ownerRoleBitmap),
            acknowledgedTransferable = checkNotNull(current.transferable), accessValidUntil = validUntil)
    }
    fun requireAcknowledged(session: IssuanceSession, current: CredentialSnapshot) {
        val fresh = review(session, current)
        require(fresh.configurationOwner.equals(session.configurationOwner, true) &&
            fresh.configurationRoles == session.configurationRoles &&
            fresh.configurationTransferable == session.configurationTransferable) { "CREDENTIAL_CHANGED_AFTER_REGISTRATION" }
    }
}

object StudioManagementPreflight {
    fun validate(reviewed: CredentialSnapshot, mutation: ManagementMutation, current: CredentialSnapshot,
        namespaceExpiry: BigInteger) {
        StudioAccessPolicy.requireRegistered(current)
        require(current.fullName == reviewed.fullName && current.snapshotBlock != null && reviewed.snapshotBlock != null &&
            current.snapshotBlock >= reviewed.snapshotBlock && current.owner.equals(reviewed.owner, true) &&
            current.ownerRoleBitmap == reviewed.ownerRoleBitmap) { "CREDENTIAL_CHANGED_REVIEW_AGAIN" }
        val rebuilt = when (mutation.action) {
            StudioActionType.RESOURCE_POLICY_UPDATE -> {
                require(current.resourcesRaw == reviewed.resourcesRaw) { "CREDENTIAL_CHANGED_REVIEW_AGAIN" }
                PassManagementPolicy.resources(current, checkNotNull(mutation.expectedResources))
            }
            StudioActionType.ACCESS_SUSPEND, StudioActionType.ACCESS_RESTORE, StudioActionType.ACCESS_VALIDITY -> {
                require(current.accessActive == reviewed.accessActive && current.accessValidUntil == reviewed.accessValidUntil) { "CREDENTIAL_CHANGED_REVIEW_AGAIN" }
                PassManagementPolicy.access(current, checkNotNull(mutation.expectedAccessActive),
                    checkNotNull(mutation.expectedAccessValidUntil), checkNotNull(current.snapshotTimestamp))
            }
            StudioActionType.PRESENTATION_UPDATE -> {
                require(current.description == reviewed.description && current.avatarUri == reviewed.avatarUri) { "CREDENTIAL_CHANGED_REVIEW_AGAIN" }
                PassManagementPolicy.presentation(current,
                    mutation.expectedDescription?.takeIf { it != current.description.orEmpty() },
                    mutation.expectedArtwork?.takeIf { it.isNotEmpty() && it != current.avatarUri.orEmpty() },
                    mutation.expectedArtwork == "" && current.avatarUri.orEmpty().isNotEmpty())
            }
            StudioActionType.REGISTRATION_RENEW -> {
                require(current.registryExpiry == reviewed.registryExpiry && current.accessValidUntil == reviewed.accessValidUntil) { "CREDENTIAL_CHANGED_REVIEW_AGAIN" }
                PassManagementPolicy.renew(current, checkNotNull(mutation.expectedRegistrationExpiry), namespaceExpiry)
            }
            else -> error("WRITE_INTENT_MISMATCH")
        }
        require(rebuilt.action == mutation.action && rebuilt.target == mutation.target && rebuilt.calldata == mutation.calldata) { "WRITE_INTENT_MISMATCH" }
    }
}

/** No untrusted message or exception-class text is retained as a diagnostic. */
object StudioErrors {
    private val codes = setOf(
        "WRITE_IN_PROGRESS", "WALLET_CHANGED_REVIEW_AGAIN", "WRITE_INTENT_MISMATCH",
        "PENDING_TRANSACTION", "WRONG_CHAIN", "ACTIVE_OPERATION_EXISTS", "MANAGEMENT_FINALIZATION_PENDING",
        "ANOTHER_PASS_UNFINISHED", "REVIEW_REQUIRED",
        "CREDENTIAL_CHANGED_REVIEW_AGAIN", "CREDENTIAL_CHANGED_AFTER_REGISTRATION", "ACCESS_EXPIRED_REVIEW_AGAIN",
        "INVALID_CALENDAR_DATE", "DST_GAP_CHOOSE_ANOTHER_TIME", "DST_OVERLAP_CHOOSE_ANOTHER_TIME",
        "INVALID_RECIPIENT", "INVALID_RECIPIENT_CHECKSUM", "ZERO_RECIPIENT", "INVALID_CREDENTIAL_NAME",
        "ACCESS_VALIDITY_AFTER_REGISTRATION", "EXPIRED_ACCESS_CANNOT_RESTORE", "EXPIRY_OUTSIDE_NAMESPACE",
        "REGISTRATION_EXPIRY_MUST_INCREASE", "NO_PRESENTATION_CHANGE", "USE_REMOVE_ARTWORK",
        "MANAGEMENT_READBACK_UNAVAILABLE", "READBACK_BEHIND_CONFIRMED_RECEIPT", "READBACK_UNAVAILABLE",
        "TRANSACTION_HASH_MISMATCH", "TRANSACTION_FROM_MISMATCH", "TRANSACTION_TO_MISMATCH",
        "TRANSACTION_VALUE_MISMATCH", "TRANSACTION_DATA_MISMATCH", "TRANSACTION_NONCE_MISMATCH",
        "RECEIPT_HASH_MISMATCH", "RECEIPT_BLOCK_MISMATCH", "RECEIPT_STATUS_INVALID",
        "RECEIPT_FROM_MISMATCH", "RECEIPT_TO_MISMATCH", "ILLEGAL_ISSUANCE_TRANSITION",
        "UNKNOWN_TRANSACTION_RESULT", "PASS_STATE_UNAVAILABLE", "CREDENTIAL_NOT_AVAILABLE",
    )
    fun category(error: Throwable): String = error.message?.takeIf(codes::contains) ?: when (error) {
        is ReadOnlyRpcException -> "RPC_UNAVAILABLE"
        is IllegalArgumentException -> "VALIDATION_FAILED"
        is IllegalStateException -> "STATE_UNAVAILABLE"
        else -> "PROVIDER_OR_OPERATION_UNAVAILABLE"
    }
    fun exceptionClass(error: Throwable): String = when (error) {
        is IllegalArgumentException -> "IllegalArgumentException"
        is IllegalStateException -> "IllegalStateException"
        is ReadOnlyRpcException -> "ReadOnlyRpcException"
        else -> "Exception"
    }
}
