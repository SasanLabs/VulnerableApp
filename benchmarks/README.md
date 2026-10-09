# Scanner Benchmark Framework

This framework grades a security scanner against the ground truth that
VulnerableApp already ships. It supports two scan modes today:

- **DAST** — graded against the live `/scanner/dast` endpoint (URL + vulnerability type).
- **SAST** — graded against the expected-issues ground truth served by `/scanner/sast`
  (file path + line + CWE / vulnerability type).

You POST the scanner's findings as JSON to `/scanner/benchmark`; the framework
returns coverage, missed issues, and unmatched items (findings the scanner
reported that don't line up with any ground-truth row), and writes a JSON report to
`benchmarks/<tool>-results.json`.

> Running the scanner itself is **out of scope**. You are responsible for
> running ZAP / Burp / Semgrep / your tool against VulnerableApp (or its source
> tree) and converting its output into the input format below.

---
 
## ZAP By Checkmarx
 
ZAP is benchmarked against VulnerableApp in two modes — **Modern UI** (React
frontend via VulnerableApp-facade) and **Legacy UI** (JSP backend directly).
 
### Latest results
 
| UI | Results file |
|---|---|
| Modern | [`benchmarks/ZAP/zap-results.json`](ZAP/zap-results.json) |
| Legacy | [`benchmarks/ZAP/zap-results-legacy.json`](ZAP/zap-results-legacy.json) |
 
Results are auto-updated everyday and on every manual workflow run.
 
### Running the benchmark
 
The full pipeline (start VulnerableApp → run ZAP → convert → benchmark → commit
results) is automated via GitHub Actions:
 
1. Go to **Actions** tab → **ZAP Benchmark**
2. Click **Run workflow**
3. Select `legacy`, `modern`, or `both`
4. Results are committed automatically to the files above
See [`.github/workflows/zap-benchmark.yml`](../.github/workflows/zap-benchmark.yml)
for the full workflow definition.
 
### Conversion script
 
ZAP's raw JSON report must be converted to the benchmark input format before
posting to the endpoint:
 
```bash
python3 benchmarks/ZAP/scripts/convert_zap_to_benchmark.py \
    --input  benchmarks/ZAP/zap-raw-report.json \
    --output benchmarks/ZAP/zap-benchmark-input.json
```
 
The script maps each ZAP alert instance to a `Finding` using CWE and WASC IDs
natively — no manual alert-name mapping needed.
 
A sample benchmark output is at
[`benchmarks/ZAP/zap-results.json`](ZAP/zap-results.json).
 
---

## Semgrep

Semgrep is benchmarked in SAST mode against the VulnerableApp source tree.

### Latest results

| Ruleset | Results file |
|---|---|
| `p/default` | [`benchmarks/Semgrep/semgrep-results.json`](Semgrep/semgrep-results.json) |

Results are auto-updated on `master` every day and on every manual workflow
run. Each run installs the latest Semgrep release and prints its version in the
run log.

### Running the benchmark

The full pipeline (build the VulnerableApp image from the checkout, start the
Docker stack like the ZAP benchmark, run Semgrep, convert, benchmark, commit
results) is automated via GitHub Actions:

1. Open the **Actions** tab and pick **Semgrep Benchmark**
2. Click **Run workflow**
3. On `master`, results are committed automatically to the file above, along
   with the converted findings in `benchmarks/Semgrep/findings/semgrep-findings.json`.
   On other branches, nothing is committed; download the
   `semgrep-benchmark-artifacts` artifact of the run instead.

See [`.github/workflows/semgrep-benchmark.yml`](../.github/workflows/semgrep-benchmark.yml)
for the full workflow definition.

To run it locally:

1. Run the scan from the repository root, so that Semgrep reports
   project-relative paths. That is what the SAST matcher compares.

   ```bash
   semgrep scan --config p/default --metrics=off --timeout 30 --json \
       --output benchmarks/Semgrep/semgrep-raw-report.json src/main/java
   ```

2. Convert the report with the script described below.
3. Start VulnerableApp from the same checkout (`./gradlew bootRun`, port 9090),
   so that the ground truth it serves matches the sources that were scanned.
4. Post the findings and save the response:

   ```bash
   curl -s -X POST http://localhost:9090/VulnerableApp/scanner/benchmark \
       -H "Content-Type: application/json" \
       -d @benchmarks/Semgrep/findings/semgrep-findings.json \
       -o benchmarks/Semgrep/semgrep-results.json
   ```

### Conversion script

Semgrep's JSON report must be converted to the benchmark input format before
posting to the endpoint:

```bash
python3 benchmarks/Semgrep/scripts/convert_semgrep_to_benchmark.py \
    --input  benchmarks/Semgrep/semgrep-raw-report.json \
    --output benchmarks/Semgrep/findings/semgrep-findings.json
```

The script emits one finding per CWE of each Semgrep result: the path, the
start line, the CWE ID, and the rule's `vulnerability_class` as the type. When a
rule lists several CWEs, only the first finding carries the type, because the
SAST matcher skips a finding that repeats the file, line and type of an earlier
one. A result with neither a CWE nor a `vulnerability_class` is skipped with a
warning, because the matcher cannot score it. No manual rule mapping is needed.

---

## w3af

w3af is benchmarked in **DAST** mode. Its findings are graded on the URL that
carried the payload plus the CWE and WASC ids w3af attaches to the plugin that
reported them.

### Running the scan

Running w3af is not automated yet -- there is no workflow equivalent to the ZAP
one above, so you run the scan yourself and keep the JSON report:

```bash
./w3af_console
w3af>>> plugins
w3af/plugins>>> discovery web_spider
w3af/plugins>>> audit xss,sqli,lfi,os_commanding,ssrf
w3af/plugins>>> output json_file
w3af/plugins>>> output config json_file
w3af/plugins/output/config:json_file>>> set output_file benchmarks/w3af/w3af-raw.json
w3af/plugins/output/config:json_file>>> back
w3af/plugins>>> back
w3af>>> target
w3af/config:target>>> set target http://localhost/VulnerableApp/
w3af/config:target>>> back
w3af>>> start
```

`json_file` is the output plugin this converter reads; the older `console` and
`text_file` plugins do not carry the CWE and WASC ids. Pick the audit plugins
that cover the classes you want graded -- the benchmark grades what the scan
reports and counts the rest of the ground truth as missed, so a narrow plugin
set produces a low coverage number rather than an error.

### Conversion script

```bash
python3 benchmarks/w3af/scripts/convert_w3af_to_benchmark.py \
    --input  benchmarks/w3af/w3af-raw.json \
    --output benchmarks/w3af/w3af-benchmark-input.json
```

The script reads the `items` array, takes `URL` as the finding URL, `HTTP
method` as the method, and the first non-zero entry of `CWE IDs` / `WASC IDs` as
the ids. Plugin names are deliberately **not** mapped to `VulnerabilityType`
values, so matching runs on the CWE and WASC axes; a name table would be one
more thing to keep in sync as plugins are renamed.

Then submit it like any other DAST payload:

```bash
curl -X POST http://localhost/VulnerableApp/scanner/benchmark \
  -H "Content-Type: application/json" \
  -d @benchmarks/w3af/w3af-benchmark-input.json
```

A converted sample is at
[`benchmarks/w3af/findings/w3af-findings.json`](w3af/findings/w3af-findings.json),
produced by that script from
[`benchmarks/w3af/w3af-raw.json`](w3af/w3af-raw.json), which is a trimmed run
rather than a full report.

---
## Choosing a scan type

The optional `scanType` field on the request body selects the strategy. When
omitted, it defaults to `DAST` so existing payloads keep working.

| `scanType`        | Ground truth                                | Per-finding fields                        |
|-------------------|---------------------------------------------|-------------------------------------------|
| `DAST` (default)  | live `/scanner/dast` endpoint               | `url`, `type`                             |
| `SAST`            | `/scanner/sast` (from `expectedIssues.csv`) | `filePath`, `line`, plus `cwe` and/or `type` |

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

A scanner finding matches a ground-truth row when **all three** of these hold:

1. The URL agrees, AND
2. The HTTP method check passes (opt-in — see below for the omitted-method
   semantics), AND
3. **Any one of** these axes agrees:
   - `type` matches the `VulnerabilityType` enum name (case-insensitive), OR
   - `cwe` matches the type's `cweID`, OR
   - `wascId` matches the type's `wascID`.

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
      "line": 93,
      "cwe": "CWE-89",
      "type": "SQL Injection"
    }
  ]
}
```

The full sample at `benchmarks/semgrep-sast-sample.json` includes one
deliberately invalid entry so a successful run produces a non-empty
`unmatchedItems` list.

Ground truth is loaded from `src/main/resources/scanner/sast/expectedIssues.csv`,
which ships inside the jar, so it resolves regardless of the working directory the
app was started from. The same rows are served as JSON by `GET /scanner/sast`. The
source is configurable via the `benchmark.sast.ground-truth.path` property (default:
`classpath:scanner/sast/expectedIssues.csv`); a value without the `classpath:` prefix
is read from the filesystem, relative to the working directory or absolute.

### What a ground-truth row points at

Each row of `expectedIssues.csv` gives the line of the vulnerable statement,
which is the line a SAST tool is expected to report:

- Where user input reaches a dangerous call (a query, a process, a file or
  network access, a response), it is the line on which the input reaches that
  call. When the call is split over several lines, that is the line of the
  argument that carries the input, not the line where the call starts.
- Where there is no such call (a weak hash, a missing check, a token accepted
  without verification), it is the line of the weak call or the faulty check.
  That line can be in a service class such as `AuthLoginService` or
  `JWTValidator` when the flawed code lives there.

When several vulnerable levels go through the same line, for example a helper
shared by all levels of a class, there is one row for that line and
`Number of Sources` holds the number of levels that expose the issue through
it. Levels marked `Variant.SECURE` have no rows.

A row never points at an annotation, a comment, a blank line or a brace.
`ExpectedIssuesAlignmentTest` fails the build when one does, which is what
happens when a vulnerable class is edited and its rows are not moved with it.

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
- **`Number of Sources` column:** the number of vulnerable levels that expose
  the issue through that line (`1` unless several levels share it). It is there
  for human reference and is **not used for scoring**: the first match gets
  full credit.

## Calling the endpoint

```bash
# DAST
curl -X POST http://localhost/VulnerableApp/scanner/benchmark \
  -H "Content-Type: application/json" \
  -d @benchmarks/ZAP/findings/zap-findings.json

