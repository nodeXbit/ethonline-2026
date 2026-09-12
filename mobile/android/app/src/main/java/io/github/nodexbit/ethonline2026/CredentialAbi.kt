package io.github.nodexbit.ethonline2026

import java.math.BigInteger
import java.nio.charset.StandardCharsets
import org.web3j.abi.FunctionEncoder
import org.web3j.abi.TypeReference
import org.web3j.abi.datatypes.Address
import org.web3j.abi.datatypes.Bool
import org.web3j.abi.datatypes.DynamicArray
import org.web3j.abi.datatypes.DynamicBytes
import org.web3j.abi.datatypes.Function
import org.web3j.abi.datatypes.Utf8String
import org.web3j.abi.datatypes.generated.Bytes32
import org.web3j.abi.datatypes.generated.Uint64
import org.web3j.abi.datatypes.generated.Uint256
import org.web3j.utils.Numeric
import org.bouncycastle.jcajce.provider.digest.Keccak

object IssuerSpace {
    const val STAFF_LABEL = "staff-001"
    const val STAFF_HOLDER = "0x3419148731087b970d2059C53780163B452D5FF7"
    const val STAFF_EXPIRY = 1_793_487_599L
    const val DEFAULT_DESCRIPTION = "Staff Access Pass"
    const val ACCESS_KEY = "access.v1"
    const val ZERO_ADDRESS = "0x0000000000000000000000000000000000000000"

    val chainId: Long get() = BuildConfig.ISSUER_CHAIN_ID
    val namespace: String get() = BuildConfig.ISSUER_NAMESPACE
    val issuer: String get() = BuildConfig.ISSUER_ADDRESS
    val ethRegistry: String get() = BuildConfig.ETH_REGISTRY
    val parentRegistry: String get() = BuildConfig.PARENT_REGISTRY
    val registry: String get() = BuildConfig.ISSUER_REGISTRY
    val resolver: String get() = BuildConfig.ISSUER_RESOLVER
    val factory: String get() = BuildConfig.VERIFIABLE_FACTORY
    val registryImplementation: String get() = BuildConfig.USER_REGISTRY_IMPLEMENTATION
    val resolverImplementation: String get() = BuildConfig.PERMISSIONED_RESOLVER_IMPLEMENTATION
    val namespaceExpiry: Long get() = BuildConfig.ISSUER_NAMESPACE_EXPIRY
    val registryDeploymentBlock: Long get() = BuildConfig.ISSUER_REGISTRY_DEPLOYMENT_BLOCK
    val registryRootRoles: BigInteger get() = Numeric.toBigInt(BuildConfig.ISSUER_REGISTRY_ROOT_ROLES)
    val resolverRootRoles: BigInteger get() = Numeric.toBigInt(BuildConfig.ISSUER_RESOLVER_ROOT_ROLES)
    val fullName: String get() = "$STAFF_LABEL.$namespace"
}

object CredentialValidation {
    private val LABEL = Regex("^[a-z0-9](?:[a-z0-9-]{0,61}[a-z0-9])?$")
    private val ADDRESS = Regex("^0x[0-9a-fA-F]{40}$")

    fun normalizeLabel(value: String): String {
        val label = value.trim().lowercase()
        require(LABEL.matches(label)) { "INVALID_CREDENTIAL_NAME" }
        return label
    }

    fun normalizeFullName(value: String): String {
        val name = value.trim().trimEnd('.').lowercase()
        val suffix = ".${IssuerSpace.namespace.lowercase()}"
        require(name.endsWith(suffix)) { "CREDENTIAL_OUTSIDE_ISSUER_NAMESPACE" }
        require(name.count { it == '.' } == IssuerSpace.namespace.count { it == '.' } + 1) {
            "INVALID_CREDENTIAL_NAME"
        }
        normalizeLabel(name.removeSuffix(suffix))
        return name
    }

    fun requireAddress(value: String): String {
        require(ADDRESS.matches(value)) { "INVALID_RECIPIENT" }
        return value
    }

    fun requireNonZeroAddress(value: String): String {
        val address = requireAddress(value.trim())
        require(!address.equals(IssuerSpace.ZERO_ADDRESS, true)) { "ZERO_RECIPIENT" }
        val letters = address.drop(2)
        val checksummed = checksumAddress(address)
        require(letters == letters.lowercase() || letters == letters.uppercase() || address == checksummed) {
            "INVALID_RECIPIENT_CHECKSUM"
        }
        return checksummed
    }

