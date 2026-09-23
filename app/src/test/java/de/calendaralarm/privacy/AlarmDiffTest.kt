package de.calendaralarm.privacy

import de.calendaralarm.privacy.domain.AlarmDiff
import de.calendaralarm.privacy.domain.AlarmDiffAction
import de.calendaralarm.privacy.domain.DesiredAlarm
import de.calendaralarm.privacy.domain.ExistingAlarm
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AlarmDiffTest {
    private val desired = DesiredAlarm(1L, 2L, 3L, 100L, 100L, "event")

    @Test
    fun newEventIsScheduled() {
        assertTrue(AlarmDiff.calculate(listOf(desired), emptyList()).single() is AlarmDiffAction.Schedule)
    }

    @Test
    fun unchangedEventIsNoop() {
        val existing = ExistingAlarm(1L, 100L, 100L, 42, false)
        assertTrue(AlarmDiff.calculate(listOf(desired), listOf(existing)).single() is AlarmDiffAction.Noop)
    }

    @Test
    fun movedEventCancelsAndSchedules() {
        val existing = ExistingAlarm(1L, 200L, 200L, 42, false)
        val actions = AlarmDiff.calculate(listOf(desired), listOf(existing))
        assertEquals(2, actions.size)
        assertTrue(actions[0] is AlarmDiffAction.Cancel)
        assertTrue(actions[1] is AlarmDiffAction.Schedule)
    }

    @Test
    fun mutedEventIsNotRescheduled() {
        val existing = ExistingAlarm(1L, 200L, 200L, 42, true)
        assertTrue(AlarmDiff.calculate(listOf(desired), listOf(existing)).single() is AlarmDiffAction.Noop)
    }
}
