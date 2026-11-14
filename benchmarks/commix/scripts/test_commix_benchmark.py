import json
from pathlib import Path
import unittest

from commix_log import parse_log
from convert_commix_to_benchmark import NEGATIVE_CONCLUSION, clean_url, convert
from run_commix import diagnostics, discover_targets

FIXTURES = Path(__file__).resolve().parents[1] / "fixtures"
BASE = "http://127.0.0.1:9090/VulnerableApp"
URL = BASE + "/CommandInjection/LEVEL_1"
MARKER = "commixrce2f9c1a7b4e"


def log(name):
    return (FIXTURES / (name + "-logs.txt")).read_text()


def report(executions=None, injection_points=None):
    return {
        "testedUrl": URL + "?ipaddress=127.0.0.1",
        "injectionPoints": injection_points
        if injection_points is not None
        else [{"type": "Results-Based Os Command Injection",
               "technique": "Classic Command Injection Technique",
               "method": "GET", "parameter": "ipaddress"}],
        "executions": executions
        if executions is not None
        else [{"command": "echo " + MARKER, "output": MARKER}],
    }


def envelope(url=URL, marker=MARKER, **updates):
    run = dict(url=url, method="GET", variant="UNSECURE", marker=marker,
               completed=True, returncode=0, report=report(), diagnostics=[])
    run.update(updates)
    return {"schemaVersion": 1, "commixVersion": "commix/v4.1", "targetCount": 1, "runs": [run]}


class LogParserTest(unittest.TestCase):
    def test_positive_log_yields_injection_point_and_execution(self):
        parsed = parse_log(log("positive"))
        self.assertEqual(len(parsed["injectionPoints"]), 1)
        point = parsed["injectionPoints"][0]
        self.assertEqual(point["technique"], "Classic Command Injection Technique")
        self.assertEqual(point["method"], "GET")
        self.assertEqual(point["parameter"], "ipaddress")
        self.assertEqual(parsed["executions"],
                         [{"command": "echo " + MARKER, "output": MARKER}])

    def test_negative_log_has_no_injection_or_execution(self):
        parsed = parse_log(log("negative"))
        self.assertEqual(parsed["injectionPoints"], [])
        self.assertEqual(parsed["executions"], [])
        self.assertTrue(parsed["testedUrl"].endswith("/CommandInjection/LEVEL_7?ipaddress=127.0.0.1"))

    def test_multiple_injection_points_are_kept_separate(self):
        parsed = parse_log(log("positive") + "\n" + log("positive"))
        self.assertEqual(len(parsed["injectionPoints"]), 2)
        self.assertEqual(len(parsed["executions"]), 2)

    def test_empty_log_is_inert(self):
        self.assertEqual(parse_log(""),
                         {"testedUrl": None, "injectionPoints": [], "executions": []})


class ConverterTest(unittest.TestCase):
    def test_confirmed_execution_becomes_one_typed_finding(self):
        self.assertEqual(convert(envelope()), {
            "tool": "Commix", "scanType": "DAST",
            "findings": [{"url": URL, "method": "GET", "type": "COMMAND_INJECTION"}]})

    def test_completed_negative_is_empty_not_failed(self):
        run = dict(report=report(executions=[], injection_points=[]),
                   diagnostics=[NEGATIVE_CONCLUSION + " Try to increase ..."])
        self.assertEqual(convert(envelope(**run))["findings"], [])

    def test_duplicate_endpoint_is_counted_once(self):
        wrapped = envelope()
        wrapped["runs"] *= 2
        wrapped["targetCount"] = 2
        self.assertEqual(len(convert(wrapped)["findings"]), 1)

    def test_marker_must_match_exactly(self):
        # commix ran but the echoed output is not our marker: not a confirmed finding.
        run = dict(report=report(executions=[{"command": "echo " + MARKER, "output": "other"}]),
                   diagnostics=[NEGATIVE_CONCLUSION])
        self.assertEqual(convert(envelope(**run))["findings"], [])
        # And with neither a marker echo nor a negative conclusion, the run is incomplete.
        with self.assertRaises(ValueError):
            convert(envelope(report=report(executions=[{"command": "echo " + MARKER, "output": "other"}])))

    def test_confirmed_execution_with_error_is_conflicting(self):
        with self.assertRaises(ValueError):
            convert(envelope(diagnostics=[NEGATIVE_CONCLUSION]))

    def test_non_negative_diagnostics_fail(self):
        with self.assertRaises(ValueError):
            convert(envelope(report=report(executions=[], injection_points=[]),
                             diagnostics=["unable to connect to the target URL"]))

    def test_failed_timeout_or_nonzero_scan_never_becomes_clean(self):
        for changes in [dict(completed=False), dict(returncode=1), dict(report=None),
                        dict(diagnostics=["Per-target scan timeout exceeded"]),
                        dict(marker="")]:
            with self.subTest(changes=changes), self.assertRaises(ValueError):
                convert(envelope(**changes))

    def test_unsupported_version_and_schema_fail(self):
        for bad in [dict(commixVersion="commix/v4.2.dev311"), dict(commixVersion="4.1"),
                    dict(schemaVersion=2), dict(runs=[]), dict(targetCount=5)]:
            wrapped = envelope()
            wrapped.update(bad)
            with self.subTest(bad=bad), self.assertRaises(ValueError):
                convert(wrapped)


class DiscoveryTest(unittest.TestCase):
    def rows(self, **overrides):
        row = {"url": URL, "variant": "UNSECURE", "method": "GET",
               "vulnerabilityTypes": ["COMMAND_INJECTION"]}
        row.update(overrides)
        return [row]

    def test_selects_command_injection_get_endpoints(self):
        targets = discover_targets(self.rows(), BASE)
        self.assertEqual(targets, [{"url": URL, "method": "GET", "variant": "UNSECURE",
                                    "vulnerabilityTypes": ["COMMAND_INJECTION"]}])

    def test_ignores_other_families(self):
        rows = self.rows() + [{"url": BASE + "/XXEVulnerability/LEVEL_1", "variant": "UNSECURE",
                               "method": "GET", "vulnerabilityTypes": ["XXE"]}]
        self.assertEqual(len(discover_targets(rows, BASE)), 1)

    def test_rejects_foreign_origin(self):
        rows = self.rows(url="http://evil.example/VulnerableApp/CommandInjection/LEVEL_1")
        with self.assertRaises(ValueError):
            discover_targets(rows, BASE)

    def test_rejects_non_get_method(self):
        with self.assertRaises(ValueError):
            discover_targets(self.rows(method="POST"), BASE)

    def test_empty_scope_is_an_error(self):
        with self.assertRaises(ValueError):
            discover_targets([], BASE)


class DiagnosticsTest(unittest.TestCase):
    def test_extracts_error_and_critical_lines(self):
        text = ("[11:29:40] \x1b[91m[critical]\x1b[0m " + NEGATIVE_CONCLUSION + " Try ...\n"
                "[11:29:40] [info] harmless\n")
        self.assertEqual(diagnostics(text), [NEGATIVE_CONCLUSION + " Try ..."])

    def test_flags_traceback(self):
        self.assertIn("Scanner traceback; inspect scan.log",
                      diagnostics("Traceback (most recent call last):\n  File ...\n"))


class CleanUrlTest(unittest.TestCase):
    def test_strips_query_and_trailing_slash(self):
        self.assertEqual(clean_url(URL + "/?ipaddress=1"), URL)

    def test_rejects_credentials(self):
        with self.assertRaises(ValueError):
            clean_url("http://user:pass@host/CommandInjection/LEVEL_1")


if __name__ == "__main__":
    unittest.main()
