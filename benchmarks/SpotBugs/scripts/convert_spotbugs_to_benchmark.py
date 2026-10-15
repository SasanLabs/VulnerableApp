#!/usr/bin/env python3
"""
convert_spotbugs_to_benchmark.py
================================
Converts a SpotBugs (with FindSecBugs) XML report into the JSON input format
expected by VulnerableApp's POST /scanner/benchmark endpoint (SAST mode).

Usage
-----
    # Run SpotBugs with FindSecBugs via Gradle:
    ./gradlew spotbugsMain

    python benchmarks/SpotBugs/scripts/convert_spotbugs_to_benchmark.py \\
        --input  build/reports/spotbugs/spotbugs.xml \\
        --output benchmarks/SpotBugs/findings/spotbugs-findings.json

    # Then POST to VulnerableApp:
    curl -X POST http://localhost:9090/VulnerableApp/scanner/benchmark \\
        -H "Content-Type: application/json" \\
        -d @benchmarks/SpotBugs/findings/spotbugs-findings.json \\
        -o benchmarks/SpotBugs/spotbugs-results.json

Input format (SpotBugs XML)
---------------------------
SpotBugs writes an XML report with a root <BugCollection>.
Each issue is represented as a <BugInstance>:

    <BugInstance type="SQL_INJECTION_SPRING_JDBC" category="SECURITY" cweid="89">
      <ShortMessage>Potential JDBC Injection (Spring JDBC)</ShortMessage>
      <SourceLine classname="..." primary="true" start="84" end="84"
                  sourcepath="org/sasanlabs/service/vulnerability/sqlInjection/UnionBasedSQLInjectionVulnerability.java"/>
    </BugInstance>

Output format (VulnerableApp benchmark input)
---------------------------------------------
    {
      "tool": "SpotBugs-FindSecBugs",
      "scanType": "SAST",
      "findings": [
        {
          "filePath": "src/main/java/org/sasanlabs/service/vulnerability/sqlInjection/UnionBasedSQLInjectionVulnerability.java",
          "line": 84,
          "cwe": "CWE-89",
          "type": "Potential JDBC Injection (Spring JDBC)"
        },
        ...
      ]
    }
"""

import argparse
import json
import re
import sys
import xml.etree.ElementTree as ET
from pathlib import Path
from typing import Optional, Union

_CWE_NUMERIC = re.compile(r"^\d+$")


def _format_cwe(raw_cwe: Optional[str]) -> Optional[str]:
    """
    Format a CWE ID into the standard 'CWE-XX' format.
    E.g.: '89' -> 'CWE-89', 'cwe-89' -> 'CWE-89', 'CWE-89' -> 'CWE-89'.
    """
    if not raw_cwe or not raw_cwe.strip():
        return None
    raw = raw_cwe.strip().upper()
    if _CWE_NUMERIC.match(raw):
        return f"CWE-{raw}"
    match = re.search(r"CWE-\d+", raw)
    if match:
        return match.group(0)
    return raw


def _find_source_line(bug_elem: ET.Element) -> Optional[ET.Element]:
    """
    Locates the most specific SourceLine element inside a BugInstance.
    Priority:
    1. Direct <SourceLine> child with primary="true"
    2. Direct <SourceLine> child with 'start' and 'sourcepath'
    3. <Method> -> <SourceLine> with primary="true" or with 'start' and 'sourcepath'
    4. <Class> -> <SourceLine> with primary="true" or with 'start' and 'sourcepath'
    """
    # 1. Direct primary SourceLine child
    for sl in bug_elem.findall("./SourceLine[@primary='true']"):
        if sl.get("start") and sl.get("sourcepath"):
            return sl

    # 2. Any direct SourceLine child with start & sourcepath
    for sl in bug_elem.findall("./SourceLine"):
        if sl.get("start") and sl.get("sourcepath"):
            return sl

    # 3. Method SourceLine
    for method in bug_elem.findall("./Method"):
        for sl in method.findall("./SourceLine[@primary='true']"):
            if sl.get("start") and sl.get("sourcepath"):
                return sl
        for sl in method.findall("./SourceLine"):
            if sl.get("start") and sl.get("sourcepath"):
                return sl

    # 4. Class SourceLine
    for cls in bug_elem.findall("./Class"):
        for sl in cls.findall("./SourceLine[@primary='true']"):
            if sl.get("start") and sl.get("sourcepath"):
                return sl
        for sl in cls.findall("./SourceLine"):
            if sl.get("start") and sl.get("sourcepath"):
                return sl

    return None


