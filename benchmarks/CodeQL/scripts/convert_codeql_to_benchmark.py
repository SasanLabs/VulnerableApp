#!/usr/bin/env python3
"""
convert_codeql_to_benchmark.py
==============================
Converts a CodeQL SARIF report into the JSON input format expected by
VulnerableApp's POST /scanner/benchmark endpoint (SAST mode).

Usages
-----
    # From the repository root, so reported paths are project-relative:
    codeql database analyze <database> \\
        codeql/java-queries:codeql-suites/java-security-extended.qls \\
        --format=sarif-latest \\
        --output=benchmarks/CodeQL/scripts/codeql-results.json

    python3 benchmarks/CodeQL/scripts/convert_codeql_to_benchmark.py \\
        --input  benchmarks/CodeQL/scripts/codeql-results.json \\
        --output benchmarks/CodeQL/findings/codeql-findings.json

    # Then POST to VulnerableApp:
    curl -X POST http://localhost:9090/VulnerableApp/scanner/benchmark \\
        -H "Content-Type: application/json" \\
        -d @benchmarks/CodeQL/findings/codeql-findings.json

Input format (CodeQL SARIF 2.1.0)
---------------------------------
CodeQL writes a SARIF document with a "runs" array. Each run lists its rules
under tool.driver.rules (and sometimes under tool.extensions[].rules) and its
findings under "results". Each result looks like:

    {
      "ruleId": "java/sql-injection",
      "ruleIndex": 0,
      "locations": [{
        "physicalLocation": {
          "artifactLocation": { "uri": "src/main/java/.../ErrorBasedSQLInjectionVulnerability.java" },
          "region": { "startLine": 143 }
        }
      }]
    }

and the matching rule carries the CWE metadata:

    {
      "id": "java/sql-injection",
      "helpUri": "https://codeql.github.com/codeql-query-help/java/java-sql-injection/",
      "properties": { "tags": ["security", "external/cwe/cwe-089"] }
    }

Output format (VulnerableApp benchmark input)
---------------------------------------------
    {
      "tool": "CodeQL",
      "scanType": "SAST",
      "findings": [
        {
          "filePath": "src/main/java/.../ErrorBasedSQLInjectionVulnerability.java",
          "line": 143,
          "cwe": "CWE-89"
        },
        ...
      ]
    }

Notes
-----
- line is the startLine of the primary (first) location of the result.

- cwe comes from the rule metadata: the "external/cwe/cwe-NNN" tags first,
  then the cwe.mitre.org helpUri as a fallback. Leading zeroes are removed
  (cwe-089 -> CWE-89). A rule that lists several CWEs gives one finding per
  CWE, in order and without repeats.

- The script does not invent a vulnerability "type": CodeQL does not report
  one, and CWE matching is the main axis (like the ZAP converter).

- The result is looked up in the rules by ruleId first, then by ruleIndex.

- A result without a primary location, without a valid project-relative path
  or line, or whose rule has no CWE is skipped with a warning on stderr,
  because the SAST matcher cannot score it.

- Paths are made project-relative: "./", "%SRCROOT%/", percent-encoding,
  backslashes and absolute file paths under --source-root are handled. Paths
  that point outside --source-root are skipped.

- Duplicate (filePath, line, cwe) tuples are de-duplicated.

- A report whose analysis did not finish (executionSuccessful is false) is
  refused, so an incomplete scan never produces a misleading benchmark.
"""

import argparse
import json
import posixpath
import re
import sys
from pathlib import Path
from typing import Optional
from urllib.parse import unquote, urlparse

_CWE_TAG = re.compile(r"(?<![a-z0-9])cwe-(\d+)", re.IGNORECASE)
_CWE_HELP_URI = re.compile(r"cwe\.mitre\.org/data/definitions/(\d+)", re.IGNORECASE)
_DRIVE_PATH = re.compile(r"^[A-Za-z]:/")
_FILE_URI_DRIVE = re.compile(r"^/[A-Za-z]:/")
_SRCROOT_PREFIX = "%SRCROOT%/"


def _as_list(value) -> list:
    """Return value if it is a list, otherwise an empty list."""
    return value if isinstance(value, list) else []


