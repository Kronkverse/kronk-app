package info.kronk.core.designsystem.primitive

import androidx.annotation.DrawableRes
import info.kronk.core.designsystem.R

// Maps a manifest `icon.material` string (as declared in
// config/korners/<slug>.yaml on the web) to a bundled VectorDrawable.
//
// The full material-icons/400-24px catalogue is many hundreds of icons.
// This map covers the 25 icons currently declared across all shipped
// manifests; when a new korner declares an icon not in this list, the
// server sends the name anyway and we fall back to `apps` (the generic
// grid glyph) so the tile still renders. Adding a new icon is: drop
// the SVG-converted VectorDrawable into res/drawable/ as
// `ic_korner_<name>.xml`, add a line to the map. No app update is
// forced when the server ships a new korner using an already-mapped
// icon — that's the whole point of the manifest-driven approach.

@DrawableRes
fun kornerIconRes(material: String?): Int = when (material) {
    "apps" -> R.drawable.ic_korner_apps
    "australia" -> R.drawable.ic_korner_australia
    "cinema" -> R.drawable.ic_korner_cinema
    "construction" -> R.drawable.ic_korner_construction
    "diversity_2" -> R.drawable.ic_korner_diversity_2
    "groups" -> R.drawable.ic_korner_groups
    "gynecology" -> R.drawable.ic_korner_gynecology
    "headphones" -> R.drawable.ic_korner_headphones
    "home" -> R.drawable.ic_korner_home
    "hourglass" -> R.drawable.ic_korner_hourglass
    "in_flow" -> R.drawable.ic_korner_in_flow
    "karporn" -> R.drawable.ic_korner_karporn
    "kronikles" -> R.drawable.ic_korner_kronikles
    "kronk_coin" -> R.drawable.ic_korner_kronk_coin
    "kuestion" -> R.drawable.ic_korner_kuestion
    "palette" -> R.drawable.ic_korner_palette
    "photo_library" -> R.drawable.ic_korner_photo_library
    "raven" -> R.drawable.ic_korner_raven
    "rose" -> R.drawable.ic_korner_rose
    "settings" -> R.drawable.ic_korner_settings
    "snowflake" -> R.drawable.ic_korner_snowflake
    "spiral" -> R.drawable.ic_korner_spiral
    "taunt" -> R.drawable.ic_korner_taunt
    "waving_hand" -> R.drawable.ic_korner_waving_hand
    "zhong" -> R.drawable.ic_korner_zhong
    // Unknown material name → the generic apps grid glyph. Keeps the
    // grid renderable when a new korner ships an icon we haven't
    // bundled yet.
    else -> R.drawable.ic_korner_apps
}
