package com.example.inoreaderlite.data.repository

import com.example.inoreaderlite.data.local.dao.FeedDao
import com.example.inoreaderlite.data.local.entity.SourceEntity
import com.example.inoreaderlite.data.remote.FeedService
import com.example.inoreaderlite.data.remote.RssParser
import com.example.inoreaderlite.domain.repository.FeedRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class FeedRepositoryImpl(
    private val feedDao: FeedDao,
    private val feedService: FeedService,
    private val rssParser: RssParser
) : FeedRepository {

    override fun getAllArticles() = feedDao.getAllArticles()

    override fun getArticlesBySource(sourceUrl: String) = feedDao.getArticlesBySource(sourceUrl)

    override fun getAllSources() = feedDao.getAllSources()

    override suspend fun addSource(url: String, title: String?) {
        try {
            val response = feedService.fetchFeed(url)
            val articles = rssParser.parse(response.byteStream(), url)
            
            val sourceTitle = if (!title.isNullOrBlank()) title else "Feed from $url" 
            
            val source = SourceEntity(url = url, title = sourceTitle, iconUrl = null)
            feedDao.insertSource(source)
            feedDao.insertArticles(articles)
        } catch (e: Exception) {
            e.printStackTrace()
            throw e
        }
    }

    override suspend fun syncFeeds() = withContext(Dispatchers.IO) {
        val sources = feedDao.getAllSourcesList()
        for (source in sources) {
            syncSource(source)
        }
    }

    override suspend fun markAsRead(link: String) {
        feedDao.markArticleAsRead(link)
    }
    
    suspend fun syncSource(source: SourceEntity) {
        try {
            val response = feedService.fetchFeed(source.url)
            val articles = rssParser.parse(response.byteStream(), source.url)
            feedDao.insertArticles(articles)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
