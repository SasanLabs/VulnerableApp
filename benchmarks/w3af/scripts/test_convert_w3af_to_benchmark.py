"""
test_convert_w3af_to_benchmark.py
===================================
Unit tests for the w3af → benchmark conversion script.

Run:
    python3 -m pytest benchmarks/w3af/scripts/test_convert_w3af_to_benchmark.py -v
    # or from repo root:
    python3 -m pytest benchmarks/w3af/scripts/ -v
"""

import sys
import os
sys.path.insert(0, os.path.dirname(__file__))

from convert_w3af_to_benchmark import convert, _cwe_tag, _first_id, _path_only


# ---------------------------------------------------------------------------
# Helpers
# ---------------------------------------------------------------------------

def _make_report(items: list) -> dict:
    """Wrap items in a minimal w3af json_file structure."""
    return {
        "w3af-version": "1.6.0",
        "scan-info": {
            "target_urls": ["http://localhost/VulnerableApp/"],
            "target_domain": "localhost",
            "enabled_plugins": {"audit": ["xss", "sqli"]},
            "findings": [],
            "known_urls": [],
        },
        "start": "1759900000",
        "start-long": "Tue Oct  7 12:00:00 2025",
        "items": items,
    }


def _item(name="Cross Site Scripting", method="GET",
          url="http://localhost/VulnerableApp/XSSInImgTagAttribute/LEVEL_1?text=abc",
          cwe=None, wasc=None) -> dict:
    return {
        "Severity": "High",
        "Name": name,
        "HTTP method": method,
        "URL": url,
        "Vulnerable parameter": "text",
        "POST data": "",
        "Vulnerability IDs": "",
        "CWE IDs": [79] if cwe is None else cwe,
        "WASC IDs": [8] if wasc is None else wasc,
        "Tags": [],
        "VulnDB ID": "",
        "Description": "",
    }


# ---------------------------------------------------------------------------
# _first_id
# ---------------------------------------------------------------------------

def test_first_id_returns_none_for_missing():
    assert _first_id(None) is None


def test_first_id_returns_none_for_empty_list():
    assert _first_id([]) is None


def test_first_id_returns_none_for_zero():
    assert _first_id([0]) is None


def test_first_id_skips_leading_zero():
    assert _first_id([0, 89]) == "89"


def test_first_id_accepts_scalar():
    assert _first_id(89) == "89"


def test_first_id_accepts_string():
    assert _first_id(["79"]) == "79"


def test_first_id_strips_whitespace():
    assert _first_id(["  79  "]) == "79"


# ---------------------------------------------------------------------------
# _cwe_tag
# ---------------------------------------------------------------------------

def test_cwe_tag_prefixes_bare_digits():
    assert _cwe_tag([79]) == "CWE-79"


def test_cwe_tag_leaves_existing_prefix():
    assert _cwe_tag(["CWE-79"]) == "CWE-79"


def test_cwe_tag_is_case_insensitive_on_the_prefix():
    assert _cwe_tag(["cwe-79"]) == "cwe-79"


def test_cwe_tag_returns_none_for_zero():
    assert _cwe_tag([0]) is None


def test_cwe_tag_returns_none_for_missing():
    assert _cwe_tag(None) is None


# ---------------------------------------------------------------------------
# _path_only
# ---------------------------------------------------------------------------

def test_path_only_drops_the_query():
    assert _path_only("http://localhost/VulnerableApp/XSS/LEVEL_1?text=abc") == "/VulnerableApp/XSS/LEVEL_1"


def test_path_only_keeps_a_bare_path():
    assert _path_only("/XSS/LEVEL_1") == "/XSS/LEVEL_1"


def test_path_only_drops_the_fragment():
    assert _path_only("http://localhost/VulnerableApp/XSS#frag") == "/VulnerableApp/XSS"


def test_path_only_handles_empty():
    assert _path_only("") == ""


def test_path_only_handles_none():
    assert _path_only(None) == ""


# ---------------------------------------------------------------------------
# convert — shape
# ---------------------------------------------------------------------------

def test_convert_emits_tool_and_scan_type():
    result = convert(_make_report([_item()]))
    assert result["tool"] == "w3af"
    assert result["scanType"] == "DAST"


def test_convert_emits_one_finding_per_item():
    result = convert(_make_report([_item(), _item(url="http://localhost/VulnerableApp/SQLInjection/LEVEL_1")]))
    assert len(result["findings"]) == 2


def test_convert_maps_cwe_and_wasc():
    result = convert(_make_report([_item(cwe=[79], wasc=[8])]))
    assert result["findings"][0]["cwe"] == "CWE-79"
    assert result["findings"][0]["wascId"] == "8"


def test_convert_maps_method_uppercase():
    result = convert(_make_report([_item(method="post")]))
    assert result["findings"][0]["method"] == "POST"


def test_convert_omits_method_when_blank():
    result = convert(_make_report([_item(method="")]))
    assert "method" not in result["findings"][0]


def test_convert_omits_cwe_when_zero():
    result = convert(_make_report([_item(cwe=[0])]))
    assert "cwe" not in result["findings"][0]


def test_convert_omits_wasc_when_missing():
    result = convert(_make_report([_item(wasc=[])]))
    assert "wascId" not in result["findings"][0]


# ---------------------------------------------------------------------------
# convert — url handling
# ---------------------------------------------------------------------------

def test_convert_strips_the_query_from_the_url():
    result = convert(_make_report([_item(url="http://localhost/VulnerableApp/XSS/LEVEL_1?text=abc")]))
    assert result["findings"][0]["url"] == "/VulnerableApp/XSS/LEVEL_1"


def test_convert_skips_an_item_with_no_url():
    result = convert(_make_report([_item(url="")]))
    assert result["findings"] == []


# ---------------------------------------------------------------------------
# convert — dedup
# ---------------------------------------------------------------------------

def test_convert_deduplicates_identical_findings():
    a = _item(url="http://localhost/VulnerableApp/SQLInjection/LEVEL_1")
    b = _item(url="http://localhost/VulnerableApp/SQLInjection/LEVEL_1")
    result = convert(_make_report([a, b]))
    assert len(result["findings"]) == 1


def test_convert_keeps_findings_that_differ_by_method():
    a = _item(url="http://localhost/VulnerableApp/SQLInjection/LEVEL_1", method="GET")
    b = _item(url="http://localhost/VulnerableApp/SQLInjection/LEVEL_1", method="POST")
    result = convert(_make_report([a, b]))
    assert len(result["findings"]) == 2


def test_convert_keeps_findings_that_differ_by_cwe():
    a = _item(url="http://localhost/VulnerableApp/X/LEVEL_1", cwe=[79])
    b = _item(url="http://localhost/VulnerableApp/X/LEVEL_1", cwe=[89])
    result = convert(_make_report([a, b]))
    assert len(result["findings"]) == 2


# ---------------------------------------------------------------------------
# convert — degenerate input
# ---------------------------------------------------------------------------

def test_convert_handles_missing_items():
    assert convert({})["findings"] == []


def test_convert_handles_non_list_items():
    assert convert({"items": "nope"})["findings"] == []


def test_convert_handles_non_dict_item():
    assert convert({"items": [None, 42, "x"]})["findings"] == []


def test_convert_handles_empty_report():
    assert convert(_make_report([]))["findings"] == []
