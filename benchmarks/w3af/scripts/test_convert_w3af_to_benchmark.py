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

from convert_w3af_to_benchmark import convert, _all_ids, _cwe_tag, _path_only


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
# _all_ids
# ---------------------------------------------------------------------------

def test_all_ids_returns_empty_for_missing():
    assert _all_ids(None) == []


def test_all_ids_returns_empty_for_empty_list():
    assert _all_ids([]) == []


def test_all_ids_returns_empty_for_zero():
    assert _all_ids([0]) == []


def test_all_ids_skips_leading_zero():
    assert _all_ids([0, 89]) == ["89"]


def test_all_ids_accepts_scalar():
    assert _all_ids(89) == ["89"]


def test_all_ids_accepts_string():
    assert _all_ids(["79"]) == ["79"]


def test_all_ids_strips_whitespace():
    assert _all_ids(["  79  "]) == ["79"]


def test_all_ids_keeps_every_entry():
    assert _all_ids([74, 79]) == ["74", "79"]


def test_all_ids_keeps_later_entries_around_a_zero():
    assert _all_ids([0, 74, 79]) == ["74", "79"]


# ---------------------------------------------------------------------------
# _cwe_tag
# ---------------------------------------------------------------------------

def test_cwe_tag_prefixes_bare_digits():
    assert _cwe_tag("79") == "CWE-79"


def test_cwe_tag_leaves_existing_prefix():
    assert _cwe_tag("CWE-79") == "CWE-79"


def test_cwe_tag_is_case_insensitive_on_the_prefix():
    assert _cwe_tag("cwe-79") == "cwe-79"


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


def test_convert_keeps_every_cwe_id():
    result = convert(_make_report([_item(cwe=[74, 79], wasc=[8])]))
    assert [f["cwe"] for f in result["findings"]] == ["CWE-74", "CWE-79"]


def test_convert_can_match_on_a_later_cwe_id():
    # A ground truth row keyed on CWE-79 must not read as a miss just because
    # w3af happened to list CWE-74 first.
    result = convert(_make_report([_item(cwe=[74, 79], wasc=[8])]))
    assert "CWE-79" in [f["cwe"] for f in result["findings"]]


def test_convert_keeps_every_wasc_id():
    result = convert(_make_report([_item(cwe=[79], wasc=[8, 40])]))
    assert [f["wascId"] for f in result["findings"]] == ["8", "40"]


def test_convert_pairs_each_cwe_with_each_wasc():
    result = convert(_make_report([_item(cwe=[74, 79], wasc=[8, 40])]))
    assert {(f["cwe"], f["wascId"]) for f in result["findings"]} == {
        ("CWE-74", "8"),
        ("CWE-74", "40"),
        ("CWE-79", "8"),
        ("CWE-79", "40"),
    }


def test_convert_deduplicates_the_same_alternatives_from_two_plugins():
    a = _item(cwe=[74, 79], wasc=[8])
    b = _item(cwe=[79, 74], wasc=[8])
    result = convert(_make_report([a, b]))
    assert len(result["findings"]) == 2


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
