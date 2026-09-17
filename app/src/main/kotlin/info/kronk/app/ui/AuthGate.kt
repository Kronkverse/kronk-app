package info.kronk.app.ui

import androidx.compose.runtime.Composable
import info.kronk.app.ui.shell.ShellHost

// Root Composable. With the WebView shell, the web app itself owns
// sign-in: if the user's WebView has no session cookie, `/home` (the
// initial pillar) redirects to Kronk's own /auth/sign_in form. After
// sign-in the session cookie persists across app restarts, so this
// stays a thin wrapper.
//
// The native OAuth flow from Phase 2 (AuthViewModel / AuthStorage /
// SignInScreen) still compiles but isn't mounted here. It stays around
// for the future push-notification registration (server needs an OAuth
// token to associate an FCM/UnifiedPush endpoint with the account).

@Composable
fun AuthGate() {
    ShellHost()
}
