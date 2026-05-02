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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.cos
import kotlin.math.sin

private data class SpaceBubble(
    val label: String,
    val icon: ImageVector,
    val sizeDp: Float,      // circle diameter in dp
    val angleDeg: Float,    // 0 = top, clockwise
    val floatDurationMs: Int,
    val floatDelayMs: Int,
    val floatAmplitudeDp: Float,
)

private val ORBIT_RADIUS_DP = 140f
private val CONTAINER_DP = ORBIT_RADIUS_DP * 2 + 80f  // padding for largest bubble + label

private val BUBBLES = listOf(
    SpaceBubble("Murmur",      Icons.Default.Forum,           62f,   0f,   4200, 0,    8f),
    SpaceBubble("Messages",    Icons.Default.MailOutline,     40f,   45f,  5800, 300,  6f),
    SpaceBubble("Kommons",     Icons.Default.Groups,          62f,   90f,  5100, 600,  9f),
    SpaceBubble("Kalendar",    Icons.Default.CalendarMonth,   50f,   135f, 6200, 900,  7f),
    SpaceBubble("Orbit",       Icons.Default.Public,          50f,   180f, 4800, 1200, 8f),
    SpaceBubble("Marketplace", Icons.Default.Storefront,      50f,   225f, 5500, 1500, 6f),
    SpaceBubble("Huddle",      Icons.Default.RecordVoiceOver, 62f,   270f, 4600, 1800, 9f),
    SpaceBubble("More",        Icons.Default.MoreHoriz,       40f,   315f, 5300, 2400, 7f),
)

@Composable
fun KronkHomeScreen(
    displayName: String = "Tal",
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
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            TopBar(
                displayName = displayName,
                onNotificationsTapped = onNotificationsTapped,
                onProfileTapped = onProfileTapped,
            )

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                OrbitLayout(onSpaceTapped = onSpaceTapped)
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                ComposeBar(onTap = onComposeTapped)
                TabBar()
            }
        }
    }
}

@Composable
private fun TopBar(
    displayName: String,
    onNotificationsTapped: () -> Unit,
    onProfileTapped: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Good evening,",
                color = KronkColors.TextMuted,
                fontSize = 13.sp,
                fontWeight = FontWeight.Normal,
            )
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = displayName,
                    color = KronkColors.TextPrimary,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                // Decorative Ȣ glyph in accent serif
                Text(
                    text = " Ȣ",
                    color = KronkColors.Accent,
                    fontSize = 22.sp,
                    fontFamily = FontFamily.Serif,
                )
            }
        }

        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .border(1.dp, KronkColors.BubbleBorder, CircleShape)
                .background(KronkColors.ComposeBg)
                .clickable(onClick = onNotificationsTapped),
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

        Spacer(modifier = Modifier.width(12.dp))

        Box(
            modifier = Modifier
                .size(44.dp)
                .drawBehind {
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                KronkColors.BrandLight.copy(alpha = 0.5f),
                                Color.Transparent,
                            ),
                            radius = size.minDimension,
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
                .clickable(onClick = onProfileTapped),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = displayName.first().uppercase(),
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
private fun OrbitLayout(onSpaceTapped: (String) -> Unit) {
    // Fixed-size container; bubbles are positioned via absoluteOffset
    // from the container's top-left corner.
    val containerDp = CONTAINER_DP.dp
    val center = (CONTAINER_DP / 2).dp  // center of the container square

    // Build float animations outside the loop (rules of hooks require stable call count)
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
        // Faint center dot
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(KronkColors.Accent.copy(alpha = 0.2f))
                .align(Alignment.Center),
        )

        BUBBLES.forEachIndexed { index, bubble ->
            val floatY by floatValues[index]

            val angleRad = Math.toRadians(bubble.angleDeg.toDouble() - 90.0)
            val orbitX = (cos(angleRad) * ORBIT_RADIUS_DP).toFloat()
            val orbitY = (sin(angleRad) * ORBIT_RADIUS_DP).toFloat()

            // Place so that the CIRCLE center sits at (center + orbitX, center + orbitY).
            // The Column width is bubble.sizeDp + 20dp (label padding), so x offset:
            //   center + orbitX - columnWidth/2
            // Circle sits at top of column, so y offset:
            //   center + orbitY - bubble.sizeDp/2
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
            val iconSize = when {
                bubble.sizeDp >= 60f -> 26.dp
                bubble.sizeDp >= 50f -> 22.dp
                else -> 18.dp
            }
            Icon(
                imageVector = bubble.icon,
                contentDescription = bubble.label,
                tint = KronkColors.Accent,
                modifier = Modifier.size(iconSize),
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
private fun ComposeBar(onTap: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
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
            fontFamily = FontFamily.Serif,
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
private fun TabBar() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .clip(RoundedCornerShape(26.dp))
            .background(KronkColors.TabBarBg)
            .border(1.dp, KronkColors.BubbleBorder, RoundedCornerShape(26.dp))
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
        TabItem(label = "Home",  icon = Icons.Default.Home,     active = true)
        TabItem(label = "Focus", icon = Icons.Default.FilterCenterFocus, active = false)
        TabItem(label = "You",   icon = Icons.Default.Person,   active = false)
    }
}

@Composable
private fun RowScope.TabItem(label: String, icon: ImageVector, active: Boolean) {
    val tint = if (active) KronkColors.Accent else KronkColors.TextMuted.copy(alpha = 0.5f)
    Column(
        modifier = Modifier
            .weight(1f)
            .clip(RoundedCornerShape(20.dp))
            .clickable {}
            .padding(vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = tint,
            modifier = Modifier.size(20.dp),
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(text = label, color = tint, fontSize = 10.sp)
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
