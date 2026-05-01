package org.sasanlabs.benchmark.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/** Tool-agnostic input payload for the benchmark endpoint. */
public class ScannerFindings {

    @JsonProperty("tool")
    private String tool;

    @JsonProperty("findings")
    private List<Finding> findings;

    public ScannerFindings(
            @JsonProperty("tool") String tool, @JsonProperty("findings") List<Finding> findings) {
        this.tool = tool;
        this.findings = findings;
    }

    public String getTool() {
        return tool;
    }

    public List<Finding> getFindings() {
        return findings;
    }
}
