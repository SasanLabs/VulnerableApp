# Commix benchmark

This integration measures the Java OS command-injection challenges against a
controlled running instance. It uses upstream
[Commix](https://github.com/commixproject/commix) pinned to the release tag `v4.1`
(set as `COMMIX_TAG` in the workflow), so reruns of the same application revision
use the same scanner and untagged `master` code is never used. Update the tag in
the workflow and in the commands below together.
The runner checks that the scanner reports that tag as its stable version, and
the run artifacts record the reported version and the checked-out commit. Commix
is GPLv3-or-later; it is fetched from upstream and its source is not bundled
here. See upstream's licensing terms.

The [GitHub Actions workflow](../../.github/workflows/commix-benchmark.yml) runs
weekly, manually, and on PRs changing this integration. It builds the
checked-out Java application, starts its JAR on port 9090, runs the scan and
converter, submits to `/VulnerableApp/scanner/benchmark`, and uploads artifacts
even after a failure. It does not push results to master or need repository write
permissions.

## Scope and discovery

Targets come from the deployed Java `/VulnerableApp/scanner/dast` response.
Selection uses the `CommandInjection` family name; there is no per-level URL
table. Every selected URL must belong to the configured application origin and
context. The current implementation reads a single `ipaddress` query parameter
and shells out through `ProcessBuilder`, so the runner tests
`GET ?ipaddress=127.0.0.1`. A newly exposed unsupported method fails the run
rather than being silently treated as scanned. Any exposed `SECURE` rows are
scanned as controls, and a confirmed injection on one is retained for
investigation.

Inventory at application revision `02ee7c4`:

| Family | Source levels | Exposed in live DAST ground truth | Method / parameter |
|---|---|---|---|
| `CommandInjection` | 1–8 | 1–5 UNSECURE; 7–8 SECURE | GET / `ipaddress` |

Level 6 is not advertised by `/scanner/dast`; this integration does not invent
additional ground-truth rows or claim to scan it. Recheck the uploaded
`ground-truth.json` and `targets.json` for each run's actual scope. Only the Java
`CommandInjection` lab exercises a shell; other Java process-execution code and
the PHP, JSP and facade applications are not scan targets here.

## Scan settings and limits

Each target uses a fresh session directory, `--batch`, `--ignore-stdin`,
`--disable-coloring`, `--technique=c` (classic results-based command injection),
`--timeout=10` and `--retries=0`. Commix is driven non-interactively with
`--answers=shell=n,recognize=u,continue=N,skip=Y` so it never opens a
pseudo-terminal shell or waits on a prompt. Time-based blind, file-based,
dynamic-code and shellshock techniques, enumeration, file access and
post-exploitation are outside this run's scope. A completed negative means no
injection was confirmed by these settings; it does not prove that a level is
secure.

Detection is by **confirmed command execution**, not heuristics: each target is
scanned with `--os-cmd=echo <marker>`, where `<marker>` is a fresh random token
per target. A finding is recorded only when commix's log shows the target
returning that exact marker, which proves arbitrary command execution rather than
a reflected payload. Each scanner process has a 150-second deadline (configurable
from 1–600 seconds), and the Actions job has a 30-minute limit. CI bounds scanner
virtual memory to 2 GiB and the Java heap to 768 MiB. A target timeout makes the
overall run incomplete; it is not submitted to the comparator as a clean scan.

## Local run

Use only an instance you control. Start a fresh build of VulnerableApp
separately:

```bash
./gradlew bootJar
java -Xmx768m -jar build/libs/VulnerableApp-1.0.0.jar --server.port=9090
```

Check out the pinned tag, as the workflow does, then run:

```bash
tag=v4.1
git clone --depth 1 --branch "$tag" https://github.com/commixproject/commix.git /tmp/commix-benchmark
python3 -m unittest discover -s benchmarks/commix/scripts -v
python3 benchmarks/commix/scripts/run_commix.py \
  --base-url http://127.0.0.1:9090/VulnerableApp \
  --commix-dir /tmp/commix-benchmark \
  --commix-tag "$tag" \
  --output-dir benchmarks/commix/artifacts
python3 benchmarks/commix/scripts/convert_commix_to_benchmark.py \
  --input benchmarks/commix/artifacts/runs.json \
  --output benchmarks/commix/artifacts/commix-findings.json
curl --fail-with-body --show-error --max-time 60 \
  http://127.0.0.1:9090/VulnerableApp/scanner/benchmark \
  -H 'Content-Type: application/json' \
  --data-binary @benchmarks/commix/artifacts/commix-findings.json \
  -o benchmarks/commix/artifacts/commix-results.json
```

Use a new output directory for each run; the runner refuses to reuse an existing
one. Stop after a failed command and inspect artifacts before submitting.
Python's standard library is sufficient; no extra Python packages are needed.

## Reports and failure handling

Commix does not emit a stable JSON report, so `run_commix.py` parses each
target's `logs.txt` — a fixed `[info] <field>: <value>` format — into a small
structured report of injection points and `--os-cmd` executions, and keeps the
console's ERROR/CRITICAL lines as diagnostics. Both are stored in the combined
`runs.json` manifest, which is what the converter grades. The parser lives in
`commix_log.py` and is unit-tested directly.

The converter emits a finding only for a run whose structured report shows the
target echoing the run's unique marker; it records the original challenge URL and
method with `type: "COMMAND_INJECTION"` (CWE-77, WASC-31 — the ground truth uses
CWE-77, so the canonical type is used rather than substituting CWE-78) and
deduplicates repeated endpoints. Only commix's explicit
`All tested parameters do not appear to be injectable.` conclusion is accepted as
a completed negative. A run that neither confirms execution nor prints that
conclusion, any other error or traceback, a non-zero or timed-out scan, a missing
marker or report, or an inconsistent version or target count fails conversion, so
a release that changes this message or the log fields the converter checks fails
the run instead of producing a result.

Artifacts include each target's parsed report, console log, full invocation and
fresh session; the combined run manifest; live ground truth and selected targets;
scanner version and revision; application revision, JAR checksum and startup log;
converted findings and the comparator response when submission succeeds. The
fixtures in `fixtures/` are real `logs.txt` files captured from local positive
and negative probes of commix `v4.1`; tests also cover failures and timeouts.

## Reading coverage

The comparator's `coverage` denominator is **all UNSECURE Java DAST ground
truth**, counted once per URL, method and vulnerability type, not just the
command-injection category. In a local commix `v4.1` run against revision
`02ee7c4`, 7 advertised `CommandInjection` endpoints were scanned (5 UNSECURE and
2 SECURE); commix confirmed execution on all 5 UNSECURE endpoints and none of the
2 SECURE controls. Within the command-injection category that is full coverage;
this narrower fraction must not be presented as whole-benchmark coverage.

Results depend on the commix release, scanner settings and application revision.
The workflow uploads fresh evidence rather than promising this count on every
run. Unmatched or out-of-scope findings are not automatically false positives.
