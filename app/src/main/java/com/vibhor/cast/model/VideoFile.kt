package com.vibhor.cast.model

import android.net.Uri

data class VideoFile(
    val id: Long,
    val name: String,
    val path: String,
    val uri: Uri,
    val size: Long,
    val duration: Long,
    val mimeType: String,
    val dateModified: Long,
    val width: Int = 0,
    val height: Int = 0
) {
    val formattedSize: String get() {
        val kb = size / 1024.0
        val mb = kb / 1024.0
        val gb = mb / 1024.0
        return when {
            gb >= 1.0 -> String.format("%.1f GB", gb)
            mb >= 1.0 -> String.format("%.1f MB", mb)
            else -> String.format("%.0f KB", kb)
        }
    }

    val formattedDuration: String get() {
        val totalSeconds = duration / 1000
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60
        return if (hours > 0) String.format("%d:%02d:%02d", hours, minutes, seconds)
        else String.format("%d:%02d", minutes, seconds)
    }

    val resolution: String get() = if (width > 0 && height > 0) "${width}x${height}" else ""
}
