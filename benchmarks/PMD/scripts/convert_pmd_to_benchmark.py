#!/usr/bin/env python3
"""
convert_pmd_to_benchmark.py
===========================
Converts a PMD JSON report (``pmd check -f json``) into the SAST input format
expected by VulnerableApp's POST /scanner/benchmark endpoint.

Usage
-----
    pmd check -d src/main/java \\
        -R category/java/security.xml,category/java/bestpractices.xml/AvoidUsingHardCodedIP,category/java/bestpractices.xml/AvoidPrintStackTrace,category/java/errorprone.xml/CloseResource,category/java/errorprone.xml/AvoidLosingExceptionInformation \\
        -f json -r benchmarks/PMD/pmd-raw-report.json

    python3 benchmarks/PMD/scripts/convert_pmd_to_benchmark.py \\
        --input  benchmarks/PMD/pmd-raw-report.json \\
        --output benchmarks/PMD/pmd-benchmark-input.json

    curl -X POST http://localhost:9090/VulnerableApp/scanner/benchmark \\
        -H "Content-Type: application/json" \\
        -d @benchmarks/PMD/pmd-benchmark-input.json

Input format (PMD JSON)
-----------------------
    {
      "files": [
        { "filename": "src/main/java/.../Foo.java",
          "violations": [ { "beginline": 12, "rule": "HardCodedCryptoKey", ... } ] }
      ]
    }

Output format (VulnerableApp benchmark input)
---------------------------------------------
    {
      "tool": "PMD",
      "scanType": "SAST",
      "findings": [
        { "filePath": "src/main/java/.../Foo.java", "line": 12,
          "cwe": "CWE-321", "type": "Hard-coded Cryptographic Key" }
      ]
    }

Self-test:  python3 benchmarks/PMD/scripts/convert_pmd_to_benchmark.py --test

Notes
-----
- PMD does not put CWE IDs in its reports, so each rule name is mapped to a CWE
  and a type label via RULE_MAP below. Rules that are not in the map are still
  emitted, with ``type`` set to the PMD rule name and no ``cwe`` (they will land
  in ``unmatchedItems`` unless the line happens to match a ground-truth row).
- File paths are made project-relative: backslashes become forward slashes, a
  leading ``./`` is dropped, and the ``--root`` prefix (default: current
  directory) is stripped from absolute paths. The comparator needs paths such as
  ``src/main/java/org/sasanlabs/...``.
- Suppressed violations and processing errors are not reported as findings.
- Duplicate (filePath, line, cwe, type) tuples are emitted once.
"""

import argparse
import json
import os
import sys
import unittest
from pathlib import Path
from typing import Optional

TOOL_NAME = "PMD"
SCAN_TYPE = "SAST"

# PMD rule name -> (CWE, vulnerability type label).
RULE_MAP: dict[str, tuple[str, str]] = {
    # category/java/security.xml
    "HardCodedCryptoKey": ("CWE-321", "Hard-coded Cryptographic Key"),
    "InsecureCryptoIv": ("CWE-329", "Insecure Crypto IV"),
    # security-adjacent rules from other categories (see the -R list in the README)
    "AvoidUsingHardCodedIP": ("CWE-547", "Hard-coded Security-relevant Constant"),
    "AvoidPrintStackTrace": ("CWE-209", "Information Exposure Through Error Message"),
    "CloseResource": ("CWE-772", "Missing Resource Release"),
    "AvoidLosingExceptionInformation": ("CWE-390", "Error Condition Without Action"),
}


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


def convert(pmd_report: dict, root: Optional[str] = None) -> dict:
    """Convert a parsed PMD JSON report into a benchmark input dict."""
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

            cwe, vuln_type = RULE_MAP.get(rule, (None, rule))

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

    def test_rule_map_well_formed(self):
        for rule, (cwe, label) in RULE_MAP.items():
            self.assertTrue(cwe.startswith("CWE-") and cwe[4:].isdigit(), rule)
            self.assertTrue(label.strip(), rule)


def main() -> None:
    if "--test" in sys.argv:
        sys.argv.remove("--test")
        unittest.main(verbosity=1)
        return

    parser = argparse.ArgumentParser(
        description="Convert a PMD JSON report to VulnerableApp benchmark input format."
    )
    parser.add_argument("--input", "-i", required=True,
                        help="Path to the PMD JSON report (pmd check -f json).")
    parser.add_argument("--output", "-o",
                        default="benchmarks/PMD/pmd-benchmark-input.json",
                        help="Where to write the benchmark input JSON.")
    parser.add_argument("--root", default=os.getcwd(),
                        help="Project root stripped from absolute paths (default: cwd).")
    args = parser.parse_args()

    input_path = Path(args.input)
    output_path = Path(args.output)

    if not input_path.exists():
        print(f"ERROR: input file not found: {input_path}", file=sys.stderr)
        sys.exit(1)

    with input_path.open(encoding="utf-8") as fh:
        pmd_report = json.load(fh)

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