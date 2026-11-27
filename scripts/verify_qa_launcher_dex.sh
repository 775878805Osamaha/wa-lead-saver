#!/usr/bin/env bash
set -euo pipefail
APK="${1:?APK path}"
DEXDUMP="${ANDROID_HOME:?Android SDK}/build-tools/36.0.0/dexdump"
test -x "$DEXDUMP"
TMP="$(mktemp -d)"
trap 'rm -rf "$TMP"' EXIT
unzip -j "$APK" 'classes*.dex' -d "$TMP" >/dev/null
found=0
for dex in "$TMP"/classes*.dex; do
    test -s "$dex"
    "$DEXDUMP" "$dex" > "$TMP/descriptors.txt"
    if grep -Eq "Class descriptor[[:space:]]*:[[:space:]]*'Lcom/example/flutterbridge/WaFlutterHostActivity;'" "$TMP/descriptors.txt"; then
        echo "Verified actual launcher class definition in $(basename "$dex")"
        found=1
        break
    fi
done
if [[ "$found" != 1 ]]; then
    echo "::error::Launcher class is missing from all APK DEX files (ClassNotFoundException)"
    exit 1
fi
