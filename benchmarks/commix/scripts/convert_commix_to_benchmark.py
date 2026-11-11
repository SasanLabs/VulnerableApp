#!/usr/bin/env python3
"""Convert a completed commix run manifest to VulnerableApp benchmark findings.

Only an endpoint on which commix proved command execution — by echoing back the
unique marker the runner asked it to print — becomes a finding. A clean negative
(commix's "do not appear to be injectable" conclusion) yields no finding; any
other error, a timed-out or non-zero scan, or a missing marker fails conversion
so a broken run can never masquerade as a clean empty result.
"""
import argparse
import json
from pathlib import Path
import re
from urllib.parse import urlsplit, urlunsplit

# Release tags report e.g. commix/v4.1; untagged builds report commix/v4.2.dev311.
STABLE_VERSION = re.compile(r"commix/v\d+(?:\.\d+)*")
# commix's exact completed-negative conclusion, printed as a [critical] message.
NEGATIVE_CONCLUSION = "All tested parameters do not appear to be injectable."
# The canonical VulnerabilityType for the CommandInjection lab (CWE-77, WASC-31).
VULNERABILITY_TYPE = "COMMAND_INJECTION"


def clean_url(value):
    if not isinstance(value, str):
        raise ValueError("Missing target URL")
    parts = urlsplit(value.strip())
    if parts.scheme not in ("http", "https") or not parts.netloc or parts.username or parts.password:
        raise ValueError("Expected an HTTP(S) target URL without credentials")
    path = parts.path.rstrip("/") or "/"
    return urlunsplit((parts.scheme, parts.netloc, path, "", ""))


def _executed_marker(report, marker):
    """Whether commix echoed our unique marker back, i.e. proved code execution."""
    executions = report.get("executions")
    if not isinstance(executions, list):
        raise ValueError("Native report is missing its executions list")
    command = "echo " + marker
    for execution in executions:
        if not isinstance(execution, dict):
            raise ValueError("Invalid execution record")
        if execution.get("command") == command and str(execution.get("output", "")).strip() == marker:
            return True
    return False


def convert(envelope):
    if not isinstance(envelope, dict) or envelope.get("schemaVersion") != 1:
        raise ValueError("Expected a commix benchmark run manifest (schemaVersion 1)")
    runs = envelope.get("runs")
    version = envelope.get("commixVersion")
    if (not isinstance(version, str) or not STABLE_VERSION.fullmatch(version)
            or not isinstance(runs, list) or not runs):
        raise ValueError("Missing runs or unsupported commix version")
    if envelope.get("targetCount") != len(runs):
        raise ValueError("Not all planned targets were scanned")
    findings, seen = [], set()
    for run in runs:
        if not isinstance(run, dict) or run.get("completed") is not True or run.get("returncode") != 0:
            raise ValueError("Scan failed or did not complete; inspect run artifacts")
        url = clean_url(run.get("url"))
        method = str(run.get("method", "")).upper()
        if method not in ("GET", "POST"):
            raise ValueError("Unknown request method")
        marker = run.get("marker")
        if not isinstance(marker, str) or not marker:
            raise ValueError("Run is missing the command-execution marker")
        report = run.get("report")
        if not isinstance(report, dict):
            raise ValueError("Native commix report is missing")
        messages = run.get("diagnostics")
        if not isinstance(messages, list) or any(not isinstance(m, str) for m in messages):
            raise ValueError("Missing scanner diagnostics")
        confirmed = _executed_marker(report, marker)
        negative = bool(messages) and all(m.startswith(NEGATIVE_CONCLUSION) for m in messages)
        if confirmed and messages:
            raise ValueError("Conflicting command execution and negative conclusion")
        if not confirmed and not negative:
            raise ValueError(
                "commix neither confirmed execution nor reported a completed negative: "
                + ("; ".join(messages) if messages else "no conclusion")
            )
        if confirmed and (url, method) not in seen:
            seen.add((url, method))
            findings.append({"url": url, "method": method, "type": VULNERABILITY_TYPE})
    return {"tool": "Commix", "scanType": "DAST", "findings": findings}


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--input", required=True, type=Path)
    parser.add_argument("--output", required=True, type=Path)
    args = parser.parse_args()
    try:
        result = convert(json.loads(args.input.read_text()))
        args.output.parent.mkdir(parents=True, exist_ok=True)
        args.output.write_text(json.dumps(result, indent=2) + "\n")
        print("Converted %d confirmed endpoint finding(s)" % len(result["findings"]))
    except (ValueError, OSError) as error:
        parser.exit(1, str(error) + "\n")


if __name__ == "__main__":
    main()