    fun checksumAddress(value: String): String {
        val clean = requireAddress(value).drop(2).lowercase()
        val digest = Keccak.Digest256().digest(clean.toByteArray(StandardCharsets.US_ASCII))
            .joinToString("") { "%02x".format(it.toInt() and 0xff) }
        return "0x" + clean.mapIndexed { index, char ->
            if (char in 'a'..'f' && digest[index].digitToInt(16) >= 8) char.uppercaseChar() else char
        }.joinToString("")
    }

    fun validateExpiry(expiry: BigInteger, now: BigInteger) {
        validateExpiry(expiry, now, BigInteger.valueOf(IssuerSpace.namespaceExpiry))
    }

    fun validateExpiry(expiry: BigInteger, now: BigInteger, namespaceExpiry: BigInteger) {
        require(expiry > now) { "CREDENTIAL_EXPIRED" }
        require(expiry <= namespaceExpiry - BigInteger.valueOf(60)) { "EXPIRY_OUTSIDE_NAMESPACE" }
        require(expiry.bitLength() <= 64) { "EXPIRY_OUTSIDE_UINT64" }
    }

    fun normalizeAvatar(value: String): String {
        val uri = value.trim()
        if (uri.isEmpty()) return ""
        require(uri.length <= 2_048) { "AVATAR_URI_TOO_LONG" }
        require(uri.startsWith("https://", true) || uri.startsWith("ipfs://", true)) {
            "AVATAR_URI_SCHEME_NOT_ALLOWED"
        }
        require(!uri.any { it.isWhitespace() || it.code < 0x20 }) { "INVALID_AVATAR_URI" }
        if (uri.startsWith("https://", true)) {
            val host = java.net.URI(uri).host?.lowercase() ?: error("INVALID_AVATAR_URI")
            require(host != "localhost" && host != "0.0.0.0" && host != "::1" &&
                !host.startsWith("127.") && !host.startsWith("10.") && !host.startsWith("192.168.") &&
                !Regex("""^172\.(1[6-9]|2[0-9]|3[01])\.""").containsMatchIn(host)) {
                "PRIVATE_AVATAR_URI_NOT_ALLOWED"
            }
        }
        return uri
    }
}

object CredentialAbi {
    private fun outputUint() = listOf(object : TypeReference<Uint256>() {})
    private fun hex(bytes: ByteArray): String = Numeric.toHexString(bytes)
    private fun bytes(value: String): ByteArray = Numeric.hexStringToByteArray(value)

    fun register(
        label: String,
        holder: String,
        resolver: String,
        expiry: BigInteger,
        roleBitmap: BigInteger = BigInteger.ZERO,
    ): String = FunctionEncoder.encode(
        Function(
            "register",
            listOf(
                Utf8String(CredentialValidation.normalizeLabel(label)),
                Address(CredentialValidation.requireAddress(holder)),
                Address(IssuerSpace.ZERO_ADDRESS),
                Address(resolver),
                Uint256(roleBitmap),
                Uint64(expiry),
            ),
            outputUint(),
        ),
    )

    fun setText(node: ByteArray, key: String, value: String): String = FunctionEncoder.encode(
        Function("setText", listOf(Bytes32(node), Utf8String(key), Utf8String(value)), emptyList()),
    )

    fun setData(node: ByteArray, key: String, value: ByteArray): String = FunctionEncoder.encode(
        Function("setData", listOf(Bytes32(node), Utf8String(key), DynamicBytes(value)), emptyList()),
    )

    fun accessValue(active: Boolean, validUntil: BigInteger): ByteArray = bytes(
        FunctionEncoder.encodeConstructor(listOf(Bool(active), Uint64(validUntil))).let {
            if (it.startsWith("0x")) it else "0x$it"
        },
    )

    fun credentialRecords(
        fullName: String,
        avatarUri: String,
        description: String,
        expiry: BigInteger,
        accessActive: Boolean = true,
        accessValidUntil: BigInteger = expiry,
    ): List<String> {
        val node = namehash(fullName)
        return buildList {
            if (avatarUri.isNotBlank()) add(setText(node, "avatar", avatarUri))
            add(setText(node, "description", description))
            add(setData(node, IssuerSpace.ACCESS_KEY, accessValue(accessActive, accessValidUntil)))
        }
    }

    fun renew(tokenId: BigInteger, newExpiry: BigInteger): String = FunctionEncoder.encode(
        Function("renew", listOf(Uint256(tokenId), Uint64(newExpiry)), emptyList()),
    )

