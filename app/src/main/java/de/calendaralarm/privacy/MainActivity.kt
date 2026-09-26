package de.calendaralarm.privacy

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.CalendarContract
import android.database.ContentObserver
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import de.calendaralarm.privacy.data.prefs.AppSettings
import de.calendaralarm.privacy.ui.home.HomeScreen
import de.calendaralarm.privacy.ui.home.HomeViewModel
import de.calendaralarm.privacy.ui.onboarding.OnboardingScreen
import de.calendaralarm.privacy.ui.settings.SettingsScreen
import de.calendaralarm.privacy.ui.theme.CalendarAlarmTheme
import de.calendaralarm.privacy.worker.CalendarSyncWorker

class MainActivity : ComponentActivity() {
    private var calendarObserver: ContentObserver? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            CalendarAlarmTheme { CalendarAlarmApp() }
        }
    }

    override fun onResume() {
        super.onResume()
        val app = application as CalendarAlarmApplication
        CalendarSyncWorker.enqueueOneTime(app)
        val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
            private var lastEnqueueMs = 0L

            override fun onChange(selfChange: Boolean) {
                val now = System.currentTimeMillis()
                if (now - lastEnqueueMs >= 1_000L) {
                    lastEnqueueMs = now
                    CalendarSyncWorker.enqueueOneTime(app)
                }
            }
        }
        calendarObserver = observer
        contentResolver.registerContentObserver(CalendarContract.Instances.CONTENT_URI, true, observer)
    }

    override fun onPause() {
        calendarObserver?.let(contentResolver::unregisterContentObserver)
        calendarObserver = null
        super.onPause()
    }
}

@Composable
private fun CalendarAlarmApp() {
    val app = androidx.compose.ui.platform.LocalContext.current.applicationContext as CalendarAlarmApplication
    val settings by app.preferences.settings.collectAsStateWithLifecycle(initialValue = AppSettings())
    var showSettings by rememberSaveable { mutableStateOf(false) }

    if (!settings.onboardingCompleted) {
        OnboardingScreen(app) { showSettings = false }
    } else if (showSettings) {
        SettingsScreen(app) { showSettings = false }
    } else {
        val factory = remember(app) {
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T = HomeViewModel(app) as T
            }
        }
        val homeViewModel: HomeViewModel = viewModel(factory = factory)
        HomeScreen(homeViewModel) { showSettings = true }
    }
}
