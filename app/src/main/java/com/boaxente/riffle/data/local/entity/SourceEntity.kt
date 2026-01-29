package com.boaxente.riffle.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "sources",
    foreignKeys = [
        ForeignKey(
            entity = FolderEntity::class,
            parentColumns = ["name"],
            childColumns = ["folderName"],
            onDelete = ForeignKey.CASCADE,
            onUpdate = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["folderName"])]
)
data class SourceEntity(
    @PrimaryKey
    val url: String,
    val title: String,
    val iconUrl: String?,
    val folderName: String? = null
)
