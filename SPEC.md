# Calendar Alarm Privacy — Complete Implementation Specification

## 0. Agent Instructions

You are implementing a complete Android application, not producing an example, prototype, pseudocode, tutorial, or partial scaffold.

Your task is to:

1. Create the complete Android Studio / Gradle project described by this specification.
2. Implement every required feature.
3. Build the project locally in the development environment.
4. Run all available unit tests, lint checks, and build checks.
5. Fix build errors and obvious implementation defects before completion.
6. Perform the security/privacy review defined in this specification.
7. Commit all required source files to the Git repository.
8. Push the finished implementation to the configured GitHub repository/branch.
9. Include a GitHub Actions workflow that automatically builds an installable Android APK.
10. Do not stop after scaffolding.
11. Do not leave TODOs for required functionality.
12. Do not require the user to manually copy source files into Android Studio.
13. Do not add features, permissions, dependencies, network access, SDKs, or services that are not explicitly permitted by this specification.

When implementation choices are ambiguous:

- prefer Android platform APIs;
- prefer simpler code;
- prefer fewer permissions;
- prefer fewer dependencies;
- prefer local/offline operation;
- prefer reliability over visual complexity.

Android Developers documentation is the authoritative source when platform behavior is unclear.

The application must compile successfully before the task is considered complete.

---

# 1. Product

Application name:

**Calendar Alarm Privacy**

Suggested package/application ID:

`de.calendaralarm.privacy`

Do not use:

`com.example.*`

The application is a small privacy-focused Android utility that reads calendars already synchronized to Android and automatically converts selected calendar events into reliable audible Android alarms.

It must NOT connect directly to Google Calendar, Outlook, Microsoft, Google APIs, or any other cloud service.

Calendar synchronization remains the responsibility of Android and the user's existing calendar provider.

Architecture:

```text
Google Calendar / Outlook / other account
                |
                v
Android Calendar Provider
                |
                v
Calendar Alarm Privacy
                |
                v
AlarmManager.setAlarmClock()
                |
                v
Android alarm
```

There is no server component.

There is no cloud component.

There is no account system.

There is no network communication.

---

# 2. Primary User Goal

The user currently misses ordinary Google Calendar notifications and task notifications.

The purpose of this application is therefore NOT to provide another ordinary notification.

The purpose is to turn selected calendar events automatically into real, audible, high-priority Android alarms.

The user must continue maintaining appointments exclusively in their normal calendar.

They must NOT need to duplicate calendar events inside this application.

Example:

```text
Google Calendar event:
Filter wechseln
12 October 2026
10:00

Calendar Alarm Privacy:
automatically detects event
        ↓
schedules Android alarm
        ↓
10:00 alarm rings
```

Changing or deleting the calendar event must automatically change or delete the corresponding alarm.

---

# 3. Design Priorities

Priorities, in this order:

1. Alarm reliability
2. Privacy
3. Correct synchronization with calendar changes
4. Simplicity
5. Battery efficiency
6. UI appearance

A beautiful UI must never be implemented at the cost of alarm reliability.

---

# 4. Strict Privacy Requirements

These requirements are NON-NEGOTIABLE.

## 4.1 Completely offline

The app MUST NOT request:

```xml
android.permission.INTERNET
```

The app MUST NOT request:

```xml
android.permission.ACCESS_NETWORK_STATE
```

The app MUST NOT perform any network operation.

There must be no HTTP client.

There must be no WebSocket client.

There must be no WebView used for remote content.

There must be no online API.

---

## 4.2 Forbidden SDKs and libraries

DO NOT include:

- Firebase
- Firebase Analytics
- Google Analytics
- Crashlytics
- Sentry
- Bugsnag
- AppCenter
- advertising SDKs
- telemetry SDKs
- tracking SDKs
- remote configuration systems
- Retrofit
- OkHttp
- Ktor networking
- Volley
- Apollo
- WebSocket libraries
- cloud databases
- authentication SDKs

Normal AndroidX / Jetpack dependencies are allowed.

---

## 4.3 Calendar access

The app MUST request:

```xml
android.permission.READ_CALENDAR
```

The app MUST NOT request:

```xml
android.permission.WRITE_CALENDAR
```

The application must never modify, create, delete, accept, decline, or otherwise alter calendar events.

Calendar access is strictly read-only.

---

## 4.4 Android backup

The application must use:

```xml
android:allowBackup="false"
```

No application database, settings, diagnostic files, or calendar-derived information may be uploaded through Android Auto Backup.

If modern Android data extraction rules are needed, configure them consistently with the backup prohibition.

---

## 4.5 Stored information

Store only information required to:

- identify an event instance;
- know when its alarm should fire;
- cancel/reschedule the corresponding PendingIntent;
- track whether an alarm is currently active/muted/dismissed.

DO NOT persist:

- event description
- attendee list
- attendee email addresses
- organizer
- account email addresses
- conferencing URLs
- calendar notes
- attachments
- event location unless required by a future explicit feature
- arbitrary calendar metadata

Event titles should preferably NOT be permanently persisted.

