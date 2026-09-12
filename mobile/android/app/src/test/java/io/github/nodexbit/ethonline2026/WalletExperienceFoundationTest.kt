package io.github.nodexbit.ethonline2026

import java.math.BigInteger
import kotlinx.coroutines.runBlocking
import org.bouncycastle.jcajce.provider.digest.Keccak
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import org.web3j.abi.FunctionEncoder
import org.web3j.abi.datatypes.Address
import org.web3j.abi.datatypes.Utf8String
import org.web3j.abi.datatypes.generated.Uint64

class WalletExperienceFoundationTest {
    @Test
    fun `restoring session never exposes login`() {
        val restoring = SessionHydrationPolicy.surface(SessionHydrationState.RESTORING_SESSION)
        assertTrue(restoring.restoringVisible)
        assertFalse(restoring.loginVisible)
        assertFalse(restoring.authenticatedVisible)
    }

    @Test
    fun `authenticated and unauthenticated restoration select exclusive surfaces`() {
        assertEquals(
            SessionSurface(false, true, false),
            SessionHydrationPolicy.surface(SessionHydrationState.AUTHENTICATED),
        )
        assertEquals(
            SessionSurface(false, false, true),
            SessionHydrationPolicy.surface(SessionHydrationState.UNAUTHENTICATED),
        )
    }

    @Test
    fun `multiple wallet enumeration restores persisted active address`() {
        val wallets = listOf(wallet(HOLDER, 0), wallet(ISSUER, 1))
        val store = MemoryStore()
        ActiveWalletStore(store).save(wallets[1])
        val restored = ActiveWalletPolicy.select(wallets, ActiveWalletStore(store).loadAddress())
        assertEquals(ISSUER.lowercase(), restored?.address?.lowercase())
    }

    @Test
    fun `invalid selected wallet safely falls back by hd index`() {
        val wallets = listOf(wallet(ISSUER, 4), wallet(HOLDER, 2))
        assertEquals(HOLDER, ActiveWalletPolicy.select(wallets, OTHER)?.address)
        assertThrows(IllegalArgumentException::class.java) {
            ActiveWalletPolicy.requireSelectable(wallets, OTHER)
        }
    }

    @Test
    fun `selected pass persists per wallet and invalidates on ownership mismatch`() {
        val store = MemoryStore()
        val selected = SelectedPassStore(store)
        selected.select(IssuerSpace.chainId, HOLDER, snapshot(owner = HOLDER))
        assertEquals(IssuerSpace.fullName, SelectedPassStore(store).selected(IssuerSpace.chainId, HOLDER))
        assertNull(selected.selected(IssuerSpace.chainId, ISSUER))
        assertNull(selected.validate(IssuerSpace.chainId, HOLDER, listOf(snapshot(owner = OTHER))))
        assertNull(SelectedPassStore(store).selected(IssuerSpace.chainId, HOLDER))
    }

    @Test
    fun `only freshly owned verified pass can be selected`() {
        val selected = SelectedPassStore(MemoryStore())
        assertThrows(IllegalArgumentException::class.java) {
            selected.select(IssuerSpace.chainId, HOLDER, snapshot(owner = OTHER))
        }
        assertThrows(IllegalArgumentException::class.java) {
            selected.select(IssuerSpace.chainId, HOLDER, snapshot(read = CredentialReadStatus.UNKNOWN))
        }
    }

    @Test
    fun `single and stacked pass presentation policy is deterministic`() {
        assertEquals(PassCardMode.SINGLE_EXPANDED, PassStackPolicy.mode(1, false))
        assertEquals(PassCardMode.STACKED_SUMMARY, PassStackPolicy.mode(3, false))
        assertEquals(PassCardMode.STACKED_SELECTED, PassStackPolicy.mode(3, true))
        assertEquals(0, PassStackPolicy.overlapDp(1, 0))
        assertTrue(PassStackPolicy.overlapDp(3, 1) < 0)
    }

    @Test
    fun `R1 registration event decodes strict label and token id`() {
        val decoded = R1RegistrationEvent.decode(registrationLog("staff-001", HOLDER, BigInteger.TEN))
        assertEquals(BigInteger.TEN, decoded.tokenId)
        assertEquals("staff-001", decoded.label)
        assertEquals(IssuerSpace.fullName, decoded.fullName)
    }

    @Test
    fun `R1 registration event rejects mismatched label hash`() {
        val malformed = registrationLog("staff-001", HOLDER, BigInteger.TEN).let {
            it.copy(topics = it.topics.toMutableList().also { topics -> topics[2] = "0x${"00".repeat(32)}" })
        }
        assertThrows(IllegalArgumentException::class.java) { R1RegistrationEvent.decode(malformed) }
    }

