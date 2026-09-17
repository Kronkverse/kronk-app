package info.kronk.app.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import info.kronk.app.R
import info.kronk.core.designsystem.primitive.Stage
import info.kronk.core.designsystem.primitive.StageArchetype
import info.kronk.core.designsystem.theme.KronkTheme

// Kronk 2.0 first-run welcome pane — Phase 1 smoke test.
//
// This composable is the whole app right now. It proves the aesthetic
// system flows end-to-end:
//   - KronkTheme applies (surface + text colours),
//   - the Stage.Column primitive lays out (capped column, scrolling
//     content, safe padding),
//   - the bespoke Kronk rose SVG renders as a VectorDrawable and
//     picks up KronkTheme.colors.purpleBright via ColorFilter.
//
// Copy matches the web walkthrough's "Welcome Home" opener bubble
// (kronk/app/javascript/mastodon/components/walkthrough/steps.tsx —
// intro/welcome step). Kept in strings.xml so future localisation
// works out of the box.

@Composable
fun WelcomePane(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Stage(archetype = StageArchetype.Column) {
            Column(
                modifier = Modifier.padding(vertical = 48.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_rose),
                    contentDescription = null,
                    colorFilter = ColorFilter.tint(KronkTheme.colors.purpleBright),
                    modifier = Modifier.size(96.dp),
                )
                Text(
                    text = stringResource(id = R.string.welcome_title),
                    style = MaterialTheme.typography.headlineMedium,
                    color = KronkTheme.colors.textPrimary,
                    textAlign = TextAlign.Center,
                )
                Text(
                    text = stringResource(id = R.string.welcome_body).trim(),
                    style = MaterialTheme.typography.bodyLarge,
                    color = KronkTheme.colors.textSecondary,
                    textAlign = TextAlign.Start,
                )
            }
        }
    }
}