If a title must temporarily be persisted for reliable display when an alarm fires, document the reason in code comments and store nothing beyond the title.

Do not store descriptions or other sensitive event content.

---

# 5. Logging Privacy

Logging must never expose personal calendar content.

Never write the following to Logcat or files:

- event title
- event description
- event location
- attendees
- account names
- email addresses
- meeting URLs

Allowed diagnostic data:

```text
event instance ID
event ID
calendar numeric ID
request code
scheduled UTC timestamp
fired UTC timestamp
alarm state
error type
exception class
```

Debug builds may contain lifecycle logging using only these fields.

Release builds should minimize diagnostic logging.

Do not implement persistent diagnostic files unless necessary.

---

# 6. Supported Android Versions

Language:

**Kotlin**

UI:

**Jetpack Compose**

Minimum Android:

**Android 10 / API 29**

Target:

Use the latest stable Android SDK available in the installed build environment.

Prefer:

```text
targetSdk 36
compileSdk 36
```

if the available Android Gradle Plugin/toolchain supports them.

Do not lower compatibility unnecessarily just to avoid modern Android permission requirements.

Java/Kotlin toolchain:

Prefer Java 17 unless the current Android Gradle Plugin strongly requires another supported version.

---

# 7. Required Dependencies

Prefer only:

- AndroidX Core KTX
- Activity Compose
- Compose BOM
- Compose UI
- Material 3
- Lifecycle
- ViewModel
- Navigation Compose
- Room
- Room KTX
- KSP
- DataStore Preferences
- WorkManager KTX
- JUnit
- AndroidX test libraries

Do not add dependency injection frameworks unless clearly necessary.

Use a simple application-level dependency container or constructors.

Do not use Hilt/Dagger solely for this small project.

---

# 8. Calendar Source

The application reads Android's local Calendar Provider.

Supported calendars therefore automatically include accounts synchronized into Android, such as:

- Google Calendar
- Outlook
- Exchange
- local calendars
- other providers exposing calendars through CalendarContract

Do not implement provider-specific APIs.

---

# 9. Calendar Query

Use:

```kotlin
CalendarContract.Instances
```

NOT direct `CalendarContract.Events` queries for upcoming alarms.

Reason:

`Instances` correctly expands recurring events.

Query approximately:

```text
now → now + 7 days
```

Required fields:

```text
Instances._ID
Instances.EVENT_ID
Instances.TITLE
Instances.BEGIN
Instances.END
Instances.ALL_DAY
Instances.SELF_ATTENDEE_STATUS
Instances.CALENDAR_ID
Instances.CALENDAR_COLOR
Instances.CALENDAR_DISPLAY_NAME
```

Do not query unnecessary fields.

In particular, do not retrieve:

- descriptions
- attendee email addresses
- organizer
- conferencing metadata

---

# 10. Calendar Selection

On first setup, show all visible Android calendars.

For each calendar show:

- display name
- account/provider label if Android exposes one and showing it is useful
- calendar color

Allow multi-selection.

The selected calendar IDs are saved locally using DataStore.

No calendar is uploaded anywhere.

---

# 11. Event Filtering

Global settings:

### Skip all-day events

Default:

**ON**

### Skip declined events

Default:

**ON**

### Ignore past events

Always enabled.

### Ignore events whose calculated alarm time already passed

Yes.

Do not schedule alarms retroactively.

---

# 12. Alarm Lead Time

Global configurable lead time.

Allowed values:

```text
0–120 minutes
```

Default:

```text
0 minutes
```

The default of zero is intentional.

For the primary use case, an appointment scheduled for 10:00 should alarm at 10:00.

Users may optionally configure e.g.:

```text
1 minute before
5 minutes before
10 minutes before
```

Alarm time:

```text
event BEGIN - configured lead time
```

---

# 13. Alarm Scheduling

Use Android:

```kotlin
AlarmManager.setAlarmClock()
```

This is a core design requirement.

Do NOT replace the production alarm scheduler with:

```text
WorkManager delays
Handler
Timer
ScheduledExecutor
set()
setWindow()
setExact()
setExactAndAllowWhileIdle()
```

`setAlarmClock()` is specifically selected because calendar alarms are user-visible, time-critical alarms.

WorkManager is only responsible for synchronization/rescheduling work.

---

# 14. Exact Alarm Permission

Use:

```xml
android.permission.SCHEDULE_EXACT_ALARM
```

Do NOT simultaneously declare:

```xml
android.permission.USE_EXACT_ALARM
```

Before scheduling exact alarms on supported Android versions, call:

```kotlin
AlarmManager.canScheduleExactAlarms()
```

If access has not been granted:

- clearly explain why it is required;
- provide a button opening:

```kotlin
Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM
```

The app must detect when the permission becomes available.

Implement handling for:

```text
AlarmManager.ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED
```

When exact-alarm access is granted/re-granted:

1. verify `canScheduleExactAlarms()`;
2. immediately resynchronize upcoming events;
3. recreate required alarms.

If permission is unavailable, do not crash.

Display a clear reliability warning.

---

# 15. Alarm Trigger PendingIntent

