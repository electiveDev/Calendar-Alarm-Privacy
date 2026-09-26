package de.calendaralarm.privacy.domain

import de.calendaralarm.privacy.data.calendar.CalendarEvent

object EventFilter {
    const val ATTENDEE_STATUS_DECLINED = 2

    enum class RejectionReason {
        UNSELECTED_CALENDAR,
        ALL_DAY,
        DECLINED,
        ALARM_TIME_PASSED,
    }

    fun isEligible(
        event: CalendarEvent,
        selectedCalendarIds: Set<Long>,
        skipAllDay: Boolean,
        skipDeclined: Boolean,
        nowUtc: Long,
        leadTimeMinutes: Int,
    ): Boolean = rejectionReason(
        event = event,
        selectedCalendarIds = selectedCalendarIds,
        skipAllDay = skipAllDay,
        skipDeclined = skipDeclined,
        nowUtc = nowUtc,
        leadTimeMinutes = leadTimeMinutes,
    ) == null

    fun rejectionReason(
        event: CalendarEvent,
        selectedCalendarIds: Set<Long>,
        skipAllDay: Boolean,
        skipDeclined: Boolean,
        nowUtc: Long,
        leadTimeMinutes: Int,
    ): RejectionReason? = when {
        event.calendarId !in selectedCalendarIds -> RejectionReason.UNSELECTED_CALENDAR
        skipAllDay && event.allDay -> RejectionReason.ALL_DAY
        skipDeclined && event.selfAttendeeStatus == ATTENDEE_STATUS_DECLINED -> RejectionReason.DECLINED
        event.beginUtc <= nowUtc || AlarmTimeCalculator.alarmFireUtc(event.beginUtc, leadTimeMinutes) <= nowUtc ->
            RejectionReason.ALARM_TIME_PASSED
        else -> null
    }
}
