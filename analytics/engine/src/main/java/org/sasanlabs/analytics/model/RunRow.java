package org.sasanlabs.analytics.model;

import java.util.List;

/** One row of {@code runs.csv}: the summary of one benchmark on one day. */
public record RunRow(
        String runDate,
        String benchmark,
        String scanner,
        String variant,
        String scanType,
        String scannerVersion,
        int totalExpected,
        int detected,
        int missed,
        int unmatched,
        String coverage,
        String sourceCommit) {

    public static final List<String> HEADER =
            List.of(
                    "run_date",
                    "benchmark",
                    "scanner",
                    "variant",
                    "scan_type",
                    "scanner_version",
                    "total_expected",
                    "detected",
                    "missed",
                    "unmatched",
                    "coverage",
                    "source_commit");

    public List<String> toCells() {
        return List.of(
                runDate,
                benchmark,
                scanner,
                variant,
                scanType,
                scannerVersion,
                Integer.toString(totalExpected),
                Integer.toString(detected),
                Integer.toString(missed),
                Integer.toString(unmatched),
                coverage,
                sourceCommit);
    }

    public static RunRow fromCells(List<String> c) {
        return new RunRow(
                c.get(0),
                c.get(1),
                c.get(2),
                c.get(3),
                c.get(4),
                c.get(5),
                Integer.parseInt(c.get(6)),
                Integer.parseInt(c.get(7)),
                Integer.parseInt(c.get(8)),
                Integer.parseInt(c.get(9)),
                c.get(10),
                c.get(11));
    }

    /** Identity of a row: a benchmark has at most one row per day. */
    public String key() {
        return runDate + "|" + benchmark;
    }
}
