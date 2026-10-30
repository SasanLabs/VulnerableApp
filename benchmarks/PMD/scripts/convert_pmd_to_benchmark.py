#!/usr/bin/env python3
"""
convert_pmd_to_benchmark.py
===========================
Converts a PMD JSON report (``pmd check -f json``) into the SAST input format
expected by VulnerableApp's POST /scanner/benchmark endpoint.

The PMD rules to run, and the CWE each one maps to, live in one place:
``benchmarks/PMD/pmd-rule-cwe-mapping.csv`` (columns: category, rule, cwe, type).
Add a row there to scan with a new rule and map it to a CWE.

Usage
-----
    pmd check -d src/main/java \\
        -R "$(python3 benchmarks/PMD/scripts/convert_pmd_to_benchmark.py --print-rulesets)" \\
        -f json -r benchmarks/PMD/pmd-raw-report.json

    python3 benchmarks/PMD/scripts/convert_pmd_to_benchmark.py \\
        --input  benchmarks/PMD/pmd-raw-report.json \\
        --output benchmarks/PMD/findings/pmd-findings.json

    curl -X POST http://localhost:9090/VulnerableApp/scanner/benchmark \\
        -H "Content-Type: application/json" \\
        -d @benchmarks/PMD/findings/pmd-findings.json

Input format (PMD JSON)
-----------------------
PMD's report has no CWE field; each violation only names the PMD rule:

    {
      "files": [
        { "filename": "src/main/java/org/sasanlabs/internal/utility/GenericUtils.java",
          "violations": [ { "beginline": 10, "rule": "AvoidUsingHardCodedIP", ... } ] }
      ]
    }

Output format (VulnerableApp benchmark input, with CWE)
-------------------------------------------------------
The rule name is looked up in the mapping CSV to add ``cwe`` and ``type``. Real
output from a scan of ``src/main/java`` (abridged):

    {
      "tool": "PMD",
      "scanType": "SAST",
      "findings": [
        { "filePath": "src/main/java/org/sasanlabs/internal/utility/GenericUtils.java",
          "line": 10, "cwe": "CWE-547", "type": "Hard-coded Security-relevant Constant" },
        { "filePath": "src/main/java/org/sasanlabs/service/vulnerability/xxe/XXEVulnerability.java",
          "line": 108, "cwe": "CWE-772", "type": "Missing Resource Release" }
      ]
    }

Self-test:  python3 benchmarks/PMD/scripts/convert_pmd_to_benchmark.py --test

Notes
-----
- Rules that are not in the mapping CSV are still emitted, with ``type`` set to
  the PMD rule name and no ``cwe`` (they will land in ``unmatchedItems`` unless
  the line happens to match a ground-truth row).
- File paths are made project-relative: backslashes become forward slashes, a
  leading ``./`` is dropped, and the ``--root`` prefix (default: current
  directory) is stripped from absolute paths. The comparator needs paths such as
  ``src/main/java/org/sasanlabs/...``.
- Suppressed violations and processing errors are not reported as findings.
- Duplicate (filePath, line, cwe, type) tuples are emitted once.
"""

import argparse
import csv
import json
import os
import re
import sys
import tempfile
import unittest
from pathlib import Path
from typing import NamedTuple, Optional

TOOL_NAME = "PMD"
SCAN_TYPE = "SAST"
DEFAULT_RULES_CSV = Path(__file__).resolve().parent.parent / "pmd-rule-cwe-mapping.csv"

_CWE_RE = re.compile(r"^CWE-\d+$")
_NAME_RE = re.compile(r"^[A-Za-z][A-Za-z0-9]*$")


class Rule(NamedTuple):
    category: str  # PMD Java rule category, e.g. "security" or "errorprone"
    name: str      # PMD rule name, e.g. "HardCodedCryptoKey"
    cwe: str       # e.g. "CWE-321"
    type: str      # human-readable vulnerability type label


