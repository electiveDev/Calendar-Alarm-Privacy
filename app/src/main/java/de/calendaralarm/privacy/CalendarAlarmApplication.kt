package de.calendaralarm.privacy

import android.app.Application
import de.calendaralarm.privacy.alarm.AlarmScheduler
import de.calendaralarm.privacy.data.calendar.CalendarReader
import de.calendaralarm.privacy.data.db.AppDatabase
import de.calendaralarm.privacy.data.prefs.AppPreferences
import de.calendaralarm.privacy.domain.AlarmSyncManager
import de.calendaralarm.privacy.worker.CalendarSyncWorker

class CalendarAlarmApplication : Application() {
    val preferences: AppPreferences by lazy { AppPreferences(this) }
    val database: AppDatabase by lazy { AppDatabase.create(this) }
    val calendarReader: CalendarReader by lazy { CalendarReader(this) }
    val alarmScheduler: AlarmScheduler by lazy { AlarmScheduler(this) }
    val syncManager: AlarmSyncManager by lazy {
        AlarmSyncManager(this, preferences, database, calendarReader, alarmScheduler)
    }

    override fun onCreate() {
        super.onCreate()
        NotificationHelper.createAlarmChannel(this)
        CalendarSyncWorker.enqueuePeriodic(this)
    }
}
