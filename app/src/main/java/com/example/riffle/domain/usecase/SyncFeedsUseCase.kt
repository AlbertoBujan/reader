package com.example.riffle.domain.usecase

import com.example.riffle.domain.repository.FeedRepository
import javax.inject.Inject

class SyncFeedsUseCase @Inject constructor(
    private val repository: FeedRepository
) {
    suspend operator fun invoke() {
        repository.syncFeeds()
    }
}
