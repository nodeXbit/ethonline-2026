package io.github.nodexbit.ethonline2026

import java.math.BigInteger
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay

enum class CredentialRegistryStatus { AVAILABLE, RESERVED, REGISTERED, UNKNOWN }
enum class CredentialReadStatus { FRESH, UNKNOWN, MALFORMED }

data class CredentialSnapshot(
    val fullName: String,
    val node: String,
    val registry: String,
    val resolver: String?,
    val subregistry: String?,
    val tokenId: BigInteger?,
    val status: CredentialRegistryStatus,
    val owner: String?,
    val registryExpiry: BigInteger?,
    val ownerRoleBitmap: BigInteger?,
    val transferable: Boolean?,
    val accessActive: Boolean?,
    val accessValidUntil: BigInteger?,
    val avatarUri: String?,
    val description: String?,
    val snapshotBlock: BigInteger?,
    val snapshotTimestamp: BigInteger?,
    val readStatus: CredentialReadStatus,
    val provenanceMatches: Boolean,
    val failureCategory: String? = null,
    val resourcesRaw: String? = null,
) {
    val allowedResources: Set<String>? get() = runCatching { AccessResources.decode(resourcesRaw ?: "0x") }.getOrNull()
    val resourcePolicyInvalid: Boolean get() = runCatching { AccessResources.decode(resourcesRaw ?: "0x") }.isFailure
    val authoritativeAllowed: Boolean
        get() = StudioAccessPolicy.status(this) == "Allowed"
}

data class IssuerCapability(
    val state: IssuerCapabilityState,
    val category: String,
    val snapshotBlock: BigInteger? = null,
    val namespaceExpiry: BigInteger? = null,
) {
    val allowed: Boolean get() = state == IssuerCapabilityState.ALLOWED
}

object IssuerAuthorityPolicy {
    fun evaluate(
        account: String,
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
    ): IssuerCapability {
        val category = when {
            !account.equals(IssuerSpace.issuer, true) -> "NOT_CONFIGURED_ISSUER"
            registryRoles.and(IssuerSpace.registryRootRoles) != IssuerSpace.registryRootRoles -> "R1_AUTHORITY_MISSING"
            resolverRoles.and(IssuerSpace.resolverRootRoles) != IssuerSpace.resolverRootRoles -> "S1_AUTHORITY_MISSING"
            !provenanceMatches -> "PROVENANCE_MISMATCH"
            namespaceStatus != BigInteger.valueOf(2) -> "NAMESPACE_NOT_REGISTERED"
            !namespaceOwner.equals(account, true) -> "NAMESPACE_OWNER_MISMATCH"
            !namespaceRegistry.equals(IssuerSpace.registry, true) ||
                !namespaceResolver.equals(IssuerSpace.resolver, true) -> "NAMESPACE_TARGET_MISMATCH"
            namespaceExpiry <= now -> "NAMESPACE_EXPIRED"
            else -> null
        }
        return if (category == null) {
            IssuerCapability(IssuerCapabilityState.ALLOWED, "CONFIRMED_ONCHAIN", blockNumber, namespaceExpiry)
        } else {
            IssuerCapability(IssuerCapabilityState.DENIED, category, blockNumber, namespaceExpiry)
        }
    }
}

data class CredentialExpectation(
    val fullName: String,
    val holder: String,
    val resolver: String,
    val expiry: BigInteger,
    val description: String,
    val avatarUri: String,
    val accessActive: Boolean = true,
    val accessValidUntil: BigInteger = expiry,
    val roleBitmap: BigInteger = BigInteger.ZERO,
    val allowedResources: Set<String>? = null,
)

