package com.shieldtv.launcher.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.darkColorScheme

object LauncherColors {
    val Background = Color(0xFF0B0D12)
    val TextPrimary = Color(0xFFF5F6F8)
    val TextSecondary = Color(0xA6F5F6F8)
    val Glass = Color(0x1AFFFFFF)
    val GlassBorder = Color(0x26FFFFFF)
    val Accent = Color(0xFF76B900)
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun LauncherTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = LauncherColors.Accent,
            background = LauncherColors.Background,
            surface = LauncherColors.Background,
            onSurface = LauncherColors.TextPrimary,
            onBackground = LauncherColors.TextPrimary,
        ),
        content = content,
    )
}
