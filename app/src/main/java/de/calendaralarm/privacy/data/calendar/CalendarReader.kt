package de.calendaralarm.privacy.data.calendar

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.provider.CalendarContract
import androidx.core.content.ContextCompat

class CalendarReader(private val context: Context) {
    fun hasReadPermission(): Boolean = ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.READ_CALENDAR,
    ) == PackageManager.PERMISSION_GRANTED

    fun readCalendars(): List<CalendarInfo> {
        if (!hasReadPermission()) return emptyList()
        val result = mutableListOf<CalendarInfo>()
        val projection = arrayOf(
            CalendarContract.Calendars._ID,
            CalendarContract.Calendars.CALENDAR_DISPLAY_NAME,
            CalendarContract.Calendars.ACCOUNT_NAME,
            CalendarContract.Calendars.CALENDAR_COLOR,
            CalendarContract.Calendars.VISIBLE,
        )
        context.contentResolver.query(
            CalendarContract.Calendars.CONTENT_URI,
            projection,
            "${CalendarContract.Calendars.VISIBLE} = 1",
            null,
            CalendarContract.Calendars.CALENDAR_DISPLAY_NAME + " COLLATE NOCASE ASC",
        )?.use { cursor ->
            val idIndex = cursor.getColumnIndexOrThrow(CalendarContract.Calendars._ID)
            val nameIndex = cursor.getColumnIndexOrThrow(CalendarContract.Calendars.CALENDAR_DISPLAY_NAME)
            val accountIndex = cursor.getColumnIndex(CalendarContract.Calendars.ACCOUNT_NAME)
            val colorIndex = cursor.getColumnIndex(CalendarContract.Calendars.CALENDAR_COLOR)
            while (cursor.moveToNext()) {
                result += CalendarInfo(
                    id = cursor.getLong(idIndex),
                    displayName = cursor.getString(nameIndex).orEmpty(),
                    accountLabel = accountIndex.takeIf { it >= 0 && !cursor.isNull(it) }?.let(cursor::getString),
                    color = colorIndex.takeIf { it >= 0 && !cursor.isNull(it) }?.let(cursor::getInt) ?: 0,
                )
            }
        }
        return result
    }

    fun readInstances(beginUtc: Long, endUtc: Long): List<CalendarEvent> {
        if (!hasReadPermission()) return emptyList()
        val uri = CalendarContract.Instances.CONTENT_URI.buildUpon()
            .appendPath(beginUtc.toString())
            .appendPath(endUtc.toString())
            .build()
        val projection = arrayOf(
            CalendarContract.Instances._ID,
            CalendarContract.Instances.EVENT_ID,
            CalendarContract.Instances.TITLE,
            CalendarContract.Instances.BEGIN,
            CalendarContract.Instances.END,
            CalendarContract.Instances.ALL_DAY,
            CalendarContract.Instances.SELF_ATTENDEE_STATUS,
            CalendarContract.Instances.CALENDAR_ID,
            CalendarContract.Instances.CALENDAR_COLOR,
            CalendarContract.Instances.CALENDAR_DISPLAY_NAME,
        )
        val result = mutableListOf<CalendarEvent>()
        context.contentResolver.query(uri, projection, null, null, CalendarContract.Instances.BEGIN + " ASC")
            ?.use { cursor ->
                val id = cursor.getColumnIndexOrThrow(CalendarContract.Instances._ID)
                val eventId = cursor.getColumnIndexOrThrow(CalendarContract.Instances.EVENT_ID)
                val title = cursor.getColumnIndexOrThrow(CalendarContract.Instances.TITLE)
                val begin = cursor.getColumnIndexOrThrow(CalendarContract.Instances.BEGIN)
                val end = cursor.getColumnIndexOrThrow(CalendarContract.Instances.END)
                val allDay = cursor.getColumnIndexOrThrow(CalendarContract.Instances.ALL_DAY)
                val attendee = cursor.getColumnIndexOrThrow(CalendarContract.Instances.SELF_ATTENDEE_STATUS)
                val calendarId = cursor.getColumnIndexOrThrow(CalendarContract.Instances.CALENDAR_ID)
                val color = cursor.getColumnIndexOrThrow(CalendarContract.Instances.CALENDAR_COLOR)
                val calendarName = cursor.getColumnIndexOrThrow(CalendarContract.Instances.CALENDAR_DISPLAY_NAME)
                while (cursor.moveToNext()) {
                    result += CalendarEvent(
                        instanceId = cursor.getLong(id),
                        eventId = cursor.getLong(eventId),
                        calendarId = cursor.getLong(calendarId),
                        title = cursor.getString(title),
                        beginUtc = cursor.getLong(begin),
                        endUtc = cursor.getLong(end),
                        allDay = cursor.getInt(allDay) != 0,
                        selfAttendeeStatus = cursor.getInt(attendee),
                        calendarName = cursor.getString(calendarName),
                        calendarColor = cursor.getInt(color),
                    )
                }
            }
        return result
    }
}
