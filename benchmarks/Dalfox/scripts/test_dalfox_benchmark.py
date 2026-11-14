import copy
import json
from pathlib import Path
import unittest
from unittest import mock

from convert_dalfox_to_benchmark import classify, clean_url, convert
from run_dalfox import build_command, discover_targets, preflight, seed_url

FIXTURES = Path(__file__).resolve().parents[1] / "fixtures"
BASE = "http://127.0.0.1:9090/VulnerableApp"
HTML_LEVEL_1 = BASE + "/XSSWithHtmlTagInjection/LEVEL_1"
HTML_LEVEL_4 = BASE + "/XSSWithHtmlTagInjection/LEVEL_4"
IMG_LEVEL_6 = BASE + "/XSSInImgTagAttribute/LEVEL_6"


def native(name):
    """A real Dalfox 3.3.0 JSON report captured from a local scan."""
    return json.loads((FIXTURES / (name + "-report.json")).read_text())


def run_record(name, url, variant="UNSECURE", **updates):
    report = native(name)
    record = dict(
        url=url, method="GET", variant=variant,
        seedUrl=report["meta"]["target_summary"][0]["target"],
        preflight={"status": 200, "contentType": "text/html", "seedReflected": True},
        completed=True, returncode=1 if report["findings"] else 0, report=report,
    )
    record.update(updates)
    return record


def envelope(*runs, **updates):
    runs = list(runs) or [run_record("confirmed", HTML_LEVEL_1)]
    wrapped = {"schemaVersion": 1, "dalfoxVersion": "3.3.0", "targetCount": len(runs), "runs": runs}
    wrapped.update(updates)
    return wrapped