object CredentialReadPolicy {
    fun requireReady(snapshot: CredentialSnapshot, expected: CredentialExpectation) {
        require(snapshot.readStatus == CredentialReadStatus.FRESH) { "READBACK_NOT_FRESH" }
        require(snapshot.fullName == expected.fullName) { "FULL_NAME_MISMATCH" }
        require(snapshot.registry.equals(IssuerSpace.registry, true)) { "WRONG_REGISTRY" }
        require(snapshot.status == CredentialRegistryStatus.REGISTERED) { "NOT_REGISTERED" }
        require(snapshot.owner.equals(expected.holder, true)) { "WRONG_OWNER" }
        require(snapshot.resolver.equals(expected.resolver, true)) { "WRONG_RESOLVER" }
        require(snapshot.subregistry.equals(IssuerSpace.ZERO_ADDRESS, true)) { "WRONG_SUBREGISTRY" }
        require(snapshot.registryExpiry == expected.expiry) { "WRONG_REGISTRY_EXPIRY" }
        require(snapshot.snapshotTimestamp != null && snapshot.registryExpiry > snapshot.snapshotTimestamp) {
            "CREDENTIAL_EXPIRED"
        }
        require(snapshot.ownerRoleBitmap == expected.roleBitmap &&
            snapshot.transferable == (expected.roleBitmap.and(StudioRoles.CAN_TRANSFER_ADMIN) != BigInteger.ZERO)) {
            "WRONG_TRANSFERABILITY"
        }
        require(snapshot.description == expected.description) { "WRONG_DESCRIPTION" }
        require((snapshot.avatarUri ?: "") == expected.avatarUri) { "WRONG_AVATAR" }
        require(snapshot.accessActive == expected.accessActive && snapshot.accessValidUntil == expected.accessValidUntil) {
            "WRONG_ACCESS_RECORD"
        }
        require(snapshot.accessValidUntil >= snapshot.snapshotTimestamp) { "ACCESS_EXPIRED" }
        if (expected.allowedResources != null) require(!snapshot.resourcePolicyInvalid && snapshot.allowedResources == expected.allowedResources) {
            "WRONG_RESOURCE_RECORD"
        }
        require(snapshot.provenanceMatches) { "WRONG_PROVENANCE" }
    }
}

object CredentialProductPolicy {
    fun issuerControlsVisible(capability: IssuerCapability): Boolean = capability.allowed

    fun requireAvailable(snapshot: CredentialSnapshot) {
        require(snapshot.readStatus == CredentialReadStatus.FRESH) { "CREDENTIAL_STATE_UNKNOWN" }
        require(snapshot.status == CredentialRegistryStatus.AVAILABLE) { "CREDENTIAL_NOT_AVAILABLE" }
    }

    fun ownedBy(snapshot: CredentialSnapshot, wallet: String): Boolean =
        snapshot.readStatus == CredentialReadStatus.FRESH &&
            snapshot.status == CredentialRegistryStatus.REGISTERED &&
            snapshot.owner.equals(wallet, true) &&
            snapshot.registry.equals(IssuerSpace.registry, true) &&
            snapshot.resolver.equals(IssuerSpace.resolver, true) &&
            snapshot.subregistry.equals(IssuerSpace.ZERO_ADDRESS, true) &&
            snapshot.provenanceMatches
}

object CredentialRecordDecoding {
    fun access(value: ByteArray): Pair<Boolean, BigInteger>? =
        value.takeUnless(ByteArray::isEmpty)?.let(CredentialAbi::decodeAccess)
}

object CredentialFinalReadbackPolicy {
    fun confirmedReceiptBlock(
        session: IssuanceSession,
        recordsOperation: PersistedTransactionOperation?,
    ): BigInteger {
        require(session.recordsOperationId != null && session.recordsOperationId == recordsOperation?.operationId) {
            "RECORDS_OPERATION_MISSING"
        }
        require((session.fullName == IssuerSpace.fullName && recordsOperation.operationType == ContractTransactionRunner.RECORDS_OPERATION) ||
            recordsOperation.operationType == StudioOperationIdentity.type(
                StudioActionType.ISSUE_CONFIGURE, session.fullName,
            )) {
            "RECORDS_OPERATION_TYPE_MISMATCH"
        }
        require(recordsOperation.state == TransactionOperationState.CONFIRMED) { "RECORDS_NOT_CONFIRMED" }
        require(recordsOperation.walletAddress.equals(session.wallet, true) &&
            recordsOperation.chainId == IssuerSpace.chainId && recordsOperation.valueWei == "0" &&
            recordsOperation.targetAddress.equals(IssuerSpace.resolver, true)) { "WRITE_INTENT_MISMATCH" }
        return recordsOperation.receiptBlock?.toBigIntegerOrNull()
            ?: throw IllegalArgumentException("RECORDS_RECEIPT_BLOCK_MISSING")
    }
}

