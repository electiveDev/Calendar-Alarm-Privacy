package de.calendaralarm.privacy.alarm

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import de.calendaralarm.privacy.CalendarAlarmApplication
import de.calendaralarm.privacy.MainActivity
import de.calendaralarm.privacy.NotificationHelper
import de.calendaralarm.privacy.R
import de.calendaralarm.privacy.data.db.AlarmState
import de.calendaralarm.privacy.ui.alarm.AlarmActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class AlarmService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var mediaPlayer: MediaPlayer? = null
    private var stopRunnable: Runnable? = null
    private val activeTitles = linkedSetOf<String>()
    private val activeInstances = linkedSetOf<Long>()

    override fun onCreate() {
        super.onCreate()
        NotificationHelper.createAlarmChannel(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopAlarm()
            return START_NOT_STICKY
        }

        val title = intent?.getStringExtra(AlarmReceiver.EXTRA_EVENT_TITLE).orEmpty().ifBlank { "Calendar event" }
        val eventInstanceId = intent?.getLongExtra(AlarmReceiver.EXTRA_EVENT_INSTANCE_ID, Long.MIN_VALUE) ?: Long.MIN_VALUE
        val meetingStartUtc = intent?.getLongExtra(AlarmReceiver.EXTRA_MEETING_START_UTC, System.currentTimeMillis())
            ?: System.currentTimeMillis()
        val isTest = intent?.getBooleanExtra(AlarmReceiver.EXTRA_IS_TEST, false) ?: false
        activeTitles += title
        if (!isTest && eventInstanceId != Long.MIN_VALUE) {
            activeInstances += eventInstanceId
            scope.launch {
                (application as CalendarAlarmApplication).database.scheduledAlarmDao()
                    .updateState(eventInstanceId, AlarmState.FIRED, muted = false)
            }
        }
        val notification = buildNotification(activeTitles.joinToString(" • "), title, meetingStartUtc, eventInstanceId)
        startForeground(NOTIFICATION_ID, notification)
        if (mediaPlayer == null) startAudio()

        val duration = (application as CalendarAlarmApplication).preferences.settings
        scope.launch {
            val seconds = duration.first().maxAlarmDurationSeconds
            stopRunnable?.let(mainHandler::removeCallbacks)
            stopRunnable = Runnable { stopAlarm() }.also {
                mainHandler.postDelayed(it, seconds * 1_000L)
            }
        }
        return START_NOT_STICKY
    }

    private val mainHandler by lazy { android.os.Handler(mainLooper) }

    private fun startAudio() {
        val settings = (application as CalendarAlarmApplication).preferences
        scope.launch {
            val uri = settings.settings.first().alarmSoundUri
            val player = MediaPlayer()
            try {
                player.setAudioAttributes(NotificationHelper.alarmAudioAttributes())
                player.setDataSource(this@AlarmService, uri)
                player.isLooping = true
                player.setOnPreparedListener { it.start() }
                player.prepareAsync()
                mediaPlayer = player
            } catch (_: Exception) {
                player.release()
                startFallbackAudio()
            }
        }
    }

    private fun startFallbackAudio() {
        val fallback = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            ?: return
        try {
            val player = MediaPlayer()
            player.setAudioAttributes(NotificationHelper.alarmAudioAttributes())
            player.setDataSource(this, fallback)
            player.isLooping = true
            player.setOnPreparedListener { it.start() }
            player.prepareAsync()
            mediaPlayer = player
        } catch (_: Exception) {
            // The notification remains visible even if no ringtone URI is available.
        }
    }

    private fun buildNotification(summary: String, title: String, meetingStartUtc: Long, eventInstanceId: Long): Notification {
        val stopIntent = PendingIntent.getService(
            this,
            STOP_REQUEST_CODE,
            Intent(this, AlarmService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val fullScreenIntent = PendingIntent.getActivity(
            this,
            ACTIVE_REQUEST_CODE,
            Intent(this, AlarmActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                putExtra(AlarmReceiver.EXTRA_EVENT_TITLE, title)
                putExtra(AlarmReceiver.EXTRA_MEETING_START_UTC, meetingStartUtc)
                putExtra(AlarmReceiver.EXTRA_EVENT_INSTANCE_ID, eventInstanceId)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        return NotificationCompat.Builder(this, NotificationHelper.CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_alarm)
            .setContentTitle(getString(R.string.alarm_notification_title))
            .setContentText(summary)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setFullScreenIntent(fullScreenIntent, true)
            .addAction(R.drawable.ic_alarm, getString(R.string.alarm_notification_stop), stopIntent)
            .build()
    }

    private fun stopAlarm() {
        scope.launch {
            val dao = (application as CalendarAlarmApplication).database.scheduledAlarmDao()
            activeInstances.forEach { dao.markStateAndClearTitle(it, AlarmState.DISMISSED, muted = false) }
        }
        mediaPlayer?.runCatching { stop() }
        mediaPlayer?.release()
        mediaPlayer = null
        stopRunnable?.let(mainHandler::removeCallbacks)
        stopRunnable = null
        scope.cancel()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        stopAlarm()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val ACTION_STOP = "de.calendaralarm.privacy.action.STOP_ALARM"
        private const val NOTIFICATION_ID = 8_400
        private const val STOP_REQUEST_CODE = 8_401
        private const val ACTIVE_REQUEST_CODE = 8_402
    }
}