Use an explicit `PendingIntent` targeting an internal `BroadcastReceiver`.

PendingIntents must use:

```kotlin
PendingIntent.FLAG_IMMUTABLE
```

plus:

```kotlin
PendingIntent.FLAG_UPDATE_CURRENT
```

when appropriate.

Do not use implicit intents.

Request codes must be stable and unique enough to allow reliable cancellation/rescheduling.

Avoid collisions.

---

# 16. Alarm Receiver

Implement:

```text
AlarmReceiver
```

Manifest registered.

Set:

```xml
android:exported="false"
```

Responsibilities:

1. receive the scheduled alarm;
2. acquire only a short wake lock if necessary;
3. launch the alarm foreground service;
4. pass only the necessary internal identifiers;
5. return promptly.

Do not perform database-heavy work directly inside `onReceive()`.

Use `goAsync()` only if absolutely necessary and correctly finalized.

---

# 17. Alarm Foreground Service

Implement:

```text
AlarmService
```

Set:

```xml
android:exported="false"
```

Declare:

```xml
android:foregroundServiceType="mediaPlayback"
```

Required permissions:

```xml
android.permission.FOREGROUND_SERVICE
android.permission.FOREGROUND_SERVICE_MEDIA_PLAYBACK
```

When started because of an exact alarm:

1. immediately call `startForeground()`;
2. create/show the alarm notification;
3. begin alarm audio;
4. optionally launch/show the full-screen alarm UI;
5. automatically stop after configured maximum duration.

Never leave an orphaned foreground service running.

---

# 18. Alarm Audio

Alarm audio must use Android alarm audio semantics.

Use:

```kotlin
AudioAttributes.USAGE_ALARM
```

with:

```kotlin
AudioAttributes.CONTENT_TYPE_SONIFICATION
```

Use the user's selected alarm sound.

Default:

```kotlin
RingtoneManager.TYPE_ALARM
```

Alarm audio should loop until:

- user presses Stop;
- configured maximum duration expires.

Default maximum duration:

```text
60 seconds
```

Allowed range:

```text
15 seconds – 10 minutes
```

Audio must use the Android alarm stream/usage rather than media volume.

---

# 19. Do Not Disturb

Request:

```xml
android.permission.ACCESS_NOTIFICATION_POLICY
```

DND bypass is optional from a functional-state perspective but strongly recommended for reliability.

The app must clearly show whether Notification Policy Access has been granted.

If granted, configure the alarm notification channel appropriately for alarm behavior and DND bypass.

If not granted:

- alarms remain enabled;
- show a warning that DND may prevent or alter audible behavior;
- provide a button to the appropriate Android settings page.

Do NOT completely disable all alarms merely because DND access is absent.

---

# 20. Notifications

Android 13+ requires:

```xml
android.permission.POST_NOTIFICATIONS
```

Request it during onboarding.

Create a dedicated alarm notification channel.

Properties:

```text
IMPORTANCE_HIGH
CATEGORY_ALARM
vibration enabled
alarm AudioAttributes
lock-screen visible
```

Use an understandable channel name:

```text
Calendar alarms
```

Do not create unnecessary notification channels.

---

# 21. Full-Screen Alarm

The application acts as an alarm application.

Declare:

```xml
android.permission.USE_FULL_SCREEN_INTENT
```

On Android versions supporting:

```kotlin
NotificationManager.canUseFullScreenIntent()
```

check whether Full-Screen Intent access is available.

If unavailable:

- alarms must still sound;
- display a high-priority heads-up notification;
- show a Reliability warning;
- provide the appropriate settings button using the supported Android Settings intent.

Do not crash if Full-Screen Intent permission/access is denied.

Full-screen behavior is an enhancement to alarm visibility, not a condition for scheduling alarms.

---

# 22. Alarm Activity

Implement:

```text
AlarmActivity
```

It may be displayed above the lock screen using the currently supported Android APIs.

Show:

- app/alarm icon
- `Calendar alarm`
- event title if available
- event start time
- large Stop button

Optional:

```text
Starts now
Starts in 5 minutes
```

depending on configured lead time.

Do not show sensitive event descriptions.

The Stop button must:

1. immediately stop sound;
2. stop foreground service;
3. update local alarm state;
4. close AlarmActivity.

---

# 23. Snooze

Version 1:

**Do not implement snooze.**

Keep behavior simple.

User gets:

```text
STOP
```

only.

This reduces scheduling complexity and error potential.

---

# 24. One-Event Mute

Home screen should provide:

```text
Mute alarm for this event
```

for upcoming instances.

Muting an event:

- cancels the corresponding AlarmManager alarm;
- records the event instance as muted locally;
- must survive normal resynchronization;
- must NOT alter the source calendar.

If the underlying calendar event changes substantially, preserve mute only when it can be unambiguously associated with the same event instance.

Keep implementation conservative to avoid accidentally suppressing unrelated future recurring events.

---

# 25. Database

Use Room.

Recommended entity:

```text
ScheduledAlarm
```

Fields:

