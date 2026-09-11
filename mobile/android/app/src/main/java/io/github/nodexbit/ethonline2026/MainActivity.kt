package io.github.nodexbit.ethonline2026

import android.app.Activity
import android.app.AlertDialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.InputType
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import io.privy.auth.PrivyUser
import io.privy.wallet.ethereum.EmbeddedEthereumWallet
import io.privy.wallet.ethereum.EthereumChain
import io.privy.wallet.ethereum.EthereumRpcRequest
import io.github.nodexbit.ethonline2026.hce.GateC2ManualHarness
import io.github.nodexbit.ethonline2026.hce.PrivyProofProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout

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
    private lateinit var issuerConfirmation: CheckBox
    private lateinit var m1WalletText: TextView
    private lateinit var m1NetworkText: TextView
    private lateinit var m1BalanceText: TextView
    private lateinit var m1StatusText: TextView
    private lateinit var m1Button: Button
    private lateinit var m1RearmButton: Button
    private lateinit var m1ExplorerButton: Button
    private var currentUser: PrivyUser? = null
    private var ethereumWallet: EmbeddedEthereumWallet? = null
    private var gateBSignature: String? = null
    private var m1Runner: MobileIssuerAdmissionRunner? = null
    private var m1OperationId: String? = null
    private val m1ReadinessGate = MobileIssuerReadinessGate()
    private var m1ReadinessJob: Job? = null
    private val m1TransactionEngine by lazy {
        val preferences = getSharedPreferences(M1_PREFERENCES, MODE_PRIVATE)
        RecoverableTransactionEngine(
            TransactionJournal(
                object : TransactionJournalStore {
                    override fun load(): String? = preferences.getString(M1_JOURNAL, null)

                    override fun save(serializedJournal: String) {
                        check(preferences.edit().putString(M1_JOURNAL, serializedJournal).commit()) {
                            "Transaction journal persistence failed"
                        }
                    }
                },
            ),
        )
    }

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

        content.addView(label("MOBILE ISSUER ADMISSION").apply { textSize = 21f })
        m1WalletText = label("CURRENT WALLET:\nNot available").apply { setTextIsSelectable(true) }
        content.addView(m1WalletText, matchWrapParams())
        m1NetworkText = label("NETWORK:\nNot verified")
        content.addView(m1NetworkText, matchWrapParams())
        m1BalanceText = label("BALANCE:\nNot available")
        content.addView(m1BalanceText, matchWrapParams())
        m1StatusText = label("STATUS:\nBLOCKED - dedicated issuer wallet required")
        content.addView(m1StatusText, matchWrapParams())
        issuerConfirmation = CheckBox(this).apply {
            text = "I confirm this separate login is the new dedicated issuer account"
            isEnabled = false
            setOnCheckedChangeListener { _, checked ->
                onIssuerConfirmationChanged(checked)
            }
        }
        content.addView(issuerConfirmation, matchWrapParams())
        m1Button = button("RUN ZERO-VALUE SEPOLIA ADMISSION", ::confirmMobileIssuerAdmission).apply {
            isEnabled = false
        }
        content.addView(m1Button, matchWrapParams())
        m1RearmButton = button("RE-ARM ADMISSION", ::rearmMobileIssuerAdmission).apply {
            visibility = View.GONE
        }
        content.addView(m1RearmButton, matchWrapParams())
        m1ExplorerButton = button("VIEW TRANSACTION IN EXPLORER", ::openM1TransactionExplorer).apply {
            visibility = View.GONE
        }
        content.addView(m1ExplorerButton, matchWrapParams())

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
        resetMobileIssuerAdmission(wallet)
        showStatus(status)
    }

    private fun resetMobileIssuerAdmission(wallet: EmbeddedEthereumWallet) {
        m1ReadinessJob?.cancel()
        m1ReadinessJob = null
        m1ReadinessGate.confirmationChanged(false)
        m1WalletText.text = "CURRENT WALLET:\n${wallet.address}"
        m1NetworkText.text = "NETWORK:\nNot verified"
        m1BalanceText.text = "BALANCE:\nNot available"
        m1RearmButton.visibility = View.GONE
        m1ExplorerButton.visibility = View.GONE
        issuerConfirmation.setOnCheckedChangeListener(null)
        issuerConfirmation.isChecked = false
        val provider = object : MobileIssuerWalletProvider {
            override suspend fun switchToSepolia() {
                wallet.provider.switchChain(EthereumChain.Sepolia)
            }

            override suspend fun sendTransaction(transactionJson: String): String =
                wallet.provider.request(
                    EthereumRpcRequest.ethSendTransaction(transactionJson),
                ).getOrThrow().data
        }
        m1Runner = MobileIssuerAdmissionRunner(
            provider = provider,
            readProvider = ReadOnlyMobileIssuerProvider(ReadOnlyEthereumRpcClient()),
        )
        val operation = migrateLegacyM1IfNeeded(wallet.address) ?:
            m1TransactionEngine.latestForWallet(wallet.address, MobileIssuerJournalObserver.OPERATION_TYPE)
        m1OperationId = operation?.operationId
        issuerConfirmation.isChecked = MobileIssuerUiPolicy.restoreConfirmedCheckbox(operation?.state)
        issuerConfirmation.isEnabled = MobileIssuerUiPolicy.issuerConfirmationEnabled(operation?.state)
        issuerConfirmation.isEnabled = issuerConfirmation.isEnabled &&
            !wallet.address.equals(MobileIssuerAdmissionRunner.KNOWN_HOLDER, ignoreCase = true)
        issuerConfirmation.setOnCheckedChangeListener { _, checked -> onIssuerConfirmationChanged(checked) }

        when {
            operation == null && issuerConfirmation.isEnabled ->
                blockMobileIssuer("DEDICATED_ISSUER_CONFIRMATION_REQUIRED")
            operation?.state in setOf(
                TransactionOperationState.LEGACY_ATTEMPT_REQUIRES_RECONCILIATION,
                TransactionOperationState.SUBMITTING_NO_HASH,
            ) -> reconcileM1Operation(operation!!)
            operation?.state == TransactionOperationState.UNKNOWN && operation.txHash == null ->
                reconcileM1Operation(operation)
            operation?.txHash != null && operation.state !in setOf(
                TransactionOperationState.CONFIRMED,
                TransactionOperationState.REVERTED,
            ) -> reconcileM1Operation(operation)
            operation != null -> renderM1Operation(operation)
            issuerConfirmation.isEnabled ->
            blockMobileIssuer("DEDICATED_ISSUER_CONFIRMATION_REQUIRED")
            else -> blockMobileIssuer("KNOWN_HOLDER_BLOCKED")
        }
    }

    private fun onIssuerConfirmationChanged(checked: Boolean) {
        Log.i(M1_LOG_TAG, "M1_CONFIRMATION_CHANGED checked=$checked")
        val generation = m1ReadinessGate.confirmationChanged(checked)
        if (!checked) {
            m1ReadinessJob?.cancel()
            m1ReadinessJob = null
            blockMobileIssuer("DEDICATED_ISSUER_CONFIRMATION_REQUIRED")
            setBusy(false)
            return
        }
        if (generation != null) prepareMobileIssuerAdmission(generation)
    }

    private fun prepareMobileIssuerAdmission(generation: Long) {
        val wallet = ethereumWallet ?: run {
            m1ReadinessGate.finish(generation)
            return blockMobileIssuer("WALLET_REQUIRED")
        }
        val runner = m1Runner ?: run {
            m1ReadinessGate.finish(generation)
            return blockMobileIssuer("RUNNER_NOT_AVAILABLE")
        }
        val existing = m1TransactionEngine.latestForWallet(
            wallet.address,
            MobileIssuerJournalObserver.OPERATION_TYPE,
        )
        if (existing != null && existing.state != TransactionOperationState.DRAFT) {
            m1ReadinessGate.finish(generation)
            return renderM1Operation(existing)
        }
        m1Button.isEnabled = false
        operationButtons.forEach { it.isEnabled = false }
        m1StatusText.text = "STATUS:\nCHECKING"
        Log.i(M1_LOG_TAG, "M1_READINESS_STARTED issuer=${wallet.address}")
        m1ReadinessJob = activityScope.launch {
            val inspection = try {
                withTimeout(M1_READINESS_TIMEOUT_MILLIS) {
                    withContext(Dispatchers.IO) {
                        runner.inspect(wallet.address, dedicatedIssuerConfirmed = true) { event ->
                            val diagnostic = event.diagnostic
                            when (event.outcome) {
                                MobileIssuerStageOutcome.START ->
                                    Log.i(M1_LOG_TAG, "M1_STAGE_START stage=${event.stage.name}")
                                MobileIssuerStageOutcome.PASS ->
                                    Log.i(M1_LOG_TAG, "M1_STAGE_PASS stage=${event.stage.name}")
                                MobileIssuerStageOutcome.FAIL ->
                                    Log.i(
                                        M1_LOG_TAG,
                                        "M1_STAGE_FAIL stage=${event.stage.name} " +
                                            "category=${diagnostic?.category ?: "ERROR"} " +
                                            "code=${diagnostic?.code ?: "UNAVAILABLE"}",
                                    )
                            }
                        }
                    }
                }
            } catch (_: TimeoutCancellationException) {
                if (!m1ReadinessGate.isCurrent(generation)) return@launch
                blockMobileIssuer("READINESS_TIMEOUT")
                Log.i(M1_LOG_TAG, "M1_READINESS_ERROR category=TIMEOUT")
                m1ReadinessGate.finish(generation)
                m1ReadinessJob = null
                setBusy(false)
                return@launch
            } catch (_: CancellationException) {
                return@launch
            } catch (error: Throwable) {
                if (!m1ReadinessGate.isCurrent(generation)) return@launch
                val category = safeErrorClass(error)
                blockMobileIssuer("READINESS_ERROR_$category")
                Log.i(M1_LOG_TAG, "M1_READINESS_ERROR category=$category")
                m1ReadinessGate.finish(generation)
                m1ReadinessJob = null
                setBusy(false)
                return@launch
            }
            if (!m1ReadinessGate.isCurrent(generation) || !issuerConfirmation.isChecked) return@launch
            m1NetworkText.text = if (inspection.chainId == MobileIssuerAdmissionRunner.SEPOLIA_CHAIN_ID) {
                "NETWORK:\nSepolia (11155111)"
            } else {
                "NETWORK:\nNot verified"
            }
            m1BalanceText.text = "BALANCE:\n${MobileIssuerAdmissionRunner.formatBalance(inspection.balanceWei)}"
            m1StatusText.text = inspection.diagnostic?.let { diagnostic ->
                buildString {
                    append("STATUS:\nBLOCKED - ${diagnostic.stage.name} - ${diagnostic.category}")
                    diagnostic.code?.let { append("\nCODE: $it") }
                    diagnostic.message?.let { append("\nMESSAGE: $it") }
                }
            } ?: "STATUS:\n${inspection.status} - ${inspection.category}"
            Log.i(
                M1_LOG_TAG,
                "M1_READINESS_${if (inspection.canSubmit) "READY" else "BLOCKED"} " +
                    "issuer=${wallet.address} category=${inspection.category} " +
                    "chainId=${inspection.chainId ?: "unknown"} " +
                    "balanceWei=${inspection.balanceWei ?: "unknown"} " +
                    "latestNonce=${inspection.latestNonce ?: "unknown"} " +
                    "pendingNonce=${inspection.pendingNonce ?: "unknown"}",
            )
            if (inspection.canSubmit) {
                val existing = m1TransactionEngine.latestForWallet(
                    wallet.address,
                    MobileIssuerJournalObserver.OPERATION_TYPE,
                )
                val operation = existing?.takeIf { it.state == TransactionOperationState.DRAFT }
                    ?: m1TransactionEngine.create(m1Intent(wallet.address))
                val reviewable = m1TransactionEngine.transition(
                    operation.operationId,
                    TransactionOperationState.READY_TO_REVIEW,
                )
                val submittable = m1TransactionEngine.transition(
                    reviewable.operationId,
                    TransactionOperationState.READY_TO_SUBMIT,
                )
                m1OperationId = submittable.operationId
            }
            if (m1ReadinessGate.finish(generation)) {
                m1ReadinessJob = null
                setBusy(false)
            }
        }
    }

    private fun confirmMobileIssuerAdmission() {
        val wallet = ethereumWallet ?: return blockMobileIssuer("WALLET_REQUIRED")
        val operationId = m1OperationId ?: return blockMobileIssuer("OPERATION_REQUIRED")
        val operation = m1TransactionEngine.find(operationId)
            ?: return blockMobileIssuer("OPERATION_REQUIRED")
        val reviewSummary = try {
            MobileIssuerReviewFlow.open(operation, wallet.address)
        } catch (_: IllegalArgumentException) {
            return blockMobileIssuer("OPERATION_NOT_READY_TO_REVIEW")
        }
        AlertDialog.Builder(this)
            .setTitle("M1 developer admission")
            .setMessage(reviewSummary)
            .setNegativeButton("CANCEL", null)
            .setPositiveButton("SUBMIT ONCE") { _, _ ->
                val current = m1TransactionEngine.find(operationId) ?: return@setPositiveButton
                if (!MobileIssuerUiPolicy.reviewEnabled(current.state)) {
                    return@setPositiveButton blockMobileIssuer("OPERATION_NOT_READY_TO_SUBMIT")
                }
                runMobileIssuerAdmission(wallet.address, operationId)
            }
            .show()
    }

    private fun runMobileIssuerAdmission(issuer: String, operationId: String) {
        val runner = m1Runner ?: return blockMobileIssuer("RUNNER_NOT_AVAILABLE")
        setBusy(true)
        activityScope.launch {
            val result = runner.submitReviewedOperation(
                issuer = issuer,
                dedicatedIssuerConfirmed = issuerConfirmation.isChecked,
                onStatus = { m1StatusText.text = "STATUS:\nSubmitting" },
                observer = m1Observer(operationId),
            )
            val evidence = result.evidence
            m1StatusText.text = if (evidence == null) {
                "STATUS:\n${result.status} - ${result.category}"
            } else {
                "STATUS:\nM1 CONFIRMED\n" +
                    "Issuer I: ${evidence.issuer}\n" +
                    "Chain: 11155111\n" +
                    "Transaction: ${evidence.transactionHash}\n" +
                    "Block: ${evidence.blockNumber}\n" +
                    "Pre nonce: ${evidence.preNonce}\n" +
                    "Post latest nonce: ${evidence.postLatestNonce}\n" +
                    "Post pending nonce: ${evidence.postPendingNonce}\n" +
                    "Transaction readback: PASS\nReceipt: PASS"
            }
            val persisted = m1TransactionEngine.find(operationId)
            if (persisted?.state == TransactionOperationState.UNKNOWN && persisted.txHash == null) {
                reconcileM1Operation(persisted)
                return@launch
            }
            persisted?.let(::renderM1Operation)
            result.failure?.let { m1StatusText.text = MobileIssuerUiPolicy.preSubmitFailureText(it) }
            setBusy(false)
        }
    }

    private fun m1Observer(operationId: String) = MobileIssuerJournalObserver(
        m1TransactionEngine,
        operationId,
    ) { operation ->
        runOnUiThread {
            m1StatusText.text = when (operation.state) {
                TransactionOperationState.SUBMITTING_NO_HASH -> "STATUS:\nSubmitting"
                TransactionOperationState.HASH_RECEIVED ->
                    "STATUS:\nTransaction submitted\n${operation.txHash}"
                TransactionOperationState.CONFIRMING -> "STATUS:\nConfirming"
                TransactionOperationState.ONCHAIN_READBACK -> "STATUS:\nVerifying on-chain readback"
                else -> m1StatusText.text
            }
        }
    }

    private fun migrateLegacyM1IfNeeded(walletAddress: String): PersistedTransactionOperation? {
        val preferences = getSharedPreferences(M1_PREFERENCES, MODE_PRIVATE)
        if (!walletAddress.equals(M1_RECOVERY_ISSUER, ignoreCase = true) ||
            !preferences.getBoolean(M1_ATTEMPTED, false) ||
            preferences.getBoolean(M1_LEGACY_MIGRATED, false)
        ) {
            return null
        }
        val operation = m1TransactionEngine.migrateLegacyAttemptIfNeeded(
            m1Intent(walletAddress),
            legacyAttempted = true,
            alreadyMigrated = false,
        ) ?: return null
        check(preferences.edit().putBoolean(M1_LEGACY_MIGRATED, true).commit()) {
            "Legacy M1 migration marker persistence failed"
        }
        return operation
    }

    private fun reconcileM1Operation(operation: PersistedTransactionOperation) {
        val runner = m1Runner ?: return blockMobileIssuer("RUNNER_NOT_AVAILABLE")
        issuerConfirmation.isEnabled = false
        m1Button.isEnabled = false
        m1RearmButton.visibility = View.GONE
        m1StatusText.text = "STATUS:\nReconciling previous admission attempt"
        activityScope.launch {
            val current = try {
                withTimeout(M1_READINESS_TIMEOUT_MILLIS) {
                    withContext(Dispatchers.IO) {
                        if (operation.txHash == null) {
                            val (latest, pending) = runner.reconcileNoBroadcast(operation.walletAddress)
                            m1TransactionEngine.proveNoBroadcast(
                                operation.operationId,
                                latest.toString(),
                                pending.toString(),
                            )
                        } else {
                            val preNonce = operation.preLatestNonce?.toBigIntegerOrNull()
                                ?: return@withContext m1TransactionEngine.markUnknown(
                                    operation.operationId,
                                    "RECOVERY_PRE_NONCE_MISSING",
                                )
                            runner.reconcile(
                                operation.walletAddress,
                                operation.txHash,
                                preNonce,
                                m1Observer(operation.operationId),
                            )
                            m1TransactionEngine.find(operation.operationId)!!
                        }
                    }
                }
            } catch (_: TimeoutCancellationException) {
                val latest = m1TransactionEngine.find(operation.operationId)!!
                if (latest.state == TransactionOperationState.UNKNOWN) latest
                else m1TransactionEngine.markUnknown(operation.operationId, "RECOVERY_TIMEOUT")
            } catch (_: CancellationException) {
                return@launch
            } catch (error: Throwable) {
                val latest = m1TransactionEngine.find(operation.operationId)!!
                if (latest.state == TransactionOperationState.UNKNOWN) latest
                else m1TransactionEngine.markUnknown(
                    operation.operationId,
                    "RECOVERY_${safeErrorClass(error).uppercase()}",
                )
            }
            renderM1Operation(current)
            setBusy(false)
        }
    }

    private fun renderM1Operation(operation: PersistedTransactionOperation) {
        m1OperationId = operation.operationId
        if (MobileIssuerUiPolicy.restoreConfirmedCheckbox(operation.state)) {
            issuerConfirmation.setOnCheckedChangeListener(null)
            issuerConfirmation.isChecked = true
            issuerConfirmation.setOnCheckedChangeListener { _, checked -> onIssuerConfirmationChanged(checked) }
        }
        issuerConfirmation.isEnabled = MobileIssuerUiPolicy.issuerConfirmationEnabled(operation.state)
        m1Button.isEnabled = MobileIssuerUiPolicy.reviewEnabled(operation.state)
        m1RearmButton.visibility = if (operation.state == TransactionOperationState.NO_BROADCAST_PROVEN) {
            View.VISIBLE
        } else {
            View.GONE
        }
        m1ExplorerButton.visibility = if (operation.txHash != null) View.VISIBLE else View.GONE
        m1StatusText.text = MobileIssuerUiPolicy.operationStatusText(operation) ?: when (operation.state) {
            TransactionOperationState.NO_BROADCAST_PROVEN ->
                "STATUS:\nPrevious admission attempt did not reach Sepolia.\n" +
                    "Operation: ${operation.operationId}" +
                    operation.safeFailureDetails() +
                    "\nRe-arm admission to create a new operation."
            TransactionOperationState.CONFIRMED ->
                "STATUS:\nConfirmed\nOperation: ${operation.operationId}\n" +
                    "Transaction: ${operation.txHash}\nBlock: ${operation.receiptBlock}\n" +
                    "Pre nonce: ${operation.preLatestNonce}\nPost nonce: ${operation.postLatestNonce}"
            TransactionOperationState.REVERTED ->
                "STATUS:\nFailed - transaction reverted\nOperation: ${operation.operationId}\n" +
                    "Transaction: ${operation.txHash}"
            TransactionOperationState.UNKNOWN ->
                "STATUS:\nCould not determine transaction status\nOperation: ${operation.operationId}\n" +
                    "Stage/category: ${operation.safeErrorCategory ?: "UNKNOWN"}"
            TransactionOperationState.DRAFT ->
                "STATUS:\nAdmission re-armed. Confirm the dedicated issuer to run readiness."
            else -> "STATUS:\n${operation.state.name.replace('_', ' ')}\nOperation: ${operation.operationId}"
        }
    }

    private fun rearmMobileIssuerAdmission() {
        val wallet = ethereumWallet ?: return blockMobileIssuer("WALLET_REQUIRED")
        val historicalId = m1OperationId ?: return blockMobileIssuer("RECOVERY_OPERATION_REQUIRED")
        val rearmed = try {
            m1TransactionEngine.rearm(historicalId, m1Intent(wallet.address))
        } catch (error: Throwable) {
            return blockMobileIssuer("REARM_${safeErrorClass(error).uppercase()}")
        }
        m1OperationId = rearmed.operationId
        issuerConfirmation.setOnCheckedChangeListener(null)
        issuerConfirmation.isChecked = false
        issuerConfirmation.isEnabled = true
        issuerConfirmation.setOnCheckedChangeListener { _, checked -> onIssuerConfirmationChanged(checked) }
        renderM1Operation(rearmed)
    }

    private fun PersistedTransactionOperation.safeFailureDetails(): String = buildString {
        failureStage?.let { append("\nFailure stage: $it") }
        safeErrorCategory?.let { append("\nCategory: $it") }
        safeExceptionClass?.let { append("\nException: $it") }
        safeErrorMessage?.let { append("\nMessage: $it") }
    }

    private fun openM1TransactionExplorer() {
        val hash = m1OperationId
            ?.let(m1TransactionEngine::find)
            ?.txHash
            ?: return
        startActivity(
            Intent(
                Intent.ACTION_VIEW,
                Uri.parse(RecoverableTransactionEngine.sepoliaExplorerUrl(hash)),
            ),
        )
    }

    private fun m1Intent(walletAddress: String) = TransactionIntent(
        operationType = MobileIssuerJournalObserver.OPERATION_TYPE,
        walletAddress = walletAddress,
        chainId = MobileIssuerAdmissionRunner.SEPOLIA_CHAIN_ID.toLong(),
        targetAddress = walletAddress,
        valueWei = "0",
        dataSummary = "0x",
    )

    private fun blockMobileIssuer(category: String) {
        if (::m1StatusText.isInitialized) m1StatusText.text = "STATUS:\nBLOCKED - $category"
        if (::m1Button.isInitialized) m1Button.isEnabled = false
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
        if (::issuerConfirmation.isInitialized) {
            val operation = ethereumWallet?.address?.let {
                m1TransactionEngine.latestForWallet(it, MobileIssuerJournalObserver.OPERATION_TYPE)
            }
            issuerConfirmation.isEnabled = !isBusy && ethereumWallet != null &&
                MobileIssuerUiPolicy.issuerConfirmationEnabled(operation?.state) &&
                !ethereumWallet!!.address.equals(MobileIssuerAdmissionRunner.KNOWN_HOLDER, true)
        }
        if (::m1Button.isInitialized) {
            val operation = ethereumWallet?.address?.let {
                m1TransactionEngine.latestForWallet(it, MobileIssuerJournalObserver.OPERATION_TYPE)
            }
            m1Button.isEnabled = !isBusy && MobileIssuerUiPolicy.reviewEnabled(operation?.state)
        }
        if (::m1RearmButton.isInitialized) m1RearmButton.isEnabled = !isBusy
        if (::m1ExplorerButton.isInitialized) m1ExplorerButton.isEnabled = !isBusy
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
        const val M1_PREFERENCES = "mobile_issuer_admission"
        const val M1_ATTEMPTED = "submission_attempted"
        const val M1_LEGACY_MIGRATED = "legacy_submission_attempt_migrated"
        const val M1_JOURNAL = "transaction_journal_v1"
        const val M1_RECOVERY_ISSUER = "0xFa90e8301A22833B74378C5fA3a7c120Ac512685"
        const val M1_LOG_TAG = "M1Admission"
        const val M1_READINESS_TIMEOUT_MILLIS = 30_000L
        val SIGNATURE_PATTERN = Regex("^0x[0-9a-fA-F]{130}$")
    }
}
