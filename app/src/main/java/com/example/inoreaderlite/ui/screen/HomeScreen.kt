package com.example.inoreaderlite.ui.screen

import android.content.ClipData
import android.content.ClipDescription
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
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.RssFeed
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
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
    val isDarkMode by viewModel.isDarkMode.collectAsState()
    val unreadCounts by viewModel.unreadCounts.collectAsState()
    
    var showAddDialog by remember { mutableStateOf(false) }
    var showFolderDialog by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }
    var renamingSource by remember { mutableStateOf<SourceEntity?>(null) }
    var renamingFolder by remember { mutableStateOf<String?>(null) }

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

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
                                    onDeleteSource = { viewModel.deleteSource(it) },
                                    onRenameSource = { renamingSource = it },
                                    onRenameFolder = { renamingFolder = it },
                                    onDeleteFolder = { viewModel.deleteFolder(it) },
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
                                        SwipeableSourceItem(
                                            source = source,
                                            isSelected = selectedSource == source.url,
                                            onClick = {
                                                viewModel.selectSource(source.url)
                                                scope.launch { drawerState.close() }
                                            },
                                            onDelete = { viewModel.deleteSource(it) },
                                            onRename = { renamingSource = it }
                                        )
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
                                    selectedSource == null -> "Inoreader Lite"
                                    selectedSource!!.startsWith("folder:") -> "Folder: ${selectedSource!!.removePrefix("folder:")}"
                                    else -> sources.find { it.url == selectedSource }?.title ?: "Filtered Feed"
                                }
                            )
                            val currentUnread = when {
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
            Button(onClick = onDismiss) { Text("Close") }
        }
    )
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
    var showMenu by remember { mutableStateOf(false) }
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
        Box {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .combinedClickable(
                        onClick = { 
                            isExpanded = !isExpanded
                            onFolderClick(folderName) 
                        },
                        onLongClick = { showMenu = true }
                    )
                    .padding(horizontal = 16.dp, vertical = 8.dp),
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
            
            DropdownMenu(
                expanded = showMenu,
                onDismissRequest = { showMenu = false }
            ) {
                DropdownMenuItem(
                    text = { Text("Rename Folder") },
                    leadingIcon = { Icon(Icons.Default.Edit, null) },
                    onClick = {
                        showMenu = false
                        onRenameFolder(folderName)
                    }
                )
                DropdownMenuItem(
                    text = { Text("Delete Folder & Feeds") },
                    leadingIcon = { Icon(Icons.Default.Delete, null) },
                    onClick = {
                        showMenu = false
                        onDeleteFolder(folderName)
                    }
                )
            }
        }
        
        if (isExpanded) {
            sources.forEach { source ->
                SwipeableSourceItem(
                    source = source,
                    isSelected = selectedSource == source.url,
                    onClick = onSourceClick,
                    onDelete = onDeleteSource,
                    onRename = onRenameSource
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SwipeableSourceItem(
    source: SourceEntity,
    isSelected: Boolean,
    onClick: (String) -> Unit,
    onDelete: (String) -> Unit,
    onRename: (SourceEntity) -> Unit
) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = {
            when (it) {
                SwipeToDismissBoxValue.EndToStart -> {
                    onDelete(source.url)
                    true
                }
                SwipeToDismissBoxValue.StartToEnd -> {
                    onRename(source)
                    false // Snap back
                }
                else -> false
            }
        }
    )

    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = true,
        enableDismissFromEndToStart = true,
        backgroundContent = {
            val direction = dismissState.dismissDirection
            when (direction) {
                SwipeToDismissBoxValue.EndToStart -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(NavigationDrawerItemDefaults.ItemPadding)
                            .background(Color.Red, CircleShape)
                            .padding(horizontal = 20.dp),
                        contentAlignment = Alignment.CenterEnd
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.White)
                    }
                }
                SwipeToDismissBoxValue.StartToEnd -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(NavigationDrawerItemDefaults.ItemPadding)
                            .background(MaterialTheme.colorScheme.primary, CircleShape)
                            .padding(horizontal = 20.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = "Rename", tint = Color.White)
                    }
                }
                else -> {}
            }
        }
    ) {
        SourceDrawerItem(source, isSelected, onClick)
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
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
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
            ArticleItem(article, onArticleClick)
        }
    }
}

@Composable
fun ArticleItem(article: ArticleEntity, onClick: (String, Boolean) -> Unit) {
    Card(
        onClick = { onClick(article.link, article.isRead) },
        modifier = Modifier
            .fillMaxWidth()
            .alpha(if (article.isRead) 0.5f else 1f)
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
