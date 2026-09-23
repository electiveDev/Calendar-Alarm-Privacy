package de.calendaralarm.privacy.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import java.util.concurrent.TimeUnit
import de.calendaralarm.privacy.CalendarAlarmApplication

class CalendarSyncWorker(
    appContext: Context,
    workerParams: WorkerParameters,
) : CoroutineWorker(appContext, workerParams) {
    override suspend fun doWork(): Result {
        return runCatching {
            (applicationContext as CalendarAlarmApplication).syncManager.synchronize()
            Result.success()
        }.getOrElse { Result.retry() }
    }

    companion object {
        private const val PERIODIC_NAME = "calendar_alarm_periodic_sync"
        private const val ONE_TIME_NAME = "calendar_alarm_one_time_sync"

        fun enqueuePeriodic(context: Context) {
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                PERIODIC_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                PeriodicWorkRequestBuilder<CalendarSyncWorker>(15, TimeUnit.MINUTES).build(),
            )
        }

        fun enqueueOneTime(context: Context) {
            WorkManager.getInstance(context).enqueueUniqueWork(
                ONE_TIME_NAME,
                ExistingWorkPolicy.REPLACE,
                OneTimeWorkRequestBuilder<CalendarSyncWorker>().build(),
            )
        }
    }
}
