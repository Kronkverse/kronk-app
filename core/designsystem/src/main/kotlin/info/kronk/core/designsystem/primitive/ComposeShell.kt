package info.kronk.core.designsystem.primitive

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import info.kronk.core.designsystem.theme.KronkTheme

// Modal shell for korner composers. Mirror of the web's <ComposeShell>:
// a full-height sheet with a title bar (title + close), a scrolling
// body, and an optional footer row (typically the Post/Send button).
//
// Every korner's composer route (`/hub/<slug>/composer` on the web)
// mounts inside this shell — the shell owns the layout, the composer
// owns the fields.

@Composable
fun ComposeShell(
    title: String,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    footer: (@Composable RowScope.() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(KronkTheme.colors.surfacePrimary),
    ) {
        ComposeShellHeader(title = title, onClose = onClose)
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            content = content,
        )
        if (footer != null) {
            ComposeShellFooter(content = footer)
        }
    }
}

@Composable
private fun ComposeShellHeader(title: String, onClose: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(KronkTheme.colors.surfaceElevated)
            .padding(horizontal = 16.dp)
            .height(52.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = KronkTheme.colors.textPrimary,
        )
        // Close hit-target. Consumers can swap the ✕ glyph later — for
        // now a text × so we don't depend on material-icons-extended
        // in :core:designsystem.
        Box(
            modifier = Modifier
                .clickable(onClick = onClose)
                .padding(8.dp),
        ) {
            Text(
                text = "×",
                style = MaterialTheme.typography.titleLarge,
                color = KronkTheme.colors.textSecondary,
            )
        }
    }
}

@Composable
private fun ComposeShellFooter(content: @Composable RowScope.() -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(KronkTheme.colors.surfaceElevated)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.End,
        content = content,
    )
}
