package com.example.riffle.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.riffle.data.local.dao.FeedDao
import com.example.riffle.data.local.entity.ArticleEntity
import com.example.riffle.data.local.entity.FolderEntity
import com.example.riffle.data.local.entity.SourceEntity

@Database(
    entities = [ArticleEntity::class, SourceEntity::class, FolderEntity::class],
    version = 5,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun feedDao(): FeedDao
}
