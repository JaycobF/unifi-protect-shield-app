package com.shieldtv.launcher.data

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap
import androidx.palette.graphics.Palette
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class LauncherApp(
    val packageName: String,
    val component: ComponentName,
    val label: String,
    /** Wide 16:9 artwork supplied by TV apps. Null for phone apps without one. */
    val banner: ImageBitmap?,
    val icon: ImageBitmap,
    /** Dominant colour of the artwork, used for ambient glows and fallback tiles. */
    val accent: Color,
    val isGame: Boolean,
) {
    val key: String get() = component.flattenToShortString()

    fun launchIntent(): Intent = Intent(Intent.ACTION_MAIN)
        .setComponent(component)
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
}

class AppRepository(private val context: Context) {

    private val pm: PackageManager = context.packageManager

    suspend fun loadApps(): List<LauncherApp> = withContext(Dispatchers.IO) {
        // TV apps first so their LEANBACK activity wins over a phone LAUNCHER one.
        val resolved = LinkedHashMap<String, ResolveInfo>()
        for (category in listOf(Intent.CATEGORY_LEANBACK_LAUNCHER, Intent.CATEGORY_LAUNCHER)) {
            val intent = Intent(Intent.ACTION_MAIN).addCategory(category)
            for (info in pm.queryIntentActivities(intent, 0)) {
                resolved.putIfAbsent(info.activityInfo.packageName, info)
            }
        }
        resolved.remove(context.packageName)

        resolved.values
            .mapNotNull { runCatching { toLauncherApp(it) }.getOrNull() }
            .sortedBy { it.label.lowercase() }
    }

    private fun toLauncherApp(info: ResolveInfo): LauncherApp {
        val activity = info.activityInfo
        val appInfo = activity.applicationInfo
        val bannerDrawable: Drawable? = activity.loadBanner(pm) ?: appInfo.loadBanner(pm)
        val iconDrawable = activity.loadIcon(pm)

        val banner = bannerDrawable?.toBitmap(
            width = bannerDrawable.intrinsicWidth.takeIf { it > 0 } ?: BANNER_W,
            height = bannerDrawable.intrinsicHeight.takeIf { it > 0 } ?: BANNER_H,
            config = Bitmap.Config.ARGB_8888,
        )
        val icon = iconDrawable.toBitmap(ICON_SIZE, ICON_SIZE, Bitmap.Config.ARGB_8888)

        return LauncherApp(
            packageName = activity.packageName,
            component = ComponentName(activity.packageName, activity.name),
            label = info.loadLabel(pm).toString(),
            banner = banner?.asImageBitmap(),
            icon = icon.asImageBitmap(),
            accent = extractAccent(banner ?: icon),
            isGame = appInfo.category == ApplicationInfo.CATEGORY_GAME,
        )
    }

    private fun extractAccent(bitmap: Bitmap): Color {
        val palette = Palette.from(bitmap).maximumColorCount(12).generate()
        val swatch = palette.vibrantSwatch
            ?: palette.darkVibrantSwatch
            ?: palette.dominantSwatch
        return swatch?.let { Color(it.rgb) } ?: FALLBACK_ACCENT
    }

    private companion object {
        const val BANNER_W = 320
        const val BANNER_H = 180
        const val ICON_SIZE = 192
        val FALLBACK_ACCENT = Color(0xFF3A4A6B)
    }
}
