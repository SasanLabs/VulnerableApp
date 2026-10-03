"""
test_convert_wapiti_to_benchmark.py
===================================
Unit tests for the Wapiti -> VulnerableApp benchmark conversion script.

Run:
    python -m pytest benchmarks/Wapiti/scripts/ -v
"""

import sys
from pathlib import Path

# Add scripts directory to sys.path
SCRIPTS_DIR = Path(__file__).resolve().parent
if str(SCRIPTS_DIR) not in sys.path:
    sys.path.insert(0, str(SCRIPTS_DIR))

import pytest
from convert_wapiti_to_benchmark import (
    convert,
    _extract_cwe_from_classifications,
    _clean_url
)


def test_clean_url_strips_query_and_fragments():
    assert _clean_url("/VulnerableApp/SQLInjection/LEVEL_1?id=1") == "/VulnerableApp/SQLInjection/LEVEL_1"
    assert _clean_url("/VulnerableApp/SQLInjection/LEVEL_1#anchor") == "/VulnerableApp/SQLInjection/LEVEL_1"
    assert _clean_url("/VulnerableApp/SQLInjection/LEVEL_1;payload") == "/VulnerableApp/SQLInjection/LEVEL_1"
    assert _clean_url("") == ""


def test_extract_cwe_from_reference():
    classifications = {
        "SQL Injection": {
            "ref": {
                "CWE-89: SQL Injection": "https://cwe.mitre.org/data/definitions/89.html"
            }
        }
    }
    assert _extract_cwe_from_classifications("SQL Injection", classifications) == "CWE-89"


def test_convert_minimal_report():
    report = {
        "classifications": {
            "SQL Injection": {
                "ref": {"CWE-89: SQL Injection": "https://cwe.mitre.org/data/definitions/89.html"}
            }
        },
        "vulnerabilities": {
            "SQL Injection": [
                {
                    "method": "GET",
                    "path": "/VulnerableApp/SQLInjection/LEVEL_1?id=1",
                    "parameter": "id"
                }
            ]
        }
    }
    res = convert(report)
    assert res["tool"] == "Wapiti"
    assert res["scanType"] == "DAST"
    assert len(res["findings"]) == 1
    assert res["findings"][0]["url"] == "/VulnerableApp/SQLInjection/LEVEL_1"
    assert res["findings"][0]["method"] == "GET"
    assert res["findings"][0]["cwe"] == "CWE-89"
