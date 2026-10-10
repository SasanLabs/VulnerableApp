#!/usr/bin/env python3
"""
convert_w3af_to_benchmark.py
=============================
Converts a w3af JSON report (``json_file`` output plugin) into the JSON input
format expected by VulnerableApp's POST /scanner/benchmark endpoint.

Usage
-----
    python3 convert_w3af_to_benchmark.py \\
        --input  benchmarks/w3af/w3af-raw.json \\
        --output benchmarks/w3af/w3af-benchmark-input.json

    # Then POST to VulnerableApp:
    curl -X POST http://localhost/VulnerableApp/scanner/benchmark \\
        -H "Content-Type: application/json" \\
        -d @benchmarks/w3af/w3af-benchmark-input.json

Input format (w3af ``json_file``)
---------------------------------
The ``json_file`` output plugin writes one object with a top-level ``items``
array, one entry per identified finding:

    {
      "w3af-version": "1.6.0",
      "scan-info": { "target_urls": [...], "known_urls": [...], ... },
      "start": "1759900000",
      "start-long": "Tue Oct  7 12:00:00 2025",
      "items": [
        {
          "Severity": "High",
          "Name": "Cross Site Scripting",
          "HTTP method": "GET",
          "URL": "http://localhost/VulnerableApp/XSS/LEVEL_1?text=...",
          "Vulnerable parameter": "text",
          "POST data": "...",            # base64, only for POST
          "Vulnerability IDs": "...",
          "CWE IDs": [79],
          "WASC IDs": [8],
          "Tags": [...],
          "VulnDB ID": "...",
          "Description": "..."
        },
        ...
      ]
    }

Only ``Name``, ``HTTP method``, ``URL``, ``CWE IDs`` and ``WASC IDs`` are read.
The rest is w3af's own reporting surface and has no counterpart on the
benchmark side.

Output format (VulnerableApp benchmark input)
----------------------------------------------
    {
      "tool": "w3af",
      "scanType": "DAST",
      "findings": [
        {
          "url":    "/XSS/LEVEL_1",
          "cwe":    "CWE-79",
          "wascId": "8",
          "method": "GET"
        },
        ...
      ]
    }

Notes
-----
- CWE and WASC ids are forwarded as-is; the benchmark endpoint normalises them
  ("79" and "CWE-79" both match). An id of 0 counts as absent, same rule the
  ZAP converter uses.
- The query string is dropped from the URL. w3af reports the URL it sent the
  payload on, so the injection parameters are in the query; VulnerableApp's
  ground truth is keyed on the path (``/XSS/LEVEL_1``), so keeping the query
  would guarantee a miss. The path is what both tools agree on.
- Duplicate (url, cwe, wascId, method) tuples are de-duplicated, because w3af
  can report the same finding from more than one plugin.
- The script intentionally does NOT map w3af plugin names to
  ``VulnerabilityType`` values. CWE/WASC matching is preferred and already
  sufficient for most w3af findings against VulnerableApp's ground truth, and a
  name table would be one more thing to keep in sync as plugins are renamed.
"""

import argparse
import json
import sys
from pathlib import Path
from typing import Optional
from urllib.parse import urlsplit


def _first_id(raw: object) -> Optional[str]:
    """w3af emits CWE/WASC ids as a list; take the first non-zero entry.

    Returns None for a missing, empty or zero id, so a finding with no id keeps
    matching on whatever else it carries rather than on a literal "0".
    """
    if raw is None:
        return None
    values = raw if isinstance(raw, (list, tuple)) else [raw]
    for value in values:
        if value is None:
            continue
        text = str(value).strip()
        if text not in ("", "0"):
            return text
    return None


def _cwe_tag(raw: object) -> Optional[str]:
    """Prefix a bare id with 'CWE-' (79 -> 'CWE-79')."""
    value = _first_id(raw)
    if value is None:
        return None
    return value if value.upper().startswith("CWE-") else f"CWE-{value}"


def _path_only(url: str) -> str:
    """Drop the scheme, host and query, keeping the path.

    w3af reports the URL it actually sent the payload on, so the query carries
    injection parameters. Ground truth is the path.
    """
    cleaned = (url or "").strip()
    if not cleaned:
        return ""
    # urlsplit needs a scheme to populate .path; a bare "/XSS/LEVEL_1" already
    # is one, and split() on it would throw away the leading segment.
    if "://" not in cleaned:
        return cleaned.split("?", 1)[0].split("#", 1)[0]
    return urlsplit(cleaned).path


def _method(raw: object) -> Optional[str]:
    """Upper-case the HTTP method, or None when w3af left it blank."""
    text = str(raw or "").strip().upper()
    return text or None


def convert(w3af_report: dict) -> dict:
    """Convert a parsed w3af ``json_file`` report into a benchmark input dict."""
    seen: set[tuple] = set()
    findings: list[dict] = []

    items = w3af_report.get("items", []) if isinstance(w3af_report, dict) else []
    if not isinstance(items, list):
        items = []

    for item in items:
        if not isinstance(item, dict):
            continue

        url = _path_only(item.get("URL", ""))
        if not url:
            continue

        cwe = _cwe_tag(item.get("CWE IDs"))
        wasc = _first_id(item.get("WASC IDs"))
        method = _method(item.get("HTTP method"))

        key = (url, cwe, wasc, method)
        if key in seen:
            continue
        seen.add(key)

        finding: dict = {"url": url}
        if cwe:
            finding["cwe"] = cwe
        if wasc:
            finding["wascId"] = wasc
        if method:
            finding["method"] = method

        findings.append(finding)

    return {
        "tool": "w3af",
        "scanType": "DAST",
        "findings": findings,
    }


def main() -> None:
    parser = argparse.ArgumentParser(
        description="Convert a w3af JSON report to VulnerableApp benchmark input format."
    )
    parser.add_argument(
        "--input", "-i",
        required=True,
        help="Path to the JSON file produced by w3af's json_file output plugin.",
    )
    parser.add_argument(
        "--output", "-o",
        default="benchmarks/w3af/w3af-benchmark-input.json",
        help="Path to write the benchmark input JSON (default: benchmarks/w3af/w3af-benchmark-input.json).",
    )
    args = parser.parse_args()

    input_path = Path(args.input)
    output_path = Path(args.output)

    if not input_path.exists():
        print(f"ERROR: input file not found: {input_path}", file=sys.stderr)
        sys.exit(1)

    with input_path.open(encoding="utf-8") as fh:
        w3af_report = json.load(fh)

    benchmark_input = convert(w3af_report)

    output_path.parent.mkdir(parents=True, exist_ok=True)
    with output_path.open("w", encoding="utf-8") as fh:
        json.dump(benchmark_input, fh, indent=2)

    total = len(benchmark_input["findings"])
    print(f"Converted {total} finding(s)  →  {output_path}")


if __name__ == "__main__":
    main()
