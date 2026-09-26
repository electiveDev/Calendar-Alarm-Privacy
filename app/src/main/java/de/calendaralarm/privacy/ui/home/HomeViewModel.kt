package de.calendaralarm.privacy.ui.home

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import de.calendaralarm.privacy.CalendarAlarmApplication
import de.calendaralarm.privacy.data.db.ScheduledAlarm
import de.calendaralarm.privacy.domain.CalendarSyncResult
import de.calendaralarm.privacy.worker.CalendarSyncWorker
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class ReliabilityState(
    val calendarAccess: Boolean,
    val notifications: Boolean,
    val exactAlarms: Boolean,
    val fullScreen: Boolean,
    val dnd: Boolean,
    val selectedCalendars: Boolean,
) {
    val canSchedule: Boolean get() = calendarAccess && exactAlarms && selectedCalendars
}

data class HomeSyncState(
    val isRefreshing: Boolean = false,
    val result: CalendarSyncResult? = null,
    val failed: Boolean = false,
)

class HomeViewModel(private val app: CalendarAlarmApplication) : ViewModel() {
    private val now = System.currentTimeMillis()
    val alarms: StateFlow<List<ScheduledAlarm>> = app.database.scheduledAlarmDao()
        .observeUpcoming(now, Long.MAX_VALUE)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    private val _reliability = MutableStateFlow(ReliabilityState(false, false, false, false, false, false))
    val reliability: StateFlow<ReliabilityState> = _reliability
    private val _calendarNames = MutableStateFlow<Map<Long, String>>(emptyMap())
    val calendarNames: StateFlow<Map<Long, String>> = _calendarNames
    private val _syncState = MutableStateFlow(HomeSyncState(isRefreshing = true))
    val syncState: StateFlow<HomeSyncState> = _syncState
    private var refreshJob: Job? = null

    fun refresh() {
        if (refreshJob?.isActive == true) return
        refreshJob = viewModelScope.launch {
            _syncState.value = _syncState.value.copy(isRefreshing = true, failed = false)
            try {
                _calendarNames.value = withContext(Dispatchers.IO) {
                    app.calendarReader.readCalendars().associate { it.id to it.displayName }
                }
                val result = withContext(Dispatchers.IO) { app.syncManager.synchronize() }
                _syncState.value = HomeSyncState(result = result)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                _syncState.value = HomeSyncState(failed = true)
            } finally {
                _reliability.value = readReliability()
            }
        }
    }

    fun mute(alarm: ScheduledAlarm) {
        viewModelScope.launch {
            app.alarmScheduler.cancel(alarm.requestCode)
            app.database.scheduledAlarmDao().updateState(alarm.eventInstanceId, de.calendaralarm.privacy.data.db.AlarmState.MUTED, muted = true)
        }
    }

    fun unmute(alarm: ScheduledAlarm) {
        viewModelScope.launch {
            if (app.alarmScheduler.schedule(
                    de.calendaralarm.privacy.alarm.AlarmSpec(
                        eventInstanceId = alarm.eventInstanceId,
                        eventId = alarm.eventId,
                        calendarId = alarm.calendarId,
                        meetingStartUtc = alarm.meetingStartUtc,
                        alarmFireUtc = alarm.alarmFireUtc,
                        requestCode = alarm.requestCode,
                        eventTitle = alarm.eventTitle,
                    ),
                )) {
                app.database.scheduledAlarmDao().upsert(alarm.copy(state = de.calendaralarm.privacy.data.db.AlarmState.SCHEDULED, muted = false))
            }
        }
    }

    private suspend fun readReliability(): ReliabilityState {
        val context = app as Context
        val exact = app.alarmScheduler.canScheduleExactAlarms()
        val fullScreen = if (Build.VERSION.SDK_INT >= 34) {
            context.getSystemService(NotificationManager::class.java).canUseFullScreenIntent()
        } else true
        val dnd = context.getSystemService(NotificationManager::class.java).isNotificationPolicyAccessGranted
        val notifications = NotificationManagerCompat.from(context).areNotificationsEnabled() &&
            (Build.VERSION.SDK_INT < 33 || ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED)
        val calendar = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALENDAR) == PackageManager.PERMISSION_GRANTED
        return ReliabilityState(calendar, notifications, exact, fullScreen, dnd, app.preferences.settings.first().selectedCalendarIds.isNotEmpty())
    }
}
