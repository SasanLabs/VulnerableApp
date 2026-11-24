# sqlmap benchmark

This integration runs upstream [sqlmap](https://github.com/sqlmapproject/sqlmap)
against every URL in the facade sitemap, for issue #860. Each benchmark run
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

## Discovery and targets

Discovery starts with the facade's `/sitemap.xml`. The small
`scripts/sitemap_to_targets.py` adapter preserves every listed path and query,
replaces Docker-internal origins with the configured facade origin, and adds
`?id=1` only when a URL has no query. sqlmap scans every resulting URL; nothing
selects vulnerability families, levels, variants, or known-vulnerable endpoints.
No ground-truth endpoint is read to choose targets. The workflow captures
`/VulnerableApp/scanner/dast` after scanning for audit; only the benchmark
comparator uses it for scoring.

sqlmap 1.10.11 has no `--sitemap-url` option, and its `-m` bulk input skips a URL
without a query, so a sitemap URL without one would not be scanned at all. sqlmap
tests the parameters in the URL; it does not discover parameter names, and this is
not browser-based form discovery. `id` is the parameter the Java SQL-injection labs
read. Labs that read other parameters, such as Authentication SQLi with `username`
and `password`, are only tested with the `id` seed, so a negative result there says
nothing about their real parameters.

## Scan and report handling

The workflow invokes sqlmap directly, without a Python process runner. Settings
are `--batch`, `--technique=BEU`, `--level=1`, `--risk=1`, `--threads=1`,
`--timeout=5`, and `--retries=0`. At level 1 sqlmap tests GET and POST parameters,
not cookies or other headers. BEU selects boolean-based blind, error-based,
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
report, alone or with the WAF/IPS heuristic or page-stability message sqlmap logs
during those tests. When a lab answers the first request with 401, or with another
HTTP error and an empty body, sqlmap stops before testing. That URL is not tested:
it is neither a finding nor a failure, and the native report and scan log record
it. Any other error, missing target identity, partial records, or a report with
neither confirmed techniques nor one of these outcomes stops conversion. These
checks distinguish a failed scan from a completed or untested target; they do not
score findings or validate them against ground truth.

The converter reads `targets.txt` the way sqlmap's `-m` option does and needs
exactly one report entry per URL. sqlmap skips a repeated path whose parameters it
has already tested, so `targets.txt` must list each path once. A missing target,
an extra report entry, or a repeated path stops the run before submission.

Artifacts retain the sitemap, bulk input, native report, session files, console
log, scanner version and revision, workflow revision, container image IDs and
digests, stack logs, converted findings, ground truth, and benchmark response.
The Actions step log records the exact CLI invocation. The step summary uses
the benchmark response.
Containers and volumes are removed in the cleanup step, including after failure.

## Local run

Use a controlled instance and a fresh artifact directory. The workflow contains
the complete commands; the same pipeline can be run locally. The commands use
their own Compose project, `vulnerableapp-sqlmap`, so the final `down -v` deletes
only that project's MongoDB and Mailpit volumes. The stack publishes ports 80,
8025 and 27017, so stop anything else using them first.

```bash
docker compose -p vulnerableapp-sqlmap -f docker-compose.without_llm.yml up -d
for attempt in {1..40}; do
  curl --fail --silent --max-time 2 http://127.0.0.1/VulnerableApp/ > /dev/null && break
  sleep 2
done
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
  --expected-urls benchmarks/sqlmap/artifacts/targets.txt \
  --output benchmarks/sqlmap/artifacts/sqlmap-findings.json
curl --fail-with-body http://127.0.0.1/VulnerableApp/scanner/benchmark \
  -H 'Content-Type: application/json' \
  --data-binary @benchmarks/sqlmap/artifacts/sqlmap-findings.json \
  -o benchmarks/sqlmap/artifacts/sqlmap-results.json
docker compose -p vulnerableapp-sqlmap -f docker-compose.without_llm.yml down -v --remove-orphans
```

Stop after a failed command and inspect the artifacts. Python's standard library
is sufficient for both scripts and their tests.

## Reading results

The comparator's coverage denominator contains all UNSECURE Java DAST expected
items, not just SQL injection. A finding on a URL the ground truth does not list
is unmatched; `/scanner/dast` currently lists only Java endpoints. A completed
negative means the selected scanner settings confirmed no injection; it does not
establish that a lab is secure.
Results depend on the scanner release, application revision, and settings.
Unmatched findings require review and are not automatically false positives.
