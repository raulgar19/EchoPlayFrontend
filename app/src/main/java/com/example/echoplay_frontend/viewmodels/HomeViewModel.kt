import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.echoplay_frontend.data.models.Song
import com.example.echoplay_frontend.data.models.Playlist
import com.example.echoplay_frontend.data.network.RetrofitInstance
import kotlinx.coroutines.launch
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import com.example.echoplay_frontend.data.models.CreatePlaylistRequest
import com.example.echoplay_frontend.data.models.SongToAdd

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = application.getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
    private val userId: Int = prefs.getInt("userID", -1)

    var songs by mutableStateOf<List<Song>>(emptyList())
        private set

    var playlists by mutableStateOf<List<Playlist>>(emptyList())
        private set

    var searchQuery by mutableStateOf("")
        private set

    var isLoading by mutableStateOf(true)
        private set

    var selectedTab by mutableStateOf(0)
        private set

    init {
        loadData()
    }

    fun updateSelectedTab(index: Int) {
        selectedTab = index
    }

    private fun loadData() {
        viewModelScope.launch {
            isLoading = true
            try {
                val songsResponse = RetrofitInstance.api.getSongs()
                val playlistsResponse = if (userId != -1) {
                    RetrofitInstance.api.getPlaylists(userId)
                } else emptyList()

                songs = songsResponse
                playlists = playlistsResponse
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                isLoading = false
            }
        }
    }

    fun updateSearch(query: String) {
        searchQuery = query
    }

    fun filteredSongs(): List<Song> {
        val q = searchQuery.trim().lowercase()
        return if (q.isEmpty()) songs
        else songs.filter { it.name.lowercase().contains(q) || it.artist.lowercase().contains(q) }
    }

    fun addPlaylist(name: String) {
        if (userId == -1) return

        viewModelScope.launch {
            try {
                val newPlaylist = RetrofitInstance.api.createPlaylist(
                    CreatePlaylistRequest(
                        name = name,
                        userId = userId
                    )
                )

                playlists = playlists + Playlist(
                    id = newPlaylist.id,
                    name = newPlaylist.name,
                    userId = newPlaylist.userId
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun addSongToPlaylist(songId: Int, playlistId: Int) {
        viewModelScope.launch {
            try {
                val response = RetrofitInstance.api.addSongToPlaylist(
                    playlistId = playlistId,
                    body = SongToAdd(songId)
                )
                if (response.isSuccessful) {
                    loadData()
                } else {
                    println("Error al añadir la canción a la playlist: ${response.code()}")
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun deletePlaylist(id: Int) {
        viewModelScope.launch {
            try {
                val response = RetrofitInstance.api.deletePlaylist(id) // 👈 llamada al backend
                if (response.isSuccessful) {
                    // 🔹 refrescamos la lista desde el backend
                    loadData()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun createAndPlayFusionMix(playlistIds: List<Int>, onComplete: () -> Unit) {
        viewModelScope.launch {
            try {
                // Obtener todas las canciones de las playlists seleccionadas
                val allSongs = mutableListOf<Song>()
                
                playlistIds.forEach { playlistId ->
                    try {
                        val songsFromPlaylist = RetrofitInstance.api.getSongsFromPlaylist(playlistId)
                        allSongs.addAll(songsFromPlaylist)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
                
                // Eliminar duplicados basándose en el ID de la canción
                val uniqueSongs = allSongs.distinctBy { it.id }
                
                // Mezclar aleatoriamente
                val shuffledSongs = uniqueSongs.shuffled()
                
                if (shuffledSongs.isNotEmpty()) {
                    // Configurar el MusicService para modo Fusion Mix
                    com.example.echoplay_frontend.data.services.MusicService.playlist = shuffledSongs
                    com.example.echoplay_frontend.data.services.MusicService.currentIndex = 0
                    com.example.echoplay_frontend.data.services.MusicService.isPlaylistMode = true
                    com.example.echoplay_frontend.data.services.MusicService.isFusionMixMode = true
                    com.example.echoplay_frontend.data.services.MusicService.isShuffleMode = false
                    com.example.echoplay_frontend.data.services.MusicService.shufflePlaylist = emptyList()
                    
                    // Llamar al callback
                    onComplete()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}