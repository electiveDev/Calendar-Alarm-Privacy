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

enum class SyncBlocker {
    CALENDAR_PERMISSION,
    EXACT_ALARM_PERMISSION,
    NO_SELECTED_CALENDARS,
}

data class CalendarSyncResult(
    val blocker: SyncBlocker? = null,
    val eventsFound: Int = 0,
    val scheduledCount: Int = 0,
    val mutedCount: Int = 0,
    val skippedAllDay: Int = 0,
    val skippedDeclined: Int = 0,
    val skippedOtherCalendars: Int = 0,
    val skippedPastOrTooLate: Int = 0,
    val scheduleFailures: Int = 0,
    val completedAtUtc: Long = System.currentTimeMillis(),
)

class AlarmSyncManager(
    private val context: Context,
    private val preferences: AppPreferences,
    private val database: AppDatabase,
    private val calendarReader: CalendarReader,
    private val scheduler: AlarmScheduler,
) {
    suspend fun synchronize(): CalendarSyncResult {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALENDAR) != PackageManager.PERMISSION_GRANTED) {
            return CalendarSyncResult(blocker = SyncBlocker.CALENDAR_PERMISSION)
        }
        if (!scheduler.canScheduleExactAlarms()) {
            return CalendarSyncResult(blocker = SyncBlocker.EXACT_ALARM_PERMISSION)
        }

        val settings = preferences.settings.first()
        if (settings.selectedCalendarIds.isEmpty()) {
            val dao = database.scheduledAlarmDao()
            dao.getActive().forEach {
                scheduler.cancel(it.requestCode)
                dao.updateState(it.eventInstanceId, AlarmState.CANCELLED, muted = false)
            }
            dao.purgeCancelled()
            return CalendarSyncResult(blocker = SyncBlocker.NO_SELECTED_CALENDARS)
        }

        val now = System.currentTimeMillis()
        val events = calendarReader.readInstances(now, now + SEVEN_DAYS_MS)
        val rejected = mutableMapOf<EventFilter.RejectionReason, Int>()
        val desired = events.mapNotNull { event ->
            val reason = EventFilter.rejectionReason(
                event = event,
                selectedCalendarIds = settings.selectedCalendarIds,
                skipAllDay = settings.skipAllDay,
                skipDeclined = settings.skipDeclined,
                nowUtc = now,
                leadTimeMinutes = settings.leadTimeMinutes,
            )
            if (reason != null) {
                rejected[reason] = rejected.getOrDefault(reason, 0) + 1
                null
            } else {
                event
            }
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
        var scheduledCount = 0
        var mutedCount = 0
        var scheduleFailures = 0
        desired.forEach { item ->
            val old = existing.firstOrNull { it.eventInstanceId == item.eventInstanceId }
            if (old?.muted == true) {
                mutedCount++
                return@forEach
            }
            if (old != null && old.meetingStartUtc == item.meetingStartUtc && old.alarmFireUtc == item.alarmFireUtc) {
                // setAlarmClock alarms do not survive a device reboot; re-applying the same
                // PendingIntent also restores them when this sync runs after boot or update.
                if (scheduler.schedule(item.toSpec(old.requestCode))) {
                    scheduledCount++
                } else {
                    scheduleFailures++
                    scheduler.cancel(old.requestCode)
                    dao.updateState(item.eventInstanceId, AlarmState.CANCELLED, muted = false)
                }
                return@forEach
            }
            val requestCode = old?.requestCode ?: RequestCodeAllocator.allocate(item.eventInstanceId, usedCodes)
            if (old != null) scheduler.cancel(old.requestCode)
            if (scheduler.schedule(item.toSpec(requestCode))) {
                usedCodes += requestCode
                scheduledCount++
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
            } else {
                scheduleFailures++
                if (old != null) dao.updateState(item.eventInstanceId, AlarmState.CANCELLED, muted = false)
            }
        }
        dao.purgeCancelled()

        return CalendarSyncResult(
            eventsFound = events.size,
            scheduledCount = scheduledCount,
            mutedCount = mutedCount,
            skippedAllDay = rejected[EventFilter.RejectionReason.ALL_DAY] ?: 0,
            skippedDeclined = rejected[EventFilter.RejectionReason.DECLINED] ?: 0,
            skippedOtherCalendars = rejected[EventFilter.RejectionReason.UNSELECTED_CALENDAR] ?: 0,
            skippedPastOrTooLate = rejected[EventFilter.RejectionReason.ALARM_TIME_PASSED] ?: 0,
            scheduleFailures = scheduleFailures,
            completedAtUtc = System.currentTimeMillis(),
        )
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
