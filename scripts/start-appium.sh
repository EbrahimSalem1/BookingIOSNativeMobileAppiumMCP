#!/usr/bin/env bash
# Starts Appium in the background and waits until /status answers (no blind sleep).
set -euo pipefail

PORT="${APPIUM_PORT:-4723}"
LOG="${APPIUM_LOG:-target/appium-server.log}"
mkdir -p "$(dirname "$LOG")"

if ! command -v appium >/dev/null; then
  npm install -g appium
fi
appium driver list --installed 2>&1 | grep -q xcuitest || appium driver install xcuitest

nohup appium --port "$PORT" --log-level info --log-timestamp --relaxed-security >"$LOG" 2>&1 &
echo $! > target/appium.pid

for _ in $(seq 1 60); do
  if curl -fsS "http://127.0.0.1:${PORT}/status" >/dev/null 2>&1; then
    echo "Appium is ready on :${PORT}"
    exit 0
  fi
  sleep 1   # shell polling with a bound, not a test wait
done
echo "Appium did not start; see $LOG" >&2
tail -50 "$LOG" >&2
exit 1
