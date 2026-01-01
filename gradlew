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

# If we already have a local Gradle 8.2 unpacked, use it.
GRADLE_VERSION=8.2
INSTALL_ROOT="$HOME/.gradle/candidates/gradle"
INSTALL_DIR="$INSTALL_ROOT/gradle-$GRADLE_VERSION"
GRADLE_BIN="$INSTALL_DIR/bin/gradle"

if [ -x "$GRADLE_BIN" ]; then
  exec "$GRADLE_BIN" "$@"
fi

echo "Gradle $GRADLE_VERSION not found locally — downloading distribution..."
DIST_ZIP="gradle-${GRADLE_VERSION}-bin.zip"
DOWNLOAD_URL="https://services.gradle.org/distributions/${DIST_ZIP}"
TMPZIP=$(mktemp /tmp/gradle.XXXX.zip)
if ! curl -fsSL -o "$TMPZIP" "$DOWNLOAD_URL"; then
  echo "Failed to download Gradle from $DOWNLOAD_URL" >&2
  rm -f "$TMPZIP"
  exit 3
fi

mkdir -p "$INSTALL_ROOT"
unzip -q "$TMPZIP" -d "$INSTALL_ROOT" || {
  echo "Failed to unzip Gradle distribution." >&2
  rm -f "$TMPZIP"
  exit 4
}
rm -f "$TMPZIP"

if [ -x "$GRADLE_BIN" ]; then
  exec "$GRADLE_BIN" "$@"
else
  echo "gradle binary not found after extracting distribution." >&2
  exit 5
fi
