package io.github.nodexbit.ethonline2026

import android.app.Application
import io.privy.logging.PrivyLogLevel
import io.privy.sdk.Privy
import io.privy.sdk.PrivyConfig

class GateBApplication : Application() {
    val isPrivyConfigured: Boolean
        get() = BuildConfig.PRIVY_APP_ID.isNotBlank() &&
            BuildConfig.PRIVY_APP_CLIENT_ID.isNotBlank()

    lateinit var privy: Privy
        private set

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
    }
}