def load_rules(path: Path = DEFAULT_RULES_CSV) -> list[Rule]:
    """Read the rule -> CWE mapping CSV, raising ValueError on any malformed row."""
    with path.open(encoding="utf-8", newline="") as fh:
        reader = csv.DictReader(fh)
        expected = ["category", "rule", "cwe", "type"]
        if [c.strip() for c in (reader.fieldnames or [])] != expected:
            raise ValueError(f"{path}: header must be {','.join(expected)}")
        rules: list[Rule] = []
        seen: set[str] = set()
        for lineno, row in enumerate(reader, start=2):
            rule = Rule(*(row[c].strip() for c in expected))
            if not (_NAME_RE.match(rule.category) and _NAME_RE.match(rule.name)):
                raise ValueError(f"{path}:{lineno}: invalid category or rule name")
            if not _CWE_RE.match(rule.cwe):
                raise ValueError(f"{path}:{lineno}: invalid CWE {rule.cwe!r}")
            if not rule.type:
                raise ValueError(f"{path}:{lineno}: missing type")
            if rule.name in seen:
                raise ValueError(f"{path}:{lineno}: duplicate rule {rule.name}")
            seen.add(rule.name)
            rules.append(rule)
    if not rules:
        raise ValueError(f"{path}: no rules defined")
    return rules


def ruleset_refs(rules: list[Rule]) -> str:
    """Return the comma-separated rule references to pass to ``pmd check -R``."""
    return ",".join(f"category/java/{r.category}.xml/{r.name}" for r in rules)


# PMD rule name -> (CWE, vulnerability type label), loaded from the mapping CSV.
RULE_MAP: dict[str, tuple[str, str]] = {r.name: (r.cwe, r.type) for r in load_rules()}


def normalise_path(raw: str, root: Optional[str] = None) -> str:
    """Return a project-relative, forward-slash path."""
    path = (raw or "").strip().replace("\\", "/")
    if root:
        prefix = root.strip().replace("\\", "/").rstrip("/") + "/"
        if path.startswith(prefix):
            path = path[len(prefix):]
    while path.startswith("./"):
        path = path[2:]
    return path


def load_report(path: Path) -> dict:
    """Read a PMD JSON report, raising ValueError if it is malformed or not an object."""
    try:
        with path.open(encoding="utf-8") as fh:
            report = json.load(fh)
    except json.JSONDecodeError as exc:
        raise ValueError(f"invalid JSON in PMD report {path}: {exc}") from exc
    if not isinstance(report, dict):
        raise ValueError(f"unexpected PMD report format in {path}: expected a JSON "
                         f"object, got {type(report).__name__}")
    return report


def convert(pmd_report: dict, root: Optional[str] = None,
            rule_map: Optional[dict[str, tuple[str, str]]] = None) -> dict:
    """Convert a parsed PMD JSON report into a benchmark input dict."""
    rule_map = RULE_MAP if rule_map is None else rule_map
    seen: set[tuple] = set()
    findings: list[dict] = []

    for file_entry in pmd_report.get("files", []) or []:
        file_path = normalise_path(file_entry.get("filename", ""), root)
        if not file_path:
            continue

        for violation in file_entry.get("violations", []) or []:
            line = violation.get("beginline")
            rule = (violation.get("rule") or "").strip()
            if not isinstance(line, int) or line <= 0 or not rule:
                continue

            cwe, vuln_type = rule_map.get(rule, (None, rule))

            key = (file_path, line, cwe, vuln_type)
            if key in seen:
                continue
            seen.add(key)

            finding: dict = {"filePath": file_path, "line": line}
            if cwe:
                finding["cwe"] = cwe
            finding["type"] = vuln_type
            findings.append(finding)

    return {"tool": TOOL_NAME, "scanType": SCAN_TYPE, "findings": findings}


