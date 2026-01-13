package com.example.inoreaderlite.domain.repository

import com.example.inoreaderlite.data.local.entity.ArticleEntity
import com.example.inoreaderlite.data.local.entity.SourceEntity
import kotlinx.coroutines.flow.Flow

interface FeedRepository {
    fun getAllArticles(): Flow<List<ArticleEntity>>
    fun getArticlesBySource(sourceUrl: String): Flow<List<ArticleEntity>>
    fun getAllSources(): Flow<List<SourceEntity>>
    suspend fun addSource(url: String)
    suspend fun syncFeeds()
    suspend fun markAsRead(link: String)
}
