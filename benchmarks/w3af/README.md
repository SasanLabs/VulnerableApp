# w3af Benchmark

w3af is benchmarked against VulnerableApp in **DAST** mode. Its findings are
graded on the URL that carried the payload plus the CWE and WASC ids w3af
attaches to the plugin that reported them.

## Latest results

| Results file |
|---|
| [`benchmarks/w3af/w3af-results.json`](w3af-results.json) |

Results are auto-updated every day and on every manual workflow run.

## Running the benchmark

The full pipeline (start VulnerableApp → run w3af → convert → benchmark →
commit results) is automated via GitHub Actions:

1. Go to the **Actions** tab → **w3af Benchmark**
2. Click **Run workflow**
3. Results are committed automatically to the file above

See
[`.github/workflows/w3af-benchmark.yml`](../../.github/workflows/w3af-benchmark.yml)
for the full workflow definition.

## Running the scan

To run w3af yourself, walk the console steps below and keep the JSON report:

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

## Conversion script

```bash
python3 benchmarks/w3af/scripts/convert_w3af_to_benchmark.py \
    --input  benchmarks/w3af/w3af-raw.json \
    --output benchmarks/w3af/w3af-benchmark-input.json
```

The script reads the `items` array, takes `URL` as the finding URL, `HTTP
method` as the method, and keeps every non-zero entry of `CWE IDs` / `WASC IDs`
as one finding per id. Plugin names are deliberately **not** mapped to
`VulnerabilityType` values, so matching runs on the CWE and WASC axes; a name
table would be one more thing to keep in sync as plugins are renamed.

Then submit it like any other DAST payload:

```bash
curl -X POST http://localhost/VulnerableApp/scanner/benchmark \
  -H "Content-Type: application/json" \
  -d @benchmarks/w3af/w3af-benchmark-input.json
```

A converted sample is at
[`benchmarks/w3af/findings/w3af-findings.json`](findings/w3af-findings.json),
produced by that script from
[`benchmarks/w3af/w3af-raw.json`](w3af-raw.json), which is a trimmed run rather
than a full report.
