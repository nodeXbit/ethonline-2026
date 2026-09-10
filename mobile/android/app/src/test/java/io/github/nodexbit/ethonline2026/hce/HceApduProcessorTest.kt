package io.github.nodexbit.ethonline2026.hce

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test

class HceApduProcessorTest {
    @Test
    fun `correct SELECT succeeds and wrong AID is rejected`() {
        val processor = processor()
        assertArrayEquals(HceApduProcessor.SUCCESS, processor.process(selectApdu()))
        assertArrayEquals(
            HceApduProcessor.NOT_FOUND,
            processor.process(selectApdu(HceApduProcessor.AID.copyOf().also { it[7] = 0x32 })),
        )
    }

    @Test
    fun `wrong CLA and unsupported INS are rejected`() {
        val processor = processor()
        assertArrayEquals(HceApduProcessor.SUCCESS, processor.process(selectApdu()))
        assertArrayEquals(
            HceApduProcessor.CLASS_NOT_SUPPORTED,
            processor.process(byteArrayOf(0x81.toByte(), 0x20, 0x01, 0x00)),
        )
        assertArrayEquals(
            HceApduProcessor.INS_NOT_SUPPORTED,
            processor.process(byteArrayOf(0x80.toByte(), 0x7F, 0x01, 0x00)),
        )
    }

    @Test
    fun `malformed challenge length is rejected and exact body is accepted`() {
        val processor = processor()
        processor.process(selectApdu())
        assertArrayEquals(
            HceApduProcessor.WRONG_LENGTH,
            processor.process(sendChallenge(challengeBody().copyOf(103))),
        )
        assertArrayEquals(HceApduProcessor.SUCCESS, processor.process(sendChallenge(challengeBody())))
    }

    @Test
    fun `challenge fields decode exactly including uint64 big-endian`() {
        val processor = processor()
        val body = challengeBody(expiresAt = ULong.MAX_VALUE)
        processor.process(selectApdu())
        processor.process(sendChallenge(body))

        val decoded = requireNotNull(processor.currentChallenge())
        assertArrayEquals(body.copyOfRange(0, 32), decoded.credential)
        assertArrayEquals(body.copyOfRange(32, 64), decoded.resource)
        assertArrayEquals(body.copyOfRange(64, 96), decoded.nonce)
        assertEquals(ULong.MAX_VALUE, decoded.expiresAt)
    }

    @Test
    fun `SEND enters PROCESSING and status returns compact PROCESSING`() {
        val processor = processor()
        processor.process(selectApdu())
        processor.process(sendChallenge(challengeBody()))
        assertEquals(HceApduProcessor.State.PROCESSING, processor.currentState())
        assertArrayEquals(
            byteArrayOf(HceApduProcessor.State.PROCESSING.wireValue) + HceApduProcessor.SUCCESS,
            processor.process(getStatus()),
        )
    }

    @Test
    fun `test-only completion enters READY and exposes exactly 65 bytes plus 9000`() {
        val provider = DeterministicTestProofProvider()
        val processor = processor(provider)
        processor.process(selectApdu())
        processor.process(sendChallenge(challengeBody()))
        assertArrayEquals(
            HceApduProcessor.CONDITIONS_NOT_SATISFIED,
            processor.process(getSignature()),
        )

        provider.completeLatest()
        assertEquals(HceApduProcessor.State.READY, processor.currentState())
        assertArrayEquals(
            byteArrayOf(HceApduProcessor.State.READY.wireValue) + HceApduProcessor.SUCCESS,
            processor.process(getStatus()),
        )
        val response = processor.process(getSignature())
        assertEquals(67, response.size)
        assertArrayEquals(TEST_SIGNATURE + HceApduProcessor.SUCCESS, response)
        assertArrayEquals(response, processor.process(getSignature()))
    }

    @Test
    fun `new challenge clears prior proof and ignores stale completion`() {
        val provider = DeterministicTestProofProvider()
        val processor = processor(provider)
        processor.process(selectApdu())
        processor.process(sendChallenge(challengeBody(1uL)))
        processor.process(sendChallenge(challengeBody(2uL)))

        provider.complete(0)
        assertEquals(HceApduProcessor.State.PROCESSING, processor.currentState())
        assertArrayEquals(
            HceApduProcessor.CONDITIONS_NOT_SATISFIED,
            processor.process(getSignature()),
        )
        provider.complete(1)
        assertEquals(HceApduProcessor.State.READY, processor.currentState())
        assertEquals(2uL, processor.currentChallenge()?.expiresAt)
    }

