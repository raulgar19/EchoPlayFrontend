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

    init {
        loadData()
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
}