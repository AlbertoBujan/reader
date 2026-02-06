package com.boaxente.riffle.data.model

import com.google.firebase.Timestamp

/**
 * Modelo de datos para comentarios de artículos.
 * Los comentarios se guardan en Firestore en /comments/{articleId}/comments/{commentId}
 */
data class Comment(
    val id: String = "",
    val userId: String = "",
    val userName: String = "",
    val userEmail: String = "",
    val text: String = "",
    val parentId: String? = null,  // null si es comentario raíz
    val createdAt: Timestamp = Timestamp.now(),
    val likes: Int = 0,
    val dislikes: Int = 0,
    val likedBy: List<String> = emptyList(),
    val dislikedBy: List<String> = emptyList(),
    val replyCount: Int = 0,
    val userPhotoUrl: String = ""
) {
    // Constructor sin argumentos requerido por Firestore
    constructor() : this(id = "")
    
    /**
     * Convierte el modelo a un Map para guardar en Firestore
     */
    fun toMap(): Map<String, Any?> = mapOf(
        "userId" to userId,
        "userName" to userName,
        "userEmail" to userEmail,
        "text" to text,
        "parentId" to parentId,
        "createdAt" to createdAt,
        "likes" to likes,
        "dislikes" to dislikes,
        "likedBy" to likedBy,
        "dislikedBy" to dislikedBy,
        "replyCount" to replyCount,
        "userPhotoUrl" to userPhotoUrl
    )
}

/**
 * Estructura para mostrar comentarios anidados en la UI
 */
data class CommentNode(
    val comment: Comment,
    val replies: List<CommentNode> = emptyList(),
    val depth: Int = 0
)
