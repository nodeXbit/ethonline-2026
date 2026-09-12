package io.github.nodexbit.ethonline2026.gate

import android.app.Activity
import android.graphics.Color
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.IsoDep
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import io.github.nodexbit.ethonline2026.AccessResources
import java.util.concurrent.Executors

class GateReaderActivity : Activity(), GateReaderEvents {
    private val worker = Executors.newSingleThreadExecutor()
    private lateinit var controller: GateReaderController
    private lateinit var stateText: TextView
    private lateinit var credentialText: TextView
    private lateinit var holderText: TextView
    private lateinit var globalText: TextView
    private lateinit var resourceText: TextView
    private lateinit var finalText: TextView
    private var mode: AndroidReaderMode? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        controller = GateReaderController(
            PixelGateHttpApi(), this, worker,
            transportOnly = intent.getBooleanExtra("transport_only", false),
        )
        setContentView(buildScreen())
    }

    override fun onResume() {
        super.onResume()
        val adapter = NfcAdapter.getDefaultAdapter(this)
        if (adapter == null || !adapter.isEnabled) {
            failure("NFC_UNAVAILABLE")
            return
        }
        AndroidReaderMode(adapter).also { mode = it; controller.onResume(it) }
    }

    override fun onPause() {
        controller.onPause()
        mode = null
        super.onPause()
    }

    override fun onDestroy() {
        worker.shutdownNow()
        super.onDestroy()
    }

    override fun state(value: String) = ui {
        Log.i("LockENS-GateReader", value)
        stateText.text = value
        finalText.text = ""
    }
    override fun credential(value: String) = ui {
        Log.i("LockENS-GateReader", "GET_CREDENTIAL 9000 credential=$value")
        credentialText.text = value
    }
    override fun decision(value: PixelGateDecision) = ui {
        Log.i("LockENS-GateReader", "decision=${value.reason} allowed=${value.allowed}")
        holderText.text = value.holder
        globalText.text = value.globalAccess
        resourceText.text = value.resourcePolicy
        finalText.text = if (value.allowed) "ACCESS GRANTED" else "ACCESS DENIED\n${value.reason}"
        finalText.setTextColor(if (value.allowed) Color.rgb(15, 118, 65) else Color.rgb(180, 35, 24))
        stateText.text = "VERIFIER TRANSPORT CONFIRMED — VIRTUAL GATE DECISION"
    }
    override fun failure(reason: String) = ui {
        Log.i("LockENS-GateReader", "failure=$reason")
        stateText.text = "READER STOPPED"
        finalText.text = reason
        finalText.setTextColor(Color.rgb(180, 35, 24))
    }

    private fun buildScreen(): View {
        fun text(size: Float = 16f) = TextView(this).apply { textSize = size; setPadding(0, 10, 0, 10) }
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 48, 48, 48)
            setBackgroundColor(Color.rgb(247, 246, 250))
        }
        root.addView(text(26f).apply { text = "LOCKENS GATE READER" })
        root.addView(text(14f).apply { text = "Virtual Gate" })
        val choices = RadioGroup(this).apply { orientation = RadioGroup.HORIZONTAL }
        AccessResources.all.forEachIndexed { index, resource ->
            choices.addView(RadioButton(this).apply {
                id = View.generateViewId(); text = resource.displayName; isChecked = index == 1
                setOnClickListener { controller.selectResource(resource) }
            })
        }
        root.addView(choices)
        stateText = text(18f); root.addView(stateText)
        credentialText = text(); root.addView(row("Credential", credentialText))
        holderText = text(); root.addView(row("Holder", holderText))
        globalText = text(); root.addView(row("Global access", globalText))
        resourceText = text(); root.addView(row("Resource policy", resourceText))
        finalText = text(24f); root.addView(finalText)
        root.addView(Button(this).apply {
            text = "RESET READER"
            setOnClickListener { recreate() }
        })
        return root
    }

    private fun row(label: String, value: TextView) = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        addView(TextView(this@GateReaderActivity).apply { text = label; textSize = 12f })
        addView(value.apply { text = "…" })
    }

    private fun ui(block: () -> Unit) = runOnUiThread(block)

    private inner class AndroidReaderMode(private val adapter: NfcAdapter) : GateReaderMode {
        override fun enable(callback: (GateTagSession?) -> Unit) {
            adapter.enableReaderMode(
                this@GateReaderActivity,
                { tag: Tag -> callback(IsoDep.get(tag)?.let(::AndroidIsoDepSession)) },
                NfcAdapter.FLAG_READER_NFC_A or NfcAdapter.FLAG_READER_SKIP_NDEF_CHECK,
                null,
            )
        }
        override fun disable() = adapter.disableReaderMode(this@GateReaderActivity)
    }

    private class AndroidIsoDepSession(private val isoDep: IsoDep) : GateTagSession {
        override fun connect() { isoDep.timeout = 2_000; isoDep.connect() }
        override fun transceive(command: ByteArray): ByteArray = isoDep.transceive(command)
        override fun close() = isoDep.close()
    }
}
