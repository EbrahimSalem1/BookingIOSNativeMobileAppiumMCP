#!/usr/bin/env bash
# Creates and boots N identical simulators for parallel runs and prints the DEVICE_POOL value.
#
#   ./scripts/boot-simulators.sh 3 "iPhone 15" "17.5"
#   export DEVICE_POOL="$(./scripts/boot-simulators.sh 3 'iPhone 15' 17.5 | tail -1)"
#
# One simulator per thread: sharing a simulator between sessions is the #1 cause of
# "random" failures in parallel iOS runs.
set -euo pipefail

COUNT="${1:-1}"
DEVICE_TYPE="${2:-iPhone 15}"
IOS_VERSION="${3:-17.5}"
RUNTIME="com.apple.CoreSimulator.SimRuntime.iOS-${IOS_VERSION//./-}"

if ! xcrun simctl list runtimes | grep -q "iOS ${IOS_VERSION}"; then
  echo "iOS ${IOS_VERSION} runtime not installed. Available:" >&2
  xcrun simctl list runtimes >&2
  exit 1
fi

# Software keyboard must appear (tests check keyboard behaviour) -> disable hardware keyboard.
defaults write com.apple.iphonesimulator ConnectHardwareKeyboard -bool false

POOL=()
for i in $(seq 1 "$COUNT"); do
  NAME="QA ${DEVICE_TYPE} #${i}"
  UDID="$(xcrun simctl list devices | grep "${NAME} (" | head -1 | grep -oE '[0-9A-F-]{36}' || true)"
  if [[ -z "$UDID" ]]; then
    UDID="$(xcrun simctl create "$NAME" "$DEVICE_TYPE" "$RUNTIME")"
  fi
  xcrun simctl boot "$UDID" 2>/dev/null || true          # already booted is fine
  xcrun simctl bootstatus "$UDID" -b >/dev/null           # block until fully booted
  # Deterministic UI: English, no "slide to type" tips, fixed status bar for screenshots.
  xcrun simctl spawn "$UDID" defaults write -g AppleLanguages -array en
  xcrun simctl spawn "$UDID" defaults write -g AppleLocale en_US
  xcrun simctl status_bar "$UDID" override --time "09:41" --batteryState charged --batteryLevel 100 || true
  echo "Booted ${NAME} -> ${UDID}" >&2
  POOL+=("${NAME}@${UDID}@${IOS_VERSION}")
done

if [[ -n "${BOOKING_APP_PATH:-}" ]]; then
  for entry in "${POOL[@]}"; do
    udid="$(echo "$entry" | cut -d@ -f2)"
    xcrun simctl install "$udid" "$BOOKING_APP_PATH"
  done
fi

(IFS=,; echo "${POOL[*]}")
