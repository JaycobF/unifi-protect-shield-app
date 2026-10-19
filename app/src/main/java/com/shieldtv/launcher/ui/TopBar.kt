package com.shieldtv.launcher.ui

import android.text.format.DateFormat
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Place
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Icon
import androidx.tv.material3.IconButton
import androidx.tv.material3.IconButtonDefaults
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Tab
import androidx.tv.material3.TabRow
import androidx.tv.material3.TabRowDefaults
import androidx.tv.material3.Text
import kotlinx.coroutines.delay
import java.util.Calendar

enum class LauncherTab(val title: String) { Home("Home"), Apps("Apps") }

/** Greeting on the left, M3 pill tab row in the centre, clock + settings on the right. */
@Composable
fun TopBar(
    selected: LauncherTab,
    onSelect: (LauncherTab) -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
    /** Where the current backdrop photo was taken; shown under the greeting. */
    location: String = "",
) {
    val now = rememberNow()
    val context = LocalContext.current
    val colors = MaterialTheme.colorScheme
    val type = MaterialTheme.typography

    Box(
        modifier
            .fillMaxWidth()
            .padding(start = 56.dp, end = 56.dp, top = 28.dp),
    ) {
        Column(Modifier.align(Alignment.CenterStart)) {
            Text(
                text = greeting(now),
                color = colors.onSurface,
                style = type.titleMedium,
            )
            if (location.isNotEmpty()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Rounded.Place,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = colors.onSurfaceVariant,
                    )
                    Text(
                        text = location,
                        modifier = Modifier
                            .padding(start = 4.dp)
                            .widthIn(max = 260.dp),
                        color = colors.onSurfaceVariant,
                        style = type.labelMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }

        TabRow(
            selectedTabIndex = selected.ordinal,
            modifier = Modifier
                .align(Alignment.Center)
                .background(SurfaceContainers.High, CircleShape)
                .padding(4.dp),
            indicator = { tabPositions, doesTabRowHaveFocus ->
                TabRowDefaults.PillIndicator(
                    currentTabPosition = tabPositions[selected.ordinal],
                    doesTabRowHaveFocus = doesTabRowHaveFocus,
                    activeColor = colors.onSurface,
                    inactiveColor = colors.secondaryContainer,
                )
            },
        ) {
            LauncherTab.entries.forEach { tab ->
                // onFocus selects the tab, matching Google TV's focus-to-switch behaviour.
                Tab(
                    selected = tab == selected,
                    onFocus = { onSelect(tab) },
                    onClick = { onSelect(tab) },
                ) {
                    Text(
                        text = tab.title,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp),
                        style = type.labelLarge,
                    )
                }
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
                    color = colors.onSurface,
                    style = type.titleLarge,
                )
                Text(
                    text = DateFormat.format("EEE, MMM d", now).toString(),
                    color = colors.onSurfaceVariant,
                    style = type.labelMedium,
                )
            }
            IconButton(
                onClick = onOpenSettings,
                colors = IconButtonDefaults.colors(
                    containerColor = SurfaceContainers.High,
                    contentColor = colors.onSurfaceVariant,
                    focusedContainerColor = colors.inverseSurface,
                    focusedContentColor = colors.inverseOnSurface,
                ),
            ) {
                Icon(Icons.Rounded.Settings, contentDescription = "Settings", modifier = Modifier.size(20.dp))
            }
        }
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
