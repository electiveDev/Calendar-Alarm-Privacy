#!/usr/bin/env bash
set -euo pipefail

manifest="app/src/main/AndroidManifest.xml"
merged="app/build/intermediates/merged_manifests/debug/processDebugManifest/AndroidManifest.xml"
failed=0

if [[ -f "$manifest" ]] && grep -nE 'android\.permission\.(INTERNET|WRITE_CALENDAR)' "$manifest"; then
  echo "Forbidden permission found in source manifest" >&2
  failed=1
fi
if [[ -f "$merged" ]]; then
  matches=$(grep -nE 'android\.permission\.(INTERNET|ACCESS_NETWORK_STATE|WRITE_CALENDAR)' "$merged" || true)
  if [[ -n "$matches" ]]; then
    echo "$matches"
    echo "Forbidden effective permission found in merged manifest" >&2
    failed=1
  fi
else
  echo "Merged debug manifest not found; run assembleDebug before the privacy check." >&2
  failed=1
fi

for term in firebase analytics crashlytics sentry bugsnag appcenter okhttp retrofit ktor-client volley; do
  if grep -RniE --exclude-dir=.git --exclude-dir=build --exclude='*.md' --exclude='SPEC.md' "$term" app/src app/build.gradle.kts gradle 2>/dev/null; then
    echo "Forbidden dependency or telemetry term found: $term" >&2
    failed=1
  fi
done

if grep -RniE --exclude-dir=.git --exclude-dir=build --include='*.kt' --include='*.java' --include='*.xml' \
  'HttpURLConnection|WebView|OkHttpClient|Retrofit|WebSocket|java\.net\.|android\.permission\.INTERNET|android\.permission\.WRITE_CALENDAR|android\.permission\.ACCESS_NETWORK_STATE' app/src/main/java app/build.gradle.kts gradle 2>/dev/null; then
  echo "Forbidden runtime network/calendar-write API found" >&2
  failed=1
fi

if [[ "$failed" -ne 0 ]]; then
  exit 1
fi

echo "Privacy check passed: forbidden network/calendar-write permissions and runtime APIs absent."
