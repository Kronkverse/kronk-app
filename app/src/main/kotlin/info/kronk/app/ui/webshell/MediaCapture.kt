package info.kronk.app.ui.webshell

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.webkit.WebChromeClient
import androidx.core.content.FileProvider
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// Builds the right ActivityResult intent for a WebView
// `onShowFileChooser` callback.
//
// The web sets `<input type="file" accept="image/*" capture="environment">`
// (Moments composer, Albutts add-photo, etc.) when it wants a fresh
// capture rather than the document picker. Default WebView behaviour
// is to always launch the document picker; here we intercept when
// `capture` is set and hand back a real camera or camcorder intent,
// with a FileProvider-backed URI so the resulting file is a normal
// `content://` upload target for the Rails media endpoint.
//
// If capture isn't requested, or if the file provider can't allocate
// a target (rare — cache dir full), we fall back to the WebView's
// own default intent so the user still gets a file picker.

data class CaptureIntent(
    val chooser: Intent,
    // URI the camera/camcorder writes to. `null` for the default
    // document-picker path, in which case the result's `data`
    // contains the picked URIs (parsed via
    // `FileChooserParams.parseResult`).
    val outputUri: Uri?,
)

fun buildFileChooserIntent(
    context: Context,
    params: WebChromeClient.FileChooserParams,
): CaptureIntent {
    val acceptTypes = params.acceptTypes?.joinToString(" ")?.lowercase() ?: ""
    val wantsImage = acceptTypes.contains("image/")
    val wantsVideo = acceptTypes.contains("video/")
    val wantsAudio = acceptTypes.contains("audio/")
    val capture = params.isCaptureEnabled

    if (capture) {
        when {
            wantsImage -> return imageCaptureIntent(context)?.let { (intent, uri) ->
                CaptureIntent(intent, uri)
            } ?: fallback(params)
            wantsVideo -> return videoCaptureIntent(context)?.let { (intent, uri) ->
                CaptureIntent(intent, uri)
            } ?: fallback(params)
            wantsAudio -> return audioCaptureIntent()?.let { intent ->
                // Audio capture writes to a system-provided URI in the
                // result; we don't need EXTRA_OUTPUT.
                CaptureIntent(intent, null)
            } ?: fallback(params)
        }
    }

    // Not a capture request — fall back to the default document picker
    // path. WebView's own createIntent() respects `accept` filters.
    return fallback(params)
}

private fun fallback(params: WebChromeClient.FileChooserParams): CaptureIntent {
    return CaptureIntent(params.createIntent(), null)
}

private fun imageCaptureIntent(context: Context): Pair<Intent, Uri>? {
    val (uri, _) = allocCaptureUri(context, prefix = "IMG_", extension = "jpg") ?: return null
    val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE).apply {
        putExtra(MediaStore.EXTRA_OUTPUT, uri)
        addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    return intent to uri
}

private fun videoCaptureIntent(context: Context): Pair<Intent, Uri>? {
    val (uri, _) = allocCaptureUri(context, prefix = "VID_", extension = "mp4") ?: return null
    val intent = Intent(MediaStore.ACTION_VIDEO_CAPTURE).apply {
        putExtra(MediaStore.EXTRA_OUTPUT, uri)
        putExtra(MediaStore.EXTRA_VIDEO_QUALITY, 1) // high
        addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    return intent to uri
}

private fun audioCaptureIntent(): Intent? {
    // System sound recorder isn't guaranteed present on every device.
    // Where absent, ActivityNotFoundException falls through to the
    // WebView's default file picker (which shows document sources).
    return Intent(MediaStore.Audio.Media.RECORD_SOUND_ACTION).takeIf {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP
    }
}

// Allocates a FileProvider-backed URI for the camera app to write to.
// Files land under the app's private cache dir (see file_paths.xml);
// Android eviction reclaims them under memory pressure.
private fun allocCaptureUri(
    context: Context,
    prefix: String,
    extension: String,
): Pair<Uri, File>? {
    return runCatching {
        val dir = File(context.cacheDir, "captures").apply { mkdirs() }
        val stamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val file = File(dir, "$prefix$stamp.$extension")
        val authority = "${context.packageName}.captures"
        val uri = FileProvider.getUriForFile(context, authority, file)
        uri to file
    }.getOrNull()
}

// Parses the ActivityResult from a chooser intent into the URIs the
// WebView expects. Handles both capture paths (result data is null,
// content lives at the pre-allocated outputUri) and picker paths
// (WebView's own parseResult).
fun parseFileChooserResult(
    outputUri: Uri?,
    resultCode: Int,
    data: Intent?,
): Array<Uri> {
    if (resultCode != android.app.Activity.RESULT_OK) return emptyArray()
    // Camera/video capture path: EXTRA_OUTPUT already carries the
    // written file; some camera apps also return a data URI in the
    // result data, prefer the EXTRA_OUTPUT since we know its path.
    if (outputUri != null) return arrayOf(outputUri)
    // Picker path: WebView's own parseResult reads clip data + data URI.
    return WebChromeClient.FileChooserParams.parseResult(resultCode, data) ?: emptyArray()
}