class ConverterTest(unittest.TestCase):
    def test_confirmed_finding_becomes_one_typed_endpoint_finding(self):
        self.assertEqual(convert(envelope()), {
            "tool": "Dalfox", "scanType": "DAST",
            "findings": [{"url": HTML_LEVEL_1, "method": "GET", "type": "REFLECTED_XSS"}]})

    def test_reflection_only_result_is_an_observation_not_a_detection(self):
        confirmed, observations = classify(envelope(
            run_record("reflection-only", HTML_LEVEL_4, variant="SECURE")))
        self.assertEqual(confirmed, [])
        self.assertEqual(len(observations), 1)
        self.assertEqual(observations[0]["url"], HTML_LEVEL_4)
        self.assertEqual(observations[0]["tier"], "R")
        self.assertEqual(observations[0]["variant"], "SECURE")

    def test_clean_report_yields_no_findings(self):
        self.assertEqual(classify(envelope(run_record("clean", IMG_LEVEL_6, variant="SECURE"))),
                         ([], []))

    def test_mixed_runs_are_graded_independently(self):
        confirmed, observations = classify(envelope(
            run_record("confirmed", HTML_LEVEL_1),
            run_record("reflection-only", HTML_LEVEL_4, variant="SECURE"),
            run_record("clean", IMG_LEVEL_6, variant="SECURE")))
        self.assertEqual([f["url"] for f in confirmed], [HTML_LEVEL_1])
        self.assertEqual([o["url"] for o in observations], [HTML_LEVEL_4])

    def test_duplicate_findings_and_endpoints_are_counted_once(self):
        run = run_record("confirmed", HTML_LEVEL_1)
        run["report"]["findings"] *= 2
        run["report"]["meta"]["findings_count"] = 2
        wrapped = envelope(run, copy.deepcopy(run))
        self.assertEqual(len(convert(wrapped)["findings"]), 1)

    def test_proof_of_concept_on_another_url_is_not_attributed(self):
        # e.g. a PoC on the facade origin, or on a sibling level, never credits this row.
        for poc in ["http://127.0.0.1/VulnerableApp/XSSWithHtmlTagInjection/LEVEL_1?value=x",
                    BASE + "/XSSWithHtmlTagInjection/LEVEL_2?value=x"]:
            run = run_record("confirmed", HTML_LEVEL_1)
            run["report"]["findings"][0]["data"] = poc
            with self.subTest(poc=poc):
                confirmed, observations = classify(envelope(run))
                self.assertEqual(confirmed, [])
                self.assertEqual(observations[0]["reason"], "off-target proof of concept")

    def test_confirmed_tier_without_xss_cwe_is_not_submitted(self):
        run = run_record("confirmed", HTML_LEVEL_1)
        run["report"]["findings"][0]["cwe"] = "CWE-1104"
        confirmed, observations = classify(envelope(run))
        self.assertEqual(confirmed, [])
        self.assertEqual(observations[0]["reason"], "not a confirmed XSS (tier V, CWE-1104)")

    def test_skipped_target_fails_even_with_a_completed_exit_code(self):
        for returncode in (0, 2):
            run = run_record("skipped", HTML_LEVEL_1, returncode=returncode)
            with self.subTest(returncode=returncode), self.assertRaises(ValueError):
                convert(envelope(run))

    def test_failed_timeout_or_errored_scan_never_becomes_clean(self):
        for changes in [dict(completed=False), dict(returncode=2), dict(returncode=None),
                        dict(report=None), dict(report={}), dict(report={"meta": {}}),
                        dict(method="POST")]:
            with self.subTest(changes=changes), self.assertRaises(ValueError):
                convert(envelope(run_record("clean", IMG_LEVEL_6, **changes)))
        with self.assertRaises(ValueError):
            convert(envelope(run_record("clean", "ftp://127.0.0.1/x")))

    def test_report_inconsistencies_fail(self):
        mutations = {
            "incomplete": lambda r: r["report"]["meta"].update(incomplete=True),
            "missing incomplete flag": lambda r: r["report"]["meta"].pop("incomplete"),
            "count mismatch": lambda r: r["report"]["meta"].update(findings_count=3),
            "other version": lambda r: r["report"]["meta"].update(dalfox_version="3.2.4"),
            "other target": lambda r: r.update(seedUrl=HTML_LEVEL_4 + "?value=vulnerableapp"),
            "two targets": lambda r: r["report"]["meta"]["target_summary"].append(
                r["report"]["meta"]["target_summary"][0]),
            "timed out": lambda r: r["report"]["meta"]["target_summary"][0].update(
                status="incomplete", error_code="SCAN_TIMEOUT"),
            "exit code disagrees": lambda r: r.update(returncode=0),
            "findings not a list": lambda r: r["report"].update(findings=None),
        }
        for name, mutate in mutations.items():
            run = run_record("confirmed", HTML_LEVEL_1)
            mutate(run)
            with self.subTest(name), self.assertRaises(ValueError):
                convert(envelope(run))

    def test_seed_that_does_not_reach_the_sink_fails(self):
        for bad in [{"status": 400, "contentType": "text/html", "seedReflected": False},
                    {"status": 200, "contentType": "text/plain", "seedReflected": True},
                    {"status": 200, "contentType": "text/html", "seedReflected": False},
                    None]:
            with self.subTest(preflight=bad), self.assertRaises(ValueError):
                convert(envelope(run_record("clean", IMG_LEVEL_6, preflight=bad)))

    def test_unsupported_version_and_schema_fail(self):
        for bad in [dict(dalfoxVersion="v3.3.0"), dict(dalfoxVersion="3.3.0-dev"),
                    dict(schemaVersion=2), dict(runs=[]), dict(targetCount=5)]:
            with self.subTest(bad=bad), self.assertRaises(ValueError):
                convert(envelope(**bad))

    def test_clean_url_rejects_credentials_and_strips_query(self):
        self.assertEqual(clean_url(HTML_LEVEL_1 + "/?value=x#f"), HTML_LEVEL_1)
        for bad in ["http://user:pw@127.0.0.1/x", "javascript:alert(1)", None]:
            with self.subTest(bad=bad), self.assertRaises(ValueError):
                clean_url(bad)


