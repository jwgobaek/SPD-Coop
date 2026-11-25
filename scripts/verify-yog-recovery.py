#!/usr/bin/env python3
"""Crash after durable publication, authenticate both seats again and replay the same command."""
import runpy
import sys
from pathlib import Path
from tempfile import TemporaryDirectory

root = Path(__file__).resolve().parents[1]
fixture = runpy.run_path(str(root / "scripts/verify-alpha-migration.py"))
with TemporaryDirectory() as work:
    parent = Path(work)
    cp = fixture["classpath"](root, parent)
    for boundary in sys.argv[1:] or ("warning", "first", "ray", "regular", "first-death", "second", "second-death", "third", "phase5", "down", "revive", "kill", "last", "amulet", "propose", "win", "return25", "revisit26", "mine-return", "branch-capacity"):
        room = parent / boundary
        room.mkdir()
        try:
            for operation, status in (("setup", 0), ("commit", 78), ("read", 0)):
                fixture["run"](root, cp, room, "tengu-" + operation + "-yog-" + boundary, expected_exit=status)
                if operation == "commit":
                    image = fixture["generation"](room)
                    assert image["schema"] == 14
                    generation_size = max(f.stat().st_size for f in room.glob("coop-saves-v1/*/g*") if f.is_file())
            for report in room.glob("perf*"):
                print(report.read_text(), flush=True)
            print("PASS", boundary, "HEAD published, reply lost, two seats reauthenticated, identical receipt; generation bytes", generation_size, flush=True)
        except Exception:
            print((room / "host.log").read_text()[-2000:], file=sys.stderr)
            raise
