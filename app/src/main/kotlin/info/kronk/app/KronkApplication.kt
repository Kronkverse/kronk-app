package info.kronk.app

import android.app.Application
import android.webkit.WebView
import dagger.hilt.android.HiltAndroidApp
import info.kronk.app.push.PushRegistrar
import info.kronk.core.common.KronkHost

// Hilt entry point. Must be registered in AndroidManifest.xml's
// application android:name attribute.
//
// Enables Chrome DevTools debugging of the in-app WebViews on debug
// builds — visit `chrome://inspect` on a laptop while a debug APK is
// running to inspect the shell's tabs.

@HiltAndroidApp
class KronkApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // Wire the build-variant host into :core:common. Must happen
        // before any consumer (Retrofit factory, WebView loadUrl,
        // intent resolver) reads KronkHost — application onCreate
        // runs before Activity onCreate so any composition triggered
        // by MainActivity sees the correct value.
        KronkHost.initFromApp(BuildConfig.KRONK_HOST)
        if (BuildConfig.DEBUG) {
            WebView.setWebContentsDebuggingEnabled(true)
        }
        // Auto-detect push transport: UnifiedPush distributor if
        // installed, FCM-via-GSF fallback (wired in push #2).
        // Idempotent — safe on every launch.
        PushRegistrar.ensureRegistered(this)
    }
}
