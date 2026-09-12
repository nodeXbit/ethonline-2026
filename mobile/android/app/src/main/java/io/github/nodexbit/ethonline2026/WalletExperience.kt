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
        IssuerCapabilityState.UNAVAILABLE -> "Studio capabilities unavailable"
    }
}

data class WalletPickerItem(val address: String, val active: Boolean)

data class WalletPickerPresentation(
    val items: List<WalletPickerItem>,
    val createWalletActionVisible: Boolean,
)

object WalletPickerPolicy {
    fun presentation(wallets: List<ActiveWallet>, activeAddress: String?): WalletPickerPresentation =
        WalletPickerPresentation(
            items = wallets
                .sortedWith(compareBy<ActiveWallet> { it.hdWalletIndex }.thenBy { it.address.lowercase() })
                .map { WalletPickerItem(it.address, it.address.equals(activeAddress, ignoreCase = true)) },
            createWalletActionVisible = true,
        )

    fun select(
        wallets: List<ActiveWallet>,
        address: String,
        onSelected: (ActiveWallet) -> Unit,
    ) = onSelected(ActiveWalletPolicy.requireSelectable(wallets, address))
}

data class HeaderWalletAction(
    val openSelector: Boolean,
    val copyAddress: String?,
    val selectedAddress: String?,
)

data class HeaderWalletPresentation(
    val address: String,
    val destinations: Set<ProductDestination>,
)

object GlobalWalletHeaderPolicy {
    private val PRODUCT_DESTINATIONS = setOf(
        ProductDestination.MY_KEYS,
        ProductDestination.STUDIO,
        ProductDestination.SETTINGS,
    )

    fun presentation(address: String) = HeaderWalletPresentation(
        address = ProductShellPolicy.compactAddress(address),
        destinations = PRODUCT_DESTINATIONS,
    )

    fun walletAreaAction() = HeaderWalletAction(
        openSelector = true,
        copyAddress = null,
        selectedAddress = null,
    )

    fun copyAction(address: String) = HeaderWalletAction(
        openSelector = false,
        copyAddress = AddressCopyPolicy.publicAddress(address),
        selectedAddress = null,
    )
}

data class SettingsAccountPresentation(
    val primaryWalletSelectorVisible: Boolean,
    val fullAddressVisible: Boolean,
    val createWalletActionVisible: Boolean,
)

object SettingsAccountPolicy {
    fun detailedManagement() = SettingsAccountPresentation(
        primaryWalletSelectorVisible = false,
        fullAddressVisible = true,
        createWalletActionVisible = true,
    )
}

object AddressCopyPolicy {
    fun publicAddress(address: String): String {
        require(Regex("^0x[0-9a-fA-F]{40}$").matches(address)) { "INVALID_PUBLIC_ADDRESS" }
        return address
    }
}

data class NetworkPresentation(
    val ecosystem: String,
    val network: String,
    val chainId: Long,
    val interactive: Boolean,
)

object NetworkPresentationPolicy {
    fun configured() = NetworkPresentation(
        ecosystem = "Ethereum",
        network = "Sepolia Testnet",
        chainId = IssuerSpace.chainId,
        interactive = false,
    )
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

    fun overlapDp(total: Int, position: Int): Int = if (total > 1 && position > 0) -64 else 0

    fun ordered(credentials: List<CredentialSnapshot>, selectedName: String?): List<CredentialSnapshot> =
        credentials.sortedWith(
            compareBy<CredentialSnapshot> { it.fullName == selectedName }.thenBy { it.fullName },
        )
}

sealed interface PassArtworkSource {
    data object Fallback : PassArtworkSource
    data class Remote(val url: String) : PassArtworkSource
}

object PassArtworkPolicy {
    private const val IPFS_GATEWAY = "https://ipfs.io/ipfs/"
    private val IPFS_PATH = Regex("^[A-Za-z0-9]+(?:/[A-Za-z0-9._~-]+)*$")

    fun source(raw: String?): PassArtworkSource {
        val value = raw?.trim().orEmpty()
        if (value.isEmpty()) return PassArtworkSource.Fallback
        val uri = runCatching { java.net.URI(value) }.getOrNull() ?: return PassArtworkSource.Fallback
        return when (uri.scheme?.lowercase()) {
            "https" -> if (!safeHttpsAuthority(uri)) {
                PassArtworkSource.Fallback
            } else {
                PassArtworkSource.Remote(uri.toASCIIString())
            }
            "ipfs" -> {
                val path = (uri.host.orEmpty() + uri.path.orEmpty()).trimStart('/')
                val safeSegments = path.split('/').none { it == "." || it == ".." }
                if (uri.userInfo == null && uri.query == null && uri.fragment == null &&
                    IPFS_PATH.matches(path) && safeSegments
                ) {
                    PassArtworkSource.Remote(IPFS_GATEWAY + path)
                } else {
                    PassArtworkSource.Fallback
                }
            }
            else -> PassArtworkSource.Fallback
        }
    }

    private fun safeHttpsAuthority(uri: java.net.URI): Boolean {
        val host = uri.host?.lowercase() ?: return false
        if (uri.userInfo != null || uri.port !in setOf(-1, 443)) return false
        if (host == "localhost" || host.endsWith(".localhost") || ':' in host) return false
        val octets = host.split('.').mapNotNull(String::toIntOrNull)
        val numericAlias = host.startsWith("0x") || host.all { it.isDigit() || it == '.' }
        if (octets.size != 4) return !numericAlias
        if (octets.any { it !in 0..255 }) return false
        return when {
            octets[0] == 10 || octets[0] == 127 || octets[0] == 0 -> false
            octets[0] == 169 && octets[1] == 254 -> false
            octets[0] == 172 && octets[1] in 16..31 -> false
            octets[0] == 192 && octets[1] == 168 -> false
            else -> true
        }
    }
}