class CredentialFinalReadbackReconciler(
    private val read: suspend (String) -> CredentialSnapshot,
    private val wait: suspend (Long) -> Unit = { delay(it) },
    private val attempts: Int = 4,
    private val pollDelayMillis: Long = 1_500L,
) {
    init {
        require(attempts > 0) { "READBACK_ATTEMPTS_REQUIRED" }
        require(pollDelayMillis >= 0) { "READBACK_POLL_DELAY_INVALID" }
    }

    suspend fun reconcile(
        expected: CredentialExpectation,
        minimumBlock: BigInteger,
    ): CredentialSnapshot {
        var sourceBehindReceipt = false
        repeat(attempts) { attempt ->
            val snapshot = read(expected.fullName)
            if (snapshot.readStatus == CredentialReadStatus.FRESH && snapshot.snapshotBlock != null) {
                if (snapshot.snapshotBlock >= minimumBlock) {
                    CredentialReadPolicy.requireReady(snapshot, expected)
                    return snapshot
                }
                sourceBehindReceipt = true
            }
            if (attempt + 1 < attempts) wait(pollDelayMillis)
        }
        throw IllegalStateException(
            if (sourceBehindReceipt) "READBACK_BEHIND_CONFIRMED_RECEIPT" else "READBACK_UNAVAILABLE",
        )
    }
}

class CredentialReader(private val client: ReadOnlyEthereumRpcClient) {
    suspend fun read(fullNameInput: String): CredentialSnapshot {
        val fullName = try {
            CredentialValidation.normalizeFullName(fullNameInput)
        } catch (error: Throwable) {
            return unknown(fullNameInput.trim(), "INVALID_CREDENTIAL_NAME")
        }
        val label = fullName.removeSuffix(".${IssuerSpace.namespace}")
        val node = CredentialAbi.nodeHex(fullName)
        return try {
            require(client.chainId() == BigInteger.valueOf(IssuerSpace.chainId)) { "WRONG_CHAIN" }
            val block = client.blockByNumber("latest", false) ?: error("LATEST_BLOCK_MISSING")
            val blockNumber = quantityField(block, "number")
            val timestamp = quantityField(block, "timestamp")
            require(timestamp <= BigInteger.valueOf(System.currentTimeMillis() / 1_000L + 15L)) {
                "FUTURE_BLOCK"
            }
            require(BigInteger.valueOf(System.currentTimeMillis() / 1_000L) - timestamp <= BigInteger.valueOf(120L)) {
                "STALE_BLOCK"
            }
            val tag = quantity(blockNumber)
            val tokenId = CredentialAbi.decodeWord(call(IssuerSpace.registry, CredentialAbi.findTokenId(label), tag))
            val stateRaw = call(IssuerSpace.registry, CredentialAbi.getState(tokenId), tag)
            val status = when (CredentialAbi.decodeWord(stateRaw, 0).intValueExact()) {
                0 -> CredentialRegistryStatus.AVAILABLE
                1 -> CredentialRegistryStatus.RESERVED
                2 -> CredentialRegistryStatus.REGISTERED
                else -> CredentialRegistryStatus.UNKNOWN
            }
            val stateExpiry = CredentialAbi.decodeWord(stateRaw, 1)
            val stateOwner = CredentialAbi.decodeAddress(stateRaw, 2)
            val stateTokenId = CredentialAbi.decodeWord(stateRaw, 3)
            require(stateTokenId == tokenId) { "INCONSISTENT_TOKEN_ID" }
            if (status != CredentialRegistryStatus.REGISTERED) {
                val provenance = provenanceMatches(tag)
                return CredentialSnapshot(
                    fullName, node, IssuerSpace.registry, null, null, tokenId, status, null,
                    stateExpiry, null, null, null, null, null, null, blockNumber, timestamp,
                    CredentialReadStatus.FRESH, provenance,
                )
            }
            coroutineScope {
                val owner = async { CredentialAbi.decodeAddress(call(IssuerSpace.registry, CredentialAbi.getOwner(tokenId), tag)) }
                val expiry = async { CredentialAbi.decodeWord(call(IssuerSpace.registry, CredentialAbi.getExpiry(tokenId), tag)) }
                val resolver = async { CredentialAbi.decodeAddress(call(IssuerSpace.registry, CredentialAbi.getResolver(label), tag)) }
                val subregistry = async { CredentialAbi.decodeAddress(call(IssuerSpace.registry, CredentialAbi.getSubregistry(label), tag)) }
                val roleCount = async { CredentialAbi.decodeWord(call(IssuerSpace.registry, CredentialAbi.roleCount(tokenId), tag)) }
                val resolvedOwner = owner.await()
                require(resolvedOwner.equals(stateOwner, true)) { "INCONSISTENT_OWNER" }
                val ownerRoles = async {
                    CredentialAbi.decodeWord(call(IssuerSpace.registry, CredentialAbi.roles(tokenId, resolvedOwner), tag))
                }
                val resolvedResolver = resolver.await()
                val avatar = async { readText(resolvedResolver, node, "avatar", tag) }
                val description = async { readText(resolvedResolver, node, "description", tag) }
                val access = async { readAccess(resolvedResolver, node, tag) }
                val resourcePolicy = async {
                    if (!resolvedResolver.equals(IssuerSpace.resolver, true)) null else
                        org.web3j.utils.Numeric.toHexString(CredentialAbi.decodeDynamicBytes(
                            call(resolvedResolver, CredentialAbi.data(CredentialAbi.namehash(fullName), AccessResources.KEY), tag)))
                }
                val provenance = async { provenanceMatches(tag) }
                val resolvedExpiry = expiry.await()
                require(resolvedExpiry == stateExpiry) { "INCONSISTENT_EXPIRY" }
                val roles = ownerRoles.await()
                val assignments = roleCount.await()
                val accessValue = access.await()
                CredentialSnapshot(
                    fullName = fullName,
                    node = node,
                    registry = IssuerSpace.registry,
                    resolver = resolvedResolver,
                    subregistry = subregistry.await(),
                    tokenId = tokenId,
                    status = status,
                    owner = resolvedOwner,
                    registryExpiry = resolvedExpiry,
                    ownerRoleBitmap = roles,
                    transferable = roles.and(StudioRoles.CAN_TRANSFER_ADMIN) == StudioRoles.CAN_TRANSFER_ADMIN,
                    accessActive = accessValue?.first,
                    accessValidUntil = accessValue?.second,
                    avatarUri = avatar.await(),
                    description = description.await(),
                    snapshotBlock = blockNumber,
                    snapshotTimestamp = timestamp,
                    readStatus = CredentialReadStatus.FRESH,
                    provenanceMatches = provenance.await(),
                    resourcesRaw = resourcePolicy.await(),
                )
            }
        } catch (error: Throwable) {
            if (error is CancellationException) throw error
            unknown(fullName, safeCategory(error))
        }
    }

