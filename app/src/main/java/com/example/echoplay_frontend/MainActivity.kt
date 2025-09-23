package com.example.echoplay_frontend

import android.annotation.SuppressLint
import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.core.content.FileProvider
import com.example.echoplay_frontend.data.models.Version
import com.example.echoplay_frontend.data.models.VersionResponse
import com.example.echoplay_frontend.data.network.RetrofitInstance
import com.example.echoplay_frontend.data.services.MusicService
import com.example.echoplay_frontend.navigation.AppNavigation
import com.example.echoplay_frontend.ui.theme.EchoPlayfrontendTheme
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.File

class MainActivity : ComponentActivity() {

    private var updateAvailable by mutableStateOf<String?>(null)   // diálogo inicial
    private var updateResult by mutableStateOf<String?>(null)      // diálogo final
    private var apkUrl: String? = null                             // URL del APK

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val intent = Intent(this, MusicService::class.java)
        startService(intent)
        checkForUpdate()

        setContent {
            EchoPlayfrontendTheme {
                Surface(color = MaterialTheme.colorScheme.background) {
                    AppNavigation()

                    updateAvailable?.let { version ->
                        AlertDialog(
                            onDismissRequest = { },
                            title = { Text("Nueva versión disponible") },
                            text = { Text("Se ha detectado la versión $version de EchoPlay. Debes actualizar para continuar.") },
                            confirmButton = {
                                TextButton(onClick = {
                                    downloadAndInstall()
                                    updateAvailable = null
                                }) {
                                    Text("Actualizar")
                                }
                            }
                        )
                    }

                    updateResult?.let { result ->
                        AlertDialog(
                            onDismissRequest = { updateResult = null },
                            title = { Text("Resultado de la actualización") },
                            text = { Text(result) },
                            confirmButton = {
                                TextButton(onClick = { updateResult = null }) {
                                    Text("OK")
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    private fun checkForUpdate() {
        RetrofitInstance.api.getLatestVersion().enqueue(object : Callback<VersionResponse> {
            override fun onResponse(
                call: Call<VersionResponse>,
                response: Response<VersionResponse>
            ) {
                if (response.isSuccessful) {
                    val latest = response.body()
                    if (latest != null && isVersionNewer(latest.latest_version, Version.CURRENT)) {
                        updateAvailable = latest.latest_version
                        apkUrl = latest.apk_url
                    }
                }
            }

            override fun onFailure(call: Call<VersionResponse>, t: Throwable) {
                updateResult = "Error al verificar versión: ${t.message}"
            }
        })
    }

    // 🔹 Función que compara versiones semánticas
    private fun isVersionNewer(latest: String, current: String): Boolean {
        val latestParts = latest.split(".").map { it.toIntOrNull() ?: 0 }
        val currentParts = current.split(".").map { it.toIntOrNull() ?: 0 }
        val maxLength = maxOf(latestParts.size, currentParts.size)

        for (i in 0 until maxLength) {
            val l = latestParts.getOrElse(i) { 0 }
            val c = currentParts.getOrElse(i) { 0 }
            if (l > c) return true
            if (l < c) return false
        }
        return false
    }

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    private fun downloadAndInstall() {
        val url = apkUrl ?: return
        val request = DownloadManager.Request(Uri.parse(url))
            .setTitle("Actualización EchoPlay")
            .setDescription("Descargando nueva versión...")
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationInExternalFilesDir(
                this,
                Environment.DIRECTORY_DOWNLOADS,
                "echoplay-latest.apk"
            )

        val manager = getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        manager.enqueue(request)

        val onComplete = object : BroadcastReceiver() {
            @SuppressLint("UnspecifiedRegisterReceiverFlag")
            override fun onReceive(ctxt: Context?, intent: Intent?) {
                val file = File(
                    getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS),
                    "echoplay-latest.apk"
                )
                val uri = FileProvider.getUriForFile(
                    applicationContext,
                    "${packageName}.provider",
                    file
                )

                val installIntent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(uri, "application/vnd.android.package-archive")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                startActivity(installIntent)

                val deleted = file.delete()
                updateResult = if (deleted) {
                    "La actualización se instaló correctamente."
                } else {
                    "La actualización se instaló, pero no se pudo borrar el archivo."
                }

                unregisterReceiver(this)
            }
        }

        registerReceiver(onComplete, IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE))
    }
}