package de.calendaralarm.privacy.domain

object AlarmTimeCalculator {
    fun alarmFireUtc(eventBeginUtc: Long, leadTimeMinutes: Int): Long =
        eventBeginUtc - leadTimeMinutes.coerceIn(0, 120) * 60_000L
}
