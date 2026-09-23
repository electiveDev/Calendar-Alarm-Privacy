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

## Download

The newest installable APK is available under [GitHub Releases](https://github.com/electiveDev/Calendar-Alarm-Privacy/releases/latest).

## GitHub Actions

Pushes to `main`, pull requests, and manual workflow runs execute tests, lint, the privacy check, and a debug APK build. The artifact is named `CalendarAlarmPrivacy-APK`. After a successful `main` push, the release workflow builds a stable-signed APK and publishes it as `Calendar-Alarm-Privacy.apk` under GitHub Releases.

### One-time stable signing setup

Stable signing is intentionally not committed to Git. Until all four secrets below exist, debug CI continues to work but the release workflow skips publication:

- `ANDROID_KEYSTORE_BASE64`
- `ANDROID_KEYSTORE_PASSWORD`
- `ANDROID_KEY_ALIAS`
- `ANDROID_KEY_PASSWORD`

Create the keystore once on a secure machine and keep the original file and passwords in a password manager. For example, with PowerShell and the GitHub CLI:

```powershell
keytool -genkeypair -v -keystore Calendar-Alarm-Privacy-release.jks -alias calendar-alarm-privacy -keyalg RSA -keysize 2048 -validity 10000
$base64 = [Convert]::ToBase64String([IO.File]::ReadAllBytes("Calendar-Alarm-Privacy-release.jks"))
$base64 | gh secret set ANDROID_KEYSTORE_BASE64 --repo electiveDev/Calendar-Alarm-Privacy
gh secret set ANDROID_KEYSTORE_PASSWORD --repo electiveDev/Calendar-Alarm-Privacy
gh secret set ANDROID_KEY_ALIAS --repo electiveDev/Calendar-Alarm-Privacy
gh secret set ANDROID_KEY_PASSWORD --repo electiveDev/Calendar-Alarm-Privacy
Remove-Item Calendar-Alarm-Privacy-release.jks
```

The three interactive `gh secret set` commands read their values without putting the credentials into the repository. Never commit the keystore or credentials. After the secrets are configured, the next successful `main` build creates a release automatically.

See [SPEC.md](SPEC.md), [PRIVACY.md](PRIVACY.md), [SECURITY.md](SECURITY.md), and [TESTING.md](TESTING.md) for the full design and verification checklist.
