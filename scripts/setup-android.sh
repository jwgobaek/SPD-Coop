#!/usr/bin/env bash
# Linux x86_64. Supply JDK 21 and an Android SDK directory writable by this user.
set -eu
: "${ANDROID_HOME:?Set ANDROID_HOME to the SDK directory}"
javac -version 2>&1 | grep -Eq 'javac 21([.]|$)'
manager="$ANDROID_HOME/cmdline-tools/13114758/bin/sdkmanager"
if [[ ! -x "$manager" ]]; then
  temp=$(mktemp -d)
  trap 'rm -rf "$temp"' EXIT
  curl --fail --location --retry 2 https://dl.google.com/android/repository/commandlinetools-linux-13114758_latest.zip -o "$temp/tools.zip"
  echo "7ec965280a073311c339e571cd5de778b9975026cfcbe79f2b1cdcb1e15317ee  $temp/tools.zip" | sha256sum -c -
  unzip -q "$temp/tools.zip" -d "$temp"
  mkdir -p "$ANDROID_HOME/cmdline-tools"
  mv "$temp/cmdline-tools" "$ANDROID_HOME/cmdline-tools/13114758"
fi
yes | "$manager" --sdk_root="$ANDROID_HOME" 'platforms;android-36' 'build-tools;36.0.0'
