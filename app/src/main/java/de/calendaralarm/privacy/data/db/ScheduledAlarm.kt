package de.calendaralarm.privacy.data.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "scheduled_alarms",
    indices = [
        Index(value = ["eventInstanceId"], unique = true),
        Index(value = ["requestCode"], unique = true),
    ],
)
data class ScheduledAlarm(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val eventInstanceId: Long,
    val eventId: Long,
    val calendarId: Long,
    val meetingStartUtc: Long,
    val alarmFireUtc: Long,
    val requestCode: Int,
    val state: AlarmState,
    val muted: Boolean,
    // The title is retained only while an alarm is scheduled so the alarm UI remains useful
    // if the provider is temporarily unavailable at fire time. No other event content is saved.
    val eventTitle: String?,
)
