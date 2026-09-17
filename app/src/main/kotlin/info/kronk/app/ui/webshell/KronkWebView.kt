package info.kronk.app.ui.webshell

import android.annotation.SuppressLint
import android.app.DownloadManager
import android.content.Context
import android.graphics.Color
import android.net.Uri
import android.os.Environment
import android.webkit.URLUtil
import android.webkit.CookieManager
import android.webkit.PermissionRequest
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import info.kronk.app.audio.KronkJsBridge
import info.kronk.app.push.PushRegistration
import info.kronk.app.push.SessionTracker
import androidx.browser.customtabs.CustomTabColorSchemeParams
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import info.kronk.core.common.KronkHost

// Observable state for a single WebView. Kept as MutableStates so
// composables that read them recompose when the WebView reports
// progress or history changes.

@Stable
class WebState {
    var progress by mutableIntStateOf(100)
    var loading by mutableStateOf(false)
    var canGoBack by mutableStateOf(false)
    var currentUrl by mutableStateOf<String?>(null)
    // Last error surface for a page that failed to load — e.g.
    // network unreachable, DNS failure, TLS handshake failure, or
    // an HTTP 5xx on the main-frame request. Cleared on next
    // successful onPageFinished for the main-frame URL.
    var lastError by mutableStateOf<String?>(null)
}

// Non-Composable WebView factory — ShellHost owns the WebView
// instances (one per pillar) so back-button dispatch and file-chooser
// callbacks can reach them from the hosting Activity.
//
// Same-host navigation stays in-WebView; external links pop out to a
// Chrome Custom Tab. `onShowFileChooser` is delegated to the caller so
// ShellHost can wire an ActivityResultLauncher for <input type="file">
// on Moments / Booth / profile pic surfaces.
//
// Injects a stylesheet on page finish that hides the web's own mobile
// bottom bar (`.kronk-frame__bottom-band`) — the native BottomTabBar
// is the only nav bar on the phone; without this the user sees both
// stacked.

