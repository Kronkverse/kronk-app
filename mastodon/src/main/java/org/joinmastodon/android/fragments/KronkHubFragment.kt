package org.joinmastodon.android.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import me.grishka.appkit.Nav
import me.grishka.appkit.fragments.AppKitFragment
import org.joinmastodon.android.api.session.AccountSessionManager
import org.joinmastodon.android.ui.compose.KronkHomeScreen
import org.parceler.Parcels

class KronkHubFragment : AppKitFragment() {

    private val accountId: String get() = arguments?.getString("account") ?: ""

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        return ComposeView(activity).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                KronkHomeScreen(
                    displayName = resolveDisplayName(),
                    onSpaceTapped = ::navigateToSpace,
                    onNotificationsTapped = ::navigateToNotifications,
                    onProfileTapped = ::navigateToProfile,
                    onComposeTapped = ::navigateToCompose,
                )
            }
        }
    }

    private fun resolveDisplayName(): String {
        val id = accountId.ifBlank { return "You" }
        val account = AccountSessionManager.getInstance().getAccount(id)?.self ?: return "You"
        val name = account.displayName.ifBlank { null } ?: account.username ?: return "You"
        return name.split(" ").firstOrNull() ?: name
    }

    private fun navigateToSpace(space: String) {
        val target = when (space) {
            "Murmur"      -> HomeFragment.Space.FEED
            "Kommons"     -> HomeFragment.Space.KOMMONS
            "Huddle"      -> HomeFragment.Space.HUDDLE
            "Kalendar"    -> HomeFragment.Space.EVENTS
            else          -> null
        } ?: return
        homeFragment()?.openSpace(target)
    }

    private fun navigateToNotifications() {
        homeFragment()?.showNotifications()
    }

    private fun navigateToProfile() {
        val id = accountId.ifBlank { return }
        val session = AccountSessionManager.getInstance().getAccount(id) ?: return
        val args = Bundle().apply {
            putString("account", id)
            putParcelable("profileAccount", Parcels.wrap(session.self))
        }
        Nav.go(activity, ProfileFragment::class.java, args)
    }

    private fun navigateToCompose() {
        val id = accountId.ifBlank { return }
        val args = Bundle().apply { putString("account", id) }
        Nav.go(activity, ComposeFragment::class.java, args)
    }

    private fun homeFragment(): HomeFragment? = (parentFragment as? HomeFragment)
}
