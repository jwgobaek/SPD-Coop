#!/usr/bin/env python3
"""Time both seats' consent, durable publication and both refreshed views across native mine gates."""
from pathlib import Path
from statistics import median
from tempfile import TemporaryDirectory
import math
import runpy
import shutil

root = Path(__file__).resolve().parents[1]
fixture = runpy.run_path(str(root / "scripts/verify-alpha-migration.py"))
with TemporaryDirectory() as directory:
    temp = Path(directory)
    cp = fixture["classpath"](root, temp)
    for mine, seed in (("gnoll", 571), ("crystal", 570)):
        for direction in ("enter", "return"):
            original = temp / (mine + "-" + direction)
            original.mkdir()
            fixture["run"](root, cp, original, "tengu-setup-mine-" + original.name)
            samples = []
            for i in range(7):
                attempt = temp / "sample"
                shutil.copytree(original, attempt)
                fixture["run"](root, cp, attempt, "measure-mine-crossing")
                samples.append(int((attempt / "crossing-micros").read_text()))
                shutil.rmtree(attempt)
            samples.sort()
            print("mine_crossing", mine, "seed", seed, direction, "count", len(samples),
                  "median_us", median(samples), "p95_us", samples[math.ceil(.95 * len(samples)) - 1])
