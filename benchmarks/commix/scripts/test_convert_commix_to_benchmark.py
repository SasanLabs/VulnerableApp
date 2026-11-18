import tempfile
import unittest
from pathlib import Path

from convert_commix_to_benchmark import convert, convert_log, read_logs

BASE = "http://localhost/VulnerableApp/CommandInjection"

POSITIVE = """
=====================================
| Started in 2026-11-11 at 11:31:26 |
=====================================
[info] Tested URL : {base}/LEVEL_1?ipaddress=127.0.0.1
[info] Type: Results-Based Os Command Injection
[info] Technique: Classic Command Injection Technique
[info] Parameter: (GET) ipaddress
[info] Used payload: 127.0.0.1;echo ABCDEF$((96%2B67))$(echo ABCDEF)ABCDEF
""".format(base=BASE)

NEGATIVE = """
=====================================
| Started in 2026-11-11 at 11:29:23 |
=====================================
[info] Tested URL : {base}/LEVEL_7?ipaddress=127.0.0.1
""".format(base=BASE)


class ConvertLogTest(unittest.TestCase):
    def test_injection_point_becomes_finding_without_query(self):
        self.assertEqual(
            convert_log(POSITIVE),
            [{"url": BASE + "/LEVEL_1", "method": "GET", "type": "COMMAND_INJECTION"}],
        )

    def test_target_without_injection_has_no_finding(self):
        self.assertEqual(convert_log(NEGATIVE), [])

    def test_blocks_are_attributed_to_their_own_url(self):
        findings = convert_log(NEGATIVE + POSITIVE + NEGATIVE.replace("LEVEL_7", "LEVEL_8"))
        self.assertEqual([f["url"] for f in findings], [BASE + "/LEVEL_1"])

    def test_post_method_is_kept(self):
        log = POSITIVE.replace("(GET)", "(POST)")
        self.assertEqual(convert_log(log)[0]["method"], "POST")

    def test_empty_log(self):
        self.assertEqual(convert_log(""), [])


class ConvertTest(unittest.TestCase):
    def test_envelope(self):
        self.assertEqual(
            convert([POSITIVE, NEGATIVE]),
            {
                "tool": "Commix",
                "scanType": "DAST",
                "findings": [{"url": BASE + "/LEVEL_1", "method": "GET", "type": "COMMAND_INJECTION"}],
            },
        )

    def test_read_logs_from_directory_and_file(self):
        with tempfile.TemporaryDirectory() as tmp:
            nested = Path(tmp) / "localhost" / "logs.txt"
            nested.parent.mkdir()
            nested.write_text(POSITIVE)
            self.assertEqual(read_logs(Path(tmp)), [POSITIVE])
            self.assertEqual(read_logs(nested), [POSITIVE])

    def test_missing_logs_fail(self):
        with tempfile.TemporaryDirectory() as tmp:
            with self.assertRaises(FileNotFoundError):
                read_logs(Path(tmp))


if __name__ == "__main__":
    unittest.main()
