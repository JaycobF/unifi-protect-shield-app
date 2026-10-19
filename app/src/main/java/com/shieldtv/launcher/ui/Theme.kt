package com.shieldtv.launcher.ui

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Shapes
import androidx.tv.material3.Typography
import androidx.tv.material3.darkColorScheme

/**
 * Material 3 dark scheme generated with material-color-utilities (SchemeTonalSpot)
 * from the seed colour #76B900.
 */
private val LauncherColorScheme = darkColorScheme(
    primary = Color(0xFFB2D189),
    onPrimary = Color(0xFF203601),
    primaryContainer = Color(0xFF354E15),
    onPrimaryContainer = Color(0xFFCEEDA3),
    inversePrimary = Color(0xFF4C662B),
    secondary = Color(0xFFC0CBAC),
    onSecondary = Color(0xFF2A331E),
    secondaryContainer = Color(0xFF404A33),
    onSecondaryContainer = Color(0xFFDCE7C7),
    tertiary = Color(0xFFA0D0CB),
    onTertiary = Color(0xFF003734),
    tertiaryContainer = Color(0xFF1F4E4B),
    onTertiaryContainer = Color(0xFFBCECE7),
    background = Color(0xFF12140E),
    onBackground = Color(0xFFE2E3D8),
    surface = Color(0xFF12140E),
    onSurface = Color(0xFFE2E3D8),
    surfaceVariant = Color(0xFF44483D),
    onSurfaceVariant = Color(0xFFC5C8BA),
    surfaceTint = Color(0xFFB2D189),
    inverseSurface = Color(0xFFE2E3D8),
    inverseOnSurface = Color(0xFF2F312A),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    // TV Material uses `border` for focus rings and `borderVariant` for quiet outlines.
    border = Color(0xFFE2E3D8),
    borderVariant = Color(0xFF44483D),
    scrim = Color(0xFF000000),
)

/** M3 surface-container roles, which the TV colour scheme doesn't model yet. */
object SurfaceContainers {
    val Lowest = Color(0xFF0D0F09)
    val Low = Color(0xFF1A1C16)
    val Default = Color(0xFF1E201A)
    val High = Color(0xFF282B24)
    val Highest = Color(0xFF33362E)
}

/** M3 shape scale. */
private val LauncherShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

@Composable
fun LauncherTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LauncherColorScheme,
        shapes = LauncherShapes,
        typography = Typography(), // M3 type scale
        content = content,
    )
}
