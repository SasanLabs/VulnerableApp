package org.sasanlabs.benchmark.model;

/**
 * One row of SAST ground truth — a known vulnerability site in VulnerableApp's source tree, as
 * declared by {@code scanner/sast/expectedIssues.csv}. Used by the SAST benchmark strategy to
 * compare scanner findings against expected issues.
 */
public class ExpectedIssue {

    private final String cwe;
    private final String vulnerabilityType;
    private final String filePath;
    private final int line;
    private final int numberOfSources;

    public ExpectedIssue(
            String cwe, String vulnerabilityType, String filePath, int line, int numberOfSources) {
        this.cwe = cwe;
        this.vulnerabilityType = vulnerabilityType;
        this.filePath = filePath;
        this.line = line;
        this.numberOfSources = numberOfSources;
    }

    public String getCwe() {
        return cwe;
    }

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
}
