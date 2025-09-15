package com.example.echoplay_frontend.data.network

import com.example.echoplay_frontend.data.models.CreatePlaylistRequest
import com.example.echoplay_frontend.data.models.Playlist
import com.example.echoplay_frontend.data.models.Song
import com.example.echoplay_frontend.data.models.SongToAdd
import com.example.echoplay_frontend.data.models.User
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface ApiService {
    @GET("users")
    suspend fun getUsers(): List<User>

    @GET("songs")
    suspend fun getSongs(): List<Song>

    @GET("users/{userId}/playlists")
    suspend fun getPlaylists(@Path("userId") userId: Int): List<Playlist>

    @POST("playlists")
    suspend fun createPlaylist(
        @Body body: CreatePlaylistRequest
    ): Playlist

    @GET("playlists/{playlistId}/songs")
    suspend fun getSongsFromPlaylist(@Path("playlistId") playlistId: Int): List<Song>

    @GET("songs/{id}")
    suspend fun getSongById(@Path("id") songId: Int): Song

    @POST("playlists/{playlistId}/songs")
    suspend fun addSongToPlaylist(
        @Path("playlistId") playlistId: Int,
        @Body body: SongToAdd
    ): Response<Unit>

    @DELETE("playlists/{playlistId}/songs/{songId}")
    suspend fun removeSongFromPlaylist(
        @Path("playlistId") playlistId: Int,
        @Path("songId") songId: Int
    )
}