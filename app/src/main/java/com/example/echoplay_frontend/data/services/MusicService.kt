package com.example.echoplay_frontend.data.services

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.media.MediaPlayer
import android.os.Build
import android.os.IBinder
import androidx.annotation.RequiresApi
import com.example.echoplay_frontend.data.models.Song

class MusicService : Service() {

    companion object {
        var mediaPlayer: MediaPlayer? = null
        var isPlaying: Boolean = false
        var currentFile: String? = null

        // 🔹 Nueva variable global para loop
        var isLoopingEnabled: Boolean = false

        var playlist: List<Song> = listOf()
        var currentIndex: Int = 0
        var isPlaylistMode: Boolean = false

        var isPrepared: Boolean = false
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate() {
        super.onCreate()

        if (mediaPlayer == null) {
            mediaPlayer = MediaPlayer()
        }

        val channel = NotificationChannel(
            "music_channel",
            "Music Playback",
            NotificationManager.IMPORTANCE_LOW
        )
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)

        // Notificación mínima para servicio foreground
        val notification: Notification = Notification.Builder(this, "music_channel")
            .setContentTitle("Echo Play")
            .setContentText("Reproduciendo música...")
            .setSmallIcon(android.R.drawable.ic_media_play)
            .build()

        startForeground(1, notification)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        mediaPlayer?.release()
        mediaPlayer = null
        isPlaying = false
        currentFile = null
        isLoopingEnabled = false
        super.onDestroy()
    }

    // Métodos auxiliares para controlar el loop
    fun setLooping(loop: Boolean) {
        isLoopingEnabled = loop
        mediaPlayer?.isLooping = loop
    }

    fun toggleLooping() {
        setLooping(!isLoopingEnabled)
    }
}