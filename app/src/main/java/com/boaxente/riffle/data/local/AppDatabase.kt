package com.boaxente.riffle.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.boaxente.riffle.data.local.dao.FeedDao
import com.boaxente.riffle.data.local.entity.ArticleEntity
import com.boaxente.riffle.data.local.entity.FolderEntity
import com.boaxente.riffle.data.local.entity.SourceEntity

@Database(
    entities = [ArticleEntity::class, SourceEntity::class, FolderEntity::class],
    version = 5,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun feedDao(): FeedDao
}
