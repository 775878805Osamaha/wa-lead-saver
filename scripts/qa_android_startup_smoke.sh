#!/usr/bin/env bash
# Runs only on a fresh, disposable CI emulator, never on a customer's phone.
set -euo pipefail
APK="${1:?APK path}"
OUT="${2:?diagnostic directory}"
PKG=com.example.waleadsaver.flutterqa
ACT=com.example.flutterbridge.WaFlutterHostActivity
mkdir -p "$OUT"
capture() {
  adb logcat -b crash -d -v threadtime > "$OUT/crash.log" 2>&1 || true
  adb logcat -d -v threadtime -s AndroidRuntime FlutterActivity FlutterEngine flutter DartVM libc DEBUG ActivityTaskManager > "$OUT/startup.log" 2>&1 || true
  adb shell dumpsys activity activities > "$OUT/activity.txt" 2>&1 || true
  adb exec-out screencap -p > "$OUT/screen.png" 2>/dev/null || true
}
trap capture EXIT
adb wait-for-device
adb shell getprop ro.build.version.sdk > "$OUT/android-api.txt"
adb shell getprop ro.product.cpu.abi > "$OUT/abi.txt"
adb uninstall "$PKG" >/dev/null 2>&1 || true
adb logcat -c
adb install "$APK" | tee "$OUT/install.txt"
adb shell input keyevent 82 || true
for round in 1 2; do
  adb shell am force-stop "$PKG"
  adb shell am start -W -n "$PKG/$ACT" | tee "$OUT/start-$round.txt"
  grep -q 'Status: ok' "$OUT/start-$round.txt"
  found=0
  for attempt in $(seq 1 20); do
    sleep 2
    adb shell pidof "$PKG" > "$OUT/pid-$round.txt" || true
    adb logcat -b crash -d -v brief > "$OUT/crash-$round.log"
    if grep -Fq "$PKG" "$OUT/crash-$round.log"; then
      echo "::error::Android crash in QA package; see diagnostic artifact"
      exit 1
    fi
    if [[ ! -s "$OUT/pid-$round.txt" ]]; then continue; fi
    adb shell uiautomator dump /sdcard/wa-smoke.xml >/dev/null 2>&1 || continue
    adb pull /sdcard/wa-smoke.xml "$OUT/ui-$round.xml" >/dev/null 2>&1 || continue
    if grep -Fq 'WA Lead Saver' "$OUT/ui-$round.xml" && grep -Eq 'تسجيل الدخول|الدخول إلى التطبيق|إعادة التحقق' "$OUT/ui-$round.xml"; then
      found=1
      break
    fi
  done
  if [[ "$found" != 1 ]]; then
    echo "::error::Flutter login UI not verified on Android"
    exit 1
  fi
  sleep 5
  adb shell pidof "$PKG" >/dev/null
  adb logcat -b crash -d -v brief > "$OUT/crash-$round.log"
  if grep -Fq "$PKG" "$OUT/crash-$round.log"; then exit 1; fi
  adb exec-out screencap -p > "$OUT/screen-$round.png"
done
echo 'Two cold starts reached the Flutter login gate without a recorded process crash.'
