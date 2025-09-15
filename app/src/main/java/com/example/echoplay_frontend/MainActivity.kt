package com.example.echoplay_frontend

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import com.example.echoplay_frontend.data.services.MusicService
import com.example.echoplay_frontend.navigation.AppNavigation
import com.example.echoplay_frontend.ui.theme.EchoPlayfrontendTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val intent = Intent(this, MusicService::class.java)
        startService(intent)

        setContent {
            EchoPlayfrontendTheme {
                Surface (color = MaterialTheme.colorScheme.background) {
                    AppNavigation()
                }
            }
        }
    }
}