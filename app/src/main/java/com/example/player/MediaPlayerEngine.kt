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
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream

/**
 * Robust, production-grade audio playback engine with Universal Fade transitions:
 * - Deterministic Main-thread MediaPlayer state transitions
 * - Universal, smooth automatic Fade-in and Fade-out on Play, Pause, Next, Previous, and Skip
 * - Reusable AudioFocus listener preventing focus-loss self-pause loops
 * - Dynamic customizable fade duration configurable via settings
 * - Equalizer, Bass Boost, Virtualizer, and Sleep Timer integration
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
    private var hasAudioFocus = false

    // Audio effects
    private var equalizer: Equalizer? = null
    private var bassBoost: BassBoost? = null
    private var virtualizer: Virtualizer? = null

    private val _playerState = MutableStateFlow(PlayerState())
    val playerState: StateFlow<PlayerState> = _playerState.asStateFlow()

    private var playlist: List<MediaItem> = emptyList()
    private var currentIndex: Int = -1

    @Volatile
    private var isUserPlayIntent: Boolean = false
    private var progressJob: Job? = null
    private var fadeJob: Job? = null

    var onTrackChangedListener: ((MediaItem) -> Unit)? = null

    // Single persistent audio focus change listener to prevent focus-theft self-pausing
    private val audioFocusChangeListener = AudioManager.OnAudioFocusChangeListener { focusChange ->
        when (focusChange) {
            AudioManager.AUDIOFOCUS_LOSS -> {
                hasAudioFocus = false
                pause()
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                pause()
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                setVolume(0.2f)
            }
            AudioManager.AUDIOFOCUS_GAIN -> {
                setVolume(1.0f)
            }
        }
    }

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
     * Prepares and starts playback of the selected track with smooth universal fade-in.
     * When switching between tracks (Next/Prev), performs a rapid fade-out on the existing player.
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
        fadeJob?.cancel()
        fadeJob = null

        _playerState.update {
            it.copy(
                currentTrack = track,
                durationMs = track.durationMs,
                currentPositionMs = 0L,
                isPlaying = true,
                isBuffering = true,
                isFadingIn = false,
                isFadingOut = false
            )
        }

        scope.launch(Dispatchers.Main) {
            val existingPlayer = mediaPlayer
            val isCurrentlyPlaying = try { existingPlayer?.isPlaying == true } catch (e: Exception) { false }
            val fadeEnabled = preferencesManager.fadeEffectsEnabled

            // Smoothly dip out the current track before switching if already playing
            if (isCurrentlyPlaying && existingPlayer != null && fadeEnabled) {
                val fadeOutDuration = 100
                val steps = 4
                val stepDelay = (fadeOutDuration / steps).toLong()
                for (i in steps downTo 0) {
                    val vol = i.toFloat() / steps.toFloat()
                    try { existingPlayer.setVolume(vol, vol) } catch (e: Exception) {}
                    delay(stepDelay)
                }
            }

            prepareAndStart(track)
        }
    }

    private fun prepareAndStart(track: MediaItem) {
        try {
            stopProgressTracker()
            releaseEffects()

            try {
                mediaPlayer?.reset()
                mediaPlayer?.release()
            } catch (e: Exception) {
                Log.w(TAG, "Error resetting previous MediaPlayer", e)
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

                setPlayerDataSource(this, track)

                setOnPreparedListener { mp ->
                    try {
                        val duration = mp.duration.toLong().let { if (it > 0) it else track.durationMs }
                        _playerState.update {
                            it.copy(
                                currentTrack = track,
                                durationMs = duration,
                                isPlaying = isUserPlayIntent,
                                isBuffering = false
                            )
                        }

                        setupAudioEffects(mp.audioSessionId)

                        if (!isUserPlayIntent) {
                            return@setOnPreparedListener
                        }

                        val fadeEnabled = preferencesManager.fadeEffectsEnabled
                        val fadeDuration = (preferencesManager.fadeDurationSeconds * 1000).toInt().coerceIn(150, 1000)

                        if (fadeEnabled) {
                            setVolume(0.0f)
                            mp.start()
                            startProgressTracker()
                            startFadeIn(fadeDuration)
                        } else {
                            setVolume(1.0f)
                            mp.start()
                            startProgressTracker()
                        }
                        onTrackChangedListener?.invoke(track)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error in onPrepared callback", e)
                    }
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
                    true
                }

                prepareAsync()
            }

            mediaPlayer = player
            _playerState.update {
                it.copy(
                    currentTrack = track,
                    isBuffering = true
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error setting up MediaPlayer: ${track.title}", e)
            _playerState.update { it.copy(isPlaying = false, isBuffering = false) }
        }
    }

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
     * Resumes playback with smooth universal fade-in.
     */
    fun resume() {
        isUserPlayIntent = true
        fadeJob?.cancel()
        fadeJob = null
        _playerState.update { it.copy(isPlaying = true, isFadingOut = false) }

        scope.launch(Dispatchers.Main) {
            val track = _playerState.value.currentTrack ?: playlist.getOrNull(currentIndex)
            if (track == null) {
                _playerState.update { it.copy(isPlaying = false) }
                return@launch
            }

            val player = mediaPlayer
            if (player == null) {
                prepareAndStart(track)
                return@launch
            }

            try {
                // If track completed or is at end, seek back to beginning
                val currentPos = player.currentPosition
                val duration = player.duration
                if (duration > 0 && currentPos >= duration - 300) {
                    player.seekTo(0)
                }

                requestAudioFocus()
                val fadeEnabled = preferencesManager.fadeEffectsEnabled
                val fadeDuration = (preferencesManager.fadeDurationSeconds * 1000).toInt().coerceIn(150, 1000)

                if (fadeEnabled) {
                    setVolume(0.0f)
                    player.start()
                    _playerState.update { it.copy(isPlaying = true, isBuffering = false) }
                    startProgressTracker()
                    startFadeIn(fadeDuration)
                } else {
                    setVolume(1.0f)
                    player.start()
                    _playerState.update { it.copy(isPlaying = true, isBuffering = false) }
                    startProgressTracker()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error resuming player, re-preparing track", e)
                prepareAndStart(track)
            }
        }
    }

    /**
     * Pauses playback with smooth universal fade-out.
     */
    fun pause() {
        isUserPlayIntent = false
        fadeJob?.cancel()
        fadeJob = null

        val player = mediaPlayer
        val isCurrentlyPlaying = try { player?.isPlaying == true } catch (e: Exception) { false }
        val fadeEnabled = preferencesManager.fadeEffectsEnabled
        val fadeDuration = (preferencesManager.fadeDurationSeconds * 1000).toInt().coerceIn(100, 400)

        if (fadeEnabled && isCurrentlyPlaying && player != null) {
            startFadeOut(fadeDuration) {
                try {
                    if (!isUserPlayIntent) {
                        player.pause()
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Error pausing MediaPlayer after fade", e)
                }
                _playerState.update { it.copy(isPlaying = false, isFadingOut = false) }
                stopProgressTracker()
            }
        } else {
            _playerState.update { it.copy(isPlaying = false, isFadingIn = false, isFadingOut = false) }
            try {
                if (isCurrentlyPlaying) {
                    player?.pause()
                }
            } catch (e: Exception) {
                Log.w(TAG, "Error pausing MediaPlayer", e)
            }
            stopProgressTracker()
        }
    }

    /**
     * Master Toggle Play/Pause:
     * Instant, deterministic, and always synchronizes UI and audio state.
     */
    fun togglePlayPause() {
        val isCurrentlyPlaying = try { mediaPlayer?.isPlaying == true } catch (e: Exception) { false }
        if (isCurrentlyPlaying || _playerState.value.isPlaying) {
            pause()
        } else {
            resume()
        }
    }

    /**
     * Dedicated Skip Forward (+X seconds) with micro-fade to eliminate pop/click.
     */
    fun skipForward(seconds: Int) {
        val player = mediaPlayer ?: return
        try {
            val current = player.currentPosition.toLong()
            val duration = _playerState.value.durationMs
            val target = (current + seconds * 1000L).coerceIn(0L, duration)
            if (preferencesManager.fadeEffectsEnabled && player.isPlaying) {
                performSkipWithMicroFade(player, target)
            } else {
                seekTo(target)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error skipping forward", e)
        }
    }

    /**
     * Dedicated Skip Backward (-X seconds) with micro-fade to eliminate pop/click.
     */
    fun skipBackward(seconds: Int) {
        val player = mediaPlayer ?: return
        try {
            val current = player.currentPosition.toLong()
            val target = (current - seconds * 1000L).coerceAtLeast(0L)
            if (preferencesManager.fadeEffectsEnabled && player.isPlaying) {
                performSkipWithMicroFade(player, target)
            } else {
                seekTo(target)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error skipping backward", e)
        }
    }

    private fun performSkipWithMicroFade(player: MediaPlayer, targetPositionMs: Long) {
        scope.launch(Dispatchers.Main) {
            try { player.setVolume(0.3f, 0.3f) } catch (e: Exception) {}
            delay(35)
            player.seekTo(targetPositionMs.toInt())
            _playerState.update { it.copy(currentPositionMs = targetPositionMs) }
            delay(25)
            val restoreDuration = (preferencesManager.fadeDurationSeconds * 300).toInt().coerceIn(100, 300)
            startFadeIn(restoreDuration)
        }
    }

    /**
     * Universal Next track transition with fade.
     */
    fun skipNext() {
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
     * Universal Previous track transition with fade.
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
     * Stops playback cleanly with fade-out (used by Sleep Timer).
     */
    fun smoothFadeAndStop(onStopped: (() -> Unit)? = null) {
        isUserPlayIntent = false
        fadeJob?.cancel()
        fadeJob = null

        val player = mediaPlayer
        val isCurrentlyPlaying = try { player?.isPlaying == true } catch (e: Exception) { false }
        val fadeDuration = (preferencesManager.fadeDurationSeconds * 1000).toInt().coerceIn(600, 1500)

        if (isCurrentlyPlaying && player != null) {
            startFadeOut(fadeDuration) {
                try {
                    player.pause()
                } catch (e: Exception) {
                    Log.w(TAG, "Error pausing on sleep timer", e)
                }
                _playerState.update { it.copy(isPlaying = false, isFadingOut = false) }
                stopProgressTracker()
                onStopped?.invoke()
            }
        } else {
            pause()
            onStopped?.invoke()
        }
    }

    private fun startFadeIn(durationMs: Int, onComplete: (() -> Unit)? = null) {
        fadeJob?.cancel()
        _playerState.update { it.copy(isFadingIn = true, isFadingOut = false) }
        fadeJob = scope.launch(Dispatchers.Main) {
            val steps = (durationMs / 25).coerceIn(4, 25)
            val stepInterval = (durationMs / steps).toLong().coerceAtLeast(10L)
            for (i in 0..steps) {
                if (!isActive) break
                val vol = (i.toFloat() / steps.toFloat()).coerceIn(0f, 1f)
                setVolume(vol)
                delay(stepInterval)
            }
            if (isActive) {
                setVolume(1.0f)
                _playerState.update { it.copy(isFadingIn = false) }
                onComplete?.invoke()
            }
        }
    }

    private fun startFadeOut(durationMs: Int, onComplete: (() -> Unit)? = null) {
        fadeJob?.cancel()
        _playerState.update { it.copy(isFadingOut = true, isFadingIn = false) }
        fadeJob = scope.launch(Dispatchers.Main) {
            val steps = (durationMs / 25).coerceIn(4, 25)
            val stepInterval = (durationMs / steps).toLong().coerceAtLeast(10L)
            for (i in steps downTo 0) {
                if (!isActive) break
                val vol = (i.toFloat() / steps.toFloat()).coerceIn(0f, 1f)
                setVolume(vol)
                delay(stepInterval)
            }
            if (isActive) {
                setVolume(0.0f)
                _playerState.update { it.copy(isFadingOut = false) }
                onComplete?.invoke()
            }
        }
    }

    private fun setVolume(volume: Float) {
        try {
            mediaPlayer?.setVolume(volume, volume)
        } catch (e: Exception) {
            // ignore
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
                    stopProgressTracker()
                }
            }
        }
    }

    private fun startProgressTracker() {
        stopProgressTracker()
        progressJob = scope.launch(Dispatchers.Main) {
            while (isActive) {
                val player = mediaPlayer
                val isPlaying = try { player?.isPlaying == true } catch (e: Exception) { false }
                if (player != null && isPlaying) {
                    try {
                        val pos = player.currentPosition.toLong()
                        val dur = player.duration.toLong().let { if (it > 0) it else _playerState.value.durationMs }
                        _playerState.update {
                            it.copy(
                                currentPositionMs = pos,
                                durationMs = dur,
                                isPlaying = true
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

    private fun stopProgressTracker() {
        progressJob?.cancel()
        progressJob = null
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
        } catch (e: Throwable) {
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

    private fun requestAudioFocus(): Boolean {
        if (hasAudioFocus) return true
        val result = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (audioFocusRequest == null) {
                audioFocusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .build()
                    )
                    .setOnAudioFocusChangeListener(audioFocusChangeListener)
                    .build()
            }
            audioManager.requestAudioFocus(audioFocusRequest!!)
        } else {
            @Suppress("DEPRECATION")
            audioManager.requestAudioFocus(
                audioFocusChangeListener,
                AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN
            )
        }
        hasAudioFocus = (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED)
        return hasAudioFocus
    }

    private fun abandonAudioFocus() {
        if (!hasAudioFocus) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            audioFocusRequest?.let { audioManager.abandonAudioFocusRequest(it) }
        } else {
            @Suppress("DEPRECATION")
            audioManager.abandonAudioFocus(audioFocusChangeListener)
        }
        hasAudioFocus = false
    }

    fun release() {
        isUserPlayIntent = false
        fadeJob?.cancel()
        fadeJob = null
        stopProgressTracker()
        releaseEffects()
        try {
            mediaPlayer?.reset()
            mediaPlayer?.release()
        } catch (e: Exception) {
            // ignore
        }
        mediaPlayer = null
        abandonAudioFocus()
    }
}
