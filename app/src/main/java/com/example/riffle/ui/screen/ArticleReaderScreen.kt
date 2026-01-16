package com.example.riffle.ui.screen

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.size
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.runtime.DisposableEffect
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.text.HtmlCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.riffle.ui.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArticleReaderScreen(
    url: String,
    onBack: () -> Unit,
    viewModel: MainViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    // Fetch article directly from DB instead of UI state (which filters unread)
    val article by viewModel.getArticle(url).collectAsState(initial = null)

    // 2. OBTENEMOS LOS ESTADOS DE LA IA (LO NUEVO)
    val summary by viewModel.summaryState.collectAsState()
    val isSummarizing by viewModel.isSummarizing.collectAsState()

    // Limpiamos el resumen al salir de la pantalla
    DisposableEffect(Unit) {
        onDispose { viewModel.clearSummary() }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Article") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                        context.startActivity(intent)
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = "Open in Browser")
                    }
                }
            )
        },
        // 3. EL BOTÓN MÁGICO DE LA IA
        floatingActionButton = {
            if (article != null) { // Solo mostramos botón si hay artículo
                FloatingActionButton(
                    onClick = {
                        // Si ya hay resumen, lo borramos (toggle), si no, lo pedimos
                        if (summary != null) {
                            viewModel.clearSummary()
                        } else {
                            // Enviamos título y contenido (o descripción si contenido es nulo)
                            viewModel.summarizeArticle(
                                title = article!!.title,
                                content = article!!.description ?: "Sin contenido"
                            )
                        }
                    },
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ) {
                    if (isSummarizing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        // Cambia el icono si ya hay resumen o no
                        Icon(
                            imageVector = if (summary != null) Icons.Default.Close else Icons.Default.AutoAwesome,
                            contentDescription = "IA Resumen"
                        )
                    }
                }
            }
        }
    ) { padding ->
        if (article != null) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
            ) {
                // 4. LA TARJETA DEL RESUMEN (APARECE ARRIBA)
                AnimatedVisibility(visible = summary != null) {
                    Card(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(bottom = 16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer
                        )
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Resumen Inteligente",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            // Usamos MarkdownText si tienes librería, o Text normal
                            Text(
                                text = summary ?: "",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
                Text(
                    text = article!!.title,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = article!!.sourceUrl,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(16.dp))
                
                // Clean text from description (HTML to plain text)
                val cleanDescription = HtmlCompat.fromHtml(
                    article!!.description ?: "",
                    HtmlCompat.FROM_HTML_MODE_LEGACY
                ).toString().trim()

                Text(
                    text = cleanDescription,
                    style = MaterialTheme.typography.bodyLarge,
                    lineHeight = MaterialTheme.typography.bodyLarge.lineHeight * 1.2
                )
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text("Loading article content...")
            }
        }
    }
}
