package io.github.nodexbit.ethonline2026

import java.math.BigInteger
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Base64

/** Product capabilities are independent; Studio is not an account classification. */
data class StudioCapabilities(
    val canIssue: IssuerCapabilityState,
    val canRenew: IssuerCapabilityState,
    val canManageAccess: IssuerCapabilityState,
    val canManagePresentation: IssuerCapabilityState,
    val category: String,
    val snapshotBlock: BigInteger? = null,
    val namespaceExpiry: BigInteger? = null,
) {
    val visible: Boolean get() = listOf(canIssue, canRenew, canManageAccess, canManagePresentation)
        .any { it == IssuerCapabilityState.ALLOWED }

    companion object {
        fun unavailable(category: String) = StudioCapabilities(
            IssuerCapabilityState.UNAVAILABLE,
            IssuerCapabilityState.UNAVAILABLE,
            IssuerCapabilityState.UNAVAILABLE,
            IssuerCapabilityState.UNAVAILABLE,
            category,
        )
    }
}

object StudioRoles {
    val REGISTRAR: BigInteger = BigInteger.ONE.shiftLeft(0)
    val RENEW: BigInteger = BigInteger.ONE.shiftLeft(16)
    val SET_TEXT: BigInteger = BigInteger.ONE.shiftLeft(4)
    val SET_DATA: BigInteger = BigInteger.ONE.shiftLeft(36)

    // RegistryRolesLib.ROLE_CAN_TRANSFER_ADMIN is the admin-only bit at 28 + 128.
    val CAN_TRANSFER_ADMIN: BigInteger = BigInteger.ONE.shiftLeft(156)
}

object StudioCapabilityPolicy {
    fun evaluate(
        registryRoles: BigInteger,
        resolverRoles: BigInteger,
        provenanceMatches: Boolean,
        namespaceStatus: BigInteger,
        namespaceOwner: String,
        namespaceRegistry: String,
        namespaceResolver: String,
        namespaceExpiry: BigInteger,
        now: BigInteger,
        blockNumber: BigInteger,
    ): StudioCapabilities {
        val invalid = when {
            !provenanceMatches -> "PROVENANCE_MISMATCH"
            namespaceStatus != BigInteger.TWO -> "NAMESPACE_NOT_REGISTERED"
            namespaceOwner.equals(IssuerSpace.ZERO_ADDRESS, true) -> "NAMESPACE_OWNER_MISSING"
            !namespaceRegistry.equals(IssuerSpace.registry, true) ||
                !namespaceResolver.equals(IssuerSpace.resolver, true) -> "NAMESPACE_TARGET_MISMATCH"
            namespaceExpiry <= now -> "NAMESPACE_EXPIRED"
            else -> null
        }
        if (invalid != null) {
            return StudioCapabilities(
                IssuerCapabilityState.DENIED,
                IssuerCapabilityState.DENIED,
                IssuerCapabilityState.DENIED,
                IssuerCapabilityState.DENIED,
                invalid,
                blockNumber,
                namespaceExpiry,
            )
        }
        fun state(bitmap: BigInteger, role: BigInteger) = if (bitmap.and(role) == role) {
            IssuerCapabilityState.ALLOWED
        } else {
            IssuerCapabilityState.DENIED
        }
        return StudioCapabilities(
            canIssue = state(registryRoles, StudioRoles.REGISTRAR),
            canRenew = state(registryRoles, StudioRoles.RENEW),
            canManageAccess = state(resolverRoles, StudioRoles.SET_DATA),
            canManagePresentation = state(resolverRoles, StudioRoles.SET_TEXT),
            category = "CONFIRMED_ONCHAIN",
            snapshotBlock = blockNumber,
            namespaceExpiry = namespaceExpiry,
        )
    }
}

enum class PassTemplate(val title: String) { STAFF("STAFF"), VISITOR("VISITOR"), CONTRACTOR("CONTRACTOR") }