    @Test
    fun `candidate dedup and current owner override historical event owner`() = runBlocking {
        val source = CredentialCandidateSource {
            listOf(
                CredentialReference(IssuerSpace.chainId, IssuerSpace.fullName),
                CredentialReference(IssuerSpace.chainId, IssuerSpace.fullName),
            ) to BigInteger.TEN
        }
        val result = CredentialDiscoveryService(source, read = { snapshot(owner = HOLDER) })
            .discover(HOLDER, emptyList()) as CredentialDiscoveryResult.Available
        assertEquals(1, result.candidateCount)
        assertEquals(1, result.credentials.size)
        val moved = CredentialDiscoveryService(source, read = { snapshot(owner = OTHER) })
            .discover(HOLDER, emptyList()) as CredentialDiscoveryResult.Available
        assertTrue(moved.credentials.isEmpty())
    }

    @Test
    fun `bounded R1 source scans registry logs and deduplicates candidates`() = runBlocking {
        var call = 0
        val log = registrationLog("staff-001", HOLDER, BigInteger.TEN)
        val logJson = logJson(log)
        val now = System.currentTimeMillis() / 1_000L
        val client = ReadOnlyEthereumRpcClient(transport = ReadOnlyRpcTransport { request, _ ->
            call += 1
            when (call) {
                1 -> "{\"jsonrpc\":\"2.0\",\"id\":1,\"result\":\"0xaa36a7\"}"
                2 -> "{\"jsonrpc\":\"2.0\",\"id\":2,\"result\":{" +
                    "\"number\":\"0xa\",\"timestamp\":\"0x${now.toString(16)}\"}}"
                3 -> {
                    assertTrue(request.contains(IssuerSpace.registry.lowercase()))
                    assertTrue(request.contains(R1RegistrationEvent.topic0))
                    "{\"jsonrpc\":\"2.0\",\"id\":3,\"result\":[$logJson,$logJson]}"
                }
                else -> error("unexpected call")
            }
        })
        val (candidates, scannedTo) = R1CredentialCandidateSource(client, startBlock = 10).candidates()
        assertEquals(BigInteger.TEN, scannedTo)
        assertEquals(listOf(CredentialReference(IssuerSpace.chainId, IssuerSpace.fullName)), candidates)
        assertEquals(3, call)
    }

    @Test
    fun `staff is automatic for holder and excluded for another wallet`() = runBlocking {
        val source = CredentialCandidateSource {
            listOf(CredentialReference(IssuerSpace.chainId, IssuerSpace.fullName)) to BigInteger.ONE
        }
        val service = CredentialDiscoveryService(source, read = { snapshot(owner = HOLDER) })
        val holder = service.discover(HOLDER, emptyList()) as CredentialDiscoveryResult.Available
        val other = service.discover(OTHER, emptyList()) as CredentialDiscoveryResult.Available
        assertEquals(listOf(IssuerSpace.fullName), holder.credentials.map { it.fullName })
        assertTrue(other.credentials.isEmpty())
    }

    @Test
    fun `manual add remains merged with automatic discovery`() = runBlocking {
        val source = CredentialCandidateSource { emptyList<CredentialReference>() to BigInteger.ONE }
        val result = CredentialDiscoveryService(source, read = { snapshot(owner = HOLDER) })
            .discover(HOLDER, listOf(CredentialReference(IssuerSpace.chainId, IssuerSpace.fullName)))
            as CredentialDiscoveryResult.Available
        assertEquals(1, result.credentials.size)
    }

    @Test
    fun `RPC or readback failure makes discovery explicitly unavailable`() = runBlocking {
        val rpcFailure = CredentialDiscoveryService(
            CredentialCandidateSource { throw ReadOnlyRpcException("TIMEOUT", "safe") },
            read = { snapshot() },
        ).discover(HOLDER, emptyList())
        assertEquals("TIMEOUT", (rpcFailure as CredentialDiscoveryResult.Unavailable).category)

        val source = CredentialCandidateSource {
            listOf(CredentialReference(IssuerSpace.chainId, IssuerSpace.fullName)) to BigInteger.ONE
        }
        val readFailure = CredentialDiscoveryService(
            source,
            read = { snapshot(read = CredentialReadStatus.UNKNOWN) },
        ).discover(HOLDER, emptyList())
        assertTrue(readFailure is CredentialDiscoveryResult.Unavailable)
    }

    @Test
    fun `issuer capability has allowed denied and unavailable states`() = runBlocking {
        assertEquals(IssuerCapabilityState.ALLOWED, authority(ISSUER).state)
        assertEquals(IssuerCapabilityState.DENIED, authority(HOLDER).state)
        val unavailable = CredentialReader(
            ReadOnlyEthereumRpcClient(transport = ReadOnlyRpcTransport { _, _ -> error("offline") }),
        ).issuerCapability(ISSUER)
        assertEquals(IssuerCapabilityState.UNAVAILABLE, unavailable.state)
    }

