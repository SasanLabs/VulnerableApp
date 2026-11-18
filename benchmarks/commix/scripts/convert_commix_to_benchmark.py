#!/usr/bin/env python3
"""
convert_commix_to_benchmark.py
==============================
Converts Commix's text log (logs.txt) into the JSON input format expected by
VulnerableApp's POST /VulnerableApp/scanner/benchmark endpoint.

Usage
-----
    python3 convert_commix_to_benchmark.py \
        --input  benchmarks/commix/artifacts/commix-output \
        --output benchmarks/commix/findings/commix-findings.json

``--input`` is either a single ``logs.txt`` or the Commix ``--output-dir``, in
which case every ``logs.txt`` below it is read.

Input format (Commix logs.txt)
------------------------------
Commix appends one block per scanned target. A target with a confirmed
injection lists the injection point right after the tested URL:

    [info] Tested URL : http://localhost/VulnerableApp/CommandInjection/LEVEL_1?ipaddress=127.0.0.1
    [info] Type: Results-Based Os Command Injection
    [info] Technique: Classic Command Injection Technique
    [info] Parameter: (GET) ipaddress

A target without an injection only has the ``Tested URL`` line.

Output format (VulnerableApp benchmark input)
----------------------------------------------
    {
      "tool": "Commix",
      "scanType": "DAST",
      "findings": [
        {
          "url": "http://localhost/VulnerableApp/CommandInjection/LEVEL_1",
          "method": "GET",
          "type": "COMMAND_INJECTION"
        }
      ]
    }

Notes
-----
- Every injection point Commix reports becomes a finding. Matching against the
  ground truth, including the method and de-duplication, is done by the
  benchmark endpoint.
- URLs are cleaned to remove query parameters and fragments.
- ``COMMAND_INJECTION`` is the canonical vulnerability type (CWE-77, WASC-31).
"""

import argparse
import json
import re
import sys
from pathlib import Path
from urllib.parse import urlsplit, urlunsplit

VULNERABILITY_TYPE = "COMMAND_INJECTION"

_TESTED_URL = re.compile(r"^\[info\]\s*Tested URL\s*:\s*(\S+)\s*$")
_INJECTION_TYPE = re.compile(r"^\[info\]\s*Type\s*:\s*.+$")
_PARAMETER = re.compile(r"^\[info\]\s*Parameter\s*:\s*\((?P<method>[A-Z]+)\)\s+\S.*$")


def clean_url(url):
    parts = urlsplit(url)
    return urlunsplit((parts.scheme, parts.netloc, parts.path.rstrip("/") or "/", "", ""))


def convert_log(text):
    """Return the benchmark findings for the contents of one Commix logs.txt."""
    findings = []
    url = None
    in_injection = False
    for line in text.splitlines():
        line = line.rstrip()
        match = _TESTED_URL.match(line)
        if match:
            url = clean_url(match.group(1))
            in_injection = False
        elif _INJECTION_TYPE.match(line):
            in_injection = True
        elif in_injection and url:
            match = _PARAMETER.match(line)
            if match:
                findings.append(
                    {"url": url, "method": match.group("method"), "type": VULNERABILITY_TYPE}
                )
                in_injection = False
    return findings


def convert(logs):
    return {"tool": "Commix", "scanType": "DAST", "findings": [f for log in logs for f in convert_log(log)]}


def read_logs(path):
    files = [path] if path.is_file() else sorted(path.rglob("logs.txt"))
    if not files:
        raise FileNotFoundError("No Commix logs.txt found in " + str(path))
    return [file.read_text(errors="replace") for file in files]


def main():
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("--input", required=True, type=Path, help="logs.txt or the Commix output directory")
    parser.add_argument("--output", required=True, type=Path)
    args = parser.parse_args()
    try:
        result = convert(read_logs(args.input))
    except OSError as error:
        sys.exit(str(error))
    args.output.parent.mkdir(parents=True, exist_ok=True)
    args.output.write_text(json.dumps(result, indent=2) + "\n")
    print("Converted %d finding(s)" % len(result["findings"]))


if __name__ == "__main__":
    main()
