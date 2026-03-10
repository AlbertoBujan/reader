package com.boaxente.riffle.util

import com.google.firebase.crashlytics.FirebaseCrashlytics
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jsoup.Jsoup
import java.net.URL
import javax.inject.Inject

class FeedDiscovery @Inject constructor(
    private val client: OkHttpClient
) {
    private val crashlytics: FirebaseCrashlytics
        get() = FirebaseCrashlytics.getInstance()

    suspend fun discoverFeeds(urlInput: String): List<String> = withContext(Dispatchers.IO) {
        val foundFeeds = mutableListOf<String>()
        var cleanedUrl = ""

        try {
            // 1. Limpieza de URL
            cleanedUrl = cleanUrl(urlInput)

            // 2. Extracción por HTML (Paso principal)
            val request = Request.Builder()
                .url(cleanedUrl)
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36")
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val html = response.body?.string() ?: ""
                    
                    // Jsoup parse con la URL base para futuras resoluciones de rutas relativas
                    val document = Jsoup.parse(html, cleanedUrl)

                    // Buscar etiquetas <link> con rel="alternate" en el <head>
                    val linkElements = document.head().select("link[rel=alternate]")
                    val validTypes = listOf("application/rss+xml", "application/atom+xml", "application/json")

                    for (link in linkElements) {
                        val type = link.attr("type").lowercase()
                        if (validTypes.any { type.contains(it) }) {
                            // 3. Resolución de URLs (ruta absoluta usando absUrl de Jsoup)
                            val href = link.absUrl("href")
                            if (href.isNotBlank()) {
                                foundFeeds.add(href)
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            // 5. Manejo de errores silencioso
            crashlytics.recordException(e)
        }

        // 4. Fuerza bruta (Plan B)
        if (foundFeeds.isEmpty() && cleanedUrl.isNotEmpty()) {
            val baseUri = extractBaseUrl(cleanedUrl)
            val bruteForcePaths = listOf("/feed", "/rss", "/rss.xml", "/atom.xml", "/feed.xml")

            for (path in bruteForcePaths) {
                val fallbackUrl = if (baseUri.endsWith("/")) {
                    baseUri.dropLast(1) + path
                } else {
                    baseUri + path
                }

                try {
                    val fallbackRequest = Request.Builder()
                        .url(fallbackUrl)
                        .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36")
                        .build()

                    client.newCall(fallbackRequest).execute().use { fallbackResponse ->
                        // Verificar que devuelva un código HTTP 200 válido (isSuccessful cubre de 200 a 299)
                        if (fallbackResponse.isSuccessful) {
                            val contentType = fallbackResponse.header("Content-Type")?.lowercase() ?: ""
                            // Chequear Content-Type
                            if (contentType.contains("xml") || contentType.contains("json")) {
                                foundFeeds.add(fallbackUrl)
                            }
                        }
                    }
                } catch (e: Exception) {
                    // Silencioso. Se omiten errores individuales en la fuerza bruta
                    // Podríamos registrar también en Crashlytics cada fallo si fallan mucho, 
                    // pero es normal que muchas URLs den 404.
                }
            }
        }

        return@withContext foundFeeds.distinct()
    }

    private fun cleanUrl(input: String): String {
        val trimmed = input.trim()
        return if (!trimmed.startsWith("http://") && !trimmed.startsWith("https://")) {
            "https://$trimmed"
        } else {
            trimmed
        }
    }

    private fun extractBaseUrl(url: String): String {
        return try {
            val parsed = URL(url)
            val portStr = if (parsed.port != -1) ":${parsed.port}" else ""
            "${parsed.protocol}://${parsed.host}$portStr"
        } catch (e: Exception) {
            url
        }
    }
}
