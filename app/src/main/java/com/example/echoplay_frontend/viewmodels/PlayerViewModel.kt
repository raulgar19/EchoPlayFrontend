package com.example.echoplay_frontend.viewmodels

import android.annotation.SuppressLint
import android.app.Application
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

    init {
        isLoading = true

        if (MusicService.isPlaylistMode) {
            // 🔹 Cargar playlist desde MusicService
            currentPlaylist = MusicService.playlist

            // 🔹 Asignar la primera canción si currentIndex es válido
            if (currentPlaylist != null && currentPlaylist!!.isNotEmpty()) {
                // Asegurarse de que currentIndex esté en 0
                if (MusicService.currentIndex >= currentPlaylist!!.size) {
                    MusicService.currentIndex = 0
                }
                song = currentPlaylist!![MusicService.currentIndex]
            }

            isLoading = false
        } else {
            if (songId != -1) loadSong()
            else isLoading = false
        }

        loadPlaylists()
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
                song = MusicService.shufflePlaylist.getOrNull(MusicService.currentIndex)
            } else {
                song = MusicService.playlist.getOrNull(MusicService.currentIndex)
            }
        }

        val s = song ?: return

        try {
            MusicService.isPrepared = false
            if (mp.isPlaying) mp.stop()
            mp.reset()

            mp.setDataSource(s.file)
            mp.isLooping = MusicService.isLoopingEnabled

            mp.setOnPreparedListener {
                MusicService.isPrepared = true
                MusicService.currentFile = s.file
                it.start()
                MusicService.isPlaying = true
            }

            mp.setOnCompletionListener {
                if (!MusicService.isPrepared) return@setOnCompletionListener
                MusicService.isPlaying = false
                MusicService.currentFile = null

                if (MusicService.isPlaylistMode) {
                    playNextInPlaylist()
                }
            }

            mp.prepareAsync()
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
            } catch (e: Exception) {
                e.printStackTrace()
            }
        } else {
            // Si está pausado y es la misma canción ya cargada, reanudamos desde la posición actual
            if (currentFile != null && songFile != null && currentFile == songFile && MusicService.isPrepared) {
                try {
                    mp.start()
                    MusicService.isPlaying = true
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
        }
    }

    fun stopSong() {
        val mp = MusicService.mediaPlayer ?: return
        if (mp.isPlaying) mp.stop()
        MusicService.isPlaying = false
        MusicService.currentFile = null
    }

    fun seekTo(percent: Float) {
        val mp = MusicService.mediaPlayer ?: return
        if (mp.duration > 0) {
            val pos = (percent / 100f * mp.duration).toInt()
            mp.seekTo(pos)
        }
    }

    fun updateSlider() {
        MusicService.mediaPlayer?.let {
            sliderPosition = (it.currentPosition.toFloat() / it.duration.coerceAtLeast(1)) * 100f
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
        if (MusicService.isPlaylistMode && currentPlaylist != null && currentPlaylist!!.isNotEmpty()) {
            // 🔹 Avanzar al siguiente índice circular
            MusicService.currentIndex = (MusicService.currentIndex + 1) % currentPlaylist!!.size
            song = currentPlaylist!![MusicService.currentIndex]
            playSong()
        } else {
            stopSong()
        }
    }

    fun playPrevious() {
        if (MusicService.isPlaylistMode && currentPlaylist != null && currentPlaylist!!.isNotEmpty()) {
            // 🔹 Retroceder al índice anterior circular
            MusicService.currentIndex =
                if (MusicService.currentIndex - 1 < 0) currentPlaylist!!.size - 1
                else MusicService.currentIndex - 1
            song = currentPlaylist!![MusicService.currentIndex]
            playSong()
        } else {
            song?.let { playSong() }
        }
    }
}