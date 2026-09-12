package io.github.nodexbit.ethonline2026

import java.math.BigInteger
import java.nio.ByteBuffer
import java.nio.charset.CodingErrorAction
import java.nio.charset.StandardCharsets
import kotlinx.coroutines.CancellationException
import org.bouncycastle.jcajce.provider.digest.Keccak

data class R1RegistrationCandidate(
    val tokenId: BigInteger,
    val label: String,
    val fullName: String,
    val blockNumber: BigInteger,
)

sealed interface CredentialDiscoveryResult {
    data class Available(
        val credentials: List<CredentialSnapshot>,
        val candidateCount: Int,
        val scannedToBlock: BigInteger,
    ) : CredentialDiscoveryResult

    data class Unavailable(val category: String) : CredentialDiscoveryResult
}

fun interface CredentialCandidateSource {
    suspend fun candidates(): Pair<List<CredentialReference>, BigInteger>
}

object R1RegistrationEvent {
    private const val EVENT_SIGNATURE = "LabelRegistered(uint256,bytes32,string,address,uint64,address)"
    val topic0: String = "0x" + Keccak.Digest256().digest(EVENT_SIGNATURE.toByteArray(StandardCharsets.UTF_8))
        .joinToString("") { "%02x".format(it.toInt() and 0xff) }

    fun decode(log: EthereumLog): R1RegistrationCandidate {
        require(log.address.equals(IssuerSpace.registry, true)) { "WRONG_LOG_ADDRESS" }
        require(log.topics.size == 4 && log.topics[0].equals(topic0, true)) { "WRONG_EVENT_TOPIC" }
        val tokenId = word(log.topics[1])
        val labelHash = log.topics[2].lowercase()
        val clean = log.data.removePrefix("0x")
        require(clean.length >= 64 * 5 && clean.length <= 8_192 && clean.length % 64 == 0) {
            "MALFORMED_REGISTRATION_EVENT"
        }
        val offset = word("0x${clean.substring(0, 64)}").intValueExact()
        require(offset == 96 && offset % 32 == 0) { "MALFORMED_REGISTRATION_EVENT" }
        CredentialValidation.requireAddress("0x${clean.substring(64 + 24, 128)}")
        require(word("0x${clean.substring(128, 192)}").bitLength() <= 64) { "MALFORMED_REGISTRATION_EVENT" }
        val stringStart = offset * 2
        require(stringStart + 64 <= clean.length) { "MALFORMED_REGISTRATION_EVENT" }
        val length = word("0x${clean.substring(stringStart, stringStart + 64)}").intValueExact()
        require(length in 1..63 && stringStart + 64 + length * 2 <= clean.length) {
            "MALFORMED_REGISTRATION_EVENT"
        }
        val bytes = hexBytes(clean.substring(stringStart + 64, stringStart + 64 + length * 2))
        val label = StandardCharsets.UTF_8.newDecoder()
            .onMalformedInput(CodingErrorAction.REPORT)
            .onUnmappableCharacter(CodingErrorAction.REPORT)
            .decode(ByteBuffer.wrap(bytes)).toString()
        val normalized = CredentialValidation.normalizeLabel(label)
        require(label == normalized) { "NONCANONICAL_REGISTRATION_LABEL" }
        val expectedHash = "0x" + Keccak.Digest256().digest(bytes)
            .joinToString("") { "%02x".format(it.toInt() and 0xff) }
        require(labelHash == expectedHash) { "REGISTRATION_LABEL_HASH_MISMATCH" }
        return R1RegistrationCandidate(
            tokenId = tokenId,
            label = normalized,
            fullName = "$normalized.${IssuerSpace.namespace}",
            blockNumber = log.blockNumber,
        )
    }

    private fun word(value: String): BigInteger {
        require(WORD.matches(value)) { "MALFORMED_EVENT_WORD" }
        return BigInteger(value.drop(2), 16)
    }

    private fun hexBytes(value: String): ByteArray {
        require(value.length % 2 == 0 && HEX.matches(value)) { "MALFORMED_EVENT_HEX" }
        return ByteArray(value.length / 2) { index -> value.substring(index * 2, index * 2 + 2).toInt(16).toByte() }
    }

    private val WORD = Regex("^0x[0-9a-fA-F]{64}$")
    private val HEX = Regex("^[0-9a-fA-F]*$")
}

