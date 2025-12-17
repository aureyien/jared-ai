package com.music.sttnotes.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

// E-Ink optimized color scheme - maximum contrast
private val EInkColorScheme = lightColorScheme(
    // Primary - pure black
    primary = EInkBlack,
    onPrimary = EInkWhite,
    primaryContainer = EInkGrayLight,
    onPrimaryContainer = EInkBlack,

    // Secondary - dark gray
    secondary = EInkGrayDark,
    onSecondary = EInkWhite,
    secondaryContainer = EInkGrayLighter,
    onSecondaryContainer = EInkBlack,

    // Tertiary - medium gray
    tertiary = EInkGrayMedium,
    onTertiary = EInkWhite,
    tertiaryContainer = EInkGrayLight,
    onTertiaryContainer = EInkBlack,

    // Background & Surface - pure white
    background = EInkWhite,
    onBackground = EInkBlack,
    surface = EInkWhite,
    onSurface = EInkBlack,
    surfaceVariant = EInkGrayLighter,
    onSurfaceVariant = EInkGrayDark,

    // Outline - visible borders
    outline = EInkBlack,
    outlineVariant = EInkGrayMedium,

    // Error - still black (use icons for error indication)
    error = EInkBlack,
    onError = EInkWhite,
    errorContainer = EInkGrayLight,
    onErrorContainer = EInkBlack,

    // Inverse
    inverseSurface = EInkBlack,
    inverseOnSurface = EInkWhite,
    inversePrimary = EInkGrayLight,

    // Scrim
    scrim = EInkBlack.copy(alpha = 0.5f)
)

private val DarkColorScheme = darkColorScheme(
    primary = Purple80,
    secondary = PurpleGrey80,
    tertiary = Pink80
)

private val LightColorScheme = lightColorScheme(
    primary = Purple40,
    secondary = PurpleGrey40,
    tertiary = Pink40
)

// Custom text selection colors - light blue selection
private val customTextSelectionColors = TextSelectionColors(
    handleColor = Color(0xFF64B5F6),  // Light blue handle
    backgroundColor = Color(0xFF64B5F6).copy(alpha = 0.4f)  // Light blue background with transparency
)

@Composable
fun WhisperNotesTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,  // Disabled for e-ink
    eInkMode: Boolean = true,       // Enable e-ink optimizations by default
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        eInkMode -> EInkColorScheme
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    val typography = if (eInkMode) EInkTypography else Typography

    // Wrap with custom text selection colors
    CompositionLocalProvider(LocalTextSelectionColors provides customTextSelectionColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = typography,
            content = content
        )
    }
}
