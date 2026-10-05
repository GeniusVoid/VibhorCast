package com.vibhor.cast.util

import android.content.ContentResolver
import android.content.ContentUris
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import com.vibhor.cast.model.VideoFile

object MediaScanner {
    fun scanVideos(contentResolver: ContentResolver): List<VideoFile> {
        val videos = mutableListOf<VideoFile>()
        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        }

        val projection = arrayOf(
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.DISPLAY_NAME,
            MediaStore.Video.Media.DATA,
            MediaStore.Video.Media.SIZE,
            MediaStore.Video.Media.DURATION,
            MediaStore.Video.Media.MIME_TYPE,
            MediaStore.Video.Media.DATE_MODIFIED,
            MediaStore.Video.Media.WIDTH,
            MediaStore.Video.Media.HEIGHT
        )

        val sortOrder = "${MediaStore.Video.Media.DATE_MODIFIED} DESC"

        contentResolver.query(collection, projection, null, null, sortOrder)?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
            val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME)
            val pathCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATA)
            val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE)
            val durationCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION)
            val mimeCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.MIME_TYPE)
            val dateCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_MODIFIED)
            val widthCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.WIDTH)
            val heightCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.HEIGHT)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idCol)
                val uri = ContentUris.withAppendedId(collection, id)
                videos.add(VideoFile(
                    id = id,
                    name = cursor.getString(nameCol) ?: "Unknown",
                    path = cursor.getString(pathCol) ?: "",
                    uri = uri,
                    size = cursor.getLong(sizeCol),
                    duration = cursor.getLong(durationCol),
                    mimeType = cursor.getString(mimeCol) ?: "video/mp4",
                    dateModified = cursor.getLong(dateCol),
                    width = cursor.getInt(widthCol),
                    height = cursor.getInt(heightCol)
                ))
            }
        }
        return videos
    }
}
