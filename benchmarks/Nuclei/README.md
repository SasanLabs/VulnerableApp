# Nuclei Benchmark

Nuclei is benchmarked against VulnerableApp in **DAST** mode. Its templates match
a request rather than a vulnerability class, so a finding is graded by the URL
that matched plus the CWE id the template declares.

## Latest results

| Results file |
|---|
| [`benchmarks/Nuclei/nuclei-results.json`](nuclei-results.json) |

Results are auto-updated every day and on every manual workflow run.

## Running the benchmark

The full pipeline (start VulnerableApp → run Nuclei → convert → benchmark →
commit results) is automated via GitHub Actions:

1. Go to the **Actions** tab → **Nuclei Benchmark**
2. Click **Run workflow**
3. Run the workflow to commit results to the file above

See
[`.github/workflows/nuclei-benchmark.yml`](../../.github/workflows/nuclei-benchmark.yml)
for the full workflow definition.

## Running the scan

To run Nuclei yourself against a standalone VulnerableApp (port 9090):

```bash
nuclei -u http://localhost:9090/VulnerableApp/ -jsonl \
    -o benchmarks/Nuclei/nuclei-raw.jsonl
```

Pick the template set that covers the classes you want graded. The benchmark
grades what the scan reports and counts the rest of the ground truth as missed,
so a narrow template set produces a low coverage number rather than an error.

## Conversion script

```bash
python3 benchmarks/Nuclei/scripts/convert_nuclei_to_benchmark.py \
    --input  benchmarks/Nuclei/nuclei-raw.jsonl \
    --output benchmarks/Nuclei/nuclei-benchmark-input.json
```

The script takes `matched-at` as the URL, the first entry of
`info.classification.cwe-id` as the CWE, and the method off the request line when
the template recorded one. Template names are deliberately **not** mapped to
`VulnerabilityType` values, so matching runs on the CWE axis.

Then submit it like any other DAST payload:

```bash
curl -X POST http://localhost:9090/VulnerableApp/scanner/benchmark \
  -H "Content-Type: application/json" \
  -d @benchmarks/Nuclei/nuclei-benchmark-input.json
```

A converted sample is at
[`benchmarks/Nuclei/findings/nuclei-findings.json`](findings/nuclei-findings.json),
produced by that script from
[`benchmarks/Nuclei/nuclei-raw.jsonl`](nuclei-raw.jsonl), which is a trimmed run
rather than a full report.
