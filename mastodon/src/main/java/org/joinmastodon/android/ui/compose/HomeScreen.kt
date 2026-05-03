package org.joinmastodon.android.ui.compose

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.cos
import kotlin.math.sin

// Icon for each space — exhaustive when forces an update here whenever KronkSpace grows.
private val KronkSpace.icon: ImageVector get() = when (this) {
    KronkSpace.MURMUR   -> Icons.Default.Home
    KronkSpace.KOMMONS  -> Icons.Default.Gavel
    KronkSpace.HUDDLE   -> Icons.Default.Diversity2
    KronkSpace.KALENDAR -> Icons.Default.CalendarMonth
}

// Orbit placement config — visual positioning only, independent of space identity.
private data class OrbitEntry(
    val space: KronkSpace,
    val angleDeg: Float,
    val sizeDp: Float = 62f,
    val floatDurationMs: Int,
    val floatDelayMs: Int,
    val floatAmplitudeDp: Float,
)

private val ORBIT_RADIUS_DP = 130f
private val CENTER_BUBBLE_DP = 72f
private val CONTAINER_DP = ORBIT_RADIUS_DP * 2 + 100f

// To add a space: add it to KronkSpace, add its icon above, add a row here.
private val ORBIT = listOf(
    OrbitEntry(KronkSpace.MURMUR,   0f,   62f, 4200, 0,    8f),
    OrbitEntry(KronkSpace.KOMMONS,  90f,  62f, 5100, 600,  9f),
    OrbitEntry(KronkSpace.HUDDLE,   180f, 62f, 4600, 1200, 9f),
    OrbitEntry(KronkSpace.KALENDAR, 270f, 62f, 6200, 1800, 7f),
)

@Composable
fun KronkHomeScreen(
    displayName: String = "You",
    onSpaceTapped: (KronkSpace) -> Unit = {},
    onNotificationsTapped: () -> Unit = {},
    onProfileTapped: () -> Unit = {},
    onComposeTapped: () -> Unit = {},
) {
    val bgBrush = Brush.radialGradient(
        colorStops = arrayOf(
            0.0f to KronkColors.BgTop,
            0.5f to KronkColors.BgMid,
            1.0f to KronkColors.BgBottom,
        ),
        radius = 1200f,
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bgBrush),
    ) {
        StarField()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                OrbitLayout(
                    displayName = displayName,
                    onSpaceTapped = onSpaceTapped,
                    onProfileTapped = onProfileTapped,
                )
            }
        }

        // Notification bell — top-right, clear of status bar
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .statusBarsPadding()
                .padding(end = 20.dp, top = 20.dp)
                .size(40.dp)
                .clip(CircleShape)
                .border(1.dp, KronkColors.BubbleBorder, CircleShape)
                .background(KronkColors.ComposeBg)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onNotificationsTapped,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Default.Notifications,
                contentDescription = "Notifications",
                tint = KronkColors.Accent,
                modifier = Modifier.size(20.dp),
            )
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(KronkColors.NotifRed)
                    .align(Alignment.TopEnd)
                    .offset(x = 1.dp, y = (-1).dp),
            )
        }
    }
}

