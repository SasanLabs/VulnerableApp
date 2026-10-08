#!/usr/bin/env python3
"""
convert_wapiti_to_benchmark.py
==============================
Converts a Wapiti JSON report (-f json) into the JSON input format expected
by VulnerableApp's POST /VulnerableApp/scanner/benchmark endpoint.

Usage
-----
    python3 convert_wapiti_to_benchmark.py \
        --input  benchmarks/Wapiti/wapiti-raw-report.json \
        --output benchmarks/Wapiti/findings/wapiti-findings.json

    # Then POST to VulnerableApp:
    curl -X POST http://localhost/VulnerableApp/scanner/benchmark \
        -H "Content-Type: application/json" \
        -d @benchmarks/Wapiti/findings/wapiti-findings.json

Input format (Wapiti JSON)
--------------------------
Wapiti writes a root JSON dictionary containing 'classifications',
'vulnerabilities', 'anomalies', and scan metadata.
- 'classifications' contains vulnerability definitions and external references,
  including CWE mappings:
    {
      "classifications": {
        "SQL Injection": {
          "desc": "...",
          "sol": "...",
          "ref": {
            "CWE-89: SQL Injection": "https://cwe.mitre.org/data/definitions/89.html"
          }
        }
      }
    }
- 'vulnerabilities' (and 'anomalies') maps each vulnerability category name to
  a list of detected occurrences:
    {
      "vulnerabilities": {
        "SQL Injection": [
          {
            "method": "GET",
            "path": "/VulnerableApp/SQLInjection/LEVEL_1?id=1",
            "info": "...",
            "parameter": "id",
            "http_request": "..."
          }
        ]
      }
    }

Output format (VulnerableApp benchmark input)
----------------------------------------------
    {
      "tool": "Wapiti",
      "scanType": "DAST",
      "findings": [
        {
          "url": "/VulnerableApp/SQLInjection/LEVEL_1",
          "method": "GET",
          "cwe": "CWE-89"
        }
      ]
    }

Notes
-----
- CWE identifiers are extracted dynamically from the category's 'ref' mapping
  under 'classifications'.
- If a vulnerability category lacks a CWE reference, 'cwe' is omitted and the
  raw category name is emitted as 'type' for unmatched benchmark reporting.
- URLs are cleaned to remove query parameters, fragments, matrix parameters,
  and trailing slashes.
- Findings are de-duplicated on (url, method, cwe, type).
"""

import argparse
import json
import re
import sys
from pathlib import Path
from typing import Optional


CATEGORY_TO_TYPE_NAME = {
    "command execution": "COMMAND_INJECTION",
    "clickjacking protection": "CLICKJACKING",
}


def _extract_cwe_from_classifications(category: str, classifications: dict) -> Optional[str]:
    """Extract CWE-XXX from classifications ref block (keys or URLs)."""
    cat_info = classifications.get(category, {}) if isinstance(classifications, dict) else {}
    refs = cat_info.get("ref", {}) if isinstance(cat_info, dict) else {}
    if isinstance(refs, dict):
        for ref_name, ref_url in refs.items():
            match = re.search(r"CWE-(\d+)", ref_name, re.I) or re.search(r"CWE-(\d+)", str(ref_url), re.I)
            if match:
                return f"CWE-{match.group(1)}"
            url_match = re.search(r"definitions/(\d+)\.html", str(ref_url), re.I)
            if url_match:
                return f"CWE-{url_match.group(1)}"
    return None


def _clean_url(url: str) -> str:
    """Normalize path by stripping query strings, fragments, matrix params, and trailing slashes."""
    if not url:
        return ""
    cleaned = re.sub(r";[^/]*", "", url)
    cleaned = cleaned.split("?")[0].split("#")[0].strip()
    if len(cleaned) > 1 and cleaned.endswith("/"):
        cleaned = cleaned.rstrip("/")
    elif cleaned == "/":
        return ""
    return cleaned


def convert(wapiti_report: dict) -> dict:
    """
    Convert a parsed Wapiti JSON report dict into a VulnerableApp benchmark input dict.
    """
    findings: list[dict] = []
    seen: set[tuple] = set()

    if not isinstance(wapiti_report, dict):
        return {
            "tool": "Wapiti",
            "scanType": "DAST",
            "findings": [],
        }

    classifications = wapiti_report.get("classifications", {})
    vuln_sections = [
        wapiti_report.get("vulnerabilities", {}),
        wapiti_report.get("anomalies", {})
    ]

    for section in vuln_sections:
        if not isinstance(section, dict):
            continue
        for category, instances in section.items():
            if not isinstance(instances, list):
                continue

            cwe = _extract_cwe_from_classifications(category, classifications)
            type_name = CATEGORY_TO_TYPE_NAME.get(category.strip().lower())
            if not cwe and not type_name:
                type_name = category.strip()

            for inst in instances:
                if not isinstance(inst, dict):
                    continue

                raw_url = inst.get("path") or inst.get("url") or ""
                url = _clean_url(raw_url)
                if not url:
                    continue

                method = (inst.get("method") or "GET").strip().upper() or None
                key = (url, method, cwe, type_name)
                if key in seen:
                    continue
                seen.add(key)

                finding: dict = {"url": url}
                if cwe:
                    finding["cwe"] = cwe
                if type_name:
                    finding["type"] = type_name
                if method:
                    finding["method"] = method

                findings.append(finding)

    return {
        "tool": "Wapiti",
        "scanType": "DAST",
        "findings": findings,
    }


def main() -> None:
    parser = argparse.ArgumentParser(
        description="Convert Wapiti JSON report to VulnerableApp benchmark input format."
    )
    parser.add_argument(
        "--input", "-i",
        required=True,
        help="Path to raw Wapiti JSON report (-f json).",
    )
    parser.add_argument(
        "--output", "-o",
        default="benchmarks/Wapiti/findings/wapiti-findings.json",
        help="Path to write the benchmark input JSON.",
    )
    args = parser.parse_args()

    input_path = Path(args.input)
    output_path = Path(args.output)

    if not input_path.exists():
        print(f"ERROR: input file not found: {input_path}", file=sys.stderr)
        sys.exit(1)

    with input_path.open(encoding="utf-8") as fh:
        raw_report = json.load(fh)

    benchmark_input = convert(raw_report)

    output_path.parent.mkdir(parents=True, exist_ok=True)
    with output_path.open("w", encoding="utf-8") as fh:
        json.dump(benchmark_input, fh, indent=2)

    print(f"Converted {len(benchmark_input['findings'])} finding(s) -> {output_path}")


if __name__ == "__main__":
    main()