class DiscoveryTest(unittest.TestCase):
    def rows(self, *extra):
        rows = [
            {"url": BASE + "/XSSInImgTagAttribute/LEVEL_1", "variant": "UNSECURE",
             "method": "GET", "protocol": "HTTP", "vulnerabilityTypes": ["REFLECTED_XSS"]},
            {"url": HTML_LEVEL_4, "variant": "SECURE", "method": "GET", "protocol": "HTTP",
             "vulnerabilityTypes": ["REFLECTED_XSS"]},
            {"url": BASE + "/PersistentXSSInHTMLTagVulnerability/LEVEL_1", "variant": "UNSECURE",
             "method": "GET", "protocol": "HTTP", "vulnerabilityTypes": ["PERSISTENT_XSS"]},
            {"url": BASE + "/CommandInjection/LEVEL_1", "variant": "UNSECURE",
             "method": "GET", "protocol": "HTTP", "vulnerabilityTypes": ["COMMAND_INJECTION"]},
        ]
        return rows + list(extra)

    def test_selects_supported_families_with_their_parameter_and_seed(self):
        targets = discover_targets(self.rows(), BASE)
        self.assertEqual(targets, [
            {"url": BASE + "/XSSInImgTagAttribute/LEVEL_1", "method": "GET", "variant": "UNSECURE",
             "vulnerabilityTypes": ["REFLECTED_XSS"], "parameter": "src",
             "seed": "/VulnerableApp/images/OWASP.png"},
            {"url": HTML_LEVEL_4, "method": "GET", "variant": "SECURE",
             "vulnerabilityTypes": ["REFLECTED_XSS"], "parameter": "value",
             "seed": "vulnerableapp"},
        ])
        self.assertEqual(seed_url(targets[0]),
                         BASE + "/XSSInImgTagAttribute/LEVEL_1?src=%2FVulnerableApp%2Fimages%2FOWASP.png")

    def test_image_seed_stays_fixed_while_urls_follow_the_deployed_context_path(self):
        rows = [dict(row, url=row["url"].replace("/VulnerableApp/", "/lab/")) for row in self.rows()]
        targets = discover_targets(rows, "http://127.0.0.1:9090/lab")
        self.assertEqual(targets[0]["url"], "http://127.0.0.1:9090/lab/XSSInImgTagAttribute/LEVEL_1")
        self.assertEqual(targets[0]["seed"], "/VulnerableApp/images/OWASP.png")

    def test_duplicate_rows_are_collapsed(self):
        self.assertEqual(len(discover_targets(self.rows(self.rows()[1]), BASE)), 2)

    def test_unsupported_shapes_fail_instead_of_being_skipped(self):
        base_row = self.rows()[1]
        for bad in [dict(base_row, method="POST"), dict(base_row, url=HTML_LEVEL_4 + "?value=x"),
                    dict(base_row, protocol="WS"), dict(base_row, variant="UNKNOWN"),
                    dict(base_row, url="http://other:9090/VulnerableApp/XSSWithHtmlTagInjection/LEVEL_9"),
                    dict(base_row, variant="UNSECURE")]:
            with self.subTest(bad=bad), self.assertRaises(ValueError):
                discover_targets(self.rows(bad), BASE)

    def test_missing_family_or_malformed_response_fails(self):
        for rows in [[], self.rows()[1:], {"url": HTML_LEVEL_1}, [None], [{"url": 1}]]:
            with self.subTest(rows=rows), self.assertRaises(ValueError):
                discover_targets(rows, BASE)

    def test_command_scopes_the_scan_to_the_seeded_query_parameter(self):
        target = discover_targets(self.rows(), BASE)[1]
        command = build_command("dalfox", target, Path("/tmp/report.json"), 180, 4)
        self.assertEqual(command[:3], ["dalfox", "scan", HTML_LEVEL_4 + "?value=vulnerableapp"])
        for expected in [["--param", "value:query"], ["--headers", "Accept: text/html"],
                         ["--format", "json"], ["--output", "/tmp/report.json"],
                         ["--scan-timeout", "180"], ["--workers", "4"]]:
            index = command.index(expected[0])
            self.assertEqual(command[index:index + 2], expected)
        self.assertIn("--skip-mining", command)


class PreflightTest(unittest.TestCase):
    target = {"url": HTML_LEVEL_1, "parameter": "value", "seed": "vulnerableapp"}

    def response(self, body, content_type="text/html;charset=UTF-8", status=200):
        response = mock.MagicMock(status=status)
        response.__enter__.return_value = response
        response.headers.get_content_type.return_value = content_type.split(";")[0]
        response.read.return_value = body.encode()
        return response

    def test_reflected_html_seed_passes(self):
        with mock.patch("run_dalfox.urlopen", return_value=self.response("<div>vulnerableapp<div>")) as call:
            self.assertEqual(preflight(self.target),
                             {"status": 200, "contentType": "text/html", "seedReflected": True})
        self.assertEqual(call.call_args[0][0].get_header("Accept"), "text/html")

    def test_unreflected_seed_is_recorded(self):
        with mock.patch("run_dalfox.urlopen", return_value=self.response("")):
            self.assertFalse(preflight(self.target)["seedReflected"])


if __name__ == "__main__":
    unittest.main()
