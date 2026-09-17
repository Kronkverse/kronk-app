package info.kronk.core.designsystem.primitive

import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import info.kronk.core.designsystem.theme.KronkTheme

// Renders the icon for a korner. Mirror of the web's <KornerGlyph>
// component, which reads `icon.material` from the korner's manifest
// and looks up the Material Symbol.
//
// On Android we don't yet have manifest loading — the KornerRegistry
// module lands in Phase 4 (see docs/rebuild/plan.md). Until then,
// consumers pass an ImageVector directly. When the registry lands,
// this primitive gains an overload `KornerGlyph(slug: String, ...)`
// that resolves the icon from the manifest cache.

@Composable
fun KornerGlyph(
    icon: ImageVector,
    modifier: Modifier = Modifier,
    tint: Color = KronkTheme.colors.purpleBright,
    size: Dp = 24.dp,
    contentDescription: String? = null,
) {
    Icon(
        imageVector = icon,
        contentDescription = contentDescription,
        tint = tint,
        modifier = modifier.size(size),
    )
}
