package com.example.inoreaderlite.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.inoreaderlite.data.local.dao.FeedDao
import com.example.inoreaderlite.data.local.entity.ArticleEntity
import com.example.inoreaderlite.data.local.entity.FolderEntity
import com.example.inoreaderlite.data.local.entity.SourceEntity
import com.example.inoreaderlite.domain.usecase.AddSourceUseCase
import com.example.inoreaderlite.domain.usecase.GetAllSourcesUseCase
import com.example.inoreaderlite.domain.usecase.GetArticlesUseCase
import com.example.inoreaderlite.domain.usecase.MarkArticleReadUseCase
import com.example.inoreaderlite.domain.usecase.SyncFeedsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface FeedUiState {
    data object Loading : FeedUiState
    data class Success(val articles: List<ArticleEntity>) : FeedUiState
    data class Error(val message: String) : FeedUiState
}

@HiltViewModel
class MainViewModel @Inject constructor(
    private val getArticlesUseCase: GetArticlesUseCase,
    private val addSourceUseCase: AddSourceUseCase,
    private val syncFeedsUseCase: SyncFeedsUseCase,
    private val markArticleReadUseCase: MarkArticleReadUseCase,
    private val feedDao: FeedDao,
    getAllSourcesUseCase: GetAllSourcesUseCase
) : ViewModel() {

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _selectedSource = MutableStateFlow<String?>(null)
    val selectedSource: StateFlow<String?> = _selectedSource.asStateFlow()

    private val _markAsReadOnScroll = MutableStateFlow(false)
    val markAsReadOnScroll: StateFlow<Boolean> = _markAsReadOnScroll.asStateFlow()

    val sources: StateFlow<List<SourceEntity>> = getAllSourcesUseCase()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val folders: StateFlow<List<FolderEntity>> = feedDao.getAllFolders()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<FeedUiState> = _selectedSource
        .flatMapLatest { selector ->
            when {
                selector == null -> getArticlesUseCase()
                selector.startsWith("folder:") -> {
                    val folderName = selector.removePrefix("folder:")
                    feedDao.getArticlesByFolder(folderName)
                }
                else -> getArticlesUseCase(selector)
            }
        }
        .map<List<ArticleEntity>, FeedUiState> { articles ->
            FeedUiState.Success(articles.filter { !it.isRead })
        }
        .catch { emit(FeedUiState.Error(it.message ?: "Unknown error")) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = FeedUiState.Loading
        )

    fun getArticle(url: String): Flow<ArticleEntity?> {
        return feedDao.getArticleByLink(url)
    }

    fun selectSource(url: String?) {
        _selectedSource.value = url
    }

    fun selectFolder(name: String) {
        _selectedSource.value = "folder:$name"
    }

    fun toggleMarkAsReadOnScroll(enabled: Boolean) {
        _markAsReadOnScroll.value = enabled
    }

    fun addSource(url: String) {
        viewModelScope.launch {
            try {
                addSourceUseCase(url)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun addFolder(name: String) {
        viewModelScope.launch {
            feedDao.insertFolder(FolderEntity(name))
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
}
