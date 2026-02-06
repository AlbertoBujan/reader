package com.boaxente.riffle.data.repository

import com.boaxente.riffle.data.model.UserInteraction
import com.boaxente.riffle.data.model.UserProfile
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserProfileRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) {
    
    /**
     * Obtiene el perfil social del usuario actual en tiempo real
     */
    fun getCurrentUserProfile(): Flow<UserProfile?> = callbackFlow {
        val user = auth.currentUser
        if (user == null) {
            trySend(null)
            close()
            return@callbackFlow
        }
        
        val profileRef = firestore.collection("users")
            .document(user.uid)
            .collection("profile")
            .document("social")
        
        val listener = profileRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                trySend(null)
                return@addSnapshotListener
            }
            
            val profile = snapshot?.toObject(UserProfile::class.java)
            trySend(profile ?: UserProfile())
        }
        
        awaitClose { listener.remove() }
    }
    
    /**
     * Obtiene el perfil social de un usuario específico
     */
    fun getUserProfile(userId: String): Flow<UserProfile?> = callbackFlow {
        val profileRef = firestore.collection("users")
            .document(userId)
            .collection("profile")
            .document("social")
        
        val listener = profileRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                trySend(null)
                return@addSnapshotListener
            }
            
            val profile = snapshot?.toObject(UserProfile::class.java)
            trySend(profile ?: UserProfile())
        }
        
        awaitClose { listener.remove() }
    }
    
    /**
     * Actualiza el nombre público del usuario
     */
    suspend fun updateDisplayName(name: String): Result<Unit> {
        val user = auth.currentUser ?: return Result.failure(Exception("Usuario no autenticado"))
        
        return try {
            firestore.collection("users")
                .document(user.uid)
                .collection("profile")
                .document("social")
                .set(mapOf("displayName" to name), SetOptions.merge())
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Obtiene el displayName del usuario actual
     */
    suspend fun getDisplayName(): String? {
        val user = auth.currentUser ?: return null
        return try {
            val doc = firestore.collection("users")
                .document(user.uid)
                .collection("profile")
                .document("social")
                .get()
                .await()
            doc.getString("displayName")
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * Obtiene las interacciones del usuario actual (comentarios y respuestas)
     */
    fun getUserInteractions(): Flow<List<UserInteraction>> = callbackFlow {
        val user = auth.currentUser
        if (user == null) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }
        
        val interactionsRef = firestore.collection("users")
            .document(user.uid)
            .collection("profile")
            .document("social")
            .collection("interactions")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(50)
        
        val listener = interactionsRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                trySend(emptyList())
                return@addSnapshotListener
            }
            
            val interactions = snapshot?.documents?.mapNotNull { doc ->
                doc.toObject(UserInteraction::class.java)?.copy(id = doc.id)
            } ?: emptyList()
            
            trySend(interactions)
        }
        
        awaitClose { listener.remove() }
    }
}
