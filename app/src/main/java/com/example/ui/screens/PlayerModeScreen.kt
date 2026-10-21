package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode as AnimRepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.WbIncandescent
import androidx.compose.material.icons.outlined.WbIncandescent
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.MediaItem
import com.example.player.PlayerState
import com.example.player.RepeatMode
import kotlin.math.sin

@Composable
fun PlayerModeScreen(
    playerState: PlayerState,
    fadeEffectsEnabled: Boolean,
    fadeDurationSeconds: Float,
    skipDurationSeconds: Int,
    sleepTimerRemainingSeconds: Int?,
    onBackToHome: () -> Unit,
    onTogglePlayPause: () -> Unit,
    onSkipNext: () -> Unit,
    onSkipPrevious: () -> Unit,
    onSkipForward: () -> Unit,
    onSkipBackward: () -> Unit,
    onSeekTo: (Long) -> Unit,
    onToggleShuffle: () -> Unit,
    onCycleRepeat: () -> Unit,
    onToggleKeepScreenAwake: () -> Unit,
    onOpenEqualizer: () -> Unit,
    onOpenSongDetails: () -> Unit,
    onOpenFadeSettings: () -> Unit,
    onOpenSkipSettings: () -> Unit,
    onOpenSleepTimer: () -> Unit,
    onOpenBlacklistSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val track = playerState.currentTrack ?: return
    var showMoreMenu by remember { mutableStateOf(false) }

    // Intercept hardware and gesture back presses to return to song list
    BackHandler { onBackToHome() }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .testTag("player_mode_screen"),
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Back Button returning to Home Song List
                IconButton(
                    onClick = onBackToHome,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("player_back_button")
                        .semantics { contentDescription = "Back to song list" }
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = null,
                        modifier = Modifier.size(28.dp)
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "PLAYER MODE",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.2.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = track.folderName,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Keep Screen Awake (Wake Lock) Toggle
                    IconButton(
                        onClick = onToggleKeepScreenAwake,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("player_wake_lock_button")
                            .semantics {
                                contentDescription = if (playerState.isKeepScreenAwake) {
                                    "Keep Screen Awake is ON. Double tap to disable."
                                } else {
                                    "Keep Screen Awake is OFF. Double tap to enable."
                                }
                            }
                    ) {
                        Icon(
                            imageVector = if (playerState.isKeepScreenAwake) Icons.Filled.WbIncandescent else Icons.Outlined.WbIncandescent,
                            contentDescription = null,
                            tint = if (playerState.isKeepScreenAwake) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Dedicated Player Mode 'More Options' (3-Dot) Menu
                    Box {
                        IconButton(
                            onClick = { showMoreMenu = true },
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("player_more_options_button")
                                .semantics { contentDescription = "More player and track options" }
                        ) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = null
                            )
                        }

                        DropdownMenu(
                            expanded = showMoreMenu,
                            onDismissRequest = { showMoreMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Equalizer & Sound Effects") },
                                onClick = {
                                    showMoreMenu = false
                                    onOpenEqualizer()
                                },
                                leadingIcon = { Icon(Icons.Default.Equalizer, contentDescription = null) },
                                modifier = Modifier.testTag("menu_equalizer")
                            )
                            DropdownMenuItem(
                                text = { Text("Fade Duration (${"%.1f".format(fadeDurationSeconds)}s)") },
                                onClick = {
                                    showMoreMenu = false
                                    onOpenFadeSettings()
                                },
                                leadingIcon = { Icon(Icons.Default.Tune, contentDescription = null) },
                                modifier = Modifier.testTag("menu_fade_settings")
                            )
                            DropdownMenuItem(
                                text = { Text("Skip Duration (${skipDurationSeconds}s)") },
                                onClick = {
                                    showMoreMenu = false
                                    onOpenSkipSettings()
                                },
                                leadingIcon = { Icon(Icons.Default.FastForward, contentDescription = null) },
                                modifier = Modifier.testTag("menu_skip_settings")
                            )
                            DropdownMenuItem(
                                text = {
                                    val sleepLabel = if (sleepTimerRemainingSeconds != null) {
                                        val mins = sleepTimerRemainingSeconds / 60
                                        "Sleep Timer (${mins}m left)"
                                    } else "Sleep Timer"
                                    Text(sleepLabel)
                                },
                                onClick = {
                                    showMoreMenu = false
                                    onOpenSleepTimer()
                                },
                                leadingIcon = { Icon(Icons.Default.Bedtime, contentDescription = null) },
                                modifier = Modifier.testTag("menu_sleep_timer")
                            )
                            DropdownMenuItem(
                                text = { Text("Song Details") },
                                onClick = {
                                    showMoreMenu = false
                                    onOpenSongDetails()
                                },
                                leadingIcon = { Icon(Icons.Default.Info, contentDescription = null) },
                                modifier = Modifier.testTag("menu_song_details")
                            )
                            DropdownMenuItem(
                                text = { Text("Folder Selection & Blacklist") },
                                onClick = {
                                    showMoreMenu = false
                                    onOpenBlacklistSettings()
                                },
                                leadingIcon = { Icon(Icons.Default.Folder, contentDescription = null) },
                                modifier = Modifier.testTag("menu_blacklist_settings")
                            )
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp)
                .padding(bottom = 20.dp)
                .verticalScroll(rememberScrollState())
                .widthIn(max = 600.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Spacer(modifier = Modifier.height(4.dp))

            // Large Animated Visualizer / Cover Art Canvas
            PlayerWaveformArtwork(
                isPlaying = playerState.isPlaying,
                modifier = Modifier
                    .size(240.dp)
                    .clip(RoundedCornerShape(26.dp))
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Song Title and Artist
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = track.title,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = track.artist,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Indicator Badges: Folder tag, Fade effect status, Sleep timer badge
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Folder,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = track.folderName,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Fade In / Out Active Indicator Badge
                FilterChip(
                    selected = fadeEffectsEnabled,
                    onClick = onOpenFadeSettings,
                    label = {
                        Text(
                            text = if (playerState.isFadingIn) "Fading In..."
                            else if (playerState.isFadingOut) "Fading Out..."
                            else "Fade: ${"%.1f".format(fadeDurationSeconds)}s",
                            style = MaterialTheme.typography.labelSmall
                        )
                    },
                    modifier = Modifier.testTag("player_fade_chip")
                )

                // Sleep Timer badge if active
                if (sleepTimerRemainingSeconds != null) {
                    val mins = sleepTimerRemainingSeconds / 60
                    val secs = sleepTimerRemainingSeconds % 60
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.tertiaryContainer,
                        modifier = Modifier.testTag("player_sleep_timer_badge")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Bedtime,
                                contentDescription = null,
                                modifier = Modifier.size(12.dp),
                                tint = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "%d:%02d".format(mins, secs),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Smooth, Responsive Scrubber / Seek Bar
            var sliderPosition by remember { mutableFloatStateOf(0f) }
            var isUserSeeking by remember { mutableStateOf(false) }

            val currentFraction = if (isUserSeeking) sliderPosition else playerState.progressFraction

            Column(modifier = Modifier.fillMaxWidth()) {
                Slider(
                    value = currentFraction,
                    onValueChange = { fraction ->
                        isUserSeeking = true
                        sliderPosition = fraction
                    },
                    onValueChangeFinished = {
                        val targetMs = (sliderPosition * playerState.durationMs).toLong()
                        onSeekTo(targetMs)
                        isUserSeeking = false
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("player_seek_slider")
                        .semantics {
                            contentDescription = "Song playback progress slider"
                            stateDescription = "${playerState.positionFormatted} of ${playerState.durationFormatted}"
                        },
                    colors = SliderDefaults.colors(
                        thumbColor = MaterialTheme.colorScheme.primary,
                        activeTrackColor = MaterialTheme.colorScheme.primary
                    )
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = playerState.positionFormatted,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = playerState.durationFormatted,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Main Playback Controls: Shuffle, Previous, Skip Backward, Play/Pause FAB, Skip Forward, Next, Repeat
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Shuffle Toggle
                IconButton(
                    onClick = onToggleShuffle,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("player_shuffle_button")
                        .semantics {
                            contentDescription = if (playerState.isShuffleEnabled) "Shuffle is ON" else "Shuffle is OFF"
                        }
                ) {
                    Icon(
                        imageVector = Icons.Default.Shuffle,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                        tint = if (playerState.isShuffleEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Previous Track
                IconButton(
                    onClick = onSkipPrevious,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("player_prev_button")
                        .semantics { contentDescription = "Previous track" }
                ) {
                    Icon(
                        imageVector = Icons.Default.SkipPrevious,
                        contentDescription = null,
                        modifier = Modifier.size(32.dp)
                    )
                }

                // Dedicated Skip Backward (-X seconds)
                IconButton(
                    onClick = onSkipBackward,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("player_skip_backward_button")
                        .semantics { contentDescription = "Skip backward $skipDurationSeconds seconds" }
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = if (skipDurationSeconds == 10) Icons.Default.Replay10 else Icons.Default.FastRewind,
                            contentDescription = null,
                            modifier = Modifier.size(26.dp)
                        )
                        Text(
                            text = "-${skipDurationSeconds}s",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Large Play/Pause FAB
                FloatingActionButton(
                    onClick = onTogglePlayPause,
                    shape = CircleShape,
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    elevation = FloatingActionButtonDefaults.elevation(6.dp),
                    modifier = Modifier
                        .size(72.dp)
                        .testTag("player_play_pause_button")
                        .semantics {
                            contentDescription = if (playerState.isPlaying) "Pause track" else "Play track"
                        }
                ) {
                    Icon(
                        imageVector = if (playerState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(40.dp)
                    )
                }

                // Dedicated Skip Forward (+X seconds)
                IconButton(
                    onClick = onSkipForward,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("player_skip_forward_button")
                        .semantics { contentDescription = "Skip forward $skipDurationSeconds seconds" }
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = if (skipDurationSeconds == 10) Icons.Default.Forward10 else Icons.Default.FastForward,
                            contentDescription = null,
                            modifier = Modifier.size(26.dp)
                        )
                        Text(
                            text = "+${skipDurationSeconds}s",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Next Track
                IconButton(
                    onClick = onSkipNext,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("player_next_button")
                        .semantics { contentDescription = "Next track" }
                ) {
                    Icon(
                        imageVector = Icons.Default.SkipNext,
                        contentDescription = null,
                        modifier = Modifier.size(32.dp)
                    )
                }

                // Repeat Mode Toggle
                IconButton(
                    onClick = onCycleRepeat,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("player_repeat_button")
                        .semantics {
                            contentDescription = when (playerState.repeatMode) {
                                RepeatMode.OFF -> "Repeat off"
                                RepeatMode.ALL -> "Repeat all songs"
                                RepeatMode.ONE -> "Repeat current song"
                            }
                        }
                ) {
                    Icon(
                        imageVector = when (playerState.repeatMode) {
                            RepeatMode.ONE -> Icons.Default.RepeatOne
                            else -> Icons.Default.Repeat
                        },
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                        tint = if (playerState.repeatMode != RepeatMode.OFF) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Quick Shortcut Pills: Equalizer, Skip Duration, Sleep Timer
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilterChip(
                    selected = false,
                    onClick = onOpenEqualizer,
                    label = { Text("Equalizer", style = MaterialTheme.typography.labelSmall) },
                    leadingIcon = { Icon(Icons.Default.Equalizer, contentDescription = null, modifier = Modifier.size(14.dp)) },
                    modifier = Modifier.testTag("quick_eq_button")
                )
                Spacer(modifier = Modifier.width(8.dp))
                FilterChip(
                    selected = false,
                    onClick = onOpenSkipSettings,
                    label = { Text("Skip: ${skipDurationSeconds}s", style = MaterialTheme.typography.labelSmall) },
                    leadingIcon = { Icon(Icons.Default.FastForward, contentDescription = null, modifier = Modifier.size(14.dp)) },
                    modifier = Modifier.testTag("quick_skip_button")
                )
                Spacer(modifier = Modifier.width(8.dp))
                FilterChip(
                    selected = sleepTimerRemainingSeconds != null,
                    onClick = onOpenSleepTimer,
                    label = {
                        val text = if (sleepTimerRemainingSeconds != null) {
                            val mins = sleepTimerRemainingSeconds / 60
                            "Sleep: ${mins}m"
                        } else "Sleep Timer"
                        Text(text, style = MaterialTheme.typography.labelSmall)
                    },
                    leadingIcon = { Icon(Icons.Default.Bedtime, contentDescription = null, modifier = Modifier.size(14.dp)) },
                    modifier = Modifier.testTag("quick_sleep_button")
                )
            }
        }
    }
}

/**
 * Animated dynamic waveform art placeholder for Player Mode.
 */
@Composable
fun PlayerWaveformArtwork(
    isPlaying: Boolean,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "player_wave")
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = if (isPlaying) (2 * Math.PI).toFloat() else 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2000, easing = LinearEasing),
            repeatMode = AnimRepeatMode.Restart
        ),
        label = "phase"
    )

    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.secondary

    Box(
        modifier = modifier
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primaryContainer,
                        MaterialTheme.colorScheme.surfaceVariant
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val barCount = 20
            val barWidth = size.width / (barCount * 1.8f)
            val spacing = barWidth * 0.8f
            val startX = (size.width - (barCount * (barWidth + spacing))) / 2f
            val centerY = size.height / 2f

            for (i in 0 until barCount) {
                val x = startX + i * (barWidth + spacing)
                val waveFactor = if (isPlaying) {
                    (sin(phase + i * 0.4) * 0.42 + 0.58).toFloat()
                } else {
                    0.25f
                }
                val barHeight = (size.height * 0.48f) * waveFactor
                val topY = centerY - barHeight / 2f

                drawRoundRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(primaryColor, secondaryColor),
                        startY = topY,
                        endY = topY + barHeight
                    ),
                    topLeft = Offset(x, topY),
                    size = androidx.compose.ui.geometry.Size(barWidth, barHeight),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(barWidth / 2f, barWidth / 2f)
                )
            }
        }

        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
            modifier = Modifier.size(64.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.MusicNote,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(34.dp)
                )
            }
        }
    }
}

