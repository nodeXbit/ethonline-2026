package io.github.nodexbit.ethonline2026.gate

import android.app.Activity
import android.content.Intent
import android.content.pm.ActivityInfo
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.IsoDep
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import io.github.nodexbit.ethonline2026.AccessResource
import io.github.nodexbit.ethonline2026.R
import java.util.concurrent.Executors

class GateReaderActivity : Activity(), GateReaderEvents {
    private val worker = Executors.newSingleThreadExecutor()
    private lateinit var controller: GateReaderController
    private lateinit var profile: AccessResource
    private lateinit var door: GateDoorView
    private lateinit var statusText: TextView
    private lateinit var credentialText: TextView
    private lateinit var holderText: TextView
    private lateinit var globalText: TextView
    private lateinit var resourceText: TextView
    private lateinit var proofText: TextView
    private lateinit var finalText: TextView
    private var mode: AndroidReaderMode? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        enterFullScreen()
        profile = configuredProfile(intent)
        controller = GateReaderController(
            PixelGateHttpApi(), this, worker,
            transportOnly = intent.getBooleanExtra(EXTRA_TRANSPORT_ONLY, false),
        )
        check(controller.selectResource(profile))
        setContentView(buildScreen())
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        recreate()
    }

    override fun onResume() {
        super.onResume()
        enterFullScreen()
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
        Log.i(LOG_TAG, value)
        statusText.text = when {
            value.startsWith("READY") -> "READY — TAP LOCKENS PASS"
            value.startsWith("HOLDER") -> "VERIFYING HOLDER PROOF"
            value.startsWith("TRANSPORT CONFIRMED") -> "TRANSPORT CONFIRMED"
            else -> value
        }
        if (value.startsWith("HOLDER")) {
            door.reset()
            credentialText.text = "READING…"
            holderText.text = "VERIFYING…"
            globalText.text = "PENDING"
            resourceText.text = "PENDING"
            proofText.text = "PENDING"
            finalText.text = "VERIFYING…"
            finalText.setTextColor(Color.WHITE)
        }
    }

    override fun credential(value: String) = ui {
        Log.i(LOG_TAG, "GET_CREDENTIAL 9000 credential=$value")
        credentialText.text = value
    }

    override fun decision(value: PixelGateDecision) = ui {
        Log.i(LOG_TAG, "decision=${value.reason} allowed=${value.allowed}")
        holderText.text = value.holder.uppercase()
        globalText.text = value.globalAccess.uppercase()
        resourceText.text = value.resourcePolicy.uppercase()
        proofText.text = value.proof.uppercase()
        finalText.text = if (value.allowed) "ACCESS GRANTED" else "ACCESS DENIED\n${value.reason}"
        finalText.setTextColor(if (value.allowed) GRANTED else DENIED)
        statusText.text = "VERIFIER TRANSPORT CONFIRMED"
        door.showAuthoritativeDecision(value.allowed)
    }

    override fun failure(reason: String) = ui {
        Log.i(LOG_TAG, "failure=$reason")
        statusText.text = "READER STOPPED"
        proofText.text = "FAILED"
        finalText.text = "TECHNICAL ERROR\n$reason"
        finalText.setTextColor(DENIED)
        door.showAuthoritativeDecision(false)
    }

    private fun configuredProfile(intent: Intent): AccessResource {
        val preferences = getSharedPreferences(PROFILE_PREFERENCES, MODE_PRIVATE)
        val store = GateStandProfileStore(
            read = { preferences.getString(PROFILE_KEY, null) },
            write = { preferences.edit().putString(PROFILE_KEY, it).apply() },
        )
        val requested = intent.getStringExtra(EXTRA_GATE_PROFILE)
        return if (requested == null) store.load() else store.configure(requested)
    }

    private fun buildScreen(): View {
        val root = FrameLayout(this).apply { setBackgroundColor(Color.BLACK) }
        root.addView(ImageView(this).apply {
            scaleType = ImageView.ScaleType.CENTER_CROP
            setImageResource(sceneFor(profile.slug))
            contentDescription = "${profile.displayName} original background asset slot"
        }, FrameLayout.LayoutParams(MATCH, MATCH))
        root.addView(View(this).apply {
            background = GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                intArrayOf(Color.argb(80, 0, 0, 0), Color.argb(25, 0, 0, 0), Color.argb(210, 3, 8, 14)),
            )
        }, FrameLayout.LayoutParams(MATCH, MATCH))

        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(22), dp(26), dp(22), dp(20))
        }
        root.addView(content, FrameLayout.LayoutParams(MATCH, MATCH))
        content.addView(label("LOCKENS  •  GATE STAND", 12f, Color.argb(210, 255, 255, 255)).apply {
            letterSpacing = 0.14f
        })
        content.addView(label(profile.displayName.uppercase(), 31f, Color.WHITE).apply {
            typeface = Typeface.DEFAULT_BOLD
            setPadding(0, dp(4), 0, 0)
        })
        content.addView(label(sceneCaption(profile.slug), 13f, Color.argb(210, 255, 255, 255)))

        door = GateDoorView(this, profile.slug)
        content.addView(door, LinearLayout.LayoutParams(MATCH, 0, 1f).apply {
            topMargin = dp(8)
            bottomMargin = dp(8)
        })

        statusText = label("STARTING READER", 13f, Color.WHITE).apply {
            gravity = Gravity.CENTER
            typeface = Typeface.DEFAULT_BOLD
            background = rounded(Color.argb(190, 17, 25, 34), dp(18).toFloat(), Color.argb(100, 255, 255, 255))
            setPadding(dp(12), dp(9), dp(12), dp(9))
        }
        content.addView(statusText, LinearLayout.LayoutParams(MATCH, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
            bottomMargin = dp(10)
        })

        val panel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(12), dp(16), dp(12))
            background = rounded(Color.argb(225, 12, 18, 26), dp(20).toFloat(), Color.argb(80, 255, 255, 255))
        }
        credentialText = value(); panel.addView(securityRow("Credential", credentialText))
        holderText = value(); panel.addView(securityRow("Holder", holderText))
        globalText = value(); panel.addView(securityRow("Global Access", globalText))
        resourceText = value(); panel.addView(securityRow("Resource Access", resourceText))
        proofText = value(); panel.addView(securityRow("Proof", proofText))
        finalText = value().apply { typeface = Typeface.DEFAULT_BOLD }
        panel.addView(securityRow("Final Decision", finalText))
        content.addView(panel, LinearLayout.LayoutParams(MATCH, ViewGroup.LayoutParams.WRAP_CONTENT))
        return root
    }

    private fun securityRow(title: String, value: TextView): View = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        minimumHeight = dp(38)
        addView(label(title.uppercase(), 10f, Color.argb(175, 255, 255, 255)).apply {
            letterSpacing = 0.08f
        }, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 0.37f))
        addView(value, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 0.63f))
    }

    private fun value() = label("—", 12f, Color.WHITE).apply {
        gravity = Gravity.END
        maxLines = 2
    }

    private fun label(value: String, size: Float, color: Int) = TextView(this).apply {
        text = value
        textSize = size
        setTextColor(color)
        includeFontPadding = false
    }

    private fun rounded(color: Int, radius: Float, stroke: Int) = GradientDrawable().apply {
        setColor(color)
        cornerRadius = radius
        setStroke(1, stroke)
    }

    private fun sceneFor(slug: String): Int = when (slug) {
        "front-door" -> R.drawable.gate_scene_front_door
        "server-room" -> R.drawable.gate_scene_server_room
        else -> R.drawable.gate_scene_lab
    }

    private fun sceneCaption(slug: String): String = when (slug) {
        "front-door" -> "MAIN ENTRY • VISITOR APPROACH"
        "server-room" -> "RESTRICTED INFRASTRUCTURE ZONE"
        else -> "RESEARCH FLOOR • CONTROLLED ACCESS"
    }

    @Suppress("DEPRECATION")
    private fun enterFullScreen() {
        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = Color.BLACK
        window.decorView.systemUiVisibility =
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                View.SYSTEM_UI_FLAG_FULLSCREEN or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
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

    private companion object {
        const val LOG_TAG = "LockENS-GateReader"
        const val EXTRA_GATE_PROFILE = "gate_profile"
        const val EXTRA_TRANSPORT_ONLY = "transport_only"
        const val PROFILE_PREFERENCES = "lockens_gate_stand"
        const val PROFILE_KEY = "resource_slug"
        const val MATCH = ViewGroup.LayoutParams.MATCH_PARENT
        val GRANTED = Color.rgb(73, 225, 152)
        val DENIED = Color.rgb(255, 107, 107)
    }
}
