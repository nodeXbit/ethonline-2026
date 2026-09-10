package io.github.nodexbit.ethonline2026.hce

/** Gate C2 can replace this provider without changing the APDU V1 contract. */
fun interface ProofProvider {
    fun requestProof(challenge: HceChallenge, completion: (Result<ByteArray>) -> Unit)
}

/** Gate C1 production code deliberately performs no signing and remains PROCESSING. */
object PendingProofProvider : ProofProvider {
    override fun requestProof(
        challenge: HceChallenge,
        completion: (Result<ByteArray>) -> Unit,
    ) = Unit
}
