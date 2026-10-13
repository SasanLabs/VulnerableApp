# Analytics

Turns VulnerableApp's scanner benchmark results into a standardized dataset that
any tool can analyze or visualize. The dataset is the contract: the engine that
builds it and the page that draws it can each change without touching the other.

```
scanner outputs ──▶ benchmarks/<Tool>/*-results.json ──▶ analytics engine ──▶ analytics/data/benchmark/*.csv ──▶ docs/benchmark (Vega-Lite)
                    (existing benchmark workflows)        (Java, GitHub Action)  (Frictionless Data Package)     (GitHub Pages)
```

| Path | What it is |
|---|---|
| [`benchmark-sources.json`](benchmark-sources.json) | The scanner results the engine reads, one entry per benchmark. |
| [`challenge-mapping.json`](challenge-mapping.json) | Challenge names for SAST classes whose name differs from the challenge. |
| [`engine/`](engine) | The Java engine: reads results, maps findings to challenges, computes metrics, writes the dataset. |
| [`data/benchmark/`](data/benchmark) | The dataset, and its schema in [`datapackage.json`](data/benchmark/datapackage.json). |

The [Scanner Benchmark page](https://sasanlabs.github.io/VulnerableApp/benchmark/)
of the documentation site ([`docs/benchmark/`](../docs/benchmark)) draws the
dataset straight from this folder on `master`.

## The dataset

Two CSV files, described column by column in
[`datapackage.json`](data/benchmark/datapackage.json) (a
[Frictionless Data Package](https://specs.frictionlessdata.io/data-package/), so
tools such as `frictionless validate` can check it):

- **`runs.csv`**: one row per benchmark per day its results changed, with
  expected / detected / missed / unmatched counts and coverage. Rows accumulate,
  so this is the history.
- **`findings.csv`**: every item of each benchmark's latest result, with its
  status (`detected`, `missed` or `unmatched`), challenge, level, CWE and
  location. It is rewritten on every build; older versions are in git history.

Challenges are resolved from the URL for DAST (`/JWTVulnerability/LEVEL_3`) and
from the class name for SAST (`.../jwt/JWTVulnerability.java`), using
`challenge-mapping.json` where the two differ. CWE is the common key when
comparing scanners that name vulnerabilities differently.

`detected` rows require results that list `detectedItems`, which the
`/scanner/benchmark` endpoint does from this change on. Results written before
that only carry the detected *count*, so the page marks those benchmarks as
pending until their next run.

## Adding a scanner

Once a scanner has a benchmark workflow that commits a results file (see
[`benchmarks/README.md`](../benchmarks/README.md)), add it to
`benchmark-sources.json`:

```json
{
  "id": "nuclei",
  "scanner": "Nuclei",
  "variant": "default templates",
  "scanType": "DAST",
  "results": "benchmarks/Nuclei/nuclei-results.json"
}
```

No engine or page change is needed. The id joins rows across the two files, so
keep it stable. The page colors benchmarks in the order they are listed.

## Running the engine

The [Benchmark Analytics workflow](../.github/workflows/benchmark-analytics.yml)
runs it whenever a results file changes on `master` and commits the dataset. To
run it locally from the repository root:

```bash
./gradlew -p analytics/engine test
./gradlew -p analytics/engine run
```

`run` updates today's rows in `runs.csv` and rewrites `findings.csv`. Add
`--args="--backfill"` to rebuild the run history from every past commit of each
results file (the workflow offers this as a manual option too). Run dates are
the commit dates of the results files, so re-running is idempotent.

To preview the page against a dataset on a fork or branch, open it with
`?data=<url of the folder holding the CSVs>`.
