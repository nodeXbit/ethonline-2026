package io.github.nodexbit.ethonline2026.gate

import io.github.nodexbit.ethonline2026.AccessResource
import io.github.nodexbit.ethonline2026.AccessResources

class GateStandProfileStore(
    private val read: () -> String?,
    private val write: (String) -> Unit,
) {
    fun load(): AccessResource = resource(read()) ?: lab()

    fun configure(slug: String): AccessResource {
        val selected = requireNotNull(resource(slug)) { "UNKNOWN_GATE_PROFILE" }
        write(selected.slug)
        return selected
    }

    private fun resource(slug: String?): AccessResource? =
        AccessResources.all.singleOrNull { it.slug == slug }

    private fun lab(): AccessResource = AccessResources.all.single { it.slug == "lab" }
}
