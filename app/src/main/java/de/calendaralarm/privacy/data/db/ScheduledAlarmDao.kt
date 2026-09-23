package de.calendaralarm.privacy.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ScheduledAlarmDao {
    @Query(
        "SELECT * FROM scheduled_alarms " +
            "WHERE state IN ('SCHEDULED', 'MUTED') " +
            "AND alarmFireUtc BETWEEN :fromUtc AND :untilUtc " +
            "ORDER BY alarmFireUtc ASC",
    )
    fun observeUpcoming(fromUtc: Long, untilUtc: Long): Flow<List<ScheduledAlarm>>

    @Query("SELECT * FROM scheduled_alarms WHERE state IN ('SCHEDULED', 'MUTED')")
    suspend fun getActive(): List<ScheduledAlarm>

    @Query("SELECT * FROM scheduled_alarms WHERE eventInstanceId = :eventInstanceId LIMIT 1")
    suspend fun findByEventInstanceId(eventInstanceId: Long): ScheduledAlarm?

    @Query("SELECT requestCode FROM scheduled_alarms WHERE state IN ('SCHEDULED', 'MUTED')")
    suspend fun getActiveRequestCodes(): List<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(alarm: ScheduledAlarm)

    @Query("UPDATE scheduled_alarms SET state = :state, muted = :muted WHERE eventInstanceId = :eventInstanceId")
    suspend fun updateState(eventInstanceId: Long, state: AlarmState, muted: Boolean)

    @Query("UPDATE scheduled_alarms SET state = :state, muted = :muted, eventTitle = NULL WHERE eventInstanceId = :eventInstanceId")
    suspend fun markStateAndClearTitle(eventInstanceId: Long, state: AlarmState, muted: Boolean)

    @Query("DELETE FROM scheduled_alarms WHERE eventInstanceId = :eventInstanceId")
    suspend fun deleteByEventInstanceId(eventInstanceId: Long)

    @Query("DELETE FROM scheduled_alarms WHERE state = 'CANCELLED'")
    suspend fun purgeCancelled()
}