data class PassDraft(
    val template: PassTemplate,
    val label: String,
    val recipient: String,
    val registrationExpiry: BigInteger,
    val accessActive: Boolean,
    val accessValidUntil: BigInteger,
    val transferable: Boolean,
    val description: String,
    val artworkUri: String,
    val allowedResources: Set<String> = AccessResources.defaults(template),
) {
    val normalizedLabel: String get() = CredentialValidation.normalizeLabel(label)
    val fullName: String get() = "$normalizedLabel.${IssuerSpace.namespace}"
    val roleBitmap: BigInteger get() = if (transferable) StudioRoles.CAN_TRANSFER_ADMIN else BigInteger.ZERO

    fun validated(now: BigInteger, namespaceExpiry: BigInteger): PassDraft {
        CredentialValidation.normalizeLabel(label)
        AccessResources.encode(allowedResources)
        val normalizedRecipient = CredentialValidation.requireNonZeroAddress(recipient)
        CredentialValidation.validateExpiry(registrationExpiry, now, namespaceExpiry)
        require(accessValidUntil > now) { "ACCESS_VALIDITY_NOT_FUTURE" }
        require(accessValidUntil.bitLength() <= 64) { "ACCESS_VALIDITY_OUTSIDE_UINT64" }
        require(accessValidUntil <= registrationExpiry) { "ACCESS_VALIDITY_AFTER_REGISTRATION" }
        require(description.length <= 1_024 && !description.any { it.code < 0x20 && it != '\n' && it != '\t' }) {
            "INVALID_DESCRIPTION"
        }
        return copy(
            label = normalizedLabel,
            recipient = normalizedRecipient,
            description = description.trim(),
            artworkUri = CredentialValidation.normalizeAvatar(artworkUri),
        )
    }
}

object PassTemplateDefaults {
    private val DAY = BigInteger.valueOf(86_400)

    fun apply(template: PassTemplate, recipient: String, now: BigInteger, namespaceExpiry: BigInteger): PassDraft {
        val requestedDuration = when (template) {
            PassTemplate.VISITOR -> DAY
            PassTemplate.STAFF, PassTemplate.CONTRACTOR -> DAY.multiply(BigInteger.valueOf(30))
        }
        val expiry = (now + requestedDuration).min(namespaceExpiry - BigInteger.valueOf(60))
        require(expiry > now) { "NAMESPACE_EXPIRY_TOO_CLOSE" }
        return PassDraft(
            template = template,
            label = when (template) {
                PassTemplate.STAFF -> "staff-new"
                PassTemplate.VISITOR -> "visitor-new"
                PassTemplate.CONTRACTOR -> "contractor-new"
            },
            recipient = recipient,
            registrationExpiry = expiry,
            accessActive = template != PassTemplate.CONTRACTOR,
            accessValidUntil = expiry,
            transferable = template == PassTemplate.VISITOR,
            description = when (template) {
                PassTemplate.STAFF -> "Staff Access Pass"
                PassTemplate.VISITOR -> "Visitor Pass"
                PassTemplate.CONTRACTOR -> "Contractor Access Pass"
            },
            artworkUri = "",
        )
    }
}

data class PassReviewPresentation(
    val credential: String,
    val recipientCompact: String,
    val recipientExact: String,
    val registrationLocal: String,
    val registrationUtc: String,
    val access: String,
    val accessLocal: String,
    val accessUtc: String,
    val transferability: String,
    val description: String,
    val artwork: String,
)

object PassReviewPolicy {
    private val local = DateTimeFormatter.ofPattern("d MMM uuuu, HH:mm:ss z")

