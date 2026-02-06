package com.boaxente.riffle.data.model

import com.google.firebase.Timestamp

/**
 * Perfil social del usuario con estadísticas de interacción.
 * Se guarda en Firestore en /users/{userId}/profile/social
 */
data class UserProfile(
    val displayName: String? = null,
    val totalComments: Int = 0,
    val totalLikes: Int = 0,
    val totalDislikes: Int = 0
) {
    constructor() : this(displayName = null)
    
    fun toMap(): Map<String, Any?> = mapOf(
        "displayName" to displayName,
        "totalComments" to totalComments,
        "totalLikes" to totalLikes,
        "totalDislikes" to totalDislikes
    )
}

/**
 * Interacción del usuario (comentario o respuesta) en un artículo.
 */
data class UserInteraction(
    val id: String = "",
    val articleId: String = "",
    val articleTitle: String = "",
    val articleLink: String = "",
    val type: String = "comment",  // "comment" o "reply"
    val commentText: String = "",
    val commentId: String = "", // Added field
    val createdAt: Timestamp = Timestamp.now()
) {
    constructor() : this(id = "")
    
    fun toMap(): Map<String, Any?> = mapOf(
        "articleId" to articleId,
        "articleTitle" to articleTitle,
        "articleLink" to articleLink,
        "type" to type,
        "commentText" to commentText,
        "commentId" to commentId,
        "createdAt" to createdAt
    )
}
