package info.kronk.core.designsystem.primitive

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import info.kronk.core.designsystem.theme.KronkTheme
import info.kronk.core.designsystem.theme.durationMediumMs

// The shared tab idiom for every tabbed surface in Kronk. Mirrors the
// web's <MembraneNav>: a hairline wire runs under the row, with a
// moving light-pool underneath the active tab.
//
// Consumers pass a list of items, the currently-selected index, and a
// callback. Each item renders its label via `label(item)`. The pool
// glides between tab centres when `selected` changes.

@Composable
fun <T> MembraneNav(
    items: List<T>,
    selected: T,
    onSelect: (T) -> Unit,
    label: (T) -> String,
    modifier: Modifier = Modifier,
) {
    if (items.isEmpty()) return
    val selectedIndex = items.indexOf(selected).coerceAtLeast(0)
    val motion = KronkTheme.motion
    val animatedPosition by animateFloatAsState(
        targetValue = selectedIndex.toFloat(),
        animationSpec = androidx.compose.animation.core.tween(
            durationMillis = motion.durationMediumMs,
            easing = motion.easeOut,
        ),
        label = "MembraneNavPool",
    )
    val poolColor = KronkTheme.colors.purpleBright
    val wireColor = KronkTheme.colors.borderSubtle
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(44.dp)
            .drawBehind {
                // Hairline wire — 1dp equivalent line just below the
                // baseline. Runs the full row width.
                val wireY = size.height - 1f
                drawRect(
                    color = wireColor,
                    topLeft = Offset(0f, wireY),
                    size = Size(size.width, 1f),
                )
                // Moving light pool — a soft radial gradient centred
                // under the active tab. Width per tab is size.width /
                // items.size; centre glides via animatedPosition.
                if (items.isNotEmpty()) {
                    val tabWidth = size.width / items.size
                    val centreX = tabWidth * animatedPosition + tabWidth / 2f
                    val poolRadius = tabWidth * 0.55f
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                poolColor.copy(alpha = 0.32f),
                                poolColor.copy(alpha = 0f),
                            ),
                            center = Offset(centreX, size.height - 1f),
                            radius = poolRadius,
                        ),
                        radius = poolRadius,
                        center = Offset(centreX, size.height - 1f),
                    )
                }
            },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        items.forEachIndexed { index, item ->
            MembraneNavItem(
                label = label(item),
                selected = index == selectedIndex,
                onClick = { onSelect(item) },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun RowScope.MembraneNavItem(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = if (selected) {
                KronkTheme.colors.textPrimary
            } else {
                KronkTheme.colors.textMuted
            },
        )
    }
}

// Private helper to keep dp maths tidy where needed.
@Suppress("unused")
private fun androidx.compose.ui.unit.Dp.raw(): Float = value
