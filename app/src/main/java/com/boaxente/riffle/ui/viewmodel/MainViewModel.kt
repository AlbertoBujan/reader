package com.boaxente.riffle.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.boaxente.riffle.data.local.PreferencesManager
import com.boaxente.riffle.data.local.dao.FeedDao
import com.boaxente.riffle.data.local.entity.ArticleEntity
import com.boaxente.riffle.data.local.entity.ArticleWithSource
import com.boaxente.riffle.data.local.entity.FolderEntity
import com.boaxente.riffle.data.local.entity.SourceEntity
import com.boaxente.riffle.data.remote.ClearbitService
import com.boaxente.riffle.data.remote.FeedSearchService
import com.boaxente.riffle.domain.usecase.AddSourceUseCase
import com.boaxente.riffle.domain.usecase.GetAllSourcesUseCase
import com.boaxente.riffle.domain.usecase.GetArticlesUseCase
import com.boaxente.riffle.domain.usecase.MarkArticleReadUseCase
import com.boaxente.riffle.domain.usecase.SyncFeedsUseCase
import com.boaxente.riffle.util.RiffleLogger
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import java.security.cert.X509Certificate
import javax.inject.Inject
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSocketFactory
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager
import com.google.ai.client.generativeai.GenerativeModel
import com.boaxente.riffle.util.extractFirstImageUrl

sealed interface FeedUiState {
    data object Loading : FeedUiState
    data class Success(val articles: List<ArticleEntity>) : FeedUiState
    data class Error(val message: String) : FeedUiState
}

sealed interface SourceAdditionState {
    data object Idle : SourceAdditionState
    data class Loading(val targetUrl: String) : SourceAdditionState
    data object Success : SourceAdditionState
    data class Error(val title: String, val message: String) : SourceAdditionState
}

data class DiscoveredFeed(
    val title: String,
    val url: String,
    val iconUrl: String? = null,
    val siteName: String? = null
)

enum class FeedHealth {
    GOOD, // Green (<= 5 days)
    WARNING, // Yellow (> 5 days and <= 10 days)
    BAD, // Red (> 10 days and <= 50 days)
    DEAD, // Black (> 50 days)
    UNKNOWN // Gray/Empty
}

