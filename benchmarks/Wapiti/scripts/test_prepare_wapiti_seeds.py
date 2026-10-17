"""Exercise the seed CLI with sitemap URLs and actual lab input names."""
import subprocess
import sys
from pathlib import Path

import pytest

SCRIPT = Path(__file__).with_name("prepare_wapiti_seeds.py")


def run_cli(tmp_path, locations, base="http://localhost/VulnerableApp/"):
    sitemap = tmp_path / "sitemap.xml"
    sitemap.write_text(
        '<urlset xmlns="http://www.sitemaps.org/schemas/sitemap/0.9">'
        + "".join(f"<url><loc>{url}</loc></url>" for url in locations)
        + "</urlset>", encoding="utf-8",
    )
    output = tmp_path / "seeds.txt"
    result = subprocess.run(
        [sys.executable, str(SCRIPT), "--sitemap", str(sitemap),
         "--base-url", base, "--output", str(output)],
        capture_output=True, text=True,
    )
    return result, output


@pytest.mark.parametrize("family,query", [
    ("XSSWithHtmlTagInjection", "value=test"),
    ("XSSInImgTagAttribute", "src=%2FVulnerableApp%2Fimages%2FOWASP.png"),
    ("ErrorBasedSQLInjectionVulnerability", "id=1"),
    ("UnionBasedSQLInjectionVulnerability", "id=1"),
    ("BlindSQLInjectionVulnerability", "id=1"),
    ("PathTraversal", "fileName=UserInfo.json"),
    ("CommandInjection", "ipaddress=127.0.0.1"),
    ("SSRFVulnerability", "fileurl=http%3A%2F%2F169.254.169.254%2Flatest%2Fmeta-data%2F"),
])
def test_cli_adds_real_input_parameters_and_rebases_container_host(tmp_path, family, query):
    result, output = run_cli(tmp_path, [
        f"http://VulnerableApp-base:9090/VulnerableApp/{family}/LEVEL_1",
    ])
    assert result.returncode == 0, result.stderr
    assert output.read_text().splitlines() == [
        f"http://localhost/VulnerableApp/{family}/LEVEL_1?{query}",
    ]


def test_seeds_include_secure_levels_deduplicate_and_skip_unmapped_or_out_of_scope_paths(tmp_path):
    result, output = run_cli(tmp_path, [
        "http://localhost:9090/VulnerableApp/XSSWithHtmlTagInjection/LEVEL_1",
        "http://localhost:9090/VulnerableApp/XSSWithHtmlTagInjection/LEVEL_1",
        "http://localhost:9090/VulnerableApp/XSSWithHtmlTagInjection/LEVEL_4",
        "http://localhost:9090/VulnerableApp/UnrestrictedFileUpload/LEVEL_1",
        "http://localhost:9090/OtherApp/XSSWithHtmlTagInjection/LEVEL_1",
        "http://localhost:9090/VulnerableApp/XSSWithHtmlTagInjection/LEVEL_1/extra",
    ])
    assert result.returncode == 0, result.stderr
    assert output.read_text().splitlines() == [
        "http://localhost/VulnerableApp/XSSWithHtmlTagInjection/LEVEL_1?value=test",
        "http://localhost/VulnerableApp/XSSWithHtmlTagInjection/LEVEL_4?value=test",
    ]


def test_empty_seed_set_fails_without_overwriting_output(tmp_path):
    output = tmp_path / "seeds.txt"
    output.write_text("previous seeds\n")
    result, _ = run_cli(tmp_path, ["http://localhost/VulnerableApp/UnrestrictedFileUpload/LEVEL_1"])
    assert result.returncode != 0
    assert "No supported GET lab endpoints" in result.stderr
    assert output.read_text() == "previous seeds\n"


def test_malformed_sitemap_does_not_replace_previous_seeds(tmp_path):
    sitemap = tmp_path / "sitemap.xml"
    sitemap.write_text("not XML")
    output = tmp_path / "seeds.txt"
    output.write_text("previous seeds\n")
    result = subprocess.run(
        [sys.executable, str(SCRIPT), "--sitemap", str(sitemap),
         "--output", str(output)], capture_output=True, text=True,
    )
    assert result.returncode != 0
    assert "sitemap" in result.stderr.lower()
    assert output.read_text() == "previous seeds\n"
