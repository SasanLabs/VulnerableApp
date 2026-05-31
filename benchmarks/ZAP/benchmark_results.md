# ZAP Benchmark Results — VulnerableApp

> This file is auto-updated by the [ZAP Benchmark GitHub Action](../../.github/workflows/zap-benchmark.yml)
> every Monday and on every manual workflow run.

| Metric | Value |
|---|---|
| Tool | OWASP ZAP by Checkmarx |
| Scan Type | DAST |
| Mode | Insane (strength: INSANE, threshold: LOW) |
| UI | Modern UI (VulnerableApp-facade) |
| Total ground-truth vulnerabilities | 140 |
| **Detected by ZAP** | **0** |
| Missed by ZAP | 140 |
| **Coverage** | **0.00%** |
| Unmatched (outside benchmark scope) | 15 |
| Date | 2026-05-31 |

## Why coverage is not 100%

The following vulnerability categories are outside ZAP's detection capability
with standard rules regardless of scan strength:

| Category | Reason |
|---|---|
| JWT vulnerabilities | Requires semantic token analysis |
| Cryptographic failures | Server-side logic, not observable via HTTP |
| Web cache poisoning | No CWE/WASC mapping available |

## Reproducing these results

See [how_to_run_zap.md](./how_to_run_zap.md) for the full end-to-end guide, or re-run this
GitHub Action via the **Actions** tab → **ZAP Benchmark** → **Run workflow**.