    fun multicall(calls: List<String>): String = FunctionEncoder.encode(
        Function(
            "multicall",
            listOf(DynamicArray(DynamicBytes::class.java, calls.map { DynamicBytes(bytes(it)) })),
            listOf(object : TypeReference<DynamicArray<DynamicBytes>>() {}),
        ),
    )

    fun findTokenId(label: String) = function("findTokenId", listOf(Utf8String(label)), outputUint())
    fun getOwner(tokenId: BigInteger) = function("getOwner", listOf(Uint256(tokenId)), listOf(object : TypeReference<Address>() {}))
    fun getExpiry(tokenId: BigInteger) = function("getExpiry", listOf(Uint256(tokenId)), listOf(object : TypeReference<Uint64>() {}))
    fun getState(tokenId: BigInteger) = function("getState", listOf(Uint256(tokenId)), emptyList())
    fun getSubregistry(label: String) = function("getSubregistry", listOf(Utf8String(label)), listOf(object : TypeReference<Address>() {}))
    fun getResolver(label: String) = function("getResolver", listOf(Utf8String(label)), listOf(object : TypeReference<Address>() {}))
    fun roles(resource: BigInteger, account: String) = function(
        "roles", listOf(Uint256(resource), Address(account)), outputUint(),
    )
    fun roleCount(resource: BigInteger) = function("roleCount", listOf(Uint256(resource)), outputUint())
    fun text(node: ByteArray, key: String) = function(
        "text", listOf(Bytes32(node), Utf8String(key)), listOf(object : TypeReference<Utf8String>() {}),
    )
    fun data(node: ByteArray, key: String) = function(
        "data", listOf(Bytes32(node), Utf8String(key)), listOf(object : TypeReference<DynamicBytes>() {}),
    )
    fun verifyContract(proxy: String) = function(
        "verifyContract", listOf(Address(proxy)), listOf(object : TypeReference<Address>() {}),
    )

    private fun function(name: String, inputs: List<org.web3j.abi.datatypes.Type<*>>, outputs: List<TypeReference<*>>) =
        FunctionEncoder.encode(Function(name, inputs, outputs))

    fun namehash(name: String): ByteArray {
        var node = ByteArray(32)
        name.trim().trimEnd('.').lowercase().split('.').asReversed().forEach { label ->
            val labelHash = keccak(label.toByteArray(StandardCharsets.UTF_8))
            node = keccak(node + labelHash)
        }
        return node
    }

    fun decodeWord(data: String, index: Int = 0): BigInteger {
        val clean = data.removePrefix("0x")
        require(clean.length >= (index + 1) * 64) { "MALFORMED_ABI_WORD" }
        return BigInteger(clean.substring(index * 64, (index + 1) * 64), 16)
    }

    fun decodeAddress(data: String, index: Int = 0): String =
        "0x${decodeWord(data, index).toString(16).padStart(40, '0')}"

    fun decodeDynamicBytes(data: String): ByteArray {
        val clean = data.removePrefix("0x")
        val offset = decodeWord(data).intValueExact() * 2
        require(offset + 64 <= clean.length) { "MALFORMED_DYNAMIC_ABI" }
        val length = BigInteger(clean.substring(offset, offset + 64), 16).intValueExact()
        val start = offset + 64
        require(length >= 0 && start + length * 2 <= clean.length) { "MALFORMED_DYNAMIC_ABI" }
        return Numeric.hexStringToByteArray("0x${clean.substring(start, start + length * 2)}")
    }

    fun decodeString(data: String): String = decodeDynamicBytes(data).toString(StandardCharsets.UTF_8)

    fun decodeAccess(value: ByteArray): Pair<Boolean, BigInteger> {
        require(value.size == 64) { "MALFORMED_ACCESS" }
        val active = BigInteger(1, value.copyOfRange(0, 32))
        require(active == BigInteger.ZERO || active == BigInteger.ONE) { "MALFORMED_ACCESS" }
        val validUntil = BigInteger(1, value.copyOfRange(32, 64))
        require(validUntil.bitLength() <= 64) { "MALFORMED_ACCESS" }
        return (active == BigInteger.ONE) to validUntil
    }

    fun calldataFingerprint(data: String): String = hex(keccak(bytes(data))).lowercase()
    fun nodeHex(name: String): String = hex(namehash(name))

    private fun keccak(value: ByteArray): ByteArray = Keccak.Digest256().digest(value)
}
