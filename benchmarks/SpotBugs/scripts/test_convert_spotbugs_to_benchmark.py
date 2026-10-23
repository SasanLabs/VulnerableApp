"""
test_convert_spotbugs_to_benchmark.py
=====================================
Unit tests for the SpotBugs (with FindSecBugs) to benchmark conversion script.

Run:
    python3 -m pytest benchmarks/SpotBugs/scripts/test_convert_spotbugs_to_benchmark.py -v
    # or from repo root:
    python3 -m pytest benchmarks/SpotBugs/scripts/ -v
"""

import os
import sys
import xml.etree.ElementTree as ET

sys.path.insert(0, os.path.dirname(__file__))

from convert_spotbugs_to_benchmark import (
    _find_source_line,
    _format_cwe,
    _normalize_path,
    _resolve_line,
    convert,
)

SAMPLE_PATH = "org/sasanlabs/service/vulnerability/sqlInjection/UnionBasedSQLInjectionVulnerability.java"
NORMALIZED_PATH = "src/main/java/" + SAMPLE_PATH


def _make_xml(bug_instances_xml: str, bug_patterns_xml: str = "") -> str:
    return f"""<?xml version="1.0" encoding="UTF-8"?>
<BugCollection version="4.8.6" sequence="0" timestamp="1700000000000" analysisTimestamp="1700000000000" release="">
  {bug_instances_xml}
  {bug_patterns_xml}
</BugCollection>
"""


# ----------------------------------------------------------------------
# 1. CWE formatting tests
# ----------------------------------------------------------------------


def test_format_cwe_numeric():
    assert _format_cwe("89") == "CWE-89"


def test_format_cwe_with_prefix():
    assert _format_cwe("cwe-89") == "CWE-89"
    assert _format_cwe("CWE-89") == "CWE-89"


def test_format_cwe_empty_or_none():
    assert _format_cwe(None) is None
    assert _format_cwe("") is None
    assert _format_cwe("   ") is None


def test_format_cwe_embedded_in_text():
    assert _format_cwe("CWE-79: Cross-Site Scripting") == "CWE-79"


# ----------------------------------------------------------------------
# 2. Path normalization tests
# ----------------------------------------------------------------------


def test_normalize_path_prepends_src_main_java():
    assert _normalize_path(SAMPLE_PATH) == NORMALIZED_PATH


def test_normalize_path_keeps_existing_src_prefix():
    assert _normalize_path(NORMALIZED_PATH) == NORMALIZED_PATH


def test_normalize_path_handles_leading_dots_and_backslashes():
    assert _normalize_path(r".\org\sasanlabs\Foo.java") == "src/main/java/org/sasanlabs/Foo.java"
    assert _normalize_path("./src/main/java/org/sasanlabs/Foo.java") == "src/main/java/org/sasanlabs/Foo.java"


# ----------------------------------------------------------------------
# 3. Source line extraction tests
# ----------------------------------------------------------------------


def test_find_source_line_prefers_direct_primary():
    xml = """
    <BugInstance type="TEST">
      <SourceLine classname="Foo" start="10" end="10" sourcepath="Foo.java" primary="false"/>
      <SourceLine classname="Foo" start="20" end="20" sourcepath="Foo.java" primary="true"/>
    </BugInstance>
    """
    elem = ET.fromstring(xml)
    sl = _find_source_line(elem)
    assert sl is not None
    assert sl.get("start") == "20"


def test_find_source_line_falls_back_to_method():
    xml = """
    <BugInstance type="TEST">
      <Method classname="Foo" name="bar">
        <SourceLine classname="Foo" start="42" end="45" sourcepath="Foo.java" primary="true"/>
      </Method>
    </BugInstance>
    """
    elem = ET.fromstring(xml)
    sl = _find_source_line(elem)
    assert sl is not None
    assert sl.get("start") == "42"


def test_find_source_line_falls_back_to_class():
    xml = """
    <BugInstance type="TEST">
      <Class classname="Foo">
        <SourceLine classname="Foo" start="1" end="100" sourcepath="Foo.java"/>
      </Class>
    </BugInstance>
    """
    elem = ET.fromstring(xml)
    sl = _find_source_line(elem)
    assert sl is not None
    assert sl.get("start") == "1"


def test_find_source_line_returns_none_if_missing():
    xml = """<BugInstance type="TEST"/>"""
    elem = ET.fromstring(xml)
    assert _find_source_line(elem) is None


# ----------------------------------------------------------------------
# 4. XML report conversion tests
# ----------------------------------------------------------------------


def test_convert_maps_bug_instance_to_sast_finding():
    xml = _make_xml(
        bug_instances_xml=f"""
        <BugInstance type="SQL_INJECTION_SPRING_JDBC" category="SECURITY" cweid="89">
          <ShortMessage>Potential JDBC Injection (Spring JDBC)</ShortMessage>
          <SourceLine classname="UnionBasedSQLInjection" primary="true" start="84" end="84"
                      sourcepath="{SAMPLE_PATH}"/>
        </BugInstance>
        """
    )
    result = convert(xml)
    # Line 84 opens applicationJdbcTemplate.query(...); resolved to ground-truth line 85
    assert result == {
        "tool": "SpotBugs-FindSecBugs",
        "scanType": "SAST",
        "findings": [
            {
                "filePath": NORMALIZED_PATH,
                "line": 85,
                "cwe": "CWE-89",
                "type": "Potential JDBC Injection (Spring JDBC)",
            }
        ],
    }


