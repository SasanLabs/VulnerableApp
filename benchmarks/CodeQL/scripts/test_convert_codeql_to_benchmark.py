"""
test_convert_codeql_to_benchmark.py
===================================
Unit tests for the CodeQL to benchmark conversion script.

Run:
    python3 -m pytest benchmarks/CodeQL/scripts/test_convert_codeql_to_benchmark.py -v
    # or from repo root:
    python3 -m pytest benchmarks/CodeQL/scripts/ -v
"""

import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))

import pytest

from convert_codeql_to_benchmark import _cwe_tags, _normalise_path, convert

PATH = (
    "src/main/java/org/sasanlabs/service/vulnerability/sqlInjection/"
    "ErrorBasedSQLInjectionVulnerability.java"
)
RULE_ID = "java/sql-injection"
SQLI_RULE = {
    "id": RULE_ID,
    "helpUri": "https://codeql.github.com/codeql-query-help/java/java-sql-injection/",
    "properties": {
        "tags": ["security", "external/cwe/cwe-089", "external/cwe/cwe-564"]
    },
}


def _result(uri=PATH, line=143, rule_id=RULE_ID, rule_index=None) -> dict:
    result = {
        "ruleId": rule_id,
        "locations": [
            {
                "physicalLocation": {
                    "artifactLocation": {"uri": uri},
                    "region": {"startLine": line},
                }
            }
        ],
    }
    if rule_index is not None:
        result["ruleIndex"] = rule_index
    return result


def _report(results, rules=None, extensions=None, invocations=None) -> dict:
    run = {
        "tool": {
            "driver": {
                "name": "CodeQL",
                "rules": rules if rules is not None else [SQLI_RULE],
            }
        },
        "results": results,
    }
    if extensions is not None:
        run["tool"]["extensions"] = extensions
    if invocations is not None:
        run["invocations"] = invocations
    return {"version": "2.1.0", "runs": [run]}


def _result_with_code_flow(primary_line=145, source_line=120, sink_line=143) -> dict:
    """Result whose primary location differs from the flow's source and sink lines."""

    def loc(line):
        return {
            "location": {
                "physicalLocation": {
                    "artifactLocation": {"uri": PATH},
                    "region": {"startLine": line},
                }
            }
        }

    result = _result(line=primary_line)
    result["codeFlows"] = [{"threadFlows": [{"locations": [loc(source_line), loc(sink_line)]}]}]
    return result


# CWE metadata


def test_cwe_tags_read_tags_and_remove_leading_zeroes():
    assert _cwe_tags(SQLI_RULE) == ["CWE-89", "CWE-564"]


def test_cwe_tags_accept_a_single_string_tag():
    assert _cwe_tags({"properties": {"tags": "external/cwe/cwe-079"}}) == ["CWE-79"]


def test_cwe_tags_fall_back_to_mitre_help_uri():
    rule = {"helpUri": "https://cwe.mitre.org/data/definitions/078.html"}
    assert _cwe_tags(rule) == ["CWE-78"]


def test_cwe_tags_keep_each_cwe_once_in_order():
    rule = {
        "helpUri": "https://cwe.mitre.org/data/definitions/89.html",
        "properties": {
            "tags": ["external/cwe/cwe-089", "external/cwe/cwe-564", "CWE-564"]
        },
    }
    assert _cwe_tags(rule) == ["CWE-89", "CWE-564"]


def test_cwe_tags_are_empty_without_valid_cwe_metadata():
    assert _cwe_tags(None) == []
    assert _cwe_tags({}) == []
    assert _cwe_tags({"properties": {"tags": ["security", "maintainability"]}}) == []
    assert _cwe_tags({"properties": {"tags": [None, 42]}}) == []
    assert _cwe_tags({"properties": {"tags": ["CWE-0"]}}) == []


# File paths


def test_normalise_path_keeps_project_relative_path():
    assert _normalise_path(PATH) == PATH


def test_normalise_path_strips_leading_dot_slash():
    assert _normalise_path("./src/A.java") == "src/A.java"


def test_normalise_path_decodes_percent_encoding_and_backslashes():
    assert _normalise_path("src/My%20Dir/A.java") == "src/My Dir/A.java"
    assert _normalise_path(r"src\main\A.java") == "src/main/A.java"


def test_normalise_path_strips_srcroot_prefix():
    assert _normalise_path("%SRCROOT%/src/A.java") == "src/A.java"


@pytest.mark.parametrize("uri", [None, "", "   ", 42])
def test_normalise_path_rejects_missing_or_invalid_uri(uri):
    assert _normalise_path(uri) is None


def test_normalise_path_rejects_relative_paths_that_escape_source_root(tmp_path):
    assert _normalise_path("../outside/A.java", tmp_path) is None
    assert _normalise_path("src/../../outside/A.java", tmp_path) is None


def test_normalise_path_rejects_unrelated_absolute_path(tmp_path):
    assert _normalise_path("/etc/passwd", tmp_path) is None


def test_normalise_path_accepts_absolute_path_inside_source_root(tmp_path):
    source = tmp_path / "src" / "A.java"
    assert _normalise_path(str(source), tmp_path) == "src/A.java"
    assert _normalise_path(source.as_uri(), tmp_path) == "src/A.java"


