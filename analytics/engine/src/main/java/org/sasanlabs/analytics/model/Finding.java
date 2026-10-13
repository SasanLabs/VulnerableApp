package org.sasanlabs.analytics.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * One item of a benchmark result, as written by VulnerableApp's {@code /scanner/benchmark}
 * endpoint. DAST items carry {@code url}; SAST items carry {@code filePath} and {@code line}.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record Finding(
        String url,
        String type,
        String filePath,
        Integer line,
        String cwe,
        String wascId,
        String method) {}
