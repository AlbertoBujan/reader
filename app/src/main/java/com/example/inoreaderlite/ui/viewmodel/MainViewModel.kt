package com.example.inoreaderlite.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.inoreaderlite.data.local.entity.ArticleEntity
import com.example.inoreaderlite.domain.usecase.AddSourceUseCase
import com.example.inoreaderlite.domain.usecase.GetArticlesUseCase
import com.example.inoreaderlite.domain.usecase.SyncFeedsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
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
    getArticlesUseCase: GetArticlesUseCase,
    private val addSourceUseCase: AddSourceUseCase,
    private val syncFeedsUseCase: SyncFeedsUseCase
) : ViewModel() {

    val uiState: StateFlow<FeedUiState> = getArticlesUseCase()
        .map<List<ArticleEntity>, FeedUiState> { FeedUiState.Success(it) }
        .catch { emit(FeedUiState.Error(it.message ?: "Unknown error")) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = FeedUiState.Loading
        )

    fun addSource(url: String) {
        viewModelScope.launch {
            try {
                addSourceUseCase(url)
            } catch (e: Exception) {
                // Handle specific error, maybe expose via another flow or event
                e.printStackTrace()
            }
        }
    }

    fun sync() {
        viewModelScope.launch {
            try {
                syncFeedsUseCase()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
