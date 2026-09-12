package io.github.nodexbit.ethonline2026

import java.math.BigInteger
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class ReadOnlyEthereumRpcClientTest {
    @Test
    fun `scalar result is preserved`() = runBlocking {
        val client = client("{\"jsonrpc\":\"2.0\",\"id\":1,\"result\":\"0xaa36a7\"}")

        assertEquals(
            ReadOnlyJsonValue.StringValue("0xaa36a7"),
            client.call("eth_chainId"),
        )
    }

    @Test
    fun `object result is preserved`() = runBlocking {
        val client = client(
            "{\"jsonrpc\":\"2.0\",\"id\":1,\"result\":{" +
                "\"hash\":\"$HASH\",\"value\":\"0x0\"}}",
        )

        val result = client.transactionByHash(HASH)!!

        assertEquals(ReadOnlyJsonValue.StringValue(HASH), result.fields["hash"])
        assertEquals(ReadOnlyJsonValue.StringValue("0x0"), result.fields["value"])
    }

    @Test
    fun `array result is preserved`() = runBlocking {
        val client = client("{\"jsonrpc\":\"2.0\",\"id\":1,\"result\":[\"0x1\",{\"ok\":true}]}")

        val result = client.call("eth_call") as ReadOnlyJsonValue.ArrayValue

        assertEquals(2, result.values.size)
        assertEquals(ReadOnlyJsonValue.BooleanValue(true), (result.values[1] as ReadOnlyJsonValue.ObjectValue).fields["ok"])
    }

    @Test
    fun `null result is preserved`() = runBlocking {
        val client = client("{\"jsonrpc\":\"2.0\",\"id\":1,\"result\":null}")

        assertNull(client.receipt(HASH))
    }

    @Test
    fun `JSON-RPC error is sanitized and classified`() = runBlocking {
        val client = client(
            "{\"jsonrpc\":\"2.0\",\"id\":1,\"error\":{" +
                "\"code\":-32601,\"message\":\"method unavailable\"}}",
        )

        val error = assertThrows(ReadOnlyRpcException::class.java) {
            runBlocking { client.call("eth_chainId") }
        }

        assertEquals("JSON_RPC_ERROR", error.category)
        assertEquals("RPC -32601: method unavailable", error.message)
    }

    @Test
    fun `HTTP failure category is preserved without endpoint details`() {
        val client = ReadOnlyEthereumRpcClient(
            transport = ReadOnlyRpcTransport { _, _ ->
                throw ReadOnlyRpcException("HTTP_FAILURE", "Public RPC returned HTTP 503")
            },
        )

        val error = assertThrows(ReadOnlyRpcException::class.java) {
            runBlocking { client.call("eth_chainId") }
        }

        assertEquals("HTTP_FAILURE", error.category)
        assertFalse(error.message.orEmpty().contains("http", ignoreCase = true) && error.message.orEmpty().contains("://"))
    }

    @Test
    fun `total timeout is enforced`() {
        val client = ReadOnlyEthereumRpcClient(
            transport = ReadOnlyRpcTransport { _, _ ->
                delay(100)
                "{\"jsonrpc\":\"2.0\",\"id\":1,\"result\":\"0x1\"}"
            },
            totalTimeoutMillis = 1,
        )

        val error = assertThrows(ReadOnlyRpcException::class.java) {
            runBlocking { client.call("eth_chainId") }
        }

        assertEquals("TIMEOUT", error.category)
    }

    @Test
    fun `oversized response is rejected`() {
        val client = ReadOnlyEthereumRpcClient(
            transport = ReadOnlyRpcTransport { _, _ -> "x".repeat(257) },
            maxResponseBytes = 256,
        )

        val error = assertThrows(ReadOnlyRpcException::class.java) {
            runBlocking { client.call("eth_chainId") }
        }

        assertEquals("OVERSIZED_RESPONSE", error.category)
    }

    @Test
    fun `wrong Sepolia chain is rejected by integration boundary`() = runBlocking {
        val readProvider = ReadOnlyMobileIssuerProvider(
            client("{\"jsonrpc\":\"2.0\",\"id\":1,\"result\":\"0x1\"}"),
        )

        val chain = MobileIssuerAdmissionRunner.parseQuantity(readProvider.request("eth_chainId"))

        assertFalse(chain == MobileIssuerAdmissionRunner.SEPOLIA_CHAIN_ID)
    }

    @Test
    fun `malformed JSON is rejected`() {
        val client = client("{not-json")

        val error = assertThrows(ReadOnlyRpcException::class.java) {
            runBlocking { client.call("eth_chainId") }
        }

        assertEquals("MALFORMED_JSON", error.category)
    }

    @Test
    fun `typed quantity helpers decode exact values`() = runBlocking {
        val client = client("{\"jsonrpc\":\"2.0\",\"id\":1,\"result\":\"0xaa36a7\"}")

        assertEquals(BigInteger("11155111"), client.chainId())
    }

    @Test
    fun `write and signing methods are blocked before transport`() {
        var transportCalls = 0
        val client = ReadOnlyEthereumRpcClient(
            transport = ReadOnlyRpcTransport { _, _ ->
                transportCalls += 1
                error("unreachable")
            },
        )
        val forbidden = listOf(
            "eth_sendTransaction",
            "eth_sendRawTransaction",
            "personal_sign",
            "eth_sign",
            "eth_signTypedData_v4",
            "wallet_switchEthereumChain",
            "privy_sign",
        )

        forbidden.forEach { method ->
            val error = assertThrows(ReadOnlyRpcException::class.java) {
                runBlocking { client.call(method) }
            }
            assertEquals("METHOD_BLOCKED", error.category)
        }
        assertEquals(0, transportCalls)
    }

    @Test
    fun `eth_getLogs is read only allowlisted with a strict bounded filter`() = runBlocking {
        var request = ""
        val client = ReadOnlyEthereumRpcClient(
            transport = ReadOnlyRpcTransport { body, _ ->
                request = body
                "{\"jsonrpc\":\"2.0\",\"id\":1,\"result\":[]}"
            },
        )
        val logs = client.logs(
            address = ADDRESS,
            fromBlock = BigInteger.TEN,
            toBlock = BigInteger.valueOf(20),
            topic0 = HASH,
        )
        assertTrue(logs.isEmpty())
        assertTrue(request.contains("\"method\":\"eth_getLogs\""))
        assertTrue(request.contains("\"fromBlock\":\"0xa\""))
        assertTrue(request.contains("\"toBlock\":\"0x14\""))
        assertTrue(request.contains(ADDRESS.lowercase()))
    }

    @Test
    fun `eth_getLogs rejects unbounded ranges and malformed filters before transport`() {
        var calls = 0
        val client = ReadOnlyEthereumRpcClient(transport = ReadOnlyRpcTransport { _, _ ->
            calls += 1
            error("unreachable")
        })
        assertThrows(ReadOnlyRpcException::class.java) {
            runBlocking { client.logs(ADDRESS, BigInteger.ZERO, BigInteger.valueOf(100_001), HASH) }
        }
        assertThrows(ReadOnlyRpcException::class.java) {
            runBlocking { client.logs("not-an-address", BigInteger.ZERO, BigInteger.ONE, HASH) }
        }
        assertEquals(0, calls)
    }

    @Test
    fun `endpoint must be HTTPS and contain no credentials or metadata`() {
        val insecure = assertThrows(IllegalArgumentException::class.java) {
            HttpsReadOnlyRpcTransport("http://example.invalid")
        }
        val credentialed = assertThrows(IllegalArgumentException::class.java) {
            HttpsReadOnlyRpcTransport("https://secret@example.invalid/rpc?token=secret")
        }

        assertEquals("PUBLIC_RPC_REQUIRES_HTTPS", insecure.message)
        assertEquals("PUBLIC_RPC_ENDPOINT_MUST_NOT_CONTAIN_CREDENTIALS_OR_METADATA", credentialed.message)
        assertFalse(credentialed.message.orEmpty().contains("secret"))
    }

    @Test
    fun `request contains a generated id and plain string params`() = runBlocking {
        var request = ""
        val client = ReadOnlyEthereumRpcClient(
            transport = ReadOnlyRpcTransport { body, _ ->
                request = body
                "{\"jsonrpc\":\"2.0\",\"id\":1,\"result\":null}"
            },
        )

        client.transactionByHash(HASH)

        assertEquals(
            "{\"jsonrpc\":\"2.0\", \"id\":1, \"method\":\"eth_getTransactionByHash\", " +
                "\"params\":[\"$HASH\"]}",
            request,
        )
    }

    private fun client(response: String) = ReadOnlyEthereumRpcClient(
        transport = ReadOnlyRpcTransport { _, _ -> response },
    )

    companion object {
        private const val HASH = "0x6c4f42f2d368936d4aaf7edf3e0395c376f92b699563b34fee4ed053a0a53e32"
        private const val ADDRESS = "0x0AeB395be893149c0b60D9DAC3Ba55139D0dcA1a"
    }
}
