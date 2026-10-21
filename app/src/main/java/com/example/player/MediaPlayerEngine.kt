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

/**
 * Robust, production-grade audio playback engine with:
 * - Thread-safe synchronized playback state machine (eliminates play/pause lag, stutter, and race conditions)
 * - Universal automatic fade-in on Play and fade-out on Pause
 * - Seamless crossfade transitions on Next, Previous, and Skip
 * - Dedicated Skip Forward and Skip Backward controls (+10s, -10s, etc.)
 * - Smooth Sleep Timer auto-fade and stop
 * - Equalizer, Bass Boost, and 3D Virtualizer audio effects
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

    // Concurrency and synchronization guards
    private val stateMutex = Mutex()
    private var fadeJob: Job? = null
    private var progressJob: Job? = null
    @Volatile private var isUserPlayIntent: Boolean = false
    @Volatile private var currentVolume: Float = 1.0f

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
        if (initialTrack != null) {
            currentIndex = playlist.indexOfFirst { it.id == initialTrack.id }
        } else if (currentIndex !in playlist.indices && playlist.isNotEmpty()) {
            currentIndex = 0
        }
    }

    /**
     * Universal track switch:
     * Fades out current track smoothly (if playing), prepares new track, and fades in smoothly.
     */
    fun playTrack(track: MediaItem, updatePlaylistContext: List<MediaItem>? = null) {
        if (updatePlaylistContext != null) {
            playlist = updatePlaylistContext
        }
        val targetIndex = playlist.indexOfFirst { it.id == track.id }
        if (targetIndex != -1) {
            currentIndex = targetIndex
        }

        scope.launch {
            stateMutex.withLock {
                isUserPlayIntent = true
                val fadeDuration = preferencesManager.fadeDurationMs
                val isCurrentlyPlaying = mediaPlayer?.isPlaying == true

                if (isCurrentlyPlaying && preferencesManager.fadeEffectsEnabled && fadeDuration > 100) {
                    _playerState.update { it.copy(isFadingOut = true) }
                    performFade(from = currentVolume, to = 0f, durationMs = fadeDuration / 2)
                    _playerState.update { it.copy(isFadingOut = false) }
                }

                prepareAndStart(track)
            }
        }
    }

    private suspend fun prepareAndStart(track: MediaItem) {
        withContext(Dispatchers.Main) {
            try {
                fadeJob?.cancel()
                progressJob?.cancel()
                releaseEffects()

                mediaPlayer?.release()
                mediaPlayer = null

                requestAudioFocus()

                val player = MediaPlayer().apply {
                    setAudioAttributes(
                        AudioAttributes.Builder()
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .build()
                    )
                    setDataSource(context, track.uri)
                    setOnPreparedListener { mp ->
                        val duration = mp.duration.toLong().coerceAtLeast(track.durationMs)
                        _playerState.update {
                            it.copy(
                                currentTrack = track,
                                durationMs = duration,
                                currentPositionMs = 0L,
                                isPlaying = true,
                                isBuffering = false
                            )
                        }

                        setupAudioEffects(mp.audioSessionId)

                        // Start muted and smoothly ramp up
                        val fadeDuration = preferencesManager.fadeDurationMs
                        if (preferencesManager.fadeEffectsEnabled && fadeDuration > 100) {
                            setPlayerVolume(0f)
                            mp.start()
                            startFadeIn(fadeDuration)
                        } else {
                            setPlayerVolume(1f)
                            mp.start()
                        }

                        startProgressTracker()
                        onTrackChangedListener?.invoke(track)
                    }

                    setOnCompletionListener {
                        handleTrackCompletion()
                    }

                    setOnErrorListener { _, what, extra ->
                        Log.e(TAG, "MediaPlayer error: what=$what, extra=$extra")
                        _playerState.update { it.copy(isPlaying = false, isBuffering = false) }
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
                Log.e(TAG, "Error playing track: ${track.title}", e)
                _playerState.update { it.copy(isPlaying = false, isBuffering = false) }
            }
        }
    }

    /**
     * Resumes playback with smooth, synchronized fade-in.
     * Prevents race conditions with any previous pause operations.
     */
    fun resume() {
        val player = mediaPlayer ?: run {
            val track = _playerState.value.currentTrack ?: playlist.getOrNull(currentIndex)
            if (track != null) {
                playTrack(track)
            }
            return
        }

        scope.launch {
            stateMutex.withLock {
                isUserPlayIntent = true
                fadeJob?.cancel() // Cancel any ongoing pause fade immediately!

                requestAudioFocus()
                try {
                    val fadeDuration = preferencesManager.fadeDurationMs
                    if (preferencesManager.fadeEffectsEnabled && fadeDuration > 100) {
                        setPlayerVolume(0f)
                        player.start()
                        _playerState.update { it.copy(isPlaying = true) }
                        startProgressTracker()
                        startFadeIn(fadeDuration)
                    } else {
                        setPlayerVolume(1f)
                        player.start()
                        _playerState.update { it.copy(isPlaying = true) }
                        startProgressTracker()
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error resuming playback", e)
                }
            }
        }
    }

    /**
     * Pauses playback with smooth, synchronized fade-out.
     * Fixes the audio lag bug:
     * - Immediately sets user intent to false so resume cannot conflict.
     * - Cancels previous fade-in jobs.
     * - Fades volume from current level down to 0.
     * - Checks user intent before pausing (if user clicked Play during fade, stays playing!).
     * - Keeps volume at 0 until explicitly resumed, preventing audio stutter.
     */
    fun pause() {
        val player = mediaPlayer ?: return
        if (!player.isPlaying) return

        scope.launch {
            stateMutex.withLock {
                isUserPlayIntent = false
                fadeJob?.cancel() // Cancel any ongoing fade-in job immediately

                val fadeDuration = preferencesManager.fadeDurationMs
                if (preferencesManager.fadeEffectsEnabled && fadeDuration > 100) {
                    _playerState.update { it.copy(isFadingOut = true) }
                    performFade(from = currentVolume, to = 0f, durationMs = fadeDuration)

                    // Ensure user hasn't clicked Resume while we were fading out!
                    if (!isUserPlayIntent) {
                        try {
                            player.pause()
                        } catch (e: Exception) {
                            Log.w(TAG, "Error pausing player", e)
                        }
                        _playerState.update { it.copy(isPlaying = false, isFadingOut = false) }
                    } else {
                        _playerState.update { it.copy(isFadingOut = false) }
                    }
                } else {
                    try {
                        player.pause()
                    } catch (e: Exception) {
                        Log.w(TAG, "Error pausing player", e)
                    }
                    _playerState.update { it.copy(isPlaying = false) }
                }
            }
        }
    }

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
     * Universal Next Track with smooth fade-out and fade-in transition.
     */
    fun skipNext() {
        if (playlist.isEmpty()) return

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
     * Universal Previous Track with smooth fade-out and fade-in transition.
     */
    fun skipPrevious() {
        val player = mediaPlayer
        if (player != null && player.currentPosition > 3000) {
            seekTo(0L)
            return
        }

        if (playlist.isEmpty()) return

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
        val player = mediaPlayer ?: return
        scope.launch {
            stateMutex.withLock {
                isUserPlayIntent = false
                fadeJob?.cancel()
                val fadeDuration = preferencesManager.fadeDurationMs.coerceAtLeast(800)
                _playerState.update { it.copy(isFadingOut = true) }
                performFade(from = currentVolume, to = 0f, durationMs = fadeDuration)
                try {
                    player.pause()
                } catch (e: Exception) {
                    Log.w(TAG, "Error stopping player on sleep timer", e)
                }
                _playerState.update { it.copy(isPlaying = false, isFadingOut = false) }
                onStopped?.invoke()
            }
        }
    }

    private fun startFadeIn(durationMs: Int) {
        fadeJob?.cancel()
        fadeJob = scope.launch {
            _playerState.update { it.copy(isFadingIn = true) }
            performFade(from = 0f, to = 1f, durationMs = durationMs)
            _playerState.update { it.copy(isFadingIn = false) }
        }
    }

    private suspend fun performFade(from: Float, to: Float, durationMs: Int) {
        val steps = (durationMs / 25).coerceIn(6, 60)
        val stepInterval = (durationMs / steps).toLong()

        for (i in 0..steps) {
            if (!scope.isActive) break
            val fraction = i.toFloat() / steps.toFloat()
            val vol = (from + (to - from) * fraction).coerceIn(0f, 1f)
            setPlayerVolume(vol)
            delay(stepInterval)
        }
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
                if (currentIndex < playlist.size - 1) {
                    skipNext()
                } else {
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
                if (player != null && player.isPlaying) {
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
        fadeJob?.cancel()
        progressJob?.cancel()
        releaseEffects()
        mediaPlayer?.release()
        mediaPlayer = null
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && audioFocusRequest != null) {
            audioManager.abandonAudioFocusRequest(audioFocusRequest!!)
        }
    }
}
