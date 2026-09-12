package io.github.nodexbit.ethonline2026

import android.app.Activity
import android.app.AlertDialog
import android.app.Dialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.content.res.ColorStateList
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.text.InputType
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.WindowInsets
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import java.math.BigInteger
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
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
    private lateinit var productStatusText: TextView
    private lateinit var myKeysText: TextView
    private lateinit var importCredentialInput: EditText
    private lateinit var issuerProductSection: LinearLayout
    private lateinit var artworkInput: EditText
    private lateinit var createReviewButton: Button
    private lateinit var resumeSetupButton: Button
    private lateinit var copyCredentialButton: Button
    private lateinit var loggedOutView: LinearLayout
    private lateinit var restoringView: LinearLayout
    private lateinit var restoringStatusText: TextView
    private lateinit var authenticatedView: LinearLayout
    private lateinit var navigationBar: LinearLayout
    private lateinit var productScrollView: ScrollView
    private lateinit var myKeysSection: LinearLayout
    private lateinit var myKeysCards: LinearLayout
    private lateinit var myKeysEmptyCard: LinearLayout
    private lateinit var myKeysActions: LinearLayout
    private lateinit var settingsSection: LinearLayout
    private lateinit var diagnosticsSection: LinearLayout
    private lateinit var actionFailureText: TextView
    private lateinit var shellIdentityText: TextView
    private lateinit var settingsIdentityText: TextView
    private lateinit var walletsList: LinearLayout
    private lateinit var capabilityStatusText: TextView
    private lateinit var createWalletButton: Button
    private lateinit var shellFeedbackText: TextView
    private lateinit var loginFeedbackText: TextView
    private lateinit var myKeysNavButton: Button
    private lateinit var issuerNavButton: Button
    private lateinit var settingsNavButton: Button
    private var currentDestination = ProductDestination.MY_KEYS
    private var issuerCapabilityConfirmed = false
    private var issuerCapabilityState = IssuerCapabilityState.UNAVAILABLE
    private var lastActionFailure: SafeActionFailure? = null
    private var currentUser: PrivyUser? = null
    private var ethereumWallet: EmbeddedEthereumWallet? = null
    private var gateBSignature: String? = null
    private var m1Runner: MobileIssuerAdmissionRunner? = null
    private var m1OperationId: String? = null
    private var contractRunner: ContractTransactionRunner? = null
    private var authenticatedWallets: List<EmbeddedEthereumWallet> = emptyList()
    private var lastOwnedCredentials: List<CredentialSnapshot> = emptyList()
    private var lastCredentialRefreshAtMillis = 0L
    private var credentialRefreshJob: Job? = null
    private val credentialRpcClient by lazy { ReadOnlyEthereumRpcClient() }
    private val credentialReader by lazy { CredentialReader(credentialRpcClient) }
    private val credentialFinalReadback by lazy {
        CredentialFinalReadbackReconciler(read = { fullName -> credentialReader.read(fullName) })
    }
    private val credentialTransactionEngine by lazy {
        val preferences = getSharedPreferences(CREDENTIAL_PREFERENCES, MODE_PRIVATE)
        RecoverableTransactionEngine(
            TransactionJournal(
                object : TransactionJournalStore {
                    override fun load(): String? = preferences.getString(CREDENTIAL_TRANSACTION_JOURNAL, null)
                    override fun save(serializedJournal: String) {
                        check(preferences.edit().putString(CREDENTIAL_TRANSACTION_JOURNAL, serializedJournal).commit()) {
                            "Credential transaction journal persistence failed"
                        }
                    }
                },
            ),
        )
    }
    private val issuanceCoordinator by lazy {
        sharedStringStore(CREDENTIAL_ISSUANCE_JOURNAL).let(::IssuanceCoordinator)
    }
    private val credentialIndex by lazy {
        sharedStringStore(CREDENTIAL_LOCAL_INDEX).let(::LocalCredentialIndex)
    }
    private val activeWalletStore by lazy {
        sharedStringStore(ACTIVE_WALLET).let(::ActiveWalletStore)
    }
    private val selectedPassStore by lazy {
        sharedStringStore(SELECTED_PASS).let(::SelectedPassStore)
    }
    private val credentialDiscovery by lazy {
        CredentialDiscoveryService(
            source = R1CredentialCandidateSource(credentialRpcClient),
            read = credentialReader::read,
        )
    }
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
        window.statusBarColor = pageColor()
        window.navigationBarColor = pageColor()
        setContentView(buildContentView())

        if (!gateBApplication.isPrivyConfigured) {
            showLoggedOutShell()
            setBusy(true)
            showStatus(
                "Local Privy configuration is missing. Copy " +
                    "privy.local.properties.example to privy.local.properties and fill only " +
                    "PRIVY_APP_ID and PRIVY_APP_CLIENT_ID.",
            )
            return
        }

        activityScope.launch {
            try {
                currentUser = gateBApplication.privy.getUser()
                if (currentUser == null) {
                    showLoggedOutShell()
                    showHceSignerStatus("LOGIN REQUIRED")
                    showStatus("Ready for email login.")
                } else {
                    restoreAuthenticatedUser(currentUser!!, "Existing authenticated session restored.")
                }
            } catch (error: CancellationException) {
                throw error
            } catch (_: Throwable) {
                restoringStatusText.text = "Session restoration is temporarily unavailable. Reopen the app to retry."
            }
        }
    }

    override fun onResume() {
        super.onResume()
        val wallet = ethereumWallet ?: return
        if (System.currentTimeMillis() - lastCredentialRefreshAtMillis >= FOREGROUND_REFRESH_AGE_MILLIS) {
            refreshProduct(wallet.address)
        }
    }

    override fun onDestroy() {
        activityScope.cancel()
        super.onDestroy()
    }

    private fun buildContentView(): ScrollView {
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(
                dp(ProductSpacing.CONTENT_GUTTER_DP),
                dp(ProductSpacing.TOP_GUTTER_DP),
                dp(ProductSpacing.CONTENT_GUTTER_DP),
                dp(ProductSpacing.BOTTOM_GUTTER_DP),
            )
        }

        restoringView = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            addView(productTitle("ENS Access").apply { gravity = Gravity.CENTER }, matchWrapParams())
            restoringStatusText = productBody("Restoring your secure session…").apply {
                gravity = Gravity.CENTER
                setPadding(0, dp(10), 0, 0)
            }
            addView(restoringStatusText, matchWrapParams())
        }
        content.addView(restoringView, matchWrapParams())

        loggedOutView = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            visibility = View.GONE
        }
        loggedOutView.addView(productTitle("ENS Access"), matchWrapParams())
        loggedOutView.addView(productBody("Programmable credentials powered by ENS").apply {
            gravity = Gravity.CENTER
            setPadding(0, dp(4), 0, dp(28))
        }, matchWrapParams())
        val signInCard = productCard()
        signInCard.addView(productHeading("Sign in"), matchWrapParams())
        signInCard.addView(productCaption("EMAIL"), matchWrapParams())
        emailInput = productInput("you@example.com").apply {
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
        }
        signInCard.addView(emailInput, matchWrapParams())
        val sendCodeButton = productButton("Send code", primary = false, action = ::sendCode)
        signInCard.addView(sendCodeButton, matchWrapParams())
        signInCard.addView(productCaption("VERIFICATION CODE").apply { setPadding(0, dp(18), 0, 0) }, matchWrapParams())
        otpInput = productInput("6-digit code").apply {
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_VARIATION_PASSWORD
        }
        signInCard.addView(otpInput, matchWrapParams())
        val loginButton = productButton("Sign in", action = ::logIn)
        signInCard.addView(loginButton, matchWrapParams())
        loggedOutView.addView(signInCard, cardParams())
        loginFeedbackText = productBody("Enter your email to receive a secure sign-in code.").apply {
            gravity = Gravity.CENTER
            setPadding(dp(12), dp(18), dp(12), 0)
        }
        loggedOutView.addView(loginFeedbackText, matchWrapParams())
        content.addView(loggedOutView, matchWrapParams())

        authenticatedView = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            visibility = View.GONE
        }
        val top = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        val brand = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        brand.addView(productTitle("ENS Access"), matchWrapParams())
        shellIdentityText = accountChip("Wallet not ready")
        brand.addView(shellIdentityText, ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
        ))
        top.addView(brand, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
            marginEnd = dp(12)
        })
        top.addView(statusChip("Sepolia", positive = true), ViewGroup.LayoutParams(dp(92), dp(36)))
        authenticatedView.addView(top, matchWrapParams())

        navigationBar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, dp(20), 0, dp(18))
        }
        myKeysNavButton = navigationButton("My Keys") { showDestination(ProductDestination.MY_KEYS) }
        issuerNavButton = navigationButton("Issuer") { showDestination(ProductDestination.ISSUER) }.apply {
            visibility = View.GONE
        }
        settingsNavButton = navigationButton("Settings") { showDestination(ProductDestination.SETTINGS) }
        navigationBar.addView(myKeysNavButton, weightedParams())
        navigationBar.addView(issuerNavButton, weightedParams())
        navigationBar.addView(settingsNavButton, weightedParams())
        authenticatedView.addView(navigationBar, matchWrapParams())

        shellFeedbackText = productBody("").apply {
            visibility = View.GONE
            background = roundedBackground(surfaceMutedColor(), dp(14).toFloat())
            setPadding(dp(14), dp(12), dp(14), dp(12))
        }
        authenticatedView.addView(shellFeedbackText, cardParams())

        myKeysSection = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        myKeysSection.addView(sectionHeader("MY KEYS", "Your digital access cards."), matchWrapParams())
        myKeysEmptyCard = productCard().apply { gravity = Gravity.CENTER_HORIZONTAL }
        myKeysEmptyCard.addView(emptyCredentialVisual(), LinearLayout.LayoutParams(dp(72), dp(72)).apply {
            bottomMargin = dp(18)
        })
        myKeysText = productHeading("No credentials yet").apply { gravity = Gravity.CENTER }
        myKeysEmptyCard.addView(myKeysText, matchWrapParams())
        myKeysEmptyCard.addView(productBody("Credentials you receive or import will appear here.").apply {
            gravity = Gravity.CENTER
            setPadding(0, dp(8), 0, dp(14))
        }, matchWrapParams())
        importCredentialInput = productInput("credential.keys.demo-access.eth")
        val importButton = productButton("Add by ENS name", action = ::showImportCredentialDialog)
        myKeysEmptyCard.addView(importButton, matchWrapParams())
        myKeysSection.addView(myKeysEmptyCard, cardParams())
        myKeysCards = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        myKeysSection.addView(myKeysCards, matchWrapParams())
        myKeysActions = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            visibility = View.GONE
        }
        val importAnotherButton = productButton("Add by ENS name", primary = false, action = ::showImportCredentialDialog)
        val refreshKeysButton = productButton("Refresh onchain", primary = false, action = ::refreshMyKeys)
        myKeysActions.addView(importAnotherButton, weightedParams())
        myKeysActions.addView(refreshKeysButton, weightedParams())
        myKeysSection.addView(myKeysActions, actionParams())
        authenticatedView.addView(myKeysSection, matchWrapParams())

        issuerProductSection = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            visibility = View.GONE
        }
        issuerProductSection.addView(sectionHeader("NEW CREDENTIAL", "Create a verified digital key for staff access."), matchWrapParams())
        issuerProductSection.addView(detailCard("CREDENTIAL IDENTITY", listOf(
            "Name" to "Staff access",
            "ENS name" to IssuerSpace.fullName,
        )), cardParams())
        issuerProductSection.addView(detailCard("RECIPIENT", listOf(
            "Wallet" to ProductShellPolicy.compactAddress(IssuerSpace.STAFF_HOLDER),
        )), cardParams())
        issuerProductSection.addView(detailCard("ACCESS POLICY", listOf(
            "Expires" to "31 Oct 2026, 23:59",
            "Transferability" to "Non-transferable",
            "Initial access" to "Allowed",
        )), cardParams())
        val presentationCard = productCard()
        presentationCard.addView(productCaption("PRESENTATION"), matchWrapParams())
        presentationCard.addView(productBody("Artwork URI · optional"), matchWrapParams())
        artworkInput = productInput("https:// or ipfs://").apply {
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_URI
        }
        presentationCard.addView(artworkInput, matchWrapParams())
        presentationCard.addView(valueRow("Description", IssuerSpace.DEFAULT_DESCRIPTION), matchWrapParams())
        issuerProductSection.addView(presentationCard, cardParams())
        createReviewButton = productButton("Review credential", action = ::reviewCredential)
        issuerProductSection.addView(createReviewButton, actionParams())
        resumeSetupButton = productButton("Resume setup", action = ::resumeIssuance).apply { visibility = View.GONE }
        issuerProductSection.addView(resumeSetupButton, actionParams())
        copyCredentialButton = productButton("Copy credential name", primary = false, action = ::copyCredentialName).apply {
            visibility = View.GONE
        }
        issuerProductSection.addView(copyCredentialButton, actionParams())
        productStatusText = productBody("").apply {
            visibility = View.GONE
            setTextColor(textPrimaryColor())
            setTypeface(typeface, Typeface.BOLD)
            background = roundedBackground(surfaceMutedColor(), dp(14).toFloat())
            setPadding(dp(16), dp(14), dp(16), dp(14))
        }
        issuerProductSection.addView(productStatusText, cardParams())
        authenticatedView.addView(issuerProductSection, matchWrapParams())

        settingsSection = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            visibility = View.GONE
        }
        settingsSection.addView(sectionHeader("SETTINGS", "Account, network, and developer tools."), matchWrapParams())
        val accountCard = productCard()
        accountCard.addView(productCaption("ACCOUNT"), matchWrapParams())
        accountCard.addView(productCaption("ACTIVE WALLET"), matchWrapParams())
        settingsIdentityText = productBody("Wallet not ready").apply {
            setTextColor(textPrimaryColor())
            setTypeface(typeface, Typeface.BOLD)
            setPadding(0, 0, 0, dp(12))
        }
        accountCard.addView(settingsIdentityText, matchWrapParams())
        walletText = productBody("Current wallet\nNot available").apply {
            textSize = 14f
            setTypeface(Typeface.MONOSPACE)
            setTextIsSelectable(true)
        }
        accountCard.addView(walletText, matchWrapParams())
        copyWalletButton = productButton("Copy full address", primary = false, action = ::copyWalletAddress).apply {
            isEnabled = false
        }
        accountCard.addView(copyWalletButton, matchWrapParams())
        capabilityStatusText = productBody(WalletCapabilityPresentation.issuerLabel(IssuerCapabilityState.UNAVAILABLE)).apply {
            setPadding(0, dp(12), 0, dp(4))
        }
        accountCard.addView(capabilityStatusText, matchWrapParams())
        accountCard.addView(productCaption("WALLETS").apply { setPadding(0, dp(16), 0, dp(6)) }, matchWrapParams())
        walletsList = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        accountCard.addView(walletsList, matchWrapParams())
        createWalletButton = productButton("Create wallet", primary = false, action = ::createAnotherWallet)
        accountCard.addView(createWalletButton, matchWrapParams())
        val logoutButton = productButton("Switch account / Log out", primary = false, action = ::logout)
        accountCard.addView(logoutButton, matchWrapParams())
        settingsSection.addView(accountCard, cardParams())
        settingsSection.addView(detailCard("NETWORK", listOf("Selected network" to "Sepolia")), cardParams())
        val developerCard = productCard()
        developerCard.addView(productCaption("DEVELOPER OPTIONS"), matchWrapParams())
        developerCard.addView(productBody("Admission, signing, HCE, and transaction diagnostics."), matchWrapParams())
        developerCard.addView(productButton("Developer diagnostics", primary = false) {
            showDestination(ProductDestination.DIAGNOSTICS)
        }, matchWrapParams())
        settingsSection.addView(developerCard, cardParams())
        authenticatedView.addView(settingsSection, matchWrapParams())

        diagnosticsSection = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            visibility = View.GONE
        }
        diagnosticsSection.addView(productButton("← Back to Settings", primary = false) {
            showDestination(ProductDestination.SETTINGS)
        }, matchWrapParams())
        diagnosticsSection.addView(sectionHeader("DEVELOPER DIAGNOSTICS", "Technical test surfaces. Not required for normal product use."), matchWrapParams())
        actionFailureText = diagnosticLabel("LAST ACTION FAILURE\nNone")
        diagnosticsSection.addView(actionFailureText, matchWrapParams())
        hceSignerText = diagnosticLabel("HCE SIGNER:\nLOGIN REQUIRED")
        diagnosticsSection.addView(hceSignerText, matchWrapParams())
        val signButton = diagnosticButton("Sign Gate B challenge", ::signGateBChallenge)
        diagnosticsSection.addView(signButton, matchWrapParams())
        val hceTestButton = diagnosticButton("Run Gate C2 HCE signing test", ::runGateC2HceTest)
        diagnosticsSection.addView(hceTestButton, matchWrapParams())
        hceResultText = diagnosticLabel("GATE C2 HCE TEST:\nNot run")
        diagnosticsSection.addView(hceResultText, matchWrapParams())
        signatureText = diagnosticLabel("SIGNATURE:\nNot available").apply { setTextIsSelectable(true) }
        diagnosticsSection.addView(signatureText, matchWrapParams())
        copySignatureButton = diagnosticButton("Copy signature", ::copySignature).apply { isEnabled = false }
        diagnosticsSection.addView(copySignatureButton, matchWrapParams())
        diagnosticsSection.addView(diagnosticLabel("MOBILE ISSUER ADMISSION").apply {
            textSize = 20f
            setTypeface(typeface, Typeface.BOLD)
        }, matchWrapParams())
        m1WalletText = diagnosticLabel("CURRENT WALLET:\nNot available").apply { setTextIsSelectable(true) }
        diagnosticsSection.addView(m1WalletText, matchWrapParams())
        m1NetworkText = diagnosticLabel("NETWORK:\nNot verified")
        diagnosticsSection.addView(m1NetworkText, matchWrapParams())
        m1BalanceText = diagnosticLabel("BALANCE:\nNot available")
        diagnosticsSection.addView(m1BalanceText, matchWrapParams())
        m1StatusText = diagnosticLabel("STATUS:\nBLOCKED - dedicated issuer wallet required")
        diagnosticsSection.addView(m1StatusText, matchWrapParams())
        issuerConfirmation = CheckBox(this).apply {
            text = "I confirm this separate login is the new dedicated issuer account"
            isEnabled = false
            setOnCheckedChangeListener { _, checked -> onIssuerConfirmationChanged(checked) }
        }
        diagnosticsSection.addView(issuerConfirmation, matchWrapParams())
        m1Button = diagnosticButton("RUN ZERO-VALUE SEPOLIA ADMISSION", ::confirmMobileIssuerAdmission).apply {
            isEnabled = false
        }
        diagnosticsSection.addView(m1Button, matchWrapParams())
        m1RearmButton = diagnosticButton("RE-ARM ADMISSION", ::rearmMobileIssuerAdmission).apply {
            visibility = View.GONE
        }
        diagnosticsSection.addView(m1RearmButton, matchWrapParams())
        m1ExplorerButton = diagnosticButton("VIEW TRANSACTION IN EXPLORER", ::openM1TransactionExplorer).apply {
            visibility = View.GONE
        }
        diagnosticsSection.addView(m1ExplorerButton, matchWrapParams())
        statusText = diagnosticLabel("Initializing…")
        diagnosticsSection.addView(statusText, matchWrapParams())
        authenticatedView.addView(diagnosticsSection, matchWrapParams())

        content.addView(authenticatedView, matchWrapParams())
        operationButtons = listOf(
            sendCodeButton, loginButton, createWalletButton, logoutButton, importButton, importAnotherButton,
            refreshKeysButton, signButton, hceTestButton,
        )
        showRestoringShell()
        productScrollView = ScrollView(this).apply {
            isFillViewport = true
            setBackgroundColor(pageColor())
            addView(content)
            setOnApplyWindowInsetsListener { _, insets ->
                val systemLeft: Int
                val systemTop: Int
                val systemRight: Int
                val systemBottom: Int
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    val bars = insets.getInsets(
                        WindowInsets.Type.systemBars() or WindowInsets.Type.displayCutout(),
                    )
                    systemLeft = bars.left
                    systemTop = bars.top
                    systemRight = bars.right
                    systemBottom = bars.bottom
                } else {
                    systemLeft = insets.systemWindowInsetLeft
                    systemTop = insets.systemWindowInsetTop
                    systemRight = insets.systemWindowInsetRight
                    systemBottom = insets.systemWindowInsetBottom
                }
                val contentPadding = ProductSpacing.contentPadding(resources.displayMetrics.density)
                content.setPadding(
                    contentPadding.left,
                    contentPadding.top,
                    contentPadding.right,
                    contentPadding.bottom,
                )
                val viewportPadding = ProductSpacing.viewportPadding(
                    systemLeft,
                    systemTop,
                    systemRight,
                    systemBottom,
                )
                setPadding(
                    viewportPadding.left,
                    viewportPadding.top,
                    viewportPadding.right,
                    viewportPadding.bottom,
                )
                clipToPadding = true
                insets
            }
            requestApplyInsets()
        }
        return productScrollView
    }

    private fun matchWrapParams() = ViewGroup.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT,
        ViewGroup.LayoutParams.WRAP_CONTENT,
    )

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    private fun isDarkMode(): Boolean =
        resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES

    private fun pageColor(): Int = Color.parseColor(if (isDarkMode()) "#121117" else "#F7F6FA")
    private fun surfaceColor(): Int = Color.parseColor(if (isDarkMode()) "#1E1C25" else "#FFFFFF")
    private fun surfaceMutedColor(): Int = Color.parseColor(if (isDarkMode()) "#2A2733" else "#EFEDF5")
    private fun textPrimaryColor(): Int = Color.parseColor(if (isDarkMode()) "#F6F3FA" else "#1B1722")
    private fun textSecondaryColor(): Int = Color.parseColor(if (isDarkMode()) "#BDB6C8" else "#6F6878")
    private fun accentColor(): Int = Color.parseColor(if (isDarkMode()) "#A993FF" else "#5B3FD3")
    private fun successColor(): Int = Color.parseColor(if (isDarkMode()) "#65D99B" else "#16794D")
    private fun borderColor(): Int = Color.parseColor(if (isDarkMode()) "#3A3644" else "#E2DFE8")

    private fun roundedBackground(
        color: Int,
        radius: Float,
        strokeColor: Int? = null,
    ): GradientDrawable = GradientDrawable().apply {
        shape = GradientDrawable.RECTANGLE
        cornerRadius = radius
        setColor(color)
        strokeColor?.let { setStroke(dp(1), it) }
    }

    private fun productTitle(value: String) = TextView(this).apply {
        text = value
        textSize = 28f
        setTextColor(textPrimaryColor())
        setTypeface(typeface, Typeface.BOLD)
    }

    private fun productHeading(value: String) = TextView(this).apply {
        text = value
        textSize = 19f
        setTextColor(textPrimaryColor())
        setTypeface(typeface, Typeface.BOLD)
        setLineSpacing(dp(2).toFloat(), 1f)
    }

    private fun productBody(value: String) = TextView(this).apply {
        text = value
        textSize = 15f
        setTextColor(textSecondaryColor())
        setLineSpacing(dp(3).toFloat(), 1f)
    }

    private fun productCaption(value: String) = TextView(this).apply {
        text = value
        textSize = 12f
        letterSpacing = 0.08f
        setTextColor(textSecondaryColor())
        setTypeface(typeface, Typeface.BOLD)
        setPadding(0, 0, 0, dp(6))
    }

    private fun productCard() = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp(18), dp(18), dp(18), dp(18))
        background = roundedBackground(surfaceColor(), dp(18).toFloat(), borderColor())
        elevation = dp(2).toFloat()
    }

    private fun cardParams() = LinearLayout.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT,
        ViewGroup.LayoutParams.WRAP_CONTENT,
    ).apply { bottomMargin = dp(ProductSpacing.CARD_GAP_DP) }

    private fun actionParams() = LinearLayout.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT,
        ViewGroup.LayoutParams.WRAP_CONTENT,
    ).apply {
        topMargin = dp(4)
        bottomMargin = dp(12)
    }

    private fun weightedParams() = LinearLayout.LayoutParams(0, dp(ProductSpacing.CONTROL_HEIGHT_DP), 1f).apply {
        marginStart = dp(3)
        marginEnd = dp(3)
    }

    private fun fullWidthControlParams(minimumHeightDp: Int) = LinearLayout.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT,
        dp(minimumHeightDp),
    )

    private fun productInput(hintValue: String) = EditText(this).apply {
        hint = hintValue
        textSize = 16f
        setTextColor(textPrimaryColor())
        setHintTextColor(textSecondaryColor())
        minHeight = dp(52)
        setSingleLine(false)
        maxLines = 2
        backgroundTintList = ColorStateList.valueOf(accentColor())
        setPadding(dp(4), dp(8), dp(4), dp(8))
    }

    private fun productButton(textValue: String, primary: Boolean = true, action: () -> Unit) = Button(this).apply {
        text = textValue
        isAllCaps = false
        textSize = 15f
        minHeight = dp(ProductSpacing.CONTROL_HEIGHT_DP)
        setTypeface(typeface, Typeface.BOLD)
        setTextColor(if (primary) Color.WHITE else textPrimaryColor())
        backgroundTintList = ColorStateList.valueOf(if (primary) accentColor() else surfaceMutedColor())
        setOnClickListener { action() }
    }

    private fun navigationButton(textValue: String, action: () -> Unit) = Button(this).apply {
        text = textValue
        isAllCaps = false
        textSize = 13f
        setTypeface(typeface, Typeface.BOLD)
        setTextColor(textPrimaryColor())
        backgroundTintList = ColorStateList.valueOf(surfaceMutedColor())
        setOnClickListener { action() }
    }

    private fun statusChip(textValue: String, positive: Boolean) = TextView(this).apply {
        text = textValue
        gravity = Gravity.CENTER
        textSize = 13f
        setTypeface(typeface, Typeface.BOLD)
        setTextColor(if (positive) successColor() else textSecondaryColor())
        background = roundedBackground(surfaceMutedColor(), dp(18).toFloat(), borderColor())
    }

    private fun accountChip(textValue: String) = productBody(textValue).apply {
        textSize = 13f
        setTextColor(textPrimaryColor())
        setTypeface(typeface, Typeface.BOLD)
        background = roundedBackground(surfaceMutedColor(), dp(14).toFloat(), borderColor())
        setPadding(dp(10), dp(5), dp(10), dp(5))
        maxLines = 1
    }

    private fun emptyCredentialVisual() = TextView(this).apply {
        text = "KEY"
        gravity = Gravity.CENTER
        textSize = 16f
        letterSpacing = 0.08f
        setTypeface(typeface, Typeface.BOLD)
        setTextColor(Color.WHITE)
        background = roundedBackground(accentColor(), dp(20).toFloat())
        contentDescription = "Digital credential placeholder"
    }

    private fun sectionHeader(label: String, description: String) = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        addView(productCaption(label), matchWrapParams())
        addView(productBody(description), matchWrapParams())
        setPadding(dp(2), 0, dp(2), dp(14))
    }

    private fun detailCard(title: String, values: List<Pair<String, String>>) = productCard().apply {
        addView(productCaption(title), matchWrapParams())
        values.forEach { (label, value) -> addView(valueRow(label, value), matchWrapParams()) }
    }

    private fun valueRow(label: String, value: String) = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(0, dp(6), 0, dp(8))
        addView(productCaption(label.uppercase()), matchWrapParams())
        addView(productBody(value).apply {
            setTextColor(textPrimaryColor())
            setTextIsSelectable(value.startsWith("0x"))
        }, matchWrapParams())
    }

    private fun diagnosticLabel(value: String) = TextView(this).apply {
        text = value
        textSize = 14f
        setTextColor(textPrimaryColor())
        setPadding(dp(8), dp(10), dp(8), dp(10))
    }

    private fun diagnosticButton(textValue: String, action: () -> Unit) =
        productButton(textValue, primary = false, action = action)

    private fun showLoggedOutShell() {
        restoringView.visibility = View.GONE
        loggedOutView.visibility = View.VISIBLE
        authenticatedView.visibility = View.GONE
        currentDestination = ProductDestination.MY_KEYS
        resetProductScroll()
    }

    private fun showRestoringShell() {
        restoringView.visibility = View.VISIBLE
        loggedOutView.visibility = View.GONE
        authenticatedView.visibility = View.GONE
        resetProductScroll()
    }

    private fun showAuthenticatedShell() {
        restoringView.visibility = View.GONE
        loggedOutView.visibility = View.GONE
        authenticatedView.visibility = View.VISIBLE
        showDestination(ProductDestination.MY_KEYS)
    }

    private fun showDestination(destination: ProductDestination) {
        if (destination == ProductDestination.ISSUER && !issuerCapabilityConfirmed) return
        currentDestination = destination
        navigationBar.visibility = if (destination == ProductDestination.DIAGNOSTICS) View.GONE else View.VISIBLE
        myKeysSection.visibility = if (destination == ProductDestination.MY_KEYS) View.VISIBLE else View.GONE
        issuerProductSection.visibility = if (destination == ProductDestination.ISSUER) View.VISIBLE else View.GONE
        settingsSection.visibility = if (destination == ProductDestination.SETTINGS) View.VISIBLE else View.GONE
        diagnosticsSection.visibility = if (destination == ProductDestination.DIAGNOSTICS) View.VISIBLE else View.GONE
        shellFeedbackText.visibility = View.GONE
        resetProductScroll()
        if (destination != ProductDestination.DIAGNOSTICS) {
            val selected = accentColor()
            val idle = surfaceMutedColor()
            myKeysNavButton.backgroundTintList = ColorStateList.valueOf(
                if (destination == ProductDestination.MY_KEYS) selected else idle,
            )
            issuerNavButton.backgroundTintList = ColorStateList.valueOf(
                if (destination == ProductDestination.ISSUER) selected else idle,
            )
            settingsNavButton.backgroundTintList = ColorStateList.valueOf(
                if (destination == ProductDestination.SETTINGS) selected else idle,
            )
            myKeysNavButton.setTextColor(if (destination == ProductDestination.MY_KEYS) Color.WHITE else textPrimaryColor())
            issuerNavButton.setTextColor(if (destination == ProductDestination.ISSUER) Color.WHITE else textPrimaryColor())
            settingsNavButton.setTextColor(if (destination == ProductDestination.SETTINGS) Color.WHITE else textPrimaryColor())
        }
        if (destination == ProductDestination.MY_KEYS) {
            val wallet = ethereumWallet
            if (wallet != null && System.currentTimeMillis() - lastCredentialRefreshAtMillis >= MY_KEYS_REFRESH_AGE_MILLIS) {
                refreshMyKeysAsync(wallet.address)
            }
        }
    }

    private fun resetProductScroll() {
        if (::productScrollView.isInitialized) {
            productScrollView.post { productScrollView.scrollTo(0, 0) }
        }
    }

    private fun showImportCredentialDialog() {
        val input = productInput("credential.keys.demo-access.eth").apply {
            setSingleLine(true)
            setText(importCredentialInput.text)
        }
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(4), dp(20), 0)
            addView(productBody("Enter the full ENS credential name. Ownership will be verified onchain."), matchWrapParams())
            addView(input, matchWrapParams())
        }
        AlertDialog.Builder(this)
            .setTitle("Add by ENS name")
            .setView(container)
            .setNegativeButton("Back", null)
            .setPositiveButton("Verify & import") { _, _ ->
                importCredentialInput.setText(input.text)
                importCredential()
            }
            .show()
    }

    private fun credentialCard(
        snapshot: CredentialSnapshot,
        mode: PassCardMode,
        selected: Boolean,
        onSelect: () -> Unit,
    ) = productCard().apply {
        val fullName = snapshot.fullName
        val artwork = snapshot.avatarUri.orEmpty()
        val access = if (snapshot.authoritativeAllowed) "Allowed" else "Not allowed"
        val label = fullName.substringBefore('.')
        val passType = if (label.startsWith("staff-")) "STAFF ACCESS" else "ACCESS PASS"
        elevation = dp(if (selected) 12 else 3).toFloat()
        setOnClickListener { onSelect() }
        val header = LinearLayout(this@MainActivity).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        val placeholder = TextView(this@MainActivity).apply {
            text = if (passType == "STAFF ACCESS") "SA" else "AP"
            gravity = Gravity.CENTER
            textSize = 20f
            setTextColor(Color.WHITE)
            setTypeface(typeface, Typeface.BOLD)
            background = roundedBackground(accentColor(), dp(16).toFloat())
            contentDescription = if (artwork.isBlank()) "Access pass artwork placeholder" else "Access pass artwork"
        }
        header.addView(placeholder, LinearLayout.LayoutParams(dp(66), dp(66)).apply { marginEnd = dp(14) })
        val identity = LinearLayout(this@MainActivity).apply { orientation = LinearLayout.VERTICAL }
        identity.addView(productCaption(passType), matchWrapParams())
        identity.addView(productHeading(fullName).apply {
            textSize = 17f
            maxLines = 2
        }, matchWrapParams())
        header.addView(identity, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        if (selected) {
            header.addView(statusChip("SELECTED", positive = true), LinearLayout.LayoutParams(dp(104), dp(34)))
        }
        addView(header, matchWrapParams())
        addView(statusChip(access.uppercase(), positive = access == "Allowed"), LinearLayout.LayoutParams(dp(116), dp(36)).apply {
            topMargin = dp(16)
            bottomMargin = dp(10)
        })
        addView(valueRow("Valid until", formatPassExpiry(snapshot.registryExpiry)), matchWrapParams())
        if (mode != PassCardMode.STACKED_SUMMARY) {
            addView(productBody(if (snapshot.transferable == false) "Non-transferable" else "Transferable").apply {
                setTextColor(textPrimaryColor())
                setTypeface(typeface, Typeface.BOLD)
                setPadding(0, dp(2), 0, dp(12))
            }, matchWrapParams())
            addView(valueRow("Description", snapshot.description.orEmpty().ifBlank { "Not set" }), matchWrapParams())
            addView(valueRow("Owner", snapshot.owner.orEmpty()), matchWrapParams())
            addView(valueRow("Issuer registry", ProductShellPolicy.compactAddress(snapshot.registry)), matchWrapParams())
            addView(valueRow("Provenance", if (snapshot.provenanceMatches) "Verified" else "Unavailable"), matchWrapParams())
            if (artwork.isNotBlank()) addView(productCaption("ARTWORK LINKED"), matchWrapParams())
        } else {
            addView(productBody("Tap to select and expand").apply { setPadding(0, dp(4), 0, 0) }, matchWrapParams())
        }
    }

    private fun formatPassExpiry(expiry: BigInteger?): String = expiry?.let {
        runCatching {
            PASS_DATE_FORMAT.format(Instant.ofEpochSecond(it.longValueExact()).atZone(ZoneId.systemDefault()))
        }.getOrNull()
    } ?: "Unavailable"

    private fun reviewPreviewCard(review: StaffReviewPresentation) = productCard().apply {
        val header = LinearLayout(this@MainActivity).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        header.addView(TextView(this@MainActivity).apply {
            text = "SA"
            gravity = Gravity.CENTER
            textSize = 18f
            setTextColor(Color.WHITE)
            setTypeface(typeface, Typeface.BOLD)
            background = roundedBackground(accentColor(), dp(14).toFloat())
            contentDescription = "Staff access artwork placeholder"
        }, LinearLayout.LayoutParams(dp(56), dp(56)).apply { marginEnd = dp(14) })
        header.addView(productCaption("STAFF ACCESS"), LinearLayout.LayoutParams(
            0,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            1f,
        ))
        header.addView(statusChip(review.access.uppercase(), positive = true), LinearLayout.LayoutParams(dp(110), dp(34)))
        addView(header, matchWrapParams())
        addView(productHeading(review.credential).apply {
            textSize = 16f
            maxLines = 2
            setPadding(0, dp(12), 0, 0)
        }, matchWrapParams())
    }

    private fun reviewRow(label: String, value: String) = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        minimumHeight = dp(48)
        addView(productCaption(label.uppercase()), LinearLayout.LayoutParams(dp(112), ViewGroup.LayoutParams.WRAP_CONTENT))
        addView(productBody(value).apply {
            setTextColor(textPrimaryColor())
            gravity = Gravity.END
            maxLines = 2
        }, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
    }

    private fun exactReviewField(label: String, value: String, monospace: Boolean = false) =
        LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, dp(5), 0, dp(7))
            addView(productCaption(label.uppercase()), matchWrapParams())
            addView(productBody(value).apply {
                setTextColor(textPrimaryColor())
                setTextIsSelectable(true)
                if (monospace) setTypeface(Typeface.MONOSPACE)
            }, matchWrapParams())
        }

    private fun exactReviewDetails(review: StaffReviewPresentation) = productCard().apply {
        addView(productCaption("EXACT TRANSACTION DETAILS"), matchWrapParams())
        addView(exactReviewField("Credential", review.credential), matchWrapParams())
        addView(exactReviewField("Recipient", review.exactRecipient, monospace = true), matchWrapParams())
        addView(exactReviewField("Issuing wallet", review.issuingWallet, monospace = true), matchWrapParams())
        addView(exactReviewField("Access", review.access), matchWrapParams())
        addView(exactReviewField("Expiry", review.exactExpiry), matchWrapParams())
        addView(exactReviewField("UTC", review.utcExpiry, monospace = true), matchWrapParams())
        addView(exactReviewField("Transferability", review.transferability), matchWrapParams())
        addView(exactReviewField("Network", review.network), matchWrapParams())
    }

    private fun showCredentialReview(
        reviewDraft: CredentialReviewDraft,
        recordsOnly: Boolean,
        configurationCalldata: String? = null,
    ) {
        val review = reviewDraft.presentation
        val panel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(20), dp(20), dp(16))
            background = roundedBackground(surfaceColor(), dp(24).toFloat(), borderColor())
            addView(productHeading(if (recordsOnly) "Configure credential" else "Review credential").apply {
                textSize = 23f
                setPadding(0, 0, 0, dp(14))
            }, matchWrapParams())
        }
        val details = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            if (recordsOnly) addView(reviewRow("Existing credential", review.credential), matchWrapParams())
            addView(reviewPreviewCard(review), cardParams())
            addView(reviewRow("Recipient", if (recordsOnly) review.exactRecipient else review.recipient), matchWrapParams())
            addView(reviewRow("Access", review.access), matchWrapParams())
            addView(reviewRow(
                "Valid until",
                if (recordsOnly) review.exactExpiry else review.expires.replace(", ", " · "),
            ), matchWrapParams())
            addView(reviewRow("Transfer", review.transferability), matchWrapParams())
            addView(reviewRow("Description", review.description), matchWrapParams())
            addView(reviewRow("Artwork", review.artwork), matchWrapParams())
            addView(reviewRow("Network", review.network), matchWrapParams())
            val exactDetails = exactReviewDetails(review).apply { visibility = View.GONE }
            lateinit var exactToggle: Button
            exactToggle = productButton("Show exact details", primary = false) {
                val expanding = exactDetails.visibility != View.VISIBLE
                exactDetails.visibility = if (expanding) View.VISIBLE else View.GONE
                exactToggle.text = if (expanding) "Hide exact details" else "Show exact details"
            }
            addView(exactToggle, actionParams())
            addView(exactDetails, cardParams())
            addView(productBody(
                if (recordsOnly) {
                    "The credential is already created. This does not register it again.\n\n" +
                        "Purpose: ${CredentialConfigurationPolicy.PURPOSE}"
                } else {
                    "Creation requires two Sepolia transactions:\n" +
                        "1. ${review.transactionPurposes[0]}\n" +
                        "2. ${review.transactionPurposes[1]}"
                },
            ).apply {
                setTextColor(textPrimaryColor())
                setPadding(dp(14), dp(12), dp(14), dp(12))
                background = roundedBackground(surfaceMutedColor(), dp(14).toFloat())
            }, cardParams())
        }
        panel.addView(ScrollView(this).apply { addView(details) }, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            0,
            1f,
        ))
        val dialog = Dialog(this)
        val actionPlan = ReviewDialogPolicy.actionPlan(recordsOnly)
        val actions = LinearLayout(this).apply {
            orientation = if (actionPlan.stackedFullWidth) LinearLayout.VERTICAL else LinearLayout.HORIZONTAL
        }
        actions.addView(productButton(actionPlan.primaryLabel) {
            dialog.dismiss()
            if (recordsOnly) {
                submitRecords(reviewDraft, checkNotNull(configurationCalldata))
            } else {
                submitRegister(reviewDraft)
            }
        }, fullWidthControlParams(actionPlan.minimumControlHeightDp))
        actions.addView(productButton("Back", primary = false) { dialog.dismiss() },
            fullWidthControlParams(actionPlan.minimumControlHeightDp).apply { topMargin = dp(6) })
        panel.addView(actions, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
        ).apply { topMargin = dp(10) })
        dialog.setContentView(panel)
        dialog.setCanceledOnTouchOutside(false)
        dialog.show()
        dialog.window?.apply {
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            setLayout(
                (resources.displayMetrics.widthPixels * 0.92f).toInt(),
                minOf((resources.displayMetrics.heightPixels * 0.86f).toInt(), dp(760)),
            )
        }
    }

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
                    restoreAuthenticatedUser(user, "Email authentication succeeded.")
                },
                onFailure = { showSafeFailure("Email authentication", it) },
            )
        }
    }

    private fun logout() {
        runAction("Logging out…") {
            gateBApplication.privy.logout()
            currentUser = null
            ethereumWallet = null
            authenticatedWallets = emptyList()
            lastOwnedCredentials = emptyList()
            credentialRefreshJob?.cancel()
            credentialRefreshJob = null
            contractRunner = null
            walletText.text = "Current wallet\nNot available"
            settingsIdentityText.text = "Wallet not ready"
            copyWalletButton.isEnabled = false
            issuerCapabilityConfirmed = false
            issuerCapabilityState = IssuerCapabilityState.UNAVAILABLE
            issuerNavButton.visibility = View.GONE
            walletsList.removeAllViews()
            capabilityStatusText.text = WalletCapabilityPresentation.issuerLabel(issuerCapabilityState)
            myKeysCards.removeAllViews()
            myKeysEmptyCard.visibility = View.VISIBLE
            myKeysActions.visibility = View.GONE
            myKeysText.text = "No credentials yet"
            showHceSignerStatus("LOGIN REQUIRED")
            clearSignature()
            showLoggedOutShell()
            showStatus("Logged out. Sign in with the other account to continue.")
        }
    }

    private fun createAnotherWallet() {
        val hasWallet = authenticatedWallets.isNotEmpty()
        runAction(if (hasWallet) "Creating another embedded Ethereum wallet…" else "Creating embedded Ethereum wallet…") {
            val user = currentUser ?: gateBApplication.privy.getUser()
            if (user == null) {
                showStatus("Log in before creating a wallet.")
                return@runAction
            }
            currentUser = user
            user.createEthereumWallet(allowAdditional = user.embeddedEthereumWallets.isNotEmpty()).fold(
                onSuccess = { created ->
                    authenticatedWallets = (user.embeddedEthereumWallets + created)
                        .distinctBy { it.address.lowercase() }
                    selectWallet(created, "Embedded Ethereum wallet created and selected.")
                },
                onFailure = { showSafeFailure("Wallet creation", it) },
            )
        }
    }

    private fun restoreAuthenticatedUser(user: PrivyUser, status: String) {
        currentUser = user
        authenticatedWallets = user.embeddedEthereumWallets.distinctBy { it.address.lowercase() }
        val selectedModel = ActiveWalletPolicy.select(
            authenticatedWallets.map(::walletModel),
            activeWalletStore.loadAddress(),
        )
        val selected = selectedModel?.let { model ->
            authenticatedWallets.single { it.address.equals(model.address, true) }
        }
        if (selected == null) {
            showAuthenticatedShell()
            shellIdentityText.text = "Account setup\nWallet not ready"
            settingsIdentityText.text = "Wallet not ready"
            renderWallets()
            showHceSignerStatus("CREATE WALLET FIRST")
            showStatus(status)
            return
        }
        selectWallet(selected, status)
    }

    private fun selectWallet(wallet: EmbeddedEthereumWallet, status: String) {
        val model = ActiveWalletPolicy.requireSelectable(authenticatedWallets.map(::walletModel), wallet.address)
        activeWalletStore.save(model)
        ethereumWallet = wallet
        credentialRefreshJob?.cancel()
        credentialRefreshJob = null
        issuerCapabilityConfirmed = false
        issuerCapabilityState = IssuerCapabilityState.UNAVAILABLE
        shellIdentityText.text = WalletCapabilityPresentation.identity(wallet.address)
        settingsIdentityText.text = WalletCapabilityPresentation.identity(wallet.address)
        walletText.text = "Current wallet\n${wallet.address}"
        capabilityStatusText.text = WalletCapabilityPresentation.issuerLabel(issuerCapabilityState)
        copyWalletButton.isEnabled = true
        lastOwnedCredentials = emptyList()
        lastCredentialRefreshAtMillis = 0L
        renderWallets()
        showHceSignerStatus("READY")
        clearSignature()
        resetMobileIssuerAdmission(wallet)
        contractRunner = ContractTransactionRunner(
            walletProvider = object : MobileIssuerWalletProvider {
                override suspend fun switchToSepolia() = wallet.provider.switchChain(EthereumChain.Sepolia)
                override suspend fun sendTransaction(transactionJson: String): String = wallet.provider.request(
                    EthereumRpcRequest.ethSendTransaction(transactionJson),
                ).getOrThrow().data
            },
            client = credentialRpcClient,
            engine = credentialTransactionEngine,
        )
        refreshProduct(wallet.address)
        showAuthenticatedShell()
        showStatus(status)
    }

    private fun walletModel(wallet: EmbeddedEthereumWallet) = ActiveWallet(
        address = wallet.address,
        providerIdentity = wallet.id,
        hdWalletIndex = wallet.hdWalletIndex,
    )

    private fun renderWallets() {
        walletsList.removeAllViews()
        authenticatedWallets.sortedWith(compareBy<EmbeddedEthereumWallet> { it.hdWalletIndex }.thenBy { it.address })
            .forEach { wallet ->
                val active = wallet.address.equals(ethereumWallet?.address, true)
                val row = LinearLayout(this).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.CENTER_VERTICAL
                    setPadding(0, dp(5), 0, dp(5))
                }
                row.addView(productBody(ProductShellPolicy.compactAddress(wallet.address)).apply {
                    setTextColor(textPrimaryColor())
                    setTypeface(Typeface.MONOSPACE)
                }, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
                if (active) {
                    row.addView(statusChip("ACTIVE", positive = true), LinearLayout.LayoutParams(dp(92), dp(34)))
                } else {
                    row.addView(productButton("Select", primary = false) {
                        selectWallet(wallet, "Active wallet changed. My Keys and capabilities refreshed.")
                    }, LinearLayout.LayoutParams(dp(104), dp(44)))
                }
                walletsList.addView(row, matchWrapParams())
            }
        createWalletButton.text = if (authenticatedWallets.isEmpty()) "Create wallet" else "Create another wallet"
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

    private fun sharedStringStore(key: String): LoadableStringStateStore {
        val preferences = getSharedPreferences(CREDENTIAL_PREFERENCES, MODE_PRIVATE)
        return object : LoadableStringStateStore {
            override fun load(): String? = preferences.getString(key, null)
            override fun save(value: String) {
                check(preferences.edit().putString(key, value).commit()) { "Credential state persistence failed" }
            }
        }
    }

    private fun refreshProduct(walletAddress: String) {
        if (credentialRefreshJob?.isActive == true) return
        credentialRefreshJob = activityScope.launch {
            val capability = credentialReader.issuerCapability(walletAddress)
            if (ethereumWallet?.address?.equals(walletAddress, true) != true) return@launch
            issuerCapabilityState = capability.state
            issuerCapabilityConfirmed = capability.allowed
            issuerNavButton.visibility = if (capability.allowed) View.VISIBLE else View.GONE
            shellIdentityText.text = WalletCapabilityPresentation.identity(walletAddress)
            settingsIdentityText.text = WalletCapabilityPresentation.identity(walletAddress)
            capabilityStatusText.text = WalletCapabilityPresentation.issuerLabel(capability.state)
            if (!capability.allowed && currentDestination == ProductDestination.ISSUER) {
                showDestination(ProductDestination.MY_KEYS)
            }
            if (capability.allowed) {
                productStatusText.visibility = View.GONE
                try {
                    recoverIssuance(walletAddress)
                } catch (error: Throwable) {
                    val stage = if (issuanceCoordinator.current(walletAddress)?.state ==
                        IssuanceState.AUTHORITATIVE_READBACK
                    ) "FINAL_READBACK" else "RECOVERY"
                    showSafeFailure("Verify credential", error, stage)
                }
            }
            refreshMyKeysInternal(walletAddress)
        }
    }

    private fun reviewCredential() {
        val wallet = ethereumWallet ?: return showStatus("Log in and prepare an Ethereum wallet first.")
        runAction("Checking issuer authority and credential availability…") {
            val existing = issuanceCoordinator.current(wallet.address)
            if (existing != null && existing.state !in setOf(IssuanceState.DRAFT, IssuanceState.REGISTER_READY)) {
                renderIssuance(existing)
                showStatus("An issuance is already in progress. Resume it instead of registering again.")
                return@runAction
            }
            val review = CredentialReviewPolicy.prepare(artworkInput.text.toString())
            val request = registerRequest(wallet.address)
            preflightRegister(request)
            showCredentialReview(review, recordsOnly = false)
        }
    }

    private fun submitRegister(review: CredentialReviewDraft) {
        val wallet = ethereumWallet ?: return
        val runner = contractRunner ?: return
        val request = registerRequest(wallet.address)
        runAction(
            "Rechecking credential before wallet approval…",
            actionName = "Create credential",
            stage = "TX1_SUBMISSION",
        ) {
            require(CredentialReviewPolicy.matchesCurrentArtwork(review, artworkInput.text.toString())) {
                "CREDENTIAL_CHANGED_REVIEW_AGAIN"
            }
            val existing = issuanceCoordinator.current(wallet.address)
            require(existing == null || existing.state in setOf(IssuanceState.DRAFT, IssuanceState.REGISTER_READY)) {
                "ISSUANCE_ALREADY_IN_PROGRESS"
            }
            runner.verifyNewSubmission(request) {
                require(CredentialReviewPolicy.matchesCurrentArtwork(review, artworkInput.text.toString())) {
                    "CREDENTIAL_CHANGED_REVIEW_AGAIN"
                }
                preflightRegister(request)
            }
            require(CredentialReviewPolicy.matchesCurrentArtwork(review, artworkInput.text.toString())) {
                "CREDENTIAL_CHANGED_REVIEW_AGAIN"
            }
            val fingerprint = CredentialAbi.calldataFingerprint(request.data)
            val operation = credentialTransactionEngine.latestForWallet(
                wallet.address, ContractTransactionRunner.REGISTER_OPERATION,
            )?.takeIf {
                it.state in setOf(
                    TransactionOperationState.DRAFT,
                    TransactionOperationState.READY_TO_REVIEW,
                    TransactionOperationState.READY_TO_SUBMIT,
                ) && it.targetAddress.equals(request.to, true) && it.dataSummary == fingerprint
            } ?: runner.create(request)
            val session = issuanceCoordinator.beginRegister(wallet.address, review.avatarUri, operation.operationId)
            runner.review(operation.operationId)
            renderIssuance(session)
            val result = runner.submit(
                operationId = operation.operationId,
                request = request,
                preflight = {
                    require(CredentialReviewPolicy.matchesCurrentArtwork(review, artworkInput.text.toString())) {
                        "CREDENTIAL_CHANGED_REVIEW_AGAIN"
                    }
                    preflightRegister(request)
                },
                onChanged = { operation ->
                    if (operation.state in setOf(
                            TransactionOperationState.SUBMITTING_NO_HASH,
                            TransactionOperationState.HASH_RECEIVED,
                            TransactionOperationState.CONFIRMING,
                            TransactionOperationState.ONCHAIN_READBACK,
                        )
                    ) {
                        issuanceCoordinator.registerSubmitted()
                    }
                    issuanceCoordinator.current(wallet.address)?.let(::renderIssuance)
                },
            )
            if (result.status == MobileIssuerStatus.CONFIRMED) {
                try {
                    renderIssuance(issuanceCoordinator.registerConfirmed())
                    showStatus("Registration confirmed. Review and resume setup to configure records.")
                } catch (error: Throwable) {
                    throw StagedActionException("POST_TX1_FINALIZATION", error)
                }
            } else {
                renderIssuance(issuanceCoordinator.current(wallet.address)!!)
                showStatus("We could not confirm registration. The existing attempt will be recovered before any retry.")
            }
        }
    }

    private fun reviewRecords() {
        val wallet = ethereumWallet ?: return
        runAction(
            "Checking registered credential before record setup…",
            actionName = "Resume setup",
            stage = "TX2_REVIEW_PREFLIGHT",
        ) {
            val session = issuanceCoordinator.current(wallet.address) ?: error("ISSUANCE_SESSION_REQUIRED")
            val configuration = CredentialConfigurationPolicy.prepare(session)
            val request = recordsRequest(wallet.address, configuration.calldata)
            preflightRecords(request, session)
            showCredentialReview(
                configuration.review,
                recordsOnly = true,
                configurationCalldata = configuration.calldata,
            )
        }
    }

    private fun resumeIssuance() {
        val wallet = ethereumWallet ?: return
        if (issuanceCoordinator.current(wallet.address)?.state == IssuanceState.AUTHORITATIVE_READBACK) {
            runAction(
                "Retrying confirmed credential verification…",
                actionName = "Verify credential",
                stage = "FINAL_READBACK",
            ) { authoritativeReadback() }
        } else {
            reviewRecords()
        }
    }

    private fun submitRecords(review: CredentialReviewDraft, reviewedCalldata: String) {
        val wallet = ethereumWallet ?: return
        val runner = contractRunner ?: return
        runAction(
            "Rechecking access setup before wallet approval…",
            actionName = "Configure credential",
            stage = "TX2_SUBMISSION",
        ) {
            var session = issuanceCoordinator.current(wallet.address) ?: error("ISSUANCE_SESSION_REQUIRED")
            val configuration = CredentialConfigurationPolicy.prepare(session)
            require(review.avatarUri == session.avatarUri) { "CREDENTIAL_CHANGED_REVIEW_AGAIN" }
            require(reviewedCalldata == configuration.calldata) { "CREDENTIAL_CHANGED_REVIEW_AGAIN" }
            val request = recordsRequest(wallet.address, configuration.calldata)
            runner.verifyNewSubmission(request) { preflightRecords(request, session) }
            val fingerprint = CredentialAbi.calldataFingerprint(request.data)
            val operation = credentialTransactionEngine.latestForWallet(
                wallet.address, ContractTransactionRunner.RECORDS_OPERATION,
            )?.takeIf {
                it.state in setOf(
                    TransactionOperationState.DRAFT,
                    TransactionOperationState.READY_TO_REVIEW,
                    TransactionOperationState.READY_TO_SUBMIT,
                ) && it.targetAddress.equals(request.to, true) && it.dataSummary == fingerprint
            } ?: runner.create(request)
            if (session.state == IssuanceState.REGISTERED_CONFIGURING) {
                session = issuanceCoordinator.recordsReady(operation.operationId)
            }
            runner.review(operation.operationId)
            renderIssuance(session)
            val result = runner.submit(
                operation.operationId,
                request,
                preflight = { preflightRecords(request, session) },
                onChanged = { operation ->
                    if (operation.state in setOf(
                            TransactionOperationState.SUBMITTING_NO_HASH,
                            TransactionOperationState.HASH_RECEIVED,
                            TransactionOperationState.CONFIRMING,
                            TransactionOperationState.ONCHAIN_READBACK,
                        )
                    ) {
                        issuanceCoordinator.recordsSubmitted()
                    }
                    issuanceCoordinator.current(wallet.address)?.let(::renderIssuance)
                },
            )
            when {
                result.status == MobileIssuerStatus.CONFIRMED -> {
                    issuanceCoordinator.recordsConfirmed()
                    authoritativeReadback()
                }
                credentialTransactionEngine.find(operation.operationId)?.state == TransactionOperationState.REVERTED -> {
                    renderIssuance(issuanceCoordinator.recordsFailed())
                    showStatus("Record transaction failed. Registration is preserved; Resume setup retries records only.")
                }
                else -> {
                    renderIssuance(issuanceCoordinator.current(wallet.address)!!)
                    showStatus("We could not confirm setup. The existing attempt will be recovered before any retry.")
                }
            }
        }
    }

    private suspend fun preflightRegister(request: ContractTransactionRequest) {
        val capability = credentialReader.issuerCapability(request.from)
        require(capability.allowed) { "ISSUER_AUTHORITY_${capability.category}" }
        val snapshot = credentialReader.read(IssuerSpace.fullName)
        CredentialProductPolicy.requireAvailable(snapshot)
        CredentialValidation.validateExpiry(
            BigInteger.valueOf(IssuerSpace.STAFF_EXPIRY),
            snapshot.snapshotTimestamp ?: error("BLOCK_TIME_MISSING"),
        )
        require(BigInteger.valueOf(IssuerSpace.STAFF_EXPIRY) < (capability.namespaceExpiry
            ?: error("NAMESPACE_EXPIRY_MISSING"))) { "EXPIRY_OUTSIDE_NAMESPACE" }
        credentialReader.simulate(request.from, request.to, request.data)
    }

    private suspend fun preflightRecords(request: ContractTransactionRequest, session: IssuanceSession) {
        require(request.from.equals(IssuerSpace.issuer, true)) { "WRONG_ISSUER" }
        require(request.to.equals(IssuerSpace.resolver, true)) { "WRONG_RESOLVER" }
        require(request.data == CredentialConfigurationPolicy.calldata(session)) { "TRANSACTION_DATA_MISMATCH" }
        require(credentialRpcClient.chainId() == BigInteger.valueOf(IssuerSpace.chainId)) { "WRONG_CHAIN" }
        val latest = credentialRpcClient.transactionCount(request.from, "latest")
        val pending = credentialRpcClient.transactionCount(request.from, "pending")
        require(latest == BigInteger.TWO && pending == BigInteger.TWO) { "UNEXPECTED_ISSUER_NONCE" }
        val capability = credentialReader.issuerCapability(request.from)
        require(capability.allowed) { "ISSUER_AUTHORITY_${capability.category}" }
        val registerOperation = session.registerOperationId?.let(credentialTransactionEngine::find)
        require(registerOperation?.state == TransactionOperationState.CONFIRMED) { "REGISTER_RECEIPT_NOT_CONFIRMED" }
        require(registerOperation.postLatestNonce == "2" && registerOperation.postPendingNonce == "2") {
            "REGISTER_NONCE_EVIDENCE_MISMATCH"
        }
        val snapshot = credentialReader.read(session.fullName)
        require(snapshot.readStatus == CredentialReadStatus.FRESH) { "CREDENTIAL_STATE_UNKNOWN" }
        require(snapshot.status == CredentialRegistryStatus.REGISTERED) { "CREDENTIAL_NOT_REGISTERED" }
        require(snapshot.owner.equals(session.holder, true)) { "WRONG_OWNER" }
        require(snapshot.resolver.equals(IssuerSpace.resolver, true)) { "WRONG_RESOLVER" }
        require(snapshot.subregistry.equals(IssuerSpace.ZERO_ADDRESS, true)) { "WRONG_SUBREGISTRY" }
        require(snapshot.registryExpiry == session.expiry) { "WRONG_REGISTRY_EXPIRY" }
        require(snapshot.ownerRoleBitmap == BigInteger.ZERO && snapshot.transferable == false) {
            "CREDENTIAL_TRANSFERABLE"
        }
        require(snapshot.description.orEmpty().isEmpty()) { "DESCRIPTION_ALREADY_SET" }
        require(snapshot.avatarUri.orEmpty().isEmpty()) { "AVATAR_ALREADY_SET" }
        require(snapshot.accessActive == null && snapshot.accessValidUntil == null) { "ACCESS_ALREADY_SET" }
        require(snapshot.provenanceMatches) { "WRONG_PROVENANCE" }
        credentialReader.simulate(request.from, request.to, request.data)
    }

    private fun registerRequest(wallet: String) = ContractTransactionRequest(
        ContractTransactionRunner.REGISTER_OPERATION,
        wallet,
        IssuerSpace.registry,
        CredentialAbi.register(
            IssuerSpace.STAFF_LABEL,
            IssuerSpace.STAFF_HOLDER,
            IssuerSpace.resolver,
            BigInteger.valueOf(IssuerSpace.STAFF_EXPIRY),
        ),
    )

    private fun recordsRequest(wallet: String, calldata: String) = ContractTransactionRequest(
        ContractTransactionRunner.RECORDS_OPERATION,
        wallet,
        IssuerSpace.resolver,
        calldata,
    )

    private suspend fun authoritativeReadback() {
        val wallet = ethereumWallet ?: return
        val session = issuanceCoordinator.current(wallet.address) ?: return
        renderIssuance(issuanceCoordinator.beginReadback())
        val recordsOperation = session.recordsOperationId?.let(credentialTransactionEngine::find)
        val minimumBlock = CredentialFinalReadbackPolicy.confirmedReceiptBlock(session, recordsOperation)
        credentialFinalReadback.reconcile(
            expected = CredentialExpectation(
                session.fullName,
                session.holder,
                IssuerSpace.resolver,
                session.expiry,
                session.description,
                session.avatarUri,
            ),
            minimumBlock = minimumBlock,
        )
        val ready = issuanceCoordinator.ready()
        renderIssuance(ready)
        showStatus(ProductShellPolicy.credentialVerificationMessage(ready.state))
    }

    private suspend fun recoverIssuance(wallet: String) {
        val session = issuanceCoordinator.current(wallet) ?: return
        val register = session.registerOperationId?.let(credentialTransactionEngine::find)
        val records = session.recordsOperationId?.let(credentialTransactionEngine::find)
        when (issuanceCoordinator.recoveryAction(session, register, records)) {
            IssuanceRecoveryAction.RECOVER_REGISTER -> {
                renderIssuance(session)
                val result = checkNotNull(contractRunner).recover(
                    register!!.operationId, registerRequest(wallet),
                ) {
                    issuanceCoordinator.current(wallet)?.let(::renderIssuance)
                }
                if (result.status == MobileIssuerStatus.CONFIRMED) {
                    renderIssuance(issuanceCoordinator.registerConfirmed())
                }
            }
            IssuanceRecoveryAction.RECOVER_RECORDS -> {
                renderIssuance(session)
                val result = checkNotNull(contractRunner).recover(
                    records!!.operationId,
                    recordsRequest(wallet, CredentialConfigurationPolicy.calldata(session)),
                ) {
                    issuanceCoordinator.current(wallet)?.let(::renderIssuance)
                }
                if (result.status == MobileIssuerStatus.CONFIRMED) {
                    issuanceCoordinator.recordsConfirmed()
                    authoritativeReadback()
                }
            }
            IssuanceRecoveryAction.READBACK -> {
                if (session.state == IssuanceState.RECORDS_SUBMITTED &&
                    records?.state == TransactionOperationState.CONFIRMED
                ) issuanceCoordinator.recordsConfirmed()
                authoritativeReadback()
            }
            IssuanceRecoveryAction.RESUME_RECORDS -> {
                val resumed = if (session.state in setOf(IssuanceState.REGISTER_SUBMITTED, IssuanceState.REGISTER_CONFIRMED) &&
                    register?.state == TransactionOperationState.CONFIRMED
                ) issuanceCoordinator.registerConfirmed() else session
                renderIssuance(resumed)
            }
            else -> renderIssuance(session)
        }
    }

    private fun renderIssuance(session: IssuanceSession) {
        createReviewButton.visibility = if (ProductShellPolicy.createCredentialVisible(session.state)) {
            View.VISIBLE
        } else {
            View.GONE
        }
        val retryVerification = ProductShellPolicy.retryVerificationVisible(session.state)
        resumeSetupButton.text = if (retryVerification) "Retry verification" else "Resume setup"
        resumeSetupButton.visibility = if (ProductShellPolicy.resumeSetupVisible(session.state) || retryVerification) {
            View.VISIBLE
        } else {
            View.GONE
        }
        copyCredentialButton.visibility = if (session.state == IssuanceState.READY) View.VISIBLE else View.GONE
        val register = session.registerOperationId?.let(credentialTransactionEngine::find)
        val records = session.recordsOperationId?.let(credentialTransactionEngine::find)
        val progress = ProductShellPolicy.issuanceProgress(session, register, records)
        productStatusText.visibility = if (progress == null) View.GONE else View.VISIBLE
        productStatusText.text = progress?.visibleText().orEmpty()
    }

    private fun importCredential() {
        val wallet = ethereumWallet ?: return showStatus("Log in before importing a credential.")
        runAction("Verifying credential ownership onchain…") {
            val fullName = CredentialValidation.normalizeFullName(importCredentialInput.text.toString())
            val snapshot = credentialReader.read(fullName)
            require(CredentialProductPolicy.ownedBy(snapshot, wallet.address)) {
                "CREDENTIAL_NOT_OWNED_BY_THIS_WALLET"
            }
            credentialIndex.add(wallet.address, CredentialReference(IssuerSpace.chainId, fullName))
            importCredentialInput.text.clear()
            refreshMyKeysInternal(wallet.address)
            showStatus("Credential imported after authoritative ownership verification.")
        }
    }

    private fun refreshMyKeys() {
        val wallet = ethereumWallet ?: return showStatus("Log in before refreshing My Keys.")
        runAction("Refreshing My Keys from Sepolia…") { refreshMyKeysInternal(wallet.address) }
    }

    private fun refreshMyKeysAsync(wallet: String) {
        if (credentialRefreshJob?.isActive == true) return
        credentialRefreshJob = activityScope.launch {
            refreshMyKeysInternal(wallet)
        }
    }

    private suspend fun refreshMyKeysInternal(wallet: String) {
        if (ethereumWallet?.address?.equals(wallet, true) != true) return
        val result = credentialDiscovery.discover(wallet, credentialIndex.list(wallet))
        if (ethereumWallet?.address?.equals(wallet, true) != true) return
        lastCredentialRefreshAtMillis = System.currentTimeMillis()
        myKeysCards.removeAllViews()
        when (result) {
            is CredentialDiscoveryResult.Unavailable -> {
                lastOwnedCredentials = emptyList()
                myKeysEmptyCard.visibility = View.VISIBLE
                myKeysActions.visibility = View.VISIBLE
                myKeysText.text = "Pass discovery unavailable\nPull to retry safely"
                showStatus("My Keys is unavailable. No cached ownership was treated as authoritative.")
            }
            is CredentialDiscoveryResult.Available -> {
                lastOwnedCredentials = result.credentials
                Log.i(
                    DISCOVERY_LOG_TAG,
                    "R1_DISCOVERY active=${ProductShellPolicy.compactAddress(wallet)} " +
                        "automaticCandidates=${result.candidateCount} owned=${result.credentials.size} " +
                        "scannedTo=${result.scannedToBlock}",
                )
                val selected = selectedPassStore.validate(IssuerSpace.chainId, wallet, result.credentials)
                renderCredentialStack(wallet, result.credentials, selected)
            }
        }
    }

    private fun renderCredentialStack(
        wallet: String,
        credentials: List<CredentialSnapshot>,
        selectedName: String?,
    ) {
        myKeysCards.removeAllViews()
        if (credentials.isEmpty()) {
            myKeysEmptyCard.visibility = View.VISIBLE
            myKeysActions.visibility = View.VISIBLE
            myKeysText.text = "No passes found for this wallet"
            return
        }
        myKeysEmptyCard.visibility = View.GONE
        myKeysActions.visibility = View.VISIBLE
        val ordered = credentials.sortedWith(
            compareBy<CredentialSnapshot> { it.fullName == selectedName }.thenBy { it.fullName },
        )
        ordered.forEachIndexed { index, snapshot ->
            val selected = snapshot.fullName == selectedName
            val mode = PassStackPolicy.mode(ordered.size, selected)
            myKeysCards.addView(
                credentialCard(snapshot, mode, selected) {
                    selectedPassStore.select(IssuerSpace.chainId, wallet, snapshot)
                    renderCredentialStack(wallet, lastOwnedCredentials, snapshot.fullName)
                    showStatus("Pass selected for future presentation. NFC behavior is unchanged.")
                },
                cardParams().apply { topMargin = dp(PassStackPolicy.overlapDp(ordered.size, index)) },
            )
        }
    }

    private fun copyCredentialName() = copyPublicProof("Credential name", IssuerSpace.fullName)

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

    private fun runAction(
        progress: String,
        actionName: String = "Operation",
        stage: String = "ACTION",
        action: suspend () -> Unit,
    ) {
        setBusy(true)
        showStatus(progress)
        activityScope.launch {
            try {
                action()
            } catch (error: Throwable) {
                val staged = error as? StagedActionException
                showSafeFailure(actionName, staged?.original ?: error, staged?.safeStage ?: stage)
            } finally {
                setBusy(false)
            }
        }
    }

    private fun setBusy(isBusy: Boolean) {
        operationButtons.forEach { it.isEnabled = !isBusy }
        if (::createReviewButton.isInitialized) createReviewButton.isEnabled = !isBusy
        if (::resumeSetupButton.isInitialized) resumeSetupButton.isEnabled = !isBusy
        if (::copyCredentialButton.isInitialized) copyCredentialButton.isEnabled = !isBusy
        if (::artworkInput.isInitialized) artworkInput.isEnabled = !isBusy
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

    private fun showSafeFailure(operation: String, error: Throwable, stage: String = "ACTION") {
        val failure = SafeActionFailurePolicy.from(operation, stage, error)
        lastActionFailure = failure
        actionFailureText.text = failure.diagnosticText()
        showStatus(failure.humanMessage)
    }

    private fun safeErrorClass(error: Throwable): String =
        error::class.simpleName?.take(80) ?: "Error"

    private class StagedActionException(
        val safeStage: String,
        val original: Throwable,
    ) : RuntimeException(null, original)

    private fun showStatus(status: String) {
        statusText.text = "STATUS:\n$status"
        when {
            loggedOutView.visibility == View.VISIBLE -> loginFeedbackText.text = status
            currentDestination != ProductDestination.DIAGNOSTICS -> {
                shellFeedbackText.text = status
                shellFeedbackText.visibility = View.VISIBLE
                val persistent = listOf("could not", "failed", "missing", "unavailable")
                    .any { status.contains(it, ignoreCase = true) }
                if (!persistent) {
                    shellFeedbackText.postDelayed({
                        if (shellFeedbackText.text.toString() == status &&
                            currentDestination != ProductDestination.DIAGNOSTICS
                        ) {
                            shellFeedbackText.visibility = View.GONE
                        }
                    }, 3_500L)
                }
            }
        }
    }

    private companion object {
        const val M1_PREFERENCES = "mobile_issuer_admission"
        const val M1_ATTEMPTED = "submission_attempted"
        const val M1_LEGACY_MIGRATED = "legacy_submission_attempt_migrated"
        const val M1_JOURNAL = "transaction_journal_v1"
        const val M1_RECOVERY_ISSUER = "0xFa90e8301A22833B74378C5fA3a7c120Ac512685"
        const val M1_LOG_TAG = "M1Admission"
        const val M1_READINESS_TIMEOUT_MILLIS = 30_000L
        const val CREDENTIAL_PREFERENCES = "credential_product"
        const val CREDENTIAL_TRANSACTION_JOURNAL = "transaction_journal_v1"
        const val CREDENTIAL_ISSUANCE_JOURNAL = "staff_issuance_v1"
        const val CREDENTIAL_LOCAL_INDEX = "local_index_v1"
        const val ACTIVE_WALLET = "active_wallet_v1"
        const val SELECTED_PASS = "selected_pass_v1"
        const val MY_KEYS_REFRESH_AGE_MILLIS = 60_000L
        const val FOREGROUND_REFRESH_AGE_MILLIS = 120_000L
        const val DISCOVERY_LOG_TAG = "R1Discovery"
        val PASS_DATE_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMM uuuu, HH:mm")
        val SIGNATURE_PATTERN = Regex("^0x[0-9a-fA-F]{130}$")
    }
}