    @Test
    fun `wallet can own pass and issue at the same time`() {
        assertTrue(CredentialProductPolicy.ownedBy(snapshot(owner = ISSUER), ISSUER))
        assertEquals(IssuerCapabilityState.ALLOWED, authority(ISSUER).state)
    }

    private fun authority(account: String) = IssuerAuthorityPolicy.evaluate(
        account = account,
        registryRoles = IssuerSpace.registryRootRoles,
        resolverRoles = IssuerSpace.resolverRootRoles,
        provenanceMatches = true,
        namespaceStatus = BigInteger.valueOf(2),
        namespaceOwner = ISSUER,
        namespaceRegistry = IssuerSpace.registry,
        namespaceResolver = IssuerSpace.resolver,
        namespaceExpiry = BigInteger.valueOf(IssuerSpace.namespaceExpiry),
        now = BigInteger.ONE,
        blockNumber = BigInteger.ONE,
    )

    private fun registrationLog(label: String, historicalOwner: String, tokenId: BigInteger): EthereumLog {
        val labelBytes = label.toByteArray()
        val labelHash = Keccak.Digest256().digest(labelBytes).toHexWord()
        val data = FunctionEncoder.encodeConstructor(listOf(
            Utf8String(label),
            Address(historicalOwner),
            Uint64(BigInteger.valueOf(IssuerSpace.STAFF_EXPIRY)),
        )).let { if (it.startsWith("0x")) it else "0x$it" }
        return EthereumLog(
            address = IssuerSpace.registry,
            blockNumber = BigInteger.TEN,
            transactionHash = "0x${"11".repeat(32)}",
            logIndex = BigInteger.ZERO,
            topics = listOf(
                R1RegistrationEvent.topic0,
                "0x${tokenId.toString(16).padStart(64, '0')}",
                labelHash,
                "0x${historicalOwner.removePrefix("0x").lowercase().padStart(64, '0')}",
            ),
            data = data,
        )
    }

    private fun logJson(log: EthereumLog): String = ReadOnlyJsonValue.ObjectValue(linkedMapOf(
        "address" to ReadOnlyJsonValue.StringValue(log.address),
        "blockNumber" to ReadOnlyJsonValue.StringValue("0x${log.blockNumber.toString(16)}"),
        "transactionHash" to ReadOnlyJsonValue.StringValue(log.transactionHash),
        "logIndex" to ReadOnlyJsonValue.StringValue("0x${log.logIndex.toString(16)}"),
        "topics" to ReadOnlyJsonValue.ArrayValue(log.topics.map(ReadOnlyJsonValue::StringValue)),
        "data" to ReadOnlyJsonValue.StringValue(log.data),
    )).toJson()

    private fun ByteArray.toHexWord() = "0x" + joinToString("") { "%02x".format(it.toInt() and 0xff) }

    private fun wallet(address: String, index: Int) = ActiveWallet(address, "wallet-$index", index)

    private fun snapshot(
        owner: String = HOLDER,
        read: CredentialReadStatus = CredentialReadStatus.FRESH,
    ) = CredentialSnapshot(
        fullName = IssuerSpace.fullName,
        node = CredentialAbi.nodeHex(IssuerSpace.fullName),
        registry = IssuerSpace.registry,
        resolver = IssuerSpace.resolver,
        subregistry = IssuerSpace.ZERO_ADDRESS,
        tokenId = BigInteger.TEN,
        status = CredentialRegistryStatus.REGISTERED,
        owner = owner,
        registryExpiry = BigInteger.valueOf(IssuerSpace.STAFF_EXPIRY),
        ownerRoleBitmap = BigInteger.ZERO,
        transferable = false,
        accessActive = true,
        accessValidUntil = BigInteger.valueOf(IssuerSpace.STAFF_EXPIRY),
        avatarUri = "",
        description = IssuerSpace.DEFAULT_DESCRIPTION,
        snapshotBlock = BigInteger.ONE,
        snapshotTimestamp = BigInteger.ONE,
        readStatus = read,
        provenanceMatches = true,
        failureCategory = if (read == CredentialReadStatus.FRESH) null else "TIMEOUT",
    )

    private class MemoryStore(var value: String? = null) : LoadableStringStateStore {
        override fun load(): String? = value
        override fun save(value: String) { this.value = value }
    }

    private companion object {
        const val HOLDER = "0x3419148731087b970d2059C53780163B452D5FF7"
        const val ISSUER = "0xFa90e8301A22833B74378C5fA3a7c120Ac512685"
        const val OTHER = "0x0000000000000000000000000000000000000001"
    }
}
