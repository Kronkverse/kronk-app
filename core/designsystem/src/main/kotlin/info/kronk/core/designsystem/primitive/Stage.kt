package info.kronk.core.designsystem.primitive

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import info.kronk.core.designsystem.theme.KronkTheme

// Content container. Mirrors the web's three stage archetypes:
//
//   Fill   — full-bleed (canvases, maps, starfield). No inner padding,
//            no max-width cap.
//   Column — capped reading column (default). Max width ~38.75rem =
//            620dp on the web; mirrored here. Vertical scroll only.
//   Grid   — adaptive tile grid. minmax(140dp, 1fr) on the web; we
//            use GridCells.Adaptive(140dp). Vertical-only on phone.
//
// Every korner screen mounts inside a Stage — the invariant that no
// horizontal scroll ever appears on mobile lives here.

enum class StageArchetype { Fill, Column, Grid }

@Composable
fun Stage(
    modifier: Modifier = Modifier,
    archetype: StageArchetype = StageArchetype.Column,
    contentPadding: PaddingValues = PaddingValues(16.dp),
    content: @Composable ColumnScope.() -> Unit,
) {
    when (archetype) {
        StageArchetype.Fill -> StageFill(modifier, content)
        StageArchetype.Column -> StageColumn(modifier, contentPadding, content)
        StageArchetype.Grid -> error(
            "Stage(archetype = Grid) is a lazy-grid layout — call StageGrid(...) " +
                "with a LazyGridScope content lambda instead.",
        )
    }
}

// Grid variant — separate entry point because LazyVerticalGrid needs a
// LazyGridScope, not a ColumnScope. Keeps the type-signature honest.
@Composable
fun StageGrid(
    modifier: Modifier = Modifier,
    minCellWidth: androidx.compose.ui.unit.Dp = 140.dp,
    contentPadding: PaddingValues = PaddingValues(16.dp),
    content: LazyGridScope.() -> Unit,
) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minCellWidth),
        modifier = modifier
            .fillMaxSize()
            .background(KronkTheme.colors.surfacePrimary),
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        content = content,
    )
}

@Composable
private fun StageFill(
    modifier: Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(KronkTheme.colors.surfacePrimary),
        content = content,
    )
}

@Composable
private fun StageColumn(
    modifier: Modifier,
    contentPadding: PaddingValues,
    content: @Composable ColumnScope.() -> Unit,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(KronkTheme.colors.surfacePrimary),
        contentAlignment = Alignment.TopCenter,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                // 620dp mirrors the web's 38.75rem reading column cap.
                // On phones this collapses to fillMaxWidth; on tablets
                // + foldables the column stays a comfortable reading
                // width instead of stretching edge-to-edge.
                .widthIn(max = 620.dp)
                .verticalScroll(rememberScrollState())
                .padding(contentPadding),
            content = content,
        )
    }
}

// Explicit alias for readability when a screen wants a full-bleed
// Stage with no wrapper Column (a bare Box scope).
@Composable
fun StageFillBox(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(KronkTheme.colors.surfacePrimary),
        content = content,
    )
}
