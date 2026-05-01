# Scanner Benchmark Framework

This framework lets you grade a DAST scanner (ZAP, Burp, anything else) against the
ground truth that VulnerableApp already exposes via `/scanner`. You feed it the
scanner's findings as JSON; it returns coverage, missed issues, and false
positives, and writes a JSON report to `benchmarks/<tool>-results.json`.

> Running the scanner is **out of scope**. You are responsible for running ZAP /
> Burp / your tool against a live VulnerableApp instance and converting its output
> into the input format below.

## Input format

```json
{
  "tool": "ZAP",
  "findings": [
    { "url": "/BlindSQLInjectionVulnerability/LEVEL_1", "type": "BLIND_SQL_INJECTION" },
    { "url": "/XSSInImgTagAttribute/LEVEL_1",          "type": "REFLECTED_XSS" }
  ]
}
```

To discover the real URL/type pairs to send, hit
`GET http://<baseurl>/VulnerableApp/scanner` — that's the ground truth you're being
graded against. The full sample at `benchmarks/samples/zap-findings-sample.json`
includes one deliberately invalid entry so a successful run produces a non-empty
`falsePositiveItems` list for demonstration.

- `tool` — required, non-empty. Used as the report filename.
- `findings` — required (use `[]` if the scanner found nothing).
- `findings[].url` — relative path (`/SQLInjection/LEVEL_1`) or absolute URL
  (`http://localhost:9090/VulnerableApp/SQLInjection/LEVEL_1`); the comparator
  normalises both. Query strings, the `/VulnerableApp` context path, and trailing
  slashes are stripped.
- `findings[].type` — case-insensitive match against
  [`VulnerabilityType`](../src/main/java/org/sasanlabs/vulnerability/types/VulnerabilityType.java)
  enum names. See the canonical values below.

## Canonical vulnerability type values (v1)

The benchmark matches `findings[].type` against the `VulnerabilityType` enum by
name. A scanner reporting `"SQL_INJECTION"` will not match — use a specific
sub-type. The full set lives in
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

## Calling the endpoint

Start VulnerableApp, then:

```bash
curl -X POST http://localhost:9090/VulnerableApp/scanner/benchmark \
  -H "Content-Type: application/json" \
  -d @benchmarks/samples/zap-findings-sample.json
```

The HTTP response contains the same JSON that gets persisted to disk.

## Output format

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

- `coverage` — `detected / totalExpected * 100`. Reported as `0.0` when ground
  truth is empty.
- `totalExpected` — number of `(URL, vulnerabilityType)` pairs across all
  `UNSECURE` ground-truth entries. `SECURE` entries are intentionally clean and
  do not count toward expected.
- `missedItems` — expected pairs the scanner did not report.
- `falsePositiveItems` — pairs the scanner reported that are not in the
  expected set. Includes findings against `SECURE` URLs (those endpoints are
  intentionally clean, so flagging them is over-eager).

## Where the report is written

By default, `benchmarks/<sanitised-tool>-results.json` relative to the working
directory of the running VulnerableApp process. The directory is configurable
via the `benchmark.output.dir` property in `application.properties`. Filenames
are lowercased and stripped of anything outside `[a-z0-9_-]`. Re-running the
endpoint for the same tool overwrites the previous report.

## Limitations (v1)

- DAST only; SAST benchmarking against `scanner/sast/expectedIssues.csv` is not
  implemented.
- Type matching is by enum name. CWE-based matching is a likely follow-up.
- HTTP method is shown in the ground truth but is not part of the match key —
  if the scanner reports the right URL + type with a different method, it still
  counts as detected.
