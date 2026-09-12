package io.github.nodexbit.ethonline2026

import java.io.IOException
import java.net.HttpURLConnection
import java.net.SocketTimeoutException
import java.net.URI
import java.net.URL
import java.nio.charset.StandardCharsets
import java.util.concurrent.atomic.AtomicLong
import javax.net.ssl.HttpsURLConnection
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout

sealed interface ReadOnlyJsonValue {
    data class StringValue(val value: String) : ReadOnlyJsonValue
    data class NumberValue(val value: String) : ReadOnlyJsonValue
    data class BooleanValue(val value: Boolean) : ReadOnlyJsonValue
    data class ObjectValue(val fields: Map<String, ReadOnlyJsonValue>) : ReadOnlyJsonValue
    data class ArrayValue(val values: List<ReadOnlyJsonValue>) : ReadOnlyJsonValue
    data object NullValue : ReadOnlyJsonValue

    fun toJson(): String = when (this) {
        is StringValue -> "\"${escape(value)}\""
        is NumberValue -> value
        is BooleanValue -> value.toString()
        is ObjectValue -> fields.entries.joinToString(prefix = "{", postfix = "}") {
            "\"${escape(it.key)}\":${it.value.toJson()}"
        }
        is ArrayValue -> values.joinToString(prefix = "[", postfix = "]") { it.toJson() }
        NullValue -> "null"
    }

    companion object {
        private fun escape(value: String): String = buildString {
            value.forEach { character ->
                when (character) {
                    '"' -> append("\\\"")
                    '\\' -> append("\\\\")
                    '\b' -> append("\\b")
                    '\u000c' -> append("\\f")
                    '\n' -> append("\\n")
                    '\r' -> append("\\r")
                    '\t' -> append("\\t")
                    else -> if (character.code < 0x20) {
                        append("\\u${character.code.toString(16).padStart(4, '0')}")
                    } else {
                        append(character)
                    }
                }
            }
        }
    }
}

class ReadOnlyRpcException(
    val category: String,
    message: String,
) : IllegalStateException(message)

data class EthereumLog(
    val address: String,
    val blockNumber: java.math.BigInteger,
    val transactionHash: String,
    val logIndex: java.math.BigInteger,
    val topics: List<String>,
    val data: String,
)

fun interface ReadOnlyRpcTransport {
    suspend fun post(requestBody: String, maxResponseBytes: Int): String
}

class HttpsReadOnlyRpcTransport(
    endpoint: String,
    private val connectTimeoutMillis: Int = 5_000,
    private val readTimeoutMillis: Int = 10_000,
) : ReadOnlyRpcTransport {
    private val endpointUrl: URL

    init {
        val uri = try {
            URI(endpoint)
        } catch (_: Exception) {
            throw IllegalArgumentException("INVALID_PUBLIC_RPC_ENDPOINT")
        }
        require(uri.scheme.equals("https", ignoreCase = true)) { "PUBLIC_RPC_REQUIRES_HTTPS" }
        require(uri.rawUserInfo == null && uri.rawQuery == null && uri.rawFragment == null) {
            "PUBLIC_RPC_ENDPOINT_MUST_NOT_CONTAIN_CREDENTIALS_OR_METADATA"
        }
        endpointUrl = uri.toURL()
    }

    override suspend fun post(requestBody: String, maxResponseBytes: Int): String =
        withContext(Dispatchers.IO) {
            val connection = endpointUrl.openConnection() as HttpsURLConnection
            try {
                connection.requestMethod = "POST"
                connection.instanceFollowRedirects = false
                connection.connectTimeout = connectTimeoutMillis
                connection.readTimeout = readTimeoutMillis
                connection.doOutput = true
                connection.setRequestProperty("Content-Type", "application/json")
                connection.setRequestProperty("Accept", "application/json")
                connection.outputStream.use { output ->
                    output.write(requestBody.toByteArray(StandardCharsets.UTF_8))
                }
                val status = connection.responseCode
                if (status !in 200..299) {
                    throw ReadOnlyRpcException("HTTP_FAILURE", "Public RPC returned HTTP $status")
                }
                val contentLength = connection.contentLengthLong
                if (contentLength > maxResponseBytes) {
                    throw ReadOnlyRpcException("OVERSIZED_RESPONSE", "Public RPC response exceeded limit")
                }
                val bytes = connection.inputStream.use { input ->
                    val output = java.io.ByteArrayOutputStream()
                    val buffer = ByteArray(8_192)
                    while (true) {
                        val count = input.read(buffer)
                        if (count < 0) break
                        if (output.size() + count > maxResponseBytes) {
                            throw ReadOnlyRpcException(
                                "OVERSIZED_RESPONSE",
                                "Public RPC response exceeded limit",
                            )
                        }
                        output.write(buffer, 0, count)
                    }
                    output.toByteArray()
                }
                String(bytes, StandardCharsets.UTF_8)
            } catch (_: SocketTimeoutException) {
                throw ReadOnlyRpcException("TIMEOUT", "Public RPC timed out")
            } catch (error: ReadOnlyRpcException) {
                throw error
            } catch (_: IOException) {
                throw ReadOnlyRpcException("NETWORK_FAILURE", "Public RPC network failure")
            } finally {
                connection.disconnect()
            }
        }
}

