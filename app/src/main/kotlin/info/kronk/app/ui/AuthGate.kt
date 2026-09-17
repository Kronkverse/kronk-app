package info.kronk.app.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import info.kronk.feature.auth.data.SignInState
import info.kronk.feature.auth.ui.AuthViewModel
import info.kronk.feature.auth.ui.SignInScreen
import info.kronk.feature.home.ui.HomeScreen

// Top-level router. Reads the auth session state and mounts:
//   SignedOut → SignInScreen (from :feature:auth)
//   SignedIn  → HomeScreen   (from :feature:home)
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
        is SignInState.SignedIn -> HomeScreen()
    }
}
