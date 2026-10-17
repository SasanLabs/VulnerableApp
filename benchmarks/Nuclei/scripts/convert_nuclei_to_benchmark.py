"""
convert_nuclei_to_benchmark.py
==============================
Convert a Nuclei result file into the VulnerableApp benchmark input format.

Input format (Nuclei)
---------------------
Nuclei writes one JSON object per line (``-jsonl``). Each object carries the
template that matched, where it matched, and the request that triggered it::

    {"template-id":"sql-injection",
     "info":{"name":"SQL Injection","severity":"high",
             "classification":{"cwe-id":["CWE-89"]}},
     "type":"http",
     "host":"http://localhost:9090",
     "matched-at":"http://localhost:9090/VulnerableApp/SQLInjection/LEVEL_1",
     "request":"GET /VulnerableApp/SQLInjection/LEVEL_1 HTTP/1.1\\r\\nHost: localhost:9090\\r\\n"}

A JSON array holding the same objects is accepted as well, so a file assembled
from several runs converts without being re-serialised by hand.

One Finding is emitted per (matched-at, CWE, method) tuple.

Output format (VulnerableApp benchmark input)
---------------------------------------------
    {
      "tool": "Nuclei",
      "scanType": "DAST",
      "findings": [
        {
          "url":    "http://localhost:9090/VulnerableApp/SQLInjection/LEVEL_1",
          "cwe":    "CWE-89",
          "method": "GET"
        },
        ...
      ]
    }

Notes
-----
- Nuclei reports CWE ids under ``info.classification.cwe-id``, usually as a
  one-element list. The first entry is forwarded as ``cwe``; the benchmark
  accepts both bare digits (``89``) and the ``CWE-`` prefix and normalises them.
- ``method`` comes from the request line when ``request`` starts with an HTTP
  method, and is omitted otherwise. Omitting it keeps the benchmark's lenient
  URL-only matching, which is what a template that only records ``host`` needs.
- ``matched-at`` is forwarded as it stands. The comparator already strips query
  strings, the ``/VulnerableApp`` context path and trailing slashes, so no
  cleaning happens here.
- Like the ZAP converter, this does NOT hard-code a Nuclei-template-name to
  VulnerabilityType mapping. The benchmark matches ``type`` against the
  ``VulnerabilityType`` enum name, and a template name such as ``"SQL Injection"``
  is not one of those, so ``info.name`` is not emitted and CWE matching carries
  the comparison.
"""

import argparse
import json
import sys
from pathlib import Path
from typing import Optional

# Nouns a request line can open with. Listed explicitly so that a relative path
# which happens to be spelled like a verb is not mistaken for one.
_HTTP_METHODS = frozenset(
    {"GET", "POST", "PUT", "PATCH", "DELETE", "HEAD", "OPTIONS", "TRACE", "CONNECT"}
)


def _load_results(path: Path) -> list:
    """
    Read a Nuclei output file.

    Accepts the JSONL that ``nuclei -jsonl`` writes, a JSON array of the same
    objects, or a single object. Returns a list of parsed results.
    """
    text = path.read_text(encoding="utf-8").strip()
    if not text:
        return []

    try:
        parsed = json.loads(text)
    except json.JSONDecodeError:
        parsed = None

    if isinstance(parsed, list):
        return parsed
    if isinstance(parsed, dict):
        return [parsed]

    results = []
    for line in text.splitlines():
        line = line.strip()
        if line:
            results.append(json.loads(line))
    return results


def _cwe_tag(raw) -> Optional[str]:
    """Return the first CWE id, prefixed with 'CWE-' when it is bare digits."""
    if isinstance(raw, (list, tuple)):
        raw = raw[0] if raw else None
    if raw is None:
        return None
    value = str(raw).strip()
    if value in ("", "0"):
        return None
    return value if value.upper().startswith("CWE-") else f"CWE-{value}"


def _method_from_request(request) -> Optional[str]:
    """Read the HTTP method off the first line of a raw request, if it has one."""
    if not isinstance(request, str) or not request.strip():
        return None
    parts = request.strip().splitlines()[0].split()
    if len(parts) >= 2 and parts[0].upper() in _HTTP_METHODS:
        return parts[0].upper()
    return None


def convert(results: list) -> dict:
    """
    Convert parsed Nuclei result objects into a benchmark input dict.
    """
    seen: set[tuple] = set()
    findings: list[dict] = []

    for result in results:
        if not isinstance(result, dict):
            continue

        url = str(result.get("matched-at") or result.get("host") or "").strip()
        if not url:
            continue

        info = result.get("info") or {}
        classification = info.get("classification") or {}
        cwe = _cwe_tag(classification.get("cwe-id"))
        method = _method_from_request(result.get("request"))

        key = (url, cwe, method)
        if key in seen:
            continue
        seen.add(key)

        finding: dict = {"url": url}
        if cwe:
            finding["cwe"] = cwe
        if method:
            finding["method"] = method

        findings.append(finding)

    return {
        "tool": "Nuclei",
        "scanType": "DAST",
        "findings": findings,
    }


def main() -> None:
    parser = argparse.ArgumentParser(
        description="Convert a Nuclei result file to VulnerableApp benchmark input format."
    )
    parser.add_argument(
        "--input", "-i",
        required=True,
        help="Path to the Nuclei output file (JSONL, or a JSON array of the same objects).",
    )
    parser.add_argument(
        "--output", "-o",
        default="benchmarks/Nuclei/nuclei-benchmark-input.json",
        help="Path to write the benchmark input JSON (default: benchmarks/Nuclei/nuclei-benchmark-input.json).",
    )
    args = parser.parse_args()

    input_path = Path(args.input)
    output_path = Path(args.output)

    if not input_path.exists():
        print(f"ERROR: input file not found: {input_path}", file=sys.stderr)
        sys.exit(1)

    results = _load_results(input_path)
    benchmark_input = convert(results)

    output_path.parent.mkdir(parents=True, exist_ok=True)
    with output_path.open("w", encoding="utf-8") as fh:
        json.dump(benchmark_input, fh, indent=2)

    total = len(benchmark_input["findings"])
    print(f"Converted {total} finding(s)  →  {output_path}")


if __name__ == "__main__":
    main()
