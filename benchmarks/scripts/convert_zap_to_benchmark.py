#!/usr/bin/env python3
"""
convert_zap_to_benchmark.py
============================
Converts a ZAP JSON report (traditional-json template) into the JSON input
format expected by VulnerableApp's POST /scanner/benchmark endpoint.

Usage
-----
    python3 convert_zap_to_benchmark.py \\
        --input  benchmarks/zap-raw-report.json \\
        --output benchmarks/zap-benchmark-input.json

    # Then POST to VulnerableApp:
    curl -X POST http://localhost:9090/VulnerableApp/scanner/benchmark \\
        -H "Content-Type: application/json" \\
        -d @benchmarks/zap-benchmark-input.json

Input format (ZAP traditional-json)
------------------------------------
ZAP writes a top-level "site" array. Each site has an "alerts" array.
Each alert looks like:

    {
      "name": "SQL Injection",
      "cweid": "89",
      "wascid": "19",
      "instances": [
        { "uri": "http://localhost:9090/VulnerableApp/SQLInjection/LEVEL_1",
          "method": "GET", ... }
      ]
    }

One Finding is emitted per (alert, instance) pair — i.e. per unique URL+method.

Output format (VulnerableApp benchmark input)
----------------------------------------------
    {
      "tool": "ZAP",
      "scanType": "DAST",
      "findings": [
        {
          "url":    "http://localhost:9090/VulnerableApp/SQLInjection/LEVEL_1",
          "cwe":    "CWE-89",
          "wascId": "19",
          "method": "GET"
        },
        ...
      ]
    }

Notes
-----
- cwe / wascId are forwarded as-is from ZAP; the benchmark endpoint normalises
  them ("89" and "CWE-89" both match).
- Duplicate (url, cwe, wascId, method) tuples are de-duplicated.
- Alerts with cweid == "0" or wascid == "0" have those fields omitted so the
  benchmark falls back to type-name matching if type is supplied separately.
- The script intentionally does NOT hard-code a ZAP-alert-name → VulnerabilityType
  mapping; CWE/WASC matching is preferred and already sufficient for most ZAP
  findings against VulnerableApp's ground truth.
"""

import argparse
import json
import sys
from pathlib import Path
from typing import Optional


def _normalise_id(raw: Optional[str]) -> Optional[str]:
    """Return None for missing/zero IDs, otherwise the raw string value."""
    if not raw:
        return None
    stripped = str(raw).strip()
    return None if stripped in ("", "0") else stripped


def _cwe_tag(raw: Optional[str]) -> Optional[str]:
    """Prefix bare digits with 'CWE-' (e.g. '89' → 'CWE-89')."""
    val = _normalise_id(raw)
    if val is None:
        return None
    return val if val.upper().startswith("CWE-") else f"CWE-{val}"


def convert(zap_report: dict) -> dict:
    """
    Convert a parsed ZAP traditional-json report dict into a benchmark input dict.
    """
    seen: set[tuple] = set()
    findings: list[dict] = []

    sites = zap_report.get("site", [])
    # The report may be wrapped: {"@version":…, "site":[…]}  OR the array itself.
    if isinstance(sites, dict):
        sites = [sites]

    for site in sites:
        for alert in site.get("alerts", []):
            cwe    = _cwe_tag(alert.get("cweid"))
            wasc   = _normalise_id(alert.get("wascid"))

            for instance in alert.get("instances", []):
                url    = instance.get("uri", "").strip()
                method = instance.get("method", "").strip().upper() or None

                if not url:
                    continue

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
        "tool": "ZAP",
        "scanType": "DAST",
        "findings": findings,
    }


def main() -> None:
    parser = argparse.ArgumentParser(
        description="Convert a ZAP JSON report to VulnerableApp benchmark input format."
    )
    parser.add_argument(
        "--input", "-i",
        required=True,
        help="Path to zap-raw-report.json produced by ZAP's traditional-json template.",
    )
    parser.add_argument(
        "--output", "-o",
        default="benchmarks/zap-benchmark-input.json",
        help="Path to write the benchmark input JSON (default: benchmarks/zap-benchmark-input.json).",
    )
    args = parser.parse_args()

    input_path  = Path(args.input)
    output_path = Path(args.output)

    if not input_path.exists():
        print(f"ERROR: input file not found: {input_path}", file=sys.stderr)
        sys.exit(1)

    with input_path.open(encoding="utf-8") as fh:
        zap_report = json.load(fh)

    benchmark_input = convert(zap_report)

    output_path.parent.mkdir(parents=True, exist_ok=True)
    with output_path.open("w", encoding="utf-8") as fh:
        json.dump(benchmark_input, fh, indent=2)

    total = len(benchmark_input["findings"])
    print(f"Converted {total} finding(s)  →  {output_path}")


if __name__ == "__main__":
    main()
