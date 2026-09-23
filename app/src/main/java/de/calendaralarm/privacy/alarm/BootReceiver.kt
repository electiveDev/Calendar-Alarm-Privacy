package de.calendaralarm.privacy.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import de.calendaralarm.privacy.worker.CalendarSyncWorker

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED || intent.action == Intent.ACTION_MY_PACKAGE_REPLACED) {
            CalendarSyncWorker.enqueueOneTime(context)
        }
    }
}
