#!/usr/bin/env python3
"""Check the final APK and its combined backup locally without writing to Drive."""
import hashlib
import re
import subprocess
import sys
import tarfile
import zipfile
from pathlib import Path, PurePosixPath

APK = "SPD-Coop-latest.apk"
BACKUP = "SPD-Coop-latest-backup.zip"
EXCLUDE = {".apk", ".apks", ".aab", ".zip", ".keystore", ".jks", ".p12", ".pem", ".key"}
PRIVATE_DIRECTORIES = {".git", ".gradle", "coop-saves-v1", "seat-credentials"}


def allowed(name):
    path = PurePosixPath(name)
    return (not path.is_absolute() and ".." not in path.parts
            and path.suffix.lower() not in EXCLUDE
            and not PRIVATE_DIRECTORIES.intersection(path.parts))


def digest(stream):
    return hashlib.file_digest(stream, "sha256").hexdigest()


def verify(directory, commit):
    if not re.fullmatch("[a-f0-9]{40}", commit):
        raise ValueError("Supply the full APK source commit")
    repo = Path(__file__).resolve().parents[1]
    with (directory / APK).open("rb") as apk, zipfile.ZipFile(directory / BACKUP) as bundle:
        names = bundle.namelist()
        if len(names) != len(set(names)) or any(not allowed(name) for name in names):
            raise ValueError("Unexpected archive member or duplicate path")
        required = {"BUILD-INFO.txt", "SHA256SUMS.txt", "source/LICENSE.txt", "source/gradlew",
                    "source/gradle/wrapper/gradle-wrapper.jar", "evidence/build.log", "evidence/package-signature.txt"}
        if not required.issubset(names) or not any(n.startswith("evidence/junit/TEST-") for n in names):
            raise ValueError("Missing build evidence or source material")
        entries = bundle.read("SHA256SUMS.txt").decode().splitlines()
        hashes = {}
        for line in entries:
            expected, name = line.split("  ", 1)
            if not re.fullmatch("[a-f0-9]{64}", expected) or name in hashes:
                raise ValueError("Invalid or duplicate checksum")
            hashes[name] = expected
        if set(hashes) != (set(names) - {"SHA256SUMS.txt"}) | {APK} or hashes[APK] != digest(apk):
            raise ValueError("Checksum inventory or APK mismatch")
        for name in names:
            if name != "SHA256SUMS.txt":
                with bundle.open(name) as member:
                    if digest(member) != hashes[name]:
                        raise ValueError(f"Checksum mismatch for {name}")

        actual_source = {name for name in names if name.startswith("source/")}
        expected_source = set()
        with subprocess.Popen(["git", "archive", "--format=tar", commit],
                              cwd=repo, stdout=subprocess.PIPE) as git:
            with tarfile.open(fileobj=git.stdout, mode="r|") as tree:
                for member in tree:
                    if not member.isfile() or not allowed(member.name):
                        continue
                    name = "source/" + member.name
                    expected_source.add(name)
                    with tree.extractfile(member) as file:
                        if hashes.get(name) != digest(file):
                            raise ValueError(f"Source differs from the APK commit at {name}")
            if git.wait() != 0:
                raise RuntimeError("Could not inspect the APK source commit")
        if actual_source != expected_source:
            raise ValueError("Source file set differs from APK commit")
        print(f"PASS {len(expected_source)} source files matched {commit}, {len(hashes)} checksums verified")


if __name__ == "__main__":
    if len(sys.argv) != 3:
        sys.exit("Usage: python3 scripts/verify-final-deliverables.py DIRECTORY FULL_APK_COMMIT")
    verify(Path(sys.argv[1]), sys.argv[2])
