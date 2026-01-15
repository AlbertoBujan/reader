package com.example.riffle.data.repository

import com.example.riffle.data.local.dao.FeedDao
import com.example.riffle.data.local.entity.SourceEntity
import com.example.riffle.data.remote.FeedService
import com.example.riffle.data.remote.RssParser
import com.example.riffle.domain.repository.FeedRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext

class FeedRepositoryImpl(
    private val feedDao: FeedDao,
    private val feedService: FeedService,
    private val rssParser: RssParser
) : FeedRepository {

    override fun getAllArticles() = feedDao.getAllArticles()

    override fun getArticlesBySource(sourceUrl: String) = feedDao.getArticlesBySource(sourceUrl)

    override fun getAllSources() = feedDao.getAllSources()

    override suspend fun addSource(url: String, title: String?, iconUrl: String?) {
        try {
            val response = withContext(Dispatchers.IO) { feedService.fetchFeed(url) }
            val articles = withContext(Dispatchers.Default) {
                rssParser.parse(response.byteStream(), url)
            }
            
            val sourceTitle = if (!title.isNullOrBlank()) title else "Feed from $url" 
            
            val source = SourceEntity(url = url, title = sourceTitle, iconUrl = iconUrl)
            feedDao.insertSource(source)
            feedDao.insertArticles(articles)
        } catch (e: Exception) {
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
    }
    
    suspend fun syncSource(source: SourceEntity) {
        try {
            val response = feedService.fetchFeed(source.url)
            val articles = withContext(Dispatchers.Default) {
                rssParser.parse(response.byteStream(), source.url)
            }
            feedDao.insertArticles(articles)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
