package io.github.nodexbit.ethonline2026.gate

import io.github.nodexbit.ethonline2026.AccessResources
import io.github.nodexbit.ethonline2026.CredentialAbi
import io.github.nodexbit.ethonline2026.hce.HceApduProcessor
import java.io.IOException
import java.util.concurrent.Executor
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GateReaderControllerTest {
    @Test
    fun `ReaderMode lifecycle enables and disables with active cleanup`() {
        val h = harness(queued = true)
        h.controller.onResume(h.mode)
        h.mode.callback!!.invoke(h.tag)
        h.controller.onPause()
        assertEquals(1, h.mode.enables)
        assertEquals(1, h.mode.disables)
        assertTrue(h.tag.closed)
    }

    @Test
    fun `non IsoDep tag is rejected`() {
        val h = harness()
        h.controller.onResume(h.mode)
        h.mode.callback!!.invoke(null)
        assertEquals("ISO_DEP_REQUIRED", h.events.failure)
        assertEquals(0, h.api.starts)
    }

    @Test
    fun `SELECT status failure stops before credential`() {
        val h = harness(tag = FakeTag(mutableListOf(byteArrayOf(0x6a, 0x82.toByte()))))
        h.controller.onResume(h.mode); h.mode.callback!!.invoke(h.tag)
        assertEquals("SELECT_STATUS", h.events.failure)
        assertEquals(0, h.api.starts)
    }

    @Test
    fun `GET_CREDENTIAL is bounded strict UTF8 and successful flow sends challenge`() {
        val h = harness()
        h.controller.onResume(h.mode); h.mode.callback!!.invoke(h.tag)
        assertEquals(CREDENTIAL, h.events.credential)
        assertEquals(1, h.api.starts)
        assertArrayEquals(GateReaderProtocol.sendChallenge(h.api.challenge), h.tag.commands[2])
        assertEquals("RESOURCE_POLICY_MISSING", h.events.decision?.reason)
        assertTrue(h.tag.closed)
    }

    @Test
    fun `oversized credential fails closed`() {
        val tag = standardTag(credential = ByteArray(65) { 'a'.code.toByte() })
        val h = harness(tag = tag)
        h.controller.onResume(h.mode); h.mode.callback!!.invoke(tag)
        assertEquals("GET_CREDENTIAL_LENGTH", h.events.failure)
    }

    @Test
    fun `signature response must contain exactly 65 bytes plus success`() {
        val tag = standardTag(signature = ByteArray(64))
        val h = harness(tag = tag)
        h.controller.onResume(h.mode); h.mode.callback!!.invoke(tag)
        assertEquals("GET_SIGNATURE_LENGTH", h.events.failure)
        assertEquals(0, h.api.completes)
    }

    @Test
    fun `tag removal and IO exception close safely`() {
        val tag = standardTag().apply { failureAt = 1 }
        val h = harness(tag = tag)
        h.controller.onResume(h.mode); h.mode.callback!!.invoke(tag)
        assertEquals("TRANSPORT_FAILURE", h.events.failure)
        assertTrue(tag.closed)
    }

    @Test
    fun `Activity pause cancels queued attempt and duplicate callback is one session`() {
        val h = harness(queued = true)
        h.controller.onResume(h.mode)
        h.mode.callback!!.invoke(h.tag)
        h.mode.callback!!.invoke(standardTag())
        assertEquals(1, h.queue.size)
        h.controller.onPause()
        assertTrue(h.tag.closed)
    }

    @Test
    fun `Node unavailable is a transport failure without local authorization`() {
        val h = harness(apiFailure = true)
        h.controller.onResume(h.mode); h.mode.callback!!.invoke(h.tag)
        assertEquals("NODE_UNAVAILABLE", h.events.failure)
        assertEquals(null, h.events.decision)
    }

    @Test
    fun `resource selection is latched before asynchronous tag work`() {
        val h = harness(queued = true)
        h.controller.onResume(h.mode)
        assertTrue(h.controller.selectResource(AccessResources.all[1]))
        h.mode.callback!!.invoke(h.tag)
        assertFalse(h.controller.selectResource(AccessResources.all[0]))
        h.queue.single().run()
        assertEquals("lab", h.api.resource)
    }

    @Test
    fun `protocol APDUs reuse frozen HCE constants`() {
        assertArrayEquals(
            byteArrayOf(0x00, 0xa4.toByte(), 0x04, 0x00, 0x08) + HceApduProcessor.AID,
            GateReaderProtocol.select,
        )
        assertArrayEquals(byteArrayOf(0x80.toByte(), 0x40, 0x01, 0x00), GateReaderProtocol.getCredential)
        assertEquals(109, GateReaderProtocol.sendChallenge(ByteArray(104)).size)
    }

    @Test
    fun `transport-only checkpoint stops before challenge or holder signing`() {
        val events = FakeEvents()
        val api = FakeApi(false)
        val controller = GateReaderController(
            api, events, Executor { it.run() }, pause = {}, nowSeconds = { NOW }, transportOnly = true,
        )
        val mode = FakeMode()
        val tag = FakeTag(mutableListOf(
            HceApduProcessor.SUCCESS,
            CREDENTIAL.toByteArray() + HceApduProcessor.SUCCESS,
        ))
        controller.onResume(mode); mode.callback!!.invoke(tag)
        assertEquals(0, api.starts)
        assertEquals("TRANSPORT CONFIRMED — SELECT 9000 — GET_CREDENTIAL 9000", events.state)
        assertTrue(tag.closed)
    }

    private fun harness(
        tag: FakeTag = standardTag(),
        queued: Boolean = false,
        apiFailure: Boolean = false,
    ): Harness {
        val events = FakeEvents()
        val api = FakeApi(apiFailure)
        val queue = mutableListOf<Runnable>()
        val executor = if (queued) Executor { queue += it } else Executor { it.run() }
        val controller = GateReaderController(api, events, executor, pause = {}, nowSeconds = { NOW })
        return Harness(controller, FakeMode(), tag, api, events, queue)
    }

    private class FakeApi(private val fail: Boolean) : PixelGateApi {
        var starts = 0; var completes = 0; var resource: String? = null
        val challenge = challenge()
        override fun start(resource: String, credential: String): PixelGateSession {
            starts++; this.resource = resource
            if (fail) error("NODE_UNAVAILABLE")
            return PixelGateSession("12".repeat(16), challenge, AccessResources.all.first { it.slug == resource }.resourceId)
        }
        override fun complete(sessionId: String, signature: ByteArray): PixelGateDecision {
            completes++
            return PixelGateDecision(false, "RESOURCE_POLICY_MISSING", "Verified", "Valid", "Allowed", "Not checked")
        }
    }

    private class FakeMode : GateReaderMode {
        var enables = 0; var disables = 0; var callback: ((GateTagSession?) -> Unit)? = null
        override fun enable(callback: (GateTagSession?) -> Unit) { enables++; this.callback = callback }
        override fun disable() { disables++ }
    }

    private class FakeEvents : GateReaderEvents {
        var state: String? = null; var credential: String? = null; var failure: String? = null; var decision: PixelGateDecision? = null
        override fun state(value: String) { state = value }
        override fun credential(value: String) { credential = value }
        override fun decision(value: PixelGateDecision) { decision = value }
        override fun failure(reason: String) { failure = reason }
    }

    private class FakeTag(private val responses: MutableList<ByteArray>) : GateTagSession {
        val commands = mutableListOf<ByteArray>(); var closed = false; var failureAt = -1
        override fun connect() = Unit
        override fun transceive(command: ByteArray): ByteArray {
            commands += command.copyOf()
            if (commands.size - 1 == failureAt) throw IOException("tag removed")
            return responses.removeFirst()
        }
        override fun close() { closed = true }
    }

    private data class Harness(
        val controller: GateReaderController, val mode: FakeMode, val tag: FakeTag,
        val api: FakeApi, val events: FakeEvents, val queue: MutableList<Runnable>,
    )

    companion object {
        private const val CREDENTIAL = "staff-001.keys.demo-access.eth"
        private val NOW = 1_800_000_000uL
        private fun challenge(): ByteArray = CredentialAbi.namehash(CREDENTIAL) +
            hex(AccessResources.all[1].resourceId) + ByteArray(32) { 0x5a } +
            byteArrayOf(0, 0, 0, 0, 0x6b, 0x49, 0xd2.toByte(), 0x3c)
        private fun hex(value: String) = value.drop(2).chunked(2).map { it.toInt(16).toByte() }.toByteArray()
        private fun standardTag(
            credential: ByteArray = CREDENTIAL.toByteArray(),
            signature: ByteArray = ByteArray(65) { 0x44 },
        ) = FakeTag(mutableListOf(
            HceApduProcessor.SUCCESS,
            credential + HceApduProcessor.SUCCESS,
            HceApduProcessor.SUCCESS,
            byteArrayOf(HceApduProcessor.State.READY.wireValue) + HceApduProcessor.SUCCESS,
            signature + HceApduProcessor.SUCCESS,
        ))
    }
}
