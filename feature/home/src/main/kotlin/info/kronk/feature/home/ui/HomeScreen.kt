package info.kronk.feature.home.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import info.kronk.core.designsystem.primitive.MembraneNav
import info.kronk.core.designsystem.theme.KronkTheme
import info.kronk.feature.home.data.FeedScope

// Home screen: MembraneNav feed-scope picker at the top, LazyColumn
// of statuses below. Loading spinner while fetching; error text when
// a load fails.

@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(KronkTheme.colors.surfacePrimary),
    ) {
        MembraneNav(
            items = FeedScope.entries,
            selected = state.scope,
            onSelect = viewModel::selectScope,
            label = { it.label },
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            contentAlignment = Alignment.Center,
        ) {
            when {
                state.loading && state.statuses.isEmpty() -> {
                    CircularProgressIndicator(color = KronkTheme.colors.purpleBright)
                }
                state.error != null && state.statuses.isEmpty() -> {
                    Text(
                        text = state.error ?: "",
                        style = MaterialTheme.typography.bodyMedium,
                        color = KronkTheme.colors.warningRed,
                    )
                }
                state.statuses.isEmpty() -> {
                    Text(
                        text = "No posts yet.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = KronkTheme.colors.textSecondary,
                    )
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        items(state.statuses, key = { it.id }) { status ->
                            StatusCard(status = status)
                        }
                    }
                }
            }
        }
    }
}
