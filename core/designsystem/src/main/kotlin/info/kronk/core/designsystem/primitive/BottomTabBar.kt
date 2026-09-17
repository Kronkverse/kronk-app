package info.kronk.core.designsystem.primitive

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Surface
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.unit.dp
import info.kronk.core.designsystem.theme.KronkTheme

// Mobile bottom tab-bar — the Kronk mobile treatment for the primary
// personal surfaces (Me / Home / AWAWB / Hub / Nudges).
//
// Mirrors _kronk_chrome.scss `.hub-switcher--bottom`:
//   - full-width flex row, black background, 1.5dp purple-accent top border,
//   - each pillar is a rounded (radius-medium) tile,
//   - active pillar is filled with purple-accent + white glyph,
//   - resting pillar is a soft off-white glyph on black,
//   - the whole bar respects the system navigation-bar inset (Android's
//     `env(safe-area-inset-bottom)` equivalent).
//
// Visual language is ICONS ONLY (Tal 2026-08-28 "remove the words written
// on the bottom nav hub, that's unnecessary"). Each pillar accepts an
// optional trailing `badge` Composable slot which stacks in the top-right
// of the tile — that's how Nudges renders its WavingHandBadge for unread
// activity.
//
// This primitive is generic over the item type: the app-level shell
// declares its own PillarKey enum and passes the list of tabs with icons
// + click handlers. The primitive owns no route or state.

data class BottomTabItem(
    val icon: Painter,
    val contentDescription: String,
    val selected: Boolean,
    val onSelect: () -> Unit,
    val badge: (@Composable () -> Unit)? = null,
)

@Composable
fun BottomTabBar(
    items: List<BottomTabItem>,
    modifier: Modifier = Modifier,
) {
    if (items.isEmpty()) return
    val colors = KronkTheme.colors
    // Bar background: pure black per SCSS (rgb(0 0 0)). Kept theme-invariant
    // — the mobile bar reads as chrome the whole app rides above, not a
    // surface that reflows with light/dark.
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = Color.Black,
        contentColor = colors.textPrimary,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                // 1.5dp purple-accent top border, drawn via a background stripe
                // above the row's own padding. Keeps the border sharp under
                // safe-area padding below.
                .background(colors.purpleAccent)
                .padding(top = 1.5.dp)
                .background(Color.Black)
                // Safe-area inset for the phone gesture bar / nav-bar.
                .padding(bottomInset())
                .padding(horizontal = 4.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            items.forEach { item ->
                BottomTabPillar(item = item, modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun RowScope.BottomTabPillar(
    item: BottomTabItem,
    modifier: Modifier = Modifier,
) {
    val colors = KronkTheme.colors
    // Resting tint mirrors the web's color-mix(text-on-accent 75%,
    // purple-accent 25%) — mostly white with a hint of purple so the icon
    // reads as inactive-but-present against the black bar. Compose has no
    // color-mix; we approximate by lerping toward purple-accent.
    val restingTint = remember(colors.textPrimary, colors.purpleAccent) {
        lerp(colors.textPrimary, colors.purpleAccent, 0.25f)
    }
    val tileShape = KronkTheme.shapes.medium
    Box(
        modifier = modifier
            .clip(tileShape)
            .background(if (item.selected) colors.purpleAccent else Color.Transparent)
            .clickable(onClick = item.onSelect)
            .padding(horizontal = 8.dp, vertical = 7.dp),
        contentAlignment = Alignment.Center,
    ) {
        val iconTint = if (item.selected) colors.textOnAccent else restingTint
        CompositionLocalProvider(LocalContentColor provides iconTint) {
            Icon(
                painter = item.icon,
                contentDescription = item.contentDescription,
                tint = iconTint,
                modifier = Modifier.size(22.dp),
            )
        }
        // Badge slot — Nudges puts its unread hand-badge here. The slot is
        // absolutely positioned in the top-right corner of the tile so it
        // never displaces the icon.
        item.badge?.let { badgeContent ->
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 2.dp, end = 2.dp),
            ) {
                badgeContent()
            }
        }
    }
}

@Composable
private fun bottomInset(): PaddingValues =
    WindowInsets.navigationBars.asPaddingValues()

// Small local color lerp so we don't pull in AndroidX Compose's Color.lerp
// (which is fine, but avoiding one more dep for one call).
private fun lerp(from: Color, to: Color, t: Float): Color {
    val u = t.coerceIn(0f, 1f)
    return Color(
        red = from.red + (to.red - from.red) * u,
        green = from.green + (to.green - from.green) * u,
        blue = from.blue + (to.blue - from.blue) * u,
        alpha = from.alpha + (to.alpha - from.alpha) * u,
    )
}
