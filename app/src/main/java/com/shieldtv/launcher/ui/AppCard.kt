package com.shieldtv.launcher.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Border
import androidx.tv.material3.Card
import androidx.tv.material3.CardDefaults
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.shieldtv.launcher.data.LauncherApp

/**
 * A 16:9 app tile built on the TV Material 3 card: focus scales it up and draws the
 * scheme's focus ring, and the app's title fades in beneath it.
 */
@Composable
fun AppCard(
    app: LauncherApp,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onFocused: () -> Unit,
    modifier: Modifier = Modifier,
    shape: Shape = MaterialTheme.shapes.medium,
) {
    var focused by remember { mutableStateOf(false) }
    val labelAlpha by animateFloatAsState(if (focused) 1f else 0f, tween(200), label = "label")
    val colors = MaterialTheme.colorScheme

    Column(modifier) {
        Card(
            onClick = onClick,
            onLongClick = onLongClick,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .onFocusChanged {
                    focused = it.isFocused
                    if (it.isFocused) onFocused()
                },
            shape = CardDefaults.shape(shape),
            colors = CardDefaults.colors(containerColor = SurfaceContainers.High),
            scale = CardDefaults.scale(focusedScale = 1.1f),
            border = CardDefaults.border(
                focusedBorder = Border(BorderStroke(3.dp, colors.border), shape = shape),
            ),
        ) {
            AppArtwork(app, Modifier.fillMaxSize())
        }
        // Title fades in under the focused tile only, keeping rows visually quiet.
        Text(
            text = app.label,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 14.dp)
                .alpha(labelAlpha),
            color = colors.onSurface,
            style = MaterialTheme.typography.titleSmall,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/**
 * App banner, or for apps without one a tonal tile (the app's colour blended into the
 * surface container) with its icon and name.
 */
@Composable
fun AppArtwork(app: LauncherApp, modifier: Modifier = Modifier) {
    val banner = app.banner
    if (banner != null) {
        Image(
            bitmap = banner,
            contentDescription = app.label,
            contentScale = ContentScale.Crop,
            modifier = modifier,
        )
        return
    }
    val container = lerp(SurfaceContainers.Highest, app.accent, 0.35f)
    Column(
        modifier = modifier.background(
            Brush.verticalGradient(listOf(container, lerp(container, SurfaceContainers.Low, 0.6f))),
        ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Image(
            bitmap = app.icon,
            contentDescription = app.label,
            modifier = Modifier
                .size(44.dp)
                .clip(MaterialTheme.shapes.medium),
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = app.label,
            color = MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.labelLarge,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 10.dp),
        )
    }
}
