package de.calendaralarm.privacy.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = Color(0xFF3559A8),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD9E2FF),
    onPrimaryContainer = Color(0xFF001A41),
    secondary = Color(0xFF5A5F71),
    surface = Color(0xFFFAF8FF),
    surfaceVariant = Color(0xFFE2E2EC),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFB0C6FF),
    onPrimary = Color(0xFF002D6C),
    primaryContainer = Color(0xFF1C438F),
    onPrimaryContainer = Color(0xFFD9E2FF),
    secondary = Color(0xFFC2C5D6),
    surface = Color(0xFF121318),
    surfaceVariant = Color(0xFF45464F),
)

@Composable
fun CalendarAlarmTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) DarkColors else LightColors,
        content = content,
    )
}
