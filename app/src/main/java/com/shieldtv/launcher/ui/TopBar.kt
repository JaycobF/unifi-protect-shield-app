package com.shieldtv.launcher.ui

import android.text.format.DateFormat
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Border
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Icon
import androidx.tv.material3.SelectableSurfaceDefaults
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import kotlinx.coroutines.delay
import java.util.Calendar

enum class LauncherTab(val title: String) { Home("Home"), Apps("Apps") }

/** Greeting on the left (Google TV), frosted tab pill in the centre (tvOS), clock + settings on the right. */
@Composable
fun TopBar(
    selected: LauncherTab,
    onSelect: (LauncherTab) -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val now = rememberNow()
    val context = LocalContext.current

    Box(
        modifier
            .fillMaxWidth()
            .padding(start = 56.dp, end = 56.dp, top = 28.dp),
    ) {
        Text(
            text = greeting(now),
            modifier = Modifier.align(Alignment.CenterStart),
            color = LauncherColors.TextSecondary,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
        )

        Row(
            Modifier
                .align(Alignment.Center)
                .background(LauncherColors.Glass, CircleShape)
                .border(1.dp, LauncherColors.GlassBorder, CircleShape)
                .padding(5.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            LauncherTab.entries.forEach { tab ->
                TabPill(tab.title, selected = tab == selected, onSelect = { onSelect(tab) })
            }
        }

        Row(
            Modifier.align(Alignment.CenterEnd),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = DateFormat.getTimeFormat(context).format(now.time),
                    color = LauncherColors.TextPrimary,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = DateFormat.format("EEE, MMM d", now).toString(),
                    color = LauncherColors.TextSecondary,
                    fontSize = 12.sp,
                )
            }
            SettingsButton(onOpenSettings)
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun TabPill(title: String, selected: Boolean, onSelect: () -> Unit) {
    Surface(
        selected = selected,
        onClick = onSelect,
        // Google TV switches tabs as soon as focus lands on them.
        modifier = Modifier.onFocusChanged { if (it.isFocused && !selected) onSelect() },
        shape = SelectableSurfaceDefaults.shape(CircleShape),
        colors = SelectableSurfaceDefaults.colors(
            containerColor = Color.Transparent,
            contentColor = LauncherColors.TextSecondary,
            selectedContainerColor = Color.White.copy(alpha = 0.16f),
            selectedContentColor = LauncherColors.TextPrimary,
            focusedContainerColor = Color.White,
            focusedContentColor = Color.Black,
            focusedSelectedContainerColor = Color.White,
            focusedSelectedContentColor = Color.Black,
        ),
        scale = SelectableSurfaceDefaults.scale(focusedScale = 1.05f),
    ) {
        Text(
            text = title,
            modifier = Modifier.padding(horizontal = 22.dp, vertical = 8.dp),
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun SettingsButton(onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = Modifier.size(44.dp),
        shape = ClickableSurfaceDefaults.shape(CircleShape),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = LauncherColors.Glass,
            contentColor = LauncherColors.TextPrimary,
            focusedContainerColor = Color.White,
            focusedContentColor = Color.Black,
        ),
        border = ClickableSurfaceDefaults.border(
            border = Border(BorderStroke(1.dp, LauncherColors.GlassBorder), shape = CircleShape),
        ),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1.1f),
    ) {
        Icon(
            imageVector = Icons.Filled.Settings,
            contentDescription = "Settings",
            modifier = Modifier
                .align(Alignment.Center)
                .size(22.dp),
        )
    }
}

@Composable
private fun rememberNow(): Calendar {
    var now by remember { mutableStateOf(Calendar.getInstance()) }
    LaunchedEffect(Unit) {
        while (true) {
            // Tick on the minute boundary so the clock never lags.
            val secondsLeft = 60 - Calendar.getInstance().get(Calendar.SECOND)
            delay(secondsLeft * 1000L)
            now = Calendar.getInstance()
        }
    }
    return now
}

private fun greeting(now: Calendar): String = when (now.get(Calendar.HOUR_OF_DAY)) {
    in 5..11 -> "Good morning"
    in 12..16 -> "Good afternoon"
    else -> "Good evening"
}
