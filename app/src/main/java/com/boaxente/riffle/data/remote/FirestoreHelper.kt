package com.boaxente.riffle.data.remote

import com.boaxente.riffle.data.local.dao.FeedDao
import com.boaxente.riffle.data.local.entity.SourceEntity
import com.boaxente.riffle.data.local.entity.FolderEntity
import com.boaxente.riffle.data.local.PreferencesManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.FieldValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
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
    
    // Debounce / Batching
    private val debounceTime = 10000L // 10 seconds
    private var readDebounceJob: Job? = null
    private var savedDebounceJob: Job? = null
    
    private val readQueueAdding = mutableSetOf<String>()
    
    private val savedQueueAdding = mutableSetOf<String>()
    private val savedQueueRemoving = mutableSetOf<String>()
    
    private val queueMutex = Mutex()

    // --- State Caching for Race Conditions ---
    private val remoteReadLinks = java.util.Collections.synchronizedSet(HashSet<String>())
    private val remoteSavedLinks = java.util.Collections.synchronizedSet(HashSet<String>())

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
        
        // Flush any pending changes immediately?
        // Ideally we should await them, but this method is usually synchronous.
        // We'll let the jobs finish or die.
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
                            // Sync folder, title or icon updates if changed
                            val existing = feedDao.getSourceByUrl(url)
                            if (existing != null) {
                                val needUpdate = existing.folderName != folderName || 
                                                 existing.title != title ||
                                                 (iconUrl != null && existing.iconUrl != iconUrl)
                                
                                if (needUpdate) {
                                    feedDao.insertSource(existing.copy(
                                        folderName = folderName, 
                                        title = title ?: existing.title,
                                        iconUrl = iconUrl ?: existing.iconUrl
                                    ))
                                }
                            }
                        }
                    }
                    
                    // Delete local feeds that are missing from remote
                    val feedsToDelete = localUrls.minus(remoteUrls)
                    feedsToDelete.forEach { url ->
                        feedDao.deleteSource(url)
                    }
                    
                    if (hasNewFeeds) {
                         val syncRequest = androidx.work.OneTimeWorkRequestBuilder<com.boaxente.riffle.worker.FeedSyncWorker>()
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
        // Optimized: Single document for all read states
        val readDocRef = firestore.collection("users").document(userId).collection("sync").document("read")
        
        readStatesListener = readDocRef.addSnapshotListener { snapshot, e ->
            if (e != null) return@addSnapshotListener
            
            if (snapshot != null && snapshot.exists()) {
                scope.launch {
                    // Handle both old format (List<String>) and new format (List<Map>) for transitions
                    val rawItems = snapshot.get("items") as? List<*> ?: emptyList<Any>()
                    
                    val validLinks = mutableSetOf<String>()
                    val itemsToRemove = mutableListOf<Any>()
                    // 2 weeks retention
                    val retentionLimit = System.currentTimeMillis() - (14L * 24 * 60 * 60 * 1000)
                    
                    rawItems.forEach { item ->
                        when (item) {
                            is String -> {
                                // Old format: String URL.
                                validLinks.add(item)
                            }
                            is Map<*, *> -> {
                                val url = item["u"] as? String
                                val timestamp = item["t"] as? Long ?: 0L
                                
                                if (url != null) {
                                    if (timestamp < retentionLimit) {
                                        // Too old, mark for removal
                                        itemsToRemove.add(item)
                                    } else {
                                        validLinks.add(url)
                                    }
                                }
                            }
                        }
                    }
                    
                    // Update local DB
                    validLinks.forEach { link ->
                        if (!remoteReadLinks.contains(link)) {
                            remoteReadLinks.add(link)
                            feedDao.markArticleAsRead(link)
                        }
                    }
                    
                    // Trigger Lazy Cleanup in Cloud
                    if (itemsToRemove.isNotEmpty()) {
                        // Remove the specific object instances we found
                        // Chunking to be safe, though arrayRemove supports varargs
                        itemsToRemove.chunked(500).forEach { chunk ->
                            readDocRef.update("items", FieldValue.arrayRemove(*chunk.toTypedArray()))
                        }
                    }
                }
            }
        }
    }

    private fun startSavedStatesListener(userId: String) {
        // Optimized: Single document for all saved states
        val savedDocRef = firestore.collection("users").document(userId).collection("sync").document("saved")
        
        savedStatesListener = savedDocRef.addSnapshotListener { snapshot, e ->
            if (e != null) return@addSnapshotListener
            
            if (snapshot != null && snapshot.exists()) {
                scope.launch {
                    val items = (snapshot.get("items") as? List<*>)?.filterIsInstance<String>() ?: emptyList()
                    val currentRemoteSet = items.toSet()
                    
                    // Handle additions
                    currentRemoteSet.forEach { link ->
                        if (!remoteSavedLinks.contains(link)) {
                            remoteSavedLinks.add(link)
                            feedDao.updateArticleSavedStatus(link, true)
                        }
                    }
                    
                    val toRemove = remoteSavedLinks.minus(currentRemoteSet)
                    toRemove.forEach { link ->
                        remoteSavedLinks.remove(link)
                        feedDao.updateArticleSavedStatus(link, false)
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
                     val theme = snapshot.getLong("theme")
                     if (theme != null) preferencesManager.setDarkMode(theme.toInt() == 1)

                     val language = snapshot.getString("language")
                     if (language != null) preferencesManager.setLanguage(language)

                     val syncInterval = snapshot.getLong("sync_interval")
                     if (syncInterval != null) preferencesManager.setSyncInterval(syncInterval)

                     val markOnScroll = snapshot.getBoolean("mark_on_scroll")
                     if (markOnScroll != null) preferencesManager.setMarkAsReadOnScroll(markOnScroll)
                }
            }
        }
    }

    fun applyRemoteStates() {
        scope.launch {
            val readCopy = remoteReadLinks.toSet() 
            val savedCopy = remoteSavedLinks.toSet()
            readCopy.forEach { link -> feedDao.markArticleAsRead(link) }
            savedCopy.forEach { link -> feedDao.updateArticleSavedStatus(link, true) }
        }
    }

    // --- Upload Methods with Debouncing ---

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
        scope.launch {
            queueMutex.withLock {
                readQueueAdding.add(articleLink)
            }
            scheduleReadFlush(user.uid)
        }
    }
    
    private fun scheduleReadFlush(userId: String) {
        if (readDebounceJob?.isActive == true) return
        
        readDebounceJob = scope.launch {
            delay(debounceTime)
            flushReadQueue(userId)
        }
    }
    
    private suspend fun flushReadQueue(userId: String) {
        val toAdd = queueMutex.withLock {
            val copy = readQueueAdding.toList()
            readQueueAdding.clear()
            copy
        }
        
        if (toAdd.isNotEmpty()) {
            val validItems = toAdd.filter { it.isNotBlank() }
            if (validItems.isEmpty()) return

            val userRef = firestore.collection("users").document(userId).collection("sync").document("read")
            
            // New format: Map with url (u) and timestamp (t)
            // Using short keys to save space.
            val now = System.currentTimeMillis()
            val objectsToAdd = validItems.map { url ->
                mapOf("u" to url, "t" to now)
            }
            
            objectsToAdd.chunked(500).forEach { chunk ->
                userRef.set(mapOf("items" to FieldValue.arrayUnion(*chunk.toTypedArray())), SetOptions.merge())
                    .addOnFailureListener { e ->
                        e.printStackTrace()
                    }
            }
        }
    }
    
    fun updateSavedStatusInCloud(articleLink: String, isSaved: Boolean) {
        val user = auth.currentUser ?: return
        scope.launch {
            queueMutex.withLock {
                if (isSaved) {
                    savedQueueAdding.add(articleLink)
                    savedQueueRemoving.remove(articleLink)
                } else {
                    savedQueueRemoving.add(articleLink)
                    savedQueueAdding.remove(articleLink)
                }
            }
            scheduleSavedFlush(user.uid)
        }
    }
    
    private fun scheduleSavedFlush(userId: String) {
        if (savedDebounceJob?.isActive == true) return
        
        savedDebounceJob = scope.launch {
            delay(debounceTime)
            flushSavedQueue(userId)
        }
    }
    
    private suspend fun flushSavedQueue(userId: String) {
        val (toAdd, toRemove) = queueMutex.withLock {
            val add = savedQueueAdding.toList()
            val remove = savedQueueRemoving.toList()
            savedQueueAdding.clear()
            savedQueueRemoving.clear()
            Pair(add, remove)
        }
        
        val userRef = firestore.collection("users").document(userId).collection("sync").document("saved")
        
        if (toAdd.isNotEmpty()) {
             toAdd.chunked(500).forEach { chunk ->
                 userRef.set(mapOf("items" to FieldValue.arrayUnion(*chunk.toTypedArray())), SetOptions.merge())
             }
        }
        
        if (toRemove.isNotEmpty()) {
             toRemove.chunked(500).forEach { chunk ->
                 userRef.update("items", FieldValue.arrayRemove(*chunk.toTypedArray()))
             }
        }
    }

    fun updateSourceFolderInCloud(url: String, folderName: String?) {
        val user = auth.currentUser ?: return
        val docId = hashString(url)
        val data = hashMapOf("folderName" to folderName)
        firestore.collection("users").document(user.uid).collection("feeds")
            .document(docId)
            .set(data, SetOptions.merge())
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

    fun updateSourceIconInCloud(url: String, iconUrl: String) {
        val user = auth.currentUser ?: return
        val docId = hashString(url)
        val data = hashMapOf("iconUrl" to iconUrl)
        firestore.collection("users").document(user.uid).collection("feeds")
            .document(docId)
            .set(data, SetOptions.merge())
    }
    
    fun updateSettingInCloud(key: String, value: Any) {
        val user = auth.currentUser ?: return
        val data = hashMapOf(key to value)
        firestore.collection("users").document(user.uid).collection("settings")
            .document("preferences")
            .set(data, SetOptions.merge())
    }

    private fun hashString(input: String): String {
        return MessageDigest.getInstance("MD5")
            .digest(input.toByteArray())
            .joinToString("") { "%02x".format(it) }
    }
}