def _as_dict(value) -> dict:
    """Return value if it is a dict, otherwise an empty dict."""
    return value if isinstance(value, dict) else {}


def _add_cwe(tags: list[str], number: str) -> None:
    """Append 'CWE-<number>' (leading zeroes removed) unless it is 0 or already present."""
    value = int(number)
    if value > 0 and f"CWE-{value}" not in tags:
        tags.append(f"CWE-{value}")


def _cwe_tags(rule) -> list[str]:
    """
    Extract the CWE IDs from a SARIF rule, in order and without repeats
    (e.g. tags ['external/cwe/cwe-089', 'external/cwe/cwe-564']
    -> ['CWE-89', 'CWE-564']). The tags can be a list or a single string.
    If the tags give no CWE, the cwe.mitre.org helpUri is used instead.
    """
    if not isinstance(rule, dict):
        return []

    raw_tags = _as_dict(rule.get("properties")).get("tags")
    if isinstance(raw_tags, str):
        raw_tags = [raw_tags]

    tags: list[str] = []
    for tag in _as_list(raw_tags):
        if isinstance(tag, str):
            for match in _CWE_TAG.finditer(tag):
                _add_cwe(tags, match.group(1))

    if not tags:
        help_uri = rule.get("helpUri")
        if isinstance(help_uri, str):
            for match in _CWE_HELP_URI.finditer(help_uri):
                _add_cwe(tags, match.group(1))
    return tags


def _relative_to_root(path: str, source_root) -> Optional[str]:
    """Make an absolute path relative to source_root, or None if it is outside it."""
    if source_root is None:
        return None
    try:
        relative = Path(path).resolve().relative_to(Path(source_root).resolve())
    except (ValueError, OSError):
        return None
    result = relative.as_posix()
    return None if result == "." else result


def _normalise_path(uri, source_root=None) -> Optional[str]:
    """
    Turn a SARIF artifact URI into a project-relative path with forward slashes,
    or None if the URI is missing or points outside the project.
    """
    if not isinstance(uri, str) or not uri.strip():
        return None

    text = uri.strip()
    if text.lower().startswith("file:"):
        text = unquote(urlparse(text).path)
        if _FILE_URI_DRIVE.match(text):  # file:///C:/x -> C:/x
            text = text[1:]
    else:
        text = unquote(text)

    text = text.replace("\\", "/")
    if text.startswith(_SRCROOT_PREFIX):
        text = text[len(_SRCROOT_PREFIX):]
    while text.startswith("./"):
        text = text[2:]

    if text.startswith("/") or _DRIVE_PATH.match(text):
        return _relative_to_root(text, source_root)

    normalised = posixpath.normpath(text)
    if normalised in ("", ".", "..") or normalised.startswith("../"):
        return None
    return normalised


def _collect_rules(run: dict) -> tuple[list, dict]:
    """Return (driver rules in SARIF order, all rules keyed by id)."""
    tool = _as_dict(run.get("tool"))
    driver_rules = _as_list(_as_dict(tool.get("driver")).get("rules"))

    all_rules = list(driver_rules)
    for extension in _as_list(tool.get("extensions")):
        all_rules.extend(_as_list(_as_dict(extension).get("rules")))

    rules_by_id: dict = {}
    for rule in all_rules:
        if isinstance(rule, dict) and isinstance(rule.get("id"), str):
            rules_by_id.setdefault(rule["id"], rule)
    return driver_rules, rules_by_id


def _find_rule(result: dict, driver_rules: list, rules_by_id: dict):
    """Find the rule of a result by ruleId, then by ruleIndex."""
    rule_id = result.get("ruleId")
    rule = rules_by_id.get(rule_id) if isinstance(rule_id, str) else None
    if rule is None:
        index = result.get("ruleIndex")
        if isinstance(index, int) and not isinstance(index, bool) \
                and 0 <= index < len(driver_rules):
            rule = driver_rules[index]
    return rule if isinstance(rule, dict) else None


