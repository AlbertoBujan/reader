package com.example.inoreaderlite.ui.screen

import android.content.ClipData
import android.content.ClipDescription
import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.draganddrop.dragAndDropSource
import androidx.compose.foundation.draganddrop.dragAndDropTarget
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.BookmarkRemove
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.RssFeed
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxState
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.rememberDrawerState
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draganddrop.DragAndDropEvent
import androidx.compose.ui.draganddrop.DragAndDropTarget
import androidx.compose.ui.draganddrop.DragAndDropTransferData
import androidx.compose.ui.draganddrop.mimeTypes
import androidx.compose.ui.draganddrop.toAndroidDragEvent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.example.inoreaderlite.data.local.entity.ArticleEntity
import com.example.inoreaderlite.data.local.entity.SourceEntity
import com.example.inoreaderlite.ui.viewmodel.DiscoveredFeed
import com.example.inoreaderlite.ui.viewmodel.FeedUiState
import com.example.inoreaderlite.ui.viewmodel.MainViewModel
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.absoluteValue

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
    val unreadCounts by viewModel.unreadCounts.collectAsState()
    val isDarkMode by viewModel.isDarkMode.collectAsState()
    val savedCount by viewModel.savedCount.collectAsState()
    
    var showAddDialog by remember { mutableStateOf(false) }
    var showFolderDialog by remember { mutableStateOf(false) }
    var showSearchDialog by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }
    var renamingSource by remember { mutableStateOf<SourceEntity?>(null) }
    var renamingFolder by remember { mutableStateOf<String?>(null) }

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    // Manejo del botón atrás de Android
    BackHandler(enabled = drawerState.isOpen) {
        scope.launch { drawerState.close() }
    }
    
    // Si el drawer está cerrado, el botón atrás abrirá el drawer la primera vez
    if (drawerState.isClosed) {
        BackHandler(enabled = true) {
            scope.launch { drawerState.open() }
        }
    }

    ModalNavigationDrawer(
        modifier = Modifier.fillMaxSize(),
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Box(modifier = Modifier.fillMaxSize()) {
                    Column(modifier = Modifier.fillMaxSize()) {
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
                                IconButton(onClick = { showSearchDialog = true }) {
                                    Icon(Icons.Default.Search, contentDescription = "Search Feeds")
                                }
                                IconButton(onClick = { showFolderDialog = true }) {
                                    Icon(Icons.Filled.CreateNewFolder, contentDescription = "New Folder")
                                }
                            }
                        }
                        
                        LazyColumn(modifier = Modifier.fillMaxSize()) {
                            item(key = "all_feeds") {
                                val totalUnread = unreadCounts.values.sum()
                                NavigationDrawerItem(
                                    icon = { Icon(Icons.Filled.RssFeed, null) },
                                    label = { 
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text("All Feeds")
                                            if (totalUnread > 0) {
                                                Spacer(Modifier.width(8.dp))
                                                Text(
                                                    text = totalUnread.toString(),
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                            }
                                        }
                                    },
                                    selected = selectedSource == null,
                                    onClick = {
                                        viewModel.selectSource(null)
                                        scope.launch { drawerState.close() }
                                    },
                                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                                )
                            }

                            item(key = "read_later") {
                                NavigationDrawerItem(
                                    icon = { Icon(Icons.Default.Bookmark, null) },
                                    label = { 
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text("Read Later")
                                            if (savedCount > 0) {
                                                Spacer(Modifier.width(8.dp))
                                                Text(
                                                    text = savedCount.toString(),
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                            }
                                        }
                                    },
                                    selected = selectedSource == "saved",
                                    onClick = {
                                        viewModel.selectSaved()
                                        scope.launch { drawerState.close() }
                                    },
                                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                                )
                                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                            }

                            items(folders, key = { it.name }) { folder ->
                                val folderSources = sources.filter { it.folderName == folder.name }
                                val folderUnread = folderSources.sumOf { unreadCounts[it.url] ?: 0 }
                                FolderItem(
                                    folderName = folder.name,
                                    sources = folderSources,
                                    folderUnreadCount = folderUnread,
                                    selectedSource = selectedSource,
                                    onFolderClick = { name ->
                                        viewModel.selectFolder(name)
                                    },
                                    onSourceClick = { url ->
                                        viewModel.selectSource(url)
                                        scope.launch { drawerState.close() }
                                    },
                                    onDeleteSource = { url -> viewModel.deleteSource(url) },
                                    onRenameSource = { source -> renamingSource = source },
                                    onRenameFolder = { name -> renamingFolder = name },
                                    onDeleteFolder = { name -> viewModel.deleteFolder(name) },
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
                                        key(source.url) {
                                            SwipeableSourceItem(
                                                source = source,
                                                isSelected = selectedSource == source.url,
                                                onClick = {
                                                    viewModel.selectSource(source.url)
                                                    scope.launch { drawerState.close() }
                                                },
                                                onDelete = { viewModel.deleteSource(source.url) },
                                                onRename = { renamingSource = source }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                    FloatingActionButton(
                        onClick = { showAddDialog = true },
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(16.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Add Source")
                    }
                }
            }
        }
    ) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            topBar = {
                TopAppBar(
                    title = { 
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                when {
                                    selectedSource == "saved" -> "Read Later"
                                    selectedSource == null -> "Riffle"
                                    selectedSource!!.startsWith("folder:") -> "Folder: ${selectedSource!!.removePrefix("folder:")}"
                                    else -> sources.find { it.url == selectedSource }?.title ?: "Filtered Feed"
                                }
                            )
                            val currentUnread = when {
                                selectedSource == "saved" -> 0
                                selectedSource == null -> unreadCounts.values.sum()
                                selectedSource!!.startsWith("folder:") -> {
                                    val folderName = selectedSource!!.removePrefix("folder:")
                                    sources.filter { it.folderName == folderName }.sumOf { unreadCounts[it.url] ?: 0 }
                                }
                                else -> unreadCounts[selectedSource] ?: 0
                            }
                            if (currentUnread > 0) {
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = "($currentUnread)",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Default.Menu, contentDescription = "Menu")
                        }
                    }
                )
            }
        ) { padding ->
            PullToRefreshBox(
                isRefreshing = isRefreshing,
                onRefresh = { viewModel.sync() },
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
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
                            onMarkAllAsRead = { viewModel.markAllAsRead() },
                            onToggleSave = { link, isSaved -> 
                                viewModel.toggleSaveArticle(link, isSaved)
                                val message = if (isSaved) "Removed from Read Later" else "Added to Read Later"
                                Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                            },
                            onShare = { article ->
                                val sendIntent: Intent = Intent().apply {
                                    action = Intent.ACTION_SEND
                                    putExtra(Intent.EXTRA_TEXT, "${article.title}\n\n${article.link}")
                                    type = "text/plain"
                                }
                                val shareIntent = Intent.createChooser(sendIntent, null)
                                context.startActivity(shareIntent)
                            },
                            onArticleClick = { link, isRead ->
                                if (!isRead) viewModel.markAsRead(link)
                                onArticleClick(link, isRead)
                            },
                            isReadLaterView = selectedSource == "saved"
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
                    onAdd = { url, title ->
                        viewModel.addSource(url, title)
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

            if (showSearchDialog) {
                SearchFeedDialog(
                    onDismiss = { showSearchDialog = false },
                    viewModel = viewModel
                )
            }

            if (showSettingsDialog) {
                SettingsDialog(
                    onDismiss = { showSettingsDialog = false },
                    markAsReadOnScroll = markAsReadOnScroll,
                    onToggleMarkAsReadOnScroll = { viewModel.toggleMarkAsReadOnScroll(it) },
                    isDarkMode = isDarkMode,
                    onToggleDarkMode = { viewModel.toggleDarkMode(it) }
                )
            }

            renamingSource?.let { source ->
                RenameSourceDialog(
                    initialTitle = source.title,
                    onDismiss = { renamingSource = null },
                    onConfirm = { newTitle ->
                        viewModel.renameSource(source.url, newTitle)
                        renamingSource = null
                    }
                )
            }

            renamingFolder?.let { folderName ->
                RenameFolderDialog(
                    initialName = folderName,
                    onDismiss = { renamingFolder = null },
                    onConfirm = { newName ->
                        viewModel.renameFolder(folderName, newName)
                        renamingFolder = null
                    }
                )
            }
        }
    }
}

