# Commix benchmark

This integration measures the Java OS command-injection challenges with
[Commix](https://github.com/commixproject/commix) in **DAST** mode. The
[GitHub Actions workflow](../../.github/workflows/commix-benchmark.yml) always
installs Commix's newest version-numbered release tag, so changes in results over
time reflect scanner improvements. Commix is GPLv3-or-later; it is fetched from
upstream and not bundled here.

The workflow runs weekly, manually, and on PRs changing this integration. It
starts the application with `docker-compose.without_llm.yml`, scans, converts the
Commix logs, submits them to `/VulnerableApp/scanner/benchmark` and uploads the
raw logs and results as artifacts. It does not push results or need repository
write permissions.

## Flow

```
GitHub Actions -> Commix CLI -> logs.txt -> convert_commix_to_benchmark.py -> /scanner/benchmark
```

## Target discovery

The `CommandInjection` URLs are read from the application's `sitemap.xml`; the
`/scanner/dast` ground truth is only used by the benchmark endpoint after the
scan. Commix's `-x` option only accepts sitemap URLs that already carry GET
parameters, and the sitemap lists none, so the workflow adds
`?ipaddress=127.0.0.1` to each URL and passes `-p ipaddress`. This parameter is
hardcoded for now.

## Scan settings

Each URL is scanned with `--technique=c` (classic results-based command
injection), `--batch`, `--timeout=10` and `--retries=0`. `--answers` keeps Commix
from opening a pseudo-terminal shell or waiting on a prompt.

## Conversion

Commix has no stable JSON report, so the converter reads its `logs.txt` files
(a fixed `[info] <field>: <value>` format) and emits a `COMMAND_INJECTION`
finding (CWE-77, WASC-31) for every injection point, with the query string
removed. Matching, de-duplication and method checks are left to the benchmark
endpoint.

## Local run

Use only an instance you control.

```bash
docker compose -f docker-compose.without_llm.yml up -d
git clone --depth 1 https://github.com/commixproject/commix.git /tmp/commix
```

Check out the newest version-numbered tag (`git -C /tmp/commix tag`), then for each
`CommandInjection` URL from `http://localhost/VulnerableApp/sitemap.xml`:

```bash
python3 /tmp/commix/commix.py --url "<url>?ipaddress=127.0.0.1" -p ipaddress \
  --technique=c --batch --ignore-stdin --disable-coloring --timeout=10 --retries=0 \
  --answers='shell=n,recognize=u,continue=N,skip=Y' \
  --output-dir=benchmarks/commix/artifacts/commix-output
python3 benchmarks/commix/scripts/convert_commix_to_benchmark.py \
  --input benchmarks/commix/artifacts/commix-output \
  --output benchmarks/commix/findings/commix-findings.json
curl --fail-with-body -X POST http://localhost/VulnerableApp/scanner/benchmark \
  -H 'Content-Type: application/json' \
  -d @benchmarks/commix/findings/commix-findings.json
```

Run the converter tests with `python3 -m unittest discover -s benchmarks/commix/scripts`.

## Reading coverage

The comparator's `coverage` denominator is all UNSECURE Java DAST ground truth,
not just command injection, so a command-injection-only scan reports a small
whole-benchmark percentage. Check `detected` and `unmatched` for this category.
