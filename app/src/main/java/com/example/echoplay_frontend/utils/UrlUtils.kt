package com.example.echoplay_frontend.utils

/**
 * Convierte enlaces de Google Drive al formato de descarga directa
 * Formatos soportados:
 * - https://drive.google.com/file/d/FILE_ID/view?usp=sharing
 * - https://drive.google.com/open?id=FILE_ID
 * - https://drive.google.com/uc?id=FILE_ID
 */
fun convertGoogleDriveUrl(url: String): String {
    if (url.isEmpty()) return url
    
    // Si ya es una URL de descarga directa, devolverla sin cambios
    if (url.contains("drive.google.com/uc?") && url.contains("export=")) {
        return url
    }
    
    // Extraer el ID del archivo de diferentes formatos de URL de Google Drive
    val fileIdRegex = """(?:id=|/d/|/file/d/)([a-zA-Z0-9_-]+)""".toRegex()
    val match = fileIdRegex.find(url)
    
    return if (match != null) {
        val fileId = match.groupValues[1]
        // Convertir a URL de descarga directa
        "https://drive.google.com/uc?export=view&id=$fileId"
    } else {
        url // Si no es un enlace de Google Drive, devolver original
    }
}
