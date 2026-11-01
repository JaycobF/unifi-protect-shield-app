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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.shieldtv.launcher.data.LauncherApp

val HeroExpandedHeight = 200.dp

/**
 * Featured area for the focused app: icon, display title and remote hints on the left,
 * leaving the right side open so the backdrop photo shows through. It is the first item of
 * the Home list, so it collapses by scrolling away rather than by animating its height.
 */
@Composable
fun HeroShelf(app: LauncherApp, horizontalPadding: Dp, modifier: Modifier = Modifier) {
    AnimatedContent(
        targetState = app,
        contentKey = { it.key },
        transitionSpec = {
            (fadeIn(tween(250)) + slideInHorizontally(tween(250)) { it / 24 }) togetherWith fadeOut(tween(120)) using SizeTransform(clip = false)
        },
        modifier = modifier.fillMaxWidth(),
        label = "hero",
    ) { current ->
        HeroText(
            current,
            Modifier
                .fillMaxWidth()
                .height(HeroExpandedHeight)
                .padding(horizontal = horizontalPadding, vertical = 20.dp),
        )
    }
}

@Composable
private fun HeroText(app: LauncherApp, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    val type = MaterialTheme.typography
    Column(modifier, verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Image(
                bitmap = app.icon,
                contentDescription = null,
                modifier = Modifier
                    .size(44.dp)
                    .clip(MaterialTheme.shapes.medium),
            )
            Text(
                text = app.label,
                color = colors.onSurface,
                style = type.displaySmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Keycap("OK")
            HintText("Open", Modifier.padding(start = 8.dp))
            HintText("Hold", Modifier.padding(start = 32.dp, end = 8.dp))
            Keycap("OK")
            HintText("Options", Modifier.padding(start = 8.dp))
        }
    }
}

@Composable
private fun HintText(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        modifier = modifier,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        style = MaterialTheme.typography.bodyLarge,
    )
}

/** The remote's OK button, drawn as a small keycap. */
@Composable
private fun Keycap(label: String) {
    val colors = MaterialTheme.colorScheme
    Text(
        text = label,
        modifier = Modifier
            .border(1.dp, colors.onSurface.copy(alpha = 0.24f), MaterialTheme.shapes.extraSmall)
            .background(colors.onSurface.copy(alpha = 0.06f), MaterialTheme.shapes.extraSmall)
            .padding(horizontal = 8.dp, vertical = 2.dp),
        color = colors.onSurface,
        style = MaterialTheme.typography.labelMedium,
    )
}
