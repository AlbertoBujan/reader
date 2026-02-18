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
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
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
    private val debounceTime = 2000L // 2 seconds for faster sync
    private var readDebounceJob: Job? = null
    // private var savedDebounceJob: Job? = null // Eliminado
    
    private val readQueueAdding = mutableSetOf<String>()
    
    // Batches de guardado eliminados en favor de actualizaciones directas de documentos
    
    private val queueMutex = Mutex()

    // --- State Caching for Race Conditions ---
    private val remoteReadLinks = HashSet<String>()
    private val remoteSavedLinks = HashSet<String>()
    private val stateMutex = Mutex()
    
    // Stats for User Profile
    private val _readCount = kotlinx.coroutines.flow.MutableStateFlow(0)
    val readCount = _readCount.asStateFlow()
    
    private val _savedCount = kotlinx.coroutines.flow.MutableStateFlow(0)
    val savedCount = _savedCount.asStateFlow()

    fun startSync() {
        auth.addAuthStateListener { firebaseAuth ->
            val user = firebaseAuth.currentUser
            if (user != null) {
                // User logged in, start listeners
                startFeedsListener(user.uid)
                startReadStatesListener(user.uid)
                startSavedArticlesListener(user.uid)
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
                    val rawItems = snapshot.get("items") as? List<Any?> ?: emptyList()
                    
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
                    stateMutex.withLock {
                        val newLinks = mutableListOf<String>()
                        validLinks.forEach { link ->
                            if (!remoteReadLinks.contains(link)) {
                                newLinks.add(link)
                            }
                        }
                        
                        // Synchronize cache with truth from cloud
                        remoteReadLinks.clear()
                        remoteReadLinks.addAll(validLinks)
                        
                        if (newLinks.isNotEmpty()) {
                             newLinks.chunked(900).forEach { chunk ->
                                 feedDao.markArticlesAsRead(chunk)
                             }
                        }
                        
                        _readCount.value = remoteReadLinks.size
                    }
                    
                    // --- CLEANUP Strategy: Remove read items from Cloud if they are not in Local DB ---
                    // Caution: This assumes Local DB is up to date with Feeds.
                    // To be safe, we only remove items that are OLDER than retention limit (already handled)
                    // OR if users specifically requested "if not in feed".
                    
                    // Get all local article links efficiently
                    // We can't query all links for performance if DB is huge, but usually < 10k.
                    // Doing this on every sync change might be heavy. 
                    // Let's do it only if new items came in, or Periodically.
                    // For now, let's implement effective cleanup based on "validLinks" (cloud items).
                    
                    val allLocalLinks = feedDao.getAllArticles().firstOrNull()?.map { it.link }?.toSet() ?: emptySet()
                    
                    if (allLocalLinks.isNotEmpty()) {
                        val itemsToPrune = mutableListOf<Any>()
                        
                        rawItems.forEach { item ->
                            val url = when (item) {
                                is String -> item
                                is Map<*, *> -> item["u"] as? String
                                else -> null
                            }
                            
                            if (url != null) {
                                // If the read article is NOT in our local database (meaning not in feed), remove it.
                                // EXCEPTION: Saved articles? No, saved logic is separate.
                                // BUT verify we don't delete just because we haven't synced recent feed yet.
                                // This is risky if user device is stale.
                                // User explicitly asked: "if read and no longer in feed, delete from firestore".
                                // We trust local state.
                                
                                if (!allLocalLinks.contains(url)) {
                                    if (item != null) {
                                        itemsToPrune.add(item)
                                    }
                                }
                            }
                        }
                        
                        if (itemsToPrune.isNotEmpty()) {
                            itemsToPrune.chunked(500).forEach { chunk ->
                                readDocRef.update("items", FieldValue.arrayRemove(*chunk.toTypedArray()))
                            }
                        }
                    }

                    // Trigger Lazy Cleanup in Cloud (Retention Limit)
                    if (itemsToRemove.isNotEmpty()) {
                        itemsToRemove.chunked(500).forEach { chunk ->
                            readDocRef.update("items", FieldValue.arrayRemove(*chunk.toTypedArray()))
                        }
                    }
                }
            }
        }
    }

    // Modificado: Escuchar colección de artículos guardados completos
    private fun startSavedArticlesListener(userId: String) {
        val savedCollectionRef = firestore.collection("users").document(userId).collection("saved_articles")
        
        savedStatesListener = savedCollectionRef.addSnapshotListener { snapshot, e ->
            if (e != null) return@addSnapshotListener
            
            if (snapshot != null) {
                scope.launch {
                    val currentSavedLinks = mutableSetOf<String>()
                    
                    snapshot.documents.forEach { doc ->
                        val link = doc.getString("link")
                        if (link != null) {
                            currentSavedLinks.add(link)
                            
                            val timestamp = doc.getLong("pubDate") ?: System.currentTimeMillis()
                            val srcUrl = doc.getString("sourceUrl") ?: ""
                            
                            // Mapear documento a ArticleEntity
                            val article = com.boaxente.riffle.data.local.entity.ArticleEntity(
                                link = link,
                                title = doc.getString("title") ?: "",
                                description = doc.getString("description"),
                                pubDate = timestamp,
                                sourceUrl = srcUrl, 
                                imageUrl = doc.getString("imageUrl"),
                                isRead = doc.getBoolean("isRead") ?: false, 
                                isSaved = true,
                                hasVideo = doc.getBoolean("hasVideo") ?: false
                            )
                            
                            // Insertar/Actualizar en local
                            // Usamos firstOrNull() suspendiendo para evitar bloqueos
                            val existing = feedDao.getArticleByLink(link).firstOrNull()
                            
                            if (existing == null) {
                                try {
                                    // Verificar source para evitar error FK
                                    val sourceExists = feedDao.getSourceByUrl(srcUrl)
                                    if (sourceExists == null && srcUrl.isNotEmpty()) {
                                         // Crear source placeholder
                                         val placeholderSource = com.boaxente.riffle.data.local.entity.SourceEntity(
                                             url = srcUrl,
                                             title = doc.getString("sourceTitle") ?: "Unknown Source",
                                             iconUrl = null
                                         )
                                         feedDao.insertSource(placeholderSource)
                                    }
                                    
                                    // Insertar artículo completo
                                    if (srcUrl.isNotEmpty()) {
                                        feedDao.insertArticles(listOf(article))
                                    }
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                            } else {
                                // Ya existe, asegurar isSaved=true
                                if (!existing.isSaved) {
                                    feedDao.updateArticleSavedStatus(link, true)
                                }
                            }
                        }
                    }
                    
                    // Sincronizar borrados: Desmarcar los que ya no están en la colección remota
                    val localSaved = feedDao.getSavedArticlesList()
                    localSaved.forEach { localArticle ->
                        if (!currentSavedLinks.contains(localArticle.link)) {
                            feedDao.updateArticleSavedStatus(localArticle.link, false)
                        }
                    }
                    
                    stateMutex.withLock {
                        this@FirestoreHelper.remoteSavedLinks.clear()
                        this@FirestoreHelper.remoteSavedLinks.addAll(currentSavedLinks)
                        _savedCount.value = this@FirestoreHelper.remoteSavedLinks.size
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
            val (readCopy, savedCopy) = stateMutex.withLock {
                Pair(remoteReadLinks.toList(), remoteSavedLinks.toList())
            }
            
            if (readCopy.isNotEmpty()) {
                readCopy.chunked(900).forEach { chunk ->
                     feedDao.markArticlesAsRead(chunk)
                }
            }

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
    
    fun saveArticleToCloud(article: com.boaxente.riffle.data.local.entity.ArticleEntity, sourceTitle: String?) {
        val user = auth.currentUser ?: return
        val docId = hashString(article.link)
        
        val data = hashMapOf(
            "link" to article.link,
            "title" to article.title,
            "description" to article.description,
            "pubDate" to article.pubDate,
            "sourceUrl" to article.sourceUrl,
            "sourceTitle" to sourceTitle, // Guardamos titulo del source para reconstruirlo si falta
            "imageUrl" to article.imageUrl,
            "isRead" to article.isRead,
            "hasVideo" to article.hasVideo,
            "savedAt" to System.currentTimeMillis()
        )
        
        firestore.collection("users").document(user.uid).collection("saved_articles")
            .document(docId)
            .set(data, SetOptions.merge())
    }
    
    fun removeSavedArticleFromCloud(link: String) {
        val user = auth.currentUser ?: return
        val docId = hashString(link)
        
        firestore.collection("users").document(user.uid).collection("saved_articles")
            .document(docId)
            .delete()
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
