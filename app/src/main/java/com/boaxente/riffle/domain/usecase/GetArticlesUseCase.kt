package com.boaxente.riffle.domain.usecase

import com.boaxente.riffle.data.local.entity.ArticleEntity
import com.boaxente.riffle.domain.repository.FeedRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetArticlesUseCase @Inject constructor(
    private val repository: FeedRepository
) {
    operator fun invoke(sourceUrl: String? = null): Flow<List<ArticleEntity>> {
        return if (sourceUrl == null) {
            repository.getAllArticles()
        } else {
            repository.getArticlesBySource(sourceUrl)
        }
    }
}
