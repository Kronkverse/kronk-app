package info.kronk.app.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import info.kronk.app.ui.shell.ShellHost
import info.kronk.feature.auth.data.SignInState
import info.kronk.feature.auth.ui.AuthViewModel
import info.kronk.feature.auth.ui.SignInScreen

// Top-level router. Reads the auth session state and mounts:
//   SignedOut → SignInScreen (from :feature:auth)
//   SignedIn  → ShellHost    (5-pillar bottom nav + per-tab content)
//
// AuthGate stays the outer guard so nav-graph work runs only after
// sign-in and can trust `session is SignedIn`.

@Composable
fun AuthGate() {
    val vm: AuthViewModel = hiltViewModel()
    val session by vm.session.collectAsStateWithLifecycle()
    when (session) {
        SignInState.SignedOut -> SignInScreen(viewModel = vm)
        is SignInState.SignedIn -> ShellHost()
    }
}
