#!/usr/bin/env python3
"""Use real host/client processes to interrupt vault rewards and promote a genuine schema 11 save."""
from pathlib import Path
from tempfile import TemporaryDirectory
import os
import runpy
import shutil
import subprocess
import sys

root = Path(__file__).resolve().parents[1]
previous = Path(sys.argv[1]).resolve()
expected = "960e4bc9ad4b94fdb4de7959c41c17827066ff39"
assert subprocess.check_output(["git", "rev-parse", "HEAD"], cwd=previous, text=True).strip() == expected
fixture = runpy.run_path(str(root / "scripts/verify-alpha-migration.py"))
run = fixture["run"]
generation = fixture["generation"]

with TemporaryDirectory() as name:
    work = Path(name)
    current = fixture["classpath"](root, work)
    old = fixture["classpath"](previous, work)
    os.environ["JAVA_TOOL_OPTIONS"] = os.environ.get("JAVA_TOOL_OPTIONS", "") + " -Dspd.measureVault=true"
    for kind in ("pick", "full", "enter", "hall24"):
        path = work / kind
        path.mkdir()
        run(root, current, path, "tengu-setup-vault-" + kind)
        run(root, current, path, "tengu-commit-vault-" + kind, expected_exit=78)
        published = generation(path)
        run(root, current, path, "tengu-read-vault-" + kind)
        assert published["schema"] == (13 if kind == "hall24" else 12)
        for report in path.glob("perf*"):
            print(kind, report.read_text())
        print("PASS post-HEAD receipt", kind)

    source = work / "schema11"
    source.mkdir()
    run(previous, old, source, "tengu-setup-king-claim")
    run(previous, old, source, "tengu-commit-king-claim", expected_exit=78)
    original = generation(source)
    assert original["schema"] == 11
    copied = work / "promoted"
    shutil.copytree(source, copied)
    run(root, current, copied, "tengu-read-king-claim")
    run(root, current, copied, "late-down20")
    promoted = generation(copied)
    assert promoted["schema"] == 13 and promoted["state"]["engine"]["world"]["depth"] == 21
    assert original["session"] == promoted["session"]
    for key in ("key0", "key1", "epoch"):
        assert original["state"][key] == promoted["state"][key]
    assert original["state"]["engine"]["players"] == promoted["state"]["engine"]["players"]
    assert generation(source) == original
    print("PASS unchanged schema 11 source, existing seats and receipt, native 20 to 21 crossing into schema 13")
