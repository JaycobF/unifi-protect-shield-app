package com.shieldtv.launcher.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Icon
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import com.shieldtv.launcher.data.LauncherApp

private data class DialogOption(val label: String, val icon: ImageVector, val action: () -> Unit)

/** tvOS-style context menu shown on long-press of an app tile. */
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
        add(DialogOption("Open", Icons.Filled.PlayArrow, onOpen))
        if (isFavorite) {
            add(DialogOption("Remove from Favorites", Icons.Filled.FavoriteBorder, onToggleFavorite))
            if (favoriteIndex > 0) add(DialogOption("Move left", Icons.AutoMirrored.Filled.ArrowBack) { onMove(-1) })
            if (favoriteIndex in 0 until favoriteCount - 1) {
                add(DialogOption("Move right", Icons.AutoMirrored.Filled.ArrowForward) { onMove(1) })
            }
        } else {
            add(DialogOption("Add to Favorites", Icons.Filled.Favorite, onToggleFavorite))
        }
        add(DialogOption("App info", Icons.Filled.Info, onAppInfo))
    }
    val firstOption = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { firstOption.requestFocus() } }

    Dialog(onDismissRequest = onDismiss) {
        val shape = RoundedCornerShape(28.dp)
        Column(
            Modifier
                .width(380.dp)
                .background(Color(0xF21A1D24), shape)
                .border(1.dp, LauncherColors.GlassBorder, shape)
                .padding(20.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Image(
                    bitmap = app.icon,
                    contentDescription = null,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(10.dp)),
                )
                Spacer(Modifier.width(14.dp))
                Text(
                    text = app.label,
                    color = LauncherColors.TextPrimary,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(Modifier.height(16.dp))
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                options.forEachIndexed { index, option ->
                    OptionRow(
                        option = option,
                        modifier = if (index == 0) Modifier.focusRequester(firstOption) else Modifier,
                        onClick = {
                            onDismiss()
                            option.action()
                        },
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun OptionRow(option: DialogOption, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(14.dp)),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = Color.Transparent,
            contentColor = LauncherColors.TextPrimary,
            focusedContainerColor = Color.White,
            focusedContentColor = Color.Black,
        ),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1.03f),
    ) {
        Row(
            Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(option.icon, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(14.dp))
            Text(option.label, fontSize = 15.sp, fontWeight = FontWeight.Medium)
        }
    }
}
