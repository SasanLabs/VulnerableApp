package org.sasanlabs.analytics.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

/**
 * A scanner's benchmark result file ({@code benchmarks/<Tool>/*-results.json}). Every scanner
 * integration produces this shape, which is what lets the engine handle a new scanner without code
 * changes.
 *
 * <p>{@code detectedItems} is {@code null} in results written before the endpoint started listing
 * them. {@code scannerVersion} is {@code null} until scanner integrations record it.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record BenchmarkResult(
        String tool,
        String scannerVersion,
        int totalExpected,
        int detected,
        int missed,
        int unmatched,
        List<Finding> detectedItems,
        List<Finding> missedItems,
        List<Finding> unmatchedItems) {}
