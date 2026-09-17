package info.kronk.app

import android.app.Application
import android.webkit.WebView
import dagger.hilt.android.HiltAndroidApp

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
        if (BuildConfig.DEBUG) {
            WebView.setWebContentsDebuggingEnabled(true)
        }
    }
}
