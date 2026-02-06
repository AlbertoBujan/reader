package com.boaxente.riffle.data.repository

import com.boaxente.riffle.data.model.Comment
import com.boaxente.riffle.data.model.CommentNode
import com.boaxente.riffle.data.model.UserInteraction
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.CollectionReference
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
            .whereEqualTo("parentId", null) // Solo comentarios raíz
            // Eliminamos orderBy de la query para evitar necesitar un índice compuesto
            // Ordenaremos en memoria ya que la cantidad de comentarios raíz no suele ser masiva
            
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
            
            // Ya no construimos todo el árbol, solo nodos raíz con replies vacíos por ahora
            // La UI usará replyCount para saber si hay respuestas
            // Ordenamos en cliente: primero por likes, luego por fecha
            val nodes = comments
                .sortedWith(compareByDescending<Comment> { it.likes }.thenByDescending { it.createdAt })
                .map { CommentNode(it, emptyList(), 0) }
                
            trySend(nodes)
        }
        
        awaitClose { listener.remove() }
    }

    suspend fun getReplies(articleLink: String, parentId: String): Result<List<Comment>> {
        val articleId = hashString(articleLink)
        return try {
            val snapshot = firestore.collection("comments")
                .document(articleId)
                .collection("comments")
                .whereEqualTo("parentId", parentId)
                // Eliminamos orderBy para evitar índice compuesto. Ordenamos en memoria.
                .get()
                .await()
            
            val replies = snapshot.documents.mapNotNull { doc ->
                 doc.toObject(Comment::class.java)?.copy(id = doc.id)
            }.sortedBy { it.createdAt } // Orden cronológico (más antiguo primero)
            
            Result.success(replies)
        } catch (e: Exception) {
            Result.failure(e)
        }
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
            userPhotoUrl = user.photoUrl?.toString() ?: "", // Guardar foto
            text = text,
            parentId = parentId,
            createdAt = Timestamp.now()
        )
        
        return try {
            firestore.runTransaction { transaction ->
                // 1. Crear referencia para el nuevo comentario
                val newCommentRef = commentsRef.document()
                val commentWithId = comment.copy(id = newCommentRef.id)
                
                // 2. Guardar el comentario
                transaction.set(newCommentRef, commentWithId.toMap())
                
                // 3. Si es respuesta, incrementar contador del padre
                if (parentId != null) {
                    val parentRef = commentsRef.document(parentId)
                    transaction.update(parentRef, "replyCount", FieldValue.increment(1))
                }
                
                // Retornar el comentario para usarlo fuera (aunque id ya lo tenemos)
                commentWithId
            }.await().let { newComment ->
                 // Acciones post-transacción (no críticas para la integridad de datos del comentario en sí)
                
                // Registrar la interacción del usuario
                saveUserInteraction(
                    articleId = articleId,
                    articleTitle = articleTitle,
                    articleLink = articleLink,
                    type = if (parentId == null) "comment" else "reply",
                    commentText = text,
                    commentId = newComment.id
                )
                
                // Incrementar contador de comentarios del usuario
                incrementUserCommentCount()
                
                Result.success(newComment)
            }
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
     * Elimina un comentario y todas sus respuestas recursivamente
     */
    suspend fun deleteComment(articleLink: String, commentId: String): Result<Unit> {
        val currentUser = auth.currentUser ?: return Result.failure(Exception("Usuario no autenticado"))
        val articleId = hashString(articleLink)
        
        val commentsCollection = firestore.collection("comments")
            .document(articleId)
            .collection("comments")
            
        return try {
            // 1. Obtener todos los comentarios a eliminar (target + descendientes)
            val commentsToDelete = getAllDescendants(commentsCollection, commentId).toMutableList()
            
            // Añadir el comentario objetivo
            val targetDoc = commentsCollection.document(commentId).get().await()
            if (targetDoc.exists()) {
                targetDoc.toObject(Comment::class.java)?.copy(id = targetDoc.id)?.let {
                    commentsToDelete.add(it)
                }
            }

            if (commentsToDelete.isEmpty()) return Result.success(Unit)
            
            // 2. Ejecutar borrado en batch
            val batch = firestore.batch()
            commentsToDelete.forEach { comment ->
                batch.delete(commentsCollection.document(comment.id))
            }
            
            // Si el comentario principal eliminado (target) tiene padre, decrementar contador del padre
            // Nota: Si borramos una respuesta que tiene respuestas, borramos todo el subárbol,
            // pero el "padre" de la respuesta original solo pierde 1 respuesta directa.
            // targetDoc ya lo leímos arriba.
             targetDoc.toObject(Comment::class.java)?.parentId?.let { parentId ->
                 val parentRef = commentsCollection.document(parentId)
                 batch.update(parentRef, "replyCount", FieldValue.increment(-1))
             }

            // 3. Actualizar contadores de usuarios afectados (DENTRO DEL BATCH para atomicidad)
            // Agrupar por usuario para restar el número correcto de comentarios a cada uno
            val usersAffected = commentsToDelete.groupBy { it.userId }
            usersAffected.forEach { (userId, userComments) ->
                val userStatsRef = firestore.collection("users")
                    .document(userId)
                    .collection("profile")
                    .document("social")
                
                batch.set(
                    userStatsRef, 
                    mapOf("totalComments" to FieldValue.increment(-userComments.size.toLong())), 
                    com.google.firebase.firestore.SetOptions.merge()
                )
                
                // 4. Buscar y borrar las interacciones asociadas a estos comentarios
                val interactionsRef = userStatsRef.collection("interactions")
                
                // Buscamos las interacciones una a una (o en grupos si pudiéramos) para añadirlas al batch
                // Esto añade lecturas pero asegura limpieza
                for (comment in userComments) {
                    // Query para encontrar la interacción asociada al comentario
                    val interactionSnapshot = interactionsRef
                        .whereEqualTo("commentId", comment.id)
                        .get()
                        .await()
                        
                    for (doc in interactionSnapshot.documents) {
                        batch.delete(doc.reference)
                    }
                }
            }
             
            batch.commit().await()
             
             Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    private suspend fun getAllDescendants(collection: CollectionReference, parentId: String): List<Comment> {
        val descendants = mutableListOf<Comment>()
        
        val childrenSnapshot = collection.whereEqualTo("parentId", parentId).get().await()
        val children = childrenSnapshot.documents.mapNotNull { it.toObject(Comment::class.java)?.copy(id = it.id) }
        
        descendants.addAll(children)
        
        for (child in children) {
            descendants.addAll(getAllDescendants(collection, child.id))
        }
        
        return descendants
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
        commentText: String,
        commentId: String
    ) {
        val user = auth.currentUser ?: return
        val interaction = UserInteraction(
            articleId = articleId,
            articleTitle = articleTitle,
            articleLink = articleLink,
            type = type,
            commentText = commentText,
            commentId = commentId,
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

    private suspend fun decrementUserCommentCount(userId: String, count: Int) {
        try {
            firestore.collection("users")
                .document(userId)
                .collection("profile")
                .document("social")
                .set(
                    mapOf("totalComments" to FieldValue.increment(-count.toLong())),
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
