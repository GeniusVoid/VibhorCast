package com.vibhor.cast.cast

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

class ExoAirPlayerClient(private var tvAddress: String = "172.24.238.117:8192") {

    companion object {
        private const val TAG = "ExoAirPlayerClient"
    }

    fun setTvAddress(address: String) {
        tvAddress = address
    }

    private val baseUrl get() = "http://$tvAddress"

    /**
     * Cast a video URL to the TV.
     * The URL should be an HTTP URL pointing to the phone's media server.
     */
    suspend fun play(videoUrl: String, startPosition: Double = 0.0): Result<String> = withContext(Dispatchers.IO) {
        try {
            val url = URL("$baseUrl/play")
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.setRequestProperty("Content-Type", "text/parameters")
            conn.doOutput = true
            conn.connectTimeout = 5000
            conn.readTimeout = 5000

            val body = "Content-Location: $videoUrl\nStart-Position: $startPosition"
            conn.outputStream.use { it.write(body.toByteArray()) }

            val responseCode = conn.responseCode
            Log.d(TAG, "Play response: $responseCode")
            conn.disconnect()

            if (responseCode in 200..299) Result.success("Playing")
            else Result.failure(Exception("HTTP $responseCode"))
        } catch (e: Exception) {
            Log.e(TAG, "Play failed", e)
            Result.failure(e)
        }
    }

    /** Add video to queue */
    suspend fun queue(videoUrl: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val url = URL("$baseUrl/queue")
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.setRequestProperty("Content-Type", "text/parameters")
            conn.doOutput = true
            conn.connectTimeout = 5000
            conn.readTimeout = 5000

            val body = "Content-Location: $videoUrl"
            conn.outputStream.use { it.write(body.toByteArray()) }

            val code = conn.responseCode
            conn.disconnect()
            if (code in 200..299) Result.success("Queued") else Result.failure(Exception("HTTP $code"))
        } catch (e: Exception) {
            Log.e(TAG, "Queue failed", e)
            Result.failure(e)
        }
    }

    /** Toggle pause */
    suspend fun togglePause(): Result<String> = sendGet("/pause")

    /** Pause playback */
    suspend fun pause(): Result<String> = sendGet("/pause?toggle=1")

    /** Resume playback */
    suspend fun resume(): Result<String> = sendGet("/pause?toggle=0")

    /** Stop playback */
    suspend fun stop(): Result<String> = sendGet("/stop")

    /** Seek to position in seconds */
    suspend fun seek(positionSeconds: Double): Result<String> = sendGet("/scrub?position=$positionSeconds")

    /** Seek forward/backward by offset in milliseconds */
    suspend fun seekOffset(offsetMs: Long): Result<String> = sendGet("/add-scrub-offset?value=$offsetMs")

    /** Next track in queue */
    suspend fun next(): Result<String> = sendGet("/next")

    /** Previous track in queue */
    suspend fun previous(): Result<String> = sendGet("/previous")

    /** Set playback rate */
    suspend fun setRate(rate: Float): Result<String> = sendGet("/rate?value=$rate")

    /** Set volume (0.0 to 1.0) */
    suspend fun setVolume(volume: Float): Result<String> = sendGet("/volume?value=$volume")

    /** Mute/unmute */
    suspend fun toggleMute(): Result<String> = sendGet("/volume?toggle=mute")

    /** Check if TV is reachable */
    suspend fun isReachable(): Boolean = withContext(Dispatchers.IO) {
        try {
            val url = URL("$baseUrl/server-info")
            val conn = url.openConnection() as HttpURLConnection
            conn.connectTimeout = 3000
            conn.readTimeout = 3000
            val code = conn.responseCode
            conn.disconnect()
            code in 200..299
        } catch (e: Exception) {
            false
        }
    }

    private suspend fun sendGet(endpoint: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val url = URL("$baseUrl$endpoint")
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "GET"
            conn.connectTimeout = 5000
            conn.readTimeout = 5000

            val code = conn.responseCode
            val body = try { conn.inputStream.bufferedReader().readText() } catch (_: Exception) { "" }
            conn.disconnect()

            Log.d(TAG, "GET $endpoint -> $code")
            if (code in 200..299) Result.success(body) else Result.failure(Exception("HTTP $code"))
        } catch (e: Exception) {
            Log.e(TAG, "GET $endpoint failed", e)
            Result.failure(e)
        }
    }
}
