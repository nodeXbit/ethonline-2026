package io.github.nodexbit.ethonline2026.hce

import io.github.nodexbit.ethonline2026.*
import java.io.File
import java.math.BigInteger
import java.util.Properties
import org.bouncycastle.jcajce.provider.digest.Keccak
import org.junit.Assert.*
import org.junit.Test
import org.web3j.utils.Numeric

class DynamicHceTest {
    private val vectors = Properties().apply { File(checkNotNull(System.getProperty("lockens.vectors"))).reader().use(::load) }
    private fun v(key: String) = vectors.getProperty(key)
    private fun bytes(value: String) = Numeric.hexStringToByteArray(value)
    // Opaque transport test bytes, not a cryptographic signature.
    private val proofBytes = ByteArray(65) { (it + 1).toByte() }
    private val now = v("challenge.now").toLong() * 1000
    private val name = v("credential.name")
    private val binding = StudioWalletBinding(v("holder.address"), "fixture-provider", 1)
    private fun snapshot() = CredentialSnapshot(name, v("credential.node"), IssuerSpace.registry, IssuerSpace.resolver,
        IssuerSpace.ZERO_ADDRESS, BigInteger.ONE, CredentialRegistryStatus.REGISTERED, binding.address,
        BigInteger.valueOf(now / 1000 + 600), BigInteger.ZERO, false, true,
        BigInteger.valueOf(now / 1000 + 300), "", "Fixture", BigInteger.TEN,
        BigInteger.valueOf(now / 1000), CredentialReadStatus.FRESH, true)
    private class Store : LoadableStringStateStore { var value: String? = null; override fun load() = value; override fun save(value: String) { this.value = value } }
    private inner class Fixture {
        var time = now
        var active: StudioWalletBinding? = binding
        var calls = 0
        var completion: ((Result<ByteArray>) -> Unit)? = null
        val state = NfcSelectionState({ time }, { active })
        val provider = ProofProvider { _, done -> calls++; completion = done }
        val processor = HceApduProcessor(PendingProofProvider, state::current) { (time / 1000).toULong() }
        fun publish(pass: CredentialSnapshot = snapshot()) { state.publish(binding, name, listOf(pass), provider) }
        fun select() = processor.process(bytes(v("apdu.select")))
        fun credential() = processor.process(bytes(v("apdu.credential")))
        fun challenge(raw: ByteArray = bytes(v("apdu.challenge"))) = processor.process(raw)
        fun complete() { completion!!(Result.success(proofBytes.copyOf())) }
    }
    @Test fun `GET_CREDENTIAL exact command response and selected pass from wallet partition`() {
        val f = Fixture(); val store = SelectedPassStore(Store())
        store.select(IssuerSpace.chainId, binding.address, snapshot())
        f.state.publish(binding, store.selected(IssuerSpace.chainId, binding.address), listOf(snapshot()), f.provider)
        assertArrayEquals(HceApduProcessor.SUCCESS, f.select())
        assertArrayEquals(bytes(v("apdu.credentialResponse")), f.credential())
        assertEquals(0, f.calls)
    }
    @Test fun `diagnostic summary identifies STAFF binding and clock offset without nonce proof or signing`() {
        val f = Fixture(); f.publish(); f.select(); f.credential()
        val command = bytes(v("apdu.challenge"))
        val summary = f.processor.diagnosticSummary(command)
        assertTrue(summary.contains("credential=$name"))
        assertTrue(summary.contains("credentialMatches=true"))
        assertTrue(summary.contains("resourceKnown=true"))
        assertTrue(summary.contains("remainingSeconds=60"))
        val nonce = HceChallenge.decode(bytes(v("challenge.bytes"))).nonce
        assertFalse(summary.contains(Numeric.toHexStringNoPrefix(nonce)))
        assertFalse(summary.contains(Numeric.toHexStringNoPrefix(proofBytes)))
        f.time -= 1_000
        f.publish(); f.select(); f.credential()
        assertTrue(f.processor.diagnosticSummary(command).contains("remainingSeconds=61"))
        assertEquals(0, f.calls)
        assertArrayEquals(HceApduProcessor.CONDITIONS_NOT_SATISFIED, f.challenge(command))
        assertEquals(0, f.calls)
    }
    @Test fun `diagnostic summary detects credential substitution without changing APDU result`() {
        val f = Fixture(); f.publish(); f.select(); f.credential()
        val command = bytes(v("apdu.challenge")).also { it[5] = (it[5].toInt() xor 1).toByte() }
        assertTrue(f.processor.diagnosticSummary(command).contains("credentialMatches=false"))
        assertArrayEquals(HceApduProcessor.CONDITIONS_NOT_SATISFIED, f.challenge(command))
        assertEquals(0, f.calls)
    }
    @Test fun `no selected pass no ownership and stale state expose no credential`() {
        val f = Fixture(); f.select(); assertArrayEquals(HceApduProcessor.CONDITIONS_NOT_SATISFIED, f.credential())
        f.state.publish(binding, null, listOf(snapshot()), f.provider); assertNull(f.state.current())
        f.publish(snapshot().copy(owner = IssuerSpace.issuer)); assertNull(f.state.current())
        f.publish(); f.time += 61_000; assertNull(f.state.current())
        f.time = now; f.publish(snapshot().copy(registryExpiry = BigInteger.valueOf(now / 1000))); assertNull(f.state.current())
        f.publish(snapshot().copy(provenanceMatches = false)); assertNull(f.state.current())
    }
    @Test fun `wrong AID version and malformed credential command fail without signing`() {
        val f = Fixture(); f.publish()
        val wrong = bytes(v("apdu.select")).also { it[it.lastIndex] = 0x32 }
        assertArrayEquals(HceApduProcessor.NOT_FOUND, f.processor.process(wrong))
        f.select()
        assertArrayEquals(HceApduProcessor.INCORRECT_P1_P2, f.processor.process(bytes("80400200")))
        assertArrayEquals(HceApduProcessor.WRONG_LENGTH, f.processor.process(bytes("8040010001")))
        assertEquals(0, f.calls)
    }
    @Test fun `64 byte credential limit is exact`() {
        val suffix = ".${IssuerSpace.namespace}"
        for (size in listOf(64, 65)) {
            val fullName = "a".repeat(size - suffix.length) + suffix
            val f = Fixture(); val pass = snapshot().copy(fullName = fullName, node = CredentialAbi.nodeHex(fullName))
            f.state.publish(binding, fullName, listOf(pass), f.provider); f.select()
            assertEquals(if (size == 64) 66 else 2, f.credential().size)
        }
    }
    @Test fun `wallet switch before SELECT uses new wallet state with no fallback`() {
        val f = Fixture(); f.publish(); f.active = binding.copy(address = IssuerSpace.issuer, generation = 2)
        f.select(); assertArrayEquals(HceApduProcessor.CONDITIONS_NOT_SATISFIED, f.credential()); assertEquals(0, f.calls)
    }
    @Test fun `wallet switch during session fails closed and discards late signature`() {
        val f = Fixture(); f.publish(); f.select(); f.credential(); f.challenge()
        f.active = binding.copy(generation = 3); f.complete()
        assertArrayEquals(HceApduProcessor.CONDITIONS_NOT_SATISFIED, f.processor.process(bytes("80300100")))
        assertEquals(1, f.calls)
    }
    @Test fun `selection changes during session cannot silently switch credential or revive proof`() {
        val f = Fixture(); f.publish(); f.select(); f.credential()
        val changed = snapshot().copy(fullName = "visitor-001.${IssuerSpace.namespace}")
        f.state.publish(binding, changed.fullName, listOf(changed), f.provider)
        assertArrayEquals(HceApduProcessor.CONDITIONS_NOT_SATISFIED, f.challenge()); assertEquals(0, f.calls)
    }
    @Test fun `selection change while signing rejects late completion and chain substitution fails discovery`() {
        val f = Fixture(); f.publish(); f.select(); f.credential(); f.challenge()
        val changed = "visitor-001.${IssuerSpace.namespace}"
        f.state.publish(binding, changed, listOf(snapshot().copy(fullName = changed, node = CredentialAbi.nodeHex(changed))), f.provider)
        f.complete(); assertArrayEquals(HceApduProcessor.CONDITIONS_NOT_SATISFIED, f.processor.process(bytes("80300100")))
        val wrongChain = HceIdentity(binding, name, 1, f.provider)
        val processor = HceApduProcessor(PendingProofProvider, { wrongChain })
        processor.process(bytes(v("apdu.select")))
        assertArrayEquals(HceApduProcessor.CONDITIONS_NOT_SATISFIED, processor.process(bytes(v("apdu.credential"))))
    }
    @Test fun `challenge requires discovery first and binds namehash known resource expiry TTL`() {
        val noDiscovery = Fixture(); noDiscovery.publish(); noDiscovery.select()
        assertArrayEquals(HceApduProcessor.CONDITIONS_NOT_SATISFIED, noDiscovery.challenge())
        for (offset in listOf(5, 37)) {
            val f = Fixture(); f.publish(); f.select(); f.credential()
            assertArrayEquals(HceApduProcessor.CONDITIONS_NOT_SATISFIED, f.challenge(bytes(v("apdu.challenge")).also { it[offset] = (it[offset].toInt() xor 1).toByte() }))
            assertEquals(0, f.calls)
        }
        for (delta in listOf(60_000L, -1_000L)) {
            val f = Fixture(); f.time += delta; f.publish(snapshot().copy(snapshotTimestamp = BigInteger.valueOf(f.time / 1000)))
            f.select(); f.credential(); assertArrayEquals(HceApduProcessor.CONDITIONS_NOT_SATISFIED, f.challenge()); assertEquals(0, f.calls)
        }
    }
    @Test fun `one challenge per session nonce reuse across SELECT denied and resource policy is not phone authorization`() {
        val f = Fixture(); f.publish(snapshot().copy(resourcesRaw = AccessResources.encode(emptySet())))
        f.select(); f.credential(); assertArrayEquals(HceApduProcessor.SUCCESS, f.challenge()); f.complete()
        assertArrayEquals(proofBytes.copyOf() + HceApduProcessor.SUCCESS, f.processor.process(bytes("80300100")))
        f.select(); f.credential(); assertArrayEquals(HceApduProcessor.CONDITIONS_NOT_SATISFIED, f.challenge()); assertEquals(1, f.calls)
    }
    @Test fun `ownership invalidation clears session and Ready to tap needs service active wallet and freshness`() {
        val f = Fixture(); assertEquals("Select a pass to use NFC", f.state.readyText(true))
        f.publish(); assertEquals("Ready to tap", f.state.readyText(true)); assertEquals("Select a pass to use NFC", f.state.readyText(false))
        f.select(); f.credential(); f.state.clear(); assertArrayEquals(HceApduProcessor.CONDITIONS_NOT_SATISFIED, f.challenge())
        assertEquals("Select a pass to use NFC", f.state.readyText(true))
    }
    @Test fun `cross language resource IDs ABI encoding namehash challenge and EIP712 digest agree`() {
        for (resource in AccessResources.all) assertEquals(v("resource.${resource.slug}"), resource.resourceId)
        val ids = AccessResources.all.map { it.resourceId }.toSet()
        assertEquals(v("policy.all"), AccessResources.encode(ids)); assertEquals(ids, AccessResources.decode(v("policy.all")))
        assertEquals(v("policy.empty"), AccessResources.encode(emptySet())); assertEquals(emptySet<String>(), AccessResources.decode(v("policy.empty")))
        assertNull(AccessResources.decode("0x")); assertEquals(v("credential.node"), CredentialAbi.nodeHex(name))
        val challenge = HceChallenge.decode(bytes(v("challenge.bytes")))
        assertEquals(v("challenge.expiry").toULong(), challenge.expiresAt)
        fun hash(value: ByteArray) = Keccak.Digest256().digest(value)
        fun word(value: Long) = bytes(value.toString(16).padStart(64, '0'))
        val domain = hash(hash("EIP712Domain(string name,string version,uint256 chainId)".toByteArray()) +
            hash(v("domain.name").toByteArray()) + hash(v("domain.version").toByteArray()) + word(IssuerSpace.chainId))
        val message = hash(hash("AccessChallenge(bytes32 credential,bytes32 resource,bytes32 nonce,uint64 expiresAt)".toByteArray()) +
            challenge.credential + challenge.resource + challenge.nonce + word(challenge.expiresAt.toLong()))
        assertEquals(v("challenge.digest"), Numeric.toHexString(hash(byteArrayOf(0x19, 0x01) + domain + message)))
        assertTrue(AccessChallengeTypedData.json(challenge).contains("\"chainId\":11155111"))
    }
    @Test fun `malformed unknown duplicated unsorted or oversized resource policies never decode`() {
        val all = v("policy.all")
        val words = all.drop(2).chunked(64)
        val malformed = listOf("0x01", all + "00", "0x" + words.take(2).joinToString("") + words.drop(2).reversed().joinToString(""),
            "0x" + words.take(2).joinToString("") + words[2].repeat(3), "0x" + words.take(2).joinToString("") + "f".repeat(192))
        malformed.forEach { assertTrue(it, runCatching { AccessResources.decode(it) }.isFailure) }
    }
}
