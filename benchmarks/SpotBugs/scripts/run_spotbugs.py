#!/usr/bin/env python3
"""
run_spotbugs.py
===============
Runs SpotBugs with FindSecBugs plugin as a standalone CLI tool against
VulnerableApp's compiled bytecode without modifying the project's build.gradle.

Usage:
    python benchmarks/SpotBugs/scripts/run_spotbugs.py

Outputs:
    benchmarks/SpotBugs/spotbugs-raw-report.xml
"""

import os
import shutil
import subprocess
import sys
import tarfile
import urllib.request
from pathlib import Path

SPOTBUGS_VERSION = "4.8.6"
FINDSECBUGS_VERSION = "1.13.0"

SPOTBUGS_URL = (
    f"https://github.com/spotbugs/spotbugs/releases/download/{SPOTBUGS_VERSION}/"
    f"spotbugs-{SPOTBUGS_VERSION}.tgz"
)
FINDSECBUGS_URL = (
    f"https://repo1.maven.org/maven2/com/h3xstream/findsecbugs/findsecbugs-plugin/"
    f"{FINDSECBUGS_VERSION}/findsecbugs-plugin-{FINDSECBUGS_VERSION}.jar"
)


def find_repo_root() -> Path:
    current = Path(__file__).resolve().parent
    for parent in [current] + list(current.parents):
        if (parent / "gradlew").is_file() and (parent / "src").is_dir():
            return parent
    return Path.cwd().resolve()


def ensure_spotbugs(tools_dir: Path) -> tuple[Path, Path]:
    tools_dir.mkdir(parents=True, exist_ok=True)
    spotbugs_home = tools_dir / f"spotbugs-{SPOTBUGS_VERSION}"
    spotbugs_bin = spotbugs_home / "bin" / ("spotbugs.bat" if os.name == "nt" else "spotbugs")
    plugin_jar = tools_dir / f"findsecbugs-plugin-{FINDSECBUGS_VERSION}.jar"

    if not spotbugs_bin.is_file():
        tgz_path = tools_dir / f"spotbugs-{SPOTBUGS_VERSION}.tgz"
        print(f"Downloading SpotBugs {SPOTBUGS_VERSION} from {SPOTBUGS_URL}...")
        urllib.request.urlretrieve(SPOTBUGS_URL, tgz_path)
        print("Extracting SpotBugs...")
        with tarfile.open(tgz_path, "r:gz") as tar:
            tar.extractall(path=tools_dir)
        if tgz_path.exists():
            tgz_path.unlink()
        if not os.name == "nt":
            spotbugs_bin.chmod(0o755)

    if not plugin_jar.is_file():
        print(f"Downloading FindSecBugs {FINDSECBUGS_VERSION} from {FINDSECBUGS_URL}...")
        urllib.request.urlretrieve(FINDSECBUGS_URL, plugin_jar)

    return spotbugs_bin, plugin_jar


def compile_classes(repo_root: Path) -> None:
    gradle_cmd = str(repo_root / ("gradlew.bat" if os.name == "nt" else "gradlew"))
    print("Compiling VulnerableApp Java classes (./gradlew compileJava)...")
    res = subprocess.run([gradle_cmd, "compileJava"], cwd=repo_root)
    if res.returncode != 0:
        print("ERROR: ./gradlew compileJava failed", file=sys.stderr)
        sys.exit(res.returncode)


def extract_classpath(repo_root: Path, tools_dir: Path) -> Path:
    gradle_cmd = str(repo_root / ("gradlew.bat" if os.name == "nt" else "gradlew"))
    init_script = tools_dir / "print_classpath_init.gradle"
    init_script.write_text(
        'allprojects { tasks.register("printClasspath") { doLast { println sourceSets.main.compileClasspath.asPath } } }\n',
        encoding="utf-8",
    )

    try:
        res = subprocess.run(
            [gradle_cmd, "-q", "-I", str(init_script), "printClasspath"],
            cwd=repo_root,
            capture_output=True,
            text=True,
            check=True,
        )
        raw_cp = res.stdout.strip()
    finally:
        if init_script.exists():
            init_script.unlink()

    cp_file = tools_dir / "auxcp.txt"
    entries = [entry.strip() for entry in raw_cp.split(os.pathsep) if entry.strip()]
    cp_file.write_text("\n".join(entries), encoding="utf-8")
    return cp_file


def run_scan(
    spotbugs_bin: Path,
    plugin_jar: Path,
    cp_file: Path,
    classes_dir: Path,
    output_xml: Path,
    repo_root: Path,
) -> None:
    output_xml.parent.mkdir(parents=True, exist_ok=True)
    cmd = [
        str(spotbugs_bin),
        "-textui",
        "-xml:withMessages",
        "-output",
        str(output_xml),
        "-pluginList",
        str(plugin_jar),
        "-effort:max",
        "-low",
        "-auxclasspathFromFile",
        str(cp_file),
        str(classes_dir),
    ]

    print("Executing standalone SpotBugs scan...")
    # FindBugs / SpotBugs returns non-zero when warnings are found; do not raise on exit code
    res = subprocess.run(cmd, cwd=repo_root)

    if not output_xml.is_file() or output_xml.stat().st_size == 0:
        print("ERROR: SpotBugs failed to produce an XML report", file=sys.stderr)
        sys.exit(1)

    print(f"Scan complete. Raw XML report saved to: {output_xml}")


def main() -> None:
    repo_root = find_repo_root()
    tools_dir = repo_root / ".spotbugs"
    classes_dir = repo_root / "build" / "classes" / "java" / "main"
    output_xml = repo_root / "benchmarks" / "SpotBugs" / "spotbugs-raw-report.xml"

    compile_classes(repo_root)
    spotbugs_bin, plugin_jar = ensure_spotbugs(tools_dir)
    cp_file = extract_classpath(repo_root, tools_dir)
    run_scan(spotbugs_bin, plugin_jar, cp_file, classes_dir, output_xml, repo_root)


if __name__ == "__main__":
    main()
