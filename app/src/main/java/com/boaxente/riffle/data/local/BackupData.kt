package com.boaxente.riffle.data.local

import com.boaxente.riffle.data.local.entity.ArticleEntity
import com.boaxente.riffle.data.local.entity.FolderEntity
import com.boaxente.riffle.data.local.entity.SourceEntity

data class BackupData(
    val version: Int = 1,
    val timestamp: Long,
    val preferences: Map<String, Any?>,
    val folders: List<FolderEntity>,
    val sources: List<SourceEntity>,
    val savedArticles: List<ArticleEntity>
)
