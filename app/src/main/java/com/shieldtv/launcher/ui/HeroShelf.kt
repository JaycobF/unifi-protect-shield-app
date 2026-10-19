package com.shieldtv.launcher.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.SurfaceDefaults
import androidx.tv.material3.Text
import com.shieldtv.launcher.data.LauncherApp

val HeroExpandedHeight = 200.dp

/**
 * Featured area for the focused app: label, display title and supporting text on the left,
 * elevated artwork on the right. Collapses to [height] = 0 while browsing lower rows.
 */
@Composable
fun HeroShelf(app: LauncherApp, height: Dp, horizontalPadding: Dp, modifier: Modifier = Modifier) {
    Box(
        modifier
            .fillMaxWidth()
            .height(height)
            // Only clip while collapsing, so the artwork's shadow isn't cut off at rest.
            .then(if (height < HeroExpandedHeight) Modifier.clipToBounds() else Modifier)
            .graphicsLayer { alpha = (height / HeroExpandedHeight).coerceIn(0f, 1f) },
    ) {
        AnimatedContent(
            targetState = app,
            contentKey = { it.key },
            transitionSpec = {
                (fadeIn(tween(350)) + slideInHorizontally(tween(350)) { it / 24 }) togetherWith fadeOut(tween(150)) using SizeTransform(clip = false)
            },
            label = "hero",
        ) { current ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .height(HeroExpandedHeight)
                    .padding(horizontal = horizontalPadding, vertical = 20.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                HeroText(current, Modifier.weight(1f))
                Spacer(Modifier.width(32.dp))
                HeroArtwork(current, Modifier.fillMaxHeight())
            }
        }
    }
}

@Composable
private fun HeroText(app: LauncherApp, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    val type = MaterialTheme.typography
    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Image(
                bitmap = app.icon,
                contentDescription = null,
                modifier = Modifier
                    .size(28.dp)
                    .clip(MaterialTheme.shapes.small),
            )
            // Non-interactive label styled like an M3 assist chip.
            Surface(
                shape = MaterialTheme.shapes.small,
                colors = SurfaceDefaults.colors(
                    containerColor = colors.secondaryContainer,
                    contentColor = colors.onSecondaryContainer,
                ),
            ) {
                Text(
                    text = if (app.isGame) "Game" else "App",
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                    style = type.labelLarge,
                )
            }
        }
        Text(
            text = app.label,
            color = colors.onSurface,
            style = type.displaySmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = "Press OK to open  ·  Hold OK for options",
            color = colors.onSurfaceVariant,
            style = type.bodyLarge,
        )
    }
}

@Composable
private fun HeroArtwork(app: LauncherApp, modifier: Modifier = Modifier) {
    val shape = MaterialTheme.shapes.extraLarge
    Box(
        modifier
            .aspectRatio(16f / 9f)
            .shadow(elevation = 6.dp, shape = shape) // M3 elevation level 3
            .clip(shape),
    ) {
        AppArtwork(app, Modifier.fillMaxSize())
    }
}
