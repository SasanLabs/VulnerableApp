# Wapiti Benchmark Integration

This directory contains the integration scripts, sample findings, and benchmark results for benchmarking the **Wapiti** DAST scanner against VulnerableApp.

## Latest Results

| Results File | Daily Workflow |
|---|---|
| [`benchmarks/Wapiti/wapiti-results.json`](wapiti-results.json) | [`.github/workflows/wapiti-benchmark.yml`](../../.github/workflows/wapiti-benchmark.yml) |

Results are updated daily via GitHub Actions.

---

## Running the Benchmark Locally

### 1. Start VulnerableApp

```bash
docker compose -f docker-compose.without_llm.yml up -d
```

Verify that VulnerableApp is reachable at `http://localhost/VulnerableApp/`.

### 2. Install Wapiti

```bash
pip install wapiti3
```

### 3. Run Wapiti Scan

```bash
wapiti -u http://localhost/VulnerableApp/ \
    --scope folder \
    -f json \
    -o benchmarks/Wapiti/wapiti-raw-report.json \
    --flush-session \
    --tasks 4
```

### 4. Convert the Report

Convert Wapiti's raw JSON report to VulnerableApp's benchmark input format:

```bash
python3 benchmarks/Wapiti/scripts/convert_wapiti_to_benchmark.py \
    --input  benchmarks/Wapiti/wapiti-raw-report.json \
    --output benchmarks/Wapiti/findings/wapiti-findings.json
```

### 5. Submit to Benchmark Endpoint

```bash
curl -X POST http://localhost/VulnerableApp/scanner/benchmark \
    -H "Content-Type: application/json" \
    -d @benchmarks/Wapiti/findings/wapiti-findings.json \
    -o benchmarks/Wapiti/wapiti-results.json
```

### 6. Run Converter Unit Tests

```bash
pytest benchmarks/Wapiti/scripts/test_convert_wapiti_to_benchmark.py -v
```
