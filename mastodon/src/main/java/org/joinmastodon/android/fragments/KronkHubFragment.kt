package org.joinmastodon.android.fragments

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.runtime.mutableStateOf
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
import me.grishka.appkit.fragments.AppKitFragment
import org.joinmastodon.android.api.session.AccountSessionManager
import org.joinmastodon.android.ui.compose.KosmosSheet
import org.joinmastodon.android.ui.compose.KronkSpace
import org.joinmastodon.android.ui.compose.SpaceUsageTracker

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

    // Mutable slide offset — updated by HomeFragment via updateSlideOffset()
    private var slideOffset = mutableStateOf(0f)

    // Update called by HomeFragment's BottomSheetBehavior.onSlide callback
    fun updateSlideOffset(offset: Float) {
        slideOffset.value = offset
    }

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
                KosmosSheet(
                    slideOffset = slideOffset.value,
                    recentSpaces = SpaceUsageTracker.getRecents(activity!!, accountId),
                    onSpaceTapped = ::navigateToSpace,
                )
            }
        }
        return OwnerAnchorView(activity!!).apply {
            addView(composeView, ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
        }
    }

    private fun navigateToSpace(space: KronkSpace) {
        val target = when (space) {
            KronkSpace.MURMUR   -> HomeFragment.Space.FEED
            KronkSpace.KOMMONS  -> HomeFragment.Space.KOMMONS
            KronkSpace.HUDDLE   -> HomeFragment.Space.HUDDLE
            KronkSpace.KALENDAR -> HomeFragment.Space.EVENTS
            KronkSpace.NUDGES   -> HomeFragment.Space.NUDGES
        }
        homeFragment()?.openSpace(target)
    }

    private fun homeFragment(): HomeFragment? = (parentFragment as? HomeFragment)
}
