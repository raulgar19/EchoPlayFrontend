package com.example.echoplay_frontend.data.services

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.os.Build
import android.os.IBinder
import android.widget.RemoteViews
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import com.example.echoplay_frontend.MainActivity
import com.example.echoplay_frontend.R
import com.example.echoplay_frontend.data.models.Song
import com.example.echoplay_frontend.utils.convertGoogleDriveUrl
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.URL

class MusicService : Service() {

    companion object {
        const val ACTION_PLAY_PAUSE = "com.example.echoplay_frontend.ACTION_PLAY_PAUSE"
        const val ACTION_NEXT = "com.example.echoplay_frontend.ACTION_NEXT"
        const val ACTION_PREVIOUS = "com.example.echoplay_frontend.ACTION_PREVIOUS"
        const val CHANNEL_ID = "music_channel"
        const val NOTIFICATION_ID = 1
        @Volatile
        var mediaPlayer: MediaPlayer? = null
        @Volatile
        var isPlaying: Boolean = false
        var currentFile: String? = null
        var currentSong: Song? = null // ✅ Canción actual para mostrar en notificación
        var isLoopingEnabled: Boolean = false
        var playlist: List<Song> = listOf()
        var shufflePlaylist: List<Song> = emptyList()
        var currentIndex: Int = 0
        var isPlaylistMode: Boolean = false
        var isPrepared: Boolean = false
        var isShuffleMode: Boolean = false
        var isReturningFromPlayerButton: Boolean = false
        
        // Thread-safe release helper
        @Synchronized
        fun safeRelease() {
            try {
                mediaPlayer?.apply {
                    try { setOnPreparedListener(null) } catch (_: Exception) {}
                    try { setOnCompletionListener(null) } catch (_: Exception) {}
                    try { setOnErrorListener(null) } catch (_: Exception) {}
                    if (isPlaying) try { stop() } catch (_: Exception) {}
                    reset()
                    release()
                }
            } finally {
                mediaPlayer = null
                isPlaying = false
                currentFile = null
                currentSong = null
                isPrepared = false
            }
        }
    }

    private var notificationManager: NotificationManager? = null
    private var albumArtBitmap: Bitmap? = null // Cache para la carátula
    private var lastCoverUrl: String? = null // Última URL cargada
    private var isForegroundStarted = false // ✅ Control de foreground service

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate() {
        super.onCreate()

        notificationManager = getSystemService(NotificationManager::class.java)

        if (mediaPlayer == null) {
            mediaPlayer = MediaPlayer().apply {
                // Set reasonable audio attributes for playback
                try {
                    setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .build()
                    )
                } catch (_: Exception) {
                    // ignore on older APIs or if it fails
                }
                // Generic error handler to avoid MediaPlayer being left in bad state
                setOnErrorListener { mp, what, extra ->
                    try {
                        mp.reset()
                    } catch (_: Exception) {}
                    MusicService.isPlaying = false
                    MusicService.isPrepared = false
                    MusicService.currentFile = null
                    true
                }
            }
        }

        val channel = NotificationChannel(
            CHANNEL_ID,
            "Music Playback",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Controles de reproducción de música"
            setShowBadge(false)
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        }
        notificationManager?.createNotificationChannel(channel)

