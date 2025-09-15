package com.example.echoplay_frontend.viewmodels

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

    var isLoading by mutableStateOf(true)
        private set

    var sliderPosition by mutableFloatStateOf(0f)

    var isLooping by mutableStateOf(MusicService.isLoopingEnabled)
        private set

    private val prefs = application.getSharedPreferences("MyPrefs", Application.MODE_PRIVATE)
    private val songId = prefs.getInt("songID", -1)
    private val userId = prefs.getInt("userID", -1)

    init {
        if (songId != -1) loadSong()
        else isLoading = false

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
        val s = song ?: return
        val mp: MediaPlayer = MusicService.mediaPlayer ?: MediaPlayer().also { MusicService.mediaPlayer = it }

        // Si la canción actual ya es la que suena y está pausada, simplemente reproducir desde la posición actual
        if (MusicService.currentFile == s.file && !mp.isPlaying) {
            mp.start()
            MusicService.isPlaying = true
            return
        }

        // Si la canción actual ya está sonando, no hacer nada
        if (MusicService.currentFile == s.file && mp.isPlaying) return

        try {
            // Reiniciar el MediaPlayer solo si es una canción diferente
            if (mp.isPlaying) mp.stop()
            mp.reset()

            MusicService.currentFile = s.file
            mp.isLooping = MusicService.isLoopingEnabled

            mp.setDataSource(s.file)
            mp.setOnPreparedListener {
                it.start()
                MusicService.isPlaying = true
            }
            mp.setOnCompletionListener {
                MusicService.isPlaying = false
                MusicService.currentFile = null
            }

            mp.prepareAsync()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun playSongAtIndex(index: Int) {
        if (MusicService.playlist.isEmpty()) return
        // Aseguramos que el índice es circular
        val safeIndex = (index % MusicService.playlist.size + MusicService.playlist.size) % MusicService.playlist.size
        song = MusicService.playlist[safeIndex]
        MusicService.currentIndex = safeIndex
        playSong()
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

    fun nextSong() {
        val nextIndex = MusicService.currentIndex + 1
        playSongAtIndex(nextIndex)
    }

    fun previousSong() {
        val prevIndex = MusicService.currentIndex - 1
        playSongAtIndex(prevIndex)
    }
}