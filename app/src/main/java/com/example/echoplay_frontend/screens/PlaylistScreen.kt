package com.example.echoplay_frontend.screens

import android.annotation.SuppressLint
import android.content.Context
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.echoplay_frontend.viewmodel.PlaylistViewModel
import com.example.echoplay_frontend.data.models.Song
import com.example.echoplay_frontend.R
import com.example.echoplay_frontend.data.services.MusicService

@SuppressLint("UnusedBoxWithConstraintsScope", "UseKtx")
@Composable
fun PlaylistScreen(navController: NavController, playlistViewModel: PlaylistViewModel = viewModel()) {

    val songs = playlistViewModel.songs
    val playlistName = playlistViewModel.playlistName
    val isLoading = playlistViewModel.isLoading
    val context = LocalContext.current
    var isShuffleMode by remember { mutableStateOf(MusicService.isShuffleMode) }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val gradient = Brush.linearGradient(
            colorStops = arrayOf(
                0.0f to Color(0xFF1595FF), // Azul claro
                0.25f to Color(0xFF4442FF), // Azul intenso
                0.5f to Color(0xFF8F12FF),  // Violeta brillante
                0.75f to Color(0xFF611FFE), // Violeta oscuro
                1.0f to Color(0xFF000519)   // Azul casi negro
            ),
            start = Offset(0f, 0f),
            end = Offset(maxWidth.value, maxHeight.value)
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(gradient)
                .padding(top = 60.dp, start = 40.dp, end = 40.dp)
        ) {
            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Color(0xFF8F12FF))
                }
            } else {
                Column(modifier = Modifier.fillMaxSize()) {
                    Text(
                        text = playlistName,
                        color = Color.White,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "${songs.size} canciones",
                            color = Color.LightGray,
                            fontSize = 14.sp,
                        )

                        Row {
                            IconButton(onClick = {
                                if (songs.isNotEmpty()) {
                                    isShuffleMode = !isShuffleMode
                                    MusicService.isShuffleMode = isShuffleMode
                                }
                            }) {
                                Icon(
                                    Icons.Filled.Shuffle,
                                    contentDescription = "Shuffle",
                                    tint = if (isShuffleMode) Color(0xFF000519) else Color.White
                                )
                            }
                            IconButton(onClick = {
                                if (songs.isNotEmpty()) {
                                    MusicService.playlist = songs
                                    MusicService.currentIndex = 0
                                    MusicService.isPlaylistMode = true
                                    navController.navigate("player")
                                }
                            }) {
                                Icon(Icons.Filled.PlayArrow, contentDescription = "Play", tint = Color.White)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    if (songs.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Ups! No hay canciones en esta playlist",
                                color = Color.White,
                                fontSize = 16.sp
                            )
                        }
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(songs) { song ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(60.dp)
                                        .background(Color(0xFF121212), RoundedCornerShape(8.dp))
                                        .clickable {
                                            val prefs = context.getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
                                            prefs.edit().putInt("songID", song.id).apply()
                                            MusicService.isPlaylistMode = false
                                            navController.navigate("player")
                                        }
                                        .padding(horizontal = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    AsyncImage(
                                        model = song.cover,
                                        contentDescription = "Carátula",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .size(50.dp)
                                            .background(Color.Gray, RoundedCornerShape(4.dp))
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = song.name,
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 16.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                        Text(song.artist, color = Color.LightGray, fontSize = 14.sp)
                                    }
                                    IconButton(
                                        onClick = {
                                            playlistViewModel.removeSongFromPlaylist(song.id)
                                        }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.Delete,
                                            contentDescription = "Eliminar canción",
                                            tint = Color.Red
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}