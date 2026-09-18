"""
Guards for ZAP automation plans used by GitHub Actions.
"""

from pathlib import Path
import re


ZAP_DIR = Path(__file__).resolve().parents[1]


def _automation_value(plan_name: str, job_type: str, key: str) -> int:
    plan = (ZAP_DIR / plan_name).read_text()
    job_start = plan.index(f"- type: {job_type}")
    next_job = plan.find("\n  - type:", job_start + 1)
    job = plan[job_start:] if next_job == -1 else plan[job_start:next_job]
    match = re.search(rf"^\s+{re.escape(key)}:\s+(\d+)\s*$", job, re.MULTILINE)
    assert match is not None
    return int(match.group(1))


def test_modern_ajax_spider_browser_count_is_ci_safe():
    assert _automation_value(
        "zap-automation-modern.yaml", "spiderAjax", "numberOfBrowsers"
    ) <= 2
