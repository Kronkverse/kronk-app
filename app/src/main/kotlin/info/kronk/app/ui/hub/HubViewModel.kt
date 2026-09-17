package info.kronk.app.ui.hub

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import info.kronk.core.network.dto.KornerDto
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

// Owns the Hub grid state. Loads once on construction and again on
// explicit refresh() calls (pull-to-refresh comes later). Filters to
// `enforced && !core` so the tiles are the pluggable korners only —
// the core spaces (feed / profile / hub / nudges / settings) already
// live in the bottom nav.
//
// Sorting mirrors the web's Hub grid (spec §4): tune_in_count DESC,
// name ASC as tie-break.

data class HubUiState(
    val loading: Boolean = false,
    val error: String? = null,
    val tiles: List<KornerDto> = emptyList(),
)

@HiltViewModel
class HubViewModel @Inject constructor(
    private val repository: HubRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(HubUiState())
    val state: StateFlow<HubUiState> = _state.asStateFlow()

    init {
        load()
    }

    fun refresh() = load()

    private fun load() {
        _state.value = _state.value.copy(loading = true, error = null)
        viewModelScope.launch {
            runCatching { repository.listKorners() }
                .onSuccess { all ->
                    val tiles = all
                        .filter { it.enforced && !it.core }
                        .sortedWith(
                            compareByDescending<KornerDto> { it.tuneInCount }
                                .thenBy { it.name.lowercase() },
                        )
                    _state.value = HubUiState(loading = false, tiles = tiles)
                }
                .onFailure { t ->
                    _state.value = HubUiState(
                        loading = false,
                        error = t.message ?: "Couldn't reach Kronk",
                    )
                }
        }
    }
}
