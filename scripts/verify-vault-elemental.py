#!/usr/bin/env python3
"""Compare uninterrupted native draws against clean JVMs, then exercise durable TCP receipts."""
from pathlib import Path
from tempfile import TemporaryDirectory
import os
import runpy
import subprocess
import sys

root = Path(__file__).resolve().parents[1]
fixture = runpy.run_path(str(root / "scripts/verify-alpha-migration.py"))
with TemporaryDirectory() as tmp:
    work = Path(tmp)
    cp = fixture["classpath"](root, work)
    stages = sys.argv[1:] or ["replay", "beacon-host", "beacon-guest", "spawn", "FIRE", "FROST", "SHOCK", "environment", "warning", "kill", "statue", "escape",
                               "ascent-offer", "ascent-agree", "ascent-24", "ascent-damage", "ascent-kill", "ascent-down", "ascent-revive", "ascent-20", "ascent-1", "ascent-surface"]
    for stage in stages:
        if stage == "replay":
            for form in ("write", "FIRE", "FROST", "SHOCK"):
                subprocess.run([os.environ["JAVA_HOME"]+"/bin/java", "-Xmx256m", "-cp", cp,
                                "com.shatteredpixel.shatteredpixeldungeon.VaultElementalTest", form, tmp],
                               cwd=root/"core/src/main/assets", check=True)
            continue
        room = work/stage
        room.mkdir()
        for op, code in (("setup", 0), ("commit", 78), ("read", 0)):
            try:
                fixture["run"](root, cp, room, "tengu-"+op+"-"+(stage if stage.startswith("ascent-") else "elemental-"+stage), expected_exit=code)
            except Exception:
                print((room/"host.log").read_text()[-4000:], file=sys.stderr)
                raise
        for perf in room.glob("perf*"):
            print(perf.read_text(), flush=True)
        size = max(p.stat().st_size for p in room.glob("coop-saves-v1/*/g*") if p.is_file())
        print("PASS", stage, "durable HEAD, reply lost, both seats authenticated and identical receipt; generation bytes", size, flush=True)
