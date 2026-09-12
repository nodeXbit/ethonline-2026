package io.github.nodexbit.ethonline2026.gate

import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

data class PixelGateSession(
    val sessionId: String,
    val challenge: ByteArray,
    val resourceId: String,
)

data class PixelGateDecision(
    val allowed: Boolean,
    val reason: String,
    val holder: String,
    val registration: String,
    val globalAccess: String,
    val resourcePolicy: String,
)

interface PixelGateApi {
    fun start(resource: String, credential: String): PixelGateSession
    fun complete(sessionId: String, signature: ByteArray): PixelGateDecision
}

class PixelGateHttpApi(private val baseUrl: String = "http://127.0.0.1:8792") : PixelGateApi {
    override fun start(resource: String, credential: String): PixelGateSession {
        val result = post(
            "/gate/sessions",
            JSONObject().put("resource", resource).put("credential", credential),
            expectedStatus = 201,
        )
        val selected = result.getJSONObject("resource")
        return PixelGateSession(
            sessionId = result.getString("sessionId").also {
                require(it.matches(Regex("^[0-9a-f]{32}$"))) { "INVALID_NODE_RESPONSE" }
            },
            challenge = GateReaderProtocol.decodeChallenge(result.getString("challenge")),
            resourceId = selected.getString("resourceId"),
        )
    }

    override fun complete(sessionId: String, signature: ByteArray): PixelGateDecision {
        val result = post(
            "/gate/sessions/$sessionId/complete",
            JSONObject().put("signature", GateReaderProtocol.hex(signature)),
            expectedStatus = 200,
        )
        val checks = result.getJSONObject("checks")
        return PixelGateDecision(
            allowed = result.getBoolean("allowed"),
            reason = result.getString("reason"),
            holder = checks.getString("holder"),
            registration = checks.getString("registration"),
            globalAccess = checks.getString("globalAccess"),
            resourcePolicy = checks.getString("resourcePolicy"),
        )
    }

    private fun post(path: String, body: JSONObject, expectedStatus: Int): JSONObject {
        val connection = URL(baseUrl + path).openConnection() as HttpURLConnection
        try {
            connection.requestMethod = "POST"
            connection.connectTimeout = 5_000
            connection.readTimeout = 10_000
            connection.doOutput = true
            connection.setRequestProperty("Content-Type", "application/json")
            connection.setRequestProperty("Cache-Control", "no-store")
            val bytes = body.toString().toByteArray(Charsets.UTF_8)
            connection.setFixedLengthStreamingMode(bytes.size)
            connection.outputStream.use { it.write(bytes) }
            val status = connection.responseCode
            val stream = if (status in 200..299) connection.inputStream else connection.errorStream
            val response = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
            require(status == expectedStatus && response.length <= 8_192) { "NODE_UNAVAILABLE" }
            return JSONObject(response)
        } catch (error: Exception) {
            throw IllegalStateException("NODE_UNAVAILABLE", error)
        } finally {
            connection.disconnect()
        }
    }
}
