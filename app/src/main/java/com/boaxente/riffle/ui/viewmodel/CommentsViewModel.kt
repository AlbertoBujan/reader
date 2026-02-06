package com.boaxente.riffle.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.boaxente.riffle.data.model.CommentNode
import com.boaxente.riffle.data.repository.CommentRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CommentsViewModel @Inject constructor(
    private val commentRepository: CommentRepository
) : ViewModel() {
    
    private val _comments = MutableStateFlow<List<CommentNode>>(emptyList())
    val comments: StateFlow<List<CommentNode>> = _comments.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _commentCount = MutableStateFlow(0)
    val commentCount: StateFlow<Int> = _commentCount.asStateFlow()
    
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()
    
    private val _replyingTo = MutableStateFlow<CommentNode?>(null)
    val replyingTo: StateFlow<CommentNode?> = _replyingTo.asStateFlow()
    
    private var currentArticleLink: String? = null
    private var currentArticleTitle: String = ""
    
    fun loadComments(articleLink: String, articleTitle: String) {
        currentArticleLink = articleLink
        currentArticleTitle = articleTitle
        
        viewModelScope.launch {
            _isLoading.value = true
            commentRepository.getComments(articleLink).collect { commentNodes ->
                _comments.value = commentNodes
                _isLoading.value = false
            }
        }
        
        viewModelScope.launch {
            commentRepository.getCommentCount(articleLink).collect { count ->
                _commentCount.value = count
            }
        }
    }
    
    fun addComment(text: String) {
        val articleLink = currentArticleLink ?: return
        val parentId = _replyingTo.value?.comment?.id
        
        viewModelScope.launch {
            _isLoading.value = true
            val result = commentRepository.addComment(
                articleLink = articleLink,
                articleTitle = currentArticleTitle,
                text = text,
                parentId = parentId
            )
            
            result.onFailure { e ->
                _error.value = e.message
            }
            
            _replyingTo.value = null
            _isLoading.value = false
        }
    }
    
    fun likeComment(commentId: String) {
        val articleLink = currentArticleLink ?: return
        viewModelScope.launch {
            commentRepository.likeComment(articleLink, commentId).onFailure { e ->
                _error.value = e.message
            }
        }
    }
    
    fun dislikeComment(commentId: String) {
        val articleLink = currentArticleLink ?: return
        viewModelScope.launch {
            commentRepository.dislikeComment(articleLink, commentId).onFailure { e ->
                _error.value = e.message
            }
        }
    }
    
    fun deleteComment(commentId: String) {
        val articleLink = currentArticleLink ?: return
        viewModelScope.launch {
            commentRepository.deleteComment(articleLink, commentId).onFailure { e ->
                _error.value = e.message
            }
        }
    }
    
    fun setReplyingTo(commentNode: CommentNode?) {
        _replyingTo.value = commentNode
    }
    
    fun clearError() {
        _error.value = null
    }
}
