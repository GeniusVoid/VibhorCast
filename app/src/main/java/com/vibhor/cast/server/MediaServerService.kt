package com.vibhor.cast.server

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.util.Log

class MediaServerService : Service() {
    companion object {
        private const val TAG = "MediaServerService"
        private const val CHANNEL_ID = "vibhorcast_server"
        private const val NOTIFICATION_ID = 1
    }

    private var server: MediaServer? = null
    private val binder = LocalBinder()

    inner class LocalBinder : Binder() {
        fun getService(): MediaServerService = this@MediaServerService
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(NOTIFICATION_ID, createNotification())
        startServer()
        return START_STICKY
    }

    private fun startServer() {
        if (server == null) {
            server = MediaServer(this, 8080)
            try {
                server?.start()
                Log.i(TAG, "Media server started on port 8080")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to start media server", e)
            }
        }
    }

    fun stopServer() {
        server?.stop()
        server = null
        Log.i(TAG, "Media server stopped")
    }

    override fun onDestroy() {
        stopServer()
        super.onDestroy()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "VibhorCast Media Server",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Keeps the media server running for TV casting"
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        return Notification.Builder(this, CHANNEL_ID)
            .setContentTitle("VibhorCast")
            .setContentText("Media server running - serving videos to TV")
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setOngoing(true)
            .build()
    }
}
