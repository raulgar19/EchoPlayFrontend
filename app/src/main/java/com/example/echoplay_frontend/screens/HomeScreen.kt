package com.example.echoplay_frontend.screens

import HomeViewModel
import android.annotation.SuppressLint
import android.content.Context
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.LibraryAdd
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.echoplay_frontend.data.services.MusicService
import com.example.echoplay_frontend.utils.convertGoogleDriveUrl

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter", "UnusedBoxWithConstraintsScope")
@Composable
fun HomeScreen(navController: NavController, homeViewModel: HomeViewModel = viewModel()) {
    val isLoading = homeViewModel.isLoading

    if (isLoading) {
        // Loader con fondo degradado original
        BoxWithConstraints(
            modifier = Modifier.fillMaxSize()
        ) { 
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
                    .background(gradient),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = Color(0xFF8F12FF))
            }
        }
    } else {
        HomeContent(navController, homeViewModel)
    }
}

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter", "UnusedBoxWithConstraintsScope")
@Composable
private fun HomeContent(navController: NavController, homeViewModel: HomeViewModel) {
    val selectedItem = homeViewModel.selectedTab
    val items = listOf("Mis listas", "MixLab", "Buscar")
    val icons = listOf(Icons.Filled.LibraryMusic, Icons.Filled.Science, Icons.Default.Search)

    Scaffold(
        bottomBar = {
            NavigationBar(containerColor = Color(0xFF000519)) { // fondo acorde al gradiente
                items.forEachIndexed { index, item ->
                    NavigationBarItem(
                        selected = selectedItem == index,
                        onClick = { homeViewModel.updateSelectedTab(index) },
                        label = { Text(item) },
                        icon = { Icon(icons[index], contentDescription = item) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color(0xFF8F12FF),   // violeta brillante
                            selectedTextColor = Color(0xFF8F12FF),
                            unselectedIconColor = Color.White,
                            unselectedTextColor = Color.White,
                            indicatorColor = Color.Transparent
                        )
                    )
                }
            }
        }
    ) { innerPadding ->
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
                Text(
                    text = "Bienvenid@ de nuevo",
                    color = Color.White,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.align(Alignment.TopStart)
                )

                when (selectedItem) {
                    0 -> MisListasContent(
                        navController = navController,
                        homeViewModel = homeViewModel,
                        modifier = Modifier
                            .padding(top = 80.dp)
                            .padding(innerPadding)
                    )
                    1 -> MixLabContent(
                        navController = navController,
                        homeViewModel = homeViewModel,
                        modifier = Modifier
                            .padding(top = 80.dp)
                            .padding(innerPadding)
                    )
                    2 -> SearchContent(
                        navController = navController,
                        homeViewModel = homeViewModel,
                        modifier = Modifier
                            .padding(top = 80.dp)
                            .padding(innerPadding)
                    )
                }
            }
        }
    }
}