def _normalize_path(sourcepath: str) -> str:
    """
    Normalises a source path to project-relative format:
    e.g. 'org/sasanlabs/.../Foo.java' -> 'src/main/java/org/sasanlabs/.../Foo.java'.
    """
    path = sourcepath.strip().replace("\\", "/")
    while path.startswith("./"):
        path = path[2:]
    if not path.startswith("src/"):
        path = f"src/main/java/{path}"
    return path


def convert(xml_source: Union[str, bytes, Path, ET.Element]) -> dict:
    """
    Parses SpotBugs XML and converts it into VulnerableApp benchmark input format.
    """
    if isinstance(xml_source, ET.Element):
        root = xml_source
    elif isinstance(xml_source, Path):
        tree = ET.parse(xml_source)
        root = tree.getroot()
    elif isinstance(xml_source, (str, bytes)):
        # Could be file path or raw XML content
        if isinstance(xml_source, str) and (xml_source.strip().startswith("<") or "\n" in xml_source):
            root = ET.fromstring(xml_source)
        else:
            p = Path(xml_source)
            if p.exists() and p.is_file():
                tree = ET.parse(p)
                root = tree.getroot()
            else:
                root = ET.fromstring(xml_source)
    else:
        raise TypeError(f"Unsupported xml_source type: {type(xml_source)}")

    # Index BugPatterns by pattern type
    patterns: dict[str, dict] = {}
    for bp in root.findall(".//BugPattern"):
        b_type = bp.get("type")
        if b_type:
            patterns[b_type] = {
                "cweid": bp.get("cweid"),
                "short_description": (bp.findtext("ShortDescription") or "").strip(),
                "category": bp.get("category"),
            }

    seen: set[tuple] = set()
    findings: list[dict] = []

    for bug in root.findall(".//BugInstance"):
        b_type = (bug.get("type") or "").strip()
        bp_info = patterns.get(b_type, {})

        # Determine CWE
        raw_cwe = bug.get("cweid") or bp_info.get("cweid")
        cwe = _format_cwe(raw_cwe)

        # Determine type
        short_msg = (bug.findtext("ShortMessage") or "").strip()
        short_desc = bp_info.get("short_description", "")
        vuln_type = short_msg or short_desc or b_type or None

        # Locate source line
        source_line = _find_source_line(bug)
        if source_line is None:
            continue

        raw_path = source_line.get("sourcepath") or source_line.get("sourcefile")
        raw_start = source_line.get("start")
        if not raw_path or not raw_start:
            continue

        try:
            line = int(raw_start)
        except ValueError:
            continue

        file_path = _normalize_path(raw_path)

        if not cwe and not vuln_type:
            bug_label = b_type or "bug"
            print(f"WARNING: skipped {bug_label} at {file_path}:{line}: no CWE or "
                  "type, so the SAST matcher cannot score it", file=sys.stderr)
            continue

        key = (file_path, line, cwe, vuln_type)
        if key in seen:
            continue
        seen.add(key)

        finding: dict = {
            "filePath": file_path,
            "line": line
        }
        if cwe:
            finding["cwe"] = cwe
        if vuln_type:
            finding["type"] = vuln_type

        findings.append(finding)

    return {
        "tool": "SpotBugs-FindSecBugs",
        "scanType": "SAST",
        "findings": findings
    }


def main() -> None:
    parser = argparse.ArgumentParser(
        description="Convert SpotBugs XML report to VulnerableApp benchmark input format."
    )
    parser.add_argument(
        "--input", "-i",
        required=True,
        help="Path to the XML report generated by SpotBugs (e.g. build/reports/spotbugs/spotbugs.xml).",
    )
    parser.add_argument(
        "--output", "-o",
        default="benchmarks/SpotBugs/findings/spotbugs-findings.json",
        help="Path to write the benchmark input JSON (default: benchmarks/SpotBugs/findings/spotbugs-findings.json).",
    )
    args = parser.parse_args()

    input_path = Path(args.input)
    output_path = Path(args.output)

    if not input_path.exists():
        print(f"ERROR: input file not found: {input_path}", file=sys.stderr)
        sys.exit(1)

    benchmark_input = convert(input_path)

    output_path.parent.mkdir(parents=True, exist_ok=True)
    with output_path.open("w", encoding="utf-8") as fh:
        json.dump(benchmark_input, fh, indent=2)

    total = len(benchmark_input["findings"])
    print(f"Converted {total} finding(s) -> {output_path}")


if __name__ == "__main__":
    main()
