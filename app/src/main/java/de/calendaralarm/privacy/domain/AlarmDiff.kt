package de.calendaralarm.privacy.domain

data class DesiredAlarm(
    val eventInstanceId: Long,
    val eventId: Long,
    val calendarId: Long,
    val meetingStartUtc: Long,
    val alarmFireUtc: Long,
    val eventTitle: String?,
)

data class ExistingAlarm(
    val eventInstanceId: Long,
    val meetingStartUtc: Long,
    val alarmFireUtc: Long,
    val requestCode: Int,
    val muted: Boolean,
)

sealed interface AlarmDiffAction {
    data class Schedule(val desired: DesiredAlarm) : AlarmDiffAction
    data class Cancel(val existing: ExistingAlarm) : AlarmDiffAction
    data class Noop(val existing: ExistingAlarm) : AlarmDiffAction
}

object AlarmDiff {
    fun calculate(
        desired: List<DesiredAlarm>,
        existing: List<ExistingAlarm>,
    ): List<AlarmDiffAction> {
        val existingById = existing.associateBy { it.eventInstanceId }
        val desiredById = desired.associateBy { it.eventInstanceId }
        val actions = mutableListOf<AlarmDiffAction>()
        existing.filter { it.eventInstanceId !in desiredById }.forEach { actions += AlarmDiffAction.Cancel(it) }
        desired.forEach { item ->
            val old = existingById[item.eventInstanceId]
            when {
                old == null -> actions += AlarmDiffAction.Schedule(item)
                old.muted -> actions += AlarmDiffAction.Noop(old)
                old.meetingStartUtc == item.meetingStartUtc && old.alarmFireUtc == item.alarmFireUtc -> {
                    actions += AlarmDiffAction.Noop(old)
                }
                else -> {
                    actions += AlarmDiffAction.Cancel(old)
                    actions += AlarmDiffAction.Schedule(item)
                }
            }
        }
        return actions
    }
}
