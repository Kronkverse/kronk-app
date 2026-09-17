package info.kronk.app.ui.webshell

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.net.Uri
import android.webkit.CookieManager
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
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
    onShowFileChooser: (ValueCallback<Array<Uri>>, WebChromeClient.FileChooserParams) -> Boolean,
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
        },
        onError = { message -> state.lastError = message },
        onSuccess = { state.lastError = null },
    )
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
        onHistoryChange()
        onSuccess()
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
// bottom-band-hider.
private const val PUSH_STAGE_ABOVE_NATIVE_BAR_JS = """
(function() {
  if (window.__kronkAppShellPadded) return;
  window.__kronkAppShellPadded = true;
  var nonceMeta = document.querySelector('meta[name="style-nonce"]');
  var nonce = nonceMeta ? nonceMeta.getAttribute('content') : null;
  var s = document.createElement('style');
  s.id = 'kronk-app-shell-pad';
  if (nonce) s.setAttribute('nonce', nonce);
  s.textContent = 'body { padding-bottom: 88px !important; } ' +
                  '.kronk-menu { bottom: 96px !important; }';
  (document.head || document.documentElement).appendChild(s);
})();
"""
