package com.example.echoplay_frontend.viewmodels

import android.annotation.SuppressLint
import android.app.Application
import android.content.Intent
import android.media.MediaPlayer
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.echoplay_frontend.data.services.MusicService
import com.example.echoplay_frontend.data.models.Playlist
import com.example.echoplay_frontend.data.models.Song
import com.example.echoplay_frontend.data.models.SongToAdd
import com.example.echoplay_frontend.data.network.RetrofitInstance
import com.example.echoplay_frontend.utils.convertGoogleDriveUrl
import kotlinx.coroutines.launch

class PlayerViewModel(application: Application) : AndroidViewModel(application) {

    var song by mutableStateOf<Song?>(null)
        private set

    var playlists by mutableStateOf<List<Playlist>>(emptyList())
        private set

    // Nueva variable para guardar la playlist actual
    var currentPlaylist by mutableStateOf<List<Song>?>(null)
        private set

    var isLoading by mutableStateOf(true)
        private set

    var sliderPosition by mutableFloatStateOf(0f)

    var isLooping by mutableStateOf(MusicService.isLoopingEnabled)
        private set

    var isShuffleMode by mutableStateOf(MusicService.isShuffleMode)
        private set

    private val prefs = application.getSharedPreferences("MyPrefs", Application.MODE_PRIVATE)
    private val songId = prefs.getInt("songID", -1)
    private val userId = prefs.getInt("userID", -1)
    private val context = application.applicationContext

    init {
        isLoading = true

        // 🔹 Verificar si estamos volviendo al reproductor o reproduciendo una nueva canción
        if (MusicService.isReturningFromPlayerButton && MusicService.currentSong != null) {
            // Volviendo al reproductor - mantener la canción actual
            song = MusicService.currentSong
            currentPlaylist = MusicService.playlist.takeIf { it.isNotEmpty() }
            isLoading = false
        } else if (MusicService.isPlaylistMode) {
            // Modo playlist - cargar desde MusicService
            currentPlaylist = MusicService.playlist

            // Asignar la canción si currentIndex es válido
            if (currentPlaylist != null && currentPlaylist!!.isNotEmpty()) {
                // Asegurarse de que currentIndex esté en rango
                if (MusicService.currentIndex >= currentPlaylist!!.size) {
                    MusicService.currentIndex = 0
                }
                song = currentPlaylist!![MusicService.currentIndex]
            }

            isLoading = false
        } else {
            // Reproducir canción individual - cargar desde songID
            if (songId != -1) {
                loadSong()
            } else if (MusicService.currentSong != null) {
                // Fallback: si no hay songID pero hay una canción en el servicio
                song = MusicService.currentSong
                isLoading = false
            } else {
                isLoading = false
            }
        }

        loadPlaylists()
    }

    // 🔹 Función para actualizar la canción desde MusicService
    fun updateSongFromService(currentSong: Song) {
        song = currentSong
        currentPlaylist = MusicService.playlist.takeIf { it.isNotEmpty() }
    }

    private fun loadSong() {
        viewModelScope.launch {
            isLoading = true
            try {
                song = RetrofitInstance.api.getSongById(songId)
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                isLoading = false
            }
        }
    }

    fun loadPlaylists() {
        viewModelScope.launch {
            try {
                playlists = if (userId != -1) RetrofitInstance.api.getPlaylists(userId)
                else emptyList()
            } catch (e: Exception) {
                e.printStackTrace()
                playlists = emptyList()
            }
        }
    }

