package info.kronk.app.ui.shell

import android.net.Uri
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.viewinterop.AndroidView
import info.kronk.app.ui.webshell.WebState
import info.kronk.app.ui.webshell.createKronkWebView
import info.kronk.core.common.KronkHost
import info.kronk.core.designsystem.primitive.BottomTabBar
import info.kronk.core.designsystem.primitive.BottomTabItem
import info.kronk.core.designsystem.theme.KronkTheme
import kotlinx.coroutines.launch

// Top-level app scaffold. Five WebViews (one per pillar) mounted in a
// HorizontalPager with `beyondBoundsPageCount = 4` so every tab is
// composed and kept warm — tab-swap is instant, no reload, no lost
// scroll position, no lost form state. The pager itself is
// non-swipeable (Tal 2026-08-13: no side-scroll on phone); tabs move
// only through the BottomTabBar.
//
// This host also owns:
//   - back-button dispatch — Android Back navigates back inside the
//     current pillar's WebView if it can, otherwise falls through to
//     the system (exit app),
//   - file-chooser plumbing — <input type="file"> in Moments / Booth /
//     profile pic surfaces launches the native document picker, then
//     hands the result back to the WebView,
//   - a hairline loading indicator across the top edge of the current
//     tab while its page loads.

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ShellHost(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val colors = KronkTheme.colors
    val toolbarColorArgb = colors.surfaceElevated.toArgb()

    val pillars = PillarKey.values()
    val pagerState = rememberPagerState(initialPage = PillarKey.Home.ordinal) { pillars.size }
    val currentPillar = pillars[pagerState.currentPage]

    // Per-pillar observable state — WebView progress + history + URL.
    val states = remember { pillars.associateWith { WebState() } }

    // File-picker plumbing. When the web calls `onShowFileChooser`, we
    // stash the callback and launch the system document picker; when
    // it returns, we ship the URIs back through the callback.
    // Single-slot: only one picker in flight at a time (WebChromeClient
    // enforces this via its own filePathCallback contract).
    var pendingFileCallback by remember { mutableStateOf<ValueCallback<Array<Uri>>?>(null) }
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        val cb = pendingFileCallback ?: return@rememberLauncherForActivityResult
        val uris = WebChromeClient.FileChooserParams.parseResult(result.resultCode, result.data)
        cb.onReceiveValue(uris ?: emptyArray())
        pendingFileCallback = null
    }

    // Create one WebView per pillar. Kept in `remember` so they survive
    // recomposition and stay warm across tab swaps. URL loading is
    // deferred to first activation (LaunchedEffect below) so a
    // redirect-to-sign_in on cold start only lands on the tab the user
    // is looking at; the other tabs start fresh after the session
    // cookie is set on Home.
    val webViews = remember {
        pillars.associateWith { pillar ->
            createKronkWebView(
                context = context,
                state = states[pillar]!!,
                toolbarColorArgb = toolbarColorArgb,
                onShowFileChooser = { callback, params ->
                    pendingFileCallback = callback
                    try {
                        filePickerLauncher.launch(params.createIntent())
                        true
                    } catch (t: Throwable) {
                        // If the launcher can't fire (no picker app,
                        // etc.), tell the WebView the pick was cancelled
                        // so the <input> element is released.
                        pendingFileCallback = null
                        callback.onReceiveValue(emptyArray())
                        false
                    }
                },
            )
        }
    }

    // Lazy load: on first activation of a tab, load its URL. Later
    // re-visits keep whatever the WebView last navigated to (tab
    // history persists).
    LaunchedEffect(pagerState.currentPage) {
        val view = webViews[currentPillar]!!
        if (view.url == null) {
            view.loadUrl(KronkHost.origin + currentPillar.webPath)
        }
    }

    // System Back → in-WebView back navigation when there's history to
    // pop; otherwise falls through so Android's default (exit app) runs.
    val currentState = states[currentPillar]!!
    BackHandler(enabled = currentState.canGoBack) {
        webViews[currentPillar]!!.goBack()
    }

    // Flush cookies to disk when the user leaves a tab so a background
    // kill doesn't lose the session.
    LaunchedEffect(pagerState.currentPage) {
        android.webkit.CookieManager.getInstance().flush()
    }

    Scaffold(
        modifier = modifier,
        bottomBar = {
            val items = pillars.map { pillar ->
                BottomTabItem(
                    icon = painterResource(pillar.iconRes),
                    contentDescription = stringResource(pillar.labelRes),
                    selected = pagerState.currentPage == pillar.ordinal,
                    onSelect = {
                        if (pagerState.currentPage != pillar.ordinal) {
                            scope.launch { pagerState.scrollToPage(pillar.ordinal) }
                        }
                    },
                )
            }
            BottomTabBar(items = items)
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            HorizontalPager(
                state = pagerState,
                userScrollEnabled = false,
                beyondBoundsPageCount = pillars.size - 1,
                modifier = Modifier.fillMaxSize(),
            ) { page ->
                val pillar = pillars[page]
                val view = webViews[pillar]!!
                AndroidView(
                    factory = { view },
                    modifier = Modifier.fillMaxSize(),
                )
            }
            // Hairline progress indicator at the top of the current tab
            // while its page loads. Kronk-purple, invisible when idle.
            if (currentState.loading) {
                LinearProgressIndicator(
                    progress = { currentState.progress / 100f },
                    color = colors.purpleBright,
                    trackColor = colors.borderSubtle,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .fillMaxWidth(),
                )
            }
        }
    }
}
