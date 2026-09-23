package de.calendaralarm.privacy.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import de.calendaralarm.privacy.data.db.ScheduledAlarm
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onOpenSettings: () -> Unit,
) {
    val alarms by viewModel.alarms.collectAsStateWithLifecycleCompat()
    val reliability by viewModel.reliability.collectAsStateWithLifecycleCompat()
    val calendarNames by viewModel.calendarNames.collectAsStateWithLifecycleCompat()
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Calendar Alarm Privacy") },
                actions = {
                    IconButton(onClick = viewModel::refresh) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh calendars")
                    }
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "Open settings")
                    }
                },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item { ReliabilityCard(reliability) }
            item { Text("Upcoming alarms", style = MaterialTheme.typography.headlineSmall) }
            if (alarms.isEmpty()) {
                item { Text("No upcoming alarms.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
            } else {
                items(alarms, key = { it.eventInstanceId }) { alarm ->
                    AlarmCard(alarm, calendarNames[alarm.calendarId] ?: "Selected calendar", onMutedChanged = { enabled -> if (enabled) viewModel.unmute(alarm) else viewModel.mute(alarm) })
                }
            }
        }
    }
}

@Composable
private fun ReliabilityCard(state: ReliabilityState) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Alarm reliability", style = MaterialTheme.typography.titleLarge)
            ReliabilityRow("Calendar access", state.calendarAccess)
            ReliabilityRow("Notifications", state.notifications)
            ReliabilityRow("Exact alarm access", state.exactAlarms)
            ReliabilityRow("Full-screen alarm", state.fullScreen)
            ReliabilityRow("DND bypass", state.dnd)
            ReliabilityRow("Calendar selected", state.selectedCalendars)
            FilterChip(
                selected = state.canSchedule,
                onClick = {},
                label = { Text(if (state.canSchedule) "Alarms active" else "Alarms need attention") },
                enabled = false,
            )
        }
    }
}

@Composable
private fun ReliabilityRow(label: String, okay: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Icon(
            imageVector = if (okay) Icons.Default.CheckCircle else Icons.Default.Warning,
            contentDescription = if (okay) "Available" else "Needs attention",
            tint = if (okay) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
        )
        Text(label)
    }
}

@Composable
private fun AlarmCard(alarm: ScheduledAlarm, calendarName: String, onMutedChanged: (Boolean) -> Unit) {
    val start = Instant.ofEpochMilli(alarm.meetingStartUtc).atZone(ZoneId.systemDefault())
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(start.format(timeFormatter), style = MaterialTheme.typography.headlineMedium)
                Switch(
                    checked = !alarm.muted,
                    onCheckedChange = onMutedChanged,
                    modifier = Modifier.semantics { contentDescription = "Alarm enabled" },
                )
            }
            Text(alarm.eventTitle.orEmpty().ifBlank { "Calendar event" }, style = MaterialTheme.typography.titleMedium)
            Text("Alarm: ${Instant.ofEpochMilli(alarm.alarmFireUtc).atZone(ZoneId.systemDefault()).format(timeFormatter)}")
            Text("Calendar: $calendarName", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(if (alarm.muted) "Muted for this event" else "Alarm enabled", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun <T> kotlinx.coroutines.flow.StateFlow<T>.collectAsStateWithLifecycleCompat(): androidx.compose.runtime.State<T> =
    collectAsState()
