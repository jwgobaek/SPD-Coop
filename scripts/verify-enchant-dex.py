#!/usr/bin/env python3
"""Check the Android verifier regression in the compiled scroll selector."""
import argparse
import os
import re
import subprocess
import tempfile
import zipfile
from pathlib import Path

CLASS = "Lcom/shatteredpixel/shatteredpixeldungeon/CoopEnchantScroll;"
ARRAY = "[Lcom/watabou/utils/Bundlable;"


def check(apk, dexdump):
    with tempfile.TemporaryDirectory() as directory, zipfile.ZipFile(apk) as archive:
        for name in archive.namelist():
            if not re.fullmatch(r"classes(?:[0-9]+)?\.dex", name):
                continue
            path = Path(archive.extract(name, directory))
            if CLASS.encode() not in path.read_bytes():
                continue
            with subprocess.Popen([dexdump, "-d", path], stdout=subprocess.PIPE,
                                  text=True, errors="replace") as proc:
                target = method = False
                previous = ""
                stores = 0
                for line in proc.stdout:
                    if "Class descriptor" in line:
                        target = "'" + CLASS + "'" in line
                        method = False
                    if target and re.match(r"\s*name\s*:", line):
                        method = "'select'" in line
                        previous = ""
                    if not method or "|" not in line:
                        continue
                    instruction = line.split("|", 1)[1].strip()
                    if "iput-object" in instruction and ".options:" + ARRAY in instruction:
                        register = instruction.split("iput-object ", 1)[1].split(",")[0]
                        if "check-cast " + register + ", " + ARRAY not in previous:
                            raise ValueError(f"{name}: scroll option store lacks the tested typed array boundary")
                        stores += 1
                    previous = instruction
                if proc.wait():
                    raise ValueError(f"dexdump failed for {name}")
                if stores:
                    print(f"PASS {name}: verified {stores} typed selector array store")
                    return
    raise ValueError("Compiled scroll selector not found")


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("apk", type=Path)
    args = parser.parse_args()
    check(args.apk, Path(os.environ["ANDROID_HOME"]) / "build-tools/36.0.0/dexdump")
