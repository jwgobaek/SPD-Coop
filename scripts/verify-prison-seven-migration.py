#!/usr/bin/env python3
"""Read the approved six-floor host's real schema-4 generation, then persist a seventh floor."""
from pathlib import Path
from tempfile import TemporaryDirectory
import runpy
import shutil
import subprocess
import sys

helper = runpy.run_path(str(Path(__file__).with_name("verify-alpha-migration.py")))


def boundary(old, new):
    assert old["schema"] == new["schema"] == 4 and old["session"] == new["session"]
    a, b = old["state"], new["state"]
    for key in ("epoch", "key0", "key1"):
        assert a[key] == b[key], key
    for key in ("players", "random", "turn", "nextItem", "items", "before0", "before1",
                "life0", "life1", "sight0", "sight1", "schedule", "dormant"):
        assert a["engine"][key] == b["engine"][key], key
    assert a["engine"]["world"]["gold"] == b["engine"]["world"]["gold"]
    for seat in ("seat0", "seat1"):
        for key, value in a["sync"][seat].items():
            if key != "cursor":
                assert value == b["sync"][seat][key], key


def main(old):
    old = Path(old).resolve()
    new = Path(__file__).resolve().parents[1]
    revision = subprocess.check_output(["git", "rev-parse", "HEAD"], cwd=old, text=True).strip()
    if revision != "9b6130fd46aff4e99317ab1ce396c96bd4faac0b":
        raise ValueError("Expected approved H6 development checkpoint")
    with TemporaryDirectory() as temp:
        temp = Path(temp)
        original = temp / "original"
        original.mkdir()
        helper["run"](old, helper["classpath"](old, temp), original, "prison", client_timeout=240)
        saved = helper["generation"](original)
        resumed = temp / "resumed"
        shutil.copytree(original, resumed)
        cp = helper["classpath"](new, temp)
        # First restore has no position fixture. Check the frozen boundary before the client binds.
        helper["run"](new, cp, resumed, "legacy-prison", previous=saved, verify_boundary=boundary)
        helper["run"](new, cp, resumed, "seven-travel", expected_exit=78)
        assert helper["generation"](resumed)["schema"] == 5
        (resumed / "kill-before-reply").unlink()
        helper["run"](new, cp, resumed, "seven-resume")
        helper["run"](new, cp, resumed, "seven-return")
        assert helper["generation"](original) == saved
        print("PASS original H6 schema 4, both credentials, world, RNG, all parked floors and receipts; schema 5 on travel, lost reply and return")


if __name__ == "__main__":
    main(sys.argv[1])
