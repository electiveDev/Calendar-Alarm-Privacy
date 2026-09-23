# Privacy

Calendar Alarm Privacy processes calendar information locally on the Android device.

The application does not request Internet access and contains no code intended to communicate with external servers.

Calendar access is read-only. The app never creates, edits, accepts, declines, or deletes calendar events.

No analytics, advertising, telemetry, cloud synchronization, account system, or tracking SDK is included. Android application backup is disabled.

## Local data

The internal Room database stores only the identity and scheduling state needed to cancel or recreate an alarm: event-instance ID, event ID, calendar ID, meeting time, alarm time, request code, mute/state values, and the event title needed for a reliable alarm screen while that alarm is active. DataStore stores selected calendar IDs and alarm preferences. Descriptions, locations, attendees, organizers, accounts, conference links, and attachments are not stored.

The app does not deliberately write calendar-derived data to shared storage, Downloads, or public files.
