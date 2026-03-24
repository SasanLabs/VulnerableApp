import json
from pathlib import Path

# Load Semgrep results
semgrep_count = 0
semgrep_findings = []

if Path("semgrep.json").exists():
    with open("semgrep.json") as f:
        semgrep_data = json.load(f)
        semgrep_findings = semgrep_data.get("results", [])
        semgrep_count = len(semgrep_findings)

# Load Gitleaks SARIF
gitleaks_count = 0
gitleaks_findings = []

if Path("gitleaks-results.sarif").exists():
    with open("gitleaks-results.sarif") as f:
        sarif = json.load(f)  
        runs = sarif.get("runs", [])
        if runs:
            gitleaks_findings = runs[0].get("results", [])
            gitleaks_count = len(gitleaks_findings)

# Generate Markdown report
with open("report.md", "w") as report:
    report.write("# Security Scan Report\n\n")
    report.write(f"## Semgrep Issues: {semgrep_count}\n")
    report.write(f"## Gitleaks Issues: {gitleaks_count}\n\n")
    report.write("---\n\n")

    report.write("## Summary\n")
    report.write("- Static code analysis completed using Semgrep\n")
    report.write("- Secret detection completed using Gitleaks\n\n")
    report.write("---\n\n")

    report.write("## Semgrep Findings\n")
    for finding in semgrep_findings[:20]:  # limit for readability
        rule = finding.get("check_id", "unknown-rule")
        path = finding["path"]
        line = finding["start"]["line"]
        report.write(f"- {rule} in {path}:{line}\n")

    report.write("\n---\n\n")
    report.write("## Gitleaks Findings\n")

    if gitleaks_findings:
        for r in gitleaks_findings:
            location = r["locations"][0]["physicalLocation"]
            file = location["artifactLocation"]["uri"]
            line = location["region"]["startLine"]
            rule = r["ruleId"]
            report.write(f"- {rule} in {file}:{line}\n")
    else:
        report.write("No secrets detected by Gitleaks.\n")
