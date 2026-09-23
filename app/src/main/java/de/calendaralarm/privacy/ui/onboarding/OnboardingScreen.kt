package de.calendaralarm.privacy.ui.onboarding

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationManager
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
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
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import de.calendaralarm.privacy.CalendarAlarmApplication
import de.calendaralarm.privacy.data.calendar.CalendarInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun OnboardingScreen(app: CalendarAlarmApplication, onComplete: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var step by rememberSaveable { mutableIntStateOf(0) }
    var calendars by remember { mutableStateOf<List<CalendarInfo>>(emptyList()) }
    var selected by remember { mutableStateOf<Set<Long>>(emptySet()) }
    val calendarPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    val notificationPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }

    LaunchedEffect(Unit) {
        val settings = app.preferences.settings.first()
        selected = settings.selectedCalendarIds
        calendars = withContext(Dispatchers.IO) { app.calendarReader.readCalendars() }
    }

    Scaffold { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            Text("Set up reliable calendar alarms", style = MaterialTheme.typography.headlineMedium)
            LinearProgressIndicator(progress = { (step + 1) / 7f }, modifier = Modifier.fillMaxWidth())
            when (step) {
                0 -> PermissionStep(
                    title = "Calendar access",
                    body = "Calendar Alarm Privacy reads events stored on this device so it can create alarms. Calendar access is read-only. Nothing is uploaded.",
                    button = "Allow calendar access",
                    onAction = { calendarPermission.launch(Manifest.permission.READ_CALENDAR); step = 1 },
                )
                1 -> PermissionStep(
                    title = "Notifications",
                    body = "Alarm notifications accompany the audible alarm and remain visible on the lock screen.",
                    button = "Allow notifications",
                    onAction = {
                        if (Build.VERSION.SDK_INT >= 33) notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                        step = 2
                    },
                )
                2 -> PermissionStep(
                    title = "Exact alarms",
                    body = "Android requires special access for alarms that must fire at an exact time.",
                    button = "Allow alarms & reminders",
                    onAction = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                            context.startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:${context.packageName}")))
                        }
                        step = 3
                    },
                )
                3 -> PermissionStep(
                    title = "Full-screen alarm",
                    body = "This lets the alarm screen appear prominently when the screen is locked. It is optional; alarms still sound without it.",
                    button = "Open full-screen settings",
                    secondary = "Continue without it",
                    onAction = {
                        if (Build.VERSION.SDK_INT >= 34) context.startActivity(Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT, Uri.parse("package:${context.packageName}")))
                        step = 4
                    },
                    onSecondary = { step = 4 },
                )
                4 -> PermissionStep(
                    title = "Do Not Disturb",
                    body = "Optional but recommended if alarms should remain audible during Do Not Disturb.",
                    button = "Open DND access",
                    secondary = "Continue without it",
                    onAction = { context.startActivity(Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)); step = 5 },
                    onSecondary = { step = 5 },
                )
                5 -> CalendarSelectionStep(calendars, selected) { selected = it }
                else -> TestAlarmStep(
                    selectedCount = selected.size,
                    onTest = { scope.launch { app.alarmScheduler.scheduleTest(app.preferences.settings.first()) } },
                    onFinish = {
                        scope.launch {
                            app.preferences.setSelectedCalendarIds(selected)
                            app.preferences.setOnboardingCompleted(true)
                            onComplete()
                        }
                    },
                )
            }
            if (step == 5) {
                Button(
                    onClick = { step = 6 },
                    enabled = selected.isNotEmpty(),
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("Continue") }
            }
        }
    }
}

@Composable
private fun PermissionStep(
    title: String,
    body: String,
    button: String,
    onAction: () -> Unit,
    secondary: String? = null,
    onSecondary: (() -> Unit)? = null,
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(title, style = MaterialTheme.typography.headlineSmall)
        Text(body, style = MaterialTheme.typography.bodyLarge)
        Button(onClick = onAction, modifier = Modifier.fillMaxWidth()) { Text(button) }
        if (secondary != null && onSecondary != null) {
            OutlinedButton(onClick = onSecondary, modifier = Modifier.fillMaxWidth()) { Text(secondary) }
        }
    }
}

@Composable
private fun CalendarSelectionStep(calendars: List<CalendarInfo>, selected: Set<Long>, onChanged: (Set<Long>) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Choose calendars", style = MaterialTheme.typography.headlineSmall)
        Text("Only selected calendars are used to schedule alarms. Calendar data remains on this device.")
        if (calendars.isEmpty()) {
            Card { Text("No visible calendars were found. Grant calendar access and return here.", Modifier.padding(16.dp)) }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                items(calendars, key = { it.id }) { calendar ->
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = calendar.id in selected,
                            onCheckedChange = { checked ->
                                onChanged(if (checked) selected + calendar.id else selected - calendar.id)
                            },
                        )
                        Column {
                            Text(calendar.displayName)
                            calendar.accountLabel?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TestAlarmStep(selectedCount: Int, onTest: () -> Unit, onFinish: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("Ready", style = MaterialTheme.typography.headlineSmall)
        Text("Selected calendars: $selectedCount. You can test the complete alarm path now or finish setup and test it later in Settings.")
        OutlinedButton(onClick = { onTest() }, modifier = Modifier.fillMaxWidth()) { Text("Test alarm in 10 seconds") }
        Button(onClick = onFinish, modifier = Modifier.fillMaxWidth()) { Text("Finish setup") }
    }
}
