package info.kronk.feature.home.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import info.kronk.core.network.dto.StatusDto
import info.kronk.feature.home.data.FeedScope
import info.kronk.feature.home.data.HomeRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

// State for the Home screen. Loads one page on init; feed-scope
// toggle re-loads. No pagination yet.

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repo: HomeRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(HomeUiState())
    val state: StateFlow<HomeUiState> = _state.asStateFlow()

    init {
        load(FeedScope.Mates)
    }

    fun selectScope(scope: FeedScope) {
        if (_state.value.scope == scope) return
        load(scope)
    }

    fun refresh() {
        load(_state.value.scope)
    }

    private fun load(scope: FeedScope) {
        _state.update { it.copy(scope = scope, loading = true, error = null) }
        viewModelScope.launch {
            runCatching { repo.homeTimeline(scope) }
                .onSuccess { statuses ->
                    _state.update {
                        it.copy(statuses = statuses, loading = false)
                    }
                }
                .onFailure { t ->
                    _state.update {
                        it.copy(loading = false, error = t.message ?: "Failed to load")
                    }
                }
        }
    }
}

data class HomeUiState(
    val scope: FeedScope = FeedScope.Mates,
    val statuses: List<StatusDto> = emptyList(),
    val loading: Boolean = true,
    val error: String? = null,
)
