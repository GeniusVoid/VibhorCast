package com.vibhor.cast.server

import android.content.Context
import android.util.Log
import fi.iki.elonen.NanoHTTPD
import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import java.net.URLDecoder

class MediaServer(private val context: Context, port: Int = 8080) : NanoHTTPD(port) {

    companion object {
        private const val TAG = "MediaServer"
    }

    override fun serve(session: IHTTPSession): Response {
        val uri = URLDecoder.decode(session.uri, "UTF-8")
        Log.d(TAG, "Request: ${session.method} $uri")

        if (uri == "/" || uri == "/ping") {
            return newFixedLengthResponse(Response.Status.OK, "text/plain", "VibhorCast Media Server")
        }

        val filePath = uri.removePrefix("/")
        val file = File(filePath)

        if (!file.exists() || !file.isFile) {
            Log.w(TAG, "File not found: $filePath")
            return newFixedLengthResponse(Response.Status.NOT_FOUND, "text/plain", "File not found")
        }

        val mimeType = getMimeType(file.name)
        val fileLength = file.length()
        val rangeHeader = session.headers["range"]

        return if (rangeHeader != null) {
            servePartialContent(file, fileLength, mimeType, rangeHeader)
        } else {
            serveFullContent(file, fileLength, mimeType)
        }
    }

    private fun serveFullContent(file: File, fileLength: Long, mimeType: String): Response {
        val fis = FileInputStream(file)
        val response = newFixedLengthResponse(Response.Status.OK, mimeType, fis, fileLength)
        response.addHeader("Accept-Ranges", "bytes")
        response.addHeader("Content-Length", fileLength.toString())
        return response
    }

    private fun servePartialContent(file: File, fileLength: Long, mimeType: String, rangeHeader: String): Response {
        // Parse Range header: "bytes=START-END" or "bytes=START-"
        val rangeValue = rangeHeader.replace("bytes=", "").trim()
        val parts = rangeValue.split("-")

        val start = parts[0].toLongOrNull() ?: 0L
        val end = if (parts.size > 1 && parts[1].isNotEmpty()) {
            parts[1].toLongOrNull() ?: (fileLength - 1)
        } else {
            fileLength - 1
        }

        val contentLength = end - start + 1

        val fis = FileInputStream(file)
        fis.skip(start)

        val response = newFixedLengthResponse(
            Response.Status.PARTIAL_CONTENT,
            mimeType,
            fis,
            contentLength
        )
        response.addHeader("Accept-Ranges", "bytes")
        response.addHeader("Content-Range", "bytes $start-$end/$fileLength")
        response.addHeader("Content-Length", contentLength.toString())
        return response
    }

    private fun getMimeType(fileName: String): String {
        val ext = fileName.substringAfterLast('.', "").lowercase()
        return when (ext) {
            "mp4" -> "video/mp4"
            "mkv" -> "video/x-matroska"
            "avi" -> "video/x-msvideo"
            "webm" -> "video/webm"
            "mov" -> "video/quicktime"
            "flv" -> "video/x-flv"
            "m4v" -> "video/mp4"
            "3gp" -> "video/3gpp"
            "ts" -> "video/mp2t"
            "wmv" -> "video/x-ms-wmv"
            else -> "application/octet-stream"
        }
    }
}
