import json
 
def load_file(path, default):
    try:
        with open(path) as f:
            return json.load(f)
    except:
        return default
 
 
semgrep = load_file("semgrep.json", {})
gitleaks_results = sarif["runs"][0].get("results", []) 
 
semgrep_count = len(semgrep.get("results", []))
gitleaks_count = len(gitleaks)
 
 
report = f"""
# Security Scan Report
 
## Semgrep Issues: {semgrep_count}
## Gitleaks Issues: {gitleaks_count}
 
---
 
## Summary
- Static code analysis completed using Semgrep
- Secret detection completed using Gitleaks

 
---
 
## Semgrep Findings
"""
 
# Add some semgrep details
for r in semgrep.get("results", [])[:10]:
    report += f"- {r.get('check_id')} in {r.get('path')}:{r.get('start', {}).get('line')}\n"
 
report += "\n---\n\n## Gitleaks Findings\n"
 
# Add some gitleaks details
for g in gitleaks[:10]:
    report += f"- {g.get('Description')} in {g.get('File')}:{g.get('StartLine')}\n"
 
 
with open("report.md", "w") as f:
    f.write(report)