    @Test
    fun `new SELECT resets session and cannot expose old proof`() {
        val provider = DeterministicTestProofProvider()
        val processor = processor(provider)
        processor.process(selectApdu())
        processor.process(sendChallenge(challengeBody()))
        provider.completeLatest()
        processor.process(selectApdu())

        assertEquals(HceApduProcessor.State.IDLE, processor.currentState())
        assertEquals(null, processor.currentChallenge())
        assertArrayEquals(
            HceApduProcessor.CONDITIONS_NOT_SATISFIED,
            processor.process(getSignature()),
        )
    }

    @Test
    fun `application commands require SELECT and protocol version one`() {
        val processor = processor()
        assertArrayEquals(
            HceApduProcessor.CONDITIONS_NOT_SATISFIED,
            processor.process(getStatus()),
        )
        processor.process(selectApdu())
        assertArrayEquals(
            HceApduProcessor.INCORRECT_P1_P2,
            processor.process(byteArrayOf(0x80.toByte(), 0x20, 0x02, 0x00)),
        )
    }

    @Test
    fun `malformed APDUs never crash`() {
        val processor = processor()
        val malformed = listOf<ByteArray?>(
            null,
            byteArrayOf(),
            byteArrayOf(0x00),
            byteArrayOf(0x00, 0xA4.toByte(), 0x04),
            byteArrayOf(0x00, 0xA4.toByte(), 0x04, 0x00),
            byteArrayOf(0x00, 0xA4.toByte(), 0x04, 0x00, 0x7F),
        )
        malformed.forEach {
            assertArrayEquals(HceApduProcessor.WRONG_LENGTH, processor.process(it))
        }
    }

    @Test
    fun `sequential challenge sessions stay isolated`() {
        val provider = DeterministicTestProofProvider()
        val processor = processor(provider)
        processor.process(selectApdu())
        processor.process(sendChallenge(challengeBody(10uL)))
        provider.completeLatest()
        assertArrayEquals(TEST_SIGNATURE + HceApduProcessor.SUCCESS, processor.process(getSignature()))

        processor.process(sendChallenge(challengeBody(11uL)))
        assertEquals(HceApduProcessor.State.PROCESSING, processor.currentState())
        assertArrayEquals(
            HceApduProcessor.CONDITIONS_NOT_SATISFIED,
            processor.process(getSignature()),
        )
        provider.completeLatest()
        assertEquals(11uL, processor.currentChallenge()?.expiresAt)
    }

    private fun processor(provider: DeterministicTestProofProvider = DeterministicTestProofProvider()) =
        HceApduProcessor(provider)

    private fun selectApdu(aid: ByteArray = HceApduProcessor.AID): ByteArray =
        byteArrayOf(0x00, 0xA4.toByte(), 0x04, 0x00, aid.size.toByte()) + aid

    private fun sendChallenge(body: ByteArray): ByteArray =
        byteArrayOf(
            HceApduProcessor.APPLICATION_CLA.toByte(),
            HceApduProcessor.INS_SEND_CHALLENGE.toByte(),
            HceApduProcessor.PROTOCOL_VERSION.toByte(),
            0x00,
            body.size.toByte(),
        ) + body

    private fun getStatus(): ByteArray = byteArrayOf(
        HceApduProcessor.APPLICATION_CLA.toByte(),
        HceApduProcessor.INS_GET_STATUS.toByte(),
        HceApduProcessor.PROTOCOL_VERSION.toByte(),
        0x00,
    )

    private fun getSignature(): ByteArray = byteArrayOf(
        HceApduProcessor.APPLICATION_CLA.toByte(),
        HceApduProcessor.INS_GET_SIGNATURE.toByte(),
        HceApduProcessor.PROTOCOL_VERSION.toByte(),
        0x00,
    )

    private fun challengeBody(expiresAt: ULong = 0x0102030405060708uL): ByteArray {
        val body = ByteArray(HceChallenge.BODY_LENGTH)
        for (index in 0 until 32) body[index] = index.toByte()
        for (index in 32 until 64) body[index] = (0x40 + index - 32).toByte()
        for (index in 64 until 96) body[index] = (0x80 + index - 64).toByte()
        var value = expiresAt
        for (index in 103 downTo 96) {
            body[index] = value.toByte()
            value = value shr 8
        }
        return body
    }

    private class DeterministicTestProofProvider : ProofProvider {
        private val completions = mutableListOf<(Result<ByteArray>) -> Unit>()

        override fun requestProof(
            challenge: HceChallenge,
            completion: (Result<ByteArray>) -> Unit,
        ) {
            completions += completion
        }

        fun complete(index: Int) {
            completions[index](Result.success(TEST_SIGNATURE.copyOf()))
        }

        fun completeLatest() {
            complete(completions.lastIndex)
        }
    }

    companion object {
        // Obvious fixture bytes only; this is not generated by or associated with a key.
        val TEST_SIGNATURE = ByteArray(HceApduProcessor.SIGNATURE_LENGTH) { (it + 1).toByte() }
    }
}
