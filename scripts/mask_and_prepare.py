import json, re, os
from pathlib import Path

# ---------------------------
# REDACTION PATTERNS
# ---------------------------
PRIVATE_KEY_BLOCK = re.compile(r"-----BEGIN [^-]+-----.*?-----END [^-]+-----", re.DOTALL)
JWT = re.compile(r"\b[A-Za-z0-9_-]+\.[A-Za-z0-9_-]+\.[A-Za-z0-9_-]+\b")
AWS_KEY = re.compile(r"\bAKIA[0-9A-Z]{16}\b")
EMAIL = re.compile(r"\b[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}\b")
GENERIC_TOKEN = re.compile(r"\b[a-zA-Z0-9_/\-+=]{30,}\b")

SECRET_KV = re.compile(
    r"(?i)(password|passwd|pwd|secret|token|apikey|api_key|access_key)\s*[:=]\s*[^\s\"']+"
)

BLOCKED_EXTENSIONS = (".pem", ".p12", ".jks", ".keystore", ".env")

# ---------------------------
def redact(text: str):
    stats = {}

    def sub(pattern, label, s):
        s2, n = pattern.subn(label, s)
        if n:
            stats[label] = stats.get(label, 0) + n
        return s2

    text = sub(PRIVATE_KEY_BLOCK, "[REDACTED_PRIVATE_KEY]", text)
    text = sub(SECRET_KV, "[REDACTED_SECRET_VALUE]", text)
    text = sub(AWS_KEY, "[REDACTED_AWS_KEY]", text)
    text = sub(JWT, "[REDACTED_JWT]", text)
    text = sub(EMAIL, "[REDACTED_EMAIL]", text)
    text = sub(GENERIC_TOKEN, "[REDACTED_TOKEN]", text)

    return text, stats

def should_block_file(path: str) -> bool:
    return path.lower().endswith(BLOCKED_EXTENSIONS)

def get_context(path: str, line: int, radius: int = 10):
    p = Path(path)
    if not p.exists():
        return "[File not found]"
    lines = p.read_text(errors="ignore").splitlines()
    start = max(0, line - 1 - radius)
    end = min(len(lines), line - 1 + radius + 1)
    return "\n".join(f"{i+1:>4}: {lines[i]}" for i in range(start, end))

# ---------------------------
def main():
    semgrep_file = Path("semgrep-results.json")
    if not semgrep_file.exists():
        raise SystemExit("❌ semgrep-results.json not found")

    results = json.loads(semgrep_file.read_text()).get("results", [])
    output_dir = Path("llm_payloads")
    output_dir.mkdir(exist_ok=True)

    summary = []

    for i, r in enumerate(results[:5], start=1):  # limit for PoC
        path = r["path"]
        line = r["start"]["line"]

        if should_block_file(path):
            summary.append({"file": path, "status": "BLOCKED"})
            continue

        raw = get_context(path, line)
        masked, stats = redact(raw)

        payload = {
            "severity": r["extra"]["severity"],
            "rule": r["check_id"],
            "location": f"{path}:{line}",
            "message": r["extra"]["message"],
            "masked_code": masked,
            "redactions": stats
        }

        (output_dir / f"payload_{i}.json").write_text(
            json.dumps(payload, indent=2)
        )

        summary.append({"file": path, "status": "MASKED", "redactions": stats})

    Path("masking-summary.json").write_text(json.dumps(summary, indent=2))
    print("✅ Masked payloads created in ./llm_payloads")

if __name__ == "__main__":
    main()
