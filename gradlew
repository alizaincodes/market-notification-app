#!/usr/bin/env bash
set -euo pipefail
# Minimal gradlew shim: if 'gradle' is available, use it; otherwise install via SDKMAN and use it.
if command -v gradle >/dev/null 2>&1; then
  # If system gradle exists, prefer it only if it's compatible (major version 8).
  GRADLE_VER=$(gradle -v 2>/dev/null | awk '/Gradle/ {print $2; exit}') || true
  GRADLE_MAJOR=${GRADLE_VER%%.*}
  if [ -n "$GRADLE_MAJOR" ] && [ "$GRADLE_MAJOR" -eq 8 ] 2>/dev/null; then
    exec gradle "$@"
  else
    echo "Detected Gradle version: ${GRADLE_VER:-unknown}. Installing Gradle 8.2 via SDKMAN for compatibility..."
  fi
fi

if [ -z "${HOME:-}" ]; then
  HOME=~
fi

if [ ! -d "$HOME/.sdkman" ]; then
  echo "SDKMAN not found — installing SDKMAN to obtain Gradle (non-interactive)..."
  curl -s "https://get.sdkman.io" | bash
fi

# shellcheck source=/dev/null
if [ -s "$HOME/.sdkman/bin/sdkman-init.sh" ]; then
  # shellcheck disable=SC1090
  source "$HOME/.sdkman/bin/sdkman-init.sh"
else
  echo "Could not source SDKMAN init script. Exiting." >&2
  exit 1
fi

echo "Installing Gradle 8.2 via SDKMAN (if not already installed)..."
sdk install gradle 8.2 || true

GRADLE_822_PATH="$HOME/.sdkman/candidates/gradle/8.2/bin/gradle"
if [ -x "$GRADLE_822_PATH" ]; then
  exec "$GRADLE_822_PATH" "$@"
elif command -v gradle >/dev/null 2>&1; then
  exec gradle "$@"
else
  echo "gradle command still not found after SDKMAN install." >&2
  exit 2
fi
