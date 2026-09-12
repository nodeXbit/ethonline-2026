package io.github.nodexbit.ethonline2026

enum class SessionHydrationState { RESTORING_SESSION, AUTHENTICATED, UNAUTHENTICATED }

data class SessionSurface(
    val restoringVisible: Boolean,
    val authenticatedVisible: Boolean,
    val loginVisible: Boolean,
)

object SessionHydrationPolicy {
    fun surface(state: SessionHydrationState): SessionSurface = SessionSurface(
        restoringVisible = state == SessionHydrationState.RESTORING_SESSION,
        authenticatedVisible = state == SessionHydrationState.AUTHENTICATED,
        loginVisible = state == SessionHydrationState.UNAUTHENTICATED,
    )
}

data class ActiveWallet(
    val address: String,
    val providerIdentity: String?,
    val hdWalletIndex: Int,
)

class ActiveWalletStore(private val store: LoadableStringStateStore) {
    fun loadAddress(): String? = store.load()?.takeIf(ADDRESS::matches)?.lowercase()

    fun save(wallet: ActiveWallet) {
        require(ADDRESS.matches(wallet.address)) { "INVALID_ACTIVE_WALLET" }
        store.save(wallet.address.lowercase())
    }

    private companion object {
        val ADDRESS = Regex("^0x[0-9a-fA-F]{40}$")
    }
}

object ActiveWalletPolicy {
    fun select(wallets: List<ActiveWallet>, persistedAddress: String?): ActiveWallet? {
        if (wallets.isEmpty()) return null
        require(wallets.map { it.address.lowercase() }.distinct().size == wallets.size) {
            "DUPLICATE_EMBEDDED_WALLET"
        }
        return wallets.firstOrNull { it.address.equals(persistedAddress, ignoreCase = true) }
            ?: wallets.minWithOrNull(compareBy<ActiveWallet> { it.hdWalletIndex }.thenBy { it.address.lowercase() })
    }

    fun requireSelectable(wallets: List<ActiveWallet>, address: String): ActiveWallet =
        wallets.singleOrNull { it.address.equals(address, ignoreCase = true) }
            ?: throw IllegalArgumentException("WALLET_NOT_IN_AUTHENTICATED_USER")
}

enum class IssuerCapabilityState { ALLOWED, DENIED, UNAVAILABLE }

object WalletCapabilityPresentation {
    fun identity(address: String): String = "Active wallet · ${ProductShellPolicy.compactAddress(address)}"

    fun issuerLabel(state: IssuerCapabilityState): String = when (state) {
        IssuerCapabilityState.ALLOWED -> "Can issue passes"
        IssuerCapabilityState.DENIED -> "Cannot issue passes"
        IssuerCapabilityState.UNAVAILABLE -> "Issuer capability unavailable · Retry"
    }
}

data class SelectedPassKey(val chainId: Long, val wallet: String)

class SelectedPassStore(private val store: LoadableStringStateStore) {
    private val selections = decode(store.load()).toMutableMap()

    @Synchronized
    fun selected(chainId: Long, wallet: String): String? = selections[key(chainId, wallet)]

    @Synchronized
    fun select(chainId: Long, wallet: String, snapshot: CredentialSnapshot) {
        require(chainId == IssuerSpace.chainId) { "WRONG_CHAIN" }
        require(CredentialProductPolicy.ownedBy(snapshot, wallet)) { "PASS_NOT_FRESHLY_OWNED" }
        selections[key(chainId, wallet)] = CredentialValidation.normalizeFullName(snapshot.fullName)
        persist()
    }

    @Synchronized
    fun validate(chainId: Long, wallet: String, owned: List<CredentialSnapshot>): String? {
        val storageKey = key(chainId, wallet)
        val selected = selections[storageKey] ?: return null
        if (owned.none { it.fullName == selected && CredentialProductPolicy.ownedBy(it, wallet) }) {
            selections.remove(storageKey)
            persist()
            return null
        }
        return selected
    }

    private fun key(chainId: Long, wallet: String) = SelectedPassKey(chainId, wallet.lowercase())

    private fun persist() = store.save(selections.entries.joinToString("\n") { (key, name) ->
        "${key.chainId}|${key.wallet}|$name"
    })

    private fun decode(raw: String?): Map<SelectedPassKey, String> = raw.orEmpty().lineSequence()
        .mapNotNull { line ->
            val fields = line.split('|')
            val chainId = fields.getOrNull(0)?.toLongOrNull() ?: return@mapNotNull null
            val wallet = fields.getOrNull(1)?.takeIf { ACTIVE_ADDRESS.matches(it) } ?: return@mapNotNull null
            val name = runCatching { CredentialValidation.normalizeFullName(fields.getOrNull(2).orEmpty()) }
                .getOrNull() ?: return@mapNotNull null
            key(chainId, wallet) to name
        }
        .toMap()

    private companion object {
        val ACTIVE_ADDRESS = Regex("^0x[0-9a-f]{40}$")
    }
}

enum class PassCardMode { SINGLE_EXPANDED, STACKED_SUMMARY, STACKED_SELECTED }

object PassStackPolicy {
    fun mode(total: Int, selected: Boolean): PassCardMode = when {
        total <= 1 -> PassCardMode.SINGLE_EXPANDED
        selected -> PassCardMode.STACKED_SELECTED
        else -> PassCardMode.STACKED_SUMMARY
    }

    fun overlapDp(total: Int, position: Int): Int = if (total > 1 && position > 0) -38 else 0
}
