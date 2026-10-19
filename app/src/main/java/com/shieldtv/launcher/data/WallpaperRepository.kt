package com.shieldtv.launcher.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import com.materialkolor.quantize.QuantizerCelebi
import com.materialkolor.score.Score
import com.shieldtv.launcher.R
import com.shieldtv.launcher.ui.DEFAULT_SEED_COLOR
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

data class Wallpaper(
    val image: ImageBitmap,
    /** Short place name, e.g. "Marble Canyon, United States". May be empty. */
    val location: String,
    /** Imagery copyright line that must be shown with the photo. */
    val attribution: String,
    /** Source colour for the dynamic M3 scheme, extracted from the photo. */
    val seedColor: Int,
)

/**
 * Backdrops from Google Earth View, the stock "Earth" wallpaper collection used by Chrome,
 * Chromecast and Pixel. Photos are fetched on demand from Google's static feed (only the
 * photo IDs ship with the app) and the most recent one is cached for offline start-up.
 */
class WallpaperRepository(private val context: Context) {

    private val ids: List<Int> by lazy {
        context.resources.openRawResource(R.raw.earthview_ids).bufferedReader()
            .useLines { lines -> lines.mapNotNull { it.trim().toIntOrNull() }.toList() }
    }

    private val cacheImage = File(context.filesDir, "wallpaper.jpg")
    private val cacheMeta = File(context.filesDir, "wallpaper.json")

    /** The last photo shown, if any, so the launcher never starts on a blank background. */
    suspend fun cached(): Wallpaper? = withContext(Dispatchers.IO) {
        runCatching {
            if (!cacheImage.exists() || !cacheMeta.exists()) return@runCatching null
            val meta = JSONObject(cacheMeta.readText())
            val bitmap = decode(cacheImage.readBytes()) ?: return@runCatching null
            val seed = if (meta.has("seed")) meta.getInt("seed") else extractSeedColor(bitmap)
            Wallpaper(bitmap.asImageBitmap(), meta.optString("location"), meta.optString("attribution"), seed)
        }.getOrNull()
    }

    /** Fetches a random photo, retrying a couple of times; null when offline. */
    suspend fun next(): Wallpaper? = withContext(Dispatchers.IO) {
        repeat(3) {
            runCatching { fetch(ids.random()) }.getOrNull()?.let { return@withContext it }
        }
        null
    }

    private fun fetch(id: Int): Wallpaper? {
        val json = JSONObject(download("$FEED_URL/$id.json"))
        val bytes = Base64.decode(json.getString("dataUri").substringAfter(","), Base64.DEFAULT)
        val bitmap = decode(bytes) ?: return null
        val location = formatLocation(json.optJSONObject("geocode"))
        val attribution = json.optString("attribution")
        val seed = extractSeedColor(bitmap)

        cacheImage.writeBytes(bytes)
        cacheMeta.writeText(
            JSONObject()
                .put("location", location)
                .put("attribution", attribution)
                .put("seed", seed)
                .toString()
        )
        return Wallpaper(bitmap.asImageBitmap(), location, attribution, seed)
    }

    private fun download(url: String): String {
        val connection = URL(url).openConnection() as HttpURLConnection
        connection.connectTimeout = TIMEOUT_MS
        connection.readTimeout = TIMEOUT_MS
        return try {
            check(connection.responseCode == HttpURLConnection.HTTP_OK) { "HTTP ${connection.responseCode}" }
            connection.inputStream.bufferedReader().use { it.readText() }
        } finally {
            connection.disconnect()
        }
    }

    // RGB_565 halves memory for a full-screen photo that is always drawn under a scrim.
    private fun decode(bytes: ByteArray): Bitmap? =
        BitmapFactory.decodeByteArray(
            bytes, 0, bytes.size,
            BitmapFactory.Options().apply { inPreferredConfig = Bitmap.Config.RGB_565 },
        )

    private fun formatLocation(geocode: JSONObject?): String {
        if (geocode == null) return ""
        val place = geocode.optString("locality").ifEmpty { geocode.optString("administrative_area_level_1") }
        return listOf(place, geocode.optString("country")).filter { it.isNotEmpty() }.distinct().joinToString(", ")
    }

    private companion object {
        const val FEED_URL = "https://www.gstatic.com/prettyearth/assets/data/v3"
        const val TIMEOUT_MS = 15_000
    }
}

private const val SEED_SAMPLE_WIDTH = 128

/**
 * Picks an image's best source colour the way Android does for wallpapers: quantize a
 * downscaled copy (Celebi), then score candidates for chroma and coverage.
 */
fun extractSeedColor(bitmap: Bitmap): Int {
    val height = (SEED_SAMPLE_WIDTH * bitmap.height / bitmap.width).coerceAtLeast(1)
    val small = Bitmap.createScaledBitmap(bitmap, SEED_SAMPLE_WIDTH, height, true)
    val pixels = IntArray(small.width * small.height)
    small.getPixels(pixels, 0, small.width, 0, 0, small.width, small.height)
    if (small !== bitmap) small.recycle()
    val colors = QuantizerCelebi.quantize(pixels, maxColors = 128)
    return Score.score(colors, fallbackColorArgb = DEFAULT_SEED_COLOR).first()
}
