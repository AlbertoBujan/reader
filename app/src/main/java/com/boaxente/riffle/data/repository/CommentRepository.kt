package com.boaxente.riffle.data.repository

import com.boaxente.riffle.data.model.Comment
import com.boaxente.riffle.data.model.CommentNode
import com.boaxente.riffle.data.model.UserInteraction
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CommentRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) {
    
    /**
     * Obtiene los comentarios de un artículo en tiempo real como un árbol
     */
    fun getComments(articleLink: String): Flow<List<CommentNode>> = callbackFlow {
        val articleId = hashString(articleLink)
        val commentsRef = firestore.collection("comments")
            .document(articleId)
            .collection("comments")
            .orderBy("likes", Query.Direction.DESCENDING)
            .orderBy("createdAt", Query.Direction.DESCENDING)
        
        val listener = commentsRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                // Log error pero no cerrar el flow - devolver lista vacía
                error.printStackTrace()
                trySend(emptyList())
                return@addSnapshotListener
            }
            
            val comments = snapshot?.documents?.mapNotNull { doc ->
                doc.toObject(Comment::class.java)?.copy(id = doc.id)
            } ?: emptyList()
            
            // Convertir lista plana a árbol
            val tree = buildCommentTree(comments)
            trySend(tree)
        }
        
        awaitClose { listener.remove() }
    }
    
    /**
     * Obtiene el número de comentarios de un artículo
     */
    fun getCommentCount(articleLink: String): Flow<Int> = callbackFlow {
        val articleId = hashString(articleLink)
        val commentsRef = firestore.collection("comments")
            .document(articleId)
            .collection("comments")
        
        val listener = commentsRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                trySend(0)
                return@addSnapshotListener
            }
            trySend(snapshot?.size() ?: 0)
        }
        
        awaitClose { listener.remove() }
    }
    
    /**
     * Añade un nuevo comentario a un artículo
     */
    suspend fun addComment(
        articleLink: String,
        articleTitle: String,
        text: String,
        parentId: String? = null
    ): Result<Comment> {
        val user = auth.currentUser ?: return Result.failure(Exception("Usuario no autenticado"))
        
        val articleId = hashString(articleLink)
        val commentsRef = firestore.collection("comments")
            .document(articleId)
            .collection("comments")
        
        // Obtener displayName del perfil del usuario
        val displayName = getDisplayName(user.uid) ?: user.displayName ?: user.email?.split("@")?.firstOrNull() ?: "Anónimo"
        
        val comment = Comment(
            userId = user.uid,
            userName = displayName,
            userEmail = user.email ?: "",
            text = text,
            parentId = parentId,
            createdAt = Timestamp.now()
        )
        
        return try {
            val docRef = commentsRef.add(comment.toMap()).await()
            val newComment = comment.copy(id = docRef.id)
            
            // Registrar la interacción del usuario
            saveUserInteraction(
                articleId = articleId,
                articleTitle = articleTitle,
                articleLink = articleLink,
                type = if (parentId == null) "comment" else "reply",
                commentText = text
            )
            
            // Incrementar contador de comentarios del usuario
            incrementUserCommentCount()
            
            Result.success(newComment)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Da like a un comentario
     */
    suspend fun likeComment(articleLink: String, commentId: String): Result<Unit> {
        val user = auth.currentUser ?: return Result.failure(Exception("Usuario no autenticado"))
        val articleId = hashString(articleLink)
        
        val commentRef = firestore.collection("comments")
            .document(articleId)
            .collection("comments")
            .document(commentId)
        
        return try {
            firestore.runTransaction { transaction ->
                val snapshot = transaction.get(commentRef)
                val likedBy = (snapshot.get("likedBy") as? List<*>)?.filterIsInstance<String>() ?: emptyList()
                val dislikedBy = (snapshot.get("dislikedBy") as? List<*>)?.filterIsInstance<String>() ?: emptyList()
                
                when {
                    likedBy.contains(user.uid) -> {
                        // Quitar like
                        transaction.update(commentRef, mapOf(
                            "likes" to FieldValue.increment(-1),
                            "likedBy" to FieldValue.arrayRemove(user.uid)
                        ))
                    }
                    dislikedBy.contains(user.uid) -> {
                        // Cambiar de dislike a like
                        transaction.update(commentRef, mapOf(
                            "likes" to FieldValue.increment(1),
                            "dislikes" to FieldValue.increment(-1),
                            "likedBy" to FieldValue.arrayUnion(user.uid),
                            "dislikedBy" to FieldValue.arrayRemove(user.uid)
                        ))
                    }
                    else -> {
                        // Nuevo like
                        transaction.update(commentRef, mapOf(
                            "likes" to FieldValue.increment(1),
                            "likedBy" to FieldValue.arrayUnion(user.uid)
                        ))
                    }
                }
            }.await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Da dislike a un comentario
     */
    suspend fun dislikeComment(articleLink: String, commentId: String): Result<Unit> {
        val user = auth.currentUser ?: return Result.failure(Exception("Usuario no autenticado"))
        val articleId = hashString(articleLink)
        
        val commentRef = firestore.collection("comments")
            .document(articleId)
            .collection("comments")
            .document(commentId)
        
        return try {
            firestore.runTransaction { transaction ->
                val snapshot = transaction.get(commentRef)
                val likedBy = (snapshot.get("likedBy") as? List<*>)?.filterIsInstance<String>() ?: emptyList()
                val dislikedBy = (snapshot.get("dislikedBy") as? List<*>)?.filterIsInstance<String>() ?: emptyList()
                
                when {
                    dislikedBy.contains(user.uid) -> {
                        // Quitar dislike
                        transaction.update(commentRef, mapOf(
                            "dislikes" to FieldValue.increment(-1),
                            "dislikedBy" to FieldValue.arrayRemove(user.uid)
                        ))
                    }
                    likedBy.contains(user.uid) -> {
                        // Cambiar de like a dislike
                        transaction.update(commentRef, mapOf(
                            "dislikes" to FieldValue.increment(1),
                            "likes" to FieldValue.increment(-1),
                            "dislikedBy" to FieldValue.arrayUnion(user.uid),
                            "likedBy" to FieldValue.arrayRemove(user.uid)
                        ))
                    }
                    else -> {
                        // Nuevo dislike
                        transaction.update(commentRef, mapOf(
                            "dislikes" to FieldValue.increment(1),
                            "dislikedBy" to FieldValue.arrayUnion(user.uid)
                        ))
                    }
                }
            }.await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Convierte una lista plana de comentarios en una estructura de árbol
     */
    private fun buildCommentTree(comments: List<Comment>): List<CommentNode> {
        val commentMap = comments.associateBy { it.id }
        val rootComments = mutableListOf<CommentNode>()
        
        // Primero, construir los nodos raíz
        comments.filter { it.parentId == null }.forEach { comment ->
            rootComments.add(buildNode(comment, comments, 0))
        }
        
        return rootComments.sortedByDescending { it.comment.likes }
    }
    
    private fun buildNode(comment: Comment, allComments: List<Comment>, depth: Int): CommentNode {
        val replies = allComments
            .filter { it.parentId == comment.id }
            .map { buildNode(it, allComments, depth + 1) }
            .sortedByDescending { it.comment.likes }
        
        return CommentNode(comment = comment, replies = replies, depth = depth)
    }
    
    private suspend fun getDisplayName(userId: String): String? {
        return try {
            val doc = firestore.collection("users")
                .document(userId)
                .collection("profile")
                .document("social")
                .get()
                .await()
            doc.getString("displayName")
        } catch (e: Exception) {
            null
        }
    }
    
    private suspend fun saveUserInteraction(
        articleId: String,
        articleTitle: String,
        articleLink: String,
        type: String,
        commentText: String
    ) {
        val user = auth.currentUser ?: return
        val interaction = UserInteraction(
            articleId = articleId,
            articleTitle = articleTitle,
            articleLink = articleLink,
            type = type,
            commentText = commentText,
            createdAt = Timestamp.now()
        )
        
        try {
            firestore.collection("users")
                .document(user.uid)
                .collection("profile")
                .document("social")
                .collection("interactions")
                .add(interaction.toMap())
                .await()
        } catch (e: Exception) {
            // Log but don't fail the main operation
            e.printStackTrace()
        }
    }
    
    private suspend fun incrementUserCommentCount() {
        val user = auth.currentUser ?: return
        try {
            firestore.collection("users")
                .document(user.uid)
                .collection("profile")
                .document("social")
                .set(
                    mapOf("totalComments" to FieldValue.increment(1)),
                    com.google.firebase.firestore.SetOptions.merge()
                )
                .await()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    private fun hashString(input: String): String {
        return MessageDigest.getInstance("MD5")
            .digest(input.toByteArray())
            .joinToString("") { "%02x".format(it) }
    }
}
