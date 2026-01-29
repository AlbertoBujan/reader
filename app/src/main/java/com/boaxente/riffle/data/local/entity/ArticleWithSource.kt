package com.boaxente.riffle.data.local.entity

import androidx.room.Embedded

data class ArticleWithSource(
    @Embedded val article: ArticleEntity,
    val sourceTitle: String
)
