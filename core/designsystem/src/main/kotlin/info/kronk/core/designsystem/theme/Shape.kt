package info.kronk.core.designsystem.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.dp

// Radius scale mirrored from tokens.yaml. The web has four values —
// small (6), medium (10), large (16), round (999) — while Material 3
// takes five shape slots. Small maps to both `extraSmall` and `small`,
// large to both `large` and `extraLarge`, so the closest-Material-slot
// choice for a component picks up a Kronk radius. Pill-shaped surfaces
// (buttons, badges) opt out of `Shapes` and use `KronkShapes.round`
// directly.

@Immutable
data class KronkShapes(
    val small: RoundedCornerShape = RoundedCornerShape(6.dp),
    val medium: RoundedCornerShape = RoundedCornerShape(10.dp),
    val large: RoundedCornerShape = RoundedCornerShape(16.dp),
    val round: RoundedCornerShape = RoundedCornerShape(999.dp),
) {
    fun toMaterial(): Shapes = Shapes(
        extraSmall = small,
        small = small,
        medium = medium,
        large = large,
        extraLarge = large,
    )
}
