package com.zalmoxis.app

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * Zalmoxis dark palette follows KILN's charcoal-and-lime visual language:
 * charcoal structure, restrained lime actions/focus, and high-contrast warm text.
 */
internal val ZalmoxisDarkColors = darkColorScheme(
    primary = Color(0xFFC2FA59),
    onPrimary = Color(0xFF0B0E0D),
    primaryContainer = Color(0xFF303A23),
    onPrimaryContainer = Color(0xFFE7F3C7),
    secondary = Color(0xFFA7B58C),
    onSecondary = Color(0xFF1B2410),
    secondaryContainer = Color(0xFF293123),
    onSecondaryContainer = Color(0xFFDCE7CA),
    tertiary = Color(0xFFC4EF66),
    onTertiary = Color(0xFF1B2410),
    background = Color(0xFF0B0E0D),
    onBackground = Color(0xFFF1F2EF),
    surface = Color(0xFF191E1C),
    onSurface = Color(0xFFF1F2EF),
    surfaceVariant = Color(0xFF242B27),
    onSurfaceVariant = Color(0xFFA8B4AE),
    inverseSurface = Color(0xFFF1F2EF),
    inverseOnSurface = Color(0xFF101110),
    inversePrimary = Color(0xFF536D2D),
    error = Color(0xFFFFB4A9),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD4),
    outline = Color(0xFF717972),
    outlineVariant = Color(0xFF34403A),
    scrim = Color(0x99000000),
)

private val ZalmoxisShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(26.dp),
)

@Composable
internal fun ZalmoxisTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = ZalmoxisDarkColors,
        shapes = ZalmoxisShapes,
        content = content,
    )
}
