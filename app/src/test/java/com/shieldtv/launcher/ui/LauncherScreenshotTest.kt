package com.shieldtv.launcher.ui

import android.content.ComponentName
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
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
import com.shieldtv.launcher.data.Wallpaper
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

    /**
     * Stand-in backdrop. Set PREVIEW_WALLPAPER to a local JPEG (e.g. an Earth View photo) to
     * preview with a real image; committed screenshots use the synthetic one.
     */
    private val wallpaper = Wallpaper(
        image = (System.getenv("PREVIEW_WALLPAPER")?.let { BitmapFactory.decodeFile(it) } ?: landscape()).asImageBitmap(),
        location = "Marble Canyon, United States",
        attribution = "©2019 Maxar Technologies",
    )

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
                    wallpaper = wallpaper,
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

    /** Painted aerial-style landscape: river valley through layered terrain. */
    private fun landscape(): Bitmap {
        val w = 1800f
        val h = 1200f
        val bmp = Bitmap.createBitmap(w.toInt(), h.toInt(), Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        canvas.drawRect(0f, 0f, w, h, Paint().apply {
            shader = LinearGradient(0f, 0f, w, h, 0xFFC9A46B.toInt(), 0xFF6E4A2E.toInt(), Shader.TileMode.CLAMP)
        })
        val bands = listOf(0xFFB5895A, 0xFF9C6F45, 0xFF845A37, 0xFF6B472B)
        bands.forEachIndexed { i, color ->
            val path = Path().apply {
                val y = 200f + i * 260f
                moveTo(0f, y)
                cubicTo(w * 0.3f, y - 180f, w * 0.6f, y + 220f, w, y - 60f)
                lineTo(w, y + 140f)
                cubicTo(w * 0.6f, y + 340f, w * 0.3f, y + 40f, 0f, y + 160f)
                close()
            }
            canvas.drawPath(path, Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = color.toInt() })
        }
        val river = Path().apply {
            moveTo(-50f, 900f)
            cubicTo(500f, 650f, 900f, 1050f, 1300f, 700f)
            cubicTo(1500f, 520f, 1700f, 560f, 1850f, 420f)
        }
        canvas.drawPath(river, Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 46f
            color = 0xFF2F6F73.toInt()
        })
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
