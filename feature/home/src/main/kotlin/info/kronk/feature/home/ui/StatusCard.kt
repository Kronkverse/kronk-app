package info.kronk.feature.home.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import info.kronk.core.designsystem.theme.KronkTheme
import info.kronk.core.network.dto.StatusDto

// Renders one plain status. Deliberately minimal for Phase 2C:
//   - avatar (Coil)
//   - display name + @acct
//   - stripped-to-text content
//   - reply/reblog/favourite counts
//
// Not yet: media attachments, polls, mentions/hashtag linkification,
// reply threading, timestamps as "3m ago". Each lands with its own
// consumer in later phases.

@Composable
fun StatusCard(
    status: StatusDto,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            AsyncImage(
                model = status.account.avatar,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(KronkTheme.colors.surfaceElevated),
            )
            Column {
                Text(
                    text = status.account.displayName.ifBlank { status.account.username },
                    style = MaterialTheme.typography.titleSmall,
                    color = KronkTheme.colors.textPrimary,
                )
                Text(
                    text = "@${status.account.acct}",
                    style = MaterialTheme.typography.bodySmall,
                    color = KronkTheme.colors.textMuted,
                )
            }
        }
        val body = stripHtml(status.content)
        if (body.isNotEmpty()) {
            Text(
                text = body,
                style = MaterialTheme.typography.bodyMedium,
                color = KronkTheme.colors.textPrimary,
            )
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            CountLabel(count = status.repliesCount, label = "replies")
            CountLabel(count = status.reblogsCount, label = "reblogs")
            CountLabel(count = status.favouritesCount, label = "froths")
        }
    }
}

@Composable
private fun CountLabel(count: Long, label: String) {
    Text(
        text = "$count $label",
        style = MaterialTheme.typography.bodySmall,
        color = KronkTheme.colors.textMuted,
    )
}

// Cheap HTML → text. Real HTML rendering (paragraphs, mentions,
// hashtags, custom emoji) lands with a dedicated content composable
// in a later phase — for Phase 2C's plain-status baseline, stripping
// tags is enough to render the body.
private val htmlTagRegex = Regex("<[^>]+>")

private fun stripHtml(html: String): String =
    html.replace("</p>", "\n\n")
        .replace("<br>", "\n")
        .replace("<br/>", "\n")
        .replace("<br />", "\n")
        .replace(htmlTagRegex, "")
        .replace("&amp;", "&")
        .replace("&lt;", "<")
        .replace("&gt;", ">")
        .replace("&quot;", "\"")
        .replace("&#39;", "'")
        .trim()
