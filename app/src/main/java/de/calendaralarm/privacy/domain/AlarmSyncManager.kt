package de.calendaralarm.privacy.domain

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import de.calendaralarm.privacy.alarm.AlarmScheduler
import de.calendaralarm.privacy.alarm.AlarmSpec
import de.calendaralarm.privacy.data.calendar.CalendarReader
import de.calendaralarm.privacy.data.db.AlarmState
import de.calendaralarm.privacy.data.db.AppDatabase
import de.calendaralarm.privacy.data.db.ScheduledAlarm
import de.calendaralarm.privacy.data.prefs.AppPreferences
import kotlinx.coroutines.flow.first

class AlarmSyncManager(
    private val context: Context,
    private val preferences: AppPreferences,
    private val database: AppDatabase,
    private val calendarReader: CalendarReader,
    private val scheduler: AlarmScheduler,
) {
    suspend fun synchronize() {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALENDAR) != PackageManager.PERMISSION_GRANTED) return
        if (!scheduler.canScheduleExactAlarms()) return

        val settings = preferences.settings.first()
        if (settings.selectedCalendarIds.isEmpty()) {
            database.scheduledAlarmDao().getActive().forEach { scheduler.cancel(it.requestCode) }
            database.scheduledAlarmDao().purgeCancelled()
            return
        }

        val now = System.currentTimeMillis()
        val events = calendarReader.readInstances(now, now + SEVEN_DAYS_MS)
        val desired = events.filter {
            EventFilter.isEligible(
                event = it,
                selectedCalendarIds = settings.selectedCalendarIds,
                skipAllDay = settings.skipAllDay,
                skipDeclined = settings.skipDeclined,
                nowUtc = now,
                leadTimeMinutes = settings.leadTimeMinutes,
            )
        }.map {
            DesiredAlarm(
                eventInstanceId = it.instanceId,
                eventId = it.eventId,
                calendarId = it.calendarId,
                meetingStartUtc = it.beginUtc,
                alarmFireUtc = AlarmTimeCalculator.alarmFireUtc(it.beginUtc, settings.leadTimeMinutes),
                eventTitle = it.title,
            )
        }

        val dao = database.scheduledAlarmDao()
        val existing = dao.getActive()
        val desiredById = desired.associateBy { it.eventInstanceId }
        existing.filter { it.eventInstanceId !in desiredById }.forEach {
            scheduler.cancel(it.requestCode)
            dao.updateState(it.eventInstanceId, AlarmState.CANCELLED, muted = false)
        }

        val usedCodes = existing.map { it.requestCode }.toMutableSet()
        desired.forEach { item ->
            val old = existing.firstOrNull { it.eventInstanceId == item.eventInstanceId }
            if (old?.muted == true) return@forEach
            if (old != null && old.meetingStartUtc == item.meetingStartUtc && old.alarmFireUtc == item.alarmFireUtc) return@forEach
            val requestCode = old?.requestCode ?: RequestCodeAllocator.allocate(item.eventInstanceId, usedCodes)
            if (old != null) scheduler.cancel(old.requestCode)
            if (scheduler.schedule(item.toSpec(requestCode))) {
                usedCodes += requestCode
                dao.upsert(
                    ScheduledAlarm(
                        id = old?.id ?: 0,
                        eventInstanceId = item.eventInstanceId,
                        eventId = item.eventId,
                        calendarId = item.calendarId,
                        meetingStartUtc = item.meetingStartUtc,
                        alarmFireUtc = item.alarmFireUtc,
                        requestCode = requestCode,
                        state = AlarmState.SCHEDULED,
                        muted = false,
                        eventTitle = item.eventTitle,
                    ),
                )
            }
        }
        dao.purgeCancelled()
    }

    private fun DesiredAlarm.toSpec(requestCode: Int) = AlarmSpec(
        eventInstanceId = eventInstanceId,
        eventId = eventId,
        calendarId = calendarId,
        meetingStartUtc = meetingStartUtc,
        alarmFireUtc = alarmFireUtc,
        requestCode = requestCode,
        eventTitle = eventTitle,
    )

    companion object {
        private const val SEVEN_DAYS_MS = 7L * 24L * 60L * 60L * 1000L
    }
}
