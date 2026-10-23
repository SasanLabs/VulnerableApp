# SpotBugs with FindSecBugs Benchmark

SpotBugs, enhanced with the FindSecBugs security rules plugin, is benchmarked in
SAST mode against VulnerableApp's compiled bytecode.

SpotBugs runs as a standalone CLI tool without requiring any scanner plugins or
dependencies inside VulnerableApp's core `build.gradle`.

---

## Latest Results

| Tool | Results File | Raw XML Report |
|---|---|---|
| SpotBugs + FindSecBugs | [`benchmarks/SpotBugs/spotbugs-results.json`](spotbugs-results.json) | [`benchmarks/SpotBugs/spotbugs-raw-report.xml`](spotbugs-raw-report.xml) |

Results are auto-updated on `master` every day and on every manual workflow run.

---

## Running the Benchmark

The full pipeline (build the VulnerableApp image from the checkout, start the
Docker stack, run standalone SpotBugs with FindSecBugs, convert, benchmark, commit
results and raw XML) is automated via GitHub Actions:

1. Open the **Actions** tab and pick **SpotBugs Benchmark**
2. Click **Run workflow**
3. On `master`, results and raw reports are committed automatically:
   - `benchmarks/SpotBugs/spotbugs-results.json`
   - `benchmarks/SpotBugs/findings/spotbugs-findings.json`
   - `benchmarks/SpotBugs/spotbugs-raw-report.xml`
   On other branches, nothing is committed; download the
   `spotbugs-benchmark-artifacts` artifact of the run instead.

See [`.github/workflows/spotbugs-benchmark.yml`](../../.github/workflows/spotbugs-benchmark.yml)
for the full workflow definition.

---

## Running Locally

To run the benchmark locally:

### 1. Run the standalone SpotBugs scan

You can use the helper script which compiles Java classes, prepares standalone SpotBugs CLI and the FindSecBugs plugin, and generates the raw XML report:

```bash
python3 benchmarks/SpotBugs/scripts/run_spotbugs.py
```

This outputs the raw XML report to `benchmarks/SpotBugs/spotbugs-raw-report.xml`.

Alternatively, if you already have the SpotBugs CLI installed:

```bash
# Compile classes
./gradlew compileJava

# Run SpotBugs CLI with FindSecBugs plugin
spotbugs -textui -xml:withMessages \
    -output benchmarks/SpotBugs/spotbugs-raw-report.xml \
    -pluginList /path/to/findsecbugs-plugin-1.13.0.jar \
    -effort:max -low \
    -auxclasspathFromFile <(./gradlew -q -I benchmarks/SpotBugs/scripts/print_classpath_init.gradle printClasspath) \
    build/classes/java/main
```

### 2. Convert the report to benchmark input format

Convert the XML report to benchmark input format using the conversion script:

```bash
python3 benchmarks/SpotBugs/scripts/convert_spotbugs_to_benchmark.py \
    --input  benchmarks/SpotBugs/spotbugs-raw-report.xml \
    --output benchmarks/SpotBugs/findings/spotbugs-findings.json
```

### 3. Start VulnerableApp

Start VulnerableApp from the same checkout:

```bash
./gradlew bootRun
```

### 4. Submit findings to the benchmark endpoint

Post the converted findings to VulnerableApp's benchmark endpoint:

```bash
curl -s -X POST http://localhost:9090/VulnerableApp/scanner/benchmark \
    -H "Content-Type: application/json" \
    -d @benchmarks/SpotBugs/findings/spotbugs-findings.json \
    -o benchmarks/SpotBugs/spotbugs-results.json
```

---

## Conversion Script & Ground-Truth Alignment

The conversion script (`benchmarks/SpotBugs/scripts/convert_spotbugs_to_benchmark.py`)
parses `<BugInstance>` elements from SpotBugs' XML report, resolves CWE identifiers
and vulnerability descriptions, normalises file paths, and maps reported lines to
VulnerableApp's SAST ground truth.

### Bytecode line resolution

In compiled Java bytecode, method invocations spanning multiple lines (such as
`applicationJdbcTemplate.query(\n "select * ...")`) attribute the `INVOKEVIRTUAL`
instruction to the line where the method call begins. VulnerableApp's SAST ground
truth (`scanner/sast/expectedIssues.csv`) expects the exact line where the SQL query
expression is defined.

The converter checks the source code for multiline sink calls (e.g. `query(`,
`prepareStatement(`, `execute(`, `executeUpdate(`, `batchUpdate(`) and aligns
the finding to the query argument line so that valid vulnerabilities are correctly
credited by the SAST benchmark matcher.
