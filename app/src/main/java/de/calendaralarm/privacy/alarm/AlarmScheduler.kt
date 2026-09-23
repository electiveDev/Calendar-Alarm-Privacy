package de.calendaralarm.privacy.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import de.calendaralarm.privacy.MainActivity
import de.calendaralarm.privacy.data.prefs.AppSettings

data class AlarmSpec(
    val eventInstanceId: Long,
    val eventId: Long,
    val calendarId: Long,
    val meetingStartUtc: Long,
    val alarmFireUtc: Long,
    val requestCode: Int,
    val eventTitle: String?,
    val isTest: Boolean = false,
)

class AlarmScheduler(private val context: Context) {
    private val alarmManager = context.getSystemService(AlarmManager::class.java)

    fun canScheduleExactAlarms(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager.canScheduleExactAlarms()

    fun schedule(spec: AlarmSpec): Boolean {
        if (!canScheduleExactAlarms()) return false
        alarmManager.setAlarmClock(
            AlarmManager.AlarmClockInfo(spec.alarmFireUtc, showIntent(spec.requestCode)),
            triggerIntent(spec),
        )
        return true
    }

    fun scheduleTest(settings: AppSettings): Boolean {
        val triggerAt = System.currentTimeMillis() + 10_000L
        return schedule(
            AlarmSpec(
                eventInstanceId = -triggerAt,
                eventId = -1L,
                calendarId = -1L,
                meetingStartUtc = triggerAt,
                alarmFireUtc = triggerAt,
                requestCode = TEST_REQUEST_CODE,
                eventTitle = "Test alarm",
                isTest = true,
            ),
        )
    }

    fun cancel(requestCode: Int) {
        alarmManager.cancel(triggerPendingIntent(requestCode))
    }

    private fun triggerIntent(spec: AlarmSpec): PendingIntent = PendingIntent.getBroadcast(
        context,
        spec.requestCode,
        Intent(context, AlarmReceiver::class.java).apply {
            putExtra(AlarmReceiver.EXTRA_EVENT_INSTANCE_ID, spec.eventInstanceId)
            putExtra(AlarmReceiver.EXTRA_EVENT_ID, spec.eventId)
            putExtra(AlarmReceiver.EXTRA_CALENDAR_ID, spec.calendarId)
            putExtra(AlarmReceiver.EXTRA_MEETING_START_UTC, spec.meetingStartUtc)
            putExtra(AlarmReceiver.EXTRA_EVENT_TITLE, spec.eventTitle)
            putExtra(AlarmReceiver.EXTRA_IS_TEST, spec.isTest)
        },
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    private fun triggerPendingIntent(requestCode: Int): PendingIntent = PendingIntent.getBroadcast(
        context,
        requestCode,
        Intent(context, AlarmReceiver::class.java),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    private fun showIntent(requestCode: Int): PendingIntent = PendingIntent.getActivity(
        context,
        requestCode,
        Intent(context, MainActivity::class.java),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    companion object {
        const val TEST_REQUEST_CODE = 2_147_000_001
    }
}
