package io.github.nodexbit.ethonline2026.hce

/**
 * Gate A challenge fields in Gate C1 wire order:
 * credential[32] || resource[32] || nonce[32] || expiresAt uint64 big-endian.
 */
class HceChallenge private constructor(
    credential: ByteArray,
    resource: ByteArray,
    nonce: ByteArray,
    val expiresAt: ULong,
) {
    val credential: ByteArray = credential.copyOf()
    val resource: ByteArray = resource.copyOf()
    val nonce: ByteArray = nonce.copyOf()

    fun copy(): HceChallenge = HceChallenge(credential, resource, nonce, expiresAt)

    companion object {
        const val BODY_LENGTH = 104

        fun decode(body: ByteArray): HceChallenge {
            require(body.size == BODY_LENGTH) { "Gate C1 challenge must be 104 bytes" }
            var expiresAt = 0uL
            for (index in 96 until BODY_LENGTH) {
                expiresAt = (expiresAt shl 8) or body[index].toUByte().toULong()
            }
            return HceChallenge(
                credential = body.copyOfRange(0, 32),
                resource = body.copyOfRange(32, 64),
                nonce = body.copyOfRange(64, 96),
                expiresAt = expiresAt,
            )
        }
    }
}
