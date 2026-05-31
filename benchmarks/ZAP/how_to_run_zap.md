# Benchmarking ZAP by Checkmarx against VulnerableApp
---

## Prerequisites

| Tool | Version | Notes |
|---|---|---|
| Docker | 20+ |
| Python | 3.9+ |

---

## Overview

```
┌─────────────┐    zap-automation.yaml    ┌─────────────────────────┐
│ VulnerableApp   │ ◄────── ZAP scans ──────  │ OWASP ZAP (Docker)        │
│ :80 (modern UI) │                           │ writes zap-raw-report.json│
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
                                          benchmarks/ZAP/zap-results.json
```

---

## Step 1 — Start VulnerableApp (modern UI)

```bash

#Linux/Mac
./scripts/testWithModernUI.sh

# Windows
.\scripts\testWithModernUI.bat
```
The modern UI will be available at `http://localhost` once the stack is up.

## Step 2 — Run ZAP 

The `benchmarks/ZAP/zap-automation.yaml` file defines a full ZAP Automation
Framework plan: traditional spider, Ajax spider, and active scan.

```bash
docker run --rm \
  --network host \
  -v "$(pwd)/benchmarks/ZAP:/zap/wrk/:rw" \
  ghcr.io/zaproxy/zaproxy:stable \
  zap.sh -cmd -autorun /zap/wrk/zap-automation.yaml
```

After the scan completes, `benchmarks/ZAP/zap-raw-report.json` will be created.

---

## Step 3 — Convert the ZAP report

ZAP's raw JSON format is not directly accepted by the benchmark endpoint.
The conversion script maps each ZAP alert instance to a Finding using its
CWE and WASC IDs — no manual vulnerability-name mapping is needed.

```bash
python3 benchmarks/ZAP/scripts/convert_zap_to_benchmark.py \
    --input  benchmarks/ZAP/zap-raw-report.json \
    --output benchmarks/ZAP/zap-benchmark-input.json
```

---

## Step 4 — Submit to the benchmark endpoint

```bash
curl -s -X POST http://localhost/VulnerableApp/scanner/benchmark \
  -H "Content-Type: application/json" \
  -d @benchmarks/ZAP/zap-benchmark-input.json | python3 -m json.tool
```
Real ZAP scan results are in `benchmarks/ZAP/zap-results.json` and `benchmarks/ZAP/benchmark_results.md`

---

### Step 5 - Stop Stack 

```bash
docker-compose -f docker-compose.local.yml down
```


## Running the sample (without a live ZAP scan)

```bash
curl -s -X POST http://localhost/VulnerableApp/scanner/benchmark \
  -H "Content-Type: application/json" \
  -d @benchmarks/ZAP/samples/zap-full-findings-sample.json | python3 -m json.tool
```

A sample benchmark output showing what the response looks like is at
`benchmarks/ZAP/samples/zap-benchmark-output-example.json`.

---

## GitHub Actions 

Instead of running locally, use the provided GitHub Action for a fully
automated scan on GitHub's infrastructure:

1. Go to **Actions** tab → **ZAP Benchmark** → **Run workflow**
2. Results are committed automatically to `benchmarks/ZAP/benchmark_results.md`

See [`.github/workflows/zap-benchmark.yml`](../../.github/workflows/zap-benchmark.yml).

---

## Known Limitations

### JWT and some cryptographic vulnerability types are not detectable by ZAP
`CLIENT_SIDE_VULNERABLE_JWT`, `SERVER_SIDE_VULNERABLE_JWT`,
`INSECURE_CONFIGURATION_JWT`, and `WEB_CACHE_POISONING` have no CWE or WASC
mapping. ZAP has no built-in rules for these, so they will
always appear in `missedItems`. This is expected behaviour, not a bug in the
benchmark or the conversion script.

---

## Troubleshooting

**ZAP cannot reach VulnerableApp**
Make sure `--network host` is set and the modern UI stack is fully up
(`./scripts/testWithModernUI.sh`). On Mac/Windows replace `localhost` with
`host.docker.internal` in `zap-automation.yaml`.

**Coverage is unexpectedly low**
JWT and cryptographic vulnerability types cannot be detected by standard ZAP
rules. This is expected — see Known Limitations above.

