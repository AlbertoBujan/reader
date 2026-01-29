package com.example.riffle.domain.usecase

import com.example.riffle.data.local.entity.SourceEntity
import com.example.riffle.domain.repository.FeedRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetAllSourcesUseCase @Inject constructor(
    private val repository: FeedRepository
) {
    operator fun invoke(): Flow<List<SourceEntity>> {
        return repository.getAllSources()
    }
}
