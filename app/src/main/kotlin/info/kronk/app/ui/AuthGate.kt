package info.kronk.app.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import info.kronk.feature.auth.data.SignInState
import info.kronk.feature.auth.ui.AuthViewModel
import info.kronk.feature.auth.ui.SignInScreen

// Top-level router. Reads the auth session state and mounts:
//   SignedOut → SignInScreen (from :feature:auth)
//   SignedIn  → WelcomePane  (placeholder until Phase 2C's Home)
//
// This composable is currently the whole app's routing. When Phase 3+
// adds real navigation (nav-graph, back-stack), AuthGate stays the
// outer guard: navigation only runs inside the SignedIn branch.

@Composable
fun AuthGate() {
    val vm: AuthViewModel = hiltViewModel()
    val session by vm.session.collectAsStateWithLifecycle()
    when (session) {
        SignInState.SignedOut -> SignInScreen(viewModel = vm)
        is SignInState.SignedIn -> WelcomePane()
    }
}
