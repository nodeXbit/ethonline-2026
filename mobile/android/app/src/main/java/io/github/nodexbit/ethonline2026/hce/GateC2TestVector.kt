package io.github.nodexbit.ethonline2026.hce

/** Public deterministic manual-test vector; it contains no credential or secret. */
object GateC2TestVector {
    const val EXPIRES_AT = 1_700_000_060uL

    fun challengeBody(): ByteArray = ByteArray(HceChallenge.BODY_LENGTH).also { body ->
        body.fill(0x11, 0, 32)
        body.fill(0x22, 32, 64)
        body.fill(0x33, 64, 96)
        var value = EXPIRES_AT
        for (index in 103 downTo 96) {
            body[index] = value.toByte()
            value = value shr 8
        }
    }

    fun challenge(): HceChallenge = HceChallenge.decode(challengeBody())
}
