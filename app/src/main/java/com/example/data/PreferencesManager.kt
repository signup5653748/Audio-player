package com.example.data

import android.content.Context
import android.content.SharedPreferences

/**
 * Manages user preferences:
 * - Folder blacklist / whitelist
 * - Short audio filter threshold
 * - Universal customizable fade duration (in seconds)
 * - Skip interval duration (in seconds, default 10s)
 * - Visual effects master toggle and active effect selection (20 styles)
 * - Keep Screen Awake (WakeLock) setting
 * - Equalizer presets and levels
 */
class PreferencesManager(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("smart_music_player_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_EXCLUDED_FOLDERS = "excluded_folders"
        private const val KEY_MIN_AUDIO_DURATION = "min_audio_duration_seconds"
        private const val KEY_KEEP_SCREEN_AWAKE = "keep_screen_awake"
        private const val KEY_FADE_EFFECTS_ENABLED = "fade_effects_enabled"
        private const val KEY_FADE_DURATION_SECONDS = "fade_duration_seconds"
        private const val KEY_SKIP_DURATION_SECONDS = "skip_duration_seconds"
        private const val KEY_VISUAL_EFFECTS_ENABLED = "visual_effects_enabled"
        private const val KEY_SELECTED_VISUAL_EFFECT_ID = "selected_visual_effect_id"
        private const val KEY_EQ_PRESET = "eq_preset"
        private const val KEY_BASS_BOOST = "bass_boost"
        private const val KEY_VIRTUALIZER = "virtualizer"

        const val DEFAULT_MIN_AUDIO_DURATION = 30 // Default 30s to hide WhatsApp/ringtones
        const val DEFAULT_FADE_DURATION_SECONDS = 0.8f // Default 0.8s universal smooth fade
        const val DEFAULT_SKIP_DURATION_SECONDS = 10 // Default 10s skip interval
        const val DEFAULT_VISUAL_EFFECT_ID = 0 // Crimson Neon Pulse
    }

    var excludedFolders: Set<String>
        get() = prefs.getStringSet(KEY_EXCLUDED_FOLDERS, emptySet()) ?: emptySet()
        set(value) = prefs.edit().putStringSet(KEY_EXCLUDED_FOLDERS, value).apply()

    fun isFolderExcluded(folderPath: String): Boolean {
        return excludedFolders.contains(folderPath)
    }

    fun setFolderExcluded(folderPath: String, excluded: Boolean) {
        val current = excludedFolders.toMutableSet()
        if (excluded) {
            current.add(folderPath)
        } else {
            current.remove(folderPath)
        }
        excludedFolders = current
    }

    var minAudioDurationSeconds: Int
        get() = prefs.getInt(KEY_MIN_AUDIO_DURATION, DEFAULT_MIN_AUDIO_DURATION)
        set(value) = prefs.edit().putInt(KEY_MIN_AUDIO_DURATION, value).apply()

    var keepScreenAwake: Boolean
        get() = prefs.getBoolean(KEY_KEEP_SCREEN_AWAKE, false)
        set(value) = prefs.edit().putBoolean(KEY_KEEP_SCREEN_AWAKE, value).apply()

    var fadeEffectsEnabled: Boolean
        get() = prefs.getBoolean(KEY_FADE_EFFECTS_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_FADE_EFFECTS_ENABLED, value).apply()

    var fadeDurationSeconds: Float
        get() = prefs.getFloat(KEY_FADE_DURATION_SECONDS, DEFAULT_FADE_DURATION_SECONDS)
        set(value) = prefs.edit().putFloat(KEY_FADE_DURATION_SECONDS, value.coerceIn(0.2f, 3.5f)).apply()

    val fadeDurationMs: Int
        get() = (fadeDurationSeconds * 1000f).toInt()

    var skipDurationSeconds: Int
        get() = prefs.getInt(KEY_SKIP_DURATION_SECONDS, DEFAULT_SKIP_DURATION_SECONDS)
        set(value) = prefs.edit().putInt(KEY_SKIP_DURATION_SECONDS, value.coerceIn(3, 60)).apply()

    var visualEffectsEnabled: Boolean
        get() = prefs.getBoolean(KEY_VISUAL_EFFECTS_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_VISUAL_EFFECTS_ENABLED, value).apply()

    var selectedVisualEffectId: Int
        get() = prefs.getInt(KEY_SELECTED_VISUAL_EFFECT_ID, DEFAULT_VISUAL_EFFECT_ID)
        set(value) = prefs.edit().putInt(KEY_SELECTED_VISUAL_EFFECT_ID, value.coerceIn(0, 19)).apply()

    var equalizerPreset: Int
        get() = prefs.getInt(KEY_EQ_PRESET, 0) // 0 = Flat
        set(value) = prefs.edit().putInt(KEY_EQ_PRESET, value).apply()

    var bassBoostLevel: Int
        get() = prefs.getInt(KEY_BASS_BOOST, 0)
        set(value) = prefs.edit().putInt(KEY_BASS_BOOST, value.coerceIn(0, 1000)).apply()

    var virtualizerLevel: Int
        get() = prefs.getInt(KEY_VIRTUALIZER, 0)
        set(value) = prefs.edit().putInt(KEY_VIRTUALIZER, value.coerceIn(0, 1000)).apply()
}
