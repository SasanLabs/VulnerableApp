"""
test_convert_zap_to_benchmark.py
=================================
Unit tests for the ZAP → benchmark conversion script.

Run:
    python3 -m pytest benchmarks/ZAP/scripts/test_convert_zap_to_benchmark.py -v
    # or from repo root:
    python3 -m pytest benchmarks/ZAP/scripts/ -v
"""

import sys
import os
sys.path.insert(0, os.path.dirname(__file__))

from convert_zap_to_benchmark import convert, _cwe_tag, _normalise_id


# ---------------------------------------------------------------------------
# Helpers
# ---------------------------------------------------------------------------

def _make_report(alerts: list) -> dict:
    """Wrap alerts in a minimal ZAP traditional-json structure."""
    return {
        "@version": "2.14.0",
        "site": [
            {
                "@name": "http://localhost",
                "@host": "localhost",
                "alerts": alerts,
            }
        ],
    }


def _alert(name="SQL Injection", cweid="89", wascid="19",
           instances=None) -> dict:
    if instances is None:
        instances = [
            {
                "uri": "http://localhost/VulnerableApp/SQLInjection/LEVEL_1",
                "method": "GET",
            }
        ]
    return {
        "name": name,
        "cweid": cweid,
        "wascid": wascid,
        "instances": instances,
    }


# ---------------------------------------------------------------------------
# _normalise_id
# ---------------------------------------------------------------------------

def test_normalise_id_returns_none_for_zero():
    assert _normalise_id("0") is None


def test_normalise_id_returns_none_for_empty():
    assert _normalise_id("") is None


def test_normalise_id_returns_none_for_none():
    assert _normalise_id(None) is None


def test_normalise_id_returns_value_for_valid():
    assert _normalise_id("89") == "89"


def test_normalise_id_strips_whitespace():
    assert _normalise_id("  89  ") == "89"


# ---------------------------------------------------------------------------
# _cwe_tag
# ---------------------------------------------------------------------------

def test_cwe_tag_prefixes_bare_digits():
    assert _cwe_tag("89") == "CWE-89"


def test_cwe_tag_does_not_double_prefix():
    assert _cwe_tag("CWE-89") == "CWE-89"


def test_cwe_tag_case_insensitive_prefix():
    assert _cwe_tag("cwe-89") == "cwe-89"   # preserves original casing after check


def test_cwe_tag_returns_none_for_zero():
    assert _cwe_tag("0") is None


def test_cwe_tag_returns_none_for_none():
    assert _cwe_tag(None) is None


# ---------------------------------------------------------------------------
# convert — basic happy path
# ---------------------------------------------------------------------------

def test_convert_produces_correct_tool_and_scan_type():
    result = convert(_make_report([_alert()]))
    assert result["tool"] == "ZAP"
    assert result["scanType"] == "DAST"


def test_convert_single_finding_fields():
    result = convert(_make_report([_alert()]))
    assert len(result["findings"]) == 1
    f = result["findings"][0]
    assert f["url"] == "http://localhost/VulnerableApp/SQLInjection/LEVEL_1"
    assert f["cwe"] == "CWE-89"
    assert f["wascId"] == "19"
    assert f["method"] == "GET"


def test_convert_multiple_instances_produce_multiple_findings():
    alert = _alert(instances=[
        {"uri": "http://localhost/VulnerableApp/SQLInjection/LEVEL_1", "method": "GET"},
        {"uri": "http://localhost/VulnerableApp/SQLInjection/LEVEL_2", "method": "POST"},
    ])
    result = convert(_make_report([alert]))
    assert len(result["findings"]) == 2


def test_convert_multiple_alerts_produce_multiple_findings():
    alerts = [
        _alert(name="SQL Injection",     cweid="89",  wascid="19"),
        _alert(name="Cross Site Scripting", cweid="79", wascid="8",
               instances=[{"uri": "http://localhost/VulnerableApp/XSS/LEVEL_1",
                            "method": "GET"}]),
    ]
    result = convert(_make_report(alerts))
    assert len(result["findings"]) == 2


# ---------------------------------------------------------------------------
# convert — deduplication
# ---------------------------------------------------------------------------

def test_convert_deduplicates_identical_findings():
    instance = {"uri": "http://localhost/VulnerableApp/SQLInjection/LEVEL_1",
                "method": "GET"}
    alert = _alert(instances=[instance, instance])
    result = convert(_make_report([alert]))
    assert len(result["findings"]) == 1


def test_convert_deduplicates_across_alerts():
    instance = {"uri": "http://localhost/VulnerableApp/SQLInjection/LEVEL_1",
                "method": "GET"}
    alerts = [_alert(instances=[instance]), _alert(instances=[instance])]
    result = convert(_make_report(alerts))
    assert len(result["findings"]) == 1


# ---------------------------------------------------------------------------
# convert — zero / missing CWE / WASC
# ---------------------------------------------------------------------------

def test_convert_omits_cwe_when_zero():
    alert = _alert(cweid="0", wascid="19")
    result = convert(_make_report([alert]))
    assert "cwe" not in result["findings"][0]


def test_convert_omits_wasc_when_zero():
    alert = _alert(cweid="89", wascid="0")
    result = convert(_make_report([alert]))
    assert "wascId" not in result["findings"][0]


def test_convert_omits_both_when_zero():
    alert = _alert(cweid="0", wascid="0")
    result = convert(_make_report([alert]))
    f = result["findings"][0]
    assert "cwe" not in f
    assert "wascId" not in f


# ---------------------------------------------------------------------------
# convert — missing / empty method
# ---------------------------------------------------------------------------

def test_convert_omits_method_when_empty():
    alert = _alert(instances=[{
        "uri": "http://localhost/VulnerableApp/SQLInjection/LEVEL_1",
        "method": "",
    }])
    result = convert(_make_report([alert]))
    assert "method" not in result["findings"][0]


def test_convert_uppercases_method():
    alert = _alert(instances=[{
        "uri": "http://localhost/VulnerableApp/SQLInjection/LEVEL_1",
        "method": "post",
    }])
    result = convert(_make_report([alert]))
    assert result["findings"][0]["method"] == "POST"


# ---------------------------------------------------------------------------
# convert — empty / missing instances
# ---------------------------------------------------------------------------

def test_convert_skips_alerts_with_no_instances():
    alert = _alert(instances=[])
    result = convert(_make_report([alert]))
    assert result["findings"] == []


def test_convert_skips_instances_with_empty_uri():
    alert = _alert(instances=[{"uri": "", "method": "GET"}])
    result = convert(_make_report([alert]))
    assert result["findings"] == []


# ---------------------------------------------------------------------------
# convert — empty report
# ---------------------------------------------------------------------------

def test_convert_empty_report_returns_empty_findings():
    result = convert({"site": []})
    assert result["findings"] == []


def test_convert_no_site_key_returns_empty_findings():
    result = convert({})
    assert result["findings"] == []

def test_convert_bare_list_input():
    """ZAP CLI modes can emit a bare site array instead of a wrapped dict."""
    site = {
        "@name": "http://localhost",
        "alerts": [_alert()],
    }
    result = convert([site])
    assert len(result["findings"]) == 1
    assert result["findings"][0]["cwe"] == "CWE-89"