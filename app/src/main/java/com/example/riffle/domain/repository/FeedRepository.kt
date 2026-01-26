package com.example.riffle.domain.repository

import com.example.riffle.data.local.entity.ArticleEntity
import com.example.riffle.data.local.entity.SourceEntity
import kotlinx.coroutines.flow.Flow

interface FeedRepository {
    fun getAllArticles(): Flow<List<ArticleEntity>>
    fun getArticlesBySource(sourceUrl: String): Flow<List<ArticleEntity>>
    fun getAllSources(): Flow<List<SourceEntity>>
    suspend fun addSource(url: String, title: String?, iconUrl: String? = null)
    suspend fun syncFeeds()
    suspend fun markAsRead(link: String)
    suspend fun toggleArticleSaved(link: String, isSaved: Boolean)
    suspend fun moveSourceToFolder(url: String, folderName: String?)
    suspend fun deleteSource(url: String)
    suspend fun deleteFolder(folderName: String)
    suspend fun renameSource(url: String, newTitle: String)
    
    suspend fun importOpml(inputStream: java.io.InputStream)
    suspend fun exportOpml(): String
}
