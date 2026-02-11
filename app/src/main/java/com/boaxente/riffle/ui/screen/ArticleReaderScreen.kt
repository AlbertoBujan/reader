package com.boaxente.riffle.ui.screen


import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import android.speech.tts.TextToSpeech
import java.util.Locale
import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.IntSize
import android.net.Uri
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Badge
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.size
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.runtime.DisposableEffect
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
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
import com.boaxente.riffle.ui.viewmodel.MainViewModel
import com.boaxente.riffle.R
import androidx.compose.ui.res.stringResource
import com.boaxente.riffle.util.extractFirstImageUrl
import coil.compose.AsyncImage
import androidx.compose.ui.layout.ContentScale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArticleReaderScreen(
    url: String,
    onBack: () -> Unit,
    onCommentsClick: (articleLink: String, articleTitle: String) -> Unit,
    viewModel: MainViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    // Fetch article with source title directly from DB
    val articleWithSource by viewModel.getArticleWithSource(url).collectAsState(initial = null)
    val article = articleWithSource?.article

    // 2. OBTENEMOS LOS ESTADOS DE LA IA (LO NUEVO)
    val summary by viewModel.summaryState.collectAsState()
    val isSummarizing by viewModel.isSummarizing.collectAsState()

    // Haptic feedback logic
    val vibrator = remember { androidx.core.content.ContextCompat.getSystemService(context, android.os.Vibrator::class.java) }
    DisposableEffect(isSummarizing) {
        if (isSummarizing) {
            vibrator?.let { v ->
                if (v.hasVibrator()) {
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                        // Heartbeat pattern: lub-dub, lub-dub...
                        // Timings: [delay, vibrate, pause, vibrate, pause]
                        // Amplitudes: [0, subtle, 0, very subtle, 0]
                        val timings = longArrayOf(0, 80, 100, 80, 1000)
                        val amplitudes = intArrayOf(0, 40, 0, 20, 0) 
                        
                        val effect = android.os.VibrationEffect.createWaveform(timings, amplitudes, 0) // 0 means repeat at index 0
                        v.vibrate(effect)
                    } else {
                        // Fallback for older devices (pattern only, cannot control amplitude)
                        val pattern = longArrayOf(0, 50, 100, 50, 1000)
                        @Suppress("DEPRECATION")
                        v.vibrate(pattern, 0)
                    }
                }
            }
        }
        onDispose {
            vibrator?.cancel()
        }
    }

    val scrollState = rememberScrollState()
    var previousScrollOffset by remember { mutableIntStateOf(0) }
    var isFabVisible by remember { mutableStateOf(true) }

    // Detección scroll down/up
    LaunchedEffect(scrollState.value) {
        val currentOffset = scrollState.value
        if (currentOffset > previousScrollOffset) {
            isFabVisible = false
        } else if (currentOffset < previousScrollOffset) {
            isFabVisible = true
        }
        previousScrollOffset = currentOffset
    }

    val coroutineScope = rememberCoroutineScope()

    var tts: TextToSpeech? by remember { mutableStateOf(null) }
    var isSpeaking by remember { mutableStateOf(false) }

    DisposableEffect(Unit) {
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale("es", "ES")
            }
        }
        onDispose {
            tts?.stop()
            tts?.shutdown()
        }
    }

    // Limpiamos el resumen al salir de la pantalla
    DisposableEffect(Unit) {
        onDispose { viewModel.clearSummary() }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.article_screen_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.article_back))
                    }
                },
                actions = {
                    if (article != null) {
                        IconButton(onClick = {
                            val sendIntent: Intent = Intent().apply {
                                action = Intent.ACTION_SEND
                                putExtra(Intent.EXTRA_TEXT, "${article!!.title}\n\n${article!!.link}")
                                type = "text/plain"
                            }
                            val shareIntent = Intent.createChooser(sendIntent, null)
                            context.startActivity(shareIntent)
                        }) {
                            Icon(Icons.Default.Share, contentDescription = "Share")
                        }
                        val commentCount by remember(article!!.link) { viewModel.getCommentCount(article!!.link) }.collectAsState(initial = 0)
                        IconButton(onClick = {
                            onCommentsClick(article!!.link, article!!.title)
                        }) {
                            BadgedBox(
                                badge = {
                                    Badge(
                                        containerColor = MaterialTheme.colorScheme.primary,
                                        contentColor = MaterialTheme.colorScheme.onPrimary
                                    ) {
                                        Text(commentCount.toString())
                                    } 
                                }
                            ) {
                                Icon(Icons.Default.ChatBubbleOutline, contentDescription = stringResource(R.string.comments_title))
                            }
                        }
                        IconButton(onClick = {
                            val newSavedState = !article!!.isSaved
                            viewModel.toggleSaveArticle(article!!.link, article!!.isSaved)
                            val message = if (newSavedState) context.getString(R.string.msg_added_read_later) else context.getString(R.string.msg_removed_read_later)
                            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                        }) {
                            Icon(
                                imageVector = if (article!!.isSaved) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                                contentDescription = stringResource(R.string.nav_read_later),
                                tint = if (article!!.isSaved) Color(0xFFFFD600) else LocalContentColor.current
                            )
                        }
                    }
                    IconButton(onClick = {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                        context.startActivity(intent)
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = stringResource(R.string.article_open_browser))
                    }
                }
            )
        },
        // 3. EL BOTÓN MÁGICO DE LA IA
        floatingActionButton = {
            if (article != null) { // Solo mostramos botón si hay artículo
                val noContentString = stringResource(R.string.article_no_content)
                AnimatedVisibility(
                    visible = isFabVisible,
                    enter = scaleIn(),
                    exit = scaleOut()
                ) {
                    FloatingActionButton(
                    onClick = {
                        // Si ya hay resumen, lo borramos (toggle), si no, lo pedimos
                        if (summary != null) {
                            viewModel.clearSummary()
                        } else {
                            // Enviamos título y contenido (o descripción si contenido es nulo)
                            viewModel.summarizeArticle(
                                title = article!!.title,
                                content = article!!.description ?: noContentString
                            )
                            coroutineScope.launch {
                                scrollState.animateScrollTo(0)
                            }
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
                            contentDescription = stringResource(R.string.ai_summary_button_desc)
                        )
                    }
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
                    .verticalScroll(scrollState)
                    .padding(horizontal = 24.dp, vertical = 16.dp)
            ) {
                // 4. LA TARJETA DEL RESUMEN (APARECE ARRIBA)
                AnimatedVisibility(visible = summary != null || isSummarizing) {
                    val isDark = isSystemInDarkTheme()
                    Card(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(bottom = 16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isDark) Color(0xFF424242) else MaterialTheme.colorScheme.secondaryContainer
                        )
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = stringResource(R.string.ai_summary_title),
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.weight(1f)
                                )

                                if (!isSummarizing && summary != null) {
                                    IconButton(
                                        onClick = {
                                            if (isSpeaking) {
                                                tts?.stop()
                                                isSpeaking = false
                                            } else {
                                                tts?.speak(summary, TextToSpeech.QUEUE_FLUSH, null, "SummaryTTS")
                                                isSpeaking = true
                                            }
                                        },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.GraphicEq,
                                            contentDescription = "Leer resumen",
                                            tint = if (isSpeaking) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            if (isSummarizing) {
                                // Skeleton Loading
                                Column {
                                    Box(modifier = Modifier.fillMaxWidth().height(16.dp).clip(RoundedCornerShape(4.dp)).aiShimmer())
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Box(modifier = Modifier.fillMaxWidth(0.9f).height(16.dp).clip(RoundedCornerShape(4.dp)).aiShimmer())
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Box(modifier = Modifier.fillMaxWidth(0.7f).height(16.dp).clip(RoundedCornerShape(4.dp)).aiShimmer())
                                }
                            } else {
                                // Markdown simple parsing
                                MarkdownText(
                                    text = summary ?: "",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }
                }
                val displayImageUrl = remember(article) {
                    article.imageUrl ?: article.description?.extractFirstImageUrl()
                }

                if (displayImageUrl != null) {
                    AsyncImage(
                        model = displayImageUrl,
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .clip(RoundedCornerShape(12.dp)),
                        contentScale = ContentScale.Crop
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }
                Text(
                    text = article!!.title,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                        context.startActivity(intent)
                    }
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = articleWithSource?.sourceTitle ?: article!!.sourceUrl,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    if (article!!.hasVideo) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Default.PlayCircle,
                            contentDescription = "Video",
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(16.dp))
                
                // Clean text from description (HTML to plain text)
                // Clean text from description (HTML to plain text)
                // First remove script tags and their content, then parse HTML
                val htmlDescription = article!!.description ?: ""
                val noScriptDescription = htmlDescription.replace(Regex("<script.*?>.*?</script>", RegexOption.DOT_MATCHES_ALL), "")
                
                val cleanDescription = HtmlCompat.fromHtml(
                    noScriptDescription,
                    HtmlCompat.FROM_HTML_MODE_LEGACY
                ).toString()
                .replace("\uFFFC", "")
                .trim()
                .replace(Regex("\\n{3,}"), "\n\n") // Reduce more than 2 newlines to just 2
                .replace(Regex("(\\n\\s*\\n)+"), "\n\n") // Ensure consistent paragraph spacing

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
                Text(stringResource(R.string.article_loading))
            }
        }
    }
}

fun Modifier.aiShimmer(): Modifier = composed {
    var size by remember { mutableStateOf(IntSize.Zero) }
    val transition = rememberInfiniteTransition(label = "ai_shimmer")
    val startOffsetX by transition.animateFloat(
        initialValue = -2 * size.width.toFloat(),
        targetValue = 2 * size.width.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(1000)
        ),
        label = "ai_shimmer_offset"
    )

    val isDark = androidx.compose.foundation.isSystemInDarkTheme()
    // Colores SUPER oscuros para el modo noche, casi invisibles
    val baseColor = if (isDark) Color(0xFF1E1E1E) else Color(0xFFB8B5B5) 
    val highlightColor = if (isDark) Color(0xFF2C2C2C) else Color(0xFF8F8B8B)

    background(
        brush = Brush.linearGradient(
            colors = listOf(
                baseColor,
                highlightColor,
                baseColor,
            ),
            start = Offset(startOffsetX, 0f),
            end = Offset(startOffsetX + size.width.toFloat(), size.height.toFloat())
        )
    )
    .onGloballyPositioned {
        size = it.size
    }
}

@Composable
fun MarkdownText(text: String, modifier: Modifier = Modifier, style: TextStyle = MaterialTheme.typography.bodyMedium) {
    val annotatedString = remember(text) {
        buildAnnotatedString {
            val lines = text.split("\n")
            lines.forEachIndexed { index, line ->
                var processedLine = line.trim()
                // Handle bullet points
                if (processedLine.startsWith("* ")) {
                   processedLine = "•  " + processedLine.substring(2)
                }
                
                // Process bold (**text**)
                val parts = processedLine.split("**")
                parts.forEachIndexed { partIndex, part ->
                    if (partIndex % 2 == 1) { // Odd parts are inside ** ** (e.g. "bold")
                         withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                             append(part)
                         }
                    } else {
                        append(part)
                    }
                }
                
                if (index < lines.size - 1) {
                    append("\n")
                }
            }
        }
    }
    
    Text(text = annotatedString, modifier = modifier, style = style)
}
