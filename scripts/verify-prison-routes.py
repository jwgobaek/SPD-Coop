#!/usr/bin/env python3
"""Replay both approved normal-start seeds through their next prison outcome over TCP."""
from pathlib import Path
from tempfile import TemporaryDirectory
import runpy


def main():
    tree = Path(__file__).resolve().parents[1]
    runtime = runpy.run_path(str(tree / "scripts/verify-alpha-migration.py"))
    with TemporaryDirectory() as temporary:
        work = Path(temporary)
        classes = runtime["classpath"](tree, work)
        for seed in (581, 1105):
            directory = work / str(seed)
            directory.mkdir()
            runtime["run"](tree, classes, directory, f"journey-seven-{seed}", client_timeout=280)


if __name__ == "__main__":
    main()
