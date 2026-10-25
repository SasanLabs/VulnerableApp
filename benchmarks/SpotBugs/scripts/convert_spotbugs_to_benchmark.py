#!/usr/bin/env python3
"""
convert_spotbugs_to_benchmark.py
================================
Converts a SpotBugs (with FindSecBugs) XML report into the JSON input format
expected by VulnerableApp's POST /scanner/benchmark endpoint (SAST mode).

Usage
-----
    # Run SpotBugs with FindSecBugs via Gradle init script:
    python benchmarks/SpotBugs/scripts/run_spotbugs.py

    # Or convert an existing SpotBugs XML report:
    python benchmarks/SpotBugs/scripts/convert_spotbugs_to_benchmark.py \\
        --input  benchmarks/SpotBugs/spotbugs-raw-report.xml \\
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

# Default CWE mappings for FindSecBugs and SpotBugs security patterns
# used as a fallback when the XML report does not embed <BugPattern> elements.
DEFAULT_PATTERN_CWES: dict[str, str] = {
    "ANDROID_BROADCAST": "276",
    "ANDROID_EXTERNAL_FILE_ACCESS": "276",
    "ANDROID_GEOLOCATION": "359",
    "ANDROID_WEB_VIEW_JAVASCRIPT": "79",
    "ANDROID_WEB_VIEW_JAVASCRIPT_INTERFACE": "285",
    "ANDROID_WORLD_WRITABLE": "276",
    "BAD_HEXA_CONVERSION": "704",
    "BLOWFISH_KEY_SIZE": "326",
    "CIPHER_INTEGRITY": "353",
    "COMMAND_INJECTION": "78",
    "COOKIE_PERSISTENT": "539",
    "CRLF_INJECTION_LOGS": "117",
    "CUSTOM_INJECTION": "74",
    "CUSTOM_MESSAGE_DIGEST": "327",
    "DANGEROUS_PERMISSION_COMBINATION": "732",
    "DESERIALIZATION_GADGET": "502",
    "DES_USAGE": "327",
    "ECB_MODE": "327",
    "EL_INJECTION": "94",
    "ENTITY_LEAK": "212",
    "ENTITY_MASS_ASSIGNMENT": "915",
    "EXTERNAL_CONFIG_CONTROL": "15",
    "GROOVY_SHELL": "94",
    "HARD_CODE_KEY": "321",
    "HARD_CODE_PASSWORD": "259",
    "HAZELCAST_SYMMETRIC_ENCRYPTION": "327",
    "HTTPONLY_COOKIE": "1004",
    "HTTP_RESPONSE_SPLITTING": "113",
    "IMPROPER_UNICODE": "176",
    "INFORMATION_EXPOSURE_THROUGH_AN_ERROR_MESSAGE": "209",
    "INSECURE_COOKIE": "614",
    "JACKSON_UNSAFE_DESERIALIZATION": "502",
    "JSP_INCLUDE": "98",
    "JSP_JSTL_OUT": "79",
    "JSP_SPRING_EVAL": "917",
    "JSP_XSLT": "94",
    "LDAP_ENTRY_POISONING": "90",
    "LDAP_INJECTION": "90",
    "MALICIOUS_XSLT": "94",
    "MODIFICATION_AFTER_VALIDATION": "176",
    "NORMALIZATION_AFTER_VALIDATION": "176",
    "NULL_CIPHER": "327",
    "OBJECT_DESERIALIZATION": "502",
    "OGNL_INJECTION": "94",
    "PADDING_ORACLE": "326",
    "PATH_TRAVERSAL_IN": "22",
    "PATH_TRAVERSAL_OUT": "22",
    "PLAY_UNVALIDATED_REDIRECT": "601",
    "PREDICTABLE_RANDOM": "330",
    "PREDICTABLE_RANDOM_SCALA": "330",
    "REDOS": "400",
    "RSA_KEY_SIZE": "326",
    "RSA_NO_PADDING": "780",
    "SCALA_COMMAND_INJECTION": "78",
    "SCALA_PATH_TRAVERSAL_IN": "22",
    "SCALA_PLAY_SSRF": "918",
    "SCALA_SENSITIVE_DATA_EXPOSURE": "200",
    "SCALA_SQL_INJECTION_ANORM": "89",
    "SCALA_SQL_INJECTION_SLICK": "89",
    "SCALA_XSS_MVC_API": "79",
    "SCALA_XSS_TWIRL": "79",
    "SCRIPT_ENGINE_INJECTION": "94",
    "SEAM_LOG_INJECTION": "94",
    "SPEL_INJECTION": "94",
    "SPRING_CSRF_PROTECTION_DISABLED": "352",
    "SPRING_CSRF_UNRESTRICTED_REQUEST_MAPPING": "352",
    "SPRING_UNVALIDATED_REDIRECT": "601",
    "SQL_INJECTION": "564",
    "SQL_INJECTION_ANDROID": "89",
    "SQL_INJECTION_HIBERNATE": "564",
    "SQL_INJECTION_JDBC": "89",
    "SQL_INJECTION_JDO": "89",
    "SQL_INJECTION_JPA": "89",
    "SQL_INJECTION_SPRING_JDBC": "89",
    "SQL_INJECTION_TURBINE": "564",
    "SQL_INJECTION_VERTX": "564",
    "SQL_NONCONSTANT_STRING_PASSED_TO_EXECUTE": "89",
    "SQL_PREPARED_STATEMENT_GENERATED_FROM_NONCONSTANT_STRING": "89",
    "STATIC_IV": "329",
    "STRUTS_FORM_VALIDATION": "106",
    "TDES_USAGE": "326",
    "TEMPLATE_INJECTION_FREEMARKER": "94",
    "TEMPLATE_INJECTION_PEBBLE": "94",
    "TEMPLATE_INJECTION_VELOCITY": "94",
    "TRUST_BOUNDARY_VIOLATION": "501",
    "UNENCRYPTED_SERVER_SOCKET": "319",
    "UNENCRYPTED_SOCKET": "319",
    "UNSAFE_HASH_EQUALS": "203",
    "UNVALIDATED_REDIRECT": "601",
    "URLCONNECTION_SSRF_FD": "918",
    "WEAK_HOSTNAME_VERIFIER": "295",
    "WEAK_MESSAGE_DIGEST_MD5": "328",
    "WEAK_MESSAGE_DIGEST_SHA1": "328",
    "WEAK_TRUST_MANAGER": "295",
    "WICKET_XSS1": "79",
    "XML_DECODER": "502",
    "XPATH_INJECTION": "643",
    "XSS_JSP_PRINT": "79",
    "XSS_REQUEST_WRAPPER": "79",
    "XSS_SERVLET": "79",
    "XXE_DOCUMENT": "611",
    "XXE_DTD_TRANSFORM_FACTORY": "611",
    "XXE_SAXPARSER": "611",
    "XXE_SCHEMA_FACTORY": "611",
    "XXE_VALIDATOR": "611",
    "XXE_XMLREADER": "611",
    "XXE_XMLSTREAMREADER": "611",
    "XXE_XPATH": "611",
    "XXE_XSLT_TRANSFORM_FACTORY": "611",
}


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


SQL_SINKS = (
    "query(",
    "queryForObject(",
    "queryForList(",
    "queryForRowSet(",
    "prepareStatement(",
    "execute(",
    "executeUpdate(",
    "batchUpdate(",
)


def _resolve_line(
    file_path: str,
    line: int,
    bug_type: str,
    cwe: Optional[str] = None,
    base_dir: Optional[Path] = None,
) -> int:
    """
    Aligns bytecode method invocation lines with benchmark ground-truth source lines.

    In compiled Java bytecode, method invocations that span multiple source lines
    (such as `applicationJdbcTemplate.query(\\n \"select * ...\")`) attribute the
    INVOKEVIRTUAL instruction to the line where the method call begins, whereas
    VulnerableApp's SAST ground-truth annotations (in expectedIssues.csv) mark the line
    where the SQL query string or argument expression is passed.

    For SQL injection warnings, if the reported line opens a sink method invocation
    without completing its argument list on that line, and the subsequent line contains
    the query argument, we resolve the finding to the argument line.
    """
    if not (
        bug_type.startswith("SQL_INJECTION")
        or bug_type.startswith("SQL_PREPARED_STATEMENT")
        or cwe == "CWE-89"
    ):
        return line

    candidates = []
    if base_dir:
        candidates.append(base_dir / file_path)
    candidates.append(Path(file_path))
    candidates.append(Path.cwd() / file_path)

    # Walk up to find repo root containing file_path
    curr = Path.cwd().resolve()
    for parent in [curr] + list(curr.parents):
        p = parent / file_path
        if p.is_file():
            candidates.append(p)
            break

    target_path = None
    for cand in candidates:
        if cand.is_file():
            target_path = cand
            break

    if target_path is None:
        return line

    try:
        source_lines = target_path.read_text(encoding="utf-8", errors="replace").splitlines()
    except Exception:
        return line

    if not (1 <= line <= len(source_lines)):
        return line

    curr_line = source_lines[line - 1].strip()
    if any(sink in curr_line for sink in SQL_SINKS) and not curr_line.endswith(";"):
        if line < len(source_lines):
            next_line = source_lines[line].strip()
            # If next line has the query argument (string literal, concat, or variable)
            if any(
                token in next_line.lower()
                for token in ('"', "'", "select", "insert", "update", "delete", "sql", "id", "from")
            ):
                return line + 1

    return line


def convert(
    xml_source: Union[str, bytes, Path, ET.Element],
    base_dir: Optional[Path] = None,
) -> dict:
    """
    Parses SpotBugs XML and converts it into VulnerableApp benchmark input format.
    """
    if isinstance(xml_source, ET.Element):
        root = xml_source
    elif isinstance(xml_source, Path):
        tree = ET.parse(xml_source)
        root = tree.getroot()
    elif isinstance(xml_source, bytes):
        root = ET.fromstring(xml_source)
    elif isinstance(xml_source, str):
        if xml_source.strip().startswith("<") or "\n" in xml_source:
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
        raw_cwe = bug.get("cweid") or bp_info.get("cweid") or DEFAULT_PATTERN_CWES.get(b_type)
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
            raw_line = int(raw_start)
        except ValueError:
            continue

        file_path = _normalize_path(raw_path)
        line = _resolve_line(file_path, raw_line, b_type, cwe, base_dir=base_dir)

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

    base_dir = Path.cwd()
    benchmark_input = convert(input_path, base_dir=base_dir)

    output_path.parent.mkdir(parents=True, exist_ok=True)
    with output_path.open("w", encoding="utf-8") as fh:
        json.dump(benchmark_input, fh, indent=2)

    total = len(benchmark_input["findings"])
    print(f"Converted {total} finding(s) -> {output_path}")


if __name__ == "__main__":
    main()
