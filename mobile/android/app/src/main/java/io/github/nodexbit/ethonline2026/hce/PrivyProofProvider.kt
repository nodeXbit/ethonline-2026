package io.github.nodexbit.ethonline2026.hce

import io.privy.sdk.Privy
import io.privy.wallet.ethereum.EmbeddedEthereumWallet
import io.privy.wallet.ethereum.EthereumRpcRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

fun interface AsyncRunner {
    fun launch(block: suspend () -> Unit)
}

class CoroutineAsyncRunner(private val scope: CoroutineScope) : AsyncRunner {
    override fun launch(block: suspend () -> Unit) {
        scope.launch { block() }
    }
}

interface TypedDataSigner {
    val address: String
    suspend fun signTypedData(typedDataJson: String): String
}

fun interface TypedDataSignerSource {
    suspend fun currentSigner(): TypedDataSigner?
}

/** Reads the SDK-managed authenticated session and never creates a wallet. */
class PrivyTypedDataSignerSource(private val privy: Privy) : TypedDataSignerSource {
    override suspend fun currentSigner(): TypedDataSigner? {
        val wallet = privy.getUser()?.embeddedEthereumWallets?.firstOrNull() ?: return null
        return PrivyTypedDataSigner(wallet)
    }
}

private class PrivyTypedDataSigner(
    private val wallet: EmbeddedEthereumWallet,
) : TypedDataSigner {
    override val address: String = wallet.address

    override suspend fun signTypedData(typedDataJson: String): String {
        val request = EthereumRpcRequest.ethSignTypedDataV4(
            address = address,
            typedDataJson = typedDataJson,
        )
        return wallet.provider.request(request).getOrThrow().data
    }
}

class PrivyProofProvider(
    private val asyncRunner: AsyncRunner,
    private val signerSource: TypedDataSignerSource,
) : ProofProvider {
    override fun requestProof(
        challenge: HceChallenge,
        completion: (Result<ByteArray>) -> Unit,
    ) {
        asyncRunner.launch {
            completion(runCatching {
                val signer = signerSource.currentSigner()
                    ?: error("An authenticated embedded Ethereum wallet is required")
                val response = signer.signTypedData(AccessChallengeTypedData.json(challenge))
                decodeSignature(response)
            })
        }
    }

    companion object {
        fun decodeSignature(response: String): ByteArray {
            val hex = response.trim().removeSurrounding("\"")
            require(SIGNATURE_PATTERN.matches(hex)) { "Signer returned a malformed signature" }
            return ByteArray(HceApduProcessor.SIGNATURE_LENGTH) { index ->
                hex.substring(2 + index * 2, 4 + index * 2).toInt(16).toByte()
            }
        }

        fun encodeSignature(signature: ByteArray): String {
            require(signature.size == HceApduProcessor.SIGNATURE_LENGTH)
            return buildString(132) {
                append("0x")
                signature.forEach { append("%02x".format(it.toUByte().toInt())) }
            }
        }

        private val SIGNATURE_PATTERN = Regex("^0x[0-9a-fA-F]{130}$")
    }
}
