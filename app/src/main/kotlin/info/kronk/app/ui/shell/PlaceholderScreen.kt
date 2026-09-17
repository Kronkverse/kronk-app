package info.kronk.app.ui.shell

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import info.kronk.app.R
import info.kronk.core.designsystem.theme.KronkTheme

// Kronk-branded "coming soon" screen for a pillar whose real content
// hasn't landed yet. Shows the pillar's own icon at hero size in
// Kronk-purple, the pillar's name in Liberation Serif, and a short
// strap. Deliberately quiet — the point is that the shell feels like
// Kronk immediately, not that this specific pillar has anything to say.

@Composable
fun PlaceholderScreen(pillar: PillarKey, modifier: Modifier = Modifier) {
    val colors = KronkTheme.colors
    Surface(
        modifier = modifier.fillMaxSize(),
        color = colors.surfacePrimary,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(
                painter = painterResource(pillar.iconRes),
                contentDescription = null,
                tint = colors.purpleBright,
                modifier = Modifier.size(72.dp),
            )
            Text(
                text = stringResource(pillar.labelRes),
                style = MaterialTheme.typography.displaySmall,
                color = colors.textPrimary,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 20.dp),
            )
            Text(
                text = stringResource(R.string.pillar_coming_soon),
                style = MaterialTheme.typography.bodyMedium,
                color = colors.textMuted,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
    }
}

