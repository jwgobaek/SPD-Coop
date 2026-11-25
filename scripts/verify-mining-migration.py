#!/usr/bin/env python3
"""Restore two original caves generations on the mining host and replay both seats' receipts."""
from pathlib import Path
import runpy
import shutil
import subprocess
import sys
from tempfile import TemporaryDirectory

previous = Path(sys.argv[1]).resolve()
root = Path(__file__).resolve().parents[1]
assert subprocess.check_output(["git","rev-parse","HEAD"],cwd=previous,text=True).strip() == "df645fc08d0b33dbd5e2d7a847f075dc26d4b020"
tools = runpy.run_path(str(root/"scripts/verify-alpha-migration.py"))
with TemporaryDirectory() as directory:
    work = Path(directory)
    old_cp = tools["classpath"](previous,work)
    new_cp = tools["classpath"](root,work)
    for schema,mode in ((8,"interior-13"), (9,"dm-visit")):
        original = work/str(schema)
        original.mkdir()
        tools["run"](previous,old_cp,original,"tengu-setup-"+mode)
        tools["run"](previous,old_cp,original,"tengu-commit-"+mode,expected_exit=78)
        before=tools["generation"](original)
        assert before["schema"]==schema
        restored=work/("resume-"+str(schema))
        shutil.copytree(original,restored)
        tools["run"](root,new_cp,restored,"tengu-read-"+mode)
        after=tools["generation"](restored)
        assert after["schema"]==schema and after["session"]==before["session"]
        for key in ("epoch","key0","key1"):
            assert after["state"][key]==before["state"][key]
        assert after["state"]["engine"]["players"]==before["state"]["engine"]["players"]
        assert tools["generation"](original)==before
        print(f"PASS original schema {schema}, fresh host, both seats, committed receipt, next action and untouched original")
