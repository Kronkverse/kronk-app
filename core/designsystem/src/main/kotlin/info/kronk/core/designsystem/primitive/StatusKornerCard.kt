package info.kronk.core.designsystem.primitive

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import info.kronk.core.designsystem.theme.KronkTheme

// Card frame for feed items sourced from a korner. Mirror of the
// web's <StatusKornerCard>: icon + label header, big title, summary
// body, optional action slot in the bottom-right corner.
//
// Every korner's feed card wraps this — the outer chrome (elevation,
// radius, purple accents) is invariant so cards read as a family.
// Individual card types (proposal_card, event_card, etc.) customise
// only the inner content, not the frame.

@Composable
fun StatusKornerCard(
    icon: ImageVector,
    label: String,
    title: String,
    summary: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    action: (@Composable () -> Unit)? = null,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .background(
                color = KronkTheme.colors.surfaceElevated,
                shape = KronkTheme.shapes.medium,
            )
            .border(
                width = 1.dp,
                color = KronkTheme.colors.borderSubtle,
                shape = KronkTheme.shapes.medium,
            )
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            KornerGlyph(icon = icon, size = 18.dp)
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = KronkTheme.colors.textSecondary,
            )
        }
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = KronkTheme.colors.textPrimary,
        )
        Text(
            text = summary,
            style = MaterialTheme.typography.bodyMedium,
            color = KronkTheme.colors.textSecondary,
        )
        if (action != null) {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.CenterEnd,
            ) {
                action()
            }
        }
    }
}
