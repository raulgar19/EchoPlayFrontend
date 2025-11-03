package com.example.echoplay_frontend

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.animation.AnimationUtils
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.ComponentActivity
import com.example.echoplay_frontend.data.models.Version

@SuppressLint("CustomSplashScreen")
class SplashActivity : ComponentActivity() {
    
    private val splashTimeOut: Long = 2500 // 2.5 segundos

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Configurar el layout de la splash screen
        setContentView(R.layout.activity_splash)

        // Configurar la versión desde el modelo Version
        val versionText = findViewById<TextView>(R.id.version_text)
        versionText.text = "v${Version.CURRENT}"

        // Añadir animaciones
        startAnimations()

        // Navegar a MainActivity después del tiempo definido
        Handler(Looper.getMainLooper()).postDelayed({
            startActivity(Intent(this, MainActivity::class.java))
            finish() // Cerrar SplashActivity para que no vuelva al presionar back
        }, splashTimeOut)
    }

    private fun startAnimations() {
        // Animación de fade in para el ícono
        val appIcon = findViewById<ImageView>(R.id.app_icon)
        val fadeIn = AnimationUtils.loadAnimation(this, android.R.anim.fade_in)
        fadeIn.duration = 1000
        appIcon.startAnimation(fadeIn)

        // Animación de slide up para el nombre
        val appName = findViewById<TextView>(R.id.app_name)
        val slideUp = AnimationUtils.loadAnimation(this, android.R.anim.slide_in_left)
        slideUp.duration = 800
        slideUp.startOffset = 300
        appName.startAnimation(slideUp)

        // Animación para el subtítulo
        val appSubtitle = findViewById<TextView>(R.id.app_subtitle)
        val fadeInSlow = AnimationUtils.loadAnimation(this, android.R.anim.fade_in)
        fadeInSlow.duration = 1000
        fadeInSlow.startOffset = 600
        appSubtitle.startAnimation(fadeInSlow)
    }
}
