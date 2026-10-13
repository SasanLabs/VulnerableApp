package org.sasanlabs.analytics;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.sasanlabs.analytics.mapping.ChallengeMapper;
import org.sasanlabs.analytics.mapping.ChallengeRef;
import org.sasanlabs.analytics.model.BenchmarkResult;
import org.sasanlabs.analytics.model.BenchmarkSource;
import org.sasanlabs.analytics.model.Finding;
import org.sasanlabs.analytics.model.FindingRow;
import org.sasanlabs.analytics.model.FindingRow.Status;
import org.sasanlabs.analytics.model.RunRow;

/** Turns scanner benchmark results into the rows of the standardized dataset. */
public final class BenchmarkAnalytics {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final Pattern LEVEL_NUMBER = Pattern.compile("LEVEL_(\\d+)");

    private BenchmarkAnalytics() {}

    public static BenchmarkResult parseResult(String json) throws IOException {
        return MAPPER.readValue(json, BenchmarkResult.class);
    }

    public static RunRow toRun(
            BenchmarkSource source, BenchmarkResult result, String runDate, String commit) {
        return new RunRow(
                runDate,
                source.id(),
                source.scanner(),
                source.variant(),
                source.scanType().toUpperCase(Locale.ROOT),
                nullToEmpty(result.scannerVersion()),
                result.totalExpected(),
                result.detected(),
                result.missed(),
                result.unmatched(),
                coverage(result.detected(), result.totalExpected()),
                commit);
    }

    /**
     * Detected, missed and unmatched rows of one result, each mapped to its challenge.
     *
     * @param sastClassToChallenge challenge names for SAST classes named differently
     */
    public static List<FindingRow> toFindings(
            BenchmarkSource source,
            BenchmarkResult result,
            String runDate,
            Map<String, String> sastClassToChallenge) {
        ChallengeMapper mapper = ChallengeMapper.forScanType(source.isSast(), sastClassToChallenge);
        List<FindingRow> rows = new ArrayList<>();
        addFindings(rows, source, runDate, mapper, Status.DETECTED, result.detectedItems());
        addFindings(rows, source, runDate, mapper, Status.MISSED, result.missedItems());
        addFindings(rows, source, runDate, mapper, Status.UNMATCHED, result.unmatchedItems());
        rows.sort(FINDING_ORDER);
        return rows;
    }

    /**
     * Upserts {@code updates} into {@code existing}: a benchmark keeps one row per day, the update
     * winning. Sorted by date, then benchmark, so the file only grows at the end.
     */
    public static List<RunRow> mergeRuns(List<RunRow> existing, List<RunRow> updates) {
        Map<String, RunRow> byKey = new LinkedHashMap<>();
        for (RunRow row : existing) {
            byKey.put(row.key(), row);
        }
        for (RunRow row : updates) {
            byKey.put(row.key(), row);
        }
        List<RunRow> merged = new ArrayList<>(byKey.values());
        merged.sort(Comparator.comparing(RunRow::runDate).thenComparing(RunRow::benchmark));
        return merged;
    }

    /**
     * The rows of {@code benchmark} in a previously written findings file, so a benchmark whose
     * results file is missing keeps its last published findings instead of disappearing.
     */
    public static List<FindingRow> previousFindings(List<FindingRow> existing, String benchmark) {
        return existing.stream().filter(row -> row.benchmark().equals(benchmark)).toList();
    }

    /** Detection coverage as a percentage with two decimals; {@code 0.00} without ground truth. */
    static String coverage(int detected, int totalExpected) {
        double value = totalExpected == 0 ? 0.0 : detected * 100.0 / totalExpected;
        return String.format(Locale.ROOT, "%.2f", value);
    }

    private static void addFindings(
            List<FindingRow> rows,
            BenchmarkSource source,
            String runDate,
            ChallengeMapper mapper,
            Status status,
            List<Finding> findings) {
        if (findings == null) {
            return;
        }
        for (Finding finding : findings) {
            if (finding == null) {
                continue;
            }
            ChallengeRef ref = mapper.map(finding);
            rows.add(
                    new FindingRow(
                            runDate,
                            source.id(),
                            source.scanner(),
                            source.variant(),
                            source.scanType().toUpperCase(Locale.ROOT),
                            status,
                            ref.challenge(),
                            ref.level(),
                            nullToEmpty(finding.type()),
                            nullToEmpty(finding.cwe()),
                            mapper.location(finding)));
        }
    }

    /** Stable order, so a re-run with the same results produces an identical file. */
    private static final Comparator<FindingRow> FINDING_ORDER =
            Comparator.comparing(FindingRow::status)
                    .thenComparing(FindingRow::challenge)
                    .thenComparingInt(row -> levelNumber(row.level()))
                    .thenComparing(FindingRow::location)
                    .thenComparing(FindingRow::vulnerabilityType)
                    .thenComparing(FindingRow::cwe);

    private static int levelNumber(String level) {
        Matcher m = LEVEL_NUMBER.matcher(level);
        return m.matches() ? Integer.parseInt(m.group(1)) : -1;
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
