package com.moises.vitalodyssey.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.moises.vitalodyssey.domain.repository.UserRepository
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class SyncWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params), KoinComponent {

    private val userRepository: UserRepository by inject()

    override suspend fun doWork(): Result {
        return try {
            userRepository.performFullCloudSync()
            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            // If there's an error (like no internet despite constraints, or Firestore issue), we can retry later.
            Result.retry()
        }
    }
}
