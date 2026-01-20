package com.example.riffle.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.riffle.domain.repository.FeedRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class FeedSyncWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val feedRepository: FeedRepository
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            feedRepository.syncFeeds()
            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            // If it fails, we can either retry or return failure.
            // For now, let's return retry if it's a network issue, or failure otherwise.
            // But simple impl: retry
            Result.retry()
        }
    }
}
