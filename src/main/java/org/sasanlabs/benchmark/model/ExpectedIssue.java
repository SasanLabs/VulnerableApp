package org.sasanlabs.benchmark.model;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * One row of SAST ground truth — a known vulnerability site in VulnerableApp's source tree, as
 * declared by {@code scanner/sast/expectedIssues.csv}. Used by the SAST benchmark strategy to
 * compare scanner findings against expected issues, and served as-is by {@code /scanner/sast}.
 */
public class ExpectedIssue {

    private final String cwe;
    private final String vulnerabilityType;
    private final String filePath;
    private final int line;
    private final int numberOfSources;
    private final int lineCount;

    public ExpectedIssue(
            String cwe, String vulnerabilityType, String filePath, int line, int numberOfSources) {
        this(cwe, vulnerabilityType, filePath, line, numberOfSources, 1);
    }

    public ExpectedIssue(
            String cwe,
            String vulnerabilityType,
            String filePath,
            int line,
            int numberOfSources,
            int lineCount) {
        this.cwe = cwe;
        this.vulnerabilityType = vulnerabilityType;
        this.filePath = filePath;
        this.line = line;
        this.numberOfSources = numberOfSources;
        this.lineCount = lineCount;
    }

    public String getCwe() {
        return cwe;
    }

    /**
     * Serialised as {@code type}, matching the scanner-facing contract shared with {@code Finding}.
     */
    @JsonProperty("type")
    public String getVulnerabilityType() {
        return vulnerabilityType;
    }

    public String getFilePath() {
        return filePath;
    }

    public int getLine() {
        return line;
    }

    public int getNumberOfSources() {
        return numberOfSources;
    }

    /**
     * Number of lines the vulnerable statement occupies, starting at {@link #getLine()}. A finding
     * on any line from {@code line} to {@code line + lineCount - 1} matches this row.
     */
    public int getLineCount() {
        return lineCount;
    }
}