```text
id: Long primary key autoGenerate
eventInstanceId: Long
eventId: Long
calendarId: Long
meetingStartUtc: Long
alarmFireUtc: Long
requestCode: Int
state: AlarmState
muted: Boolean
```

Optional:

```text
eventTitle: String?
```

ONLY if required so the alarm screen can display the title reliably after the calendar provider becomes temporarily unavailable.

Do not store other calendar content.

Add useful unique indexes for:

```text
eventInstanceId
requestCode
```

Alarm states:

```text
SCHEDULED
FIRED
DISMISSED
CANCELLED
MUTED
```

---

# 26. Settings Storage

Use DataStore Preferences.

Settings:

```text
selectedCalendarIds
leadTimeMinutes
skipAllDay
skipDeclined
alarmSoundUri
maxAlarmDurationSeconds
onboardingCompleted
```

Do not store account credentials.

---

# 27. Synchronization Strategy

Perform synchronization:

1. when application opens;
2. when application resumes;
3. periodically using WorkManager;
4. after the user changes settings affecting alarms;
5. after exact alarm permission becomes available;
6. after boot;
7. after app/package replacement/update.

Periodic WorkManager interval:

```text
15 minutes
```

which is Android's normal minimum periodic interval.

Query a rolling seven-day event window.

---

# 28. Foreground Calendar Observer

While the application is running in the foreground, register a:

```kotlin
ContentObserver
```

for relevant Calendar Provider changes.

When calendar data changes:

- debounce rapid callbacks;
- trigger one synchronization;
- avoid repeated expensive rescheduling.

Unregister the observer appropriately.

Do not rely exclusively on the ContentObserver because it is not guaranteed to keep the app alive in the background.

---

# 29. Diff Synchronization

Synchronization must be idempotent.

Algorithm:

```text
read current eligible calendar event instances
        ↓
calculate desired alarms
        ↓
read locally tracked scheduled alarms
        ↓
compare desired vs current
```

For each desired event:

### New event
Schedule alarm.

### Unchanged event
Do nothing.

### Event moved
Cancel old PendingIntent.
Schedule new alarm.

### Event deleted
Cancel PendingIntent.
Mark/remove local entry.

### Event muted
Do not reschedule it accidentally.

### Alarm time already in the past
Do not schedule.

Avoid cancelling/recreating every alarm unnecessarily.

---

# 30. Reboot Handling

Declare:

```xml
android.permission.RECEIVE_BOOT_COMPLETED
```

Implement:

```text
BootReceiver
```

Listen for:

```text
android.intent.action.BOOT_COMPLETED
android.intent.action.MY_PACKAGE_REPLACED
```

Do not unnecessarily use `LOCKED_BOOT_COMPLETED` unless all required storage and Calendar Provider behavior have been explicitly implemented for Direct Boot.

After normal boot:

- enqueue a one-time synchronization;
- query Android Calendar Provider;
- recreate future alarms.

The boot receiver itself must not start alarm audio.

---

# 31. App Update Handling

After:

```text
MY_PACKAGE_REPLACED
```

resynchronize upcoming alarms.

This ensures PendingIntents and database state remain valid after installing a newer APK.

---

# 32. WorkManager

Use a unique periodic work name.

Example:

```text
calendar_alarm_periodic_sync
```

Use:

```text
ExistingPeriodicWorkPolicy.UPDATE
```

or the current appropriate API so duplicate periodic workers are never created.

For one-shot synchronization use unique work when practical.

Do not enqueue hundreds of redundant synchronization jobs.

---

# 33. Battery Optimization

Do NOT require blanket battery-optimization exemption as an unconditional prerequisite.

The core alarm uses:

```text
AlarmManager.setAlarmClock()
```

Battery optimization exemption may be exposed as an OPTIONAL troubleshooting/reliability recommendation for OEM devices that behave incorrectly.

Do not request:

```xml
REQUEST_IGNORE_BATTERY_OPTIMIZATIONS
```

unless implementation can demonstrate that it is actually required.

Prefer not to request it in version 1.

The Reliability page may explain OEM battery restrictions without forcing exemption.

---

# 34. Wake Lock

If required during receiver → service transition, request:

```xml
android.permission.WAKE_LOCK
```

Use only a short, timeout-bound partial wake lock.

Never hold a long-lived wake lock while the alarm is not actively firing.

Release it safely using `try/finally`.

---

# 35. Onboarding

First launch should provide a short guided onboarding.

No account creation.

No welcome marketing screens.

Steps:

## Step 1 — Calendar

Explain:

```text
Calendar Alarm Privacy reads calendar events stored on this device so it can create alarms.

Calendar access is read-only.
Nothing is uploaded.
```

Request:

```text
READ_CALENDAR
```

---

## Step 2 — Notifications

Android 13+:

Request notification permission.

Explain that alarm notifications accompany audible alarms.

---

## Step 3 — Exact alarms

Check:

```text
canScheduleExactAlarms()
```

If unavailable, explain:

```text
Android requires special permission for alarms that must fire at an exact time.
```

Button:

```text
Allow alarms & reminders
```

---

## Step 4 — Full-screen alarm

Where relevant, verify Full-Screen Intent access.

Explain:

