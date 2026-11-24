#!/usr/bin/env python3
"""Use a genuine schema-2 durable floor crossing and the new client, host and receipt ledger."""
from pathlib import Path
from tempfile import TemporaryDirectory
import runpy
import shutil
import subprocess
import sys

lib = runpy.run_path(str(Path(__file__).with_name("verify-alpha-migration.py")))
old = Path(sys.argv[1]).resolve()
new = Path(__file__).resolve().parents[1]
if subprocess.check_output(["git", "rev-parse", "HEAD"], cwd=old, text=True).strip() != "9b6f58a59e190b761f872a3c6f55156dd9bb7617":
    raise ValueError("Expected the pre-depths development checkpoint")
with TemporaryDirectory() as tmp:
    tmp = Path(tmp)
    original = tmp / "original"
    original.mkdir()
    lib["run"](old, lib["classpath"](old, tmp), original, "walk", expected_exit=78)
    before = lib["generation"](original)
    assert before["schema"] == 2
    resumed = tmp / "resumed"
    shutil.copytree(original, resumed)
    (resumed / "kill-before-reply").unlink()
    lib["run"](new, lib["classpath"](new, tmp), resumed, "schema2-to-three")
    after = lib["generation"](resumed)
    assert after["schema"] == 3 and before["session"] == after["session"]
    a, b = before["state"], after["state"]
    for field in ("epoch", "key0", "key1"):
        assert a[field] == b[field]
    assert a["engine"]["players"] == b["engine"]["players"]
    assert a["engine"]["parked"] == b["engine"]["dormant"]["1"]
    assert b["engine"]["world"]["depth"] == 3
    assert a["engine"]["guest"]["id"] == b["engine"]["guest"]["id"]
    assert a["engine"]["world"]["hero"]["id"] == b["engine"]["world"]["hero"]["id"]
    assert lib["generation"](original) == before
    print("PASS schema 2 to 3, authenticated seats, confirmed receipt, two native dormant floors and intact original")
