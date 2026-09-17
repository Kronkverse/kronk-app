package info.kronk.app.ui.hub

import android.net.Uri
import androidx.browser.customtabs.CustomTabColorSchemeParams
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import info.kronk.core.common.KronkHost
import info.kronk.core.designsystem.theme.KronkTheme
import info.kronk.core.network.dto.KornerDto

// The Hub grid — every enforced pluggable korner as a tile, three per
// row. Tapping a tile launches the korner's web surface in a Chrome
// Custom Tab; that keeps the app-shell small while the web owns the
// per-korner UX. Embedded WebView per korner is a follow-up when
// deeper native/web bridging (share intents, camera, upload) earns
// its keep.

@Composable
fun HubScreen(viewModel: HubViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val colors = KronkTheme.colors
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = colors.surfacePrimary,
    ) {
        when {
            state.loading && state.tiles.isEmpty() -> LoadingBox()
            state.error != null && state.tiles.isEmpty() -> ErrorBox(state.error!!)
            else -> LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                contentPadding = PaddingValuesOf(top = 12.dp, sides = 12.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxSize(),
            ) {
                items(state.tiles, key = { it.slug }) { korner ->
                    KornerTile(
                        korner = korner,
                        onClick = { k ->
                            launchKorner(context, k, toolbarColorArgb = colors.surfaceElevated.toArgb())
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun LoadingBox() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = KronkTheme.colors.purpleBright)
    }
}

@Composable
private fun ErrorBox(message: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = KronkTheme.colors.textSecondary,
            textAlign = TextAlign.Center,
        )
    }
}

// Custom Tab launcher matching the sign-in flow's tint choice — the
// toolbar picks up surfaceElevated so it reads as part of the app,
// not a bare Chrome window.
private fun launchKorner(
    context: android.content.Context,
    korner: KornerDto,
    toolbarColorArgb: Int,
) {
    // Prefer the manifest's declared mount when present (for core
    // spaces or a korner that lives outside `/hub/<slug>`); otherwise
    // the canonical korner mount.
    val path = korner.mount ?: "/hub/${korner.slug}"
    val url = KronkHost.origin.toString().trimEnd('/') + path
    val colorScheme = CustomTabColorSchemeParams.Builder()
        .setToolbarColor(toolbarColorArgb)
        .build()
    val intent = CustomTabsIntent.Builder()
        .setDefaultColorSchemeParams(colorScheme)
        .setShowTitle(true)
        .build()
    intent.launchUrl(context, Uri.parse(url))
}

// Small helper because Compose's PaddingValues has a verbose call site
// when the four sides don't all match.
@Suppress("FunctionName")
private fun PaddingValuesOf(
    top: androidx.compose.ui.unit.Dp,
    sides: androidx.compose.ui.unit.Dp,
    bottom: androidx.compose.ui.unit.Dp,
) = androidx.compose.foundation.layout.PaddingValues(
    start = sides,
    end = sides,
    top = top,
    bottom = bottom,
)
