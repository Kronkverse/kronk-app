package info.kronk.app.ui.webshell

import android.annotation.SuppressLint
import android.net.Uri
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.browser.customtabs.CustomTabColorSchemeParams
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import info.kronk.core.common.KronkHost
import info.kronk.core.designsystem.theme.KronkTheme

// The web-hosted content of a Kronk tab.
//
// One instance per pillar. The WebView is kept warm across tab swaps
// (ShellHost mounts all five inside a HorizontalPager with
// `beyondBoundsPageCount = 4`), so switching tabs is instant — no
// reload, no lost scroll position, no lost form state.
//
// Same-host navigation stays inside the WebView; external links
// (target=_blank, arbitrary https://) pop out to a Chrome Custom Tab
// tinted to Kronk's `surfaceElevated` so the return-to-app feels
// intentional.

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun KronkWebView(path: String, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val toolbarColor = KronkTheme.colors.surfaceElevated.toArgb()
    val webView = remember(path) {
        val view = WebView(context)
        view.settings.javaScriptEnabled = true
        view.settings.domStorageEnabled = true
        view.settings.mediaPlaybackRequiresUserGesture = false
        // Kronk's SPA + Rails backend both set session cookies on
        // sign-in; make sure the WebView persists them so the user
        // stays signed in across app restarts.
        CookieManager.getInstance().apply {
            setAcceptCookie(true)
            setAcceptThirdPartyCookies(view, true)
        }
        view.webViewClient = KronkWebViewClient(toolbarColor)
        // Default WebChromeClient wires JS alerts, upload dialogs,
        // and progress reporting. Custom hooks for camera / share
        // intents come in a follow-up when we're ready to bridge
        // native capabilities into the WebView.
        view.webChromeClient = WebChromeClient()
        view.loadUrl(KronkHost.origin + path)
        view
    }
    AndroidView(
        factory = { webView },
        modifier = modifier.fillMaxSize(),
    )
}

// Same-host navigations stay in-WebView; anything else pops out to a
// Custom Tab. Kronk's own URL is `https://<KronkHost.value>` (currently
// shadow); we also treat the production `kronk.info` host as same-host
// so links that point at the target-post-cutover URL still stay in the
// WebView after the flip.
private class KronkWebViewClient(private val toolbarColorArgb: Int) : WebViewClient() {

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

    private fun isSameHost(url: String): Boolean {
        val host = Uri.parse(url).host ?: return false
        return host == KronkHost.value ||
            host == "kronk.info" ||
            host.endsWith(".kronk.info")
    }
}