@Composable
fun SearchFeedDialog(onDismiss: () -> Unit, viewModel: MainViewModel) {
    var query by remember { mutableStateOf("") }
    val feeds by viewModel.discoveredFeeds.collectAsState()
    val isSearching by viewModel.isSearching.collectAsState()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Search Feeds") },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TextField(
                        value = query,
                        onValueChange = { query = it },
                        label = { Text("URL or Domain (e.g. xataka.com)") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    IconButton(onClick = { viewModel.searchFeeds(query) }, enabled = query.isNotBlank() && !isSearching) {
                        Icon(Icons.Default.Search, contentDescription = "Search")
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                if (isSearching) {
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                } else {
                    LazyColumn(modifier = Modifier.height(300.dp)) {
                        items(feeds) { feed ->
                            ListItem(
                                leadingContent = {
                                    if (feed.iconUrl != null) {
                                        AsyncImage(
                                            model = feed.iconUrl,
                                            contentDescription = null,
                                            modifier = Modifier.size(32.dp).clip(CircleShape)
                                        )
                                    } else {
                                        Icon(Icons.Default.RssFeed, null)
                                    }
                                },
                                headlineContent = { Text(feed.siteName ?: feed.title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                                supportingContent = { Text(feed.url, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                                trailingContent = {
                                    IconButton(onClick = { 
                                        viewModel.addSource(feed.url, feed.siteName ?: feed.title, feed.iconUrl)
                                        onDismiss()
                                    }) {
                                        Icon(Icons.Default.Add, contentDescription = "Add")
                                    }
                                }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) { Text("Close") }
        }
    )
}

@Composable
fun RenameSourceDialog(initialTitle: String, onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var text by remember { mutableStateOf(initialTitle) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Rename Feed") },
        text = {
            TextField(value = text, onValueChange = { text = it }, label = { Text("Feed Title") }, singleLine = true)
        },
        confirmButton = {
            Button(onClick = { if(text.isNotBlank()) onConfirm(text) }) { Text("Rename") }
        },
        dismissButton = {
            Button(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun RenameFolderDialog(initialName: String, onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var text by remember { mutableStateOf(initialName) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Rename Folder") },
        text = {
            TextField(value = text, onValueChange = { text = it }, label = { Text("Folder Name") }, singleLine = true)
        },
        confirmButton = {
            Button(onClick = { if(text.isNotBlank()) onConfirm(text) }) { Text("Rename") }
        },
        dismissButton = {
            Button(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun SettingsDialog(
    onDismiss: () -> Unit,
    markAsReadOnScroll: Boolean,
    onToggleMarkAsReadOnScroll: (Boolean) -> Unit,
    isDarkMode: Boolean,
    onToggleDarkMode: (Boolean) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Settings") },
        text = {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Dark Mode")
                    Switch(
                        checked = isDarkMode,
                        onCheckedChange = onToggleDarkMode
                    )
                }
                HorizontalDivider()
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
            }
        },
        confirmButton = {
            Button(onClick = { onDismiss() }) { Text("Close") }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SwipeActionBackground(dismissState: SwipeToDismissBoxState, shape: Shape) {
    val direction = dismissState.dismissDirection ?: return
    val isMoving = dismissState.currentValue != dismissState.targetValue || dismissState.progress != 0f

    if (!isMoving) return

    val isDelete = direction == SwipeToDismissBoxValue.EndToStart
    val color = if (isDelete) Color(0xFFE53935) else Color(0xFF1E88E5)
    val alignment = if (isDelete) Alignment.CenterEnd else Alignment.CenterStart
    val icon = if (isDelete) Icons.Default.Delete else Icons.Default.Edit

    Box(
        modifier = Modifier
            .fillMaxSize()
            .clip(shape)
            .background(color)
            .padding(horizontal = 24.dp),
        contentAlignment = alignment
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(24.dp)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SwipeableItem(
    onDelete: () -> Unit,
    onEdit: () -> Unit,
    modifier: Modifier = Modifier,
    shape: Shape = RectangleShape,
    content: @Composable () -> Unit
) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = {
            when (it) {
                SwipeToDismissBoxValue.EndToStart -> {
                    onDelete()
                    true
                }
                SwipeToDismissBoxValue.StartToEnd -> {
                    onEdit()
                    false
                }
                else -> false
            }
        }
    )

    SwipeToDismissBox(
        state = dismissState,
        modifier = modifier,
        enableDismissFromStartToEnd = true,
        enableDismissFromEndToStart = true,
        backgroundContent = { SwipeActionBackground(dismissState, shape) }
    ) {
        // We use a Surface here to ensure the item is opaque and covers the swipe background
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = shape,
            color = MaterialTheme.colorScheme.surfaceContainerLow
        ) {
            content()
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FolderItem(
    folderName: String,
    sources: List<SourceEntity>,
    folderUnreadCount: Int,
    selectedSource: String?,
    onFolderClick: (String) -> Unit,
    onSourceClick: (String) -> Unit,
    onDeleteSource: (String) -> Unit,
    onRenameSource: (SourceEntity) -> Unit,
    onRenameFolder: (String) -> Unit,
    onDeleteFolder: (String) -> Unit,
    onDrop: (String) -> Unit
) {
    var isOver by remember { mutableStateOf(false) }
    val isFolderSelected = selectedSource == "folder:$folderName"
    val isAnySourceSelected = remember(sources, selectedSource) { sources.any { it.url == selectedSource } }
    var isExpanded by remember { mutableStateOf(isFolderSelected || isAnySourceSelected) }

    LaunchedEffect(isFolderSelected, isAnySourceSelected) {
        if (isFolderSelected || isAnySourceSelected) {
            isExpanded = true
        }
    }
    
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
        SwipeableItem(
            onDelete = { onDeleteFolder(folderName) },
            onEdit = { onRenameFolder(folderName) },
            modifier = Modifier.padding(horizontal = 12.dp),
            shape = CircleShape
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { 
                        isExpanded = !isExpanded
                        onFolderClick(folderName) 
                    }
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    if (isExpanded) Icons.Filled.ExpandMore else Icons.Filled.ChevronRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(4.dp))
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
                    color = if (isOver || isFolderSelected) MaterialTheme.colorScheme.primary else Color.Unspecified,
                    modifier = Modifier.weight(1f)
                )
                if (folderUnreadCount > 0) {
                    Text(
                        text = folderUnreadCount.toString(),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isOver || isFolderSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        
        if (isExpanded) {
            sources.forEach { source ->
                key(source.url) {
                    SwipeableSourceItem(
                        source = source,
                        isSelected = selectedSource == source.url,
                        onClick = onSourceClick,
                        onDelete = { onDeleteSource(source.url) },
                        onRename = { onRenameSource(source) }
                    )
                }
            }
        }
    }
}

@Composable
fun SwipeableSourceItem(
    source: SourceEntity,
    isSelected: Boolean,
    onClick: (String) -> Unit,
    onDelete: () -> Unit,
    onRename: () -> Unit
) {
    SwipeableItem(
        onDelete = onDelete,
        onEdit = onRename,
        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding),
        shape = CircleShape
    ) {
        SourceDrawerItemContent(source, isSelected, onClick)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SourceDrawerItemContent(source: SourceEntity, isSelected: Boolean, onClick: (String) -> Unit) {
    val backgroundColor = if (isSelected) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent
    val contentColor = if (isSelected) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant

    Surface(
        onClick = { onClick(source.url) },
        shape = CircleShape,
        color = backgroundColor,
        contentColor = contentColor,
        modifier = Modifier
            .fillMaxWidth()
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
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (source.iconUrl != null) {
                AsyncImage(
                    model = source.iconUrl,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp).clip(CircleShape)
                )
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
    onMarkAllAsRead: () -> Unit,
    onToggleSave: (String, Boolean) -> Unit,
    onShare: (ArticleEntity) -> Unit,
    onArticleClick: (String, Boolean) -> Unit,
    isReadLaterView: Boolean = false
) {
    val listState = rememberLazyListState()

    val currentArticles by rememberUpdatedState(articles)

    if (markAsReadOnScroll) {
        LaunchedEffect(listState) {
            snapshotFlow { listState.firstVisibleItemIndex }
                .distinctUntilChanged()
                .filter { it > 0 }
                .collect { firstIndex ->
                    for (i in 0 until firstIndex) {
                        // Check bounds and only mark if NOT already read to avoid valid ANR loop
                        if (i < currentArticles.size) {
                            val article = currentArticles[i]
                            if (!article.isRead) {
                                onMarkAsRead(article.link)
                            }
                        }
                    }
                }
        }
    }

    LazyColumn(
        state = listState,
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        items(articles, key = { it.link }) { article ->
            SwipeableArticleItem(
                article = article,
                onToggleSave = { onToggleSave(article.link, article.isSaved) },
                onShare = { onShare(article) },
                onArticleClick = onArticleClick,
                isReadLaterView = isReadLaterView
            )
        }
        
        if (articles.isNotEmpty() && !isReadLaterView) {
            item {
                MarkAllAsReadButton(onMarkAllAsRead)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SwipeableArticleItem(
    article: ArticleEntity,
    onToggleSave: () -> Unit,
    onShare: () -> Unit,
    onArticleClick: (String, Boolean) -> Unit,
    isReadLaterView: Boolean
) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = {
            when (it) {
                SwipeToDismissBoxValue.StartToEnd -> {
                    onShare()
                    false
                }
                SwipeToDismissBoxValue.EndToStart -> {
                    onToggleSave()
                    false
                }
                else -> false
            }
        }
    )

    SwipeToDismissBox(
        state = dismissState,
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)),
        backgroundContent = {
            val direction = dismissState.dismissDirection ?: return@SwipeToDismissBox
            
            // Standardized actions:
            // StartToEnd (Right) -> Share
            // EndToStart (Left) -> Toggle Save (Delete in Read Later)
            
            val isSharing = direction == SwipeToDismissBoxValue.StartToEnd
            
            val color = when {
                isSharing -> Color(0xFF4CAF50) // Share (Green)
                isReadLaterView -> Color(0xFFE53935) // Delete from Read Later (Red)
                else -> Color(0xFFFFD600) // Save to Read Later (Yellow)
            }
            
            val alignment = if (direction == SwipeToDismissBoxValue.StartToEnd) Alignment.CenterStart else Alignment.CenterEnd
            
            val icon = when {
                isSharing -> Icons.Default.Share
                isReadLaterView -> Icons.Default.BookmarkRemove
                article.isSaved -> Icons.Default.Bookmark
                else -> Icons.Default.BookmarkBorder
            }
            
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(color)
                    .padding(horizontal = 24.dp),
                contentAlignment = alignment
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (isSharing || isReadLaterView) Color.White else Color.Black,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    ) {
        ArticleItem(article, onArticleClick)
    }
}

@Composable
fun MarkAllAsReadButton(onMarkAllAsRead: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 32.dp),
        contentAlignment = Alignment.Center
    ) {
        Button(
            onClick = onMarkAllAsRead,
            shape = RoundedCornerShape(12.dp),
            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            )
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.DoneAll, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text("Mark everything as read")
            }
        }
    }
}

@Composable
fun ArticleItem(article: ArticleEntity, onClick: (String, Boolean) -> Unit) {
    Card(
        onClick = { onClick(article.link, article.isRead) },
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .alpha(if (article.isRead) 0.5f else 1f)
        ) {
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
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = article.title,
                        style = MaterialTheme.typography.titleMedium,
                        color = if (article.isRead) Color.Gray else Color.Unspecified,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    if (article.isSaved) {
                        Icon(
                            Icons.Default.Bookmark, 
                            contentDescription = "Saved",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp).padding(start = 4.dp)
                        )
                    }
                }
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
fun AddSourceDialog(onDismiss: () -> Unit, onAdd: (String, String?) -> Unit) {
    var url by remember { mutableStateOf("") }
    var title by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add RSS Feed") },
        text = {
            Column {
                TextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title (Optional)") },
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(8.dp))
                TextField(
                    value = url,
                    onValueChange = { url = it },
                    label = { Text("URL") },
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(onClick = { if (url.isNotBlank()) onAdd(url, if(title.isBlank()) null else title) }) {
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
