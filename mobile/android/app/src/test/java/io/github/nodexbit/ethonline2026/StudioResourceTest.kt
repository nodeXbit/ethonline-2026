package io.github.nodexbit.ethonline2026

import java.math.BigInteger
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class StudioResourceTest {
    private val now = BigInteger.valueOf(1800000000)
    private val expiry = now + BigInteger.valueOf(600)
    private class Store : LoadableStringStateStore { var value: String? = null; override fun load() = value; override fun save(value: String) { this.value = value } }
    private fun pass() = CredentialSnapshot(IssuerSpace.fullName, CredentialAbi.nodeHex(IssuerSpace.fullName), IssuerSpace.registry,
        IssuerSpace.resolver, IssuerSpace.ZERO_ADDRESS, BigInteger.ONE, CredentialRegistryStatus.REGISTERED, IssuerSpace.STAFF_HOLDER,
        expiry, BigInteger.ZERO, false, true, expiry, "", "Staff", BigInteger.TEN, now, CredentialReadStatus.FRESH, true)
    @Test fun `editable template resources and new TX2 persist exact canonical policy`() {
        for ((template, slugs) in listOf(PassTemplate.STAFF to setOf("front-door", "lab", "server-room"),
                PassTemplate.VISITOR to setOf("front-door"), PassTemplate.CONTRACTOR to setOf("lab"))) {
            val draft = PassTemplateDefaults.apply(template, IssuerSpace.STAFF_HOLDER, now, expiry + BigInteger.valueOf(600))
            assertEquals(slugs, AccessResources.all.filter { it.resourceId in draft.allowedResources }.map { it.slug }.toSet())
            val edited = draft.copy(allowedResources = setOf(AccessResources.all[2].resourceId)).validated(now, expiry + BigInteger.valueOf(1000))
            val store = Store(); val coordinator = IssuanceCoordinator(store)
            val session = coordinator.beginRegister(IssuerSpace.issuer, edited, "register")
            assertEquals(edited.allowedResources, IssuanceCoordinator(store).get(session.identity).allowedResources)
            val calls = CredentialConfigurationPolicy.prepare(session.copy(state = IssuanceState.REGISTERED_CONFIGURING)).calls
            assertEquals(3, calls.size)
            assertTrue(calls.last().contains(AccessResources.encode(edited.allowedResources).drop(2)))
        }
    }
    @Test fun `resource manage review recovery and fresh preflight preserve exact old new intent`() = runBlocking {
        val original = pass().copy(resourcesRaw = AccessResources.encode(setOf(AccessResources.all[0].resourceId)))
        val target = setOf(AccessResources.all[1].resourceId)
        val mutation = PassManagementPolicy.resources(original, target)
        assertEquals("Front Door", mutation.oldValue); assertEquals("Lab", mutation.newValue)
        assertEquals(StudioActionType.RESOURCE_POLICY_UPDATE, mutation.action)
        StudioManagementPreflight.validate(original, mutation, original, expiry + BigInteger.TEN)
        assertTrue(runCatching { StudioManagementPreflight.validate(original, mutation, original.copy(resourcesRaw = "0x"), expiry + BigInteger.TEN) }.isFailure)
        assertTrue(runCatching { StudioManagementPreflight.validate(original, mutation, original.copy(owner = IssuerSpace.issuer), expiry + BigInteger.TEN) }.isFailure)
        val store = Store(); val session = ManagementSession(IssuerSpace.issuer, original.fullName, "resources", mutation)
        ManagementSessionCoordinator(store).save(session)
        val coordinator = ManagementSessionCoordinator(store)
        assertEquals(session, coordinator.current(IssuerSpace.issuer))
        val operation = PersistedTransactionOperation("resources", StudioOperationIdentity.type(mutation.action, original.fullName),
            session.wallet, IssuerSpace.chainId, mutation.target, "0", CredentialAbi.calldataFingerprint(mutation.calldata),
            state = TransactionOperationState.CONFIRMED, receiptBlock = "10", createdAt = 1, updatedAt = 2)
        assertTrue(runCatching { ManagementFinalReadbackReconciler({ original }, {}, 1).finalize(session, operation, coordinator) }.isFailure)
        assertEquals(listOf(session), coordinator.pending())
        ManagementFinalReadbackReconciler({ original.copy(resourcesRaw = AccessResources.encode(target)) }, {}, 1).finalize(session, operation, coordinator)
        assertTrue(coordinator.pending().isEmpty())
    }
    @Test fun `missing policy displays missing and SET_DATA is required independently of presentation`() {
        assertEquals("No resource policy configured", AccessResources.names(pass().allowedResources))
        val denied = StudioCapabilities(IssuerCapabilityState.DENIED, IssuerCapabilityState.DENIED,
            IssuerCapabilityState.DENIED, IssuerCapabilityState.ALLOWED, "fixture")
        assertFalse(StudioActionType.RESOURCE_POLICY_UPDATE in PassManagementPolicy.actions(pass(), denied))
        assertTrue(StudioActionType.RESOURCE_POLICY_UPDATE in PassManagementPolicy.actions(pass(), denied.copy(canManageAccess = IssuerCapabilityState.ALLOWED)))
    }
}
