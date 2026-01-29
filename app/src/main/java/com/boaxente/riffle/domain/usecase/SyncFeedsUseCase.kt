package com.boaxente.riffle.domain.usecase

import com.boaxente.riffle.domain.repository.FeedRepository
import javax.inject.Inject

class SyncFeedsUseCase @Inject constructor(
    private val repository: FeedRepository
) {
    suspend operator fun invoke() {
        repository.syncFeeds()
    }
}
