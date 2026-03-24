import json
from pathlib import Path

# Load Semgrep results
semgrep_findings = []

if Path("semgrep.json").exists():
    with open("semgrep.json", "r") as f:
        semgrep_data = json.load(f)
        semgrep_findings = semgrep_data.get("results", [])

semgrep_count = len(semgrep_findings)

# Load Gitleaks SARIF
# IMPORTANT: gitleaks-action ALWAYS writes to results.sarif
gitleaks_findings = []

if Path("results.sarif").exists():
    with open("results.sarif", "r") as f:
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
    report.write("- Static code analysis completed using **Semgrep**\n")
    report.write("- Secret detection completed using **Gitleaks**\n\n")

    report.write("---\n\n")

    # Semgrep Findings 
    report.write("## Semgrep Findings\n\n")

    if semgrep_findings:
        for finding in semgrep_findings:
            issue_type = (
                finding.get("check_id", "UNKNOWN")
                .split(".")[-1]
                .replace("-", " ")
                .upper()
            )
            file_path = finding.get("path", "unknown-file")
            line = finding.get("start", {}).get("line", "N/A")
            risk = finding.get("extra", {}).get(
                "message", "No risk description available."
            )

            report.write(f"Issue Type : {issue_type}\n")
            report.write(f"Location   : {file_path} (line {line})\n")
            report.write(f"Risk       : {risk}\n\n")
    else:
        report.write("No security issues detected by Semgrep.\n\n")

    report.write("---\n\n")

    # Gitleaks Findings
    report.write("## Gitleaks Findings\n\n")

    if gitleaks_findings:
        for finding in gitleaks_findings:
            location = finding["locations"][0]["physicalLocation"]
            file_path = location["artifactLocation"]["uri"]
            line = location["region"]["startLine"]
            rule = finding.get("ruleId", "UNKNOWN_SECRET")

            report.write(f"Issue Type : {rule}\n")
            report.write(f"Location   : {file_path} (line {line})\n")
            report.write(
                "Risk       : Hardcoded secrets may allow unauthorized access "
                "to systems, services, or data if exposed.\n\n"
            )
    else:
        report.write("No secrets detected by Gitleaks.\n")
