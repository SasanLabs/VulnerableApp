#!/usr/bin/env python3
"""Scan the Java reflected-XSS ground-truth endpoints on a controlled deployed app.

Targets come from the live ``/scanner/dast`` response. Each endpoint is seeded
with the value its challenge page sends, checked to reflect that seed as HTML,
and then scanned by its own Dalfox process so every finding in a report belongs
to exactly one ground-truth URL. The combined manifest (``runs.json``) holds
each target's native Dalfox JSON report and is what the converter grades.
"""
import argparse
import json
from pathlib import Path
import subprocess
from urllib.error import HTTPError
from urllib.parse import quote, urlsplit
from urllib.request import Request, urlopen

from convert_dalfox_to_benchmark import clean_url, convert

# Challenge family -> (query parameter, seed path or value). The seeds are the
# values the legacy challenge pages send: XSSInImgTagAttribute's secure levels
# only accept the allow-listed /VulnerableApp image path whatever context the
# app is deployed under, and the HTML-tag levels echo any value.
FAMILIES = {
    "XSSInImgTagAttribute": ("src", "/VulnerableApp/images/OWASP.png"),
    "XSSWithHtmlTagInjection": ("value", "vulnerableapp"),
}
# The challenge pages insert these responses into the DOM with innerHTML, so
# the endpoints are requested as HTML. Without it Spring answers text/plain,
# which Dalfox (like a browser) correctly treats as inert.
ACCEPT_HTML = "text/html"


def discover_targets(rows, base_url):
    if not isinstance(rows, list):
        raise ValueError("Expected the Java standalone /scanner/dast array")
    base = urlsplit(clean_url(base_url))
    context = base.path.rstrip("/")
    prefix = context + "/"
    selected = {}
    for row in rows:
        if not isinstance(row, dict) or not isinstance(row.get("url"), str):
            raise ValueError("Invalid ground-truth row")
        parsed = urlsplit(row["url"])
        route = parsed.path[len(prefix):].split("/") if parsed.path.startswith(prefix) else []
        if len(route) != 2 or route[0] not in FAMILIES:
            continue
        if (parsed.scheme, parsed.netloc) != (base.scheme, base.netloc):
            raise ValueError("XSS ground truth points outside the configured app origin")
        if row.get("method") != "GET" or parsed.query or parsed.fragment:
            raise ValueError("Runner supports GET endpoints with a single query parameter only")
        if row.get("protocol", "HTTP") != "HTTP":
            raise ValueError("Runner supports HTTP ground truth only")
        if row.get("variant") not in ("SECURE", "UNSECURE"):
            raise ValueError("Unknown ground-truth variant")
        parameter, seed = FAMILIES[route[0]]
        target = {
            "url": clean_url(row["url"]),
            "method": "GET",
            "variant": row["variant"],
            "vulnerabilityTypes": row.get("vulnerabilityTypes", []),
            "parameter": parameter,
            "seed": seed,
        }
        if target["url"] in selected and selected[target["url"]] != target:
            raise ValueError("Conflicting ground-truth entries for one URL")
        selected[target["url"]] = target
    missing = set(FAMILIES) - {urlsplit(url).path.split("/")[-2] for url in selected}
    if missing:
        raise ValueError("No ground truth for supported families: " + ", ".join(sorted(missing)))
    return list(selected.values())


def seed_url(target):
    return target["url"] + "?" + target["parameter"] + "=" + quote(target["seed"], safe="")


def preflight(target, timeout=15):
    """Confirm the seeded request is answered as HTML and reflects its seed.

    Dalfox reports a target whose baseline request is rejected (e.g. HTTP 400)
    as clean, so the runner proves the seed reaches the sink before scanning.
    """
    request = Request(seed_url(target), headers={"Accept": ACCEPT_HTML})
    try:
        with urlopen(request, timeout=timeout) as response:
            status = response.status
            content_type = response.headers.get_content_type()
            body = response.read().decode("utf-8", errors="replace")
    except HTTPError as error:
        status, content_type, body = error.code, error.headers.get_content_type(), ""
    return {
        "status": status,
        "contentType": content_type,
        "seedReflected": target["seed"] in body,
    }


def dalfox_version(dalfox):
    output = subprocess.check_output([str(dalfox), "--version"], text=True, timeout=30)
    return output.strip()