```text
Allows the alarm screen to appear prominently when the screen is locked.
```

Do not make this permission mandatory for basic alarm scheduling.

---

## Step 5 — DND access

Explain:

```text
Optional but recommended if alarms should remain audible during Do Not Disturb.
```

Allow user to continue without it.

---

## Step 6 — Calendar selection

Multi-select calendars.

At least one calendar must be selected before alarms can be enabled.

---

## Step 7 — Test alarm

Offer:

```text
Test alarm in 10 seconds
```

This test MUST use essentially the same production alarm path:

```text
AlarmManager
→ AlarmReceiver
→ AlarmService
→ audio
→ alarm notification/activity
```

Do not implement the test as a simple immediate MediaPlayer sound.

---

# 36. Reliability Status

Home screen must contain a compact status card.

Example:

```text
Alarm reliability

✓ Calendar access
✓ Notifications
✓ Exact alarm access
✓ Full-screen alarm
✓ DND bypass

Alarms active
```

Possible warnings:

```text
⚠ DND access not granted
⚠ Full-screen alarm access not granted
⚠ Exact alarm access missing
⚠ Notification permission missing
```

Critical conditions:

- no READ_CALENDAR
- no selected calendars
- no exact alarm access

These conditions prevent scheduling.

Non-critical warnings:

- no DND bypass
- no Full-Screen Intent

These must NOT prevent scheduling.

---

# 37. Home Screen

Keep interface simple.

Top:

```text
Calendar Alarm Privacy
```

Reliability card.

Then:

```text
Upcoming alarms
```

Show upcoming approximately 24–48 hours.

For each entry:

```text
10:00
Filter wechseln

Alarm: 10:00
Calendar: Personal

[Alarm enabled toggle]
```

Do not show event descriptions.

Do not show attendee lists.

If there are no upcoming events:

```text
No upcoming alarms.
```

Pull-to-refresh is optional.

A manual:

```text
Refresh
```

button is acceptable.

---

# 38. Settings Screen

Settings:

## Calendars
Multi-select.

## Alarm timing

```text
Alarm before event:
0 minutes
```

Simple number selector.

Range:

```text
0–120
```

## Event filters

```text
Skip all-day events ON
Skip declined events ON
```

## Alarm sound

Use Android ringtone picker.

Restrict/prefer alarm-capable sounds.

Default Android alarm sound.

## Maximum ringing time

Default:

```text
60 seconds
```

## Test alarm

Button:

```text
Test alarm in 10 seconds
```

## Privacy information

Show:

```text
Calendar Alarm Privacy works entirely on this device.

The app has no Internet permission and cannot upload calendar data.

Calendar access is read-only.
```

---

# 39. App Theme

Use Material 3.

Support:

- system light theme
- system dark theme

No custom design framework.

No external fonts.

No downloaded graphics.

Prefer built-in Material icons where appropriate.

Keep UI clean and functional.

---

# 40. Accessibility

Buttons must have appropriate labels.

Alarm Stop button must be:

- large;
- high contrast;
- accessible through TalkBack.

Avoid tiny touch targets.

---

# 41. Alarm Request Codes

Do not rely naïvely on:

```kotlin
eventInstanceId.toInt()
```

because truncation/collision is possible.

Implement a deterministic/stored request-code strategy with uniqueness guaranteed by the Room table.

Request codes must remain stable for an already scheduled alarm so it can later be cancelled.

---

# 42. PendingIntent Security

All app-internal PendingIntents should be explicit.

Use:

```text
FLAG_IMMUTABLE
```

unless mutable behavior is demonstrably required.

Do not expose components unnecessarily.

---

# 43. Exported Components

Default:

```xml
android:exported="false"
```

for:

- AlarmActivity
- AlarmReceiver
- AlarmService

Main launcher activity:

```xml
android:exported="true"
```

because Android requires it for launcher intent filters.

Boot receiver may need to be exported according to Android broadcast behavior, but restrict it to only the necessary system actions.

Do not expose arbitrary services or receivers.

---

# 44. Manifest Permission Allowlist

The final app SHOULD need only these permissions:

```text
READ_CALENDAR
POST_NOTIFICATIONS
SCHEDULE_EXACT_ALARM
RECEIVE_BOOT_COMPLETED
WAKE_LOCK
FOREGROUND_SERVICE
FOREGROUND_SERVICE_MEDIA_PLAYBACK
ACCESS_NOTIFICATION_POLICY
USE_FULL_SCREEN_INTENT
```

Some are version-specific/runtime-specific.

Any additional manifest permission must be justified in source comments and in `SECURITY.md`.

Explicitly forbidden:

```text
INTERNET
ACCESS_NETWORK_STATE
WRITE_CALENDAR
READ_CONTACTS
WRITE_CONTACTS
ACCESS_FINE_LOCATION
ACCESS_COARSE_LOCATION
CAMERA
RECORD_AUDIO
READ_SMS
SEND_SMS
QUERY_ALL_PACKAGES
MANAGE_EXTERNAL_STORAGE
```

Do not add storage/media permissions.

---

# 45. Local Security

