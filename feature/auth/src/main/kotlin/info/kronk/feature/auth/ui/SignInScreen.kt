package info.kronk.feature.auth.ui

import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import info.kronk.core.designsystem.theme.KronkTheme

// Signed-out screen. Kronk-purple pane with a single "Sign in" button
// that launches Chrome Custom Tab to the OAuth authorize URL. The
// Custom Tab toolbar is themed with the Kronk surface colour so it
// reads as part of the app.

@Composable
fun SignInScreen(
    modifier: Modifier = Modifier,
    viewModel: AuthViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val launchUrl by viewModel.launch.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()
    val toolbarArgb = KronkTheme.colors.surfaceElevated.toArgb()

    // One-shot: when the VM emits a URL, launch Custom Tab, then clear.
    LaunchedEffect(launchUrl) {
        val url = launchUrl ?: return@LaunchedEffect
        val intent = CustomTabsIntent.Builder()
            .setDefaultColorSchemeParams(
                androidx.browser.customtabs.CustomTabColorSchemeParams.Builder()
                    .setToolbarColor(toolbarArgb)
                    .build(),
            )
            .build()
        intent.launchUrl(context, url.toUri())
        viewModel.onLaunchHandled()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = "Kronk",
            style = MaterialTheme.typography.displayMedium,
            color = KronkTheme.colors.textPrimary,
        )
        Text(
            text = "A place chosen with care.",
            modifier = Modifier.padding(top = 8.dp, bottom = 32.dp),
            style = MaterialTheme.typography.bodyLarge,
            color = KronkTheme.colors.textSecondary,
            textAlign = TextAlign.Center,
        )
        Button(
            onClick = viewModel::beginSignIn,
        ) {
            Text("Sign in")
        }
        val currentError = error
        if (currentError != null) {
            Text(
                text = currentError,
                modifier = Modifier.padding(top = 16.dp),
                style = MaterialTheme.typography.bodySmall,
                color = KronkTheme.colors.warningRed,
                textAlign = TextAlign.Center,
            )
        }
    }
}