    suspend fun issuerCapability(account: String): IssuerCapability {
        val studio = studioCapabilities(account)
        return IssuerCapability(studio.canIssue, studio.category, studio.snapshotBlock, studio.namespaceExpiry)
    }

    suspend fun studioCapabilities(account: String): StudioCapabilities {
        return try {
            CredentialValidation.requireAddress(account)
            require(client.chainId() == BigInteger.valueOf(IssuerSpace.chainId)) { "WRONG_CHAIN" }
            val block = client.blockByNumber("latest", false) ?: error("LATEST_BLOCK_MISSING")
            val blockNumber = quantityField(block, "number")
            val timestamp = quantityField(block, "timestamp")
            val tag = quantity(blockNumber)
            require(BigInteger.valueOf(System.currentTimeMillis() / 1_000L) - timestamp <= BigInteger.valueOf(120L)) {
                "STALE_BLOCK"
            }
            val r1Roles = CredentialAbi.decodeWord(call(IssuerSpace.registry, CredentialAbi.roles(BigInteger.ZERO, account), tag))
            val s1Roles = CredentialAbi.decodeWord(call(IssuerSpace.resolver, CredentialAbi.roles(BigInteger.ZERO, account), tag))
            val provenance = provenanceMatches(tag)
            val keyToken = CredentialAbi.decodeWord(call(IssuerSpace.parentRegistry, CredentialAbi.findTokenId("keys"), tag))
            val keyState = call(IssuerSpace.parentRegistry, CredentialAbi.getState(keyToken), tag)
            val namespaceStatus = CredentialAbi.decodeWord(keyState, 0)
            val expiry = CredentialAbi.decodeWord(keyState, 1)
            val owner = CredentialAbi.decodeAddress(call(IssuerSpace.parentRegistry, CredentialAbi.getOwner(keyToken), tag))
            val registry = CredentialAbi.decodeAddress(call(IssuerSpace.parentRegistry, CredentialAbi.getSubregistry("keys"), tag))
            val resolver = CredentialAbi.decodeAddress(call(IssuerSpace.parentRegistry, CredentialAbi.getResolver("keys"), tag))
            StudioCapabilityPolicy.evaluate(
                r1Roles, s1Roles, provenance, namespaceStatus, owner, registry, resolver,
                expiry, timestamp, blockNumber,
            )
        } catch (error: Throwable) {
            if (error is CancellationException) throw error
            StudioCapabilities.unavailable(safeCategory(error))
        }
    }

