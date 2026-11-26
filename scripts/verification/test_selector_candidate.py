"""Verify the release evidence checker without a device, release APK, or upload."""
import csv
import hashlib
import io
from pathlib import Path
import runpy
import tempfile
import unittest
from contextlib import redirect_stdout

verify = runpy.run_path(str(Path(__file__).resolve().parents[1] / "verify-final-deliverables.py"))["verify_selector_candidate"]
FIELDS = "role boot_id base_apk_sha256 selector result crashes anrs".split()
SELECTORS = ["HolyIntuition", "RecallInscription", "BodyForm", "MindForm", "SpiritForm"]


class SelectorCandidateTest(unittest.TestCase):
    def setUp(self):
        tmp = tempfile.TemporaryDirectory()
        self.addCleanup(tmp.cleanup)
        self.apk = Path(tmp.name, "synthetic.bin")
        self.apk.write_bytes(b"test-only byte stream")
        digest = hashlib.sha256(self.apk.read_bytes()).hexdigest()
        self.rows = []
        for seat, role in enumerate(("host", "guest"), 1):
            boot = f"00000000-0000-0000-0000-{seat:012d}"
            for selector in SELECTORS:
                self.rows.append(dict(zip(FIELDS, (role, boot, digest, selector, "PASS", "0", "0"))))

    def check(self, rows):
        report = self.apk.with_suffix(".tsv")
        with report.open("w", newline="") as stream:
            writer = csv.DictWriter(stream, FIELDS, delimiter="\t")
            writer.writeheader()
            writer.writerows(rows)
        with redirect_stdout(io.StringIO()):
            verify(self.apk, report)

    def test_complete_observations_match_original_bytes_only(self):
        self.check(self.rows)
        self.apk.write_bytes(b"second build")
        with self.assertRaises(ValueError):
            self.check(self.rows)

    def test_two_aliases_for_one_android_boot_do_not_satisfy_two_seats(self):
        with self.assertRaises(ValueError):
            self.check([dict(r, boot_id=self.rows[0]["boot_id"]) for r in self.rows])

    def test_require_exactly_the_measured_pairs(self):
        for rows in (self.rows[:-1], self.rows + self.rows[:1]):
            with self.subTest(length=len(rows)), self.assertRaises(ValueError):
                self.check(rows)

    def test_incomplete_failed_or_invalid_measurements_are_rejected(self):
        for field, value in (("result", "NOT RUN"), ("result", "FAIL"), ("crashes", "1"), ("anrs", "1"),
                             ("selector", "unknown"), ("role", "unknown"), ("boot_id", "")):
            with self.subTest(field=field, value=value), self.assertRaises(ValueError):
                self.check([dict(self.rows[0], **{field: value})] + self.rows[1:])


if __name__ == "__main__":
    unittest.main()
