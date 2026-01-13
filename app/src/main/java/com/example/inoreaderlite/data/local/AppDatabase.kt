package com.example.inoreaderlite.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.inoreaderlite.data.local.dao.FeedDao
import com.example.inoreaderlite.data.local.entity.ArticleEntity
import com.example.inoreaderlite.data.local.entity.FolderEntity
import com.example.inoreaderlite.data.local.entity.SourceEntity

@Database(
    entities = [ArticleEntity::class, SourceEntity::class, FolderEntity::class],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun feedDao(): FeedDao
}
