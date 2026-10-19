package com.shieldtv.launcher.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.tv.material3.Icon
import androidx.tv.material3.ListItem
import androidx.tv.material3.ListItemDefaults
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.shieldtv.launcher.data.LauncherApp

private data class DialogOption(val label: String, val icon: ImageVector, val action: () -> Unit)

/** M3 dialog listing actions for an app, shown on long-press of its tile. */
@Composable
fun AppOptionsDialog(
    app: LauncherApp,
    isFavorite: Boolean,
    favoriteIndex: Int,
    favoriteCount: Int,
    onDismiss: () -> Unit,
    onOpen: () -> Unit,
    onToggleFavorite: () -> Unit,
    onMove: (delta: Int) -> Unit,
    onAppInfo: () -> Unit,
) {
    val options = buildList {
        add(DialogOption("Open", Icons.Rounded.PlayArrow, onOpen))
        if (isFavorite) {
            add(DialogOption("Remove from Favorites", Icons.Rounded.FavoriteBorder, onToggleFavorite))
            if (favoriteIndex > 0) add(DialogOption("Move left", Icons.AutoMirrored.Rounded.ArrowBack) { onMove(-1) })
            if (favoriteIndex in 0 until favoriteCount - 1) {
                add(DialogOption("Move right", Icons.AutoMirrored.Rounded.ArrowForward) { onMove(1) })
            }
        } else {
            add(DialogOption("Add to Favorites", Icons.Rounded.Favorite, onToggleFavorite))
        }
        add(DialogOption("App info", Icons.Rounded.Info, onAppInfo))
    }
    val firstOption = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { firstOption.requestFocus() } }
    val colors = MaterialTheme.colorScheme

    Dialog(onDismissRequest = onDismiss) {
        Column(
            Modifier
                .width(400.dp)
                .background(SurfaceContainers.High, MaterialTheme.shapes.extraLarge)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // M3 dialog anatomy: icon, headline, then content.
            Image(
                bitmap = app.icon,
                contentDescription = null,
                modifier = Modifier
                    .size(40.dp)
                    .clip(MaterialTheme.shapes.medium),
            )
            Spacer(Modifier.height(16.dp))
            Text(
                text = app.label,
                color = colors.onSurface,
                style = MaterialTheme.typography.headlineSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(16.dp))
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                options.forEachIndexed { index, option ->
                    ListItem(
                        selected = false,
                        onClick = {
                            onDismiss()
                            option.action()
                        },
                        modifier = if (index == 0) Modifier.focusRequester(firstOption) else Modifier,
                        headlineContent = { Text(option.label) },
                        leadingContent = { Icon(option.icon, contentDescription = null, modifier = Modifier.size(ListItemDefaults.IconSize)) },
                        colors = ListItemDefaults.colors(
                            containerColor = SurfaceContainers.High,
                            contentColor = colors.onSurface,
                        ),
                    )
                }
            }
        }
    }
}
