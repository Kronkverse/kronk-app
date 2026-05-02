package org.joinmastodon.android.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import me.grishka.appkit.Nav
import me.grishka.appkit.fragments.AppKitFragment
import org.joinmastodon.android.api.session.AccountSessionManager
import org.joinmastodon.android.ui.compose.KronkHomeScreen
import org.parceler.Parcels

// AppKitFragment extends android.app.Activity (not ComponentActivity), so there is no
// ambient ViewTreeLifecycleOwner in the window hierarchy. This fragment owns its lifecycle.
class KronkHubFragment : AppKitFragment(), LifecycleOwner {

    private val lifecycleRegistry = LifecycleRegistry(this)
    override val lifecycle: Lifecycle get() = lifecycleRegistry

    private val accountId: String get() = arguments?.getString("account") ?: ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
    }

    override fun onStart() {
        super.onStart()
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
    }

    override fun onResume() {
        super.onResume()
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
    }

    override fun onPause() {
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
        super.onPause()
    }

    override fun onStop() {
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
        super.onStop()
    }

    override fun onDestroy() {
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        super.onDestroy()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        return ComposeView(activity).apply {
            // FragmentStackActivity extends android.app.Activity (not ComponentActivity), so no
            // ambient ViewTreeLifecycleOwner is set on the window hierarchy. WindowRecomposer
            // calls ViewTreeLifecycleOwner.get(view.rootView) during onAttachedToWindow, but
            // "rootView" varies by timing: it may be this ComposeView (before first traversal),
            // the fragment container (fragment_wrap, if its parent chain is momentarily
            // disconnected), or the DecorView. Tag all three via reflection so it's found
            // regardless of which view Compose treats as the root.
            try {
                val cls = Class.forName("androidx.lifecycle.ViewTreeLifecycleOwner")
                val set = cls.getMethod("set", android.view.View::class.java, LifecycleOwner::class.java)
                val owner = this@KronkHubFragment as LifecycleOwner
                for (v in listOfNotNull(this, container, activity?.window?.decorView)) {
                    set.invoke(null, v, owner)
                }
            } catch (_: ReflectiveOperationException) {}
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindowOrReleasedFromPool)
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
