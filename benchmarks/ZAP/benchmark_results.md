# ZAP Benchmark Results — VulnerableApp

> This file is auto-updated by the ZAP Benchmark GithHub Actions workflows 
> [zap-benchmark-modern.yml](../../.github/workflows/zap-benchmark-modern.yml) and [zap-benchmark-legacy.yml](../../.github/workflows/zap-benchmark-legacy.yml)
> every Monday and on every manual workflow run.

| Metric | Value |
|---|---|
| Tool | ZAP by Checkmarx |
| Scan Type | DAST |
| Mode | Insane (strength: INSANE, threshold: LOW) |
| Total ground-truth vulnerabilities | 140 |
| **Detected by ZAP (Modern UI)** | **0** |
| **Detected by ZAP (Legacy UI)** | **0** |
| Missed (Modern UI) | 140 |
| Missed (Legacy UI) | 140 |
| **Coverage (Modern UI)** | **0.00%** |
| **Coverage (Legacy UI)** | **0.00%** |
| Unmatched (Modern UI) | 28 |
| Unmatched (Legacy UI) | 47 |
| Date | 2026-06-13 |

## Why coverage is not 100%

The following vulnerability categories are outside ZAP's detection capability
with standard rules regardless of scan strength:

| Category | Reason |
|---|---|
| JWT vulnerabilities | Requires semantic token analysis |
| Cryptographic failures | Server-side logic, not observable via HTTP |
| Web cache poisoning | No CWE/WASC mapping available |

## Modern UI vs Legacy UI
 
| Aspect | Modern UI | Legacy UI |
|---|---|---|
| Entry point | `http://VulnerableApp-facade` | `http://VulnerableApp-facade/VulnerableApp` |
| Ajax Spider | firefox-headless | chrome-headless |

## Reproducing these results

See [how_to_run_zap.md](./how_to_run_zap.md) for the full end-to-end guide, or re-run this
GitHub Action via the **Actions** tab → **ZAP Benchmark Modern** AND **ZAP Benchmark Legacy** → **Run workflow**.