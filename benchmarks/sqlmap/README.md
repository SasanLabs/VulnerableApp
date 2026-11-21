# sqlmap benchmark

This integration runs upstream [sqlmap](https://github.com/sqlmapproject/sqlmap)
against the three Java SQL-injection families in issue #860. Each benchmark run
resolves the newest version-numbered release tag, checks out that tag, and checks
the scanner's `--version` before scanning. Untagged development builds are not used.
The scanner's source is fetched from upstream and is not bundled here.

The [workflow](../../.github/workflows/sqlmap-benchmark.yml) runs the converter and
sitemap fixture tests and checks pipeline exit status with a dummy process on PRs
that change this integration. The full benchmark runs only on a weekly schedule
or manual dispatch, after those tests pass. Like the other DAST workflows, it
starts the modern UI Docker stack from `docker-compose.without_llm.yml` with the
published images (Java, JSP, PHP, MongoDB, Mailpit, and the Nginx facade) and
records the Java, JSP, PHP and facade image IDs and digests. All scan requests
go through the facade.
LLMForge and its model downloads are outside this SQL benchmark.

## Discovery and scope

Discovery starts with the facade's `/sitemap.xml`. The small
`scripts/sitemap_to_targets.py` adapter preserves every listed path and query,
replaces Docker-internal origins with the configured facade origin, and adds
`?id=1` only when a URL has no query. It does not select vulnerability families,
levels, variants, or known-vulnerable endpoints.

sqlmap's native `-m` bulk input and `--scope` filter select these Java families:

- `BlindSQLInjectionVulnerability`
- `ErrorBasedSQLInjectionVulnerability`
- `UnionBasedSQLInjectionVulnerability`

The scope is deliberately limited to issue #860. The native filter includes secure
controls when they appear in the sitemap. No ground-truth endpoint is read to
choose targets. The workflow captures `/VulnerableApp/scanner/dast` after scanning
for audit; only the benchmark comparator uses it for scoring.

sqlmap 1.10.11 has no `--sitemap-url` option, and its bulk parser needs URLs with
parameters. The adapter supplies the sitemap URLs and the common `id` seed needed
by these Java labs. This is not browser-based form discovery. Authentication SQLi
uses `username` and `password`, so it remains outside this run, as do PHP's NoSQL
labs and the JSP application. Extending the scope requires reviewing the request
parameters and scanner settings; being present in the sitemap alone does not mean
an endpoint was scanned.

## Scan and report handling

The workflow invokes sqlmap directly, without a Python process runner. Settings
are `-p id`, `--batch`, `--technique=BEU`, `--level=1`, `--risk=1`, `--threads=1`,
`--timeout=5`, and `--retries=0`. BEU selects boolean-based blind, error-based,
and UNION-query tests. Time-based and stacked-query techniques, authenticated
scenarios, and data extraction are outside this run.

`--answers='skip further tests=N'` prevents sqlmap's default bulk-mode behavior
from skipping the rest of a host after the first confirmed injection.

Each workflow gets fresh sessions and uses `--flush-session`. GNU `timeout` stops
the whole scanner after 20 minutes and kills it after a further 10 seconds if
necessary. The Actions job has a 30-minute bound, and scanner address space is
limited to 2 GiB. A timeout or nonzero scanner exit stops the workflow before
conversion or submission.

`convert_sqlmap_to_benchmark.py` reads the native `--report-json` output, including
its bulk `targets` groups. A confirmed TECHNIQUES record becomes a finding with
the native TARGET URL without its query, the request's method, and
`cwe: "CWE-89"`. The comparator ignores the query when it matches URLs, so the
finding carries the path only. Multiple parameter findings are retained. Matching
and deduplication belong to `/scanner/benchmark`.

A completed negative has sqlmap's explicit negative conclusion in the native
report. Transport errors, missing target identity, partial records, or a report
with neither confirmed techniques nor that conclusion stop conversion. These
checks distinguish a failed scan from a completed negative; they do not score
findings or validate them against ground truth.

The workflow defines the scan scope once, in `SQLMAP_SCOPE`, and passes it to
sqlmap and to the converter. The converter reads `targets.txt` the way sqlmap's
`-m` option does and needs exactly one report entry per scanned path, so a
missing target or an extra report entry stops the run before submission.

Artifacts retain the sitemap, bulk input, native report, session files, console
log, scanner version and revision, workflow revision, container image IDs and
digests, stack logs, converted findings, ground truth, and benchmark response.
The Actions step log records the exact CLI invocation. The step summary uses
the benchmark response.
Containers and volumes are removed in the cleanup step, including after failure.

## Local run

Use a controlled instance and a fresh artifact directory. The workflow contains
the complete commands; the same pipeline can be run locally:

```bash
docker compose -f docker-compose.without_llm.yml up -d
python3 -m unittest discover -s benchmarks/sqlmap/scripts -v

mkdir -p benchmarks/sqlmap/artifacts
curl --fail http://127.0.0.1/sitemap.xml -o benchmarks/sqlmap/artifacts/sitemap.xml
python3 benchmarks/sqlmap/scripts/sitemap_to_targets.py \
  --sitemap benchmarks/sqlmap/artifacts/sitemap.xml --base-url http://127.0.0.1 \
  --output benchmarks/sqlmap/artifacts/targets.txt
```

Check out the newest stable tag as the workflow does, then run its sqlmap CLI
command against `targets.txt`. Convert the native report and submit the findings:

```bash
python3 benchmarks/sqlmap/scripts/convert_sqlmap_to_benchmark.py \
  --input benchmarks/sqlmap/artifacts/report.json \
  --output benchmarks/sqlmap/artifacts/sqlmap-findings.json
curl --fail-with-body http://127.0.0.1/VulnerableApp/scanner/benchmark \
  -H 'Content-Type: application/json' \
  --data-binary @benchmarks/sqlmap/artifacts/sqlmap-findings.json \
  -o benchmarks/sqlmap/artifacts/sqlmap-results.json
docker compose -f docker-compose.without_llm.yml down -v --remove-orphans
```

Stop after a failed command and inspect the artifacts. Python's standard library
is sufficient for both scripts and their tests.

## Reading results

The comparator's coverage denominator contains all UNSECURE Java DAST expected
items, not just SQL injection. A completed negative means the selected scanner
settings confirmed no injection; it does not establish that a lab is secure.
Results depend on the scanner release, application revision, and settings.
Unmatched or out-of-scope findings require review and are not automatically
false positives.
