package org.sasanlabs.benchmark.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * Result of comparing a scanner's findings against VulnerableApp's DAST ground truth.
 *
 * <p>{@code coverage} is detected / totalExpected as a percentage, or 0.0 when the ground truth is
 * empty.
 */
public class BenchmarkResult {

    @JsonProperty("tool")
    private String tool;

    @JsonProperty("coverage")
    private double coverage;

    @JsonProperty("totalExpected")
    private int totalExpected;

    @JsonProperty("detected")
    private int detected;

    @JsonProperty("missed")
    private int missed;

    @JsonProperty("unmatched")
    private int unmatched;

    @JsonProperty("missedItems")
    private List<Finding> missedItems;

    @JsonProperty("unmatchedItems")
    private List<Finding> unmatchedItems;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonProperty("persistenceError")
    private String persistenceError;

    public BenchmarkResult(
            @JsonProperty("tool") String tool,
            @JsonProperty("coverage") double coverage,
            @JsonProperty("totalExpected") int totalExpected,
            @JsonProperty("detected") int detected,
            @JsonProperty("missed") int missed,
            @JsonProperty("unmatched") int unmatched,
            @JsonProperty("missedItems") List<Finding> missedItems,
            @JsonProperty("unmatchedItems") List<Finding> unmatchedItems) {
        this.tool = tool;
        this.coverage = coverage;
        this.totalExpected = totalExpected;
        this.detected = detected;
        this.missed = missed;
        this.unmatched = unmatched;
        this.missedItems = missedItems;
        this.unmatchedItems = unmatchedItems;
    }

    public String getTool() {
        return tool;
    }

    public double getCoverage() {
        return coverage;
    }

    public int getTotalExpected() {
        return totalExpected;
    }

    public int getDetected() {
        return detected;
    }

    public int getMissed() {
        return missed;
    }

    public int getUnmatched() {
        return unmatched;
    }

    public List<Finding> getMissedItems() {
        return missedItems;
    }

    public List<Finding> getUnmatchedItems() {
        return unmatchedItems;
    }

    public String getPersistenceError() {
        return persistenceError;
    }

    public void setPersistenceError(String persistenceError) {
        this.persistenceError = persistenceError;
    }
}