@SuppressLint("SetJavaScriptEnabled")
fun createKronkWebView(
    context: Context,
    state: WebState,
    toolbarColorArgb: Int,
    pillarKey: info.kronk.app.ui.shell.PillarKey,
    onShowFileChooser: (ValueCallback<Array<Uri>>, WebChromeClient.FileChooserParams) -> Boolean,
    onPermissionRequest: (PermissionRequest) -> Unit,
): WebView {
    val view = WebView(context)
    // Match Kronk's surfacePrimary (#191b22) so the load transition
    // reads as Kronk-dark rather than a white flash or a hardware-
    // accelerated black. The web's own body background is the same
    // colour so there's no visible seam once the page paints.
    view.setBackgroundColor(Color.parseColor("#191B22"))
    view.settings.javaScriptEnabled = true
    view.settings.domStorageEnabled = true
    view.settings.mediaPlaybackRequiresUserGesture = false
    // Stamp the WebView's UA so shadow's request logs / analytics can
    // distinguish native-app traffic from a straight browser session
    // — useful for Kronk when reasoning about which surfaces get the
    // most use through the shell vs the browser (and for the eventual
    // KronkAppShell detection the SPA might grow, e.g. to skip the
    // Web-Push subscription prompt when native push is bound).
    view.settings.userAgentString = view.settings.userAgentString + " KronkAppShell/1.0"
    // Content-provider URIs from the native document picker are loaded
    // by the WebView when uploading; the local `file://` scheme stays
    // disabled (its default on API 30+) since we never load it.
    view.settings.allowContentAccess = true
    // Cookies — Kronk's Rails backend uses session cookies for the SPA
    // sign-in; make sure the WebView persists them so the user stays
    // signed in across app restarts.
    CookieManager.getInstance().apply {
        setAcceptCookie(true)
        setAcceptThirdPartyCookies(view, true)
    }
    view.webViewClient = KronkWebViewClient(
        toolbarColorArgb = toolbarColorArgb,
        onHistoryChange = {
            state.canGoBack = view.canGoBack()
            state.currentUrl = view.url
            view.url?.let { PillarUrlStore.save(context, pillarKey, it) }
        },
        onError = { message -> state.lastError = message },
        onSuccess = { state.lastError = null },
    )
    // JS ↔ native bridge. Only kronk.info and shadow.kronk.info
    // pages ever load here (shouldOverrideUrlLoading pops any
    // other host out to a Custom Tab), so `KronkNative` is safe
    // to expose. Backing methods carry the @JavascriptInterface
    // annotation as Android 4.2+ requires.
    view.addJavascriptInterface(KronkJsBridge(context.applicationContext), "KronkNative")
    view.webChromeClient = object : WebChromeClient() {
        override fun onProgressChanged(v: WebView?, newProgress: Int) {
            state.progress = newProgress
            state.loading = newProgress < 100
        }

        override fun onShowFileChooser(
            webView: WebView?,
            filePathCallback: ValueCallback<Array<Uri>>,
            fileChooserParams: FileChooserParams,
        ): Boolean = onShowFileChooser(filePathCallback, fileChooserParams)

        // Getters like getUserMedia({audio:true}) call this. We
        // delegate to ShellHost which owns the Android permission
        // launcher — if the app doesn't have RECORD_AUDIO / CAMERA
        // yet, the launcher prompts before the WebView is granted.
        override fun onPermissionRequest(request: PermissionRequest) {
            onPermissionRequest(request)
        }
    }
    // Download links (media exports, data archives, APK updates)
    // route to the system DownloadManager so the file lands in the
    // user's Downloads folder and shows in the notification tray.
    view.setDownloadListener { url, userAgent, contentDisposition, mimetype, _ ->
        runCatching {
            val filename = URLUtil.guessFileName(url, contentDisposition, mimetype)
            val request = DownloadManager.Request(Uri.parse(url))
                .setTitle(filename)
                .setDescription("Downloading from Kronk")
                .setMimeType(mimetype)
                .addRequestHeader("User-Agent", userAgent)
                .setNotificationVisibility(
                    DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED
                )
                .setDestinationInExternalPublicDir(
                    Environment.DIRECTORY_DOWNLOADS,
                    filename,
                )
            val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            dm.enqueue(request)
        }
    }
    // No initial loadUrl — ShellHost lazy-loads each tab on first
    // activation so tabs the user never opens don't spin up
    // (and, more importantly, so a redirect-to-sign_in on cold start
    // only lands on the tab the user is looking at; the other tabs
    // start fresh once the session cookie is set).
    return view
}

