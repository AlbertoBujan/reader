package com.example.inoreaderlite.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.inoreaderlite.data.local.PreferencesManager
import com.example.inoreaderlite.data.local.dao.FeedDao
import com.example.inoreaderlite.data.local.entity.ArticleEntity
import com.example.inoreaderlite.data.local.entity.FolderEntity
import com.example.inoreaderlite.data.local.entity.SourceEntity
import com.example.inoreaderlite.data.remote.ClearbitService
import com.example.inoreaderlite.domain.usecase.AddSourceUseCase
import com.example.inoreaderlite.domain.usecase.GetAllSourcesUseCase
import com.example.inoreaderlite.domain.usecase.GetArticlesUseCase
import com.example.inoreaderlite.domain.usecase.MarkArticleReadUseCase
import com.example.inoreaderlite.domain.usecase.SyncFeedsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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
    private val preferencesManager: PreferencesManager,
    private val clearbitService: ClearbitService,
    getAllSourcesUseCase: GetAllSourcesUseCase
) : ViewModel() {

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

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
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<FeedUiState> = combine(_selectedSource, _hiddenArticleLinks) { selector, hiddenLinks ->
        selector to hiddenLinks
    }
    .flatMapLatest { (selector, hiddenLinks) ->
        val articlesFlow = when {
            selector == "saved" -> feedDao.getSavedArticles()
            selector == null -> getArticlesUseCase()
            selector.startsWith("folder:") -> {
                val folderName = selector.removePrefix("folder:")
                feedDao.getArticlesByFolder(folderName)
            }
            else -> getArticlesUseCase(selector)
        }
        articlesFlow.map<List<ArticleEntity>, FeedUiState> { articles ->
            // Filtramos solo los que estaban leídos al momento de la última recarga/navegación
            // EXCEPTO si estamos en la vista de "saved", donde mostramos todo independientemente de si es leído
            if (selector == "saved") {
                FeedUiState.Success(articles)
            } else {
                FeedUiState.Success(articles.filter { it.link !in hiddenLinks })
            }
        }
    }
    .catch { emit(FeedUiState.Error(it.message ?: "Unknown error")) }
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

    fun selectSource(url: String?) {
        updateHiddenArticles()
        _selectedSource.value = url
    }

    fun selectSaved() {
        _selectedSource.value = "saved"
    }

    fun selectFolder(name: String) {
        updateHiddenArticles()
        _selectedSource.value = "folder:$name"
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
            } catch (e: Exception) {
                e.printStackTrace()
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
                // Al terminar la sincronización, actualizamos qué artículos ocultar
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

    fun markAllAsRead() {
        viewModelScope.launch {
            val selector = _selectedSource.value
            when {
                selector == "saved" -> return@launch // No marcamos todo como leído en saved (opcional)
                selector == null -> feedDao.markAllArticlesAsRead()
                selector.startsWith("folder:") -> {
                    val folderName = selector.removePrefix("folder:")
                    feedDao.markArticlesAsReadByFolder(folderName)
                }
                else -> feedDao.markArticlesAsReadBySource(selector)
            }
            updateHiddenArticles()
            sync() 
        }
    }

    fun toggleSaveArticle(link: String, isSaved: Boolean) {
        viewModelScope.launch {
            feedDao.updateArticleSavedStatus(link, !isSaved)
        }
    }

    fun searchFeeds(query: String) {
        viewModelScope.launch {
            _isSearching.value = true
            _discoveredFeeds.value = emptyList()
            try {
                val discovered = withContext(Dispatchers.IO) {
                    val results = mutableListOf<DiscoveredFeed>()
                    val cleanQuery = query.trim().lowercase()
                    
                    val domainsToTry = mutableListOf<String>()
                    var clearbitName: String? = null
                    var queryIconUrl: String? = null

                    // 1. Usar Clearbit para obtener el dominio si no parece una URL directa
                    if (!cleanQuery.contains(".") || !cleanQuery.startsWith("http")) {
                        try {
                            Log.d("FeedSearch", "Querying Clearbit for: $cleanQuery")
                            val suggestions = clearbitService.suggestCompanies(cleanQuery)
                            Log.d("FeedSearch", "Clearbit returned ${suggestions.size} suggestions")
                            suggestions.forEach { suggestion ->
                                domainsToTry.add(suggestion.domain)
                                if (clearbitName == null) clearbitName = suggestion.name
                                if (queryIconUrl == null) queryIconUrl = suggestion.logo
                            }
                        } catch (e: Exception) {
                            Log.e("FeedSearch", "Clearbit error: ${e.message}")
                        }
                    }
                    
                    // Siempre añadir el query original por si acaso
                    if (!domainsToTry.contains(cleanQuery)) {
                        domainsToTry.add(0, cleanQuery)
                    }

                    for (domain in domainsToTry) {
                        val urlsToTry = mutableListOf<String>()
                        if (domain.startsWith("http")) {
                            urlsToTry.add(domain)
                        } else {
                            urlsToTry.add("https://$domain")
                            urlsToTry.add("https://www.$domain")
                            urlsToTry.add("http://$domain")
                        }

                        for (baseUrl in urlsToTry) {
                            try {
                                Log.d("FeedSearch", "Trying base URL: $baseUrl")
                                val doc = Jsoup.connect(baseUrl)
                                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36")
                                    .timeout(8000)
                                    .sslSocketFactory(createTrustAllSslSocketFactory())
                                    .followRedirects(true)
                                    .get()

                                // Buscar en etiquetas <link> del head
                                doc.select("link[type*=rss], link[type*=atom], link[type*=xml][rel=alternate]").forEach { element ->
                                    val href = element.attr("abs:href")
                                    val title = element.attr("title").ifBlank { doc.title() }
                                    if (href.isNotBlank()) {
                                        results.add(DiscoveredFeed(title, href, queryIconUrl, clearbitName))
                                    }
                                }

                                // Si no hay nada, buscar en enlaces <a> con texto sospechoso
                                if (results.isEmpty()) {
                                    doc.select("a[href*=rss], a[href*=feed]").forEach { element ->
                                        val href = element.attr("abs:href")
                                        if (href.contains(".xml") || href.contains("rss") || href.contains("feed")) {
                                            results.add(DiscoveredFeed(element.text().ifBlank { "Feed: ${doc.title()}" }, href, queryIconUrl, clearbitName))
                                        }
                                    }
                                }
                                
                                if (results.isNotEmpty()) break
                                
                            } catch (e: Exception) {
                                Log.d("FeedSearch", "Failed base URL $baseUrl: ${e.message}")
                            }
                        }

                        // Fallback: Probar rutas directas comunes
                        if (results.isEmpty()) {
                            val commonPaths = listOf("/feed", "/rss", "/rss.xml", "/index.xml", "/atom.xml", "/feed.xml")
                            var host = if (domain.startsWith("http")) {
                                try { URL(domain).host } catch (e: Exception) { domain }
                            } else {
                                domain
                            }
                            // Normalize host
                            host = host.removePrefix("www.")
                            
                            val hostsToTry = listOf(host, "www.$host")
                            
                            for (h in hostsToTry) {
                                for (path in commonPaths) {
                                    val testUrl = "https://$h$path"
                                    try {
                                        Log.d("FeedSearch", "Trying common path: $testUrl")
                                        val response = Jsoup.connect(testUrl)
                                            .userAgent("Mozilla/5.0")
                                            .timeout(3000)
                                            .sslSocketFactory(createTrustAllSslSocketFactory())
                                            .ignoreContentType(true)
                                            .execute()
                                        if (response.contentType()?.contains("xml") == true) {
                                            results.add(DiscoveredFeed("Direct Feed: $path", testUrl, queryIconUrl, clearbitName))
                                        }
                                    } catch (e: Exception) { }
                                }
                                if (results.isNotEmpty()) break
                            }
                        }
                        
                        // Si ya encontramos feeds para un dominio de Clearbit, paramos de probar los demás dominios
                        if (results.isNotEmpty()) break
                    }

                    results.distinctBy { it.url }
                }
                Log.d("FeedSearch", "Final discovery count: ${discovered.size}")
                _discoveredFeeds.value = discovered
            } catch (e: Exception) {
                Log.e("FeedSearch", "SearchFeeds top level error: ${e.message}")
            } finally {
                _isSearching.value = false
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
