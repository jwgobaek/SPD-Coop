#!/usr/bin/env python3
"""Exercise the release gate with synthetic data isolated in a temporary directory."""
import csv
import hashlib
import json
import runpy
import tempfile
import unittest
import uuid
from pathlib import Path

mod = runpy.run_path(str(Path(__file__).with_name("verify-final-deliverables.py")))
columns = mod["ANDROID_COLUMNS"]


class AndroidGateTest(unittest.TestCase):
    def setUp(self):
        tmp = tempfile.TemporaryDirectory()
        self.addCleanup(tmp.cleanup)
        root = Path(tmp.name)
        self.apk = root / "fixture.apk"
        self.apk.write_bytes(b"synthetic")
        self.manifest = root / "manifest.json"
        self.output = root / "observations.tsv"
        self.plan = json.loads((Path(__file__).resolve().parents[1] /
                                "docs/coop/android-acceptance-sync39.json").read_text())
        self.plan.update(apk_sha256=hashlib.sha256(self.apk.read_bytes()).hexdigest(),
                         bytes=self.apk.stat().st_size)
        self.manifest.write_text(json.dumps(self.plan))
        capture = b"test fixture only"
        digest = hashlib.sha256(capture).hexdigest()
        (root / "captures").mkdir()
        self.capture = root / "captures" / digest
        self.capture.write_bytes(capture)
        self.rows = []
        for seat, role in enumerate(("host", "guest")):
            boot = str(uuid.uuid4())
            for name, spec in (self.plan["scenarios"] | self.plan.get("role_scenarios", {}).get(role, {})).items():
                row = dict.fromkeys(columns, "0")
                status = spec["outcome"]
                result = "APPLIED" if status == "REPLAY" else status
                version = 6 if result == "REJECT_INVALID" else 7
                receipt = f"RESULT 2 {seat} 14 5 6 {version} {result}"
                row.update(role=role, boot_id=boot, base_apk_sha256=self.plan["apk_sha256"],
                           build_commit=self.plan["build_commit"], scenario=name, result="PASS",
                           action=spec["action"], receipt=receipt, replay=receipt if status=="REPLAY" else "-",
                           head="11", state_version="10", next_action="WAIT",
                           next_receipt=f"RESULT 3 {seat} 14 7 9 10 APPLIED", evidence_sha256=digest)
                self.rows.append(row)

    def check(self, rows):
        with self.output.open("w", newline="") as stream:
            writer = csv.DictWriter(stream, fieldnames=columns, delimiter="\t")
            writer.writeheader()
            writer.writerows(rows)
        mod["verify_android_candidate"](self.apk, self.manifest, self.output)

    def test_complete_reviewed_plan(self):
        self.check(self.rows)

    def test_missing_new_feature_and_duplicate(self):
        for rows in (self.rows[:-1], self.rows + self.rows[:1],
                     [r for r in self.rows if r["action"] not in ("ENCHANT_CHOOSE", "ENCHANT_TARGET")]):
            with self.assertRaises(ValueError):
                self.check(rows)

    def test_failed_or_unverified_device_steps(self):
        for key, value in (("base_apk_sha256","0"*64), ("build_commit","f"*40),
                           ("result","NOT RUN"), ("result","FAIL"), ("receipt","-"), ("replay","-"),
                           ("next_receipt","-"), ("state_version","2"), ("head","0"), ("pending","1"),
                           ("verify_errors","1"), ("linkage_errors","1"), ("crashes","1"),
                           ("glthread_fatal","1"), ("anrs","1"), ("stalls","1")):
            with self.subTest(key=key, value=value), self.assertRaises(ValueError):
                self.check([dict(row, **{key:value}) if row["scenario"]=="duplicate-selection" else row
                            for row in self.rows])

    def test_guest_cannot_release_stored_ally(self):
        rows = [dict(row, result="FAIL", receipt="-") if row["scenario"]=="stasis-manual-release" else row
                for row in self.rows]
        with self.assertRaisesRegex(ValueError,"stasis-manual-release"):
            self.check(rows)

    def test_blocked_landing_cannot_advance_the_version(self):
        rows = [dict(row, receipt=row["receipt"].replace("6 6 REJECT_INVALID", "6 7 REJECT_INVALID"))
                if row["scenario"] == "stasis-blocked-release" else row for row in self.rows]
        with self.assertRaisesRegex(ValueError, "authoritative state version"):
            self.check(rows)

    def test_independent_boots_and_content(self):
        with self.assertRaises(ValueError):
            self.check([dict(row, boot_id=self.rows[0]["boot_id"]) for row in self.rows])
        self.capture.write_bytes(b"changed")
        with self.assertRaises(ValueError):
            self.check(self.rows)


if __name__ == "__main__":
    unittest.main()
