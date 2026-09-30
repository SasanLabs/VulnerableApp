#!/usr/bin/env python3
"""
convert_semgrep_to_benchmark.py
===============================
Converts a Semgrep JSON report into the JSON input format expected by
VulnerableApp's POST /scanner/benchmark endpoint (SAST mode).

Usage
-----
    # From the repository root, so reported paths are project-relative:
    semgrep scan --config p/default --metrics=off --timeout 30 --json \\
        --output benchmarks/Semgrep/semgrep-raw-report.json src/main/java

    python3 benchmarks/Semgrep/scripts/convert_semgrep_to_benchmark.py \\
        --input  benchmarks/Semgrep/semgrep-raw-report.json \\
        --output benchmarks/Semgrep/findings/semgrep-findings.json

    # Then POST to VulnerableApp:
    curl -X POST http://localhost:9090/VulnerableApp/scanner/benchmark \\
        -H "Content-Type: application/json" \\
        -d @benchmarks/Semgrep/findings/semgrep-findings.json

Input format (semgrep --json)
-----------------------------
Semgrep writes a top-level "results" array. Each result looks like:

    {
      "check_id": "java.lang.security.audit.tainted-sql-string...",
      "path": "src/main/java/.../ErrorBasedSQLInjectionVulnerability.java",
      "start": { "line": 143, "col": 21 },
      "extra": {
        "metadata": {
          "cwe": ["CWE-89: Improper Neutralization of Special Elements ..."],
          "vulnerability_class": ["SQL Injection"]
        }
      }
    }

One finding is emitted per result.

Output format (VulnerableApp benchmark input)
---------------------------------------------
    {
      "tool": "Semgrep",
      "scanType": "SAST",
      "findings": [
        {
          "filePath": "src/main/java/.../ErrorBasedSQLInjectionVulnerability.java",
          "line": 143,
          "cwe": "CWE-89",
          "type": "SQL Injection"
        },
        ...
      ]
    }

Notes
-----
- line is the start line of the result.
- cwe is the first CWE ID in the rule metadata, without its description.
- type is the first vulnerability_class of the rule, passed through as-is.
  Like the ZAP converter, the script does not hard-code a mapping to
  VulnerableApp's type names; CWE matching is the main axis.
- Duplicate (filePath, line, cwe, type) tuples are de-duplicated.
"""

import argparse
import json
import re
import sys
from pathlib import Path
from typing import Optional

_CWE_ID = re.compile(r"CWE-\d+", re.IGNORECASE)


def _first(value) -> Optional[str]:
    """Semgrep metadata fields can be a string or a list of strings."""
    if isinstance(value, list):
        value = value[0] if value else None
    if isinstance(value, str) and value.strip():
        return value.strip()
    return None


def _cwe_tag(raw) -> Optional[str]:
    """Extract the ID from a Semgrep CWE label (e.g. 'CWE-89: ...' -> 'CWE-89')."""
    match = _CWE_ID.search(_first(raw) or "")
    return match.group(0).upper() if match else None


def convert(semgrep_report: dict) -> dict:
    """
    Convert a parsed Semgrep JSON report dict into a benchmark input dict.
    """
    seen: set[tuple] = set()
    findings: list[dict] = []

    for result in semgrep_report.get("results", []):
        path = result.get("path")
        line = result.get("start", {}).get("line")
        if not path or not line:
            continue

        metadata = result.get("extra", {}).get("metadata", {})
        cwe = _cwe_tag(metadata.get("cwe"))
        vuln_type = _first(metadata.get("vulnerability_class"))

        key = (path, line, cwe, vuln_type)
        if key in seen:
            continue
        seen.add(key)

        finding: dict = {"filePath": path, "line": line}
        if cwe:
            finding["cwe"] = cwe
        if vuln_type:
            finding["type"] = vuln_type

        findings.append(finding)

    return {
        "tool": "Semgrep",
        "scanType": "SAST",
        "findings": findings,
    }


def main() -> None:
    parser = argparse.ArgumentParser(
        description="Convert a Semgrep JSON report to VulnerableApp benchmark input format."
    )
    parser.add_argument(
        "--input", "-i",
        required=True,
        help="Path to the report written by `semgrep scan --json`.",
    )
    parser.add_argument(
        "--output", "-o",
        default="benchmarks/Semgrep/findings/semgrep-findings.json",
        help="Path to write the benchmark input JSON (default: benchmarks/Semgrep/findings/semgrep-findings.json).",
    )
    args = parser.parse_args()

    input_path = Path(args.input)
    output_path = Path(args.output)

    if not input_path.exists():
        print(f"ERROR: input file not found: {input_path}", file=sys.stderr)
        sys.exit(1)

    with input_path.open(encoding="utf-8") as fh:
        semgrep_report = json.load(fh)

    benchmark_input = convert(semgrep_report)

    output_path.parent.mkdir(parents=True, exist_ok=True)
    with output_path.open("w", encoding="utf-8") as fh:
        json.dump(benchmark_input, fh, indent=2)

    total = len(benchmark_input["findings"])
    print(f"Converted {total} finding(s) -> {output_path}")


if __name__ == "__main__":
    main()
