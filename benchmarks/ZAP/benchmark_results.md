# ZAP Benchmark Results — VulnerableApp

> This file is auto-updated by the [ZAP Benchmark GitHub Action](../../.github/workflows/zap-benchmark.yml)
> every Monday and on every manual workflow run.

| Metric | Value |
|---|---|
| Tool | OWASP ZAP by Checkmarx |
| Scan Type | DAST |
| Mode | Insane (strength: INSANE, threshold: LOW) |
| UI | Modern UI (VulnerableApp-facade) |
| Total ground-truth vulnerabilities | TBD |
| **Detected by ZAP** | **TBD** |
| Missed by ZAP | TBD |
| **Coverage** | **TBD** |
| Unmatched (outside benchmark scope) | TBD |
| Date | TBD |

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