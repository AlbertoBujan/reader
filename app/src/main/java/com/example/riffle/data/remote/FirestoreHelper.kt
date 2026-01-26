package com.example.riffle.data.remote

import com.example.riffle.data.local.dao.FeedDao
import com.example.riffle.data.local.entity.SourceEntity
import com.example.riffle.data.local.entity.ArticleEntity
import com.example.riffle.data.local.entity.FolderEntity
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
    private val preferencesManager: PreferencesManager,
    @dagger.hilt.android.qualifiers.ApplicationContext private val context: android.content.Context
) {

    private var feedsListener: ListenerRegistration? = null
    private var readStatesListener: ListenerRegistration? = null
    private var savedStatesListener: ListenerRegistration? = null
    private var settingsListener: ListenerRegistration? = null
    
    private val scope = CoroutineScope(Dispatchers.IO)

    fun startSync() {
        auth.addAuthStateListener { firebaseAuth ->
            val user = firebaseAuth.currentUser
            if (user != null) {
                // User logged in, start listeners
                startFeedsListener(user.uid)
                startReadStatesListener(user.uid)
                startSavedStatesListener(user.uid)
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
        savedStatesListener?.remove()
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

                    var hasNewFeeds = false
                    
                    // Add new remote feeds to local
                    snapshot.documents.forEach { doc ->
                        val url = doc.getString("url")
                        val title = doc.getString("title")
                        val folderName = doc.getString("folderName")
                        
                        if (!folderName.isNullOrBlank()) {
                            feedDao.insertFolder(FolderEntity(folderName))
                        }
                        val iconUrl = doc.getString("iconUrl")
                        
                        if (url != null && !localUrls.contains(url)) {
                             val source = SourceEntity(url = url, title = title ?: url, folderName = folderName, iconUrl = iconUrl)
                             feedDao.insertSource(source)
                             hasNewFeeds = true
                        } else if (url != null) {
                            // Sync folder or title updates if changed
                            val existing = feedDao.getSourceByUrl(url)
                            if (existing != null && (existing.folderName != folderName || existing.title != title)) {
                                 feedDao.insertSource(existing.copy(folderName = folderName, title = title ?: existing.title))
                            }
                        }
                    }
                    
                    // Delete local feeds that are missing from remote
                    // Note: Firestore snapshot includes local pending writes, so this is safe for new feeds too
                    val feedsToDelete = localUrls.minus(remoteUrls)
                    feedsToDelete.forEach { url ->
                        feedDao.deleteSource(url)
                    }
                    
                    if (hasNewFeeds) {
                         val syncRequest = androidx.work.OneTimeWorkRequestBuilder<com.example.riffle.worker.FeedSyncWorker>()
                            .setConstraints(
                                androidx.work.Constraints.Builder()
                                    .setRequiredNetworkType(androidx.work.NetworkType.CONNECTED)
                                    .build()
                            )
                            .build()
                        androidx.work.WorkManager.getInstance(context).enqueue(syncRequest)
                    }
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
                        val articleLink = doc.getString("articleUrl") 
                        if (articleLink != null) {
                             remoteReadLinks.add(articleLink)
                             feedDao.markArticleAsRead(articleLink)
                        }
                    }
                }
            }
        }
    }

    private fun startSavedStatesListener(userId: String) {
        val savedRef = firestore.collection("users").document(userId).collection("saved_states")
        savedStatesListener = savedRef.addSnapshotListener { snapshot, e ->
            if (e != null) return@addSnapshotListener
            
            if (snapshot != null) {
                scope.launch {
                    // Update local database. 
                    // Note: This logic assumes if it's in the collection, it's saved.
                    // If it's REMOVED from collection, we might want to unsave it?
                    // Snapshot listener gives document changes.
                    
                    snapshot.documentChanges.forEach { change ->
                         val doc = change.document
                         val articleLink = doc.getString("articleUrl")
                         
                         if (articleLink != null) {
                             when (change.type) {
                                 com.google.firebase.firestore.DocumentChange.Type.ADDED,
                                 com.google.firebase.firestore.DocumentChange.Type.MODIFIED -> {
                                     remoteSavedLinks.add(articleLink)
                                     feedDao.updateArticleSavedStatus(articleLink, true)
                                 }
                                 com.google.firebase.firestore.DocumentChange.Type.REMOVED -> {
                                     remoteSavedLinks.remove(articleLink)
                                     feedDao.updateArticleSavedStatus(articleLink, false)
                                 }
                             }
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
                     // Check common settings
                     
                     // Theme: 1 = dark, 0 = light
                     val theme = snapshot.getLong("theme")
                     if (theme != null) {
                         preferencesManager.setDarkMode(theme.toInt() == 1)
                     }

                     // Language
                     val language = snapshot.getString("language")
                     if (language != null) {
                         preferencesManager.setLanguage(language)
                     }

                     // Sync Interval
                     val syncInterval = snapshot.getLong("sync_interval")
                     if (syncInterval != null) {
                         preferencesManager.setSyncInterval(syncInterval)
                     }

                     // Mark on Scroll
                     val markOnScroll = snapshot.getBoolean("mark_on_scroll")
                     if (markOnScroll != null) {
                         preferencesManager.setMarkAsReadOnScroll(markOnScroll)
                     }
                }
            }
        }
    }

    // --- State Caching for Race Conditions ---
    private val remoteReadLinks = java.util.Collections.synchronizedSet(HashSet<String>())
    private val remoteSavedLinks = java.util.Collections.synchronizedSet(HashSet<String>())

    fun applyRemoteStates() {
        scope.launch {
            // Re-apply known states to potentially new-arrived articles
            val readCopy = remoteReadLinks.toSet() 
            val savedCopy = remoteSavedLinks.toSet()
            
            // Simplified batch or loop. For SQLite room, loop is fine for hundreds.
            // For thousands we might want a batch query "UPDATE... WHERE link IN (...)"
            // But let's stick to loop for now or custom Dao method if needed.
            
            // Actually, we can just spawn jobs.
            readCopy.forEach { link -> feedDao.markArticleAsRead(link) }
            savedCopy.forEach { link -> feedDao.updateArticleSavedStatus(link, true) }
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

    fun updateSavedStatusInCloud(articleLink: String, isSaved: Boolean) {
        val user = auth.currentUser ?: return
        val docId = hashString(articleLink)
        
        if (isSaved) {
            val data = hashMapOf(
                "articleUrl" to articleLink,
                "savedAt" to System.currentTimeMillis()
            )
            firestore.collection("users").document(user.uid).collection("saved_states")
                .document(docId)
                .set(data, SetOptions.merge())
        } else {
             firestore.collection("users").document(user.uid).collection("saved_states")
                .document(docId)
                .delete()
        }
    }

    fun deleteSourceFromCloud(url: String) {
        val user = auth.currentUser ?: return
        val docId = hashString(url)
        firestore.collection("users").document(user.uid).collection("feeds")
            .document(docId)
            .delete()
    }

    fun updateSourceTitleInCloud(url: String, newTitle: String) {
        val user = auth.currentUser ?: return
        val docId = hashString(url)
        val data = hashMapOf("title" to newTitle)
        firestore.collection("users").document(user.uid).collection("feeds")
            .document(docId)
            .set(data, SetOptions.merge())
    }
    
    // --- Settings Sync ---
    
    fun updateSettingInCloud(key: String, value: Any) {
        val user = auth.currentUser ?: return
        val data = hashMapOf(key to value)
        firestore.collection("users").document(user.uid).collection("settings")
            .document("preferences")
            .set(data, SetOptions.merge())
    }

    // Helper
    private fun hashString(input: String): String {
        return MessageDigest.getInstance("MD5")
            .digest(input.toByteArray())
            .joinToString("") { "%02x".format(it) }
    }
}
