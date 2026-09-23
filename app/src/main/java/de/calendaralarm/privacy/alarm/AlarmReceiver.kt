package de.calendaralarm.privacy.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.PowerManager
import androidx.core.content.ContextCompat

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val wakeLock = context.getSystemService(PowerManager::class.java)
            .newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "CalendarAlarmPrivacy:alarm-transition")
        wakeLock.acquire(10_000L)
        try {
            val serviceIntent = Intent(context, AlarmService::class.java).apply {
                putExtras(intent)
            }
            ContextCompat.startForegroundService(context, serviceIntent)
        } finally {
            if (wakeLock.isHeld) wakeLock.release()
        }
    }

    companion object {
        const val EXTRA_EVENT_INSTANCE_ID = "event_instance_id"
        const val EXTRA_EVENT_ID = "event_id"
        const val EXTRA_CALENDAR_ID = "calendar_id"
        const val EXTRA_MEETING_START_UTC = "meeting_start_utc"
        const val EXTRA_EVENT_TITLE = "event_title"
        const val EXTRA_IS_TEST = "is_test"
    }
}
