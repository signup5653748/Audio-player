package com.example.player

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import android.media.audiofx.Virtualizer
import android.os.Build
import android.util.Log
import com.example.data.PreferencesManager
import com.example.model.MediaItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream

/**
 * Robust, production-grade audio playback engine:
 * - Thread-safe, synchronized playback state machine
 * - Deterministic, instant Play/Pause toggle with zero latency or state mismatch
 * - Universal, smooth fade-in and fade-out audio transitions
 * - Seamless Next / Previous track switching with FileDescriptor data source loading
 * - Automatic error recovery so playback never halts or leaves the player dead
 * - Equalizer, Bass Boost, 3D Virtualizer, and Sleep Timer integration
 */
class MediaPlayerEngine(
    private val context: Context,
    private val preferencesManager: PreferencesManager,
    private val scope: CoroutineScope
) {
    companion object {
        private const val TAG = "MediaPlayerEngine"
    }

    private var mediaPlayer: MediaPlayer? = null
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private var audioFocusRequest: AudioFocusRequest? = null

    // Audio effects
    private var equalizer: Equalizer? = null
    private var bassBoost: BassBoost? = null
    private var virtualizer: Virtualizer? = null

    private val _playerState = MutableStateFlow(PlayerState())
    val playerState: StateFlow<PlayerState> = _playerState.asStateFlow()

    private var playlist: List<MediaItem> = emptyList()
    private var currentIndex: Int = -1

    // Concurrency guards and explicit intent tracking
    private val stateMutex = Mutex()
    private var fadeJob: Job? = null
    private var progressJob: Job? = null
    @Volatile private var currentVolume: Float = 1.0f
    @Volatile private var isUserPlayIntent: Boolean = false

    var onTrackChangedListener: ((MediaItem) -> Unit)? = null

    init {
        _playerState.update {
            it.copy(
                isKeepScreenAwake = preferencesManager.keepScreenAwake
            )
        }
    }

    fun setPlaylist(tracks: List<MediaItem>, initialTrack: MediaItem? = null) {
        playlist = tracks
        val chosenTrack = if (initialTrack != null) {
            currentIndex = playlist.indexOfFirst { it.id == initialTrack.id }
            initialTrack
        } else if (playlist.isNotEmpty()) {
            if (currentIndex !in playlist.indices) {
                currentIndex = 0
            }
            playlist.getOrNull(currentIndex) ?: playlist.first()
        } else {
            currentIndex = -1
            null
        }

        if (chosenTrack != null && _playerState.value.currentTrack == null) {
            _playerState.update {
                it.copy(
                    currentTrack = chosenTrack,
                    durationMs = chosenTrack.durationMs,
                    currentPositionMs = 0L
                )
            }
        }
    }

    /**
     * Universal track switch:
     * Prepares new track, and plays smoothly.
     */
    fun playTrack(track: MediaItem, updatePlaylistContext: List<MediaItem>? = null) {
        if (updatePlaylistContext != null) {
            playlist = updatePlaylistContext
        }
        val targetIndex = playlist.indexOfFirst { it.id == track.id }
        if (targetIndex != -1) {
            currentIndex = targetIndex
        }

        isUserPlayIntent = true

        // Immediately update player state with track info and playing state for instantaneous UI response
        _playerState.update {
            it.copy(
                currentTrack = track,
                durationMs = track.durationMs,
                currentPositionMs = 0L,
                isPlaying = true,
                isBuffering = true,
                isFadingOut = false
            )
        }

        scope.launch {
            stateMutex.withLock {
                fadeJob?.cancel()
                fadeJob = null

                // Perform quick fade out of existing audio if playing
                val isCurrentlyPlaying = try { mediaPlayer?.isPlaying == true } catch (e: Exception) { false }
                if (isCurrentlyPlaying && preferencesManager.fadeEffectsEnabled) {
                    performFadeImmediate(from = currentVolume, to = 0f, durationMs = 100)
                }

                prepareAndStart(track)
            }
        }
    }

    private suspend fun prepareAndStart(track: MediaItem) {
        withContext(Dispatchers.Main) {
            try {
                fadeJob?.cancel()
                fadeJob = null
                progressJob?.cancel()
                releaseEffects()

                try {
                    mediaPlayer?.release()
                } catch (e: Exception) {
                    Log.w(TAG, "Error releasing old mediaPlayer", e)
                }
                mediaPlayer = null

                requestAudioFocus()

                val player = MediaPlayer().apply {
                    setAudioAttributes(
                        AudioAttributes.Builder()
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .build()
                    )

                    // Safe data source loading via FileDescriptor when applicable
                    setPlayerDataSource(this, track)

                    setOnPreparedListener { mp ->
                        val duration = mp.duration.toLong().coerceAtLeast(track.durationMs)
                        _playerState.update {
                            it.copy(
                                currentTrack = track,
                                durationMs = duration,
                                isPlaying = isUserPlayIntent,
                                isBuffering = false
                            )
                        }

                        setupAudioEffects(mp.audioSessionId)

                        // If user paused while preparing, don't start audio
                        if (!isUserPlayIntent) {
                            return@setOnPreparedListener
                        }

                        // Start playback with universal fade if configured
                        try {
                            val fadeDuration = preferencesManager.fadeDurationMs
                            if (preferencesManager.fadeEffectsEnabled && fadeDuration > 100) {
                                setPlayerVolume(0f)
                                mp.start()
                                startFade(from = 0f, to = 1f, durationMs = fadeDuration.coerceIn(150, 400)) {
                                    _playerState.update { it.copy(isFadingIn = false) }
                                }
                            } else {
                                setPlayerVolume(1f)
                                mp.start()
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Error starting MediaPlayer onPrepared", e)
                        }

                        startProgressTracker()
                        onTrackChangedListener?.invoke(track)
                    }

                    setOnCompletionListener {
                        handleTrackCompletion()
                    }

                    setOnErrorListener { mp, what, extra ->
                        Log.e(TAG, "MediaPlayer error: what=$what, extra=$extra")
                        try {
                            mp.reset()
                            mp.release()
                        } catch (e: Exception) {
                            // ignore
                        }
                        if (mediaPlayer == mp) {
                            mediaPlayer = null
                        }
                        _playerState.update {
                            it.copy(
                                isPlaying = false,
                                isBuffering = false,
                                isFadingIn = false,
                                isFadingOut = false
                            )
                        }

                        // Auto-skip to next track if available so playback never dead-ends
                        scope.launch {
                            delay(300)
                            if (playlist.size > 1 && isUserPlayIntent) {
                                skipNext()
                            }
                        }
                        true
                    }

                    prepareAsync()
                }

                _playerState.update {
                    it.copy(
                        currentTrack = track,
                        isBuffering = true
                    )
                }
                mediaPlayer = player

            } catch (e: Exception) {
                Log.e(TAG, "Error in prepareAndStart: ${track.title}", e)
                _playerState.update { it.copy(isPlaying = false, isBuffering = false) }
            }
        }
    }

    /**
     * Sets the media source robustly via FileDescriptor if available,
     * which avoids Android media extractor file extension and permission issues.
     */
    private fun setPlayerDataSource(player: MediaPlayer, track: MediaItem) {
        if (track.uri.scheme == "file") {
            val file = File(track.uri.path ?: "")
            if (file.exists() && file.canRead()) {
                FileInputStream(file).use { fis ->
                    player.setDataSource(fis.fd)
                }
                return
            }
        }

        try {
            context.contentResolver.openFileDescriptor(track.uri, "r")?.use { pfd ->
                player.setDataSource(pfd.fileDescriptor)
                return
            }
        } catch (e: Exception) {
            Log.w(TAG, "openFileDescriptor failed for ${track.uri}, falling back to context URI", e)
        }

        player.setDataSource(context, track.uri)
    }

    /**
     * Resumes playback with instant UI response and smooth, synchronized fade-in.
     * Recovers automatically if the player was paused, released, or at end-of-track.
     */
    fun resume() {
        isUserPlayIntent = true
        // 1. Instantly update UI state so button and badges reflect PLAY with zero lag
        _playerState.update { it.copy(isPlaying = true, isFadingOut = false) }

        scope.launch {
            stateMutex.withLock {
                fadeJob?.cancel()
                fadeJob = null

                val track = _playerState.value.currentTrack ?: playlist.getOrNull(currentIndex)
                if (track == null) {
                    _playerState.update { it.copy(isPlaying = false) }
                    return@withLock
                }

                val player = mediaPlayer
                var needRecreate = player == null

                if (player != null) {
                    try {
                        val pos = player.currentPosition
                        val dur = player.duration
                        // If track was at completion (end of file), rewind to start
                        if (pos < 0 || (dur > 0 && pos >= dur - 200)) {
                            player.seekTo(0)
                        }
                    } catch (e: Exception) {
                        needRecreate = true
                    }
                }

                if (needRecreate) {
                    prepareAndStart(track)
                    return@withLock
                }

                requestAudioFocus()
                try {
                    player!!.start()
                    startProgressTracker()

                    val fadeDuration = preferencesManager.fadeDurationMs
                    if (preferencesManager.fadeEffectsEnabled && fadeDuration > 100) {
                        _playerState.update { it.copy(isFadingIn = true) }
                        startFade(from = currentVolume.coerceAtMost(0.2f), to = 1f, durationMs = fadeDuration.coerceIn(150, 400)) {
                            _playerState.update { it.copy(isFadingIn = false) }
                        }
                    } else {
                        setPlayerVolume(1f)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error resuming player, re-preparing track", e)
                    prepareAndStart(track)
                }
            }
        }
    }

    /**
     * Pauses playback with instant UI response.
     * Synchronizes state safely without race conditions or playback freezing.
     */
    fun pause() {
        isUserPlayIntent = false
        // 1. Instantly update UI state so button and badges reflect PAUSE with zero lag
        _playerState.update { it.copy(isPlaying = false, isFadingIn = false) }

        scope.launch {
            stateMutex.withLock {
                fadeJob?.cancel()
                fadeJob = null

                val player = mediaPlayer ?: return@withLock

                val fadeDuration = preferencesManager.fadeDurationMs
                if (preferencesManager.fadeEffectsEnabled && fadeDuration > 100) {
                    _playerState.update { it.copy(isFadingOut = true) }
                    startFade(from = currentVolume, to = 0f, durationMs = fadeDuration.coerceIn(100, 250)) {
                        try {
                            if (!isUserPlayIntent) {
                                player.pause()
                            }
                        } catch (e: Exception) {
                            Log.w(TAG, "Error pausing player after fade", e)
                        }
                        _playerState.update { it.copy(isFadingOut = false) }
                    }
                } else {
                    try {
                        player.pause()
                    } catch (e: Exception) {
                        Log.w(TAG, "Error pausing player", e)
                    }
                    setPlayerVolume(0f)
                }
            }
        }
    }

    /**
     * Master Toggle Play/Pause:
     * Instant, deterministic, and always synchronizes UI and audio state.
     */
    fun togglePlayPause() {
        if (_playerState.value.isPlaying) {
            pause()
        } else {
            resume()
        }
    }

    /**
     * Dedicated Skip Forward (+X seconds).
     */
    fun skipForward(seconds: Int) {
        val player = mediaPlayer ?: return
        try {
            val current = player.currentPosition.toLong()
            val duration = _playerState.value.durationMs
            val target = (current + seconds * 1000L).coerceIn(0L, duration)
            seekTo(target)
        } catch (e: Exception) {
            Log.e(TAG, "Error skipping forward", e)
        }
    }

    /**
     * Dedicated Skip Backward (-X seconds).
     */
    fun skipBackward(seconds: Int) {
        val player = mediaPlayer ?: return
        try {
            val current = player.currentPosition.toLong()
            val target = (current - seconds * 1000L).coerceAtLeast(0L)
            seekTo(target)
        } catch (e: Exception) {
            Log.e(TAG, "Error skipping backward", e)
        }
    }

    /**
     * Universal Next Track with smooth transition.
     */
    fun skipNext() {
        if (playlist.isEmpty()) {
            val fallback = _playerState.value.currentTrack
            if (fallback != null) {
                playTrack(fallback)
            }
            return
        }

        // Align current index with player state if needed
        val curTrack = _playerState.value.currentTrack
        if (curTrack != null) {
            val foundIdx = playlist.indexOfFirst { it.id == curTrack.id }
            if (foundIdx != -1) {
                currentIndex = foundIdx
            }
        }

        val nextTrack = if (_playerState.value.isShuffleEnabled && playlist.size > 1) {
            val otherIndices = playlist.indices.filter { it != currentIndex }
            val randomIdx = otherIndices.random()
            currentIndex = randomIdx
            playlist[randomIdx]
        } else {
            val nextIdx = (currentIndex + 1) % playlist.size
            currentIndex = nextIdx
            playlist[nextIdx]
        }

        playTrack(nextTrack)
    }

    /**
     * Universal Previous Track with smooth transition.
     */
    fun skipPrevious() {
        val player = mediaPlayer
        val pos = try { player?.currentPosition ?: 0 } catch (e: Exception) { 0 }
        if (pos > 3000) {
            seekTo(0L)
            return
        }

        if (playlist.isEmpty()) {
            val fallback = _playerState.value.currentTrack
            if (fallback != null) {
                playTrack(fallback)
            }
            return
        }

        val curTrack = _playerState.value.currentTrack
        if (curTrack != null) {
            val foundIdx = playlist.indexOfFirst { it.id == curTrack.id }
            if (foundIdx != -1) {
                currentIndex = foundIdx
            }
        }

        val prevIdx = if (currentIndex - 1 < 0) playlist.size - 1 else currentIndex - 1
        currentIndex = prevIdx
        playTrack(playlist[prevIdx])
    }

    fun seekTo(positionMs: Long) {
        val player = mediaPlayer ?: return
        try {
            val bounded = positionMs.coerceIn(0L, _playerState.value.durationMs)
            player.seekTo(bounded.toInt())
            _playerState.update { it.copy(currentPositionMs = bounded) }
        } catch (e: Exception) {
            Log.e(TAG, "Error seeking player", e)
        }
    }

    fun toggleShuffle() {
        _playerState.update { it.copy(isShuffleEnabled = !it.isShuffleEnabled) }
    }

    fun cycleRepeatMode(): RepeatMode {
        val newMode = when (_playerState.value.repeatMode) {
            RepeatMode.OFF -> RepeatMode.ALL
            RepeatMode.ALL -> RepeatMode.ONE
            RepeatMode.ONE -> RepeatMode.OFF
        }
        _playerState.update { it.copy(repeatMode = newMode) }
        return newMode
    }

    fun toggleKeepScreenAwake(): Boolean {
        val newState = !_playerState.value.isKeepScreenAwake
        preferencesManager.keepScreenAwake = newState
        _playerState.update { it.copy(isKeepScreenAwake = newState) }
        return newState
    }

    /**
     * Smoothly fades out audio and stops playback (used by Sleep Timer).
     */
    fun smoothFadeAndStop(onStopped: (() -> Unit)? = null) {
        isUserPlayIntent = false
        _playerState.update { it.copy(isPlaying = false) }
        scope.launch {
            stateMutex.withLock {
                fadeJob?.cancel()
                fadeJob = null
                val player = mediaPlayer ?: return@withLock
                val fadeDuration = preferencesManager.fadeDurationMs.coerceIn(400, 1000)
                _playerState.update { it.copy(isFadingOut = true) }
                performFadeImmediate(from = currentVolume, to = 0f, durationMs = fadeDuration)
                try {
                    player.pause()
                } catch (e: Exception) {
                    Log.w(TAG, "Error stopping player on sleep timer", e)
                }
                _playerState.update { it.copy(isFadingOut = false) }
                onStopped?.invoke()
            }
        }
    }

    private fun startFade(from: Float, to: Float, durationMs: Int, onComplete: (() -> Unit)? = null) {
        fadeJob?.cancel()
        fadeJob = scope.launch {
            val steps = (durationMs / 25).coerceIn(4, 20)
            val stepInterval = (durationMs / steps).toLong().coerceAtLeast(10L)

            for (i in 0..steps) {
                if (!isActive) break
                val fraction = i.toFloat() / steps.toFloat()
                val vol = (from + (to - from) * fraction).coerceIn(0f, 1f)
                setPlayerVolume(vol)
                delay(stepInterval)
            }
            if (isActive) {
                setPlayerVolume(to)
                onComplete?.invoke()
            }
        }
    }

    private suspend fun performFadeImmediate(from: Float, to: Float, durationMs: Int) {
        val steps = (durationMs / 25).coerceIn(4, 20)
        val stepInterval = (durationMs / steps).toLong().coerceAtLeast(10L)

        for (i in 0..steps) {
            val fraction = i.toFloat() / steps.toFloat()
            val vol = (from + (to - from) * fraction).coerceIn(0f, 1f)
            setPlayerVolume(vol)
            delay(stepInterval)
        }
        setPlayerVolume(to)
    }

    private fun setPlayerVolume(volume: Float) {
        currentVolume = volume
        try {
            mediaPlayer?.setVolume(volume, volume)
        } catch (e: Exception) {
            // Ignore if player is released
        }
    }

    private fun handleTrackCompletion() {
        when (_playerState.value.repeatMode) {
            RepeatMode.ONE -> {
                seekTo(0L)
                resume()
            }
            RepeatMode.ALL -> {
                skipNext()
            }
            RepeatMode.OFF -> {
                if (playlist.isNotEmpty() && currentIndex < playlist.size - 1) {
                    skipNext()
                } else {
                    try {
                        mediaPlayer?.seekTo(0)
                    } catch (e: Exception) {
                        // ignore
                    }
                    _playerState.update { it.copy(isPlaying = false, currentPositionMs = 0L) }
                }
            }
        }
    }

    private fun startProgressTracker() {
        progressJob?.cancel()
        progressJob = scope.launch {
            while (isActive) {
                val player = mediaPlayer
                val isPlaying = try { player?.isPlaying == true } catch (e: Exception) { false }
                if (player != null && isPlaying) {
                    try {
                        val pos = player.currentPosition.toLong()
                        val dur = player.duration.toLong().coerceAtLeast(_playerState.value.durationMs)
                        _playerState.update {
                            it.copy(
                                currentPositionMs = pos,
                                durationMs = dur
                            )
                        }
                    } catch (e: Exception) {
                        // ignore race condition on track switch
                    }
                }
                delay(250)
            }
        }
    }

    private fun setupAudioEffects(sessionId: Int) {
        try {
            equalizer = Equalizer(0, sessionId).apply {
                enabled = true
                val presetIndex = preferencesManager.equalizerPreset
                if (presetIndex in 0 until numberOfPresets) {
                    usePreset(presetIndex.toShort())
                }
            }

            bassBoost = BassBoost(0, sessionId).apply {
                enabled = true
                if (strengthSupported) {
                    setStrength(preferencesManager.bassBoostLevel.toShort())
                }
            }

            virtualizer = Virtualizer(0, sessionId).apply {
                enabled = true
                if (strengthSupported) {
                    setStrength(preferencesManager.virtualizerLevel.toShort())
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Audio effects not supported on this device/session", e)
        }
    }

    fun applyEqualizerPreset(presetIndex: Short) {
        preferencesManager.equalizerPreset = presetIndex.toInt()
        try {
            equalizer?.usePreset(presetIndex)
        } catch (e: Exception) {
            Log.w(TAG, "Cannot apply preset", e)
        }
    }

    fun applyBassBoost(strength: Short) {
        preferencesManager.bassBoostLevel = strength.toInt()
        try {
            bassBoost?.setStrength(strength)
        } catch (e: Exception) {
            Log.w(TAG, "Cannot apply bass boost", e)
        }
    }

    fun applyVirtualizer(strength: Short) {
        preferencesManager.virtualizerLevel = strength.toInt()
        try {
            virtualizer?.setStrength(strength)
        } catch (e: Exception) {
            Log.w(TAG, "Cannot apply virtualizer", e)
        }
    }

    private fun releaseEffects() {
        try {
            equalizer?.release()
            bassBoost?.release()
            virtualizer?.release()
        } catch (e: Exception) {
            Log.w(TAG, "Error releasing audio effects", e)
        } finally {
            equalizer = null
            bassBoost = null
            virtualizer = null
        }
    }

    private fun requestAudioFocus() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val request = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                .setOnAudioFocusChangeListener { focusChange ->
                    when (focusChange) {
                        AudioManager.AUDIOFOCUS_LOSS -> pause()
                        AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> pause()
                        AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                            setPlayerVolume(0.2f)
                        }
                        AudioManager.AUDIOFOCUS_GAIN -> {
                            setPlayerVolume(1.0f)
                        }
                    }
                }
                .build()
            audioFocusRequest = request
            audioManager.requestAudioFocus(request)
        } else {
            @Suppress("DEPRECATION")
            audioManager.requestAudioFocus(
                { focusChange ->
                    if (focusChange == AudioManager.AUDIOFOCUS_LOSS) pause()
                },
                AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN
            )
        }
    }

    fun release() {
        isUserPlayIntent = false
        fadeJob?.cancel()
        progressJob?.cancel()
        releaseEffects()
        try {
            mediaPlayer?.release()
        } catch (e: Exception) {
            // ignore
        }
        mediaPlayer = null
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && audioFocusRequest != null) {
            audioManager.abandonAudioFocusRequest(audioFocusRequest!!)
        }
    }
}
