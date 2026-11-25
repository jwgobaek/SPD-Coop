#!/usr/bin/env python3
"""Read the approved pre-Yog schema 13, then promote a copy via two authenticated stair votes."""
import runpy
import shutil
import subprocess
import sys
from pathlib import Path
from tempfile import TemporaryDirectory

root = Path(__file__).resolve().parents[1]
previous = Path(sys.argv[1]).resolve()
assert subprocess.check_output(["git", "rev-parse", "HEAD"], cwd=previous, text=True).strip() == "ce70d083eaa29c31bd704dab9595c2e59da1d1d3"
fixture = runpy.run_path(str(root / "scripts/verify-alpha-migration.py"))
with TemporaryDirectory() as temp:
    work = Path(temp)
    old_cp = fixture["classpath"](previous, work)
    new_cp = fixture["classpath"](root, work)
    original = work / "original"
    original.mkdir()
    run = fixture["run"]
    run(previous, old_cp, original, "tengu-setup-vault-hall24")
    run(previous, old_cp, original, "tengu-commit-vault-hall24", expected_exit=78)
    before = fixture["generation"](original)
    assert before["schema"] == 13
    resumed = work / "copy"
    shutil.copytree(original, resumed)
    run(root, new_cp, resumed, "tengu-read-vault-hall24")
    run(root, new_cp, resumed, "late-down24")
    promoted = fixture["generation"](resumed)
    assert promoted["schema"] == 14 and promoted["state"]["engine"]["world"]["depth"] == 25
    run(root, new_cp, resumed, "late-up25")
    returned = fixture["generation"](resumed)
    assert returned["schema"] == 14 and returned["state"]["engine"]["world"]["depth"] == 24
    for state in (promoted, returned):
        assert before["session"] == state["session"]
        assert before["state"]["engine"]["players"] == state["state"]["engine"]["players"]
        for key in ("epoch", "key0", "key1"):
            assert before["state"][key] == state["state"][key]
    assert before == fixture["generation"](original)
    print("PASS original schema 13 isolated copy, both seats and receipt, TCP 24 to 25 to 24, original generation intact")
    vault = work / "original-vault"
    vault.mkdir()
    run(previous, old_cp, vault, "tengu-setup-vault-enter")
    run(previous, old_cp, vault, "tengu-commit-vault-enter", expected_exit=78)
    old_vault = fixture["generation"](vault)
    assert old_vault["schema"] == 12
    vault_copy = work / "vault-copy"
    shutil.copytree(vault, vault_copy)
    run(root, new_cp, vault_copy, "tengu-read-vault-enter")
    after = fixture["generation"](vault_copy)
    assert after["session"] == old_vault["session"]
    assert after["state"]["engine"]["players"] == old_vault["state"]["engine"]["players"]
    for key in ("key0", "key1", "epoch"):
        assert after["state"][key] == old_vault["state"][key]
    assert fixture["generation"](vault) == old_vault
    print("PASS original schema 12 vault, both seats and enter receipt, next action and original intact")
