#!/usr/bin/env python3
"""Restore a real SYNC25 host's schema-15 victory with two SYNC26 TCP seats."""
from pathlib import Path
from tempfile import TemporaryDirectory
import runpy
import shutil
import subprocess
import sys

root = Path(__file__).resolve().parents[1]
if len(sys.argv) != 2:
    sys.exit("Supply the checked-out ecccda0 APK source directory")
old = Path(sys.argv[1]).resolve()
if subprocess.check_output(["git", "rev-parse", "HEAD"], cwd=old, text=True).strip() != "ecccda0e62aac4b68e6ae1f33bcd5e98ef882fa9":
    sys.exit("Expected the original vault-ascent APK source")
fixture = runpy.run_path(str(root / "scripts/verify-alpha-migration.py"))
with TemporaryDirectory() as work:
    temp = Path(work)
    original = temp / "original"
    original.mkdir()
    old_cp = fixture["classpath"](old, temp)
    new_cp = fixture["classpath"](root, temp)
    run = fixture["run"]
    run(old, old_cp, original, "tengu-setup-ascent-surface")
    run(old, old_cp, original, "tengu-commit-ascent-surface", expected_exit=78)
    source = fixture["generation"](original)
    assert source["schema"] == 15
    copy = temp / "copy"
    shutil.copytree(original, copy)
    run(root, new_cp, copy, "tengu-read-ascent-surface")
    assert fixture["generation"](original) == source
    assert fixture["generation"](copy)["session"] == source["session"]
    print("PASS original schema 15, Ascension victory, both seats, identical receipt and untouched source")
