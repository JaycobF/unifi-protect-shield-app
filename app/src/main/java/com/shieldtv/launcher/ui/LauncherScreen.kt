package com.shieldtv.launcher.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.shieldtv.launcher.LauncherState
import com.shieldtv.launcher.data.LauncherApp
import com.shieldtv.launcher.data.Wallpaper

private val EdgePadding = 56.dp

private data class HomeRow(val title: String, val apps: List<LauncherApp>, val cardWidth: Dp)

@Composable
fun LauncherScreen(
    state: LauncherState,
    wallpaper: Wallpaper?,
    onLaunch: (LauncherApp) -> Unit,
    onAppInfo: (LauncherApp) -> Unit,
    onOpenSettings: () -> Unit,
    onToggleFavorite: (LauncherApp) -> Unit,
    onMoveFavorite: (LauncherApp, Int) -> Unit,
    initialTab: LauncherTab = LauncherTab.Home,
) {
    var tab by rememberSaveable { mutableStateOf(initialTab) }
    var featured by remember { mutableStateOf<LauncherApp?>(null) }
    var optionsFor by remember { mutableStateOf<LauncherApp?>(null) }
    val hero = featured ?: state.favorites.firstOrNull() ?: state.allApps.firstOrNull()

    // A launcher never exits on Back; it just returns to the Home tab.
    BackHandler { tab = LauncherTab.Home }

    Box(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        Backdrop(wallpaper)
        AmbientBackground(accent = hero?.accent ?: MaterialTheme.colorScheme.primary)

        Column(Modifier.fillMaxSize()) {
            TopBar(
                selected = tab,
                onSelect = { tab = it },
                onOpenSettings = onOpenSettings,
                location = wallpaper?.location.orEmpty(),
            )

            when (tab) {
                LauncherTab.Home -> HomeContent(
                    state = state,
                    hero = hero,
                    onFocusApp = { featured = it },
                    onLaunch = onLaunch,
                    onOptions = { optionsFor = it },
                )
                LauncherTab.Apps -> AppsGrid(
                    apps = state.allApps,
                    onFocusApp = { featured = it },
                    onLaunch = onLaunch,
                    onOptions = { optionsFor = it },
                )
            }
        }

        wallpaper?.attribution?.takeIf { it.isNotEmpty() }?.let {
            Text(
                text = "Google Earth  ·  $it",
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = EdgePadding, bottom = 12.dp)
                    .widthIn(max = 560.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                style = MaterialTheme.typography.labelSmall,
                textAlign = TextAlign.End,
                maxLines = 2,
            )
        }
    }

    optionsFor?.let { app ->
        val favorites = state.favorites
        AppOptionsDialog(
            app = app,
            isFavorite = state.isFavorite(app),
            favoriteIndex = favorites.indexOfFirst { it.key == app.key },
            favoriteCount = favorites.size,
            onDismiss = { optionsFor = null },
            onOpen = { onLaunch(app) },
            onToggleFavorite = { onToggleFavorite(app) },
            onMove = { delta -> onMoveFavorite(app, delta) },
            onAppInfo = { onAppInfo(app) },
        )
    }
}

/**
 * Full-bleed Earth View photo under an M3 scrim: lighter at the top so the photo reads,
 * near-opaque surface at the bottom where the rows sit, and darker on the left behind the hero text.
 */
