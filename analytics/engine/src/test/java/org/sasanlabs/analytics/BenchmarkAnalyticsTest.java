package org.sasanlabs.analytics;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.sasanlabs.analytics.model.BenchmarkResult;
import org.sasanlabs.analytics.model.BenchmarkSource;
import org.sasanlabs.analytics.model.FindingRow;
import org.sasanlabs.analytics.model.FindingRow.Status;
import org.sasanlabs.analytics.model.RunRow;

class BenchmarkAnalyticsTest {

    private static final BenchmarkSource ZAP =
            new BenchmarkSource("zap-modern", "ZAP", "Modern UI", "dast", "zap-results.json");
    private static final BenchmarkSource SEMGREP =
            new BenchmarkSource("semgrep", "Semgrep", "p/default", "SAST", "semgrep-results.json");

    private static final String DAST_RESULT =
            """
            {
              "tool": "ZAP", "coverage": 33.3333, "totalExpected": 3,
              "detected": 1, "missed": 2, "unmatched": 1, "someFutureField": true,
              "detectedItems": [
                {"url": "/XSS/LEVEL_1", "type": "REFLECTED_XSS", "cwe": "CWE-79", "method": "GET"}
              ],
              "missedItems": [
                {"url": "/XSS/LEVEL_10", "type": "REFLECTED_XSS", "cwe": "CWE-79"},
                {"url": "/XSS/LEVEL_2", "type": "REFLECTED_XSS", "cwe": "CWE-79"}
              ],
              "unmatchedItems": [{"url": "/", "cwe": "CWE-693", "wascId": "15"}]
            }
            """;

    @Test
    void toRun_recomputesCoverageAndTakesCountsFromTheResult() throws Exception {
        RunRow run =
                BenchmarkAnalytics.toRun(
                        ZAP, BenchmarkAnalytics.parseResult(DAST_RESULT), "2026-10-13", "abc123");

        assertThat(run.toCells())
                .containsExactly(
                        "2026-10-13",
                        "zap-modern",
                        "ZAP",
                        "Modern UI",
                        "DAST",
                        "",
                        "3",
                        "1",
                        "2",
                        "1",
                        "33.33",
                        "abc123");
    }

    @Test
    void coverage_isZeroWithoutGroundTruth() {
        assertThat(BenchmarkAnalytics.coverage(0, 0)).isEqualTo("0.00");
        assertThat(BenchmarkAnalytics.coverage(13, 168)).isEqualTo("7.74");
    }

    @Test
    void toFindings_mapsEveryItemToItsChallenge_inStableOrder() throws Exception {
        List<FindingRow> rows =
                BenchmarkAnalytics.toFindings(
                        ZAP, BenchmarkAnalytics.parseResult(DAST_RESULT), "2026-10-13", Map.of());

        assertThat(rows)
                .extracting(
                        FindingRow::status,
                        FindingRow::challenge,
                        FindingRow::level,
                        FindingRow::cwe)
                .containsExactly(
                        tuple(Status.DETECTED, "XSS", "LEVEL_1", "CWE-79"),
                        tuple(Status.MISSED, "XSS", "LEVEL_2", "CWE-79"),
                        tuple(Status.MISSED, "XSS", "LEVEL_10", "CWE-79"),
                        tuple(Status.UNMATCHED, "", "", "CWE-693"));
        assertThat(rows.get(3).toCells())
                .containsExactly(
                        "2026-10-13",
                        "zap-modern",
                        "ZAP",
                        "Modern UI",
                        "DAST",
                        "unmatched",
                        "",
                        "",
                        "",
                        "CWE-693",
                        "/");
    }

    @Test
    void toFindings_withoutDetectedItems_listsMissedAndUnmatchedOnly() throws Exception {
        BenchmarkResult result =
                BenchmarkAnalytics.parseResult(
                        """
                        {"tool": "Semgrep", "totalExpected": 2, "detected": 1, "missed": 1,
                         "unmatched": 0,
                         "missedItems": [{"type": "SQL Injection", "cwe": "CWE-89",
                           "filePath": "src/main/java/a/Sqli.java", "line": 12}],
                         "unmatchedItems": []}
                        """);

        assertThat(BenchmarkAnalytics.toFindings(SEMGREP, result, "2026-10-13", Map.of()))
                .extracting(FindingRow::status, FindingRow::challenge, FindingRow::location)
                .containsExactly(tuple(Status.MISSED, "Sqli", "src/main/java/a/Sqli.java:12"));
        assertThat(
                        BenchmarkAnalytics.toFindings(
                                SEMGREP, result, "2026-10-13", Map.of("Sqli", "SQLInjection")))
                .extracting(FindingRow::challenge)
                .containsExactly("SQLInjection");
    }

    @Test
    void mergeRuns_keepsOneRowPerBenchmarkAndDay_updatesWinning() {
        RunRow old = run("2026-10-12", "zap-modern", 10);
        RunRow sameDayStale = run("2026-10-13", "zap-modern", 11);
        RunRow other = run("2026-10-13", "semgrep", 18);
        RunRow sameDayFresh = run("2026-10-13", "zap-modern", 13);

        List<RunRow> merged =
                BenchmarkAnalytics.mergeRuns(
                        List.of(sameDayStale, old, other), List.of(sameDayFresh));

        assertThat(merged)
                .extracting(RunRow::runDate, RunRow::benchmark, RunRow::detected)
                .containsExactly(
                        tuple("2026-10-12", "zap-modern", 10),
                        tuple("2026-10-13", "semgrep", 18),
                        tuple("2026-10-13", "zap-modern", 13));
    }

    private static RunRow run(String date, String benchmark, int detected) {
        return new RunRow(
                date, benchmark, "S", "v", "DAST", "", 100, detected, 100 - detected, 0, "", "");
    }
}
