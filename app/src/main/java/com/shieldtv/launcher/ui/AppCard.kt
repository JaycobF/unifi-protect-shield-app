package com.shieldtv.launcher.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Border
import androidx.tv.material3.Card
import androidx.tv.material3.CardDefaults
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Glow
import androidx.tv.material3.Text
import com.shieldtv.launcher.data.LauncherApp

/**
 * A 16:9 app tile. Focus lifts and scales it (tvOS) with a coloured glow taken from the
 * app's artwork (Google TV), and a specular sheen sweeps across it like tvOS parallax.
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun AppCard(
    app: LauncherApp,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onFocused: () -> Unit,
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 16.dp,
    focusedScale: Float = 1.1f,
) {
    var focused by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(cornerRadius)
    val labelAlpha by animateFloatAsState(if (focused) 1f else 0f, tween(200), label = "label")

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
            colors = CardDefaults.colors(containerColor = Color.Transparent),
            scale = CardDefaults.scale(focusedScale = focusedScale),
            border = CardDefaults.border(
                focusedBorder = Border(BorderStroke(2.dp, Color.White.copy(alpha = 0.85f)), shape = shape),
            ),
            glow = CardDefaults.glow(
                focusedGlow = Glow(elevationColor = app.accent.copy(alpha = 0.55f), elevation = 20.dp),
            ),
        ) {
            Box(Modifier.fillMaxSize()) {
                AppArtwork(app, Modifier.fillMaxSize())
                Sheen(active = focused, modifier = Modifier.fillMaxSize())
            }
        }
        // Title fades in under the focused tile only, keeping rows visually quiet.
        Text(
            text = app.label,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 14.dp)
                .alpha(labelAlpha),
            color = LauncherColors.TextPrimary,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** App banner, or a generated tile (accent gradient + icon + name) for apps without one. */
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
    Column(
        modifier = modifier.background(
            Brush.linearGradient(
                listOf(lerp(app.accent, Color.White, 0.12f), lerp(app.accent, Color.Black, 0.55f)),
            )
        ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Image(
            bitmap = app.icon,
            contentDescription = app.label,
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(10.dp)),
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = app.label,
            color = Color.White,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 10.dp),
        )
    }
}

/** Glassy top highlight plus a one-shot diagonal light sweep when focus lands. */
@Composable
private fun Sheen(active: Boolean, modifier: Modifier = Modifier) {
    val sweep = remember { Animatable(-1f) }
    val glossAlpha by animateFloatAsState(if (active) 1f else 0f, tween(250), label = "gloss")

    LaunchedEffect(active) {
        if (active) {
            sweep.snapTo(-1f)
            sweep.animateTo(2f, tween(durationMillis = 1100, easing = FastOutSlowInEasing))
        }
    }

    Canvas(modifier.alpha(glossAlpha)) {
        drawRect(
            Brush.verticalGradient(
                0f to Color.White.copy(alpha = 0.22f),
                0.45f to Color.White.copy(alpha = 0.04f),
                1f to Color.Transparent,
            )
        )
        val x = sweep.value * size.width
        val band = size.width * 0.35f
        drawRect(
            Brush.linearGradient(
                0f to Color.Transparent,
                0.5f to Color.White.copy(alpha = 0.18f),
                1f to Color.Transparent,
                start = Offset(x - band, 0f),
                end = Offset(x + band, size.height),
            )
        )
    }
}
