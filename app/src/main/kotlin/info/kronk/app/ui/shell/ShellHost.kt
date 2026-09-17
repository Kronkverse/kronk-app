package info.kronk.app.ui.shell

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.webkit.PermissionRequest
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import info.kronk.app.R
import info.kronk.app.push.SessionTracker
import info.kronk.app.ui.webshell.IntentEvents
import info.kronk.app.ui.webshell.KronkIntent
import info.kronk.app.ui.webshell.PillarUrlStore
import info.kronk.app.ui.webshell.WebState
import info.kronk.app.ui.webshell.buildFileChooserIntent
import info.kronk.app.ui.webshell.createKronkWebView
import info.kronk.app.ui.webshell.parseFileChooserResult
import info.kronk.core.common.KronkHost
import info.kronk.core.designsystem.primitive.BottomTabBar
import info.kronk.core.designsystem.primitive.BottomTabItem
import info.kronk.core.designsystem.theme.KronkTheme
import kotlinx.coroutines.launch

// Map from the webkit resource identifier the WebView sends to the
// Android runtime permission it needs. Only mic + camera today; if a
// later WebView surface asks for MIDI or protected media the request
// falls through (we deny by returning null here).
private fun androidPermFor(webResource: String): String? = when (webResource) {
    PermissionRequest.RESOURCE_AUDIO_CAPTURE -> Manifest.permission.RECORD_AUDIO
    PermissionRequest.RESOURCE_VIDEO_CAPTURE -> Manifest.permission.CAMERA
    else -> null
}

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

    // File-picker plumbing. When the web calls `onShowFileChooser` we
    // build the right intent (camera / camcorder / document picker
    // depending on the `<input>`'s accept + capture attrs), stash the
    // callback + output URI, and launch. On return, ship URIs back to
    // the WebView. Single-slot: only one picker in flight at a time
    // (WebChromeClient enforces this via its own filePathCallback
    // contract).
    var pendingFileCallback by remember { mutableStateOf<ValueCallback<Array<Uri>>?>(null) }
    var pendingCaptureUri by remember { mutableStateOf<Uri?>(null) }
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        val cb = pendingFileCallback ?: return@rememberLauncherForActivityResult
        val uris = parseFileChooserResult(pendingCaptureUri, result.resultCode, result.data)
        cb.onReceiveValue(uris)
        pendingFileCallback = null
        pendingCaptureUri = null
    }

    // getUserMedia({audio:true}) / getUserMedia({video:true}) plumbing.
    // WebView's onPermissionRequest fires when the SPA asks for mic or
    // camera; we translate its webkit resource names to the underlying
    // Android runtime permissions, prompt if not held, then grant back
    // to the WebView. Single-slot: only one WebView permission request
    // in flight at a time (matches the WebView contract).
    var pendingWebPermission by remember { mutableStateOf<PermissionRequest?>(null) }
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
    ) { grants ->
        val req = pendingWebPermission ?: return@rememberLauncherForActivityResult
        val webResourcesToGrant = req.resources.filter { resource ->
            val androidPerm = androidPermFor(resource) ?: return@filter false
            grants[androidPerm] == true ||
                ContextCompat.checkSelfPermission(context, androidPerm) ==
                PackageManager.PERMISSION_GRANTED
        }.toTypedArray()
        if (webResourcesToGrant.isNotEmpty()) req.grant(webResourcesToGrant) else req.deny()
        pendingWebPermission = null
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
                pillarKey = pillar,
                onShowFileChooser = { callback, params ->
                    pendingFileCallback = callback
                    val capture = buildFileChooserIntent(context, params)
                    pendingCaptureUri = capture.outputUri
                    try {
                        filePickerLauncher.launch(capture.chooser)
                        true
                    } catch (t: Throwable) {
                        // No matching activity (no camera, no document
                        // picker, etc.) — release the <input> so the
                        // web page isn't stuck waiting.
                        pendingFileCallback = null
                        pendingCaptureUri = null
                        callback.onReceiveValue(emptyArray())
                        false
                    }
                },
                onPermissionRequest = { req ->
                    val needed = req.resources
                        .mapNotNull { androidPermFor(it) }
                        .filter { p ->
                            ContextCompat.checkSelfPermission(context, p) !=
                                PackageManager.PERMISSION_GRANTED
                        }
                        .distinct()
                        .toTypedArray()
                    if (needed.isEmpty()) {
                        // All Android perms already granted — grant the
                        // webkit resources we know how to translate.
                        val grantable = req.resources
                            .filter { androidPermFor(it) != null }
                            .toTypedArray()
                        if (grantable.isNotEmpty()) req.grant(grantable) else req.deny()
                    } else {
                        pendingWebPermission = req
                        permissionLauncher.launch(needed)
                    }
                },
            )
        }
    }

    // Lazy load: on first activation of a tab, load its URL — the
    // saved last-visited URL for that pillar if any, else the
    // pillar's canonical entry point. Later re-visits keep whatever
    // the WebView last navigated to (tab history persists in-process
    // via the WebView; across process death via PillarUrlStore).
    LaunchedEffect(pagerState.currentPage) {
        val view = webViews[currentPillar]!!
        if (view.url == null) {
            view.loadUrl(PillarUrlStore.urlFor(context, currentPillar))
        }
    }

    // Deep-link + share intents. MainActivity's dispatch() emits into
    // IntentEvents; we consume here and route to the right pillar.
    // OpenUrl: switch tab + WebView.loadUrl(target).
    // Compose (share-into-Kronk): switch to Home, load /publish; the
    // web feature reads the query params to hydrate its state.
    LaunchedEffect(Unit) {
        IntentEvents.events.collect { evt ->
            when (evt) {
                is KronkIntent.OpenUrl -> {
                    scope.launch { pagerState.scrollToPage(evt.pillar.ordinal) }
                    webViews[evt.pillar]!!.loadUrl(evt.url)
                }
                is KronkIntent.Compose -> {
                    val text = evt.text.orEmpty()
                    val encoded = android.net.Uri.encode(text)
                    val url = KronkHost.origin + "/publish" +
                        (if (encoded.isNotEmpty()) "?text=$encoded" else "")
                    scope.launch { pagerState.scrollToPage(PillarKey.Home.ordinal) }
                    webViews[PillarKey.Home]!!.loadUrl(url)
                    // Attachments arrive via ACTION_SEND EXTRA_STREAM;
                    // wiring them into Kronk's compose feature requires
                    // a JS bridge (upload-then-attach). Deferred to
                    // Week 2 (this landing at least gets the text
                    // populated so the user isn't stuck).
                }
            }
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

    // Ask for POST_NOTIFICATIONS the first time we see the user
    // signed in on Android 13+. Push registration works without
    // this perm — but the tray silently drops incoming pushes if
    // it's missing, so ask at a natural moment (right after
    // sign-in) rather than at first display.
    val notificationLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { /* result ignored — user's choice is the choice */ }
    LaunchedEffect(Unit) {
        SessionTracker.signedIn.collect { signedIn ->
            if (!signedIn) return@collect
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return@collect
            val perm = Manifest.permission.POST_NOTIFICATIONS
            if (
                ContextCompat.checkSelfPermission(context, perm) ==
                PackageManager.PERMISSION_GRANTED
            ) return@collect
            notificationLauncher.launch(perm)
        }
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
            // Error overlay — shown when the WebView reports a
            // main-frame load failure. Kept intentionally opinionated
            // so a blank tab always tells the user (and me) what went
            // wrong, instead of a dark rectangle. Tapping reloads the
            // current tab's WebView.
            val err = currentState.lastError
            if (err != null && !currentState.loading) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .fillMaxWidth()
                        .background(colors.surfaceElevated)
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                ) {
                    Text(
                        text = err,
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.textSecondary,
                        modifier = Modifier.weight(1f),
                    )
                    TextButton(
                        onClick = {
                            currentState.lastError = null
                            webViews[currentPillar]!!.reload()
                        },
                    ) {
                        Text(
                            text = stringResource(R.string.retry),
                            color = colors.purpleBright,
                        )
                    }
                }
            }
        }
    }
}
