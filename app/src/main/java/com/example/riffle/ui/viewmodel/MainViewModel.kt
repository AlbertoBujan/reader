package com.example.riffle.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.riffle.data.local.PreferencesManager
import com.example.riffle.data.local.dao.FeedDao
import com.example.riffle.data.local.entity.ArticleEntity
import com.example.riffle.data.local.entity.ArticleWithSource
import com.example.riffle.data.local.entity.FolderEntity
import com.example.riffle.data.local.entity.SourceEntity
import com.example.riffle.data.remote.ClearbitService
import com.example.riffle.data.remote.FeedSearchService
import com.example.riffle.domain.usecase.AddSourceUseCase
import com.example.riffle.domain.usecase.GetAllSourcesUseCase
import com.example.riffle.domain.usecase.GetArticlesUseCase
import com.example.riffle.domain.usecase.MarkArticleReadUseCase
import com.example.riffle.domain.usecase.SyncFeedsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
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
import java.net.URL
import java.security.cert.X509Certificate
import javax.inject.Inject
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSocketFactory
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager
import com.google.ai.client.generativeai.GenerativeModel
import com.example.riffle.BuildConfig

sealed interface FeedUiState {
    data object Loading : FeedUiState
    data class Success(val articles: List<ArticleEntity>) : FeedUiState
    data class Error(val message: String) : FeedUiState
}

data class DiscoveredFeed(
    val title: String,
    val url: String,
    val iconUrl: String? = null,
    val siteName: String? = null
)

