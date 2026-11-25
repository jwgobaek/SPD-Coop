#!/usr/bin/env python3
"""Take original merchant custody through floor parking, process death and seller-only redemption."""
from pathlib import Path
from tempfile import TemporaryDirectory
import runpy
import shutil


def custody(engine, identifier):
    indexes = [(engine["world"]["depth"], engine["items"])]
    indexes.extend((int(k), record["items"]) for k, record in engine["dormant"].items())
    matches = [(depth, path, entry) for depth, items in indexes for path, entry in items.items()
               if entry["id"] == identifier]
    assert len(matches) == 1
    return matches[0]


def main():
    tree = Path(__file__).resolve().parents[1]
    runtime = runpy.run_path(str(tree / "scripts/verify-alpha-migration.py"))
    with TemporaryDirectory() as temp:
        work = Path(temp)
        natural = work / "original"
        natural.mkdir()
        cp = runtime["classpath"](tree, work)
        runtime["run"](tree, cp, natural, "prison", client_timeout=280)
        for seat in (0, 1):
            directory = work / str(seat)
            shutil.copytree(natural, directory)

            def run(mode, status=0):
                (directory / "kill-before-reply").unlink(missing_ok=True)
                runtime["run"](tree, cp, directory, mode, expected_exit=status)
                return runtime["generation"](directory)["state"]["engine"]

            bought = run(f"market-buy{seat}", 78)
            ident = int((directory / f"stock{seat}").read_text().split()[1])
            _, path, sale_item = custody(bought, ident)
            assert path.startswith(f"s{seat}_")
            sold = run(f"market-sell{seat}", 78)
            folded = custody(sold, ident)
            assert folded[0] == 6 and folded[1].startswith("shop") and folded[2] == sale_item
            parked = run("seven-travel", 78)
            assert parked["world"]["depth"] == 7 and custody(parked, ident) == folded
            assert sold["world"]["gold"] == parked["world"]["gold"]
            if seat == 0:
                (directory / "measure-seven").touch()
            returned = run("seven-resume")
            if seat == 0:
                print((directory / "perf7").read_text(), flush=True)
            assert returned["world"]["depth"] == 6 and custody(returned, ident) == folded
            redeemed = run(f"market-reclaim{seat}", 78)
            _, path, restored = custody(redeemed, ident)
            assert path.startswith(f"s{seat}_") and restored == sale_item
            run(f"market-finish{seat}")
            print(f"PASS seat={seat} bought, sold, parked at 6, cold-resumed at 7, "
                  "returned and redeemed the same physical item; receipts and gold verified", flush=True)


if __name__ == "__main__":
    main()
