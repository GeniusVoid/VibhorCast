package com.vibhor.cast.util

import android.content.ContentResolver
import android.content.ContentUris
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import com.vibhor.cast.model.VideoFile

object MediaScanner {
    fun scanVideos(contentResolver: ContentResolver): List<VideoFile> {
        val videos = mutableListOf<VideoFile>()
        try {
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
                val nameCol = cursor.getColumnIndex(MediaStore.Video.Media.DISPLAY_NAME)
                val pathCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATA)
                val sizeCol = cursor.getColumnIndex(MediaStore.Video.Media.SIZE)
                val durationCol = cursor.getColumnIndex(MediaStore.Video.Media.DURATION)
                val mimeCol = cursor.getColumnIndex(MediaStore.Video.Media.MIME_TYPE)
                val dateCol = cursor.getColumnIndex(MediaStore.Video.Media.DATE_MODIFIED)
                val widthCol = cursor.getColumnIndex(MediaStore.Video.Media.WIDTH)
                val heightCol = cursor.getColumnIndex(MediaStore.Video.Media.HEIGHT)

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idCol)
                    val uri = ContentUris.withAppendedId(collection, id)
                    videos.add(VideoFile(
                        id = id,
                        name = if (nameCol >= 0) cursor.getString(nameCol) ?: "Unknown" else "Unknown",
                        path = cursor.getString(pathCol) ?: "",
                        uri = uri,
                        size = if (sizeCol >= 0) cursor.getLong(sizeCol) else 0L,
                        duration = if (durationCol >= 0) cursor.getLong(durationCol) else 0L,
                        mimeType = if (mimeCol >= 0) cursor.getString(mimeCol) ?: "video/mp4" else "video/mp4",
                        dateModified = if (dateCol >= 0) cursor.getLong(dateCol) else 0L,
                        width = if (widthCol >= 0) cursor.getInt(widthCol) else 0,
                        height = if (heightCol >= 0) cursor.getInt(heightCol) else 0
                    ))
                }
            }
        } catch (e: Exception) {
            Log.e("MediaScanner", "Error scanning videos", e)
        }
        return videos
    }
}
