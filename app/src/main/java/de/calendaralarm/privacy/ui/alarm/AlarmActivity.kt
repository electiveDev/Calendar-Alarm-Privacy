package de.calendaralarm.privacy.ui.alarm

import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import de.calendaralarm.privacy.alarm.AlarmReceiver
import de.calendaralarm.privacy.alarm.AlarmService
import de.calendaralarm.privacy.CalendarAlarmApplication
import de.calendaralarm.privacy.data.db.AlarmState
import de.calendaralarm.privacy.ui.theme.CalendarAlarmTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AlarmActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setShowWhenLocked(true)
        setTurnScreenOn(true)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        getSystemService(KeyguardManager::class.java)?.requestDismissKeyguard(this, null)
        setContent {
            CalendarAlarmTheme {
                AlarmScreen(
                    title = intent.getStringExtra(AlarmReceiver.EXTRA_EVENT_TITLE).orEmpty().ifBlank { "Calendar event" },
                    startUtc = intent.getLongExtra(AlarmReceiver.EXTRA_MEETING_START_UTC, System.currentTimeMillis()),
                    onStop = ::stopAlarm,
                )
            }
        }
    }

    private fun stopAlarm() {
        val eventInstanceId = intent.getLongExtra(AlarmReceiver.EXTRA_EVENT_INSTANCE_ID, Long.MIN_VALUE)
        if (eventInstanceId != Long.MIN_VALUE) {
            lifecycleScope.launch(Dispatchers.IO) {
                (application as CalendarAlarmApplication).database.scheduledAlarmDao()
                    .markStateAndClearTitle(eventInstanceId, AlarmState.DISMISSED, muted = false)
            }
        }
        ContextCompat.startForegroundService(
            this,
            Intent(this, AlarmService::class.java).setAction(AlarmService.ACTION_STOP),
        )
        finish()
    }
}
