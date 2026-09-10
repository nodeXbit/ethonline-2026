package io.github.nodexbit.ethonline2026.hce

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GateC2IntegrationTest {
    @Test
    fun `HCE challenge maps exactly to canonical Gate A typed data`() {
        val json = AccessChallengeTypedData.json(GateC2TestVector.challenge())

        assertEquals(EXPECTED_TYPED_DATA, json)
        assertFalse(json.contains("verifyingContract"))
    }

    @Test
    fun `unsigned big-endian uint64 maps safely to typed-data decimal`() {
        val body = GateC2TestVector.challengeBody()
        for (index in 96 until 104) body[index] = 0xff.toByte()

        val challenge = HceChallenge.decode(body)

        assertEquals(ULong.MAX_VALUE, challenge.expiresAt)
        assertTrue(
            AccessChallengeTypedData.json(challenge)
                .contains("\"expiresAt\":\"18446744073709551615\""),
        )
    }

    @Test
    fun `successful async signer moves PROCESSING to READY with exact proof`() {
        val fixture = ByteArray(65) { (0xa0 + it).toByte() }
        val runner = QueuedAsyncRunner()
        val signer = FakeSigner(results = ArrayDeque(listOf(Result.success(hex(fixture)))))
        val processor = processor(runner, TypedDataSignerSource { signer })

        selectAndSend(processor)
        assertEquals(HceApduProcessor.State.PROCESSING, processor.currentState())
        runner.run(0)

        assertEquals(HceApduProcessor.State.READY, processor.currentState())
        assertArrayEquals(fixture + HceApduProcessor.SUCCESS, processor.process(signatureApdu()))
        assertEquals(EXPECTED_TYPED_DATA, signer.typedData.single())
    }

    @Test
    fun `malformed signer output moves PROCESSING to ERROR`() {
        val runner = QueuedAsyncRunner()
        val signer = FakeSigner(results = ArrayDeque(listOf(Result.success("0x1234"))))
        val processor = processor(runner, TypedDataSignerSource { signer })

        selectAndSend(processor)
        runner.run(0)

        assertEquals(HceApduProcessor.State.ERROR, processor.currentState())
        assertArrayEquals(
            HceApduProcessor.CONDITIONS_NOT_SATISFIED,
            processor.process(signatureApdu()),
        )
    }

    @Test
    fun `signer failure moves PROCESSING to ERROR`() {
        val runner = QueuedAsyncRunner()
        val signer = FakeSigner(results = ArrayDeque(listOf(Result.failure(TestFailure()))))
        val processor = processor(runner, TypedDataSignerSource { signer })

        selectAndSend(processor)
        runner.run(0)

        assertEquals(HceApduProcessor.State.ERROR, processor.currentState())
    }

    @Test
    fun `unauthenticated or no-wallet source fails safely without wallet creation`() {
        val runner = QueuedAsyncRunner()
        var sourceReads = 0
        val processor = processor(runner, TypedDataSignerSource {
            sourceReads += 1
            null
        })

        selectAndSend(processor)
        runner.run(0)

        assertEquals(1, sourceReads)
        assertEquals(HceApduProcessor.State.ERROR, processor.currentState())
        assertArrayEquals(
            HceApduProcessor.CONDITIONS_NOT_SATISFIED,
            processor.process(signatureApdu()),
        )
    }

    @Test
    fun `old signer completion after new challenge cannot install stale proof`() {
        val runner = QueuedAsyncRunner()
        val signer = FakeSigner(
            results = ArrayDeque(
                listOf(Result.success(hex(ByteArray(65) { 1 })), Result.success(hex(ByteArray(65) { 2 }))),
            ),
        )
        val processor = processor(runner, TypedDataSignerSource { signer })
        processor.process(GateC2ManualHarness.selectApdu())
        processor.process(GateC2ManualHarness.sendChallengeApdu())
        processor.process(GateC2ManualHarness.sendChallengeApdu())

        runner.run(0)
        assertEquals(HceApduProcessor.State.PROCESSING, processor.currentState())
        runner.run(1)

        assertEquals(HceApduProcessor.State.READY, processor.currentState())
        assertArrayEquals(ByteArray(65) { 2 } + HceApduProcessor.SUCCESS, processor.process(signatureApdu()))
    }

    @Test
    fun `old signer completion after SELECT reset cannot install proof`() {
        val runner = QueuedAsyncRunner()
        val signer = successfulSigner()
        val processor = processor(runner, TypedDataSignerSource { signer })
        selectAndSend(processor)

        processor.process(GateC2ManualHarness.selectApdu())
        runner.run(0)

        assertEquals(HceApduProcessor.State.IDLE, processor.currentState())
    }

    @Test
    fun `old signer completion after HCE deactivation reset cannot install proof`() {
        val runner = QueuedAsyncRunner()
        val signer = successfulSigner()
        val processor = processor(runner, TypedDataSignerSource { signer })
        selectAndSend(processor)

        processor.reset() // GateC1HostApduService.onDeactivated delegates here.
        runner.run(0)

        assertEquals(HceApduProcessor.State.IDLE, processor.currentState())
        assertArrayEquals(
            HceApduProcessor.CONDITIONS_NOT_SATISFIED,
            processor.process(signatureApdu()),
        )
    }

    @Test
    fun `APDU v1 bytes and 104-byte challenge remain frozen`() {
        assertEquals("F0454E5356324331", HceApduProcessor.AID_HEX)
        assertArrayEquals(
            byteArrayOf(0x00, 0xa4.toByte(), 0x04, 0x00, 0x08) + HceApduProcessor.AID,
            GateC2ManualHarness.selectApdu(),
        )
        val send = GateC2ManualHarness.sendChallengeApdu()
        assertEquals(109, send.size)
        assertArrayEquals(
            byteArrayOf(0x80.toByte(), 0x10, 0x01, 0x00, 0x68),
            send.copyOfRange(0, 5),
        )
        assertArrayEquals(GateC2TestVector.challengeBody(), send.copyOfRange(5, send.size))
        assertArrayEquals(
            byteArrayOf(0x80.toByte(), 0x20, 0x01, 0x00),
            GateC2ManualHarness.statusApdu(),
        )
        assertArrayEquals(
            byteArrayOf(0x80.toByte(), 0x30, 0x01, 0x00),
            GateC2ManualHarness.signatureApdu(),
        )
    }

    private fun processor(runner: AsyncRunner, source: TypedDataSignerSource) =
        HceApduProcessor(PrivyProofProvider(runner, source))

    private fun selectAndSend(processor: HceApduProcessor) {
        assertArrayEquals(HceApduProcessor.SUCCESS, processor.process(GateC2ManualHarness.selectApdu()))
        assertArrayEquals(
            HceApduProcessor.SUCCESS,
            processor.process(GateC2ManualHarness.sendChallengeApdu()),
        )
    }

    private fun signatureApdu() = GateC2ManualHarness.signatureApdu()

    private fun successfulSigner() = FakeSigner(
        results = ArrayDeque(listOf(Result.success(hex(ByteArray(65) { 0x5a })))),
    )

    private fun hex(bytes: ByteArray): String = PrivyProofProvider.encodeSignature(bytes)

    private class QueuedAsyncRunner : AsyncRunner {
        private val blocks = mutableListOf<suspend () -> Unit>()

        override fun launch(block: suspend () -> Unit) {
            blocks += block
        }

        fun run(index: Int) = runBlocking { blocks[index]() }
    }

    private class FakeSigner(
        private val results: ArrayDeque<Result<String>>,
    ) : TypedDataSigner {
        override val address = "0x1111111111111111111111111111111111111111"
        val typedData = mutableListOf<String>()

        override suspend fun signTypedData(typedDataJson: String): String {
            typedData += typedDataJson
            return results.removeFirst().getOrThrow()
        }
    }

    private class TestFailure : RuntimeException()

    companion object {
        private val EXPECTED_TYPED_DATA =
            """{"types":{"EIP712Domain":[{"name":"name","type":"string"},{"name":"version","type":"string"},{"name":"chainId","type":"uint256"}],"AccessChallenge":[{"name":"credential","type":"bytes32"},{"name":"resource","type":"bytes32"},{"name":"nonce","type":"bytes32"},{"name":"expiresAt","type":"uint64"}]},"primaryType":"AccessChallenge","domain":{"name":"ENSv2 Access","version":"1","chainId":11155111},"message":{"credential":"0x${"11".repeat(32)}","resource":"0x${"22".repeat(32)}","nonce":"0x${"33".repeat(32)}","expiresAt":"1700000060"}}"""
    }
}