    suspend fun simulate(from: String, to: String, data: String, blockTag: String = "latest") {
        call(to, data, blockTag, from)
    }

    private suspend fun provenanceMatches(tag: String): Boolean {
        val registryImpl = CredentialAbi.decodeAddress(
            call(IssuerSpace.factory, CredentialAbi.verifyContract(IssuerSpace.registry), tag),
        )
        val resolverImpl = CredentialAbi.decodeAddress(
            call(IssuerSpace.factory, CredentialAbi.verifyContract(IssuerSpace.resolver), tag),
        )
        return registryImpl.equals(IssuerSpace.registryImplementation, true) &&
            resolverImpl.equals(IssuerSpace.resolverImplementation, true)
    }

    private suspend fun readText(resolver: String, node: String, key: String, tag: String): String {
        if (!resolver.equals(IssuerSpace.resolver, true)) return ""
        return CredentialAbi.decodeString(call(resolver, CredentialAbi.text(hexBytes(node), key), tag))
    }

    private suspend fun readAccess(resolver: String, node: String, tag: String): Pair<Boolean, BigInteger>? {
        require(resolver.equals(IssuerSpace.resolver, true)) { "WRONG_RESOLVER" }
        val value = CredentialAbi.decodeDynamicBytes(
            call(resolver, CredentialAbi.data(hexBytes(node), IssuerSpace.ACCESS_KEY), tag),
        )
        return CredentialRecordDecoding.access(value)
    }

    private suspend fun call(to: String, data: String, tag: String, from: String? = null): String {
        val fields = linkedMapOf<String, ReadOnlyJsonValue>(
            "to" to ReadOnlyJsonValue.StringValue(to),
            "data" to ReadOnlyJsonValue.StringValue(data),
        )
        if (from != null) fields["from"] = ReadOnlyJsonValue.StringValue(from)
        return client.ethCall(ReadOnlyJsonValue.ObjectValue(fields), tag)
    }

    private fun quantityField(value: ReadOnlyJsonValue.ObjectValue, name: String): BigInteger {
        val raw = (value.fields[name] as? ReadOnlyJsonValue.StringValue)?.value ?: error("MISSING_$name")
        return MobileIssuerAdmissionRunner.parseQuantity(raw)
    }

    private fun quantity(value: BigInteger) = "0x${value.toString(16)}"
    private fun hexBytes(value: String) = org.web3j.utils.Numeric.hexStringToByteArray(value)
    private fun safeCategory(error: Throwable): String = StudioErrors.category(error)

    private fun unknown(fullName: String, category: String) = CredentialSnapshot(
        fullName = fullName,
        node = runCatching { CredentialAbi.nodeHex(fullName) }.getOrDefault(""),
        registry = IssuerSpace.registry,
        resolver = null,
        subregistry = null,
        tokenId = null,
        status = CredentialRegistryStatus.UNKNOWN,
        owner = null,
        registryExpiry = null,
        ownerRoleBitmap = null,
        transferable = null,
        accessActive = null,
        accessValidUntil = null,
        avatarUri = null,
        description = null,
        snapshotBlock = null,
        snapshotTimestamp = null,
        readStatus = CredentialReadStatus.UNKNOWN,
        provenanceMatches = false,
        failureCategory = category,
    )
}
