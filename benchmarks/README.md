# Scanner Benchmark Framework

This framework grades a security scanner against the ground truth that
VulnerableApp already ships. It supports two scan modes today:

- **DAST** — graded against the live `/scanner` endpoint (URL + vulnerability type).
- **SAST** — graded against `scanner/sast/expectedIssues.csv` (file path + line +
  CWE / vulnerability type).

You POST the scanner's findings as JSON to `/scanner/benchmark`; the framework
returns coverage, missed issues, and false positives, and writes a JSON report
to `benchmarks/<tool>-results.json`.

> Running the scanner itself is **out of scope**. You are responsible for
> running ZAP / Burp / Semgrep / your tool against VulnerableApp (or its source
> tree) and converting its output into the input format below.

## Choosing a scan type

The optional `scanType` field on the request body selects the strategy. When
omitted, it defaults to `DAST` so existing payloads keep working.

| `scanType`        | Ground truth                                | Per-finding fields                        |
|-------------------|---------------------------------------------|-------------------------------------------|
| `DAST` (default)  | live `/scanner` endpoint                    | `url`, `type`                             |
| `SAST`            | `scanner/sast/expectedIssues.csv`           | `filePath`, `line`, plus `cwe` and/or `type` |

## DAST input format

```json
{
  "tool": "ZAP",
  "scanType": "DAST",
  "findings": [
    { "url": "/BlindSQLInjectionVulnerability/LEVEL_1", "type": "BLIND_SQL_INJECTION" },
    { "url": "/XSSInImgTagAttribute/LEVEL_1",          "type": "REFLECTED_XSS" }
  ]
}
```

To discover the real (URL, type) pairs to send, hit
`GET http://<baseurl>/VulnerableApp/scanner` — that's the ground truth you're
being graded against. The full sample at `benchmarks/samples/zap-findings-sample.json`
includes one deliberately invalid entry so a successful run produces a non-empty
`falsePositiveItems` list for demonstration.

- `findings[].url` — relative path (`/SQLInjection/LEVEL_1`) or absolute URL
  (`http://localhost:9090/VulnerableApp/SQLInjection/LEVEL_1`); the comparator
  normalises both. Query strings, the `/VulnerableApp` context path, and
  trailing slashes are stripped.
- `findings[].type` — case-insensitive match against
  [`VulnerabilityType`](../src/main/java/org/sasanlabs/vulnerability/types/VulnerabilityType.java)
  enum names. See the canonical values below.

### Canonical DAST vulnerability type values

A scanner reporting `"SQL_INJECTION"` will not match — use a specific sub-type.
The full set lives in
[`VulnerabilityType.java`](../src/main/java/org/sasanlabs/vulnerability/types/VulnerabilityType.java);
common values:

```
BLIND_SQL_INJECTION, ERROR_BASED_SQL_INJECTION, UNION_BASED_SQL_INJECTION,
REFLECTED_XSS, PERSISTENT_XSS, DOM_BASED_XSS,
COMMAND_INJECTION, PATH_TRAVERSAL, HEADER_INJECTION, XXE,
OPEN_REDIRECT_3XX_STATUS_CODE, SIMPLE_SSRF, BLIND_SSRF,
LDAP_INJECTION, INSECURE_DIRECT_OBJECT_REFERENCE,
UNRESTRICTED_FILE_UPLOAD, UNCONTROLLED_RESOURCE_CONSUMPTION,
WEAK_CRYPTOGRAPHIC_HASH, INSECURE_CRYPTOGRAPHIC_STORAGE,
USE_OF_BROKEN_CRYPTOGRAPHIC_ALGORITHM, CLICKJACKING,
PLAINTEXT_PASSWORD_STORAGE, WEAK_PASSWORD_HASHING, USERNAME_ENUMERATION,
WEB_CACHE_POISONING
```

## SAST input format

```json
{
  "tool": "Semgrep",
  "scanType": "SAST",
  "findings": [
    {
      "filePath": "src/main/java/org/sasanlabs/service/vulnerability/sqlInjection/BlindSQLInjectionVulnerability.java",
      "line": 56,
      "cwe": "CWE-89",
      "type": "SQL Injection"
    }
  ]
}
```

The full sample at `benchmarks/samples/semgrep-sast-sample.json` includes one
deliberately invalid entry so a successful run produces a non-empty
`falsePositiveItems` list.

Ground truth is loaded from `scanner/sast/expectedIssues.csv`. The path is
configurable via the `benchmark.sast.ground-truth.path` property (default:
`scanner/sast/expectedIssues.csv` relative to the working directory).

### SAST matching rules

A finding matches an expected issue when:

1. The normalised **`filePath`** matches, AND
2. The **`line`** matches exactly, AND
3. Either **`cwe`** matches the CSV's `CWE` column **or** **`type`** matches
   the `Vulnerability Type` column (case-insensitively).

A scanner can emit either CWE, type, or both — whichever pair
(`file + line + CWE` *or* `file + line + type`) hits the ground truth wins.

- **Path normalisation:** backslashes → forward slashes; leading `./` stripped;
  whitespace trimmed. Scanner authors should emit project-relative paths;
  absolute paths or unexpected prefixes will not match.
- **CWE comparison:** upper-cased + trimmed (e.g. `cwe-89` and `CWE-89` both
  match).
- **Type comparison:** lower-cased + trimmed (e.g. `"SQL Injection"` and
  `"sql injection"` both match).
- **Duplicates:** a scanner that emits the same `(filePath, line, CWE)` twice
  gets credit once.
- **`Number of Sources` column:** present in the CSV for human reference; **not
  used for scoring** — full credit on first match.

## Calling the endpoint

Start VulnerableApp, then for either mode:

```bash
# DAST
curl -X POST http://localhost:9090/VulnerableApp/scanner/benchmark \
  -H "Content-Type: application/json" \
  -d @benchmarks/samples/zap-findings-sample.json

# SAST
curl -X POST http://localhost:9090/VulnerableApp/scanner/benchmark \
  -H "Content-Type: application/json" \
  -d @benchmarks/samples/semgrep-sast-sample.json
```

The HTTP response contains the same JSON that gets persisted to disk.

## Output format

The output schema is the same for DAST and SAST. The fields inside each
`missedItems` / `falsePositiveItems` entry vary by scan type (unused fields are
omitted from the JSON).

```json
{
  "tool": "ZAP",
  "coverage": 78.0,
  "totalExpected": 50,
  "detected": 39,
  "missed": 11,
  "falsePositives": 12,
  "missedItems":         [ { "url": "/...", "type": "..." } ],
  "falsePositiveItems":  [ { "url": "/...", "type": "..." } ]
}
```

For SAST runs, items look like:

```json
{ "filePath": "src/main/java/.../Foo.java", "line": 56, "type": "SQL Injection", "cwe": "CWE-89" }
```

- `coverage` — `detected / totalExpected * 100`. Reported as `0.0` when ground
  truth is empty.
- `totalExpected` — number of unique ground-truth items. For DAST, count of
  `(URL, vulnerabilityType)` pairs across all `UNSECURE` ground-truth entries
  (SECURE entries are intentionally clean and don't count). For SAST, count of
  rows in the CSV.
- `missedItems` — expected items the scanner did not report.
- `falsePositiveItems` — items the scanner reported that are not in the
  expected set. For DAST this includes findings against `SECURE` URLs.

## Where the report is written

By default, `benchmarks/<sanitised-tool>-results.json` relative to the working
directory of the running VulnerableApp process. The directory is configurable
via `benchmark.output.dir`. Filenames are lowercased and stripped of anything
outside `[a-z0-9_-]`. Re-running the endpoint for the same tool (regardless of
scan type) overwrites the previous report.

## Limitations (v1)

- **LLM-based scanners are not yet supported.** There is no equivalent
  ground-truth source for LLM scanners; we'd rather see real LLM scanner output
  formats before committing to a matcher. Tracked as a follow-up.
- DAST type matching is by enum name. CWE-based matching for DAST is a likely
  follow-up.
- HTTP method (DAST) is shown in the ground truth but is not part of the match
  key — same URL + type with a different method still counts as detected.
- SAST `Number of Sources` is not used for scoring; full credit on first match.
