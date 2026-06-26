package com.maksimowiczm.foodyou.healthconnect

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class HealthConnectSyncWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params), KoinComponent {

    private val syncService: HealthConnectSyncService by inject()

    override suspend fun doWork(): Result {
        syncService.syncNow()
        return Result.success()
    }

    companion object {
        const val WORK_NAME = "health_connect_daily_sync"
    }
}
