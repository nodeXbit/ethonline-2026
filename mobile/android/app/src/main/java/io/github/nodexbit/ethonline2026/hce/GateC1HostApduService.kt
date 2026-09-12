package io.github.nodexbit.ethonline2026.hce

import android.nfc.cardemulation.HostApduService
import android.os.Bundle
import android.os.SystemClock
import android.util.Log
import io.github.nodexbit.ethonline2026.BuildConfig
import io.github.nodexbit.ethonline2026.GateBApplication

/** Thin Android adapter; all protocol and session behavior lives in HceApduProcessor. */
class GateC1HostApduService : HostApduService() {
    private var diagnosticSequence = 0L
    private val processor: HceApduProcessor
        get() = (application as GateBApplication).physicalHceProcessor

    override fun processCommandApdu(commandApdu: ByteArray?, extras: Bundle?): ByteArray {
        val activeProcessor = processor
        if (!BuildConfig.DEBUG) return activeProcessor.process(commandApdu)
        val sequence = ++diagnosticSequence
        val started = SystemClock.elapsedRealtime()
        val header = commandApdu?.take(4)?.joinToString("") { "%02X".format(it.toInt() and 255) } ?: "NONE"
        Log.i(DIAGNOSTIC_TAG, "RX id=$sequence bytes=${commandApdu?.size ?: 0} header=$header ${activeProcessor.diagnosticSummary(commandApdu)}")
        try {
            val response = activeProcessor.process(commandApdu)
            val status = if (response.size >= 2) response.takeLast(2).joinToString("") { "%02X".format(it.toInt() and 255) } else "NONE"
            Log.i(DIAGNOSTIC_TAG, "TX id=$sequence bytes=${response.size} sw=$status elapsedMs=${SystemClock.elapsedRealtime() - started} ${activeProcessor.diagnosticSummary(null)}")
            return response
        } catch (error: Exception) {
            Log.i(DIAGNOSTIC_TAG, "ERROR id=$sequence type=${error.javaClass.simpleName}")
            throw error
        }
    }

    override fun onDeactivated(reason: Int) {
        if (BuildConfig.DEBUG) Log.i(DIAGNOSTIC_TAG, "DEACTIVATED reason=$reason")
        processor.reset()
    }

    private companion object { const val DIAGNOSTIC_TAG = "LockENS-HCE" }
}
