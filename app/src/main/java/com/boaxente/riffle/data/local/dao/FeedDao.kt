package com.boaxente.riffle.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.boaxente.riffle.data.local.entity.ArticleEntity
import com.boaxente.riffle.data.local.entity.ArticleWithSource
import com.boaxente.riffle.data.local.entity.FolderEntity
import com.boaxente.riffle.data.local.entity.SourceEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FeedDao {

    @Query("SELECT * FROM articles ORDER BY pubDate DESC")
    fun getAllArticles(): Flow<List<ArticleEntity>>

    @Query("SELECT * FROM articles WHERE sourceUrl = :sourceUrl ORDER BY pubDate DESC")
    fun getArticlesBySource(sourceUrl: String): Flow<List<ArticleEntity>>

    @Query("""
        SELECT articles.* FROM articles 
        INNER JOIN sources ON articles.sourceUrl = sources.url 
        WHERE sources.folderName = :folderName 
        ORDER BY pubDate DESC
    """)
    fun getArticlesByFolder(folderName: String): Flow<List<ArticleEntity>>

    @Query("SELECT * FROM articles WHERE isSaved = 1 ORDER BY pubDate DESC")
    fun getSavedArticles(): Flow<List<ArticleEntity>>

    @Query("""
        SELECT articles.*, sources.title as sourceTitle 
        FROM articles 
        LEFT JOIN sources ON articles.sourceUrl = sources.url 
        WHERE articles.link = :link LIMIT 1
    """)
    fun getArticleWithSource(link: String): Flow<ArticleWithSource?>

    @Query("SELECT * FROM articles WHERE link = :link LIMIT 1")
    fun getArticleByLink(link: String): Flow<ArticleEntity?>

    @Query("SELECT link FROM articles WHERE isRead = 1")
    suspend fun getReadArticleLinks(): List<String>

    @Query("SELECT sourceUrl, COUNT(*) as count FROM articles WHERE isRead = 0 GROUP BY sourceUrl")
    fun getUnreadCountsBySource(): Flow<List<SourceUnreadCount>>

    @Query("SELECT COUNT(*) FROM articles WHERE isSaved = 1")
    fun getSavedCount(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertArticles(articles: List<ArticleEntity>)

    @Query("DELETE FROM articles")
    suspend fun clearAllArticles()

    @Query("UPDATE articles SET isRead = 1 WHERE link = :link")
    suspend fun markArticleAsRead(link: String)

    @Query("UPDATE articles SET isRead = 1 WHERE sourceUrl = :sourceUrl AND isRead = 0")
    suspend fun markArticlesAsReadBySource(sourceUrl: String)

    @Query("""
        UPDATE articles SET isRead = 1 
        WHERE sourceUrl IN (SELECT url FROM sources WHERE folderName = :folderName)
        AND isRead = 0
    """)
    suspend fun markArticlesAsReadByFolder(folderName: String)

    @Query("UPDATE articles SET isRead = 1 WHERE isRead = 0")
    suspend fun markAllArticlesAsRead()

    @Query("UPDATE articles SET isSaved = :isSaved WHERE link = :link")
    suspend fun updateArticleSavedStatus(link: String, isSaved: Boolean)

    // Source Management
    @Query("SELECT * FROM sources")
    fun getAllSources(): Flow<List<SourceEntity>>
    
    @Query("SELECT * FROM sources")
    suspend fun getAllSourcesList(): List<SourceEntity>

    @Query("SELECT * FROM folders")
    suspend fun getAllFoldersList(): List<FolderEntity>

    @Query("SELECT * FROM articles WHERE isSaved = 1")
    suspend fun getSavedArticlesList(): List<ArticleEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSource(source: SourceEntity)

    @Query("SELECT * FROM sources WHERE url = :url LIMIT 1")
    suspend fun getSourceByUrl(url: String): SourceEntity?

    @Query("DELETE FROM sources WHERE url = :url")
    suspend fun deleteSource(url: String)

    @Query("UPDATE sources SET folderName = :folderName WHERE url = :url")
    suspend fun updateSourceFolder(url: String, folderName: String?)

    @Query("UPDATE sources SET title = :newTitle WHERE url = :url")
    suspend fun updateSourceTitle(url: String, newTitle: String)

    @Query("UPDATE sources SET iconUrl = :iconUrl WHERE url = :url")
    suspend fun updateSourceIcon(url: String, iconUrl: String?)

    // Folder Management
    @Query("SELECT * FROM folders")
    fun getAllFolders(): Flow<List<FolderEntity>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertFolder(folder: FolderEntity)

    @Query("DELETE FROM folders WHERE name = :name")
    suspend fun deleteFolder(name: String)

    @Query("UPDATE folders SET name = :newName WHERE name = :oldName")
    suspend fun renameFolder(oldName: String, newName: String)

    @Query("UPDATE articles SET description = SUBSTR(description, 1, 100000) WHERE LENGTH(description) > 100000")
    suspend fun cleanupHugeArticles()
}

data class SourceUnreadCount(
    val sourceUrl: String,
    val count: Int
)
