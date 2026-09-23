package de.calendaralarm.privacy

import de.calendaralarm.privacy.domain.RequestCodeAllocator
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RequestCodeAllocatorTest {
    @Test
    fun allocatorDoesNotReuseActiveCode() {
        val first = RequestCodeAllocator.allocate(123L, emptySet())
        val second = RequestCodeAllocator.allocate(123L, setOf(first))
        assertNotEquals(first, second)
        assertTrue(second > 0)
    }
}
