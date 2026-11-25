#!/usr/bin/env python3
"""Resume the previous 7F build's real save in a fresh SYNC18 host and client."""
from pathlib import Path
from tempfile import TemporaryDirectory
import runpy
import shutil
import subprocess
import sys


def main(previous):
    old = Path(previous).resolve()
    current = Path(__file__).resolve().parents[1]
    revision = subprocess.check_output(["git", "rev-parse", "HEAD"], cwd=old, text=True).strip()
    if revision != "ef888933a692016cc43f42daeb2355e8fe326d77":
        raise ValueError("Expected the approved SYNC16 prison checkpoint")
    runtime = runpy.run_path(str(current / "scripts/verify-alpha-migration.py"))
    with TemporaryDirectory() as temporary:
        work = Path(temporary)
        original = work / "original"
        original.mkdir()
        old_classes = runtime["classpath"](old, work)
        runtime["run"](old, old_classes, original, "prison", client_timeout=300)
        runtime["run"](old, old_classes, original, "seven-travel", expected_exit=78)
        saved = runtime["generation"](original)
        assert saved["schema"] == 5
        resumed = work / "resumed"
        shutil.copytree(original, resumed)
        (resumed / "kill-before-reply").unlink(missing_ok=True)
        new_classes = runtime["classpath"](current, work)
        runtime["run"](current, new_classes, resumed, "seven-resume", client_timeout=120)
        runtime["run"](current, new_classes, resumed, "seven-return", client_timeout=120)
        after = runtime["generation"](resumed)
        assert after["schema"] == 5 and saved["session"] == after["session"]
        for key in ("epoch", "key0", "key1"):
            assert saved["state"][key] == after["state"][key], key
        assert runtime["generation"](original) == saved
        print("PASS original schema 5, both returning seats, travel receipt replay and floor-6 action")


if __name__ == "__main__":
    main(sys.argv[1])
