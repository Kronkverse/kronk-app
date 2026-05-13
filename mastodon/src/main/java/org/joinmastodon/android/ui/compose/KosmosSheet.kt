package org.joinmastodon.android.ui.compose

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.cos
import kotlin.math.sin

private val KronkSpace.icon get() = when (this) {
    KronkSpace.MURMUR   -> Icons.Default.Home
    KronkSpace.KOMMONS  -> Icons.Default.Gavel
    KronkSpace.HUDDLE   -> Icons.Default.Diversity2
    KronkSpace.KALENDAR -> Icons.Default.CalendarMonth
    KronkSpace.NUDGES   -> Icons.Default.Handshake
}

@Composable
fun KosmosSheet(
    slideOffset: Float,           // 0f = collapsed, 1f = fully expanded; updated by BottomSheetBehavior callback
    recentSpaces: List<KronkSpace>,
    allSpaces: List<KronkSpace> = KronkSpace.entries.sortedBy { it.orderWeight },
    onSpaceTapped: (KronkSpace) -> Unit,
    modifier: Modifier = Modifier,
) {
    // slideOffset < 0.15 = peek only; 0.15-0.6 = recents; > 0.6 = full kosmos
    val showRecents = slideOffset > 0.15f
    val showKosmos  = slideOffset > 0.60f

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(
                color = KronkColors.SheetBg,
                shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
            )
            .padding(top = 12.dp, bottom = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Drag handle pill
        Box(
            modifier = Modifier
                .width(40.dp)
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(KronkColors.Accent.copy(alpha = 0.3f)),
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (showKosmos) {
            KosmosFullGrid(spaces = allSpaces, onSpaceTapped = onSpaceTapped)
        } else if (showRecents) {
            RecentsRow(spaces = recentSpaces, onSpaceTapped = onSpaceTapped)
        }
    }
}

@Composable
private fun RecentsRow(
    spaces: List<KronkSpace>,
    onSpaceTapped: (KronkSpace) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
        spaces.forEach { space ->
            PlanetBubble(
                space = space,
                size = 58.dp,
                onClick = { onSpaceTapped(space) },
            )
        }
    }
}

@Composable
private fun KosmosFullGrid(
    spaces: List<KronkSpace>,
    onSpaceTapped: (KronkSpace) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
    ) {
        Text(
            text = Kosmos,
            color = KronkColors.TextPrimary,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 8.dp, bottom = 16.dp),
        )

        // 2-column grid
        val rows = spaces.chunked(2)
        rows.forEach { rowSpaces ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                rowSpaces.forEach { space ->
                    PlanetBubble(
                        space = space,
                        size = 72.dp,
                        onClick = { onSpaceTapped(space) },
                    )
                }
                // Pad the row if odd number of spaces
                if (rowSpaces.size < 2) {
                    Spacer(modifier = Modifier.width(72.dp + 20.dp))
                }
            }
        }
    }
}

@Composable
private fun PlanetBubble(
    space: KronkSpace,
    size: androidx.compose.ui.unit.Dp,
    onClick: () -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(size)
                .drawBehind {
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                space.glowColor.copy(alpha = 0.35f),
                                Color.Transparent,
                            ),
                            radius = this.size.minDimension * 0.9f,
                        ),
                    )
                }
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            space.planetColor.copy(alpha = 0.25f),
                            space.glowColor.copy(alpha = 0.15f),
                        )
                    )
                )
                .border(1.dp, space.planetColor.copy(alpha = 0.6f), CircleShape)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onClick,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = space.icon,
                contentDescription = space.displayName,
                tint = space.planetColor,
                modifier = Modifier.size(size * 0.45f),
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = space.displayName,
            color = KronkColors.TextPrimary.copy(alpha = 0.85f),
            fontSize = 11.sp,
            textAlign = TextAlign.Center,
            maxLines = 1,
        )
    }
}
