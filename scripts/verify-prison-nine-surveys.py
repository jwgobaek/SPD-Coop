#!/usr/bin/env python3
"""Run staged native prison stairs and a separate-JVM Tengu level-bundle survey against a checkout."""
from pathlib import Path
import runpy
import subprocess
import sys
import tempfile

tools_dir = Path(__file__).resolve().parent
repo = Path(sys.argv[1]).resolve() if len(sys.argv) > 1 else tools_dir.parent
helpers = runpy.run_path(str(repo / "scripts/verify-alpha-migration.py"))
with tempfile.TemporaryDirectory() as name:
    work = Path(name)
    cp = helpers["classpath"](repo, work)
    java = helpers["JAVA"]
    sources = sorted((tools_dir / "verification").glob("*.java"))
    subprocess.run([str(java.with_name("javac")), "-cp", cp, "-d", name, *map(str, sources)], check=True)
    for main, args in (("PrisonNineSurvey", []), ("TenguBoundaryProbe", ["write", name]),
                       ("TenguBoundaryProbe", ["read", name])):
        command = [str(java), "-Xmx512m", "-cp", name + ":" + cp,
                   "com.shatteredpixel.shatteredpixeldungeon." + main, *args]
        subprocess.run(command, cwd=repo / "core/src/main/assets", check=True, timeout=180)
