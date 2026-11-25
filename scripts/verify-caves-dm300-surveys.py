#!/usr/bin/env python3
"""Run the original mine/city generation, caves combat and Linux socket lifetime probes."""
from pathlib import Path
import runpy
import subprocess
import sys
import tempfile


def main():
    root = Path(__file__).resolve().parents[1]
    tree = Path(sys.argv[1]).resolve() if len(sys.argv) > 1 else root
    tools = runpy.run_path(str(root / "scripts/verify-alpha-migration.py"))
    java = tools["JAVA"]
    with tempfile.TemporaryDirectory() as directory:
        work = Path(directory)
        classpath = tools["classpath"](tree, work)
        probes = ("MiningBranchSurvey", "CavesBoundaryProbe", "CavesInteriorSurvey", "LanResourceProbe")
        sources = [root / "scripts/verification" / (name + ".java") for name in probes]
        subprocess.run([str(java.with_name("javac")), "-cp", classpath, "-d", str(work), *map(str, sources)], check=True)
        for name in probes:
            subprocess.run([str(java), "-Xmx512m", "-cp", str(work) + ":" + classpath,
                            "com.shatteredpixel.shatteredpixeldungeon." + name],
                           cwd=tree / "core/src/main/assets", check=True, timeout=240)


if __name__ == "__main__":
    main()
