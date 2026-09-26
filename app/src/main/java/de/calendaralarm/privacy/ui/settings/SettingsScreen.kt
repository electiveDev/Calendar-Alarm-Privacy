package de.calendaralarm.privacy.ui.settings

import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.calendaralarm.privacy.CalendarAlarmApplication
import de.calendaralarm.privacy.data.calendar.CalendarInfo
import de.calendaralarm.privacy.data.prefs.AppSettings
import de.calendaralarm.privacy.worker.CalendarSyncWorker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(app: CalendarAlarmApplication, onBack: () -> Unit) {
    val settings by app.preferences.settings.collectAsStateWithLifecycle(initialValue = AppSettings())
    val scope = rememberCoroutineScope()
    var calendars by remember { mutableStateOf<List<CalendarInfo>>(emptyList()) }
    val ringtonePicker = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val pickedUri = if (Build.VERSION.SDK_INT >= 33) {
            result.data?.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI, Uri::class.java)
        } else {
            @Suppress("DEPRECATION")
            result.data?.getParcelableExtra<Uri>(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
        }
        pickedUri?.let { uri ->
            scope.launch {
                app.preferences.setAlarmSoundUri(uri)
                CalendarSyncWorker.enqueueOneTime(app)
            }
        }
    }

    LaunchedEffect(Unit) {
        calendars = withContext(Dispatchers.IO) { app.calendarReader.readCalendars() }
    }

    fun resync(block: suspend () -> Unit) {
        scope.launch {
            block()
            CalendarSyncWorker.enqueueOneTime(app)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") }
                },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item { Text("Calendars", style = MaterialTheme.typography.headlineSmall) }
            items(calendars, key = { it.id }) { calendar ->
                Row(Modifier.fillMaxWidth()) {
                    Checkbox(
                        checked = calendar.id in settings.selectedCalendarIds,
                        onCheckedChange = { checked ->
                            val newSelection = if (checked) settings.selectedCalendarIds + calendar.id else settings.selectedCalendarIds - calendar.id
                            resync { app.preferences.setSelectedCalendarIds(newSelection) }
                        },
                    )
                    Column {
                        Text(calendar.displayName)
                        calendar.accountLabel?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
                    }
                }
            }
            item {
                NumberSetting(
                    title = "Alarm before event",
                    value = "${settings.leadTimeMinutes} minutes",
                    decreaseEnabled = settings.leadTimeMinutes > 0,
                    increaseEnabled = settings.leadTimeMinutes < 120,
                    onDecrease = { resync { app.preferences.setLeadTimeMinutes(settings.leadTimeMinutes - 1) } },
                    onIncrease = { resync { app.preferences.setLeadTimeMinutes(settings.leadTimeMinutes + 1) } },
                )
            }
            item {
                Column {
                    ToggleSetting("Skip all-day events", settings.skipAllDay) { checked -> resync { app.preferences.setSkipAllDay(checked) } }
                    if (!settings.skipAllDay) {
                        Text(
                            "All-day events use their calendar start time, which is often midnight. Set this only if that alarm time is intended.",
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
                ToggleSetting("Skip declined events", settings.skipDeclined) { checked -> resync { app.preferences.setSkipDeclined(checked) } }
            }
            item {
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Alarm sound", style = MaterialTheme.typography.titleMedium)
                        Text(settings.alarmSoundUri.toString(), style = MaterialTheme.typography.bodySmall)
                        Button(onClick = {
                            ringtonePicker.launch(Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply {
                                putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_ALARM)
                                putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, "Choose alarm sound")
                                putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, settings.alarmSoundUri)
                            })
                        }) { Text("Choose alarm sound") }
                    }
                }
            }
            item {
                NumberSetting(
                    title = "Maximum ringing time",
                    value = "${settings.maxAlarmDurationSeconds} seconds",
                    decreaseEnabled = settings.maxAlarmDurationSeconds > 15,
                    increaseEnabled = settings.maxAlarmDurationSeconds < 600,
                    onDecrease = { resync { app.preferences.setMaxAlarmDurationSeconds(settings.maxAlarmDurationSeconds - 15) } },
                    onIncrease = { resync { app.preferences.setMaxAlarmDurationSeconds(settings.maxAlarmDurationSeconds + 15) } },
                )
            }
            item {
                Button(onClick = { app.alarmScheduler.scheduleTest(settings) }, modifier = Modifier.fillMaxWidth()) {
                    Text("Test alarm in 10 seconds")
                }
            }
            item {
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Privacy information", style = MaterialTheme.typography.titleMedium)
                        Text("Calendar Alarm Privacy works entirely on this device.")
                        Text("The app has no Internet permission and cannot upload calendar data.")
                        Text("Calendar access is read-only.")
                    }
                }
            }
        }
    }
}

@Composable
private fun NumberSetting(
    title: String,
    value: String,
    decreaseEnabled: Boolean,
    increaseEnabled: Boolean,
    onDecrease: () -> Unit,
    onIncrease: () -> Unit,
) {
    Card(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            Column { Text(title, style = MaterialTheme.typography.titleMedium); Text(value) }
            Row {
                IconButton(onClick = onDecrease, enabled = decreaseEnabled) { Icon(Icons.Default.Remove, contentDescription = "Decrease") }
                IconButton(onClick = onIncrease, enabled = increaseEnabled) { Icon(Icons.Default.Add, contentDescription = "Increase") }
            }
        }
    }
}

@Composable
private fun ToggleSetting(title: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(title, modifier = Modifier.padding(vertical = 12.dp))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
