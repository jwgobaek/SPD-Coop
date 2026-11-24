#!/usr/bin/env bash
# Use the same build and verification locally in Cloud and on GitHub runners.
set -euo pipefail
reports=${1:?Supply a new output directory for this build}
mkdir "$reports"
git rev-parse HEAD > "$reports/commit.txt"
./gradlew --no-daemon :core:cleanTest :core:test :android:assembleDebug --console=plain 2>&1 | tee "$reports/build.log"
cp -r core/build/test-results/test "$reports/junit"
python3 - "$reports/junit" <<'PY'
import sys
from pathlib import Path
from xml.etree import ElementTree

cases = []
for result in Path(sys.argv[1]).glob("TEST-*.xml"):
    cases.extend(ElementTree.parse(result).getroot().findall("testcase"))
if not cases or any(case.find(tag) is not None for case in cases for tag in ("failure", "error", "skipped")):
    sys.exit("Expected executed and passing Headless test cases")
print(f"PASS: {len(cases)} JUnit cases")
PY
apk=android/build/outputs/apk/debug/android-debug.apk
scripts/inspect-apk.sh "$apk" org.spdcoop.dungeon.indev | tee "$reports/apk.txt"
cp "$apk" "$reports/"
(cd "$reports" && sha256sum android-debug.apk > SHA256SUMS && sha256sum -c SHA256SUMS)
