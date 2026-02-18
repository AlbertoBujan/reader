package com.boaxente.riffle.ui.screen


import androidx.compose.ui.res.stringArrayResource
import androidx.compose.foundation.clickable
import kotlinx.coroutines.delay
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.rememberLottieComposition
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.RssFeed
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.boaxente.riffle.R
import com.boaxente.riffle.ui.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchFeedScreen(
    onBack: () -> Unit,
    onNavigateToPreview: (String, String?, String?) -> Unit,
    viewModel: MainViewModel
) {
    var query by remember { mutableStateOf("") }
    val keyboardController = LocalSoftwareKeyboardController.current
    val feeds by viewModel.sortedDiscoveredFeeds.collectAsState()
    val isSearching by viewModel.isSearching.collectAsState()
    val focusRequester = remember { FocusRequester() }
    var isExpanded by remember { mutableStateOf(false) }
    val sources by viewModel.sources.collectAsState()
    val feedHealthState by viewModel.feedHealthState.collectAsState()
    val discoveredHealth by viewModel.discoveredFeedHealth.collectAsState()
    val savedSourceUrls = remember(sources) { sources.map { it.url }.toSet() }

    // Reset expansion when search results change or new search begins
    LaunchedEffect(isSearching) {
        if (isSearching) {
            isExpanded = false
        }
    }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    DisposableEffect(Unit) {
        onDispose {
            viewModel.clearFeedSearch()
        }
    }
    
    // Clear results when leaving screen? Maybe not necessary but good practice to clear text field
    // But we are using local state for query.

    val sourceAdditionState by viewModel.sourceAdditionState.collectAsState()

    var showDialog by remember { mutableStateOf(false) }
    var dialogTitle by remember { mutableStateOf("") }
    var dialogMessage by remember { mutableStateOf("") }

    LaunchedEffect(sourceAdditionState) {
        when (val state = sourceAdditionState) {
            is com.boaxente.riffle.ui.viewmodel.SourceAdditionState.Success -> {
                viewModel.clearSourceAdditionState()
                onBack()
            }
            is com.boaxente.riffle.ui.viewmodel.SourceAdditionState.Error -> {
                dialogTitle = state.title
                dialogMessage = state.message
                showDialog = true
            }
            else -> {}
        }
    }

    if (showDialog) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { 
                showDialog = false 
                viewModel.clearSourceAdditionState()
            },
            title = { Text(dialogTitle) },
            text = { Text(dialogMessage) },
            confirmButton = {
                TextButton(onClick = { 
                    showDialog = false
                    viewModel.clearSourceAdditionState()
                }) {
                    Text("OK")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    TextField(
                        value = query,
                        onValueChange = { query = it },
                        placeholder = { Text(stringResource(R.string.nav_search_feeds)) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(focusRequester),
                        singleLine = true,
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        ),
                        keyboardActions = KeyboardActions(onSearch = {
                            viewModel.searchFeeds(query)
                            keyboardController?.hide()
                        }),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        trailingIcon = {
                            if (query.isNotEmpty()) {
                                IconButton(onClick = { query = "" }) {
                                    Icon(Icons.Default.Close, contentDescription = "Clear")
                                }
                            }
                        }
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.Default.ChevronRight, 
                            contentDescription = stringResource(R.string.article_back), 
                            modifier = Modifier.rotate(180f)
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.searchFeeds(query) }, 
                        enabled = query.isNotBlank() && !isSearching
                    ) {
                        Icon(Icons.Default.Search, contentDescription = "Search")
                    }
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            if (isSearching) {
                val phrases = stringArrayResource(R.array.loading_phrases)
                var currentPhrase by remember { mutableStateOf(phrases.random()) }
                
                LaunchedEffect(Unit) {
                    while(true) {
                        delay(3000)
                        currentPhrase = phrases.random()
                    }
                }

                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.padding(32.dp)
                    ) {
                        val composition by rememberLottieComposition(LottieCompositionSpec.Asset("loading.json"))
                        LottieAnimation(
                            composition = composition,
                            iterations = LottieConstants.IterateForever,
                            modifier = Modifier.size(200.dp)
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Text(
                            text = currentPhrase,
                            style = MaterialTheme.typography.bodyLarge,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (query.isNotBlank() && !isSearching && feeds.isEmpty()) {
                        item {
                            val isDirectLoading = (sourceAdditionState is com.boaxente.riffle.ui.viewmodel.SourceAdditionState.Loading) &&
                                (sourceAdditionState as com.boaxente.riffle.ui.viewmodel.SourceAdditionState.Loading).targetUrl == query
                            
                            TextButton(
                                onClick = {
                                    viewModel.addSource(query, null)
                                },
                                modifier = Modifier.fillMaxWidth(),
                                enabled = !isDirectLoading
                            ) {
                                if (isDirectLoading) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(24.dp),
                                        strokeWidth = 2.dp,
                                        color = MaterialTheme.colorScheme.secondary
                                    )
                                } else {
                                    Icon(Icons.Default.Add, null)
                                    Spacer(Modifier.width(8.dp))
                                    Text(stringResource(R.string.search_add_directly, query))
                                }
                            }
                        }
                    }
                    
                    val displayedFeeds = if (isExpanded) feeds else feeds.take(5)
                    
                    items(displayedFeeds) { feed ->
                        val isAlreadySaved = savedSourceUrls.contains(feed.url)
                        val health = if (isAlreadySaved) feedHealthState[feed.url] else discoveredHealth[feed.url]
                        val isItemLoading = (sourceAdditionState is com.boaxente.riffle.ui.viewmodel.SourceAdditionState.Loading) &&
                            (sourceAdditionState as com.boaxente.riffle.ui.viewmodel.SourceAdditionState.Loading).targetUrl == feed.url
                        
                        ListItem(
                            modifier = Modifier.clickable {
                                onNavigateToPreview(feed.url, feed.title, feed.iconUrl)
                            },
                            leadingContent = {
                                Box {
                                    if (feed.iconUrl != null) {
                                        AsyncImage(
                                            model = feed.iconUrl,
                                            contentDescription = null,
                                            modifier = Modifier.size(32.dp).clip(CircleShape)
                                        )
                                    } else {
                                        Icon(Icons.Default.RssFeed, null, modifier = Modifier.size(32.dp))
                                    }
                                    // Health indicator dot overlaid on bottom-right of icon
                                    // Health indicator dot overlaid on bottom-right of icon
                                    val healthStatus = health ?: com.boaxente.riffle.ui.viewmodel.FeedHealth.UNKNOWN
                                    androidx.compose.foundation.Canvas(
                                        modifier = Modifier
                                            .size(10.dp)
                                            .align(Alignment.BottomEnd)
                                    ) {
                                        drawCircle(
                                            color = when (healthStatus) {
                                                com.boaxente.riffle.ui.viewmodel.FeedHealth.GOOD -> Color(0xFF4CAF50)
                                                com.boaxente.riffle.ui.viewmodel.FeedHealth.WARNING -> Color(0xFFFFC107)
                                                com.boaxente.riffle.ui.viewmodel.FeedHealth.BAD -> Color(0xFFF44336)
                                                com.boaxente.riffle.ui.viewmodel.FeedHealth.DEAD -> Color(0xFF616161)
                                                com.boaxente.riffle.ui.viewmodel.FeedHealth.UNKNOWN -> Color(0xFF9E9E9E)
                                            }
                                        )
                                    }
                                }
                            },
                            headlineContent = { Text(feed.siteName ?: feed.title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                            supportingContent = { 
                                Text(
                                    text = feed.url, 
                                    maxLines = 2, 
                                    overflow = TextOverflow.Ellipsis,
                                    style = MaterialTheme.typography.bodySmall
                                ) 
                            },
                            trailingContent = {
                                if (isAlreadySaved) {
                                    Text(
                                        text = stringResource(R.string.search_already_added),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                } else if (isItemLoading) {
                                    Box(
                                        modifier = Modifier.size(48.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(24.dp),
                                            strokeWidth = 2.dp
                                        )
                                    }
                                } else {
                                    IconButton(
                                        onClick = { 
                                            viewModel.addSource(feed.url, feed.siteName ?: feed.title, feed.iconUrl)
                                        },
                                        enabled = !(sourceAdditionState is com.boaxente.riffle.ui.viewmodel.SourceAdditionState.Loading)
                                    ) {
                                        Icon(Icons.Default.Add, contentDescription = stringResource(R.string.dialog_add))
                                    }
                                }
                            }
                        )
                    }

                    if (!isExpanded && feeds.size > 5) {
                        item {
                            TextButton(
                                onClick = { isExpanded = true },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(stringResource(R.string.action_show_more))
                            }
                        }
                    }
                }
            }
        }
    }
}
