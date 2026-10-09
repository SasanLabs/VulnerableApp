"""
test_convert_semgrep_to_benchmark.py
====================================
Unit tests for the Semgrep to benchmark conversion script.

Run:
    python3 -m pytest benchmarks/Semgrep/scripts/test_convert_semgrep_to_benchmark.py -v
    # or from repo root:
    python3 -m pytest benchmarks/Semgrep/scripts/ -v
"""

import sys
import os
sys.path.insert(0, os.path.dirname(__file__))

from convert_semgrep_to_benchmark import convert, _cwe_tags

SQLI_CWE = "CWE-89: Improper Neutralization of Special Elements used in an SQL Command ('SQL Injection')"
XXE_CWES = [
    "CWE-611: Improper Restriction of XML External Entity Reference",
    "CWE-776: Improper Restriction of Recursive Entity References in DTDs ('XML Entity Expansion')",
]
PATH = "src/main/java/org/sasanlabs/service/vulnerability/sqlInjection/ErrorBasedSQLInjectionVulnerability.java"


def _result(path=PATH, line=143, cwe=None, vulnerability_class=None) -> dict:
    metadata = {}
    if cwe is not None:
        metadata["cwe"] = cwe
    if vulnerability_class is not None:
        metadata["vulnerability_class"] = vulnerability_class
    return {
        "check_id": "java.lang.security.audit.tainted-sql-string.tainted-sql-string",
        "path": path,
        "start": {"line": line, "col": 21},
        "end": {"line": line, "col": 60},
        "extra": {"metadata": metadata},
    }


def test_cwe_tags_strip_the_description():
    assert _cwe_tags([SQLI_CWE]) == ["CWE-89"]


def test_cwe_tags_accept_a_plain_string():
    assert _cwe_tags(SQLI_CWE) == ["CWE-89"]


def test_cwe_tags_keep_every_cwe_once_in_order():
    assert _cwe_tags(XXE_CWES + ["cwe-611"]) == ["CWE-611", "CWE-776"]


def test_cwe_tags_are_empty_without_a_cwe():
    assert _cwe_tags(None) == []
    assert _cwe_tags([]) == []
    assert _cwe_tags("no id here") == []


def test_convert_maps_a_result_to_a_sast_finding():
    report = {"results": [_result(cwe=[SQLI_CWE], vulnerability_class=["SQL Injection"])]}
    assert convert(report) == {
        "tool": "Semgrep",
        "scanType": "SAST",
        "findings": [
            {"filePath": PATH, "line": 143, "cwe": "CWE-89", "type": "SQL Injection"}
        ],
    }


def test_convert_emits_one_finding_per_cwe_with_the_type_on_the_first():
    report = {"results": [_result(cwe=XXE_CWES, vulnerability_class=["XML Injection"])]}
    assert convert(report)["findings"] == [
        {"filePath": PATH, "line": 143, "cwe": "CWE-611", "type": "XML Injection"},
        {"filePath": PATH, "line": 143, "cwe": "CWE-776"},
    ]


def test_convert_keeps_type_only_findings():
    report = {"results": [_result(vulnerability_class="SQL Injection")]}
    assert convert(report)["findings"] == [
        {"filePath": PATH, "line": 143, "type": "SQL Injection"}
    ]


def test_convert_skips_findings_without_cwe_or_type(capsys):
    assert convert({"results": [_result()]})["findings"] == []
    assert "tainted-sql-string at " + PATH + ":143" in capsys.readouterr().err


def test_convert_deduplicates_identical_findings():
    result = _result(cwe=[SQLI_CWE])
    assert len(convert({"results": [result, result]})["findings"]) == 1


def test_convert_keeps_findings_on_different_lines():
    report = {"results": [_result(line=83, cwe=[SQLI_CWE]), _result(line=143, cwe=[SQLI_CWE])]}
    assert [f["line"] for f in convert(report)["findings"]] == [83, 143]


def test_convert_skips_results_without_a_location():
    assert convert({"results": [{"extra": {}}]})["findings"] == []


def test_convert_handles_an_empty_report():
    assert convert({})["findings"] == []