@Composable
private fun Backdrop(wallpaper: Wallpaper?) {
    val surface = MaterialTheme.colorScheme.surface
    Crossfade(targetState = wallpaper, animationSpec = tween(1500), label = "wallpaper") { current ->
        if (current != null) {
            Image(
                bitmap = current.image,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
    if (wallpaper == null) return
    Canvas(Modifier.fillMaxSize()) {
        drawRect(
            Brush.verticalGradient(
                0f to surface.copy(alpha = 0.45f),
                0.4f to surface.copy(alpha = 0.6f),
                0.65f to surface.copy(alpha = 0.85f),
                1f to surface.copy(alpha = 0.95f),
            )
        )
        drawRect(
            Brush.horizontalGradient(
                0f to surface.copy(alpha = 0.55f),
                0.55f to Color.Transparent,
            )
        )
    }
}

/** Subtle colour wash from the top right, tinted by the focused app (content-based colour, M3 style). */
@Composable
private fun AmbientBackground(accent: Color) {
    val color by animateColorAsState(accent, tween(700), label = "ambient")
    Canvas(Modifier.fillMaxSize()) {
        drawRect(
            Brush.radialGradient(
                0f to color.copy(alpha = 0.22f),
                0.55f to color.copy(alpha = 0.06f),
                1f to Color.Transparent,
                center = Offset(size.width * 0.85f, size.height * 0.05f),
                radius = size.width * 0.75f,
            )
        )
        drawRect(
            Brush.radialGradient(
                0f to color.copy(alpha = 0.05f),
                1f to Color.Transparent,
                center = Offset(0f, size.height),
                radius = size.width * 0.5f,
            )
        )
    }
}

@Composable
private fun HomeContent(
    state: LauncherState,
    hero: LauncherApp?,
    onFocusApp: (LauncherApp) -> Unit,
    onLaunch: (LauncherApp) -> Unit,
    onOptions: (LauncherApp) -> Unit,
) {
    val rows = listOf(
        HomeRow("Favorites", state.favorites, cardWidth = 232.dp),
        HomeRow("Apps", state.otherApps, cardWidth = 196.dp),
        HomeRow("Games", state.games, cardWidth = 196.dp),
    ).filter { it.apps.isNotEmpty() }

    // Like Google TV, the featured area collapses once you browse below the first row.
    var focusedRow by remember { mutableIntStateOf(0) }
    val heroHeight by animateDpAsState(if (focusedRow == 0) HeroExpandedHeight else 0.dp, tween(350), label = "hero")

    val firstCard = remember { FocusRequester() }
    var initialFocusDone by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(state.loaded, rows.isNotEmpty()) {
        if (!state.loaded || rows.isEmpty() || initialFocusDone) return@LaunchedEffect
        // Lazy rows compose their items during layout, so wait a few frames for the card to attach.
        repeat(10) {
            withFrameNanos { }
            if (runCatching { firstCard.requestFocus() }.isSuccess) {
                initialFocusDone = true
                return@LaunchedEffect
            }
        }
    }

    Column(Modifier.fillMaxSize()) {
        if (hero != null) {
            HeroShelf(app = hero, height = heroHeight, horizontalPadding = EdgePadding)
        }

        if (state.loaded && rows.isEmpty()) {
            Text(
                "No apps found",
                modifier = Modifier.padding(EdgePadding),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyLarge,
            )
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 48.dp),
        ) {
            itemsIndexed(rows, key = { _, row -> row.title }) { rowIndex, row ->
                Column(Modifier.fillMaxWidth()) {
                    Text(
                        text = row.title,
                        modifier = Modifier.padding(start = EdgePadding, top = 8.dp),
                        color = MaterialTheme.colorScheme.onSurface,
                        style = MaterialTheme.typography.titleLarge,
                    )
                    LazyRow(
                        // Vertical padding leaves room for the focused card's scale + glow.
                        contentPadding = PaddingValues(horizontal = EdgePadding, vertical = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(24.dp),
                    ) {
                        itemsIndexed(row.apps, key = { _, app -> app.key }) { index, app ->
                            AppCard(
                                app = app,
                                modifier = Modifier
                                    .width(row.cardWidth)
                                    .then(
                                        if (rowIndex == 0 && index == 0) Modifier.focusRequester(firstCard)
                                        else Modifier
                                    ),
                                shape = if (rowIndex == 0) MaterialTheme.shapes.large else MaterialTheme.shapes.medium,
                                onClick = { onLaunch(app) },
                                onLongClick = { onOptions(app) },
                                onFocused = {
                                    focusedRow = rowIndex
                                    onFocusApp(app)
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AppsGrid(
    apps: List<LauncherApp>,
    onFocusApp: (LauncherApp) -> Unit,
    onLaunch: (LauncherApp) -> Unit,
    onOptions: (LauncherApp) -> Unit,
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(5),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = EdgePadding, end = EdgePadding, top = 36.dp, bottom = 48.dp),
        horizontalArrangement = Arrangement.spacedBy(24.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        items(apps, key = { it.key }) { app ->
            AppCard(
                app = app,
                onClick = { onLaunch(app) },
                onLongClick = { onOptions(app) },
                onFocused = { onFocusApp(app) },
            )
        }
    }
}
