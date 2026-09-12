package io.github.nodexbit.ethonline2026

import java.math.BigInteger
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CredentialAbiTest {
    private val expiry = BigInteger("1793487599")
    private val node = CredentialAbi.namehash(IssuerSpace.fullName)

    @Test
    fun `register calldata matches viem golden and keeps uint64 exact`() {
        val actual = CredentialAbi.register(
            IssuerSpace.STAFF_LABEL, IssuerSpace.STAFF_HOLDER, IssuerSpace.resolver, expiry,
        )
        assertEquals(
            "0x85f3e64300000000000000000000000000000000000000000000000000000000000000c0" +
                "0000000000000000000000003419148731087b970d2059c53780163b452d5ff7" +
                "0000000000000000000000000000000000000000000000000000000000000000" +
                "00000000000000000000000020766fb21498a99350f922ee3163f5a95f354a7f" +
                "0000000000000000000000000000000000000000000000000000000000000000" +
                "000000000000000000000000000000000000000000000000000000006ae672ef" +
                "0000000000000000000000000000000000000000000000000000000000000009" +
                "73746166662d3030310000000000000000000000000000000000000000000000",
            actual,
        )
        val clean = actual.removePrefix("0x").drop(8)
        assertEquals(BigInteger.ZERO, BigInteger(clean.substring(4 * 64, 5 * 64), 16))
        assertEquals(expiry, BigInteger(clean.substring(5 * 64, 6 * 64), 16))
    }

    @Test
    fun `setText calldata matches viem golden`() {
        assertEquals(
            "0x10f13a8c722b665341600276994f7946e93c68f89942ea7408788a95775871e63fc3f94d" +
                "0000000000000000000000000000000000000000000000000000000000000060" +
                "00000000000000000000000000000000000000000000000000000000000000a0" +
                "000000000000000000000000000000000000000000000000000000000000000b" +
                "6465736372697074696f6e000000000000000000000000000000000000000000" +
                "0000000000000000000000000000000000000000000000000000000000000011" +
                "5374616666204163636573732050617373000000000000000000000000000000",
            CredentialAbi.setText(node, "description", IssuerSpace.DEFAULT_DESCRIPTION),
        )
    }

    @Test
    fun `setData access value matches viem golden`() {
        val value = CredentialAbi.accessValue(true, expiry)
        assertEquals(
            "0000000000000000000000000000000000000000000000000000000000000001" +
                "000000000000000000000000000000000000000000000000000000006ae672ef",
            value.joinToString("") { "%02x".format(it) },
        )
        assertEquals(
            "0x4eb9c45e722b665341600276994f7946e93c68f89942ea7408788a95775871e63fc3f94d" +
                "0000000000000000000000000000000000000000000000000000000000000060" +
                "00000000000000000000000000000000000000000000000000000000000000a0" +
                "0000000000000000000000000000000000000000000000000000000000000009" +
                "6163636573732e76310000000000000000000000000000000000000000000000" +
                "0000000000000000000000000000000000000000000000000000000000000040" +
                "0000000000000000000000000000000000000000000000000000000000000001" +
                "000000000000000000000000000000000000000000000000000000006ae672ef",
            CredentialAbi.setData(node, IssuerSpace.ACCESS_KEY, value),
        )
        assertEquals(true to expiry, CredentialAbi.decodeAccess(value))
    }

    @Test
    fun `multicall without blank avatar matches viem golden`() {
        val calls = CredentialAbi.credentialRecords(
            IssuerSpace.fullName, "", IssuerSpace.DEFAULT_DESCRIPTION, expiry,
        )
        assertEquals(2, calls.size)
        assertEquals(CredentialAbi.setText(node, "description", IssuerSpace.DEFAULT_DESCRIPTION), calls[0])
        assertEquals(
            CredentialAbi.setData(node, IssuerSpace.ACCESS_KEY, CredentialAbi.accessValue(true, expiry)),
            calls[1],
        )
        val actual = CredentialAbi.multicall(calls)
        assertEquals("ac9650d8", actual.removePrefix("0x").take(8))
        assertEquals(
            "0xac9650d80000000000000000000000000000000000000000000000000000000000000020" +
                "0000000000000000000000000000000000000000000000000000000000000002" +
                "0000000000000000000000000000000000000000000000000000000000000040" +
                "0000000000000000000000000000000000000000000000000000000000000160" +
                "00000000000000000000000000000000000000000000000000000000000000e4" +
                calls[0].removePrefix("0x") + "00000000000000000000000000000000000000000000000000000000" +
                "0000000000000000000000000000000000000000000000000000000000000104" +
                calls[1].removePrefix("0x") + "00000000000000000000000000000000000000000000000000000000",
            actual,
        )
    }

    @Test
    fun `name and avatar validation is strict`() {
        assertEquals(IssuerSpace.fullName, CredentialValidation.normalizeFullName("STAFF-001.KEYS.DEMO-ACCESS.ETH."))
        assertEquals("ipfs://bafy-test", CredentialValidation.normalizeAvatar("ipfs://bafy-test"))
        assertEquals("https://example.com/a.png", CredentialValidation.normalizeAvatar("https://example.com/a.png"))
        listOf("visitor-001.demo-access.eth", "staff_001.keys.demo-access.eth").forEach {
            assertTrue(runCatching { CredentialValidation.normalizeFullName(it) }.isFailure)
        }
        listOf("file:///x", "javascript:alert(1)", "https://127.0.0.1/a", "https://192.168.1.2/a").forEach {
            assertTrue(runCatching { CredentialValidation.normalizeAvatar(it) }.isFailure)
        }
        assertFalse(CredentialAbi.credentialRecords(IssuerSpace.fullName, "", "x", expiry).first().contains("avatar"))
    }
}
