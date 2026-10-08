"""
test_convert_wapiti_to_benchmark.py
===================================
Unit tests for the Wapiti -> VulnerableApp benchmark conversion script.

Run:
    python -m pytest benchmarks/Wapiti/scripts/ -v
"""

import json
import subprocess
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


def test_extract_cwe_missing_from_classifications_returns_none():
    assert _extract_cwe_from_classifications("Command execution", {}) is None
    assert _extract_cwe_from_classifications("Path Traversal", {}) is None
    assert _extract_cwe_from_classifications("NonExistentCategory", {}) is None


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


def test_convert_deduplicates_identical_endpoints():
    report = {
        "classifications": {
            "SQL Injection": {
                "ref": {"CWE-89: SQL Injection": "https://cwe.mitre.org/data/definitions/89.html"}
            }
        },
        "vulnerabilities": {
            "SQL Injection": [
                {"method": "GET", "path": "/VulnerableApp/SQLInjection/LEVEL_1?id=1"},
                {"method": "GET", "path": "/VulnerableApp/SQLInjection/LEVEL_1?id=2"}
            ]
        }
    }
    res = convert(report)
    assert len(res["findings"]) == 1
    assert res["findings"][0]["cwe"] == "CWE-89"


def test_convert_distinguishes_different_http_methods():
    report = {
        "classifications": {"SQL Injection": {}},
        "vulnerabilities": {
            "SQL Injection": [
                {"method": "GET", "path": "/VulnerableApp/SQLInjection/LEVEL_1"},
                {"method": "POST", "path": "/VulnerableApp/SQLInjection/LEVEL_1"}
            ]
        }
    }
    res = convert(report)
    assert len(res["findings"]) == 2
    methods = {f["method"] for f in res["findings"]}
    assert methods == {"GET", "POST"}


def test_convert_parses_anomalies_section():
    report = {
        "classifications": {
            "Command execution": {
                "ref": {"CWE-78: OS Command Injection": "https://cwe.mitre.org/data/definitions/78.html"}
            }
        },
        "vulnerabilities": {},
        "anomalies": {
            "Command execution": [
                {"method": "POST", "path": "/VulnerableApp/CommandInjection/LEVEL_1"}
            ]
        }
    }
    res = convert(report)
    assert len(res["findings"]) == 1
    f = res["findings"][0]
    assert f["url"] == "/VulnerableApp/CommandInjection/LEVEL_1"
    assert f["cwe"] == "CWE-78"
    assert f["type"] == "COMMAND_INJECTION"


def test_convert_empty_or_corrupt_structures():
    assert convert({})["findings"] == []
    assert convert({"vulnerabilities": None})["findings"] == []
    assert convert({"vulnerabilities": {"SQL Injection": "not-a-list"}})["findings"] == []
    assert convert("not-a-dict")["findings"] == []


def test_cli_execution(tmp_path):
    input_file = tmp_path / "wapiti-raw.json"
    output_file = tmp_path / "benchmark-input.json"

    input_file.write_text(json.dumps({
        "classifications": {
            "Path Traversal": {
                "ref": {"CWE-22: Path Traversal": "https://cwe.mitre.org/data/definitions/22.html"}
            }
        },
        "vulnerabilities": {
            "Path Traversal": [{"method": "GET", "path": "/VulnerableApp/PathTraversal/LEVEL_1"}]
        }
    }))

    proc = subprocess.run([
        sys.executable,
        str(SCRIPTS_DIR / "convert_wapiti_to_benchmark.py"),
        "--input", str(input_file),
        "--output", str(output_file)
    ], capture_output=True, text=True)

    assert proc.returncode == 0
    assert output_file.exists()
    data = json.loads(output_file.read_text())
    assert data["tool"] == "Wapiti"
    assert len(data["findings"]) == 1
    assert data["findings"][0]["cwe"] == "CWE-22"


def test_clean_url_strips_trailing_slash():
    assert _clean_url("/VulnerableApp/SQLInjection/LEVEL_1/") == "/VulnerableApp/SQLInjection/LEVEL_1"
    assert _clean_url("/") == ""


def test_extract_cwe_from_reference_url():
    classifications = {
        "Custom Vuln": {
            "ref": {
                "Generic Title": "https://cwe.mitre.org/data/definitions/89.html"
            }
        }
    }
    assert _extract_cwe_from_classifications("Custom Vuln", classifications) == "CWE-89"


def test_convert_deduplicates_trailing_slash_variants():
    report = {
        "classifications": {"SQL Injection": {}},
        "vulnerabilities": {
            "SQL Injection": [
                {"method": "GET", "path": "/VulnerableApp/SQLInjection/LEVEL_1"},
                {"method": "GET", "path": "/VulnerableApp/SQLInjection/LEVEL_1/"}
            ]
        }
    }
    res = convert(report)
    assert len(res["findings"]) == 1


def test_convert_retains_category_as_type_when_no_cwe():
    report = {
        "classifications": {},
        "vulnerabilities": {
            "Unknown Custom Vulnerability": [
                {"method": "GET", "path": "/VulnerableApp/Custom/LEVEL_1"}
            ]
        }
    }
    res = convert(report)
    assert len(res["findings"]) == 1
    assert res["findings"][0]["type"] == "Unknown Custom Vulnerability"
    assert "cwe" not in res["findings"][0]