@Composable
fun MisListasContent(
    navController: NavController,
    homeViewModel: HomeViewModel,
    modifier: Modifier = Modifier
) {
    var showDialog by remember { mutableStateOf(false) }
    var newListName by remember { mutableStateOf("") }

    var showDeleteDialog by remember { mutableStateOf(false) }
    var playlistToDelete by remember { mutableStateOf<Int?>(null) }

    val context = LocalContext.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        if (homeViewModel.playlists.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.8f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Ups! Todavía no tienes playlists",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.8f),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(homeViewModel.playlists) { playlist ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(60.dp)
                            .background(Color(0xFF121212), RoundedCornerShape(8.dp))
                            .padding(horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            playlist.name,
                            color = Color.White,
                            fontSize = 16.sp,
                            modifier = Modifier
                                .weight(1f)
                                .clickable {
                                    val prefs = context.getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
                                    prefs.edit()
                                        .putInt("playlistID", playlist.id)
                                        .putString("playlistName", playlist.name)
                                        .apply()
                                    navController.navigate("playlist")
                                }
                        )

                        IconButton(
                            onClick = {
                                playlistToDelete = playlist.id
                                showDeleteDialog = true
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Delete,
                                contentDescription = "Eliminar playlist",
                                tint = Color.Red
                            )
                        }
                    }
                }
            }
        }

        // 🔹 Parte inferior: Botón + indicador si hay música
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (MusicService.isPlaying) {
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .height(60.dp)
                        .clickable {
                            MusicService.isReturningFromPlayerButton = true
                            navController.navigate("player")
                        },
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)),
                    elevation = CardDefaults.cardElevation(6.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Ir al reproductor",
                            tint = Color(0xFF8F12FF),
                            modifier = Modifier.size(26.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Volver al reproductor",
                            color = Color.White,
                            fontWeight = FontWeight.Medium,
                            fontSize = 15.sp
                        )
                    }
                }
            } else {
                Spacer(modifier = Modifier.weight(1f))
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Botón para añadir playlist
            Button(
                onClick = { showDialog = true },
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8F12FF)),
                modifier = Modifier.size(60.dp)
            ) {
                Text("+", color = Color.White, fontSize = 24.sp)
            }
        }
    }

    // 🔹 Diálogo para añadir playlist (sin cambios)
    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            confirmButton = {
                Button(
                    onClick = {
                        if (newListName.isNotBlank()) {
                            homeViewModel.addPlaylist(newListName)
                            newListName = ""
                            showDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                ) {
                    Text("Añadir", color = Color.White)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showDialog = false },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Red)
                ) {
                    Text("Cancelar")
                }
            },
            title = { Text("Nueva lista") },
            text = {
                Column {
                    Text("Introduce el nombre de la nueva lista:")
                    Spacer(modifier = Modifier.height(8.dp))
                    TextField(
                        value = newListName,
                        onValueChange = { newListName = it },
                        singleLine = true,
                        placeholder = { Text("Ej: Chill, Fiesta...") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    )
                }
            },
            containerColor = Color(0xFF1E1E1E),
            titleContentColor = Color.White,
            textContentColor = Color.White
        )
    }

    // 🔹 Diálogo eliminar playlist (sin cambios)
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            confirmButton = {
                Button(
                    onClick = {
                        playlistToDelete?.let { id ->
                            homeViewModel.deletePlaylist(id)
                        }
                        showDeleteDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                ) {
                    Text("Eliminar", color = Color.White)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showDeleteDialog = false },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Red)
                ) {
                    Text("Cancelar")
                }
            },
            title = { Text("Eliminar playlist") },
            text = { Text("¿Estás seguro de que deseas eliminar esta playlist?") },
            containerColor = Color(0xFF1E1E1E),
            titleContentColor = Color.White,
            textContentColor = Color.White
        )
    }
}

