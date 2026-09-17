package info.kronk.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import dagger.hilt.android.AndroidEntryPoint
import info.kronk.app.ui.AuthGate
import info.kronk.app.ui.webshell.AppReadyState
import info.kronk.app.ui.webshell.IntentEvents
import info.kronk.core.designsystem.theme.KronkTheme
import info.kronk.feature.auth.ui.AuthViewModel
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

// Composition root. Sets edge-to-edge, wraps everything in KronkTheme,
// hands off to AuthGate which routes signed-out → SignInScreen, signed-
// in → WelcomePane (real Home comes in Phase 2C).
//
// Also handles the kronk-auth://callback deep link — Custom Tab
// deposits the OAuth code here as an Intent.data URI.

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val authViewModel: AuthViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        // installSplashScreen MUST run before super.onCreate so the
        // splash surface is bound to this Activity instance and the
        // rose icon is what the user sees between launcher-tap and
        // first Compose paint. Hold the splash until the first
        // WebView finishes loading so there's no "rose -> blank
        // dark -> content" flash on slow cold-starts.
        installSplashScreen().setKeepOnScreenCondition { !AppReadyState.isReady }
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        dispatch(intent)
        setContent {
            KronkTheme {
                AuthGate()
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        dispatch(intent)
    }

    // One entry point for every intent the Activity receives —
    // OAuth callback (Phase-2 kronk-auth://), Kronk deep-link
    // (https://kronk.info/*), and Share (ACTION_SEND / SEND_MULTIPLE).
    // The auth callback is captured explicitly; everything else is
    // handed to IntentEvents which the Compose ShellHost observes.
    @OptIn(kotlinx.coroutines.DelicateCoroutinesApi::class)
    private fun dispatch(intent: Intent?) {
        if (intent == null) return
        val data = intent.data
        if (data?.scheme == "kronk-auth" && data.host == "callback") {
            authViewModel.handleAuthCallback(data)
            return
        }
        val kronkIntent = IntentEvents.resolve(intent) ?: return
        // Emit on the app-scoped coroutine so the Composable side sees
        // it whether or not it's currently in composition (SharedFlow
        // with extraBufferCapacity=4 caches until observed).
        GlobalScope.launch { IntentEvents.events.emit(kronkIntent) }
    }
}