def build_command(dalfox, target, report_path, scan_timeout, workers):
    return [
        str(dalfox), "scan", seed_url(target),
        "--param", target["parameter"] + ":query",
        "--headers", "Accept: " + ACCEPT_HTML,
        # Only the documented query parameter is in scope; header, cookie and
        # path reflections or mined parameters cannot be attributed to the row.
        "--skip-mining",
        "--skip-reflection-header", "--skip-reflection-cookie", "--skip-reflection-path",
        "--skip-waf-probe",
        "--format", "json", "--output", str(report_path),
        "--no-color", "--silence",
        "--workers", str(workers), "--max-concurrent-targets", "1",
        "--timeout", "10", "--scan-timeout", str(scan_timeout),
    ]


def run(base_url, dalfox, expected_version, output_dir, target_timeout, scan_timeout, workers):
    output_dir.mkdir(parents=True, exist_ok=False)
    version = dalfox_version(dalfox)
    if version != "dalfox " + expected_version:
        raise ValueError("Expected dalfox %s, got %s" % (expected_version, version))
    (output_dir / "dalfox-version.txt").write_text(version + "\n")
    endpoint = base_url.rstrip("/") + "/scanner/dast"
    with urlopen(endpoint, timeout=15) as response:
        rows = json.load(response)
    (output_dir / "ground-truth.json").write_text(json.dumps(rows, indent=2) + "\n")
    targets = discover_targets(rows, base_url)
    (output_dir / "targets.json").write_text(json.dumps(targets, indent=2) + "\n")
    manifest = {
        "schemaVersion": 1,
        "dalfoxVersion": expected_version,
        "targetCount": len(targets),
        "runs": [],
    }
    manifest_path = output_dir / "runs.json"
    manifest_path.write_text(json.dumps(manifest, indent=2) + "\n")
    for index, target in enumerate(targets):
        directory = output_dir / ("target-%02d" % index)
        directory.mkdir()
        report_path = directory / "dalfox-report.json"
        command = build_command(dalfox, target, report_path.resolve(), scan_timeout, workers)
        (directory / "invocation.json").write_text(json.dumps(command, indent=2) + "\n")
        record = dict(target, seedUrl=seed_url(target), preflight=preflight(target),
                      completed=False, returncode=None, report=None)
        with (directory / "scan.log").open("w") as log:
            try:
                result = subprocess.run(command, stdin=subprocess.DEVNULL, stdout=log,
                                        stderr=subprocess.STDOUT, timeout=target_timeout)
                record.update(completed=True, returncode=result.returncode)
            except subprocess.TimeoutExpired:
                log.write("Per-target process timeout exceeded\n")
        if report_path.is_file():
            try:
                record["report"] = json.loads(report_path.read_text())
            except ValueError:
                record["report"] = None
        manifest["runs"].append(record)
        manifest_path.write_text(json.dumps(manifest, indent=2) + "\n")
        print("Scanned %d/%d: %s" % (index + 1, len(targets), target["url"]), flush=True)
    # A failed/incomplete scan keeps its artifacts but must not be submitted as a clean result.
    convert(manifest)
    return manifest


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--base-url", required=True)
    parser.add_argument("--dalfox", required=True, type=Path, help="path to the dalfox binary")
    parser.add_argument("--dalfox-version", required=True,
                        help="release the binary must report, e.g. 3.3.0")
    parser.add_argument("--output-dir", required=True, type=Path)
    parser.add_argument("--target-timeout", type=int, default=240,
                        help="process deadline per target in seconds")
    parser.add_argument("--scan-timeout", type=int, default=180,
                        help="Dalfox --scan-timeout per target in seconds")
    parser.add_argument("--workers", type=int, default=4)
    args = parser.parse_args()
    if not 1 <= args.target_timeout <= 900:
        parser.error("--target-timeout must be between 1 and 900 seconds")
    if not 1 <= args.scan_timeout < args.target_timeout:
        parser.error("--scan-timeout must be at least 1 and below --target-timeout")
    if not 1 <= args.workers <= 16:
        parser.error("--workers must be between 1 and 16")
    try:
        run(args.base_url, args.dalfox, args.dalfox_version, args.output_dir,
            args.target_timeout, args.scan_timeout, args.workers)
    except (ValueError, OSError, subprocess.SubprocessError) as error:
        parser.exit(1, str(error) + "\n")


if __name__ == "__main__":
    main()
