#!/usr/bin/env python3
"""Resume an original schema-10 city save and measure the restored native king arena."""
import os
from pathlib import Path
import runpy
import shutil
import subprocess
import sys
from tempfile import TemporaryDirectory

root = Path(__file__).resolve().parents[1]
old = Path(sys.argv[1]).resolve()
assert subprocess.check_output(["git", "rev-parse", "HEAD"], cwd=old, text=True).strip() == "555688c6da5bb803c9280393295175e49014157e"
fixture = runpy.run_path(str(root / "scripts/verify-alpha-migration.py"))
with TemporaryDirectory() as folder:
    tmp = Path(folder)
    current = fixture["classpath"](root, tmp)
    legacy = fixture["classpath"](old, tmp)
    os.environ["JAVA_TOOL_OPTIONS"] = os.environ.get("JAVA_TOOL_OPTIONS", "") + " -Dspd.measureKing=true"
    for boundary, tree, cp, schema in (
        ("city-19", old, legacy, 10),
        ("king-shield", root, current, 11),
    ):
        original = tmp / boundary
        original.mkdir()
        run = fixture["run"]
        run(tree, cp, original, "tengu-setup-" + boundary)
        run(tree, cp, original, "tengu-commit-" + boundary, expected_exit=78)
        before = fixture["generation"](original)
        assert before["schema"] == schema
        copied = tmp / ("restored-" + boundary)
        shutil.copytree(original, copied)
        run(root, current, copied, "tengu-read-" + boundary)
        after = fixture["generation"](copied)
        for key in ("epoch", "key0", "key1"):
            assert before["state"][key] == after["state"][key]
        assert before["session"] == after["session"]
        assert before["state"]["engine"]["players"] == after["state"]["engine"]["players"]
        assert fixture["generation"](original) == before
        print("PASS schema", schema, "receipt and next actions from fresh authenticated JVM")
        print((copied / ("perf19" if schema == 10 else "perf20")).read_text())
        if schema == 10:
            promoted = tmp / "promotion"
            shutil.copytree(original, promoted)
            run(root, current, promoted, "late-down19")
            arrived = fixture["generation"](promoted)
            assert arrived["schema"] == 11 and arrived["session"] == before["session"]
            assert arrived["state"]["engine"]["world"]["depth"] == 20
            for key in ("epoch", "key0", "key1"):
                assert arrived["state"][key] == before["state"][key]
            assert arrived["state"]["engine"]["players"] == before["state"]["engine"]["players"]
            run(root, current, promoted, "late-up20")
            returned = fixture["generation"](promoted)
            assert returned["schema"] == 11 and returned["state"]["engine"]["world"]["depth"] == 19
            assert fixture["generation"](original) == before
            print("PASS original schema 10 promoted to 11 on committed TCP crossing, both seats, fresh JVM return and next action")
