package org.joinmastodon.android.fragments

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import me.grishka.appkit.Nav
import me.grishka.appkit.fragments.AppKitFragment
import org.joinmastodon.android.api.session.AccountSessionManager
import org.joinmastodon.android.ui.compose.KronkHomeScreen
import org.parceler.Parcels

// FragmentStackActivity extends android.app.Activity (not ComponentActivity), so no
// ViewTree owners are provided on the window hierarchy. This fragment provides all three.
class KronkHubFragment : AppKitFragment(), LifecycleOwner, ViewModelStoreOwner, SavedStateRegistryOwner {

    private val lifecycleRegistry = LifecycleRegistry(this)
    override val lifecycle: Lifecycle get() = lifecycleRegistry

    private val _viewModelStore = ViewModelStore()
    override val viewModelStore: ViewModelStore get() = _viewModelStore

    private val savedStateController = SavedStateRegistryController.create(this)
    override val savedStateRegistry: SavedStateRegistry get() = savedStateController.savedStateRegistry

    private val accountId: String get() = arguments?.getString("account") ?: ""

    // FrameLayout wrapper that tags itself and all ancestors with ViewTree owners before
    // its children attach. Because dispatchAttachedToWindow runs top-down, this fires
    // before ComposeView.onAttachedToWindow() creates the WindowRecomposer, so Compose
    // finds the owners when it walks up the parent chain. ComposeView itself is final
    // in Kotlin so it cannot be subclassed directly.
    private inner class OwnerAnchorView(context: Context) : FrameLayout(context) {
        override fun onAttachedToWindow() {
            var v: View? = this
            while (v != null) {
                tagViewTreeOwners(v)
                v = v.parent as? View
            }
            super.onAttachedToWindow()
        }
    }

    // R8 can rename interface class names (LifecycleOwner etc.), so we look up the
    // "set" method by name and arity rather than by full signature to stay robust.
    private fun tagViewTreeOwners(view: View) {
        for (cls in listOf(
            "androidx.lifecycle.ViewTreeLifecycleOwner",
            "androidx.lifecycle.ViewTreeViewModelStoreOwner",
            "androidx.savedstate.ViewTreeSavedStateRegistryOwner",
        )) {
            try {
                val m = Class.forName(cls).methods
                    .find { it.name == "set" && it.parameterCount == 2 } ?: continue
                m.invoke(null, view, this@KronkHubFragment)
            } catch (_: Exception) {}
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        savedStateController.performRestore(savedInstanceState)
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
        _viewModelStore.clear()
        super.onDestroy()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        savedStateController.performSave(outState)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        val composeView = ComposeView(activity!!).apply {
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
        return OwnerAnchorView(activity!!).apply {
            addView(composeView, ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
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
            "Murmur"   -> HomeFragment.Space.FEED
            "Kommons"  -> HomeFragment.Space.KOMMONS
            "Huddle"   -> HomeFragment.Space.HUDDLE
            "Kalendar" -> HomeFragment.Space.EVENTS
            else       -> null
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
        Nav.go(activity!!, ProfileFragment::class.java, args)
    }

    private fun navigateToCompose() {
        val id = accountId.ifBlank { return }
        val args = Bundle().apply { putString("account", id) }
        Nav.go(activity!!, ComposeFragment::class.java, args)
    }

    private fun homeFragment(): HomeFragment? = (parentFragment as? HomeFragment)
}
