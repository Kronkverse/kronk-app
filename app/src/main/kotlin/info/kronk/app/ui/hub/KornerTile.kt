package info.kronk.app.ui.hub

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import info.kronk.core.designsystem.primitive.kornerIconRes
import info.kronk.core.designsystem.theme.KronkTheme
import info.kronk.core.network.dto.KornerDto

// One korner card in the Hub grid. Structure mirrors the web's tile
// (docs/spaces/hub.md § anatomy): centred icon, name below, hairline
// purple border on an elevated surface. Tile fills its grid cell as a
// square; the grid enforces column count.
//
// Icon uses the semantic `tileGlyphOn` colour (a soft lavender in dark
// theme, matching --semantic-tile-glyph-on: #8c7dff in _tokens.scss).
// Unread badge (top-right waving-hand) lands in a follow-up.

@Composable
fun KornerTile(
    korner: KornerDto,
    onClick: (KornerDto) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = KronkTheme.colors
    val shape = KronkTheme.shapes.large
    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clip(shape)
            .background(colors.surfaceElevated)
            .border(width = 1.dp, color = colors.borderDefault, shape = shape)
            .clickable { onClick(korner) }
            .padding(12.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(
                painter = painterResource(kornerIconRes(korner.icon?.material)),
                contentDescription = null,
                tint = colors.tileGlyphOn,
                modifier = Modifier.size(36.dp),
            )
            Text(
                text = korner.name,
                style = MaterialTheme.typography.titleSmall,
                color = colors.textPrimary,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
    }
}
