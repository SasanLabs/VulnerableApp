# sqlmap benchmark

This integration tests the Java SQL-injection challenges against a controlled
running instance. It uses upstream [sqlmap 1.10.10](https://github.com/sqlmapproject/sqlmap/tree/65b6e7b4acb13a736614a1ab73b0908476f3c24a),
pinned to `65b6e7b4acb13a736614a1ab73b0908476f3c24a`. The scanner is fetched
from upstream; its source is not bundled here. See upstream's licensing terms.

The [GitHub Actions workflow](../../.github/workflows/sqlmap-benchmark.yml) runs
weekly, manually, and on PRs changing this integration. It builds the checked-out
Java application, starts its JAR on port 9090, runs the scan and converter,
submits to `/VulnerableApp/scanner/benchmark`, and uploads artifacts even after a
failure. It does not push results to master or need repository write permissions.

## Scope and discovery

Targets come from the deployed Java `/VulnerableApp/scanner/dast` response.
Selection uses the three family names below; there is no per-level URL table.
Every selected URL must belong to the configured application origin and context.
The runner tests `GET ?id=1`, matching the current implementations' `id` query
parameter. A newly exposed unsupported method fails the run rather than being
silently treated as scanned. Any exposed `SECURE` rows are scanned as controls,
and a confirmed injection on one is retained for investigation.

Inventory at application revision `5a94f0d`:

| Family | Source levels | Exposed in live DAST ground truth | Method / parameter |
|---|---|---|---|
| `BlindSQLInjectionVulnerability` | 1–5 | 1–2, UNSECURE | GET / `id` |
| `ErrorBasedSQLInjectionVulnerability` | 1–5 | 1–4, UNSECURE | GET / `id` |
| `UnionBasedSQLInjectionVulnerability` | 1–9 | 1–2 UNSECURE; 3 SECURE | GET / `id` |

The other source levels are not advertised by `/scanner/dast`; this integration
does not invent additional ground-truth rows or claim to scan them. Recheck the
uploaded `ground-truth.json` and `targets.json` for each run's actual scope.
PHP currently has NoSQL challenges, not corresponding SQL-injection ground
truth, so it is excluded. The facade and JSP applications are not scan targets.

## Scan settings and limits

Each target uses a fresh session directory, `--flush-session`, `--batch`,
`--technique=BEU`, `--level=1`, `--risk=1`, and `--threads=1`.
BEU enables boolean-based blind, error-based, and UNION-query tests. Stacked,
time-based blind, inline-query techniques, elevated level/risk tests, tamper
scripts, authenticated scenarios, and data extraction are outside this run's
scope. A completed negative means no injection was confirmed by these settings;
it does not prove that a level is secure.

Requests time out after 5 seconds with no retry. Each scanner process has a
120-second deadline (configurable from 1–300 seconds), and the Actions job has a
30-minute limit. CI bounds scanner virtual memory to 2 GiB and the Java heap to
768 MiB. A target timeout makes the overall run incomplete; it is not submitted
to the comparator as a clean scan.

## Local run

Use only an instance you control. Start a fresh build of VulnerableApp separately:

```bash
./gradlew bootJar
java -Xmx768m -jar build/libs/VulnerableApp-1.0.0.jar --server.port=9090
```

Check out the pinned upstream scanner, then run:

```bash
git clone https://github.com/sqlmapproject/sqlmap.git /tmp/sqlmap-benchmark
git -C /tmp/sqlmap-benchmark checkout 65b6e7b4acb13a736614a1ab73b0908476f3c24a
python3 -m unittest discover -s benchmarks/sqlmap/scripts -v
python3 benchmarks/sqlmap/scripts/run_sqlmap.py \
  --base-url http://127.0.0.1:9090/VulnerableApp \
  --sqlmap-dir /tmp/sqlmap-benchmark \
  --output-dir benchmarks/sqlmap/artifacts
python3 benchmarks/sqlmap/scripts/convert_sqlmap_to_benchmark.py \
  --input benchmarks/sqlmap/artifacts/runs.json \
  --output benchmarks/sqlmap/artifacts/sqlmap-findings.json
curl --fail-with-body --show-error --max-time 60 \
  http://127.0.0.1:9090/VulnerableApp/scanner/benchmark \
  -H 'Content-Type: application/json' \
  --data-binary @benchmarks/sqlmap/artifacts/sqlmap-findings.json \
  -o benchmarks/sqlmap/artifacts/sqlmap-results.json
```

Use a new output directory for each run; the runner refuses to reuse an existing
one. Stop after a failed command and inspect artifacts before submitting.
Python's standard library is sufficient; no extra Python packages are needed.

## Reports and failure handling

sqlmap's native `--report-json` contains `meta`, `data`, and `error`. Confirmed
injections come only from complete `TECHNIQUES` records (type 1), with a nonempty
technique list and matching request location. The converter preserves the
original challenge URL and method, emits `cwe: "CWE-89"`, and deduplicates repeated
parameters and techniques at the same endpoint.

A local probe of this pinned release showed that both a completed negative and
a refused connection can produce exit code 0 and native JSON with
`success: true, data: [], error: []`. Consequently the runner also saves and
checks ERROR/CRITICAL diagnostics from the native console log. Only the pinned
scanner's explicit `all tested parameters do not appear to be injectable.`
conclusion is accepted as a completed negative. Other errors, tracebacks,
nonzero exits, missing or partial reports, inconsistent targets, or missing
planned scans fail conversion. Partial positive reports are retained as evidence
but are not submitted as a complete benchmark.

Artifacts include each native report, console log, full invocation and fresh
session; the combined run manifest; live ground truth and selected targets;
scanner version/revision; application revision, JAR checksum and startup log;
converted findings and the comparator response when submission succeeds.
The fixtures in `fixtures/` are native JSON captured from local positive and
negative probes of the pinned scanner; tests also cover failures and timeouts.

## Reading coverage

The comparator's `coverage` denominator is **all Java DAST ground truth**, not
just the selected SQL categories. In a local run against revision `5a94f0d`,
9 advertised endpoints were scanned (8 UNSECURE and 1 SECURE); sqlmap confirmed
6 endpoints and the comparator reported 6 detected out of 168 expected, 0
unmatched, and **3.57% whole-benchmark coverage**. Within the eight advertised
UNSECURE SQL rows, that run detected six (75%); this narrower fraction must not
be presented as whole-benchmark coverage. Error-based levels 3–4 were not
confirmed under the configured settings.

Results depend on scanner settings and application revision. The workflow
uploads fresh evidence rather than promising this count on every run. Unmatched
or out-of-scope findings are not automatically false positives.