# SAST
curl -X POST http://localhost/VulnerableApp/scanner/benchmark \
  -H "Content-Type: application/json" \
  -d @benchmarks/semgrep-sast-sample.json
```

The HTTP response contains the same JSON that gets persisted to disk.

## Output format

The output schema is the same for DAST and SAST. The fields inside each
`missedItems` / `unmatchedItems` entry vary by scan type (unused fields are
omitted from the JSON).

```json
{
  "tool": "ZAP",
  "coverage": 4.29,
  "totalExpected": 140,
  "detected": 6,
  "missed": 134,
  "unmatched": 147,
  "missedItems":     [ { "url": "/...", "type": "..." } ],
  "unmatchedItems":  [ { "url": "/...", "cwe": "..." } ]
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
| `benchmark.dast.ground-truth.url` | `http://localhost:${server.port:9090}${server.servlet.context-path:/VulnerableApp}/scanner/dast` | URL the DAST comparator fetches ground truth from. Override when running behind VulnerableApp-facade so coverage spans every backing app. |
| `benchmark.dast.ground-truth.connect-timeout-ms` | `5000` | Connect timeout (ms) for the ground-truth fetch. Fail-fast bound so a stalled endpoint can't tie up Tomcat request threads. |
| `benchmark.dast.ground-truth.read-timeout-ms` | `10000` | Read timeout (ms) for the ground-truth fetch. |
| `benchmark.sast.ground-truth.path` | `classpath:scanner/sast/expectedIssues.csv` | CSV the SAST comparator loads expected issues from, and `/scanner/sast` serves. Drop the `classpath:` prefix to read a file from disk instead. |

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

## Known Limitations

### SSL/TLS and header-hardening findings are always unmatched (all DAST scanners)

DAST scanners commonly report findings such as missing `Strict-Transport-Security`,
`X-Content-Type-Options`, or insecure cookie flags. These are valid security
observations but fall outside VulnerableApp's intentional vulnerability set.
They will always appear in `unmatchedItems` and should not be interpreted as
false positives. This applies to any DAST scanner benchmarked against VulnerableApp.

### A finding on another line of the same flow is unmatched (all SAST scanners)

The SAST matcher compares exact line numbers. A scanner that reports a real
issue on a different line of the same data flow gets no credit for it: the
expected row shows up in `missedItems` and the finding in `unmatchedItems`. For
example, Semgrep reports SSRF where the `URL` object is built
(`SSRFVulnerability.java:67`), while the expected row is the `openConnection()`
call at line 85.

`unmatchedItems` also holds findings on levels marked `Variant.SECURE`, findings
outside the intended vulnerability set (CSRF on request mappings, cookie flags),
and findings in classes that have no ground-truth rows yet, so it should not be
read as a list of false positives.


