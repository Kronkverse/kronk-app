package info.kronk.core.designsystem.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf

// Kronk root theme. Wraps MaterialTheme with the Kronk palette,
// typography, shape scale, and motion tokens.
//
// Consumers read Kronk tokens through the composition locals below —
// KronkTheme.colors, KronkTheme.shapes, KronkTheme.motion. Material
// components keep working via the underlying MaterialTheme (colours
// mapped onto Material's ColorScheme).

@Composable
fun KronkTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val kronkColors = if (darkTheme) KronkColorsDark else KronkColorsLight
    val kronkShapes = KronkShapes()
    val kronkMotion = KronkMotion()

    // Bridge to Material 3: name-based mapping of Kronk semantic
    // tokens to Material's ColorScheme slots. Components that call
    // MaterialTheme.colorScheme.primary reach purpleAccent, background
    // reaches surfacePrimary, etc.
    val materialColorScheme = if (darkTheme) {
        darkColorScheme(
            primary = kronkColors.purpleAccent,
            onPrimary = kronkColors.textOnAccent,
            secondary = kronkColors.purpleBright,
            onSecondary = kronkColors.textOnAccent,
            background = kronkColors.surfacePrimary,
            onBackground = kronkColors.textPrimary,
            surface = kronkColors.surfaceElevated,
            onSurface = kronkColors.textPrimary,
            surfaceVariant = kronkColors.surfaceElevated,
            onSurfaceVariant = kronkColors.textSecondary,
            outline = kronkColors.borderDefault,
            outlineVariant = kronkColors.borderSubtle,
            error = kronkColors.warningRed,
            onError = kronkColors.textOnAccent,
        )
    } else {
        lightColorScheme(
            primary = kronkColors.purpleAccent,
            onPrimary = kronkColors.textOnAccent,
            secondary = kronkColors.purpleBright,
            onSecondary = kronkColors.textOnAccent,
            background = kronkColors.surfacePrimary,
            onBackground = kronkColors.textPrimary,
            surface = kronkColors.surfaceElevated,
            onSurface = kronkColors.textPrimary,
            surfaceVariant = kronkColors.surfaceElevated,
            onSurfaceVariant = kronkColors.textSecondary,
            outline = kronkColors.borderDefault,
            outlineVariant = kronkColors.borderSubtle,
            error = kronkColors.warningRed,
            onError = kronkColors.textOnAccent,
        )
    }

    CompositionLocalProvider(
        LocalKronkColors provides kronkColors,
        LocalKronkShapes provides kronkShapes,
        LocalKronkMotion provides kronkMotion,
    ) {
        MaterialTheme(
            colorScheme = materialColorScheme,
            typography = KronkTypography,
            shapes = kronkShapes.toMaterial(),
            content = content,
        )
    }
}

// Composition-local accessors for the Kronk-specific tokens. Read them
// through `KronkTheme.colors` etc., which resolves the local at the
// call site. Missing-local throws — never wrap non-Kronk content in
// these calls.
object KronkTheme {
    val colors: KronkColors
        @Composable
        get() = LocalKronkColors.current

    val shapes: KronkShapes
        @Composable
        get() = LocalKronkShapes.current

    val motion: KronkMotion
        @Composable
        get() = LocalKronkMotion.current
}

private val LocalKronkColors = staticCompositionLocalOf<KronkColors> {
    error("KronkColors accessed outside KronkTheme")
}

private val LocalKronkShapes = staticCompositionLocalOf<KronkShapes> {
    error("KronkShapes accessed outside KronkTheme")
}

private val LocalKronkMotion = staticCompositionLocalOf<KronkMotion> {
    error("KronkMotion accessed outside KronkTheme")
}