class R1CredentialCandidateSource(
    private val client: ReadOnlyEthereumRpcClient,
    private val startBlock: Long = IssuerSpace.registryDeploymentBlock,
    private val chunkSize: Long = 20_000,
    private val maxScanBlocks: Long = 2_000_000,
    private val candidateLimit: Int = 100,
    private val logLimitPerChunk: Int = 500,
) : CredentialCandidateSource {
    init {
        require(startBlock >= 0 && chunkSize in 1..100_000 && maxScanBlocks in 1..5_000_000 && candidateLimit in 1..1_000)
    }

    override suspend fun candidates(): Pair<List<CredentialReference>, BigInteger> {
        require(client.chainId() == BigInteger.valueOf(IssuerSpace.chainId)) { "WRONG_CHAIN" }
        val latest = client.blockByNumber("latest", false) ?: error("LATEST_BLOCK_MISSING")
        val latestNumber = quantityField(latest, "number")
        val latestTimestamp = quantityField(latest, "timestamp")
        require(latestNumber - BigInteger.valueOf(startBlock) < BigInteger.valueOf(maxScanBlocks)) {
            "DISCOVERY_SCAN_LIMIT"
        }
        val now = BigInteger.valueOf(System.currentTimeMillis() / 1_000L)
        require(latestTimestamp <= now + BigInteger.valueOf(15L) && now - latestTimestamp <= BigInteger.valueOf(120L)) {
            "STALE_BLOCK"
        }
        val deduplicated = linkedMapOf<String, R1RegistrationCandidate>()
        var from = BigInteger.valueOf(startBlock)
        while (from <= latestNumber) {
            val to = (from + BigInteger.valueOf(chunkSize - 1)).min(latestNumber)
            client.logs(
                address = IssuerSpace.registry,
                fromBlock = from,
                toBlock = to,
                topic0 = R1RegistrationEvent.topic0,
                maxLogs = logLimitPerChunk,
            ).forEach { log ->
                val candidate = R1RegistrationEvent.decode(log)
                deduplicated[candidate.fullName] = candidate
                require(deduplicated.size <= candidateLimit) { "DISCOVERY_CANDIDATE_LIMIT" }
            }
            from = to + BigInteger.ONE
        }
        return deduplicated.values.map { CredentialReference(IssuerSpace.chainId, it.fullName) } to latestNumber
    }

    private fun quantityField(value: ReadOnlyJsonValue.ObjectValue, name: String): BigInteger {
        val raw = (value.fields[name] as? ReadOnlyJsonValue.StringValue)?.value ?: error("MISSING_$name")
        return MobileIssuerAdmissionRunner.parseQuantity(raw)
    }
}

class CredentialDiscoveryService(
    private val source: CredentialCandidateSource,
    private val read: suspend (String) -> CredentialSnapshot,
    private val candidateLimit: Int = 100,
) {
    suspend fun discover(wallet: String, manual: List<CredentialReference>): CredentialDiscoveryResult = try {
        CredentialValidation.requireAddress(wallet)
        val (automatic, scannedToBlock) = source.candidates()
        val automaticCandidates = automatic
            .filter { it.chainId == IssuerSpace.chainId }
            .distinctBy { CredentialValidation.normalizeFullName(it.fullName) }
        val candidates = (automaticCandidates + manual)
            .filter { it.chainId == IssuerSpace.chainId }
            .distinctBy { CredentialValidation.normalizeFullName(it.fullName) }
        require(candidates.size <= candidateLimit) { "DISCOVERY_CANDIDATE_LIMIT" }
        val snapshots = candidates.map { read(it.fullName) }
        val failed = snapshots.firstOrNull { it.readStatus != CredentialReadStatus.FRESH }
        if (failed != null) {
            CredentialDiscoveryResult.Unavailable(failed.failureCategory ?: "READBACK_UNAVAILABLE")
        } else {
            CredentialDiscoveryResult.Available(
                snapshots.filter { CredentialProductPolicy.ownedBy(it, wallet) },
                automaticCandidates.size,
                scannedToBlock,
            )
        }
    } catch (error: Throwable) {
        if (error is CancellationException) throw error
        CredentialDiscoveryResult.Unavailable(safeDiscoveryCategory(error))
    }

    private fun safeDiscoveryCategory(error: Throwable): String = (error as? ReadOnlyRpcException)?.category
        ?: error.message?.takeIf { Regex("^[A-Z][A-Z0-9_]{1,95}$").matches(it) }
        ?: "DISCOVERY_UNAVAILABLE"
}
