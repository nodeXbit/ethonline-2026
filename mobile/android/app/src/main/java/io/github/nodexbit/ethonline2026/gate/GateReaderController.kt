package io.github.nodexbit.ethonline2026.gate

import io.github.nodexbit.ethonline2026.AccessResource
import io.github.nodexbit.ethonline2026.CredentialAbi
import io.github.nodexbit.ethonline2026.CredentialValidation
import io.github.nodexbit.ethonline2026.hce.HceApduProcessor
import java.nio.ByteBuffer
import java.nio.charset.CodingErrorAction
import java.util.concurrent.Executor

interface GateTagSession {
    fun connect()
    fun transceive(command: ByteArray): ByteArray
    fun close()
}

interface GateReaderMode {
    fun enable(callback: (GateTagSession?) -> Unit)
    fun disable()
}

interface GateReaderEvents {
    fun state(value: String)
    fun credential(value: String)
    fun decision(value: PixelGateDecision)
    fun failure(reason: String)
}

class GateReaderController(
    private val api: PixelGateApi,
    private val events: GateReaderEvents,
    private val executor: Executor,
    private val pause: (Long) -> Unit = Thread::sleep,
    private val nowSeconds: () -> ULong = { (System.currentTimeMillis() / 1_000).toULong() },
    private val transportOnly: Boolean = false,
) {
    private var readerMode: GateReaderMode? = null
    private var active: GateTagSession? = null
    private var resumed = false
    private var busy = false
    private var generation = 0L
    private var selected = io.github.nodexbit.ethonline2026.AccessResources.all[1]

    @Synchronized
    fun selectResource(resource: AccessResource): Boolean {
        if (busy) return false
        selected = resource
        return true
    }

    @Synchronized
    fun onResume(mode: GateReaderMode) {
        if (resumed) return
        resumed = true
        readerMode = mode
        mode.enable(::onTag)
        events.state("READY — TAP LOCKENS PASS")
    }

    @Synchronized
    fun onPause() {
        generation += 1
        resumed = false
        busy = false
        readerMode?.disable()
        readerMode = null
        closeActive()
    }

    fun onTag(tag: GateTagSession?) {
        val session = tag ?: run {
            events.failure("ISO_DEP_REQUIRED")
            return
        }
        val resource: AccessResource
        val attempt: Long
        synchronized(this) {
            if (!resumed || busy) return
            busy = true
            active = session
            resource = selected
            attempt = ++generation
        }
        executor.execute { runAttempt(session, resource, attempt) }
    }

    private fun runAttempt(tag: GateTagSession, resource: AccessResource, attempt: Long) {
        try {
            events.state("HOLDER — VERIFYING…")
            tag.connect()
            GateReaderProtocol.requireSuccess(tag.transceive(GateReaderProtocol.select), 0..0, "SELECT")
            val credentialBytes = GateReaderProtocol.requireSuccess(
                tag.transceive(GateReaderProtocol.getCredential), 1..64, "GET_CREDENTIAL",
            )
            val credential = Charsets.UTF_8.newDecoder()
                .onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT)
                .decode(ByteBuffer.wrap(credentialBytes)).toString()
            require(CredentialValidation.normalizeFullName(credential) == credential) { "INVALID_CREDENTIAL" }
            events.credential(credential)
            if (transportOnly) {
                events.state("TRANSPORT CONFIRMED — SELECT 9000 — GET_CREDENTIAL 9000")
                return
            }

            val session = api.start(resource.slug, credential)
            require(session.resourceId == resource.resourceId) { "RESOURCE_LATCH_FAILED" }
            require(session.challenge.copyOfRange(0, 32).contentEquals(CredentialAbi.namehash(credential))) {
                "CREDENTIAL_BINDING_FAILED"
            }
            require(GateReaderProtocol.hex(session.challenge.copyOfRange(32, 64)) == resource.resourceId) {
                "RESOURCE_LATCH_FAILED"
            }
            val expiresAt = GateReaderProtocol.expiresAt(session.challenge)
            require(expiresAt > nowSeconds() && expiresAt - nowSeconds() <= 60uL) { "CHALLENGE_EXPIRED" }
            GateReaderProtocol.requireSuccess(
                tag.transceive(GateReaderProtocol.sendChallenge(session.challenge)), 0..0, "SEND_CHALLENGE",
            )

            var ready = false
            var polls = 0
            while (!ready && polls++ < 120) {
                ensureCurrent(attempt)
                val status = GateReaderProtocol.requireSuccess(
                    tag.transceive(GateReaderProtocol.getStatus), 1..1, "GET_STATUS",
                ).single().toUByte().toInt()
                if (status == HceApduProcessor.State.READY.wireValue.toUByte().toInt()) {
                    ready = true
                    continue
                }
                require(status == HceApduProcessor.State.PROCESSING.wireValue.toUByte().toInt()) {
                    "HOLDER_PROOF_FAILED"
                }
                pause(250)
            }
            require(ready) { "HOLDER_PROOF_TIMEOUT" }
            val signature = GateReaderProtocol.requireSuccess(
                tag.transceive(GateReaderProtocol.getSignature),
                HceApduProcessor.SIGNATURE_LENGTH..HceApduProcessor.SIGNATURE_LENGTH,
                "GET_SIGNATURE",
            )
            ensureCurrent(attempt)
            events.decision(api.complete(session.sessionId, signature))
        } catch (error: Exception) {
            events.failure(error.message?.takeIf { it.matches(Regex("^[A-Z0-9_— ]{1,64}$")) } ?: "TRANSPORT_FAILURE")
        } finally {
            runCatching { tag.close() }
            synchronized(this) {
                if (active === tag) active = null
                if (generation == attempt) busy = false
            }
        }
    }

    @Synchronized
    private fun ensureCurrent(attempt: Long) {
        require(resumed && generation == attempt) { "ACTIVITY_PAUSED" }
    }

    @Synchronized
    private fun closeActive() {
        runCatching { active?.close() }
        active = null
    }
}
