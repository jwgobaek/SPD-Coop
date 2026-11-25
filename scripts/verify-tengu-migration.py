#!/usr/bin/env python3
"""Bring a real SYNC19 ninth-floor save to the current host, replay its receipt, then enter floor ten."""
from pathlib import Path
from tempfile import TemporaryDirectory
import runpy
import shutil
import subprocess
import sys

old = Path(sys.argv[1]).resolve()
repo = Path(__file__).resolve().parents[1]
if subprocess.check_output(["git", "rev-parse", "HEAD"], cwd=old, text=True).strip() != "7de79a8b1988e4f9b17532803a77cbb330e0dc98":
    raise ValueError("Expected the approved SYNC19 checkpoint")
tools = runpy.run_path(str(repo / "scripts/verify-alpha-migration.py"))
with TemporaryDirectory() as name:
    work = Path(name)
    original = work / "original"
    original.mkdir()
    previous = tools["classpath"](old, work)
    tools["run"](old, previous, original, "late-setup")
    tools["run"](old, previous, original, "late-down7")
    tools["run"](old, previous, original, "late-down8", expected_exit=78)
    before = tools["generation"](original)
    assert before["schema"] == 6
    current = tools["classpath"](repo, work)
    for mode in ("late-up9", "late-down9"):
        resumed = work / mode
        shutil.copytree(original, resumed)
        (resumed / "kill-before-reply").unlink()
        tools["run"](repo, current, resumed, mode)
        after = tools["generation"](resumed)
        assert after["session"] == before["session"]
        for key in ("epoch", "key0", "key1"):
            assert after["state"][key] == before["state"][key]
        engine = after["state"]["engine"]
        expected = 8 if mode == "late-up9" else 10
        assert after["schema"] == (6 if expected == 8 else 7)
        assert engine["world"]["depth"] == expected
        assert engine["players"] == before["state"]["engine"]["players"]
        if expected == 10:
            assert len(engine["dormant"]) == 9
            assert engine["floor"]["state"] == "START"
    assert tools["generation"](original) == before
    print("PASS original schema 6, both seats, old receipt, nine parked floors, schema 7 entrance and unchanged source")
