package com.example.model

import android.net.Uri

/**
 * Represents an audio or video media item.
 */
data class MediaItem(
    val id: Long,
    val title: String,
    val artist: String = "Unknown Artist",
    val album: String = "Unknown Album",
    val durationMs: Long,
    val uri: Uri,
    val mimeType: String,
    val isVideo: Boolean,
    val folderPath: String,
    val folderName: String,
    val sizeBytes: Long = 0L,
    val dateAdded: Long = 0L,
    val isSample: Boolean = false
) {
    val durationFormatted: String
        get() {
            val totalSeconds = (durationMs / 1000).coerceAtLeast(0)
            val minutes = totalSeconds / 60
            val seconds = totalSeconds % 60
            return "%d:%02d".format(minutes, seconds)
        }

    val accessibleDescription: String
        get() {
            val typeStr = if (isVideo) "Video track in audio mode" else "Audio track"
            val totalSeconds = (durationMs / 1000).coerceAtLeast(0)
            val minutes = totalSeconds / 60
            val seconds = totalSeconds % 60
            return "$typeStr: $title by $artist. Duration $minutes minutes and $seconds seconds. Folder $folderName."
        }
}

/**
 * Represents a storage folder containing media items.
 */
data class FolderItem(
    val path: String,
    val name: String,
    val itemCount: Int,
    val audioCount: Int,
    val videoCount: Int,
    val totalDurationMs: Long,
    val isIncluded: Boolean = true
) {
    val durationFormatted: String
        get() {
            val totalMinutes = (totalDurationMs / (1000 * 60)).coerceAtLeast(0)
            return if (totalMinutes < 60) {
                "$totalMinutes mins"
            } else {
                "${totalMinutes / 60}h ${totalMinutes % 60}m"
            }
        }
}
