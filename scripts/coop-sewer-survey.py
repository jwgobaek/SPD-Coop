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


def main(directory, gates_only=False):
    report = output(directory, "SewerGenerationTest", {
        "sampleNativeSewerRegion", "roundTripOrRestoreTheEntryBoundaryOnEachSampledSeed",
    })
    if gates_only:
        samples = re.findall(r"(gate|additional|rare) seed=(\d+) placed_at_stairs=(true|false) "
                             r"outcome=(\w+) restored=(\w+) rng=(\w+) next_action=(\w+) blocker=([^\n]+)", report)
        expected = set(range(402,446)) | {479}
        if {int(seed) for _,seed,*_ in samples} != expected or len(samples)!=len(expected):
            raise ValueError("Incomplete native seed cohorts")
        writer = csv.writer(sys.stdout, delimiter="\t", lineterminator="\n")
        writer.writerow(("cohort", "seed", "start", "placed_at_stairs", "descend", "restored_depth",
                         "ascend", "next_action", "random_state_equal", "unsupported_residents"))
        for cohort,seed,placed,outcome,restored,rng,acted,blocker in sorted(samples,key=lambda row:int(row[1])):
            entered=outcome=="ROUND_TRIP"
            writer.writerow((cohort, seed, "REJECT" if outcome=="REJECT_START" else "PASS", placed,
                             "PASS" if entered else "REJECT" if placed=="true" else "NOT RUN",
                             (2 if entered else 1) if restored=="true" else "NOT RUN",
                             "PASS" if entered else "NOT RUN", "PASS" if acted=="true" else "NOT RUN",
                             "PASS" if rng=="true" else "NOT RUN", blocker))
        return
    wire = output(directory, "HostRestartTest", {"generatedSewersRoundTripAcrossProcessDeath"})
    route = re.search(r"native socket route moves=(\d+) melee=(\d+) searches=(\d+) casts=(\d+)", wire)
    if not route or min(map(int, route.groups())) <= 0 or "mode=verifyfloor" not in wire:
        raise ValueError("Missing completed seed 415 socket route")
    gates = dict(re.findall(r"gate seed=(\d+) placed_at_stairs=true outcome=(ROUND_TRIP|REJECT_FLOOR) restored=true", report))
    engine = output(directory, "CoopFloorsTest", {"walkFromSpawnOnTheNaturalMimicAndStatueSeeds",
                    "walkBetweenGeneratedSewersPreserveDormantWorldAndResume"})
    walks = set(re.findall(r"native sewer seed=(\d+) walk=\d+ attacks=\d+ searches=\d+", engine))
    if not {"408", "411", "415", "416", "420"} <= walks:
        raise ValueError("Missing walk from the native entrance")
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
        route_verified = seed in walks and depth in ("1", "2")
        restored_depth = "2" if gates[seed] == "ROUND_TRIP" else "1"
        writer.writerow((seed, depth, *values, gate, "PASS" if route_verified else "NOT RUN",
                         "PASS" if route_verified or depth == restored_depth else "NOT RUN"))


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("junit", type=Path)
    parser.add_argument("--gates", action="store_true", help="Output re-entry and RNG results for all cohorts")
    args=parser.parse_args()
    main(args.junit,args.gates)