def test_normalise_path_rejects_absolute_path_outside_source_root(tmp_path):
    outside = tmp_path.parent / "outside" / "A.java"
    assert _normalise_path(str(outside), tmp_path) is None
    assert _normalise_path(outside.as_uri(), tmp_path) is None


# SARIF conversion


def test_convert_maps_one_result_to_a_finding_per_cwe():
    assert convert(_report([_result()])) == {
        "tool": "CodeQL",
        "scanType": "SAST",
        "findings": [
            {"filePath": PATH, "line": 143, "cwe": "CWE-89"},
            {"filePath": PATH, "line": 143, "cwe": "CWE-564"},
        ],
    }


def test_convert_does_not_invent_a_vulnerability_type():
    assert all("type" not in finding for finding in convert(_report([_result()]))["findings"])


def test_convert_finds_rule_by_index_when_rule_id_is_unknown():
    report = _report([_result(rule_id="unknown/id", rule_index=0)])
    assert [f["cwe"] for f in convert(report)["findings"]] == ["CWE-89", "CWE-564"]


def test_convert_ignores_invalid_or_boolean_rule_index(capsys):
    report = _report(
        [
            _result(rule_id="unknown/id", rule_index=5),
            _result(rule_id="unknown/id", rule_index=True, line=7),
        ]
    )
    assert convert(report)["findings"] == []
    assert capsys.readouterr().err.count("rule metadata has no CWE") == 2


def test_convert_reads_rules_from_extensions():
    extension_rule = {
        "id": "java/xxe",
        "properties": {"tags": ["external/cwe/cwe-611"]},
    }
    report = _report(
        [_result(rule_id="java/xxe")],
        rules=[],
        extensions=[{"name": "codeql/java-queries", "rules": [extension_rule]}],
    )
    assert convert(report)["findings"] == [
        {"filePath": PATH, "line": 143, "cwe": "CWE-611"}
    ]


def test_convert_skips_rules_without_cwe(capsys):
    rule = {"id": RULE_ID, "properties": {"tags": ["maintainability"]}}
    assert convert(_report([_result()], rules=[rule]))["findings"] == []
    assert f"{RULE_ID} at {PATH}:143" in capsys.readouterr().err


def test_convert_deduplicates_identical_findings():
    result = _result()
    assert len(convert(_report([result, result]))["findings"]) == 2


def test_convert_keeps_findings_on_different_lines():
    report = _report([_result(line=83), _result(line=143)])
    lines = [
        finding["line"]
        for finding in convert(report)["findings"]
        if finding["cwe"] == "CWE-89"
    ]
    assert lines == [83, 143]


def test_convert_reports_primary_line_not_code_flow_lines():
    # The flow touches lines 120 and 143, but CodeQL reported the alert on 145.
    findings = convert(_report([_result_with_code_flow()]))["findings"]
    assert {f["line"] for f in findings} == {145}


def test_convert_never_moves_a_finding_to_an_expected_line():
    # One line away from the expected line 143 must stay on 144, not become 143.
    findings = convert(_report([_result(line=144)]))["findings"]
    assert [f["line"] for f in findings if f["cwe"] == "CWE-89"] == [144]
    assert all(f["line"] != 143 for f in findings)


def test_convert_uses_only_the_primary_location():
    result = _result()
    result["locations"].append(
        {
            "physicalLocation": {
                "artifactLocation": {"uri": "src/Other.java"},
                "region": {"startLine": 1},
            }
        }
    )
    assert {f["filePath"] for f in convert(_report([result]))["findings"]} == {PATH}


def test_convert_skips_results_without_a_location(capsys):
    result = _result()
    result["locations"] = []
    assert convert(_report([result]))["findings"] == []
    assert "without a primary location" in capsys.readouterr().err


@pytest.mark.parametrize("line", [None, 0, -3, "12", True])
def test_convert_skips_invalid_line_numbers(line, capsys):
    assert convert(_report([_result(line=line)]))["findings"] == []
    assert "missing/invalid project path or line" in capsys.readouterr().err


def test_convert_skips_findings_outside_source_root(tmp_path, capsys):
    report = _report([_result(uri="../outside/A.java")])
    assert convert(report, tmp_path)["findings"] == []
    assert "missing/invalid project path or line" in capsys.readouterr().err


def test_convert_skips_malformed_results_without_crashing():
    report = _report(["not a dict", {"ruleId": RULE_ID, "locations": ["nope"]}])
    assert convert(report)["findings"] == []


def test_convert_handles_empty_report():
    expected = {"tool": "CodeQL", "scanType": "SAST", "findings": []}
    assert convert({}) == expected
    assert convert({"runs": []}) == expected


def test_convert_refuses_unsuccessful_analysis():
    report = _report([_result()], invocations=[{"executionSuccessful": False}])
    with pytest.raises(ValueError, match="unsuccessful analysis invocation"):
        convert(report)


def test_convert_accepts_successful_analysis():
    report = _report([_result()], invocations=[{"executionSuccessful": True}])
    assert len(convert(report)["findings"]) == 2


def test_convert_rejects_non_object_document():
    with pytest.raises(ValueError, match="JSON object"):
        convert([])


def test_convert_rejects_invalid_runs_shape():
    with pytest.raises(ValueError, match="'runs' must be an array"):
        convert({"runs": {}})