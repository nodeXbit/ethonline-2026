package io.github.nodexbit.ethonline2026

import io.github.nodexbit.ethonline2026.hce.AccessChallengeTypedData
import io.github.nodexbit.ethonline2026.hce.GateC2TestVector

object GateBTestVector {
    fun json(): String = AccessChallengeTypedData.json(GateC2TestVector.challenge())
}
