package info.kronk.core.designsystem.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// Font stack mirrored from tokens.yaml:
//   display → Liberation Serif (Georgia fallback) — headers, wordmark
//   body    → mastodon-font-sans-serif (system fallback) — body text
//   mono    → Roboto Mono — code/technical
//
// The bundled Liberation Serif + Roboto Mono font resources come in a
// follow-up PR. For now we substitute Android's built-in FontFamily
// values — Serif for display, SansSerif for body, Monospace for
// mono — which is enough to prove the theme wires end-to-end and
// avoids a font-resource step during scaffolding.

internal val KronkFontFamilyDisplay: FontFamily = FontFamily.Serif
internal val KronkFontFamilyBody: FontFamily = FontFamily.SansSerif
internal val KronkFontFamilyMono: FontFamily = FontFamily.Monospace

// Material 3's Typography has 15 slots (display/headline/title/body/label
// × 3 sizes each). We hydrate each with the appropriate Kronk font
// family — display/headline use the serif face; title/body/label use
// the sans body face.

internal val KronkTypography: Typography = Typography(
    displayLarge = TextStyle(
        fontFamily = KronkFontFamilyDisplay,
        fontWeight = FontWeight.Normal,
        fontSize = 57.sp,
        lineHeight = 64.sp,
    ),
    displayMedium = TextStyle(
        fontFamily = KronkFontFamilyDisplay,
        fontWeight = FontWeight.Normal,
        fontSize = 45.sp,
        lineHeight = 52.sp,
    ),
    displaySmall = TextStyle(
        fontFamily = KronkFontFamilyDisplay,
        fontWeight = FontWeight.Normal,
        fontSize = 36.sp,
        lineHeight = 44.sp,
    ),
    headlineLarge = TextStyle(
        fontFamily = KronkFontFamilyDisplay,
        fontWeight = FontWeight.Normal,
        fontSize = 32.sp,
        lineHeight = 40.sp,
    ),
    headlineMedium = TextStyle(
        fontFamily = KronkFontFamilyDisplay,
        fontWeight = FontWeight.Normal,
        fontSize = 28.sp,
        lineHeight = 36.sp,
    ),
    headlineSmall = TextStyle(
        fontFamily = KronkFontFamilyDisplay,
        fontWeight = FontWeight.Normal,
        fontSize = 24.sp,
        lineHeight = 32.sp,
    ),
    titleLarge = TextStyle(
        fontFamily = KronkFontFamilyBody,
        fontWeight = FontWeight.Medium,
        fontSize = 22.sp,
        lineHeight = 28.sp,
    ),
    titleMedium = TextStyle(
        fontFamily = KronkFontFamilyBody,
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
        lineHeight = 24.sp,
    ),
    titleSmall = TextStyle(
        fontFamily = KronkFontFamilyBody,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
    ),
    bodyLarge = TextStyle(
        fontFamily = KronkFontFamilyBody,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = KronkFontFamilyBody,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
    ),
    bodySmall = TextStyle(
        fontFamily = KronkFontFamilyBody,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp,
    ),
    labelLarge = TextStyle(
        fontFamily = KronkFontFamilyBody,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
    ),
    labelMedium = TextStyle(
        fontFamily = KronkFontFamilyBody,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
    ),
    labelSmall = TextStyle(
        fontFamily = KronkFontFamilyBody,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 16.sp,
    ),
)
