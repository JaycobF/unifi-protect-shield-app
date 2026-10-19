package com.shieldtv.launcher.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Text
import com.shieldtv.launcher.data.LauncherApp

val HeroExpandedHeight = 200.dp

/**
 * Featured area for the focused app: big title and hints on the left (Google TV),
 * floating artwork with a soft shadow on the right (tvOS top shelf).
 * Collapses to [height] = 0 while browsing lower rows.
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
    Column(modifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Image(
                bitmap = app.icon,
                contentDescription = null,
                modifier = Modifier
                    .size(28.dp)
                    .clip(RoundedCornerShape(7.dp)),
            )
            Text(
                text = if (app.isGame) "GAME" else "APP",
                modifier = Modifier
                    .background(Color.White.copy(alpha = 0.12f), CircleShape)
                    .padding(horizontal = 10.dp, vertical = 3.dp),
                color = LauncherColors.TextSecondary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.2.sp,
            )
        }
        Text(
            text = app.label,
            color = LauncherColors.TextPrimary,
            fontSize = 44.sp,
            lineHeight = 48.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = "Press OK to open  ·  Hold OK for options",
            color = LauncherColors.TextSecondary,
            fontSize = 15.sp,
        )
    }
}

@Composable
private fun HeroArtwork(app: LauncherApp, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(22.dp)
    Box(
        modifier
            .aspectRatio(16f / 9f)
            .shadow(elevation = 16.dp, shape = shape, ambientColor = app.accent, spotColor = app.accent)
            .clip(shape)
            .border(1.dp, Color.White.copy(alpha = 0.15f), shape),
    ) {
        AppArtwork(app, Modifier.fillMaxWidth().fillMaxHeight())
    }
}
