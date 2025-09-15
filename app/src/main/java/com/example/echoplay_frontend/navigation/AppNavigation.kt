package com.example.echoplay_frontend.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.echoplay_frontend.screens.HomeScreen
import com.example.echoplay_frontend.screens.PlayerScreen
import com.example.echoplay_frontend.screens.PlaylistScreen
import com.example.echoplay_frontend.screens.UserSelectionScreen

@Composable
fun AppNavigation() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = AppScreens.UserSelectionScreen.route
    ) {
        composable(route = AppScreens.UserSelectionScreen.route) {
            UserSelectionScreen(navController)
        }
        composable(route = AppScreens.HomeScreen.route) {
            HomeScreen(navController)
        }
        composable(route = AppScreens.PlaylistScreen.route) {
            PlaylistScreen(navController)
        }
        composable(route = AppScreens.PlayerScreen.route) {
            PlayerScreen(navController)
        }
    }
}
