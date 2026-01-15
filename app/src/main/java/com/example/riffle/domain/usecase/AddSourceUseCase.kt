package com.example.riffle.domain.usecase

import com.example.riffle.domain.repository.FeedRepository
import javax.inject.Inject

class AddSourceUseCase @Inject constructor(
    private val repository: FeedRepository
) {
    suspend operator fun invoke(url: String, title: String?, iconUrl: String? = null) {
        repository.addSource(url, title, iconUrl)
    }
}
