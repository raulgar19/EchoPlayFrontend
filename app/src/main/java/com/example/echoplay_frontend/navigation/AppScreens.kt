package com.example.echoplay_frontend.navigation

sealed class AppScreens(val route: String) {
    object UserSelectionScreen : AppScreens("userSelection")
    object HomeScreen : AppScreens("home")
    object PlaylistScreen : AppScreens("playlist")
    object PlayerScreen : AppScreens("player")

}
