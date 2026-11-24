#!/usr/bin/env bash
set -euo pipefail
apk=${1:?APK path}
expected=${2:?expected application ID}
bin="$ANDROID_HOME/build-tools/36.0.0"
badging=$("$bin/aapt" dump badging "$apk")
printf '%s\n' "$badging" | sed -n '/^package:/p;/^sdkVersion:/p;/^targetSdkVersion:/p;/^native-code:/p'
[[ "$badging" == "package: name='$expected'"* ]]
"$bin/apksigner" verify --verbose --print-certs "$apk"
stat --format='bytes=%s' "$apk"
sha256sum "$apk"
