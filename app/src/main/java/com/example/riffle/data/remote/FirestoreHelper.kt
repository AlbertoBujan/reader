package com.example.riffle.data.remote

import com.example.riffle.data.local.dao.FeedDao
import com.example.riffle.data.local.entity.SourceEntity
import com.example.riffle.data.local.entity.ArticleEntity
import com.example.riffle.data.local.PreferencesManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirestoreHelper @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth,
    private val feedDao: FeedDao,
    private val preferencesManager: PreferencesManager
) {

    private var feedsListener: ListenerRegistration? = null
    private var readStatesListener: ListenerRegistration? = null
    private var settingsListener: ListenerRegistration? = null
    
    private val scope = CoroutineScope(Dispatchers.IO)

    fun startSync() {
        auth.addAuthStateListener { firebaseAuth ->
            val user = firebaseAuth.currentUser
            if (user != null) {
                // User logged in, start listeners
                startFeedsListener(user.uid)
                startReadStatesListener(user.uid)
                startSettingsListener(user.uid)
            } else {
                // User logged out, remove listeners
                stopSync()
            }
        }
    }

    fun stopSync() {
        feedsListener?.remove()
        readStatesListener?.remove()
        settingsListener?.remove()
    }

    private fun startFeedsListener(userId: String) {
        val feedsRef = firestore.collection("users").document(userId).collection("feeds")
        feedsListener = feedsRef.addSnapshotListener { snapshot, e ->
            if (e != null) return@addSnapshotListener
            
            if (snapshot != null) {
                scope.launch {
                    val remoteUrls = snapshot.documents.mapNotNull { it.getString("url") }.toSet()
                    val localSources = feedDao.getAllSourcesList()
                    val localUrls = localSources.map { it.url }.toSet()

                    // Add new remote feeds to local
                    snapshot.documents.forEach { doc ->
                        val url = doc.getString("url")
                        val title = doc.getString("title")
                        val folderName = doc.getString("folderName")
                        val iconUrl = doc.getString("iconUrl")
                        
                        if (url != null && !localUrls.contains(url)) {
                             // This is a simplified insertion. 
                             // Ideally we should trigger a fetch for this new source, or just insert it and let the daily worker fetch it.
                             val source = SourceEntity(url = url, title = title ?: url, folderName = folderName, iconUrl = iconUrl)
                             feedDao.insertSource(source)
                        } else if (url != null) {
                            // Sync folder or title updates if changed
                            val existing = feedDao.getSourceByUrl(url)
                            if (existing != null && (existing.folderName != folderName || existing.title != title)) {
                                 feedDao.insertSource(existing.copy(folderName = folderName, title = title ?: existing.title))
                            }
                        }
                    }
                    
                    // Optional: Handle deletions (If remote doesn't have it, delete local? Or keep safe?)
                    // For now, let's assume additions-only or manual deletion syncing to be safe,
                    // but usually sync means mirroring. Let's stick to additive for MVP safety unless requested.
                }
            }
        }
    }

    private fun startReadStatesListener(userId: String) {
        val readRef = firestore.collection("users").document(userId).collection("read_states")
        readStatesListener = readRef.addSnapshotListener { snapshot, e ->
            if (e != null) return@addSnapshotListener
            
            if (snapshot != null) {
                scope.launch {
                    snapshot.documents.forEach { doc ->
                        val articleLink = doc.getString("articleUrl") // Assuming we use link as ID or field
                        if (articleLink != null) {
                             feedDao.markArticleAsRead(articleLink)
                        }
                    }
                }
            }
        }
    }

    private fun startSettingsListener(userId: String) {
        val settingsRef = firestore.collection("users").document(userId).collection("settings").document("preferences")
        settingsListener = settingsRef.addSnapshotListener { snapshot, e ->
            if (e != null) return@addSnapshotListener
            
            if (snapshot != null && snapshot.exists()) {
                scope.launch {
                     val theme = snapshot.getLong("theme")?.toInt()
                     // Add other settings as needed
                     // Example: preferencesManager.setTheme(theme)
                     // Since PreferencesManager is datastore, we probably need methods to update it from here.
                     // But typically PreferencesManager exposes Flow. We need to check if it has update methods exposed.
                }
            }
        }
    }

    // --- Upload Methods ---

    fun addSourceToCloud(source: SourceEntity) {
        val user = auth.currentUser ?: return
        val docId = hashString(source.url)
        val data = hashMapOf(
            "url" to source.url,
            "title" to source.title,
            "folderName" to source.folderName,
            "iconUrl" to source.iconUrl
        )
        firestore.collection("users").document(user.uid).collection("feeds")
            .document(docId)
            .set(data, SetOptions.merge())
    }

    fun markReadInCloud(articleLink: String) {
        val user = auth.currentUser ?: return
        val docId = hashString(articleLink)
        val data = hashMapOf(
            "articleUrl" to articleLink,
            "readAt" to System.currentTimeMillis()
        )
        firestore.collection("users").document(user.uid).collection("read_states")
            .document(docId)
            .set(data, SetOptions.merge())
    }
    
    fun updateSourceFolderInCloud(url: String, folderName: String?) {
        val user = auth.currentUser ?: return
        val docId = hashString(url)
        val data = hashMapOf("folderName" to folderName)
        firestore.collection("users").document(user.uid).collection("feeds")
            .document(docId)
            .set(data, SetOptions.merge())
    }

    // Helper
    private fun hashString(input: String): String {
        return MessageDigest.getInstance("MD5")
            .digest(input.toByteArray())
            .joinToString("") { "%02x".format(it) }
    }
}
