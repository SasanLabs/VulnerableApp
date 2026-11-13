package org.sasanlabs.benchmark.model;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * One row of SAST ground truth — a known vulnerability site in VulnerableApp's source tree, as
 * declared by {@code scanner/sast/expectedIssues.csv}. Used by the SAST benchmark strategy to
 * compare scanner findings against expected issues, and served as-is by {@code /scanner/sast}.
 */
public record ExpectedIssue(String cwe, String vulnerabilityType, String filePath, int line, int numberOfSources,
                            int lineCount) {

    public ExpectedIssue(
            String cwe, String vulnerabilityType, String filePath, int line, int numberOfSources) {
        this(cwe, vulnerabilityType, filePath, line, numberOfSources, 1);
    }

    /**
     * Serialised as {@code type}, matching the scanner-facing contract shared with {@code Finding}.
     */
    @Override
    @JsonProperty("type")
    public String vulnerabilityType() {
        return vulnerabilityType;
    }

    /**
     * Number of lines the vulnerable statement occupies, starting at {@link #line ()}. A finding
     * on any line from {@code line} to {@code line + lineCount - 1} matches this row.
     */
    @Override
    public int lineCount() {
        return lineCount;
    }

    @Override
    public boolean equals(Object other) {
        return this == other;
    }

    @Override
    public int hashCode() {
        return System.identityHashCode(this);
    }
}
