package com.example.inoreaderlite.domain.usecase

import com.example.inoreaderlite.domain.repository.FeedRepository
import javax.inject.Inject

class SyncFeedsUseCase @Inject constructor(
    private val repository: FeedRepository
) {
    suspend operator fun invoke() {
        repository.syncFeeds()
    }
}
