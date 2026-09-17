package info.kronk.app.ui.webshell

import android.content.Intent
import android.net.Uri
import info.kronk.app.ui.shell.PillarKey
import info.kronk.core.common.KronkHost
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow

// Deep-link + share intent bridge from MainActivity to the Compose
// ShellHost. MainActivity resolves an incoming Intent into a
// KronkIntent describing which pillar to activate and which URL to
// load; ShellHost's LaunchedEffect on this flow does the navigate.
//
// Kept as an object with a SharedFlow (not a StateFlow) so a repeat
// intent with the same URL still fires — the user might tap the same
// link twice expecting the app to re-load it.

sealed interface KronkIntent {
    // Deep-link navigation — https://kronk.info/<path>.
    data class OpenUrl(val pillar: PillarKey, val url: String) : KronkIntent

    // ACTION_SEND — user shared content from another app. Payload
    // travels to /publish so Kronk's Compose feature can populate
    // itself from the URL query + attachment stream.
    data class Compose(val text: String?, val attachments: List<Uri>) : KronkIntent
}

object IntentEvents {
    // `replay = 1` so a cold-start intent (e.g. a notification tap
    // that launched the process) survives the gap between
    // MainActivity.onCreate emitting it and the Compose ShellHost
    // subscribing. Without replay the emission fires with no
    // subscriber and the deep-link is lost — user taps a Nudge
    // notification from a killed app, lands on Home instead of the
    // Nudge thread.
    val events: MutableSharedFlow<KronkIntent> = MutableSharedFlow(
        replay = 1,
        extraBufferCapacity = 4,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )

    // Convert an Activity intent into a KronkIntent, if one applies.
    // Returns null if the intent isn't a Kronk deep-link or share.
    fun resolve(intent: Intent): KronkIntent? {
        return when (intent.action) {
            Intent.ACTION_VIEW -> resolveView(intent)
            Intent.ACTION_SEND, Intent.ACTION_SEND_MULTIPLE -> resolveSend(intent)
            else -> null
        }
    }

    private fun resolveView(intent: Intent): KronkIntent? {
        val data = intent.data ?: return null
        if (!isKronkHost(data)) return null
        val path = (data.path ?: "/").ifEmpty { "/" }
        val fragment = data.fragment?.let { "#$it" } ?: ""
        val query = data.encodedQuery?.let { "?$it" } ?: ""
        val webPath = "$path$query$fragment"
        return KronkIntent.OpenUrl(pillar = pillarFor(path), url = KronkHost.origin + webPath)
    }

    private fun resolveSend(intent: Intent): KronkIntent? {
        val text = intent.getStringExtra(Intent.EXTRA_TEXT)
        val attachments: List<Uri> = when (intent.action) {
            Intent.ACTION_SEND -> {
                val s = @Suppress("DEPRECATION") intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM)
                listOfNotNull(s)
            }
            Intent.ACTION_SEND_MULTIPLE -> {
                @Suppress("DEPRECATION")
                intent.getParcelableArrayListExtra<Uri>(Intent.EXTRA_STREAM) ?: emptyList()
            }
            else -> emptyList()
        }
        if (text.isNullOrEmpty() && attachments.isEmpty()) return null
        return KronkIntent.Compose(text = text, attachments = attachments)
    }

    private fun isKronkHost(uri: Uri): Boolean {
        val host = uri.host ?: return false
        return host == KronkHost.value ||
            host == "kronk.info" ||
            host.endsWith(".kronk.info")
    }

    // Pick which pillar to land on for a given path. Falls back to
    // Home for anything that doesn't obviously belong elsewhere; the
    // ShellHost then loads the actual URL in the Home WebView so the
    // user still lands on the deep-linked content.
    private fun pillarFor(path: String): PillarKey = when {
        path.startsWith("/me") -> PillarKey.Me
        path.startsWith("/@") -> PillarKey.Me
        path.startsWith("/home") -> PillarKey.Home
        path.startsWith("/awawb") -> PillarKey.Awawb
        path.startsWith("/hub") -> PillarKey.Hub
        path.startsWith("/nudges") -> PillarKey.Nudges
        else -> PillarKey.Home
    }
}

