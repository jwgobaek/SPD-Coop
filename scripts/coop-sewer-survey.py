#!/usr/bin/env python3
"""Print the sewer survey from an executed JUnit report directory."""
import argparse
import csv
from pathlib import Path
import re
import sys
from xml.etree import ElementTree


def output(directory, suite, cases):
    matches = list(directory.glob(f"TEST-*.{suite}.xml"))
    if len(matches) != 1:
        raise ValueError(f"Expected one {suite} report")
    root = ElementTree.parse(matches[0]).getroot()
    executed = {case.attrib["name"].split("(")[0] for case in root.findall("testcase")}
    if any(int(root.get(key, 0)) for key in ("errors", "failures", "skipped")) or not cases <= executed:
        raise ValueError(f"Expected passing {suite} cases {sorted(cases)}")
    return root.findtext("system-out", "")


def main(directory):
    report = output(directory, "SewerGenerationTest", {
        "sampleNativeSewerRegion", "roundTripOrRestoreTheEntryBoundaryOnEachSampledSeed",
    })
    wire = output(directory, "HostRestartTest", {"generatedSewersRoundTripAcrossProcessDeath"})
    route = re.search(r"native socket route moves=(\d+) melee=(\d+) searches=(\d+) casts=(\d+)", wire)
    if not route or min(map(int, route.groups())) <= 0 or "mode=verifyfloor" not in wire:
        raise ValueError("Missing completed seed 415 socket route")
    gates = dict(re.findall(r"gate seed=(\d+) placed_at_stairs=true outcome=(ROUND_TRIP|REJECT_FLOOR) restored=true", report))
    levels = re.findall(r"generation seed=(\d+) depth=(\d+) actors=\[(.*?)\] rooms=\[(.*?)\] traps=\[(.*?)\] "
                        r"mobs=(\d+) level_json_bytes=(\d+)", report)
    keys = {(seed, depth) for seed, depth, *_ in levels}
    expected = {(str(seed), str(depth)) for seed in range(402, 424) for depth in range(1, 6)}
    if keys != expected or len(levels) != len(expected) or set(gates) != {seed for seed, _ in expected}:
        raise ValueError("Incomplete generation or staircase sample")
    writer = csv.writer(sys.stdout, delimiter="\t", lineterminator="\n")
    writer.writerow(("seed", "depth", "actor_classes", "special_rooms", "trap_classes", "initial_mob_count",
                     "level_bundle_utf8_bytes", "placed_gate_result", "normal_route", "restored_at_this_depth"))
    for seed, depth, *values in levels:
        gate = {"1": "START", "2": gates[seed]}.get(depth, "NOT RUN")
        route_verified = seed == "415" and depth in ("1", "2")
        restored_depth = "2" if gates[seed] == "ROUND_TRIP" else "1"
        writer.writerow((seed, depth, *values, gate, "PASS" if route_verified else "NOT RUN",
                         "PASS" if route_verified or depth == restored_depth else "NOT RUN"))


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("junit", type=Path)
    main(parser.parse_args().junit)
