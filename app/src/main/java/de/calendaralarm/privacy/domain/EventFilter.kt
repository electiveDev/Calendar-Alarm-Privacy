package de.calendaralarm.privacy.domain

import de.calendaralarm.privacy.data.calendar.CalendarEvent

object EventFilter {
    const val ATTENDEE_STATUS_DECLINED = 2

    fun isEligible(
        event: CalendarEvent,
        selectedCalendarIds: Set<Long>,
        skipAllDay: Boolean,
        skipDeclined: Boolean,
        nowUtc: Long,
        leadTimeMinutes: Int,
    ): Boolean {
        if (event.calendarId !in selectedCalendarIds) return false
        if (skipAllDay && event.allDay) return false
        if (skipDeclined && event.selfAttendeeStatus == ATTENDEE_STATUS_DECLINED) return false
        val alarmTime = AlarmTimeCalculator.alarmFireUtc(event.beginUtc, leadTimeMinutes)
        return event.beginUtc > nowUtc && alarmTime > nowUtc
    }
}