@HiltViewModel
class MainViewModel @Inject constructor(
    private val getArticlesUseCase: GetArticlesUseCase,
    private val addSourceUseCase: AddSourceUseCase,
    private val syncFeedsUseCase: SyncFeedsUseCase,
    private val markArticleReadUseCase: MarkArticleReadUseCase,
    private val feedDao: FeedDao,
    private val feedRepository: com.example.riffle.domain.repository.FeedRepository,
    private val preferencesManager: PreferencesManager,
    private val feedSearchService: FeedSearchService,
    private val clearbitService: ClearbitService,
    getAllSourcesUseCase: GetAllSourcesUseCase,
    @dagger.hilt.android.qualifiers.ApplicationContext private val context: android.content.Context
) : ViewModel() {

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _messageEvent = kotlinx.coroutines.flow.MutableSharedFlow<String>()
    val messageEvent = _messageEvent.asSharedFlow()

    private val _selectedSource = MutableStateFlow<String?>(null)
    val selectedSource: StateFlow<String?> = _selectedSource.asStateFlow()

    private val _markAsReadOnScroll = MutableStateFlow(preferencesManager.isMarkAsReadOnScroll())
    val markAsReadOnScroll: StateFlow<Boolean> = _markAsReadOnScroll.asStateFlow()

    private val _isDarkMode = MutableStateFlow(preferencesManager.isDarkMode())
    val isDarkMode: StateFlow<Boolean> = _isDarkMode.asStateFlow()

    private val _discoveredFeeds = MutableStateFlow<List<DiscoveredFeed>>(emptyList())
    val discoveredFeeds: StateFlow<List<DiscoveredFeed>> = _discoveredFeeds.asStateFlow()

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
    }

    private val _language = MutableStateFlow(preferencesManager.getLanguage())
    val language: StateFlow<String> = _language.asStateFlow()

    fun setLanguage(code: String) {
        _language.value = code
        preferencesManager.setLanguage(code)
    }

    private val _modelStatuses = MutableStateFlow<Map<String, String>>(emptyMap())
    val modelStatuses: StateFlow<Map<String, String>> = _modelStatuses.asStateFlow()

    private val _syncInterval = MutableStateFlow(preferencesManager.getSyncInterval())
    val syncInterval: StateFlow<Long> = _syncInterval.asStateFlow()

    fun setSyncInterval(hours: Long) {
        _syncInterval.value = hours
        preferencesManager.setSyncInterval(hours)
        
        // Reschedule Work
        val constraints = androidx.work.Constraints.Builder()
            .setRequiredNetworkType(androidx.work.NetworkType.CONNECTED)
            .setRequiresBatteryNotLow(true)
            .build()

        val syncRequest = androidx.work.PeriodicWorkRequestBuilder<com.example.riffle.worker.FeedSyncWorker>(hours, java.util.concurrent.TimeUnit.HOURS)
            .setConstraints(constraints)
            .build()

        androidx.work.WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            "FeedSync",
            androidx.work.ExistingPeriodicWorkPolicy.UPDATE,
            syncRequest
        )
    }

    // Función que llama a la IA
    fun summarizeArticle(title: String, content: String) {
        val currentKey = _geminiApiKey.value
        if (currentKey.isBlank()) {
            _summaryState.value = context.getString(com.example.riffle.R.string.ai_error_api_key)
            return
        }

        viewModelScope.launch {
            _isSummarizing.value = true
            _summaryState.value = null // Limpiamos resumen anterior

            // Preparar prompt una única vez
            val cleanContent = try {
                Jsoup.parse(content).text().take(10000)
            } catch (e: Exception) {
                content.take(10000)
            }
            
            val currentLanguage = _language.value
            val isEnglish = currentLanguage == "en" || (currentLanguage == "system" && java.util.Locale.getDefault().language == "en")

            val prompt = if (isEnglish) {
                """
                Act as an expert news assistant.
                Summarize the following article using an informative and direct tone.
                Title: $title
                Content: $cleanContent
                """.trimIndent()
            } else {
                """
                Actúa como un asistente experto en noticias.
                Resume el siguiente artículo usando un tono informativo y directo.
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
                    _summaryState.value = context.getString(com.example.riffle.R.string.ai_error_overloaded)
                } else {
                    _summaryState.value = context.getString(
                        com.example.riffle.R.string.ai_error_connection, 
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

    init {
        // Carga inicial de artículos para ocultar
        updateHiddenArticles()
        // Sync inicial una sola vez

    }

    // Sorting
    enum class SortOrder {
        NEWEST, OLDEST
    }

    private val _sortOrder = MutableStateFlow(
        try {
            SortOrder.valueOf(preferencesManager.getSortOrder())
        } catch (e: Exception) {
            SortOrder.NEWEST
        }
    )
    val sortOrder: StateFlow<SortOrder> = _sortOrder.asStateFlow()

    fun setSortOrder(order: SortOrder) {
        _sortOrder.value = order
        preferencesManager.setSortOrder(order.name)
    }

    private data class FilterState(
        val selector: String?,
        val hiddenLinks: Set<String>,
        val limit: Int,
        val searchQuery: String,
        val sortOrder: SortOrder
    )

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<FeedUiState> = combine(_selectedSource, _hiddenArticleLinks, _articleLimit, _articleSearchQuery, _sortOrder) { selector, hiddenLinks, limit, searchQuery, sortOrder ->
        FilterState(selector, hiddenLinks, limit, searchQuery, sortOrder)
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
            var filtered = if (state.searchQuery.isNotEmpty()) {
                articles.filter { it.title.contains(state.searchQuery, ignoreCase = true) }
            } else if (state.selector == "saved") {
                articles
            } else {
                articles.filter { it.link !in state.hiddenLinks }
            }

            // Apply sorting
            filtered = if (state.sortOrder == SortOrder.OLDEST) {
                filtered.sortedBy { it.pubDate }
            } else {
                filtered.sortedByDescending { it.pubDate }
            }
            
            FeedUiState.Success(filtered.take(state.limit))
        }
    }
    .catch { emit(FeedUiState.Error(it.message ?: context.getString(com.example.riffle.R.string.msg_unknown_error))) }
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
        _markAsReadOnScroll.value = enabled
        preferencesManager.setMarkAsReadOnScroll(enabled)
    }

    fun toggleDarkMode(enabled: Boolean) {
        _isDarkMode.value = enabled
        preferencesManager.setDarkMode(enabled)
    }

    fun renameSource(url: String, newTitle: String) {
        viewModelScope.launch {
            feedDao.updateSourceTitle(url, newTitle)
        }
    }

    fun addSource(url: String, title: String?, iconUrl: String? = null) {
        viewModelScope.launch {
            try {
                addSourceUseCase(url, title, iconUrl)
                _messageEvent.emit(context.getString(com.example.riffle.R.string.msg_feed_added))
            } catch (e: Exception) {
                e.printStackTrace()
                _messageEvent.emit(context.getString(com.example.riffle.R.string.msg_feed_error, e.message))
            }
        }
    }

    fun deleteSource(url: String) {
        viewModelScope.launch {
            feedDao.deleteSource(url)
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
            feedDao.deleteFolder(name)
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
            feedDao.updateSourceFolder(url, folderName)
        }
    }

    fun sync() {
        viewModelScope.launch {
            _isRefreshing.value = true
            try {
                syncFeedsUseCase()
                updateHiddenArticles()
            } catch (e: Exception) {
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
            feedDao.updateArticleSavedStatus(link, !isSaved)
        }
    }

    fun clearFeedSearch() {
        _discoveredFeeds.value = emptyList()
        _isSearching.value = false
    }

    fun searchFeeds(query: String) {
        viewModelScope.launch {
            _isSearching.value = true
            _discoveredFeeds.value = emptyList()
            try {
                _discoveredFeeds.value = withContext(Dispatchers.IO) {
                    try {
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
                                Log.e("FeedSearch", "Clearbit lookup failed: ${e.message}")
                            }
                            // Fallback: append .com just in case
                            if (domainsToSearch.isEmpty()) {
                                domainsToSearch.add("$cleanQuery.com")
                            }
                        }

                        // 3. Search FeedSearch for collected domains
                        // We run this for up to 3 domains to avoid spamming, though usually 1-2.
                        val results = mutableListOf<DiscoveredFeed>()
                        
                        // We can run these concurrently for speed
                        // Using async/awaitAll would be better but simple loop is fine for small count
                         domainsToSearch.take(3).forEach { domain ->
                            try {
                                val searchResults = feedSearchService.search(domain)
                                searchResults.forEach { dto ->
                                    // Use defaults if fields missing
                                    results.add(DiscoveredFeed(
                                        title = dto.title.ifBlank { "Feed from $domain" },
                                        url = dto.selfUrl ?: dto.url,
                                        iconUrl = dto.favicon,
                                        siteName = dto.title 
                                    ))
                                }
                            } catch (e: Exception) {
                                Log.e("FeedSearch", "FeedSearch for $domain failed: ${e.message}")
                            }
                        }
                        
                        results.distinctBy { it.url }
                        
                    } catch (e: Exception) {
                        Log.e("FeedSearch", "API Error: ${e.message}")
                        emptyList()
                    }
                }
            } catch (e: Exception) {
                Log.e("FeedSearch", "SearchFeeds error: ${e.message}")
            } finally {
                _isSearching.value = false
            }
        }
    }

    fun importOpml(uri: android.net.Uri) {
        viewModelScope.launch {
            try {
                _messageEvent.emit(context.getString(com.example.riffle.R.string.article_loading))
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    feedRepository.importOpml(stream)
                }
                _messageEvent.emit(context.getString(com.example.riffle.R.string.msg_opml_import_success))
                sync()
            } catch (e: Exception) {
                 _messageEvent.emit(context.getString(com.example.riffle.R.string.msg_opml_error, e.localizedMessage ?: "Unknown"))
                 e.printStackTrace()
            }
        }
    }

    fun exportOpml(uri: android.net.Uri) {
        viewModelScope.launch {
             try {
                 val opmlContent = feedRepository.exportOpml()
                 context.contentResolver.openOutputStream(uri)?.use { stream ->
                     stream.write(opmlContent.toByteArray())
                 }
                  _messageEvent.emit(context.getString(com.example.riffle.R.string.msg_opml_export_success))
             } catch (e: Exception) {
                  _messageEvent.emit(context.getString(com.example.riffle.R.string.msg_opml_error, e.localizedMessage ?: "Unknown"))
                  e.printStackTrace()
             }
        }
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