    fun present(draft: PassDraft, zone: ZoneId = StudioTime.zone): PassReviewPresentation {
        fun localTime(value: BigInteger) = local.format(Instant.ofEpochSecond(value.longValueExact()).atZone(zone)) + " ${zone.id}"
        fun utc(value: BigInteger) = Instant.ofEpochSecond(value.longValueExact()).toString()
        return PassReviewPresentation(
            credential = draft.fullName,
            recipientCompact = ProductShellPolicy.compactAddress(draft.recipient),
            recipientExact = CredentialValidation.checksumAddress(draft.recipient),
            registrationLocal = localTime(draft.registrationExpiry),
            registrationUtc = utc(draft.registrationExpiry),
            access = if (draft.accessActive) "Allowed" else "Suspended",
            accessLocal = localTime(draft.accessValidUntil),
            accessUtc = utc(draft.accessValidUntil),
            transferability = if (draft.transferable) "Transferable" else "Non-transferable",
            description = draft.description,
            artwork = draft.artworkUri.ifBlank { "Not set" },
        )
    }
}

enum class StudioActionType {
    ISSUE_REGISTER,
    ISSUE_CONFIGURE,
    ACCESS_SUSPEND,
    ACCESS_RESTORE,
    ACCESS_VALIDITY,
    PRESENTATION_UPDATE,
    REGISTRATION_RENEW,
    RESOURCE_POLICY_UPDATE,
}

object StudioOperationIdentity {
    fun type(action: StudioActionType, fullName: String): String =
        "${action.name}:${CredentialValidation.normalizeFullName(fullName)}"
}

data class ManagementMutation(
    val action: StudioActionType,
    val target: String,
    val calldata: String,
    val oldValue: String,
    val newValue: String,
    val expectedAccessActive: Boolean? = null,
    val expectedAccessValidUntil: BigInteger? = null,
    val expectedDescription: String? = null,
    val expectedArtwork: String? = null,
    val expectedRegistrationExpiry: BigInteger? = null,
    val expectedPreservedAccessUntil: BigInteger? = null,
    val expectedResources: Set<String>? = null,
)

object PassManagementPolicy {
    fun resources(snapshot: CredentialSnapshot, allowed: Set<String>): ManagementMutation {
        requireRegistered(snapshot)
        val encoded = AccessResources.encode(allowed)
        require(snapshot.resourcesRaw != encoded) { "NO_RESOURCE_CHANGE" }
        return ManagementMutation(StudioActionType.RESOURCE_POLICY_UPDATE, IssuerSpace.resolver,
            CredentialAbi.setData(CredentialAbi.namehash(snapshot.fullName), AccessResources.KEY, org.web3j.utils.Numeric.hexStringToByteArray(encoded)),
            if (snapshot.resourcePolicyInvalid) "Invalid resource policy" else AccessResources.names(snapshot.allowedResources),
            AccessResources.names(allowed), expectedResources = allowed.toSet())
    }

    fun actions(snapshot: CredentialSnapshot, capabilities: StudioCapabilities): Set<StudioActionType> {
        if (snapshot.readStatus != CredentialReadStatus.FRESH || snapshot.status != CredentialRegistryStatus.REGISTERED ||
            !snapshot.provenanceMatches || !snapshot.registry.equals(IssuerSpace.registry, true)
        ) return emptySet()
        return buildSet {
            if (capabilities.canManageAccess == IssuerCapabilityState.ALLOWED) {
                add(StudioActionType.RESOURCE_POLICY_UPDATE)
                add(if (snapshot.accessActive == true) StudioActionType.ACCESS_SUSPEND else StudioActionType.ACCESS_RESTORE)
                add(StudioActionType.ACCESS_VALIDITY)
            }
            if (capabilities.canManagePresentation == IssuerCapabilityState.ALLOWED) {
                add(StudioActionType.PRESENTATION_UPDATE)
            }
            if (capabilities.canRenew == IssuerCapabilityState.ALLOWED) add(StudioActionType.REGISTRATION_RENEW)
        }
    }

