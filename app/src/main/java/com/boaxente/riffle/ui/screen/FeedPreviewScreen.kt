package com.boaxente.riffle.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.RssFeed
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.boaxente.riffle.R
import com.boaxente.riffle.ui.viewmodel.MainViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedPreviewScreen(
    feedUrl: String,
    feedTitle: String?,
    feedIconUrl: String?,
    onBack: () -> Unit,
    onArticleClick: (String, Boolean) -> Unit, // link, isVideo (not used here really)
    onAddFeed: () -> Unit,
    viewModel: MainViewModel
) {
    val articles by viewModel.previewArticles.collectAsState()
    val isLoading by viewModel.isPreviewLoading.collectAsState()
    val sourceAdditionState by viewModel.sourceAdditionState.collectAsState()

    LaunchedEffect(feedUrl) {
        viewModel.loadFeedPreview(feedUrl)
    }

    // Reset preview when leaving
    androidx.compose.runtime.DisposableEffect(Unit) {
        onDispose {
            viewModel.clearFeedPreview()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (feedIconUrl != null) {
                            AsyncImage(
                                model = feedIconUrl,
                                contentDescription = null,
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                        } else {
                            Icon(Icons.Default.RssFeed, null, modifier = Modifier.size(24.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        Text(
                            text = feedTitle ?: feedUrl,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.article_back)
                        )
                    }
                },
                actions = {
                    val isAdding = (sourceAdditionState is com.boaxente.riffle.ui.viewmodel.SourceAdditionState.Loading)
                    
                    if (isAdding) {
                         CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    } else {
                        IconButton(onClick = onAddFeed) {
                            Icon(Icons.Default.Add, contentDescription = stringResource(R.string.dialog_add))
                        }
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (articles.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = stringResource(R.string.article_no_content),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(articles) { article ->
                        Card(
                            onClick = { onArticleClick(article.link, false) },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(modifier = Modifier.padding(12.dp)) {
                                if (article.imageUrl != null) {
                                    AsyncImage(
                                        model = article.imageUrl,
                                        contentDescription = null,
                                        modifier = Modifier
                                            .size(80.dp)
                                            .clip(RoundedCornerShape(8.dp)),
                                        contentScale = ContentScale.Crop
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                }
                                
                                Column {
                                    Text(
                                        text = article.title,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    if (article.pubDate != null) {
                                        val date = Date(article.pubDate)
                                        val format = SimpleDateFormat("dd MMM, HH:mm", Locale.getDefault())
                                        Text(
                                            text = format.format(date),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    if (article.description != null) {
                                        val cleanDesc = androidx.core.text.HtmlCompat.fromHtml(
                                            article.description,
                                            androidx.core.text.HtmlCompat.FROM_HTML_MODE_LEGACY
                                        ).toString().trim()
                                        
                                        Text(
                                            text = cleanDesc,
                                            style = MaterialTheme.typography.bodySmall,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
            
            // Handle success/error of adding feed
            if (sourceAdditionState is com.boaxente.riffle.ui.viewmodel.SourceAdditionState.Success) {
                 LaunchedEffect(Unit) {
                     viewModel.clearSourceAdditionState()
                     onBack() // Go back to search (or home?)
                 }
            }
        }
    }
}
