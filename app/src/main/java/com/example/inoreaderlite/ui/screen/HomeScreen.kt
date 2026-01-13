package com.example.inoreaderlite.ui.screen

import android.content.ClipData
import android.content.ClipDescription
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.draganddrop.dragAndDropSource
import androidx.compose.foundation.draganddrop.dragAndDropTarget
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.RssFeed
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draganddrop.DragAndDropEvent
import androidx.compose.ui.draganddrop.DragAndDropTarget
import androidx.compose.ui.draganddrop.DragAndDropTransferData
import androidx.compose.ui.draganddrop.mimeTypes
import androidx.compose.ui.draganddrop.toAndroidDragEvent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.example.inoreaderlite.data.local.entity.ArticleEntity
import com.example.inoreaderlite.data.local.entity.SourceEntity
import com.example.inoreaderlite.ui.viewmodel.FeedUiState
import com.example.inoreaderlite.ui.viewmodel.MainViewModel
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(
    viewModel: MainViewModel = hiltViewModel(),
    onArticleClick: (String, Boolean) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val sources by viewModel.sources.collectAsState()
    val folders by viewModel.folders.collectAsState()
    val selectedSource by viewModel.selectedSource.collectAsState()
    val markAsReadOnScroll by viewModel.markAsReadOnScroll.collectAsState()
    
    var showAddDialog by remember { mutableStateOf(false) }
    var showFolderDialog by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Spacer(Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "Subscriptions", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Row {
                        IconButton(onClick = { showSettingsDialog = true }) {
                            Icon(Icons.Default.Settings, contentDescription = "Settings")
                        }
                        IconButton(onClick = { showFolderDialog = true }) {
                            Icon(Icons.Filled.CreateNewFolder, contentDescription = "New Folder")
                        }
                    }
                }
                
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    item(key = "all_feeds") {
                        NavigationDrawerItem(
                            icon = { Icon(Icons.Filled.RssFeed, null) },
                            label = { Text("All Feeds") },
                            selected = selectedSource == null,
                            onClick = {
                                viewModel.selectSource(null)
                                scope.launch { drawerState.close() }
                            },
                            modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                        )
                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    }

                    items(folders, key = { it.name }) { folder ->
                        FolderItem(
                            folderName = folder.name,
                            sources = sources.filter { it.folderName == folder.name },
                            selectedSource = selectedSource,
                            onFolderClick = { name ->
                                viewModel.selectFolder(name)
                                scope.launch { drawerState.close() }
                            },
                            onSourceClick = { url ->
                                viewModel.selectSource(url)
                                scope.launch { drawerState.close() }
                            },
                            onDrop = { sourceUrl ->
                                viewModel.moveSourceToFolder(sourceUrl, folder.name)
                            }
                        )
                    }

                    item(key = "uncategorized_header") {
                        val orphanSources = sources.filter { it.folderName == null }
                        if (orphanSources.isNotEmpty()) {
                            Text(
                                text = "Uncategorized",
                                style = MaterialTheme.typography.labelMedium,
                                modifier = Modifier.padding(start = 28.dp, top = 16.dp, bottom = 8.dp),
                                color = MaterialTheme.colorScheme.primary
                            )
                            orphanSources.forEach { source ->
                                SourceDrawerItem(source, selectedSource == source.url) {
                                    viewModel.selectSource(source.url)
                                    scope.launch { drawerState.close() }
                                }
                            }
                        }
                    }
                }
            }
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { 
                        Text(
                            when {
                                selectedSource == null -> "Inoreader Lite"
                                selectedSource!!.startsWith("folder:") -> "Folder: ${selectedSource!!.removePrefix("folder:")}"
                                else -> "Filtered Feed"
                            }
                        ) 
                    },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Default.Menu, contentDescription = "Menu")
                        }
                    },
                    actions = {
                        IconButton(onClick = { viewModel.sync() }) {
                            Icon(Icons.Default.Refresh, contentDescription = "Sync")
                        }
                    }
                )
            },
            floatingActionButton = {
                FloatingActionButton(onClick = { showAddDialog = true }) {
                    Icon(Icons.Default.Add, contentDescription = "Add Source")
                }
            }
        ) { padding ->
            PullToRefreshBox(
                isRefreshing = isRefreshing,
                onRefresh = { viewModel.sync() },
                modifier = Modifier.padding(padding)
            ) {
                when (val state = uiState) {
                    is FeedUiState.Loading -> {
                        if (!isRefreshing) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator()
                            }
                        }
                    }
                    is FeedUiState.Success -> {
                        ArticleList(
                            articles = state.articles, 
                            markAsReadOnScroll = markAsReadOnScroll,
                            onMarkAsRead = { viewModel.markAsRead(it) },
                            onArticleClick = { link, isRead ->
                                if (!isRead) viewModel.markAsRead(link)
                                onArticleClick(link, isRead)
                            }
                        )
                    }
                    is FeedUiState.Error -> {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(text = "Error: ${state.message}", color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }

            if (showAddDialog) {
                AddSourceDialog(
                    onDismiss = { showAddDialog = false },
                    onAdd = { url ->
                        viewModel.addSource(url)
                        showAddDialog = false
                    }
                )
            }
            
            if (showFolderDialog) {
                AddFolderDialog(
                    onDismiss = { showFolderDialog = false },
                    onConfirm = { name ->
                        viewModel.addFolder(name)
                        showFolderDialog = false
                    }
                )
            }

            if (showSettingsDialog) {
                SettingsDialog(
                    onDismiss = { showSettingsDialog = false },
                    markAsReadOnScroll = markAsReadOnScroll,
                    onToggleMarkAsReadOnScroll = { viewModel.toggleMarkAsReadOnScroll(it) }
                )
            }
        }
    }
}

