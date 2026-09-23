package de.calendaralarm.privacy

import de.calendaralarm.privacy.data.calendar.CalendarEvent
import de.calendaralarm.privacy.domain.EventFilter
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EventFilterTest {
    private val event = CalendarEvent(1L, 2L, 3L, "Private title", 100_000L, 120_000L, false, 1, "Personal", 0)

    @Test
    fun onlySelectedCalendarIsEligible() {
        assertTrue(EventFilter.isEligible(event, setOf(3L), true, true, 0L, 0))
        assertFalse(EventFilter.isEligible(event, setOf(4L), true, true, 0L, 0))
    }

    @Test
    fun allDayAndDeclinedEventsCanBeFiltered() {
        assertFalse(EventFilter.isEligible(event.copy(allDay = true), setOf(3L), true, true, 0L, 0))
        assertFalse(EventFilter.isEligible(event.copy(selfAttendeeStatus = EventFilter.ATTENDEE_STATUS_DECLINED), setOf(3L), true, true, 0L, 0))
    }

    @Test
    fun pastAlarmIsRejected() {
        assertFalse(EventFilter.isEligible(event.copy(beginUtc = 100L), setOf(3L), true, true, 200L, 0))
    }
}
