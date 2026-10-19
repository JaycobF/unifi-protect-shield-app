package com.shieldtv.launcher.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.tv.material3.ColorScheme
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Shapes
import androidx.tv.material3.Typography
import androidx.tv.material3.darkColorScheme
import com.materialkolor.dynamiccolor.MaterialDynamicColors
import com.materialkolor.hct.Hct
import com.materialkolor.scheme.SchemeTonalSpot

/** Seed used until a wallpaper provides one (and when offline). */
const val DEFAULT_SEED_COLOR: Int = 0xFF76B900.toInt()

/** M3 surface-container roles, which the TV colour scheme doesn't model yet. */
@Immutable
data class SurfaceContainers(
    val lowest: Color,
    val low: Color,
    val default: Color,
    val high: Color,
    val highest: Color,
)

private val LocalSurfaceContainers = staticCompositionLocalOf<SurfaceContainers> {
    error("LauncherTheme not provided")
}

/** The current theme's surface containers. */
val surfaceContainers: SurfaceContainers
    @Composable @ReadOnlyComposable get() = LocalSurfaceContainers.current

@Immutable
private data class LauncherPalette(val scheme: ColorScheme, val containers: SurfaceContainers)

/**
 * Builds the M3 dark scheme for [seed] with Material Color Utilities' tonal-spot variant,
 * the same algorithm Android uses for wallpaper-based dynamic colour.
 */
private fun paletteFromSeed(seed: Int): LauncherPalette {
    val scheme = SchemeTonalSpot(Hct.fromInt(seed), isDark = true, contrastLevel = 0.0)
    val roles = MaterialDynamicColors()
    fun c(color: com.materialkolor.dynamiccolor.DynamicColor) = Color(color.getArgb(scheme))

    return LauncherPalette(
        scheme = darkColorScheme(
            primary = c(roles.primary()),
            onPrimary = c(roles.onPrimary()),
            primaryContainer = c(roles.primaryContainer()),
            onPrimaryContainer = c(roles.onPrimaryContainer()),
            inversePrimary = c(roles.inversePrimary()),
            secondary = c(roles.secondary()),
            onSecondary = c(roles.onSecondary()),
            secondaryContainer = c(roles.secondaryContainer()),
            onSecondaryContainer = c(roles.onSecondaryContainer()),
            tertiary = c(roles.tertiary()),
            onTertiary = c(roles.onTertiary()),
            tertiaryContainer = c(roles.tertiaryContainer()),
            onTertiaryContainer = c(roles.onTertiaryContainer()),
            background = c(roles.background()),
            onBackground = c(roles.onBackground()),
            surface = c(roles.surface()),
            onSurface = c(roles.onSurface()),
            surfaceVariant = c(roles.surfaceVariant()),
            onSurfaceVariant = c(roles.onSurfaceVariant()),
            surfaceTint = c(roles.surfaceTint()),
            inverseSurface = c(roles.inverseSurface()),
            inverseOnSurface = c(roles.inverseOnSurface()),
            error = c(roles.error()),
            onError = c(roles.onError()),
            errorContainer = c(roles.errorContainer()),
            onErrorContainer = c(roles.onErrorContainer()),
            // TV Material uses `border` for focus rings and `borderVariant` for quiet outlines.
            border = c(roles.onSurface()),
            borderVariant = c(roles.outlineVariant()),
            scrim = c(roles.scrim()),
        ),
        containers = SurfaceContainers(
            lowest = c(roles.surfaceContainerLowest()),
            low = c(roles.surfaceContainerLow()),
            default = c(roles.surfaceContainer()),
            high = c(roles.surfaceContainerHigh()),
            highest = c(roles.surfaceContainerHighest()),
        ),
    )
}

/** M3 shape scale. */
private val LauncherShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

/**
 * Material 3 theme whose colours come from [seedColor] (normally extracted from the
 * wallpaper photo). Changing the seed animates every colour role to the new scheme.
 */
@Composable
fun LauncherTheme(seedColor: Int = DEFAULT_SEED_COLOR, content: @Composable () -> Unit) {
    val target = remember(seedColor) { paletteFromSeed(seedColor) }
    val scheme = target.scheme.animated()
    val containers = target.containers.animated()

    CompositionLocalProvider(LocalSurfaceContainers provides containers) {
        MaterialTheme(
            colorScheme = scheme,
            shapes = LauncherShapes,
            typography = Typography(), // M3 type scale
            content = content,
        )
    }
}

private const val COLOR_FADE_MS = 1500 // matches the wallpaper crossfade

@Composable
private fun animate(target: Color): Color =
    animateColorAsState(target, tween(COLOR_FADE_MS), label = "themeColor").value

@Composable
private fun ColorScheme.animated() = copy(
    primary = animate(primary),
    onPrimary = animate(onPrimary),
    primaryContainer = animate(primaryContainer),
    onPrimaryContainer = animate(onPrimaryContainer),
    inversePrimary = animate(inversePrimary),
    secondary = animate(secondary),
    onSecondary = animate(onSecondary),
    secondaryContainer = animate(secondaryContainer),
    onSecondaryContainer = animate(onSecondaryContainer),
    tertiary = animate(tertiary),
    onTertiary = animate(onTertiary),
    tertiaryContainer = animate(tertiaryContainer),
    onTertiaryContainer = animate(onTertiaryContainer),
    background = animate(background),
    onBackground = animate(onBackground),
    surface = animate(surface),
    onSurface = animate(onSurface),
    surfaceVariant = animate(surfaceVariant),
    onSurfaceVariant = animate(onSurfaceVariant),
    surfaceTint = animate(surfaceTint),
    inverseSurface = animate(inverseSurface),
    inverseOnSurface = animate(inverseOnSurface),
    border = animate(border),
    borderVariant = animate(borderVariant),
)

@Composable
private fun SurfaceContainers.animated() = SurfaceContainers(
    lowest = animate(lowest),
    low = animate(low),
    default = animate(default),
    high = animate(high),
    highest = animate(highest),
)
