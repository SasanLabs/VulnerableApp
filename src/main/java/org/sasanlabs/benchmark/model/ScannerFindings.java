package org.sasanlabs.benchmark.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * Tool-agnostic input payload for the benchmark endpoint. {@code scanType} selects which benchmark
 * strategy runs — defaults to {@link ScanType#DAST} when omitted, preserving the original DAST-only
 * request shape.
 */
public class ScannerFindings {

    @JsonProperty("tool")
    private String tool;

    @JsonProperty("scanType")
    private ScanType scanType;

    @JsonProperty("findings")
    private List<Finding> findings;

    @JsonCreator
    public ScannerFindings(
            @JsonProperty("tool") String tool,
            @JsonProperty("scanType") ScanType scanType,
            @JsonProperty("findings") List<Finding> findings) {
        this.tool = tool;
        this.scanType = (scanType != null) ? scanType : ScanType.DAST;
        this.findings = findings;
    }

    public ScannerFindings(String tool, List<Finding> findings) {
        this(tool, null, findings);
    }

    public String getTool() {
        return tool;
    }

    public ScanType getScanType() {
        return scanType;
    }

    public List<Finding> getFindings() {
        return findings;
    }
}
