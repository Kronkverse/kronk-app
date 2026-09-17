package info.kronk.app

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

// Hilt entry point. Must be registered in AndroidManifest.xml's
// application android:name attribute.

@HiltAndroidApp
class KronkApplication : Application()
