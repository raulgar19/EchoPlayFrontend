package com.example.echoplay_frontend

import android.annotation.SuppressLint
import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresApi
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

        // ✅ Ya NO iniciamos el servicio aquí - se iniciará cuando el usuario presione Play
        checkForUpdate()

        setContent {
            EchoPlayfrontendTheme {
                Surface(color = MaterialTheme.colorScheme.background) {
                    AppNavigation()

                    updateAvailable?.let { version ->
                        AlertDialog(
                            onDismissRequest = { },
                            title = { Text("🎵 Nueva versión disponible") },
                            text = { Text("Se ha detectado la versión $version de EchoPlay.\n\nActualiza ahora para disfrutar de las últimas mejoras y correcciones.") },
                            confirmButton = {
                                TextButton(onClick = {
                                    downloadAndInstall()
                                    updateAvailable = null
                                }) {
                                    Text("Actualizar ahora")
                                }
                            },
                            dismissButton = {
                                TextButton(onClick = {
                                    updateAvailable = null
                                }) {
                                    Text("Más tarde")
                                }
                            }
                        )
                    }

                    updateResult?.let { result ->
                        AlertDialog(
                            onDismissRequest = { updateResult = null },
                            title = { 
                                Text(
                                    if (result.contains("Error", ignoreCase = true)) "❌ Error" 
                                    else if (result.contains("Instalando", ignoreCase = true)) "⏳ Actualizando"
                                    else "✅ Actualización"
                                ) 
                            },
                            text = { Text(result) },
                            confirmButton = {
                                TextButton(onClick = { updateResult = null }) {
                                    Text("Entendido")
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
                        apkUrl = latest.url
                    }
                } else {
                    // Silenciar error de verificación para no molestar al usuario
                    println("⚠️ No se pudo verificar actualizaciones: ${response.code()}")
                }
            }

            override fun onFailure(call: Call<VersionResponse>, t: Throwable) {
                // Silenciar error de red para no molestar al usuario
                println("⚠️ Error de red al verificar actualizaciones: ${t.message}")
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
        val url = apkUrl ?: run {
            updateResult = "Error: No se encontró la URL de descarga"
            return
        }

        // ✅ Convertir URL de Google Drive a descarga directa
        val downloadUrl = if (url.contains("drive.google.com")) {
            // Extraer el ID del archivo de Google Drive
            val fileId = when {
                url.contains("id=") -> url.substringAfter("id=").substringBefore("&")
                url.contains("/d/") -> url.substringAfter("/d/").substringBefore("/")
                else -> {
                    updateResult = "Error: URL de Google Drive inválida"
                    return
                }
            }
            // Usar el endpoint directo de Google Drive que bypasea la página de confirmación
            "https://drive.usercontent.google.com/download?id=$fileId&export=download&confirm=t"
        } else {
            url
        }

        // ✅ Verificar permisos de instalación antes de descargar (Android 8.0+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (!packageManager.canRequestPackageInstalls()) {
                val intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                    data = Uri.parse("package:$packageName")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                startActivity(intent)
                updateResult = "Debes habilitar 'Permitir desde esta fuente' en Ajustes para instalar actualizaciones."
                return
            }
        }

        val request = DownloadManager.Request(Uri.parse(downloadUrl))
            .setTitle("Actualización EchoPlay")
            .setDescription("Descargando nueva versión...")
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationInExternalFilesDir(
                this,
                Environment.DIRECTORY_DOWNLOADS,
                "echoplay-latest.apk"
            )
            .setMimeType("application/vnd.android.package-archive")
            .setAllowedOverMetered(true)
            .setAllowedOverRoaming(true)

        val manager = getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val downloadId = manager.enqueue(request)

        val onComplete = object : BroadcastReceiver() {
            override fun onReceive(ctxt: Context?, intent: Intent?) {
                val id = intent?.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
                if (id != downloadId) return

                val file = File(
                    getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS),
                    "echoplay-latest.apk"
                )

                if (!file.exists()) {
                    updateResult = "Error: No se pudo descargar el archivo de actualización"
                    unregisterReceiver(this)
                    return
                }

                try {
                    val uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        // Android 7.0+ requiere FileProvider
                        FileProvider.getUriForFile(
                            applicationContext,
                            "${packageName}.provider",
                            file
                        )
                    } else {
                        Uri.fromFile(file)
                    }

                    val installIntent = Intent(Intent.ACTION_VIEW).apply {
                        setDataAndType(uri, "application/vnd.android.package-archive")
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                            addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                        }
                    }

                    startActivity(installIntent)
                    updateResult = "Instalando actualización..."

                    // ✅ Limpiar el archivo después de un delay
                    android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                        try {
                            if (file.exists()) file.delete()
                        } catch (e: Exception) {
                            println("⚠️ No se pudo eliminar el APK: ${e.message}")
                        }
                    }, 3000)

                } catch (e: Exception) {
                    e.printStackTrace()
                    updateResult = "Error al instalar: ${e.localizedMessage ?: e.message}"
                } finally {
                    try {
                        unregisterReceiver(this)
                    } catch (e: Exception) {
                        // Ignorar si ya fue desregistrado
                    }
                }
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(onComplete, IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE), RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(onComplete, IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE))
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Detener el servicio siempre al cerrar la app
        val intent = Intent(this, MusicService::class.java)
        stopService(intent)
    }
}