#!/usr/bin/env bash
set -u
adb emu geo fix 0 0
python3 scripts/e2e_fixture.py >/tmp/placy-e2e-fixture.log 2>&1 &
fixture_pid=$!
trap 'kill "$fixture_pid" 2>/dev/null || true' EXIT
for _ in {1..50}; do
  if curl --silent --fail http://127.0.0.1:18080/geomarks >/dev/null; then break; fi
  sleep 0.1
done
if ./gradlew :composeApp:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.example.placy.MapE2ETest,com.example.placy.PostCardUiTest; then
  test_exit=0
else
  test_exit=$?
fi
mkdir -p e2e-screenshots
for name in map card fullscreen before-marker-tap; do
  adb pull "/sdcard/placy-$name.png" e2e-screenshots/ || true
done
exit "$test_exit"
