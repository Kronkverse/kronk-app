package info.kronk.core.designsystem.primitive

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import info.kronk.core.designsystem.theme.KronkTheme

// Back pill. Shown at the top of every korner screen — a rounded
// capsule with a back arrow, the korner's glyph, and its name. Tap
// pops back to whichever screen it came from.
//
// Mirror of the web's <SpaceBadge> primitive. Kept minimal for now:
// icon + label + onClick; no korner-slug resolution (upcoming
// KornerGlyph does that lookup).

@Composable
fun SpaceBadge(
    icon: ImageVector,
    label: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .clickable(onClick = onBack)
            .background(
                color = KronkTheme.colors.surfaceElevated,
                shape = KronkTheme.shapes.round,
            )
            .border(
                width = 1.dp,
                color = KronkTheme.colors.borderDefault,
                shape = KronkTheme.shapes.round,
            )
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        // The back-arrow glyph is a Material Symbols "arrow_back" —
        // consumer supplies the ImageVector so we don't hardcode a
        // dep on material-icons-extended here. Placeholder in the
        // interim: the caller's `icon` argument doubles as the badge
        // glyph, and the back-arrow row prefix comes from the caller.
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = KronkTheme.colors.purpleBright,
            modifier = Modifier.size(16.dp),
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = KronkTheme.colors.textPrimary,
        )
    }
}

// The dot separator visual — a small circle used elsewhere in the
// space-nav row (between title and view-picker). Exposed for
// consumers that need it without exporting a whole SpaceNav layout.
@Composable
fun SpaceNavDot(modifier: Modifier = Modifier) {
    androidx.compose.foundation.layout.Spacer(
        modifier = modifier
            .size(4.dp)
            .background(KronkTheme.colors.borderDefault, CircleShape),
    )
}

@Composable
internal fun SpaceBadgeSpacer(width: androidx.compose.ui.unit.Dp = 8.dp) {
    androidx.compose.foundation.layout.Spacer(modifier = Modifier.width(width))
}
