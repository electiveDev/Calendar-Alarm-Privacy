package de.calendaralarm.privacy.data.calendar

data class CalendarInfo(
    val id: Long,
    val displayName: String,
    val accountLabel: String?,
    val color: Int,
)

data class CalendarEvent(
    val instanceId: Long,
    val eventId: Long,
    val calendarId: Long,
    val title: String?,
    val beginUtc: Long,
    val endUtc: Long,
    val allDay: Boolean,
    val selfAttendeeStatus: Int?,
    val calendarName: String?,
    val calendarColor: Int?,
)
