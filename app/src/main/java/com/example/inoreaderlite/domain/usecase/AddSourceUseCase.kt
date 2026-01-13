package com.example.inoreaderlite.domain.usecase

import com.example.inoreaderlite.domain.repository.FeedRepository
import javax.inject.Inject

class AddSourceUseCase @Inject constructor(
    private val repository: FeedRepository
) {
    suspend operator fun invoke(url: String, title: String?) {
        repository.addSource(url, title)
    }
}
