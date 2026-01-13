package com.example.inoreaderlite.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sources")
data class SourceEntity(
    @PrimaryKey
    val url: String,
    val title: String,
    val iconUrl: String?
)
