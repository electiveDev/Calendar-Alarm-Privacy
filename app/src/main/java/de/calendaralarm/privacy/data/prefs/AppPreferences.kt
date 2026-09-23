package de.calendaralarm.privacy.data.prefs

import android.content.Context
import android.media.RingtoneManager
import android.net.Uri
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore by preferencesDataStore(name = "settings")

data class AppSettings(
    val selectedCalendarIds: Set<Long> = emptySet(),
    val leadTimeMinutes: Int = 0,
    val skipAllDay: Boolean = true,
    val skipDeclined: Boolean = true,
    val alarmSoundUri: Uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM),
    val maxAlarmDurationSeconds: Int = 60,
    val onboardingCompleted: Boolean = false,
)

class AppPreferences(private val context: Context) {
    private object Keys {
        val selectedCalendarIds = stringSetPreferencesKey("selected_calendar_ids")
        val leadTimeMinutes = intPreferencesKey("lead_time_minutes")
        val skipAllDay = booleanPreferencesKey("skip_all_day")
        val skipDeclined = booleanPreferencesKey("skip_declined")
        val alarmSoundUri = stringPreferencesKey("alarm_sound_uri")
        val maxAlarmDurationSeconds = intPreferencesKey("max_alarm_duration_seconds")
        val onboardingCompleted = booleanPreferencesKey("onboarding_completed")
    }

    val settings: Flow<AppSettings> = context.settingsDataStore.data.map { preferences ->
        preferences.toSettings()
    }

    suspend fun setSelectedCalendarIds(ids: Set<Long>) {
        context.settingsDataStore.edit { it[Keys.selectedCalendarIds] = ids.map(Long::toString).toSet() }
    }

    suspend fun setLeadTimeMinutes(value: Int) {
        context.settingsDataStore.edit { it[Keys.leadTimeMinutes] = value.coerceIn(0, 120) }
    }

    suspend fun setSkipAllDay(value: Boolean) {
        context.settingsDataStore.edit { it[Keys.skipAllDay] = value }
    }

    suspend fun setSkipDeclined(value: Boolean) {
        context.settingsDataStore.edit { it[Keys.skipDeclined] = value }
    }

    suspend fun setAlarmSoundUri(value: Uri) {
        context.settingsDataStore.edit { it[Keys.alarmSoundUri] = value.toString() }
    }

    suspend fun setMaxAlarmDurationSeconds(value: Int) {
        context.settingsDataStore.edit { it[Keys.maxAlarmDurationSeconds] = value.coerceIn(15, 600) }
    }

    suspend fun setOnboardingCompleted(value: Boolean) {
        context.settingsDataStore.edit { it[Keys.onboardingCompleted] = value }
    }

    private fun Preferences.toSettings(): AppSettings {
        val defaultUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
        val parsedUri = this[Keys.alarmSoundUri]?.let(Uri::parse) ?: defaultUri
        val ids = this[Keys.selectedCalendarIds].orEmpty().mapNotNull(String::toLongOrNull).toSet()
        return AppSettings(
            selectedCalendarIds = ids,
            leadTimeMinutes = this[Keys.leadTimeMinutes] ?: 0,
            skipAllDay = this[Keys.skipAllDay] ?: true,
            skipDeclined = this[Keys.skipDeclined] ?: true,
            alarmSoundUri = parsedUri,
            maxAlarmDurationSeconds = this[Keys.maxAlarmDurationSeconds] ?: 60,
            onboardingCompleted = this[Keys.onboardingCompleted] ?: false,
        )
    }
}
