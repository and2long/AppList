#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
APK_PATH="$ROOT_DIR/applist/build/outputs/apk/debug/applist-debug.apk"
PACKAGE_NAME="com.and2long.applist"
ACTIVITY_NAME="com.and2long.applist.MainActivity"

usage() {
  echo "Usage: $0 [device_serial]"
  echo
  echo "Builds the debug APK, installs it on a connected Android device, and launches it."
}

if [[ "${1:-}" == "-h" || "${1:-}" == "--help" ]]; then
  usage
  exit 0
fi

if ! command -v adb >/dev/null 2>&1; then
  echo "Error: adb was not found in PATH." >&2
  exit 1
fi

DEVICE_SERIAL="${1:-}"

if [[ -z "$DEVICE_SERIAL" ]]; then
  DEVICES=()
  while IFS= read -r device; do
    DEVICES+=("$device")
  done < <(adb devices | awk 'NR > 1 && $2 == "device" { print $1 }')

  if [[ "${#DEVICES[@]}" -eq 0 ]]; then
    echo "Error: no connected Android devices found." >&2
    exit 1
  fi

  if [[ "${#DEVICES[@]}" -gt 1 ]]; then
    echo "Error: multiple Android devices found. Pass one device serial explicitly:" >&2
    for device in "${DEVICES[@]}"; do
      printf '  %s %s\n' "$0" "$device" >&2
    done
    exit 1
  fi

  DEVICE_SERIAL="${DEVICES[0]}"
fi

echo "Building debug APK..."
"$ROOT_DIR/gradlew" -p "$ROOT_DIR" :applist:assembleDebug

echo "Installing on device $DEVICE_SERIAL..."
adb -s "$DEVICE_SERIAL" install -r "$APK_PATH"

echo "Launching $PACKAGE_NAME..."
adb -s "$DEVICE_SERIAL" shell am start -n "$PACKAGE_NAME/$ACTIVITY_NAME"

echo "Done."
