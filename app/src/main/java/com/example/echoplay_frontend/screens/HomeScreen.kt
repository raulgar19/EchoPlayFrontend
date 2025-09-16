package com.example.echoplay_frontend.screens

import HomeViewModel
import android.annotation.SuppressLint
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.List
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
                    0.0f to Color.Red,
                    0.3f to Color.Red,
                    1.0f to Color.Black
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
                CircularProgressIndicator(color = Color.White)
            }
        }
    } else {
        HomeContent(navController, homeViewModel)
    }
}

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter", "UnusedBoxWithConstraintsScope")
@Composable
private fun HomeContent(navController: NavController, homeViewModel: HomeViewModel) {
    var selectedItem by remember { mutableStateOf(0) }
    val items = listOf("Mis listas", "Buscar")
    val icons = listOf(Icons.Default.List, Icons.Default.Search)

    Scaffold(
        bottomBar = {
            NavigationBar(containerColor = Color(0xFF121212)) {
                items.forEachIndexed { index, item ->
                    NavigationBarItem(
                        selected = selectedItem == index,
                        onClick = { selectedItem = index },
                        label = { Text(item) },
                        icon = { Icon(icons[index], contentDescription = item) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color.Red,
                            selectedTextColor = Color.Red,
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
                    0.0f to Color.Red,
                    0.3f to Color.Red,
                    1.0f to Color.Black
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
                    1 -> SearchContent(
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

    val context = LocalContext.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        if (homeViewModel.playlists.isEmpty()) {
            // Mensaje cuando no hay playlists
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.98f), // lista más pequeña
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
                    .fillMaxHeight(0.6f), // lista más pequeña
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(homeViewModel.playlists) { playlist ->
                    Button(
                        onClick = {
                            val prefs = context.getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
                            prefs.edit()
                                .putInt("playlistID", playlist.id)
                                .putString("playlistName", playlist.name)
                                .apply()
                            navController.navigate("playlist")
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF121212)),
                        contentPadding = PaddingValues(0.dp),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(60.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                playlist.name,
                                color = Color.White,
                                fontSize = 16.sp
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Button(
            onClick = { showDialog = true },
            shape = CircleShape,
            colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
            modifier = Modifier
                .align(Alignment.End)
                .size(60.dp)
        ) {
            Text("+", color = Color.White, fontSize = 24.sp)
        }
    }

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
            modifier = Modifier.fillMaxHeight(0.98f) // lista más pequeña
        ) {
            items(filteredSongsList) { song ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp)
                        .background(Color(0xFF121212), RoundedCornerShape(8.dp))
                        .padding(8.dp)
                        .clickable {
                            val prefs = context.getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
                            prefs.edit().putInt("songID", song.id).apply()
                            navController.navigate("player")
                        },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AsyncImage(
                        model = song.cover,
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
            }
        }
    }
}