        // ✅ NO crear notificación aquí, solo cuando se reproduzca por primera vez
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_PLAY_PAUSE -> handlePlayPause()
            ACTION_NEXT -> handleNext()
            ACTION_PREVIOUS -> handlePrevious()
            else -> {
                // Cuando se llama startService() sin acción específica, actualizar la notificación
                updateNotification()
            }
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun handlePlayPause() {
        val mp = mediaPlayer
        if (mp == null) {
            return
        }

        try {
            if (isPlaying) {
                // Pausar
                mp.pause()
                isPlaying = false
            } else {
                // Reanudar/Reproducir
                if (isPrepared) {
                    mp.start()
                    isPlaying = true
                }
            }
            updateNotification()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun handleNext() {
        // ✅ Solo funciona si estamos en modo playlist
        if (!isPlaylistMode) {
            return
        }

        try {
            val targetPlaylist = if (isShuffleMode) shufflePlaylist else playlist
            
            if (targetPlaylist.isEmpty()) {
                return
            }

            // Avanzar al siguiente índice
            currentIndex = (currentIndex + 1) % targetPlaylist.size
            
            // Reproducir siguiente canción
            playNextSong()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun handlePrevious() {
        // ✅ Solo funciona si estamos en modo playlist
        if (!isPlaylistMode) {
            return
        }

        try {
            val targetPlaylist = if (isShuffleMode) shufflePlaylist else playlist
            
            if (targetPlaylist.isEmpty()) {
                return
            }

            // Retroceder al índice anterior
            currentIndex = if (currentIndex - 1 < 0) {
                targetPlaylist.size - 1
            } else {
                currentIndex - 1
            }
            
            // Reproducir canción anterior
            playNextSong()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun playNextSong() {
        val mp = mediaPlayer ?: return
        val targetPlaylist = if (isShuffleMode) shufflePlaylist else playlist
        val nextSong = targetPlaylist.getOrNull(currentIndex) ?: return

        try {
            isPrepared = false
            currentSong = nextSong
            
            if (mp.isPlaying) mp.stop()
            mp.reset()
            
            // ✅ Convertir URL de Google Drive si es necesario
            val audioUrl = convertGoogleDriveUrl(nextSong.file)
            mp.setDataSource(audioUrl)
            mp.isLooping = isLoopingEnabled

            mp.setOnPreparedListener { preparedMp ->
                isPrepared = true
                currentFile = nextSong.file
                try {
                    preparedMp.start()
                    isPlaying = true
                    updateNotification()
                } catch (ex: Exception) {
                    ex.printStackTrace()
                    isPlaying = false
                }
            }

            mp.prepareAsync()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun updateNotification() {
        // ✅ Solo crear/actualizar notificación si hay una canción y el servicio debe estar activo
        if (currentSong == null || (!isPlaying && !isForegroundStarted)) {
            return // No mostrar notificación si no hay canción o no se está reproduciendo
        }
        
        val notification = createNotification()
        
        // ✅ Iniciar foreground service solo la primera vez que se reproduce
        if (!isForegroundStarted && isPlaying) {
            startForeground(NOTIFICATION_ID, notification)
            isForegroundStarted = true
        } else if (isForegroundStarted) {
            // Actualizar notificación existente solo si ya se inició
            notificationManager?.notify(NOTIFICATION_ID, notification)
        }
        
        // Cargar imagen en segundo plano si es necesario
        currentSong?.let { song ->
            if (song.cover != lastCoverUrl) {
                // URL cambió, limpiar caché y cargar nueva imagen
                albumArtBitmap = null
                lastCoverUrl = null
                if (song.cover.isNotEmpty()) {
                    loadAlbumArt(song.cover)
                }
            }
        } ?: run {
            // Si no hay canción, limpiar la carátula
            albumArtBitmap = null
            lastCoverUrl = null
        }
    }

    private fun loadAlbumArt(coverUrl: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Convertir URL de Google Drive si es necesario
                val directUrl = convertGoogleDriveUrl(coverUrl)
                val url = URL(directUrl)
                val bitmap = BitmapFactory.decodeStream(url.openConnection().getInputStream())
                albumArtBitmap = bitmap
                lastCoverUrl = coverUrl // ✅ Guardar URL original cargada
                
                // Actualizar notificación en el hilo principal
                withContext(Dispatchers.Main) {
                    notificationManager?.notify(NOTIFICATION_ID, createNotification())
                }
            } catch (e: Exception) {
                e.printStackTrace()
                albumArtBitmap = null
                lastCoverUrl = null
            }
        }
    }

    private fun createNotification(): Notification {
        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val openAppPendingIntent = PendingIntent.getActivity(
            this, 0, openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Intent para el botón Play/Pause - EXPLÍCITO con clase específica
        val playPauseIntent = Intent(this, MusicNotificationReceiver::class.java).apply {
            action = "com.example.echoplay_frontend.PLAY_PAUSE"
        }
        val playPausePendingIntent = PendingIntent.getBroadcast(
            this, 
            100, // Usar request code único
            playPauseIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Intent para el botón Previous
        val previousIntent = Intent(this, MusicNotificationReceiver::class.java).apply {
            action = "com.example.echoplay_frontend.PREVIOUS"
        }
        val previousPendingIntent = PendingIntent.getBroadcast(
            this,
            101, // Request code único
            previousIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Intent para el botón Next
        val nextIntent = Intent(this, MusicNotificationReceiver::class.java).apply {
            action = "com.example.echoplay_frontend.NEXT"
        }
        val nextPendingIntent = PendingIntent.getBroadcast(
            this,
            102, // Request code único
            nextIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Definir icono según el estado
        val playPauseIcon = if (isPlaying) {
            android.R.drawable.ic_media_pause
        } else {
            android.R.drawable.ic_media_play
        }

        // Obtener información de la canción actual
        val songTitle = currentSong?.name ?: "Echo Play"
        val songArtist = currentSong?.artist ?: if (isPlaying) "Reproduciendo..." else "En pausa"

        // Crear RemoteViews personalizado
        val notificationLayout = RemoteViews(packageName, R.layout.notification_music)
        
        // Configurar textos
        notificationLayout.setTextViewText(R.id.song_title, songTitle)
        notificationLayout.setTextViewText(R.id.song_artist, songArtist)
        
        // Configurar imagen del álbum
        if (albumArtBitmap != null) {
            notificationLayout.setImageViewBitmap(R.id.album_art, albumArtBitmap)
        } else {
            // Imagen por defecto si no hay carátula
            notificationLayout.setImageViewResource(R.id.album_art, android.R.drawable.ic_menu_gallery)
        }
        
        // Configurar botón Play/Pause
        notificationLayout.setImageViewResource(R.id.play_pause_button, playPauseIcon)
        notificationLayout.setOnClickPendingIntent(R.id.play_pause_button, playPausePendingIntent)

        // Configurar botones Previous y Next (solo visibles en modo playlist)
        if (isPlaylistMode) {
            notificationLayout.setOnClickPendingIntent(R.id.previous_button, previousPendingIntent)
            notificationLayout.setOnClickPendingIntent(R.id.next_button, nextPendingIntent)
            // Los botones ya están en el layout, solo configuramos los clicks
        } else {
            // Si no estamos en modo playlist, los botones siguen visibles pero no hacen nada
            // (el handler ya valida isPlaylistMode)
        }

        // Crear notificación con layout personalizado
        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setCustomContentView(notificationLayout)
            .setCustomBigContentView(notificationLayout) // Mismo layout para vista expandida
            .setContentIntent(openAppPendingIntent)
            .setOngoing(isPlaying)
            .setShowWhen(false)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_TRANSPORT)

        return builder.build()
    }

    override fun onDestroy() {
        safeRelease()
        isLoopingEnabled = false
        isForegroundStarted = false // ✅ Resetear flag
        albumArtBitmap = null // ✅ Limpiar imagen
        lastCoverUrl = null // ✅ Limpiar URL
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            @Suppress("DEPRECATION")
            stopForeground(true)
        }
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