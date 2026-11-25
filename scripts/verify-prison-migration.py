#!/usr/bin/env python3
"""Resume a schema-3 save produced by the released graphics alpha on a fresh prison-capable host."""
from pathlib import Path
from tempfile import TemporaryDirectory
import runpy
import shutil
import subprocess
import sys

helpers = runpy.run_path(str(Path(__file__).with_name("verify-alpha-migration.py")))


def same_sewer_boundary(old, new):
    assert old["schema"] == new["schema"] == 3
    assert old["session"] == new["session"]
    a, b = old["state"], new["state"]
    for key in ("epoch", "key0", "key1"):
        assert a[key] == b[key], key
    for key in ("players", "random", "turn", "nextItem", "items", "before0", "before1",
                "life0", "life1", "sight0", "sight1", "schedule", "dormant"):
        assert a["engine"][key] == b["engine"][key], key
    for side in ("seat0", "seat1"):
        for key, value in a["sync"][side].items():
            if key != "cursor":
                assert value == b["sync"][side][key], key
    for state in (a, b):
        engine = state["engine"]
        assert engine["world"]["depth"] == 3 and len(engine["dormant"]) == 2
    for key in ("HP", "pos", "id"):
        for field in ("guest", "world"):
            left, right = a["engine"][field], b["engine"][field]
            if field == "world":
                left, right = left["hero"], right["hero"]
            assert left[key] == right[key], key


def main():
    old = Path(sys.argv[1]).resolve()
    new = Path(__file__).resolve().parents[1]
    rev = subprocess.check_output(["git", "rev-parse", "HEAD"], cwd=old, text=True).strip()
    if rev != "2dafb4e8fb1067c1bef3eb049034e1a2d14a6e25":
        raise ValueError("Expected the approved V3 graphics alpha")
    with TemporaryDirectory() as temp:
        tmp = Path(temp)
        original = tmp / "original"
        original.mkdir()
        old_cp = helpers["classpath"](old, tmp)
        helpers["run"](old, old_cp, original, "walk", expected_exit=78)
        (original / "kill-before-reply").unlink()
        helpers["run"](old, old_cp, original, "schema2-to-three")
        saved = helpers["generation"](original)
        resumed = tmp / "resumed"
        shutil.copytree(original, resumed)
        for seat in (0, 1):
            highest = saved["state"]["sync"]["seat" + str(seat)]["highest"]
            (resumed / ("last-seat-" + str(seat))).write_text(str(highest))
        helpers["run"](new, helpers["classpath"](new, tmp), resumed, "legacy-sewer",
                       previous=saved, verify_boundary=same_sewer_boundary)
        assert helpers["generation"](original) == saved
        print("PASS native graphics-alpha schema 3, two parked floors, RNG, ownership, seats, receipts and next input")


if __name__ == "__main__":
    main()
