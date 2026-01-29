package com.example.riffle.data.local

import com.example.riffle.data.local.entity.ArticleEntity
import com.example.riffle.data.local.entity.FolderEntity
import com.example.riffle.data.local.entity.SourceEntity

data class BackupData(
    val version: Int = 1,
    val timestamp: Long,
    val preferences: Map<String, Any?>,
    val folders: List<FolderEntity>,
    val sources: List<SourceEntity>,
    val savedArticles: List<ArticleEntity>
)
