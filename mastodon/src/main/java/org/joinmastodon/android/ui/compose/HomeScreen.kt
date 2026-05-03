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

private data class SpaceBubble(
    val label: String,
    val icon: ImageVector,
    val sizeDp: Float,
    val angleDeg: Float,
    val floatDurationMs: Int,
    val floatDelayMs: Int,
    val floatAmplitudeDp: Float,
)

private val ORBIT_RADIUS_DP = 130f
private val CENTER_BUBBLE_DP = 72f
private val CONTAINER_DP = ORBIT_RADIUS_DP * 2 + 100f

private val BUBBLES = listOf(
    SpaceBubble("Murmur",   Icons.Default.Home,          62f,   0f,   4200, 0,    8f),
    SpaceBubble("Kommons",  Icons.Default.Gavel,         62f,   90f,  5100, 600,  9f),
    SpaceBubble("Huddle",   Icons.Default.Diversity2,    62f,   180f, 4600, 1200, 9f),
    SpaceBubble("Kalendar", Icons.Default.CalendarMonth, 62f,   270f, 6200, 1800, 7f),
)

@Composable
fun KronkHomeScreen(
    displayName: String = "You",
    onSpaceTapped: (String) -> Unit = {},
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
                    onNotificationsTapped = onNotificationsTapped,
                )
            }

            ComposeBar(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 14.dp),
                onTap = onComposeTapped,
            )
        }
    }
}

@Composable
private fun OrbitLayout(
    displayName: String,
    onSpaceTapped: (String) -> Unit,
    onProfileTapped: () -> Unit,
    onNotificationsTapped: () -> Unit,
) {
    val containerDp = CONTAINER_DP.dp
    // absoluteOffset is from top-left of the Box, so profile must be
    // explicitly placed at the container center rather than using contentAlignment.
    val profileOffsetDp = ((CONTAINER_DP - CENTER_BUBBLE_DP) / 2).dp

    val floatValues = BUBBLES.mapIndexed { index, bubble ->
        val infinite = rememberInfiniteTransition(label = "float_$index")
        infinite.animateFloat(
            initialValue = 0f,
            targetValue = bubble.floatAmplitudeDp,
            animationSpec = infiniteRepeatable(
                animation = tween(
                    durationMillis = bubble.floatDurationMs,
                    easing = FastOutSlowInEasing,
                ),
                repeatMode = RepeatMode.Reverse,
                initialStartOffset = StartOffset(bubble.floatDelayMs),
            ),
            label = "floatY_$index",
        )
    }

    Box(modifier = Modifier.size(containerDp)) {
        BUBBLES.forEachIndexed { index, bubble ->
            val floatY by floatValues[index]

            val angleRad = Math.toRadians(bubble.angleDeg.toDouble() - 90.0)
            val orbitX = (cos(angleRad) * ORBIT_RADIUS_DP).toFloat()
            val orbitY = (sin(angleRad) * ORBIT_RADIUS_DP).toFloat()

            val colWidth = bubble.sizeDp + 20f
            val leftDp = (CONTAINER_DP / 2 + orbitX - colWidth / 2).dp
            val topDp = (CONTAINER_DP / 2 + orbitY - bubble.sizeDp / 2).dp

            SpaceBubbleItem(
                bubble = bubble,
                floatYDp = floatY,
                modifier = Modifier.absoluteOffset(x = leftDp, y = topDp),
                onClick = { onSpaceTapped(bubble.label) },
            )
        }

        // Profile rendered last (on top), offset to container center
        ProfileBubble(
            modifier = Modifier.absoluteOffset(x = profileOffsetDp, y = profileOffsetDp),
            displayName = displayName,
            onClick = onProfileTapped,
            onNotificationsClick = onNotificationsTapped,
        )
    }
}

@Composable
private fun ProfileBubble(
    modifier: Modifier = Modifier,
    displayName: String,
    onClick: () -> Unit,
    onNotificationsClick: () -> Unit,
) {
    val initial = displayName.firstOrNull()?.uppercaseChar()?.toString() ?: "?"

    Box(modifier = modifier.size(CENTER_BUBBLE_DP.dp)) {
        // Profile circle
        Box(
            modifier = Modifier
                .fillMaxSize()
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

        // Notification badge — top-right corner of profile circle
        Box(
            modifier = Modifier
                .size(26.dp)
                .align(Alignment.TopEnd)
                .offset(x = 6.dp, y = (-6).dp)
                .clip(CircleShape)
                .background(KronkColors.ComposeBg)
                .border(1.dp, KronkColors.BubbleBorder, CircleShape)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onNotificationsClick,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Default.Notifications,
                contentDescription = "Notifications",
                tint = KronkColors.Accent,
                modifier = Modifier.size(13.dp),
            )
            Box(
                modifier = Modifier
                    .size(7.dp)
                    .clip(CircleShape)
                    .background(KronkColors.NotifRed)
                    .align(Alignment.TopEnd)
                    .offset(x = 2.dp, y = (-2).dp),
            )
        }
    }
}

@Composable
private fun SpaceBubbleItem(
    bubble: SpaceBubble,
    floatYDp: Float,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val sizeDp = bubble.sizeDp.dp
    val bubbleBrush = Brush.radialGradient(
        colors = listOf(KronkColors.BubbleFill1, KronkColors.BubbleFill2),
    )

    Column(
        modifier = modifier
            .width(bubble.sizeDp.dp + 20.dp)
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
                imageVector = bubble.icon,
                contentDescription = bubble.label,
                tint = KronkColors.Accent,
                modifier = Modifier.size(26.dp),
            )
        }

        Spacer(modifier = Modifier.height(5.dp))

        Text(
            text = bubble.label,
            color = KronkColors.TextPrimary.copy(alpha = 0.82f),
            fontSize = 10.sp,
            textAlign = TextAlign.Center,
            maxLines = 1,
        )
    }
}

@Composable
private fun ComposeBar(modifier: Modifier = Modifier, onTap: () -> Unit) {
    Row(
        modifier = modifier
            .height(48.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(KronkColors.ComposeBg)
            .border(1.dp, KronkColors.BubbleBorder, RoundedCornerShape(24.dp))
            .clickable(onClick = onTap)
            .padding(horizontal = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "✦",
            color = KronkColors.Accent.copy(alpha = 0.7f),
            fontSize = 14.sp,
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = "What's on your mind?",
            color = KronkColors.TextMuted.copy(alpha = 0.7f),
            fontSize = 14.sp,
            modifier = Modifier.weight(1f),
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
