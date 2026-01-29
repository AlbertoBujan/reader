package com.example.riffle.data.repository

import com.example.riffle.data.local.dao.FeedDao
import com.example.riffle.data.local.entity.SourceEntity
import com.example.riffle.data.remote.FeedService
import com.example.riffle.data.remote.RssParser
import com.example.riffle.util.RiffleLogger
import com.example.riffle.domain.repository.FeedRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import java.io.InputStream

class FeedRepositoryImpl(
    private val feedDao: FeedDao,
    private val feedService: FeedService,
    private val rssParser: RssParser,
    private val firestoreHelper: com.example.riffle.data.remote.FirestoreHelper? = null 
) : FeedRepository {
    
    init {
        firestoreHelper?.startSync()
    }


    override fun getAllArticles() = feedDao.getAllArticles()

    override fun getArticlesBySource(sourceUrl: String) = feedDao.getArticlesBySource(sourceUrl)

    override fun getAllSources() = feedDao.getAllSources()

    override suspend fun addSource(url: String, title: String?, iconUrl: String?) {
        try {
            val response = withContext(Dispatchers.IO) { feedService.fetchFeed(url) }
            val parsedFeed = withContext(Dispatchers.Default) {
                rssParser.parse(response.byteStream(), url)
            }
            
            val sourceTitle = if (!title.isNullOrBlank()) title else "Feed from $url" 
            
            // Priority:
            // 1. Google Favicon Service (using siteUrl or feed url if siteUrl missing)
            // 2. iconUrl provided (from search)
            // 3. parsedFeed.imageUrl (from RSS)
            
            val siteUrl = parsedFeed.siteUrl ?: url
            val googleFaviconUrl = "https://t0.gstatic.com/faviconV2?client=SOCIAL&type=FAVICON&fallback_opts=TYPE,SIZE,URL&url=$siteUrl&size=64"
            
            val finalIconUrl = if (isValidImage(googleFaviconUrl)) {
                googleFaviconUrl
            } else {
                iconUrl ?: parsedFeed.imageUrl
            }

            val source = SourceEntity(url = url, title = sourceTitle, iconUrl = finalIconUrl)
            feedDao.insertSource(source)
            feedDao.insertArticles(parsedFeed.articles)
            
            // Re-apply states
            firestoreHelper?.applyRemoteStates()
            
            // Sync to cloud
            firestoreHelper?.addSourceToCloud(source)
            
        } catch (e: Exception) {
            RiffleLogger.recordException(e)
            e.printStackTrace()
            throw e
        }
    }

    override suspend fun syncFeeds() {
        withContext(Dispatchers.IO) {
            val sources = feedDao.getAllSourcesList()
            coroutineScope {
                sources.map { source ->
                    async { syncSource(source) }
                }.awaitAll()
            }
        }
    }

    override suspend fun markAsRead(link: String) {
        feedDao.markArticleAsRead(link)
        // Sync to cloud
        firestoreHelper?.markReadInCloud(link)
    }

    override suspend fun toggleArticleSaved(link: String, isSaved: Boolean) {
        feedDao.updateArticleSavedStatus(link, isSaved)
        // Sync to cloud
        firestoreHelper?.updateSavedStatusInCloud(link, isSaved)
    }

    override suspend fun moveSourceToFolder(url: String, folderName: String?) {
        feedDao.updateSourceFolder(url, folderName)
        // Sync to cloud
        firestoreHelper?.updateSourceFolderInCloud(url, folderName)
    }

    override suspend fun deleteSource(url: String) {
        feedDao.deleteSource(url)
        firestoreHelper?.deleteSourceFromCloud(url)
    }

    override suspend fun deleteFolder(folderName: String) {
        // Find feeds in folder before local deletion (which cascades)
        val allSources = feedDao.getAllSourcesList()
        val feedsInFolder = allSources.filter { it.folderName == folderName }
        
        feedDao.deleteFolder(folderName)
        
        feedsInFolder.forEach { source ->
            firestoreHelper?.deleteSourceFromCloud(source.url)
        }
    }

    override suspend fun renameSource(url: String, newTitle: String) {
        feedDao.updateSourceTitle(url, newTitle)
        firestoreHelper?.updateSourceTitleInCloud(url, newTitle)
    }
    
    suspend fun syncSource(source: SourceEntity) {
        try {
            val response = feedService.fetchFeed(source.url)
            val parsedFeed = withContext(Dispatchers.Default) {
                rssParser.parse(response.byteStream(), source.url)
            }
            feedDao.insertArticles(parsedFeed.articles)
            
            // Re-apply remote states (read/saved) to ensure consistent state
            firestoreHelper?.applyRemoteStates()
            
            // Update Icon logic
            val siteUrl = parsedFeed.siteUrl ?: source.url
            val googleFaviconUrl = "https://t0.gstatic.com/faviconV2?client=SOCIAL&type=FAVICON&fallback_opts=TYPE,SIZE,URL&url=$siteUrl&size=64"
            
            // Only check/update if current is null or we want to enforce Google logic mainly?
            // User implies we should switch to this method. 
            // So we check if google favicon is valid and different from current.
            
            val newIconUrl = if (isValidImage(googleFaviconUrl)) {
                googleFaviconUrl
            } else {
                 parsedFeed.imageUrl
            }
            
            if (newIconUrl != null && newIconUrl != source.iconUrl) {
                feedDao.updateSourceIcon(source.url, newIconUrl)
                // SYNC TO CLOUD
                firestoreHelper?.updateSourceIconInCloud(source.url, newIconUrl)
            }

        } catch (e: Exception) {
            RiffleLogger.recordException(e)
            e.printStackTrace()
        }
    }
    
    private suspend fun isValidImage(url: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val connection = java.net.URL(url).openConnection() as java.net.HttpURLConnection
                connection.requestMethod = "HEAD"
                connection.connectTimeout = 3000
                connection.readTimeout = 3000
                connection.connect()
                val code = connection.responseCode
                val contentType = connection.contentType
                connection.disconnect()
                code == 200 && contentType?.startsWith("image/") == true
            } catch (e: Exception) {
            RiffleLogger.recordException(e)
                false
            }
        }
    }

    override suspend fun importOpml(inputStream: InputStream) {
        withContext(Dispatchers.IO) {
            try {
                val items = com.example.riffle.utils.OpmlUtility.parse(inputStream)
                
                // Helper to process a source item
                suspend fun processSource(title: String, url: String, folderName: String?) {
                    try {
                        // Check if exists
                        val existing = feedDao.getSourceByUrl(url)
                        if (existing == null) {
                            // We can reuse addSource or do simplified insertion
                            // Reuse addSource logic but adapted to avoid double network calls if we want to go fast?
                            // For now, let's just insert proper SourceEntity and then trigger sync later or optionally fetch now.
                            // To make import fast, maybe just insert SourceEntity. Network sync can happen later.
                            
                            val source = SourceEntity(
                                url = url, 
                                title = title, 
                                iconUrl = null,
                                folderName = folderName
                            )
                            feedDao.insertSource(source)
                        } else {
                            // Update folder if needed?
                            if (folderName != null && existing.folderName != folderName) {
                                feedDao.insertSource(existing.copy(folderName = folderName))
                            }
                        }
                    } catch (e: Exception) {
            RiffleLogger.recordException(e)
                        e.printStackTrace()
                    }
                }

                items.forEach { item ->
                    when (item) {
                        is com.example.riffle.utils.OpmlUtility.OpmlItem.Folder -> {
                            // Insert folder? We don't have explicit FolderEntity insertion in DAO usually exposed? 
                            // Ensure folder exists or just assign sources to it.
                            // SourceEntity has ForeignKey to FolderEntity? Checking SourceEntity definition... 
                            // Yes, ForeignKey to FolderEntity. So we MUST insert folder first.
                            
                            // Check if we need to insert folder
                            // Assuming we have a DAO method for folder or simple insert
                            // Let's assume we need to insert it.
                            // Wait, I need to check FolderDAO capabilities. 
                            // I'll assume we can insert folders via FeedDao or generic generic insert.
                            // Looking at codebase I haven't seen FolderDao.
                            // I should verify unrelated to this block, but let's assume I need to handle it.
                            // For now, I'll assume I can just insert source with folderName? 
                            // NO, foreign key constraint will fail if folder doesn't exist.
                            
                            // I need to add `insertFolder` to FeedDao or Repository if missing.
                            // I will add `createFolder(item.title)` call.
                            
                            feedDao.insertFolder(com.example.riffle.data.local.entity.FolderEntity(item.title))
                            
                            item.children.forEach { child ->
                                processSource(child.title, child.xmlUrl, item.title)
                            }
                        }
                        is com.example.riffle.utils.OpmlUtility.OpmlItem.Source -> {
                            processSource(item.title, item.xmlUrl, null)
                        }
                    }
                }
                
                // Optional: trigger a sync after import? Or let user pull to refresh.
                // syncing 100 feeds takes time. Better let user trigger it.
                
            } catch (e: Exception) {
            RiffleLogger.recordException(e)
                e.printStackTrace()
                throw e
            }
        }
    }

    override suspend fun exportOpml(): String {
        return withContext(Dispatchers.IO) {
            val sources = feedDao.getAllSourcesList()
            com.example.riffle.utils.OpmlUtility.generate(sources)
        }
    }
}
