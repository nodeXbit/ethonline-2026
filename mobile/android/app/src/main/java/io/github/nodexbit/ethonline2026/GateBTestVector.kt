package io.github.nodexbit.ethonline2026

import android.content.Context

object GateBTestVector {
    const val ASSET_NAME = "gate-b-typed-data.json"

    fun readJson(context: Context): String =
        context.assets.open(ASSET_NAME).bufferedReader().use { it.readText() }
}
