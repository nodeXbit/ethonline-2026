package io.github.nodexbit.ethonline2026.hce

import io.github.nodexbit.ethonline2026.*
import java.math.BigInteger

data class HceIdentity(val wallet: StudioWalletBinding, val credential: String, val chainId: Long,
    val proofProvider: ProofProvider)

/** A persisted reference alone is never enough: publication requires current validated ownership. */
class NfcSelectionState(private val now: () -> Long = System::currentTimeMillis,
    private val activeWallet: () -> StudioWalletBinding? = { StudioRuntime.writes.currentWallet() }) {
    private var identity: HceIdentity? = null
    private var snapshot: CredentialSnapshot? = null
    private var validatedAt = 0L

    @Synchronized fun clear() { identity = null; snapshot = null }
    @Synchronized fun publish(binding: StudioWalletBinding, selected: String?, owned: List<CredentialSnapshot>, provider: ProofProvider) {
        clear()
        if (activeWallet() != binding || selected == null) return
        val pass = owned.singleOrNull { it.fullName == selected } ?: return
        if (!CredentialProductPolicy.ownedBy(pass, binding.address) ||
            selected.toByteArray(Charsets.UTF_8).size !in 1..64 ||
            runCatching { CredentialValidation.normalizeFullName(selected) }.getOrNull() != selected ||
            pass.node != CredentialAbi.nodeHex(selected)) return
        identity = HceIdentity(binding, selected, IssuerSpace.chainId, provider)
        snapshot = pass
        validatedAt = now()
    }
    @Synchronized fun current(): HceIdentity? {
        val value = identity ?: return null
        val pass = snapshot ?: return null
        val seconds = BigInteger.valueOf(now() / 1000)
        if (activeWallet() != value.wallet || now() - validatedAt !in 0..60_000 ||
            pass.snapshotTimestamp == null || seconds - pass.snapshotTimestamp > BigInteger.valueOf(60) ||
            pass.snapshotTimestamp > seconds + BigInteger.valueOf(15) || pass.registryExpiry == null ||
            pass.registryExpiry <= seconds || !CredentialProductPolicy.ownedBy(pass, value.wallet.address)) return null
        return value
    }
    fun readyText(serviceReady: Boolean): String = if (serviceReady && current() != null) "Ready to tap"
        else "Select a pass to use NFC"
}