    fun addToPlaylist(song: Song, playlistId: Int) {
        viewModelScope.launch {
            try {
                val body = SongToAdd(song.id)
                RetrofitInstance.api.addSongToPlaylist(playlistId, body)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun playSong() {
        val mp: MediaPlayer = MusicService.mediaPlayer ?: MediaPlayer().also { MusicService.mediaPlayer = it }

        if (MusicService.isReturningFromPlayerButton && MusicService.isPlaying && MusicService.currentFile == song?.file) {
            MusicService.isReturningFromPlayerButton = false
            return
        }

        if (MusicService.isPlaylistMode) {
            if (MusicService.isShuffleMode) {
                // 🔹 Crear shufflePlaylist solo si está vacía
                if (MusicService.shufflePlaylist.isEmpty() && MusicService.playlist.isNotEmpty()) {
                    MusicService.shufflePlaylist = MusicService.playlist.shuffled()
                    MusicService.currentIndex = 0
                }
                // 🔹 Solo actualizar la canción si no tenemos una ya asignada
                if (song == null) {
                    song = MusicService.shufflePlaylist.getOrNull(MusicService.currentIndex)
                }
            } else {
                // 🔹 Solo actualizar la canción si no tenemos una ya asignada
                if (song == null) {
                    song = MusicService.playlist.getOrNull(MusicService.currentIndex)
                }
            }
        }

        val s = song ?: return

        try {
            MusicService.isPrepared = false
            MusicService.currentSong = s // ✅ Actualizar canción ANTES de preparar
            if (mp.isPlaying) mp.stop()
            mp.reset()

            // ✅ Convertir URL de Google Drive si es necesario
            val audioUrl = convertGoogleDriveUrl(s.file)
            mp.setDataSource(audioUrl)
            mp.isLooping = MusicService.isLoopingEnabled

            mp.setOnPreparedListener { preparedMp ->
                MusicService.isPrepared = true
                MusicService.currentFile = s.file
                try {
                    preparedMp.start()
                    MusicService.isPlaying = true
                    updateServiceNotification() // ✅ Actualizar notificación cuando empieza a reproducir
                } catch (ex: Exception) {
                    ex.printStackTrace()
                    MusicService.isPlaying = false
                }
            }

            mp.setOnCompletionListener { _ ->
                if (!MusicService.isPrepared) return@setOnCompletionListener
                MusicService.isPlaying = false
                MusicService.currentFile = null
                updateServiceNotification() // ✅ Actualizar notificación cuando termina

                if (MusicService.isPlaylistMode) {
                    playNextInPlaylist()
                }
            }

            mp.prepareAsync()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun updateServiceNotification() {
        try {
            val intent = Intent(context, MusicService::class.java)
            context.startService(intent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun playNextInPlaylist() {
        val nextIndex = MusicService.currentIndex + 1
        if (nextIndex < MusicService.playlist.size) {
            MusicService.currentIndex = nextIndex
            song = MusicService.playlist[nextIndex]

            android.os.Handler(android.os.Looper.getMainLooper()).post {
                playSong()
            }
        } else {
            MusicService.isPlaylistMode = false
        }
    }

    fun togglePlayPause() {
        val mp = MusicService.mediaPlayer ?: return
        val currentFile = MusicService.currentFile
        val songFile = song?.file

        if (MusicService.isPlaying) {
            // Si está sonando, pausamos
            try {
                mp.pause()
                MusicService.isPlaying = false
                updateServiceNotification()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        } else {
            // Si está pausado y es la misma canción ya cargada, reanudamos desde la posición actual
            if (currentFile != null && songFile != null && currentFile == songFile && MusicService.isPrepared) {
                try {
                    mp.start()
                    MusicService.isPlaying = true
                    updateServiceNotification()
                } catch (e: Exception) {
                    e.printStackTrace()
                    // fallback: recargar la canción si hay un problema
                    playSong()
                }
            } else {
                // Si no hay canción cargada o es distinta, arrancamos la reproducción normal
                playSong()
            }
        }
    }

    fun pauseSong() {
        val mp = MusicService.mediaPlayer ?: return
        if (mp.isPlaying) {
            mp.pause()
            MusicService.isPlaying = false
            updateServiceNotification() // ✅ Actualizar notificación al pausar
        }
    }

    fun stopSong() {
        val mp = MusicService.mediaPlayer ?: return
        if (mp.isPlaying) mp.stop()
        MusicService.isPlaying = false
        MusicService.currentFile = null
        updateServiceNotification() // ✅ Actualizar notificación al detener
    }

    fun seekTo(percent: Float) {
        val mp = MusicService.mediaPlayer ?: return
        try {
            val duration = mp.duration.coerceAtLeast(1)
            val pos = (percent / 100f * duration).toInt().coerceIn(0, duration)
            mp.seekTo(pos)
        } catch (e: IllegalStateException) {
            e.printStackTrace()
        }
    }

    fun updateSlider() {
        MusicService.mediaPlayer?.let {
            try {
                val dur = it.duration.coerceAtLeast(1)
                sliderPosition = (it.currentPosition.toFloat() / dur) * 100f
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun isPlaying(): Boolean = MusicService.isPlaying

    fun toggleLooping() {
        MusicService.isLoopingEnabled = !MusicService.isLoopingEnabled
        MusicService.mediaPlayer?.isLooping = MusicService.isLoopingEnabled
        isLooping = MusicService.isLoopingEnabled
    }

    fun toggleShuffle() {
        isShuffleMode = !isShuffleMode
        MusicService.isShuffleMode = isShuffleMode
    }

    fun playNext() {
        if (MusicService.isPlaylistMode) {
            val targetPlaylist = if (MusicService.isShuffleMode) MusicService.shufflePlaylist else MusicService.playlist
            
            if (targetPlaylist.isNotEmpty()) {
                // 🔹 Avanzar al siguiente índice circular
                MusicService.currentIndex = (MusicService.currentIndex + 1) % targetPlaylist.size
                song = targetPlaylist[MusicService.currentIndex]
                playSong()
            }
        } else {
            stopSong()
        }
    }

    fun playPrevious() {
        if (MusicService.isPlaylistMode) {
            val targetPlaylist = if (MusicService.isShuffleMode) MusicService.shufflePlaylist else MusicService.playlist
            
            if (targetPlaylist.isNotEmpty()) {
                // 🔹 Retroceder al índice anterior circular
                MusicService.currentIndex =
                    if (MusicService.currentIndex - 1 < 0) targetPlaylist.size - 1
                    else MusicService.currentIndex - 1
                song = targetPlaylist[MusicService.currentIndex]
                playSong()
            }
        } else {
            song?.let { playSong() }
        }
    }
}