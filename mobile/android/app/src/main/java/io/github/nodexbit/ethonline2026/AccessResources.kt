package io.github.nodexbit.ethonline2026

import java.math.BigInteger
import org.bouncycastle.jcajce.provider.digest.Keccak

data class AccessResource(val slug: String, val displayName: String, val resourceId: String)

object AccessResources {
    const val KEY = "resources.v1"
    val all: List<AccessResource> = BuildConfig.ACCESS_RESOURCES.split(';').map { row ->
        val fields = row.split('|')
        require(fields.size == 3)
        AccessResource(fields[0], fields[1], fields[2]).also {
            require(it.resourceId == deriveId(it.slug)) { "RESOURCE_CONFIG_INVALID" }
        }
    }.also { require(it.map(AccessResource::slug) == listOf("front-door", "lab", "server-room")) }

    fun deriveId(slug: String): String = "0x" + Keccak.Digest256()
        .digest("lockens:resource:v1:$slug".toByteArray(Charsets.UTF_8)).joinToString("") { "%02x".format(it.toInt() and 255) }
    fun defaults(template: PassTemplate): Set<String> = when (template) {
        PassTemplate.STAFF -> all.map { it.resourceId }.toSet()
        PassTemplate.VISITOR -> setOf(all[0].resourceId)
        PassTemplate.CONTRACTOR -> setOf(all[1].resourceId)
    }
    fun names(ids: Set<String>?): String = if (ids == null) "No resource policy configured"
        else all.filter { it.resourceId in ids }.joinToString(", ") { it.displayName }.ifEmpty { "No resources" }
    fun encode(ids: Set<String>): String {
        require(ids.size <= 3 && ids.all { id -> all.any { it.resourceId == id } }) { "RESOURCE_POLICY_INVALID" }
        return "0x" + word(32) + word(ids.size) + ids.sorted().joinToString("") { it.removePrefix("0x") }
    }
    fun decode(raw: String): Set<String>? {
        if (raw == "0x") return null
        require(raw.matches(Regex("^0x[0-9a-fA-F]{128,320}$"))) { "RESOURCE_POLICY_INVALID" }
        val data = raw.drop(2).lowercase()
        require(data.take(64) == word(32)) { "RESOURCE_POLICY_INVALID" }
        val count = BigInteger(data.substring(64, 128), 16).intValueExact()
        require(count in 0..3 && data.length == 128 + count * 64) { "RESOURCE_POLICY_INVALID" }
        val ids = data.drop(128).chunked(64).map { "0x$it" }.toSet()
        require(encode(ids) == raw.lowercase()) { "RESOURCE_POLICY_INVALID" }
        return ids
    }
    private fun word(value: Int) = value.toString(16).padStart(64, '0')
}
