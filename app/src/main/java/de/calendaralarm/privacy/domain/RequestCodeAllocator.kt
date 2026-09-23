package de.calendaralarm.privacy.domain

object RequestCodeAllocator {
    fun allocate(eventInstanceId: Long, usedCodes: Set<Int>): Int {
        var candidate = ((eventInstanceId xor (eventInstanceId ushr 32)).toInt() and Int.MAX_VALUE)
        if (candidate == 0) candidate = 1
        while (candidate in usedCodes) {
            candidate = if (candidate == Int.MAX_VALUE) 1 else candidate + 1
        }
        return candidate
    }
}
