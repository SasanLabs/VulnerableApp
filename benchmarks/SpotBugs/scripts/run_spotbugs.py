#!/usr/bin/env python3
"""
run_spotbugs.py
===============
Runs SpotBugs with the FindSecBugs plugin via Gradle initialization script
(spotbugs.init.gradle) without modifying VulnerableApp's core build.gradle.

Usage:
    python benchmarks/SpotBugs/scripts/run_spotbugs.py

Outputs:
    benchmarks/SpotBugs/spotbugs-raw-report.xml
"""

import os
import subprocess
import sys
from pathlib import Path


def find_repo_root() -> Path:
    current = Path(__file__).resolve().parent
    for parent in [current] + list(current.parents):
        if (parent / "gradlew").is_file() and (parent / "src").is_dir():
            return parent
    return Path.cwd().resolve()


def run_spotbugs(repo_root: Path) -> None:
    gradle_cmd = str(repo_root / ("gradlew.bat" if os.name == "nt" else "gradlew"))
    init_script = repo_root / "benchmarks" / "SpotBugs" / "scripts" / "spotbugs.init.gradle"
    output_xml = repo_root / "benchmarks" / "SpotBugs" / "spotbugs-raw-report.xml"

    if not init_script.is_file():
        print(f"ERROR: Init script not found at {init_script}", file=sys.stderr)
        sys.exit(1)

    cmd = [
        gradle_cmd,
        "--refresh-dependencies",
        "--init-script",
        str(init_script),
        "spotbugsMain",
    ]

    print(f"Executing: {' '.join(cmd)}")
    try:
        res = subprocess.run(cmd, cwd=repo_root, timeout=600)
        if res.returncode != 0:
            print(f"ERROR: Gradle SpotBugs task failed with exit code {res.returncode}", file=sys.stderr)
            sys.exit(res.returncode)
    except subprocess.TimeoutExpired:
        print("ERROR: Timed out running SpotBugs scan via Gradle", file=sys.stderr)
        sys.exit(1)
    except subprocess.CalledProcessError as e:
        print(f"ERROR: Gradle SpotBugs execution failed: {e}", file=sys.stderr)
        sys.exit(e.returncode)

    if not output_xml.is_file() or output_xml.stat().st_size == 0:
        print("ERROR: SpotBugs failed to produce an XML report at expected path", file=sys.stderr)
        sys.exit(1)

    print(f"Scan complete. Raw XML report saved to: {output_xml}")


def main() -> None:
    repo_root = find_repo_root()
    run_spotbugs(repo_root)


if __name__ == "__main__":
    main()
