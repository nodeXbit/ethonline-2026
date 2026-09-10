package io.github.nodexbit.ethonline2026.hce

import android.nfc.cardemulation.HostApduService
import android.os.Bundle
import io.github.nodexbit.ethonline2026.GateBApplication

/** Thin Android adapter; all protocol and session behavior lives in HceApduProcessor. */
class GateC1HostApduService : HostApduService() {
    private val processor: HceApduProcessor
        get() = (application as GateBApplication).hceProcessor

    override fun processCommandApdu(commandApdu: ByteArray?, extras: Bundle?): ByteArray =
        processor.process(commandApdu)

    override fun onDeactivated(reason: Int) {
        processor.reset()
    }
}