def test_convert_resolves_cwe_and_desc_from_bug_pattern():
    xml = _make_xml(
        bug_instances_xml=f"""
        <BugInstance type="CUSTOM_SEC_PATTERN" category="SECURITY">
          <SourceLine classname="Foo" primary="true" start="50" end="50"
                      sourcepath="{SAMPLE_PATH}"/>
        </BugInstance>
        """,
        bug_patterns_xml="""
        <BugPattern type="CUSTOM_SEC_PATTERN" category="SECURITY" cweid="79">
          <ShortDescription>Potential Cross-Site Scripting</ShortDescription>
        </BugPattern>
        """,
    )
    result = convert(xml)
    assert result["findings"] == [
        {
            "filePath": NORMALIZED_PATH,
            "line": 50,
            "cwe": "CWE-79",
            "type": "Potential Cross-Site Scripting",
        }
    ]


def test_convert_deduplicates_identical_findings():
    xml = _make_xml(
        bug_instances_xml=f"""
        <BugInstance type="SQL_INJECTION_SPRING_JDBC" cweid="89">
          <ShortMessage>SQL Injection</ShortMessage>
          <SourceLine start="84" end="84" sourcepath="{SAMPLE_PATH}"/>
        </BugInstance>
        <BugInstance type="SQL_INJECTION_SPRING_JDBC" cweid="89">
          <ShortMessage>SQL Injection</ShortMessage>
          <SourceLine start="84" end="84" sourcepath="{SAMPLE_PATH}"/>
        </BugInstance>
        """
    )
    result = convert(xml)
    assert len(result["findings"]) == 1


def test_convert_keeps_findings_on_different_lines():
    xml = _make_xml(
        bug_instances_xml=f"""
        <BugInstance type="SQL_INJECTION" cweid="89">
          <ShortMessage>SQL Injection</ShortMessage>
          <SourceLine start="84" end="84" sourcepath="{SAMPLE_PATH}"/>
        </BugInstance>
        <BugInstance type="SQL_INJECTION" cweid="89">
          <ShortMessage>SQL Injection</ShortMessage>
          <SourceLine start="120" end="120" sourcepath="{SAMPLE_PATH}"/>
        </BugInstance>
        """
    )
    result = convert(xml)
    # Line 84 resolves to 85, line 120 remains 120
    assert [f["line"] for f in result["findings"]] == [85, 120]


def test_convert_skips_bugs_without_location():
    xml = _make_xml(
        bug_instances_xml="""
        <BugInstance type="SQL_INJECTION" cweid="89">
          <ShortMessage>SQL Injection</ShortMessage>
        </BugInstance>
        """
    )
    result = convert(xml)
    assert result["findings"] == []


def test_convert_skips_bugs_without_cwe_or_type(capsys):
    xml = _make_xml(
        bug_instances_xml=f"""
        <BugInstance type="">
          <SourceLine start="84" end="84" sourcepath="{SAMPLE_PATH}"/>
        </BugInstance>
        """
    )
    result = convert(xml)
    assert result["findings"] == []
    assert "WARNING: skipped" in capsys.readouterr().err


def test_convert_handles_empty_xml():
    xml = _make_xml("")
    result = convert(xml)
    assert result == {
        "tool": "SpotBugs-FindSecBugs",
        "scanType": "SAST",
        "findings": [],
    }


def test_convert_handles_raw_bytes():
    xml_bytes = _make_xml("").encode("utf-8")
    result = convert(xml_bytes)
    assert result == {
        "tool": "SpotBugs-FindSecBugs",
        "scanType": "SAST",
        "findings": [],
    }


# ----------------------------------------------------------------------
# 5. Line resolution tests (aligning bytecode calls with ground truth)
# ----------------------------------------------------------------------


def test_resolve_line_multiline_sql_injection():
    # UnionBasedSQLInjectionVulnerability.java: query() on 84, SQL on 85; query() on 114, SQL on 115
    res84 = _resolve_line(NORMALIZED_PATH, 84, "SQL_INJECTION_SPRING_JDBC", "CWE-89")
    assert res84 == 85

    res114 = _resolve_line(NORMALIZED_PATH, 114, "SQL_INJECTION_SPRING_JDBC", "CWE-89")
    assert res114 == 115


def test_resolve_line_single_line_sql_preserved():
    # AuthLoginService.java: line 51 contains both query() and arguments on same line
    auth_path = "src/main/java/org/sasanlabs/service/vulnerability/authentication/AuthLoginService.java"
    res51 = _resolve_line(auth_path, 51, "SQL_INJECTION_SPRING_JDBC", "CWE-89")
    assert res51 == 51


def test_resolve_line_non_sql_preserved():
    # Non-SQL warnings are not adjusted
    res = _resolve_line(NORMALIZED_PATH, 84, "PREDICTABLE_RANDOM", "CWE-330")
    assert res == 84


def test_resolve_line_nonexistent_file_preserved():
    # Missing source file gracefully falls back to reported line
    res = _resolve_line("src/main/java/NonExistent.java", 42, "SQL_INJECTION_SPRING_JDBC", "CWE-89")
    assert res == 42


