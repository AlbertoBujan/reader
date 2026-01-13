package com.example.inoreaderlite.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "articles",
    foreignKeys = [
        ForeignKey(
            entity = SourceEntity::class,
            parentColumns = ["url"],
            childColumns = ["sourceUrl"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["sourceUrl"])]
)
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
