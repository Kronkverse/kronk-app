package org.joinmastodon.android.ui.compose

import androidx.compose.ui.graphics.Color

enum class KronkSpace(
    val displayName: String,
    val planetColor: Color,
    val glowColor: Color,
    val orderWeight: Int,
) {
    MURMUR   ("Murmur",   Color(0xFF6B8CFF), Color(0xFF3A5BCC), 0),
    KOMMONS  ("₭ommons",  Color(0xFFB97FFF), Color(0xFF7A3FCC), 1),
    HUDDLE   ("Huddle",   Color(0xFFFF8C42), Color(0xFFCC5A1A), 2),
    KALENDAR ("₭alendar", Color(0xFF4FD1A0), Color(0xFF1FA070), 3),
    NUDGES   ("Nudges",   Color(0xFFFF6B9D), Color(0xFFCC3A6B), 4),
}
