package info.kronk.app.ui.webshell

import java.util.concurrent.atomic.AtomicBoolean

// Single-slot latch flipped once when the first WebView tab
// finishes loading its initial page. MainActivity's SplashScreen
// keep-on-screen condition reads this so the rose emblem stays
// visible until Kronk actually paints, avoiding the "rose ->
// blank dark -> content" flash on slow cold-starts.
//
// Kept as a bare AtomicBoolean rather than a StateFlow because the
// keep-on-screen condition polls per-frame from the platform side;
// a simple volatile read is cheaper than Flow.collect overhead in
// that hot path.

object AppReadyState {
    private val flipped = AtomicBoolean(false)
    val isReady: Boolean get() = flipped.get()
    fun markReady() {
        flipped.compareAndSet(false, true)
    }
}