def _primary_location(result: dict):
    """Return (uri, startLine) of the first location, or None if there is none."""
    locations = _as_list(result.get("locations"))
    if not locations or not isinstance(locations[0], dict):
        return None
    physical = _as_dict(locations[0].get("physicalLocation"))
    uri = _as_dict(physical.get("artifactLocation")).get("uri")
    line = _as_dict(physical.get("region")).get("startLine")
    return uri, line


def _valid_line(line) -> bool:
    """A usable line is a positive integer (booleans do not count)."""
    return isinstance(line, int) and not isinstance(line, bool) and line > 0


def _warn(message: str) -> None:
    print(f"WARNING: {message}", file=sys.stderr)


def convert(sarif_report: dict, source_root=None) -> dict:
    """
    Convert a parsed CodeQL SARIF report dict into a benchmark input dict.

    source_root is only needed when the report holds absolute paths; they are
    made relative to it. Raises ValueError for a report that is not a JSON
    object, has a malformed 'runs', or contains an unsuccessful analysis.
    """
    if not isinstance(sarif_report, dict):
        raise ValueError("SARIF report must be a JSON object")

    runs = sarif_report.get("runs", [])
    if not isinstance(runs, list):
        raise ValueError("SARIF 'runs' must be an array")

    seen: set[tuple] = set()
    findings: list[dict] = []

    for run in runs:
        if not isinstance(run, dict):
            continue

        for invocation in _as_list(run.get("invocations")):
            if isinstance(invocation, dict) and invocation.get("executionSuccessful") is False:
                raise ValueError(
                    "SARIF report contains an unsuccessful analysis invocation; "
                    "fix the CodeQL run and analyze again"
                )

        driver_rules, rules_by_id = _collect_rules(run)

        for result in _as_list(run.get("results")):
            if not isinstance(result, dict):
                continue

            rule_name = result.get("ruleId")
            if not isinstance(rule_name, str):
                rule_name = "unknown rule"

            location = _primary_location(result)
            if location is None:
                _warn(f"skipped {rule_name} result without a primary location")
                continue

            uri, line = location
            path = _normalise_path(uri, source_root)
            if path is None or not _valid_line(line):
                _warn(f"skipped {rule_name} result: missing/invalid project path or line")
                continue

            cwes = _cwe_tags(_find_rule(result, driver_rules, rules_by_id))
            if not cwes:
                _warn(f"skipped {rule_name} at {path}:{line}: rule metadata has no CWE, "
                      "so the SAST matcher cannot score it")
                continue

            for cwe in cwes:
                key = (path, line, cwe)
                if key in seen:
                    continue
                seen.add(key)
                findings.append({"filePath": path, "line": line, "cwe": cwe})

    return {
        "tool": "CodeQL",
        "scanType": "SAST",
        "findings": findings,
    }


def main() -> None:
    parser = argparse.ArgumentParser(
        description="Convert a CodeQL SARIF report to VulnerableApp benchmark input format."
    )
    parser.add_argument(
        "--input", "-i",
        required=True,
        help="Path to the SARIF report written by `codeql database analyze --format=sarif-latest`.",
    )
    parser.add_argument(
        "--output", "-o",
        default="benchmarks/CodeQL/findings/codeql-findings.json",
        help="Path to write the benchmark input JSON (default: benchmarks/CodeQL/findings/codeql-findings.json).",
    )
    parser.add_argument(
        "--source-root",
        default=".",
        help="Project root used to make absolute paths relative (default: current directory).",
    )
    args = parser.parse_args()

    input_path = Path(args.input)
    output_path = Path(args.output)

    if not input_path.exists():
        print(f"ERROR: input file not found: {input_path}", file=sys.stderr)
        sys.exit(1)

    try:
        with input_path.open(encoding="utf-8") as fh:
            sarif_report = json.load(fh)
        benchmark_input = convert(sarif_report, Path(args.source_root))
    except (json.JSONDecodeError, ValueError) as error:
        print(f"ERROR: {error}", file=sys.stderr)
        sys.exit(1)

    output_path.parent.mkdir(parents=True, exist_ok=True)
    with output_path.open("w", encoding="utf-8") as fh:
        json.dump(benchmark_input, fh, indent=2)

    total = len(benchmark_input["findings"])
    print(f"Converted {total} finding(s) -> {output_path}")


if __name__ == "__main__":
    main()