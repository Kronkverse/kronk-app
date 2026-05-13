package org.joinmastodon.android.ui.compose

import android.content.Context

object SpaceUsageTracker {
    private const val PREFS_NAME = "space_usage"
    private const val RECENTS_LIMIT = 4

    fun increment(context: Context, accountId: String, space: KronkSpace) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val key = "$accountId:${space.name}"
        prefs.edit().putInt(key, prefs.getInt(key, 0) + 1).apply()
    }

    fun getRecents(context: Context, accountId: String): List<KronkSpace> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val withCounts = KronkSpace.entries.map { space ->
            space to prefs.getInt("$accountId:${space.name}", 0)
        }
        val used = withCounts.filter { it.second > 0 }
            .sortedByDescending { it.second }
            .map { it.first }
            .take(RECENTS_LIMIT)
        return used.ifEmpty { KronkSpace.entries.take(RECENTS_LIMIT) }
    }
}
