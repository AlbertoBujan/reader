package com.boaxente.riffle.domain.usecase

import com.boaxente.riffle.data.local.entity.SourceEntity
import com.boaxente.riffle.domain.repository.FeedRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetAllSourcesUseCase @Inject constructor(
    private val repository: FeedRepository
) {
    operator fun invoke(): Flow<List<SourceEntity>> {
        return repository.getAllSources()
    }
}
