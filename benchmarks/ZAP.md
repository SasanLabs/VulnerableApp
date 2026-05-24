# Benchmarking OWASP ZAP against VulnerableApp

This guide walks through the complete process of running OWASP ZAP against
VulnerableApp and evaluating its findings using the benchmark framework
introduced in [#553](https://github.com/SasanLabs/VulnerableApp/issues/553).

---

## Prerequisites

| Tool | Version | Notes |
|---|---|---|
| Docker | 20+ | Recommended way to run ZAP |
| Python | 3.9+ | Required for the conversion script |
| VulnerableApp | latest | Must be running before scanning |

---

## Overview

```
┌─────────────┐    zap-automation.yaml    ┌─────────────────────────┐
│ VulnerableApp│ ◄────── ZAP scans ──────  │ OWASP ZAP (Docker)      │
│ :9090        │                           │ writes zap-raw-report.json│
└─────────────┘                           └─────────────────────────┘
                                                       │
                              convert_zap_to_benchmark.py
                                                       │
                                                       ▼
                                          zap-benchmark-input.json
                                                       │
                              POST /scanner/benchmark
                                                       │
                                                       ▼
                                          benchmarks/zap-results.json
```

---

## Step 1 — Start VulnerableApp

```bash
# Using Gradle
./gradlew bootRun

# Or using Docker (if available)
docker run -p 9090:9090 sasanlabs/owasp-vulnerableapp:latest
```

Verify it is running:

```bash
curl http://localhost:9090/VulnerableApp/scanner | python3 -m json.tool | head -30
```

You should see a JSON array of vulnerability entries. Keep VulnerableApp
running for the entire workflow.

---

## Step 2 — Run ZAP against VulnerableApp

The `benchmarks/zap-automation.yaml` file defines a full ZAP Automation
Framework plan: traditional spider, Ajax spider, and active scan.

### Option A — Docker (recommended)

```bash
docker run --rm \
  --network host \
  -v "$(pwd)/benchmarks:/zap/wrk/:rw" \
  ghcr.io/zaproxy/zaproxy:stable \
  zap.sh -cmd -autorun /zap/wrk/zap-automation.yaml
```

The flag `--network host` lets ZAP reach VulnerableApp on `localhost:9090`.

After the scan completes, `benchmarks/zap-raw-report.json` will be created.

### Option B — ZAP Desktop / installed ZAP

1. Open ZAP.
2. Go to **Automation → Import Automation Plan…** and select
   `benchmarks/zap-automation.yaml`.
3. Update the `reportDir` inside the plan to an absolute path on your machine.
4. Click **Run**.

---

## Step 3 — Convert the ZAP report

ZAP's raw JSON format is not directly accepted by the benchmark endpoint.
The conversion script maps each ZAP alert instance to a Finding using its
CWE and WASC IDs — no manual vulnerability-name mapping is needed.

```bash
python3 benchmarks/scripts/convert_zap_to_benchmark.py \
    --input  benchmarks/zap-raw-report.json \
    --output benchmarks/zap-benchmark-input.json
```

Example output:

```
Converted 47 finding(s)  →  benchmarks/zap-benchmark-input.json
```

The generated file follows the benchmark input contract:

```json
{
  "tool": "ZAP",
  "scanType": "DAST",
  "findings": [
    {
      "url": "http://localhost:9090/VulnerableApp/BlindSQLInjectionVulnerability/LEVEL_1",
      "cwe": "CWE-89",
      "wascId": "19",
      "method": "GET"
    },
    ...
  ]
}
```

---

## Step 4 — Submit to the benchmark endpoint

```bash
curl -s -X POST http://localhost:9090/VulnerableApp/scanner/benchmark \
  -H "Content-Type: application/json" \
  -d @benchmarks/zap-benchmark-input.json | python3 -m json.tool
```

Example response:

```json
{
  "tool": "ZAP",
  "coverage": 62.5,
  "totalExpected": 48,
  "detected": 30,
  "missed": 18,
  "unmatched": 9,
  "missedItems": [
    { "url": "/JWTVulnerability/LEVEL_1", "type": "CLIENT_SIDE_VULNERABLE_JWT" },
    ...
  ],
  "unmatchedItems": [
    { "url": "/NonExistentEndpoint/LEVEL_1", "cwe": "CWE-693" },
    ...
  ]
}
```

The report is also written to `benchmarks/zap-results.json`.

> **Note on filename:** The current benchmark framework names the output file
> `<tool>-results.json` (i.e. `zap-results.json`), regardless of scan type.
> Issue [#603](https://github.com/SasanLabs/VulnerableApp/issues/603) proposes
> including the scan type in the filename (e.g. `zap-DAST-results.json`) to
> avoid collisions when multiple scan types or tools are benchmarked. This
> requires a framework-level change and is tracked as a follow-up to this PR.

---

## Step 5 — Interpret the results

| Field | Meaning |
|---|---|
| `coverage` | `(detected / totalExpected) * 100` — ZAP's detection rate |
| `totalExpected` | Total UNSECURE ground-truth findings from `/scanner` |
| `detected` | Findings ZAP reported that match the ground truth |
| `missed` | Ground-truth findings ZAP did not report |
| `unmatched` | ZAP findings that don't correspond to any ground-truth entry |
| `missedItems` | Full list of missed ground-truth entries |
| `unmatchedItems` | Full list of unmatched ZAP findings |

### What unmatched findings mean

`unmatchedItems` are not necessarily false positives — some ZAP findings are
valid security observations that fall outside VulnerableApp's intentional
vulnerability set (e.g. missing security headers, SSL/TLS configuration, or
cookie flags on a non-HTTPS dev environment). They represent scanner findings
that have no corresponding ground-truth entry.

### What missed findings mean

`missedItems` are vulnerabilities in VulnerableApp that ZAP did not detect.
Common reasons include:
- The vulnerability requires authentication or a specific token ZAP didn't obtain.
- The vulnerability manifests only via non-standard HTTP methods ZAP didn't fuzz.
- JWT and cryptographic vulnerabilities often require semantic understanding
  beyond standard active-scan rules.

---

## Running the sample (without a live ZAP scan)

A pre-built comprehensive sample input is available at
`benchmarks/samples/zap-full-findings-sample.json`. It covers all
VulnerabilityType CWE/WASC values and includes one deliberately invalid finding
to demonstrate `unmatchedItems`.

> The original minimal demo sample (`benchmarks/samples/zap-findings-sample.json`)
> shipped with the benchmark framework in PR #602 is still present and unchanged.
> The full sample here is the ZAP-specific companion added by this PR.

```bash
curl -s -X POST http://localhost:9090/VulnerableApp/scanner/benchmark \
  -H "Content-Type: application/json" \
  -d @benchmarks/samples/zap-full-findings-sample.json | python3 -m json.tool
```

A sample benchmark output showing what the response looks like is at
`benchmarks/samples/zap-benchmark-output-example.json`.

---

## Automation / CI

To run the full pipeline non-interactively:

```bash
# 1. Start VulnerableApp in the background
./gradlew bootRun &
APP_PID=$!
sleep 30   # wait for startup

# 2. Run ZAP
docker run --rm --network host \
  -v "$(pwd)/benchmarks:/zap/wrk/:rw" \
  ghcr.io/zaproxy/zaproxy:stable \
  zap.sh -cmd -autorun /zap/wrk/zap-automation.yaml

# 3. Convert
python3 benchmarks/scripts/convert_zap_to_benchmark.py \
  --input  benchmarks/zap-raw-report.json \
  --output benchmarks/zap-benchmark-input.json

# 4. Submit
curl -f -X POST http://localhost:9090/VulnerableApp/scanner/benchmark \
  -H "Content-Type: application/json" \
  -d @benchmarks/zap-benchmark-input.json \
  -o benchmarks/zap-results.json

# 5. Stop VulnerableApp
kill $APP_PID
```

---

## Mapping: ZAP alerts → VulnerabilityType

The benchmark endpoint supports three matching axes — **any one** is enough to
count as a match. ZAP natively provides `cweid` and `wascid` per alert, so
explicit type-name mapping is rarely needed.

| VulnerabilityType | CWE | WASC |
|---|---|---|
| BLIND_SQL_INJECTION | 89 | 19 |
| ERROR_BASED_SQL_INJECTION | 89 | 19 |
| UNION_BASED_SQL_INJECTION | 89 | 19 |
| REFLECTED_XSS | 79 | 8 |
| PERSISTENT_XSS | 79 | 8 |
| DOM_BASED_XSS | 79 | 8 |
| HEADER_INJECTION | 20 | 20 |
| PATH_TRAVERSAL | 22 | 33 |
| COMMAND_INJECTION | 77 | 31 |
| UNRESTRICTED_FILE_UPLOAD | 434 | — |
| UNCONTROLLED_RESOURCE_CONSUMPTION | 400 | — |
| DENIAL_OF_SERVICE | 730 | 10 |
| OPEN_REDIRECT_3XX_STATUS_CODE | 601 | 38 |
| SIMPLE_SSRF | 918 | 15 |
| BLIND_SSRF | 918 | 15 |
| XXE | 611 | 43 |
| WEAK_CRYPTOGRAPHIC_HASH | 327 | — |
| INSECURE_CRYPTOGRAPHIC_STORAGE | 326 | — |
| USE_OF_BROKEN_CRYPTOGRAPHIC_ALGORITHM | 330 | — |
| INSECURE_DIRECT_OBJECT_REFERENCE | 639 | 13 |
| CLICKJACKING | 1021 | — |
| PLAINTEXT_PASSWORD_STORAGE | 256 | — |
| WEAK_PASSWORD_HASHING | 327 | — |
| USERNAME_ENUMERATION | 204 | — |
| LDAP_INJECTION | 90 | 29 |
| WEB_CACHE_POISONING | — | — |
| CLIENT_SIDE_VULNERABLE_JWT | — | — |
| SERVER_SIDE_VULNERABLE_JWT | — | — |
| INSECURE_CONFIGURATION_JWT | — | — |

> **Note:** Types with no CWE or WASC (JWT, WEB_CACHE_POISONING) require
> matching via `"type"` field using the exact enum name. ZAP typically does not
> have dedicated rules for these, so they appear in `missedItems` for most scans.

---

## Known Limitations

### Benchmark report filename does not include scan type
The framework currently writes `benchmarks/zap-results.json`. The ideal
filename — `zap-DAST-results.json` — would distinguish DAST results from future
SAST or LLM scan results for the same tool and prevent accidental overwrites.
This requires a change in `BenchmarkResultWriter.java` to incorporate
`scanType` into the filename. It is tracked as a follow-up; for now, if you
run both DAST and SAST scans for ZAP, rename the output file manually between
runs:

```bash
mv benchmarks/zap-results.json benchmarks/zap-DAST-results.json
```

### JWT and some cryptographic vulnerability types are not detectable by ZAP
`CLIENT_SIDE_VULNERABLE_JWT`, `SERVER_SIDE_VULNERABLE_JWT`,
`INSECURE_CONFIGURATION_JWT`, and `WEB_CACHE_POISONING` have no CWE or WASC
mapping (see table above). ZAP has no built-in rules for these, so they will
always appear in `missedItems`. This is expected behaviour, not a bug in the
benchmark or the conversion script.

### SSL/TLS and header-hardening findings are always unmatched
ZAP commonly reports findings such as missing `Strict-Transport-Security`,
`X-Content-Type-Options`, or insecure cookie flags. These are valid security
observations but fall outside VulnerableApp's intentional vulnerability set.
They will always appear in `unmatchedItems` and should not be interpreted as
false positives.

---

## Troubleshooting

**ZAP cannot reach VulnerableApp**
Make sure `--network host` is set in Docker. Alternatively, replace
`localhost` with `host.docker.internal` (Mac/Windows) in `zap-automation.yaml`.

**`benchmarks/zap-raw-report.json` is empty**
ZAP may have failed to crawl the app. Check ZAP's console output for
authentication errors or startup issues.

**HTTP 500 from `/scanner/benchmark`**
The benchmark result was computed but could not be written to disk. Check
VulnerableApp's working directory permissions. The response body still contains
the full metrics.

**Coverage is unexpectedly low**
Some VulnerableApp vulnerabilities (JWT, cryptographic types) have no
corresponding CWE/WASC entry. ZAP will not detect these with standard rules.
See the mapping table above.
