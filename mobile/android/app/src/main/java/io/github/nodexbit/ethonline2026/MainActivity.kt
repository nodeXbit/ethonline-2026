package io.github.nodexbit.ethonline2026

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.os.Bundle
import android.text.InputType
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import io.privy.auth.PrivyUser
import io.privy.wallet.ethereum.EmbeddedEthereumWallet
import io.privy.wallet.ethereum.EthereumRpcRequest
import io.github.nodexbit.ethonline2026.hce.GateC2ManualHarness
import io.github.nodexbit.ethonline2026.hce.PrivyProofProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class MainActivity : Activity() {
    private val activityScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private lateinit var emailInput: EditText
    private lateinit var otpInput: EditText
    private lateinit var walletText: TextView
    private lateinit var hceSignerText: TextView
    private lateinit var hceResultText: TextView
    private lateinit var signatureText: TextView
    private lateinit var statusText: TextView
    private lateinit var operationButtons: List<Button>
    private lateinit var copyWalletButton: Button
    private lateinit var copySignatureButton: Button
    private var currentUser: PrivyUser? = null
    private var ethereumWallet: EmbeddedEthereumWallet? = null
    private var gateBSignature: String? = null

    private val gateBApplication: GateBApplication
        get() = application as GateBApplication

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(buildContentView())

        if (!gateBApplication.isPrivyConfigured) {
            setBusy(true)
            showStatus(
                "Local Privy configuration is missing. Copy " +
                    "privy.local.properties.example to privy.local.properties and fill only " +
                    "PRIVY_APP_ID and PRIVY_APP_CLIENT_ID.",
            )
            return
        }

        activityScope.launch {
            currentUser = gateBApplication.privy.getUser()
            if (currentUser == null) {
                showHceSignerStatus("LOGIN REQUIRED")
                showStatus("Ready for email login.")
            } else {
                showStatus("Existing authenticated session restored.")
                reuseExistingWallet()
            }
        }
    }

    override fun onDestroy() {
        activityScope.cancel()
        super.onDestroy()
    }

    private fun buildContentView(): ScrollView {
        val padding = (20 * resources.displayMetrics.density).toInt()
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(padding, padding, padding, padding)
        }

        fun label(value: String) = TextView(this).apply {
            text = value
            textSize = 16f
            setPadding(0, padding / 2, 0, padding / 4)
        }

        fun button(value: String, onClick: () -> Unit) = Button(this).apply {
            text = value
            setOnClickListener { onClick() }
        }

        content.addView(label("ENSv2 Access Demo\nGate B — Privy holder signing").apply {
            textSize = 21f
        })

        emailInput = EditText(this).apply {
            hint = "Email address"
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
        }
        content.addView(emailInput, matchWrapParams())
        val sendCodeButton = button("Send code", ::sendCode)
        content.addView(sendCodeButton, matchWrapParams())

        otpInput = EditText(this).apply {
            hint = "OTP code"
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_VARIATION_PASSWORD
        }
        content.addView(otpInput, matchWrapParams())
        val loginButton = button("Log in", ::logIn)
        content.addView(loginButton, matchWrapParams())

        val walletButton = button("Create/reuse Ethereum wallet", ::createOrReuseWallet)
        content.addView(walletButton, matchWrapParams())
        walletText = label("PRIVY WALLET:\nNot available").apply {
            setTextIsSelectable(true)
        }
        content.addView(walletText, matchWrapParams())
        copyWalletButton = button("Copy wallet address", ::copyWalletAddress).apply {
            isEnabled = false
        }
        content.addView(copyWalletButton, matchWrapParams())
        hceSignerText = label("HCE SIGNER:\nLOGIN REQUIRED")
        content.addView(hceSignerText, matchWrapParams())

        val signButton = button("Sign Gate B challenge", ::signGateBChallenge)
        content.addView(signButton, matchWrapParams())
        val hceTestButton = button("Run Gate C2 HCE signing test", ::runGateC2HceTest)
        content.addView(hceTestButton, matchWrapParams())
        hceResultText = label("GATE C2 HCE TEST:\nNot run")
        content.addView(hceResultText, matchWrapParams())
        signatureText = label("SIGNATURE:\nNot available").apply {
            setTextIsSelectable(true)
        }
        content.addView(signatureText, matchWrapParams())
        copySignatureButton = button("Copy signature", ::copySignature).apply {
            isEnabled = false
        }
        content.addView(copySignatureButton, matchWrapParams())

        statusText = label("Initializing…")
        content.addView(statusText, matchWrapParams())
        operationButtons = listOf(
            sendCodeButton,
            loginButton,
            walletButton,
            signButton,
            hceTestButton,
        )

        return ScrollView(this).apply { addView(content) }
    }

    private fun matchWrapParams() = ViewGroup.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT,
        ViewGroup.LayoutParams.WRAP_CONTENT,
    )

    private fun sendCode() {
        val email = emailInput.text.toString().trim()
        if (!email.contains('@')) {
            showStatus("Enter a valid email address.")
            return
        }
        runAction("Sending code…") {
            gateBApplication.privy.email.sendCode(email = email).fold(
                onSuccess = { showStatus("Code sent. Enter the OTP manually.") },
                onFailure = { showSafeFailure("Send code", it) },
            )
        }
    }

    private fun logIn() {
        val email = emailInput.text.toString().trim()
        val code = otpInput.text.toString().trim()
        if (!email.contains('@') || code.isEmpty()) {
            showStatus("Enter the email address and OTP code.")
            return
        }
        runAction("Authenticating…") {
            gateBApplication.privy.email.loginWithCode(code = code, email = email).fold(
                onSuccess = { user ->
                    otpInput.text.clear()
                    currentUser = user
                    showStatus("Email authentication succeeded.")
                    reuseExistingWallet()
                },
                onFailure = { showSafeFailure("Email authentication", it) },
            )
        }
    }

    private fun createOrReuseWallet() {
        runAction("Preparing embedded Ethereum wallet…") {
            val user = currentUser ?: gateBApplication.privy.getUser()
            if (user == null) {
                showStatus("Log in before creating a wallet.")
                return@runAction
            }
            currentUser = user
            val existing = user.embeddedEthereumWallets.firstOrNull()
            if (existing != null) {
                selectWallet(existing, "Existing embedded Ethereum wallet reused.")
                return@runAction
            }
            user.createEthereumWallet(allowAdditional = false).fold(
                onSuccess = { selectWallet(it, "Embedded Ethereum wallet created.") },
                onFailure = { showSafeFailure("Wallet creation", it) },
            )
        }
    }

    private fun reuseExistingWallet() {
        val existing = currentUser?.embeddedEthereumWallets?.firstOrNull()
        if (existing == null) {
            showHceSignerStatus("CREATE WALLET FIRST")
            return
        }
        selectWallet(existing, "Existing embedded Ethereum wallet reused.")
    }

    private fun selectWallet(wallet: EmbeddedEthereumWallet, status: String) {
        ethereumWallet = wallet
        walletText.text = "PRIVY WALLET:\n${wallet.address}"
        copyWalletButton.isEnabled = true
        showHceSignerStatus("READY")
        clearSignature()
        showStatus(status)
    }

    private fun copyWalletAddress() {
        val address = ethereumWallet?.address ?: return
        copyPublicProof("Privy wallet address", address)
    }

    private fun copySignature() {
        val signature = gateBSignature ?: return
        copyPublicProof("EIP-712 signature", signature)
    }

    private fun copyPublicProof(label: String, value: String) {
        val clipboard = getSystemService(ClipboardManager::class.java)
        clipboard.setPrimaryClip(ClipData.newPlainText(label, value))
        showStatus("$label copied. This is public proof data.")
    }

    private fun signGateBChallenge() {
        runAction("Requesting eth_signTypedData_v4…") {
            val wallet = ethereumWallet
            if (wallet == null) {
                showStatus("Create or reuse the embedded Ethereum wallet first.")
                return@runAction
            }
            val request = EthereumRpcRequest.ethSignTypedDataV4(
                address = wallet.address,
                typedDataJson = GateBTestVector.json(),
            )
            wallet.provider.request(request).fold(
                onSuccess = { response ->
                    val signature = response.data.trim().removeSurrounding("\"")
                    if (SIGNATURE_PATTERN.matches(signature)) {
                        gateBSignature = signature
                        signatureText.text = "SIGNATURE:\n$signature"
                        copySignatureButton.isEnabled = true
                        showStatus("eth_signTypedData_v4: SUPPORTED. Signature ready for Node verification.")
                    } else {
                        clearSignature()
                        showStatus("eth_signTypedData_v4 returned an unexpected response format.")
                    }
                },
                onFailure = { error ->
                    clearSignature()
                    showStatus(
                        "eth_signTypedData_v4: UNSUPPORTED/REJECTED (${safeErrorClass(error)}). " +
                            "Gate B is unsupported for this SDK/provider result.",
                    )
                },
            )
        }
    }

    private fun runGateC2HceTest() {
        val wallet = ethereumWallet
        if (wallet == null) {
            showHceSignerStatus(if (currentUser == null) "LOGIN REQUIRED" else "CREATE WALLET FIRST")
            showStatus("Authenticate and create or reuse the embedded Ethereum wallet first.")
            return
        }
        hceResultText.text = "GATE C2 HCE TEST:\nPROCESSING"
        runAction("Running SELECT → SEND_CHALLENGE → GET_STATUS through HCE processor…") {
            try {
                val result = GateC2ManualHarness.run(gateBApplication.hceProcessor)
                val signature = PrivyProofProvider.encodeSignature(result.signature)
                gateBSignature = signature
                signatureText.text = "SIGNATURE:\n$signature"
                copySignatureButton.isEnabled = true
                hceResultText.text =
                    "HCE SIGNER:\nREADY\n" +
                    "HCE STATE:\n${result.state}\n" +
                    "HCE SIGNATURE LENGTH:\n${result.signature.size}\n" +
                    "RESULT:\nPASS"
                showStatus(
                    "Gate C2 HCE path produced a real eth_signTypedData_v4 signature " +
                        "for ${wallet.address}.",
                )
            } catch (error: Throwable) {
                clearSignature()
                hceResultText.text =
                    "HCE STATE:\n${gateBApplication.hceProcessor.currentState()}\n" +
                    "RESULT:\nFAIL (${safeErrorClass(error)})"
                showSafeFailure("Gate C2 HCE signing test", error)
            }
        }
    }

    private fun runAction(progress: String, action: suspend () -> Unit) {
        setBusy(true)
        showStatus(progress)
        activityScope.launch {
            try {
                action()
            } catch (error: Throwable) {
                showSafeFailure("Operation", error)
            } finally {
                setBusy(false)
            }
        }
    }

    private fun setBusy(isBusy: Boolean) {
        operationButtons.forEach { it.isEnabled = !isBusy }
        copyWalletButton.isEnabled = !isBusy && ethereumWallet != null
        copySignatureButton.isEnabled = !isBusy && gateBSignature != null
    }

    private fun clearSignature() {
        gateBSignature = null
        signatureText.text = "SIGNATURE:\nNot available"
        copySignatureButton.isEnabled = false
    }

    private fun showHceSignerStatus(status: String) {
        hceSignerText.text = "HCE SIGNER:\n$status"
    }

    private fun showSafeFailure(operation: String, error: Throwable) {
        showStatus("$operation failed (${safeErrorClass(error)}).")
    }

    private fun safeErrorClass(error: Throwable): String =
        error::class.simpleName?.take(80) ?: "Error"

    private fun showStatus(status: String) {
        statusText.text = "STATUS:\n$status"
    }

    private companion object {
        val SIGNATURE_PATTERN = Regex("^0x[0-9a-fA-F]{130}$")
    }
}
