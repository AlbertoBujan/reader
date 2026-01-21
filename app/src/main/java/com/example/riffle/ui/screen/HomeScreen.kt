package com.example.riffle.ui.screen

import android.content.ClipData
import android.content.ClipDescription
import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
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
import androidx.compose.material.icons.filled.Info
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
import androidx.compose.material3.TextButton
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.ImeAction
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
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.draganddrop.DragAndDropEvent
import androidx.compose.ui.draganddrop.DragAndDropTarget
import androidx.compose.ui.draganddrop.DragAndDropTransferData
import androidx.compose.ui.draganddrop.mimeTypes
import androidx.compose.ui.draganddrop.toAndroidDragEvent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.example.riffle.data.local.entity.ArticleEntity
import com.example.riffle.data.local.entity.SourceEntity
import com.example.riffle.ui.viewmodel.DiscoveredFeed
import com.example.riffle.ui.viewmodel.FeedUiState
import com.example.riffle.ui.viewmodel.MainViewModel
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.res.stringResource
import androidx.core.os.LocaleListCompat
import androidx.appcompat.app.AppCompatDelegate
import com.example.riffle.R
import kotlin.math.absoluteValue

fun Modifier.shimmerEffect(): Modifier = composed {
    var size by remember { mutableStateOf(IntSize.Zero) }
    val transition = rememberInfiniteTransition(label = "shimmer")
    val startOffsetX by transition.animateFloat(
        initialValue = -2 * size.width.toFloat(),
        targetValue = 2 * size.width.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(1000)
        ),
        label = "shimmer_offset"
    )

    val isDark = androidx.compose.foundation.isSystemInDarkTheme()
    // Aún más oscuros y con menos contraste para que no sea "shiny" agresivo
    val baseColor = if (isDark) Color(0xFF1A1B1E) else Color(0xFFB8B5B5) 
    val highlightColor = if (isDark) Color(0xFF2E3035) else Color(0xFF8F8B8B)

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
fun SkeletonArticleItem() {
    Card(
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .shimmerEffect()
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(20.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .shimmerEffect()
                )
                Spacer(modifier = Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.7f)
                        .height(20.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .shimmerEffect()
                )
                Spacer(modifier = Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.4f)
                        .height(14.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .shimmerEffect()
                )
            }
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(
    viewModel: MainViewModel = hiltViewModel(),
    onArticleClick: (String, Boolean) -> Unit,
    onNavigateToFeedSearch: () -> Unit
) {
    val articleSearchQuery by viewModel.articleSearchQuery.collectAsState()
    var isSearchActive by remember { mutableStateOf(false) }
    val focusRequester = remember { androidx.compose.ui.focus.FocusRequester() }

    // Restore missing state variables
    val uiState by viewModel.uiState.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val sources by viewModel.sources.collectAsState()
    val folders by viewModel.folders.collectAsState()
    val selectedSource by viewModel.selectedSource.collectAsState()
    val markAsReadOnScroll by viewModel.markAsReadOnScroll.collectAsState()
    val unreadCounts by viewModel.unreadCounts.collectAsState()
    val isDarkMode by viewModel.isDarkMode.collectAsState()
    val language by viewModel.language.collectAsState()

    val savedCount by viewModel.savedCount.collectAsState()
    val geminiApiKey by viewModel.geminiApiKey.collectAsState()
    val modelStatuses by viewModel.modelStatuses.collectAsState()
    
    var showFolderDialog by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }
    var renamingSource by remember { mutableStateOf<SourceEntity?>(null) }
    var renamingFolder by remember { mutableStateOf<String?>(null) }

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current



    LaunchedEffect(isSearchActive) {
        if (isSearchActive) {
            focusRequester.requestFocus()
        }
    }

    // Scroll al inicio cuando se busca
    LaunchedEffect(articleSearchQuery) {
        if (articleSearchQuery.isNotEmpty()) {
            listState.scrollToItem(0)
        }
    }

    // Manejo del botón atrás de Android: Si busca, cerrar búsqueda
    BackHandler(enabled = isSearchActive) {
         isSearchActive = false
         viewModel.setArticleSearchQuery("")
    }

    // Manejo del botón atrás de Android (Drawer)
    BackHandler(enabled = drawerState.isOpen) {
        scope.launch { drawerState.close() }
    }
    
    // Si el drawer está cerrado y NO busca, el botón atrás abrirá el drawer la primera vez
    if (drawerState.isClosed && !isSearchActive) {
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
                            Text(text = stringResource(R.string.nav_subscriptions), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                            Row {
                                IconButton(onClick = { showFolderDialog = true }) {
                                    Icon(Icons.Filled.CreateNewFolder, contentDescription = stringResource(R.string.nav_new_folder))
                                }
                            }
                        }
                        
                        LazyColumn(modifier = Modifier.weight(1f)) {
                            item(key = "all_feeds") {
                                val totalUnread = unreadCounts.values.sum()
                                NavigationDrawerItem(
                                    icon = { Icon(Icons.Filled.RssFeed, null) },
                                    label = { 
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(stringResource(R.string.nav_all_feeds))
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
                                        viewModel.sync()
                                        scope.launch { 
                                            listState.scrollToItem(0)
                                            drawerState.close() 
                                        }
                                    },
                                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                                )
                            }

                            item(key = "read_later") {
                                NavigationDrawerItem(
                                    icon = { Icon(Icons.Default.Bookmark, null) },
                                    label = { 
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(stringResource(R.string.nav_read_later))
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
                                        scope.launch { 
                                            listState.scrollToItem(0)
                                            drawerState.close() 
                                        }
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

                            val orphanSources = sources.filter { it.folderName == null }
                            if (orphanSources.isNotEmpty()) {
                                item(key = "uncategorized_header") {
                                    Text(
                                        text = stringResource(R.string.nav_uncategorized),
                                        style = MaterialTheme.typography.labelMedium,
                                        modifier = Modifier.padding(start = 28.dp, top = 16.dp, bottom = 8.dp),
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                items(orphanSources, key = { it.url }) { source ->
                                    SwipeableSourceItem(
                                        source = source,
                                        isSelected = selectedSource == source.url,
                                        onClick = {
                                            viewModel.selectSource(source.url)
                                            scope.launch { drawerState.close() }
                                        },
                                        onDelete = { viewModel.deleteSource(source.url) },
                                        onRename = { renamingSource = source },
                                        modifier = Modifier.animateItem()
                                    )
                                }
                            }
                        }
                        
                        HorizontalDivider()
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(NavigationDrawerItemDefaults.ItemPadding)
                                .clickable {
                                    showSettingsDialog = true
                                    scope.launch { drawerState.close() }
                                }
                                .padding(vertical = 12.dp, horizontal = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Settings, contentDescription = stringResource(R.string.nav_settings), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = stringResource(R.string.app_name) + " ${com.example.riffle.BuildConfig.VERSION_NAME}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(Modifier.height(12.dp))
                    }
                    FloatingActionButton(
                        onClick = onNavigateToFeedSearch,
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(16.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = stringResource(R.string.dialog_add))
                    }
                }
            }
        }
    ) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            topBar = {
                if (isSearchActive) {
                    TopAppBar(
                        title = {
                            TextField(
                                value = articleSearchQuery,
                                onValueChange = { viewModel.setArticleSearchQuery(it) },
                                placeholder = { Text(stringResource(R.string.search_articles_placeholder)) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .focusRequester(focusRequester),
                                singleLine = true,
                                colors = androidx.compose.material3.TextFieldDefaults.colors(
                                    focusedContainerColor = Color.Transparent,
                                    unfocusedContainerColor = Color.Transparent,
                                    focusedIndicatorColor = Color.Transparent,
                                    unfocusedIndicatorColor = Color.Transparent
                                ),
                                trailingIcon = {
                                    if (articleSearchQuery.isNotEmpty()) {
                                        IconButton(onClick = { viewModel.setArticleSearchQuery("") }) {
                                            Icon(Icons.Default.Delete, contentDescription = "Clear") // Using Delete icon as Clear X
                                        }
                                    }
                                }
                            )
                        },
                        navigationIcon = {
                            IconButton(onClick = { 
                                isSearchActive = false
                                viewModel.setArticleSearchQuery("")
                            }) {
                                Icon(Icons.Default.ChevronRight, contentDescription = stringResource(R.string.article_back), modifier = Modifier.rotate(180f))
                            }
                        }
                    )
                } else {
                    TopAppBar(
                        title = { 
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    when {
                                        selectedSource == "saved" -> stringResource(R.string.nav_read_later)
                                        selectedSource == null -> stringResource(R.string.app_name)
                                        selectedSource!!.startsWith("folder:") -> stringResource(R.string.folder_prefix, selectedSource!!.removePrefix("folder:"))
                                        else -> sources.find { it.url == selectedSource }?.title ?: stringResource(R.string.feed_filtered)
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
                                if (isRefreshing) {
                                    Spacer(Modifier.width(8.dp))
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        strokeWidth = 2.dp,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                } else if (currentUnread > 0) {
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
                                Icon(Icons.Default.Menu, contentDescription = stringResource(R.string.nav_menu))
                            }
                        },
                        actions = {
                            IconButton(onClick = { isSearchActive = true }) {
                                Icon(Icons.Default.Search, contentDescription = "Search Articles")
                            }
                        }
                    )
                }
            }
        ) { padding ->
            val pullState = rememberPullToRefreshState()
            PullToRefreshBox(
                isRefreshing = isRefreshing,
                onRefresh = { viewModel.sync() },
                state = pullState,
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize(),
                indicator = {
                    if (!isRefreshing) {
                        PullToRefreshDefaults.Indicator(
                            state = pullState,
                            isRefreshing = false,
                            modifier = Modifier.align(Alignment.TopCenter)
                        )
                    }
                }
            ) {
                when (val state = uiState) {
                    is FeedUiState.Loading -> {
                        LazyColumn(
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(6) {
                                SkeletonArticleItem()
                            }
                        }
                    }
                    is FeedUiState.Success -> {
                        // Scroll automático al inicio tras carga
                        var wasRefreshing by remember { mutableStateOf(false) }
                        LaunchedEffect(isRefreshing) {
                            if (wasRefreshing && !isRefreshing && state.articles.isNotEmpty()) {
                                listState.animateScrollToItem(0)
                            }
                            wasRefreshing = isRefreshing
                        }
                        
                        // Startup ONLY scroll
                        LaunchedEffect(isRefreshing, state) {
                            if (!viewModel.hasPerformedStartupScroll && !isRefreshing && state.articles.isNotEmpty()) {
                                listState.scrollToItem(0)
                                viewModel.markStartupScrollPerformed()
                            }
                        }

                        key(selectedSource) {
                            ArticleList(
                                articles = state.articles,
                                sources = sources,
                                listState = listState,
                                markAsReadOnScroll = markAsReadOnScroll,
                                isRefreshing = isRefreshing,
                                onMarkAsRead = { viewModel.markAsRead(it) },
                                onToggleSave = { link, isSaved -> 
                                    viewModel.toggleSaveArticle(link, isSaved)
                                    if (selectedSource == "saved" && isSaved) {
                                        viewModel.markAsRead(link)
                                    }
                                    val message = if (isSaved) context.getString(R.string.msg_removed_read_later) else context.getString(R.string.msg_added_read_later)
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
                                isReadLaterView = selectedSource == "saved",
                                onLoadMore = { viewModel.loadMore() }
                            )
                        }
                    }
                    is FeedUiState.Error -> {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(text = "Error: ${state.message}", color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
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



    val exportOpmlLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/x-opml"),
        onResult = { uri ->
            uri?.let { viewModel.exportOpml(it) }
        }
    )

    val importOpmlLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
        onResult = { uri ->
            uri?.let { viewModel.importOpml(it) }
        }
    )

            if (showSettingsDialog) {
                SettingsDialog(
                    onDismiss = { showSettingsDialog = false },
                    markAsReadOnScroll = markAsReadOnScroll,
                    onToggleMarkAsReadOnScroll = { viewModel.toggleMarkAsReadOnScroll(it) },
                    isDarkMode = isDarkMode,
                    onToggleDarkMode = { viewModel.toggleDarkMode(it) },
                    geminiApiKey = geminiApiKey,
                    onApiKeyChange = { viewModel.updateGeminiApiKey(it) },
                    language = language,
                    onLanguageChange = { code ->
                        viewModel.setLanguage(code)
                        val localeList = if (code == "system") LocaleListCompat.getEmptyLocaleList() else LocaleListCompat.forLanguageTags(code)
                        AppCompatDelegate.setApplicationLocales(localeList)
                    },
                    modelStatuses = modelStatuses,
                    syncInterval = viewModel.syncInterval.collectAsState().value,
                    onSyncIntervalChange = { viewModel.setSyncInterval(it) },
                    onImportOpml = { importOpmlLauncher.launch(arrayOf("text/xml", "application/xml", "text/x-opml")) },
                    onExportOpml = { exportOpmlLauncher.launch("riffle_backup.opml") }
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
        title = { Text(stringResource(R.string.dialog_rename_source_title)) },
        text = {
            TextField(value = text, onValueChange = { text = it }, label = { Text(stringResource(R.string.dialog_rename_source_label)) }, singleLine = true)
        },
        confirmButton = {
            Button(onClick = { if(text.isNotBlank()) onConfirm(text) }) { Text(stringResource(R.string.dialog_rename)) }
        },
        dismissButton = {
            Button(onClick = onDismiss) { Text(stringResource(R.string.dialog_cancel)) }
        }
    )
}

@Composable
fun RenameFolderDialog(initialName: String, onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var text by remember { mutableStateOf(initialName) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.dialog_rename_folder_title)) },
        text = {
            TextField(value = text, onValueChange = { text = it }, label = { Text(stringResource(R.string.dialog_rename_folder_label)) }, singleLine = true)
        },
        confirmButton = {
            Button(onClick = { if(text.isNotBlank()) onConfirm(text) }) { Text(stringResource(R.string.dialog_rename)) }
        },
        dismissButton = {
            Button(onClick = onDismiss) { Text(stringResource(R.string.dialog_cancel)) }
        }
    )
}

@Composable
fun SettingsDialog(
    onDismiss: () -> Unit,
    markAsReadOnScroll: Boolean,
    onToggleMarkAsReadOnScroll: (Boolean) -> Unit,
    isDarkMode: Boolean,
    onToggleDarkMode: (Boolean) -> Unit,
    geminiApiKey: String,
    onApiKeyChange: (String) -> Unit,
    language: String,
    onLanguageChange: (String) -> Unit,

    modelStatuses: Map<String, String>,
    syncInterval: Long,
    onSyncIntervalChange: (Long) -> Unit,
    onImportOpml: () -> Unit,
    onExportOpml: () -> Unit
) {
    var expandedLanguage by remember { mutableStateOf(false) }
    var expandedInterval by remember { mutableStateOf(false) }
    var showStatsDialog by remember { mutableStateOf(false) }

    if (showStatsDialog) {
        ModelStatsDialog(
            modelStatuses = modelStatuses,
            onDismiss = { showStatsDialog = false }
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.settings_title)) },
        text = {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(stringResource(R.string.settings_dark_mode))
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
                    Text(stringResource(R.string.settings_mark_read_scroll))
                    Switch(
                        checked = markAsReadOnScroll,
                        onCheckedChange = onToggleMarkAsReadOnScroll
                    )
                }
                HorizontalDivider()
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                     Text(stringResource(R.string.settings_language), modifier = Modifier.weight(1f))
                     Box {
                          Text(
                              text = when(language) {
                                  "en" -> stringResource(R.string.settings_language_en)
                                  "es" -> stringResource(R.string.settings_language_es)
                                  else -> stringResource(R.string.settings_language_system)
                              },
                              color = MaterialTheme.colorScheme.primary,
                              modifier = Modifier.clickable { expandedLanguage = true }.padding(8.dp)
                          )
                          DropdownMenu(expanded = expandedLanguage, onDismissRequest = { expandedLanguage = false }) {
                              DropdownMenuItem(
                                  text = { Text(stringResource(R.string.settings_language_system)) },
                                  onClick = { onLanguageChange("system"); expandedLanguage = false }
                              )
                              DropdownMenuItem(
                                  text = { Text(stringResource(R.string.settings_language_en)) },
                                  onClick = { onLanguageChange("en"); expandedLanguage = false }
                              )
                              DropdownMenuItem(
                                  text = { Text(stringResource(R.string.settings_language_es)) },
                                  onClick = { onLanguageChange("es"); expandedLanguage = false }
                              )
                          }
                     }
                }
                HorizontalDivider()
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                     Text(stringResource(R.string.settings_sync_interval), modifier = Modifier.weight(1f))
                     Box {
                          val intervalString = if (syncInterval == 1L) {
                               stringResource(R.string.settings_sync_interval_hour, syncInterval)
                          } else {
                               stringResource(R.string.settings_sync_interval_hours, syncInterval)
                          }
                          Text(
                              text = intervalString,
                              color = MaterialTheme.colorScheme.primary,
                              modifier = Modifier.clickable { expandedInterval = true }.padding(8.dp)
                          )
                          DropdownMenu(expanded = expandedInterval, onDismissRequest = { expandedInterval = false }) {
                              listOf(1L, 6L, 12L, 24L).forEach { hours ->
                                  DropdownMenuItem(
                                      text = {
                                           val text = if (hours == 1L) {
                                               stringResource(R.string.settings_sync_interval_hour, hours)
                                           } else {
                                               stringResource(R.string.settings_sync_interval_hours, hours)
                                           }
                                           Text(text) 
                                      },
                                      onClick = { onSyncIntervalChange(hours); expandedInterval = false }
                                  )
                              }
                          }
                     }

                }
                HorizontalDivider()
                
                // OPML Section
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Backup", 
                    style = MaterialTheme.typography.labelLarge, 
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(8.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onImportOpml,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.CreateNewFolder, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        // Shorten text if needed or keep as is. Using existing strings
                        Text(stringResource(R.string.settings_import_opml), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    
                    OutlinedButton(
                        onClick = onExportOpml,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                         // Shorten text if needed or keep as is. Using existing strings
                        Text(stringResource(R.string.settings_export_opml), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider()

                Spacer(modifier = Modifier.height(16.dp))
                
                // AI Settings / Integrations
                Spacer(modifier = Modifier.height(16.dp))
                // Section Header
                Text(
                     text = stringResource(R.string.settings_section_ai),
                     style = MaterialTheme.typography.labelLarge,
                     color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(8.dp))
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.settings_gemini_key), style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                    IconButton(onClick = { showStatsDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.Info, // Fallback icon, acts as stats/info
                            contentDescription = stringResource(R.string.ai_stats_title),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                
                // Modern Outlined/Filled Input with visual transformation
                OutlinedTextField(
                    value = geminiApiKey,
                    onValueChange = onApiKeyChange,
                    label = { Text(stringResource(R.string.settings_gemini_key_hint)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = MaterialTheme.typography.bodySmall,
                    visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Password),
                    shape = RoundedCornerShape(12.dp)
                )
            }
        },
        confirmButton = {
            Button(onClick = { onDismiss() }) { Text(stringResource(R.string.action_done)) }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SwipeActionBackground(dismissState: SwipeToDismissBoxState, shape: Shape, isReadLaterView: Boolean, isSaved: Boolean) {
    val direction = dismissState.dismissDirection ?: return
    val isMoving = dismissState.currentValue != dismissState.targetValue || dismissState.progress != 0f

    if (!isMoving) return

    val isShare = direction == SwipeToDismissBoxValue.StartToEnd
    val color = when {
        isShare -> Color(0xFF4CAF50)
        isReadLaterView -> Color(0xFFE53935)
        else -> Color(0xFFFFD600)
    }
    val alignment = if (isShare) Alignment.CenterStart else Alignment.CenterEnd
    val icon = when {
        isShare -> Icons.Default.Share
        isReadLaterView -> Icons.Default.BookmarkRemove
        isSaved -> Icons.Default.Bookmark
        else -> Icons.Default.BookmarkBorder
    }

    // Haptic Feedback Logic
    val haptic = LocalHapticFeedback.current
    LaunchedEffect(dismissState.targetValue) {
        if (dismissState.targetValue != SwipeToDismissBoxValue.Settled) {
             haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        }
    }

    // Shine Effect Logic
    val isThresholdReached = dismissState.targetValue != SwipeToDismissBoxValue.Settled
    val shineAlpha = if (isThresholdReached) 0.3f else 0f
    
    val transition = rememberInfiniteTransition(label = "shine")
    val translateAnim by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1000, easing = androidx.compose.animation.core.LinearEasing),
        ),
        label = "shine_translation"
    )

    val shineBrush = Brush.linearGradient(
        colors = listOf(
            Color.White.copy(alpha = 0f),
            Color.White.copy(alpha = 0.5f),
            Color.White.copy(alpha = 0f)
        ),
        start = Offset(translateAnim, translateAnim),
        end = Offset(translateAnim + 100f, translateAnim + 100f)
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .clip(shape)
            .background(color)
    ) {
        // Shine Overlay
        if (isThresholdReached) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(shineBrush)
            )
        }
        
        // Icon
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            contentAlignment = alignment
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isShare || isReadLaterView) Color.White else Color.Black,
                modifier = Modifier.size(24.dp)
            )
        }
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
        backgroundContent = { 
            val direction = dismissState.dismissDirection ?: return@SwipeToDismissBox
            val color = if (direction == SwipeToDismissBoxValue.EndToStart) Color(0xFFE53935) else Color(0xFF1E88E5)
            val alignment = if (direction == SwipeToDismissBoxValue.EndToStart) Alignment.CenterEnd else Alignment.CenterStart
            val icon = if (direction == SwipeToDismissBoxValue.EndToStart) Icons.Default.Delete else Icons.Default.Edit

            // Haptic Feedback
            val haptic = LocalHapticFeedback.current
            LaunchedEffect(dismissState.targetValue) {
                if (dismissState.targetValue != SwipeToDismissBoxValue.Settled) {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                }
            }
            
            // Shine Effect (Simplified copy for non-article items)
            val isThresholdReached = dismissState.targetValue != SwipeToDismissBoxValue.Settled
            val transition = rememberInfiniteTransition(label = "shine_simple")
            val translateAnim by transition.animateFloat(
                initialValue = 0f,
                targetValue = 1000f,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = 1000, easing = androidx.compose.animation.core.LinearEasing),
                ),
                label = "shine_translation_simple"
            )
            val shineBrush = Brush.linearGradient(
                colors = listOf(Color.White.copy(alpha = 0f), Color.White.copy(alpha = 0.5f), Color.White.copy(alpha = 0f)),
                start = Offset(translateAnim, translateAnim),
                end = Offset(translateAnim + 100f, translateAnim + 100f)
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(shape)
                    .background(color)
            ) {
                 if (isThresholdReached) {
                    Box(modifier = Modifier.fillMaxSize().background(shineBrush))
                }
                Box(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp),
                    contentAlignment = alignment
                ) {
                    Icon(icon, null, tint = Color.White)
                }
            }
        }
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
                    .background(
                        if (isOver) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f) 
                        else Color.Transparent
                    )
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
        
        AnimatedVisibility(
            visible = isExpanded,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Column(modifier = Modifier.animateContentSize()) {
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
}

@Composable
fun SwipeableSourceItem(
    source: SourceEntity,
    isSelected: Boolean,
    onClick: (String) -> Unit,
    onDelete: () -> Unit,

    onRename: () -> Unit,
    modifier: Modifier = Modifier
) {
    SwipeableItem(
        onDelete = onDelete,
        onEdit = onRename,
        modifier = modifier.padding(NavigationDrawerItemDefaults.ItemPadding),
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
    val haptic = LocalHapticFeedback.current

    Surface(
        onClick = { onClick(source.url) },
        shape = CircleShape,
        color = backgroundColor,
        contentColor = contentColor,
        modifier = Modifier
            .fillMaxWidth()
            .dragAndDropSource(
                drawDragDecoration = {
                    drawRoundRect(
                         color = Color(0xFFAAAAAA).copy(alpha = 0.5f),
                         size = size,
                         cornerRadius = androidx.compose.ui.geometry.CornerRadius(size.height / 2)
                    )
                }
            ) {
                detectDragGesturesAfterLongPress(
                    onDragStart = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
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
            horizontalArrangement = Arrangement.Start
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
        title = { Text(stringResource(R.string.dialog_add_folder_title)) },
        text = {
            TextField(value = text, onValueChange = { text = it }, label = { Text(stringResource(R.string.dialog_add_folder_label)) }, singleLine = true)
        },
        confirmButton = {
            Button(onClick = { if(text.isNotBlank()) onConfirm(text) }) { Text(stringResource(R.string.dialog_create)) }
        },
        dismissButton = {
            Button(onClick = onDismiss) { Text(stringResource(R.string.dialog_cancel)) }
        }
    )
}

@Composable
fun ArticleList(
    articles: List<ArticleEntity>,
    sources: List<SourceEntity>,
    listState: LazyListState,
    markAsReadOnScroll: Boolean,
    isRefreshing: Boolean,
    onMarkAsRead: (String) -> Unit,
    onToggleSave: (String, Boolean) -> Unit,
    onShare: (ArticleEntity) -> Unit,
    onArticleClick: (String, Boolean) -> Unit,
    isReadLaterView: Boolean = false,
    onLoadMore: () -> Unit = {}
) {
    val currentArticles by rememberUpdatedState(articles)

    LaunchedEffect(listState) {
        snapshotFlow { listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index }
            .distinctUntilChanged()
            .collect { lastIndex ->
                if (lastIndex != null && lastIndex >= currentArticles.size - 5) {
                    onLoadMore()
                }
            }
    }

    val screenHeight = LocalConfiguration.current.screenHeightDp.dp
    
    if (markAsReadOnScroll && !isReadLaterView) {
        var lastProcessedIndex by remember { mutableStateOf(listState.firstVisibleItemIndex) }

        LaunchedEffect(articles) {
            lastProcessedIndex = listState.firstVisibleItemIndex
        }

        LaunchedEffect(listState, isRefreshing) {
            if (isRefreshing) return@LaunchedEffect
            
            snapshotFlow { listState.firstVisibleItemIndex }
                .distinctUntilChanged()
                .collect { currentFirstIndex ->
                    if (listState.isScrollInProgress && currentFirstIndex > lastProcessedIndex) {
                        for (i in lastProcessedIndex until currentFirstIndex) {
                            if (i < currentArticles.size) {
                                val article = currentArticles[i]
                                if (!article.isRead) onMarkAsRead(article.link)
                            }
                        }
                    }
                    lastProcessedIndex = currentFirstIndex
                }
        }
    }

    LazyColumn(
        state = listState,
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        items(articles, key = { it.link }) { article ->
            val sourceName = sources.find { it.url == article.sourceUrl }?.title
            SwipeableArticleItem(
                article = article,
                sourceName = sourceName,
                onToggleSave = { onToggleSave(article.link, article.isSaved) },
                onShare = { onShare(article) },
                onArticleClick = onArticleClick,
                isReadLaterView = isReadLaterView,
                modifier = Modifier.animateItem()
            )
        }
        
        if (articles.isNotEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 32.dp, bottom = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.list_end_message),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }
            item {
                Spacer(modifier = Modifier.height(screenHeight - 60.dp))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SwipeableArticleItem(
    article: ArticleEntity,
    sourceName: String?,
    onToggleSave: () -> Unit,
    onShare: () -> Unit,
    onArticleClick: (String, Boolean) -> Unit,
    isReadLaterView: Boolean,
    modifier: Modifier = Modifier
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
        modifier = modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)),
        backgroundContent = {
            SwipeActionBackground(
                dismissState = dismissState, 
                shape = RoundedCornerShape(12.dp),
                isReadLaterView = isReadLaterView,
                isSaved = article.isSaved
            )
        }
    ) {
        ArticleItem(article, sourceName, onArticleClick, isReadLaterView)
    }
}

@Composable
fun ArticleItem(article: ArticleEntity, sourceName: String?, onClick: (String, Boolean) -> Unit, isReadLaterView: Boolean = false) {
    Card(
        onClick = { onClick(article.link, article.isRead) },
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .alpha(if (article.isRead && !isReadLaterView) 0.5f else 1f)
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = article.title,
                        style = MaterialTheme.typography.titleMedium,
                        color = if (article.isRead && !isReadLaterView) Color.Gray else Color.Unspecified,
                        modifier = Modifier.weight(1f)
                    )
                    if (article.isSaved) {
                        Icon(
                            Icons.Default.Bookmark, 
                            contentDescription = stringResource(R.string.action_saved),
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp).padding(start = 4.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${formatDate(article.pubDate)} • ${sourceName ?: article.sourceUrl}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}



private fun formatDate(timestamp: Long): String {
    val sdf = SimpleDateFormat("dd MMM HH:mm", Locale.getDefault())
    return sdf.format(Date(timestamp))
}


@Composable
fun ModelStatsDialog(
    modelStatuses: Map<String, String>,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.ai_stats_title)) },
        text = {
            Column {
                val models = listOf("gemini-2.5-flash", "gemini-2.5-flash-lite", "gemini-3-flash")
                models.forEach { model ->
                    val status = modelStatuses[model]
                    val isExhausted = status == "exhausted"
                    
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(model, style = MaterialTheme.typography.bodyMedium)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (isExhausted) {
                                Text(
                                    text = stringResource(R.string.status_exhausted),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.error
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                            }
                            
                            Canvas(modifier = Modifier.size(8.dp)) {
                                drawCircle(
                                    color = if (isExhausted) Color.Red else Color(0xFF4CAF50) // Green for available too
                                )
                            }
                        }
                    }
                    HorizontalDivider()
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text(stringResource(R.string.dialog_close))
            }
        }
    )
}
