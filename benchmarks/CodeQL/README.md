## CodeQL

CodeQL is benchmarked in SAST mode against the VulnerableApp source tree.

### Latest results

| Query suite              | Results file                                                   |
|--------------------------|----------------------------------------------------------------|
| `java-security-extended` | [`benchmarks/CodeQL/codeql-results.json`](codeql-results.json) |

Results are auto-updated on `master` every day and on every manual workflow
run. Each run installs the pinned CodeQL bundle (see "Pinned versions") and
prints the versions in the run log.

### Running the benchmark

The full pipeline (build the VulnerableApp image from the checkout, start the
Docker stack like the Semgrep benchmark, create the CodeQL database, analyze,
convert, benchmark, commit results) is automated via GitHub Actions:

1. Open the **Actions** tab and pick **CodeQL Benchmark**
2. Click **Run workflow**
3. On `master`, results are committed automatically to the file above, along
   with the converted findings in `benchmarks/CodeQL/findings/codeql-findings.json`.
   On other branches, nothing is committed; download the
   `codeql-benchmark-artifacts` artifact of the run instead. It holds the raw
   SARIF report, the converted findings, the results, the tool versions, the
   list of queries, and the build, analysis and application logs.

See [`.github/workflows/codeql-benchmark.yml`](../../.github/workflows/codeql-benchmark.yml)
for the full workflow definition.

To run it locally:

1. Create a CodeQL database and analyze it from the repository root, so that
   reported paths are project-relative. That is what the SAST matcher compares.

   ```bash
   codeql database create <database> --language=java --source-root=. \
       --command="./gradlew --no-daemon clean build -x test --no-build-cache"

   codeql database analyze <database> \
       codeql/java-queries:codeql-suites/java-security-extended.qls \
       --format=sarif-latest --output=benchmarks/CodeQL/codeql-raw-report.json
   ```

2. Convert the report with the script described below.
3. Start VulnerableApp from the same checkout (`./gradlew bootRun`, port 9090),
   so that the ground truth it serves matches the sources that were scanned.
4. Post the findings and save the response:

   ```bash
   curl -s -X POST http://localhost:9090/VulnerableApp/scanner/benchmark \
       -H "Content-Type: application/json" \
       -d @benchmarks/CodeQL/findings/codeql-findings.json \
       -o benchmarks/CodeQL/codeql-results.json
   ```

### Pinned versions and limits

| Item                              | Value                                                                                                                                                  |
|-----------------------------------|--------------------------------------------------------------------------------------------------------------------------------------------------------|
| CodeQL bundle (CLI + query packs) | `latest version` (`codeql-bundle-linux64.tar.gz`)                                                                                                      |
| Query suite                       | `codeql/java-queries:codeql-suites/java-security-extended.qls`, from the pinned bundle                                                                 |
| JDK                               | Temurin 17                                                                                                                                             |
| Build command                     | `./gradlew --no-daemon clean build -x test --no-build-cache` (the workflow also skips the Spotless formatting tasks, which are irrelevant to analysis) |
| Source and ground truth           | The same checkout: the image served by the app is built from it, and the commit is recorded in `tool-versions.txt`                                     |
| Bounds                            | 2 threads, 4096 MB for CodeQL, 30 minutes per CodeQL step, 60 minutes per job                                                                          |

The CodeQL CLI is used here on an open-source repository. Its use is covered by
the [GitHub CodeQL Terms and Conditions](https://github.com/github/codeql-cli-binaries/blob/main/LICENSE.md);
check them before using it on anything else.

### Conversion script

CodeQL's SARIF report must be converted to the benchmark input format before
posting to the endpoint:

```bash
python3 benchmarks/CodeQL/scripts/convert_codeql_to_benchmark.py \
    --input  benchmarks/CodeQL/codeql-raw-report.json \
    --output benchmarks/CodeQL/findings/codeql-findings.json
```

The script emits one finding per CWE of each result: the path, the start line
of the primary location, and the CWE ID taken from the rule's `external/cwe/...`
tags (or its `cwe.mitre.org` help link). CodeQL reports no vulnerability type,
so none is emitted and matching relies on the CWE. A result whose rule has no
CWE is skipped with a warning, and a report from an unsuccessful analysis is
refused. Reported lines are never moved to an expected line to earn credit.

### Queries and benchmark categories

The suite runs the Java security queries of the pinned bundle. The list of the
queries that ran is saved as `codeql-queries.txt` in the workflow artifacts.
The table maps benchmark categories to queries that target the same weakness.
It says that a query exists, not that it detects the issue at the expected
line; the benchmark result is the only measure of that.

| Benchmark category                              | CodeQL query (CWE)                                                                                                          |
|-------------------------------------------------|-----------------------------------------------------------------------------------------------------------------------------|
| SQL injection (blind, error based, union based) | `java/sql-injection` (CWE-89), `java/concatenated-sql-query` (CWE-89)                                                       |
| Cross-site scripting (reflected, persistent)    | `java/xss` (CWE-79)                                                                                                         |
| Command injection                               | `java/command-line-injection` (CWE-78)                                                                                      |
| Path traversal, file upload paths               | `java/path-injection` (CWE-22)                                                                                              |
| XML external entities                           | `java/xxe` (CWE-611)                                                                                                        |
| SSRF, remote file inclusion                     | `java/ssrf` (CWE-918)                                                                                                       |
| Open redirect                                   | `java/unvalidated-url-redirection` (CWE-601)                                                                                |
| Weak cryptography and randomness                | `java/weak-cryptographic-algorithm` (CWE-327), `java/insecure-randomness` (CWE-330), `java/insufficient-key-size` (CWE-326) |
| JWT signature checks                            | `java/missing-jwt-signature-check` (CWE-347)                                                                                |
| LDAP injection, header injection                | `java/ldap-injection` (CWE-90), `java/netty-http-response-splitting` and `java/http-response-splitting` (CWE-113)           |

Categories with no query in the suite are not detected and are not claimed to
be: for example IDOR, clickjacking, password reset and session management flaws,
username enumeration, web cache poisoning, and the configuration-style JWT
issues. Their expected rows appear in `missedItems`.

### Reading the results

- **Matching is exact on the line.** A finding on another line of the same data
  flow gets no credit. The expected row shows up in `missedItems`, and the
  finding in `unmatchedItems`.
- **Two kinds of coverage.** `coverage` is `detected / totalExpected`, over the
  whole benchmark, including categories CodeQL has no query for. Coverage over
  only the supported categories is higher, and can be computed from
  `missedItems` by ignoring the unsupported categories listed above.
- **Unmatched is not false positive.** A rule that lists several CWEs gives one
  finding per CWE, so a correct hit on an expected line can leave its sibling
  CWE IDs in `unmatchedItems`. Findings on `Variant.SECURE` levels, on classes
  without ground-truth rows, and on logging or cookie checks outside the
  intended vulnerability set also end up there.
---