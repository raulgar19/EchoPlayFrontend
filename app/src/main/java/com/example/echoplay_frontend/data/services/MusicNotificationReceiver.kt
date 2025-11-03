package com.example.echoplay_frontend.data.services

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class MusicNotificationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        context ?: return
        
        when (intent?.action) {
            "com.example.echoplay_frontend.PLAY_PAUSE" -> {
                val service = Intent(context, MusicService::class.java).apply {
                    action = MusicService.ACTION_PLAY_PAUSE
                }
                context.startService(service)
            }
            "com.example.echoplay_frontend.NEXT" -> {
                val service = Intent(context, MusicService::class.java).apply {
                    action = MusicService.ACTION_NEXT
                }
                context.startService(service)
            }
            "com.example.echoplay_frontend.PREVIOUS" -> {
                val service = Intent(context, MusicService::class.java).apply {
                    action = MusicService.ACTION_PREVIOUS
                }
                context.startService(service)
            }
        }
    }
}
