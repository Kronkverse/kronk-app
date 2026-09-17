package info.kronk.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import info.kronk.app.ui.WelcomePane
import info.kronk.core.designsystem.theme.KronkTheme

// Composition root. Sets edge-to-edge, wraps everything in KronkTheme,
// and hands off to the first screen. Phase 2+ adds navigation; for the
// smoke test the welcome pane is the whole app.

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            KronkTheme {
                WelcomePane()
            }
        }
    }
}
