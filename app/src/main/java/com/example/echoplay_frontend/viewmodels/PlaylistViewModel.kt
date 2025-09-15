package com.example.echoplay_frontend.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.echoplay_frontend.data.models.Song
import com.example.echoplay_frontend.data.network.RetrofitInstance
import kotlinx.coroutines.launch
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue

class PlaylistViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = application.getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
    private val playlistId: Int = prefs.getInt("playlistID", -1)
    val playlistName: String = prefs.getString("playlistName", "") ?: ""

    var songs by mutableStateOf<List<Song>>(emptyList())
        private set

    var isLoading by mutableStateOf(true)
        private set

    init {
        loadSongs()
    }

    private fun loadSongs() {
        if (playlistId == -1) return

        viewModelScope.launch {
            isLoading = true
            try {
                val songsResponse = RetrofitInstance.api.getSongsFromPlaylist(playlistId)
                songs = songsResponse
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                isLoading = false
            }
        }
    }

    fun removeSongFromPlaylist(songId: Int) {
        viewModelScope.launch {
            try {
                val playlistId = prefs.getInt("playlistID", -1)
                if (playlistId != -1) {
                    RetrofitInstance.api.removeSongFromPlaylist(playlistId, songId)
                    // Actualizar lista localmente
                    songs = songs.filter { it.id != songId }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}