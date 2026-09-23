# Calendar Alarm Privacy

Calendar Alarm Privacy reads selected calendars from Android's local Calendar Provider and turns upcoming events into real Android alarms. Calendar changes are synchronized automatically; appointments are never edited by this app.

## Privacy first

- Android 10+ (`minSdk 29`)
- completely offline; the manifest has no `INTERNET` permission
- read-only calendar access; no `WRITE_CALENDAR` permission
- no accounts, cloud sync, analytics, advertising, telemetry, or tracking SDKs
- Android application backup is disabled
- only the local alarm identity, times, state, and temporary display title are stored

## First run

Onboarding requests calendar and notification access, checks exact-alarm access, offers optional Full-Screen Intent and Do Not Disturb access, and lets you select calendars. The default alarm lead time is zero minutes. Use **Test alarm in 10 seconds** to exercise the production alarm path.

## Build locally

```bash
./gradlew clean
./gradlew test
./gradlew lintDebug
./gradlew assembleDebug
```

The installable debug APK is generated at `app/build/outputs/apk/debug/app-debug.apk`.

## GitHub Actions

Pushes to `main`, pull requests, and manual workflow runs execute tests, lint, the privacy check, and a debug APK build. The artifact is named `CalendarAlarmPrivacy-APK`. Tags beginning with `v` run the release workflow and publish the debug APK as an Actions artifact. No signing key is included; a production-signed release can be added later.

See [SPEC.md](SPEC.md), [PRIVACY.md](PRIVACY.md), [SECURITY.md](SECURITY.md), and [TESTING.md](TESTING.md) for the full design and verification checklist.
