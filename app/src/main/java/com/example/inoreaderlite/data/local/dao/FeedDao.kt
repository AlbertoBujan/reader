package com.example.inoreaderlite.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.example.inoreaderlite.data.local.entity.ArticleEntity
import com.example.inoreaderlite.data.local.entity.FolderEntity
import com.example.inoreaderlite.data.local.entity.SourceEntity
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

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertArticles(articles: List<ArticleEntity>)

    @Query("DELETE FROM articles")
    suspend fun clearAllArticles()

    @Query("UPDATE articles SET isRead = 1 WHERE link = :link")
    suspend fun markArticleAsRead(link: String)

    // Source Management
    @Query("SELECT * FROM sources")
    fun getAllSources(): Flow<List<SourceEntity>>
    
    @Query("SELECT * FROM sources")
    suspend fun getAllSourcesList(): List<SourceEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSource(source: SourceEntity)

    @Query("DELETE FROM sources WHERE url = :url")
    suspend fun deleteSource(url: String)

    @Query("UPDATE sources SET folderName = :folderName WHERE url = :url")
    suspend fun updateSourceFolder(url: String, folderName: String?)

    // Folder Management
    @Query("SELECT * FROM folders")
    fun getAllFolders(): Flow<List<FolderEntity>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertFolder(folder: FolderEntity)

    @Query("DELETE FROM folders WHERE name = :name")
    suspend fun deleteFolder(name: String)
}
