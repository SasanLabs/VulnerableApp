#!/usr/bin/env python3
"""Convert a completed Dalfox run manifest to VulnerableApp benchmark findings.

Only Dalfox's ``V`` (Vulnerable) tier with CWE-79 on the scanned endpoint
becomes a benchmark finding: Dalfox parsed the response and found the payload
in an executable position. ``R`` (reflection-only), ``A`` (AST) and ``I``
(informational) results, and confirmed results whose proof-of-concept URL is
not the scanned endpoint, are kept as observations and never submitted. A
skipped, incomplete, timed-out or errored scan fails conversion so a broken
run can never masquerade as a clean empty result.
"""
import argparse
import json
from pathlib import Path
import re
from urllib.parse import urlsplit, urlunsplit

RELEASE_VERSION = re.compile(r"\d+\.\d+\.\d+")
# Dalfox exit codes: 0 = completed without findings, 1 = completed with findings.
COMPLETED_EXIT_CODES = {0: False, 1: True}
COMPLETED_STATUSES = {"clean": False, "findings": True}
CONFIRMED_TIER = "V"
XSS_CWE = "CWE-79"
# The canonical VulnerabilityType of every in-scope ground-truth row (CWE-79, WASC-8).
VULNERABILITY_TYPE = "REFLECTED_XSS"


def clean_url(value):
    if not isinstance(value, str):
        raise ValueError("Missing target URL")
    parts = urlsplit(value.strip())
    if parts.scheme not in ("http", "https") or not parts.netloc or parts.username or parts.password:
        raise ValueError("Expected an HTTP(S) target URL without credentials")
    path = parts.path.rstrip("/") or "/"
    return urlunsplit((parts.scheme, parts.netloc, path, "", ""))


def _check_preflight(run):
    preflight = run.get("preflight")
    if (not isinstance(preflight, dict) or preflight.get("status") != 200
            or preflight.get("contentType") != "text/html"
            or preflight.get("seedReflected") is not True):
        raise ValueError("Seed request was not reflected as HTML on %s; the scan proves nothing"
                         % run.get("url"))


def _check_report(run, version):
    """Validate one native Dalfox JSON report and return its findings list."""
    report = run.get("report")
    if not isinstance(report, dict) or not isinstance(report.get("meta"), dict):
        raise ValueError("Native Dalfox report is missing or malformed for %s" % run.get("url"))
    meta, findings = report["meta"], report.get("findings")
    if not isinstance(findings, list) or any(not isinstance(f, dict) for f in findings):
        raise ValueError("Native Dalfox report has no findings list")
    if meta.get("dalfox_version") != version:
        raise ValueError("Report was produced by a different Dalfox version")
    if meta.get("incomplete") is not False:
        raise ValueError("Dalfox marked the scan incomplete for %s" % run.get("url"))
    if meta.get("findings_count") != len(findings):
        raise ValueError("Report finding count does not match its findings")
    summaries = meta.get("target_summary")
    if not isinstance(summaries, list) or len(summaries) != 1 or not isinstance(summaries[0], dict):
        raise ValueError("Expected exactly one scanned target per report")
    summary = summaries[0]
    if summary.get("target") != run.get("seedUrl") or summary.get("method") != run.get("method"):
        raise ValueError("Report describes a different target than was planned")
    status = summary.get("status")
    if status not in COMPLETED_STATUSES:
        raise ValueError("Dalfox did not complete %s: status %s %s"
                         % (run.get("url"), status, summary.get("error_code", "")))
    has_findings = COMPLETED_STATUSES[status]
    if has_findings != bool(findings) or COMPLETED_EXIT_CODES[run["returncode"]] != has_findings:
        raise ValueError("Exit code, target status and findings disagree")
    return findings


def classify(envelope):
    """Split a run manifest into confirmed endpoint findings and observations."""
    if not isinstance(envelope, dict) or envelope.get("schemaVersion") != 1:
        raise ValueError("Expected a Dalfox benchmark run manifest (schemaVersion 1)")
    runs = envelope.get("runs")
    version = envelope.get("dalfoxVersion")
    if (not isinstance(version, str) or not RELEASE_VERSION.fullmatch(version)
            or not isinstance(runs, list) or not runs):
        raise ValueError("Missing runs or unsupported Dalfox version")
    if envelope.get("targetCount") != len(runs):
        raise ValueError("Not all planned targets were scanned")
    confirmed, observations, seen = [], [], set()
    for run in runs:
        if (not isinstance(run, dict) or run.get("completed") is not True
                or run.get("returncode") not in COMPLETED_EXIT_CODES):
            raise ValueError("Scan failed or did not complete; inspect run artifacts")
        url = clean_url(run.get("url"))
        method = str(run.get("method", "")).upper()
        if method != "GET":
            raise ValueError("Unknown request method")
        _check_preflight(run)
        for finding in _check_report(run, version):
            tier = finding.get("type")
            poc = finding.get("data")
            on_target = isinstance(poc, str) and clean_url(poc) == url
            if tier == CONFIRMED_TIER and finding.get("cwe") == XSS_CWE and on_target:
                if (url, method) not in seen:
                    seen.add((url, method))
                    confirmed.append({"url": url, "method": method, "type": VULNERABILITY_TYPE})
                continue
            observations.append({
                "url": url,
                "variant": run.get("variant"),
                "tier": tier,
                "cwe": finding.get("cwe"),
                "detectionMethod": finding.get("detection_method"),
                "parameter": finding.get("param"),
                "reason": "off-target proof of concept" if not on_target
                else "not a confirmed XSS (tier %s, %s)" % (tier, finding.get("cwe")),
            })
    return confirmed, observations


def benchmark_document(confirmed):
    return {"tool": "Dalfox", "scanType": "DAST", "findings": confirmed}


def convert(envelope):
    return benchmark_document(classify(envelope)[0])


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--input", required=True, type=Path)
    parser.add_argument("--output", required=True, type=Path)
    parser.add_argument("--observations", type=Path,
                        help="optional file for reflection-only and other unsubmitted results")
    args = parser.parse_args()
    try:
        confirmed, observations = classify(json.loads(args.input.read_text()))
        args.output.parent.mkdir(parents=True, exist_ok=True)
        args.output.write_text(json.dumps(benchmark_document(confirmed), indent=2) + "\n")
        if args.observations:
            args.observations.parent.mkdir(parents=True, exist_ok=True)
            args.observations.write_text(json.dumps(observations, indent=2) + "\n")
        print("Converted %d confirmed endpoint finding(s); %d unsubmitted observation(s)"
              % (len(confirmed), len(observations)))
    except (ValueError, OSError) as error:
        parser.exit(1, str(error) + "\n")


if __name__ == "__main__":
    main()
