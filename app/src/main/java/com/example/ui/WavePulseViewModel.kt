package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.MediaScanner
import com.example.data.PreferencesManager
import com.example.model.FolderItem
import com.example.model.MediaItem
import com.example.model.VisualEffectType
import com.example.player.MediaPlayerEngine
import com.example.player.PlayerState
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class WavePulseViewModel(application: Application) : AndroidViewModel(application) {

    val preferencesManager = PreferencesManager(application)
    private val mediaScanner = MediaScanner(application)

    val playerEngine = MediaPlayerEngine(
        context = application,
        preferencesManager = preferencesManager,
        scope = viewModelScope
    )

    val playerState: StateFlow<PlayerState> = playerEngine.playerState

    // Raw scanned list
    private val _rawMediaList = MutableStateFlow<List<MediaItem>>(emptyList())
    val rawMediaList: StateFlow<List<MediaItem>> = _rawMediaList.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _hasStoragePermission = MutableStateFlow(false)
    val hasStoragePermission: StateFlow<Boolean> = _hasStoragePermission.asStateFlow()

    // Mode: Focused Player Mode (true = song list hidden, focused on active track)
    private val _isPlayerModeActive = MutableStateFlow(false)
    val isPlayerModeActive: StateFlow<Boolean> = _isPlayerModeActive.asStateFlow()

    // Preferences reactive states
    private val _excludedFolders = MutableStateFlow(preferencesManager.excludedFolders)
    val excludedFolders: StateFlow<Set<String>> = _excludedFolders.asStateFlow()

    private val _minAudioDurationSec = MutableStateFlow(preferencesManager.minAudioDurationSeconds)
    val minAudioDurationSec: StateFlow<Int> = _minAudioDurationSec.asStateFlow()

    private val _fadeEffectsEnabled = MutableStateFlow(preferencesManager.fadeEffectsEnabled)
    val fadeEffectsEnabled: StateFlow<Boolean> = _fadeEffectsEnabled.asStateFlow()

    private val _fadeDurationSeconds = MutableStateFlow(preferencesManager.fadeDurationSeconds)
    val fadeDurationSeconds: StateFlow<Float> = _fadeDurationSeconds.asStateFlow()

    private val _skipDurationSeconds = MutableStateFlow(preferencesManager.skipDurationSeconds)
    val skipDurationSeconds: StateFlow<Int> = _skipDurationSeconds.asStateFlow()

    // Visual Effects & Animations state
    private val _visualEffectsEnabled = MutableStateFlow(preferencesManager.visualEffectsEnabled)
    val visualEffectsEnabled: StateFlow<Boolean> = _visualEffectsEnabled.asStateFlow()

    private val _selectedVisualEffect = MutableStateFlow(VisualEffectType.fromId(preferencesManager.selectedVisualEffectId))
    val selectedVisualEffect: StateFlow<VisualEffectType> = _selectedVisualEffect.asStateFlow()

    // Sleep Timer state
    private val _sleepTimerRemainingSeconds = MutableStateFlow<Int?>(null)
    val sleepTimerRemainingSeconds: StateFlow<Int?> = _sleepTimerRemainingSeconds.asStateFlow()
    private var sleepTimerJob: Job? = null

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    // Navigation and screen states
    private val _selectedTab = MutableStateFlow(0) // 0: All Songs, 1: Folders
    val selectedTab: StateFlow<Int> = _selectedTab.asStateFlow()

    private val _selectedFolder = MutableStateFlow<FolderItem?>(null)
    val selectedFolder: StateFlow<FolderItem?> = _selectedFolder.asStateFlow()

    // Dialog states
    private val _showFilterDialog = MutableStateFlow(false)
    val showFilterDialog: StateFlow<Boolean> = _showFilterDialog.asStateFlow()

    private val _showBlacklistDialog = MutableStateFlow(false)
    val showBlacklistDialog: StateFlow<Boolean> = _showBlacklistDialog.asStateFlow()

    private val _showFadeDialog = MutableStateFlow(false)
    val showFadeDialog: StateFlow<Boolean> = _showFadeDialog.asStateFlow()

    private val _showSkipDialog = MutableStateFlow(false)
    val showSkipDialog: StateFlow<Boolean> = _showSkipDialog.asStateFlow()

    private val _showSleepDialog = MutableStateFlow(false)
    val showSleepDialog: StateFlow<Boolean> = _showSleepDialog.asStateFlow()

    private val _showVisualEffectsDialog = MutableStateFlow(false)
    val showVisualEffectsDialog: StateFlow<Boolean> = _showVisualEffectsDialog.asStateFlow()

    private val _showEqualizerDialog = MutableStateFlow(false)
    val showEqualizerDialog: StateFlow<Boolean> = _showEqualizerDialog.asStateFlow()

    private val _showSongDetailsDialog = MutableStateFlow(false)
    val showSongDetailsDialog: StateFlow<Boolean> = _showSongDetailsDialog.asStateFlow()

    // Accessibility announcement event channel
    private val _accessibilityAnnouncements = MutableSharedFlow<String>(extraBufferCapacity = 5)
    val accessibilityAnnouncements: SharedFlow<String> = _accessibilityAnnouncements.asSharedFlow()

    // All discovered folders across raw storage
    val allDiscoveredFolders: StateFlow<List<FolderItem>> = combine(
        _rawMediaList,
        _excludedFolders
    ) { mediaList, excluded ->
        mediaList.groupBy { it.folderPath }
            .map { (path, items) ->
                val name = items.firstOrNull()?.folderName ?: "Folder"
                val audioCount = items.count { !it.isVideo }
                val videoCount = items.count { it.isVideo }
                val totalDuration = items.sumOf { it.durationMs }
                FolderItem(
                    path = path,
                    name = name,
                    itemCount = items.size,
                    audioCount = audioCount,
                    videoCount = videoCount,
                    totalDurationMs = totalDuration,
                    isIncluded = !excluded.contains(path)
                )
            }
            .sortedBy { it.name.lowercase() }
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    // Active (visible) media items after Folder Blacklist + Short Audio Filter + Search
    val visibleMediaList: StateFlow<List<MediaItem>> = combine(
        _rawMediaList,
        _excludedFolders,
        _minAudioDurationSec,
        _searchQuery
    ) { mediaList, excluded, minDurationSec, query ->
        val minDurationMs = minDurationSec * 1000L

        mediaList.filter { item ->
            // 1. Folder exclusion check
            if (excluded.contains(item.folderPath)) {
                return@filter false
            }

            // 2. Short audio filter check (files under threshold)
            if (minDurationMs > 0 && item.durationMs < minDurationMs) {
                return@filter false
            }

            // 3. Search query filter
            if (query.isNotBlank()) {
                val matchTitle = item.title.contains(query, ignoreCase = true)
                val matchArtist = item.artist.contains(query, ignoreCase = true)
                val matchFolder = item.folderName.contains(query, ignoreCase = true)
                if (!matchTitle && !matchArtist && !matchFolder) {
                    return@filter false
                }
            }

            true
        }
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    // Visible folders in the Folders tab (only non-blacklisted folders with active tracks)
    val visibleFolders: StateFlow<List<FolderItem>> = combine(
        allDiscoveredFolders,
        _excludedFolders
    ) { folders, excluded ->
        folders.filter { !excluded.contains(it.path) }
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    // Count of hidden tracks due to short audio filtering
    val hiddenShortAudioCount: StateFlow<Int> = combine(
        _rawMediaList,
        _excludedFolders,
        _minAudioDurationSec
    ) { mediaList, excluded, minDurationSec ->
        val minDurationMs = minDurationSec * 1000L
        if (minDurationMs <= 0) 0
        else {
            mediaList.count { item ->
                !excluded.contains(item.folderPath) && item.durationMs < minDurationMs
            }
        }
    }.stateIn(viewModelScope, SharingStarted.Lazily, 0)

    init {
        playerEngine.onTrackChangedListener = { track ->
            announceAccessibility("Now playing: ${track.title} by ${track.artist}")
        }
        scanStorage()
    }

    fun setPermissionGranted(granted: Boolean) {
        _hasStoragePermission.value = granted
        if (granted) {
            scanStorage()
        }
    }

    fun scanStorage() {
        viewModelScope.launch {
            _isLoading.value = true
            val items = mediaScanner.scanStorage()
            _rawMediaList.value = items
            _isLoading.value = false

            if (playerEngine.playerState.value.currentTrack == null && items.isNotEmpty()) {
                val visible = visibleMediaList.value.ifEmpty { items }
                playerEngine.setPlaylist(visible, visible.firstOrNull())
            }
        }
    }

    fun selectTab(tabIndex: Int) {
        _selectedTab.value = tabIndex
        _selectedFolder.value = null
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun openFolder(folder: FolderItem) {
        _selectedFolder.value = folder
        announceAccessibility("Opened folder ${folder.name} with ${folder.itemCount} items")
    }

    fun closeFolder() {
        _selectedFolder.value = null
    }

    fun playTrack(track: MediaItem) {
        val currentContext = if (_selectedFolder.value != null) {
            visibleMediaList.value.filter { it.folderPath == _selectedFolder.value!!.path }
        } else {
            visibleMediaList.value
        }
        playerEngine.playTrack(track, currentContext)
        _isPlayerModeActive.value = true // Instant transition to focused Player Mode
    }

    fun playAllInFolder(folder: FolderItem) {
        val folderTracks = visibleMediaList.value.filter { it.folderPath == folder.path }
        if (folderTracks.isNotEmpty()) {
            playerEngine.playTrack(folderTracks.first(), folderTracks)
            _isPlayerModeActive.value = true
            announceAccessibility("Playing all ${folderTracks.size} tracks from ${folder.name}")
        }
    }

    fun enterPlayerMode() {
        if (playerState.value.currentTrack != null) {
            _isPlayerModeActive.value = true
        }
    }

    fun exitPlayerMode() {
        _isPlayerModeActive.value = false
    }

    fun togglePlayPause() {
        playerEngine.togglePlayPause()
        val isNowPlaying = playerEngine.playerState.value.isPlaying
        announceAccessibility(if (isNowPlaying) "Playing" else "Paused")
    }

    fun skipForward() {
        playerEngine.skipForward(_skipDurationSeconds.value)
        announceAccessibility("Skipped forward ${_skipDurationSeconds.value} seconds")
    }

    fun skipBackward() {
        playerEngine.skipBackward(_skipDurationSeconds.value)
        announceAccessibility("Skipped backward ${_skipDurationSeconds.value} seconds")
    }

    fun skipNext() {
        playerEngine.skipNext()
        val currentTrack = playerEngine.playerState.value.currentTrack
        if (currentTrack != null) {
            announceAccessibility("Now playing: ${currentTrack.title}")
        }
    }

    fun skipPrevious() {
        playerEngine.skipPrevious()
        val currentTrack = playerEngine.playerState.value.currentTrack
        if (currentTrack != null) {
            announceAccessibility("Now playing: ${currentTrack.title}")
        }
    }

    // --- Visual Effects & Animations ---

    fun toggleVisualEffects(enabled: Boolean) {
        _visualEffectsEnabled.value = enabled
        preferencesManager.visualEffectsEnabled = enabled
        val msg = if (enabled) "Visual effects and animations enabled" else "Visual effects turned off"
        announceAccessibility(msg)
    }

    fun selectVisualEffect(effect: VisualEffectType) {
        _selectedVisualEffect.value = effect
        preferencesManager.selectedVisualEffectId = effect.id
        announceAccessibility("Applied visual effect: ${effect.title}")
    }

    fun setShowVisualEffectsDialog(show: Boolean) {
        _showVisualEffectsDialog.value = show
    }

    // --- Sleep Timer ---

    fun startSleepTimer(minutes: Int) {
        sleepTimerJob?.cancel()
        val totalSeconds = minutes * 60
        _sleepTimerRemainingSeconds.value = totalSeconds
        announceAccessibility("Sleep timer set for $minutes minutes")

        sleepTimerJob = viewModelScope.launch {
            var currentSec = totalSeconds
            while (isActive && currentSec > 0) {
                delay(1000)
                currentSec--
                _sleepTimerRemainingSeconds.value = currentSec
            }
            if (isActive && currentSec <= 0) {
                _sleepTimerRemainingSeconds.value = null
                playerEngine.smoothFadeAndStop {
                    announceAccessibility("Sleep timer finished. Playback stopped.")
                }
            }
        }
    }

    fun cancelSleepTimer() {
        sleepTimerJob?.cancel()
        sleepTimerJob = null
        _sleepTimerRemainingSeconds.value = null
        announceAccessibility("Sleep timer canceled")
    }

    // --- Skip Duration Settings ---

    fun setSkipDurationSeconds(seconds: Int) {
        _skipDurationSeconds.value = seconds
        preferencesManager.skipDurationSeconds = seconds
        announceAccessibility("Skip duration set to $seconds seconds")
    }

    // --- Blacklist Management ---

    fun toggleFolderBlacklist(folderPath: String) {
        val current = _excludedFolders.value.toMutableSet()
        val isNowExcluded = if (current.contains(folderPath)) {
            current.remove(folderPath)
            false
        } else {
            current.add(folderPath)
            true
        }
        _excludedFolders.value = current
        preferencesManager.excludedFolders = current

        val folderName = folderPath.substringAfterLast('/')
        val status = if (isNowExcluded) "excluded and hidden" else "included in scan"
        announceAccessibility("Folder $folderName $status")
    }

    fun includeAllFolders() {
        _excludedFolders.value = emptySet()
        preferencesManager.excludedFolders = emptySet()
        announceAccessibility("All folders are now included and visible")
    }

    fun excludeAllFolders() {
        val allPaths = allDiscoveredFolders.value.map { it.path }.toSet()
        _excludedFolders.value = allPaths
        preferencesManager.excludedFolders = allPaths
        announceAccessibility("All folders are now excluded")
    }

    // --- Short Audio Filter ---

    fun setMinAudioDurationSec(seconds: Int) {
        _minAudioDurationSec.value = seconds
        preferencesManager.minAudioDurationSeconds = seconds
        val msg = if (seconds <= 0) "Short audio filter turned off" else "Hiding audio files under $seconds seconds"
        announceAccessibility(msg)
    }

    // --- Fade Effects Settings ---

    fun setFadeDurationSeconds(seconds: Float) {
        _fadeDurationSeconds.value = seconds
        preferencesManager.fadeDurationSeconds = seconds
        announceAccessibility("Fade duration set to ${"%.1f".format(seconds)} seconds")
    }

    fun toggleFadeEffects(enabled: Boolean) {
        _fadeEffectsEnabled.value = enabled
        preferencesManager.fadeEffectsEnabled = enabled
        val msg = if (enabled) "Universal fade transitions enabled" else "Fade transitions disabled"
        announceAccessibility(msg)
    }

    // --- Equalizer & Sound Effects ---

    fun applyEqualizerPreset(presetIndex: Short) {
        playerEngine.applyEqualizerPreset(presetIndex)
        announceAccessibility("Applied equalizer preset")
    }

    fun applyBassBoost(strength: Short) {
        playerEngine.applyBassBoost(strength)
    }

    fun applyVirtualizer(strength: Short) {
        playerEngine.applyVirtualizer(strength)
    }

    // --- Controls & Dialogs ---

    fun toggleKeepScreenAwake(): Boolean {
        val newState = playerEngine.toggleKeepScreenAwake()
        val msg = if (newState) "Keep Screen Awake is now ON" else "Keep Screen Awake is now OFF"
        announceAccessibility(msg)
        return newState
    }

    fun setShowFilterDialog(show: Boolean) {
        _showFilterDialog.value = show
    }

    fun setShowBlacklistDialog(show: Boolean) {
        _showBlacklistDialog.value = show
    }

    fun setShowFadeDialog(show: Boolean) {
        _showFadeDialog.value = show
    }

    fun setShowSkipDialog(show: Boolean) {
        _showSkipDialog.value = show
    }

    fun setShowSleepDialog(show: Boolean) {
        _showSleepDialog.value = show
    }

    fun setShowEqualizerDialog(show: Boolean) {
        _showEqualizerDialog.value = show
    }

    fun setShowSongDetailsDialog(show: Boolean) {
        _showSongDetailsDialog.value = show
    }

    fun announceAccessibility(text: String) {
        viewModelScope.launch {
            _accessibilityAnnouncements.emit(text)
        }
    }

    override fun onCleared() {
        super.onCleared()
        sleepTimerJob?.cancel()
        playerEngine.release()
    }
}
