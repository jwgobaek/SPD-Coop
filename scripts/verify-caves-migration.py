#!/usr/bin/env python3
"""Bring a real SYNC20 eleventh-floor save to the current host, replay its receipt, then enter floor twelve."""
from pathlib import Path
from tempfile import TemporaryDirectory
import runpy
import shutil
import subprocess
import sys

old = Path(sys.argv[1]).resolve()
repo = Path(__file__).resolve().parents[1]
if subprocess.check_output(["git", "rev-parse", "HEAD"], cwd=old, text=True).strip() != "76ed0506c9589c17dcbdaaca7b78a886f3fb0e92":
    raise ValueError("Expected the approved SYNC20 checkpoint")
tools = runpy.run_path(str(repo / "scripts/verify-alpha-migration.py"))
with TemporaryDirectory() as name:
    work = Path(name)
    original = work / "original"
    original.mkdir()
    previous = tools["classpath"](old, work)
    tools["run"](old, previous, original, "tengu-setup-caves")
    tools["run"](old, previous, original, "tengu-commit-caves", expected_exit=78)
    before = tools["generation"](original)
    assert before["schema"] == 7
    current = tools["classpath"](repo, work)
    resumed = work / "restored"
    shutil.copytree(original, resumed)
    (resumed / "kill-before-reply").unlink()
    tools["run"](repo, current, resumed, "tengu-read-caves")
    tools["run"](repo, current, resumed, "late-down11")
    after = tools["generation"](resumed)
    assert after["schema"] == 8 and after["session"] == before["session"]
    for key in ("epoch", "key0", "key1"):
        assert after["state"][key] == before["state"][key]
    engine = after["state"]["engine"]
    assert engine["world"]["depth"] == 12 and len(engine["dormant"]) == 11
    assert engine["players"] == before["state"]["engine"]["players"]
    assert tools["generation"](original) == before
    print("PASS original schema 7, two seats, original receipt, native 11 to 12, eleven parked floors and untouched source")
