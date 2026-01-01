#!/usr/bin/env bash
set -euo pipefail
# Minimal gradlew shim: if 'gradle' is available, use it; otherwise install via SDKMAN and use it.
if command -v gradle >/dev/null 2>&1; then
  exec gradle "$@"
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

if command -v gradle >/dev/null 2>&1; then
  exec gradle "$@"
else
  echo "gradle command still not found after SDKMAN install." >&2
  exit 2
fi
