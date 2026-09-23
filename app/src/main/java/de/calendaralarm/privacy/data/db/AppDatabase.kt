package de.calendaralarm.privacy.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters

class AlarmStateConverters {
    @TypeConverter
    fun fromState(value: AlarmState): String = value.name

    @TypeConverter
    fun toState(value: String): AlarmState = AlarmState.valueOf(value)
}

@Database(entities = [ScheduledAlarm::class], version = 1, exportSchema = false)
@TypeConverters(AlarmStateConverters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun scheduledAlarmDao(): ScheduledAlarmDao

    companion object {
        fun create(context: Context): AppDatabase = Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "calendar_alarm_privacy.db",
        ).fallbackToDestructiveMigration(dropAllTables = true).build()
    }
}