    fun access(snapshot: CredentialSnapshot, active: Boolean, validUntil: BigInteger, now: BigInteger): ManagementMutation {
        requireRegistered(snapshot)
        val registrationExpiry = snapshot.registryExpiry ?: error("REGISTRATION_EXPIRY_MISSING")
        require(validUntil.bitLength() <= 64 && validUntil <= registrationExpiry) { "ACCESS_VALIDITY_AFTER_REGISTRATION" }
        if (active) require(validUntil > now) { "EXPIRED_ACCESS_CANNOT_RESTORE" }
        val old = "${StudioAccessPolicy.status(snapshot)} until ${StudioTime.describe(snapshot.accessValidUntil)}"
        return ManagementMutation(
            action = when {
                validUntil != snapshot.accessValidUntil -> StudioActionType.ACCESS_VALIDITY
                active -> StudioActionType.ACCESS_RESTORE
                else -> StudioActionType.ACCESS_SUSPEND
            },
            target = IssuerSpace.resolver,
            calldata = CredentialAbi.setData(
                CredentialAbi.namehash(snapshot.fullName),
                IssuerSpace.ACCESS_KEY,
                CredentialAbi.accessValue(active, validUntil),
            ),
            oldValue = old,
            newValue = "${if (active) "Allowed" else "Suspended"} until ${StudioTime.describe(validUntil)}",
            expectedAccessActive = active,
            expectedAccessValidUntil = validUntil,
        )
    }

    fun presentation(
        snapshot: CredentialSnapshot,
        description: String?,
        artworkUri: String?,
        removeArtwork: Boolean = false,
    ): ManagementMutation {
        requireRegistered(snapshot)
        require(!(artworkUri != null && removeArtwork)) { "ARTWORK_CHANGE_AMBIGUOUS" }
        val node = CredentialAbi.namehash(snapshot.fullName)
        val calls = buildList {
            if (description != null && description != snapshot.description) {
                require(description.length <= 1_024) { "INVALID_DESCRIPTION" }
                add(CredentialAbi.setText(node, "description", description))
            }
            if (artworkUri != null) {
                val normalized = CredentialValidation.normalizeAvatar(artworkUri)
                require(normalized.isNotBlank()) { "USE_REMOVE_ARTWORK" }
                if (normalized != snapshot.avatarUri.orEmpty()) add(CredentialAbi.setText(node, "avatar", normalized))
            } else if (removeArtwork && snapshot.avatarUri.orEmpty().isNotBlank()) {
                add(CredentialAbi.setText(node, "avatar", ""))
            }
        }
        require(calls.isNotEmpty()) { "NO_PRESENTATION_CHANGE" }
        val nextDescription = description ?: snapshot.description.orEmpty()
        val nextArtwork = when {
            removeArtwork -> ""
            artworkUri != null -> CredentialValidation.normalizeAvatar(artworkUri)
            else -> snapshot.avatarUri.orEmpty()
        }
        return ManagementMutation(
            StudioActionType.PRESENTATION_UPDATE,
            IssuerSpace.resolver,
            if (calls.size == 1) calls.single() else CredentialAbi.multicall(calls),
            "Description: ${snapshot.description.orEmpty()} | Artwork: ${snapshot.avatarUri.orEmpty().ifBlank { "Not set" }}",
            "Description: ${description ?: snapshot.description.orEmpty()} | Artwork: ${when {
                removeArtwork -> "Not set"
                artworkUri != null -> CredentialValidation.normalizeAvatar(artworkUri)
                else -> snapshot.avatarUri.orEmpty().ifBlank { "Not set" }
            }}",
            expectedDescription = nextDescription,
            expectedArtwork = nextArtwork,
        )
    }

    fun renew(snapshot: CredentialSnapshot, newExpiry: BigInteger, namespaceExpiry: BigInteger): ManagementMutation {
        requireRegistered(snapshot)
        val current = snapshot.registryExpiry ?: error("REGISTRATION_EXPIRY_MISSING")
        require(newExpiry > current) { "REGISTRATION_EXPIRY_MUST_INCREASE" }
        require(newExpiry < namespaceExpiry && newExpiry.bitLength() <= 64) { "EXPIRY_OUTSIDE_NAMESPACE" }
        val tokenId = snapshot.tokenId ?: error("TOKEN_ID_MISSING")
        return ManagementMutation(
            StudioActionType.REGISTRATION_RENEW,
            IssuerSpace.registry,
            CredentialAbi.renew(tokenId, newExpiry),
            StudioTime.describe(current),
            StudioTime.describe(newExpiry),
            expectedRegistrationExpiry = newExpiry,
            expectedPreservedAccessUntil = snapshot.accessValidUntil,
        )
    }

    private fun requireRegistered(snapshot: CredentialSnapshot) {
        require(snapshot.readStatus == CredentialReadStatus.FRESH &&
            snapshot.status == CredentialRegistryStatus.REGISTERED && snapshot.provenanceMatches
        ) { "PASS_NOT_MANAGEABLE" }
    }
}

