package com.example.echoplay_frontend.screens

import android.annotation.SuppressLint
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.echoplay_frontend.data.services.MusicService
import com.example.echoplay_frontend.viewmodels.PlayerViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerScreen(
    navController: NavController,
    playerViewModel: PlayerViewModel = viewModel()
) {
    val song = playerViewModel.song
    val isLoading = playerViewModel.isLoading
    val coroutineScope = rememberCoroutineScope()

    var sliderPosition by remember { mutableFloatStateOf(0f) }
    var isPlaying by remember { mutableStateOf(MusicService.isPlaying) }

    // Control del dialog de playlists
    var showPlaylistDialog by remember { mutableStateOf(false) }

    // Actualizar slider y estado de reproducción
    LaunchedEffect(true) {
        coroutineScope.launch {
            while (true) {
                MusicService.mediaPlayer?.let { mp ->
                    sliderPosition =
                        (mp.currentPosition.toFloat() / mp.duration.toFloat().coerceAtLeast(1f)) * 100f
                    isPlaying = MusicService.isPlaying
                }
                delay(500)
            }
        }
    }

    if (isLoading) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = Color.White)
        }
        return
    }

    song?.let { s ->

        LaunchedEffect(s) {
            playerViewModel.playSong()
        }

        // Dialog de playlists
        if (showPlaylistDialog) {
            AlertDialog(
                onDismissRequest = { showPlaylistDialog = false },
                containerColor = Color(0xFF121212), // Fondo negro oscuro
                titleContentColor = Color.White,
                textContentColor = Color.White,
                shape = RoundedCornerShape(16.dp), // Bordes redondeados
                title = {
                    Text(
                        text = "Selecciona una Playlist",
                        fontSize = 20.sp,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                    )
                },
                text = {
                    Column(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        playerViewModel.playlists.forEach { playlist ->
                            TextButton(
                                onClick = {
                                    playerViewModel.addToPlaylist(s, playlist.id)
                                    showPlaylistDialog = false
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .background(Color(0xFF1F1F1F), RoundedCornerShape(8.dp)) // Fondo de cada opción
                            ) {
                                Text(
                                    text = playlist.name,
                                    color = Color.White,
                                    fontSize = 16.sp
                                )
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = { showPlaylistDialog = false },
                        colors = ButtonDefaults.textButtonColors(contentColor = Color.Red)
                    ) {
                        Text("Cancelar", fontSize = 16.sp)
                    }
                }
            )
        }

        Box(modifier = Modifier.fillMaxSize()) {

            // Fondo con la carátula
            AsyncImage(
                model = s.cover,
                contentDescription = "Background Cover",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            // Overlay negro difuminado
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.7f),
                                Color.Black.copy(alpha = 1f)
                            )
                        )
                    )
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 60.dp, start = 40.dp, end = 40.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(80.dp))

                AsyncImage(
                    model = s.cover,
                    contentDescription = "Carátula de la canción",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(350.dp)
                        .clip(RoundedCornerShape(16.dp))
                )

                Spacer(modifier = Modifier.height(80.dp))

                // Fila con nombre, artista y botón de añadir a playlist
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(end = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(
                        horizontalAlignment = Alignment.Start,
                        modifier = Modifier.weight(1f) // 🔹 Para que respete el espacio disponible
                    ) {
                        Text(
                            text = s.name,
                            color = Color.White,
                            fontSize = 24.sp,
                            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                            maxLines = 1, // 🔹 Máximo una línea
                            overflow = TextOverflow.Ellipsis, // 🔹 Ocultar lo que se pase con ...
                            modifier = Modifier.fillMaxWidth(0.9f) // 🔹 Ajusta el ancho disponible
                        )
                        Text(
                            text = s.artist,
                            color = Color.LightGray,
                            fontSize = 16.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    IconButton(
                        onClick = { showPlaylistDialog = true }
                    ) {
                        Icon(
                            imageVector = Icons.Filled.PlaylistAdd,
                            contentDescription = "Añadir a lista",
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Barra de progreso
                Slider(
                    value = sliderPosition,
                    onValueChange = {
                        sliderPosition = it
                        MusicService.mediaPlayer?.let { mp ->
                            val seekPos = (it / 100f * mp.duration).toInt()
                            mp.seekTo(seekPos)
                        }
                    },
                    valueRange = 0f..100f,
                    colors = SliderDefaults.colors(
                        thumbColor = Color.Red,
                        activeTrackColor = Color.Red,
                        inactiveTrackColor = Color.DarkGray
                    ),
                    modifier = Modifier.height(40.dp)
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Controles de reproducción
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    IconButton(onClick = { }) {

                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(32.dp)
                    ) {
                        IconButton(onClick = {
                            if (MusicService.isPlaylistMode) {
                                playerViewModel.playPrevious()
                            }
                        }) {
                            Icon(
                                imageVector = Icons.Filled.SkipPrevious,
                                contentDescription = "Anterior",
                                tint = Color.White,
                                modifier = Modifier.size(50.dp)
                            )
                        }

                        IconButton(
                            onClick = {
                                if (MusicService.isPlaying) playerViewModel.pauseSong()
                                else playerViewModel.playSong()
                                isPlaying = MusicService.isPlaying
                            },
                            modifier = Modifier
                                .size(50.dp)
                                .background(Color.White, CircleShape)
                        ) {
                            Icon(
                                imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                contentDescription = "Play/Pause",
                                tint = Color.Red,
                                modifier = Modifier.size(30.dp)
                            )
                        }

                        IconButton(onClick = {
                            if (MusicService.isPlaylistMode) {
                                playerViewModel.playNext()
                            }
                        }) {
                            Icon(
                                imageVector = Icons.Filled.SkipNext,
                                contentDescription = "Siguiente",
                                tint = Color.White,
                                modifier = Modifier.size(50.dp)
                            )
                        }
                    }

                    // 🔹 Botón Loop
                    IconButton(onClick = { playerViewModel.toggleLooping() }) {
                        Icon(
                            imageVector = Icons.Filled.Repeat,
                            contentDescription = "Repetir",
                            tint = if (playerViewModel.isLooping) Color.Red else Color.White,
                            modifier = Modifier.size(25.dp)
                        )
                    }
                }
            }
        }
    }
}