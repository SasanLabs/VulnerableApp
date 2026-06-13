# Benchmarking ZAP by Checkmarx against VulnerableApp
---
This guide covers running ZAP against both the **Modern UI** and **Legacy UI**
of VulnerableApp and evaluating coverage using the benchmark framework.

## Prerequisites

| Tool | Version | Notes |
|---|---|---|
| Docker | 20+ |
| Python | 3.9+ |

---

## Overview
 
```
┌──────────────────────┐   zap-automation-modern.yaml / zap-automation-legacy.yaml
│ VulnerableApp        │ ◄──────────── ZAP scans ────────────────────────────────┐
│ :80 (facade)         │                                                          │
│ /VulnerableApp (legacy)│                                               OWASP ZAP (Docker)
└──────────────────────┘                                                          │
                                                                                  │
                                         convert_zap_to_benchmark.py             │
                                                      ▼                           │
                                         zap-benchmark-input.json  ◄─────────────┘
                                                      │
                                         POST /scanner/benchmark
                                                      │
                                                      ▼
                             zap-results.json / zap-results-legacy.json
```
 
---

## Step 1 — Start VulnerableApp 

```bash

#Linux/Mac
./scripts/testWithModernUI.sh

# Windows
.\scripts\testWithModernUI.bat
```
The modern UI will be available at `http://localhost` once the stack is up.

## Step 2 — Run ZAP 

First, detect the Docker network VulnerableApp is running on:
 
```bash
NETWORK=$(docker inspect $(docker compose -f docker-compose.without_llm.yml ps -q | head -1) \
  --format '{{range $k,$v := .NetworkSettings.Networks}}{{$k}}{{end}}')
echo "Network: $NETWORK"
```

### Modern UI scan
 
```bash
docker run \
  --network "$NETWORK" \
  --shm-size=8g \
  --user root \
  -v "$(pwd)/benchmarks/ZAP:/zap/wrk/:rw" \
  ghcr.io/zaproxy/zaproxy:weekly \
  bash -c "
    zap.sh -cmd \
      -addonupdate \
      -addoninstallall \
      -autorun /zap/wrk/zap-automation-modern.yaml
  "
```

After the scan completes, `benchmarks/ZAP/zap-raw-report.json` will be created.

### Legacy UI scan
 
```bash
docker run \
  --network "$NETWORK" \
  --shm-size=8g \
  --user root \
  -v "$(pwd)/benchmarks/ZAP:/zap/wrk/:rw" \
  ghcr.io/zaproxy/zaproxy:weekly \
  bash -c "
    zap.sh -cmd \
      -addonupdate \
      -addoninstallall \
      -autorun /zap/wrk/zap-automation-legacy.yaml
  "
```
 
After the scan completes, `benchmarks/ZAP/zap-raw-report-legacy.json` will be created.
 
---

## Step 3 — Convert the ZAP reports

ZAP's raw JSON format is not directly accepted by the benchmark endpoint.
The conversion script maps each ZAP alert instance to a Finding using its
CWE and WASC IDs — no manual vulnerability-name mapping is needed.

```bash
# Modern UI
python3 benchmarks/ZAP/scripts/convert_zap_to_benchmark.py \
    --input  benchmarks/ZAP/zap-raw-report.json \
    --output benchmarks/ZAP/zap-benchmark-input.json
 
# Legacy UI
python3 benchmarks/ZAP/scripts/convert_zap_to_benchmark.py \
    --input  benchmarks/ZAP/zap-raw-report-legacy.json \
    --output benchmarks/ZAP/zap-benchmark-input-legacy.json
```

---

## Step 4 — Submit to the benchmark endpoint

```bash
# Modern UI
curl -s -X POST http://localhost/VulnerableApp/scanner/benchmark \
  -H "Content-Type: application/json" \
  -d @benchmarks/ZAP/zap-benchmark-input.json \
  -o benchmarks/ZAP/zap-results.json
 
cat benchmarks/ZAP/zap-results.json | python3 -m json.tool
 
# Legacy UI
curl -s -X POST http://localhost/VulnerableApp/scanner/benchmark \
  -H "Content-Type: application/json" \
  -d @benchmarks/ZAP/zap-benchmark-input-legacy.json \
  -o benchmarks/ZAP/zap-results-legacy.json
 
cat benchmarks/ZAP/zap-results-legacy.json | python3 -m json.tool
```
Results are saved to `benchmarks/ZAP/zap-results.json` and
`benchmarks/ZAP/zap-results-legacy.json`.
See `benchmarks/ZAP/benchmark_results.md` for the latest benchmark summary.
 
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

## GitHub Actions [recommended]

Two separate workflows are provided — run independently:
 
| Workflow | File | Schedule |
|---|---|---|
| Modern UI | `.github/workflows/zap-benchmark-modern.yml` | Every Monday 00:00 UTC |
| Legacy UI | `.github/workflows/zap-benchmark-legacy.yml` | Every Monday 01:00 UTC |
 
To trigger manually:
 
1. Go to **Actions** tab
2. Click **ZAP Benchmark Modern** or **ZAP Benchmark Legacy**
3. Click **Run workflow** → **Run workflow**
Results are committed automatically to `benchmarks/ZAP/benchmark_results.md`
and uploaded as workflow artifacts for 90 days.
 

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
Make sure ZAP is running on the same Docker network as VulnerableApp.
Use the network detection command in Step 2 to find the correct network name.
On Mac/Windows you may need to replace container names with `host.docker.internal`
in the automation yaml files.

**Coverage is unexpectedly low**
JWT and cryptographic vulnerability types cannot be detected by standard ZAP
rules. This is expected — see Known Limitations above.