Room database and DataStore remain in internal application storage.

Do not deliberately place data into:

- shared external storage;
- Downloads;
- public files directories.

No exported content provider.

No custom IPC interface.

---

# 46. Failure Handling

The app must remain stable when:

- Calendar permission revoked
- notification permission revoked
- exact alarm permission revoked
- DND access revoked
- Full-Screen Intent denied
- selected calendar removed
- alarm ringtone becomes unavailable
- event removed before firing
- Calendar Provider temporarily returns no data
- device reboots
- app is upgraded
- duplicate synchronization occurs

Never crash because a permission disappeared.

Surface useful status in the Reliability card.

---

# 47. Alarm Sound Fallback

If the selected alarm URI is unavailable:

1. try default Android alarm URI;
2. if unavailable, try default notification/ringtone suitable as fallback;
3. never silently fail without notifying the user.

Record only non-sensitive diagnostic information.

---

# 48. Alarm Collision Handling

Two calendar events may have the same alarm time.

Each must have its own unique alarm identity.

If both fire at exactly the same moment, avoid:

- multiple competing MediaPlayer instances;
- services fighting over foreground notification ID;
- an alarm becoming impossible to stop.

Implement predictable behavior.

A simple acceptable policy:

```text
If an alarm is already ringing and another alarm fires,
merge/display the additional event in the active alarm session,
or queue it immediately after dismissal.
```

At minimum, ensure the second alarm is not silently lost.

Document the implemented behavior.

---

# 49. Alarm Notification IDs

Do not use one hard-coded notification ID if this would cause simultaneous alarm events to incorrectly overwrite each other.

Use safe deterministic IDs or a managed active-alarm design.

---

# 50. Alarm Cancellation

When an event is:

- deleted;
- moved;
- muted;
- filtered out;
- moved to an unselected calendar;

the previously scheduled Android alarm must be cancelled.

Use the exact same PendingIntent identity/request code used when scheduling.

---

# 51. Test Alarm

The Test Alarm feature must:

1. schedule for approximately 10 seconds in the future;
2. use `AlarmManager.setAlarmClock()`;
3. use the normal receiver;
4. use the normal foreground service;
5. use the normal alarm sound;
6. show the normal alarm UI.

After completion, clean up its database/tracking state.

---

# 52. Unit Tests

Implement useful JVM/unit tests for logic that does not require Android framework execution.

At minimum test:

### Alarm time calculation

```text
event 10:00
lead 0
→ alarm 10:00
```

```text
event 10:00
lead 5
→ alarm 09:55
```

### Diff logic

- new event → schedule
- unchanged → no action
- moved event → cancel + reschedule
- deleted → cancel
- muted → no schedule
- filtered event → cancel/no schedule

### Request-code allocator

Must not return an existing active request code.

### Filtering

- all-day filtering
- declined event filtering
- selected calendar filtering

---

# 53. Android Tests

Where practical, add instrumented tests for:

- Room DAO;
- DataStore/settings;
- calendar-query mapping using testable abstraction;
- notification/channel creation logic if feasible.

Do not make tests brittle solely to achieve arbitrary test count.

---

# 54. Real-Device Test Checklist

Add `TESTING.md`.

Document manual test cases:

## A

```text
Screen on
ringer normal
event in 2 minutes
```

Expected: audible alarm.

## B

```text
Screen off
phone silent
```

Expected: audible alarm using alarm stream.

## C

```text
DND enabled
notification policy access granted
```

Expected: audible alarm.

## D

```text
app swiped away from recent apps
```

Expected: already scheduled alarm still fires.

Important:

Do not claim Android guarantees behavior after the user explicitly Force Stops the application in Android Settings.

Android force-stop is a special application state and can suppress broadcasts/alarms until the app is launched again.

Document this accurately.

## E

```text
device idle / Doze
```

Expected: `setAlarmClock()` fires at the expected time.

## F

```text
event moved
```

Expected: old alarm cancelled; new alarm created after synchronization.

## G

```text
event deleted
```

Expected: alarm cancelled after synchronization.

## H

```text
reboot
```

Expected: future alarms rebuilt after boot/calendar availability.

## I

```text
two events at same start time
```

Expected: neither event is silently lost.

---

# 55. Documentation

Create:

```text
README.md
SPEC.md
SECURITY.md
PRIVACY.md
TESTING.md
LICENSE
```

---

# 56. README.md

README must explain:

- what the app does;
- Android 10+ requirement;
- completely offline design;
- no Internet permission;
- read-only calendar access;
- installation from APK;
- first-run permissions;
- Test Alarm;
- build instructions;
- where GitHub Actions APK artifacts are located.

Keep it concise.

---

# 57. PRIVACY.md

State clearly:

```text
Calendar Alarm Privacy processes calendar information locally on the Android device.

The application does not request Internet access and contains no code intended to communicate with external servers.

Calendar access is read-only.

No analytics, advertising, telemetry, cloud synchronization or tracking SDK is included.

Android application backup is disabled.
```

List locally stored categories accurately.

Do not make claims not supported by implementation.

---

# 58. SECURITY.md

