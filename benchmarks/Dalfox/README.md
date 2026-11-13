# Dalfox benchmark

This integration measures the Java reflected-XSS challenges against a controlled
running instance with [Dalfox](https://github.com/hahwul/dalfox) (MIT), pinned to
release `3.3.0` (`DALFOX_VERSION` in the workflow). The workflow downloads the
upstream `linux-x86_64` release archive and verifies its SHA-256
(`DALFOX_SHA256`) before use, and the runner checks that the binary reports the
pinned version. Update the version, the checksum and the commands below together.

The [GitHub Actions workflow](../../.github/workflows/dalfox-benchmark.yml) runs
weekly, manually, and on PRs changing this integration. It builds the checked-out
Java application, starts its JAR on port 9090, runs the scan and converter,
submits to `/VulnerableApp/scanner/benchmark`, and uploads artifacts even after a
failure. It does not push results to master or need repository write permissions.

## Scope and discovery

Targets come from the deployed Java `/VulnerableApp/scanner/dast` response.
Selection uses the challenge family name; there is no per-level URL table, so new
levels are picked up automatically. `/scanner/dast` does not name the input
parameter, so the runner keeps one small family table with the parameter and the
seed value the legacy challenge page sends:

| Family | Exposed in live DAST ground truth | Method / parameter | Seed |
|---|---|---|---|
| `XSSInImgTagAttribute` | 1–5 UNSECURE; 6 SECURE | GET / `src` | `/VulnerableApp/images/OWASP.png` |
| `XSSWithHtmlTagInjection` | 1–3 UNSECURE; 4–5 SECURE | GET / `value` | `vulnerableapp` |

Inventory at application revision `2e68df7`. The image seed follows the deployed
context path; it is required because levels 5 and 6 reject any other value. Every
selected URL must belong to the configured application origin and context. A
newly exposed unsupported method, protocol or variant, or a supported family that
disappears from the ground truth, fails the run rather than being silently
skipped. `SECURE` rows are scanned as controls.

The scans exercise the **backend** challenge endpoints directly, not the facade or
the legacy HTML pages that load them. Those pages insert each response into the
DOM with `innerHTML`, so the runner requests the endpoints with
`Accept: text/html`. Without it Spring answers `text/plain`, which Dalfox, like a
browser, correctly treats as inert. Before scanning, the runner sends the seeded
request and records whether it was answered as HTML and reflected the seed.
Dalfox reports a target whose baseline request is rejected (for example HTTP 400)
as `clean`, so a seed that does not reach the sink fails conversion instead.

Out of scope:

- `PersistentXSSInHTMLTagVulnerability` (stored XSS, `PERSISTENT_XSS` ground
  truth). Each request stores its `comment` value and renders every post stored
  for that level. A response therefore cannot separate a stored payload from a
  reflected one, and payloads persist across targets and runs because there is no
  reset. It is not scanned until a reproducible write, read and reset flow exists.
- DOM-based XSS. No Java challenge is exposed as `DOM_BASED_XSS`. Dalfox's AST
  results (`A`) are kept as observations, not detections.
- `UnrestrictedFileUpload` levels, whose ground truth also lists XSS types. They
  need a multipart upload followed by a fetch of the stored file, which this
  runner does not perform.
- `CachePoisoning` levels, which also list `REFLECTED_XSS`. Their payload reaches
  other users through a poisoned shared cache (the `X-Forwarded-Host` header and
  the `banner` parameter), so scoring them needs a poison-then-fetch flow rather
  than a single reflected request.
- The PHP application. Its `FileUpload/LEVEL_11` serves uploaded active content,
  but its ground truth labels it as file upload (CWE-434), not XSS, so there is
  no PHP XSS ground truth to score against.

## Scan settings and limits

Each target is scanned by its own Dalfox process so that every finding in a
report belongs to exactly one ground-truth URL. The runner passes
`--param <name>:query` with the seeded URL, `--skip-mining` and
`--skip-reflection-header/-cookie/-path` (reflections in places the row does not
describe cannot be attributed to it), `--skip-waf-probe` (there is no WAF in front
of the lab), `--workers 4`, `--max-concurrent-targets 1` and `--timeout 10`.
Dalfox's `--scan-timeout` defaults to 180 seconds per target and the runner's
process deadline to 240 seconds. The Actions job has a 30-minute limit, and CI
bounds the scanner's virtual memory to 2 GiB and the Java heap to 768 MiB. A
target that hits a deadline makes the run incomplete. It is never submitted as a
clean scan.

## Local run

Use only an instance you control. Start a fresh build of VulnerableApp
separately:

```bash
./gradlew bootJar
java -Xmx768m -jar build/libs/VulnerableApp-1.0.0.jar --server.port=9090
```

Download the pinned release for your platform from the
[Dalfox releases page](https://github.com/hahwul/dalfox/releases/tag/v3.3.0) and
check it against the `.sha256` file published with it. Then run:

```bash
version=3.3.0
python3 -m unittest discover -s benchmarks/Dalfox/scripts -v
python3 benchmarks/Dalfox/scripts/run_dalfox.py \
  --base-url http://127.0.0.1:9090/VulnerableApp \
  --dalfox /path/to/dalfox \
  --dalfox-version "$version" \
  --output-dir benchmarks/Dalfox/artifacts
python3 benchmarks/Dalfox/scripts/convert_dalfox_to_benchmark.py \
  --input benchmarks/Dalfox/artifacts/runs.json \
  --output benchmarks/Dalfox/artifacts/dalfox-findings.json \
  --observations benchmarks/Dalfox/artifacts/observations.json
curl --fail-with-body --show-error --max-time 60 \
  http://127.0.0.1:9090/VulnerableApp/scanner/benchmark \
  -H 'Content-Type: application/json' \
  --data-binary @benchmarks/Dalfox/artifacts/dalfox-findings.json \
  -o benchmarks/Dalfox/artifacts/dalfox-results.json
```

Use a new output directory for each run; the runner refuses to reuse an existing
one. Stop after a failed command and inspect artifacts before submitting.
Python's standard library is sufficient; no extra Python packages are needed.

## Reports and failure handling

Each target's native Dalfox JSON report (`-f json`) is stored in the combined
`runs.json` manifest together with the planned target, seed URL, preflight
result and exit code. The converter grades that manifest:

- A finding is submitted only when Dalfox's tier is `V` (Vulnerable: the payload
  was found in an executable position of the parsed response), its CWE is
  `CWE-79`, and its proof-of-concept URL is the scanned endpoint. It is recorded
  with the original challenge URL and method and `type: "REFLECTED_XSS"`, the
  canonical type of every in-scope ground-truth row (CWE-79, WASC-8).
  Repeated endpoints are deduplicated.
- `R` (reflection only, not confirmed exploitable), `A` (AST) and `I`
  (informational) results, and confirmed results whose proof of concept points to
  another URL, are written to the optional `--observations` file and never
  submitted.
- A run fails conversion if any target is `skipped` or `incomplete`, Dalfox
  marks the report `incomplete`, the exit code is not 0 (no findings) or 1
  (findings), or the exit code, target status and finding count disagree. It also
  fails if the report describes a different target or Dalfox version, or the
  process times out or writes no report. A Dalfox release that changes these
  fields therefore fails the run instead of producing a result.

Artifacts include each target's native report, console log and full invocation;
the combined run manifest; live ground truth and selected targets; the Dalfox
version and archive checksum; application revision, JAR checksum and startup
log; converted findings, observations, and the comparator response when
submission succeeds. The fixtures in `fixtures/` are real Dalfox `3.3.0` reports
captured from local confirmed, reflection-only, clean and unreachable-target
scans.

`findings/dalfox-findings.json` and `dalfox-results.json` hold the results of a
local run against revision `2e68df7`.

## Reading coverage

The comparator's `coverage` denominator is **all UNSECURE Java DAST ground
truth**, counted once per URL, method and vulnerability type, not just the
reflected-XSS category. In a local Dalfox `3.3.0` run against revision `2e68df7`,
11 endpoints were scanned (8 UNSECURE, 3 SECURE). Dalfox confirmed XSS on 6 of
the 8 UNSECURE endpoints (`XSSInImgTagAttribute` levels 1–3 and
`XSSWithHtmlTagInjection` levels 1–3) and on none of the SECURE controls, with 0
unmatched findings:

- `XSSInImgTagAttribute/LEVEL_4` produced only a reflection-only observation.
- `XSSInImgTagAttribute/LEVEL_5` was reported clean.
- `XSSWithHtmlTagInjection/LEVEL_4` (SECURE) produced a reflection-only
  observation. Its escaped output is not exploitable, which is why such results
  are not submitted.

That is 6 of 8 within the supported category. This narrower fraction must not be
presented as whole-benchmark coverage.

Results depend on the Dalfox release, scanner settings and application revision.
The workflow uploads fresh evidence rather than promising this count on every
run. Unmatched or out-of-scope findings are not automatically false positives.