data class ManagementSession(
    val wallet: String,
    val fullName: String,
    val operationId: String,
    val mutation: ManagementMutation,
)

class StudioManagementConflict(val session: ManagementSession) :
    IllegalArgumentException("MANAGEMENT_FINALIZATION_PENDING")

class ManagementFinalReadbackReconciler(
    private val read: suspend (String) -> CredentialSnapshot,
    private val wait: suspend (Long) -> Unit = { kotlinx.coroutines.delay(it) },
    private val attempts: Int = 4,
) {
    suspend fun finalize(session: ManagementSession, operation: PersistedTransactionOperation,
        coordinator: ManagementSessionCoordinator) {
        require(operation.operationId == session.operationId && operation.walletAddress.equals(session.wallet, true) &&
            operation.chainId == IssuerSpace.chainId && operation.valueWei == "0" &&
            operation.operationType == StudioOperationIdentity.type(session.mutation.action, session.fullName) &&
            operation.targetAddress.equals(session.mutation.target, true) &&
            operation.dataSummary == CredentialAbi.calldataFingerprint(session.mutation.calldata)) { "WRITE_INTENT_MISMATCH" }
        require(operation.state == TransactionOperationState.CONFIRMED) { "READBACK_UNAVAILABLE" }
        val receiptBlock = BigInteger(checkNotNull(operation.receiptBlock))
        repeat(attempts) { attempt ->
            val current = read(session.fullName)
            if (current.fullName == session.fullName && current.readStatus == CredentialReadStatus.FRESH &&
                current.provenanceMatches && current.resolver.equals(IssuerSpace.resolver, true) &&
                current.snapshotBlock != null && current.snapshotBlock >= receiptBlock && matches(current, session.mutation)) {
                coordinator.clear(session.operationId)
                return
            }
            if (attempt + 1 < attempts) wait(1_500L)
        }
        error("MANAGEMENT_READBACK_UNAVAILABLE")
    }

    private fun matches(current: CredentialSnapshot, mutation: ManagementMutation): Boolean = when (mutation.action) {
        StudioActionType.RESOURCE_POLICY_UPDATE -> !current.resourcePolicyInvalid && current.allowedResources == mutation.expectedResources
        StudioActionType.ACCESS_SUSPEND, StudioActionType.ACCESS_RESTORE, StudioActionType.ACCESS_VALIDITY ->
            current.accessActive == mutation.expectedAccessActive && current.accessValidUntil == mutation.expectedAccessValidUntil
        StudioActionType.PRESENTATION_UPDATE -> current.description.orEmpty() == mutation.expectedDescription &&
            current.avatarUri.orEmpty() == mutation.expectedArtwork
        StudioActionType.REGISTRATION_RENEW -> current.registryExpiry == mutation.expectedRegistrationExpiry &&
            current.accessValidUntil == mutation.expectedPreservedAccessUntil
        else -> false
    }
}

