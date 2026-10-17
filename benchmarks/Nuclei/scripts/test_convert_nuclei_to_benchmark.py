"""
test_convert_nuclei_to_benchmark.py
===================================
Unit tests for the Nuclei to benchmark conversion script.

Run:
    python3 -m pytest benchmarks/Nuclei/scripts/test_convert_nuclei_to_benchmark.py -v
    # or from repo root:
    python3 -m pytest benchmarks/Nuclei/scripts/ -v
"""

import json
import os
import sys

sys.path.insert(0, os.path.dirname(__file__))

from convert_nuclei_to_benchmark import (
    _cwe_tag,
    _load_results,
    _method_from_request,
    convert,
)


# ---------------------------------------------------------------------------
# Helpers
# ---------------------------------------------------------------------------

def _result(template="sql-injection", cwe=("CWE-89",), url=None, request=None,
            info=None) -> dict:
    """Build a minimal Nuclei result object."""
    if url is None:
        url = "http://localhost:9090/VulnerableApp/SQLInjection/LEVEL_1"
    if info is None:
        info = {"name": "SQL Injection", "severity": "high"}
        if cwe is not None:
            info["classification"] = {"cwe-id": list(cwe)}
    result = {
        "template-id": template,
        "info": info,
        "type": "http",
        "host": "http://localhost:9090",
        "matched-at": url,
    }
    if request is not None:
        result["request"] = request
    return result


# ---------------------------------------------------------------------------
# _cwe_tag
# ---------------------------------------------------------------------------

def test_cwe_tag_prefixes_bare_digits():
    assert _cwe_tag("89") == "CWE-89"


def test_cwe_tag_keeps_existing_prefix():
    assert _cwe_tag("CWE-89") == "CWE-89"


def test_cwe_tag_prefix_is_case_insensitive():
    assert _cwe_tag("cwe-79") == "cwe-79"


def test_cwe_tag_takes_first_entry_of_list():
    assert _cwe_tag(["CWE-89", "CWE-564"]) == "CWE-89"


def test_cwe_tag_returns_none_for_empty_list():
    assert _cwe_tag([]) is None


def test_cwe_tag_returns_none_for_zero():
    assert _cwe_tag("0") is None


def test_cwe_tag_returns_none_for_none():
    assert _cwe_tag(None) is None


def test_cwe_tag_strips_whitespace():
    assert _cwe_tag(" 89 ") == "CWE-89"


# ---------------------------------------------------------------------------
# _method_from_request
# ---------------------------------------------------------------------------

def test_method_read_from_request_line():
    raw = "GET /VulnerableApp/SQLInjection/LEVEL_1 HTTP/1.1\r\nHost: localhost\r\n"
    assert _method_from_request(raw) == "GET"


def test_method_is_uppercased():
    raw = "post /VulnerableApp/CommandInjection/LEVEL_1 HTTP/1.1\r\nHost: localhost\r\n"
    assert _method_from_request(raw) == "POST"


def test_method_returns_none_for_relative_path_only():
    assert _method_from_request("/VulnerableApp/SQLInjection/LEVEL_1") is None


def test_method_returns_none_for_empty_request():
    assert _method_from_request("") is None


def test_method_returns_none_for_none():
    assert _method_from_request(None) is None


def test_method_returns_none_for_non_string():
    assert _method_from_request({"raw": "GET / HTTP/1.1"}) is None


# ---------------------------------------------------------------------------
# convert
# ---------------------------------------------------------------------------

def test_convert_produces_correct_tool_and_scan_type():
    out = convert([])
    assert out["tool"] == "Nuclei"
    assert out["scanType"] == "DAST"


def test_convert_single_finding_fields():
    out = convert([_result()])
    assert out["findings"] == [{
        "url": "http://localhost:9090/VulnerableApp/SQLInjection/LEVEL_1",
        "cwe": "CWE-89",
    }]


def test_convert_emits_method_when_request_present():
    raw = "GET /VulnerableApp/SQLInjection/LEVEL_1 HTTP/1.1\r\nHost: localhost\r\n"
    out = convert([_result(request=raw)])
    assert out["findings"][0]["method"] == "GET"