@HiltViewModel
class MainViewModel @Inject constructor(
    private val getArticlesUseCase: GetArticlesUseCase,
    private val addSourceUseCase: AddSourceUseCase,
    private val syncFeedsUseCase: SyncFeedsUseCase,
    private val markArticleReadUseCase: MarkArticleReadUseCase,
    private val feedDao: FeedDao,
    private val feedRepository: com.boaxente.riffle.domain.repository.FeedRepository,
    private val preferencesManager: PreferencesManager,
    private val firestoreHelper: com.boaxente.riffle.data.remote.FirestoreHelper,
    private val feedSearchService: FeedSearchService,
    private val clearbitService: ClearbitService,
    private val feedService: com.boaxente.riffle.data.remote.FeedService,
    private val rssParser: com.boaxente.riffle.data.remote.RssParser,

    private val authManager: com.boaxente.riffle.data.remote.AuthManager,
    private val commentRepository: com.boaxente.riffle.data.repository.CommentRepository,
    getAllSourcesUseCase: GetAllSourcesUseCase,
    @dagger.hilt.android.qualifiers.ApplicationContext private val context: android.content.Context
) : ViewModel() {

    fun getCommentCount(articleLink: String): Flow<Int> {
        return commentRepository.getCommentCount(articleLink)
    }

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()
    
    private val _messageEvent = kotlinx.coroutines.flow.MutableSharedFlow<String>()
    val messageEvent = _messageEvent.asSharedFlow()

    private val _selectedSource = MutableStateFlow<String?>(null)
    val selectedSource: StateFlow<String?> = _selectedSource.asStateFlow()

    val markAsReadOnScroll: StateFlow<Boolean> = preferencesManager.isMarkAsReadOnScrollFlow

    val isDarkMode: StateFlow<Boolean> = preferencesManager.isDarkModeFlow

    private val _discoveredFeeds = MutableStateFlow<List<DiscoveredFeed>>(emptyList())
    val discoveredFeeds: StateFlow<List<DiscoveredFeed>> = _discoveredFeeds.asStateFlow()

    private val _discoveredFeedHealth = MutableStateFlow<Map<String, FeedHealth>>(emptyMap())
    val discoveredFeedHealth: StateFlow<Map<String, FeedHealth>> = _discoveredFeedHealth.asStateFlow()

    val sortedDiscoveredFeeds: StateFlow<List<DiscoveredFeed>> = combine(
        _discoveredFeeds,
        _discoveredFeedHealth
    ) { feeds, healthMap ->
        feeds.sortedBy { feed ->
            healthMap[feed.url] ?: FeedHealth.UNKNOWN
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching.asStateFlow()

    private val _articleSearchQuery = MutableStateFlow("")
    val articleSearchQuery: StateFlow<String> = _articleSearchQuery.asStateFlow()

    fun setArticleSearchQuery(query: String) {
        _articleSearchQuery.value = query
    }

    // Startup scroll logic
    var hasPerformedStartupScroll: Boolean = false
        private set

    fun markStartupScrollPerformed() {
        hasPerformedStartupScroll = true
    }

    // --- Authentication ---
    val currentUser = authManager.currentUser

    fun signIn(idToken: String) {
        viewModelScope.launch {
            try {
                authManager.signInWithGoogle(idToken)
                _messageEvent.emit(context.getString(com.boaxente.riffle.R.string.msg_login_success))
            } catch (e: Exception) {
                RiffleLogger.recordException(e)
                _messageEvent.emit(context.getString(com.boaxente.riffle.R.string.msg_login_error, e.message))
            }
        }
    }

    fun signOut() {
        viewModelScope.launch {
            authManager.signOut()
            _messageEvent.emit(context.getString(com.boaxente.riffle.R.string.msg_logout_success))
        }
    }



    // --- ZONA IA: Variables para el resumen ---
    private val _summaryState = MutableStateFlow<String?>(null)
    val summaryState: StateFlow<String?> = _summaryState.asStateFlow()

    private val _isSummarizing = MutableStateFlow(false)
    val isSummarizing: StateFlow<Boolean> = _isSummarizing.asStateFlow()

    private val _geminiApiKey = MutableStateFlow(preferencesManager.getGeminiApiKey())
    val geminiApiKey: StateFlow<String> = _geminiApiKey.asStateFlow()

    fun updateGeminiApiKey(key: String) {
        _geminiApiKey.value = key
        preferencesManager.setGeminiApiKey(key)
        // Note: API keys shouldn't necessarily be synced for security, but user requested settings sync.
        // Assuming secure user-specific storage in Firestore rules.
        // Or skipped? Let's skip API key to be safe unless requested specifically.
    }

    val language: StateFlow<String> = preferencesManager.languageFlow

    fun setLanguage(code: String) {
        // _language.value = code  <-- Removed local backing field
        preferencesManager.setLanguage(code)
        firestoreHelper.updateSettingInCloud("language", code)
    }

    private val _modelStatuses = MutableStateFlow<Map<String, String>>(emptyMap())
    val modelStatuses: StateFlow<Map<String, String>> = _modelStatuses.asStateFlow()

    val syncInterval: StateFlow<Long> = preferencesManager.syncIntervalFlow

    fun setSyncInterval(hours: Long) {
        // _syncInterval.value = hours <-- Removed
        preferencesManager.setSyncInterval(hours)
        firestoreHelper.updateSettingInCloud("sync_interval", hours)
        
        // Reschedule Work
        val constraints = androidx.work.Constraints.Builder()
            .setRequiredNetworkType(androidx.work.NetworkType.CONNECTED)
            .setRequiresBatteryNotLow(true)
            .build()

        val syncRequest = androidx.work.PeriodicWorkRequestBuilder<com.boaxente.riffle.worker.FeedSyncWorker>(hours, java.util.concurrent.TimeUnit.HOURS)
            .setConstraints(constraints)
            .build()

        androidx.work.WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            "FeedSync",
            androidx.work.ExistingPeriodicWorkPolicy.UPDATE,
            syncRequest
        )
    }

    // Función que llama a la IA
    fun summarizeArticle(title: String, content: String, articleUrl: String? = null) {
        val currentKey = _geminiApiKey.value
        if (currentKey.isBlank()) {
            _summaryState.value = context.getString(com.boaxente.riffle.R.string.ai_error_api_key)
            return
        }

        viewModelScope.launch {
            _isSummarizing.value = true
            _summaryState.value = null // Limpiamos resumen anterior

            // Intentar obtener el contenido completo de la web
            val webContent = if (articleUrl != null) {
                withContext(Dispatchers.IO) {
                    try {
                        val doc = Jsoup.connect(articleUrl)
                            .timeout(10000)
                            .userAgent("Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36")
                            .get()
                        // Eliminar scripts, styles y nav para obtener solo el contenido
                        doc.select("script, style, nav, header, footer, aside, .comments, #comments").remove()
                        // Intentar extraer del tag <article> primero, sino del <body>
                        val articleElement = doc.selectFirst("article")
                        val text = (articleElement ?: doc.body())?.text() ?: ""
                        text.ifBlank { null }
                    } catch (e: Exception) {
                        Log.w("MainViewModel", "Failed to fetch web content from $articleUrl: ${e.message}")
                        null
                    }
                }
            } else null

            // Usar contenido web si es más largo que el RSS, sino fallback al RSS
            val rssContent = try {
                Jsoup.parse(content).text()
            } catch (e: Exception) {
                RiffleLogger.recordException(e)
                content
            }
            val bestContent = if (webContent != null && webContent.length > rssContent.length) {
                webContent
            } else {
                rssContent
            }
            val cleanContent = bestContent.take(10000)
            
            val currentLanguage = language.value
            val isEnglish = currentLanguage == "en" || (currentLanguage == "system" && java.util.Locale.getDefault().language == "en")

            val prompt = if (isEnglish) {
                """
                Act as an expert news assistant.
                Summarize the following article using an informative and direct tone. Focus on summarizing the article and do not mention anything related to the prompt.
                Title: $title
                Content: $cleanContent
                """.trimIndent()
            } else {
                """
                Actúa como un asistente experto en noticias.
                Resume el siguiente artículo usando un tono informativo y directo. Centrate en resumir el articulo y no menciones nada relacionado con el prompt.
                Título: $title
                Contenido: $cleanContent
                """.trimIndent()
            }

            // Lógica de reintento con fallback de modelos
            val fallbackModels = listOf("gemini-2.5-flash", "gemini-2.5-flash-lite", "gemini-3-flash")
            var success = false
            var lastException: Exception? = null

            val currentStatuses = _modelStatuses.value.toMutableMap()

            for (modelName in fallbackModels) {
                try {
                    val generativeModel = GenerativeModel(
                        modelName = modelName,
                        apiKey = currentKey
                    )

                    val response = generativeModel.generateContent(prompt)
                    _summaryState.value = response.text
                    success = true
                    
                    // Si ha funcionado, marcamos como disponible
                    currentStatuses[modelName] = "available"
                    _modelStatuses.value = currentStatuses
                    
                    break // Éxito, salimos del bucle
                } catch (e: Exception) {
                RiffleLogger.recordException(e)
                    lastException = e
                    val msg = e.message?.lowercase() ?: ""
                    // Detectar error de cuota (429, exhausted, quota)
                    val isQuotaError = msg.contains("429") || msg.contains("quota") || msg.contains("exhausted")
                    
                    if (isQuotaError) {
                        Log.w("MainViewModel", "Model $modelName quota exceeded. Switching to next model...")
                        currentStatuses[modelName] = "exhausted"
                        _modelStatuses.value = currentStatuses
                        continue // Intentar siguiente modelo
                    } else {
                        e.printStackTrace()
                        // Para otros errores no sabemos si está agotado o no, pero podemos marcarlo unknown o dejarlo como estaba
                        // De momento solo marcamos exhausted si es cuota.
                        break // Error no relacionado con cuota (red, api key inválida, etc), abortamos
                    }
                }
            }

            if (!success) {
                val errorMsg = lastException?.message?.lowercase() ?: ""
                val isOverloaded = errorMsg.contains("503") || errorMsg.contains("overloaded") || errorMsg.contains("capacity")
                
                if (isOverloaded) {
                    _summaryState.value = context.getString(com.boaxente.riffle.R.string.ai_error_overloaded)
                } else {
                    _summaryState.value = context.getString(
                        com.boaxente.riffle.R.string.ai_error_connection,
                        lastException?.localizedMessage ?: "Unknown error"
                    )
                }
            }

            _isSummarizing.value = false
        }
    }

    // Para borrar el resumen cuando sales del artículo
    fun clearSummary() {
        _summaryState.value = null
    }

    private val _articleLimit = MutableStateFlow(20)
    
    // Enlaces de artículos que deben estar ocultos (ya estaban leídos en la última recarga)
    private val _hiddenArticleLinks = MutableStateFlow<Set<String>>(emptySet())

    val sources: StateFlow<List<SourceEntity>> = getAllSourcesUseCase()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val folders: StateFlow<List<FolderEntity>> = feedDao.getAllFolders()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val unreadCounts: StateFlow<Map<String, Int>> = feedDao.getUnreadCountsBySource()
        .map { list -> list.associate { it.sourceUrl to it.count } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    val savedCount: StateFlow<Int> = feedDao.getSavedCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val feedHealthState: StateFlow<Map<String, FeedHealth>> = combine(
        feedDao.getLastArticleDates(),
        sources
    ) { dates, allSources ->
        val now = System.currentTimeMillis()
        val dayInMillis = 24 * 60 * 60 * 1000L
        val datesMap = dates.associate { it.sourceUrl to it.lastPubDate }
        
        allSources.associate { source ->
            val lastDate = datesMap[source.url]
            val health = if (lastDate != null) {
                val diff = now - lastDate
                when {
                    diff <= 5 * dayInMillis -> FeedHealth.GOOD
                    diff <= 10 * dayInMillis -> FeedHealth.WARNING
                    diff <= 50 * dayInMillis -> FeedHealth.BAD
                    else -> FeedHealth.DEAD
                }
            } else {
                FeedHealth.DEAD // No articles found -> Dead/Broken/Empty
            }
            source.url to health
        }
    }
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    private val _dismissedHealthNotifications = MutableStateFlow<Set<String>>(emptySet())
    
    // Feeds that are BAD or DEAD (Red/Grey) and NOT dismissed
    val brokenFeeds: StateFlow<List<Pair<SourceEntity, FeedHealth>>> = combine(
        sources,
        feedHealthState,
        _dismissedHealthNotifications
    ) { allSources, healthMap, dismissed ->
        val dismissedUrls = dismissed.map { it.split("|")[0] }.toSet()
        
        allSources.mapNotNull { source ->
            val health = healthMap[source.url] ?: FeedHealth.UNKNOWN
            if ((health == FeedHealth.BAD || health == FeedHealth.DEAD) && source.url !in dismissedUrls) {
                source to health
            } else null
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val showHealthNotificationBadge: StateFlow<Boolean> = brokenFeeds
        .map { it.isNotEmpty() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    fun dismissHealthNotification(url: String) {
        preferencesManager.dismissFeedHealthNotification(url)
        loadDismissedNotifications()
    }

    private fun loadDismissedNotifications() {
        _dismissedHealthNotifications.value = preferencesManager.getDismissedFeedHealthNotifications()
    }


    init {
        // Carga inicial de artículos para ocultar
        updateHiddenArticles()
        
        // Notification cleanup
        viewModelScope.launch {
            // 7 days in milliseconds
            val sevenDaysMillis = 7 * 24 * 60 * 60 * 1000L
            preferencesManager.clearExpiredDismissals(sevenDaysMillis)
            loadDismissedNotifications()
        }

        // Sync inicial una sola vez
        viewModelScope.launch {
            try {
                feedDao.cleanupHugeArticles()
            } catch (e: Exception) {
                RiffleLogger.recordException(e)
                e.printStackTrace()
            }
        }
    }

    private data class FilterState(
        val selector: String?,
        val hiddenLinks: Set<String>,
        val limit: Int,
        val searchQuery: String
    )

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<FeedUiState> = combine(_selectedSource, _hiddenArticleLinks, _articleLimit, _articleSearchQuery) { selector, hiddenLinks, limit, searchQuery ->
        FilterState(selector, hiddenLinks, limit, searchQuery)
    }
    .flatMapLatest { state ->
        val articlesFlow = when {
            state.selector == "saved" -> feedDao.getSavedArticles()
            state.selector == null -> getArticlesUseCase()
            state.selector.startsWith("folder:") -> {
                val folderName = state.selector.removePrefix("folder:")
                feedDao.getArticlesByFolder(folderName)
            }
            else -> getArticlesUseCase(state.selector)
        }
        articlesFlow.map<List<ArticleEntity>, FeedUiState> { articles ->
            val filtered = if (state.searchQuery.isNotEmpty()) {
                articles.filter { it.title.contains(state.searchQuery, ignoreCase = true) }
            } else if (state.selector == "saved") {
                articles
            } else {
                articles.filter { it.link !in state.hiddenLinks }
            }
            FeedUiState.Success(filtered.take(state.limit))
        }
    }
    .catch { 
        RiffleLogger.recordException(it)
        emit(FeedUiState.Error(it.message ?: context.getString(com.boaxente.riffle.R.string.msg_unknown_error))) 
    }
    .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = FeedUiState.Loading
    )

    private fun updateHiddenArticles() {
        viewModelScope.launch {
            val readLinks = feedDao.getReadArticleLinks()
            _hiddenArticleLinks.value = readLinks.toSet()
        }
    }

    fun getArticle(url: String): Flow<ArticleEntity?> {
        return feedDao.getArticleByLink(url)
    }

    fun getArticleWithSource(url: String): Flow<ArticleWithSource?> {
        return feedDao.getArticleWithSource(url)
    }

    fun selectSource(url: String?) {
        _articleLimit.value = 20
        updateHiddenArticles()
        _selectedSource.value = url
        _articleSearchQuery.value = "" // Clear search when switching source
    }

    fun selectSaved() {
        _articleLimit.value = 20
        _selectedSource.value = "saved"
        _articleSearchQuery.value = "" // Clear search
    }

    fun selectFolder(name: String) {
        _articleLimit.value = 20
        updateHiddenArticles()
        _selectedSource.value = "folder:$name"
        _articleSearchQuery.value = "" // Clear search
    }

    fun loadMore() {
        _articleLimit.value += 20
    }

    fun toggleMarkAsReadOnScroll(enabled: Boolean) {
        preferencesManager.setMarkAsReadOnScroll(enabled)
        firestoreHelper.updateSettingInCloud("mark_on_scroll", enabled)
    }

    fun toggleDarkMode(enabled: Boolean) {
        preferencesManager.setDarkMode(enabled)
        firestoreHelper.updateSettingInCloud("theme", if (enabled) 1 else 0) // Mapping 1=dark, 0=light based on remote listener
    }

    fun renameSource(url: String, newTitle: String) {
        viewModelScope.launch {
            feedRepository.renameSource(url, newTitle)
        }
    }

    private val _sourceAdditionState = MutableStateFlow<SourceAdditionState>(SourceAdditionState.Idle)
    val sourceAdditionState: StateFlow<SourceAdditionState> = _sourceAdditionState.asStateFlow()

    fun clearSourceAdditionState() {
        _sourceAdditionState.value = SourceAdditionState.Idle
    }

    fun addSource(url: String, title: String?, iconUrl: String? = null) {
        viewModelScope.launch {
            _sourceAdditionState.value = SourceAdditionState.Loading(url)
            try {
                addSourceUseCase(url, title, iconUrl)
                _messageEvent.emit(context.getString(com.boaxente.riffle.R.string.msg_feed_added))
                _sourceAdditionState.value = SourceAdditionState.Success
            } catch (e: retrofit2.HttpException) {
                if (e.code() == 404) {
                     _sourceAdditionState.value = SourceAdditionState.Error(
                         title = context.getString(com.boaxente.riffle.R.string.error_feed_not_found_title),
                         message = context.getString(com.boaxente.riffle.R.string.error_feed_not_found_message, url)
                     )
                } else if (e.code() in 400..599) {
                    _sourceAdditionState.value = SourceAdditionState.Error(
                        title = context.getString(com.boaxente.riffle.R.string.error_feed_generic_title),
                        message = context.getString(com.boaxente.riffle.R.string.error_feed_http, e.code())
                    )
                } else {
                    _sourceAdditionState.value = SourceAdditionState.Error(
                        title = context.getString(com.boaxente.riffle.R.string.error_feed_generic_title),
                        message = e.localizedMessage ?: context.getString(com.boaxente.riffle.R.string.msg_unknown_error)
                    )
                }
            } catch (e: Exception) {
                RiffleLogger.recordException(e)
                e.printStackTrace()
                _messageEvent.emit(context.getString(com.boaxente.riffle.R.string.msg_feed_error, e.message))
                _sourceAdditionState.value = SourceAdditionState.Error(
                    title = context.getString(com.boaxente.riffle.R.string.error_feed_generic_title),
                    message = e.localizedMessage ?: context.getString(com.boaxente.riffle.R.string.msg_unknown_error)
                )
            }
        }
    }

    fun deleteSource(url: String) {
        viewModelScope.launch {
            feedRepository.deleteSource(url)
            if (_selectedSource.value == url) {
                _selectedSource.value = null
            }
        }
    }

    fun addFolder(name: String) {
        viewModelScope.launch {
            feedDao.insertFolder(FolderEntity(name))
        }
    }

    fun deleteFolder(name: String) {
        viewModelScope.launch {
            feedRepository.deleteFolder(name)
            if (_selectedSource.value == "folder:$name") {
                _selectedSource.value = null
            }
        }
    }

    fun renameFolder(oldName: String, newName: String) {
        viewModelScope.launch {
            feedDao.renameFolder(oldName, newName)
            if (_selectedSource.value == "folder:$oldName") {
                _selectedSource.value = "folder:$newName"
            }
        }
    }

    fun moveSourceToFolder(url: String, folderName: String?) {
        viewModelScope.launch {
            feedRepository.moveSourceToFolder(url, folderName)
        }
    }

    fun sync() {
        viewModelScope.launch {
            _isRefreshing.value = true
            try {
                syncFeedsUseCase()
                updateHiddenArticles()
            } catch (e: Exception) {
                RiffleLogger.recordException(e)
                e.printStackTrace()
            } finally {
                _isRefreshing.value = false
            }
        }
    }

    fun markAsRead(link: String) {
        viewModelScope.launch {
            markArticleReadUseCase(link)
        }
    }


    fun toggleSaveArticle(link: String, isSaved: Boolean) {
        viewModelScope.launch {
            feedRepository.toggleArticleSaved(link, !isSaved)
        }
    }

    private val _feedSearchQuery = MutableStateFlow("")
    val feedSearchQuery: StateFlow<String> = _feedSearchQuery.asStateFlow()

    private val _requestOpenDrawer = MutableStateFlow(false)
    val requestOpenDrawer: StateFlow<Boolean> = _requestOpenDrawer.asStateFlow()

    fun setFeedSearchQuery(query: String) {
        _feedSearchQuery.value = query
    }

    fun setRequestOpenDrawer(open: Boolean) {
        _requestOpenDrawer.value = open
    }

    fun clearFeedSearch() {
        _feedSearchQuery.value = ""
        _discoveredFeeds.value = emptyList()
        _discoveredFeedHealth.value = emptyMap()
        _isSearching.value = false
    }

    private fun probeFeedsHealth(feeds: List<DiscoveredFeed>) {
        val savedUrls = sources.value.map { it.url }.toSet()
        feeds.filter { it.url !in savedUrls }.forEach { feed ->
            viewModelScope.launch(Dispatchers.IO) {
                try {
                    val response = feedService.fetchFeed(feed.url)
                    val parsed = rssParser.parse(response.byteStream(), feed.url)
                    val latestDate = parsed.articles.maxOfOrNull { it.pubDate }
                    if (latestDate != null) {
                        val now = System.currentTimeMillis()
                        val dayInMillis = 24 * 60 * 60 * 1000L
                        val diff = now - latestDate
                        val health = when {
                            diff <= 5 * dayInMillis -> FeedHealth.GOOD
                            diff <= 10 * dayInMillis -> FeedHealth.WARNING
                            diff <= 50 * dayInMillis -> FeedHealth.BAD
                            else -> FeedHealth.DEAD
                        }
                        _discoveredFeedHealth.value = _discoveredFeedHealth.value + (feed.url to health)
                    }
                } catch (e: Exception) {
                    // Probe failed - show gray indicator as fallback
                    _discoveredFeedHealth.value = _discoveredFeedHealth.value + (feed.url to FeedHealth.UNKNOWN)
                    Log.d("FeedSearch", "Health probe failed for ${feed.url}: ${e.message}")
                }
            }
        }
    }

    fun searchFeeds(query: String) {
        viewModelScope.launch {
            _isSearching.value = true
            _discoveredFeeds.value = emptyList()
            try {
                _discoveredFeeds.value = withContext(Dispatchers.IO) {
                    try {
                        withTimeout(15_000) {
                            val cleanQuery = query.trim().lowercase()
                            val domainsToSearch = mutableSetOf<String>()

                            // 1. If it looks like a URL or has a TLD, search directly
                            if (cleanQuery.contains(".") || cleanQuery.startsWith("http")) {
                                domainsToSearch.add(cleanQuery)
                            } else {
                                // 2. Otherwise, use clearbit to find domains
                                try {
                                    val suggestions = clearbitService.suggestCompanies(cleanQuery)
                                    suggestions.forEach { domainsToSearch.add(it.domain) }
                                } catch (e: Exception) {
                                    RiffleLogger.recordException(e)
                                    Log.e("FeedSearch", "Clearbit lookup failed: ${e.message}")
                                }
                                // Fallback: append .com just in case
                                if (domainsToSearch.isEmpty()) {
                                    domainsToSearch.add("$cleanQuery.com")
                                }
                            }

                            // 3. Search FeedSearch for collected domains in parallel
                            val deferredResults = domainsToSearch.take(3).map { domain ->
                                async {
                                    try {
                                        feedSearchService.search(domain).map { dto ->
                                            val feedTitle = dto.title?.takeIf { it.isNotBlank() } ?: "Feed from $domain"
                                            DiscoveredFeed(
                                                title = feedTitle,
                                                url = dto.selfUrl ?: dto.url,
                                                iconUrl = dto.favicon,
                                                siteName = dto.title?.takeIf { it.isNotBlank() }
                                            )
                                        }
                                    } catch (e: Exception) {
                                        RiffleLogger.recordException(e)
                                        Log.e("FeedSearch", "FeedSearch for $domain failed: ${e.message}")
                                        emptyList()
                                    }
                                }
                            }

                            deferredResults.awaitAll().flatten().distinctBy { it.url }
                        }
                    } catch (e: TimeoutCancellationException) {
                        Log.w("FeedSearch", "Search timed out after 15s")
                        _discoveredFeeds.value // return partial results if any
                    } catch (e: Exception) {
                        RiffleLogger.recordException(e)
                        Log.e("FeedSearch", "API Error: ${e.message}")
                        emptyList()
                    }
                }
            } catch (e: Exception) {
                RiffleLogger.recordException(e)
                Log.e("FeedSearch", "SearchFeeds error: ${e.message}")
            } finally {
                _isSearching.value = false
                // Probe health for discovered feeds asynchronously
                if (_discoveredFeeds.value.isNotEmpty()) {
                    _discoveredFeedHealth.value = emptyMap()
                    probeFeedsHealth(_discoveredFeeds.value)
                }
            }
        }
    }




    // --- Feed Preview Logic ---
    private val _previewFeedUrl = MutableStateFlow<String?>(null)
    val previewFeedUrl: StateFlow<String?> = _previewFeedUrl.asStateFlow()

    private val _previewArticles = MutableStateFlow<List<ArticleEntity>>(emptyList())
    val previewArticles: StateFlow<List<ArticleEntity>> = _previewArticles.asStateFlow()

    private val _isPreviewLoading = MutableStateFlow(false)
    val isPreviewLoading: StateFlow<Boolean> = _isPreviewLoading.asStateFlow()

    fun loadFeedPreview(url: String) {
        viewModelScope.launch {
            _isPreviewLoading.value = true
            _previewFeedUrl.value = url
            _previewArticles.value = emptyList()
            try {
                val articles = withContext(Dispatchers.IO) {
                    try {
                        val response = feedService.fetchFeed(url)
                        val parsed = rssParser.parse(response.byteStream(), url)
                        // Mapear a ArticleEntity pero sin guardar en DB
                        parsed.articles.map { item ->
                             ArticleEntity(
                                link = item.link,
                                title = item.title,
                                description = item.description,
                                imageUrl = item.imageUrl ?: item.description?.extractFirstImageUrl(),
                                pubDate = item.pubDate,
                                sourceUrl = url,
                                isRead = false,
                                isSaved = false
                             )
                        }
                    } catch (e: Exception) {
                        RiffleLogger.recordException(e)
                        Log.e("FeedPreview", "Error fetching/parsing feed: ${e.message}")
                        emptyList()
                    }
                }
                _previewArticles.value = articles
            } finally {
                _isPreviewLoading.value = false
            }
        }
    }

    fun clearFeedPreview() {
        _previewFeedUrl.value = null
        _previewArticles.value = emptyList()
        _isPreviewLoading.value = false
    }

    fun getPreviewArticle(link: String): ArticleEntity? {
        return _previewArticles.value.find { it.link == link }
    }

    
    private fun createTrustAllSslSocketFactory(): SSLSocketFactory {
        val trustAllCerts = arrayOf<TrustManager>(object : X509TrustManager {
            override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
            override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
            override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
        })

        val sslContext = SSLContext.getInstance("SSL")
        sslContext.init(null, trustAllCerts, java.security.SecureRandom())
        return sslContext.socketFactory
    }
}
