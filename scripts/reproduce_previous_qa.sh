#!/usr/bin/env bash
set -uo pipefail
mkdir -p diagnostics
a=0
b=0
bash scripts/qa_android_startup_smoke.sh previous/admin/WA-Lead-Saver-Flutter-QA.apk diagnostics/admin || a=$?
bash scripts/qa_android_startup_smoke.sh previous/auth/WA-Lead-Saver-Flutter-QA.apk diagnostics/auth || b=$?
echo "admin=$a auth=$b" | tee diagnostics/result.txt
# Negative controls must fail with the specific reproduced exception.
# A failed install, timeout or unrelated crash is not a valid reproduction.
for entry in admin auth; do
  grep -Fq 'FATAL EXCEPTION: main' "diagnostics/$entry/crash.log" || exit 1
  grep -Fq 'ClassNotFoundException: Didn' "diagnostics/$entry/crash.log" || exit 1
  grep -Fq 'com.example.flutterbridge.WaFlutterHostActivity' "diagnostics/$entry/crash.log" || exit 1
done
test "$a" -ne 0 && test "$b" -ne 0 || exit 1
echo 'Both historical APKs reproduced the expected missing launcher failure.'
