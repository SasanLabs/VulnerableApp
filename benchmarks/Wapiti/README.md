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
pip install wapiti3 pytest pyyaml
```

### 3. Prepare Start URLs and Run Wapiti

The default crawler does not execute the UI JavaScript or expand the linked
sitemap into lab requests. Download the live sitemap and prepare start URLs
with benign input parameters before scanning. Run these commands in Bash:

```bash
curl --fail --silent --show-error http://localhost/VulnerableApp/sitemap.xml \
    -o /tmp/wapiti-sitemap.xml
python3 benchmarks/Wapiti/scripts/prepare_wapiti_seeds.py \
    --sitemap /tmp/wapiti-sitemap.xml \
    --base-url http://localhost/VulnerableApp/ \
    --output /tmp/wapiti-seeds.txt

seed_args=()
while IFS= read -r url; do
    seed_args+=(-s "$url")
done < /tmp/wapiti-seeds.txt

wapiti -u http://localhost/VulnerableApp/ \
    --scope folder \
    "${seed_args[@]}" \
    -f json \
    -o benchmarks/Wapiti/wapiti-raw-report.json \
    --flush-session \
    --tasks 4
```

The seed generator supplies parameters for eight GET lab families: HTML-tag
and image-attribute XSS, error/union/blind SQL injection, path traversal,
command injection, and SSRF. It includes every sitemap level for these
families, including secure variants, and rebases container-host URLs to the
reachable application root. SSRF uses the application's metadata mock as its
initial input. POST/upload requests, authentication/stateful flows, and other
families need additional discovery support; these seeds do not claim full lab
coverage. When adding GET support, extend `GET_INPUTS` with the controller's
real parameter names and benign inputs.

The daily job uses a 45-minute job timeout rather than Wapiti's 30-second scan
limit. A scanner error or job timeout stops submission/publication. Successful
publication commits the converted findings and result together so rebasing
starts with a clean worktree. The sitemap, seeds, and scan reports are uploaded
as artifacts.

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

### 6. Run Converter, Seed, and Publication Tests

```bash
pytest benchmarks/Wapiti/scripts/ -v
```
