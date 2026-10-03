#!/usr/bin/env python3
"""
convert_wapiti_to_benchmark.py
==============================
Converts a Wapiti JSON report (-f json) into the JSON input format expected
by VulnerableApp's POST /VulnerableApp/scanner/benchmark endpoint.

Usage:
    python3 convert_wapiti_to_benchmark.py \\
        --input  benchmarks/Wapiti/wapiti-raw-report.json \\
        --output benchmarks/Wapiti/findings/wapiti-findings.json
"""

import argparse
import json
import re
import sys
from pathlib import Path
from typing import Optional


CATEGORY_FALLBACK_CWE = {
    "sql injection": "CWE-89",
    "blind sql injection": "CWE-89",
    "reflected cross site scripting": "CWE-79",
    "stored cross site scripting": "CWE-79",
    "command execution": "CWE-78",
    "path traversal": "CWE-22",
    "server side request forgery": "CWE-918",
    "ldap injection": "CWE-90",
    "open redirect": "CWE-601",
    "unrestricted file upload": "CWE-434",
    "crlf injection": "CWE-93",
    "cross site request forgery": "CWE-352",
    "cleartext submission of password": "CWE-319",
    "weak credentials": "CWE-798",
    "resource consumption": "CWE-400",
}

CATEGORY_TO_TYPE_NAME = {
    "command execution": "COMMAND_INJECTION",
    "clickjacking protection": "CLICKJACKING",
}


def _extract_cwe_from_classifications(category: str, classifications: dict) -> Optional[str]:
    """Extract CWE-XXX from classifications ref block (keys or URLs) or fallback table."""
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
    return CATEGORY_FALLBACK_CWE.get(category.strip().lower())


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