class ConverterTest(unittest.TestCase):
    SRC = "src/main/java/org/sasanlabs/service/Foo.java"

    def _report(self, violations, name=None):
        return {"files": [{"filename": name or self.SRC, "violations": violations}]}

    @staticmethod
    def _v(rule="HardCodedCryptoKey", line=10):
        return {"beginline": line, "rule": rule}

    def _write_tmp(self, text, suffix=".json"):
        fh = tempfile.NamedTemporaryFile("w", suffix=suffix, delete=False, encoding="utf-8")
        fh.write(text)
        fh.close()
        self.addCleanup(os.remove, fh.name)
        return Path(fh.name)

    def test_envelope(self):
        self.assertEqual(convert({}), {"tool": "PMD", "scanType": "SAST", "findings": []})

    def test_known_rule_gets_cwe_and_type(self):
        out = convert(self._report([self._v("HardCodedCryptoKey", 42)]))
        self.assertEqual(out["findings"], [{"filePath": self.SRC, "line": 42,
                         "cwe": "CWE-321", "type": "Hard-coded Cryptographic Key"}])

    def test_unknown_rule_uses_rule_name_without_cwe(self):
        out = convert(self._report([self._v("SomeNewRule", 7)]))
        self.assertEqual(out["findings"], [{"filePath": self.SRC, "line": 7, "type": "SomeNewRule"}])

    def test_duplicates_removed_but_distinct_rules_kept(self):
        v = self._v("HardCodedCryptoKey", 5)
        self.assertEqual(len(convert(self._report([v, dict(v)]))["findings"]), 1)
        self.assertEqual(len(convert(self._report([v, self._v("InsecureCryptoIv", 5)]))["findings"]), 2)

    def test_invalid_violations_skipped(self):
        bad = [{"rule": "X"}, {"beginline": 0, "rule": "X"}, {"beginline": 3, "rule": ""}]
        self.assertEqual(convert(self._report(bad))["findings"], [])

    def test_paths_made_relative(self):
        out = convert(self._report([self._v()], "/w/repo/" + self.SRC), root="/w/repo")
        self.assertEqual(out["findings"][0]["filePath"], self.SRC)
        self.assertEqual(normalise_path(".\\src\\Foo.java"), "src/Foo.java")
        self.assertEqual(normalise_path("C:\\r\\src\\Foo.java", root="C:\\r"), "src/Foo.java")

    def test_load_report_rejects_malformed_json(self):
        with self.assertRaises(ValueError):
            load_report(self._write_tmp('{"files": ['))  # truncated report

    def test_load_report_rejects_non_object(self):
        with self.assertRaises(ValueError):
            load_report(self._write_tmp("[]"))
        self.assertEqual(load_report(self._write_tmp('{"files": []}')), {"files": []})

    def test_mapping_csv_well_formed(self):
        rules = load_rules()
        self.assertEqual(RULE_MAP, {r.name: (r.cwe, r.type) for r in rules})
        for r in rules:
            self.assertRegex(r.cwe, _CWE_RE, r.name)
            self.assertTrue(r.type, r.name)

    def test_load_rules_rejects_bad_rows(self):
        header = "category,rule,cwe,type\n"
        for body in ("security,HardCodedCryptoKey,321,Key\n",          # CWE missing prefix
                     "security,Bad Rule,CWE-1,X\n",                     # invalid rule name
                     "security,A,CWE-1,X\nsecurity,A,CWE-2,Y\n",        # duplicate rule
                     ""):                                               # no rules
            with self.assertRaises(ValueError, msg=body):
                load_rules(self._write_tmp(header + body, suffix=".csv"))

    def test_ruleset_refs(self):
        rules = [Rule("security", "HardCodedCryptoKey", "CWE-321", "Key"),
                 Rule("errorprone", "CloseResource", "CWE-772", "Leak")]
        self.assertEqual(ruleset_refs(rules),
                         "category/java/security.xml/HardCodedCryptoKey,"
                         "category/java/errorprone.xml/CloseResource")


def main() -> None:
    if "--test" in sys.argv:
        sys.argv.remove("--test")
        unittest.main(verbosity=1)
        return

    if "--print-rulesets" in sys.argv:
        print(ruleset_refs(load_rules()))
        return

    parser = argparse.ArgumentParser(
        description="Convert a PMD JSON report to VulnerableApp benchmark input format."
    )
    parser.add_argument("--input", "-i", required=True,
                        help="Path to the PMD JSON report (pmd check -f json).")
    parser.add_argument("--output", "-o",
                        default="benchmarks/PMD/findings/pmd-findings.json",
                        help="Where to write the benchmark input (findings) JSON.")
    parser.add_argument("--root", default=os.getcwd(),
                        help="Project root stripped from absolute paths (default: cwd).")
    args = parser.parse_args()

    input_path = Path(args.input)
    output_path = Path(args.output)

    if not input_path.exists():
        print(f"ERROR: input file not found: {input_path}", file=sys.stderr)
        sys.exit(1)

    try:
        pmd_report = load_report(input_path)
    except ValueError as exc:
        print(f"ERROR: {exc}", file=sys.stderr)
        sys.exit(1)

    errors = pmd_report.get("processingErrors") or []
    if errors:
        print(f"WARNING: PMD reported {len(errors)} processing error(s); "
              "those files may be missing from the results.", file=sys.stderr)

    benchmark_input = convert(pmd_report, args.root)

    output_path.parent.mkdir(parents=True, exist_ok=True)
    with output_path.open("w", encoding="utf-8") as fh:
        json.dump(benchmark_input, fh, indent=2)
        fh.write("\n")

    print(f"Converted {len(benchmark_input['findings'])} finding(s)  ->  {output_path}")


if __name__ == "__main__":
    main()