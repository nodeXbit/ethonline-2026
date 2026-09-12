package io.github.nodexbit.ethonline2026.gate

import io.github.nodexbit.ethonline2026.hce.HceApduProcessor
import io.github.nodexbit.ethonline2026.hce.HceChallenge
import java.nio.ByteBuffer

object GateReaderProtocol {
    val select: ByteArray = byteArrayOf(0x00, 0xa4.toByte(), 0x04, 0x00, HceApduProcessor.AID.size.toByte()) +
        HceApduProcessor.AID
    val getCredential: ByteArray = command(HceApduProcessor.INS_GET_CREDENTIAL)
    val getStatus: ByteArray = command(HceApduProcessor.INS_GET_STATUS)
    val getSignature: ByteArray = command(HceApduProcessor.INS_GET_SIGNATURE)

    fun sendChallenge(body: ByteArray): ByteArray {
        require(body.size == HceChallenge.BODY_LENGTH) { "INVALID_CHALLENGE" }
        return byteArrayOf(
            HceApduProcessor.APPLICATION_CLA.toByte(),
            HceApduProcessor.INS_SEND_CHALLENGE.toByte(),
            HceApduProcessor.PROTOCOL_VERSION.toByte(),
            0x00,
            HceChallenge.BODY_LENGTH.toByte(),
        ) + body
    }

    fun requireSuccess(response: ByteArray, expectedBytes: IntRange, stage: String): ByteArray {
        require(response.size >= 2 && response.takeLast(2).toByteArray().contentEquals(HceApduProcessor.SUCCESS)) {
            "${stage}_STATUS"
        }
        val body = response.copyOf(response.size - 2)
        require(body.size in expectedBytes) { "${stage}_LENGTH" }
        return body
    }

    fun decodeChallenge(value: String): ByteArray {
        require(value.matches(Regex("^0x[0-9a-fA-F]{208}$"))) { "INVALID_CHALLENGE" }
        return value.drop(2).chunked(2).map { it.toInt(16).toByte() }.toByteArray()
    }

    fun expiresAt(body: ByteArray): ULong = ByteBuffer.wrap(body, 96, 8).long.toULong()

    fun hex(bytes: ByteArray): String = "0x" + bytes.joinToString("") { "%02x".format(it.toInt() and 0xff) }

    private fun command(instruction: Int) = byteArrayOf(
        HceApduProcessor.APPLICATION_CLA.toByte(), instruction.toByte(),
        HceApduProcessor.PROTOCOL_VERSION.toByte(), 0x00,
    )
}
