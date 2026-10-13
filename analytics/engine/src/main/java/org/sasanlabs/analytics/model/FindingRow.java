package org.sasanlabs.analytics.model;

import java.util.List;

/** One row of {@code findings.csv}: one item of a benchmark's latest result. */
public record FindingRow(
        String runDate,
        String benchmark,
        String scanner,
        String variant,
        String scanType,
        Status status,
        String challenge,
        String level,
        String vulnerabilityType,
        String cwe,
        String location) {

    /** Whether an expected item was found or missed, or a reported item matched nothing. */
    public enum Status {
        DETECTED,
        MISSED,
        UNMATCHED;

        public String label() {
            return name().toLowerCase();
        }
    }

    public static final List<String> HEADER =
            List.of(
                    "run_date",
                    "benchmark",
                    "scanner",
                    "variant",
                    "scan_type",
                    "status",
                    "challenge",
                    "level",
                    "vulnerability_type",
                    "cwe",
                    "location");

    public List<String> toCells() {
        return List.of(
                runDate,
                benchmark,
                scanner,
                variant,
                scanType,
                status.label(),
                challenge,
                level,
                vulnerabilityType,
                cwe,
                location);
    }
}