@Composable
fun SettingsDialog(
    onDismiss: () -> Unit,
    markAsReadOnScroll: Boolean,
    onToggleMarkAsReadOnScroll: (Boolean) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Settings") },
        text = {
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Mark as read on scroll")
                Switch(
                    checked = markAsReadOnScroll,
                    onCheckedChange = onToggleMarkAsReadOnScroll
                )
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) { Text("Close") }
        }
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FolderItem(
    folderName: String,
    sources: List<SourceEntity>,
    selectedSource: String?,
    onFolderClick: (String) -> Unit,
    onSourceClick: (String) -> Unit,
    onDrop: (String) -> Unit
) {
    var isOver by remember { mutableStateOf(false) }
    val isFolderSelected = selectedSource == "folder:$folderName"
    
    val dndTarget = remember(folderName) {
        object : DragAndDropTarget {
            override fun onDrop(event: DragAndDropEvent): Boolean {
                val clipData = event.toAndroidDragEvent().clipData
                if (clipData != null && clipData.itemCount > 0) {
                    val sourceUrl = clipData.getItemAt(0).text.toString()
                    onDrop(sourceUrl)
                }
                isOver = false
                return true
            }
            override fun onEntered(event: DragAndDropEvent) { isOver = true }
            override fun onExited(event: DragAndDropEvent) { isOver = false }
            override fun onEnded(event: DragAndDropEvent) { isOver = false }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .dragAndDropTarget(
                shouldStartDragAndDrop = { event ->
                    event.mimeTypes().contains(ClipDescription.MIMETYPE_TEXT_PLAIN)
                },
                target = dndTarget
            )
            .padding(vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onFolderClick(folderName) }
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Filled.Folder, 
                contentDescription = null, 
                tint = if (isOver || isFolderSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.width(12.dp))
            Text(
                text = folderName, 
                style = MaterialTheme.typography.titleSmall,
                fontWeight = if (isOver || isFolderSelected) FontWeight.Bold else FontWeight.Normal,
                color = if (isOver || isFolderSelected) MaterialTheme.colorScheme.primary else Color.Unspecified
            )
        }
        
        sources.forEach { source ->
            SourceDrawerItem(source, selectedSource == source.url, onSourceClick)
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SourceDrawerItem(source: SourceEntity, isSelected: Boolean, onClick: (String) -> Unit) {
    val backgroundColor = if (isSelected) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent
    val contentColor = if (isSelected) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant

    Surface(
        onClick = { onClick(source.url) },
        shape = CircleShape,
        color = backgroundColor,
        contentColor = contentColor,
        modifier = Modifier
            .fillMaxWidth()
            .padding(NavigationDrawerItemDefaults.ItemPadding)
            .dragAndDropSource {
                detectDragGesturesAfterLongPress(
                    onDragStart = {
                        startTransfer(
                            DragAndDropTransferData(
                                clipData = ClipData.newPlainText("sourceUrl", source.url)
                            )
                        )
                    },
                    onDrag = { _, _ -> },
                    onDragEnd = { },
                    onDragCancel = { }
                )
            }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (source.iconUrl != null) {
                AsyncImage(model = source.iconUrl, contentDescription = null, modifier = Modifier.size(24.dp))
            } else {
                Icon(Icons.Filled.RssFeed, null, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.width(12.dp))
            Text(
                text = source.title,
                style = MaterialTheme.typography.labelLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun AddFolderDialog(onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var text by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New Folder") },
        text = {
            TextField(value = text, onValueChange = { text = it }, label = { Text("Folder Name") }, singleLine = true)
        },
        confirmButton = {
            Button(onClick = { if(text.isNotBlank()) onConfirm(text) }) { Text("Create") }
        },
        dismissButton = {
            Button(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun ArticleList(
    articles: List<ArticleEntity>, 
    markAsReadOnScroll: Boolean,
    onMarkAsRead: (String) -> Unit,
    onArticleClick: (String, Boolean) -> Unit
) {
    val listState = rememberLazyListState()

    if (markAsReadOnScroll) {
        LaunchedEffect(listState) {
            snapshotFlow { listState.firstVisibleItemIndex }
                .distinctUntilChanged()
                .filter { it > 0 }
                .collect { firstIndex ->
                    // Mark articles above the current first visible item as read
                    for (i in 0 until firstIndex) {
                        if (i < articles.size) {
                            onMarkAsRead(articles[i].link)
                        }
                    }
                }
        }
    }

    LazyColumn(
        state = listState,
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(articles, key = { it.link }) { article ->
            ArticleItem(article, onArticleClick)
        }
    }
}

@Composable
fun ArticleItem(article: ArticleEntity, onClick: (String, Boolean) -> Unit) {
    Card(
        onClick = { onClick(article.link, article.isRead) },
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(modifier = Modifier.padding(16.dp)) {
            if (article.imageUrl != null) {
                AsyncImage(
                    model = article.imageUrl,
                    contentDescription = null,
                    modifier = Modifier.size(80.dp),
                    contentScale = ContentScale.Crop
                )
                Spacer(modifier = Modifier.width(16.dp))
            }
            
            Column {
                Text(
                    text = article.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = if (article.isRead) Color.Gray else Color.Unspecified,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${formatDate(article.pubDate)} • ${article.sourceUrl}",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@Composable
fun AddSourceDialog(onDismiss: () -> Unit, onAdd: (String) -> Unit) {
    var text by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add RSS Feed") },
        text = {
            TextField(
                value = text,
                onValueChange = { text = it },
                label = { Text("URL") },
                singleLine = true
            )
        },
        confirmButton = {
            Button(onClick = { onAdd(text) }) {
                Text("Add")
            }
        },
        dismissButton = {
            Button(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

private fun formatDate(timestamp: Long): String {
    val sdf = SimpleDateFormat("dd MMM HH:mm", Locale.getDefault())
    return sdf.format(Date(timestamp))
}