class ReadOnlyEthereumRpcClient(
    private val transport: ReadOnlyRpcTransport = HttpsReadOnlyRpcTransport(PUBLIC_SEPOLIA_RPC),
    private val totalTimeoutMillis: Long = 15_000,
    private val maxResponseBytes: Int = 1_048_576,
) {
    private val nextRequestId = AtomicLong(0)

    suspend fun call(
        method: String,
        params: List<ReadOnlyJsonValue> = emptyList(),
    ): ReadOnlyJsonValue {
        if (method !in ALLOWED_METHODS) {
            throw ReadOnlyRpcException("METHOD_BLOCKED", "RPC method is not read-only allowlisted")
        }
        val id = nextRequestId.incrementAndGet()
        val request = ReadOnlyJsonValue.ObjectValue(
            linkedMapOf(
                "jsonrpc" to ReadOnlyJsonValue.StringValue("2.0"),
                "id" to ReadOnlyJsonValue.NumberValue(id.toString()),
                "method" to ReadOnlyJsonValue.StringValue(method),
                "params" to ReadOnlyJsonValue.ArrayValue(params),
            ),
        ).toJson()
        val response = try {
            withTimeout(totalTimeoutMillis) {
                transport.post(request, maxResponseBytes)
            }
        } catch (error: CancellationException) {
            if (error is TimeoutCancellationException) {
                throw ReadOnlyRpcException("TIMEOUT", "Public RPC timed out")
            }
            throw error
        } catch (error: ReadOnlyRpcException) {
            throw error
        } catch (_: Throwable) {
            throw ReadOnlyRpcException("NETWORK_FAILURE", "Public RPC request failed")
        }
        if (response.toByteArray(StandardCharsets.UTF_8).size > maxResponseBytes) {
            throw ReadOnlyRpcException("OVERSIZED_RESPONSE", "Public RPC response exceeded limit")
        }
        val envelope = try {
            ReadOnlyJsonParser.parse(response) as? ReadOnlyJsonValue.ObjectValue
                ?: throw IllegalArgumentException()
        } catch (_: Throwable) {
            throw ReadOnlyRpcException("MALFORMED_JSON", "Public RPC returned malformed JSON")
        }
        val version = envelope.fields["jsonrpc"] as? ReadOnlyJsonValue.StringValue
        val responseId = envelope.fields["id"] as? ReadOnlyJsonValue.NumberValue
        if (version?.value != "2.0" || responseId?.value != id.toString()) {
            throw ReadOnlyRpcException("INVALID_RESPONSE_ENVELOPE", "Public RPC response did not match request")
        }
        val rpcError = envelope.fields["error"]
        if (rpcError != null && rpcError != ReadOnlyJsonValue.NullValue) {
            val errorObject = rpcError as? ReadOnlyJsonValue.ObjectValue
            val code = (errorObject?.fields?.get("code") as? ReadOnlyJsonValue.NumberValue)?.value
            val message = (errorObject?.fields?.get("message") as? ReadOnlyJsonValue.StringValue)?.value
            throw ReadOnlyRpcException(
                "JSON_RPC_ERROR",
                listOfNotNull(code?.let { "RPC $it" }, sanitizeMessage(message)).joinToString(": ")
                    .ifBlank { "Public RPC returned an error" },
            )
        }
        if (!envelope.fields.containsKey("result")) {
            throw ReadOnlyRpcException("MISSING_RESULT", "Public RPC response omitted result")
        }
        return envelope.fields.getValue("result")
    }

    suspend fun chainId(): java.math.BigInteger = quantity(call("eth_chainId"))

    suspend fun balance(address: String): java.math.BigInteger = quantity(
        call("eth_getBalance", listOf(string(address), string("latest"))),
    )

    suspend fun transactionCount(address: String, tag: String): java.math.BigInteger = quantity(
        call("eth_getTransactionCount", listOf(string(address), string(blockTag(tag)))),
    )

    suspend fun transactionByHash(hash: String): ReadOnlyJsonValue.ObjectValue? = nullableObject(
        call("eth_getTransactionByHash", listOf(string(transactionHash(hash)))),
    )

    suspend fun receipt(hash: String): ReadOnlyJsonValue.ObjectValue? = nullableObject(
        call("eth_getTransactionReceipt", listOf(string(transactionHash(hash)))),
    )

    suspend fun blockByNumber(tag: String, fullTransactions: Boolean): ReadOnlyJsonValue.ObjectValue? =
        nullableObject(
            call(
                "eth_getBlockByNumber",
                listOf(string(blockTag(tag)), ReadOnlyJsonValue.BooleanValue(fullTransactions)),
            ),
        )

    suspend fun ethCall(call: ReadOnlyJsonValue.ObjectValue, tag: String): String = stringResult(
        call("eth_call", listOf(call, string(blockTag(tag)))),
    )

    suspend fun estimateGas(call: ReadOnlyJsonValue.ObjectValue): java.math.BigInteger = quantity(
        call("eth_estimateGas", listOf(call)),
    )

    suspend fun logs(
        address: String,
        fromBlock: java.math.BigInteger,
        toBlock: java.math.BigInteger,
        topic0: String,
        maxLogs: Int = 500,
    ): List<EthereumLog> {
        if (!ADDRESS.matches(address)) throw ReadOnlyRpcException("INVALID_LOG_ADDRESS", "Invalid log address")
        if (fromBlock.signum() < 0 || toBlock < fromBlock || toBlock - fromBlock > MAX_LOG_BLOCK_RANGE) {
            throw ReadOnlyRpcException("INVALID_LOG_RANGE", "Log range is outside the bounded policy")
        }
        if (!HASH.matches(topic0)) throw ReadOnlyRpcException("INVALID_LOG_TOPIC", "Invalid log topic")
        if (maxLogs !in 1..MAX_LOG_RESULTS) {
            throw ReadOnlyRpcException("INVALID_LOG_LIMIT", "Invalid log result limit")
        }
        val filter = ReadOnlyJsonValue.ObjectValue(linkedMapOf(
            "address" to string(address.lowercase()),
            "fromBlock" to string(blockQuantity(fromBlock)),
            "toBlock" to string(blockQuantity(toBlock)),
            "topics" to ReadOnlyJsonValue.ArrayValue(listOf(string(topic0.lowercase()))),
        ))
        val result = call("eth_getLogs", listOf(filter)) as? ReadOnlyJsonValue.ArrayValue
            ?: throw ReadOnlyRpcException("UNEXPECTED_RESULT_TYPE", "Public RPC logs result was not an array")
        if (result.values.size > maxLogs) {
            throw ReadOnlyRpcException("LOG_RESULT_LIMIT", "Public RPC logs exceeded the bounded policy")
        }
        return result.values.map(::ethereumLog)
    }

    suspend fun rawStringParams(method: String, params: List<String>): String =
        call(method, params.map(::string)).toJson()

    private fun nullableObject(value: ReadOnlyJsonValue): ReadOnlyJsonValue.ObjectValue? = when (value) {
        ReadOnlyJsonValue.NullValue -> null
        is ReadOnlyJsonValue.ObjectValue -> value
        else -> throw ReadOnlyRpcException("UNEXPECTED_RESULT_TYPE", "Public RPC result was not an object")
    }

    private fun quantity(value: ReadOnlyJsonValue): java.math.BigInteger {
        val encoded = stringResult(value)
        if (!QUANTITY.matches(encoded)) {
            throw ReadOnlyRpcException("MALFORMED_QUANTITY", "Public RPC returned malformed quantity")
        }
        return java.math.BigInteger(encoded.drop(2), 16)
    }

    private fun stringResult(value: ReadOnlyJsonValue): String =
        (value as? ReadOnlyJsonValue.StringValue)?.value
            ?: throw ReadOnlyRpcException("UNEXPECTED_RESULT_TYPE", "Public RPC result was not a string")

    private fun string(value: String) = ReadOnlyJsonValue.StringValue(value)

    private fun transactionHash(value: String): String {
        if (!HASH.matches(value)) throw ReadOnlyRpcException("INVALID_HASH", "Invalid transaction hash")
        return value.lowercase()
    }

    private fun blockTag(value: String): String {
        if (value !in BLOCK_TAGS && !QUANTITY.matches(value)) {
            throw ReadOnlyRpcException("INVALID_BLOCK_TAG", "Invalid block tag")
        }
        return value
    }

    private fun blockQuantity(value: java.math.BigInteger): String = "0x${value.toString(16)}"

    private fun ethereumLog(value: ReadOnlyJsonValue): EthereumLog {
        val fields = (value as? ReadOnlyJsonValue.ObjectValue)?.fields
            ?: throw ReadOnlyRpcException("MALFORMED_LOG", "Public RPC returned a malformed log")
        fun field(name: String): String = (fields[name] as? ReadOnlyJsonValue.StringValue)?.value
            ?: throw ReadOnlyRpcException("MALFORMED_LOG", "Public RPC returned a malformed log")
        val address = field("address")
        val transactionHash = field("transactionHash")
        val data = field("data")
        val topics = (fields["topics"] as? ReadOnlyJsonValue.ArrayValue)?.values?.map {
            (it as? ReadOnlyJsonValue.StringValue)?.value
                ?: throw ReadOnlyRpcException("MALFORMED_LOG", "Public RPC returned a malformed log")
        } ?: throw ReadOnlyRpcException("MALFORMED_LOG", "Public RPC returned a malformed log")
        if (!ADDRESS.matches(address) || !HASH.matches(transactionHash) || !DATA.matches(data) ||
            topics.isEmpty() || topics.any { !HASH.matches(it) }
        ) throw ReadOnlyRpcException("MALFORMED_LOG", "Public RPC returned a malformed log")
        return EthereumLog(
            address = address,
            blockNumber = rpcQuantity(field("blockNumber")),
            transactionHash = transactionHash,
            logIndex = rpcQuantity(field("logIndex")),
            topics = topics,
            data = data,
        )
    }

    private fun rpcQuantity(value: String): java.math.BigInteger {
        if (!QUANTITY.matches(value)) throw ReadOnlyRpcException("MALFORMED_LOG", "Public RPC returned a malformed log")
        return java.math.BigInteger(value.drop(2), 16)
    }

    companion object {
        const val PUBLIC_SEPOLIA_RPC = "https://ethereum-sepolia-rpc.publicnode.com"
        val ALLOWED_METHODS = setOf(
            "eth_chainId",
            "eth_getBalance",
            "eth_getTransactionCount",
            "eth_getTransactionByHash",
            "eth_getTransactionReceipt",
            "eth_getBlockByNumber",
            "eth_call",
            "eth_estimateGas",
            "eth_getLogs",
        )
        private val HASH = Regex("^0x[0-9a-fA-F]{64}$")
        private val ADDRESS = Regex("^0x[0-9a-fA-F]{40}$")
        private val DATA = Regex("^0x(?:[0-9a-fA-F]{2})*$")
        private val QUANTITY = Regex("^0x(?:0|[1-9a-fA-F][0-9a-fA-F]*)$")
        private val BLOCK_TAGS = setOf("latest", "pending", "safe", "finalized", "earliest")
        private val MAX_LOG_BLOCK_RANGE = java.math.BigInteger.valueOf(100_000L)
        private const val MAX_LOG_RESULTS = 1_000

        private fun sanitizeMessage(message: String?): String? {
            val normalized = message
                ?.replace(Regex("[\\r\\n\\t]+"), " ")
                ?.replace(Regex("\\s+"), " ")
                ?.trim()
                ?.take(120)
                ?.takeIf(String::isNotEmpty)
                ?: return null
            if (Regex("(?i)(https?://|authorization|bearer|api[_ -]?key|token)").containsMatchIn(normalized)) {
                return null
            }
            return normalized.replace(Regex("0x[0-9a-fA-F]{16,}"), "<hex>")
        }
    }
}

