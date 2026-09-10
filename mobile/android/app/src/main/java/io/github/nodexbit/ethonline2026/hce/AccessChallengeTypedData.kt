package io.github.nodexbit.ethonline2026.hce

/** Canonical Gate A EIP-712 representation shared by Gate B and Gate C2 signing. */
object AccessChallengeTypedData {
    fun json(challenge: HceChallenge): String =
        """{"types":{"EIP712Domain":[{"name":"name","type":"string"},{"name":"version","type":"string"},{"name":"chainId","type":"uint256"}],"AccessChallenge":[{"name":"credential","type":"bytes32"},{"name":"resource","type":"bytes32"},{"name":"nonce","type":"bytes32"},{"name":"expiresAt","type":"uint64"}]},"primaryType":"AccessChallenge","domain":{"name":"ENSv2 Access","version":"1","chainId":11155111},"message":{"credential":"${challenge.credential.toHex()}","resource":"${challenge.resource.toHex()}","nonce":"${challenge.nonce.toHex()}","expiresAt":"${challenge.expiresAt}"}}"""

    private fun ByteArray.toHex(): String = buildString(2 + size * 2) {
        append("0x")
        for (byte in this@toHex) {
            val value = byte.toUByte().toInt()
            append(HEX[value ushr 4])
            append(HEX[value and 0x0f])
        }
    }

    private const val HEX = "0123456789abcdef"
}
