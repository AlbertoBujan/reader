package com.boaxente.riffle.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.boaxente.riffle.data.model.CommentNode
import com.boaxente.riffle.data.repository.CommentRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CommentsViewModel @Inject constructor(
    private val commentRepository: CommentRepository
) : ViewModel() {
    
    private val _rootComments = MutableStateFlow<List<CommentNode>>(emptyList())
    
    // Mapa de parentId -> Lista de Comentarios (respuestas)
    private val _loadedReplies = MutableStateFlow<Map<String, List<com.boaxente.riffle.data.model.Comment>>>(emptyMap())
    
    val comments: StateFlow<List<CommentNode>> = kotlinx.coroutines.flow.combine(_rootComments, _loadedReplies) { roots, loaded ->
        mergeTree(roots, loaded)
    }.stateIn(viewModelScope, kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5000), emptyList())
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _commentCount = MutableStateFlow(0)
    val commentCount: StateFlow<Int> = _commentCount.asStateFlow()
    
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()
    
    private val _replyingTo = MutableStateFlow<CommentNode?>(null)
    val replyingTo: StateFlow<CommentNode?> = _replyingTo.asStateFlow()
    
    // Estado para tracking de cargas en progreso de respuestas
    private val _loadingRepliesFor = MutableStateFlow<Set<String>>(emptySet())
    val loadingRepliesFor: StateFlow<Set<String>> = _loadingRepliesFor.asStateFlow()

    private var currentArticleLink: String? = null
    private var currentArticleTitle: String = ""
    
    fun loadComments(articleLink: String, articleTitle: String) {
        currentArticleLink = articleLink
        currentArticleTitle = articleTitle
        
        viewModelScope.launch {
            _isLoading.value = true
            commentRepository.getComments(articleLink).collect { commentNodes ->
                _rootComments.value = commentNodes
                _isLoading.value = false
            }
        }
        
        viewModelScope.launch {
            commentRepository.getCommentCount(articleLink).collect { count ->
                _commentCount.value = count
            }
        }
    }
    
    fun loadReplies(commentId: String) {
        val articleLink = currentArticleLink ?: return
        if (_loadingRepliesFor.value.contains(commentId)) return // Ya cargando
        
        viewModelScope.launch {
            _loadingRepliesFor.value = _loadingRepliesFor.value + commentId
            
            commentRepository.getReplies(articleLink, commentId).onSuccess { replies ->
                val currentLoaded = _loadedReplies.value.toMutableMap()
                currentLoaded[commentId] = replies
                _loadedReplies.value = currentLoaded
            }.onFailure { e ->
                _error.value = "Error loading replies: ${e.message}"
            }
            
            _loadingRepliesFor.value = _loadingRepliesFor.value - commentId
        }
    }
    
    // Función recursiva para reconstruir el árbol combinando raíces (o nodos) con respuestas cargadas
    private fun mergeTree(nodes: List<CommentNode>, loaded: Map<String, List<com.boaxente.riffle.data.model.Comment>>): List<CommentNode> {
        return nodes.map { node ->
            val replies = loaded[node.comment.id]
            if (replies != null) {
                // Si tenemos respuestas cargadas para este nodo
                val replyNodes = replies.map { comment ->
                    CommentNode(comment = comment, depth = node.depth + 1) // Base depth, replies empty initially
                }
                // Recursivamente intentar mergear respuestas de estas respuestas (por si ya estaban cargadas también)
                val mergedReplies = mergeTree(replyNodes, loaded)
                node.copy(replies = mergedReplies)
            } else {
                // Si no hay respuestas cargadas, mantenemos el nodo tal cual (con replies vacíos)
                // Ojo: si el nodo ya tenía replies (del flujo realtime original?), lo sobrescribimos.
                // En nuestra nueva lógica getComments solo trae raíces sin hijos, así que node.replies es empty.
                node
            }
        }
    }
    
    fun addComment(text: String) {
        val articleLink = currentArticleLink ?: return
        val parentNode = _replyingTo.value
        val parentId = parentNode?.comment?.id
        
        viewModelScope.launch {
            _isLoading.value = true
            val result = commentRepository.addComment(
                articleLink = articleLink,
                articleTitle = currentArticleTitle,
                text = text,
                parentId = parentId
            )
            
            result.onSuccess { newComment ->
                // Si añadimos una respuesta, actualizamos localmente las respuestas cargadas para que se vea inmediato
                if (parentId != null) {
                    val currentLoaded = _loadedReplies.value.toMutableMap()
                    val existing = currentLoaded[parentId] ?: emptyList()
                    currentLoaded[parentId] = existing + newComment
                    _loadedReplies.value = currentLoaded
                } 
                // Si es comentario raíz, el flujo realtime de getComments lo añadirá automáticamente
            }
            
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
            // Nota: Si borramos una respuesta, el flujo realtime no lo detecta automáticamente si no estamos escuchando esa sub-colección.
            // Pero como las respuestas las gestionamos manualmente en _loadedReplies...
            // Deberíamos eliminarla de _loadedReplies localmente tras el borrado exitoso.
            // Sin embargo, obtener el parentId del comentario a borrar es complicado solo con el ID sin buscarlo.
            // Una opción simple es recargar las respuestas del padre si lo sabemos, o dejar que el usuario "refresque" colapsando/expandiendo.
            // PERO `deleteComment` en repo actualiza el contador del padre.
            
            commentRepository.deleteComment(articleLink, commentId).onSuccess { 
                // Forzar refresco visual eliminando de loadedReplies?
                // Sería complejo buscar dónde estaba. 
                // Mejor:
                // Si es root, el listener de getComments lo quita.
                // Si es respuesta, no tenemos listener. 
                // Podemos iterar _loadedReplies para borrarlo.
                
                val currentLoaded = _loadedReplies.value.toMutableMap()
                var parentIdFound: String? = null
                
                for ((pid, replies) in currentLoaded) {
                    if (replies.any { it.id == commentId }) {
                        currentLoaded[pid] = replies.filter { it.id != commentId }
                        parentIdFound = pid
                        break
                    }
                }
                
                if (parentIdFound != null) {
                    _loadedReplies.value = currentLoaded
                }
            }.onFailure { e ->
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
