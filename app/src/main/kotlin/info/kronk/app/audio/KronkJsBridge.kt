package info.kronk.app.audio

import android.content.Context
import android.util.Log
import android.webkit.JavascriptInterface
import java.util.concurrent.atomic.AtomicInteger

// Bridge the WebView's `KronkNative` global exposes to Kronk's SPA.
// Only accessible when the loaded document's origin is one we
// control (KronkHost check happens at loadUrl time — the browser
// enforces same-origin for the JavascriptInterface across script
// contexts, but since we only ever load kronk.info + shadow, and
// the WebView's shouldOverrideUrlLoading pops out anything else to
// a Custom Tab, no untrusted script ever gets the reference).
//
// Currently owns the play/pause counter that drives
// `KronkAudioService`. Grows as we bridge more native features:
// share attachments upload, camera capture responses, biometric
// prompt, etc.

class KronkJsBridge(private val appContext: Context) {

    // Number of `<audio>` / `<video>` elements currently playing
    // anywhere in the WebView. When this transitions 0 -> 1 we
    // start the foreground audio service; when it drops to 0 we
    // stop it.
    private val playingCount = AtomicInteger(0)

    @JavascriptInterface
    fun onMediaPlay() {
        val next = playingCount.incrementAndGet()
        Log.d(TAG, "onMediaPlay: playing=$next")
        if (next == 1) KronkAudioService.start(appContext)
    }

    @JavascriptInterface
    fun onMediaPause() {
        val next = playingCount.updateAndGet { if (it > 0) it - 1 else 0 }
        Log.d(TAG, "onMediaPause: playing=$next")
        if (next == 0) KronkAudioService.stop(appContext)
    }

    // Called by the SPA when the current page unloads, so
    // stale-counting doesn't happen when React tears a media
    // element down without firing pause.
    @JavascriptInterface
    fun resetMediaCount() {
        playingCount.set(0)
        KronkAudioService.stop(appContext)
    }

    private companion object {
        const val TAG = "KronkJsBridge"
    }
}
