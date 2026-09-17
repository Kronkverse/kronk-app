package info.kronk.app.ui.shell

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import info.kronk.app.R
import info.kronk.core.designsystem.R as DesignR

// The five primary personal surfaces mirrored from the web's
// hub_switcher.tsx (Me / Home / AWAWB / Hub / Nudges, in visual order).
//
// Route strings match the web's paths so deep links can resolve to the
// same destination in native and (later) WebView contexts. AWAWB sits at
// the middle position — Aboriginal-flag glyph, "Always was, always will
// be" — as the visual anchor of the row.

enum class PillarKey(
    val route: String,
    @DrawableRes val iconRes: Int,
    @StringRes val labelRes: Int,
) {
    Me("me", DesignR.drawable.ic_pillar_taunt, R.string.pillar_me),
    Home("home", DesignR.drawable.ic_pillar_home, R.string.pillar_home),
    Awawb("awawb", DesignR.drawable.ic_pillar_aboriginal_flag, R.string.pillar_awawb),
    Hub("hub", DesignR.drawable.ic_pillar_apps, R.string.pillar_hub),
    Nudges("nudges", DesignR.drawable.ic_pillar_raven, R.string.pillar_nudges),
}
