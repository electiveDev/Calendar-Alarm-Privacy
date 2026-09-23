# Security and privacy self-review

## Permission allowlist

The manifest requests only:

- `READ_CALENDAR` — read Android Calendar Provider data
- `POST_NOTIFICATIONS` — alarm notification on Android 13+
- `SCHEDULE_EXACT_ALARM` — exact user-visible alarm scheduling
- `RECEIVE_BOOT_COMPLETED` — rebuild future alarms after reboot or update
- `WAKE_LOCK` — short receiver-to-service transition wake lock
- `FOREGROUND_SERVICE` and `FOREGROUND_SERVICE_MEDIA_PLAYBACK` — audible alarm service
- `ACCESS_NOTIFICATION_POLICY` — optional Do Not Disturb bypass status
- `USE_FULL_SCREEN_INTENT` — optional prominent lock-screen alarm UI

The app does not request `INTERNET`, `ACCESS_NETWORK_STATE`, `WRITE_CALENDAR`, contacts, location, camera, microphone, SMS, package-query, or external-storage permissions. AndroidX may add an app-local signature-protection permission named `de.calendaralarm.privacy.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION` for framework dynamic-receiver isolation; it grants no external access and is not a network or calendar permission.

## Component exposure

`MainActivity` is exported only because it is the launcher activity. `AlarmActivity`, `AlarmReceiver`, and `AlarmService` are not exported. `BootReceiver` is system-facing and accepts only `BOOT_COMPLETED` and `MY_PACKAGE_REPLACED`; `ExactAlarmPermissionReceiver` accepts only Android's exact-alarm permission-state broadcast.

All app-internal PendingIntents are explicit and immutable, with `FLAG_UPDATE_CURRENT` where the alarm identity requires it. Alarm identity is stored in Room; request codes are allocated without truncating an event-instance ID.

## Runtime behavior

Production alarms use `AlarmManager.setAlarmClock()`, `AudioAttributes.USAGE_ALARM`, and a short-lived media-playback foreground service. The service has a configured maximum duration and a visible Stop action. Calendar synchronization is idempotent and uses `CalendarContract.Instances`; it never writes to the provider.

No runtime code uses HTTP, WebView, sockets, cloud APIs, analytics, crash reporting, or telemetry. Log output does not include calendar titles, descriptions, locations, attendees, accounts, or URLs.

## Final self-review

- AndroidManifest permissions: reviewed; forbidden calendar/network permissions absent.
- Room entity and DataStore: reviewed; only scheduling/settings fields are persisted.
- Calendar projection: reviewed; only the required `Instances` fields are queried.
- Receivers, service, activities, and PendingIntents: reviewed for explicit targets and export state.
- GitHub workflows: reviewed; use official checkout, Java, Gradle setup, and artifact actions only.
- Generated debug manifest and APK: verify with `scripts/privacy-check.sh` after each build.

## Reporting

Please report security issues privately to the repository maintainers before public disclosure. Do not include real calendar data in an issue or log excerpt.
