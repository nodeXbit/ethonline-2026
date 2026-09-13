package io.github.nodexbit.ethonline2026.gate

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class GateStandProfileTest {
    @Test
    fun `normal launch uses persistent profile and defaults to Lab`() {
        var persisted: String? = null
        val store = GateStandProfileStore({ persisted }, { persisted = it })

        assertEquals("lab", store.load().slug)
        store.configure("front-door")
        assertEquals("front-door", store.load().slug)
    }

    @Test
    fun `all three configured profiles persist exactly`() {
        var persisted: String? = null
        val store = GateStandProfileStore({ persisted }, { persisted = it })

        for (slug in listOf("front-door", "lab", "server-room")) {
            assertEquals(slug, store.configure(slug).slug)
            assertEquals(slug, persisted)
            assertEquals(slug, store.load().slug)
        }
    }

    @Test
    fun `unknown explicit profile is rejected and corrupt persistence fails to Lab`() {
        var persisted: String? = "corrupt"
        val store = GateStandProfileStore({ persisted }, { persisted = it })

        assertEquals("lab", store.load().slug)
        assertThrows(IllegalArgumentException::class.java) { store.configure("unknown") }
        assertEquals("corrupt", persisted)
    }
}
