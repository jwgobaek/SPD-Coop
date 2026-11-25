#!/usr/bin/env python3
"""Run the isolated power, native cast, profile isolation, room-generation and socket probes."""
import os
from pathlib import Path
import runpy
import subprocess
from tempfile import TemporaryDirectory

root = Path(__file__).resolve().parents[1]
fixture = runpy.run_path(str(root / "scripts/verify-alpha-migration.py"))
java = Path(os.environ["JAVA_HOME"]) / "bin"
probes = ("YogSoloProfileProbe", "VaultFinalRoomSurvey", "LanResourceProbe")
with TemporaryDirectory() as name:
    work = Path(name)
    classpath = fixture["classpath"](root, work)
    sources = [root / "scripts/verification" / (probe + ".java") for probe in probes]
    subprocess.run([java / "javac", "-cp", classpath, "-d", work, *sources], check=True)
    for probe in ("YogEffectProbe", *probes):
        subprocess.run([java / "java", "-Xmx256m", "-cp", str(work) + os.pathsep + classpath,
                        "com.shatteredpixel.shatteredpixeldungeon." + probe],
                       cwd=root / "core/src/main/assets", check=True)
