package info.kronk.core.designsystem.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

// Kronk palette + semantic colours, mirrored from the web's
// tokens.yaml → _tokens.scss generator output. Two colour sets — a
// dark set and a light set — swapped at runtime by KronkTheme based
// on `isSystemInDarkTheme()`.
//
// Do not hand-edit these hex values without also editing the web's
// tokens.yaml; the point of a shared token set is that the two clients
// agree. When tokens.yaml changes, rerun `bin/generate-tokens` on the
// web repo and mirror the new hex values here.

@Immutable
data class KronkColors(
    // ── Kronk palette ───────────────────────────────────────────────
    val purplePrimary: Color,
    val purpleBright: Color,
    val purpleDeep: Color,
    val purpleMuted: Color,
    val purpleAccent: Color,

    // ── Semantic surfaces + text ────────────────────────────────────
    val accent: Color,
    val textOnAccent: Color,
    val surfacePrimary: Color,
    val surfaceElevated: Color,
    val borderDefault: Color,
    val borderSubtle: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textMuted: Color,
    val warningRed: Color,
    val destructive: Color,
    val successGreen: Color,
    val decisionAgree: Color,
    val decisionAbstain: Color,
    val decisionBlock: Color,
    val decisionPending: Color,
    val tileGlyphOn: Color,
    val tileGlyphOff: Color,
)

internal val KronkColorsDark = KronkColors(
    purplePrimary = Color(0xFF301E75),
    purpleBright = Color(0xFF8F7FFF),
    purpleDeep = Color(0xFF300086),
    purpleMuted = Color(0xFF4A486D),
    purpleAccent = Color(0xFF5E45CD),

    accent = Color(0xFF4414CC),
    textOnAccent = Color(0xFFFFFFFF),
    surfacePrimary = Color(0xFF191B22),
    surfaceElevated = Color(0xFF292938),
    borderDefault = Color(0xFF47368B),
    borderSubtle = Color(0xFF2A2740),
    textPrimary = Color(0xFFECE9F5),
    textSecondary = Color(0xFF9C9CC9),
    textMuted = Color(0xFF606085),
    warningRed = Color(0xFFEF4444),
    destructive = Color(0xFFC75D6E),
    successGreen = Color(0xFF4B9160),
    decisionAgree = Color(0xFF22C55E),
    decisionAbstain = Color(0xFF94A3B8),
    decisionBlock = Color(0xFFEF4444),
    decisionPending = Color(0xFFF59E0B),
    tileGlyphOn = Color(0xFF8C7DFF),
    tileGlyphOff = Color(0xFF4E4A72),
)

internal val KronkColorsLight = KronkColors(
    purplePrimary = Color(0xFF43368E),
    purpleBright = Color(0xFF7E6CF7),
    purpleDeep = Color(0xFF34008C),
    purpleMuted = Color(0xFF6E6E8E),
    purpleAccent = Color(0xFF6B58D9),

    accent = Color(0xFF6364FF),
    textOnAccent = Color(0xFFFFFFFF),
    surfacePrimary = Color(0xFFFFFFFF),
    surfaceElevated = Color(0xFFF5F4F9),
    borderDefault = Color(0xFFDDD9E8),
    borderSubtle = Color(0xFFE8E5F0),
    textPrimary = Color(0xFF191B22),
    textSecondary = Color(0xFF45455F),
    textMuted = Color(0xFF7A7194),
    warningRed = Color(0xFFC53030),
    destructive = Color(0xFFC75D6E),
    successGreen = Color(0xFF276749),
    decisionAgree = Color(0xFF16A34A),
    decisionAbstain = Color(0xFF64748B),
    decisionBlock = Color(0xFFC53030),
    decisionPending = Color(0xFFC2410C),
    tileGlyphOn = Color(0xFF6364FF),
    tileGlyphOff = Color(0xFFA89FC5),
)
