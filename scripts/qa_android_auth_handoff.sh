#!/usr/bin/env bash
# Guest-only UI test on an already installed, disposable QA emulator.
# No credentials, network mutation, contact permissions or customer data.
set -euo pipefail
OUT="${1:?diagnostic directory}"
PKG=com.example.waleadsaver.flutterqa
mkdir -p "$OUT"
capture() {
  adb logcat -b crash -d -v threadtime > "$OUT/crash.log" 2>&1 || true
  adb shell dumpsys activity activities > "$OUT/activity.txt" 2>&1 || true
  adb exec-out screencap -p > "$OUT/final.png" 2>/dev/null || true
}
trap capture EXIT
assert_alive() {
  adb shell pidof "$PKG" >/dev/null
  adb logcat -b crash -d -v brief > "$OUT/latest-crash.log"
  adb logcat -b events -d -v brief -s am_anr > "$OUT/latest-anr.log"
  if grep -Fq "$PKG" "$OUT/latest-crash.log" || grep -Fq "$PKG" "$OUT/latest-anr.log"; then
    echo "::error::QA app crashed during authentication handoff"
    exit 1
  fi
}
await_screen() {
  local expected="$1" activity="$2" name="$3"
  local deadline=$((SECONDS + 90))
  while (( SECONDS < deadline )); do
    sleep 2
    assert_alive
    adb shell rm -f /sdcard/wa-handoff.xml
    timeout 12s adb shell uiautomator dump /sdcard/wa-handoff.xml >/dev/null 2>&1 || continue
    adb pull /sdcard/wa-handoff.xml "$OUT/$name.xml" >/dev/null 2>&1 || continue
    adb shell dumpsys activity activities > "$OUT/$name-activity.txt"
    if grep -E 'mResumedActivity|topResumedActivity' "$OUT/$name-activity.txt" | grep -Fq "$PKG/$activity" &&
       grep -Fq "package=\"$PKG\"" "$OUT/$name.xml" &&
       grep -Fq "$expected" "$OUT/$name.xml"; then
      adb exec-out screencap -p > "$OUT/$name.png"
      return 0
    fi
  done
  echo "::error::Screen $name did not become active with expected UI"
  return 1
}
tap_text() {
  local xml="$1" label="$2" coords
  coords="$(python3 - "$xml" "$label" <<'PY'
import re, sys, xml.etree.ElementTree as ET
root = ET.parse(sys.argv[1]).getroot()
label = sys.argv[2]
parents = {child: parent for parent in root.iter() for child in parent}
for node in root.iter('node'):
    if label not in (node.get('text'), node.get('content-desc')):
        continue
    target = node
    while target is not None and target.get('clickable') != 'true':
        target = parents.get(target)
    if target is None or target.get('enabled') != 'true':
        continue
    points = [int(x) for x in re.findall(r'\d+', target.get('bounds', ''))]
    if len(points) == 4 and points[2] > points[0] and points[3] > points[1]:
        print((points[0]+points[2])//2, (points[1]+points[3])//2)
        sys.exit(0)
sys.exit("Enabled clickable label not found: " + label)
PY
)"
  read -r x y <<< "$coords"
  adb shell input tap "$x" "$y"
}
await_screen 'تسجيل الدخول' 'com.example.flutterbridge.WaFlutterHostActivity' 'flutter-before'
tap_text "$OUT/flutter-before.xml" 'تسجيل الدخول'
await_screen 'تسجيل الدخول مطلوب لاستخدام واجهة Flutter' 'com.example.MainActivity' 'native-login'
# Back cancels login without submitting or bypassing the authentication gate.
adb shell input keyevent 4
await_screen 'تسجيل الدخول' 'com.example.flutterbridge.WaFlutterHostActivity' 'flutter-return'
tap_text "$OUT/flutter-return.xml" 'إعادة التحقق'
await_screen 'الدخول إلى التطبيق' 'com.example.flutterbridge.WaFlutterHostActivity' 'flutter-recheck'
assert_alive
if grep -Fq 'إجمالي العملاء' "$OUT/flutter-recheck.xml"; then
  echo "::error::Customer dashboard visible without authentication"
  exit 1
fi
echo 'Guest login handoff, cancellation, and recheck verified on Android.'
