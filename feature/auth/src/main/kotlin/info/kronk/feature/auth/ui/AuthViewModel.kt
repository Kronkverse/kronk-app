package info.kronk.feature.auth.ui

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import info.kronk.feature.auth.data.AuthRepository
import info.kronk.feature.auth.data.SignInState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

// Auth state + sign-in trigger for the UI. Two flows:
//   session — the persistent SignedIn / SignedOut state from the
//             repository. Read by AuthGate to decide what to render.
//   launch  — one-shot event: when non-null, the UI should launch a
//             Custom Tab to this URL. UI clears it after launch.

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val repo: AuthRepository,
) : ViewModel() {

    val session: StateFlow<SignInState> = repo.state.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = SignInState.SignedOut,
    )

    private val _launch = MutableStateFlow<String?>(null)
    val launch: StateFlow<String?> = _launch

    // Failed sign-in surfaces here for the UI to render.
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    fun beginSignIn() {
        _error.value = null
        viewModelScope.launch {
            runCatching { repo.beginSignIn() }
                .onSuccess { _launch.value = it }
                .onFailure { _error.value = it.message ?: "Sign-in failed" }
        }
    }

    fun onLaunchHandled() {
        _launch.value = null
    }

    fun handleAuthCallback(uri: Uri) {
        viewModelScope.launch {
            runCatching { repo.handleAuthCallback(uri) }
                .onFailure { _error.value = it.message ?: "Token exchange failed" }
        }
    }

    fun signOut() {
        viewModelScope.launch { repo.signOut() }
    }
}