// Same-host navigations stay in-WebView; anything else pops out to a
// Chrome Custom Tab. Kronk host check covers `<KronkHost.value>` plus
// `kronk.info` and any `*.kronk.info` subdomain so post-cutover links
// still stay in-shell.
private class KronkWebViewClient(
    private val toolbarColorArgb: Int,
    private val onHistoryChange: () -> Unit,
    private val onError: (String) -> Unit,
    private val onSuccess: () -> Unit,
) : WebViewClient() {

    override fun shouldOverrideUrlLoading(
        view: WebView,
        request: WebResourceRequest,
    ): Boolean {
        val url = request.url.toString()
        return if (isSameHost(url)) {
            false
        } else {
            val intent = CustomTabsIntent.Builder()
                .setDefaultColorSchemeParams(
                    CustomTabColorSchemeParams.Builder()
                        .setToolbarColor(toolbarColorArgb)
                        .build(),
                )
                .setShowTitle(true)
                .build()
            intent.launchUrl(view.context, request.url)
            true
        }
    }

    override fun onPageFinished(view: WebView, url: String) {
        super.onPageFinished(view, url)
        // Hide the web's own mobile bottom bar so it doesn't stack under
        // the native BottomTabBar. Idempotent via the window flag —
        // safe to call on every SPA route change.
        view.evaluateJavascript(HIDE_WEB_BOTTOM_BAR_JS, null)
        // Push the web's stage up so the Kronk menu FAB (bottom-right
        // fixed) isn't hidden behind the native BottomTabBar.
        view.evaluateJavascript(PUSH_STAGE_ABOVE_NATIVE_BAR_JS, null)
        // Install the media-play/pause monitor so the native audio
        // service can start/stop with WebView playback. Idempotent.
        view.evaluateJavascript(MEDIA_MONITOR_JS, null)
        // Force Service Worker registration so we get offline
        // caching. Kronk's SPA gates registration on isProduction()
        // AND a signed-in `me`; for the shell we want the SW active
        // regardless so a first-launch cold-start still primes
        // the cache. Idempotent — SW.register is a no-op if the
        // scope already has one.
        view.evaluateJavascript(FORCE_SW_REGISTRATION_JS, null)
        // Extract the SPA's access token out of initial-state.
        // Mastodon injects it into `<script id="initial-state">…`
        // on every signed-in page render; sign-out pages carry
        // no token so this returns null. We track the state
        // transitions so a fresh sign-in registers push and a
        // sign-out unregisters + clears the ECDH keys.
        view.evaluateJavascript(EXTRACT_ACCESS_TOKEN_JS) { raw ->
            val token = raw?.trim('"', ' ')?.takeIf { it.isNotEmpty() && it != "null" }
            SessionTracker.onTokenSeen(view.context, token)
        }
        onHistoryChange()
        onSuccess()
        // First tab to finish loading flips the "app ready" latch —
        // MainActivity's SplashScreen keep-on-screen condition polls
        // this to know when to hand off from rose emblem to WebView.
        AppReadyState.markReady()
    }

    override fun doUpdateVisitedHistory(view: WebView, url: String, isReload: Boolean) {
        super.doUpdateVisitedHistory(view, url, isReload)
        onHistoryChange()
    }

    // Main-frame load failures — DNS unreachable, TLS handshake, connection reset.
    override fun onReceivedError(
        view: WebView,
        request: WebResourceRequest,
        error: WebResourceError,
    ) {
        super.onReceivedError(view, request, error)
        if (!request.isForMainFrame) return
        onError("Couldn't reach ${request.url.host} — ${error.description} (${error.errorCode})")
    }

    // Main-frame HTTP errors — 4xx/5xx from the server itself.
    override fun onReceivedHttpError(
        view: WebView,
        request: WebResourceRequest,
        errorResponse: WebResourceResponse,
    ) {
        super.onReceivedHttpError(view, request, errorResponse)
        if (!request.isForMainFrame) return
        onError("HTTP ${errorResponse.statusCode} on ${request.url.path ?: request.url}")
    }

    private fun isSameHost(url: String): Boolean {
        val host = Uri.parse(url).host ?: return false
        return host == KronkHost.value ||
            host == "kronk.info" ||
            host.endsWith(".kronk.info")
    }
}

// Injected on every page finish. `.kronk-frame__bottom-band` is the
// web's mobile fixed-bottom container (see kronk_frame.scss:283); on
// phone it holds the web's own HubSwitcher--bottom. The native shell
// replaces it, so hiding it in the WebView removes the double-nav.
//
// Kronk's Rails backend serves a strict CSP that forbids inline
// styles unless they carry the per-request `style-nonce`, so we read
// the nonce off the `<meta name="style-nonce">` tag Rails emits and
// stamp it on the injected `<style>` element. Without the nonce the
// browser silently blocks the injection and the double-nav returns.
// A MutationObserver re-injects when the SPA re-renders the head
// (React route changes can wipe DOM head modifications).
private const val HIDE_WEB_BOTTOM_BAR_JS = """
(function() {
  if (window.__kronkAppShellStyled) return;
  window.__kronkAppShellStyled = true;
  var nonceMeta = document.querySelector('meta[name="style-nonce"]');
  var nonce = nonceMeta ? nonceMeta.getAttribute('content') : null;
  var inject = function() {
    if (document.getElementById('kronk-app-shell-style')) return;
    var s = document.createElement('style');
    s.id = 'kronk-app-shell-style';
    if (nonce) s.setAttribute('nonce', nonce);
    s.textContent = '.kronk-frame__bottom-band { display: none !important; }';
    (document.head || document.documentElement).appendChild(s);
  };
  inject();
  new MutationObserver(inject).observe(document.documentElement, { childList: true, subtree: true });
})();
"""

