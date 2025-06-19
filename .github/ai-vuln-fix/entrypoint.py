import os
import subprocess
import git
import json
from github import Github
import openai

REPO_PATH = os.getcwd()

# 1. Generate SBOM
subprocess.run(["syft", ".", "-o", "json", "-q", "-f", "sbom.json"])

# 2. Scan with Grype
result = subprocess.run(["grype", "sbom:sbom.json", "-o", "json"], capture_output=True, text=True)
scan_output = json.loads(result.stdout)

matches = scan_output.get("matches", [])
if not matches:
    print("✅ No vulnerabilities found.")
    exit(0)

# 3. For each vuln, get context & fix
openai.api_key = os.getenv("OPENAI_API_KEY")
repo = git.Repo(REPO_PATH)

changes_made = False

for m in matches:
    vuln_id = m["vulnerability"]["id"]
    pkg_name = m["artifact"]["name"]
    print(f"🔍 Found {vuln_id} in {pkg_name}")

    # Find usages (simple grep)
    files = subprocess.check_output(["grep", "-rl", pkg_name, "."]).decode().splitlines()

    for f in files:
        with open(f) as file:
            code = file.read()
        prompt = f"""
You are a secure code assistant. Vulnerability {vuln_id} in {pkg_name} detected.
Here is the code: