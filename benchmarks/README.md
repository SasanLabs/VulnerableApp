# Scanner Benchmark Framework

This framework grades a security scanner against the ground truth that
VulnerableApp already ships. It supports two scan modes today:

- **DAST** — graded against the live `/scanner` endpoint (URL + vulnerability type).
- **SAST** — graded against `scanner/sast/expectedIssues.csv` (file path + line +
  CWE / vulnerability type).

You POST the scanner's findings as JSON to `/scanner/benchmark`; the framework
returns coverage, missed issues, and unmatched items (findings the scanner
reported that don't line up with any ground-truth row), and writes a JSON report to
`benchmarks/<tool>-results.json`.

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
    { "url": "/ErrorBasedSQLInjectionVulnerability/LEVEL_1", "cwe": "CWE-89" },
    { "url": "/PathTraversal/LEVEL_1", "wascId": "33" }
  ]
}
```

To discover the real ground-truth entries, hit
`GET http://<baseurl>/VulnerableApp/scanner` — every `UNSECURE` entry there
contributes one expected finding for each `vulnerabilityType` it carries. The
full sample at `benchmarks/samples/zap-findings-sample.json` includes one
deliberately invalid entry so a successful run produces a non-empty
`unmatchedItems` list for demonstration.

- `findings[].url` — relative path (`/SQLInjection/LEVEL_1`) or absolute URL
  (`http://localhost:9090/VulnerableApp/SQLInjection/LEVEL_1`); the comparator
  normalises both. Query strings, the `/VulnerableApp` context path, and
  trailing slashes are stripped.
- `findings[].type` — case-insensitive match against
  [`VulnerabilityType`](../src/main/java/org/sasanlabs/vulnerability/types/VulnerabilityType.java)
  enum names. See the canonical values below.
- `findings[].cwe` — optional. Numeric (`89`) or `CWE-`-prefixed (`CWE-89`);
  matches against `VulnerabilityType.getCweID()`.
- `findings[].wascId` — optional. Numeric; matches against
  `VulnerabilityType.getWascID()`.
- `findings[].method` — optional. HTTP method (`GET`, `POST`, …); matches
  against the ground-truth row's request method. See "DAST matching rules"
  below for the omitted-method semantics.

### DAST matching rules

A scanner finding matches a ground-truth row when the URL **and** HTTP method
agree AND **any one of** these axes agrees:

1. `type` matches the `VulnerabilityType` enum name (case-insensitive), OR
2. `cwe` matches the type's `cweID`, OR
3. `wascId` matches the type's `wascID`.

The method check is opt-in: if your scanner emits a `method` field, it is
matched strictly (`POST` vs ground-truth `GET` becomes unmatched). If
the field is omitted, the finding matches the URL's ground-truth row regardless
of method — leniency for scanners whose output format does not include the
method. This means existing payloads continue to work; emit `method` to enable
the stricter check.

A scanner that emits multiple axes (type + cwe, etc.) for the same alert is
counted once. A finding that matches no axis on any expected URL ends up in
`unmatchedItems`.

### Canonical DAST vulnerability type values

If you choose to match by `type`, the full set lives in
[`VulnerabilityType.java`](../src/main/java/org/sasanlabs/vulnerability/types/VulnerabilityType.java);
common values:

```text
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
`unmatchedItems` list.

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
`missedItems` / `unmatchedItems` entry vary by scan type (unused fields are
omitted from the JSON).

```json
{
  "tool": "ZAP",
  "coverage": 78.0,
  "totalExpected": 50,
  "detected": 39,
  "missed": 11,
  "unmatched": 12,
  "missedItems":         [ { "url": "/...", "type": "..." } ],
  "unmatchedItems":  [ { "url": "/...", "type": "..." } ]
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
- `unmatchedItems` — items the scanner reported that don't line up with any
  expected ground-truth row.

## Configuration

| Property | Default | Purpose |
|---|---|---|
| `benchmark.output.dir` | `benchmarks` | Directory the JSON report is written to. |
| `benchmark.dast.ground-truth.url` | `http://localhost:${server.port:9090}${server.servlet.context-path:/VulnerableApp}/scanner` | URL the DAST comparator fetches ground truth from. Override when running behind VulnerableApp-facade so coverage spans every backing app. |
| `benchmark.sast.ground-truth.path` | `scanner/sast/expectedIssues.csv` | CSV file the SAST comparator loads expected issues from. |

The default DAST URL is a self-call against the running app, which means
benchmarking works out of the box in standalone mode. In a facade-composed
deployment, point this at the facade's aggregated `/scanner/dast` endpoint so
scanner findings are graded against the union of every backing app's ground
truth.

## Where the report is written

By default, `benchmarks/<sanitised-tool>-results.json` relative to the working
directory of the running VulnerableApp process. The directory is configurable
via `benchmark.output.dir`. Filenames are lowercased and stripped of anything
outside `[a-z0-9_-]`. Re-running the endpoint for the same tool (regardless of
scan type) overwrites the previous report.

If the file write fails (disk full, permissions, etc.), the endpoint returns
**HTTP 500** with the same response body as a successful run, plus an extra
`persistenceError` string describing the failure. Callers still get the
computed metrics; the non-2xx status is the signal that the on-disk artifact
was *not* created.

## Limitations (v1)

- SAST `Number of Sources` is not used for scoring; full credit on first match.
