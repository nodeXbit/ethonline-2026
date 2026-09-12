package io.github.nodexbit.ethonline2026

import android.app.Application
import io.privy.logging.PrivyLogLevel
import io.privy.sdk.Privy
import io.privy.sdk.PrivyConfig
import io.github.nodexbit.ethonline2026.hce.CoroutineAsyncRunner
import io.github.nodexbit.ethonline2026.hce.HceApduProcessor
import io.github.nodexbit.ethonline2026.hce.PrivyProofProvider
import io.github.nodexbit.ethonline2026.hce.PrivyTypedDataSignerSource
import io.github.nodexbit.ethonline2026.hce.TypedDataSignerSource
import io.github.nodexbit.ethonline2026.hce.PrivyTypedDataSigner
import io.github.nodexbit.ethonline2026.hce.NfcSelectionState
import io.github.nodexbit.ethonline2026.hce.HceIdentity
import io.github.nodexbit.ethonline2026.hce.PendingProofProvider
import io.privy.wallet.ethereum.EmbeddedEthereumWallet
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

class GateBApplication : Application() {
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    val isPrivyConfigured: Boolean
        get() = BuildConfig.PRIVY_APP_ID.isNotBlank() &&
            BuildConfig.PRIVY_APP_CLIENT_ID.isNotBlank()

    lateinit var privy: Privy
        private set

    lateinit var hceProcessor: HceApduProcessor
        private set
    val nfcSelection = NfcSelectionState()
    val dynamicHceProcessor = HceApduProcessor(PendingProofProvider, nfcSelection::current)
    var legacyNfcEnabled = false
        set(value) { dynamicHceProcessor.reset(); if (::hceProcessor.isInitialized) hceProcessor.reset(); field = value }
    val physicalHceProcessor: HceApduProcessor get() = if (legacyNfcEnabled) hceProcessor else dynamicHceProcessor

    fun publishNfc(wallet: EmbeddedEthereumWallet, selected: String?, owned: List<CredentialSnapshot>) {
        val binding = StudioRuntime.writes.currentWallet() ?: return nfcSelection.clear()
        if (!binding.address.equals(wallet.address, true)) return nfcSelection.clear()
        var published: HceIdentity? = null
        val provider = PrivyProofProvider(CoroutineAsyncRunner(applicationScope), TypedDataSignerSource {
            if (published == null || nfcSelection.current() !== published) null
            else PrivyTypedDataSigner(wallet)
        })
        nfcSelection.publish(binding, selected, owned, provider)
        published = nfcSelection.current()
    }

    override fun onCreate() {
        super.onCreate()
        if (isPrivyConfigured) {
            // Application.onCreate runs on the main thread. Keep exactly one SDK instance.
            privy = Privy.init(
                context = applicationContext,
                config = PrivyConfig(
                    appId = BuildConfig.PRIVY_APP_ID,
                    appClientId = BuildConfig.PRIVY_APP_CLIENT_ID,
                    logLevel = PrivyLogLevel.NONE,
                ),
            )
        }
        val signerSource = if (isPrivyConfigured) {
            PrivyTypedDataSignerSource(privy)
        } else {
            TypedDataSignerSource { null }
        }
        hceProcessor = HceApduProcessor(
            PrivyProofProvider(CoroutineAsyncRunner(applicationScope), signerSource),
        )
    }
}
