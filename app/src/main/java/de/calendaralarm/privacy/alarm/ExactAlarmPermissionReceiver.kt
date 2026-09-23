package de.calendaralarm.privacy.alarm

import android.app.AlarmManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import de.calendaralarm.privacy.CalendarAlarmApplication
import de.calendaralarm.privacy.worker.CalendarSyncWorker

class ExactAlarmPermissionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val manager = context.getSystemService(AlarmManager::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
            intent.action == AlarmManager.ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED &&
            manager.canScheduleExactAlarms()
        ) {
            val app = context.applicationContext as CalendarAlarmApplication
            CalendarSyncWorker.enqueueOneTime(app)
        }
    }
}
