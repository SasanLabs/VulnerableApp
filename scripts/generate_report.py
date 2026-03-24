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

    
report.write("## Semgrep Findings\n\n")

if semgrep_findings:
    for finding in semgrep_findings:
        issue_type = finding.get("check_id", "UNKNOWN").upper()
        file_path = finding.get("path", "unknown-file")
        line = finding.get("start", {}).get("line", "N/A")
        risk = finding.get("extra", {}).get("message", "No description available.")

        report.write(f"Issue Type : {issue_type}\n")
        report.write(f"Location   : {file_path} (line {line})\n")
        report.write(f"Risk       : {risk}\n\n")
else:
    report.write("No security issues detected by Semgrep.\n\n")


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