// Also inject a body padding-bottom so the web's floating Kronk menu
// (the Ж FAB — position: fixed at bottom-right on mobile, see
// kronk_menu.tsx) isn't hidden behind the native BottomTabBar. The
// native bar is ~52dp core height + navigation-bar inset; 88px is a
// pragmatic conservative value. Uses the same CSP nonce path as the
// bottom-band-hider. MutationObserver re-injects if the SPA rerenders
// <head> on route change and wipes the tag (React can drop DOM head
// modifications when the top-level route swaps).
// Register Kronk's Workbox service worker unconditionally. Kronk's
// main.tsx gates SW registration behind `isProduction() && me`,
// which means shadow-hosted debug builds + the pre-sign-in state
// never register a SW and get no offline caching. The shell wants
// the SW active in every state so browsing what's already been
// loaded stays available on flaky/offline connections. The SW itself
// (Workbox 7.2, /sw.js) does the actual caching once installed.
private const val FORCE_SW_REGISTRATION_JS = """
(function() {
  if (window.__kronkSwRegistered) return;
  window.__kronkSwRegistered = true;
  if (!('serviceWorker' in navigator)) return;
  navigator.serviceWorker.register('/sw.js', { scope: '/' })
    .catch(function() { /* swallow — some hosts don't serve /sw.js */ });
})();
"""

// Listens on the document for HTMLMediaElement play/pause events
// and pipes them to the native KronkJsBridge, which increments /
// decrements a counter that gates the foreground audio service.
// Delegated event capture (`true`) catches every audio/video
// element on the page regardless of how the SPA mounts them.
// Idempotent via the window flag so SPA route changes don't stack
// listeners.
private const val MEDIA_MONITOR_JS = """
(function() {
  if (window.__kronkMediaMonitor) return;
  window.__kronkMediaMonitor = true;
  if (typeof KronkNative === 'undefined') return;
  var isMedia = function(t) { return t instanceof HTMLMediaElement; };
  document.addEventListener('play', function(e) {
    if (isMedia(e.target)) KronkNative.onMediaPlay();
  }, true);
  document.addEventListener('pause', function(e) {
    if (isMedia(e.target)) KronkNative.onMediaPause();
  }, true);
  document.addEventListener('ended', function(e) {
    if (isMedia(e.target)) KronkNative.onMediaPause();
  }, true);
  window.addEventListener('pagehide', function() {
    KronkNative.resetMediaCount();
  });
})();
"""

// Reads the SPA's access token out of the Rails-emitted
// `<script id="initial-state">` blob on every page finish. Returns
// null when the user isn't signed in (Rails renders the auth pages
// without an initial-state script). Kronk's SPA reads the same
// value for its own API calls — piggybacking on it means the app
// doesn't have to re-run the OAuth authorize/exchange dance.
private const val EXTRACT_ACCESS_TOKEN_JS = """
(function() {
  try {
    var el = document.getElementById('initial-state');
    if (!el) return null;
    var s = JSON.parse(el.textContent);
    return (s && s.meta && s.meta.access_token) || null;
  } catch (e) { return null; }
})();
"""

private const val PUSH_STAGE_ABOVE_NATIVE_BAR_JS = """
(function() {
  if (window.__kronkAppShellPaddedInstalled) return;
  window.__kronkAppShellPaddedInstalled = true;
  var nonceMeta = document.querySelector('meta[name="style-nonce"]');
  var nonce = nonceMeta ? nonceMeta.getAttribute('content') : null;
  var inject = function() {
    if (document.getElementById('kronk-app-shell-pad')) return;
    var s = document.createElement('style');
    s.id = 'kronk-app-shell-pad';
    if (nonce) s.setAttribute('nonce', nonce);
    s.textContent = 'body { padding-bottom: 88px !important; } ' +
                    '.kronk-menu { bottom: 96px !important; }';
    (document.head || document.documentElement).appendChild(s);
  };
  inject();
  new MutationObserver(inject).observe(document.documentElement, { childList: true, subtree: true });
})();
"""
