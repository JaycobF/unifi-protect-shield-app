package com.shieldtv.launcher.data

import android.content.Context

/** Ordered list of favourite app component keys, persisted in SharedPreferences. */
class FavoritesStore(context: Context) {

    private val prefs = context.getSharedPreferences("launcher", Context.MODE_PRIVATE)

    fun isInitialized(): Boolean = prefs.contains(KEY)

    fun load(): List<String> =
        prefs.getString(KEY, null)?.split(SEPARATOR)?.filter { it.isNotBlank() } ?: emptyList()

    fun save(keys: List<String>) {
        prefs.edit().putString(KEY, keys.joinToString(SEPARATOR)).apply()
    }

    companion object {
        private const val KEY = "favorites"
        private const val SEPARATOR = "|"

        /** Apps pinned on first run, in this order, when they are installed. */
        val DEFAULT_PACKAGES = listOf(
            "com.ubnt.unifi.protect",
            "com.google.android.youtube.tv",
            "com.netflix.ninja",
            "com.plexapp.android",
            "com.disney.disneyplus",
            "com.amazon.amazonvideo.livingroom",
            "com.apple.atve.androidtv.appletv",
            "com.spotify.tv.android",
            "com.nvidia.tegrazone3",
        )
        const val DEFAULT_COUNT = 6
    }
}
