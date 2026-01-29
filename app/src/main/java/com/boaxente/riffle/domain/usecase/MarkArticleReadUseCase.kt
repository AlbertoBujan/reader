package com.example.riffle.domain.usecase

import com.example.riffle.domain.repository.FeedRepository
import javax.inject.Inject

class MarkArticleReadUseCase @Inject constructor(
    private val repository: FeedRepository
) {
    suspend operator fun invoke(link: String) {
        repository.markAsRead(link)
    }
}
