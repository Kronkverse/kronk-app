package info.kronk.app.ui.webshell

import android.content.Context
import info.kronk.app.ui.shell.PillarKey
import info.kronk.core.common.KronkHost

// Persist the last-visited URL per pillar so a killed-then-
// relaunched app resumes each tab where the user left it —
// scrolling deep into a Nudge thread survives the process
// eviction that inevitably happens on low-memory devices.
//
// Written from `KronkWebViewClient.doUpdateVisitedHistory`
// tagged with which pillar owns the WebView; read from ShellHost
// on cold start to seed each WebView's initial `loadUrl` call.

object PillarUrlStore {

    private const val PREFS = "pillar_urls"

    fun urlFor(context: Context, pillar: PillarKey): String {
        val fallback = KronkHost.origin + pillar.webPath
        val saved = prefs(context).getString(pillar.name, null) ?: return fallback
        // Guard against stale cross-host state — if the KronkHost
        // flipped between debug + release builds, drop the saved URL
        // and start over on the current host.
        return if (saved.startsWith(KronkHost.origin)) saved else fallback
    }

    fun save(context: Context, pillar: PillarKey, url: String) {
        // Skip auth-flow interstitials — persisting `/auth/sign_in`
        // would keep bouncing the user through the login page on
        // every launch instead of resuming their real destination.
        if (url.contains("/auth/") || url.contains("/oauth/")) return
        prefs(context).edit().putString(pillar.name, url).apply()
    }

    fun clear(context: Context) {
        prefs(context).edit().clear().apply()
    }

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
}
