package info.kronk.core.designsystem.theme

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.runtime.Immutable
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

// Motion tokens mirrored from tokens.yaml. Kronk uses three duration
// scales (fast/medium/slow) and three easings (out/inOut/spring). The
// spring easing overshoots (0.34, 1.56, 0.64, 1.0) — a bounce past the
// target — used sparingly on entrances.

@Immutable
data class KronkMotion(
    val durationFast: Duration = 120.milliseconds,
    val durationMedium: Duration = 200.milliseconds,
    val durationSlow: Duration = 400.milliseconds,
    val easeOut: Easing = CubicBezierEasing(0.16f, 1f, 0.3f, 1f),
    val easeInOut: Easing = CubicBezierEasing(0.65f, 0f, 0.35f, 1f),
    val easeSpring: Easing = CubicBezierEasing(0.34f, 1.56f, 0.64f, 1f),
)

// Duration values in milliseconds (Int) for use with Compose's
// tween/animateAsState APIs, which take Int millis, not Duration.
val KronkMotion.durationFastMs: Int get() = durationFast.inWholeMilliseconds.toInt()
val KronkMotion.durationMediumMs: Int get() = durationMedium.inWholeMilliseconds.toInt()
val KronkMotion.durationSlowMs: Int get() = durationSlow.inWholeMilliseconds.toInt()
