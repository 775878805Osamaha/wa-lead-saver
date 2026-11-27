#!/usr/bin/env bash
set -uo pipefail
mkdir -p diagnostics
a=0
b=0
bash scripts/qa_android_startup_smoke.sh previous/admin/WA-Lead-Saver-Flutter-QA.apk diagnostics/admin || a=$?
bash scripts/qa_android_startup_smoke.sh previous/auth/WA-Lead-Saver-Flutter-QA.apk diagnostics/auth || b=$?
echo "admin=$a auth=$b" | tee diagnostics/result.txt
test "$a" -eq 0 && test "$b" -eq 0
