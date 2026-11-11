#!/usr/bin/env python3
"""Scan the Java CommandInjection ground-truth endpoints on a controlled deployed app.

Each endpoint is scanned with the classic results-based technique and a unique
``--os-cmd`` marker. commix's per-target ``logs.txt`` is parsed into a structured
report, and the console output's ERROR/CRITICAL lines are kept as diagnostics.
The combined manifest (``runs.json``) is what the converter grades.
"""
import argparse
import json
from pathlib import Path
import re
import secrets
import subprocess
import sys
from urllib.parse import urlsplit
from urllib.request import urlopen

from commix_log import parse_log
from convert_commix_to_benchmark import clean_url, convert

FAMILY = "CommandInjection"
PARAMETER = "ipaddress"


def diagnostics(log):
    """ERROR/CRITICAL console lines, ANSI-stripped, that justify a run's verdict."""
    plain = re.sub(r"\x1b\[[0-9;]*m", "", log)
    messages = re.findall(r"\[(?:error|critical)\]\s*([^\r\n]+)", plain, re.IGNORECASE)
    if "Traceback (most recent call last)" in plain:
        messages.append("Scanner traceback; inspect scan.log")
    return messages


def discover_targets(rows, base_url):
    if not isinstance(rows, list):
        raise ValueError("Expected the Java standalone /scanner/dast array")
    base = urlsplit(clean_url(base_url))
    prefix = base.path.rstrip("/") + "/"
    selected = {}
    for row in rows:
        if not isinstance(row, dict) or not isinstance(row.get("url"), str):
            raise ValueError("Invalid ground-truth row")
        parsed = urlsplit(row["url"])
        route = parsed.path[len(prefix):].split("/") if parsed.path.startswith(prefix) else []
        if len(route) != 2 or route[0] != FAMILY:
            continue
        if (parsed.scheme, parsed.netloc) != (base.scheme, base.netloc):
            raise ValueError("Command-injection ground truth points outside the configured app origin")
        if row.get("method") != "GET" or parsed.query or parsed.fragment:
            raise ValueError("Runner supports GET endpoints with the ipaddress query parameter only")
        if row.get("variant") not in ("SECURE", "UNSECURE"):
            raise ValueError("Unknown ground-truth variant")
        target = {
            "url": clean_url(row["url"]),
            "method": "GET",
            "variant": row["variant"],
            "vulnerabilityTypes": row.get("vulnerabilityTypes", []),
        }
        if target["url"] in selected and selected[target["url"]] != target:
            raise ValueError("Conflicting ground-truth entries for one URL")
        selected[target["url"]] = target
    if not selected:
        raise ValueError("No supported command-injection ground truth; refusing an empty scan")
    return list(selected.values())


def run(base_url, commix_dir, commix_tag, output_dir, target_timeout):
    output_dir.mkdir(parents=True, exist_ok=False)
    commix = commix_dir.resolve() / "commix.py"
    version = subprocess.check_output([sys.executable, str(commix), "--version"], text=True,
                                      timeout=30)
    version = re.sub(r"\x1b\[[0-9;]*m", "", version).strip()
    if version != commix_tag:
        raise ValueError("Expected commix %s (a stable release), got %s" % (commix_tag, version))
    (output_dir / "commix-version.txt").write_text(version + "\n")
    endpoint = base_url.rstrip("/") + "/scanner/dast"
    with urlopen(endpoint, timeout=15) as response:
        rows = json.load(response)
    (output_dir / "ground-truth.json").write_text(json.dumps(rows, indent=2) + "\n")
    targets = discover_targets(rows, base_url)
    (output_dir / "targets.json").write_text(json.dumps(targets, indent=2) + "\n")
    manifest = {
        "schemaVersion": 1,
        "commixVersion": "commix/" + version,
        "targetCount": len(targets),
        "runs": [],
    }
    manifest_path = output_dir / "runs.json"
    manifest_path.write_text(json.dumps(manifest, indent=2) + "\n")
    for index, target in enumerate(targets):
        directory = output_dir / ("target-%02d" % index)
        directory.mkdir()
        marker = "commixrce" + secrets.token_hex(8)
        command = [
            sys.executable, str(commix),
            "--url", target["url"] + "?" + PARAMETER + "=127.0.0.1",
            "-p", PARAMETER,
            "--os-cmd=echo " + marker,
            "--technique=c",
            "--batch", "--ignore-stdin", "--disable-coloring",
            "--timeout=10", "--retries=0",
            # Non-interactive answers: never spawn a shell, continue, or keep testing.
            "--answers=shell=n,recognize=u,continue=N,skip=Y",
            "--output-dir=" + str((directory / "session").resolve()),
        ]
        (directory / "invocation.json").write_text(json.dumps(command, indent=2) + "\n")
        record = dict(target, marker=marker, completed=False, returncode=None,
                      report=None, diagnostics=[])
        with (directory / "scan.log").open("w") as log:
            try:
                result = subprocess.run(command, stdin=subprocess.DEVNULL, stdout=log,
                                        stderr=subprocess.STDOUT, timeout=target_timeout)
                record.update(completed=True, returncode=result.returncode)
            except subprocess.TimeoutExpired:
                record["diagnostics"].append("Per-target scan timeout exceeded")
        record["diagnostics"] += diagnostics((directory / "scan.log").read_text(errors="replace"))
        log_files = list((directory / "session").rglob("logs.txt"))
        if len(log_files) == 1:
            record["report"] = parse_log(log_files[0].read_text(errors="replace"))
        elif len(log_files) > 1:
            record["diagnostics"].append("Multiple commix logs; ambiguous target output")
        manifest["runs"].append(record)
        manifest_path.write_text(json.dumps(manifest, indent=2) + "\n")
        print("Scanned %d/%d: %s" % (index + 1, len(targets), target["url"]), flush=True)
    # A failed/incomplete scan retains artifacts but must not be submitted as a clean result.
    convert(manifest)
    return manifest


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--base-url", required=True)
    parser.add_argument("--commix-dir", required=True, type=Path)
    parser.add_argument("--commix-tag", required=True, help="release tag checked out in --commix-dir")
    parser.add_argument("--output-dir", required=True, type=Path)
    parser.add_argument("--target-timeout", type=int, default=150)
    args = parser.parse_args()
    if not 1 <= args.target_timeout <= 600:
        parser.error("--target-timeout must be between 1 and 600 seconds")
    try:
        run(args.base_url, args.commix_dir, args.commix_tag, args.output_dir, args.target_timeout)
    except (ValueError, OSError, subprocess.SubprocessError) as error:
        parser.exit(1, str(error) + "\n")


if __name__ == "__main__":
    main()
