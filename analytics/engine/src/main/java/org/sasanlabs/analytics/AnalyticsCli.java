package org.sasanlabs.analytics;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.sasanlabs.analytics.io.Csv;
import org.sasanlabs.analytics.io.GitHistory;
import org.sasanlabs.analytics.model.BenchmarkResult;
import org.sasanlabs.analytics.model.BenchmarkSource;
import org.sasanlabs.analytics.model.FindingRow;
import org.sasanlabs.analytics.model.RunRow;

/**
 * Builds the benchmark dataset from the scanner results files listed in the sources file.
 *
 * <ul>
 *   <li>{@code runs.csv} gains (or updates) one row per benchmark for the day its results file was
 *       last committed, so it accumulates history across runs.
 *   <li>{@code findings.csv} is rewritten with every item of each benchmark's latest result.
 * </ul>
 *
 * <p>With {@code --backfill}, {@code runs.csv} is also filled from every past revision of each
 * results file in git history.
 */
public final class AnalyticsCli {

    private static final String USAGE =
            "Usage: AnalyticsCli [--sources <file>] [--mapping <file>] [--out <dir>]"
                    + " [--backfill]\n"
                    + "  --sources   benchmark sources file (default:"
                    + " analytics/benchmark-sources.json)\n"
                    + "  --mapping   challenge mapping file (default:"
                    + " analytics/challenge-mapping.json)\n"
                    + "  --out       output directory (default: analytics/data/benchmark)\n"
                    + "  --backfill  also rebuild run history from git history of the results"
                    + " files";

    private AnalyticsCli() {}

    public static void main(String[] args) throws IOException {
        Path sourcesFile = Path.of("analytics/benchmark-sources.json");
        Path mappingFile = Path.of("analytics/challenge-mapping.json");
        Path outDir = Path.of("analytics/data/benchmark");
        boolean backfill = false;
        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "--sources" -> sourcesFile = Path.of(requireValue(args, ++i));
                case "--mapping" -> mappingFile = Path.of(requireValue(args, ++i));
                case "--out" -> outDir = Path.of(requireValue(args, ++i));
                case "--backfill" -> backfill = true;
                default -> {
                    System.err.println(USAGE);
                    System.exit(2);
                }
            }
        }

        Path repoRoot = Path.of("").toAbsolutePath();
        GitHistory git = new GitHistory(repoRoot);
        List<BenchmarkSource> sources = readSources(sourcesFile);
        Map<String, String> sastClassToChallenge = readSastClassToChallenge(mappingFile);

        List<RunRow> runUpdates = new ArrayList<>();
        List<FindingRow> findings = new ArrayList<>();
        for (BenchmarkSource source : sources) {
            if (backfill) {
                runUpdates.addAll(historicalRuns(git, source));
            }
            Path resultsFile = repoRoot.resolve(source.results());
            if (!Files.exists(resultsFile)) {
                System.err.println("WARN: skipping " + source.id() + ", missing " + resultsFile);
                continue;
            }
            BenchmarkResult result =
                    BenchmarkAnalytics.parseResult(
                            Files.readString(resultsFile, StandardCharsets.UTF_8));
            GitHistory.Revision latest = git.latest(source.results());
            String runDate =
                    latest != null ? latest.date() : LocalDate.now(ZoneOffset.UTC).toString();
            String commit = latest != null ? shortCommit(latest.commit()) : "";
            runUpdates.add(BenchmarkAnalytics.toRun(source, result, runDate, commit));
            findings.addAll(
                    BenchmarkAnalytics.toFindings(source, result, runDate, sastClassToChallenge));
        }

        Path runsFile = outDir.resolve("runs.csv");
        List<RunRow> existing = new ArrayList<>();
        for (List<String> cells : Csv.readRows(runsFile)) {
            existing.add(RunRow.fromCells(cells));
        }
        List<RunRow> runs = BenchmarkAnalytics.mergeRuns(existing, runUpdates);
        Csv.write(runsFile, RunRow.HEADER, runs.stream().map(RunRow::toCells).toList());

        Path findingsFile = outDir.resolve("findings.csv");
        Csv.write(
                findingsFile,
                FindingRow.HEADER,
                findings.stream().map(FindingRow::toCells).toList());

        System.out.printf(
                "Wrote %d run rows to %s and %d finding rows to %s%n",
                runs.size(), runsFile, findings.size(), findingsFile);
    }

    /** One run per day from the git history of the results file, keeping each day's last run. */
    private static List<RunRow> historicalRuns(GitHistory git, BenchmarkSource source) {
        Map<String, RunRow> byDate = new LinkedHashMap<>();
        for (GitHistory.Revision revision : git.revisions(source.results())) {
            if (byDate.containsKey(revision.date())) {
                continue; // revisions are newest first, so the day's last run is already kept
            }
            try {
                BenchmarkResult result =
                        BenchmarkAnalytics.parseResult(
                                git.show(revision.commit(), source.results()));
                byDate.put(
                        revision.date(),
                        BenchmarkAnalytics.toRun(
                                source, result, revision.date(), shortCommit(revision.commit())));
            } catch (IOException e) {
                System.err.println(
                        "WARN: skipping "
                                + source.results()
                                + " at "
                                + revision.commit()
                                + ": "
                                + e.getMessage());
            }
        }
        return new ArrayList<>(byDate.values());
    }

    static List<BenchmarkSource> readSources(Path file) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree(file.toFile());
        return mapper.convertValue(
                root.path("benchmarks"), new TypeReference<List<BenchmarkSource>>() {});
    }

    static Map<String, String> readSastClassToChallenge(Path file) throws IOException {
        if (!Files.exists(file)) {
            return Map.of();
        }
        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree(file.toFile());
        return mapper.convertValue(
                root.path("sastClassToChallenge"), new TypeReference<Map<String, String>>() {});
    }

    private static String shortCommit(String commit) {
        return commit.length() > 12 ? commit.substring(0, 12) : commit;
    }

    private static String requireValue(String[] args, int index) {
        if (index >= args.length) {
            System.err.println(USAGE);
            System.exit(2);
        }
        return args[index];
    }
}
