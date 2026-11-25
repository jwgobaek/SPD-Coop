#!/usr/bin/env python3
"""Migrate an authentic SYNC18 seventh-floor checkpoint; authenticate both seats through floor nine."""
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
    if revision != "b3579c7f4e0ff226aa1b31f3c8e28c3677bf21f4":
        raise ValueError("Expected the approved SYNC18 prison checkpoint")
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
        # The legacy receipt scenario finishes with an ascent. Exercise it on a separate copy.
        replay = work / "old-receipt"
        shutil.copytree(resumed, replay)
        runtime["run"](current, new_classes, replay, "seven-resume", client_timeout=120)
        runtime["run"](current, new_classes, resumed, "late-down7")
        runtime["run"](current, new_classes, resumed, "late-down8", expected_exit=78)
        (resumed / "kill-before-reply").unlink()
        current_nine = runtime["generation"](resumed)
        assert current_nine["schema"] == 6 and current_nine["state"]["engine"]["world"]["depth"] == 9
        runtime["run"](current, new_classes, resumed, "late-up9")
        print((resumed / "perf9").read_text(), flush=True)
        runtime["run"](current, new_classes, resumed, "late-up8")
        after = runtime["generation"](resumed)
        assert after["schema"] == 6 and saved["session"] == after["session"]
        for key in ("epoch", "key0", "key1"):
            assert saved["state"][key] == after["state"][key], key
        assert runtime["generation"](original) == saved
        print("PASS original schema 5, both returning seats, floor 9 replay after process death, floor 7 action, unchanged source")


if __name__ == "__main__":
    main(sys.argv[1])
