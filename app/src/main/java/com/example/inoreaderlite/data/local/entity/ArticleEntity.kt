package com.example.inoreaderlite.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "articles")
data class ArticleEntity(
    @PrimaryKey(autoGenerate = false)
    val link: String,
    val title: String,
    val description: String?,
    val pubDate: Long,
    val sourceUrl: String,
    val imageUrl: String?,
    val isRead: Boolean = false,
    val isSaved: Boolean = false
)
