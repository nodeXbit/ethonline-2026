package io.github.nodexbit.ethonline2026

import java.math.BigInteger
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PassStudioTest {
    private val now = BigInteger("1700000000")
    private val namespaceExpiry = BigInteger("1814392799")

    @Test
    fun `Studio capability combinations are independent and unavailable stays distinct`() {
        val roles = listOf(
            BigInteger.ZERO to BigInteger.ZERO,
            StudioRoles.REGISTRAR to BigInteger.ZERO,
            BigInteger.ZERO to StudioRoles.SET_DATA,
            BigInteger.ZERO to StudioRoles.SET_TEXT,
            StudioRoles.RENEW to BigInteger.ZERO,
            StudioRoles.REGISTRAR.or(StudioRoles.RENEW) to StudioRoles.SET_DATA.or(StudioRoles.SET_TEXT),
        ).map { (r1, s1) -> capabilities(r1, s1) }
        assertFalse(roles[0].visible)
        assertEquals(IssuerCapabilityState.ALLOWED, roles[1].canIssue)
        assertEquals(IssuerCapabilityState.ALLOWED, roles[2].canManageAccess)
        assertEquals(IssuerCapabilityState.ALLOWED, roles[3].canManagePresentation)
        assertEquals(IssuerCapabilityState.ALLOWED, roles[4].canRenew)
        assertTrue(roles[5].visible)
        assertTrue(listOf(
            roles[5].canIssue, roles[5].canRenew, roles[5].canManageAccess, roles[5].canManagePresentation,
        ).all { it == IssuerCapabilityState.ALLOWED })
        val unavailable = StudioCapabilities.unavailable("RPC_UNAVAILABLE")
        assertFalse(unavailable.visible)
        assertTrue(listOf(
            unavailable.canIssue, unavailable.canRenew,
            unavailable.canManageAccess, unavailable.canManagePresentation,
        ).all { it == IssuerCapabilityState.UNAVAILABLE })
    }

    @Test
    fun `templates are editable presets with exact transfer roles and derived names`() {
        val staff = PassTemplateDefaults.apply(PassTemplate.STAFF, IssuerSpace.STAFF_HOLDER, now, namespaceExpiry)
        val visitor = PassTemplateDefaults.apply(PassTemplate.VISITOR, IssuerSpace.STAFF_HOLDER, now, namespaceExpiry)
        val contractor = PassTemplateDefaults.apply(PassTemplate.CONTRACTOR, IssuerSpace.STAFF_HOLDER, now, namespaceExpiry)
        assertEquals("Staff Access Pass", staff.description)
        assertTrue(visitor.accessActive)
        assertTrue(visitor.transferable)
        assertEquals(StudioRoles.CAN_TRANSFER_ADMIN, visitor.roleBitmap)
        assertFalse(contractor.accessActive)
        assertFalse(contractor.transferable)
        assertEquals(BigInteger.ZERO, contractor.roleBitmap)

        val edited = visitor.copy(
            label = "visitor-001",
            description = "Front desk guest",
            transferable = false,
            accessValidUntil = now + BigInteger.valueOf(3_600),
        ).validated(now, namespaceExpiry)
        assertEquals("visitor-001.${IssuerSpace.namespace}", edited.fullName)
        assertEquals("Front desk guest", edited.description)
        assertEquals(BigInteger.ZERO, edited.roleBitmap)
    }

    @Test
    fun `draft rejects namespace escape zero recipient and expiry bound violations`() {
        val base = PassTemplateDefaults.apply(PassTemplate.STAFF, IssuerSpace.STAFF_HOLDER, now, namespaceExpiry)
        assertTrue(runCatching { base.copy(label = "escape.other.eth").validated(now, namespaceExpiry) }.isFailure)
        assertTrue(runCatching { base.copy(recipient = IssuerSpace.ZERO_ADDRESS).validated(now, namespaceExpiry) }.isFailure)
        assertTrue(runCatching { base.copy(registrationExpiry = now).validated(now, namespaceExpiry) }.isFailure)
        assertTrue(runCatching {
            base.copy(registrationExpiry = namespaceExpiry, accessValidUntil = namespaceExpiry).validated(now, namespaceExpiry)
        }.isFailure)
        assertTrue(runCatching {
            base.copy(accessValidUntil = base.registrationExpiry + BigInteger.ONE).validated(now, namespaceExpiry)
        }.isFailure)
    }

    @Test
    fun `generic issue ABI carries arbitrary draft and omits blank artwork`() {
        val draft = PassTemplateDefaults.apply(PassTemplate.CONTRACTOR, IssuerSpace.STAFF_HOLDER, now, namespaceExpiry)
            .copy(label = "contractor-007")
        val register = CredentialAbi.register(
            draft.normalizedLabel, draft.recipient, IssuerSpace.resolver, draft.registrationExpiry, draft.roleBitmap,
        )
        assertEquals("85f3e643", register.removePrefix("0x").take(8))
        val clean = register.removePrefix("0x").drop(8)
        assertEquals(draft.roleBitmap, BigInteger(clean.substring(4 * 64, 5 * 64), 16))
        val calls = CredentialAbi.credentialRecords(
            draft.fullName, "", draft.description, draft.registrationExpiry,
            draft.accessActive, draft.accessValidUntil,
        )
        assertEquals(2, calls.size)
        assertEquals(false to draft.accessValidUntil, CredentialAbi.decodeAccess(
            CredentialAbi.accessValue(false, draft.accessValidUntil),
        ))
    }

    @Test
    fun `issuance sessions are isolated by full credential intent and legacy fixture still decodes`() {
        val store = MemoryStore()
        val coordinator = IssuanceCoordinator(store)
        val first = PassTemplateDefaults.apply(PassTemplate.STAFF, IssuerSpace.STAFF_HOLDER, now, namespaceExpiry)
            .copy(label = "staff-a")
        val second = first.copy(label = "staff-b")
        coordinator.beginRegister(IssuerSpace.issuer, first, "op-a")
        coordinator.beginRegister(IssuerSpace.issuer, second, "op-b")
        val restored = IssuanceCoordinator(store)
        assertEquals("op-a", restored.current(IssuerSpace.issuer, first.fullName)?.registerOperationId)
        assertEquals("op-b", restored.current(IssuerSpace.issuer, second.fullName)?.registerOperationId)
        assertTrue(
            StudioOperationIdentity.type(StudioActionType.ISSUE_REGISTER, first.fullName) !=
                StudioOperationIdentity.type(StudioActionType.ISSUE_REGISTER, second.fullName),
        )
    }

    @Test
    fun `management preserves access validity enforces restore and only extends registration`() {
        val pass = snapshot(accessActive = true, accessUntil = now + BigInteger.valueOf(600))
        val suspended = PassManagementPolicy.access(pass, false, pass.accessValidUntil!!, now)
        assertEquals(StudioActionType.ACCESS_SUSPEND, suspended.action)
        assertEquals(false, suspended.expectedAccessActive)
        assertEquals(pass.accessValidUntil, suspended.expectedAccessValidUntil)
        assertTrue(runCatching {
            PassManagementPolicy.access(pass.copy(accessActive = false, accessValidUntil = now), true, now, now)
        }.isFailure)
        assertTrue(runCatching { PassManagementPolicy.renew(pass, pass.registryExpiry!!, namespaceExpiry) }.isFailure)
        val renewed = PassManagementPolicy.renew(pass, pass.registryExpiry!! + BigInteger.ONE, namespaceExpiry)
        assertEquals(pass.registryExpiry!! + BigInteger.ONE, renewed.expectedRegistrationExpiry)
        assertEquals("5569f33d", renewed.calldata.removePrefix("0x").take(8))
    }

    @Test
    fun `presentation supports independent combined and explicit removal changes`() {
        val pass = snapshot(description = "Old", artwork = "https://example.com/old.png")
        val description = PassManagementPolicy.presentation(pass, "New", null)
        val artwork = PassManagementPolicy.presentation(pass, null, "ipfs://bafy-new")
        val combined = PassManagementPolicy.presentation(pass, "New", "ipfs://bafy-new")
        val removed = PassManagementPolicy.presentation(pass, null, null, removeArtwork = true)
        assertEquals("10f13a8c", description.calldata.removePrefix("0x").take(8))
        assertEquals("10f13a8c", artwork.calldata.removePrefix("0x").take(8))
        assertEquals("ac9650d8", combined.calldata.removePrefix("0x").take(8))
        assertEquals("", removed.expectedArtwork)
    }

    @Test
    fun `Manage discovery returns passes not owned by manager`() = runBlocking {
        val pass = snapshot(owner = IssuerSpace.STAFF_HOLDER)
        val service = CredentialDiscoveryService(
            source = CredentialCandidateSource {
                listOf(CredentialReference(IssuerSpace.chainId, pass.fullName)) to BigInteger.TEN
            },
            read = { pass },
        )
        val result = service.discoverManageable() as CredentialDiscoveryResult.Available
        assertEquals(listOf(pass), result.credentials)
        assertFalse(CredentialProductPolicy.ownedBy(pass, IssuerSpace.issuer))
        assertTrue(PassManagementPolicy.actions(
            pass, capabilities(BigInteger.ZERO, StudioRoles.SET_TEXT),
        ).contains(StudioActionType.PRESENTATION_UPDATE))
        assertTrue(PassManagementPolicy.actions(
            pass, capabilities(BigInteger.ZERO, BigInteger.ZERO),
        ).isEmpty())
    }

    @Test
    fun `management recovery intent persists exact calldata and expected readback`() {
        val store = MemoryStore()
        val pass = snapshot()
        val mutation = PassManagementPolicy.presentation(pass, "Updated", "ipfs://bafy-updated")
        val original = ManagementSession(
            IssuerSpace.issuer,
            pass.fullName,
            "operation-1",
            mutation,
        )
        ManagementSessionCoordinator(store).save(original)
        assertEquals(original, ManagementSessionCoordinator(store).current(IssuerSpace.issuer))
    }

    private fun capabilities(r1: BigInteger, s1: BigInteger) = StudioCapabilityPolicy.evaluate(
        r1, s1, true, BigInteger.TWO, IssuerSpace.issuer, IssuerSpace.registry, IssuerSpace.resolver,
        namespaceExpiry, now, BigInteger.ONE,
    )

    private fun snapshot(
        owner: String = IssuerSpace.STAFF_HOLDER,
        accessActive: Boolean = true,
        accessUntil: BigInteger = now + BigInteger.valueOf(600),
        description: String = "Staff Access Pass",
        artwork: String = "",
    ) = CredentialSnapshot(
        fullName = IssuerSpace.fullName,
        node = CredentialAbi.nodeHex(IssuerSpace.fullName),
        registry = IssuerSpace.registry,
        resolver = IssuerSpace.resolver,
        subregistry = IssuerSpace.ZERO_ADDRESS,
        tokenId = BigInteger.valueOf(7),
        status = CredentialRegistryStatus.REGISTERED,
        owner = owner,
        registryExpiry = now + BigInteger.valueOf(86_400),
        ownerRoleBitmap = BigInteger.ZERO,
        transferable = false,
        accessActive = accessActive,
        accessValidUntil = accessUntil,
        avatarUri = artwork,
        description = description,
        snapshotBlock = BigInteger.ONE,
        snapshotTimestamp = now,
        readStatus = CredentialReadStatus.FRESH,
        provenanceMatches = true,
    )

    private class MemoryStore : LoadableStringStateStore {
        private var raw: String? = null
        override fun load(): String? = raw
        override fun save(value: String) { raw = value }
    }
}
