package com.example.ui

import android.Manifest
import android.app.Activity
import android.os.Build
import android.view.WindowManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.ui.components.EqualizerDialog
import com.example.ui.components.FadeSettingsDialog
import com.example.ui.components.FilterSettingsDialog
import com.example.ui.components.FolderBlacklistDialog
import com.example.ui.components.MiniPlayer
import com.example.ui.components.SkipSettingsDialog
import com.example.ui.components.SleepTimerDialog
import com.example.ui.components.SmartMusicTopBar
import com.example.ui.components.SongDetailsDialog
import com.example.ui.components.VisualEffectsDialog
import com.example.ui.screens.AllTracksScreen
import com.example.ui.screens.FoldersScreen
import com.example.ui.screens.PlayerModeScreen

@Composable
fun WavePulseApp(
    viewModel: WavePulseViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val view = LocalView.current
    val snackbarHostState = remember { SnackbarHostState() }

    val playerState by viewModel.playerState.collectAsStateWithLifecycle()
    val isPlayerModeActive by viewModel.isPlayerModeActive.collectAsStateWithLifecycle()
    val visibleTracks by viewModel.visibleMediaList.collectAsStateWithLifecycle()
    val visibleFolders by viewModel.visibleFolders.collectAsStateWithLifecycle()
    val allDiscoveredFolders by viewModel.allDiscoveredFolders.collectAsStateWithLifecycle()
    val excludedFolders by viewModel.excludedFolders.collectAsStateWithLifecycle()
    val minAudioDurationSec by viewModel.minAudioDurationSec.collectAsStateWithLifecycle()
    val hiddenShortAudioCount by viewModel.hiddenShortAudioCount.collectAsStateWithLifecycle()
    val fadeEffectsEnabled by viewModel.fadeEffectsEnabled.collectAsStateWithLifecycle()
    val fadeDurationSeconds by viewModel.fadeDurationSeconds.collectAsStateWithLifecycle()
    val skipDurationSeconds by viewModel.skipDurationSeconds.collectAsStateWithLifecycle()
    val sleepTimerRemainingSeconds by viewModel.sleepTimerRemainingSeconds.collectAsStateWithLifecycle()
    val selectedTab by viewModel.selectedTab.collectAsStateWithLifecycle()
    val selectedFolder by viewModel.selectedFolder.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()

    val showFilterDialog by viewModel.showFilterDialog.collectAsStateWithLifecycle()
    val showBlacklistDialog by viewModel.showBlacklistDialog.collectAsStateWithLifecycle()
    val showFadeDialog by viewModel.showFadeDialog.collectAsStateWithLifecycle()
    val showSkipDialog by viewModel.showSkipDialog.collectAsStateWithLifecycle()
    val showSleepDialog by viewModel.showSleepDialog.collectAsStateWithLifecycle()
    val showEqualizerDialog by viewModel.showEqualizerDialog.collectAsStateWithLifecycle()
    val showSongDetailsDialog by viewModel.showSongDetailsDialog.collectAsStateWithLifecycle()
    val visualEffectsEnabled by viewModel.visualEffectsEnabled.collectAsStateWithLifecycle()
    val selectedVisualEffect by viewModel.selectedVisualEffect.collectAsStateWithLifecycle()
    val showVisualEffectsDialog by viewModel.showVisualEffectsDialog.collectAsStateWithLifecycle()

    // Keep Screen Awake (Wake Lock) via WindowManager flag
    DisposableEffect(playerState.isKeepScreenAwake, playerState.isPlaying) {
        val window = (context as? Activity)?.window
        val shouldKeepScreenOn = playerState.isKeepScreenAwake && playerState.isPlaying
        if (shouldKeepScreenOn) {
            window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
        onDispose {
            window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    // Accessibility announcements broadcast
    LaunchedEffect(Unit) {
        viewModel.accessibilityAnnouncements.collect { announcement ->
            view.announceForAccessibility(announcement)
            snackbarHostState.showSnackbar(announcement)
        }
    }

    // Permission launcher for audio media
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val isGranted = permissions.values.any { it }
        viewModel.setPermissionGranted(isGranted)
    }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissionLauncher.launch(arrayOf(Manifest.permission.READ_MEDIA_AUDIO))
        } else {
            permissionLauncher.launch(arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE))
        }
    }

    // Screen transition: Focused Player Mode vs Home Screen (Song list & Folders)
    AnimatedContent(
        targetState = isPlayerModeActive && playerState.currentTrack != null,
        transitionSpec = {
            if (targetState) {
                slideInVertically { it } + fadeIn() togetherWith slideOutVertically { -it } + fadeOut()
            } else {
                slideInVertically { -it } + fadeIn() togetherWith slideOutVertically { it } + fadeOut()
            }
        },
        label = "player_mode_transition"
    ) { inPlayerMode ->
        if (inPlayerMode) {
            // Focused Player Mode: Song list is completely hidden!
            PlayerModeScreen(
                playerState = playerState,
                fadeEffectsEnabled = fadeEffectsEnabled,
                fadeDurationSeconds = fadeDurationSeconds,
                skipDurationSeconds = skipDurationSeconds,
                sleepTimerRemainingSeconds = sleepTimerRemainingSeconds,
                visualEffectsEnabled = visualEffectsEnabled,
                selectedVisualEffect = selectedVisualEffect,
                onBackToHome = { viewModel.exitPlayerMode() },
                onTogglePlayPause = { viewModel.togglePlayPause() },
                onSkipNext = { viewModel.skipNext() },
                onSkipPrevious = { viewModel.skipPrevious() },
                onSkipForward = { viewModel.skipForward() },
                onSkipBackward = { viewModel.skipBackward() },
                onSeekTo = { viewModel.playerEngine.seekTo(it) },
                onToggleShuffle = { viewModel.playerEngine.toggleShuffle() },
                onCycleRepeat = { viewModel.playerEngine.cycleRepeatMode() },
                onToggleKeepScreenAwake = { viewModel.toggleKeepScreenAwake() },
                onOpenVisualEffects = { viewModel.setShowVisualEffectsDialog(true) },
                onOpenEqualizer = { viewModel.setShowEqualizerDialog(true) },
                onOpenSongDetails = { viewModel.setShowSongDetailsDialog(true) },
                onOpenFadeSettings = { viewModel.setShowFadeDialog(true) },
                onOpenSkipSettings = { viewModel.setShowSkipDialog(true) },
                onOpenSleepTimer = { viewModel.setShowSleepDialog(true) },
                onOpenBlacklistSettings = { viewModel.setShowBlacklistDialog(true) }
            )
        } else {
            // Home Screen: Song list and Folders with global 3-dot settings and mini-player
            Scaffold(
                modifier = modifier.fillMaxSize(),
                snackbarHost = { SnackbarHost(snackbarHostState) },
                topBar = {
                    SmartMusicTopBar(
                        isKeepScreenAwake = playerState.isKeepScreenAwake,
                        onToggleKeepScreenAwake = { viewModel.toggleKeepScreenAwake() },
                        minAudioDurationSec = minAudioDurationSec,
                        fadeDurationSeconds = fadeDurationSeconds,
                        skipDurationSeconds = skipDurationSeconds,
                        sleepTimerRemainingSeconds = sleepTimerRemainingSeconds,
                        onOpenFilterDialog = { viewModel.setShowFilterDialog(true) },
                        onOpenBlacklistDialog = { viewModel.setShowBlacklistDialog(true) },
                        onOpenFadeSettings = { viewModel.setShowFadeDialog(true) },
                        onOpenSkipSettings = { viewModel.setShowSkipDialog(true) },
                        onOpenSleepTimer = { viewModel.setShowSleepDialog(true) },
                        onRescan = { viewModel.scanStorage() },
                        searchQuery = searchQuery,
                        onSearchQueryChange = { viewModel.setSearchQuery(it) }
                    )
                },
                bottomBar = {
                    AnimatedVisibility(
                        visible = playerState.currentTrack != null,
                        enter = slideInVertically(initialOffsetY = { it }),
                        exit = slideOutVertically(targetOffsetY = { it })
                    ) {
                        MiniPlayer(
                            playerState = playerState,
                            onTogglePlayPause = { viewModel.togglePlayPause() },
                            onSkipNext = { viewModel.skipNext() },
                            onSkipPrevious = { viewModel.skipPrevious() },
                            onOpenExpandedPlayer = { viewModel.enterPlayerMode() }
                        )
                    }
                }
            ) { innerPadding ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    TabRow(
                        selectedTabIndex = selectedTab,
                        containerColor = MaterialTheme.colorScheme.surface,
                        contentColor = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.testTag("main_tabs")
                    ) {
                        Tab(
                            selected = selectedTab == 0,
                            onClick = { viewModel.selectTab(0) },
                            text = { Text(stringResource(R.string.tab_all_songs), fontWeight = FontWeight.Bold) },
                            icon = { Icon(Icons.Default.Audiotrack, contentDescription = null, modifier = Modifier.size(18.dp)) },
                            modifier = Modifier
                                .testTag("tab_all_songs")
                                .semantics { contentDescription = "All Songs tab, showing ${visibleTracks.size} items" }
                        )
                        Tab(
                            selected = selectedTab == 1,
                            onClick = { viewModel.selectTab(1) },
                            text = { Text(stringResource(R.string.tab_folders), fontWeight = FontWeight.Bold) },
                            icon = { Icon(Icons.Default.Folder, contentDescription = null, modifier = Modifier.size(18.dp)) },
                            modifier = Modifier
                                .testTag("tab_folders")
                                .semantics { contentDescription = "Folders tab, showing ${visibleFolders.size} active folders" }
                        )
                    }

                    when (selectedTab) {
                        0 -> AllTracksScreen(
                            tracks = visibleTracks,
                            playerState = playerState,
                            minAudioDurationSec = minAudioDurationSec,
                            hiddenShortAudioCount = hiddenShortAudioCount,
                            excludedFoldersCount = excludedFolders.size,
                            onTrackClick = { viewModel.playTrack(it) },
                            onOpenFilterDialog = { viewModel.setShowFilterDialog(true) },
                            onOpenBlacklistDialog = { viewModel.setShowBlacklistDialog(true) }
                        )
                        1 -> FoldersScreen(
                            folders = visibleFolders,
                            selectedFolder = selectedFolder,
                            allTracks = visibleTracks,
                            playerState = playerState,
                            onSelectFolder = { viewModel.openFolder(it) },
                            onCloseFolder = { viewModel.closeFolder() },
                            onPlayTrack = { viewModel.playTrack(it) },
                            onPlayAllInFolder = { viewModel.playAllInFolder(it) },
                            onExcludeFolder = { viewModel.toggleFolderBlacklist(it) },
                            onOpenBlacklistDialog = { viewModel.setShowBlacklistDialog(true) }
                        )
                    }
                }
            }
        }
    }

    // Dialog: Folder Blacklist / Whitelist Selector
    if (showBlacklistDialog) {
        FolderBlacklistDialog(
            allFolders = allDiscoveredFolders,
            excludedFolders = excludedFolders,
            onToggleFolder = { viewModel.toggleFolderBlacklist(it) },
            onIncludeAll = { viewModel.includeAllFolders() },
            onExcludeAll = { viewModel.excludeAllFolders() },
            onDismiss = { viewModel.setShowBlacklistDialog(false) }
        )
    }

    // Dialog: Filter Settings (Ignore short audio files like WhatsApp/ringtones)
    if (showFilterDialog) {
        FilterSettingsDialog(
            currentMinSeconds = minAudioDurationSec,
            hiddenCount = hiddenShortAudioCount,
            onSelectMinSeconds = { viewModel.setMinAudioDurationSec(it) },
            onDismiss = { viewModel.setShowFilterDialog(false) }
        )
    }

    // Dialog: Customizable Universal Fade Audio Settings
    if (showFadeDialog) {
        FadeSettingsDialog(
            fadeEffectsEnabled = fadeEffectsEnabled,
            currentDurationSeconds = fadeDurationSeconds,
            onToggleFadeEffects = { viewModel.toggleFadeEffects(it) },
            onChangeDuration = { viewModel.setFadeDurationSeconds(it) },
            onDismiss = { viewModel.setShowFadeDialog(false) }
        )
    }

    // Dialog: Customizable Skip Duration Settings
    if (showSkipDialog) {
        SkipSettingsDialog(
            currentSkipSeconds = skipDurationSeconds,
            onSelectSkipSeconds = { viewModel.setSkipDurationSeconds(it) },
            onDismiss = { viewModel.setShowSkipDialog(false) }
        )
    }

    // Dialog: Sleep Timer (with smooth auto fade-out and stop)
    if (showSleepDialog) {
        SleepTimerDialog(
            remainingSeconds = sleepTimerRemainingSeconds,
            onStartTimerMinutes = { viewModel.startSleepTimer(it) },
            onCancelTimer = { viewModel.cancelSleepTimer() },
            onDismiss = { viewModel.setShowSleepDialog(false) }
        )
    }

    // Dialog: Equalizer & Sound Effects
    if (showEqualizerDialog) {
        EqualizerDialog(
            currentPreset = viewModel.preferencesManager.equalizerPreset,
            bassBoost = viewModel.preferencesManager.bassBoostLevel,
            virtualizer = viewModel.preferencesManager.virtualizerLevel,
            onSelectPreset = { viewModel.applyEqualizerPreset(it) },
            onChangeBassBoost = { viewModel.applyBassBoost(it) },
            onChangeVirtualizer = { viewModel.applyVirtualizer(it) },
            onDismiss = { viewModel.setShowEqualizerDialog(false) }
        )
    }

    // Dialog: Song Details
    if (showSongDetailsDialog) {
        SongDetailsDialog(
            track = playerState.currentTrack,
            onDismiss = { viewModel.setShowSongDetailsDialog(false) }
        )
    }

    // Dialog: Visual Effects & Animations (20 variations)
    if (showVisualEffectsDialog) {
        VisualEffectsDialog(
            visualEffectsEnabled = visualEffectsEnabled,
            currentEffect = selectedVisualEffect,
            onToggleEnabled = { viewModel.toggleVisualEffects(it) },
            onSelectEffect = { viewModel.selectVisualEffect(it) },
            onDismiss = { viewModel.setShowVisualEffectsDialog(false) }
        )
    }
}
