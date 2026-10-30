# PMD (SAST)

[PMD](https://pmd.github.io/) is benchmarked in `SAST` mode against the
ground truth in `expectedIssues.csv`. PMD's Java security rule set is small, so
a low (even zero) coverage figure is an expected result, not a bug in the
integration.

## Files

| File | Purpose |
|---|---|
| [`pmd-rule-cwe-mapping.csv`](pmd-rule-cwe-mapping.csv) | The PMD rules to run and the CWE / type each one maps to |
| [`scripts/convert_pmd_to_benchmark.py`](scripts/convert_pmd_to_benchmark.py) | Converts PMD's JSON report into the benchmark input format |
| [`findings/pmd-findings.json`](findings/pmd-findings.json) | Latest converted PMD findings (benchmark input) |
| [`pmd-results.json`](pmd-results.json) | Latest benchmark results |

The findings and results files are updated daily and on every manual workflow
run.

## Running the benchmark

The full pipeline (build and start VulnerableApp → run PMD → convert →
benchmark → commit findings and results) is automated via GitHub Actions:

1. Go to **Actions** tab → **PMD Benchmark**
2. Click **Run workflow** (optionally enter a PMD version; the default is the
   latest PMD release)
3. The findings and results are committed automatically to the files above

See [`.github/workflows/pmd-benchmark.yml`](../../.github/workflows/pmd-benchmark.yml)
for the full workflow definition. The app is built from the same checkout that
PMD scans, so the ground-truth line numbers always match the scanned code. Runs
on other branches or in forks upload the findings and results as an artifact
but do not commit them.

To run it manually instead, follow the three steps below from the repository
root.

### 1. Run PMD

Download the latest PMD 7.x from the [releases page](https://github.com/pmd/pmd/releases).
The rules passed to `-R` are built from `pmd-rule-cwe-mapping.csv`:

```bash
pmd check -d src/main/java \
    -R "$(python3 benchmarks/PMD/scripts/convert_pmd_to_benchmark.py --print-rulesets)" \
    -f json -r benchmarks/PMD/pmd-raw-report.json
```

Use a relative `-d` so reported paths are project-relative (`src/main/java/...`).

### 2. Convert the output

```bash
python3 benchmarks/PMD/scripts/convert_pmd_to_benchmark.py \
    --input  benchmarks/PMD/pmd-raw-report.json \
    --output benchmarks/PMD/findings/pmd-findings.json
```

Self-test: `python3 benchmarks/PMD/scripts/convert_pmd_to_benchmark.py --test`.

### 3. Submit to the benchmark

```bash
curl -X POST http://localhost:9090/VulnerableApp/scanner/benchmark \
    -H "Content-Type: application/json" \
    -d @benchmarks/PMD/findings/pmd-findings.json
```

The response lists detected, missed and unmatched findings. In a run with PMD
7.28.0 against `src/main/java`, PMD reported 5 findings, none of which matched
the ground truth.

## Rule → CWE mapping

PMD reports contain no CWE IDs, so the converter adds them from
[`pmd-rule-cwe-mapping.csv`](pmd-rule-cwe-mapping.csv). To scan with an extra
rule, add a row with its PMD category (`security`, `bestpractices`,
`errorprone`, ...), rule name, CWE and a type label; both the scan and the
mapping pick it up. Current rules:

| Category | PMD rule | CWE | Type label |
|---|---|---|---|
| security | `HardCodedCryptoKey` | CWE-321 | Hard-coded Cryptographic Key |
| security | `InsecureCryptoIv` | CWE-329 | Insecure Crypto IV |
| bestpractices | `AvoidUsingHardCodedIP` | CWE-547 | Hard-coded Security-relevant Constant |
| bestpractices | `AvoidPrintStackTrace` | CWE-209 | Information Exposure Through Error Message |
| errorprone | `CloseResource` | CWE-772 | Missing Resource Release |
| errorprone | `AvoidLosingExceptionInformation` | CWE-390 | Error Condition Without Action |

Rules that are not in the CSV are still emitted, with the rule name as `type`
and no `cwe`. Absolute paths are made project-relative (override the root with
`--root`). A malformed or truncated report (e.g. from a failed `pmd check` run)
exits with a clear error and a non-zero status instead of a traceback.

## Input format with CWE

PMD's own JSON report only names the rule:

```json
{
  "files": [
    {
      "filename": "src/main/java/org/sasanlabs/internal/utility/GenericUtils.java",
      "violations": [ { "beginline": 10, "rule": "AvoidUsingHardCodedIP" } ]
    }
  ]
}
```

The converted benchmark input adds the CWE and type (real output, abridged):

```json
{
  "tool": "PMD",
  "scanType": "SAST",
  "findings": [
    {
      "filePath": "src/main/java/org/sasanlabs/internal/utility/GenericUtils.java",
      "line": 10,
      "cwe": "CWE-547",
      "type": "Hard-coded Security-relevant Constant"
    },
    {
      "filePath": "src/main/java/org/sasanlabs/service/vulnerability/xxe/XXEVulnerability.java",
      "line": 108,
      "cwe": "CWE-772",
      "type": "Missing Resource Release"
    }
  ]
}
```