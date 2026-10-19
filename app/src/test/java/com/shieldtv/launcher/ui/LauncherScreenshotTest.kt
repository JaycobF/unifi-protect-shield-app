package com.shieldtv.launcher.ui

import android.content.ComponentName
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import android.graphics.Typeface
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.pressKey
import com.github.takahirom.roborazzi.captureScreenRoboImage
import com.shieldtv.launcher.LauncherState
import com.shieldtv.launcher.data.LauncherApp
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Renders the launcher with fake apps on a 1080p TV-sized screen. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w960dp-h540dp-land-television-xhdpi")
class LauncherScreenshotTest {

    @get:Rule
    val compose = createComposeRule()

    private val apps = listOf(
        fakeApp("Protect", 0xFF1F6FEB, withBanner = true),
        fakeApp("StreamFlix", 0xFFE50914, withBanner = true),
        fakeApp("TubeTV", 0xFFFF3B30, withBanner = true),
        fakeApp("Plexus", 0xFFE5A00D, withBanner = true),
        fakeApp("Wave Music", 0xFF1DB954, withBanner = true),
        fakeApp("Cinema+", 0xFF5B3CC4, withBanner = true),
        fakeApp("Browser", 0xFF4285F4, withBanner = false),
        fakeApp("Files", 0xFF00897B, withBanner = false),
        fakeApp("Kodi Box", 0xFF17B2E7, withBanner = true),
        fakeApp("Weather", 0xFFF4B400, withBanner = false),
        fakeApp("Star Racer", 0xFFFF6D00, withBanner = true, isGame = true),
        fakeApp("Puzzle Pop", 0xFFD81B60, withBanner = true, isGame = true),
    )
    private val state = LauncherState(loaded = true, allApps = apps, favorites = apps.take(6))

    @Test
    fun home() {
        render(LauncherTab.Home)
        compose.waitForIdle()
        captureScreenRoboImage("screenshots/home.png")
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun homeBrowsingSecondRow() {
        render(LauncherTab.Home)
        compose.onRoot().performKeyInput { pressKey(Key.DirectionDown) }
        compose.mainClock.advanceTimeBy(1500)
        captureScreenRoboImage("screenshots/home_row2.png")
    }

    @Test
    fun appsTab() {
        render(LauncherTab.Apps)
        compose.waitForIdle()
        captureScreenRoboImage("screenshots/apps.png")
    }

    @Test
    fun optionsDialog() {
        compose.setContent {
            LauncherTheme {
                AppOptionsDialog(
                    app = apps[1], isFavorite = true, favoriteIndex = 1, favoriteCount = 6,
                    onDismiss = {}, onOpen = {}, onToggleFavorite = {}, onMove = {}, onAppInfo = {},
                )
            }
        }
        compose.waitForIdle()
        captureScreenRoboImage("screenshots/options.png")
    }

    private fun render(tab: LauncherTab) {
        compose.setContent {
            LauncherTheme {
                LauncherScreen(
                    state = state,
                    onLaunch = {}, onAppInfo = {}, onOpenSettings = {},
                    onToggleFavorite = {}, onMoveFavorite = { _, _ -> },
                    initialTab = tab,
                )
            }
        }
    }

    private fun fakeApp(label: String, argb: Long, withBanner: Boolean, isGame: Boolean = false): LauncherApp {
        val accent = Color(argb)
        return LauncherApp(
            packageName = "com.example.${label.lowercase().filter { it.isLetter() }}",
            component = ComponentName("com.example", label),
            label = label,
            banner = if (withBanner) banner(label, accent).asImageBitmap() else null,
            icon = icon(label, accent).asImageBitmap(),
            accent = accent,
            isGame = isGame,
        )
    }

    private fun banner(label: String, accent: Color): Bitmap {
        val bmp = Bitmap.createBitmap(640, 360, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        val bg = Paint().apply {
            shader = LinearGradient(
                0f, 0f, 640f, 360f, accent.toArgb(), android.graphics.Color.BLACK, Shader.TileMode.CLAMP,
            )
        }
        canvas.drawRect(0f, 0f, 640f, 360f, bg)
        val text = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.WHITE
            textSize = 64f
            typeface = Typeface.DEFAULT_BOLD
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText(label, 320f, 200f, text)
        return bmp
    }

    private fun icon(label: String, accent: Color): Bitmap {
        val bmp = Bitmap.createBitmap(192, 192, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        canvas.drawRoundRect(0f, 0f, 192f, 192f, 40f, 40f, Paint().apply { color = accent.toArgb() })
        val text = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.WHITE
            textSize = 110f
            typeface = Typeface.DEFAULT_BOLD
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText(label.take(1), 96f, 136f, text)
        return bmp
    }
}
