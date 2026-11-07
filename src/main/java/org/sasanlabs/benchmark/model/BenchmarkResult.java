package org.sasanlabs.benchmark.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * Result of comparing a scanner's findings against VulnerableApp's DAST ground truth.
 *
 * <p>{@code coverage} is detected / totalExpected as a percentage, or 0.0 when the ground truth is
 * empty.
 *
 * <p>{@code detectedItems} lists the expected items the scanner found, so consumers can tell which
 * vulnerabilities were detected without fetching the ground truth themselves. It is omitted from
 * the JSON when a caller does not supply it.
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

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonProperty("detectedItems")
    private List<Finding> detectedItems;

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
            @JsonProperty("detectedItems") List<Finding> detectedItems,
            @JsonProperty("missedItems") List<Finding> missedItems,
            @JsonProperty("unmatchedItems") List<Finding> unmatchedItems) {
        this.tool = tool;
        this.coverage = coverage;
        this.totalExpected = totalExpected;
        this.detected = detected;
        this.missed = missed;
        this.unmatched = unmatched;
        this.detectedItems = detectedItems;
        this.missedItems = missedItems;
        this.unmatchedItems = unmatchedItems;
    }

    public BenchmarkResult(
            String tool,
            double coverage,
            int totalExpected,
            int detected,
            int missed,
            int unmatched,
            List<Finding> missedItems,
            List<Finding> unmatchedItems) {
        this(
                tool,
                coverage,
                totalExpected,
                detected,
                missed,
                unmatched,
                null,
                missedItems,
                unmatchedItems);
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

    public List<Finding> getDetectedItems() {
        return detectedItems;
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
