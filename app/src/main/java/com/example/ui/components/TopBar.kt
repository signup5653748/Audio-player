package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.HourglassBottom
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.WbIncandescent
import androidx.compose.material.icons.outlined.WbIncandescent
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SmartMusicTopBar(
    isKeepScreenAwake: Boolean,
    onToggleKeepScreenAwake: () -> Unit,
    minAudioDurationSec: Int,
    fadeDurationSeconds: Float,
    skipDurationSeconds: Int,
    sleepTimerRemainingSeconds: Int?,
    onOpenFilterDialog: () -> Unit,
    onOpenBlacklistDialog: () -> Unit,
    onOpenFadeSettings: () -> Unit,
    onOpenSkipSettings: () -> Unit,
    onOpenSleepTimer: () -> Unit,
    onRescan: () -> Unit,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit
) {
    var isSearchActive by remember { mutableStateOf(false) }
    var showMoreMenu by remember { mutableStateOf(false) }

    if (isSearchActive) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                .padding(horizontal = 8.dp, vertical = 6.dp)
        ) {
            TextField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("search_text_field"),
                placeholder = { Text("Search songs, artists, folders...") },
                singleLine = true,
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search"
                    )
                },
                trailingIcon = {
                    IconButton(
                        onClick = {
                            if (searchQuery.isNotEmpty()) {
                                onSearchQueryChange("")
                            } else {
                                isSearchActive = false
                            }
                        },
                        modifier = Modifier.testTag("close_search_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close search"
                        )
                    }
                },
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            )
        }
    } else {
        CenterAlignedTopAppBar(
            title = {
                Text(
                    text = "Smart Music Player",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            },
            colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            navigationIcon = {
                IconButton(
                    onClick = { isSearchActive = true },
                    modifier = Modifier
                        .testTag("search_button")
                        .semantics { contentDescription = "Search songs and folders" }
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null
                    )
                }
            },
            actions = {
                // Keep Screen Awake (Wake Lock) Quick Toggle
                IconButton(
                    onClick = onToggleKeepScreenAwake,
                    modifier = Modifier
                        .testTag("home_wake_lock_toggle")
                        .semantics {
                            contentDescription = if (isKeepScreenAwake) {
                                "Keep Screen Awake is ON. Double tap to disable."
                            } else {
                                "Keep Screen Awake is OFF. Double tap to enable."
                            }
                        }
                ) {
                    Icon(
                        imageVector = if (isKeepScreenAwake) Icons.Filled.WbIncandescent else Icons.Outlined.WbIncandescent,
                        contentDescription = null,
                        tint = if (isKeepScreenAwake) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Global 'More Options' (3-Dot) Menu
                Box {
                    IconButton(
                        onClick = { showMoreMenu = true },
                        modifier = Modifier
                            .testTag("home_more_options_button")
                            .semantics { contentDescription = "Global app settings and options" }
                    ) {
                        BadgedBox(
                            badge = {
                                if (minAudioDurationSec > 0 || sleepTimerRemainingSeconds != null) {
                                    Badge(
                                        containerColor = if (sleepTimerRemainingSeconds != null) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = null
                            )
                        }
                    }

                    DropdownMenu(
                        expanded = showMoreMenu,
                        onDismissRequest = { showMoreMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Folder Selection & Blacklist") },
                            onClick = {
                                showMoreMenu = false
                                onOpenBlacklistDialog()
                            },
                            leadingIcon = { Icon(Icons.Default.Folder, contentDescription = null) },
                            modifier = Modifier.testTag("home_menu_blacklist")
                        )
                        DropdownMenuItem(
                            text = {
                                Text(if (minAudioDurationSec > 0) "Filter Short Audio (${minAudioDurationSec}s)" else "Filter Short Audio (Off)")
                            },
                            onClick = {
                                showMoreMenu = false
                                onOpenFilterDialog()
                            },
                            leadingIcon = { Icon(Icons.Default.HourglassBottom, contentDescription = null) },
                            modifier = Modifier.testTag("home_menu_filter")
                        )
                        DropdownMenuItem(
                            text = { Text("Fade Duration (${"%.1f".format(fadeDurationSeconds)}s)") },
                            onClick = {
                                showMoreMenu = false
                                onOpenFadeSettings()
                            },
                            leadingIcon = { Icon(Icons.Default.Tune, contentDescription = null) },
                            modifier = Modifier.testTag("home_menu_fade")
                        )
                        DropdownMenuItem(
                            text = { Text("Skip Duration (${skipDurationSeconds}s)") },
                            onClick = {
                                showMoreMenu = false
                                onOpenSkipSettings()
                            },
                            leadingIcon = { Icon(Icons.Default.FastForward, contentDescription = null) },
                            modifier = Modifier.testTag("home_menu_skip")
                        )
                        DropdownMenuItem(
                            text = {
                                val label = if (sleepTimerRemainingSeconds != null) {
                                    val m = sleepTimerRemainingSeconds / 60
                                    "Sleep Timer (${m}m left)"
                                } else "Sleep Timer"
                                Text(label)
                            },
                            onClick = {
                                showMoreMenu = false
                                onOpenSleepTimer()
                            },
                            leadingIcon = { Icon(Icons.Default.Bedtime, contentDescription = null) },
                            modifier = Modifier.testTag("home_menu_sleep")
                        )
                        DropdownMenuItem(
                            text = {
                                Text(if (isKeepScreenAwake) "Keep Screen Awake: ON" else "Keep Screen Awake: OFF")
                            },
                            onClick = {
                                showMoreMenu = false
                                onToggleKeepScreenAwake()
                            },
                            leadingIcon = { Icon(Icons.Default.WbIncandescent, contentDescription = null) },
                            modifier = Modifier.testTag("home_menu_wake_lock")
                        )
                        DropdownMenuItem(
                            text = { Text("Rescan Audio Storage") },
                            onClick = {
                                showMoreMenu = false
                                onRescan()
                            },
                            leadingIcon = { Icon(Icons.Default.Refresh, contentDescription = null) },
                            modifier = Modifier.testTag("home_menu_rescan")
                        )
                    }
                }
            }
        )
    }
}