class ManagementSessionCoordinator(private val store: LoadableStringStateStore) {
    private val sessions = decodeAll(store.load()).associateByTo(linkedMapOf()) { it.operationId }

    @Synchronized
    fun current(wallet: String): ManagementSession? = pending(wallet).singleOrNull()

    @Synchronized fun pending(wallet: String? = null): List<ManagementSession> = sessions.values.filter {
        wallet == null || it.wallet.equals(wallet, true)
    }

    @Synchronized fun requireNoPending() {
        sessions.values.firstOrNull()?.let { throw StudioManagementConflict(it) }
    }

    @Synchronized
    fun save(value: ManagementSession): ManagementSession {
        CredentialValidation.requireAddress(value.wallet)
        CredentialValidation.normalizeFullName(value.fullName)
        require(sessions[value.operationId] == null || sessions[value.operationId] == value) { "MANAGEMENT_INTENT_MISMATCH" }
        require(sessions.keys.all { it == value.operationId }) { "MANAGEMENT_FINALIZATION_PENDING" }
        val next = LinkedHashMap(sessions).apply { put(value.operationId, value) }
        store.save(next.values.joinToString("\n", transform = ::encode))
        sessions.clear()
        sessions.putAll(next)
        return value
    }

    @Synchronized
    fun clear(operationId: String) {
        val next = LinkedHashMap(sessions).apply { remove(operationId) }
        store.save(next.values.joinToString("\n", transform = ::encode))
        sessions.clear()
        sessions.putAll(next)
    }

    private fun encode(value: ManagementSession): String = listOf(
        value.wallet,
        value.fullName,
        value.operationId,
        value.mutation.action.name,
        value.mutation.target,
        value.mutation.calldata,
        value.mutation.oldValue,
        value.mutation.newValue,
        value.mutation.expectedAccessActive?.toString().orEmpty(),
        value.mutation.expectedAccessValidUntil?.toString().orEmpty(),
        value.mutation.expectedDescription.orEmpty(),
        value.mutation.expectedArtwork.orEmpty(),
        value.mutation.expectedRegistrationExpiry?.toString().orEmpty(),
        value.mutation.expectedPreservedAccessUntil?.toString().orEmpty(),
        value.mutation.expectedResources?.let(AccessResources::encode).orEmpty(),
    ).joinToString("|") { Base64.getUrlEncoder().withoutPadding().encodeToString(it.toByteArray(Charsets.UTF_8)) }

    private fun decodeAll(raw: String?): List<ManagementSession> = raw.orEmpty().lineSequence().mapNotNull { line ->
        runCatching {
            val fields = line.split('|').map {
                String(Base64.getUrlDecoder().decode(it), Charsets.UTF_8)
            }
            require(fields.size in setOf(14, 15))
            ManagementSession(
                fields[0], fields[1], fields[2],
                ManagementMutation(
                    action = StudioActionType.valueOf(fields[3]),
                    target = fields[4],
                    calldata = fields[5],
                    oldValue = fields[6],
                    newValue = fields[7],
                    expectedAccessActive = fields[8].takeIf(String::isNotBlank)?.toBooleanStrict(),
                    expectedAccessValidUntil = fields[9].takeIf(String::isNotBlank)?.let(::BigInteger),
                    expectedDescription = fields[10].takeIf { fields[3] == StudioActionType.PRESENTATION_UPDATE.name },
                    expectedArtwork = fields[11].takeIf { fields[3] == StudioActionType.PRESENTATION_UPDATE.name },
                    expectedRegistrationExpiry = fields[12].takeIf(String::isNotBlank)?.let(::BigInteger),
                    expectedPreservedAccessUntil = fields[13].takeIf(String::isNotBlank)?.let(::BigInteger),
                    expectedResources = fields.getOrNull(14)?.takeIf(String::isNotBlank)?.let(AccessResources::decode),
                ),
            )
        }.getOrNull()
    }.toList()
}