def test_convert_multiple_results_produce_multiple_findings():
    out = convert([
        _result(url="http://localhost:9090/VulnerableApp/SQLInjection/LEVEL_1"),
        _result(url="http://localhost:9090/VulnerableApp/SQLInjection/LEVEL_2"),
    ])
    assert len(out["findings"]) == 2


def test_convert_deduplicates_identical_findings():
    out = convert([_result(), _result()])
    assert len(out["findings"]) == 1


def test_convert_deduplicates_across_templates():
    out = convert([
        _result(template="sql-injection"),
        _result(template="sqli-error-based"),
    ])
    assert len(out["findings"]) == 1


def test_convert_keeps_same_url_with_different_cwe():
    out = convert([
        _result(cwe=("CWE-89",)),
        _result(cwe=("CWE-79",)),
    ])
    assert len(out["findings"]) == 2


def test_convert_omits_cwe_when_classification_missing():
    out = convert([_result(cwe=None)])
    assert "cwe" not in out["findings"][0]


def test_convert_falls_back_to_host_when_matched_at_missing():
    result = _result()
    del result["matched-at"]
    out = convert([result])
    assert out["findings"][0]["url"] == "http://localhost:9090"


def test_convert_skips_results_without_url():
    result = _result()
    del result["matched-at"]
    del result["host"]
    assert convert([result])["findings"] == []


def test_convert_skips_non_dict_results():
    assert convert(["not-a-dict", 42, None])["findings"] == []


def test_convert_handles_missing_info():
    result = _result()
    del result["info"]
    out = convert([result])
    assert out["findings"] == [{
        "url": "http://localhost:9090/VulnerableApp/SQLInjection/LEVEL_1",
    }]


# ---------------------------------------------------------------------------
# _load_results
# ---------------------------------------------------------------------------

def test_load_results_reads_jsonl(tmp_path):
    path = tmp_path / "nuclei.jsonl"
    path.write_text(
        json.dumps(_result(template="a")) + "\n"
        + json.dumps(_result(template="b", url="http://localhost/x")) + "\n",
        encoding="utf-8",
    )
    assert len(_load_results(path)) == 2


def test_load_results_skips_blank_lines(tmp_path):
    path = tmp_path / "nuclei.jsonl"
    path.write_text(
        json.dumps(_result()) + "\n\n   \n" + json.dumps(_result(template="b")) + "\n",
        encoding="utf-8",
    )
    assert len(_load_results(path)) == 2


def test_load_results_reads_json_array(tmp_path):
    path = tmp_path / "nuclei.json"
    path.write_text(json.dumps([_result(), _result(template="b")]), encoding="utf-8")
    assert len(_load_results(path)) == 2


def test_load_results_reads_single_object(tmp_path):
    path = tmp_path / "nuclei.json"
    path.write_text(json.dumps(_result()), encoding="utf-8")
    assert len(_load_results(path)) == 1


def test_load_results_returns_empty_for_empty_file(tmp_path):
    path = tmp_path / "nuclei.jsonl"
    path.write_text("", encoding="utf-8")
    assert _load_results(path) == []


def test_load_results_output_converts(tmp_path):
    """End to end: a JSONL file goes in, benchmark input comes out."""
    path = tmp_path / "nuclei.jsonl"
    raw = "GET /VulnerableApp/CommandInjection/LEVEL_1 HTTP/1.1\r\nHost: localhost\r\n"
    path.write_text(
        json.dumps(_result(cwe=("CWE-77",), request=raw,
                           url="http://localhost:9090/VulnerableApp/CommandInjection/LEVEL_1")) + "\n",
        encoding="utf-8",
    )
    out = convert(_load_results(path))
    assert out == {
        "tool": "Nuclei",
        "scanType": "DAST",
        "findings": [{
            "url": "http://localhost:9090/VulnerableApp/CommandInjection/LEVEL_1",
            "cwe": "CWE-77",
            "method": "GET",
        }],
    }
