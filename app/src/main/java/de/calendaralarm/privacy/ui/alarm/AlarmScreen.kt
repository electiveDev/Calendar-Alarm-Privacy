package de.calendaralarm.privacy.ui.alarm

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun AlarmScreen(title: String, startUtc: Long, onStop: () -> Unit) {
    val time = Instant.ofEpochMilli(startUtc).atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("HH:mm"))
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp, Alignment.CenterVertically),
    ) {
        Text("Calendar alarm", style = MaterialTheme.typography.headlineMedium)
        Text(title, style = MaterialTheme.typography.headlineLarge)
        Text("Starts at $time", style = MaterialTheme.typography.titleLarge)
        Button(
            onClick = onStop,
            modifier = Modifier.fillMaxWidth().heightIn(min = 72.dp).semantics { role = Role.Button },
        ) { Text("STOP", style = MaterialTheme.typography.headlineSmall) }
    }
}
