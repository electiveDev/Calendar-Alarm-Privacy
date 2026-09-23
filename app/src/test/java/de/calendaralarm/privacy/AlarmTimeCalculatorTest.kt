package de.calendaralarm.privacy

import de.calendaralarm.privacy.domain.AlarmTimeCalculator
import org.junit.Assert.assertEquals
import org.junit.Test

class AlarmTimeCalculatorTest {
    @Test
    fun zeroLeadKeepsEventTime() {
        assertEquals(10_000L, AlarmTimeCalculator.alarmFireUtc(10_000L, 0))
    }

    @Test
    fun fiveMinuteLeadMovesAlarmEarlier() {
        assertEquals(10_000L - 5 * 60_000L, AlarmTimeCalculator.alarmFireUtc(10_000L, 5))
    }
}