Document:

- permission allowlist;
- no INTERNET permission;
- no WRITE_CALENDAR permission;
- exported Android components;
- PendingIntent immutability policy;
- local data storage;
- dependency policy;
- how to report security issues.

Also include the final security self-review results.

---

# 59. License

Use:

**GPL-3.0-only**

Include full `LICENSE`.

Add SPDX headers where reasonable, but do not clutter every generated resource file unnecessarily.

---

# 60. Gradle Reproducibility

Commit:

```text
gradlew
gradlew.bat
gradle/wrapper/*
settings.gradle.kts
build.gradle.kts
gradle.properties
gradle/libs.versions.toml
```

Do not commit:

```text
.gradle/
build/
app/build/
local.properties
*.jks
*.keystore
```

Provide a proper `.gitignore`.

---

# 61. Dependency Security

Use current stable versions compatible with the selected Android Gradle Plugin.

Do not use:

- snapshots;
- random JitPack dependencies;
- abandoned third-party convenience libraries.

Prefer:

```text
Google Maven
Maven Central
Gradle Plugin Portal
```

only.

---

# 62. GitHub Actions APK Build

Create:

```text
.github/workflows/android.yml
```

Use only trusted official GitHub actions where practical.

Workflow should trigger on:

```yaml
push:
  branches:
    - main

pull_request:

workflow_dispatch:
```

Required steps:

1. checkout repository;
2. install supported Java;
3. configure Gradle;
4. run tests;
5. run lint where practical;
6. build Debug APK;
7. upload APK as GitHub Actions artifact.

Expected Gradle commands should include approximately:

```bash
./gradlew test
./gradlew lintDebug
./gradlew assembleDebug
```

Artifact should contain:

```text
app/build/outputs/apk/debug/app-debug.apk
```

Give the artifact a recognizable name:

```text
CalendarAlarmPrivacy-APK
```

Do not require signing secrets for the Debug APK.

The Debug APK must be directly installable for testing/sideloading.

---

# 63. Optional GitHub Release Build

Also create:

```text
.github/workflows/release.yml
```

Trigger only on Git tags matching:

```text
v*
```

Build the project and upload the resulting APK as an Actions artifact.

Do NOT invent or commit private signing keys.

Do NOT embed credentials.

A properly signed production release can be added later.

For immediate personal testing, the Debug APK is sufficient.

Avoid untrusted third-party release actions unless clearly necessary.

---

# 64. CI Security Check

Add a build/check script or CI step that fails if the merged manifest contains:

```text
android.permission.INTERNET
android.permission.WRITE_CALENDAR
```

Also check the source/build configuration for obvious forbidden dependencies:

```text
firebase
analytics
crashlytics
sentry
okhttp
retrofit
ktor-client
volley
```

A simple repository/manifest verification script is acceptable.

Store it for example as:

```text
scripts/privacy-check.sh
```

The CI workflow must execute it.

---

# 65. Security Self-Review Before Completion

Before final commit, inspect at minimum:

```text
AndroidManifest.xml
merged debug manifest if available
app/build.gradle.kts
gradle/libs.versions.toml
all BroadcastReceivers
all Services
all Activities
all PendingIntent creation
Room entities
DataStore settings
calendar query projection
logging calls
GitHub workflow
```

Search the repository for:

```text
http://
https://
INTERNET
WRITE_CALENDAR
WebView
Socket
URL(
HttpURLConnection
OkHttp
Retrofit
Firebase
Analytics
Crashlytics
Sentry
```

URLs appearing solely in documentation, XML schemas, Gradle repositories, or Android documentation references are acceptable.

Application runtime code must contain no network functionality.

---

# 66. Required Build Verification

Before pushing final implementation run:

```bash
./gradlew clean
./gradlew test
./gradlew lintDebug
./gradlew assembleDebug
```

If lint contains non-critical known Android warnings that cannot reasonably be resolved, document them.

BUILD FAILURE IS NOT ACCEPTABLE.

Verify APK exists:

```text
app/build/outputs/apk/debug/app-debug.apk
```

---

# 67. APK Manifest Verification

After building, inspect the generated APK or merged manifest.

Confirm it does NOT contain:

```text
android.permission.INTERNET
android.permission.WRITE_CALENDAR
```

Confirm only expected permissions are present.

If a dependency injects an unexpected permission:

1. determine why;
2. remove the dependency or explicitly remove the permission via manifest merger if safe;
3. rebuild;
4. re-check.

Do not merely document unexpected sensitive permissions.

---

# 68. Required Core Files

The implementation will likely resemble:

```text
app/src/main/java/de/calendaralarm/privacy/
    CalendarAlarmApplication.kt

    MainActivity.kt

    alarm/
        AlarmReceiver.kt
        AlarmService.kt
        AlarmScheduler.kt
        BootReceiver.kt
        ExactAlarmPermissionReceiver.kt

    data/
        calendar/
            CalendarReader.kt
            CalendarInfo.kt
            CalendarEvent.kt

        db/
            AppDatabase.kt
            ScheduledAlarm.kt
            ScheduledAlarmDao.kt
            AlarmState.kt

        prefs/
            AppPreferences.kt

    domain/
        AlarmDiff.kt
        AlarmSyncManager.kt
        EventFilter.kt

    worker/
        CalendarSyncWorker.kt

    ui/
        home/
            HomeScreen.kt
            HomeViewModel.kt

        settings/
            SettingsScreen.kt
            SettingsViewModel.kt

        onboarding/
            OnboardingScreen.kt
            OnboardingViewModel.kt

        alarm/
            AlarmActivity.kt
            AlarmScreen.kt

        theme/
            Theme.kt
```

Names can differ slightly if there is a clear reason.

Avoid unnecessary abstraction layers.

---

# 69. No Fake Implementations

Do NOT satisfy requirements using placeholders such as:

```kotlin
TODO()
```

or:

```text
// implement later
```

or:

```text
dummy event
mock calendar
fake alarm
```

Production code must use actual Android APIs.

Mocks/fakes are allowed only inside tests.

---

# 70. No Feature Creep

Do not add:

- cloud sync
- account login
- widgets
- Wear OS
- iOS
- location alarms
- contacts
- SMS
- AI functionality
- online backup
- server synchronization
- calendar editing
- task manager
- notes
- weather
- analytics

This app should remain small.

---

# 71. Acceptance Criteria

Implementation is complete only when all of the following are true.

## Build

- project builds;
- tests execute;
- Debug APK is generated.

## Privacy

- no INTERNET permission;
- no WRITE_CALENDAR permission;
- no network SDK;
- no analytics;
- no advertising;
- no telemetry;
- Android backup disabled.

## Calendar

- calendars can be selected;
- calendar events are read through CalendarContract;
- recurring instances are handled through Instances;
- deleted/moved events result in alarm changes.

## Alarm

- uses `AlarmManager.setAlarmClock()`;
- exact alarm permission handled correctly;
- alarm uses `USAGE_ALARM`;
- alarm service is a properly declared foreground service;
- Stop works;
- automatic stop works;
- Test Alarm uses production path.

## Android lifecycle

- boot rebuilds alarms;
- package update rebuilds alarms;
- app restart does not produce duplicate alarms;
- periodic synchronization does not create duplicate workers.

## UI

- onboarding works;
- Home shows upcoming alarms;
- settings work;
- Reliability card accurately reflects permissions;
- system dark/light mode supported.

## CI

- push to main starts GitHub Actions;
- GitHub Actions successfully creates an installable Debug APK artifact;
- privacy check runs in CI.

---

# 72. Final Agent Workflow

Perform work in this order:

```text
1. Inspect repository.
2. Preserve this SPEC.md in the repository.
3. Create Android project.
4. Configure Gradle.
5. Implement data/calendar layer.
6. Implement Room/DataStore.
7. Implement alarm scheduler.
8. Implement alarm receiver/service.
9. Implement synchronization/diff logic.
10. Implement reboot/update recovery.
11. Implement onboarding.
12. Implement Home UI.
13. Implement Settings UI.
14. Implement Alarm UI.
15. Implement Test Alarm.
16. Add tests.
17. Add privacy/security checks.
18. Add GitHub Actions.
19. Add documentation.
20. Run complete build.
21. Fix all build failures.
22. Run tests.
23. Run lint.
24. Build APK.
25. Inspect final manifest/APK permissions.
26. Perform security self-review.
27. Update SECURITY.md with findings.
28. git status and verify no generated build files/secrets are committed.
29. Commit all source/documentation/workflow files.
30. Push to the configured GitHub repository.
```

Do not stop before step 30 unless repository push access is technically unavailable.

If GitHub push access is unavailable, complete every other step and report the exact Git command that failed.

---

# 73. Final Response Required From Coding Agent

At completion report only concrete results:

```text
Implementation: complete / incomplete
Build: passed / failed
Unit tests: passed / failed
Lint: passed / warnings / failed
Privacy check: passed / failed
INTERNET permission: absent / present
WRITE_CALENDAR permission: absent / present
APK path: ...
Git commit: ...
GitHub push: successful / failed
GitHub Actions workflow: added / not added
```

Also list any unresolved issue.

Do not claim successful build, APK creation, test pass, or GitHub push unless it actually occurred.

---

# 74. Most Important Rules

If any other part of this specification conflicts with these rules, these win:

1. **No Internet permission.**
2. **No calendar write permission.**
3. **No analytics, advertising, tracking or telemetry.**
4. **Calendar processing stays local on the device.**
5. **Use `AlarmManager.setAlarmClock()` for production event alarms.**
6. **Use Android alarm audio semantics (`USAGE_ALARM`).**
7. **Do not expose Android components unnecessarily.**
8. **Do not log private calendar content.**
9. **Android application backup is disabled.**
10. **The project must actually build and produce an APK.**
11. **GitHub Actions must automatically build an installable APK.**
12. **Do not leave required functionality as TODOs.**
13. **Do not silently weaken alarm reliability to make implementation easier.**
14. **Do not silently add permissions or dependencies.**
15. **Push the completed work to GitHub when repository access is available.**