private object ReadOnlyJsonParser {
    fun parse(json: String): ReadOnlyJsonValue = Parser(json).parse()

    private class Parser(private val source: String) {
        private var index = 0

        fun parse(): ReadOnlyJsonValue {
            skipWhitespace()
            val value = value(depth = 0)
            skipWhitespace()
            require(index == source.length)
            return value
        }

        private fun value(depth: Int): ReadOnlyJsonValue {
            require(depth <= 64)
            skipWhitespace()
            require(index < source.length)
            return when (source[index]) {
                '"' -> ReadOnlyJsonValue.StringValue(string())
                '{' -> objectValue(depth + 1)
                '[' -> arrayValue(depth + 1)
                't' -> literal("true", ReadOnlyJsonValue.BooleanValue(true))
                'f' -> literal("false", ReadOnlyJsonValue.BooleanValue(false))
                'n' -> literal("null", ReadOnlyJsonValue.NullValue)
                '-', in '0'..'9' -> ReadOnlyJsonValue.NumberValue(number())
                else -> throw IllegalArgumentException()
            }
        }

        private fun objectValue(depth: Int): ReadOnlyJsonValue.ObjectValue {
            require(source[index++] == '{')
            skipWhitespace()
            val fields = linkedMapOf<String, ReadOnlyJsonValue>()
            if (consume('}')) return ReadOnlyJsonValue.ObjectValue(fields)
            while (true) {
                skipWhitespace()
                require(source.getOrNull(index) == '"')
                val key = string()
                require(!fields.containsKey(key))
                skipWhitespace()
                require(consume(':'))
                fields[key] = value(depth)
                skipWhitespace()
                if (consume('}')) return ReadOnlyJsonValue.ObjectValue(fields)
                require(consume(','))
            }
        }

