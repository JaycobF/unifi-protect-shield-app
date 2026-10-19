package com.shieldtv.launcher

import android.app.Application
import android.os.SystemClock
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.shieldtv.launcher.data.AppRepository
import com.shieldtv.launcher.data.FavoritesStore
import com.shieldtv.launcher.data.LauncherApp
import com.shieldtv.launcher.data.Wallpaper
import com.shieldtv.launcher.data.WallpaperRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class LauncherState(
    val loaded: Boolean = false,
    val allApps: List<LauncherApp> = emptyList(),
    val favorites: List<LauncherApp> = emptyList(),
) {
    private val favoriteKeys: Set<String> get() = favorites.mapTo(HashSet()) { it.key }

    /** Non-favourite, non-game apps, for the Home screen's "Apps" row. */
    val otherApps: List<LauncherApp> get() = allApps.filter { !it.isGame && it.key !in favoriteKeys }
    val games: List<LauncherApp> get() = allApps.filter { it.isGame && it.key !in favoriteKeys }
    fun isFavorite(app: LauncherApp) = app.key in favoriteKeys
}

class LauncherViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = AppRepository(application)
    private val favoritesStore = FavoritesStore(application)
    private val wallpapers = WallpaperRepository(application)

    private val _wallpaper = MutableStateFlow<Wallpaper?>(null)
    val wallpaper: StateFlow<Wallpaper?> = _wallpaper
    private var wallpaperChangedAt = 0L

    private val apps = MutableStateFlow<List<LauncherApp>?>(null)
    private val favoriteKeys = MutableStateFlow(favoritesStore.load())

    val state: StateFlow<LauncherState> = combine(apps, favoriteKeys) { apps, keys ->
        if (apps == null) return@combine LauncherState()
        val byKey = apps.associateBy { it.key }
        LauncherState(
            loaded = true,
            allApps = apps,
            favorites = keys.mapNotNull { byKey[it] },
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, LauncherState())

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            val loaded = repository.loadApps()
            if (!favoritesStore.isInitialized()) seedFavorites(loaded)
            apps.value = loaded
        }
    }

    /**
     * Shows the cached backdrop straight away, then swaps in a new photo on return to the
     * launcher and every [WALLPAPER_INTERVAL_MS] while it stays visible. Call while started.
     */
    suspend fun rotateWallpapers() {
        if (_wallpaper.value == null) _wallpaper.value = wallpapers.cached()
        while (true) {
            val sinceChange = SystemClock.elapsedRealtime() - wallpaperChangedAt
            if (wallpaperChangedAt != 0L && sinceChange < WALLPAPER_MIN_GAP_MS) {
                delay(WALLPAPER_INTERVAL_MS - sinceChange)
            }
            wallpapers.next()?.let {
                _wallpaper.value = it
                wallpaperChangedAt = SystemClock.elapsedRealtime()
            }
            delay(WALLPAPER_INTERVAL_MS)
        }
    }

    fun toggleFavorite(app: LauncherApp) = updateFavorites { keys ->
        if (app.key in keys) keys - app.key else keys + app.key
    }

    /** Moves a favourite by [delta] positions, clamped to the list bounds. */
    fun moveFavorite(app: LauncherApp, delta: Int) = updateFavorites { keys ->
        val from = keys.indexOf(app.key)
        if (from < 0) return@updateFavorites keys
        val to = (from + delta).coerceIn(0, keys.lastIndex)
        keys.toMutableList().apply { add(to, removeAt(from)) }
    }

    private fun updateFavorites(transform: (List<String>) -> List<String>) {
        val updated = transform(favoriteKeys.value)
        favoritesStore.save(updated)
        favoriteKeys.value = updated
    }

    private fun seedFavorites(installed: List<LauncherApp>) {
        val byPackage = installed.associateBy { it.packageName }
        val preferred = FavoritesStore.DEFAULT_PACKAGES.mapNotNull { byPackage[it] }
        // Top up with any other TV-native apps (those that ship a banner).
        val filler = installed.filter { it.banner != null && it !in preferred }
        val seed = (preferred + filler).take(FavoritesStore.DEFAULT_COUNT).map { it.key }
        favoritesStore.save(seed)
        favoriteKeys.value = seed
    }

    private companion object {
        const val WALLPAPER_INTERVAL_MS = 15 * 60 * 1000L
        /** Avoid a new photo every time the user pops back to Home for a moment. */
        const val WALLPAPER_MIN_GAP_MS = 2 * 60 * 1000L
    }
}
