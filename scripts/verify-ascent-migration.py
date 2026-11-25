#!/usr/bin/env python3
"""Generate a pre-ascent checkpoint in its own JVM and import it in the current engine."""
import hashlib
import os
from pathlib import Path
import runpy
import subprocess
import sys
from tempfile import TemporaryDirectory

root = Path(__file__).resolve().parents[1]
old = Path(sys.argv[1]).resolve()
baseline = "f239c06a638b20f2729579f5ee7a4c72f7aa911c"
if subprocess.check_output(["git", "rev-parse", "HEAD"], cwd=old, text=True).strip() != baseline:
    sys.exit("Supply the approved f239c06 checkout with compiled test classes")
classpath = runpy.run_path(str(root / "scripts/verify-alpha-migration.py"))["classpath"]
with TemporaryDirectory() as directory:
    work = Path(directory)
    for tree, probe, argument in ((old, "YogArenaTest", work), (root, "PartyAscentTest", work / "yog-amulet")):
        if tree == root:
            before = hashlib.sha256(argument.read_bytes()).digest()
        subprocess.run([os.environ["JAVA_HOME"]+"/bin/java", "-Xmx384m", "-cp", classpath(tree,work),
                        "com.shatteredpixel.shatteredpixeldungeon."+probe, str(argument)],
                       cwd=tree/"core/src/main/assets", check=True)
    assert hashlib.sha256(argument.read_bytes()).digest() == before
    print("PASS previous generation intact")