        private fun arrayValue(depth: Int): ReadOnlyJsonValue.ArrayValue {
            require(source[index++] == '[')
            skipWhitespace()
            val values = mutableListOf<ReadOnlyJsonValue>()
            if (consume(']')) return ReadOnlyJsonValue.ArrayValue(values)
            while (true) {
                values += value(depth)
                skipWhitespace()
                if (consume(']')) return ReadOnlyJsonValue.ArrayValue(values)
                require(consume(','))
            }
        }

        private fun string(): String {
            require(source[index++] == '"')
            return buildString {
                while (true) {
                    require(index < source.length)
                    val character = source[index++]
                    when (character) {
                        '"' -> return@buildString
                        '\\' -> {
                            require(index < source.length)
                            when (val escaped = source[index++]) {
                                '"', '\\', '/' -> append(escaped)
                                'b' -> append('\b')
                                'f' -> append('\u000c')
                                'n' -> append('\n')
                                'r' -> append('\r')
                                't' -> append('\t')
                                'u' -> {
                                    require(index + 4 <= source.length)
                                    append(source.substring(index, index + 4).toInt(16).toChar())
                                    index += 4
                                }
                                else -> throw IllegalArgumentException()
                            }
                        }
                        else -> {
                            require(character.code >= 0x20)
                            append(character)
                        }
                    }
                }
            }
        }

        private fun number(): String {
            val start = index
            consume('-')
            if (consume('0')) {
                require(source.getOrNull(index)?.isDigit() != true)
            } else {
                require(source.getOrNull(index) in '1'..'9')
                while (source.getOrNull(index)?.isDigit() == true) index++
            }
            if (consume('.')) {
                require(source.getOrNull(index)?.isDigit() == true)
                while (source.getOrNull(index)?.isDigit() == true) index++
            }
            if (source.getOrNull(index) == 'e' || source.getOrNull(index) == 'E') {
                index++
                if (source.getOrNull(index) == '+' || source.getOrNull(index) == '-') index++
                require(source.getOrNull(index)?.isDigit() == true)
                while (source.getOrNull(index)?.isDigit() == true) index++
            }
            return source.substring(start, index)
        }

        private fun <T : ReadOnlyJsonValue> literal(text: String, value: T): T {
            require(source.startsWith(text, index))
            index += text.length
            return value
        }

        private fun consume(character: Char): Boolean {
            if (source.getOrNull(index) != character) return false
            index++
            return true
        }

        private fun skipWhitespace() {
            while (source.getOrNull(index) in setOf(' ', '\n', '\r', '\t')) index++
        }
    }
}