@Composable
private fun OrbitLayout(
    displayName: String,
    onSpaceTapped: (KronkSpace) -> Unit,
    onProfileTapped: () -> Unit,
) {
    val containerDp = CONTAINER_DP.dp
    val profileOffsetDp = ((CONTAINER_DP - CENTER_BUBBLE_DP) / 2).dp

    val floatValues = ORBIT.mapIndexed { index, entry ->
        val infinite = rememberInfiniteTransition(label = "float_$index")
        infinite.animateFloat(
            initialValue = 0f,
            targetValue = entry.floatAmplitudeDp,
            animationSpec = infiniteRepeatable(
                animation = tween(
                    durationMillis = entry.floatDurationMs,
                    easing = FastOutSlowInEasing,
                ),
                repeatMode = RepeatMode.Reverse,
                initialStartOffset = StartOffset(entry.floatDelayMs),
            ),
            label = "floatY_$index",
        )
    }

    Box(modifier = Modifier.size(containerDp)) {
        ORBIT.forEachIndexed { index, entry ->
            val floatY by floatValues[index]

            val angleRad = Math.toRadians(entry.angleDeg.toDouble() - 90.0)
            val orbitX = (cos(angleRad) * ORBIT_RADIUS_DP).toFloat()
            val orbitY = (sin(angleRad) * ORBIT_RADIUS_DP).toFloat()

            val colWidth = entry.sizeDp + 20f
            val leftDp = (CONTAINER_DP / 2 + orbitX - colWidth / 2).dp
            val topDp = (CONTAINER_DP / 2 + orbitY - entry.sizeDp / 2).dp

            OrbitBubble(
                entry = entry,
                floatYDp = floatY,
                modifier = Modifier.absoluteOffset(x = leftDp, y = topDp),
                onClick = { onSpaceTapped(entry.space) },
            )
        }

        ProfileBubble(
            modifier = Modifier.absoluteOffset(x = profileOffsetDp, y = profileOffsetDp),
            displayName = displayName,
            onClick = onProfileTapped,
        )
    }
}

@Composable
private fun OrbitBubble(
    entry: OrbitEntry,
    floatYDp: Float,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val sizeDp = entry.sizeDp.dp
    val bubbleBrush = Brush.radialGradient(
        colors = listOf(KronkColors.BubbleFill1, KronkColors.BubbleFill2),
    )

    Column(
        modifier = modifier
            .width(entry.sizeDp.dp + 20.dp)
            .offset(y = floatYDp.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(sizeDp)
                .drawBehind {
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                KronkColors.Accent.copy(alpha = 0.2f),
                                Color.Transparent,
                            ),
                            radius = size.minDimension * 0.9f,
                        ),
                    )
                }
                .clip(CircleShape)
                .background(bubbleBrush)
                .border(1.dp, KronkColors.BubbleBorder, CircleShape)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onClick,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = entry.space.icon,
                contentDescription = entry.space.displayName,
                tint = KronkColors.Accent,
                modifier = Modifier.size(30.dp),
            )
        }

        Spacer(modifier = Modifier.height(5.dp))

        Text(
            text = entry.space.displayName,
            color = KronkColors.TextPrimary.copy(alpha = 0.82f),
            fontSize = 10.sp,
            textAlign = TextAlign.Center,
            maxLines = 1,
        )
    }
}

@Composable
private fun ProfileBubble(
    modifier: Modifier = Modifier,
    displayName: String,
    onClick: () -> Unit,
) {
    val initial = displayName.firstOrNull()?.uppercaseChar()?.toString() ?: "?"

    Box(
        modifier = modifier
            .size(CENTER_BUBBLE_DP.dp)
            .drawBehind {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            KronkColors.BrandLight.copy(alpha = 0.45f),
                            Color.Transparent,
                        ),
                        radius = size.minDimension * 1.4f,
                    ),
                )
            }
            .clip(CircleShape)
            .border(2.dp, Color.White.copy(alpha = 0.85f), CircleShape)
            .background(
                Brush.radialGradient(
                    colors = listOf(KronkColors.BrandLight, KronkColors.BrandDark),
                )
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = initial,
            color = Color.White,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun StarField() {
    val stars = remember {
        listOf(
            0.08f to 0.12f, 0.22f to 0.05f, 0.65f to 0.08f,
            0.85f to 0.15f, 0.12f to 0.32f, 0.78f to 0.28f,
            0.45f to 0.04f, 0.92f to 0.42f, 0.03f to 0.58f,
            0.55f to 0.72f, 0.88f to 0.65f, 0.30f to 0.88f,
        )
    }
    Canvas(modifier = Modifier.fillMaxSize()) {
        stars.forEach { (xFrac, yFrac) ->
            drawCircle(
                color = Color(0xAAB6A3FF),
                radius = 1.8f,
                center = Offset(size.width * xFrac, size.height * yFrac),
            )
        }
    }
}
