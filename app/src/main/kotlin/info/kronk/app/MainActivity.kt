package info.kronk.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import dagger.hilt.android.AndroidEntryPoint
import info.kronk.app.ui.AuthGate
import info.kronk.core.designsystem.theme.KronkTheme
import info.kronk.feature.auth.ui.AuthViewModel

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
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        handleAuthIntent(intent)
        setContent {
            KronkTheme {
                AuthGate()
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleAuthIntent(intent)
    }

    private fun handleAuthIntent(intent: Intent?) {
        val data = intent?.data ?: return
        if (data.scheme != "kronk-auth" || data.host != "callback") return
        authViewModel.handleAuthCallback(data)
    }
}
