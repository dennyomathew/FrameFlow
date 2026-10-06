#!/bin/bash
# Installs the Android SDK in Claude Code cloud sessions so Gradle can build,
# lint and run unit tests. Requires dl.google.com in the environment's
# network allowlist (SDK packages and AndroidX/AGP artifacts are served there).
set -euo pipefail

if [ "${CLAUDE_CODE_REMOTE:-}" != "true" ]; then
  exit 0
fi

SDK_ROOT="${ANDROID_HOME:-$HOME/android-sdk}"
CMDLINE_TOOLS_ZIP="commandlinetools-linux-13114758_latest.zip"
PACKAGES=("platform-tools" "platforms;android-35" "build-tools;35.0.0")

persist_env() {
  if [ -n "${CLAUDE_ENV_FILE:-}" ]; then
    {
      echo "export ANDROID_HOME=\"$SDK_ROOT\""
      echo "export ANDROID_SDK_ROOT=\"$SDK_ROOT\""
      echo "export PATH=\"$SDK_ROOT/cmdline-tools/latest/bin:$SDK_ROOT/platform-tools:\$PATH\""
    } >> "$CLAUDE_ENV_FILE"
  fi
  echo "sdk.dir=$SDK_ROOT" > "$CLAUDE_PROJECT_DIR/local.properties"
}

if ! curl -sSfI -o /dev/null https://dl.google.com/android/repository/repository2-3.xml 2>/dev/null; then
  echo "WARNING: dl.google.com is not reachable; skipping Android SDK install." >&2
  echo "Add dl.google.com to the cloud environment's allowed domains to enable Android builds." >&2
  exit 0
fi

SDKMANAGER="$SDK_ROOT/cmdline-tools/latest/bin/sdkmanager"
if [ ! -x "$SDKMANAGER" ]; then
  tmp="$(mktemp -d)"
  curl -sSfL -o "$tmp/tools.zip" "https://dl.google.com/android/repository/$CMDLINE_TOOLS_ZIP"
  unzip -q "$tmp/tools.zip" -d "$tmp"
  mkdir -p "$SDK_ROOT/cmdline-tools"
  rm -rf "$SDK_ROOT/cmdline-tools/latest"
  mv "$tmp/cmdline-tools" "$SDK_ROOT/cmdline-tools/latest"
  rm -rf "$tmp"
fi

yes | "$SDKMANAGER" --sdk_root="$SDK_ROOT" --licenses > /dev/null || true
"$SDKMANAGER" --sdk_root="$SDK_ROOT" "${PACKAGES[@]}" > /dev/null

persist_env

# Pre-fetch the Gradle distribution and dependencies so they are cached.
cd "$CLAUDE_PROJECT_DIR"
./gradlew --no-daemon -q :app:dependencies > /dev/null || true