@Composable
fun SearchContent(
    homeViewModel: HomeViewModel,
    navController: NavController,
    modifier: Modifier = Modifier
) {
    val filteredSongsList by remember(homeViewModel.songs, homeViewModel.searchQuery) {
        derivedStateOf {
            val query = homeViewModel.searchQuery.trim().lowercase()
            if (query.isEmpty()) homeViewModel.songs
            else homeViewModel.songs.filter {
                it.name.lowercase().contains(query) || it.artist.lowercase().contains(query)
            }
        }
    }

    val context = LocalContext.current
    var showPlaylistDialog by remember { mutableStateOf(false) }
    var songToAdd by remember { mutableStateOf<Int?>(null) }

    Column(modifier = modifier.fillMaxSize()) {
        TextField(
            value = homeViewModel.searchQuery,
            onValueChange = { homeViewModel.updateSearch(it) },
            placeholder = { Text("Buscar canción...") },
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            shape = RoundedCornerShape(8.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f, fill = true)
        ) {
            items(filteredSongsList) { song ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp)
                        .background(Color(0xFF121212), RoundedCornerShape(8.dp))
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .weight(1f)
                            .clickable {
                                val prefs = context.getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
                                prefs.edit().putInt("songID", song.id).apply()
                                MusicService.isPlaylistMode = false
                                MusicService.isFusionMixMode = false // ✅ Desactivar Fusion Mix
                                navController.navigate("player")
                            }
                    ) {
                        AsyncImage(
                            model = convertGoogleDriveUrl(song.cover),
                            contentDescription = "Cover",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(50.dp)
                                .background(Color.Gray, RoundedCornerShape(4.dp))
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
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
                    }

                    IconButton(onClick = {
                        songToAdd = song.id
                        showPlaylistDialog = true
                    }) {
                        Icon(
                            imageVector = Icons.Filled.LibraryAdd,
                            contentDescription = "Añadir a playlist",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        }

        // 🔹 Indicador debajo de la lista si hay música reproduciéndose
        if (MusicService.isPlaying) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp)
                    .clickable {
                        MusicService.isReturningFromPlayerButton = true
                        navController.navigate("player")
                    },
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)),
                elevation = CardDefaults.cardElevation(6.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 14.dp, horizontal = 20.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Ir al reproductor",
                        tint = Color(0xFF8F12FF),
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Volver al reproductor",
                        color = Color.White,
                        fontWeight = FontWeight.Medium,
                        fontSize = 16.sp
                    )
                }
            }
        }
    }

    // 🔹 Diálogo para añadir canción a playlist (sin cambios)
    if (showPlaylistDialog) {
        AlertDialog(
            onDismissRequest = { showPlaylistDialog = false },
            containerColor = Color(0xFF121212),
            titleContentColor = Color.White,
            textContentColor = Color.White,
            shape = RoundedCornerShape(16.dp),
            title = { Text("Selecciona una Playlist", fontSize = 20.sp, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    homeViewModel.playlists.forEach { playlist ->
                        TextButton(
                            onClick = {
                                songToAdd?.let { id ->
                                    homeViewModel.addSongToPlaylist(id, playlist.id)
                                }
                                showPlaylistDialog = false
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .background(Color(0xFF1F1F1F), RoundedCornerShape(8.dp))
                        ) {
                            Text(playlist.name, color = Color.White, fontSize = 16.sp)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { showPlaylistDialog = false },
                    colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFF8F12FF))
                ) {
                    Text("Cancelar", fontSize = 16.sp)
                }
            }
        )
    }
}

@Composable
fun MixLabContent(
    navController: NavController,
    homeViewModel: HomeViewModel,
    modifier: Modifier = Modifier
) {
    val playlists = homeViewModel.playlists
    var selectedPlaylists by remember { mutableStateOf(setOf<Int>()) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Text(
            text = "Selecciona las playlists que quieras",
            color = Color.White.copy(alpha = 0.7f),
            fontSize = 16.sp,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        if (playlists.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.8f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Ups! Todavía no tienes playlists",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        } else {
            // Lista de playlists
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(playlists) { playlist ->
                    val isSelected = playlist.id in selectedPlaylists
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                if (isSelected) Color(0xFF8F12FF).copy(alpha = 0.3f) else Color(0xFF1F1F1F),
                                RoundedCornerShape(12.dp)
                            )
                            .clickable {
                                selectedPlaylists = if (isSelected) {
                                    selectedPlaylists - playlist.id
                                } else {
                                    selectedPlaylists + playlist.id
                                }
                            }
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = playlist.name,
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f)
                        )
                        
                        // Checkmark icon
                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Seleccionada",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Área de playlists seleccionadas
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF1F1F1F), RoundedCornerShape(12.dp))
                    .padding(20.dp)
            ) {
                Text(
                    text = "${selectedPlaylists.size} playlist(s) seleccionada(s)",
                    color = Color(0xFF8F12FF),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Text(
                    text = "Se combinarán y mezclarán aleatoriamente",
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 14.sp
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Botón de reproducir
            Button(
                onClick = {
                    if (selectedPlaylists.isNotEmpty()) {
                        homeViewModel.createAndPlayFusionMix(selectedPlaylists.toList()) {
                            navController.navigate("player")
                        }
                    }
                },
                enabled = selectedPlaylists.size >= 2,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF8F12FF),
                    disabledContainerColor = Color(0xFF3F3F3F)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "Play",
                    tint = Color.White
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (selectedPlaylists.size < 2) {
                        "Selecciona al menos 2 playlists"
                    } else {
                        "Reproducir MixLab"
                    },
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}