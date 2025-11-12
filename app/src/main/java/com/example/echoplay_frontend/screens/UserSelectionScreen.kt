package com.example.echoplay_frontend.screens

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.echoplay_frontend.viewmodel.UserSelectionViewModel
import com.example.echoplay_frontend.utils.convertGoogleDriveUrl

@SuppressLint("UnusedBoxWithConstraintsScope")
@Composable
fun UserSelectionScreen(
    navController: androidx.navigation.NavController,
    userSelectionViewModel: UserSelectionViewModel = viewModel()
) {
    val users = userSelectionViewModel.users.value
    val isLoading = userSelectionViewModel.isLoading.value
    val context = LocalContext.current

    @SuppressLint("UseKtx")
    fun saveUserId(context: Context, userId: Int) {
        val sharedPreferences: SharedPreferences =
            context.getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
        sharedPreferences.edit().putInt("userID", userId).apply()
    }

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
            Column {
                Text(
                    text = "Selecciona un perfil",
                    color = Color.White,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.align(Alignment.Start)
                )

                Spacer(modifier = Modifier.height(20.dp))

                if (isLoading) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = Color(0xFF8F12FF))
                    }
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(users) { user ->
                            Box(
                                modifier = Modifier
                                    .width(120.dp)
                                    .height(60.dp)
                                    .background(Color(0xFF121212), RoundedCornerShape(8.dp))
                                    .clickable {
                                        saveUserId(context, user.id)
                                        navController.navigate("home")
                                    },
                                contentAlignment = Alignment.CenterStart
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    AsyncImage(
                                        model = convertGoogleDriveUrl(user.image),
                                        contentDescription = "Imagen perfil",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .size(60.dp)
                                            .clip(
                                                RoundedCornerShape(
                                                    topStart = 8.dp,
                                                    bottomStart = 8.dp,
                                                    topEnd = 0.dp,
                                                    bottomEnd = 0.dp
                                                )
                                            )
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = user.name,
                                        color = Color.White,
                                        fontSize = 16.sp
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