# Testing

## Automated checks

```bash
./gradlew clean
./gradlew test
./gradlew lintDebug
./scripts/privacy-check.sh
./gradlew assembleDebug
```

The debug APK is `app/build/outputs/apk/debug/app-debug.apk`.

## Manual device checklist

Create a short future event in a selected Android calendar and verify:

1. Screen on, ringer normal, event in two minutes: an audible alarm fires.
2. Screen off and phone silent: alarm audio uses the alarm stream.
3. Do Not Disturb enabled with Notification Policy Access: the alarm remains audible according to the device policy.
4. Swipe the app away from recents: an already scheduled alarm still fires.
5. Device idle/Doze: `setAlarmClock()` fires at the expected time.
6. Move an event: after synchronization the old alarm is cancelled and the new alarm is scheduled.
7. Delete an event: after synchronization its alarm is cancelled.
8. Reboot the device: future alarms are rebuilt after boot.
9. Schedule two events for the same time: neither is silently lost; the foreground service keeps one controlled audio session and merges titles in the notification.

Android does not guarantee alarm/broadcast behavior after the user explicitly force-stops the app in Android Settings. Force-stop can suppress broadcasts and alarms until the app is launched again.

## Privacy verification

The CI privacy check fails if the merged manifest contains `android.permission.INTERNET` or `android.permission.WRITE_CALENDAR`, or if application source/build configuration introduces the forbidden networking and telemetry dependencies listed in the specification.
