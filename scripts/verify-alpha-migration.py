#!/usr/bin/env python3
"""Exercise the real pre-migration game and the current host in temporary private profiles."""
import hashlib
import json
import os
from pathlib import Path
import shutil
import struct
import subprocess
import sys
import tempfile
import time

BASE = "92ebf943ceb40fe0c63f149f6be9f6ec3e71fa8d"
PACKAGE = "com.shatteredpixel.shatteredpixeldungeon.lan."
JAVA = Path(os.environ["JAVA_HOME"]) / "bin/java"

def classpath(tree, tmp):
    init = tmp / "classpath.gradle"
    init.write_text("""gradle.projectsEvaluated {
        def p = gradle.rootProject.project(':core')
        p.tasks.register('coopTestClasspath') {
            doLast { println p.sourceSets.test.runtimeClasspath.asPath }
        }
    }""")
    args = [str(tree / "gradlew"), "-q", "--offline", "-I", str(init), ":core:testClasses",
            ":SPD-classes:jar", ":services:jar", ":core:coopTestClasspath"]
    return subprocess.check_output(args, cwd=tree, text=True).splitlines()[-1]

def generation(base):
    directory, = (base / "coop-saves-v1").iterdir()
    number, = struct.unpack(">q", (directory / "HEAD").read_bytes()[:8])
    image = (directory / f"g{number}").read_bytes()
    assert len(image) == 36 + int.from_bytes(image[:4], "big")
    assert hashlib.sha256(image[36:]).digest() == image[4:36]
    return json.loads(image[36:])

def same_boundary(old, new):
    assert old["schema"] == 1 and new["schema"] == 2 and old["session"] == new["session"]
    a, b = old["state"], new["state"]
    for key in ("epoch", "key0", "key1"):
        assert a[key] == b[key], key
    for key in ("players", "random", "turn", "nextItem", "items", "before0", "before1", "life0",
                "life1", "sight0", "sight1", "schedule"):
        assert a["engine"][key] == b["engine"][key], key
    for key in ("HP", "pos", "id"):
        for left, right in ((a["engine"]["guest"], b["engine"]["guest"]),
                            (a["engine"]["world"]["hero"], b["engine"]["world"]["hero"])):
            assert left[key] == right[key], key
    for seat in ("seat0", "seat1"):
        for key, value in a["sync"][seat].items():
            if key != "cursor":
                assert value == b["sync"][seat][key], key

def run(tree, cp, base, mode, previous=None, expected_exit=0, verify_boundary=same_boundary, client_timeout=60):
    assets = tree / "core/src/main/assets"
    prefix = [str(JAVA), "-Xmx256m", "-cp", cp]
    for name in ("endpoint", "stop"):
        (base / name).unlink(missing_ok=True)
    with (base / "host.log").open("w") as log:
        host = subprocess.Popen(prefix + [PACKAGE + "RestartHost", str(base), mode],
                                cwd=assets, stdout=log, stderr=subprocess.STDOUT)
        try:
            end = time.monotonic() + 20
            while not (base / "endpoint").exists():
                if host.poll() is not None or time.monotonic() > end:
                    raise RuntimeError("Host did not open the test room")
                time.sleep(0.02)
            if previous is not None:
                verify_boundary(previous, generation(base))
            subprocess.run(prefix + [PACKAGE + "RestartClient", str(base), mode],
                           cwd=assets, check=True, timeout=client_timeout)
            (base / "stop").touch()
            if host.wait(15) != expected_exit:
                raise RuntimeError("Host did not save and exit")
        finally:
            if host.poll() is None:
                host.kill()
                host.wait()

def main():
    old = Path(sys.argv[1]).resolve()
    new = Path(__file__).resolve().parent.parent
    if subprocess.check_output(["git", "rev-parse", "HEAD"], cwd=old, text=True).strip() != BASE:
        raise ValueError("Expected the approved pre-migration alpha checkout")
    with tempfile.TemporaryDirectory(prefix="coop-migration-") as folder:
        tmp = Path(folder)
        original = tmp / "original"
        original.mkdir()
        run(old, classpath(old, tmp), original, "new")
        saved = generation(original)
        resumed = tmp / "resumed"
        shutil.copytree(original, resumed)
        (resumed / "legacy-id").write_text(str(saved["state"]["sync"]["seat0"]["highest"]))
        run(new, classpath(new, tmp), resumed, "migrate", saved)
        assert generation(original) == saved
        print("PASS schema 1 to 2, original seats, RNG, exploration, ownership, receipts, revival and untouched source generation")

if __name__ == "__main__":
    main()
